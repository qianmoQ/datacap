package io.edurt.datacap.executor.seatunnel;

import io.edurt.datacap.executor.ExecutorPlugin;
import io.edurt.datacap.plugin.configure.PluginConfigureField;
import io.edurt.datacap.plugin.configure.PluginFieldType;

import java.util.Arrays;
import java.util.List;

public class SeatunnelExecutor
        extends ExecutorPlugin
{
    @Override
    public List<PluginConfigureField> configures()
    {
        return Arrays.asList(
                new PluginConfigureField(
                        "home",
                        PluginFieldType.STRING,
                        "/opt/lib/seatunnel",
                        "SeaTunnel installation root (was datacap.executor.seatunnel.home).",
                        false
                ),
                new PluginConfigureField(
                        "startScript",
                        PluginFieldType.STRING,
                        "start-seatunnel-spark-connector-v2.sh",
                        "SeaTunnel launcher script under {home}/bin.",
                        false
                ),
                new PluginConfigureField(
                        "engine",
                        PluginFieldType.STRING,
                        "SPARK",
                        "Underlying engine: SPARK / FLINK / SEATUNNEL.",
                        false
                ),
                new PluginConfigureField(
                        "way",
                        PluginFieldType.STRING,
                        "LOCAL",
                        "Run way: LOCAL / CLUSTER.",
                        false
                ),
                new PluginConfigureField(
                        "mode",
                        PluginFieldType.STRING,
                        "CLIENT",
                        "Run mode: CLIENT / SERVER.",
                        false
                ),
                new PluginConfigureField(
                        "data",
                        PluginFieldType.STRING,
                        "",
                        "Executor data buffer path; empty means project root /data.",
                        false
                )
        );
    }
}
