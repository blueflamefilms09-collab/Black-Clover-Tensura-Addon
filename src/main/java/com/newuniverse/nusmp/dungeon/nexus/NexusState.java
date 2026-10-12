package com.newuniverse.nusmp.dungeon.nexus;

/**
 * Where a core cell is in its life. UNBUILT: nothing recorded (or only a forced zone). PLAN: a job has chosen the zone and nothing is
 * placed yet. BUILDING: part of it stands. DONE: built and populated. COLLAPSED: robbed and brought down (see the collapse sequence); it
 * is not rebuilt by itself, only by the admin command.
 */
public enum NexusState {
    UNBUILT(0), PLAN(1), BUILDING(2), DONE(3), COLLAPSED(4);

    /** The number saved with the world; never reorder or reuse. */
    public final int id;

    NexusState(int id) { this.id = id; }

    public static NexusState of(int id) {
        for (NexusState s : values()) if (s.id == id) return s;
        return UNBUILT;
    }

    /** Has a build ever been started here? */
    public boolean started() { return this != UNBUILT; }

    /** Does the structure stand (or did it, until the collapse)? */
    public boolean built() { return this == DONE || this == COLLAPSED; }
}
