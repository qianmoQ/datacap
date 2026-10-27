package org.devlive.metaforge.plugin.jdbc.clickhouse;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.FieldType;
import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureExecutor;
import org.devlive.metaforge.plugin.configure.IConfigureExecutorField;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;
import org.devlive.metaforge.plugin.configure.IConfigurePipelineType;
import org.devlive.metaforge.plugin.configure.RunProtocol;

import java.util.Arrays;
import java.util.Collections;

public class ClickHousePlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("ClickHouse",
                Arrays.asList(
                        IConfigureField.builder()
                                .field(IConfigureFieldName.name)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .message("name is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.host)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .value("127.0.0.1")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.port)
                                .type(IConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(9000)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.username)
                                .type(IConfigureFieldType.String)
                                .group(IConfigureFieldGroup.authorization)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.password)
                                .type(IConfigureFieldType.String)
                                .group(IConfigureFieldGroup.authorization)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.database)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .value("default")
                                .group(IConfigureFieldGroup.advanced)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.configures)
                                .type(IConfigureFieldType.Array)
                                .value(Collections.emptyList())
                                .group(IConfigureFieldGroup.custom)
                                .build()),
                Arrays.asList(
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.INPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("host")
                                                .origin("host|port")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("username")
                                                .origin("username")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("database")
                                                .origin("database")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The ClickHouse database")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("sql")
                                                .origin("context")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("The query sql used to search data though Clickhouse server")
                                                .tooltip("If this value is entered, the default sql statement will be replaced")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("result_table_name")
                                                .origin("result_table_name")
                                                .hidden(true)
                                                .override(true)
                                                .input(true)
                                                .description("When result_table_name is not specified, the data processed by this plugin will not be registered as a data set (dataStream/dataset) that can be directly accessed by other plugins, or called a temporary table (table) ;\n"
                                                        + "When result_table_name is specified, the data processed by this plugin will be registered as a data set (dataStream/dataset) that can be directly accessed by other plugins, or called a temporary table (table) . The data set (dataStream/dataset) registered here can be directly accessed by other plugins by specifying source_table_name .")
                                                .build())),
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.OUTPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("host")
                                                .origin("host|port")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("username")
                                                .origin("username")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("password")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("database")
                                                .origin("database")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The ClickHouse database")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("table")
                                                .origin("table")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The table name of the ClickHouse database")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("fields")
                                                .origin("fields")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("The data field that needs to be output to ClickHouse , if not configured, it will be automatically adapted according to the sink table schema .\n"
                                                        + "\n"
                                                        + "If there are multiple values, each value needs to be one row.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("bulk_size")
                                                .origin("bulk_size")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .type(FieldType.NUMBER)
                                                .value(20000)
                                                .description("The number of rows written through Clickhouse-jdbc each time, the default is 20000 .")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("split_mode")
                                                .origin("split_mode")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(false)
                                                .description("This mode only support clickhouse table which engine is 'Distributed'.And internal_replication option should be true. They will split distributed table data in seatunnel and perform write directly on each shard. The shard weight define is clickhouse will be counted.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("sharding_key")
                                                .origin("sharding_key")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .description("When use split_mode, which node to send data to is a problem, the default is random selection, but the 'sharding_key' parameter can be used to specify the field for the sharding algorithm. This option only worked when 'split_mode' is true.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("primary_key")
                                                .origin("primary_key")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .description("Mark the primary key column from clickhouse table, and based on primary key execute INSERT/UPDATE/DELETE to clickhouse table")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("support_upsert")
                                                .origin("support_upsert")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(false)
                                                .description("Support upsert row by query primary key")
                                                .build()))));
    }
}
