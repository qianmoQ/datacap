package org.devlive.metaforge.server.service;

import java.sql.Connection;

/** One-time navigation catalogue upgrade; callers supply a dedicated connection. */
public final class NavigationMigration
{
    private static final String KEY = "navigation-user-admin-v1";
    private static final String SCRIPT = "schema/navigation-migration.sql";

    private NavigationMigration()
    {
    }

    public static void apply(Connection connection)
            throws Exception
    {
        SchemaMigration.apply(connection, KEY, SCRIPT);
    }
}
