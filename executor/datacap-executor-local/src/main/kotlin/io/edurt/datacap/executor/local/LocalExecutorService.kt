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
import io.edurt.datacap.executor.configure.ExecutorRequest
import io.edurt.datacap.executor.configure.ExecutorResponse
import io.edurt.datacap.executor.configure.OriginColumn
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.adapter.BatchWriter
import io.edurt.datacap.spi.adapter.RowCallback
import io.edurt.datacap.spi.model.Configure
import org.slf4j.LoggerFactory

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class LocalExecutorService : ExecutorService
{
    private val log = LoggerFactory.getLogger(LocalExecutorService::class.java)

    override fun start(request: ExecutorRequest): ExecutorResponse
    {
        val response = ExecutorResponse()
        try
        {
            val input = request.input
            val output = request.output
            val inputPlugin = input.plugin ?: throw IllegalArgumentException("Input plugin is null")
            val outputPlugin = output.plugin ?: throw IllegalArgumentException("Output plugin is null")
            val inputConfigure = input.originConfigure ?: throw IllegalArgumentException("Input configure is null")
            val outputConfigure = output.originConfigure ?: throw IllegalArgumentException("Output configure is null")
            val database = output.database ?: throw IllegalArgumentException("Output database is null")
            val table = output.table ?: throw IllegalArgumentException("Output table is null")
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

            val written = if (inputPlugin.supportsStreaming() && outputPlugin.supportsStreaming())
            {
                runStreaming(
                    inputPlugin, inputConfigure, query, fetchSize,
                    outputPlugin, outputConfigure, database, table,
                    targetColumns, sourceKeys, batchSize
                )
            }
            else
            {
                runLegacy(
                    inputPlugin, inputConfigure, query,
                    outputPlugin, outputConfigure, database, table,
                    originColumns, batchSize
                )
            }

            response.count = if (written > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else written.toInt()
            response.successful = true
            response.state = RunState.SUCCESS
        }
        catch (ex: Exception)
        {
            log.error("Local executor failed", ex)
            response.successful = false
            response.state = RunState.FAILURE
            response.message = ex.message
        }
        return response
    }

    override fun stop(request: ExecutorRequest): ExecutorResponse
    {
        return ExecutorResponse(false, true, RunState.SUCCESS, null)
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
        batchSize: Int
    ): Long
    {
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
                }

                override fun onRow(row: List<Any?>)
                {
                    val projected = ArrayList<Any?>(sourceKeys.size)
                    for (key in sourceKeys)
                    {
                        val idx = indexByHeader[key.lowercase()]
                            ?: throw IllegalStateException("Source column '$key' not found in query result")
                        projected.add(row[idx])
                    }
                    writer.addRow(projected)
                    written ++
                }
            })
        }
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
        batchSize: Int
    ): Long
    {
        val inputResult = inputPlugin.execute(inputConfigure, query)
        if (inputResult.isSuccessful != true)
        {
            throw RuntimeException(inputResult.message ?: "Input plugin failed")
        }
        val rows = inputResult.columns ?: return 0L

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
                    val node = item as? ObjectNode ?: continue
                    val projected = ArrayList<Any?>(sourceKeys.size)
                    for (key in sourceKeys)
                    {
                        projected.add(jsonNodeToJdbcValue(node.get(key)))
                    }
                    writer.addRow(projected)
                    written ++
                }
            }
            return written
        }

        // 双端都不支持流式：用 INSERT 字符串，但按 batch 提交，不再拼一个巨大字符串
        var written = 0L
        val batch = ArrayList<String>(batchSize)
        for (item in rows)
        {
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
            }
        }
        if (batch.isNotEmpty())
        {
            flushLegacyBatch(outputPlugin, outputConfigure, batch)
            written += batch.size
            batch.clear()
        }
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
    }
}
