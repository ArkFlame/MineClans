package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class SchemaMigrationDAO {
    private final MySQLProvider mySQLProvider;
    private final String tableName;

    public SchemaMigrationDAO(MySQLProvider mySQLProvider, String tableName) {
        this.mySQLProvider = mySQLProvider;
        this.tableName = tableName;
    }

    public void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "migration_id VARCHAR(64) NOT NULL PRIMARY KEY," +
                "applied_at TIMESTAMP(3) NOT NULL," +
                "details VARCHAR(255) NULL)";
        mySQLProvider.executeUpdateQuery(query);
    }

    public boolean isMigrationApplied(String migrationId) {
        AtomicBoolean applied = new AtomicBoolean(false);
        String query = "SELECT 1 FROM " + tableName + " WHERE migration_id = ?";
        mySQLProvider.executeSelectQuery(query, new ResultSetProcessor() {
            @Override
            public void run(ResultSet rs) throws SQLException {
                applied.set(rs.next());
            }
        }, migrationId);
        return applied.get();
    }

    public void markMigrationApplied(String migrationId, String details) {
        String query = "INSERT INTO " + tableName + " (migration_id, applied_at, details) VALUES (?, NOW(), ?)";
        mySQLProvider.executeUpdateQuery(query, migrationId, details);
    }

    public String getMigrationDetails(String migrationId) {
        AtomicReference<String> details = new AtomicReference<>(null);
        String query = "SELECT details FROM " + tableName + " WHERE migration_id = ?";
        mySQLProvider.executeSelectQuery(query, new ResultSetProcessor() {
            @Override
            public void run(ResultSet rs) throws SQLException {
                if (rs.next()) {
                    details.set(rs.getString("details"));
                }
            }
        }, migrationId);
        return details.get();
    }
}
