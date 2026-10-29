package com.newuniverse.nusmp.arc.tier;

import java.util.UUID;

/**
 * Why: Light Yagami is a gag boss, not a fight: he runs ONE scripted scene at a player (the potato chip, then the pen and the Death Note). The rules of that scene (how long each beat
 * lasts, what cancels it, what the last tick does) must be exactly the same on the server (which decides everything) and on the client (which only draws), and must be testable without a
 * game, so they live here as a small pure state machine: no Minecraft class, one object per scene, advanced one tick at a time with what the server sees ({@link Obs}).
 *
 * <p>The scene has three beats and an end:
 * <ul>
 *   <li>{@link Phase#CHIP}: Light's over-serious monologue while he takes and eats a potato chip (the setup of the joke; the voice line plays at its start).</li>
 *   <li>{@link Phase#RAISE}: he raises his hand and the pen.</li>
 *   <li>{@link Phase#WRITE}: the pen is on the page and the name is being written (a few seconds). The counterplay window runs through all three beats until the last tick of the writing.</li>
 *   <li>The last tick ends the scene with a {@link Step#FINISH_KILL} (the real Death Note death) or a {@link Step#FINISH_JOKE} (the server setting is off: a harmless joke ending).</li>
 * </ul>
 * A scene is cancelled by anything that breaks the page: Light dies, Light is knocked away (or hit hard), the target is gone (logout, death, another dimension), is no longer a survival
 * player, or runs out of range. Cost: a constant amount of arithmetic per tick, no allocation.
 */
public final class LightScene {
    /** The beats. DONE and CANCELLED are the two ends. */
    public enum Phase { CHIP, RAISE, WRITE, DONE, CANCELLED }

    /** Why a scene was cancelled (NONE while it runs or after it finished). */
    public enum Cancel { NONE, LIGHT_DIED, KNOCKED_AWAY, TARGET_GONE, TARGET_INELIGIBLE, TARGET_ESCAPED }

    /** What one tick of the scene says to the server. */
    public enum Step { CONTINUE, PHASE_CHANGED, CANCELLED, FINISH_KILL, FINISH_JOKE }

    /** What the server sees this tick (plain values, so the machine needs no game class). */
    public record Obs(boolean lightAlive, double lightDisplacement, boolean targetPresent, boolean targetEligible, double targetDistance, boolean lethal) {}

    /** Ticks of each beat (20 ticks = 1 s): 3.5 s of chip, 2 s of the raised pen, 4 s of writing, 9.5 s in all. */
    public static final int CHIP_TICKS = 70, RAISE_TICKS = 40, WRITE_TICKS = 80, TOTAL_TICKS = CHIP_TICKS + RAISE_TICKS + WRITE_TICKS;
    /** A scene never outlives this many ticks on the client, whatever packets arrive or are lost. */
    public static final int CLIENT_HARD_LIMIT = TOTAL_TICKS + 40;
    /** Light moved more than this far from where he began (knocked back, shoved): the page is ruined. */
    public static final double KNOCK_BLOCKS = 1.5;
    /** The writing needs the target within this many blocks (squared would be cheaper but the machine is called once a tick per scene). */
    public static final double MAX_DISTANCE = 26.0;
    /** A single hit of at least this share of Light's maximum health staggers him and cancels the scene. */
    public static final double STAGGER_SHARE = 0.20;

    private final UUID light, target;
    private int age;
    private Phase phase = Phase.CHIP;
    private Cancel cancel = Cancel.NONE;

    public LightScene(UUID light, UUID target) {
        this.light = light;
        this.target = target;
    }

    public UUID light() { return light; }

    public UUID target() { return target; }

    public int age() { return age; }

    public Phase phase() { return phase; }

    public Cancel cancelReason() { return cancel; }

    public boolean running() { return phase != Phase.DONE && phase != Phase.CANCELLED; }

    /** Which beat a given age falls in (the client uses this too, so the two sides cannot disagree). */
    public static Phase phaseAt(int age) {
        if (age < CHIP_TICKS) return Phase.CHIP;
        if (age < CHIP_TICKS + RAISE_TICKS) return Phase.RAISE;
        if (age < TOTAL_TICKS) return Phase.WRITE;
        return Phase.DONE;
    }

    /** The share (0..1) of the writing that is done at this age: 0 before the pen touches the page, 1 at the end. */
    public static float writeProgress(int age) {
        return Math.max(0f, Math.min(1f, (age - CHIP_TICKS - RAISE_TICKS) / (float) WRITE_TICKS));
    }

    /** Is a hit of this size, on a boss of this maximum health, big enough to stagger him? */
    public static boolean staggers(float amount, float maxHealth) { return maxHealth > 0f && amount >= maxHealth * STAGGER_SHARE; }

    /** Ends the scene from outside (a logout, a death, a dimension change, the boss leaving the world). Does nothing to a scene that already ended. */
    public void interrupt(Cancel why) {
        if (!running() || why == Cancel.NONE) return;
        phase = Phase.CANCELLED;
        cancel = why;
    }

    /** One tick: looks at what the server sees, in the order that matters (Light first: if he is dead nothing else counts), and moves the scene on. A scene that ended answers CONTINUE and does nothing. */
    public Step tick(Obs o) {
        if (!running()) return Step.CONTINUE;
        if (!o.lightAlive()) return end(Cancel.LIGHT_DIED);
        if (o.lightDisplacement() > KNOCK_BLOCKS) return end(Cancel.KNOCKED_AWAY);
        if (!o.targetPresent()) return end(Cancel.TARGET_GONE);
        if (!o.targetEligible()) return end(Cancel.TARGET_INELIGIBLE);
        if (o.targetDistance() > MAX_DISTANCE) return end(Cancel.TARGET_ESCAPED);
        age++;
        if (age >= TOTAL_TICKS) {
            phase = Phase.DONE;
            return o.lethal() ? Step.FINISH_KILL : Step.FINISH_JOKE;
        }
        Phase now = phaseAt(age);
        if (now != phase) {
            phase = now;
            return Step.PHASE_CHANGED;
        }
        return Step.CONTINUE;
    }

    private Step end(Cancel why) {
        phase = Phase.CANCELLED;
        cancel = why;
        return Step.CANCELLED;
    }
}
