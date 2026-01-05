package com.arkflame.mineclans.api;

import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.AddDeathResult;
import com.arkflame.mineclans.api.results.AddEventsWonResult;
import com.arkflame.mineclans.api.results.AddEventsWonResult.AddEventsWonResultType;
import com.arkflame.mineclans.api.results.AddKillResult;
import com.arkflame.mineclans.api.results.AddKillResult.AddKillResultType;
import com.arkflame.mineclans.api.results.AnnouncementResult;
import com.arkflame.mineclans.api.results.ClaimResult;
import com.arkflame.mineclans.api.results.CreateResult;
import com.arkflame.mineclans.api.results.CreateResult.CreateResultState;
import com.arkflame.mineclans.api.results.DepositResult;
import com.arkflame.mineclans.api.results.DepositResult.DepositResultType;
import com.arkflame.mineclans.api.results.DisbandResult;
import com.arkflame.mineclans.api.results.DisbandResult.DisbandResultState;
import com.arkflame.mineclans.api.results.DiscordResult;
import com.arkflame.mineclans.api.results.FactionChatResult;
import com.arkflame.mineclans.api.results.FocusResult;
import com.arkflame.mineclans.api.results.FocusResult.FocusResultType;
import com.arkflame.mineclans.api.results.FriendlyFireResult;
import com.arkflame.mineclans.api.results.HomeResult;
import com.arkflame.mineclans.api.results.HomeResult.HomeResultState;
import com.arkflame.mineclans.api.results.InviteResult;
import com.arkflame.mineclans.api.results.JoinResult;
import com.arkflame.mineclans.api.results.JoinResult.JoinResultState;
import com.arkflame.mineclans.api.results.KickResult;
import com.arkflame.mineclans.api.results.KickResult.KickResultType;
import com.arkflame.mineclans.api.results.OpenChestResult;
import com.arkflame.mineclans.api.results.OpenChestResult.OpenChestResultType;
import com.arkflame.mineclans.api.results.OpenResult;
import com.arkflame.mineclans.api.results.RallyResult;
import com.arkflame.mineclans.api.results.RallyResult.RallyResultType;
import com.arkflame.mineclans.api.results.RankChangeResult;
import com.arkflame.mineclans.api.results.RankChangeResult.RankChangeResultType;
import com.arkflame.mineclans.api.results.RenameDisplayResult;
import com.arkflame.mineclans.api.results.RenameDisplayResult.RenameDisplayResultState;
import com.arkflame.mineclans.api.results.RenameResult;
import com.arkflame.mineclans.api.results.RenameResult.RenameResultState;
import com.arkflame.mineclans.api.results.SetHomeResult;
import com.arkflame.mineclans.api.results.SetHomeResult.SetHomeResultState;
import com.arkflame.mineclans.api.results.SetRelationResult;
import com.arkflame.mineclans.api.results.ToggleChatResult;
import com.arkflame.mineclans.api.results.TransferResult;
import com.arkflame.mineclans.api.results.UninviteResult;
import com.arkflame.mineclans.api.results.WithdrawResult;
import com.arkflame.mineclans.api.results.WithdrawResult.WithdrawResultType;
import com.arkflame.mineclans.claims.ClaimedChunks;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.events.ClanEvent;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.models.ChunkCoordinate;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.models.Relation;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.redis.RedisProvider;
import com.arkflame.mineclans.utils.LocationData;
import com.arkflame.mineclans.utils.NameUtil;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

/*
 * MineClans API
 * 
 * Make sure to access asynchronously
 * 
 */
public class MineClansAPI {
    private final FactionManager factionManager;
    private final FactionPlayerManager factionPlayerManager;
    private final MySQLProvider mySQLProvider;
    private final RedisProvider redisProvider;

    public MineClansAPI(FactionManager factionManager, FactionPlayerManager factionPlayerManager,
            MySQLProvider mySQLProvider, RedisProvider redisProvider) {
        this.factionManager = factionManager;
        this.factionPlayerManager = factionPlayerManager;
        this.mySQLProvider = mySQLProvider;
        this.redisProvider = redisProvider;
    }

    public Faction getFaction(Player player) {
        if (player == null) {
            return null;
        }
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        return factionPlayer != null ? factionPlayer.getFaction() : null;
    }

    public Faction getFaction(String name) {
        return factionManager.getFaction(name);
    }

    public Faction getFaction(UUID id) {
        return factionManager.getFaction(id);
    }

    public FactionPlayer getFactionPlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return factionPlayerManager.getOrLoad(uuid);
    }

    public FactionPlayer getFactionPlayer(Player player) {
        if (player == null) {
            return null;
        }
        return getFactionPlayer(player.getUniqueId());
    }

    public FactionPlayer getFactionPlayer(String name) {
        if (name != null) {
            return factionPlayerManager.getOrLoad(name);
        }
        return null;
    }

    public InviteResult invite(Player player, String toInvite) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return new InviteResult(InviteResult.InviteResultState.NO_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.MODERATOR)) {
            return new InviteResult(InviteResult.InviteResultState.NO_PERMISSION);
        }

        FactionPlayer targetPlayer = factionPlayerManager.getOrLoad(toInvite);

        if (targetPlayer == null) {
            return new InviteResult(InviteResult.InviteResultState.PLAYER_NOT_FOUND, targetPlayer, faction);
        }

        UUID targetPlayerId = targetPlayer.getPlayerId();

        if (faction.getMembers().contains(targetPlayerId)) {
            return new InviteResult(InviteResult.InviteResultState.MEMBER_EXISTS, targetPlayer, faction);
        }

        if (faction.getInvited().contains(targetPlayerId)) {
            return new InviteResult(InviteResult.InviteResultState.ALREADY_INVITED, targetPlayer, faction);
        }

        factionManager.invitePlayerToFaction(faction.getId(), targetPlayerId);
        mySQLProvider.getInvitedDAO().addInvitedMember(faction.getId(), targetPlayerId);
        redisProvider.invite(faction.getId(), targetPlayerId);
        return new InviteResult(InviteResult.InviteResultState.SUCCESS, targetPlayer, faction);
    }

    public UninviteResult uninvite(Player player, String toUninvite) {
        if (toUninvite == null) {
            return new UninviteResult(UninviteResult.UninviteResultState.NULL_NAME);
        }

        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return new UninviteResult(UninviteResult.UninviteResultState.NO_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.MODERATOR)) {
            return new UninviteResult(UninviteResult.UninviteResultState.NO_PERMISSION);
        }

        FactionPlayer targetPlayer = factionPlayerManager.getOrLoad(toUninvite);

        if (targetPlayer == null) {
            return new UninviteResult(UninviteResult.UninviteResultState.PLAYER_NOT_FOUND);
        }

        UUID targetPlayerId = targetPlayer.getPlayerId();

        if (!faction.getInvited().contains(targetPlayerId)) {
            return new UninviteResult(UninviteResult.UninviteResultState.NOT_INVITED);
        }

        factionManager.uninvitePlayerFromFaction(faction.getId(), targetPlayerId);
        mySQLProvider.getInvitedDAO().removeInvitedMember(faction.getId(), targetPlayerId);
        redisProvider.uninvite(faction.getId(), player.getUniqueId());
        return new UninviteResult(UninviteResult.UninviteResultState.SUCCESS);
    }

    public CreateResult create(Player player, String factionName) {
        if (factionName == null) {
            return new CreateResult(CreateResultState.NULL_NAME, null);
        }

        if (!NameUtil.isValidName(factionName)) {
            return new CreateResult(CreateResultState.INVALID_NAME, null);
        }

        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();
        if (faction != null) {
            return new CreateResult(CreateResultState.ALREADY_HAVE_FACTION, null);
        }

        if (factionManager.getFaction(factionName) != null) {
            return new CreateResult(CreateResultState.FACTION_EXISTS, null);
        }

        try {
            // Create the faction
            faction = factionManager.createFaction(player.getUniqueId(), factionName);
            redisProvider.createFaction(faction.getId(), player.getUniqueId(), factionName);

            // Add player to faction
            factionManager.addPlayer(faction.getId(), player.getUniqueId());
            redisProvider.addPlayer(faction.getId(), player.getUniqueId());

            // Save to database
            factionManager.saveFactionToDatabase(faction);

            // Save member to database
            mySQLProvider.getMemberDAO().addMember(faction.getId(), player.getUniqueId());

            // Update player's faction
            factionPlayerManager.updateFaction(factionPlayer.getPlayerId(), faction);
            redisProvider.updateFaction(factionPlayer.getPlayerId(), faction.getName());

            // Update Rank
            factionPlayerManager.updateRank(factionPlayer.getPlayerId(), Rank.LEADER);
            factionPlayerManager.save(factionPlayer);
            mySQLProvider.getRanksDAO().setRank(factionPlayer.getPlayerId(), Rank.LEADER);
            redisProvider.updateRank(factionPlayer.getPlayerId(), Rank.LEADER);
        } catch (IllegalArgumentException ex) {
            return new CreateResult(CreateResultState.ERROR, faction);
        }

        return new CreateResult(CreateResultState.SUCCESS, faction);
    }

    public DisbandResult disband(Player player) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();
        if (faction == null) {
            return new DisbandResult(DisbandResultState.NO_FACTION);
        }
        if (factionPlayer.getRank().isLowerThan(Rank.LEADER)) {
            return new DisbandResult(DisbandResultState.NO_PERMISSION);
        }
        MineClans.getInstance().getClaimedChunks().unclaimAllChunks(faction.getId());
        factionPlayerManager.updateFaction(factionPlayer.getPlayerId(), null);
        factionPlayerManager.save(factionPlayer);
        for (UUID uuid : faction.getMembers()) {
            factionPlayerManager.updateFaction(uuid, null);
            factionPlayerManager.updateRank(uuid, Rank.RECRUIT);
            factionPlayerManager.save(uuid);
            mySQLProvider.getRanksDAO().setRank(uuid, Rank.RECRUIT);
            redisProvider.updateRank(uuid, Rank.RECRUIT);
            redisProvider.updateFaction(uuid, null);
        }
        factionManager.disbandFaction(faction.getId());
        factionManager.removeFactionFromDatabase(faction);
        MineClans.getInstance().getLeaderboardManager().removeFaction(faction.getId());
        redisProvider.removeFaction(faction.getId());
        return new DisbandResult(DisbandResultState.SUCCESS, faction);
    }

    public TransferResult transfer(Player player, String newOwnerName) {
        if (newOwnerName == null) {
            return new TransferResult(TransferResult.TransferResultState.NULL_NAME, null);
        }

        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return new TransferResult(TransferResult.TransferResultState.NO_FACTION, null);
        }

        UUID oldOwnerId = factionPlayer.getPlayerId();

        if (!faction.getOwner().equals(oldOwnerId)) {
            return new TransferResult(TransferResult.TransferResultState.NOT_OWNER, faction);
        }

        FactionPlayer newOwnerPlayer = factionPlayerManager.getOrLoad(newOwnerName);

        if (newOwnerPlayer == null || !faction.getMembers().contains(newOwnerPlayer.getPlayerId())) {
            return new TransferResult(TransferResult.TransferResultState.MEMBER_NOT_FOUND, faction);
        }

        UUID newOwnerId = newOwnerPlayer.getPlayerId();

        factionManager.updateFactionOwner(faction.getId(), newOwnerId);
        factionManager.saveFactionToDatabase(faction);
        redisProvider.updateFactionOwner(faction.getId(), newOwnerId);
        factionPlayerManager.updateRank(newOwnerId, Rank.LEADER);
        factionPlayerManager.save(newOwnerId);
        mySQLProvider.getRanksDAO().setRank(newOwnerId, Rank.LEADER);
        redisProvider.updateRank(newOwnerId, Rank.LEADER);
        factionPlayerManager.updateRank(oldOwnerId, Rank.RECRUIT);
        factionPlayerManager.save(oldOwnerId);
        mySQLProvider.getRanksDAO().setRank(oldOwnerId, Rank.RECRUIT);
        redisProvider.updateRank(oldOwnerId, Rank.RECRUIT);

        return new TransferResult(TransferResult.TransferResultState.SUCCESS, faction);
    }

    public RenameResult rename(Player player, String newName) {
        if (newName == null) {
            return new RenameResult(null, RenameResultState.NULL_NAME);
        }

        Faction faction = MineClans.getInstance().getFactionManager().getFaction(newName);
        if (faction != null) {
            return new RenameResult(null, RenameResultState.ALREADY_EXISTS);
        }

        if (!NameUtil.isValidName(newName)) {
            return new RenameResult(null, RenameResultState.INVALID_NAME);
        }

        FactionPlayer factionPlayer = factionPlayerManager
                .getOrLoad(player.getUniqueId());
        Faction playerFaction = factionPlayer.getFaction();

        if (playerFaction == null) {
            return new RenameResult(null, RenameResultState.NOT_IN_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.LEADER)) {
            return new RenameResult(playerFaction, RenameResultState.NO_PERMISSION);
        }

        if (playerFaction.isRenameCooldown()) {
            return new RenameResult(playerFaction, RenameResultState.COOLDOWN);
        }

        try {
            factionManager.updateFactionName(playerFaction.getId(), newName);
            playerFaction.setRenameCooldown();
            factionManager.saveFactionToDatabase(playerFaction);
            redisProvider.updateName(playerFaction.getId(), newName);
        } catch (IllegalArgumentException ex) {
            return new RenameResult(playerFaction, RenameResultState.ERROR);
        }

        return new RenameResult(playerFaction, RenameResultState.SUCCESS);
    }

    public RenameDisplayResult renameDisplay(Player player, String displayName) {
        if (displayName != null) {
            if (!NameUtil.isValidName(displayName)) {
                return new RenameDisplayResult(null, RenameDisplayResultState.INVALID_NAME);
            }
            FactionPlayer factionPlayer = factionPlayerManager
                    .getOrLoad(player.getUniqueId());
            Faction playerFaction = factionPlayer.getFaction();
            if (playerFaction != null) {
                try {
                    if (!displayName.toLowerCase().equals(playerFaction.getName())) {
                        return new RenameDisplayResult(playerFaction, RenameDisplayResultState.DIFFERENT_NAME);
                    }
                    factionManager.updateFactionDisplayName(playerFaction.getId(), displayName);
                    factionManager.saveFactionToDatabase(playerFaction);
                    redisProvider.updateDisplayName(playerFaction.getId(), displayName);
                } catch (IllegalArgumentException ex) {
                    return new RenameDisplayResult(playerFaction, RenameDisplayResultState.ERROR);
                }
                return new RenameDisplayResult(playerFaction, RenameDisplayResultState.SUCCESS);
            } else {
                return new RenameDisplayResult(null, RenameDisplayResultState.NOT_IN_FACTION);
            }
        } else {
            return new RenameDisplayResult(null, RenameDisplayResultState.NULL_NAME);
        }
    }

    public ToggleChatResult toggleChat(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new ToggleChatResult(ToggleChatResult.ToggleChatState.NOT_IN_FACTION);
        }

        factionPlayer.toggleChat();
        return new ToggleChatResult(factionPlayer.getChatMode());
    }

    public FactionChatResult sendFactionMessage(Player player, String message) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new FactionChatResult(FactionChatResult.FactionChatState.NOT_IN_FACTION, message, null,
                    factionPlayer);
        }

        Faction faction = factionPlayer.getFaction();
        String chatPrefix = MineClans.getInstance().getMessages().getText("factions.chat.prefix");
        String playerName = player.getName();
        String formattedMessage = chatPrefix.replace("%player%", playerName) + message;

        // Send faction message
        factionManager.sendFactionMessage(faction, formattedMessage);

        // Send by redis
        redisProvider.sendFactionMessage(faction.getId(), formattedMessage);

        return new FactionChatResult(FactionChatResult.FactionChatState.SUCCESS, message, faction, factionPlayer);
    }

    public FactionChatResult sendAllianceMessage(Player player, String message) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new FactionChatResult(FactionChatResult.FactionChatState.NOT_IN_FACTION, message, null,
                    factionPlayer);
        }

        Faction faction = factionPlayer.getFaction();
        String chatPrefix = MineClans.getInstance().getMessages().getText("factions.chat.prefix_alliance");
        String playerName = player.getName();
        String formattedMessage = chatPrefix.replace("%player%", playerName).replace("%faction%",
                faction.getDisplayName())
                + message;

        // Send faction message
        factionManager.sendFactionMessage(faction, formattedMessage);

        // Send alliance message
        factionManager.sendAllianceMessage(faction, formattedMessage);

        // Send by redis
        redisProvider.sendFactionMessage(faction.getId(), formattedMessage);
        redisProvider.sendAllianceMessage(faction.getId(), formattedMessage);

        return new FactionChatResult(FactionChatResult.FactionChatState.SUCCESS, message, faction, factionPlayer);
    }

    public FriendlyFireResult toggleFriendlyFire(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new FriendlyFireResult(FriendlyFireResult.FriendlyFireResultState.NOT_IN_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.COLEADER)) {
            return new FriendlyFireResult(FriendlyFireResult.FriendlyFireResultState.NO_PERMISSION);
        }

        Faction faction = factionPlayer.getFaction();
        boolean friendlyFire = !faction.isFriendlyFire();
        factionManager.updateFriendlyFire(faction.getId(), friendlyFire);
        factionManager.saveFactionToDatabase(faction);

        // Send redis update
        redisProvider.updateFriendlyFire(faction.getId(), friendlyFire);

        return new FriendlyFireResult(friendlyFire ? FriendlyFireResult.FriendlyFireResultState.ENABLED
                : FriendlyFireResult.FriendlyFireResultState.DISABLED);
    }

    public SetHomeResult setHome(Player player, LocationData homeLocation) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new SetHomeResult(SetHomeResultState.NOT_IN_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.COLEADER)) {
            return new SetHomeResult(SetHomeResultState.NO_PERMISSION);
        }
        Faction faction = factionPlayer.getFaction();

        if (getClaimedChunks().isChunkClaimed(homeLocation)) {
            ChunkCoordinate claim = getClaimedChunks().getChunkAt(homeLocation);
            if (claim != null && !claim.getFactionId().equals(faction.getId())) {
                return new SetHomeResult(SetHomeResultState.AT_ENEMY_CLAIM);
            }
        }

        factionManager.updateHome(faction.getId(), homeLocation);
        redisProvider.updateHome(faction.getId(), homeLocation);

        // Save changes to the faction
        factionManager.saveFactionToDatabase(faction);

        return new SetHomeResult(SetHomeResultState.SUCCESS);
    }

    public HomeResult getHome(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new HomeResult(HomeResultState.NOT_IN_FACTION);
        }

        LocationData homeLocation = factionPlayer.getFaction().getHome();
        if (homeLocation == null) {
            return new HomeResult(HomeResultState.NO_HOME_SET);
        }
        Faction faction = factionPlayer.getFaction();
        if (getClaimedChunks().isChunkClaimed(homeLocation)) {
            ChunkCoordinate claim = getClaimedChunks().getChunkAt(homeLocation);
            if (claim != null && !claim.getFactionId().equals(faction.getId())) {
                return new HomeResult(HomeResultState.HOME_IN_ENEMY_CLAIM);
            }
        }

        return new HomeResult(HomeResultState.SUCCESS, homeLocation);
    }

    public SetRelationResult setRelation(Player player, String otherFactionName, String relationName) {
        relationName = relationName.toUpperCase();

        RelationType relationType;
        try {
            relationType = RelationType.valueOf(relationName);
        } catch (IllegalArgumentException e) {
            return new SetRelationResult(SetRelationResult.SetRelationResultState.INVALID_RELATION_TYPE, null, null,
                    null, null);
        }

        Faction faction = getFaction(player);
        if (faction == null) {
            return new SetRelationResult(SetRelationResult.SetRelationResultState.NO_FACTION, null, null, null, null);
        }

        Faction otherFaction = getFaction(otherFactionName);
        if (otherFaction == null) {
            return new SetRelationResult(SetRelationResult.SetRelationResultState.OTHER_FACTION_NOT_FOUND, faction,
                    null, null, null);
        }

        if (faction == otherFaction) {
            return new SetRelationResult(SetRelationResult.SetRelationResultState.SAME_FACTION, faction,
                    otherFaction, null, null);
        }

        UUID factionId = faction.getId();
        UUID otherFactionId = otherFaction.getId();
        RelationType otherRelation = otherFaction.getRelationType(factionId);
        Relation currentRelation = faction.getRelation(otherFactionId);

        if (currentRelation != null && currentRelation.getRelationType() == relationType) {
            return new SetRelationResult(SetRelationResult.SetRelationResultState.ALREADY_RELATION, faction,
                    otherFaction, relationType, otherRelation);
        }

        factionManager.updateFactionRelation(faction.getId(), otherFactionId, relationName);
        mySQLProvider.getRelationsDAO().insertOrUpdateRelation(factionId, otherFactionId, relationName);

        // Send update to redis
        redisProvider.updateRelation(factionId, otherFactionId, relationType);
        return new SetRelationResult(SetRelationResult.SetRelationResultState.SUCCESS, faction, otherFaction,
                relationType, otherRelation);
    }

    public RelationType getRelation(Player player, String otherFactionName) {
        // Get the player's faction
        Faction faction = getFaction(player);
        if (faction == null) {
            return RelationType.NEUTRAL; // Player is not in a faction
        }

        // Get the other faction by name
        Faction otherFaction = getFaction(otherFactionName);
        if (otherFaction == null) {
            return RelationType.NEUTRAL; // Other faction not found
        }

        // Get the relation type between the factions
        return factionManager.getEffectiveRelation(faction.getName(), otherFaction.getName());
    }

    public RankChangeResult promote(Player player, String playerName) {
        FactionPlayer target = getFactionPlayer(playerName);
        FactionPlayer sender = getFactionPlayer(player.getUniqueId());

        if (target == null || sender == null) {
            return new RankChangeResult(RankChangeResultType.PLAYER_NOT_FOUND, null);
        }

        if (!isSameFaction(sender, target)) {
            return new RankChangeResult(RankChangeResultType.NOT_IN_FACTION, null);
        }

        if (sender.getRank().isLowerThan(Rank.LEADER)) {
            return new RankChangeResult(RankChangeResultType.NO_PERMISSION, null);
        }

        if (target.getRank().isEqualOrHigherThan(sender.getRank())) {
            return new RankChangeResult(RankChangeResultType.SUPERIOR_RANK, null);
        }

        Rank nextRank = target.getRank().getNext();
        if (nextRank != null) {
            if (nextRank == Rank.LEADER) {
                return new RankChangeResult(RankChangeResultType.CANNOT_PROMOTE_TO_LEADER, null);
            }

            if (nextRank == sender.getRank()) {
                return new RankChangeResult(RankChangeResultType.CANNOT_PROMOTE, null);
            }

            UUID targetPlayerId = target.getPlayerId();

            factionPlayerManager.updateRank(targetPlayerId, nextRank);
            factionPlayerManager.save(target);
            mySQLProvider.getRanksDAO().setRank(targetPlayerId, nextRank);

            // Send update to redis
            redisProvider.updateRank(targetPlayerId, nextRank);
            return new RankChangeResult(RankChangeResultType.SUCCESS, nextRank);
        }

        return new RankChangeResult(RankChangeResultType.CANNOT_PROMOTE, null);
    }

    public RankChangeResult demote(Player senderPlayer, String targetName) {
        FactionPlayer target = getFactionPlayer(targetName);
        FactionPlayer sender = getFactionPlayer(senderPlayer.getUniqueId());

        if (target == null || sender == null) {
            return new RankChangeResult(RankChangeResultType.PLAYER_NOT_FOUND, null);
        }

        if (!isSameFaction(sender, target)) {
            return new RankChangeResult(RankChangeResultType.NOT_IN_FACTION, null);
        }

        if (sender.getRank().isLowerThan(Rank.LEADER)) {
            return new RankChangeResult(RankChangeResultType.NO_PERMISSION, null);
        }

        if (target.getRank().isEqualOrHigherThan(sender.getRank())) {
            return new RankChangeResult(RankChangeResultType.SUPERIOR_RANK, null);
        }

        Rank previousRank = target.getRank().getPrevious();
        if (previousRank != null) {
            UUID targetPlayerId = target.getPlayerId();

            factionPlayerManager.updateRank(targetPlayerId, previousRank);
            factionPlayerManager.save(target);
            mySQLProvider.getRanksDAO().setRank(targetPlayerId, previousRank);

            // Send update to redis
            redisProvider.updateRank(targetPlayerId, previousRank);
            return new RankChangeResult(RankChangeResultType.SUCCESS, previousRank);
        }

        return new RankChangeResult(RankChangeResultType.CANNOT_DEMOTE, null);
    }

    public JoinResult join(Player player, String factionName) {
        if (factionName == null) {
            return new JoinResult(JoinResultState.NULL_NAME, null, null);
        }

        UUID playerId = player.getUniqueId();
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(playerId);
        Faction faction = factionPlayer.getFaction();

        if (faction != null) {
            return new JoinResult(JoinResultState.ALREADY_HAVE_FACTION, faction, factionPlayer);
        }

        faction = factionManager.getFaction(factionName);

        if (faction != null) {
            UUID factionId = faction.getId();
            if (faction.isOpen() || faction.isInvited(player)) {
                // Update Faction
                factionPlayerManager.updateFaction(playerId, faction);
                factionPlayerManager.updateRank(playerId, Rank.RECRUIT);
                factionPlayerManager.save(factionPlayer);

                // Update Player
                factionManager.addPlayer(factionId, playerId);
                factionManager.uninvitePlayerFromFaction(factionId, playerId);
                factionManager.saveFactionToDatabase(faction);

                // Update Rank/Members
                mySQLProvider.getRanksDAO().setRank(playerId, Rank.RECRUIT);
                mySQLProvider.getMemberDAO().addMember(factionId,
                        playerId);

                // Send update to redis
                redisProvider.updateFaction(playerId, faction.getName());
                redisProvider.updateRank(playerId, Rank.RECRUIT);
                redisProvider.addPlayer(factionId, playerId);
                redisProvider.uninvite(factionId, playerId);
                return new JoinResult(JoinResultState.SUCCESS, faction, factionPlayer);
            } else {
                return new JoinResult(JoinResultState.NOT_INVITED, faction, factionPlayer);
            }
        } else {
            return new JoinResult(JoinResultState.NO_FACTION, faction, factionPlayer);
        }
    }

    public KickResult kick(String playerName) {
        return kick(null, playerName);
    }

    public KickResult kick(Player kicker, String playerName) {
        try {
            // Validate kicker if provided
            Faction kickerFaction = null;
            FactionPlayer kickerPlayer = null;

            if (kicker != null) {
                kickerPlayer = factionPlayerManager.getOrLoad(kicker.getUniqueId());
                if (kickerPlayer == null) {
                    return new KickResult(KickResultType.NOT_IN_FACTION, null, null);
                }

                kickerFaction = kickerPlayer.getFaction();
                if (kickerFaction == null) {
                    return new KickResult(KickResultType.NOT_IN_FACTION, null, null);
                }

                if (kickerPlayer.getRank().isLowerThan(Rank.MODERATOR)) {
                    return new KickResult(KickResultType.NOT_MODERATOR, kickerFaction, null);
                }
            }

            // Load player to kick with additional validation
            FactionPlayer kickedPlayer = factionPlayerManager.getOrLoad(playerName);
            if (kickedPlayer == null) {
                return new KickResult(KickResultType.PLAYER_NOT_FOUND, kickerFaction, null);
            }

            Faction faction = kickedPlayer.getFaction();
            if (faction == null) {
                return new KickResult(KickResultType.NO_FACTION, kickerFaction, kickedPlayer);
            }

            if (kickedPlayer.getRank().isEqualOrHigherThan(Rank.LEADER)) {
                return new KickResult(KickResultType.FACTION_OWNER, faction, kickedPlayer);
            }

            if (kickerFaction != null && !kickerFaction.getId().equals(faction.getId())) {
                return new KickResult(KickResultType.DIFFERENT_FACTION, kickerFaction, kickedPlayer);
            }

            if (kickerPlayer != null) {
                if (kickerPlayer.getPlayerId().equals(kickedPlayer.getPlayerId())) {
                    return new KickResult(KickResultType.NOT_YOURSELF, faction, kickedPlayer);
                }

                if (kickedPlayer.getRank().isEqualOrHigherThan(kickerPlayer.getRank())) {
                    return new KickResult(KickResultType.SUPERIOR_RANK, faction, kickedPlayer);
                }
            }

            // Re-validate after synchronization
            if (!faction.isMember(kickedPlayer.getPlayerId())) {
                return new KickResult(KickResultType.ALREADY_KICKED, faction, kickedPlayer);
            }

            // Update local cache
            faction.removeMember(kickedPlayer.getPlayerId());
            kickedPlayer.setFaction(null);

            // Update Redis
            redisProvider.updateFaction(kickedPlayer.getPlayerId(), null);
            redisProvider.removePlayer(faction.getId(), kickedPlayer.getPlayerId());

            // Update data stores
            factionPlayerManager.save(kickedPlayer);
            mySQLProvider.getMemberDAO().removeMember(faction.getId(), kickedPlayer.getPlayerId());

            return new KickResult(KickResultType.SUCCESS, faction, kickedPlayer);
        } catch (Exception e) {
            MineClans.getInstance().getLogger().log(Level.SEVERE, "Error processing kick command", e);
            return new KickResult(KickResultType.ERROR, null, null);
        }
    }

    private boolean isSameFaction(FactionPlayer factionPlayer, FactionPlayer factionPlayer2) {
        if (factionPlayer == null || factionPlayer2 == null) {
            return false;
        }
        return isSameFaction(factionPlayer.getFaction(), factionPlayer2.getFaction());
    }

    private boolean isSameFaction(Faction faction, Faction faction2) {
        if (faction == null || faction2 == null) {
            return false;
        }
        return faction.getId().equals(faction2.getId());
    }

    public OpenChestResult openChest(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player);
        Faction faction = factionPlayer.getFaction();
        if (faction == null) {
            return new OpenChestResult(OpenChestResultType.NOT_IN_FACTION, faction, factionPlayer);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.MEMBER)) {
            return new OpenChestResult(OpenChestResultType.NO_PERMISSION, faction, factionPlayer);
        }

        try {
            Inventory chestInventory = faction.getChest();
            player.openInventory(chestInventory);
            return new OpenChestResult(OpenChestResultType.SUCCESS, faction, factionPlayer);
        } catch (Exception e) {
            e.printStackTrace();
            return new OpenChestResult(OpenChestResultType.ERROR, faction, factionPlayer);
        }
    }

    public FocusResult focus(Player player, String factionName) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction faction = factionPlayer.getFaction();
        if (faction == null) {
            return new FocusResult(FocusResultType.NOT_IN_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.RECRUIT)) {
            return new FocusResult(FocusResultType.NO_PERMISSION);
        }

        Faction targetFaction = MineClans.getInstance().getFactionManager().getFaction(factionName);
        if (targetFaction == null) {
            faction.setFocusedFaction(null);
            return new FocusResult(FocusResultType.FACTION_NOT_FOUND);
        }

        if (faction == targetFaction) {
            return new FocusResult(FocusResultType.SAME_FACTION);
        }

        faction.setFocusedFaction(targetFaction.getId());
        redisProvider.focus(faction.getId(), targetFaction.getId());

        return new FocusResult(FocusResultType.SUCCESS);
    }

    public WithdrawResult withdraw(Player player, double amount) {
        if (amount <= 0) {
            return new WithdrawResult(WithdrawResultType.INVALID_AMOUNT, 0); // Invalid amount
        }

        if (!MineClans.getInstance().isVaultHooked()) {
            return new WithdrawResult(WithdrawResultType.NO_ECONOMY, 0); // Vault not hooked
        }

        Economy economy = MineClans.getInstance().getVaultEconomy();

        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new WithdrawResult(WithdrawResultType.NOT_IN_FACTION, 0); // Player is not in a faction
        }

        Faction faction = factionPlayer.getFaction();
        Rank playerRank = factionPlayer.getRank();

        // Check if player rank is above a certain rank
        if (playerRank.isLowerThan(Rank.COLEADER)) {
            return new WithdrawResult(WithdrawResultType.NO_PERMISSION, 0); // Player doesn't have sufficient permission
        }

        // Check if there's enough balance in the faction
        double factionBalance = faction.getBalance();
        if (factionBalance < amount) {
            return new WithdrawResult(WithdrawResultType.INSUFFICIENT_FUNDS, factionBalance); // Not enough balance
        }

        // Assuming your FactionManager class has a method to withdraw currency
        boolean withdrawn = factionManager.withdraw(faction.getId(), amount);
        if (!withdrawn)
            return new WithdrawResult(WithdrawResultType.ERROR, 0);
        boolean deposited = economy.depositPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
        if (!deposited) {
            factionManager.deposit(faction.getId(), amount);
            return new WithdrawResult(WithdrawResultType.ERROR, 0);
        }
        // Save to database
        factionManager.saveFactionToDatabase(faction);
        redisProvider.withdraw(faction.getId(), amount);
        return new WithdrawResult(WithdrawResultType.SUCCESS, amount); // Withdrawal successful
    }

    public DepositResult deposit(Player player, double amount) {
        if (amount <= 0) {
            return new DepositResult(DepositResultType.INVALID_AMOUNT, 0); // Invalid amount
        }

        if (!MineClans.getInstance().isVaultHooked()) {
            return new DepositResult(DepositResultType.NO_ECONOMY, 0); // Vault not hooked
        }

        Economy economy = MineClans.getInstance().getVaultEconomy();

        FactionPlayer factionPlayer = getFactionPlayer(player.getUniqueId());
        if (factionPlayer == null || factionPlayer.getFaction() == null) {
            return new DepositResult(DepositResultType.NOT_IN_FACTION, 0); // Player is not in a faction
        }

        Faction faction = factionPlayer.getFaction();
        Rank playerRank = factionPlayer.getRank();

        if (playerRank.isLowerThan(Rank.RECRUIT)) {
            return new DepositResult(DepositResultType.NO_PERMISSION, 0); // Player doesn't have sufficient permission
        }

        if (!economy.has(player, amount)) {
            return new DepositResult(DepositResultType.NO_MONEY, 0); // Player doesn't have sufficient permission
        }

        // Assuming your FactionManager class has a method to deposit currency
        boolean withdrawn = economy.withdrawPlayer(player, amount).type == EconomyResponse.ResponseType.SUCCESS;
        if (!withdrawn)
            return new DepositResult(DepositResultType.ERROR, 0);
        boolean deposited = factionManager.deposit(faction.getId(), amount);
        if (!deposited) {
            economy.depositPlayer(player, amount);
            return new DepositResult(DepositResultType.ERROR, 0);
        }
        // Save to database
        factionManager.saveFactionToDatabase(faction);
        redisProvider.deposit(faction.getId(), amount);
        return new DepositResult(DepositResultType.SUCCESS, amount); // Deposit successful
    }

    public void updatePower(FactionPlayer player, double amount, boolean publishUpdate) {
        if (player != null) {
            boolean changed = player.setPower(player.getPower() + amount);
            if (changed) {
                if (publishUpdate) {
                    factionPlayerManager.save(player);
                    redisProvider.updatePower(player.getPlayerId(), amount);
                }
                Faction faction = player.getFaction();
                if (faction != null) {
                    faction.updatePower();
                }
            }
        }
    }

    public void updatePower(Player player, double amount, boolean publishUpdate) {
        FactionPlayer factionPlayer = getFactionPlayer(player);
        updatePower(factionPlayer, amount, publishUpdate);
    }

    public AddKillResult addKill(Player player, Player killed) {
        FactionPlayer factionPlayer = getFactionPlayer(player);
        FactionPlayer killedPlayer = getFactionPlayer(killed);

        // Ensure both players exist in the system
        if (factionPlayer == null || killedPlayer == null) {
            return new AddKillResult(AddKillResultType.PLAYER_NOT_FOUND);
        }

        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return new AddKillResult(AddKillResultType.NO_FACTION);
        }

        Faction killedFaction = killedPlayer.getFaction();

        // Ignore kills within the same faction
        if (isSameFaction(faction, killedFaction)) {
            return new AddKillResult(AddKillResultType.SAME_FACTION);
        }

        // Add kill to the faction player and possibly to the faction
        boolean playerKill = factionPlayer.addKill(killed.getUniqueId());
        boolean factionKill = faction.addKill(killed.getUniqueId());
        if (playerKill || factionKill) {
            if (playerKill) {
                factionPlayerManager.save(factionPlayer);
            }
            if (factionKill) {
                factionManager.saveFactionToDatabase(faction);
            }
            return new AddKillResult(AddKillResultType.SUCCESS);
        }
        return new AddKillResult(AddKillResultType.ALREADY_KILLED);
    }

    public int getKills(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player);

        if (factionPlayer == null) {
            return 0;
        }

        return factionPlayer.getKills();
    }

    public AddEventsWonResult addEvenstsWon(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player);

        // Ensure player exist in the system
        if (factionPlayer == null) {
            return new AddEventsWonResult(AddEventsWonResultType.PLAYER_NOT_FOUND);
        }

        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return new AddEventsWonResult(AddEventsWonResultType.NO_FACTION);
        }

        // Add event won
        faction.addEventsWon();
        return new AddEventsWonResult(AddEventsWonResultType.SUCCESS);
    }

    public int getEventsWon(Player player) {
        FactionPlayer factionPlayer = getFactionPlayer(player);

        if (factionPlayer == null) {
            return 0;
        }

        Faction faction = factionPlayer.getFaction();

        if (faction == null) {
            return 0;
        }

        return faction.getEventsWon();
    }

    public ClanEvent getCurrentEvent() {
        return MineClans.getInstance().getClanEventScheduler().getEvent();
    }

    public DiscordResult setDiscord(Player player, String discordLink) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction playerFaction = factionPlayer.getFaction();

        if (playerFaction == null) {
            return new DiscordResult(DiscordResult.DiscordResultState.NO_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.LEADER)) {
            return new DiscordResult(DiscordResult.DiscordResultState.NO_PERMISSION);
        }

        // Updated Regex for a valid Discord invite link
        String discordLinkPattern = "^(https?://)?(www\\.)?(discord\\.gg|discord\\.com/invite)/[a-zA-Z0-9]{1,16}$";
        Pattern pattern = Pattern.compile(discordLinkPattern);

        // Check if the discordLink is null, empty, or doesn't match the pattern
        if (discordLink == null || discordLink.isEmpty() || !pattern.matcher(discordLink).matches()) {
            if (playerFaction.setDiscord(null)) {
                factionManager.saveFactionToDatabase(playerFaction);
            }
            return new DiscordResult(DiscordResult.DiscordResultState.INVALID_DISCORD_LINK);
        }

        try {
            if (playerFaction.setDiscord(discordLink)) {
                factionManager.saveFactionToDatabase(playerFaction);
            }
            return new DiscordResult(DiscordResult.DiscordResultState.SUCCESS, playerFaction);
        } catch (IllegalArgumentException ex) {
            return new DiscordResult(DiscordResult.DiscordResultState.ERROR, playerFaction);
        }
    }

    public AnnouncementResult setAnnouncement(Player player, String announcement) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        Faction playerFaction = factionPlayer.getFaction();

        if (playerFaction == null) {
            return new AnnouncementResult(AnnouncementResult.AnnouncementResultState.NO_FACTION);
        }

        if (factionPlayer.getRank().isLowerThan(Rank.LEADER)) {
            return new AnnouncementResult(AnnouncementResult.AnnouncementResultState.NO_PERMISSION);
        }

        // Check if the discordLink is null, empty, or doesn't match the pattern
        if (announcement == null || announcement.isEmpty()) {
            if (playerFaction.setAnnouncement(null)) {
                factionManager.saveFactionToDatabase(playerFaction);
                redisProvider.setAnnouncement(playerFaction.getId(), null);
            }
            return new AnnouncementResult(AnnouncementResult.AnnouncementResultState.NO_ANNOUNCEMENT);
        }

        try {
            if (playerFaction.setAnnouncement(announcement)) {
                factionManager.saveFactionToDatabase(playerFaction);
                redisProvider.setAnnouncement(playerFaction.getId(), announcement);
            }
            return new AnnouncementResult(AnnouncementResult.AnnouncementResultState.SUCCESS, playerFaction);
        } catch (IllegalArgumentException ex) {
            return new AnnouncementResult(AnnouncementResult.AnnouncementResultState.ERROR, playerFaction);
        }
    }

    public OpenResult toggleOpen(Player player) {
        // Retrieve the FactionPlayer instance for the given player
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());

        // Retrieve the Faction instance for the player's faction
        Faction faction = factionPlayer.getFaction();

        // Check if the player is part of a faction
        if (faction == null) {
            return new OpenResult(OpenResult.OpenResultState.NO_FACTION);
        }

        // Check if the player has sufficient rank to toggle the open state
        if (factionPlayer.getRank().isLowerThan(Rank.COLEADER)) {
            return new OpenResult(OpenResult.OpenResultState.NO_PERMISSION);
        }

        // Retrieve the current open state of the faction
        boolean currentlyOpen = faction.isOpen();

        // Toggle the open state
        boolean newOpenState = !currentlyOpen;
        faction.setOpen(newOpenState);

        // Save changes to the faction
        factionManager.saveFactionToDatabase(faction);

        // Return the result indicating the new state of the faction
        return new OpenResult(OpenResult.OpenResultState.SUCCESS, faction, newOpenState);
    }

    public AddDeathResult addDeath(Player player) {
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());

        if (factionPlayer == null) {
            return new AddDeathResult(AddDeathResult.AddDeathResultState.NO_PLAYER, factionPlayer);
        }

        try {
            factionPlayer.setDeaths(factionPlayer.getDeaths() + 1);
            factionPlayerManager.save(factionPlayer);
            return new AddDeathResult(AddDeathResult.AddDeathResultState.SUCCESS, factionPlayer);
        } catch (Exception e) {
            return new AddDeathResult(AddDeathResult.AddDeathResultState.ERROR, factionPlayer);
        }
    }

    public boolean startChestUpdate(Faction faction) {
        if (faction.isEditingChest())
            return false;
        faction.setEditingChest(true);
        faction.setReceivedSubDuringUpdate(false);
        redisProvider.startChestUpdate(faction);
        if (faction.isReceivedSubDuringUpdate())
            return false;
        return true;
    }

    public void endChestUpdate(Faction faction, boolean updateChestContent) {
        if (updateChestContent) {
            // Save chest data
            mySQLProvider.getChestDAO().saveFactionChest(faction.getId(), faction.getInventory());
        }
        redisProvider.endChestUpdate(faction, updateChestContent);
        faction.setEditingChest(false);
    }

    public ClaimedChunks getClaimedChunks() {
        return MineClans.getInstance().getClaimedChunks();
    }

    public boolean isSameTeam(Player player, UUID toFactionId) {
        if (toFactionId == null) {
            return false;
        }
        FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(player.getUniqueId());
        if (factionPlayer == null) {
            return false;
        }
        Faction faction = factionPlayer.getFaction();
        if (faction == null) {
            return false;
        }
        return toFactionId.equals(faction.getId());
    }

    public ClaimResult claimChunk(UUID claimingFaction, int x, int z, String worldName, boolean publishUpdate) {
        ClaimResult claimResult = canBeClaimed(claimingFaction, x, z, worldName);

        if (claimResult.isSuccess()) {
            MineClans.getInstance().getClaimedChunks().claimChunk(claimingFaction, x, z, worldName, publishUpdate);
            return claimResult;
        }

        return claimResult;
    }

    public ClaimResult canBeClaimed(UUID claimingFactionId, int x, int z, String worldName) {
        MineClansAPI api = MineClans.getInstance().getAPI();

        // 1. Check if claiming faction exists
        Faction claimingFaction = api.getFaction(claimingFactionId);
        if (claimingFaction == null) {
            return ClaimResult.FACTION_NOT_FOUND;
        }

        // 2. Check claim limits
        int currentClaims = claimingFaction.getClaimedLandCount();
        int maxClaims = claimingFaction.getClaimLimit();
        if (currentClaims >= maxClaims) {
            return ClaimResult.CLAIM_LIMIT_REACHED;
        }

        // 3. Check if original faction still exists
        UUID chunkFactionId = MineClans.getInstance().getClaimedChunks().getClaimingFactionId(x, z, worldName);
        Faction chunkFaction = api.getFaction(chunkFactionId);
        if (chunkFaction == null) {
            return ClaimResult.CHUNK_FACTION_GONE;
        }

        // 4. Check if chunk is already claimed
        if (!MineClans.getInstance().getClaimedChunks().isChunkClaimed(x, z, worldName)) {
            return ClaimResult.SUCCESS;
        }

        if (chunkFaction.equals(claimingFaction)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        // 5. Check adjacent claims for raiding
        boolean hasAdjacentClaim = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0)
                    continue;

                ChunkCoordinate adjacent = MineClans.getInstance().getClaimedChunks().getChunkAt(x + dx, z + dz,
                        worldName);
                if (adjacent != null && claimingFactionId.equals(adjacent.getFactionId())) {
                    hasAdjacentClaim = true; // Has adjacent claim from claiming faction
                    break;
                }
            }
        }

        if (!hasAdjacentClaim) {
            return ClaimResult.NO_ADJACENT_CLAIM;
        }

        // 6. Check if enemy chunk is raidable
        if (!chunkFaction.canBeRaided()) {
            return ClaimResult.NOT_RAIDABLE;
        }

        return ClaimResult.SUCCESS;
    }

    public String getFactionDisplayName(UUID factionId) {
        if (factionId == null) {
            return "";
        }
        Faction faction = getFaction(factionId);
        if (faction == null) {
            return "";
        }
        return faction.getDisplayName();
    }

    public String getFactionDisplayName(Player target) {
        if (target == null) {
            return "";
        }
        Faction faction = getFaction(target);
        if (faction == null) {
            return "";
        }
        return faction.getDisplayName();
    }

    public String getRankStars(UUID uniqueId) {
        if (uniqueId == null) {
            return "";
        }
        FactionPlayer factionPlayer = getFactionPlayer(uniqueId);
        if (factionPlayer == null) {
            return "";
        }
        return factionPlayer.getRank().getStars();
    }

    public String getRelationColor(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return RelationType.NEUTRAL.getColor().toString();
        }
        Faction viewerFaction = getFaction(viewer);
        Faction targetFaction = getFaction(target);
        if (viewerFaction == null || targetFaction == null) {
            return RelationType.NEUTRAL.getColor().toString();
        }
        ChatColor color = factionManager.getEffectiveRelation(viewerFaction.getName(), targetFaction.getName())
                .getColor();
        if (color == null) {
            return RelationType.NEUTRAL.getColor().toString();
        }
        return color.toString();
    }

    public boolean isFocusedFaction(Player viewer, Player target) {
        Faction viewerFaction = getFaction(viewer);
        Faction targetFaction = getFaction(target);
        if (viewerFaction == null || targetFaction == null) {
            return false;
        }
        return viewerFaction.isFocusedFaction(targetFaction.getId());
    }

    public RallyResult rally(Player player) {
        Faction faction = getFaction(player);
        if (faction == null) {
            return new RallyResult(RallyResultType.NO_FACTION);
        }
        UUID playerId = player.getUniqueId();
        if (faction.getRank(playerId).isLowerThan(Rank.RECRUIT)) {
            return new RallyResult(RallyResultType.NO_RANK);
        }
        if (faction.hasRallyCooldown()) {
            return new RallyResult(RallyResultType.IN_COOLDOWN);
        }
        ConfigWrapper messages = MineClans.getInstance().getMessages();
        faction.setRally(player.getLocation());
        faction.sendMessage(messages.getText("factions.rally.success",
                "%player%", player.getName(),
                "%x%", player.getLocation().getBlockX(),
                "%z%", player.getLocation().getBlockZ()));
        return new RallyResult(RallyResultType.SUCCESS);
    }

    public Faction getFaction(Block block) {
        ChunkCoordinate chunk = getClaimedChunks().getChunkAt(block);
        if (chunk == null) {
            return null;
        }
        Faction faction = getFaction(chunk.getFactionId());
        if (faction == null) {
            return null;
        }
        return faction;
    }

    public Faction getFactionByPlayer(UUID uniqueId) {
        FactionPlayer factionPlayer = getFactionPlayer(uniqueId);
        if (factionPlayer == null) {
            return null;
        }
        return factionPlayer.getFaction();
    }
}
