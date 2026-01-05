package com.arkflame.mineclans.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.MineClansAPI;
import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.utils.particle.ParticleUtil;

public class ClaimedChunksParticleTask extends BukkitRunnable {
    private static String[] SAME_FACTION_PARTICLE = { "VILLAGER_HAPPY", "HAPPY_VILLAGER" };
    private static String[] ENEMY_FACTION_PARTICLE = { "FLAME" };
    private final MineClansAPI api;

    public ClaimedChunksParticleTask() {
        this.api = MineClans.getInstance().getAPI();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                this.cancel();
                return;
            }
            FactionPlayer factionPlayer = api.getFactionPlayer(player);
            if (factionPlayer == null) {
                continue;
            }
            if (!factionPlayer.isMapViewer()) {
                continue;
            }
            Location playerLoc = player.getLocation();
            int centerX = playerLoc.getBlockX() >> 4;
            int centerZ = playerLoc.getBlockZ() >> 4;
            double y = playerLoc.getY() + 1; // Player's Y position +1
            String worldName = playerLoc.getWorld().getName();
            Faction faction = factionPlayer.getFaction();

            // Check all 9 chunks in 3x3 grid around player
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    int chunkX = centerX + xOffset;
                    int chunkZ = centerZ + zOffset;

                    // Check if chunk is claimed
                    if (api.getClaimedChunks().isChunkClaimed(chunkX, chunkZ, worldName)) {
                        ChunkCoordinate claim = api.getClaimedChunks().getChunkAt(chunkX, chunkZ, worldName);
                        if (claim != null) {
                            boolean isSameFaction = faction != null && faction.getId().equals(claim.getFactionId());
                            String[] factionParticle = isSameFaction ? SAME_FACTION_PARTICLE
                                    : ENEMY_FACTION_PARTICLE;

                            // Show particles around chunk borders
                            for (int i = -1; i <= 1; i++) {
                                showChunkBorders(player, chunkX, chunkZ, y + i, factionParticle);
                            }
                        }
                    }
                }
            }
        }
    }

    private void showChunkBorders(Player player, int chunkX, int chunkZ, double y, String... particleType) {
        if (particleType == null || particleType.length == 0)
            return;

        Location worldLoc = new Location(player.getWorld(), chunkX << 4, (int) y, chunkZ << 4);

        // Calculate chunk corners
        double minX = worldLoc.getX();
        double maxX = minX + 16;
        double minZ = worldLoc.getZ();
        double maxZ = minZ + 16;

        // Generate particles along the borders
        int pointsPerSide = 8; // Particles per side of the chunk
        double step = 16.0 / pointsPerSide;

        // North edge (minZ)
        for (int i = 0; i <= pointsPerSide; i++) {
            Location loc = new Location(worldLoc.getWorld(), minX + (step * i), y, minZ);
            ParticleUtil.spawnParticle(player, loc, 1, particleType);
        }

        // East edge (maxX)
        for (int i = 0; i <= pointsPerSide; i++) {
            Location loc = new Location(worldLoc.getWorld(), maxX, y, minZ + (step * i));
            ParticleUtil.spawnParticle(player, loc, 1, particleType);
        }

        // South edge (maxZ)
        for (int i = 0; i <= pointsPerSide; i++) {
            Location loc = new Location(worldLoc.getWorld(), maxX - (step * i), y, maxZ);
            ParticleUtil.spawnParticle(player, loc, 1, particleType);
        }

        // West edge (minX)
        for (int i = 0; i <= pointsPerSide; i++) {
            Location loc = new Location(worldLoc.getWorld(), minX, y, maxZ - (step * i));
            ParticleUtil.spawnParticle(player, loc, 1, particleType);
        }
    }

    public static void start(long interval) {
        new ClaimedChunksParticleTask()
                .runTaskTimerAsynchronously(MineClans.getInstance(), 0L, interval);
    }
}