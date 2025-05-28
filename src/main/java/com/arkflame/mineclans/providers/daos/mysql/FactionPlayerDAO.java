package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class FactionPlayerDAO {
    protected String CREATE_TABLES_QUERY = "CREATE TABLE IF NOT EXISTS mineclans_players ("
            + "player_id CHAR(36) NOT NULL PRIMARY KEY,"
            + "faction_id CHAR(36) NULL,"
            + "join_date TIMESTAMP,"
            + "last_active TIMESTAMP,"
            + "kills INT DEFAULT 0,"
            + "deaths INT DEFAULT 0,"
            + "power INT DEFAULT 1,"
            + "max_power INT DEFAULT 10,"
            + "name TEXT"
            + ")";

    protected String CHECK_COLUMNS_QUERY = "SELECT COUNT(*) AS column_count FROM information_schema.columns "
            + "WHERE table_name = 'mineclans_players' AND table_schema = DATABASE()";

    protected String INSERT_PLAYER_QUERY = "INSERT INTO mineclans_players (player_id, faction_id, join_date, last_active, kills, deaths, power, max_power, name) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "faction_id = VALUES(faction_id), join_date = VALUES(join_date), last_active = VALUES(last_active), "
            + "kills = VALUES(kills), deaths = VALUES(deaths), name = VALUES(name), "
            + "power = VALUES(power), max_power = VALUES(max_power)";

    protected String SELECT_BY_ID_QUERY = "SELECT * FROM mineclans_players WHERE player_id = ?";
    protected String SELECT_BY_NAME_QUERY = "SELECT * FROM mineclans_players WHERE name = ?";
    protected String DELETE_PLAYER_QUERY = "DELETE FROM mineclans_players WHERE player_id = ?";

    // Queries to add new columns if they don't exist
    protected String ADD_POWER_COLUMN_QUERY = "ALTER TABLE mineclans_players ADD COLUMN IF NOT EXISTS power INT DEFAULT 1";
    protected String ADD_MAX_POWER_COLUMN_QUERY = "ALTER TABLE mineclans_players ADD COLUMN IF NOT EXISTS max_power INT DEFAULT 10";

    protected String FIND_FK_QUERY = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
            "WHERE TABLE_NAME = 'mineclans_players' AND COLUMN_NAME = 'faction_id' AND REFERENCED_TABLE_NAME IS NOT NULL";

    protected void dropForeignKeyIfExists() {
        try {
            AtomicReference<String> fkName = new AtomicReference<>(null);
            mySQLProvider.executeSelectQuery(FIND_FK_QUERY, new ResultSetProcessor() {
                @Override
                public void run(ResultSet resultSet) throws SQLException {
                    if (resultSet.next()) {
                        fkName.set(resultSet.getString("CONSTRAINT_NAME"));
                    }
                }
            });

            if (fkName.get() != null) {
                mySQLProvider.executeUpdateQuery("ALTER TABLE mineclans_players DROP FOREIGN KEY " + fkName.get());
            }
        } catch (Exception ignored) {
            // Ignore failures
        }
    }

    private MySQLProvider mySQLProvider;
    private boolean schemaChecked = false;

    public FactionPlayerDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLES_QUERY);
    }

    private void checkAndUpdateSchema() {
        if (schemaChecked)
            return;

        try {
            // Check if the table exists and has all columns
            mySQLProvider.executeUpdateQuery(ADD_POWER_COLUMN_QUERY);
            mySQLProvider.executeUpdateQuery(ADD_MAX_POWER_COLUMN_QUERY);
            dropForeignKeyIfExists();

            schemaChecked = true;
        } catch (Exception e) {
            // If there's an error, the table might not exist yet
            createTable();
            schemaChecked = true;
        }
    }

    public void insertOrUpdatePlayer(FactionPlayer player) {
        checkAndUpdateSchema();

        mySQLProvider.executeUpdateQuery(
                INSERT_PLAYER_QUERY,
                player.getPlayerId(),
                player.getFactionId() != null
                        && MineClans.getInstance().getAPI().getFaction(player.getFactionId()) != null
                                ? player.getFactionId()
                                : null,
                player.getJoinDate(),
                player.getLastActive(),
                player.getKills(),
                player.getDeaths(),
                player.getPower(),
                player.getMaxPower(),
                player.getName());
    }

    public FactionPlayer getPlayerById(UUID playerId) {
        checkAndUpdateSchema();

        AtomicReference<FactionPlayer> player = new AtomicReference<>(null);
        mySQLProvider.executeSelectQuery(SELECT_BY_ID_QUERY,
                new ResultSetProcessor() {
                    @Override
                    public void run(ResultSet resultSet) throws SQLException {
                        if (resultSet != null && resultSet.next()) {
                            player.set(extractPlayerFromResultSet(resultSet));
                        }
                    }
                }, playerId.toString());
        return player.get();
    }

    public FactionPlayer getPlayerByName(String name) {
        checkAndUpdateSchema();

        AtomicReference<FactionPlayer> player = new AtomicReference<>(null);
        mySQLProvider.executeSelectQuery(SELECT_BY_NAME_QUERY,
                new ResultSetProcessor() {
                    public void run(ResultSet resultSet) throws SQLException {
                        if (resultSet != null && resultSet.next()) {
                            player.set(extractPlayerFromResultSet(resultSet));
                        }
                    };
                },
                name);
        return player.get();
    }

    private FactionPlayer extractPlayerFromResultSet(ResultSet resultSet) throws SQLException {
        FactionPlayer player = new FactionPlayer(UUID.fromString(resultSet.getString("player_id")));
        player.setFactionId(resultSet.getString("faction_id"));
        player.setJoinDate(resultSet.getTimestamp("join_date"));
        player.setLastActive(resultSet.getTimestamp("last_active"));
        player.setKills(resultSet.getInt("kills"));
        player.setDeaths(resultSet.getInt("deaths"));
        player.setName(resultSet.getString("name"));

        try {
            // Try to get the new columns, fall back to defaults if they don't exist
            player.setMaxPower(resultSet.getInt("max_power"));
            player.setPower(resultSet.getInt("power"));
        } catch (SQLException e) {
            // Columns don't exist, use defaults
            player.setMaxPower(10);
            player.setPower(1);
        }

        return player;
    }

    public void deletePlayer(UUID playerId) {
        checkAndUpdateSchema();
        mySQLProvider.executeUpdateQuery(DELETE_PLAYER_QUERY, playerId);
    }
}
