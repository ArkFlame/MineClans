package com.arkflame.mineclans.models;

import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.api.results.ToggleChatResult;
import com.arkflame.mineclans.api.results.ToggleChatResult.ToggleChatState;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.menus.EnteringType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class FactionPlayer {
    private UUID playerId;
    private UUID factionId;
    private String name;
    private Date joinDate;
    private Date lastActive;
    private int kills = 0;
    private int deaths = 0;
    private ToggleChatResult.ToggleChatState chat = ToggleChatState.DISABLED;
    private Collection<UUID> killedPlayers = new HashSet<>();
    private EnteringType enteringType = EnteringType.DEPOSIT;
    private long enteringTime = 0;
    private long lastHomeRequest = 0;
    private double power = 1;
    private int maxPower = 10;

    // Rank Benefits
    // Enabled by commands, effectively applied or not if condition is met
    private boolean flying = false;
    private boolean godMode = false;
    private boolean canReceiveDamage = true;

    private boolean isMapViewer = true;

    public FactionPlayer(UUID playerId) {
        this.playerId = playerId;
        this.name = null;
        this.factionId = null;
        this.joinDate = null;
        this.lastActive = null;
    }

    public FactionPlayer(String name) {
        this.playerId = null;
        this.name = name;
        this.factionId = null;
        this.joinDate = null;
        this.lastActive = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setId(UUID playerId) {
        this.playerId = playerId;
    }

    public Faction getFaction() {
        if (factionId == null) {
            return null;
        }
        Faction faction = MineClans.getInstance().getFactionManager().getFaction(factionId);
        // Faction stopped existing
        if (faction == null) {
            factionId = null;
            return null;
        }
        // No longer member
        if (!faction.isMember(playerId)) {
            setFaction(null);
            return null;
        }
        return faction;
    }

    public void setFaction(Faction faction) {
        if (faction == null) {
            this.factionId = null;
        } else {
            this.factionId = faction.getId();
            // Make sure player is a member
            if (!faction.isMember(playerId)) {
                faction.addMember(playerId);
            }
        }
    }

    private UUID getFactionId() {
        // Faction stopped existing
        if (factionId != null && MineClans.getInstance().getFactionManager().getFaction(factionId) == null) {
            return factionId = null;
        }
        return factionId;
    }

    public void setFactionId(UUID factionId) {
        this.factionId = factionId;
    }

    public void setFactionId(String factionId) {
        if (factionId == null) {
            this.factionId = null;
            return;
        }
        setFactionId(UUID.fromString(factionId));
    }

    public Rank getRank() {
        Faction faction = getFaction();
        if (faction == null) {
            return Rank.RECRUIT;
        }
        return faction.getRank(playerId);
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void updateLastActive() {
        this.lastActive = new Date();
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public boolean addKill(UUID killedPlayerId) {
        // If player is already killed by this player, return false
        if (killedPlayers.contains(killedPlayerId)) {
            return false;
        }

        killedPlayers.add(killedPlayerId);
        kills++;

        return true;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public void toggleChat() {
        this.chat = getChatMode().getNext();
    }

    public ToggleChatResult.ToggleChatState getChatMode() {
        return getFactionId() != null
                ? (chat == ToggleChatResult.ToggleChatState.NOT_IN_FACTION ? ToggleChatResult.ToggleChatState.DISABLED
                        : chat)
                : ToggleChatResult.ToggleChatState.NOT_IN_FACTION;
    }

    public boolean isOnline() {
        Player player = getPlayer();
        if (player == null) {
            return false;
        }
        return player.isOnline();
    }

    public void sendMessage(String msg) {
        Player player = getPlayer();
        if (player != null) {
            player.sendMessage(msg);
        }
    }

    public void setEnteringAmount(EnteringType type) {
        this.enteringType = type;
        this.enteringTime = System.currentTimeMillis();
    }

    public EnteringType getEnteringTypeIfValid() {
        long currentTime = System.currentTimeMillis();

        // Check if less than 30 seconds (30,000 milliseconds) have passed
        if (currentTime - enteringTime <= 30_000) {
            return enteringType;
        } else {
            return null; // Timeout, returning null
        }
    }

    public boolean shouldTeleportHome() {
        return System.currentTimeMillis() - lastHomeRequest < 2000;
    }

    public void requestHome() {
        this.lastHomeRequest = System.currentTimeMillis();
    }

    public double getPower() {
        return power;
    }

    public boolean setPower(double power) {
        double oldPower = this.power;
        this.power = power;
        if (this.power > this.maxPower) {
            this.power = this.maxPower;
        }
        if (this.power < -10) {
            this.power = -10D;
        }
        if (oldPower == this.power) {
            return false;
        }
        return true;
    }

    public double getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(int maxPower) {
        this.maxPower = maxPower;
    }

    public boolean updateMaxPower() {
        Player player = getPlayer();
        if (player == null) {
            return false;
        }
        Configuration config = MineClans.getInstance().getConfig();
        List<Integer> perms = config.getIntegerList("max_power_permissions");
        if (perms != null) {
            Collections.sort(perms, Collections.reverseOrder());
            for (int value : perms) {
                String permission = "mineclans.power." + value;
                if (player.hasPermission(permission)) {
                    if (this.maxPower != value) {
                        this.maxPower = value;
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public void setCanReceiveDamage(boolean b) {
        canReceiveDamage = b;
    }

    public boolean canReceiveDamage() {
        return canReceiveDamage;
    }

    public boolean isMapViewer() {
        return isMapViewer;
    }

    public void setMapViewer(boolean isMapViewer) {
        this.isMapViewer = isMapViewer;
    }
}
