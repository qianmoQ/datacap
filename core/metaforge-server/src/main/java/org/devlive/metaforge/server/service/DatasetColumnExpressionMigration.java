package org.devlive.metaforge.server.service;

import java.sql.Connection;
import java.sql.ResultSet;

/** Safe for old databases, new installations and a retry after MySQL DDL auto-commit. */
public final class DatasetColumnExpressionMigration
{
    private DatasetColumnExpressionMigration() {}

    public static void apply(Connection connection)
            throws Exception
    {
        String table = connection.getMetaData().storesUpperCaseIdentifiers() ? "METAFORGE_DATASET_COLUMN" : "metaforge_dataset_column";
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table, null)) {
            while (columns.next()) {
                if ("expression".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return;
                }
            }
        }
        SchemaMigration.apply(connection, "dataset-column-expression-v1", "schema/dataset-column-expression-migration.sql");
    }
}
