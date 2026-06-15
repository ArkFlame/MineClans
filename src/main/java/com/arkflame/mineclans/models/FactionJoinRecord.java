package com.arkflame.mineclans.models;

import java.util.Date;
import java.util.UUID;

public class FactionJoinRecord {
    private final UUID eventId;
    private final UUID factionId;
    private final UUID playerId;
    private final String playerName;
    private final String serverName;
    private final Date joinedAt;

    public FactionJoinRecord(UUID eventId, UUID factionId, UUID playerId, String playerName, String serverName, Date joinedAt) {
        this.eventId = eventId;
        this.factionId = factionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.serverName = serverName;
        this.joinedAt = joinedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getFactionId() {
        return factionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerName() {
        return serverName;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }
}
