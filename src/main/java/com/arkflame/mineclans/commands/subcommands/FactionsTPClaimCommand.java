package com.arkflame.mineclans.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.MineClansAPI;
import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.modernlib.utils.ChatColors;
import com.arkflame.mineclans.utils.LocationData;

public class FactionsTPClaimCommand {
    private static final String BASE_PATH = "factions.tpclaim.";

    public static void onCommand(Player player, ModernArguments args) {
        MineClans mineClans = MineClans.getInstance();
        MineClansAPI api = mineClans.getAPI();
        ConfigWrapper messages = mineClans.getMessages();

        Faction faction = api.getFaction(player);
        if (faction == null) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "not_in_faction")));
            return;
        }

        List<ChunkCoordinate> claims = new ArrayList<>(api.getClaimedChunks().getClaimedChunks(faction.getId()));

        if (claims == null || claims.isEmpty()) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "no_claims")));
            return;
        }

        final int claimIndex = args.getNumber(1);
        if (claimIndex < 0 || claimIndex >= claims.size()) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "invalid_claim_number")
                    .replace("{max}", String.valueOf(claims.size()))));
            return;
        }

        final ChunkCoordinate targetClaim = claims.get(claimIndex);
        final String worldName = targetClaim.getWorldName();

        if (worldName == null || mineClans.getServer().getWorld(worldName) == null) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "invalid_world")));
            return;
        }

        int targetChunkX = targetClaim.getX();
        int targetChunkZ = targetClaim.getZ();
        String serverName = MineClans.getServerId();

        if (api.getClaimedChunks().isChunkClaimed(targetChunkX, targetChunkZ, worldName, serverName)) {
            ChunkCoordinate claim = api.getClaimedChunks().getChunkAt(targetChunkX, targetChunkZ, worldName, serverName);
            if (claim != null && !claim.getFactionId().equals(faction.getId())) {
                player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "enemy_claim")));
                return;
            }
        }

        MineClans.runSync(() -> {
            org.bukkit.World world = mineClans.getServer().getWorld(worldName);
            if (world == null) {
                return;
            }
            Chunk chunk = world.getChunkAt(targetChunkX, targetChunkZ);
            int safeY = world.getHighestBlockYAt(targetChunkX * 16 + 8, targetChunkZ * 16 + 8) + 1;
            final Location targetLocation = new Location(
                    world,
                    targetChunkX * 16 + 8,
                    safeY,
                    targetChunkZ * 16 + 8);

            int warmup = mineClans.getCfg().getInt("claims.warmup", 10);
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "teleporting")
                    .replace("{time}", String.valueOf(warmup))
                    .replace("{claim}", String.valueOf(claimIndex))
                    .replace("{x}", String.valueOf(targetChunkX))
                    .replace("{z}", String.valueOf(targetChunkZ))));

            mineClans.getTeleportScheduler().schedule(player, new LocationData(targetLocation, null), warmup);
        });
    }

    public static void showHelp(Player player) {
        MineClans mineClans = MineClans.getInstance();
        ConfigWrapper messages = mineClans.getMessages();

        player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "help.title")));
        player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "help.usage")));
        player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "help.description")));
    }
}