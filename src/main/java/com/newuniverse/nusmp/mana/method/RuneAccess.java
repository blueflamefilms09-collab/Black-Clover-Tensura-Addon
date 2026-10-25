package com.newuniverse.nusmp.mana.method;

/**
 * 3.8.0: WHO may use the Mana Method pages, as pure rules (no game classes, so the offline tests and both sides can load it). Server setting {@code manaMethod.access}:
 *
 * <ul>
 *   <li>{@link Mode#PASSIVE} (the default): no switching on. The nine Mana Method pages are usable once the grimoire's mastery fraction reaches {@code manaMethod.passiveMastery}
 *       (default {@value #DEFAULT_MASTERY}, about Stage 3: the canon says only the Third Stage and above can use the Mana Method in combat).</li>
 *   <li>{@link Mode#TIMED}: the page "Rune State" ({@code mana_method_state}) switches the rune state ON for {@code timedSeconds} (default 480 = 8 minutes), then a cooldown of
 *       {@code timedCooldownSeconds} (default 600 = 10 minutes) runs before it can be cast again. While ON the nine pages are usable (mastery does not matter). The cooldown starts
 *       when the ON state ENDS; an ON state that ends early (logout, death, dimension change, server stop) starts its cooldown at that moment.</li>
 *   <li>{@link Mode#MANA_ZONE_GATE}: exactly the 3.7.0 behaviour, the late-game gate of the Mana Zone.</li>
 * </ul>
 *
 * The Rune State page is usable ONLY in TIMED (it would be clutter in the other two modes). The Mana Zone page, Omni Volley and Pressure Space are not Mana Method pages and keep the
 * Mana Zone gate whatever this says.
 */
public final class RuneAccess {
    private RuneAccess() {}

    /** The enum is stored by NAME in the config file; entries are only ever appended. */
    public enum Mode { PASSIVE, TIMED, MANA_ZONE_GATE }

    public static final double DEFAULT_MASTERY = 0.7, MASTERY_MIN = 0.0, MASTERY_MAX = 1.0;
    public static final int DEFAULT_SECONDS = 480, MIN_SECONDS = 30, MAX_SECONDS = 3600;
    public static final int DEFAULT_COOLDOWN = 600, MIN_COOLDOWN = 0, MAX_COOLDOWN = 7200;
    /** The ON state warns this many ticks before it ends (30 s). */
    public static final int WARN_TICKS = 600;
    /** The server looks at the ON states once every this many ticks (one second). */
    public static final int SWEEP_TICKS = 20;

    public static double mastery(double v) { return Double.isNaN(v) ? DEFAULT_MASTERY : Math.max(MASTERY_MIN, Math.min(MASTERY_MAX, v)); }

    public static int seconds(int v) { return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, v)); }

    public static int cooldownSeconds(int v) { return Math.max(MIN_COOLDOWN, Math.min(MAX_COOLDOWN, v)); }

    /**
     * The whole access decision for one page.
     *
     * @param statePage whether the page is the Rune State page
     * @param mastery   the grimoire's mastery fraction (0..1) of the grimoire the page belongs to
     * @param need      {@code manaMethod.passiveMastery}
     * @param stateOn   whether the caster's rune state is ON (TIMED)
     * @param gateOpen  whether the late-game gate of the Mana Zone is open for the caster (MANA_ZONE_GATE)
     */
    public static boolean allowed(Mode mode, boolean statePage, double mastery, double need, boolean stateOn, boolean gateOpen) {
        if (mode == null) mode = Mode.PASSIVE;
        if (statePage) return mode == Mode.TIMED;
        return switch (mode) {
            case PASSIVE -> !Double.isNaN(mastery) && mastery >= mastery(need);
            case TIMED -> stateOn;
            case MANA_ZONE_GATE -> gateOpen;
        };
    }

    /** True when the answer needs the late-game gate (so the caller only evaluates the gate in that mode). */
    public static boolean needsGate(Mode mode) { return mode == Mode.MANA_ZONE_GATE; }

    // ---------------------------------------------------------------- the TIMED timeline (game-time stamps, so a relog cannot reset anything)
    /** The two stamps kept per player: the tick the ON state ends and the tick the cooldown ends (0 = never set). */
    public record Stamps(long onUntil, long cooldownUntil) {
        public static final Stamps NONE = new Stamps(0L, 0L);
    }

    public enum Start { STARTED, ALREADY_ON, COOLING }

    /** The outcome of a cast of the Rune State page: what happened and the new stamps. */
    public record Cast(Start result, Stamps stamps) {}

    public static boolean on(Stamps s, long now) { return now < s.onUntil(); }

    /** Ticks until the cooldown ends (0 when it is over). While the state is ON the cooldown is still ahead, so the page also refuses. */
    public static long cooldownLeft(Stamps s, long now) { return Math.max(0L, s.cooldownUntil() - now); }

    /** Ticks of the ON state left (0 when it is over). */
    public static long onLeft(Stamps s, long now) { return Math.max(0L, s.onUntil() - now); }

    /** A cast of the page at {@code now}: refused while ON or cooling, else ON for {@code durationSeconds}, the cooldown starting when it ends. */
    public static Cast cast(Stamps s, long now, int durationSeconds, int cooldownSeconds) {
        if (on(s, now)) return new Cast(Start.ALREADY_ON, s);
        if (now < s.cooldownUntil()) return new Cast(Start.COOLING, s);
        long end = now + 20L * seconds(durationSeconds);
        return new Cast(Start.STARTED, new Stamps(end, end + 20L * cooldownSeconds(cooldownSeconds)));
    }

    /** The state ends NOW (logout, death, dimension change, server stop, a stale ON state met at login): the cooldown starts now. Nothing changes when it was not ON. */
    public static Stamps endNow(Stamps s, long now, int cooldownSeconds) {
        if (!on(s, now)) return s;
        return new Stamps(now, now + 20L * cooldownSeconds(cooldownSeconds));
    }

    /** Whether the 30 second warning is due: the state is ON, not warned yet and no more than {@link #WARN_TICKS} are left. */
    public static boolean warnDue(Stamps s, long now, boolean warned) { return !warned && on(s, now) && s.onUntil() - now <= WARN_TICKS; }

    /** A tick count as "m:ss" for the messages (negative counts as 0). */
    public static String clock(long ticks) {
        long sec = (Math.max(0L, ticks) + 19L) / 20L;
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }
}
