package com.newuniverse.nusmp.client.relics.djinn.tiny;

/**
 * One Tiny Djinn as a small state machine and a spring: where it is, which clip it plays and how far into it, how opaque it is, and what happened this frame (the
 * events the renderer turns into particles and sounds). Pure (no Minecraft types), so TinyDjinnActorTest runs every transition. The renderer ({@link TinyDjinnLayer})
 * feeds it numbers each frame: the shoulder anchor, the foe's position, the vessel point to dissolve into and the yaw to face.
 *
 * <pre>
 *   GONE --spawn--> ENTER (poof, 10 ticks) --> IDLE --cue--> CAST (12 ticks, point at the foe) --> ATTACK (12 ticks: dash out 4, hold 2, back 6) --> IDLE
 *                                                                                                          \--(a kill)--> HOP (16 ticks) --> IDLE
 *   any state --exit--> EXIT (spiral into the vessel, 12 ticks) --> GONE
 * </pre>
 * The server cues a cast 16 ticks before the Lesser Summon's bolt lands ({@code TinyDjinnCue}), so the dash arrives as the bolt does.
 */
public final class TinyDjinnActor {
    public enum State { GONE, ENTER, IDLE, CAST, ATTACK, HOP, EXIT }

    /** Events of a frame (a bit mask; read and cleared with {@link #takeEvents()}). */
    public static final int EV_POOF_IN = 1, EV_CAST = 2, EV_STRIKE = 4, EV_HOP = 8, EV_POOF_OUT = 16, EV_SPIRAL = 32;

    public static final float ENTER_TICKS = 10f, CAST_TICKS = 12f, ATTACK_TICKS = TinyDjinnMath.DASH_TOTAL, HOP_TICKS = 16f, EXIT_TICKS = 12f;
    /** The tick of the cast at which the figure points at the foe and the spell bursts (the cast clip reaches its pose at 0.24 s). */
    public static final float CAST_BURST_AT = 5f;
    /** How many ticks after it reaches the foe the renderer asks whether the foe died (the kill check). */
    public static final float KILL_CHECK_AFTER = 3f;

    public final float[] pos = new float[3];
    public final float[] vel = new float[3];
    public float yaw;
    public State state = State.GONE;
    /** The entity id of the foe of the cast in progress, or -1. */
    public int targetId = -1;

    private float stateStart, lastNow = -1f;
    private final float phase;
    private int events;
    private boolean killPending, burstDone, strikeDone, killChecked;
    private final float[] from = new float[3], tmp = new float[3], goal = new float[3];

    /** @param phase 0..1: how far out of step this figure's float and idle clip are from the others' */
    public TinyDjinnActor(float phase) { this.phase = phase; }

    // ---------------------------------------------------------------- commands
    /** Appears at {@code anchor} in a poof. */
    public void spawn(float now, float[] anchor, float faceYaw) {
        System.arraycopy(anchor, 0, pos, 0, 3);
        vel[0] = vel[1] = vel[2] = 0f;
        yaw = faceYaw;
        enter(State.ENTER, now);
        lastNow = now;
        events |= EV_POOF_IN;
        killPending = false;
        targetId = -1;
    }

    /** A cast at the foe {@code foeId}. Accepted from IDLE, from HOP and from the end of ENTER; refused while casting, attacking, leaving or not there. */
    public boolean cue(float now, int foeId) {
        boolean ok = state == State.IDLE || state == State.HOP || (state == State.ENTER && now - stateStart >= 6f);
        if (!ok) return false;
        targetId = foeId;
        killPending = false;
        burstDone = false;
        strikeDone = false;
        killChecked = false;
        enter(State.CAST, now);
        return true;
    }

    /** The foe of the last attack died: a happy hop once the attack is over (at once if it is already over). */
    public void markKill(float now) {
        if (state == State.ATTACK || state == State.CAST) killPending = true;
        else if (state == State.IDLE) { enter(State.HOP, now); events |= EV_HOP; }
    }

    /** Dissolves into the vessel with a spiral. Does nothing when it is leaving or gone. */
    public void exit(float now) {
        if (state == State.GONE || state == State.EXIT) return;
        System.arraycopy(pos, 0, from, 0, 3);
        enter(State.EXIT, now);
    }

    /** Gone at once, with no animation (the owner left the world). */
    public void vanish() { state = State.GONE; events = 0; }

    public boolean alive() { return state != State.GONE; }

    public boolean leaving() { return state == State.EXIT; }

    private void enter(State s, float now) {
        state = s;
        stateStart = now;
    }

    // ---------------------------------------------------------------- the frame
    /**
     * Advances to {@code now} (game time plus partial tick).
     *
     * @param anchor  where it hovers (see {@link TinyDjinnMath#anchor})
     * @param vessel  where it dissolves to when it leaves (the owner's chest)
     * @param foe     the foe's position while a cast or attack is on, or null
     * @param faceYaw the yaw to face when idle (the owner's); a cast and an attack face the foe
     */
    public void update(float now, float[] anchor, float[] vessel, float[] foe, float faceYaw) {
        if (state == State.GONE) return;
        float dtTicks = lastNow < 0f ? 0f : Math.max(0f, now - lastNow);
        lastNow = now;
        float age = now - stateStart;
        float omega = 9f, zeta = 0.78f;
        float bob = TinyDjinnMath.bob(now, phase);
        goal[0] = anchor[0];
        goal[1] = anchor[1] + bob;
        goal[2] = anchor[2];
        float face = faceYaw;

        switch (state) {
            case ENTER -> {
                if (age >= ENTER_TICKS) enter(State.IDLE, now);
            }
            case CAST -> {
                goal[1] += 0.05f * TinyDjinnMath.smooth(age / 4f);
                if (foe != null) face = TinyDjinnMath.yawTo(foe[0] - pos[0], foe[2] - pos[2]);
                if (!burstDone && age >= CAST_BURST_AT) { burstDone = true; events |= EV_CAST; }
                if (age >= CAST_TICKS) { enter(State.ATTACK, now); age = 0f; }
            }
            case ATTACK -> {
                if (foe != null) {
                    face = TinyDjinnMath.yawTo(foe[0] - pos[0], foe[2] - pos[2]);
                    TinyDjinnMath.strikePoint(tmp, anchor, foe, 0.65f);
                    float f = TinyDjinnMath.dash(age);
                    goal[0] = anchor[0] + (tmp[0] - anchor[0]) * f;
                    goal[1] = anchor[1] + (tmp[1] - anchor[1]) * f + bob * (1f - f);
                    goal[2] = anchor[2] + (tmp[2] - anchor[2]) * f;
                }
                omega = 30f;
                zeta = 0.9f;
                if (!strikeDone && age >= TinyDjinnMath.DASH_OUT) { strikeDone = true; events |= EV_STRIKE; }
                if (age >= ATTACK_TICKS) {
                    if (killPending) { killPending = false; enter(State.HOP, now); events |= EV_HOP; }
                    else enter(State.IDLE, now);
                }
            }
            case HOP -> {
                if (age >= HOP_TICKS) enter(State.IDLE, now);
            }
            case EXIT -> {
                float e = Math.min(1f, age / EXIT_TICKS);
                TinyDjinnMath.spiral(tmp, from, vessel, e, 0.26f);
                System.arraycopy(tmp, 0, pos, 0, 3);
                vel[0] = vel[1] = vel[2] = 0f;
                events |= EV_SPIRAL;
                if (age >= EXIT_TICKS) { state = State.GONE; events |= EV_POOF_OUT; }
                yaw += 40f * dtTicks;
                return;
            }
            default -> { }
        }
        if (state == State.IDLE && killPending) killPending = false;
        TinyDjinnMath.spring(pos, vel, goal, omega, zeta, dtTicks / 20f);
        yaw = TinyDjinnMath.turnToward(yaw, face, 22f * Math.max(dtTicks, 0.01f));
    }

    // ---------------------------------------------------------------- what the renderer reads
    public int takeEvents() {
        int e = events;
        events = 0;
        return e;
    }

    /** The clip to play (a name of tinydjinn_anim.animation.json). */
    public String clip() {
        return switch (state) {
            case ENTER -> "poof";
            case CAST -> "cast";
            case ATTACK -> "attack";
            case HOP -> "hop";
            case EXIT -> "exit";
            default -> "idle";
        };
    }

    /** Seconds into the clip: a one-shot from the start of its state, the idle clip on the figure's own clock. */
    public float clipSeconds(float now) {
        if (state == State.IDLE || state == State.GONE) return Math.max(0f, now) / 20f + phase * 6f;
        return Math.max(0f, now - stateStart) / 20f;
    }

    /** 0..1: the figure's opacity (it fades in with the poof and out with the spiral). */
    public float alpha(float now) {
        if (state == State.GONE) return 0f;
        float age = now - stateStart;
        if (state == State.ENTER) return Math.min(1f, age / 3f);
        if (state == State.EXIT) return 1f - TinyDjinnMath.smooth((age - 4f) / 8f);
        return 1f;
    }

    /** The brightness of the additive glow: it breathes, and flares while casting and attacking. */
    public float glow(float now) {
        float base = 0.75f + 0.25f * (float) Math.sin(now * 0.11 + phase * 6.28);
        if (state == State.CAST) return Math.min(1.4f, base + 0.5f * TinyDjinnMath.smooth((now - stateStart) / 6f));
        if (state == State.ATTACK) return 1.4f;
        if (state == State.HOP) return 1.2f;
        return base;
    }

    /** True once, {@link #KILL_CHECK_AFTER} ticks after the figure reached the foe: the renderer then looks whether the foe died and calls {@link #markKill}. */
    public boolean killCheckDue(float now) {
        if (state != State.ATTACK || !strikeDone || killChecked) return false;
        if (now - stateStart < TinyDjinnMath.DASH_OUT + KILL_CHECK_AFTER) return false;
        killChecked = true;
        return true;
    }

    public float stateAge(float now) { return now - stateStart; }
}
