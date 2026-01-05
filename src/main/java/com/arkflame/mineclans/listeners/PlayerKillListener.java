package com.arkflame.mineclans.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.AddKillResult.AddKillResultType;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.utils.Titles;
import com.arkflame.mineclans.utils.MelodyUtil;
import com.arkflame.mineclans.utils.MelodyUtil.Melody;

import org.bukkit.entity.Player;

public class PlayerKillListener implements Listener {

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        // Get the killed player
        Player killed = event.getEntity();

        // Check if the killer is a player
        if (killed.getKiller() != null) {
            Player killer = killed.getKiller();
            ConfigWrapper messages = MineClans.getInstance().getMessages();

            MineClans.runAsync(() -> {
                AddKillResultType result = MineClans.getInstance().getAPI().addKill(killer, killed).getType();
                if (result == AddKillResultType.SUCCESS) {
                    Titles.sendTitle(killer,
                            messages.getText("factions.kill.title").replace("%killed%", killed.getName()),
                            messages.getText("factions.kill.subtitle").replace("%killed%", killed.getName()),
                            10, 20, 10);
                    MelodyUtil.playMelody(MineClans.getInstance(), killer, Melody.KILL_REWARD_MELODY);
                } else {
                    Titles.sendTitle(killer,
                            messages.getText("factions.kill.no_reward_title").replace("%killed%", killed.getName()),
                            messages.getText("factions.kill.no_reward_subtitle").replace("%killed%", killed.getName()),
                            10, 20, 10);
                    MelodyUtil.playMelody(MineClans.getInstance(), killer, Melody.ERROR);
                }
                if (killed != null) {
                    if (!"true".equals(killed.getWorld().getGameRuleValue("keepInventory"))) {
                        MineClans.getInstance().getAPI().updatePower(killed, -4, true);
                    }
                }
            });
        }
    }
}
