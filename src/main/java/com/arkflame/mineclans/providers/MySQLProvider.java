package com.arkflame.mineclans.providers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.providers.daos.mysql.ChestDAO;
import com.arkflame.mineclans.providers.daos.mysql.ClaimedChunksDAO;
import com.arkflame.mineclans.providers.daos.mysql.FactionDAO;
import com.arkflame.mineclans.providers.daos.mysql.FactionJoinHistoryDAO;
import com.arkflame.mineclans.providers.daos.mysql.FactionPlayerDAO;
import com.arkflame.mineclans.providers.daos.mysql.InvitedDAO;
import com.arkflame.mineclans.providers.daos.mysql.MemberDAO;
import com.arkflame.mineclans.providers.daos.mysql.RanksDAO;
import com.arkflame.mineclans.providers.daos.mysql.RelationsDAO;
import com.arkflame.mineclans.providers.daos.mysql.ScoreDAO;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLProvider {
    private HikariConfig config;
    private HikariDataSource dataSource = null;

    private ChestDAO chestDAO;
    private FactionDAO factionDAO;
    private FactionPlayerDAO factionPlayerDAO;
    private InvitedDAO invitedDAO;
    private MemberDAO memberDAO;
    private RanksDAO ranksDAO;
    private RelationsDAO relationsDAO;
    private ScoreDAO scoreDAO;
    private ClaimedChunksDAO claimedChunksDAO;
    private FactionJoinHistoryDAO factionJoinHistoryDAO;

    private boolean connected = false;
    private final DatabaseExecutor databaseExecutor;
    private final Logger logger;

    public MySQLProvider(boolean enabled, String url, String username, String password, DatabaseExecutor databaseExecutor, Logger logger) {
        this.databaseExecutor = databaseExecutor;
        this.logger = logger;
        try {
            if (!enabled) {
                throw new DatabaseException("Initialization", "MineClans requires MySQL-compatible storage but mysql.enabled is false");
            }

            if (url == null || !url.startsWith("jdbc:mysql:")) {
                throw new DatabaseException("Initialization", "MineClans requires MySQL-compatible JDBC URL (jdbc:mysql:...). SQLite is not supported.");
            }

            MineClans.getInstance().getLogger().info("Using MySQL database for factions.");

            chestDAO = new ChestDAO(this);
            factionDAO = new FactionDAO(this);
            factionPlayerDAO = new FactionPlayerDAO(this);
            invitedDAO = new InvitedDAO(this);
            memberDAO = new MemberDAO(this);
            ranksDAO = new RanksDAO(this);
            relationsDAO = new RelationsDAO(this);
            scoreDAO = new ScoreDAO(this);
            claimedChunksDAO = new ClaimedChunksDAO(this);
            factionJoinHistoryDAO = new FactionJoinHistoryDAO(this);

            generateHikariConfig(url, username, password);

            initialize();
        } catch (DatabaseException e) {
            logger.severe(e.getMessage());
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("An error occurred while connecting to the database.");
        } finally {
            if (!isConnected()) {
                logger.severe("=============== DATABASE CONNECTION ERROR ================");
                logger.severe("MineClans is unable to connect to the database.");
                logger.severe("To fix this, please configure the database settings in the 'config.yml' file.");
                logger.severe("You need a MySQL database for the plugin to work properly.");
                logger.severe("=============== DATABASE CONNECTION ERROR ================");
                Bukkit.getPluginManager().disablePlugin(MineClans.getInstance());
                return;
            }
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public void generateHikariConfig(String url, String username, String password) {
        config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }

    public ChestDAO getChestDAO() {
        return chestDAO;
    }

    public FactionDAO getFactionDAO() {
        return factionDAO;
    }

    public MemberDAO getMemberDAO() {
        return memberDAO;
    }

    public InvitedDAO getInvitedDAO() {
        return invitedDAO;
    }

    public RelationsDAO getRelationsDAO() {
        return relationsDAO;
    }

    public ScoreDAO getScoreDAO() {
        return scoreDAO;
    }

    public RanksDAO getRanksDAO() {
        return ranksDAO;
    }

    public FactionPlayerDAO getFactionPlayerDAO() {
        return factionPlayerDAO;
    }

    public boolean isConnected() {
        return connected;
    }

    public void createTables() {
        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(this, logger);
        runner.runMigrations();

        chestDAO.createTable();
        memberDAO.createTable();
        factionDAO.createTable();
        invitedDAO.createTable();
        relationsDAO.createTable();
        ranksDAO.createTable();
        factionPlayerDAO.createTable();
        scoreDAO.createTable();
        claimedChunksDAO.createTable();
    }

    public void initialize() {
        try {
            this.dataSource = new HikariDataSource(config);

            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                String productName = metaData.getDatabaseProductName();
                String productVersion = metaData.getDatabaseProductVersion();
                logger.info("Database product: " + productName + " " + productVersion);

                productName = productName.toLowerCase();
                if (!productName.contains("mysql") && !productName.contains("mariadb") && !productName.contains("tidb")) {
                    throw new DatabaseException("Initialization", "Unsupported database product: " + productName + ". Only MySQL, MariaDB, and TiDB are supported.");
                }
            }

            createTables();
            this.connected = true;
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            MineClans.getInstance().getLogger().info("Failed to initialize database connection: " + e.getMessage());
            this.dataSource = null;
        }
    }

    public int executeUpdateQuery(String query, Object... params) {
        if (Bukkit.isPrimaryThread()) {
            MineClans.getInstance().getLogger()
                    .severe("WARNING: This method should not be called from the main thread.");
            new Exception().printStackTrace();
        }
        if (dataSource == null) {
            throw new DatabaseException("executeUpdateQuery", "Data source is null");
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            SqlParameterBinder.bind(statement, query, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("executeUpdateQuery", "SQL error: " + e.getMessage(), e);
        }
    }

    public void executeSelectQuery(String query, ResultSetProcessor task, Object... params) {
        if (Bukkit.isPrimaryThread()) {
            MineClans.getInstance().getLogger()
                    .severe("WARNING: This method should not be called from the main thread.");
            new Exception().printStackTrace();
        }
        if (dataSource == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            SqlParameterBinder.bind(statement, query, params);
            try (ResultSet result = statement.executeQuery()) {
                task.run(result);
            }
        } catch (SQLException e) {
            throw new DatabaseException("executeSelectQuery", "SQL error: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Data source is null");
        }
        return dataSource.getConnection();
    }

    public int executeUpdate(Connection connection, String operation, String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            SqlParameterBinder.bind(statement, sql, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(operation, "SQL error: " + e.getMessage(), e);
        }
    }

    public <T> T executeQuery(Connection connection, String operation, String sql, ResultSetMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            SqlParameterBinder.bind(statement, sql, params);
            try (ResultSet result = statement.executeQuery()) {
                return mapper.map(result);
            }
        } catch (SQLException e) {
            throw new DatabaseException(operation, "SQL error: " + e.getMessage(), e);
        }
    }

    public <T> T withTransaction(DatabaseTransaction<T> transaction) {
        try (Connection connection = getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                T result = transaction.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw new DatabaseException("withTransaction", "Transaction failed: " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("withTransaction", "Failed to get connection: " + e.getMessage(), e);
        }
    }

    public ClaimedChunksDAO getClaimedChunksDAO() {
        return claimedChunksDAO;
    }

    public FactionJoinHistoryDAO getFactionJoinHistoryDAO() {
        return factionJoinHistoryDAO;
    }

    public DatabaseExecutor getDatabaseExecutor() {
        return databaseExecutor;
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
