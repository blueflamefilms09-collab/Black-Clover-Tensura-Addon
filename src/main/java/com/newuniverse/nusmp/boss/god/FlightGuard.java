package com.newuniverse.nusmp.boss.god;

/**
 * Why: Lucifero's gravity keeps every player on the ground in the arena, whatever gave them flight (a Tensura skill, wings, a relic, a
 * Djinn, an elytra). The hard part is giving it back correctly. Flight in this mod is granted by many systems that each set and clear
 * the ability themselves, and some re-assert it every second; a naive "restore the old value" would turn a flight that ended during the
 * fight (a Full Djinn Equip that timed out inside the arena) into permanent creative-style flight. This pure class keeps the record per
 * player and makes the restore decision from evidence: flight is restored on leaving only if the source was seen to re-assert it during
 * the stay (it is alive) or the stay was short (a source that does not re-assert, such as a one-shot grant, is not lost to a quick
 * exit); an inherent flight (creative, when the config lets the gravity reach them) is always restored. Also holds the extra-gravity
 * curve. No Minecraft types, unit tested.
 */
public final class FlightGuard {
    private FlightGuard() {}

    /** A re-assertion this recent counts as "the source is still alive" on leaving. */
    public static final int REGRANT_WINDOW = 100;
    /** A stay this short restores flight even without a re-assertion. */
    public static final int SHORT_STAY = 200;
    /** Blocks above the ground over which the extra gravity starts. */
    public static final double AIR_START = 3.0;

    /** What the glue does this tick. */
    public enum Action { NONE, GROUND }

    /** The record of one player in the arena. */
    public static final class Record {
        private boolean prevMayfly, inherent, entered;
        private long enteredAt, lastRegrant = Long.MIN_VALUE;

        public boolean entered() { return entered; }

        /**
         * One tick in the arena with the player's current ability flags. GROUND means "clear mayfly and flying now". The first look
         * records the previous value; a later look that finds flight on again, after we cleared it, is a re-assertion.
         */
        public Action tick(boolean mayfly, boolean flying, boolean inherentFlight, long now) {
            if (!entered) {
                entered = true;
                enteredAt = now;
                inherent = inherentFlight;
                prevMayfly = mayfly;
                return mayfly || flying ? Action.GROUND : Action.NONE;
            }
            if (mayfly || flying) {
                if (!inherent) lastRegrant = now;
                return Action.GROUND;
            }
            return Action.NONE;
        }

        /** Should flight be given back on leaving at 'now'? */
        public boolean restore(long now) {
            if (!entered || !prevMayfly) return false;
            if (inherent) return true;
            if (lastRegrant != Long.MIN_VALUE && now - lastRegrant <= REGRANT_WINDOW) return true;
            return now - enteredAt <= SHORT_STAY;
        }

        public void reset() {
            entered = false;
            prevMayfly = inherent = false;
            lastRegrant = Long.MIN_VALUE;
            enteredAt = 0;
        }
    }

    /**
     * The downward speed to add each tick for a player 'height' blocks above the ground: nothing within {@value #AIR_START} blocks, then
     * a small push that grows with the height, never more than 0.12 a tick (a firm pull, not a fall that hurts: fall damage is
     * unchanged and the push only applies in the arena).
     */
    public static double extraGravity(double height) {
        if (!(height > AIR_START)) return 0.0;
        return Math.min(0.12, 0.03 + 0.01 * (height - AIR_START));
    }
}
