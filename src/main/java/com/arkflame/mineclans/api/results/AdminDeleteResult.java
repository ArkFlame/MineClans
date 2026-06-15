package com.arkflame.mineclans.api.results;

public class AdminDeleteResult {
    private final boolean success;
    private final String errorMessage;
    private final String factionName;

    private AdminDeleteResult(boolean success, String errorMessage, String factionName) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.factionName = factionName;
    }

    public static AdminDeleteResult success(String factionName) {
        return new AdminDeleteResult(true, null, factionName);
    }

    public static AdminDeleteResult error(String errorMessage) {
        return new AdminDeleteResult(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFactionName() {
        return factionName;
    }
}
