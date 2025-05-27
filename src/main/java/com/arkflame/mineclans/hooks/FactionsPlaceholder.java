package com.arkflame.mineclans.hooks;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.events.ClanEvent;
import com.arkflame.mineclans.events.ClanEventScheduler;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;

public class FactionsPlaceholder extends PlaceholderExpansion implements Relational {

    private MineClans plugin;

    public FactionsPlaceholder(MineClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return false;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "factions";
    }

    @Override
    public String getAuthor() {
        return "ArkFlame";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return null;
        }

        Player onlinePlayer = (Player) player;

        if (identifier.startsWith("leaderboard_")) {
            try {
                int position = Integer.parseInt(identifier.replace("leaderboard_", ""));
                Faction faction = MineClans.getInstance().getLeaderboardManager().getFactionByPosition(position);
                if (faction != null) {
                    return faction.getName();
                } else {
                    return "N/A";
                }
            } catch (NumberFormatException ex) {
                // Do nothing
            }
            return "N/A";
        }

        ClanEventScheduler eventScheduler = plugin.getClanEventScheduler();
        ClanEvent currentEvent = eventScheduler.getEvent();
        ClanEvent nextEvent = eventScheduler.getNextEvent();

        switch (identifier) {
            case "event_name":
                return currentEvent == null ? (nextEvent == null ? "" : nextEvent.getName()) : currentEvent.getName();
            case "event_time_left":
                return eventScheduler.getTimeLeftFormatted();
            default:
                break;
        }

        FactionPlayer factionPlayer = plugin.getFactionPlayerManager().getOrLoad(onlinePlayer.getUniqueId());
        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return "";
        }

        Faction focusedFaction = plugin.getFactionManager().getFaction(faction.getFocusedFaction());

        switch (identifier) {
            case "name":
                return faction.getName();
            case "displayname":
                return faction.getDisplayName();
            case "prefix":
                String stars = "";
                Rank rank = faction.getRank(player.getUniqueId());
                if (rank == Rank.LEADER) {
                    stars = "**";
                } else if (rank == Rank.COLEADER) {
                    stars = "*";
                }
                return ChatColor.GREEN + stars + faction.getDisplayName() + ChatColor.RESET + " ";
            case "online":
                return String.valueOf(faction.getOnlineMembers().size());
            case "owner":
                return plugin.getFactionPlayerManager().getOrLoad(faction.getOwner()).getName();
            case "balance":
                return String.valueOf(faction.getBalance());
            case "members":
                return String.valueOf(faction.getMembers().size());
            case "focus_name":
                return focusedFaction == null ? "" : focusedFaction.getDisplayName();
            case "focus_online":
                return focusedFaction == null ? "" : String.valueOf(focusedFaction.getOnlineMembers().size());
            default:
                return "";
        }
    }

    @Override
    public String onPlaceholderRequest(Player viewer, Player target, String identifier) {
        FactionPlayer factionPlayer = MineClans.getInstance().getFactionPlayerManager().getOrLoad(target);
        Faction faction = MineClans.getInstance().getAPI().getFaction(target);
        Faction viewerFaction = MineClans.getInstance().getAPI().getFaction(viewer);
        switch (identifier) {
            case "prefix": {
                if (target == null) {
                    return RelationType.NEUTRAL.getColor().toString();
                }
                String factionDisplayName = MineClans.getInstance().getAPI().getFactionDisplayName(target);
                if (factionDisplayName == null || factionDisplayName.isEmpty()) {
                    return RelationType.NEUTRAL.getColor().toString();
                }
                String relationColor;

                if (MineClans.getInstance().getAPI().isFocusedFaction(viewer, target)) {
                    relationColor = ChatColor.LIGHT_PURPLE.toString();
                } else {
                    relationColor = MineClans.getInstance().getAPI().getRelationColor(viewer, target);
                }
                String stars = MineClans.getInstance().getAPI().getRankStars(target.getUniqueId());
                return relationColor + stars + factionDisplayName + ChatColor.RESET + " ";
            }
            case "color": {
                if (target == null) {
                    return "";
                }
                if (MineClans.getInstance().getAPI().isFocusedFaction(viewer, target)) {
                    return ChatColor.LIGHT_PURPLE.toString();
                }
                if (MineClans.getInstance().getFactionManager().getEffectiveRelation(viewerFaction, faction) == RelationType.NEUTRAL) {
                    return "";
                }
                String relationshipColor = MineClans.getInstance().getAPI().getRelationColor(viewer, target);
                return relationshipColor;
            }
            default:
                return onRequest(target, identifier);
        }
    }
}
