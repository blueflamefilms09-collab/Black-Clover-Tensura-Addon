package com.newuniverse.nusmp.anim;

/**
 * The "active casting" state machine of a held cast key (pure Java: no Minecraft types, so it is unit tested).
 *
 * <p>Why it exists. Holding the cast key of a grimoire page used to send a brand new magic circle every 4 ticks (a 6 tick circle
 * that pops open with an overshoot and fades), plus another one on release, and it did so even when the page could not be cast
 * (cooldown, sealed, no book out). On the client that reads as runes flashing and spamming, and the 64 effect slots were flushed
 * by the circles. The fix is to see the hold as one continuous state:
 * <ul>
 *   <li>{@link Kind#START}: the first charged tick opens ONE circle on the client;</li>
 *   <li>{@link Kind#LEVEL}: the charge reached the next of {@value #LEVELS} steps, the client grows the same circle smoothly;</li>
 *   <li>{@link Kind#RENEW}: a keep-alive every {@value #RENEW_EVERY} ticks (the client drops a circle that hears nothing for
 *       {@value #LIFE} ticks, so a lost release never leaves a circle behind);</li>
 *   <li>{@link Kind#END}: the key was released, the circle holds through the wind-up and fades;</li>
 *   <li>{@link Kind#TAP}: released without ever showing the hold circle (a tap): the single release circle the cast always had.</li>
 * </ul>
 * Nothing here decides whether a spell is cast or what it costs: {@code allowed} only says whether the page could be cast right now,
 * so a blocked page shows no circles at all (the fail message on release is unchanged).
 */
public final class CastHold {
    private CastHold() {}

    /** Charge steps the circle grows in (the circle's size is smoothed on the client, so few steps are enough). */
    public static final int LEVELS = 4;
    /** Ticks of key hold before the hold circle opens: a tap shorter than this shows only the release circle. */
    public static final int MIN_CHARGE = 2;
    /** Ticks without news after which the client drops the circle. */
    public static final int LIFE = 50;
    /** Keep-alive period (must stay well under {@link #LIFE}). */
    public static final int RENEW_EVERY = 30;
    /** Ticks the circle fades over after the cast moment. */
    public static final int END_TAIL = 6;
    /** A press closer than this to the previous one does not play the chant clip again. */
    public static final int PRESS_DEBOUNCE = 3;

    public enum Kind { NONE, START, LEVEL, RENEW, END, TAP }

    /** What the server last told the client: nothing is active when {@code level} is 0. */
    public record State(int level, long sentAt) {
        public boolean active() { return level > 0; }
    }

    public static final State IDLE = new State(0, Long.MIN_VALUE / 4);

    /** The next state, the message to send ({@link Kind#NONE} = none) and the charge step it carries. */
    public record Step(State next, Kind kind, int level) {}

    /** 1..{@link #LEVELS}: the charge as a step, rounded up so the first charged tick already shows the smallest circle. */
    public static int levelOf(int charge, int castTicks) {
        if (charge <= 0 || castTicks <= 0) return 0;
        int l = (int) Math.ceil(Math.min(1.0, charge / (double) castTicks) * LEVELS);
        return Math.max(1, Math.min(LEVELS, l));
    }

    /** The circle's power (1.6 * power = radius in blocks) for a step: the old 0.45 .. 1.15 range of the charge circle. */
    public static float power(int level) {
        int l = Math.max(1, Math.min(LEVELS, level));
        return 0.45f + 0.7f * l / LEVELS;
    }

    /** Called every tick the key is held, with the charge so far. */
    public static Step held(State s, long now, int charge, int castTicks, boolean allowed) {
        boolean live = s.active() && now - s.sentAt() <= LIFE;     // an old state means the client has already dropped its circle
        if (!allowed) return live ? new Step(IDLE, Kind.END, s.level()) : new Step(IDLE, Kind.NONE, 0);
        if (charge < MIN_CHARGE) return new Step(live ? s : IDLE, Kind.NONE, 0);
        int level = levelOf(charge, castTicks);
        if (!live) return new Step(new State(level, now), Kind.START, level);
        if (level != s.level()) return new Step(new State(level, now), Kind.LEVEL, level);
        if (now - s.sentAt() >= RENEW_EVERY) return new Step(new State(level, now), Kind.RENEW, level);
        return new Step(s, Kind.NONE, 0);
    }

    /**
     * The key was released. A live hold ends (the client fades it after the wind-up); otherwise a tap gets its single release circle
     * if the page can be cast. {@code tapAllowed} is false while an earlier release is still winding up.
     */
    public static Step release(State s, long now, boolean tapAllowed) {
        boolean live = s.active() && now - s.sentAt() <= LIFE;
        if (live) return new Step(IDLE, Kind.END, s.level());
        return tapAllowed ? new Step(IDLE, Kind.TAP, 0) : new Step(IDLE, Kind.NONE, 0);
    }

    /** A new press starts from nothing (the client keeps its own circle and picks it up again when the next START arrives). */
    public static State press() { return IDLE; }

    /** True when a press is far enough from the previous one to play the chant clip again. */
    public static boolean pressAccepted(long lastPressAt, long now) { return now - lastPressAt >= PRESS_DEBOUNCE; }

    /** Client side: how long the END message makes the circle last, wind-up plus the fade. */
    public static int endDuration(int windup) { return Math.max(1, windup) + END_TAIL; }
}
