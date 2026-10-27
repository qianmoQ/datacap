package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;

import java.util.Arrays;

public class H2Plugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("H2",
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
                                .value("-")
                                .disabled(true)
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.port)
                                .type(IConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(1)
                                .disabled(true)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.username)
                                .type(IConfigureFieldType.String)
                                .required(true)
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
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build()),
                null);
    }
}
