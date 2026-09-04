package io.edurt.datacap.executor.seatunnel

import io.edurt.datacap.executor.common.RunEngine
import io.edurt.datacap.executor.common.RunMode
import io.edurt.datacap.executor.common.RunWay
import io.edurt.datacap.executor.configure.ExecutorConfigure
import io.edurt.datacap.executor.configure.ExecutorRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Seatunnel 安全网：锁定“ExecutorRequest 字段 -> seatunnel 命令行”的映射。
 *
 * 这段映射在 ExecutorRequest 结构调整（把 executor 专属字段收敛进 options）时最易出错。
 * 断言的是最终命令字符串——只要重构后 buildCommander 仍读到等价的值，命令就不变、测试保持绿。
 */
class SeatunnelCommanderMappingTest
{
    private val service = SeatunnelExecutorService()

    @Test
    fun sparkEngineCommand()
    {
        val request = ExecutorRequest(
            taskName = "task1",
            userName = "tester",
            input = ExecutorConfigure("Jdbc"),
            output = ExecutorConfigure("Jdbc"),
            executorHome = "/opt/seatunnel",
            workHome = "/work",
            runWay = RunWay.LOCAL,
            runMode = RunMode.CLIENT,
            startScript = "start-seatunnel-spark-connector-v2.sh",
            runEngine = RunEngine.SPARK
        )

        val config = "/work" + File.separator + "task1.configure"
        assertEquals(
            "/opt/seatunnel/bin/start-seatunnel-spark-connector-v2.sh " +
                    "--master local --deploy-mode client --config $config --name task1",
            service.buildCommander(request).toCommand()
        )
    }

    @Test
    fun seatunnelEngineLocalCommand()
    {
        val request = ExecutorRequest(
            taskName = "task2",
            userName = "tester",
            input = ExecutorConfigure("Jdbc"),
            output = ExecutorConfigure("Jdbc"),
            executorHome = "/opt/seatunnel",
            workHome = "/work",
            runWay = RunWay.LOCAL,
            runMode = RunMode.CLIENT,
            startScript = "seatunnel.sh",
            runEngine = RunEngine.SEATUNNEL
        )

        val config = "/work" + File.separator + "task2.configure"
        assertEquals(
            "/opt/seatunnel/bin/seatunnel.sh -e local --config $config --name task2",
            service.buildCommander(request).toCommand()
        )
    }
}
