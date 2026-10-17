package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcRoster;

/**
 * Why: the lead wires a builder's characters into {@code ArcCharacters.bootstrap()} with one line, and a boss needs more than a roster entry: its entity type is
 * registered by a static field of its class ({@code ArcEntities.boss(...)}), so the class has to be loaded before the registry events fire. This is that one line
 * for the DanMachi saga: it registers the four monsters through their classes (which loads them) in the order of the story, then the six friends. Calling it twice
 * is harmless (the same records are registered again).
 */
public final class DanMachiCast {
    private DanMachiCast() {}

    /** The four monsters (their classes are loaded, so their entity types exist) and then the six friends. */
    public static void registerAll() {
        ArcRoster.register(MinotaurBoss.CHARACTER);
        ArcRoster.register(BlackGoliathBoss.CHARACTER);
        ArcRoster.register(AmphisbaenaBoss.CHARACTER);
        ArcRoster.register(UdaeusBoss.CHARACTER);
        DanMachiNpcs.registerAll();
    }
}
