package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.RenameResult;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;

public class FactionsRenameCommand {
    public static void onCommand(Player player, ModernArguments args) {
        String newName = args.getText(1);
        RenameResult result = MineClans.getInstance().getAPI().rename(player, newName);
        String basePath = "factions.rename.";

        switch (result.getState()) {
            case COOLDOWN:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "cooldown"));
                break;
            case ALREADY_EXISTS:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "already_exists"));
                break;
            case NOT_IN_FACTION:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "not_in_faction"));
                break;
            case NO_PERMISSION:
                player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "no_permission"));
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
