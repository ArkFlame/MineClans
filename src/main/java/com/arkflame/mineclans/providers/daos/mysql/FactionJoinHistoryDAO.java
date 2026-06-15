package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.FactionJoinRecord;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class FactionJoinHistoryDAO {
    private final MySQLProvider mySQLProvider;
    private final String tableName;

    public FactionJoinHistoryDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
        this.tableName = MineClans.getSqlTableNames().getPlayerJoins();
    }

    public void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "event_id CHAR(36) NOT NULL PRIMARY KEY," +
                "faction_id CHAR(36) NOT NULL," +
                "player_id CHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "server_name VARCHAR(64) NOT NULL," +
                "joined_at TIMESTAMP(3) NOT NULL," +
                "INDEX idx_player_joins_faction_time (faction_id, joined_at))";
        mySQLProvider.executeUpdateQuery(query);
    }

    public void recordJoin(UUID eventId, UUID factionId, UUID playerId, String playerName, String serverName) {
        String query = "INSERT INTO " + tableName + " (event_id, faction_id, player_id, player_name, server_name, joined_at) VALUES (?, ?, ?, ?, ?, NOW())";
        mySQLProvider.executeUpdateQuery(query, eventId.toString(), factionId.toString(), playerId.toString(), playerName, serverName);
    }

    public List<FactionJoinRecord> getRecentJoins(UUID factionId, int limit) {
        AtomicReference<List<FactionJoinRecord>> result = new AtomicReference<>(new ArrayList<>());
        String query = "SELECT event_id, faction_id, player_id, player_name, server_name, joined_at FROM " + tableName +
                " WHERE faction_id = ? ORDER BY joined_at DESC LIMIT ?";
        mySQLProvider.executeSelectQuery(query, new ResultSetProcessor() {
            @Override
            public void run(ResultSet rs) throws SQLException {
                List<FactionJoinRecord> records = new ArrayList<>();
                while (rs.next()) {
                    FactionJoinRecord record = new FactionJoinRecord(
                            UUID.fromString(rs.getString("event_id")),
                            UUID.fromString(rs.getString("faction_id")),
                            UUID.fromString(rs.getString("player_id")),
                            rs.getString("player_name"),
                            rs.getString("server_name"),
                            rs.getTimestamp("joined_at")
                    );
                    records.add(record);
                }
                result.set(records);
            }
        }, factionId.toString(), limit);
        return result.get();
    }

    public void deleteByFaction(UUID factionId) {
        String query = "DELETE FROM " + tableName + " WHERE faction_id = ?";
        mySQLProvider.executeUpdateQuery(query, factionId.toString());
    }
}
