package com.arkflame.mineclans.commands.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.modernlib.utils.ChatColors;

public class FactionsNearbyCommand {

    private static final String BASE_PATH = "factions.nearby.";
    private static final String VANISH_PERMISSION = "mineclans.vanish";
    private static final int DEFAULT_RADIUS_CHUNKS = 1;

    public static void onCommand(Player player, ModernArguments args) {
        MineClans mineClans = MineClans.getInstance();
        ConfigWrapper messages = mineClans.getMessages();

        FactionPlayer factionPlayer = mineClans.getFactionPlayerManager().getOrLoad(player.getUniqueId());
        if (factionPlayer == null) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "error")));
            return;
        }

        Faction playerFaction = factionPlayer.getFaction();

        int radius = DEFAULT_RADIUS_CHUNKS;
        if (args.hasArg(1)) {
            try {
                radius = Integer.parseInt(args.getText(1));
                if (radius < 1) {
                    radius = DEFAULT_RADIUS_CHUNKS;
                }
            } catch (NumberFormatException e) {
                radius = DEFAULT_RADIUS_CHUNKS;
            }
        }

        Location playerLocation = player.getLocation();
        int playerChunkX = playerLocation.getChunk().getX();
        int playerChunkZ = playerLocation.getChunk().getZ();
        String playerWorld = playerLocation.getWorld().getName();

        Map<String, List<NearbyPlayerInfo>> playersByFaction = new HashMap<>();

        for (Player target : mineClans.getServer().getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }

            if (target.hasPermission(VANISH_PERMISSION)) {
                continue;
            }

            Location targetLocation = target.getLocation();
            if (!targetLocation.getWorld().getName().equals(playerWorld)) {
                continue;
            }

            int targetChunkX = targetLocation.getChunk().getX();
            int targetChunkZ = targetLocation.getChunk().getZ();

            int chunkDistanceX = Math.abs(targetChunkX - playerChunkX);
            int chunkDistanceZ = Math.abs(targetChunkZ - playerChunkZ);

            if (chunkDistanceX > radius || chunkDistanceZ > radius) {
                continue;
            }

            Faction targetFaction = mineClans.getAPI().getFaction(target);
            String factionName;
            if (targetFaction != null) {
                factionName = targetFaction.getName();
            } else {
                factionName = messages.getText(BASE_PATH + "no_faction");
            }

            double distance = Math.sqrt(
                    Math.pow(targetLocation.getX() - playerLocation.getX(), 2) +
                            Math.pow(targetLocation.getZ() - playerLocation.getZ(), 2));

            NearbyPlayerInfo info = new NearbyPlayerInfo(target.getName(), distance, targetFaction);

            playersByFaction.computeIfAbsent(factionName, k -> new ArrayList<>()).add(info);
        }

        if (playersByFaction.isEmpty()) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "no_players")));
            return;
        }

        player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "header")
                .replace("{radius}", String.valueOf(radius))));

        List<String> sortedFactionNames = new ArrayList<>(playersByFaction.keySet());
        Collections.sort(sortedFactionNames);

        for (String factionName : sortedFactionNames) {
            List<NearbyPlayerInfo> players = playersByFaction.get(factionName);
            Collections.sort(players, new Comparator<NearbyPlayerInfo>() {
                @Override
                public int compare(NearbyPlayerInfo p1, NearbyPlayerInfo p2) {
                    return Double.compare(p1.distance, p2.distance);
                }
            });

            Faction faction = players.get(0).faction;
            String factionColor = getFactionColor(faction, playerFaction);

            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "faction_header")
                    .replace("{faction}", factionColor + factionName)));

            for (NearbyPlayerInfo info : players) {
                String distanceStr = String.format("%.1f", info.distance);
                player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "player_entry")
                        .replace("{player}", info.playerName)
                        .replace("{distance}", distanceStr)));
            }
        }
    }

    private static String getFactionColor(Faction targetFaction, Faction playerFaction) {
        if (targetFaction == null || playerFaction == null) {
            return "&7";
        }

        if (targetFaction.getId().equals(playerFaction.getId())) {
            return "&a";
        }

        UUID playerFactionId = playerFaction.getId();
        if (targetFaction.getRelationType(playerFactionId) != null) {
            switch (targetFaction.getRelationType(playerFactionId)) {
                case ALLY:
                    return "&b";
                case ENEMY:
                    return "&c";
                default:
                    break;
            }
        }

        return "&7";
    }

    private static class NearbyPlayerInfo {
        final String playerName;
        final double distance;
        final Faction faction;

        NearbyPlayerInfo(String playerName, double distance, Faction faction) {
            this.playerName = playerName;
            this.distance = distance;
            this.faction = faction;
        }
    }
}
