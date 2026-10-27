package org.devlive.metaforge.plugin.configure;

import java.util.Arrays;
import java.util.Collections;

/**
 * 复刻原 configure/etc/conf/plugins/default.yaml 的通用 JDBC 连接字段，
 * 作为未显式声明 getConfigure() 的连接器插件的默认表单配置。
 *
 * Generic JDBC connection fields replicated from the former default.yaml,
 * used as the fallback form configuration for connector plugins that do not
 * declare their own {@code getConfigure()}.
 */
public final class DefaultJdbcConfigure
{
    public static final IConfigure INSTANCE = build();

    private DefaultJdbcConfigure() {}

    private static IConfigure build()
    {
        return IConfigure.of("Default",
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
                                .value(1L)
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
                                .message("database is a required field, please be sure to enter")
                                .group(IConfigureFieldGroup.advanced)
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.configures)
                                .type(IConfigureFieldType.Array)
                                .value(Collections.emptyList())
                                .group(IConfigureFieldGroup.custom)
                                .build()),
                null);
    }
}
