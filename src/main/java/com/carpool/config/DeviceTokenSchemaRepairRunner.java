package com.carpool.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenSchemaRepairRunner implements ApplicationRunner {

    private final DataSource dataSource;

    public DeviceTokenSchemaRepairRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "device_tokens")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/repair/device_tokens_startup_fix.sql"));
                ensureUniqueTokenIndex(connection);
                return;
            }

            ensureColumn(connection, "device_tokens", "token", "ALTER TABLE device_tokens ADD COLUMN token VARCHAR(2048) NULL");
            ensureColumn(connection, "device_tokens", "created_at", "ALTER TABLE device_tokens ADD COLUMN created_at TIMESTAMP NULL");
            ensureColumn(connection, "device_tokens", "updated_at", "ALTER TABLE device_tokens ADD COLUMN updated_at TIMESTAMP NULL");

            if (columnExists(connection, "device_tokens", "fcm_token") && columnExists(connection, "device_tokens", "token")) {
                execute(connection,
                    "UPDATE device_tokens SET token = COALESCE(token, fcm_token) WHERE token IS NULL AND fcm_token IS NOT NULL");
            }

            if (columnExists(connection, "device_tokens", "created_date") || columnExists(connection, "device_tokens", "updated_date")) {
                execute(connection,
                    "UPDATE device_tokens SET created_at = COALESCE(created_at, created_date, NOW()), updated_at = COALESCE(updated_at, updated_date, NOW()) WHERE created_at IS NULL OR updated_at IS NULL");
            }

            execute(connection,
                "UPDATE device_tokens SET token = COALESCE(token, CONCAT('legacy-', id)) WHERE token IS NULL");

            if (columnExists(connection, "device_tokens", "token")) {
                execute(connection,
                    "ALTER TABLE device_tokens MODIFY COLUMN token VARCHAR(2048) NOT NULL");
            }

            if (columnExists(connection, "device_tokens", "created_at")) {
                execute(connection,
                    "ALTER TABLE device_tokens MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }

            if (columnExists(connection, "device_tokens", "updated_at")) {
                execute(connection,
                    "ALTER TABLE device_tokens MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            }

            if (columnExists(connection, "device_tokens", "created_date")) {
                execute(connection, "ALTER TABLE device_tokens DROP COLUMN created_date");
            }
            if (columnExists(connection, "device_tokens", "updated_date")) {
                execute(connection, "ALTER TABLE device_tokens DROP COLUMN updated_date");
            }
            if (columnExists(connection, "device_tokens", "fcm_token")) {
                execute(connection, "ALTER TABLE device_tokens DROP COLUMN fcm_token");
            }

            ensureUniqueTokenIndex(connection);
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String alterSql) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, alterSql);
        }
    }

    private void ensureUniqueTokenIndex(Connection connection) throws SQLException {
        if (!indexExists(connection, "device_tokens", "uk_device_token_user_token")) {
            execute(connection,
                "CREATE UNIQUE INDEX uk_device_token_user_token ON device_tokens (user_id, token(255))");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rs = metadata.getTables(connection.getCatalog(), null, tableName, new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rs = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rs = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (rs.next()) {
                String currentIndexName = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
