package io.edurt.datacap.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.spi.adapter.BatchWriter
import io.edurt.datacap.spi.adapter.RowCallback

/**
 * 流式路径：源端 fetchSize 拉取，目标端 PreparedStatement 批量写。
 * 全程不在 JVM 内物化整个结果集。
 *
 * 仅当源与汇【都】支持流式时选用。逻辑从 LocalExecutorService.runStreaming 原样迁移，行为不变。
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal class StreamingSyncStrategy : SyncStrategy
{
    override fun sync(context: SyncContext): Long
    {
        val inputPlugin = context.inputPlugin
        val inputConfigure = context.inputConfigure
        val query = context.query
        val fetchSize = context.fetchSize
        val outputPlugin = context.outputPlugin
        val outputConfigure = context.outputConfigure
        val database = context.database
        val table = context.table
        val targetColumns = context.targetColumns
        val sourceKeys = context.sourceKeys
        val batchSize = context.batchSize
        val taskLog = context.taskLog
        val totalCount = context.totalCount
        val progressListener = context.progressListener
        val handle = context.handle

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
}
