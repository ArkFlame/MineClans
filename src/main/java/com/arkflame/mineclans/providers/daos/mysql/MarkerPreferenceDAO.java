package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class MarkerPreferenceDAO {
    private final MySQLProvider mySQLProvider;
    private final String tableName;

    public MarkerPreferenceDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
        this.tableName = MineClans.getSqlTableNames().getMarkerPreferences();
    }

    public void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_id CHAR(36) NOT NULL," +
                "marker_key VARCHAR(32) NOT NULL," +
                "enabled BOOLEAN NOT NULL," +
                "PRIMARY KEY (player_id, marker_key))";
        mySQLProvider.executeUpdateQuery(query);
    }

    public void setPreference(UUID playerId, String markerKey, boolean enabled) {
        String query = "INSERT INTO " + tableName + " (player_id, marker_key, enabled) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)";
        mySQLProvider.executeUpdateQuery(query, playerId.toString(), markerKey, enabled);
    }

    public Boolean getPreference(UUID playerId, String markerKey) {
        AtomicBoolean result = new AtomicBoolean();
        String query = "SELECT enabled FROM " + tableName + " WHERE player_id = ? AND marker_key = ?";
        mySQLProvider.executeSelectQuery(query, new ResultSetProcessor() {
            @Override
            public void run(ResultSet rs) throws SQLException {
                if (rs.next()) {
                    result.set(rs.getBoolean("enabled"));
                }
            }
        }, playerId.toString(), markerKey);
        return result.get() ? true : null;
    }

    public void deleteByPlayer(UUID playerId) {
        String query = "DELETE FROM " + tableName + " WHERE player_id = ?";
        mySQLProvider.executeUpdateQuery(query, playerId.toString());
    }
}
