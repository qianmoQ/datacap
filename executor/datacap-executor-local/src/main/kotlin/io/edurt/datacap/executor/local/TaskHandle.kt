package io.edurt.datacap.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 单一取消信号源。stop() 只调 [cancel]，各 sync 策略只读 [isCancelled] / 调 [throwIfCancelled]。
 * 取消时统一抛 [TaskCancelledException]，并携带“已提交行数”（committed），由 start() 转成 STOPPED。
 *
 * The single source of truth for cancellation. stop() calls [cancel]; strategies observe it via
 * [isCancelled] / [throwIfCancelled]. Cancellation always surfaces as a [TaskCancelledException]
 * carrying the committed row count.
 */
internal class CancellationToken
{
    private val cancelled: AtomicBoolean = AtomicBoolean(false)

    val isCancelled: Boolean
        get() = cancelled.get()

    fun cancel()
    {
        cancelled.set(true)
    }

    fun throwIfCancelled(processed: Long)
    {
        if (cancelled.get())
        {
            throw TaskCancelledException(processed)
        }
    }
}

/**
 * 已注册的活跃任务句柄。
 * - cancellation: 取消信号，stop() 设置、策略读取
 * - sourceStatement: stop() 会调 cancel() 立即终止源端 JDBC 查询，不必等下一行
 * - taskLog: 让 stop() 把"用户停止"事件写到任务专属日志里
 *
 * An in-flight task handle shared between start() and stop().
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class TaskHandle
{
    val cancellation: CancellationToken = CancellationToken()

    @Volatile
    var sourceStatement: java.sql.Statement? = null

    @Volatile
    var taskLog: Logger? = null
}

/** 取消传播专用异常，携带已提交行数便于上游记录 */
internal class TaskCancelledException(val processed: Long) : RuntimeException("Task cancelled by user")
