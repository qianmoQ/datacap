package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OceanBasePlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        Map<String, Object> useOldAliasMetadataBehavior = new LinkedHashMap<>();
        useOldAliasMetadataBehavior.put("field", "useOldAliasMetadataBehavior");
        useOldAliasMetadataBehavior.put("value", true);

        return IConfigure.of("OceanBase",
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
                                .value(2881)
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
                                .required(true)
                                .group(IConfigureFieldGroup.authorization)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.database)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .value("default")
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.configures)
                                .type(IConfigureFieldType.Array)
                                .value(Collections.singletonList(useOldAliasMetadataBehavior))
                                .group(IConfigureFieldGroup.custom)
                                .build()),
                null);
    }
}
