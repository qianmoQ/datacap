package org.devlive.metaforge.server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * First-boot installation: creates the schema, seeds baseline data and the
 * administrator account chosen in the setup wizard. All statements are
 * idempotent so a partially installed instance can safely re-run.
 */
@Slf4j
@Service
public class SetupService
{
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    public SetupService(JdbcTemplate jdbcTemplate, DataSource dataSource, PasswordEncoder passwordEncoder)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Installed == the user table exists and already holds an account.
     */
    public boolean isInstalled()
    {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM metaforge_user", Long.class);
            return count != null && count > 0;
        }
        catch (DataAccessException e) {
            // table missing -> fresh database, not installed yet
            return false;
        }
    }

    public Map<String, Object> environment()
    {
        Map<String, Object> info = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            info.put("connected", Boolean.TRUE);
            info.put("database", connection.getCatalog());
            info.put("product", connection.getMetaData().getDatabaseProductName());
            info.put("version", connection.getMetaData().getDatabaseProductVersion());
        }
        catch (SQLException | DataAccessException e) {
            log.warn("Setup environment probe failed: {}", e.getMessage());
            info.put("connected", Boolean.FALSE);
            info.put("error", e.getMessage());
        }
        info.put("installed", isInstalled());
        return info;
    }

    public CommonResponse<Object> testConnection()
    {
        long begin = System.currentTimeMillis();
        try {
            jdbcTemplate.execute("SELECT 1");
            Map<String, Object> result = new HashMap<>();
            result.put("connected", Boolean.TRUE);
            result.put("elapsed", System.currentTimeMillis() - begin);
            return CommonResponse.success(result);
        }
        catch (DataAccessException e) {
            log.warn("Setup connection test failed: {}", e.getMessage());
            return CommonResponse.failure(String.format("Connection failed: %s", e.getMostSpecificCause().getMessage()));
        }
    }

    public CommonResponse<Object> install(SetupInstallBody body)
    {
        if (isInstalled()) {
            return CommonResponse.failure("Instance is already installed");
        }
        if (body == null || StringUtils.isBlank(body.getUsername()) || StringUtils.isBlank(body.getPassword())) {
            return CommonResponse.failure("Username and password are required");
        }
        if (body.getUsername().length() < 3 || body.getUsername().length() > 20) {
            return CommonResponse.failure("Username must be 3-20 characters");
        }
        if (body.getPassword().length() < 6 || body.getPassword().length() > 20) {
            return CommonResponse.failure("Password must be 6-20 characters");
        }

        try {
            executeScript("schema/setup-schema.sql");
            executeScript("schema/setup-data.sql");
            createAdministrator(body);
            log.info("Setup finished, administrator [ {} ] created", body.getUsername());
            Map<String, Object> result = new HashMap<>();
            result.put("username", body.getUsername());
            result.put("installed", Boolean.TRUE);
            return CommonResponse.success(result);
        }
        catch (Exception e) {
            log.error("Setup failed", e);
            return CommonResponse.failure(String.format("Setup failed: %s", e.getMessage()));
        }
    }

    private void executeScript(String path)
            throws SQLException
    {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
        }
    }

    private void createAdministrator(SetupInstallBody body)
    {
        String username = body.getUsername();
        jdbcTemplate.update(
                "INSERT INTO metaforge_user (username, password, name, is_system, active, code) VALUES (?, ?, ?, 1, 1, ?)",
                username,
                passwordEncoder.encode(body.getPassword()),
                StringUtils.defaultIfBlank(body.getName(), username),
                UUID.randomUUID().toString().replace("-", ""));
        // role id 1 is the seeded system ADMIN role
        jdbcTemplate.update(
                "INSERT INTO metaforge_user_role_relation (user_id, role_id) "
                        + "SELECT id, 1 FROM metaforge_user WHERE username = ?",
                username);
    }
}
