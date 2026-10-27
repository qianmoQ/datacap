package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.IConfigure
import org.devlive.metaforge.plugin.configure.IConfigureField
import org.devlive.metaforge.plugin.configure.IConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.IConfigureFieldName
import org.devlive.metaforge.plugin.configure.IConfigureFieldType

import java.util.Collections

class HdfsPlugin : Plugin()
{
    override fun getConfigure(): IConfigure
    {
        return IConfigure.of("HDFS",
                listOf(
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
                                .field(IConfigureFieldName.file)
                                .type(IConfigureFieldType.File)
                                .required(true)
                                .value(Collections.emptyList<Any>())
                                .group(IConfigureFieldGroup.advanced)
                                .build()),
                null)
    }
}
