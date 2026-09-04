package io.edurt.datacap.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 进程内活跃任务表 + 取消线程池。
 *
 * 多个并发 sync 共享同一实例（Kotlin object 单例），用 taskName 唯一索引；
 * stop() 通过 taskName 查到 handle 后设置取消标志位。
 * 之前这些是 LocalExecutorService 的 companion static 成员——为了让不同 LocalExecutorService
 * 实例之间的 stop() 仍能互相找到任务，这里必须保持“单例、跨实例共享”的语义，故用 object。
 *
 * Statement.cancel() 可能会阻塞（驱动会新开连接发 KILL），放到独立线程跑避免拖住调用方。
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal object TaskRegistry
{
    private val runningTasks: ConcurrentHashMap<String, TaskHandle> = ConcurrentHashMap()

    private val cancelExecutor: java.util.concurrent.ExecutorService =
            Executors.newCachedThreadPool { r ->
                val t = Thread(r, "local-executor-cancel")
                t.isDaemon = true
                t
            }

    fun register(taskName: String, handle: TaskHandle)
    {
        runningTasks[taskName] = handle
    }

    /** 仅当当前登记的仍是同一个 handle 时才移除，避免误删同名的后续任务 */
    fun unregister(taskName: String, handle: TaskHandle)
    {
        runningTasks.remove(taskName, handle)
    }

    fun find(taskName: String): TaskHandle? = runningTasks[taskName]

    fun submitCancel(block: () -> Unit)
    {
        cancelExecutor.submit(block)
    }
}
