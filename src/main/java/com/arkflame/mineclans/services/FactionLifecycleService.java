package com.arkflame.mineclans.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.claims.ClaimedChunks;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.managers.LeaderboardManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.FactionPlayer;
import com.arkflame.mineclans.providers.DatabaseExecutor;
import com.arkflame.mineclans.providers.DatabaseTransaction;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.daos.mysql.FactionJoinHistoryDAO;
import com.arkflame.mineclans.providers.daos.mysql.FactionPlayerDAO;
import com.arkflame.mineclans.providers.daos.mysql.MemberDAO;
import com.arkflame.mineclans.providers.daos.mysql.MarkerPreferenceDAO;
import com.arkflame.mineclans.providers.daos.mysql.RanksDAO;
import com.arkflame.mineclans.providers.daos.mysql.RelationsDAO;
import com.arkflame.mineclans.providers.daos.mysql.ScoreDAO;
import com.arkflame.mineclans.providers.daos.mysql.ClaimedChunksDAO;
import com.arkflame.mineclans.providers.daos.mysql.ChestDAO;
import com.arkflame.mineclans.providers.daos.mysql.InvitedDAO;
import com.arkflame.mineclans.api.results.AdminDeleteResult;

public class FactionLifecycleService {
    private final DatabaseExecutor databaseExecutor;
    private final MySQLProvider mySQLProvider;
    private final FactionManager factionManager;
    private final FactionPlayerManager factionPlayerManager;
    private final com.arkflame.mineclans.providers.redis.RedisProvider redisProvider;
    private final ClaimedChunks claimedChunks;
    private final LeaderboardManager leaderboardManager;
    private final FactionMembershipPolicy policy = new FactionMembershipPolicy();

    public FactionLifecycleService(DatabaseExecutor databaseExecutor, MySQLProvider mySQLProvider,
            FactionManager factionManager, FactionPlayerManager factionPlayerManager,
            com.arkflame.mineclans.providers.redis.RedisProvider redisProvider,
            ClaimedChunks claimedChunks, LeaderboardManager leaderboardManager) {
        this.databaseExecutor = databaseExecutor;
        this.mySQLProvider = mySQLProvider;
        this.factionManager = factionManager;
        this.factionPlayerManager = factionPlayerManager;
        this.redisProvider = redisProvider;
        this.claimedChunks = claimedChunks;
        this.leaderboardManager = leaderboardManager;
    }

    public CompletableFuture<AdminDeleteResult> deleteAsAdmin(String factionName, UUID actorId) {
        return CompletableFuture.supplyAsync(() -> {
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) {
                return AdminDeleteResult.error("Faction not found: " + factionName);
            }

            try {
                mySQLProvider.withTransaction(conn -> {
                    deleteFactionAndDependencies(faction.getId());
                    return null;
                });

                if (redisProvider != null) {
                    redisProvider.removeFaction(faction.getId());
                }

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    factionManager.removeFactionFromCache(faction);
                    leaderboardManager.invalidateAfterFactionRemoval();
                });

                return AdminDeleteResult.success(factionName);
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Admin delete failed for " + factionName + ": " + e.getMessage());
                return AdminDeleteResult.error("Delete failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }

    private void deleteFactionAndDependencies(UUID factionId) throws SQLException {
        ClaimedChunksDAO claimedChunksDAO = mySQLProvider.getClaimedChunksDAO();
        ChestDAO chestDAO = mySQLProvider.getChestDAO();
        ScoreDAO scoreDAO = mySQLProvider.getScoreDAO();
        RanksDAO ranksDAO = mySQLProvider.getRanksDAO();
        InvitedDAO invitedDAO = mySQLProvider.getInvitedDAO();
        RelationsDAO relationsDAO = mySQLProvider.getRelationsDAO();
        FactionJoinHistoryDAO joinHistoryDAO = mySQLProvider.getFactionJoinHistoryDAO();
        MemberDAO memberDAO = mySQLProvider.getMemberDAO();
        FactionPlayerDAO factionPlayerDAO = mySQLProvider.getFactionPlayerDAO();

        claimedChunksDAO.unclaimAllChunks(factionId);
        chestDAO.deleteChest(factionId);
        scoreDAO.deleteScore(factionId);

        Collection<UUID> memberIds = memberDAO.getMembers(factionId);
        for (UUID memberId : memberIds) {
            ranksDAO.deleteRank(memberId);
        }
        ranksDAO.deleteRanksByFaction(factionId);

        invitedDAO.removeInvitedMembers(factionId);
        relationsDAO.removeRelationsById(factionId);
        relationsDAO.removeRelationsByTargetId(factionId);
        joinHistoryDAO.deleteByFaction(factionId);
        memberDAO.removeMembers(factionId);

        for (UUID memberId : memberIds) {
            factionPlayerDAO.updateFactionId(memberId, Optional.empty());
        }

        mySQLProvider.getFactionDAO().removeFaction(factionId);
    }

    public CompletableFuture<Void> joinFaction(UUID playerId, UUID factionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                FactionPlayerDAO playerDAO = mySQLProvider.getFactionPlayerDAO();
                MemberDAO memberDAO = mySQLProvider.getMemberDAO();
                FactionJoinHistoryDAO joinHistoryDAO = mySQLProvider.getFactionJoinHistoryDAO();

                mySQLProvider.withTransaction(conn -> {
                    playerDAO.updateFactionId(playerId, Optional.of(factionId));
                    memberDAO.addMember(factionId, playerId);
                    return null;
                });

                FactionPlayer factionPlayer = factionPlayerManager.getOrLoad(playerId);
                String playerName = factionPlayer != null ? factionPlayer.getName() : "unknown";
                String serverName = MineClans.getServerId();
                joinHistoryDAO.recordJoin(UUID.randomUUID(), factionId, playerId, playerName, serverName);

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    Faction faction = factionManager.getFaction(factionId);
                    if (faction != null) {
                        faction.addMember(playerId);
                        if (redisProvider != null) {
                            redisProvider.addPlayer(factionId, playerId);
                        }
                    }
                });
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Join faction failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }

    public CompletableFuture<Void> leaveFaction(UUID playerId, UUID factionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Faction faction = factionManager.getFaction(factionId);
                if (faction == null) {
                    return;
                }

                FactionMembershipPolicy.PolicyResult result = policy.evaluate(
                        playerId, faction.getOwner(), faction.getMembers().size(),
                        FactionMembershipPolicy.Operation.LEAVE);

                if (result != FactionMembershipPolicy.PolicyResult.ALLOWED) {
                    return;
                }

                mySQLProvider.withTransaction(conn -> {
                    FactionPlayerDAO playerDAO = mySQLProvider.getFactionPlayerDAO();
                    MemberDAO memberDAO = mySQLProvider.getMemberDAO();

                    playerDAO.updateFactionId(playerId, Optional.empty());
                    memberDAO.removeMember(factionId, playerId);
                    return null;
                });

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    faction.removeMember(playerId);
                    if (redisProvider != null) {
                        redisProvider.removePlayer(factionId, playerId);
                    }
                });
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Leave faction failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }

    public CompletableFuture<Void> kickPlayer(UUID actorId, UUID targetId, UUID factionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Faction faction = factionManager.getFaction(factionId);
                if (faction == null) {
                    return;
                }

                FactionMembershipPolicy.PolicyResult result = policy.evaluate(
                        actorId, faction.getOwner(), faction.getMembers().size(),
                        FactionMembershipPolicy.Operation.KICK);

                if (result != FactionMembershipPolicy.PolicyResult.ALLOWED) {
                    return;
                }

                mySQLProvider.withTransaction(conn -> {
                    FactionPlayerDAO playerDAO = mySQLProvider.getFactionPlayerDAO();
                    MemberDAO memberDAO = mySQLProvider.getMemberDAO();

                    playerDAO.updateFactionId(targetId, Optional.empty());
                    memberDAO.removeMember(factionId, targetId);
                    return null;
                });

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    faction.removeMember(targetId);
                    if (redisProvider != null) {
                        redisProvider.removePlayer(factionId, targetId);
                    }
                });
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Kick player failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }

    public CompletableFuture<Void> transferFaction(UUID factionId, UUID newOwnerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Faction faction = factionManager.getFaction(factionId);
                if (faction == null) {
                    return;
                }

                mySQLProvider.withTransaction(conn -> {
                    mySQLProvider.getFactionDAO().updateOwner(factionId, newOwnerId);
                    mySQLProvider.getRanksDAO().setRank(newOwnerId, Rank.LEADER);
                    return null;
                });

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    faction.setOwner(newOwnerId);
                    if (redisProvider != null) {
                        redisProvider.updateFactionOwner(factionId, newOwnerId);
                    }
                });
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Transfer faction failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }

    public CompletableFuture<Void> disbandFaction(UUID factionId, UUID actorId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Faction faction = factionManager.getFaction(factionId);
                if (faction == null) {
                    return;
                }

                FactionMembershipPolicy.PolicyResult result = policy.evaluate(
                        actorId, faction.getOwner(), faction.getMembers().size(),
                        FactionMembershipPolicy.Operation.DISBAND);

                if (result != FactionMembershipPolicy.PolicyResult.ALLOWED) {
                    return;
                }

                mySQLProvider.withTransaction(conn -> {
                    deleteFactionAndDependencies(factionId);
                    return null;
                });

                if (redisProvider != null) {
                    redisProvider.removeFaction(factionId);
                }

                Bukkit.getScheduler().runTask(MineClans.getInstance(), () -> {
                    factionManager.removeFactionFromCache(faction);
                    leaderboardManager.invalidateAfterFactionRemoval();
                });
            } catch (Exception e) {
                MineClans.getInstance().getLogger().severe("Disband faction failed: " + e.getMessage());
            }
        }, databaseExecutor.getExecutor());
    }
}
