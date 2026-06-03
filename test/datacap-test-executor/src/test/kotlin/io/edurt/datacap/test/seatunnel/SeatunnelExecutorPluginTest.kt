package io.edurt.datacap.test.seatunnel

import io.edurt.datacap.plugin.PluginConfigure
import io.edurt.datacap.plugin.PluginManager
import io.edurt.datacap.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class SeatunnelExecutorPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "Seatunnel"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("executor/datacap-executor-seatunnel"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        assertNotNull(pluginManager.getPlugin(pluginName).get())
    }
}