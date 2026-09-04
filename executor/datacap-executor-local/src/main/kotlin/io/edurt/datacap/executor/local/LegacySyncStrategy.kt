package io.edurt.datacap.executor.local

import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.common.sql.SqlBuilder
import io.edurt.datacap.common.sql.configure.SqlBody
import io.edurt.datacap.common.sql.configure.SqlColumn
import io.edurt.datacap.common.sql.configure.SqlType
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.model.Configure

/**
 * 回退路径：源或汇任一不支持流式（如 HTTP / Native 插件）。仍然使用旧的全量读取，
 * 但目标端按 batch 切片提交，避免一次性拼接巨大 SQL 字符串；同时修复 NULL、类型、转义问题。
 *
 * 内部再按“目标端是否支持流式”分两条子路：BatchWriter / 拼 INSERT 字符串。
 * 逻辑从 LocalExecutorService.runLegacy 原样迁移，行为不变。
 */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal class LegacySyncStrategy : SyncStrategy
{
    override fun sync(context: SyncContext): Long
    {
        val inputPlugin = context.inputPlugin
        val inputConfigure = context.inputConfigure
        val query = context.query
        val outputPlugin = context.outputPlugin
        val outputConfigure = context.outputConfigure
        val database = context.database
        val table = context.table
        val originColumns = context.originColumns
        val batchSize = context.batchSize
        val taskLog = context.taskLog
        val totalCount = context.totalCount
        val progressListener = context.progressListener
        val handle = context.handle

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
                        projected.add(ValueCodec.jsonNodeToJdbcValue(node.get(key)))
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
                        .value(ValueCodec.formatSqlLiteral(node.get(col.original)))
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
}
