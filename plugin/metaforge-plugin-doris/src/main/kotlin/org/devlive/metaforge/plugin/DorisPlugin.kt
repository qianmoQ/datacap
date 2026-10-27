package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.IConfigure
import org.devlive.metaforge.plugin.configure.IConfigureField
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.IConfigureFieldName
import org.devlive.metaforge.plugin.configure.IConfigureFieldType

import java.util.Arrays

class DorisPlugin : Plugin()
{
    override fun getConfigure(): IConfigure
    {
        return IConfigure.of("Doris",
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
                                .value(9093)
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
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build()),
                null)
    }
}
