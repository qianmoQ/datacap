package org.devlive.metaforge.plugin.natived.zookeeper;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;

import java.util.Arrays;

public class ZookeeperPlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("Zookeeper",
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
                                .value("127.0.0.1:2181")
                                .message("host is a required field, please be sure to enter")
                                .build()),
                null);
    }
}
