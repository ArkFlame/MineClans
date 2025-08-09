package com.arkflame.mineclans.listeners;

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
    Player player = event.getEntity();
    MineClans plugin = MineClans.getInstance();

    // Run async task to handle DB calls and heavy logic
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        // Add death count (DB operation)
        plugin.getAPI().addDeath(player);

        Player killer = player.getKiller();
        if (killer != null) {
            // Load faction player data (likely DB operation)
            FactionPlayer factionPlayer = plugin.getAPI().getFactionPlayer(player);
            ClanEvent currentEvent = plugin.getAPI().getCurrentEvent();

            if (currentEvent != null) {
                // Switch back to main thread to run Bukkit API code
                Bukkit.getScheduler().runTask(plugin, () -> {
                    currentEvent.onFactionKill(factionPlayer);
                });
            }
        }
    });
}

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        FactionPlayer factionPlayer = MineClans.getInstance().getAPI().getFactionPlayer(player);
        ClanEvent currentEvent = MineClans.getInstance().getAPI().getCurrentEvent();
        if (currentEvent != null) {
            currentEvent.onBlockBreak(event.getBlock(), factionPlayer);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Monster) {
            Monster monster = (Monster) entity;
            Player killer = monster.getKiller();
            if (killer != null) {
                FactionPlayer factionPlayer = MineClans.getInstance().getAPI().getFactionPlayer(killer);
                ClanEvent currentEvent = MineClans.getInstance().getAPI().getCurrentEvent();
                if (currentEvent != null) {
                    currentEvent.onMonsterKill(factionPlayer);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FactionPlayer factionPlayer = MineClans.getInstance().getAPI().getFactionPlayer(player);
        int amount = event.getCaught() != null ? 1 : 0; // Assuming the event provides the caught item
        ClanEvent currentEvent = MineClans.getInstance().getAPI().getCurrentEvent();
        if (currentEvent != null) {
            currentEvent.onFishingFrenzy(factionPlayer, amount);
        }
    }
}

