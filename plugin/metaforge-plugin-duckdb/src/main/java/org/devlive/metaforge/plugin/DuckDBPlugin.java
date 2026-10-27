package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;

import java.util.Arrays;

public class DuckDBPlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("DuckDB",
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
                                .value("/root")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.port)
                                .type(IConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(0)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.database)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .value("local")
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build()),
                null);
    }
}
