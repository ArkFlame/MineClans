package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class FactionPlayerDAO {
    private final MySQLProvider mySQLProvider;
    private final String tableName;

    private final String CREATE_TABLES_QUERY;
    private final String INSERT_PLAYER_QUERY;
    private final String SELECT_BY_ID_QUERY;
    private final String SELECT_BY_NAME_QUERY;
    private final String DELETE_PLAYER_QUERY;

    private final String UPDATE_FACTION_ID_QUERY;
    private final String UPDATE_JOIN_DATE_QUERY;
    private final String UPDATE_LAST_ACTIVE_QUERY;
    private final String UPDATE_NAME_QUERY;
    private final String UPDATE_KILLS_QUERY;
    private final String UPDATE_DEATHS_QUERY;
    private final String UPDATE_POWER_QUERY;
    private final String UPDATE_MAX_POWER_QUERY;
    private final String UPDATE_POWER_AND_MAX_POWER_QUERY;

    private boolean schemaChecked = false;

    public FactionPlayerDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
        this.tableName = MineClans.getSqlTableNames().getPlayers();

        this.CREATE_TABLES_QUERY = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_id CHAR(36) NOT NULL PRIMARY KEY," +
                "faction_id CHAR(36) NULL," +
                "join_date TIMESTAMP(3) NULL," +
                "last_active TIMESTAMP(3) NULL," +
                "kills INT NOT NULL DEFAULT 0," +
                "deaths INT NOT NULL DEFAULT 0," +
                "power DOUBLE NOT NULL DEFAULT 1," +
                "max_power INT NOT NULL DEFAULT 10," +
                "name VARCHAR(16) NULL," +
                "INDEX idx_players_faction_id (faction_id)," +
                "INDEX idx_players_name (name))";

        this.INSERT_PLAYER_QUERY = "INSERT INTO " + tableName + " (player_id, faction_id, join_date, last_active, kills, deaths, power, max_power, name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "faction_id = VALUES(faction_id), join_date = VALUES(join_date), last_active = VALUES(last_active), " +
                "kills = VALUES(kills), deaths = VALUES(deaths), name = VALUES(name), " +
                "power = VALUES(power), max_power = VALUES(max_power)";

        this.SELECT_BY_ID_QUERY = "SELECT * FROM " + tableName + " WHERE player_id = ?";
        this.SELECT_BY_NAME_QUERY = "SELECT * FROM " + tableName + " WHERE name = ?";
        this.DELETE_PLAYER_QUERY = "DELETE FROM " + tableName + " WHERE player_id = ?";

        this.UPDATE_FACTION_ID_QUERY = "UPDATE " + tableName + " SET faction_id = ? WHERE player_id = ?";
        this.UPDATE_JOIN_DATE_QUERY = "UPDATE " + tableName + " SET join_date = ? WHERE player_id = ?";
        this.UPDATE_LAST_ACTIVE_QUERY = "UPDATE " + tableName + " SET last_active = ? WHERE player_id = ?";
        this.UPDATE_NAME_QUERY = "UPDATE " + tableName + " SET name = ? WHERE player_id = ?";
        this.UPDATE_KILLS_QUERY = "UPDATE " + tableName + " SET kills = ? WHERE player_id = ?";
        this.UPDATE_DEATHS_QUERY = "UPDATE " + tableName + " SET deaths = ? WHERE player_id = ?";
        this.UPDATE_POWER_QUERY = "UPDATE " + tableName + " SET power = ? WHERE player_id = ?";
        this.UPDATE_MAX_POWER_QUERY = "UPDATE " + tableName + " SET max_power = ? WHERE player_id = ?";
        this.UPDATE_POWER_AND_MAX_POWER_QUERY = "UPDATE " + tableName + " SET power = ?, max_power = ? WHERE player_id = ?";
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLES_QUERY);
    }

    public void insertOrUpdatePlayer(FactionPlayer player) {
        Faction faction = player.getFaction();
        mySQLProvider.executeUpdateQuery(
                INSERT_PLAYER_QUERY,
                player.getPlayerId(),
                faction != null ? faction.getId() : null,
                player.getJoinDate(),
                player.getLastActive(),
                player.getKills(),
                player.getDeaths(),
                player.getPower(),
                player.getMaxPower(),
                player.getName());
    }

    public int insertPlayerIfAbsent(FactionPlayer player) {
        String query = "INSERT INTO " + tableName + " (player_id, faction_id, join_date, last_active, kills, deaths, power, max_power, name) " +
                "SELECT ?, ?, ?, ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM " + tableName + " WHERE player_id = ?)";
        UUID factionId = player.getFaction() != null ? player.getFaction().getId() : null;
        return mySQLProvider.executeUpdateQuery(query,
                player.getPlayerId(),
                factionId,
                player.getJoinDate(),
                player.getLastActive(),
                player.getKills(),
                player.getDeaths(),
                player.getPower(),
                player.getMaxPower(),
                player.getName(),
                player.getPlayerId().toString());
    }

    public int updateFactionId(UUID playerId, Optional<UUID> factionId) {
        return mySQLProvider.executeUpdateQuery(UPDATE_FACTION_ID_QUERY,
                factionId.map(UUID::toString).orElse(null),
                playerId.toString());
    }

    public int updateJoinDate(UUID playerId, Date joinDate) {
        return mySQLProvider.executeUpdateQuery(UPDATE_JOIN_DATE_QUERY, joinDate, playerId.toString());
    }

    public int updateLastActive(UUID playerId, Date lastActive) {
        return mySQLProvider.executeUpdateQuery(UPDATE_LAST_ACTIVE_QUERY, lastActive, playerId.toString());
    }

    public int updateName(UUID playerId, String name) {
        return mySQLProvider.executeUpdateQuery(UPDATE_NAME_QUERY, name, playerId.toString());
    }

    public int updateKills(UUID playerId, int kills) {
        return mySQLProvider.executeUpdateQuery(UPDATE_KILLS_QUERY, kills, playerId.toString());
    }

    public int updateDeaths(UUID playerId, int deaths) {
        return mySQLProvider.executeUpdateQuery(UPDATE_DEATHS_QUERY, deaths, playerId.toString());
    }

    public int updatePower(UUID playerId, double power) {
        return mySQLProvider.executeUpdateQuery(UPDATE_POWER_QUERY, power, playerId.toString());
    }

    public int updateMaxPower(UUID playerId, int maxPower) {
        return mySQLProvider.executeUpdateQuery(UPDATE_MAX_POWER_QUERY, maxPower, playerId.toString());
    }

    public int updatePowerAndMaxPower(UUID playerId, double power, int maxPower) {
        return mySQLProvider.executeUpdateQuery(UPDATE_POWER_AND_MAX_POWER_QUERY, power, maxPower, playerId.toString());
    }

    public FactionPlayer getPlayerById(UUID playerId, FactionPlayer factionPlayer) {
        mySQLProvider.executeSelectQuery(SELECT_BY_ID_QUERY,
                new ResultSetProcessor() {
                    @Override
                    public void run(ResultSet resultSet) throws SQLException {
                        if (resultSet != null && resultSet.next()) {
                            extractPlayerFromResultSet(resultSet, factionPlayer);
                        }
                    }
                }, playerId.toString());
        return factionPlayer;
    }

    public FactionPlayer getPlayerByName(String name, FactionPlayer factionPlayer) {
        mySQLProvider.executeSelectQuery(SELECT_BY_NAME_QUERY,
                new ResultSetProcessor() {
                    public void run(ResultSet resultSet) throws SQLException {
                        if (resultSet != null && resultSet.next()) {
                            extractPlayerFromResultSet(resultSet, factionPlayer);
                        }
                    };
                },
                name);
        return factionPlayer;
    }

    private FactionPlayer extractPlayerFromResultSet(ResultSet resultSet, FactionPlayer player) throws SQLException {
        player.setId(UUID.fromString(resultSet.getString("player_id")));
        player.setFactionId(resultSet.getString("faction_id"));
        player.setJoinDate(resultSet.getTimestamp("join_date"));
        player.setLastActive(resultSet.getTimestamp("last_active"));
        player.setKills(resultSet.getInt("kills"));
        player.setDeaths(resultSet.getInt("deaths"));
        player.setName(resultSet.getString("name"));

        try {
            player.setMaxPower(resultSet.getInt("max_power"));
            player.setPower(resultSet.getInt("power"));
        } catch (SQLException e) {
            player.setMaxPower(10);
            player.setPower(1);
        }

        return player;
    }

    public void deletePlayer(UUID playerId) {
        mySQLProvider.executeUpdateQuery(DELETE_PLAYER_QUERY, playerId);
    }
}
