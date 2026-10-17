package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.UUID;

/**
 * Why: the runtime record of one wandering boss, kept apart from the entity so the entity can be any mob at all (our own Floor Warden, a vanilla
 * brute, a Tensura beast). It holds what the boss needs to think and act: the id of the entity and of its sector, the {@link WandererMind}
 * with its state, the last order (the patrol goal reads it to keep the walk alive), when it last saw a
 * player in its sector (for the despawn), the last time it was struck (to notice a new blow), and the last place it was sent to walk to
 * (so a walk is renewed when the goal changes or the path went stale, not on every pass). All fields are touched on the server thread only.
 */
final class Wanderer {
    final UUID id;
    final int sector;
    final long born;
    final String name;
    final WandererMind mind = new WandererMind();
    WandererMind.Order order;
    long lastThink = Long.MIN_VALUE;
    long lastPlayerAt;
    int lastHurtStamp = -1;
    int inside;
    boolean goalInstalled;
    double walkX = Double.NaN, walkZ = Double.NaN;
    long walkAt = Long.MIN_VALUE;
    long lastBlock;

    Wanderer(UUID id, int sector, long born, String name) {
        this.id = id;
        this.sector = sector;
        this.born = born;
        this.name = name;
        this.lastPlayerAt = born;
    }
}
