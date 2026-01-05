package com.arkflame.mineclans.listeners;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.HomeResult;
import com.arkflame.mineclans.buff.ActiveBuff;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.utils.LocationData;

public class PlayerJoinListener implements Listener {
    private FactionPlayerManager factionPlayerManager;

    public PlayerJoinListener(FactionPlayerManager factionPlayerManager) {
        this.factionPlayerManager = factionPlayerManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        MineClans.runAsync(() -> {
            factionPlayerManager.updateJoinDate(id);
            factionPlayerManager.updateLastActive(id);
            factionPlayerManager.updateName(id, player.getName());

            MineClans mineClans = MineClans.getInstance();
            Faction faction = mineClans.getAPI().getFaction(player);
            if (faction != null) {
                Location rallyPoint = faction.getRallyPoint();
                if (rallyPoint != null) {
                    MineClans.getInstance().getProtocolLibHook().showFakeBeacon(player, rallyPoint);
                }
                FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(id);
                if (factionPlayer.updateMaxPower()) {
                    factionPlayerManager.save(factionPlayer);
                }
                MineClans.runSync(() -> {
                    // Update Buffs
                    for (ActiveBuff activeBuff : faction.getBuffs()) {
                        activeBuff.giveEffectToPlayer(player);
                    }
                    // Show faction announcement
                    ConfigWrapper messages = mineClans.getMessages();
                    String announcement = faction.getAnnouncement();
                    if (announcement != null) {
                        String joinAnnouncementMessage = messages.getText("factions.announcement.join");
                        player.sendMessage(
                                joinAnnouncementMessage.replace("%announcement%", announcement));
                    }
                    // Teleport to home
                    if (factionPlayer.shouldTeleportHome()) {
                        HomeResult homeResult = MineClans.getInstance().getAPI().getHome(player);
                        LocationData homeLocation = homeResult.getHomeLocation();
                        if (homeLocation != null) {
                            homeLocation.teleport(player);
                        }
                    }
                });
            }
        });
    }
}
