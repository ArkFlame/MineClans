package com.arkflame.mineclans.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.utils.Materials;
import com.arkflame.mineclans.claims.ClaimedChunks;

/**
 * Handles protection of claimed chunks for Factions
 */
public class ChunkProtectionListener implements Listener {

    private final MineClans plugin;
    private final ClaimedChunks claimedChunks;

    private final Map<UUID, Long> protectionMessageCooldowns = new HashMap<>();
    private static final long PROTECTION_MESSAGE_COOLDOWN_MS = 5000; // 5 seconds

    public ChunkProtectionListener(MineClans plugin) {
        this.plugin = plugin;
        this.claimedChunks = plugin.getClaimedChunks();
    }

    /**
     * Core protection logic - determines if a player can perform an action in a
     * chunk
     * 
     * @param player     The player attempting the action
     * @param block      The block being affected
     * @param event      The event that can be cancelled
     * @param actionType Description of the action for the error message
     * @return true if the action should be allowed, false if it should be blocked
     */
    private boolean canPlayerModifyInChunk(Player player, Block block, Cancellable event, String actionType) {
        if (player == null || block == null) {
            return true;
        }

        // Allow operators to bypass protection
        if (player.isOp()) {
            return true;
        }

        // Get chunk and world information
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        String worldName = block.getWorld().getName();
        String serverName = MineClans.getServerId();

        // Check if the chunk is claimed
        if (!claimedChunks.isChunkClaimed(chunkX, chunkZ, worldName, serverName)) {
            return true; // Not claimed, allow action
        }

        // Get chunk owner
        UUID claimingFactionId = claimedChunks.getClaimingFactionId(chunkX, chunkZ, worldName, serverName);
        if (claimingFactionId == null) {
            return true; // No owner found, allow action
        }

        // Get player's faction
        UUID playerFactionId = getPlayerFactionId(player.getUniqueId());

        // If player belongs to the claiming faction, allow action
        if (playerFactionId != null && playerFactionId.equals(claimingFactionId)) {
            return true;
        }

        // Otherwise, cancel the event and notify the player
        if (event != null) {
            event.setCancelled(true);
            sendProtectionMessage(player, claimingFactionId, actionType);
        }
        return false;
    }

    /**
     * Gets a player's faction ID
     * 
     * @param playerId The player's UUID
     * @return The faction ID or null if not in a faction
     */
    private UUID getPlayerFactionId(UUID playerId) {
        FactionPlayer member = plugin.getFactionPlayerManager().getOrLoad(playerId);
        if (member == null)
            return null;
        Faction faction = member.getFaction();
        if (faction == null)
            return null;
        return faction.getId();
    }

    /**
     * Sends a protection message to a player with cooldown
     *
     * @param player     The player to notify
     * @param factionId  The ID of the faction that owns the chunk
     * @param actionType The type of action that was blocked
     */
    private void sendProtectionMessage(Player player, UUID factionId, String actionType) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check cooldown
        Long lastMessage = protectionMessageCooldowns.get(playerId);
        if (lastMessage != null && now - lastMessage < PROTECTION_MESSAGE_COOLDOWN_MS) {
            return; // Still in cooldown
        }

        // Update cooldown
        protectionMessageCooldowns.put(playerId, now);

        // Send message
        Faction faction = plugin.getFactionManager().getFaction(factionId);
        String factionName = faction != null ? faction.getName() : "Unknown Faction";
        player.sendMessage(ChatColor.RED + "You cannot " + actionType + " in " +
                ChatColor.YELLOW + factionName + ChatColor.RED + "'s territory.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        canPlayerModifyInChunk(event.getPlayer(), event.getBlock(), event, "break blocks");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        canPlayerModifyInChunk(event.getPlayer(), event.getBlock(), event, "place blocks");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        canPlayerModifyInChunk(event.getPlayer(), event.getBlockClicked(), event, "use buckets");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        canPlayerModifyInChunk(event.getPlayer(), event.getBlockClicked(), event, "use buckets");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Skip if not right-clicking a block
        if (event.getClickedBlock() == null)
            return;

        Block block = event.getClickedBlock();
        // Skip if the block is not interactable
        if (!Materials.interactables.contains(block.getType())) {
            return;
        }
        canPlayerModifyInChunk(event.getPlayer(), block, event, "interact with blocks");
        if (block != null && event.isCancelled()) {
            MineClans.runAsync(() -> {
                MineClans.getInstance().getProtocolLibHook().sendBlockChange(event.getPlayer(), block);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        // Protect certain entities (armor stands, item frames, etc.)
        Entity entity = event.getRightClicked();

        // Allow interact if target is a mob
        if (entity instanceof LivingEntity)
            return;

        Block block = entity.getLocation().getBlock();
        canPlayerModifyInChunk(event.getPlayer(), block, event, "interact with entities");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Skip if not caused by a player
        if (!(event.getDamager() instanceof Player))
            return;

        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();

        // Skip if target is a player (PvP is handled elsewhere)
        if (target instanceof Player)
            return;

        // Allow hit if target is a mob
        if (target instanceof LivingEntity)
            return;

        Block block = target.getLocation().getBlock();
        canPlayerModifyInChunk(player, block, event, "damage entities");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block from = event.getBlock();
        Faction fromFaction = MineClans.getInstance().getAPI().getFaction(from);
        Block to = event.getToBlock();
        Faction toFaction = MineClans.getInstance().getAPI().getFaction(to);
        if (fromFaction == null && toFaction == null) {
            return;
        }
        if (fromFaction != null && fromFaction.equals(toFaction)) {
            return;
        }
        if (toFaction != null && toFaction.equals(fromFaction)) {
            return;
        }
        canPlayerModifyInChunk(null, from, null, "move blocks");
        canPlayerModifyInChunk(null, to, null, "move blocks");
    }
}