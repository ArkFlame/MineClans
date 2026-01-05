package com.arkflame.mineclans.api.results;

import com.arkflame.mineclans.models.Faction;

public class RenameResult {
    private Faction faction;
    private RenameResultState state;

    public RenameResult(Faction faction, RenameResultState state) {
        this.faction = faction;
        this.state = state;
    }

    public Faction getFaction() {
        return faction;
    }

    public RenameResultState getState() {
        return state;
    }

    public enum RenameResultState {
        SUCCESS,
        NOT_IN_FACTION,
        ALREADY_EXISTS, 
        NULL_NAME, 
        ERROR, 
        NO_PERMISSION,
        COOLDOWN,
        INVALID_NAME
    }
}
