package com.arkflame.mineclans.providers.redis;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.HomeResult;
import com.arkflame.mineclans.buff.Buff;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.utils.LocationData;
import com.arkflame.mineclans.utils.LocationUtil;

import redis.clients.jedis.JedisPubSub;

public class FactionMessageSub extends JedisPubSub {
    private final FactionManager factionManager;
    private final FactionPlayerManager factionPlayerManager;
    private final String channelName;
    private final String instanceId;
    private final Logger logger;
    
    public FactionMessageSub(FactionManager factionManager, FactionPlayerManager factionPlayerManager, String channelName, String instanceId, Logger logger) {
        this.factionManager = factionManager;
        this.factionPlayerManager = factionPlayerManager;
        this.channelName = channelName;
        this.instanceId = instanceId;
        this.logger = logger;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(channelName)) {
            String instanceId = message.substring(0, message.indexOf(":"));
            if (instanceId.equals(this.instanceId)) {
                return;
            }
            processMessage(message.substring(instanceId.length() + 1));
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(":");
        String action = parts[0];
        if (parts.length < 2)
            return;
        parts = Arrays.copyOfRange(parts, 1, parts.length);

        switch (action) {
            case "faction":
                processFactionUpdate(parts);
                break;
            case "player":
                processPlayerUpdate(parts);
                break;
            default:
                logger.warning("Unknown message type: " + parts[0]);
        }
    }

    private void processFactionUpdate(String[] parts) {
        UUID factionId = parseUUID(parts[1]);
        if (parts[0].equalsIgnoreCase("createFaction")) {
            UUID playerId = parseUUID(parts[2]);
            String factionName = parts[3];
            factionManager.createFaction(playerId, factionName, factionId);
            return;
        } else if (parts[0].equalsIgnoreCase("updateChunk")) {
            int x = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            String serverId = parts[4];
            String worldName = parts[5];
            boolean remove = Boolean.parseBoolean(parts[6]);
            factionManager.updateChunk(factionId, x, z, worldName, serverId, remove);
            return;
        }
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null)
            return;

        switch (parts[0]) {
            case "deposit":
            case "withdraw":
                double amount = parseDouble(parts[2]);
                updateFactionBalance(faction, amount, parts[0].equals("deposit"));
                break;
            case "updateHome":
                factionManager.updateHome(factionId, LocationUtil.parseLocationData(parts[2]));
                break;
            case "updateFriendlyFire":
                factionManager.updateFriendlyFire(factionId, Boolean.parseBoolean(parts[2]));
                break;
            case "invite":
                factionManager.invitePlayerToFaction(factionId, parseUUID(parts[2]));
                break;
            case "uninvite":
                factionManager.uninvitePlayerFromFaction(factionId, parseUUID(parts[2]));
                break;
            case "focus":
                faction.setFocusedFaction(parseUUID(parts[2]));
                break;
            case "announcement":
                faction.setAnnouncement(parts.length > 2 ? parts[2] : null);
                break;
            case "removePlayer":
                factionManager.removePlayer(factionId, parseUUID(parts[2]));
                break;
            case "addPlayer":
                factionManager.addPlayer(factionId, parseUUID(parts[2]));
                break;
            case "startChestUpdate":
                faction.setEditingChest(true);
                faction.setReceivedSubDuringUpdate(true);
                break;
            case "endChestUpdate":
                MineClans.runAsync(() -> {
                    try {
                        boolean updateChest = parts.length > 2 ? parts[2].equals("true") : false;
                        if (updateChest) {
                            faction.setChest(
                                    MineClans.getInstance().getMySQLProvider().getChestDAO().loadFactionChest(faction));
                        }
                    } finally {
                        faction.setEditingChest(false);
                    }
                });
                break;
            case "updateRelation":
                UUID otherFactionId = parseUUID(parts[2]);
                String relationName = parts[3];
                factionManager.updateFactionRelation(factionId, otherFactionId, relationName);
                break;
            case "updateDisplayName":
                String displayName = parts[2];
                factionManager.updateFactionDisplayName(factionId, displayName);
                break;
            case "updateName":
                String name = parts[2];
                factionManager.updateFactionDisplayName(factionId, name);
                break;
            case "updateFactionOwner":
                UUID newOwnerId = parseUUID(parts[2]);
                factionManager.updateFactionOwner(factionId, newOwnerId);
                break;
            case "removeFaction":
                factionManager.removeFactionFromCache(faction);
                MineClans.getInstance().getLeaderboardManager().invalidateAfterFactionRemoval();
                break;
            case "sendFactionMessage":
                factionManager.sendFactionMessage(faction, parts[2]);
                break;
            case "sendAllianceMessage":
                factionManager.sendAllianceMessage(faction, parts[2]);
                break;
            case "activateBuff":
                String playerName = parts[2];
                String buffName = parts[3];
                Buff buff = MineClans.getInstance().getBuffManager().getBuff(buffName);
                if (buff != null) {
                    buff.giveEffects(faction);
                    buff.notify(playerName, faction);
                }
                break;
            default:
                logger.warning("Unsupported faction action: " + parts[0]);
        }
    }

    private void updateFactionBalance(Faction faction, double amount, boolean isDeposit) {
        factionManager.setFactionBalance(
                faction.getId(),
                faction.getBalance() + (isDeposit ? amount : -amount));
    }

    private void processPlayerUpdate(String[] parts) {
        UUID playerId = parseUUID(parts[1]);
        FactionPlayer player = factionPlayerManager.getOrLoad(playerId);
        if (player == null)
            return;

        switch (parts[0]) {
            case "updatePower":
                MineClans.getInstance().getAPI().updatePower(player, parseDouble(parts[2]), false);
                break;
            case "updateFaction":
                factionPlayerManager.updateFaction(playerId, factionManager.getFaction(parts[2]));
                break;
            case "updateRank":
                factionPlayerManager.updateRank(playerId, Rank.valueOf(parts[2]));
                break;
            case "requestHome":
                if (player.isOnline()) {
                    Player bukkitPlayer = player.getPlayer();
                    HomeResult homeResult = MineClans.getInstance().getAPI().getHome(bukkitPlayer);
                    LocationData homeLocation = homeResult.getHomeLocation();
                    if (homeLocation != null) {
                        homeLocation.teleport(bukkitPlayer);
                    }
                } else {
                    player.requestHome();
                }
                break;
            default:
                logger.warning("Unsupported player action: " + parts[0]);
                break;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Failed to parse double: " + value, e);
            return 0;
        }
    }

    private UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Failed to parse UUID: " + value, e);
            return null;
        }
    }
}
