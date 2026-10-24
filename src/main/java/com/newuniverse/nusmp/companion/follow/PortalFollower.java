package com.newuniverse.nusmp.companion.follow;

import java.util.UUID;

/**
 * Why: the contract of an entity that walks with a player and must come along when that player changes dimension (a portal, a command, another mod's crossing). {@link #followedPlayer()} is the UUID of the
 * player she is walking with, or null while she walks with nobody. {@code CompanionTravel} carries every such entity within 32 blocks of the player (at most {@code companions.maxFollowersPerPlayer}) and puts
 * it down beside them in the new dimension. Implement it on a {@code Mob}.
 */
public interface PortalFollower {
    /** The player this entity is walking with, or null. */
    UUID followedPlayer();
}
