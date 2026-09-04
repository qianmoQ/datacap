package io.edurt.datacap.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 已注册的活跃任务句柄。
 * - cancelled: 调度循环每条行检查
 * - sourceStatement: stop() 会调 cancel() 立即终止源端 JDBC 查询，不必等下一行
 * - taskLog: 让 stop() 把"用户停止"事件写到任务专属日志里
 * - rowsAtStop: 记录被停止时已处理的行数，供 history 落库
 *
 * An in-flight task handle shared between start() and stop(). Extracted verbatim from the former
 * inner class in LocalExecutorService; behaviour is unchanged.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class TaskHandle
{
    val cancelled: AtomicBoolean = AtomicBoolean(false)

    @Volatile
    var sourceStatement: java.sql.Statement? = null

    @Volatile
    var taskLog: Logger? = null

    @Volatile
    var rowsAtStop: Long = 0L
}

/** 取消传播专用异常，携带已处理行数便于上游记录 */
internal class TaskCancelledException(val processed: Long) : RuntimeException("Task cancelled by user")
