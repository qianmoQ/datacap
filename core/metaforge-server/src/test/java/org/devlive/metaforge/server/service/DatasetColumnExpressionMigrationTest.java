package org.devlive.metaforge.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetColumnExpressionMigrationTest
{
    @Test
    public void upgradesExistingColumnsWithoutLosingRowsAndCanRunRepeatedly()
            throws Exception
    {
        for (String identifiers : new String[] {"", ";DATABASE_TO_LOWER=TRUE"}) {
            DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1" + identifiers, "sa", "");
            JdbcTemplate jdbc = new JdbcTemplate(source);
            try (Connection connection = source.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
                jdbc.execute("ALTER TABLE metaforge_dataset_column DROP COLUMN expression");
                jdbc.update("INSERT INTO metaforge_dataset_column (id, name, is_virtual_column) VALUES (10, 'legacy', 1)");
                DatasetColumnExpressionMigration.apply(connection);
                DatasetColumnExpressionMigration.apply(connection);
                assertEquals("legacy", jdbc.queryForObject("SELECT name FROM metaforge_dataset_column WHERE id = 10", String.class));
                jdbc.update("UPDATE metaforge_dataset_column SET expression = 'round(amount, 2)' WHERE id = 10");
                assertEquals("round(amount, 2)", jdbc.queryForObject("SELECT expression FROM metaforge_dataset_column WHERE id = 10", String.class));
            }
            finally {
                jdbc.execute("SHUTDOWN");
            }
        }
    }

    @Test
    public void newInstallationsAlreadyHaveTheColumnAndMigrationIsANoOp()
            throws Exception
    {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        try (Connection connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
            DatasetColumnExpressionMigration.apply(connection);
            new JdbcTemplate(source).queryForList("SELECT expression FROM metaforge_dataset_column");
        }
    }
}
