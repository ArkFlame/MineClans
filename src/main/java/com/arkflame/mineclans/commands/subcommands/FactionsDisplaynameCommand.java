package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.RenameDisplayResult;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;
import com.arkflame.mineclans.modernlib.utils.ChatColors;

public class FactionsDisplaynameCommand {
    public static void onCommand(Player player, ModernArguments args) {
        String newName = args.getText(1);
        RenameDisplayResult result = MineClans.getInstance().getAPI().renameDisplay(player, ChatColors.color(newName));
        String basePath = "factions.displayname.";

        switch (result.getState()) {
            case DIFFERENT_NAME:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "different_name"));
                break;
            case NOT_IN_FACTION:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "not_in_faction"));
                break;
            case SUCCESS:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "success"));
                break;
            case NULL_NAME:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "null_name"));
                break;
            case ERROR:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "error"));
                break;
            case INVALID_NAME:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "invalid_name"));
                break;
            default:
                break;
        }
    }
}
