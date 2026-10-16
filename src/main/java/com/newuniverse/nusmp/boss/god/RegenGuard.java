package com.newuniverse.nusmp.boss.god;

/**
 * Why: the "Time Decay" aura must stop every kind of passive recovery near Lucius (natural regeneration, the Regeneration effect,
 * Tensura's regeneration skills) but must not punish a player for drinking a Healing potion or eating. Two doors lead to a player's
 * health: {@code LivingEntity.heal} (the heal event, which can be cancelled) and a direct {@code setHealth} (which Tensura's skills may
 * use, and which no event sees). This pure class holds the rules for both: a heal is allowed only while a potion or food is being
 * used (or a thrown potion lands) and only if it is big enough to be that potion or food ({@link #BURST_HEAL}, because natural and
 * Regeneration heals are 1 point or less and would otherwise ride along), and {@link Tracker} compares the health from tick to tick
 * so a rise that no allowed heal explains is reverted. Unit tested.
 */
public final class RegenGuard {
    private RegenGuard() {}

    /** A heal smaller than this is regeneration-sized and never allowed, even while a potion is being drunk. */
    public static final float BURST_HEAL = 2.0f;
    /** Ticks a "potion or food in use" window stays open after the item is finished or a thrown potion lands. */
    public static final int WINDOW_AFTER = 4;
    private static final float EPS = 0.05f;

    /** May this heal go through? Only a burst heal inside an open window. */
    public static boolean allowsHeal(float amount, boolean windowOpen) { return windowOpen && amount >= BURST_HEAL; }

    /** The per-player comparison of health from tick to tick. */
    public static final class Tracker {
        private float last, credit;
        private boolean has;

        /** Forget everything (left the aura, died, respawned, changed dimension). */
        public void reset() {
            has = false;
            credit = 0f;
        }

        /** A heal that the event let through: the next check expects the health to have risen by this much. */
        public void credit(float amount) { if (amount > 0f) credit += amount; }

        /**
         * One tick's look at the health. Returns the health to put back when it rose by more than the allowed heals explain, or NaN when
         * nothing needs undoing. The first look only records. A dead player and a player above the maximum are never touched.
         */
        public float check(float current, float max, boolean alive) {
            if (!alive || Float.isNaN(current)) {
                reset();
                return Float.NaN;
            }
            if (!has) {
                has = true;
                last = Math.min(current, max);
                credit = 0f;
                return Float.NaN;
            }
            float limit = Math.min(max, last + credit);
            credit = 0f;
            if (current > limit + EPS) {
                last = limit;
                return limit;
            }
            last = current;
            return Float.NaN;
        }

        public boolean tracking() { return has; }
    }
}
