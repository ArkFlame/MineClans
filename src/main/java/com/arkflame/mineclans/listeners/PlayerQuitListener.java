package com.arkflame.mineclans.listeners;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.models.Faction;

public class PlayerQuitListener implements Listener {
    private FactionPlayerManager factionPlayerManager;

    public PlayerQuitListener(FactionPlayerManager factionPlayerManager) {
        this.factionPlayerManager = factionPlayerManager;
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = event.getPlayer().getUniqueId();
        MineClans.runAsync(() -> {
            factionPlayerManager.updateLastActive(id);
            factionPlayerManager.save(id);
            Faction faction = MineClans.getInstance().getAPI().getFaction(player);
            if (faction != null) {
                Location rallyPoint = faction.getRallyPoint();
                if (rallyPoint != null) {
                    MineClans.getInstance().getProtocolLibHook().removeFakeBeacon(player);
                }
                faction.removeEffects(player);
            }
        });
    }
}
