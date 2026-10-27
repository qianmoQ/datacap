package org.devlive.metaforge.server.service;

import java.sql.Connection;

/** One-time source plugin rename upgrade; callers supply a dedicated connection. */
public final class SourceTypeMigration
{
    private static final String KEY = "source-type-redis-rename-v1";
    private static final String SCRIPT = "schema/source-type-migration.sql";

    private SourceTypeMigration()
    {
    }

    public static void apply(Connection connection)
            throws Exception
    {
        SchemaMigration.apply(connection, KEY, SCRIPT);
    }
}
