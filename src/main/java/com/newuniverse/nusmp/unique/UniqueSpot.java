package com.newuniverse.nusmp.unique;

import java.util.UUID;

/**
 * Why: one sighting of a valid copy, as plain data (no Minecraft types), so the state machine can be tested without a game. {@code holder} is the carrying player for HELD, the last known holder otherwise (may be null);
 * {@code entity} is the item entity of a DROPPED copy; {@code block} is the packed block position of a CONTAINED copy whose container is a block entity, else {@link UniqueRecord#NO_BLOCK}.
 */
public record UniqueSpot(UniqueState state, UUID holder, String dim, double x, double y, double z, UUID entity, long block) {
    public static UniqueSpot held(UUID holder, String dim, double x, double y, double z) { return new UniqueSpot(UniqueState.HELD, holder, dim, x, y, z, null, UniqueRecord.NO_BLOCK); }

    public static UniqueSpot dropped(UUID lastHolder, String dim, double x, double y, double z, UUID entity) { return new UniqueSpot(UniqueState.DROPPED, lastHolder, dim, x, y, z, entity, UniqueRecord.NO_BLOCK); }

    public static UniqueSpot contained(UUID lastHolder, String dim, double x, double y, double z, long block) { return new UniqueSpot(UniqueState.CONTAINED, lastHolder, dim, x, y, z, null, block); }
}
