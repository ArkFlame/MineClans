package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;

public class FactionsMapCommand {
    public static void onCommand(Player player, ModernArguments args) {
        FactionPlayer factionPlayer = MineClans.getInstance().getFactionPlayerManager().getOrLoad(player.getUniqueId());
        boolean isMapViewer = !factionPlayer.isMapViewer();
        factionPlayer.setMapViewer(isMapViewer);
        if (isMapViewer) {
            player.sendMessage(MineClans.getInstance().getMessages().getText("factions.map.enabled"));
        } else {
            player.sendMessage(MineClans.getInstance().getMessages().getText("factions.map.disabled"));
        }
    }
}
