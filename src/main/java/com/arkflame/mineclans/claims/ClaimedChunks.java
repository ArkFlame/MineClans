package com.arkflame.mineclans.claims;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.providers.daos.mysql.ClaimedChunksDAO;
import com.arkflame.mineclans.utils.LocationData;

public class ClaimedChunks {
    private final FactionChunkMap factionChunkMap;
    private final WorldChunkMap worldChunkMap;
    private final ClaimedChunksDAO claimedChunksDAO;

    public ClaimedChunks(ClaimedChunksDAO claimedChunksDAO) {
        this.claimedChunksDAO = claimedChunksDAO;
        this.factionChunkMap = new FactionChunkMap();
        this.worldChunkMap = new WorldChunkMap();
        loadAllClaimedChunks();
    }

    private void claim(UUID factionId, Set<ChunkCoordinate> chunks) {
        factionChunkMap.addChunks(factionId, chunks);
        for (ChunkCoordinate chunk : chunks) {
            if (chunk.getServerName().equals(MineClans.getServerId())) {
                worldChunkMap.addChunk(chunk);
                MineClans.getInstance().getDynmapIntegration().updateFactionClaim(chunk, factionId);
            }
        }
    }

    public void claim(UUID factionId, ChunkCoordinate chunk) {
        factionChunkMap.addChunk(factionId, chunk);
        if (chunk.getServerName().equals(MineClans.getServerId())) {
            worldChunkMap.addChunk(chunk);
            MineClans.getInstance().getDynmapIntegration().updateFactionClaim(chunk, factionId);
        }
    }

    /**
     * Loads all claimed chunks from the database into memory
     */
    public void loadAllClaimedChunks() {
        // Clear existing data
        factionChunkMap.clear();
        worldChunkMap.clear();

        // Load all chunks from the database
        Map<UUID, Set<ChunkCoordinate>> allChunks = claimedChunksDAO.getAllClaimedChunks();

        // Populate the data structures
        for (Map.Entry<UUID, Set<ChunkCoordinate>> entry : allChunks.entrySet()) {
            claim(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Loads claimed chunks for a specific faction
     * 
     * @param factionId The faction ID to load chunks for
     */
    public void loadClaimedChunks(UUID factionId) {
        claim(factionId, claimedChunksDAO.getClaimedChunks(factionId));
    }

    public void claimChunk(UUID claimingFaction, int x, int z, String worldName, String serverName,
            boolean publishUpdate) {
        unclaimChunk(x, z, worldName, publishUpdate);

        // Current time is handled by the DAO
        ChunkCoordinate chunk = new ChunkCoordinate(claimingFaction, x, z, worldName, serverName, null);

        claim(claimingFaction, chunk);

        if (publishUpdate) {
            claimedChunksDAO.claimChunk(claimingFaction, x, z, worldName, serverName);
            MineClans.getInstance().getRedisProvider().updateChunk(claimingFaction, x, z, worldName, serverName, false);
        }
    }

    public void claimChunk(UUID claimingFaction, int x, int z, String worldName, boolean publishUpdate) {
        claimChunk(claimingFaction, x, z, worldName, MineClans.getServerId(), publishUpdate);
    }

    public boolean unclaimChunk(int x, int z, String worldName, boolean publishUpdate) {
        String serverName = MineClans.getServerId();
        return unclaimChunk(x, z, worldName, serverName, publishUpdate);
    }

    public boolean unclaimChunk(int x, int z, String worldName, String serverName, boolean publishUpdate) {
        ChunkCoordinate chunk = getChunkAt(x, z, worldName, serverName);
        if (chunk == null) {
            return false;
        }

        UUID factionId = chunk.getFactionId();
        factionChunkMap.removeChunk(factionId, chunk);
        worldChunkMap.removeChunk(chunk);
        MineClans.getInstance().getDynmapIntegration().removeFactionClaim(x, z, worldName);

        if (publishUpdate) {
            claimedChunksDAO.unclaimChunk(x, z, worldName, serverName);
            MineClans.getInstance().getRedisProvider().updateChunk(factionId, x, z, worldName, serverName, true);
        }

        return true;
    }

    public void unclaimAllChunks(UUID factionId) {
        Set<ChunkCoordinate> chunks = factionChunkMap.getChunks(factionId);
        if (chunks != null) {
            for (ChunkCoordinate chunk : chunks) {
                unclaimChunk(chunk.getX(), chunk.getZ(), chunk.getWorldName(), chunk.getServerName(), false);
            }
        }
        claimedChunksDAO.unclaimAllChunks(factionId);
    }

    public boolean isChunkClaimed(int x, int z, String worldName, String serverName) {
        ChunkCoordinate chunk = worldChunkMap.getChunk(x, z, worldName, serverName);
        if (chunk == null) {
            return false;
        }

        UUID factionId = chunk.getFactionId();
        if (factionId == null) {
            return false;
        }

        return true;
    }

    public ChunkCoordinate getChunkAt(int x, int z, String worldName, String serverName) {
        if (!isChunkClaimed(x, z, worldName, serverName)) {
            return null;
        }
        return worldChunkMap.getChunk(x, z, worldName, serverName);
    }

    public Set<ChunkCoordinate> getClaimedChunks(UUID factionId) {
        return factionChunkMap.getChunks(factionId);
    }

    public int getClaimedChunkCount(UUID factionId) {
        return factionChunkMap.getChunkCount(factionId);
    }

    public Set<ChunkCoordinate> getAllClaimedChunks() {
        return worldChunkMap.getAllChunks();
    }

    public UUID getClaimingFactionId(int x, int z, String worldName, String serverName) {
        ChunkCoordinate chunk = getChunkAt(x, z, worldName, serverName);
        return chunk == null ? null : chunk.getFactionId();
    }

    // Convenience methods that use current server and world
    public boolean isChunkClaimed(int x, int z, String worldName) {
        return isChunkClaimed(x, z, worldName, MineClans.getServerId());
    }

    public ChunkCoordinate getChunkAt(int x, int z, String worldName) {
        return getChunkAt(x, z, worldName, MineClans.getServerId());
    }

    public UUID getClaimingFactionId(int x, int z, String worldName) {
        return getClaimingFactionId(x, z, worldName, MineClans.getServerId());
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return isChunkClaimed(chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), MineClans.getServerId());
    }

    public ChunkCoordinate getChunkAt(Chunk chunk) {
        if (chunk == null) {
            return null;
        }
        return getChunkAt(chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), MineClans.getServerId());
    }

    public ChunkCoordinate getChunkAt(Block block) {
        return getChunkAt(block.getX() >> 4, block.getZ() >> 4, block.getWorld().getName(), MineClans.getServerId());
    }

    public UUID getClaimingFactionId(Chunk chunk) {
        return getClaimingFactionId(chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), MineClans.getServerId());
    }

    public void claimChunk(UUID claimingFaction, Chunk chunk, boolean publishUpdate) {
        claimChunk(claimingFaction, chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), publishUpdate);
    }

    public boolean isChunkClaimed(LocationData homeLocation) {
        int chunkX = homeLocation.getBlockX() >> 4;
        int chunkZ = homeLocation.getBlockZ() >> 4;
        String worldName = homeLocation.getWorldName();
        return isChunkClaimed(chunkX, chunkZ, worldName, MineClans.getServerId());
    }

    public ChunkCoordinate getChunkAt(LocationData homeLocation) {
        int chunkX = homeLocation.getBlockX() >> 4;
        int chunkZ = homeLocation.getBlockZ() >> 4;
        String worldName = homeLocation.getWorldName();
        return getChunkAt(chunkX, chunkZ, worldName, MineClans.getServerId());
    }
}