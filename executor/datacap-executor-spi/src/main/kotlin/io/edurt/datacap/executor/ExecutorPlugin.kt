package io.edurt.datacap.executor

import io.edurt.datacap.plugin.Plugin
import io.edurt.datacap.plugin.PluginType

/**
 * 所有 Executor 插件的统一基类。
 * - 类型固定为 EXECUTOR
 * - 名称自动从子类类名末尾剥掉 "Executor" 后缀。
 *   例如 SeatunnelExecutor -> "Seatunnel"，LocalExecutor -> "Local"。
 *
 * Common base for all executor plugins.
 * - Type is fixed to EXECUTOR.
 * - Name auto-derives from the subclass simple name by stripping the trailing "Executor".
 *   e.g. SeatunnelExecutor -> "Seatunnel", LocalExecutor -> "Local".
 */
abstract class ExecutorPlugin : Plugin()
{
    override fun getType(): PluginType = PluginType.EXECUTOR

    override fun getName(): String = this::class.java.simpleName.removeSuffix("Executor")
}
