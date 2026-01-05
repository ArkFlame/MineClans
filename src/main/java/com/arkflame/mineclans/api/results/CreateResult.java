package com.arkflame.mineclans.api.results;

import com.arkflame.mineclans.models.Faction;

public class CreateResult {
    private Faction faction;
    private CreateResultState state;

    // Constructor
    public CreateResult(CreateResultState state, Faction faction) {
        this.state = state;
        this.faction = faction;
    }

    // Getter for Faction
    public Faction getFaction() {
        return faction;
    }

    // Getter for CreateResultState
    public CreateResultState getState() {
        return state;
    }

    public enum CreateResultState {
        SUCCESS,
        NULL_NAME,
        FACTION_EXISTS,
        ALREADY_HAVE_FACTION, ERROR, INVALID_NAME,
    }
}