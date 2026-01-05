package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class ClaimedChunksDAO {
    private static final String TABLE_NAME = "mineclans_chunks";

    protected String CREATE_TABLES_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "faction_id CHAR(36) NOT NULL," +
            "chunk_x INT NOT NULL," +
            "chunk_z INT NOT NULL," +
            "server_name VARCHAR(64) NOT NULL," +
            "world_name VARCHAR(64) NOT NULL," +
            "claim_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "PRIMARY KEY (faction_id, chunk_x, chunk_z, server_name, world_name)," +
            "FOREIGN KEY (faction_id) REFERENCES mineclans_factions(faction_id) ON DELETE CASCADE)";
    protected String CLAIM_CHUNK_QUERY = "INSERT INTO " + TABLE_NAME + " (faction_id, chunk_x, chunk_z, server_name, world_name, claim_date) "
            +
            "VALUES (?, ?, ?, ?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE faction_id = VALUES(faction_id), claim_date = NOW()";

    protected String UNCLAIM_CHUNK_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE chunk_x = ? AND chunk_z = ? AND server_name = ? AND world_name = ?";

    protected String UNCLAIM_ALL_CHUNKS_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE faction_id = ?";

    protected String GET_CHUNK_OWNER_QUERY = "SELECT faction_id, chunk_x, chunk_z, server_name, world_name, claim_date FROM " + TABLE_NAME + " "
            +
            "WHERE chunk_x = ? AND chunk_z = ? AND server_name = ? AND world_name = ?";

    protected String GET_CLAIMED_CHUNKS_QUERY = "SELECT chunk_x, chunk_z, server_name, world_name, claim_date FROM " + TABLE_NAME + " WHERE faction_id = ?";

    protected String GET_CLAIMED_CHUNK_COUNT_QUERY = "SELECT COUNT(*) AS count FROM " + TABLE_NAME + " WHERE faction_id = ?";

    protected String GET_ALL_CLAIMED_CHUNKS_QUERY = "SELECT faction_id, chunk_x, chunk_z, server_name, world_name, claim_date FROM " + TABLE_NAME + "";
    private MySQLProvider mySQLProvider;

    public ClaimedChunksDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLES_QUERY);
    }

    public void claimChunk(UUID factionId, int chunkX, int chunkZ, String worldName, String serverName) {
        mySQLProvider.executeUpdateQuery(CLAIM_CHUNK_QUERY, factionId.toString(), chunkX, chunkZ, worldName,
                serverName);
    }

    public void unclaimChunk(int chunkX, int chunkZ, String worldName, String serverName) {
        mySQLProvider.executeUpdateQuery(UNCLAIM_CHUNK_QUERY, chunkX, chunkZ, worldName, serverName);
    }

    public void unclaimAllChunks(UUID factionId) {
        mySQLProvider.executeUpdateQuery(UNCLAIM_ALL_CHUNKS_QUERY, factionId.toString());
    }

    public ChunkCoordinate getChunkOwner(int chunkX, int chunkZ, String worldName, String serverName) {
        AtomicReference<ChunkCoordinate> result = new AtomicReference<>(null);
        mySQLProvider.executeSelectQuery(GET_CHUNK_OWNER_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    UUID factionId = UUID.fromString(resultSet.getString("faction_id"));
                    int x = resultSet.getInt("chunk_x");
                    int z = resultSet.getInt("chunk_z");
                    String server = resultSet.getString("server_name");
                    String world = resultSet.getString("world_name");
                    Date claimDate = resultSet.getTimestamp("claim_date");
                    result.set(new ChunkCoordinate(factionId, x, z, server, world, claimDate));
                }
            };
        }, chunkX, chunkZ, worldName, serverName);
        return result.get();
    }

    public Set<ChunkCoordinate> getClaimedChunks(UUID factionId) {
        AtomicReference<Set<ChunkCoordinate>> chunks = new AtomicReference<>(new HashSet<>());
        mySQLProvider.executeSelectQuery(GET_CLAIMED_CHUNKS_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                Set<ChunkCoordinate> chunkSet = new HashSet<>();
                while (resultSet.next()) {
                    int x = resultSet.getInt("chunk_x");
                    int z = resultSet.getInt("chunk_z");
                    String server = resultSet.getString("server_name");
                    String world = resultSet.getString("world_name");
                    Date claimDate = resultSet.getTimestamp("claim_date");
                    chunkSet.add(new ChunkCoordinate(factionId, x, z, server, world, claimDate));
                }
                chunks.set(chunkSet);
            };
        }, factionId.toString());
        return chunks.get();
    }

    public int getClaimedChunkCount(UUID factionId) {
        AtomicReference<Integer> count = new AtomicReference<>(0);
        mySQLProvider.executeSelectQuery(GET_CLAIMED_CHUNK_COUNT_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    count.set(resultSet.getInt("count"));
                }
            };
        }, factionId.toString());
        return count.get();
    }

    public Map<UUID, Set<ChunkCoordinate>> getAllClaimedChunks() {
        AtomicReference<Map<UUID, Set<ChunkCoordinate>>> result = new AtomicReference<>(new ConcurrentHashMap<>());
        mySQLProvider.executeSelectQuery(GET_ALL_CLAIMED_CHUNKS_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                Map<UUID, Set<ChunkCoordinate>> chunksMap = new ConcurrentHashMap<>();
                while (resultSet.next()) {
                    UUID factionId = UUID.fromString(resultSet.getString("faction_id"));
                    int x = resultSet.getInt("chunk_x");
                    int z = resultSet.getInt("chunk_z");
                    String server = resultSet.getString("server_name");
                    String world = resultSet.getString("world_name");
                    Date claimDate = resultSet.getTimestamp("claim_date");

                    chunksMap.computeIfAbsent(factionId, k -> ConcurrentHashMap.newKeySet())
                            .add(new ChunkCoordinate(factionId, x, z, server, world, claimDate));
                }
                result.set(chunksMap);
            };
        });
        return result.get();
    }
}