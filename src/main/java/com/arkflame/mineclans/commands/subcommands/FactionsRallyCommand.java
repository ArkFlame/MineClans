package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.RallyResult;
import com.arkflame.mineclans.api.results.RallyResult.RallyResultType;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;

public class FactionsRallyCommand {
    public static void onCommand(Player player, ModernArguments args) {
        RallyResult result = MineClans.getInstance().getAPI().rally(player);
        RallyResultType resultType = result.getResultType();
        String basePath = "factions.rally.";

        switch (resultType) {
            case NO_FACTION:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "no_faction"));
                break;
            case NO_RANK:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "no_rank"));
                break;
            case IN_COOLDOWN:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "in_cooldown"));
                break;
            case SUCCESS:
            default:
                break;
        }
    }
}
