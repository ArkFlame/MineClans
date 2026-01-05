package com.arkflame.mineclans.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.Faction;

public class PlayerTeleportListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        MineClans.runAsync(() -> {
            Player player = event.getPlayer();
            if (player != null && player.isOnline()) {
                Faction faction = MineClans.getInstance().getAPI().getFaction(player);
                if (faction != null) {
                    Location rallyPoint = faction.getRallyPoint();
                    if (rallyPoint != null) {
                        MineClans.getInstance().getProtocolLibHook().updateFakeBeacon(player, rallyPoint);
                    }
                }
            }
        }, 1L);
    }
}
