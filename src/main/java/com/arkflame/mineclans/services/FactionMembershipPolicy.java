package com.arkflame.mineclans.services;

import java.util.UUID;

public class FactionMembershipPolicy {

    public enum Operation {
        LEAVE,
        KICK,
        TRANSFER,
        DISBAND,
        ADMIN_DELETE
    }

    public enum PolicyResult {
        ALLOWED,
        DENIED_OWNER_CANNOT_LEAVE,
        DENIED_OWNER_CANNOT_BE_KICKED,
        DENIED_LAST_MEMBER,
        DENIED_TRANSFER_TO_SELF,
        DENIED_TRANSFER_TARGET_NOT_MEMBER,
        DENIED_CORRUPTED_FACTION
    }

    public PolicyResult evaluate(UUID actorId, UUID ownerId, int memberCount, Operation operation) {
        switch (operation) {
            case LEAVE:
                if (actorId.equals(ownerId)) {
                    return PolicyResult.DENIED_OWNER_CANNOT_LEAVE;
                }
                if (memberCount <= 1) {
                    return PolicyResult.DENIED_LAST_MEMBER;
                }
                return PolicyResult.ALLOWED;

            case KICK:
                if (ownerId.equals(ownerId)) {
                    return PolicyResult.DENIED_OWNER_CANNOT_BE_KICKED;
                }
                if (memberCount <= 1) {
                    return PolicyResult.DENIED_LAST_MEMBER;
                }
                return PolicyResult.ALLOWED;

            case TRANSFER:
                if (actorId.equals(ownerId)) {
                    return PolicyResult.DENIED_TRANSFER_TO_SELF;
                }
                return PolicyResult.ALLOWED;

            case DISBAND:
                if (memberCount == 0) {
                    return PolicyResult.DENIED_CORRUPTED_FACTION;
                }
                return PolicyResult.ALLOWED;

            case ADMIN_DELETE:
                return PolicyResult.ALLOWED;

            default:
                return PolicyResult.DENIED_CORRUPTED_FACTION;
        }
    }

    public boolean isAllowed(UUID actorId, UUID ownerId, int memberCount, Operation operation) {
        return evaluate(actorId, ownerId, memberCount, operation) == PolicyResult.ALLOWED;
    }
}
