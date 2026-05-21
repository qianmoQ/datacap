package io.edurt.datacap.service.initializer;

import io.edurt.datacap.service.configure.DatasetTargetSchema;
import io.edurt.datacap.service.service.RuntimeConfigureService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Map;

/**
 * 数据集目标存储的运行时配置。
 * 数据源已从 application.properties (datacap.dataset.*) 迁移到 datacap_configure 表，
 * 由 RuntimeConfigureService 提供 effective 值；首次启动若 DB 行缺失会自动 seed schema 默认值。
 *
 * Backed by datacap_configure (category=DATASET, name=Default) via RuntimeConfigureService.
 * The previous {@code @ConfigurationProperties(prefix = "datacap.dataset")} binding has been removed.
 */
@Slf4j
@Data
@Component
public class DataSetConfigure
{
    private String type;
    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
    private String tableDefaultEngine;
    private String tablePrefix;

    private final RuntimeConfigureService runtimeConfigureService;

    public DataSetConfigure(RuntimeConfigureService runtimeConfigureService)
    {
        this.runtimeConfigureService = runtimeConfigureService;
    }

    @PostConstruct
    public void load()
    {
        // 首次启动 seed：DB 行不存在则按 schema 默认值落一条，方便管理员后续从 UI 改
        runtimeConfigureService.seedIfAbsent(
                RuntimeConfigureService.CATEGORY_DATASET,
                DatasetTargetSchema.NAME,
                DatasetTargetSchema.fields(),
                "Dataset target storage configuration"
        );
        Map<String, String> cfg = runtimeConfigureService.getEffective(
                RuntimeConfigureService.CATEGORY_DATASET,
                DatasetTargetSchema.NAME,
                DatasetTargetSchema.fields()
        );
        this.type = cfg.get("type");
        this.host = cfg.get("host");
        this.port = cfg.get("port");
        this.username = cfg.get("username");
        this.password = cfg.get("password");
        this.database = cfg.get("database");
        this.tableDefaultEngine = cfg.get("tableDefaultEngine");
        this.tablePrefix = cfg.get("tablePrefix");
        log.info("DataSetConfigure loaded from runtime configure: type={} host={} database={}", type, host, database);
    }

    /** 管理员在系统配置页改完后由 controller 主动调一次，避免重启 */
    public void reload()
    {
        load();
    }
}
