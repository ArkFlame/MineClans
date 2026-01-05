package com.arkflame.mineclans.api.results;

public class RallyResult {
    private final RallyResultType resultType;

    public RallyResult(RallyResultType resultType) {
        this.resultType = resultType;
    }

    public RallyResultType getResultType() {
        return resultType;
    }

    public enum RallyResultType {
        SUCCESS,
        IN_COOLDOWN,
        NO_RANK,
        NO_FACTION,
    }
}
