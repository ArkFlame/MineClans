package com.arkflame.mineclans.listeners;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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
    private final FactionPlayerManager factionPlayerManager;

    public PlayerJoinListener(FactionPlayerManager factionPlayerManager) {
        this.factionPlayerManager = factionPlayerManager;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID id = event.getUniqueId();
        String name = event.getName();
        try {
            factionPlayerManager.preloadAsync(id).join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            MineClans.getInstance().getLogger().log(Level.WARNING,
                    "Failed to preload faction player for " + id + " (" + name + ")", cause);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String name = player.getName();
        MineClans mineClans = MineClans.getInstance();

        factionPlayerManager.preloadAsync(id).whenCompleteAsync((factionPlayer, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable;
                mineClans.getLogger().log(Level.WARNING,
                        "Failed to preload faction player for " + id + " (" + name + ")", cause);
                return;
            }

            if (factionPlayer.getJoinDate() == null) {
                factionPlayer.setJoinDate(new Date());
            }
            factionPlayer.setLastActive(new Date());
            factionPlayer.setName(name);

            Faction faction = mineClans.getAPI().getFaction(player);
            MineClans.runSync(() -> applyJoinState(player, mineClans, factionPlayer, faction));
        }, mineClans.getDatabaseExecutor().getExecutor());
    }

    private void applyJoinState(Player player, MineClans mineClans, FactionPlayer factionPlayer, Faction faction) {
        if (!player.isOnline() || faction == null) {
            return;
        }

        Location rallyPoint = faction.getRallyPoint();
        if (rallyPoint != null) {
            mineClans.getProtocolLibHook().showFakeBeacon(player, rallyPoint);
        }

        if (factionPlayer.updateMaxPower()) {
            factionPlayerManager.saveAsync(factionPlayer).whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                            ? throwable.getCause()
                            : throwable;
                    mineClans.getLogger().log(Level.WARNING,
                            "Failed to save faction player for " + factionPlayer.getPlayerId(), cause);
                }
            });
        }

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
            HomeResult homeResult = mineClans.getAPI().getHome(player);
            LocationData homeLocation = homeResult.getHomeLocation();
            if (homeLocation != null) {
                homeLocation.teleport(player);
            }
        }
    }
}
