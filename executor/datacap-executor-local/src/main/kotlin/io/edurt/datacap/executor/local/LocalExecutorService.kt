package io.edurt.datacap.executor.local

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.common.sql.SqlBuilder
import io.edurt.datacap.common.sql.configure.SqlBody
import io.edurt.datacap.common.sql.configure.SqlColumn
import io.edurt.datacap.common.sql.configure.SqlType
import io.edurt.datacap.executor.ExecutorService
import io.edurt.datacap.executor.common.RunState
import io.edurt.datacap.executor.configure.ExecutorProgressListener
import io.edurt.datacap.executor.configure.ExecutorRequest
import io.edurt.datacap.executor.configure.ExecutorResponse
import io.edurt.datacap.executor.configure.OriginColumn
import io.edurt.datacap.lib.logger.LoggerExecutor
import io.edurt.datacap.lib.logger.logback.LogbackExecutor
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.adapter.BatchWriter
import io.edurt.datacap.spi.adapter.RowCallback
import io.edurt.datacap.spi.model.Configure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "MS_MUTABLE_COLLECTION_PKGPROTECT"])
class LocalExecutorService : ExecutorService
{
    private val log = LoggerFactory.getLogger(LocalExecutorService::class.java)

    /**
     * 已注册的活跃任务句柄。
     * - cancelled: 调度循环每条行检查
     * - sourceStatement: stop() 会调 cancel() 立即终止源端 JDBC 查询，不必等下一行
     * - taskLog: 让 stop() 把"用户停止"事件写到任务专属日志里
     * - rowsAtStop: 记录被停止时已处理的行数，供 history 落库
     */
    private class TaskHandle
    {
        val cancelled: AtomicBoolean = AtomicBoolean(false)
        @Volatile var sourceStatement: java.sql.Statement? = null
        @Volatile var taskLog: Logger? = null
        @Volatile var rowsAtStop: Long = 0L
    }

    override fun start(request: ExecutorRequest): ExecutorResponse
    {
        val response = ExecutorResponse()
        val loggerExecutor: LoggerExecutor<*>? = newTaskLogger(request)
        val taskLog: Logger = loggerExecutor?.getLogger() ?: log
        val handle = TaskHandle()
        handle.taskLog = taskLog
        if (request.taskName.isNotBlank())
        {
            runningTasks[request.taskName] = handle
        }
        try
        {
            taskLog.info("Local executor task starting: task={} user={}", request.taskName, request.userName)

            val input = request.input
            val output = request.output
            val inputPlugin = input.plugin ?: throw IllegalArgumentException("Input plugin is null")
            val outputPlugin = output.plugin ?: throw IllegalArgumentException("Output plugin is null")
            val inputConfigure = input.originConfigure ?: throw IllegalArgumentException("Input configure is null")
            val outputConfigure = output.originConfigure ?: throw IllegalArgumentException("Output configure is null")
            // DataCap 约定：目标库 / 目标表 由调用方放在 input 上（见 DataSetServiceImpl）
            val database = input.database ?: throw IllegalArgumentException("Target database is null")
            val table = input.table ?: throw IllegalArgumentException("Target table is null")
            val query = input.query ?: throw IllegalArgumentException("Input query is null")

            val originColumns = input.originColumns.toList()
            if (originColumns.isEmpty())
            {
                throw IllegalArgumentException("Input column mapping is empty")
            }
            val targetColumns = originColumns.map { it.name }
            val sourceKeys = originColumns.map { it.original }

            val fetchSize = if (request.fetchSize > 0) request.fetchSize else DEFAULT_FETCH_SIZE
            val batchSize = if (request.batchSize > 0) request.batchSize else DEFAULT_BATCH_SIZE
            val progressListener = request.progressListener

            taskLog.info(
                    "Resolved input plugin={} output plugin={} streaming={}/{} fetchSize={} batchSize={} preCount={}",
                    inputPlugin.name(), outputPlugin.name(),
                    inputPlugin.supportsStreaming(), outputPlugin.supportsStreaming(),
                    fetchSize, batchSize, request.preCount
            )
            taskLog.info("Source query: {}", query)

            // 可选：先跑 SELECT COUNT(*) 拿到源端总行数，作为进度分母
            // Optional: pre-count to populate the total row count used for progress percentage
            val totalCount: Long = if (request.preCount) preCount(inputPlugin, inputConfigure, query, taskLog) else -1L
            if (totalCount >= 0)
            {
                taskLog.info("Pre-count: source total rows = {}", totalCount)
                // 报告 0 进度，让上层立即知道 total
                progressListener?.onProgress(0L, totalCount)
            }

            val written = if (inputPlugin.supportsStreaming() && outputPlugin.supportsStreaming())
            {
                runStreaming(
                    inputPlugin, inputConfigure, query, fetchSize,
                    outputPlugin, outputConfigure, database, table,
                    targetColumns, sourceKeys, batchSize, taskLog,
                    totalCount, progressListener, handle
                )
            }
            else
            {
                runLegacy(
                    inputPlugin, inputConfigure, query,
                    outputPlugin, outputConfigure, database, table,
                    originColumns, batchSize, taskLog,
                    totalCount, progressListener, handle
                )
            }

            response.count = if (written > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else written.toInt()
            response.successful = true
            response.state = RunState.SUCCESS
            // 最终上报一次：确保 100%
            progressListener?.onProgress(written, if (totalCount >= 0) totalCount else written)
            taskLog.info("Local executor task completed: rows={} state=SUCCESS", written)
        }
        catch (ex: TaskCancelledException)
        {
            taskLog.warn("Local executor task stopped by user: rows={} task={}", ex.processed, request.taskName)
            response.count = if (ex.processed > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else ex.processed.toInt()
            response.successful = false
            response.state = RunState.STOPPED
            response.message = "Stopped by user"
        }
        catch (ex: Exception)
        {
            // 取消可能以包装异常的形式抛出（例如 SQLException by Statement.cancel()），通过 handle 反向判别
            if (handle.cancelled.get())
            {
                val rows = handle.rowsAtStop
                taskLog.warn("Local executor task stopped by user (via JDBC cancel): rows≈{} task={}", rows, request.taskName)
                response.count = if (rows > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else rows.toInt()
                response.successful = false
                response.state = RunState.STOPPED
                response.message = "Stopped by user"
            }
            else
            {
                taskLog.error("Local executor failed", ex)
                response.successful = false
                response.state = RunState.FAILURE
                response.message = ex.message
            }
        }
        finally
        {
            if (request.taskName.isNotBlank())
            {
                runningTasks.remove(request.taskName, handle)
            }
            loggerExecutor?.destroy()
        }
        return response
    }

    /** 取消传播专用异常，携带已处理行数便于上游记录 */
    private class TaskCancelledException(val processed: Long) : RuntimeException("Task cancelled by user")

    private fun newTaskLogger(request: ExecutorRequest): LoggerExecutor<*>?
    {
        val workHome = request.workHome
        val taskName = request.taskName
        if (workHome.isNullOrBlank() || taskName.isBlank())
        {
            return null
        }
        return try
        {
            val dir = java.io.File(workHome)
            if (!dir.exists())
            {
                dir.mkdirs()
            }
            LogbackExecutor(workHome, "$taskName.log")
        }
        catch (ex: Exception)
        {
            log.warn("Create task logger at {} failed: {}", workHome, ex.message)
            null
        }
    }

    override fun stop(request: ExecutorRequest): ExecutorResponse
    {
        val taskName = request.taskName
        if (taskName.isBlank())
        {
            return ExecutorResponse(false, false, RunState.FAILURE, "taskName is required")
        }
        val handle = runningTasks[taskName]
        if (handle == null)
        {
            return ExecutorResponse(false, false, RunState.FAILURE, "Task [ $taskName ] is not running on this node")
        }
        // 设标志位 + 写日志：纯内存操作，立即完成
        handle.cancelled.set(true)
        log.info("Cancel requested for task [ {} ]", taskName)
        handle.taskLog?.warn("Stop requested by user for task [ {} ]", taskName)
        // Statement.cancel() 实现里通常会新开一个 JDBC 连接发 KILL QUERY，
        // 如果源 DB 网络异常会阻塞 HTTP 请求线程。所以放到独立线程，不让 HTTP 等
        val stmt = handle.sourceStatement
        if (stmt != null)
        {
            cancelExecutor.submit {
                try
                {
                    if (!stmt.isClosed)
                    {
                        stmt.cancel()
                        handle.taskLog?.warn("Source JDBC statement cancelled for task [ {} ]", taskName)
                    }
                }
                catch (ex: Exception)
                {
                    log.warn("Cancel source statement failed: {}", ex.message)
                    handle.taskLog?.warn("Cancel source statement failed: {}", ex.message)
                }
            }
        }
        return ExecutorResponse(false, true, RunState.STOPPED, null)
    }

    /**
     * 跑 SELECT COUNT(*) FROM (userQuery) 估算总行数。失败时返回 -1，不影响后续流程。
     * Run SELECT COUNT(*) over the user's query. On failure returns -1 — progress just lacks the denominator.
     */
    private fun preCount(plugin: PluginService, configure: Configure, query: String, taskLog: Logger): Long
    {
        return try
        {
            val trimmed = query.trim().trimEnd(';')
            val sql = "SELECT COUNT(*) AS total FROM ($trimmed) datacap_precount_t"
            taskLog.info("Pre-count starting: {}", sql)
            val t0 = System.nanoTime()
            val result = plugin.execute(configure, sql)
            val elapsedMs = (System.nanoTime() - t0) / 1_000_000
            taskLog.info("Pre-count returned in {} ms", elapsedMs)
            if (result.isSuccessful != true)
            {
                taskLog.warn("Pre-count failed, will run without total: {}", result.message)
                return -1L
            }
            val rows = result.columns ?: return -1L
            if (rows.isEmpty()) return -1L
            val first = rows[0]
            when (first)
            {
                is com.fasterxml.jackson.databind.node.ObjectNode -> first.get("total")?.asLong(-1L) ?: -1L
                is List<*> -> (first.firstOrNull() as? Number)?.toLong() ?: -1L
                is Number -> first.toLong()
                else -> -1L
            }
        }
        catch (ex: Exception)
        {
            taskLog.warn("Pre-count threw, will run without total: {}", ex.message)
            -1L
        }
    }

    /**
     * 流式路径：源端 fetchSize 拉取，目标端 PreparedStatement 批量写。
     * 全程不在 JVM 内物化整个结果集。
     */
    private fun runStreaming(
        inputPlugin: PluginService,
        inputConfigure: Configure,
        query: String,
        fetchSize: Int,
        outputPlugin: PluginService,
        outputConfigure: Configure,
        database: String,
        table: String,
        targetColumns: List<String>,
        sourceKeys: List<String>,
        batchSize: Int,
        taskLog: Logger,
        totalCount: Long,
        progressListener: ExecutorProgressListener?,
        handle: TaskHandle
    ): Long
    {
        taskLog.info(
                "Streaming sync start: target=`{}`.`{}` columns={} fetchSize={} batchSize={} total={}",
                database, table, targetColumns.size, fetchSize, batchSize, totalCount
        )
        val startNanos = System.nanoTime()
        var written = 0L
        val writer: BatchWriter = outputPlugin.openBatchWriter(
            outputConfigure, database, table, targetColumns, batchSize
        )
        writer.use { writer ->
            val indexByHeader = HashMap<String, Int>()
            inputPlugin.executeStream(inputConfigure, query, fetchSize, object : RowCallback
            {
                override fun onSchema(headers: List<String>, types: List<String>)
                {
                    indexByHeader.clear()
                    headers.forEachIndexed { i, h -> indexByHeader[h.lowercase()] = i }
                    taskLog.info("Streaming sync: source returned headers={}", headers)
                }

                override fun onStatement(statement: java.sql.Statement)
                {
                    handle.sourceStatement = statement
                }

                override fun onRow(row: List<Any?>)
                {
                    if (handle.cancelled.get())
                    {
                        throw TaskCancelledException(written)
                    }
                    val projected = ArrayList<Any?>(sourceKeys.size)
                    for (key in sourceKeys)
                    {
                        val idx = indexByHeader[key.lowercase()]
                            ?: throw IllegalStateException("Source column '$key' not found in query result")
                        projected.add(row[idx])
                    }
                    writer.addRow(projected)
                    written ++
                    if (written % PROGRESS_INTERVAL == 0L)
                    {
                        val seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
                        val rps = if (seconds > 0) (written / seconds).toLong() else 0L
                        taskLog.info(
                                "Streaming sync progress: read={} committed={} elapsed={}s rps={}",
                                written, writer.writtenCount(), "%.1f".format(seconds), rps
                        )
                        progressListener?.onProgress(writer.writtenCount(), totalCount)
                        // 顺手记录最近一次进度行数，给 stop 后 catch 取整时使用
                        handle.rowsAtStop = writer.writtenCount()
                    }
                }
            })
        }
        // 某些驱动在 Statement.cancel() 后直接让 rs.next() 返回 false（不抛异常），需要在循环结束后再判一次
        if (handle.cancelled.get())
        {
            throw TaskCancelledException(written)
        }
        val totalSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
        taskLog.info(
                "Streaming sync done: rows={} committed={} elapsed={}s",
                written, writer.writtenCount(), "%.1f".format(totalSeconds)
        )
        return written
    }

    /**
     * 回退路径：源或汇任一不支持流式（如 HTTP / Native 插件）。仍然使用旧的全量读取，
     * 但目标端按 batch 切片提交，避免一次性拼接巨大 SQL 字符串；同时修复 NULL、类型、转义问题。
     */
    private fun runLegacy(
        inputPlugin: PluginService,
        inputConfigure: Configure,
        query: String,
        outputPlugin: PluginService,
        outputConfigure: Configure,
        database: String,
        table: String,
        originColumns: List<OriginColumn>,
        batchSize: Int,
        taskLog: Logger,
        totalCount: Long,
        progressListener: ExecutorProgressListener?,
        handle: TaskHandle
    ): Long
    {
        taskLog.info("Legacy sync start: target=`{}`.`{}` batchSize={}", database, table, batchSize)
        val startNanos = System.nanoTime()
        val inputResult = inputPlugin.execute(inputConfigure, query)
        if (inputResult.isSuccessful != true)
        {
            throw RuntimeException(inputResult.message ?: "Input plugin failed")
        }
        val rows = inputResult.columns ?: return 0L
        taskLog.info("Legacy sync: source materialized {} rows", rows.size)
        // 全量路径已经能拿到精确总数，覆盖 pre-count 的估算
        val effectiveTotal = if (totalCount >= 0) totalCount else rows.size.toLong()

        // 目标端能流式：用 BatchWriter 安全 + 节省内存
        if (outputPlugin.supportsStreaming())
        {
            val targetColumns = originColumns.map { it.name }
            val sourceKeys = originColumns.map { it.original }
            var written = 0L
            val writer = outputPlugin.openBatchWriter(
                outputConfigure, database, table, targetColumns, batchSize
            )
            writer.use { writer ->
                for (item in rows)
                {
                    if (handle.cancelled.get())
                    {
                        throw TaskCancelledException(written)
                    }
                    val node = item as? ObjectNode ?: continue
                    val projected = ArrayList<Any?>(sourceKeys.size)
                    for (key in sourceKeys)
                    {
                        projected.add(jsonNodeToJdbcValue(node.get(key)))
                    }
                    writer.addRow(projected)
                    written ++
                    if (written % PROGRESS_INTERVAL == 0L)
                    {
                        taskLog.info("Legacy sync progress: written={} committed={}", written, writer.writtenCount())
                        progressListener?.onProgress(writer.writtenCount(), effectiveTotal)
                    }
                }
            }
            val totalSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
            taskLog.info("Legacy sync done: rows={} committed={} elapsed={}s", written, writer.writtenCount(), "%.1f".format(totalSeconds))
            return written
        }

        // 双端都不支持流式：用 INSERT 字符串，但按 batch 提交，不再拼一个巨大字符串
        var written = 0L
        val batch = ArrayList<String>(batchSize)
        for (item in rows)
        {
            if (handle.cancelled.get())
            {
                throw TaskCancelledException(written)
            }
            val node = item as? ObjectNode ?: continue
            val sqlColumns = ArrayList<SqlColumn>(originColumns.size)
            for (col in originColumns)
            {
                sqlColumns.add(
                    SqlColumn.builder()
                        .column("`${col.name}`")
                        .value(formatSqlLiteral(node.get(col.original)))
                        .build()
                )
            }
            val body = SqlBody.builder()
                .type(SqlType.INSERT)
                .database(database)
                .table(table)
                .columns(sqlColumns)
                .build()
            batch.add(SqlBuilder(body).sql)
            if (batch.size >= batchSize)
            {
                flushLegacyBatch(outputPlugin, outputConfigure, batch)
                written += batch.size
                batch.clear()
                if (written % PROGRESS_INTERVAL == 0L)
                {
                    taskLog.info("Legacy sync progress (sql batch): written={}", written)
                    progressListener?.onProgress(written, effectiveTotal)
                }
            }
        }
        if (batch.isNotEmpty())
        {
            flushLegacyBatch(outputPlugin, outputConfigure, batch)
            written += batch.size
            batch.clear()
        }
        val totalSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
        taskLog.info("Legacy sync done: rows={} elapsed={}s", written, "%.1f".format(totalSeconds))
        return written
    }

    private fun flushLegacyBatch(plugin: PluginService, configure: Configure, batch: List<String>)
    {
        val joined = batch.joinToString("\n")
        val result = plugin.execute(configure, joined)
        if (result.isSuccessful != true)
        {
            throw RuntimeException(result.message ?: "Output plugin failed")
        }
    }

    /**
     * 把 JsonNode 转成 JDBC 可识别的真实类型；保留 NULL 语义。
     */
    private fun jsonNodeToJdbcValue(node: JsonNode?): Any?
    {
        if (node == null || node.isNull) return null
        return when
        {
            node.isBoolean -> node.asBoolean()
            node.isInt -> node.asInt()
            node.isLong -> node.asLong()
            node.isBigInteger -> node.bigIntegerValue()
            node.isFloat || node.isDouble -> node.asDouble()
            node.isBigDecimal -> node.decimalValue()
            node.isBinary ->
            {
                try
                {
                    node.binaryValue()
                }
                catch (e: Exception)
                {
                    node.asText()
                }
            }

            else -> node.asText()
        }
    }

    /**
     * 旧路径下需要拼 SQL 字符串：按类型生成字面量，正确处理 NULL / 数字 / 布尔 / 字符串转义。
     * 仅在双端均不支持流式（HTTP / Native 输出插件）时使用。
     */
    private fun formatSqlLiteral(node: JsonNode?): String
    {
        if (node == null || node.isNull) return "NULL"
        return when
        {
            node.isBoolean -> if (node.asBoolean()) "TRUE" else "FALSE"
            node.isIntegralNumber -> node.asLong().toString()
            node.isFloatingPointNumber || node.isBigDecimal -> node.asText()
            else -> "'${escapeSqlString(node.asText())}'"
        }
    }

    private fun escapeSqlString(s: String): String
    {
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length + 4)
        for (c in s)
        {
            when (c)
            {
                '\'' -> sb.append("''")
                '\\' -> sb.append("\\\\")
                '\u0000' ->
                {
                    // SQL 不允许 NUL 字符，跳过以避免协议层错误
                }

                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    companion object
    {
        private const val DEFAULT_FETCH_SIZE = 1000
        private const val DEFAULT_BATCH_SIZE = 1000
        private const val PROGRESS_INTERVAL = 1_000L

        // 进程内活跃任务表。多个并发 sync 共享同一 LocalExecutorService 实例，
        // 用 taskName 唯一索引；stop() 通过 taskName 查到 handle 后设置取消标志位
        private val runningTasks: ConcurrentHashMap<String, TaskHandle> = ConcurrentHashMap()

        // Statement.cancel() 可能会阻塞（驱动会新开连接发 KILL），放到独立线程跑避免拖住调用方
        private val cancelExecutor: java.util.concurrent.ExecutorService =
                java.util.concurrent.Executors.newCachedThreadPool { r ->
                    val t = Thread(r, "local-executor-cancel")
                    t.isDaemon = true
                    t
                }
    }
}
