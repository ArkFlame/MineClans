package com.arkflame.mineclans.api.results;

import com.arkflame.mineclans.models.Faction;

public class RenameDisplayResult {
    private Faction faction;
    private RenameDisplayResultState state;

    public RenameDisplayResult(Faction faction, RenameDisplayResultState state) {
        this.faction = faction;
        this.state = state;
    }

    public Faction getFaction() {
        return faction;
    }

    public RenameDisplayResultState getState() {
        return state;
    }

    public enum RenameDisplayResultState {
        SUCCESS,
        NOT_IN_FACTION,
        DIFFERENT_NAME, 
        NULL_NAME, ERROR, INVALID_NAME
    }
}
