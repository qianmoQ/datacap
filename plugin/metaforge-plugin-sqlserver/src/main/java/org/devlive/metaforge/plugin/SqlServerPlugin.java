package org.devlive.metaforge.plugin;

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
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlServerPlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        Map<String, Object> trustServerCertificate = new LinkedHashMap<>();
        trustServerCertificate.put("field", "trustServerCertificate");
        trustServerCertificate.put("value", true);

        return IConfigure.of("Sql Server",
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
                                .value(1433)
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
                                .field(IConfigureFieldName.ssl)
                                .type(IConfigureFieldType.Boolean)
                                .group(IConfigureFieldGroup.authorization)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.database)
                                .type(IConfigureFieldType.String)
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.configures)
                                .type(IConfigureFieldType.Array)
                                .value(Collections.singletonList(trustServerCertificate))
                                .group(IConfigureFieldGroup.custom)
                                .build()),
                Arrays.asList(
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.INPUT, RunProtocol.JDBC,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("url")
                                                .generated(true)
                                                .override(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("driver")
                                                .origin("driver")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .value("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("user")
                                                .origin("username")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("query")
                                                .origin("context")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Query statement")
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
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.OUTPUT, RunProtocol.JDBC,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("url")
                                                .generated(true)
                                                .override(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("driver")
                                                .origin("driver")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .value("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("user")
                                                .origin("username")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("query")
                                                .origin("query")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Use this sql write upstream input datas to database. e.g INSERT ...")
                                                .build()))));
    }
}
