package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class RanksDAO {
    protected String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS mineclans_ranks (" +
            "player_id VARCHAR(36) PRIMARY KEY," +
            "player_rank VARCHAR(255) NOT NULL)";

    protected String INSERT_OR_UPDATE_RANK_QUERY = "INSERT INTO mineclans_ranks (player_id, player_rank) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE player_rank = VALUES(player_rank)";

    protected String SELECT_RANK_BY_PLAYER_ID_QUERY = "SELECT player_rank FROM mineclans_ranks WHERE player_id = ?";

    protected String SELECT_ALL_RANKS_QUERY = "SELECT player_id, player_rank FROM mineclans_ranks";

    protected String DELETE_RANK_BY_PLAYER_ID_QUERY = "DELETE FROM mineclans_ranks WHERE player_id = ?";

    protected String DELETE_RANKS_BY_FACTION_QUERY = "DELETE FROM mineclans_ranks WHERE player_id IN (SELECT member_id FROM mineclans_members WHERE faction_id = ?)";

    private MySQLProvider mySQLProvider;

    public RanksDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLE_QUERY);
    }

    public void setRank(UUID playerId, Rank rank) {
        mySQLProvider.executeUpdateQuery(INSERT_OR_UPDATE_RANK_QUERY, playerId.toString(), rank.toString());
    }

    public Rank getRank(UUID playerId) {
        AtomicReference<Rank> rank = new AtomicReference<>(null);
        mySQLProvider.executeSelectQuery(SELECT_RANK_BY_PLAYER_ID_QUERY, new ResultSetProcessor() {
            @Override
            public void run(ResultSet resultSet) throws SQLException {
                if (resultSet != null && resultSet.next()) {
                    String rankStr = resultSet.getString("player_rank");
                    rank.set(Rank.valueOf(rankStr));
                }
            }
        }, playerId.toString());
        return rank.get();
    }

    public Map<UUID, Rank> getAllRanks() {
        Map<UUID, Rank> ranks = new ConcurrentHashMap<>();
        mySQLProvider.executeSelectQuery(SELECT_ALL_RANKS_QUERY, new ResultSetProcessor() {
            @Override
            public void run(ResultSet resultSet) throws SQLException {
                if (resultSet != null) {
                    while (resultSet.next()) {
                        UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                        String rankStr = resultSet.getString("player_rank");
                        try {
                            Rank rank = Rank.valueOf(rankStr);
                            ranks.put(playerId, rank);
                        } catch (IllegalArgumentException ex) {
                        }
                    }
                }
            }
        });
        return ranks;
    }

    public void deleteRank(UUID playerId) {
        mySQLProvider.executeUpdateQuery(DELETE_RANK_BY_PLAYER_ID_QUERY, playerId.toString());
    }

    public void deleteRanksByFaction(UUID factionId) {
        mySQLProvider.executeUpdateQuery(DELETE_RANKS_BY_FACTION_QUERY, factionId.toString());
    }
}