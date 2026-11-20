package com.newuniverse.nusmp.relics.djinn;

import java.util.Map;
import java.util.UUID;

/**
 * The numbers of the skeletal dragon's grip (pure, so BelialGripRulesTest can check every bound). Owner notes: in the Full Djinn Equip Belial is surrounded by a massive skeletal
 * dragon aura that can physically restrain opponents. Played as a short, bounded hold: in a Full Equip the dragon seizes the foe the bearer just struck and holds it where it stands for at
 * most {@link #HOLD_TICKS} (2 s); one victim per caster at a time, a cooldown of {@link #COOLDOWN_TICKS} (12 s) per caster, {@link #MAX_GLOBAL} holds on the server, a victim that is
 * let go is immune to a new grip for {@link #IMMUNE_TICKS}; bosses, creative and spectator players, allies and anything a guard vetoes are never gripped.
 */
public final class BelialGripRules {
    private BelialGripRules() {}

    public static final int HOLD_TICKS = 40;
    /** The cooldown of a caster (never below 10 s, the owner's floor). */
    public static final int COOLDOWN_TICKS = 240, MIN_COOLDOWN_TICKS = 200;
    public static final int IMMUNE_TICKS = 100;
    public static final int MAX_GLOBAL = 16;
    /** A held target is put back at its spot this often (ticks), and only if it was moved further than this many blocks (squared below). */
    public static final int HOLD_EVERY = 4;
    public static final double HOLD_SLACK_SQ = 1.0;
    /** The caster and immune tables are bounded the same way as the sever windows. */
    public static final int MAX_TABLE = 256;

    /** How long the hold lasts: always 1 to {@link #HOLD_TICKS}. */
    public static int holdTicks(int wanted) { return Math.max(1, Math.min(HOLD_TICKS, wanted)); }

    /** Why a target is not gripped ({@link #OK}: it is). */
    public enum Refusal { OK, DISABLED, BOSS, CREATIVE, VETO, EXILED, IMMUNE, ALREADY, NOT_READY, CASTER_BUSY, GLOBAL_CAP }

    /** The one rule of who is gripped and when. */
    public static Refusal canGrip(boolean enabled, BelialSeverRules.Kind kind, boolean creativeOrSpectator, boolean vetoed, boolean exiled, boolean immune, boolean already,
                                  boolean onCooldown, boolean casterBusy, int globalCount) {
        if (!enabled) return Refusal.DISABLED;
        if (kind == BelialSeverRules.Kind.BOSS) return Refusal.BOSS;
        if (creativeOrSpectator) return Refusal.CREATIVE;
        if (vetoed) return Refusal.VETO;
        if (exiled) return Refusal.EXILED;
        if (already) return Refusal.ALREADY;
        if (immune) return Refusal.IMMUNE;
        if (onCooldown) return Refusal.NOT_READY;
        if (casterBusy) return Refusal.CASTER_BUSY;
        if (globalCount >= MAX_GLOBAL) return Refusal.GLOBAL_CAP;
        return Refusal.OK;
    }

    /** Is a time-keyed entry (game time it ends) still in force at {@code now}? A time further ahead than any wait can be is a clock that went back and counts as over. */
    public static boolean running(long until, long now, int longestWait) { return until > now && until - now <= longestWait; }

    /** Drops the ended entries of a UUID-to-time table and clears it if it is still over {@link #MAX_TABLE}. */
    public static void sweep(Map<UUID, Long> table, long now) {
        table.values().removeIf(until -> !running(until, now, COOLDOWN_TICKS + IMMUNE_TICKS));
        if (table.size() > MAX_TABLE) table.clear();
    }
}
