package com.arkflame.mineclans.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.events.ClanEvent;
import com.arkflame.mineclans.models.FactionPlayer;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

public class ClanEventListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Player killer = player.getKiller(); // MUST capture on main thread
        final MineClans plugin = MineClans.getInstance();
        final ClanEvent currentEvent = plugin.getAPI().getCurrentEvent(); // Capture on main thread

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Database operation
            plugin.getAPI().addDeath(player);

            if (killer != null && currentEvent != null) {
                // Load data (Assuming this involves DB lookup)
                FactionPlayer factionPlayer = plugin.getAPI().getFactionPlayer(player);

                // Switch back to main thread to trigger event logic
                Bukkit.getScheduler().runTask(plugin, () -> {
                    currentEvent.onFactionKill(factionPlayer);
                });
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        MineClans plugin = MineClans.getInstance();
        ClanEvent currentEvent = plugin.getAPI().getCurrentEvent();
        
        if (currentEvent != null) {
            FactionPlayer factionPlayer = plugin.getAPI().getFactionPlayer(player);
            currentEvent.onBlockBreak(event.getBlock(), factionPlayer);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            Monster monster = (Monster) event.getEntity();
            Player killer = monster.getKiller();
            
            if (killer != null) {
                MineClans plugin = MineClans.getInstance();
                ClanEvent currentEvent = plugin.getAPI().getCurrentEvent();
                if (currentEvent != null) {
                    FactionPlayer factionPlayer = plugin.getAPI().getFactionPlayer(killer);
                    currentEvent.onMonsterKill(factionPlayer);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getCaught() == null) return; // Didn't catch anything

        Player player = event.getPlayer();
        MineClans plugin = MineClans.getInstance();
        ClanEvent currentEvent = plugin.getAPI().getCurrentEvent();
        
        if (currentEvent != null) {
            FactionPlayer factionPlayer = plugin.getAPI().getFactionPlayer(player);
            currentEvent.onFishingFrenzy(factionPlayer, 1);
        }
    }
}
