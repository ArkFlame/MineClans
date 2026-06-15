package com.arkflame.mineclans.providers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.providers.daos.mysql.SchemaMigrationDAO;

public class DatabaseMigrationRunner {
    private final MySQLProvider mySQLProvider;
    private final Logger logger;
    private final SchemaMigrationDAO migrationDao;

    public DatabaseMigrationRunner(MySQLProvider mySQLProvider, Logger logger) {
        this.mySQLProvider = mySQLProvider;
        this.logger = logger;
        this.migrationDao = new SchemaMigrationDAO(mySQLProvider, MineClans.getSqlTableNames().getSchemaMigrations());
    }

    public void runMigrations() {
        migrationDao.createTable();
        migratePlayersUuidKeyV2();
        migrateInvitesCompositeKeyV2();
        migrateFactionsRallyV2();
        migratePlayerJoinHistoryV1();
        migrateMarkerPreferencesV1();
    }

    private void migratePlayersUuidKeyV2() {
        String migrationId = "2026_06_14_players_uuid_key_v2";
        if (migrationDao.isMigrationApplied(migrationId)) {
            return;
        }

        String playersTable = MineClans.getSqlTableNames().getPlayers();
        String shadowTable = playersTable + "__mc_migrate_20260614";
        String backupTable = playersTable + "__mc_backup_20260614";

        try {
            if (tableExists(playersTable) && hasTextBlobPrimaryKey(playersTable)) {
                if (tableExists(shadowTable) || tableExists(backupTable)) {
                    logger.warning("Migration tables already exist for " + playersTable + ". Manual intervention required.");
                    return;
                }

                String createShadow = "CREATE TABLE " + shadowTable + " (" +
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

                mySQLProvider.executeUpdateQuery(createShadow);

                String copyData = "INSERT INTO " + shadowTable + " (player_id, faction_id, join_date, last_active, kills, deaths, power, max_power, name) " +
                        "SELECT " +
                        "CAST(player_id AS CHAR(36)), " +
                        "CAST(faction_id AS CHAR(36)), " +
                        "join_date, last_active, kills, deaths, power, max_power, name " +
                        "FROM " + playersTable;

                mySQLProvider.executeUpdateQuery(copyData);

                long sourceCount = countRows(playersTable);
                long shadowCount = countRows(shadowTable);

                if (sourceCount != shadowCount) {
                    logger.warning("Row count mismatch in migration: source=" + sourceCount + " shadow=" + shadowCount);
                    return;
                }

                String renameCanonical = "RENAME TABLE " + playersTable + " TO " + backupTable;
                String renameShadow = "RENAME TABLE " + shadowTable + " TO " + playersTable;

                try (Connection conn = mySQLProvider.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(renameCanonical);
                    stmt.executeUpdate(renameShadow);
                }

                logger.info("Migrated " + playersTable + " player_id from TEXT to CHAR(36). Backup: " + backupTable);
            }

            migrationDao.markMigrationApplied(migrationId, "players player_id migrated to CHAR(36)");
        } catch (SQLException e) {
            logger.severe("Migration failed: " + migrationId + " - " + e.getMessage());
        }
    }

    private void migrateInvitesCompositeKeyV2() {
        String migrationId = "2026_06_14_invites_composite_key_v2";
        if (migrationDao.isMigrationApplied(migrationId)) {
            return;
        }

        String invitedTable = MineClans.getSqlTableNames().getInvited();
        String shadowTable = invitedTable + "__mc_migrate_20260614";
        String backupTable = invitedTable + "__mc_backup_20260614";

        try {
            if (tableExists(invitedTable) && hasSingleColumnPrimaryKey(invitedTable)) {
                if (tableExists(shadowTable) || tableExists(backupTable)) {
                    logger.warning("Migration tables already exist for " + invitedTable + ". Manual intervention required.");
                    return;
                }

                String createShadow = "CREATE TABLE " + shadowTable + " (" +
                        "faction_id CHAR(36) NOT NULL," +
                        "member_id CHAR(36) NOT NULL," +
                        "PRIMARY KEY (faction_id, member_id)," +
                        "INDEX idx_invited_member_id (member_id))";

                mySQLProvider.executeUpdateQuery(createShadow);

                String copyData = "INSERT INTO " + shadowTable + " (faction_id, member_id) " +
                        "SELECT CAST(faction_id AS CHAR(36)), CAST(member_id AS CHAR(36)) FROM " + invitedTable;

                mySQLProvider.executeUpdateQuery(copyData);

                long sourceCount = countRows(invitedTable);
                long shadowCount = countRows(shadowTable);

                if (sourceCount != shadowCount) {
                    logger.warning("Row count mismatch in migration: source=" + sourceCount + " shadow=" + shadowCount);
                    return;
                }

                String renameCanonical = "RENAME TABLE " + invitedTable + " TO " + backupTable;
                String renameShadow = "RENAME TABLE " + shadowTable + " TO " + invitedTable;

                try (Connection conn = mySQLProvider.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(renameCanonical);
                    stmt.executeUpdate(renameShadow);
                }

                logger.info("Migrated " + invitedTable + " to composite primary key. Backup: " + backupTable);
            }

            migrationDao.markMigrationApplied(migrationId, "invites migrated to composite primary key");
        } catch (SQLException e) {
            logger.severe("Migration failed: " + migrationId + " - " + e.getMessage());
        }
    }

    private void migrateFactionsRallyV2() {
        String migrationId = "2026_06_14_factions_rally_v2";
        if (migrationDao.isMigrationApplied(migrationId)) {
            return;
        }

        String factionsTable = MineClans.getSqlTableNames().getFactions();

        try {
            if (tableExists(factionsTable) && !columnExists(factionsTable, "rally")) {
                String alterQuery = "ALTER TABLE " + factionsTable + " ADD COLUMN rally VARCHAR(255) NULL";
                mySQLProvider.executeUpdateQuery(alterQuery);
                logger.info("Added rally column to " + factionsTable);
            }

            migrationDao.markMigrationApplied(migrationId, "factions rally column added");
        } catch (SQLException e) {
            logger.severe("Migration failed: " + migrationId + " - " + e.getMessage());
        }
    }

    private void migratePlayerJoinHistoryV1() {
        String migrationId = "2026_06_14_player_join_history_v1";
        if (migrationDao.isMigrationApplied(migrationId)) {
            return;
        }

        String joinHistoryTable = MineClans.getSqlTableNames().getPlayerJoins();

        try {
            if (!tableExists(joinHistoryTable)) {
                String createTable = "CREATE TABLE " + joinHistoryTable + " (" +
                        "event_id CHAR(36) NOT NULL PRIMARY KEY," +
                        "faction_id CHAR(36) NOT NULL," +
                        "player_id CHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "server_name VARCHAR(64) NOT NULL," +
                        "joined_at TIMESTAMP(3) NOT NULL," +
                        "INDEX idx_player_joins_faction_time (faction_id, joined_at))";
                mySQLProvider.executeUpdateQuery(createTable);
                logger.info("Created " + joinHistoryTable + " table");
            }

            migrationDao.markMigrationApplied(migrationId, "player_join_history table created");
        } catch (SQLException e) {
            logger.severe("Migration failed: " + migrationId + " - " + e.getMessage());
        }
    }

    private void migrateMarkerPreferencesV1() {
        String migrationId = "2026_06_14_marker_preferences_v1";
        if (migrationDao.isMigrationApplied(migrationId)) {
            return;
        }

        String markerPrefsTable = MineClans.getSqlTableNames().getMarkerPreferences();

        try {
            if (!tableExists(markerPrefsTable)) {
                String createTable = "CREATE TABLE " + markerPrefsTable + " (" +
                        "player_id CHAR(36) NOT NULL," +
                        "marker_key VARCHAR(32) NOT NULL," +
                        "enabled BOOLEAN NOT NULL," +
                        "PRIMARY KEY (player_id, marker_key))";
                mySQLProvider.executeUpdateQuery(createTable);
                logger.info("Created " + markerPrefsTable + " table");
            }

            migrationDao.markMigrationApplied(migrationId, "marker_preferences table created");
        } catch (SQLException e) {
            logger.severe("Migration failed: " + migrationId + " - " + e.getMessage());
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = mySQLProvider.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }

    private boolean hasTextBlobPrimaryKey(String tableName) throws SQLException {
        try (Connection conn = mySQLProvider.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
                if (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    try (ResultSet colRs = meta.getColumns(null, null, tableName, columnName)) {
                        if (colRs.next()) {
                            String typeName = colRs.getString("TYPE_NAME");
                            return "TEXT".equalsIgnoreCase(typeName) || "BLOB".equalsIgnoreCase(typeName);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasSingleColumnPrimaryKey(String tableName) throws SQLException {
        try (Connection conn = mySQLProvider.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                return count == 1;
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection conn = mySQLProvider.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        }
    }

    private long countRows(String tableName) throws SQLException {
        try (Connection conn = mySQLProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }
}
