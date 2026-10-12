package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * The rules of the Tempest Mirage that are not the swarm's: how rich its ground is kept, and the life of a fake house (docs/living_dungeon.md
 * section 5, zone 3): it stands like the others until someone comes close; then it shimmers for a moment (the warning, and the way out: step
 * away); if someone is still close when the moment is over it dissolves into an ambush; and once nobody is about it forms again. Pure: the
 * glue asks {@link #decide} what to do with a fake and does it.
 */
public final class TempestRules {
    private TempestRules() {}

    /** The biomass and magicule the village's chunks are kept at while players are in it (the Anti-Bird Compass reads the biomass). */
    public static final float BIOMASS = 360f, MAGICULE = 600f;
    /** How close to a fake someone must come to arm it, how close they must still be when it gives way, how far everyone must be for it to form again (blocks). */
    public static final double TRIGGER = 3.0, DISSOLVE_RANGE = 8.0, REBUILD_RANGE = 24.0;
    /** How long a fake shimmers before it gives way, the least it stays down, the pause between two traps of one village, how many may be down at once (ticks, ticks, ticks, count). */
    public static final int TELEGRAPH = 30, DOWN_FOR = 600, COOLDOWN = 200, MAX_DOWN = 2;

    public enum Step { NONE, ARM, DISARM, DISSOLVE, REBUILD }

    /**
     * What is known about one fake: is it standing, is it armed (and since when), the time, is someone within the trigger distance, is someone
     * within the dissolve range, is everyone beyond the rebuild range, is a creature inside the space it would form in, when it went down
     * (Long.MIN_VALUE: not known, it is taken to have been down long), when the village last sprang a trap, how many fakes of the village are down.
     */
    public record Situation(boolean standing, boolean armed, long armedAt, long now, boolean triggered, boolean someoneNear, boolean everyoneAway,
                            boolean occupied, long downSince, long lastSprung, int down) {}

    public static Step decide(Situation s) {
        if (s.standing()) {
            if (s.armed()) {
                if (!s.someoneNear()) return Step.DISARM;                                      // they stepped away in time
                return s.now() - s.armedAt() >= TELEGRAPH ? Step.DISSOLVE : Step.NONE;
            }
            boolean rested = s.lastSprung() == Long.MIN_VALUE || s.now() - s.lastSprung() >= COOLDOWN;
            return s.triggered() && rested && s.down() < MAX_DOWN ? Step.ARM : Step.NONE;
        }
        boolean longAgo = s.downSince() == Long.MIN_VALUE || s.now() - s.downSince() >= DOWN_FOR;
        return s.everyoneAway() && !s.occupied() && longAgo ? Step.REBUILD : Step.NONE;
    }
}
