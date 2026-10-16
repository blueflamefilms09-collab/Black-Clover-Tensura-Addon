package com.newuniverse.nusmp.arena;

/**
 * Why: Lucius may exist only in his arena (1.46.0, config {@code luciusArenaOnly}), and the places a mob can come from are many: a
 * spawn egg, {@code /summon}, the operator command, a structure, a spawner, another mod, a portal that carries it off. Every one of them
 * ends in the same question, so the answer is one pure table here (no Minecraft types, unit tested) that the event handlers of
 * {@link ArenaSpawnEvents} ask with four facts. The rules, in order:
 * <ol>
 *   <li>Not a Lucius-family entity (Lucius himself or one of his clones): allowed, this gate is not about it.</li>
 *   <li>{@code luciusArenaOnly} off: allowed (the old free spawning is back).</li>
 *   <li>Loaded from disk: allowed. A Lucius that already stands in a world from an older version is not deleted by an update, and the
 *       arena's own boss comes back from disk after a restart.</li>
 *   <li>Inside the arena dimension: allowed (an operator in creative may {@code /summon} him there).</li>
 *   <li>Carries the persistent tag {@value #TAG} (set by the arena director on the boss it spawns): allowed.</li>
 *   <li>Anything else: denied.</li>
 * </ol>
 * The same table, with 'inArena' meaning "the destination is the arena", answers a dimension change or a teleport of him
 * ({@link #mayLeave}): he can go nowhere that is not the arena, tag or no tag.
 */
public final class SpawnGate {
    private SpawnGate() {}

    /** The persistent-data tag the arena director puts on the boss it spawns. */
    public static final String TAG = "nusmp_arena_ok";

    public enum Verdict { ALLOW, DENY }

    public static Verdict decide(boolean luciusFamily, boolean arenaOnly, boolean fromDisk, boolean inArena, boolean tagged) {
        if (!luciusFamily || !arenaOnly || fromDisk || inArena || tagged) return Verdict.ALLOW;
        return Verdict.DENY;
    }

    /** May a Lucius-family entity leave for a place (a dimension change, a teleport)? Only if the switch is off, or the place is the arena. */
    public static boolean mayLeave(boolean luciusFamily, boolean arenaOnly, boolean destinationIsArena) {
        return !luciusFamily || !arenaOnly || destinationIsArena;
    }

    /** Is a natural or chunk-generation spawn of anything refused in the arena? Always: it is a closed room with one guest. */
    public static boolean refusesNatural(boolean inArena, boolean naturalSpawn) { return inArena && naturalSpawn; }
}
