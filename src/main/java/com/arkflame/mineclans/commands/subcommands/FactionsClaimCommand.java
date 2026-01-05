package com.arkflame.mineclans.commands.subcommands;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.MineClansAPI;
import com.arkflame.mineclans.api.results.ClaimResult;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.modernlib.commands.ModernArguments;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.modernlib.utils.ChatColors;
import com.arkflame.mineclans.utils.LocationUtil;
import com.arkflame.mineclans.utils.NumberUtil;

public class FactionsClaimCommand {
    private static final String BASE_PATH = "factions.claim.";

    public static void onCommand(Player player, ModernArguments args) {
        MineClans mineClans = MineClans.getInstance();
        MineClansAPI api = mineClans.getAPI();
        ConfigWrapper messages = mineClans.getMessages();

        if (!mineClans.getCfg().getBoolean("claims.enabled", true)) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "disabled")));
            return;
        }

        // Check if player is in a faction
        Faction faction = api.getFaction(player);
        if (faction == null) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "not_in_faction")));
            return;
        }

        // Check player permissions
        FactionPlayer factionPlayer = api.getFactionPlayer(player);
        if (!factionPlayer.getRank().isEqualOrHigherThan(Rank.COLEADER)) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "no_permission")));
            return;
        }

        if (!LocationUtil.isChunkLoaded(player.getLocation())) {
            return;
        }

        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;
        String worldName = player.getLocation().getWorld().getName();

        // Check if overlaps with WorldGuard region
        if (MineClans.getInstance().getWorldGuardIntegration().chunkOverlapsRegion(chunkX, chunkZ, worldName)) {
            player.sendMessage(ChatColors.color(messages.getText(BASE_PATH + "overlaps_region")));
            return;
        }

        // Attempt to claim the chunk
        ClaimResult result = api.claimChunk(faction.getId(), chunkX, chunkZ, worldName, true);
        handleClaimResult(player, result, chunkX, chunkZ, worldName);
    }

    public static void handleClaimResult(Player player, ClaimResult result, int chunkX, int chunkZ, String worldName) {
        MineClans mineClans = MineClans.getInstance();
        ConfigWrapper messages = mineClans.getMessages();
        MineClansAPI api = mineClans.getAPI();
        String message = messages.getText(result.getMessagePath());
        ChunkCoordinate existingClaim = api.getClaimedChunks().getChunkAt(chunkX, chunkZ, worldName);
        String ownerName = existingClaim != null ? api.getFactionDisplayName(existingClaim.getFactionId()) : "Unknown";

        message = message
                .replace("%faction%", ownerName)
                .replace("%x%", String.valueOf(chunkX))
                .replace("%z%", String.valueOf(chunkZ));

        switch (result) {
            case CLAIM_LIMIT_REACHED:
                Faction faction = api.getFaction(player);
                player.sendMessage(ChatColors.color(message
                        .replace("%needed%", NumberUtil.formatPower(faction.getClaimedLandCount() + 1))
                        .replace("%faction_power%", NumberUtil.formatPower(faction.getPower()))
                        .replace("%hard_limit%", NumberUtil.formatPower(faction.getClaimLimit()))
                        ));
                break;
            default:
                player.sendMessage(ChatColors.color(message));
                break;
        }
    }
}