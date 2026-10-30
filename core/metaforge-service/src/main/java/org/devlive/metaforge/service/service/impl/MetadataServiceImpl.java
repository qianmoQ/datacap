package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.BiFunction;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class MetadataServiceImpl
        implements MetadataService
{
    private final PluginManager pluginManager;
    private final SourceRepository repository;

    public MetadataServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    private <T> CommonResponse<T> nativeOperation(String code, ManagementRequest request, Set<String> capabilities,
            BiFunction<PluginService, RuntimeConfigure, T> operation)
    {
        try {
            if (request == null) {
                return CommonResponse.failure("Management request is required");
            }
            request.validate();
            return repository.findByCode(code)
                    .map(entity -> pluginManager.getPlugin(entity.getType())
                            .map(plugin -> {
                                PluginService adapter = plugin.getService(PluginService.class);
                                if (capabilities.stream().noneMatch(adapter.managementCapabilities()::supports)) {
                                    return CommonResponse.<T>failure("This source does not support the requested management operation");
                                }
                                return CommonResponse.success(operation.apply(adapter, entity.toConfigure(pluginManager, plugin)));
                            })
                            .orElseGet(() -> CommonResponse.<T>failure("Source plugin not found")))
                    .orElseGet(() -> CommonResponse.failure("Source not found"));
        }
        catch (RuntimeException error) {
            return CommonResponse.failure(error.getMessage() == null ? "Management operation failed" : error.getMessage());
        }
    }

    @Override
    public CommonResponse<ManagementPage> managementNamespaces(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("namespace:list"), (adapter, configure) -> adapter.managementNamespaces(configure, request));
    }

    @Override
    public CommonResponse<ManagementPage> managementList(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("key:list", "object:list"), (adapter, configure) -> adapter.managementList(configure, request));
    }

    @Override
    public CommonResponse<ManagementDetail> managementDetail(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("key:read", "object:read"), (adapter, configure) -> adapter.managementDetail(configure, request));
    }

    @Override
    public CommonResponse<ManagementDownload> managementDownload(String code, ManagementRequest request)
    {
        return nativeOperation(code, request, Set.of("object:download"), (adapter, configure) -> adapter.managementDownload(configure, request));
    }

    @Override
    public CommonResponse<ManagementCapabilities> getCapabilities(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> CommonResponse.success(plugin.getService(PluginService.class).managementCapabilities()))
                        .orElseGet(() -> CommonResponse.<ManagementCapabilities>failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getEngines(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("engine:list")) {
                                return CommonResponse.<Response>failure("Source does not support engine:list");
                            }
                            return CommonResponse.success(service.getEngines());
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDataTypes(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("datatype:list")) {
                                return CommonResponse.<Response>failure("Source does not support datatype:list");
                            }
                            return CommonResponse.success(service.getDataTypes());
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDatabases(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("database:list")) {
                                return CommonResponse.<Response>failure("Source does not support database:list");
                            }
                            return CommonResponse.success(service.getDatabases(entity.toConfigure(pluginManager, plugin)));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTables(String code, String database)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:list")) {
                                return CommonResponse.<Response>failure("Source does not support table:list");
                            }
                            return CommonResponse.success(service.getTables(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getColumns(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:read")) {
                                return CommonResponse.<Response>failure("Source does not support column:read");
                            }
                            return CommonResponse.success(service.getColumns(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getDatabase(String code, String database)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("database:info")) {
                                return CommonResponse.<Response>failure("Source does not support database:info");
                            }
                            return CommonResponse.success(service.getDatabase(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTable(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:info")) {
                                return CommonResponse.<Response>failure("Source does not support table:info");
                            }
                            return CommonResponse.success(service.getTable(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTableStatement(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:statement")) {
                                return CommonResponse.<Response>failure("Source does not support table:statement");
                            }
                            TableDefinition definition = TableDefinition.create(database, table);
                            return CommonResponse.success(service.getTableStatement(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> updateAutoIncrement(String code, TableDefinition configure, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:auto-increment")) {
                                return CommonResponse.<Response>failure("Source does not support table:auto-increment");
                            }
                            TableDefinition definition = TableDefinition.create(database, table)
                                    .autoIncrement(configure.getAutoIncrement());
                            return CommonResponse.success(service.changeAutoIncrement(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createTable(String code, String database, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:create")) {
                                return CommonResponse.<Response>failure("Source does not support table:create");
                            }
                            configure.setDatabase(database);
                            return CommonResponse.success(service.createTable(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:drop")) {
                                return CommonResponse.<Response>failure("Source does not support table:drop");
                            }
                            TableDefinition definition = TableDefinition.create(database, table);
                            definition.setPreview(configure.isPreview());
                            return CommonResponse.success(service.dropTable(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> truncateTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:truncate")) {
                                return CommonResponse.<Response>failure("Source does not support table:truncate");
                            }
                            TableDefinition definition = TableDefinition.create(database, table);
                            definition.setPreview(configure.isPreview());
                            return CommonResponse.success(service.truncateTable(entity.toConfigure(pluginManager, plugin), definition));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> queryTable(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:read")) {
                                return CommonResponse.<Response>failure("Source does not support table:read");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.queryTable(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> deleteData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("data:delete")) {
                                return CommonResponse.<Response>failure("Source does not support data:delete");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.deleteData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> exportData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("table:export")) {
                                return CommonResponse.<Response>failure("Source does not support table:export");
                            }
                            RuntimeConfigure conf = entity.toConfigure(pluginManager, plugin);
                            conf.setFormat(configure.getFormat());
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.queryTable(conf, configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:create")) {
                                return CommonResponse.<Response>failure("Source does not support column:create");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.createColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> createIndex(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:create")) {
                                return CommonResponse.<Response>failure("Source does not support index:create");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.createIndex(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropIndex(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:drop")) {
                                return CommonResponse.<Response>failure("Source does not support index:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.dropIndex(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getIndexes(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("index:read")) {
                                return CommonResponse.<Response>failure("Source does not support index:read");
                            }
                            return CommonResponse.success(service.metadataIndexes(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getForeignKeys(String code, String database, String table)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("foreign-key:read")) {
                                return CommonResponse.<Response>failure("Source does not support foreign-key:read");
                            }
                            return CommonResponse.success(service.metadataForeignKeys(entity.toConfigure(pluginManager, plugin), database, table));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> dropColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:drop")) {
                                return CommonResponse.<Response>failure("Source does not support column:drop");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.dropColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:read")) {
                                return CommonResponse.<Response>failure("Source does not support column:read");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.getColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> changeColumn(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("column:modify")) {
                                return CommonResponse.<Response>failure("Source does not support column:modify");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.changeColumn(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> insertData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("data:insert")) {
                                return CommonResponse.<Response>failure("Source does not support data:insert");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.insertData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> updateData(String code, String database, String table, TableDefinition configure)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            if (!service.managementCapabilities().supports("data:modify")) {
                                return CommonResponse.<Response>failure("Source does not support data:modify");
                            }
                            configure.setDatabase(database);
                            configure.setName(table);
                            return CommonResponse.success(service.updateData(entity.toConfigure(pluginManager, plugin), configure));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getSuggests(String code, String keyword)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getSuggests(entity.toConfigure(pluginManager, plugin), keyword));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }
}
