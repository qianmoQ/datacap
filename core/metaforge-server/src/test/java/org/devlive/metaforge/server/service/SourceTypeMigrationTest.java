package org.devlive.metaforge.server.service;

import org.devlive.metaforge.server.runner.SchemaMigrationRunner;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class SourceTypeMigrationTest
{
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    public void initialize()
            throws Exception
    {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-data.sql"));
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO metaforge_user (id, username, code) VALUES (10, 'ordinary', 'ORDINARY')");
        jdbc.update("INSERT INTO metaforge_source (host, name, port, _type) VALUES ('127.0.0.1', 'legacy-jdbc', 6379, 'Redis Jdbc'), ('127.0.0.1', 'plain-redis', 6379, 'Redis'), ('127.0.0.1', 'mysql', 3306, 'MySQL')");
    }

    @AfterEach
    public void close()
    {
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    public void legacyRedisJdbcSourcesAreRePointedToTheUnifiedConnector()
            throws Exception
    {
        SchemaMigrationRunner runner = new SchemaMigrationRunner(dataSource, new InstallationState(dataSource));
        runner.run();
        runner.run();
        assertEquals("Redis", type("legacy-jdbc"));
        assertEquals("Redis", type("plain-redis"));
        assertEquals("MySQL", type("mysql"));
    }

    @Test
    public void uninstalledInstancesDoNotRunMigrationQueries()
            throws Exception
    {
        DataSource unavailable = mock(DataSource.class);
        new SchemaMigrationRunner(unavailable, mock(InstallationState.class)).run();
        verifyNoInteractions(unavailable);
    }

    private String type(String name)
    {
        return jdbc.queryForObject("SELECT _type FROM metaforge_source WHERE name = ?", String.class, name);
    }
}
