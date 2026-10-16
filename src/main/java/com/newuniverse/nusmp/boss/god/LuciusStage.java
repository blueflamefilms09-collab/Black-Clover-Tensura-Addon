package com.newuniverse.nusmp.boss.god;

/**
 * Why: the god-tier Lucius has three stages with a full health reset between them, and every other part of the fight (the acts, the
 * power curve, the boss bar, the refill that must never fire twice) has to agree on which stage it is. This pure class (no Minecraft
 * types, unit tested) is that one answer: the stage number, what a fatal blow means in it (the next stage, or a real death in the
 * last one), the id under which each stage's refill is remembered by {@code PhaseRefill} (so the same refill can not fire twice), and
 * the "virtual health" that lets the existing power curve (which reads one health bar) run over all three stages without being
 * rewritten: stage 1 is the top third of the virtual bar, stage 3 the bottom third, and the line between two stages is continuous
 * (the end of stage 1 and the start of stage 2 are the same virtual health).
 */
public final class LuciusStage {
    /** Number of stages. */
    public static final int COUNT = 3;

    /** What a fatal blow does. */
    public enum Fall { NEXT_STAGE, REAL_DEATH }

    private int stage = 1;

    /** The current stage, 1 to {@value #COUNT}. */
    public int stage() { return stage; }

    public boolean last() { return stage >= COUNT; }

    /**
     * The decision for a blow that would take the health to zero. A real kill (the kill command, the void) is always a real death, so an
     * admin can still end the fight; the last stage's fatal blow is a real death; anything else starts the next stage.
     */
    public Fall onFatal(boolean realKill) {
        return realKill || last() ? Fall.REAL_DEATH : Fall.NEXT_STAGE;
    }

    /** Moves to the next stage. False (and no change) in the last stage. */
    public boolean advance() {
        if (last()) return false;
        stage++;
        return true;
    }

    /** A new fight. */
    public void reset() { stage = 1; }

    /** Puts a saved stage back (garbage is made safe). */
    public void restore(int saved) { stage = Math.max(1, Math.min(COUNT, saved)); }

    /** The id under which the refill that ends stage 'fromStage' is remembered: one per stage, so it can fire only once. */
    public static String refillMode(int fromStage) { return "luciusStage" + Math.max(1, Math.min(COUNT, fromStage)); }

    /** The act of the arc that a stage plays: the stage number itself (Time and Soul, Gravity and Blood, Spatial and the clones). */
    public static int act(int stage) { return Math.max(1, Math.min(COUNT, stage)); }

    /** The health on the virtual bar that spans all stages: whole stages not yet entered plus the health of this one. */
    public static float virtualHealth(int stage, float health, float maxHealth) {
        int s = Math.max(1, Math.min(COUNT, stage));
        return (COUNT - s) * maxHealth + Math.max(0f, Math.min(health, maxHealth));
    }

    /** The size of the virtual bar. */
    public static float virtualMax(float maxHealth) { return COUNT * maxHealth; }

    /** The share of the whole fight that is left, 1 at the start of stage 1 and 0 at the end of stage 3. */
    public static float overallFraction(int stage, float health, float maxHealth) {
        float max = virtualMax(maxHealth);
        return max > 0f ? virtualHealth(stage, health, maxHealth) / max : 0f;
    }

    /** "Stage 2/3" for the boss bar. */
    public static String label(int stage) { return "Stage " + Math.max(1, Math.min(COUNT, stage)) + "/" + COUNT; }

    /** The name of the arsenal a stage unlocks. */
    public static String arsenal(int stage) {
        return switch (act(stage)) {
            case 1 -> "Time and Soul";
            case 2 -> "Gravity and Blood";
            default -> "Space and the Infinite Clones";
        };
    }
}
