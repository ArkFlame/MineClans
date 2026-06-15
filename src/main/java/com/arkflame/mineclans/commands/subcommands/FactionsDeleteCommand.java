package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.AdminDeleteResult;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;

public class FactionsDeleteCommand {
    public static void onCommand(Player player, ModernArguments args) {
        String basePath = "factions.delete.";
        String factionName = args.getText(1);

        if (factionName == null || factionName.isEmpty()) {
            player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "usage"));
            return;
        }

        if (!player.hasPermission("mineclans.admin.delete")) {
            player.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "no_permission"));
            return;
        }

        MineClans.getInstance().getFactionLifecycleService()
                .deleteAsAdmin(factionName, player.getUniqueId())
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendMessage(MineClans.getInstance().getMessages()
                                .getText(basePath + "success").replace("%faction%", result.getFactionName()));
                    } else {
                        player.sendMessage(MineClans.getInstance().getMessages()
                                .getText(basePath + "error").replace("%message%", result.getErrorMessage()));
                    }
                });
    }
}