package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.configure.IConfigure
import org.devlive.metaforge.plugin.configure.IConfigureField
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.IConfigureFieldName
import org.devlive.metaforge.plugin.configure.IConfigureFieldType
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashMap

class DolphinDBPlugin : Plugin()
{
    override fun getConfigure(): IConfigure
    {
        val database: MutableMap<String, Any> = LinkedHashMap()
        database["field"] = "database"
        database["value"] = "default"

        return IConfigure.of("DolphinDB",
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
                                .value("8848")
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
                                .field(IConfigureFieldName.configures)
                                .type(IConfigureFieldType.Array)
                                .value(Collections.singletonList(database))
                                .group(IConfigureFieldGroup.custom)
                                .build()),
                null)
    }
}
