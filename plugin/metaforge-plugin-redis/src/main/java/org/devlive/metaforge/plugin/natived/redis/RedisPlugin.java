package org.devlive.metaforge.plugin.natived.redis;

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

public class RedisPlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("Redis",
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
                                .value(6379)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.password)
                                .type(IConfigureFieldType.String)
                                .group(IConfigureFieldGroup.authorization)
                                .build()),
                Collections.singletonList(
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.OUTPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("host")
                                                .origin("host")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("port")
                                                .origin("port")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("user")
                                                .origin("username")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("auth")
                                                .origin("password")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("key")
                                                .origin("key")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The value of key you want to write to redis.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("data_type")
                                                .origin("data_type")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.SELECT)
                                                .defaultValues(Arrays.asList("key", "hash", "list", "set", "zset"))
                                                .description("Redis data types")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("mode")
                                                .origin("mode")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.SELECT)
                                                .value("single")
                                                .defaultValues(Arrays.asList("single", "cluster"))
                                                .description("Redis mode")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("nodes")
                                                .origin("nodes")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Redis nodes information, used in cluster mode\n"
                                                        + "\n"
                                                        + "If there are multiple values, each value needs to be one row.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("format")
                                                .origin("format")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.SELECT)
                                                .value("json")
                                                .defaultValues(Arrays.asList("json", "text"))
                                                .description("The format of upstream data")
                                                .build()))));
    }
}
