package com.arkflame.mineclans.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.commands.subcommands.*;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;
import com.arkflame.mineclans.modernlib.commands.ModernCommand;

public class FactionsCommand extends ModernCommand {
    public FactionsCommand() {
        super("factions", "f", "clans", "clan", "guilds", "guild");
    }

    @Override
    public void onCommand(CommandSender sender, ModernArguments args) {
        String basePath = "factions.";

        if (!args.hasArg(0)) {
            FactionsHelpCommand.onCommand(sender, 1);
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MineClans.getInstance().getMessages().getText(basePath + "no-console"));
            return;
        }

        MineClans.runAsync(() -> {
            Player player = (Player) sender;
            String subcommand = args.getText(0);
            switch (subcommand.toLowerCase()) {
                case "power":
                    FactionsPowerCommand.onCommand(player, args);
                    break;
                case "map":
                    FactionsMapCommand.onCommand(player, args);
                    break;
                case "tpclaim":
                case "claimtp":
                    FactionsTPClaimCommand.onCommand(player, args);
                    break;
                case "claim":
                    FactionsClaimCommand.onCommand(player, args);
                    break;
                case "unclaim":
                    FactionsUnclaimCommand.onCommand(player, args);
                    break;
                case "claims":
                    FactionsClaimsCommand.onCommand(player, args);
                    break;
                case "create":
                    FactionsCreateCommand.onCommand(player, args);
                    break;
                case "disband":
                    FactionsDisbandCommand.onCommand(player, args);
                    break;
                case "who":
                case "show":
                    FactionsWhoCommand.onCommand(player, args);
                    break;
                case "invite":
                    FactionsInviteCommand.onCommand(player, args);
                    break;
                case "open":
                    FactionsOpenCommand.onCommand(player, args);
                    break;
                case "accept":
                case "join":
                    FactionsJoinCommand.onCommand(player, args);
                    break;
                case "leave":
                    FactionsLeaveCommand.onCommand(player, args);
                    break;
                case "transfer":
                    FactionsTransferCommand.onCommand(player, args);
                    break;
                case "rename":
                case "tag":
                    FactionsRenameCommand.onCommand(player, args);
                    break;
                case "displayname":
                    FactionsDisplaynameCommand.onCommand(player, args);
                    break;
                case "list":
                    FactionsListCommand.onCommand(player, args);
                    break;
                case "chat":
                case "c":
                    FactionsChatCommand.onCommand(player);
                    break;
                case "tl":
                case "telllocation":
                    FactionsTellLocationCommand.onCommand(player, args);
                    break;
                case "ff":
                case "friendlyfire":
                    FactionsFriendlyFireCommand.onCommand(player, args);
                    break;
                case "home":
                    FactionsHomeCommand.onCommand(player);
                    break;
                case "sethome":
                    FactionsSetHomeCommand.onCommand(player);
                    break;
                case "chest":
                    FactionsChestCommand.onCommand(player);
                    break;
                case "enemy":
                case "neutral":
                case "ally":
                    FactionsRelationSetCommand.onCommand(player, args);
                    break;
                case "promote":
                    FactionsPromoteCommand.onCommand(player, args);
                    break;
                case "demote":
                    FactionsDemoteCommand.onCommand(player, args);
                    break;
                case "kick":
                    FactionsKickCommand.onCommand(player, args);
                    break;
                case "focus":
                    FactionsFocusCommand.onCommand(player, args);
                    break;
                case "deposit":
                case "d":
                    FactionsDepositCommand.onCommand(player, args);
                    break;
                case "withdraw":
                case "w":
                    FactionsWithdrawCommand.onCommand(player, args);
                    break;
                case "events":
                case "event":
                    FactionsEventCommand.onCommand(player, args);
                    break;
                case "melody":
                    FactionsMelodyCommand.onCommand(player, args);
                    break;
                case "uninvite":
                case "deinvite":
                    FactionsUninviteCommand.onCommand(player, args);
                    break;
                case "buff":
                    FactionsBuffCommand.onCommand(player, args);
                    break;
                case "help":
                    FactionsHelpCommand.onCommand(sender, args.getNumber(1));
                    break;
                case "discord":
                    FactionsDiscordCommand.onCommand(player, args);
                    break;
                case "announcement":
                    FactionsAnnouncementCommand.onCommand(player, args);
                    break;
                case "invites":
                    FactionsInvitesCommand.onCommand(player, args);
                    break;
                case "bank":
                    FactionsBankCommand.onCommand(player, args);
                    break;
                case "god":
                    FactionsGodCommand.onCommand(player, args);
                    break;
                case "fly":
                    FactionsFlyCommand.onCommand(player, args);
                    break;
                case "rally":
                    FactionsRallyCommand.onCommand(player, args);
                    break;
                default:
                    FactionsHelpCommand.onCommand(sender, 1);
                    break;
            }
        });
    }
}
