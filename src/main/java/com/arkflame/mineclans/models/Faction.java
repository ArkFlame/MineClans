package com.arkflame.mineclans.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.buff.ActiveBuff;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.utils.Titles;
import com.arkflame.mineclans.utils.FactionNamingUtil;
import com.arkflame.mineclans.utils.LocationData;
import com.arkflame.mineclans.utils.LocationUtil;
import com.arkflame.mineclans.utils.MelodyUtil;
import com.arkflame.mineclans.utils.MelodyUtil.Melody;

import net.md_5.bungee.api.ChatColor;

public class Faction implements InventoryHolder {
    // The ID
    private UUID id;

    // Members
    private Collection<UUID> members = ConcurrentHashMap.newKeySet();

    // Owner of the faction
    private UUID owner;

    // Invited members
    private Collection<UUID> invited = ConcurrentHashMap.newKeySet();

    // Friendly fire
    private boolean friendlyFire = false;

    // Faction Home
    private LocationData home;

    // Display name
    private String displayName;

    // Faction name
    private String name;

    // Faction balance (economy integration)
    private double balance;

    // Faction relations
    private Map<UUID, Relation> relations = new ConcurrentHashMap<>(); // UUID -> Relation

    // Faction chest permissions
    private Map<String, Boolean> chestPermissions = new ConcurrentHashMap<>(); // Role -> permission

    // Ranks
    private Map<UUID, Rank> ranks = new ConcurrentHashMap<>(); // UUID -> rank

    // Chest Inventory
    private Inventory chestInventory;

    // Focused Faction
    private UUID focusedFaction = null;

    // Kills
    private int kills = 0;
    private Collection<UUID> killedPlayers = new HashSet<>();

    // Events Won
    private int eventsWon = 0;

    // Score
    private double score = 0;

    // Power
    private int power = 0;

    private boolean receivedSubDuringUpdate = false;
    private boolean editingChest = false;

    // Active Buffs
    private Collection<ActiveBuff> activeBuffs = ConcurrentHashMap.newKeySet();

    private boolean open = false;

    private Date creationDate = new Date();

    private String announcement = null;

    private String discord = null;

    private long lastRename = 0; // Time of the last rename in milliseconds
    private static final long RENAME_COOLDOWN = 60 * 1000; // Cooldown duration in milliseconds (e.g., 1 minute)

    private long lastRally = 0; // Time of the last rally in milliseconds
    private static final long RALLY_COOLDOWN = 5 * 1000; // Cooldown duration in milliseconds (e.g., 1 minute)
    private Location rally = null;

    public Faction(String name) {
        this.name = name.toLowerCase();
        this.displayName = name;
    }

    public Faction(UUID id) {
        this.id = id;
    }

    public Faction(UUID id, UUID owner, String name, String displayName) {
        this.id = id;
        this.owner = owner;
        this.name = name.toLowerCase();
        this.displayName = displayName;
    }

    public Faction setup(UUID id, UUID owner, String name, String displayName) {
        this.id = id;
        this.owner = owner;
        this.name = name.toLowerCase();
        this.displayName = displayName;
        return this;
    }

    public Collection<UUID> getMembers() {
        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Collection<UUID> getInvited() {
        return invited;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public LocationData getHome() {
        return home;
    }

    public void setHome(LocationData home) {
        this.home = home;
    }

    public String getDisplayName() {
        if (displayName == null) {
            return "Loading";
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        String strippedName = ChatColor.stripColor(displayName).toLowerCase().trim();
        if (!strippedName.equals(name.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid faction displayname: " + strippedName + " - " + name.toLowerCase());
        }
        if (displayName.length() < 3 || displayName.length() > 32) {
            throw new IllegalArgumentException("Invalid faction name");
        }
        this.displayName = displayName;
    }

    public String getName() {
        if (name == null) {
            return "Loading";
        }
        return name.toLowerCase();
    }

    public void setName(String name) {
        FactionNamingUtil.checkName(name);
        this.displayName = name;
        this.name = name.toLowerCase();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        updateScore();
    }

    public Map<UUID, Relation> getRelations() {
        return relations;
    }

    public void setRelation(UUID factionId, Relation relation) {
        this.relations.put(factionId, relation);
    }

    public void setRelations(Collection<Relation> relationsByFactionId) {
        for (Relation relation : relationsByFactionId) {
            this.relations.put(relation.getTargetFactionId(), relation);
        }
    }

    public Relation getRelation(UUID otherFactionId) {
        return relations.get(otherFactionId);
    }

    public RelationType getRelationType(UUID otherFactionId) {
        Relation relation = relations.get(otherFactionId);

        if (relation != null) {
            return relation.getRelationType();
        }
        if (otherFactionId.equals(id)) {
            return RelationType.SAME_FACTION;
        }
        return RelationType.NEUTRAL;
    }

    public RelationType getRelationType(Faction otherFaction) {
        if (otherFaction == null) {
            return RelationType.NEUTRAL;
        }
        return getRelationType(otherFaction.getId());
    }

    public Map<String, Boolean> getChestPermissions() {
        return chestPermissions;
    }

    public void setChestPermission(String role, boolean permission) {
        this.chestPermissions.put(role, permission);
    }

    public Map<UUID, Rank> getRanks() {
        return ranks;
    }

    public Rank getRank(UUID playerId) {
        return ranks.getOrDefault(playerId, Rank.RECRUIT);
    }

    public void setRank(UUID member, Rank rank) {
        this.ranks.put(member, rank);
    }

    public void invitePlayer(UUID id) {
        this.invited.add(id);
    }

    public void uninvitePlayer(UUID id) {
        this.invited.remove(id);
    }

    public void addMember(UUID member) {
        this.members.add(member);
        // Make sure member is on faction
        FactionPlayer factionPlayer = MineClans.getInstance().getFactionPlayerManager().getOrLoad(member);
        if (factionPlayer.getFaction() != this) {
            factionPlayer.setFaction(this);
        }
        updateScore();
        updatePower();
    }

    public void removeMember(UUID member) {
        this.members.remove(member);
        updateScore();
        updatePower();
    }

    public void setMembers(Collection<UUID> members) {
        this.members = members;
    }

    public void setInvited(Collection<UUID> invited) {
        this.invited = invited;
    }

    public void disbandFaction() {
        this.members.clear();
        this.invited.clear();
        this.relations.clear();
        this.chestPermissions.clear();
        this.ranks.clear();
        this.balance = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getHomeString() {
        return LocationUtil.locationDataToString(home);
    }

    public Collection<UUID> getOnlineMembers() {
        Collection<UUID> onlineMembers = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                onlineMembers.add(memberId);
            }
        }
        return onlineMembers;
    }

    public void setRanks(Map<UUID, Rank> ranks) {
        this.ranks = ranks;
    }

    public Inventory getChest() {
        if (chestInventory == null) {
            chestInventory = Bukkit.createInventory(this, 27, "Faction Chest");
        }

        return chestInventory;
    }

    public void setChest(Inventory chestInventory) {
        if (this.chestInventory != null) {
            this.chestInventory.clear();
            ItemStack[] items = chestInventory.getContents();
            for (int i = 0; i < items.length; i++) {
                this.chestInventory.setItem(i, items[i]);
            }
        } else {
            this.chestInventory = chestInventory;
        }
    }

    public UUID getFocusedFaction() {
        return focusedFaction;
    }

    public void setFocusedFaction(UUID focusedFaction) {
        this.focusedFaction = focusedFaction;
    }

    public int getKills() {
        return kills;
    }

    public boolean addKill(UUID killedPlayerId) {
        // If player is already killed by this player, return false
        if (killedPlayers.contains(killedPlayerId)) {
            return false;
        }

        killedPlayers.add(killedPlayerId);
        setKills(kills + 1);

        return true;
    }

    public void setKills(int kills) {
        this.kills = kills;
        updateScore();
    }

    public int getEventsWon() {
        return eventsWon;
    }

    public void setEventsWon(int eventsWon) {
        this.eventsWon = eventsWon;
        updateScore();
    }

    public void addEventsWon() {
        setEventsWon(eventsWon + 1);
    }

    private double calculateScore() {
        ConfigWrapper config = MineClans.getInstance().getCfg();
        double killsWeight = config.getDouble("weights.kill");
        double moneyWeight = config.getDouble("weights.money");
        double memberCountWeight = config.getDouble("weights.member_count");
        double eventsWonWeight = config.getDouble("weights.events_won");

        double killsScore = kills * killsWeight;
        double moneyScore = balance * moneyWeight;
        double memberCountScore = members.size() * memberCountWeight;
        double eventsWonScore = eventsWon * eventsWonWeight;

        double score = killsScore + moneyScore + memberCountScore + eventsWonScore;
        return score;
    }

    @Override
    public Inventory getInventory() {
        return chestInventory;
    }

    public double getScore() {
        return this.score;
    }

    public void updateScore() {
        double newScore = calculateScore();
        if (this.score != newScore) {
            this.score = newScore;
        }
    }

    public void giveEffects(Player player) {
        Iterator<ActiveBuff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            ActiveBuff buff = iterator.next();
            if (buff.isActive()) {
                buff.giveEffectToPlayer(player);
            } else {
                iterator.remove();
            }
        }
    }

    public void removeEffects(Player player) {
        Iterator<ActiveBuff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            ActiveBuff buff = iterator.next();
            if (buff.isActive()) {
                buff.removeEffectFromPlayer(player);
            } else {
                iterator.remove();
            }
        }
    }

    public ActiveBuff addBuff(ActiveBuff buff) {
        activeBuffs.add(buff);
        return buff;
    }

    public ActiveBuff removeBuff(ActiveBuff buff) {
        activeBuffs.remove(buff);
        return buff;
    }

    public Collection<ActiveBuff> getBuffs() {
        return activeBuffs;
    }

    public boolean isOpen() {
        return open;
    }

    public String getCreationDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(creationDate);
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean setAnnouncement(String announcement) {
        // Check if the new discord link is different from the current one
        if (this.announcement == null ? announcement != null : !this.announcement.equals(announcement)) {
            this.announcement = announcement; // Update
            return true; // Indicate that the value has changed
        }
        return false; // Indicate that the value has not changed
    }

    public boolean setDiscord(String discord) {
        // Check if the new discord link is different from the current one
        if (this.discord == null ? discord != null : !this.discord.equals(discord)) {
            this.discord = discord; // Update
            return true; // Indicate that the value has changed
        }
        return false; // Indicate that the value has not changed
    }

    public String getOwnerName() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(getOwner());
        if (offlinePlayer == null) {
            return "null";
        }
        return offlinePlayer.getName();
    }

    public String getDiscord() {
        return discord;
    }

    public boolean isRenameCooldown() {
        // Check if enough time has passed since the last rename
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRename) < RENAME_COOLDOWN;
    }

    public void setRenameCooldown() {
        // Set the time for the last rename to the current time
        lastRename = System.currentTimeMillis();
    }

    public boolean hasOnlineMembers() {
        return getOnlineMembers().size() > 0;
    }

    public void sendMessage(String msg) {
        for (UUID uuid : getOnlineMembers()) {
            Player player1 = Bukkit.getPlayer(uuid);
            if (player1 != null) {
                player1.sendMessage(msg);
            }
        }
    }

    public boolean isInvited(Player player) {
        return getInvited().contains(player.getUniqueId());
    }

    public boolean isEditingChest() {
        return editingChest;
    }

    public void setReceivedSubDuringUpdate(boolean receivedSubDuringUpdate) {
        this.receivedSubDuringUpdate = receivedSubDuringUpdate;
    }

    public boolean isReceivedSubDuringUpdate() {
        return receivedSubDuringUpdate;
    }

    // Send message, title, melody
    public void sendMessageTitleMelody(String message, String title, String subtitle, int fadeIn, int stay, int fadeOut,
            Melody melody) {
        for (UUID uuid : getOnlineMembers()) {
            Player otherPlayer = Bukkit.getPlayer(uuid);
            if (otherPlayer != null) {
                otherPlayer.sendMessage(message);
                Titles.sendTitle(otherPlayer, title, subtitle, fadeIn, stay, fadeOut);
                MelodyUtil.playMelody(MineClans.getInstance(), otherPlayer, melody);
            }
        }
    }

    public void setEditingChest(boolean b) {
        editingChest = b;
    }

    public int getPower() {
        return power;
    }

    public int getClaimLimit() {
        return Math.min(40, power);
    }

    public void updatePower() {
        power = 0;
        for (UUID uuid : getMembers()) {
            FactionPlayer player = MineClans.getInstance().getAPI().getFactionPlayer(uuid);
            if (player != null) {
                power += player.getPower();
            }
        }
    }

    public int getMaxPower() {
        int maxPower = 0;
        for (UUID uuid : getMembers()) {
            FactionPlayer player = MineClans.getInstance().getAPI().getFactionPlayer(uuid);
            if (player != null) {
                maxPower += player.getMaxPower();
            }
        }
        return maxPower;
    }

    public boolean canBeRaided() {
        return getPower() < 0;
    }

    public int getClaimedLandCount() {
        return MineClans.getInstance().getClaimedChunks().getClaimedChunkCount(id);
    }

    public boolean isMember(UUID playerId) {
        return getMembers().contains(playerId);
    }

    public boolean isFocusedFaction(UUID id2) {
        return getFocusedFaction() != null && getFocusedFaction().equals(id2);
    }

    public boolean hasRallyCooldown() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRally >= RALLY_COOLDOWN) {
            return false;
        }
        return true;
    }

    public void setRally(Location location) {
        this.rally = location;
        this.lastRally = System.currentTimeMillis();
        for (UUID uuid : getOnlineMembers()) {
            Player otherPlayer = Bukkit.getPlayer(uuid);
            if (otherPlayer != null && otherPlayer.isOnline()) {
                MineClans.getInstance().getProtocolLibHook().showFakeBeacon(otherPlayer, location);
            }
        }
    }

    public Location getRallyPoint() {
        return rally;
    }
}
