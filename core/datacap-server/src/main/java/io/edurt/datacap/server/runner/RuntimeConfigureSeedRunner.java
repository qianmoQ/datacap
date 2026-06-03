package io.edurt.datacap.server.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.plugin.Plugin;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.plugin.PluginType;
import io.edurt.datacap.plugin.configure.PluginConfigureField;
import io.edurt.datacap.service.service.RuntimeConfigureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 启动时把每个 ExecutorPlugin 声明的可配置字段（Plugin.configures()）按默认值落到 datacap_configure。
 * 已存在的行不动；只为缺失的 (EXECUTOR, name) 做 seed。
 *
 * On startup, iterate ExecutorPlugin instances and seed default configuration rows into
 * datacap_configure when missing. Existing rows are left untouched.
 *
 * 在 SchedulerRunner / DatasetHistoryRecoveryRunner 之前执行（@Order=5），
 * 保证后续业务逻辑读到的是已 seed 过的配置。
 */
@Slf4j
@Service
@Order(5)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class RuntimeConfigureSeedRunner
        implements CommandLineRunner
{
    private final PluginManager pluginManager;
    private final RuntimeConfigureService runtimeConfigureService;

    public RuntimeConfigureSeedRunner(PluginManager pluginManager, RuntimeConfigureService runtimeConfigureService)
    {
        this.pluginManager = pluginManager;
        this.runtimeConfigureService = runtimeConfigureService;
    }

    @Override
    public void run(String... args)
    {
        try {
            pluginManager.getPluginInfos().stream()
                    .filter(meta -> meta.getType() == PluginType.EXECUTOR)
                    .forEach(meta -> {
                        Object instance = meta.getInstance();
                        if (!(instance instanceof Plugin)) {
                            return;
                        }
                        Plugin plugin = (Plugin) instance;
                        List<PluginConfigureField> fields = plugin.configures();
                        if (fields == null || fields.isEmpty()) {
                            return;
                        }
                        String name = plugin.getName();
                        runtimeConfigureService.seedIfAbsent(
                                RuntimeConfigureService.CATEGORY_EXECUTOR,
                                name,
                                fields,
                                "Configuration for executor " + name
                        );
                    });
        }
        catch (Exception ex) {
            log.error("Seed executor runtime configure failed", ex);
        }
    }
}
