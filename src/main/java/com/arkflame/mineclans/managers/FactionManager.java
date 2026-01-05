package com.arkflame.mineclans.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.models.Relation;
import com.arkflame.mineclans.utils.LocationData;

public class FactionManager {
    // Cache for factions by Name
    private Map<String, Faction> factionCacheByName = new ConcurrentHashMap<>();
    // Cache for factions by ID
    private Map<UUID, Faction> factionCacheByID = new ConcurrentHashMap<>();

    public Faction cache(Faction faction) {
        if (faction != null) {
            if (faction.getName() != null) factionCacheByName.put(faction.getName(), faction);
            if (faction.getId() != null) factionCacheByID.put(faction.getId(), faction);
        }
        return faction;
    }

    // Get faction from cache or load from database
    public Faction getFaction(String name) {
        if (name == null) {
            return null;
        }
        name = name.toLowerCase();
        // Check cache first
        Faction faction = factionCacheByName.get(name);
        if (faction != null) {
            return faction;
        }

        // If not in cache, load from database
        faction = cache(new Faction(name)); // Early cache, anti-multiload
        if (loadFactionFromDatabase(name, faction)) {
            return cache(faction);
        } else {
            factionCacheByName.remove(name);
            return null;
        }
    }

    // Get faction from cache or load from database
    public Faction getFaction(UUID id) {
        if (id == null) {
            return null;
        }
        // Check cache first
        Faction faction = factionCacheByID.get(id);
        if (faction != null) {
            return faction;
        }

        // If not in cache, load from database
        faction = cache(new Faction(id)); // Early cache, anti-multiload
        if (loadFactionFromDatabase(id, faction)) {
            return cache(faction);
        } else {
            factionCacheByID.remove(id);
            return null;
        }
    }

    public boolean loadFactionFromDatabase(String name, Faction faction) {
        return MineClans.getInstance().getMySQLProvider().getFactionDAO().getFactionByName(name, faction);
    }

    public boolean loadFactionFromDatabase(UUID id, Faction faction) {
        return MineClans.getInstance().getMySQLProvider().getFactionDAO().getFactionById(id, faction);
    }

    // Save a faction to the database
    public void saveFactionToDatabase(Faction faction) {
        MineClans.getInstance().getMySQLProvider().getFactionDAO().insertOrUpdateFaction(faction);
        MineClans.getInstance().getScoreManager().updateScore(faction.getId(), faction.getScore());
    }

    // Remove a faction from the database
    public void removeFactionFromDatabase(Faction faction) {
        MineClans.getInstance().getMySQLProvider().getScoreDAO().removeFaction(faction.getId());
        MineClans.getInstance().getMySQLProvider().getFactionDAO().disbandFaction(faction);
        faction.disbandFaction();
    }

    // Create a new faction
    public Faction createFaction(UUID playerId, String factionName) {
        return createFaction(playerId, factionName, UUID.randomUUID());
    }

    // Create a new faction
    public Faction createFaction(UUID playerId, String factionName, UUID factionId) {
        Faction newFaction = new Faction(factionId, playerId, factionName, factionName);
        cache(newFaction);
        return newFaction;
    }

    // Clear all factions from cache
    public void clearFactions() {
        factionCacheByName.clear();
    }

    // Add a player to a faction
    public void addPlayer(UUID factionId, UUID playerId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.addMember(playerId);
        }
    }

    // Remove a player from a faction
    public void removePlayer(UUID factionId, UUID playerId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.removeMember(playerId);
        }
    }

    // Disband a faction
    public void disbandFaction(UUID factionId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.disbandFaction();
            factionCacheByName.remove(faction.getName());
            factionCacheByID.remove(faction.getId());
        }
    }

    // Get a faction's balance
    public double getFactionBalance(String factionName) {
        Faction faction = getFaction(factionName);
        return faction != null ? faction.getBalance() : 0;
    }

    // Set a faction's balance
    public void setFactionBalance(UUID factionId, double balance) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setBalance(balance);
        }
    }

    // Update a faction's relation with another faction
    public void updateFactionRelation(UUID factionId, UUID targetFactionId, String relation) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setRelation(targetFactionId, new Relation(faction.getId(), targetFactionId, relation));
        }
    }

    // Set chest permissions for a role in a faction
    public void setFactionChestPermission(UUID factionId, String role, boolean permission) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setChestPermission(role, permission);
        }
    }

    // Add an invitation to join a faction
    public void invitePlayerToFaction(UUID factionId, UUID playerId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.invitePlayer(playerId);
        }
    }

    // Remove an invitation to join a faction
    public void uninvitePlayerFromFaction(UUID factionId, UUID playerId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.uninvitePlayer(playerId);
        }
    }

    public void updateFactionOwner(UUID factionId, UUID ownerId) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setOwner(ownerId);
        }
    }

    public void updateFactionName(UUID factionId, String newName) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            Faction existingFaction = getFaction(newName);
            if (existingFaction == null) {
                faction.setRenameCooldown();
                factionCacheByName.remove(faction.getName());
                faction.setName(newName);
                cache(faction);
            }
        }
    }

    public void updateFactionDisplayName(UUID factionId, String displayName) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setDisplayName(displayName);
        }
    }

    public void updateFriendlyFire(UUID factionId, boolean friendlyFire) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setFriendlyFire(friendlyFire);
        }
    }

    public void updateHome(UUID factionId, LocationData location) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            faction.setHome(location);
        }
    }

    public RelationType getEffectiveRelation(Faction faction1, Faction faction2) {
        if (faction1 == null || faction2 == null) {
            return RelationType.NEUTRAL; // Default relation if either faction is not found
        }

        if (faction1 == faction2 || faction1.getId().equals(faction2.getId())) {
            return RelationType.SAME_FACTION; // Same faction
        }

        RelationType relationFrom1To2 = faction1.getRelationType(faction2.getId());
        RelationType relationFrom2To1 = faction2.getRelationType(faction1.getId());

        if (relationFrom1To2 == RelationType.ENEMY || relationFrom2To1 == RelationType.ENEMY) {
            return RelationType.ENEMY;
        } else if (relationFrom1To2 == RelationType.NEUTRAL && relationFrom2To1 == RelationType.NEUTRAL) {
            return RelationType.NEUTRAL;
        } else if (relationFrom1To2 == RelationType.ALLY && relationFrom2To1 == RelationType.ALLY) {
            return RelationType.ALLY;
        }

        return RelationType.NEUTRAL; // Default relation if no specific relation is found
    }

    public RelationType getEffectiveRelation(UUID factionId1, UUID factionId2) {
        Faction faction1 = getFaction(factionId1);
        Faction faction2 = getFaction(factionId2);
        return getEffectiveRelation(faction1, faction2);
    }

    public RelationType getEffectiveRelation(String factionName1, String factionName2) {
        Faction faction1 = getFaction(factionName1);
        Faction faction2 = getFaction(factionName2);
        return getEffectiveRelation(faction1, faction2);
    }

    public boolean deposit(UUID factionId, double amount) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            double currentBalance = faction.getBalance();
            double newBalance = currentBalance + amount;
            faction.setBalance(newBalance);
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID factionId, double amount) {
        Faction faction = getFaction(factionId);
        if (faction != null) {
            double currentBalance = faction.getBalance();
            double newBalance = currentBalance - amount;
            faction.setBalance(newBalance);
            return true;
        }
        return false;
    }

    public void sendFactionMessage(Faction faction, String message) {
        faction.getMembers().forEach(memberId -> {
            Player member = MineClans.getInstance().getServer().getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        });
    }

    public void sendAllianceMessage(Faction faction, String message) {
        Map<UUID, Relation> relations = faction.getRelations();
        for (Map.Entry<UUID, Relation> entry : relations.entrySet()) {
            UUID relatedFactionId = entry.getKey();
            Relation relation = entry.getValue();

            // Check if the relation is an alliance
            if (relation.getRelationType() == RelationType.ALLY) {
                Faction relatedFaction = MineClans.getInstance().getFactionManager().getFaction(relatedFactionId);

                // Only proceed if the faction exists and has online members
                if (relatedFaction != null && relatedFaction.hasOnlineMembers()) {
                    relatedFaction.getMembers().forEach(memberId -> {
                        Player member = MineClans.getInstance().getServer().getPlayer(memberId);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(message);
                        }
                    });
                }
            }
        }
    }

    public void updateChunk(UUID factionId, int x, int z, String worldName, String serverName, boolean remove) {
        if (remove) {
            MineClans.getInstance().getClaimedChunks().unclaimChunk(x, z, worldName, serverName, false);
        } else {
            MineClans.getInstance().getClaimedChunks().claimChunk(factionId, x, z, worldName, serverName, false);
        }
    }
}
