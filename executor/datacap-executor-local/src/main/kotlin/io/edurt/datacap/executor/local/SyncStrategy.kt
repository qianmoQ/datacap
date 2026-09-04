package io.edurt.datacap.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.executor.configure.ExecutorProgressListener
import io.edurt.datacap.executor.configure.OriginColumn
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.model.Configure
import org.slf4j.Logger

/** 每处理多少行上报一次进度 / 打一次日志。行为与重构前保持一致。 */
internal const val PROGRESS_INTERVAL = 1_000L

/**
 * 一次同步任务所需的全部上下文。由 LocalExecutorService.start() 组装，交给具体 [SyncStrategy]。
 * 把原先 runStreaming / runLegacy 的一长串形参收敛到这里，行为不变，只是不再逐个透传。
 *
 * targetColumns / sourceKeys 是从 [originColumns] 预先派生的（name / original），
 * 避免各策略重复计算。
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class SyncContext(
    val inputPlugin: PluginService,
    val inputConfigure: Configure,
    val query: String,
    val fetchSize: Int,
    val outputPlugin: PluginService,
    val outputConfigure: Configure,
    val database: String,
    val table: String,
    val originColumns: List<OriginColumn>,
    val targetColumns: List<String>,
    val sourceKeys: List<String>,
    val batchSize: Int,
    val taskLog: Logger,
    val totalCount: Long,
    val progressListener: ExecutorProgressListener?,
    val handle: TaskHandle
)

/**
 * 同步策略：把“源 -> 汇”的一次搬运抽象出来，返回已处理（读入）行数。
 * 取消时应抛出 [TaskCancelledException]（携带已处理行数），由 start() 统一转成 STOPPED。
 *
 * A single "source -> sink" copy. Returns the number of rows read. On cancellation it throws
 * [TaskCancelledException] carrying the processed count, which start() maps to STOPPED.
 */
internal interface SyncStrategy
{
    fun sync(context: SyncContext): Long
}
