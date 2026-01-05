package com.arkflame.mineclans.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;

/**
 * ProtocolLibHook sends fake blocks using ProtocolLib.
 */
public class ProtocolLibHook {
    private final boolean enabled;
    private final ProtocolManager protocolManager;

    private final Map<UUID, CachedBeacon> beaconCache = new HashMap<>();

    public ProtocolLibHook(Plugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            this.enabled = true;
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            Bukkit.getLogger().info("[ProtocolLibHook] ProtocolLib detected. Fake-beacon functionality is enabled.");
        } else {
            this.enabled = false;
            this.protocolManager = null;
            Bukkit.getLogger()
                    .warning("[ProtocolLibHook] ProtocolLib not found. Fake-beacon functionality is disabled.");
        }
    }

    public void updateFakeBeacon(Player player, Location center) {
        if (!enabled || protocolManager == null || player == null || center == null)
            return;

        // Too far away
        if (!player.getWorld().equals(center.getWorld()) || player.getLocation().distance(center) > 128) {
            return;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Send the beacon one block below the center
        Location beaconLoc = new Location(center.getWorld(), cx, cy - 1, cz);
        sendBlockChange(player, beaconLoc, Material.BEACON);

        // 2. Send the 3x3 iron base two blocks below the center
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location baseLoc = new Location(center.getWorld(), cx + dx, cy - 2, cz + dz);
                sendBlockChange(player, baseLoc, Material.IRON_BLOCK);
            }
        }
    }

    public void showFakeBeacon(Player player, Location center) {
        if (!enabled || protocolManager == null)
            return;

        // Too far away
        Location loc = player.getLocation();
        if (!loc.getWorld().equals(center.getWorld()) || loc.distance(center) > 128) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (beaconCache.containsKey(uuid)) {
            removeFakeBeacon(player);
        }

        CachedBeacon cache = new CachedBeacon();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Cache and send the beacon one block below the center
        Location beaconLoc = new Location(center.getWorld(), cx, cy - 1, cz);
        cache.storeOriginal(beaconLoc);
        sendBlockChange(player, beaconLoc, Material.BEACON);

        // 2. Cache and send the 3x3 iron base two blocks below the center
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location baseLoc = new Location(center.getWorld(), cx + dx, cy - 2, cz + dz);
                cache.storeOriginal(baseLoc);
                sendBlockChange(player, baseLoc, Material.IRON_BLOCK);
            }
        }

        beaconCache.put(uuid, cache);
    }

    public void removeFakeBeacon(Player player) {
        if (!enabled || protocolManager == null)
            return;

        UUID uuid = player.getUniqueId();
        CachedBeacon cache = beaconCache.remove(uuid);
        if (cache != null) {
            cache.restoreAll(player);
        }
    }

    private void sendBlockChange(Player player, Location loc, Material mat) {
        if (!enabled || protocolManager == null)
            return;

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        BlockPosition position = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        packet.getBlockPositionModifier().write(0, position);
        packet.getBlockData().write(0, WrappedBlockData.createData(mat));

        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ProtocolLibHook] Failed to send block change packet: " + e.getMessage());
        }
    }

    private class CachedBeacon {
        private final Collection<Location> originalLocations = new HashSet<>();

        public void storeOriginal(Location loc) {
            originalLocations.add(loc);
        }

        public void restoreAll(Player player) {
            for (Location loc : originalLocations) {
                World world = loc.getWorld();
                if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    return;
                }
                sendBlockChange(player, loc.getBlock());
            }
        }
    }

    public void cleanup() {
        if (enabled && protocolManager != null) {
            for (UUID uuid : new HashSet<>(beaconCache.keySet())) {
                removeFakeBeacon(Bukkit.getPlayer(uuid));
            }
        }
    }

    public void sendBlockChange(Player player, Block block) {
        if (enabled && protocolManager != null) {
            PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
            packet.getBlockPositionModifier().write(0, position);
            packet.getBlockData().write(0,
                    WrappedBlockData.createData(block.getType(), block.getData()));
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (Exception e) {
                Bukkit.getLogger().severe("[ProtocolLibHook] Failed to restore block: " + e.getMessage());
            }
        }
    }
}