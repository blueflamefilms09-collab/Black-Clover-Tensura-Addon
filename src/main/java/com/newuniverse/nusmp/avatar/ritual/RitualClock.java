package com.newuniverse.nusmp.avatar.ritual;

/**
 * Why: the Soul Forge ritual is a timeline (the circle draws, the screen opens, the creator designs, the beam strikes, the Avatar steps out) with a
 * strict rule about who may end it and when (timeouts, cancels). The rules are pure counting, so they live here, away from the game, and a unit test can
 * walk a whole ritual tick by tick; {@code SoulForgeRitual} only asks this clock what to do next and does it. One clock per player's ritual, server thread.
 *
 * <p>Phases: {@code CHARGING} (the player is locked, the circle draws; {@code chargeTicks} long) -> {@code OPEN} (the screen is up; at most
 * {@code maxOpenTicks}) -> {@code SPECTACLE} (the creator pressed Finalize and the server accepted it; the {@link Birth} beats play) -> {@code DONE}.
 * {@code CANCELLED} can be reached from any phase and is final. Each {@link #tick} returns at most one {@link Event}.
 */
public final class RitualClock {
    public enum Phase { CHARGING, OPEN, SPECTACLE, DONE, CANCELLED }

    public enum Event { NONE, OPEN_MENU, TIMEOUT, TELL, STRIKE, SPAWN, NOVA, FINISH }

    /**
     * The beats of the birth, in ticks after the screen closed: the telegraph of the beam, the strike, the moment the Avatar is created, its first
     * special ability, and the end (the lock is lifted). Strictly increasing.
     */
    public record Birth(int tell, int strike, int spawn, int nova, int end) {
        public Birth {
            if (!(0 <= tell && tell < strike && strike < spawn && spawn < nova && nova < end)) throw new IllegalArgumentException("the beats of a birth must be strictly increasing");
        }

        /** A birth that lasts {@code total} ticks (clamped to 24..240): the strike at 36%, the spawn at 48%, the first ability at 56%. */
        public static Birth of(int total) {
            int t = Math.max(24, Math.min(240, total));
            int strike = Math.max(2, Math.round(t * 0.36f)), spawn = Math.max(strike + 2, Math.round(t * 0.48f)), nova = Math.max(spawn + 2, Math.round(t * 0.56f));
            return new Birth(0, strike, spawn, nova, Math.max(nova + 4, t));
        }
    }

    private final int chargeTicks, maxOpenTicks;
    private final Birth birth;
    private Phase phase = Phase.CHARGING;
    private int age;

    public RitualClock(int chargeTicks, int maxOpenTicks, Birth birth) {
        this.chargeTicks = Math.max(1, chargeTicks);
        this.maxOpenTicks = Math.max(1, maxOpenTicks);
        this.birth = birth;
    }

    public Phase phase() { return phase; }

    /** Ticks spent in the current phase. */
    public int age() { return age; }

    public boolean finished() { return phase == Phase.DONE || phase == Phase.CANCELLED; }

    /** Advances one server tick. */
    public Event tick() {
        switch (phase) {
            case CHARGING -> {
                age++;
                if (age >= chargeTicks) { phase = Phase.OPEN; age = 0; return Event.OPEN_MENU; }
                return Event.NONE;
            }
            case OPEN -> {
                age++;
                if (age >= maxOpenTicks) { phase = Phase.CANCELLED; return Event.TIMEOUT; }
                return Event.NONE;
            }
            case SPECTACLE -> {
                int t = age++;
                Event e = t == birth.tell() ? Event.TELL : t == birth.strike() ? Event.STRIKE : t == birth.spawn() ? Event.SPAWN : t == birth.nova() ? Event.NOVA : Event.NONE;
                if (t >= birth.end()) { phase = Phase.DONE; return Event.FINISH; }
                return e;
            }
            default -> { return Event.NONE; }
        }
    }

    /** The creator pressed Finalize and the server accepted it. Only valid while the screen is open; false otherwise. */
    public boolean accept() {
        if (phase != Phase.OPEN) return false;
        phase = Phase.SPECTACLE;
        age = 0;
        return true;
    }

    /** Ends the ritual early (closed screen, damage, death, logout ...). False when it had already finished. */
    public boolean cancel() {
        if (finished()) return false;
        phase = Phase.CANCELLED;
        return true;
    }

    public Birth birth() { return birth; }
}
