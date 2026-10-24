package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;

/**
 * The mobility runes (pure, tested). In the anime the Mana Method also writes runes that carry the mage: a platform of rune light under the feet to walk on air, and a rune that pushes
 * the body along at speed. Two universal pages:
 * <ul>
 *   <li><b>Air Step Rune</b>: for {@code airStepSeconds} an invisible barrier block is kept under the caster's feet while they are in the air (sneak to drop through). Only into air cells,
 *       only where the caster may interact, at most {@value #MAX_CELLS_PER_CASTER} blocks per caster at once, every block removed {@value #CELL_TTL} ticks after it was last needed,
 *       all removed when the duration ends, on logout, death, dimension change and server stop.</li>
 *   <li><b>Dash Rune</b>: a velocity push along the look vector, working mid-air, its strength scaled by how long the page was held, a cooldown of {@value #DASH_COOLDOWN} ticks, the velocity
 *       sent to the client properly (hurtMarked). No fall damage exploit: a dash forgives {@value #FALL_GRACE} blocks of the fall so far, no more.</li>
 * </ul>
 */
public final class MobilityRules {
    private MobilityRules() {}

    // ---------------------------------------------------------------- the pages' prices: the starter tier of the 15-spells recipe for a utility rune (8 %, floor 150), a cheaper one for the dash
    public static final double COST_PERCENT_AIR_STEP = 8.0, COST_FLOOR_AIR_STEP = 150.0, COST_PERCENT_DASH = 6.0, COST_FLOOR_DASH = 100.0;
    public static final int COOLDOWN_AIR_STEP = 240, COOLDOWN_DASH = 60;

    // ---------------------------------------------------------------- air step
    public static final int DEFAULT_SECONDS = 6, MIN_SECONDS = 2, MAX_SECONDS = 30;
    public static final int MAX_CELLS_PER_CASTER = 4, CELL_TTL = 10, MAX_SESSIONS = 16, MAX_CELLS_GLOBAL = 64;
    /** Half the width of the player's feet used to find the blocks they stand on (a player is 0.6 wide). */
    public static final double HALF_WIDTH = 0.3;

    public static int seconds(int configured) { return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configured)); }

    /** What one tick of an air-walker does. */
    public enum Step { PLACE, REFRESH, DROP, NONE }

    /**
     * Sneaking: drop through (the blocks under the feet are taken away and none is placed). In the air: keep a platform under the feet. On the ground: only keep a platform that is
     * already ours (standing on it); real ground needs no help.
     */
    public static Step decide(boolean sneaking, boolean onGround, boolean standingOnOwnCell) {
        if (sneaking) return Step.DROP;
        if (!onGround) return Step.PLACE;
        return standingOnOwnCell ? Step.REFRESH : Step.NONE;
    }

    /** The y of the cell directly under the feet at {@code feetY} (its top face is at or just below the feet). */
    public static int cellY(double feetY) { return (int) Math.floor(feetY + 1.0e-3) - 1; }

    /**
     * The distinct cells under a player's feet at (x, feetY, z): the cells of the four corners of the 0.6 wide footprint, so a player standing on an edge is held too. Writes
     * (x, y, z) triples into {@code out} (at least 12 numbers) and returns how many cells (1 to 4).
     */
    public static int footprint(double x, double feetY, double z, int[] out) {
        int y = cellY(feetY), n = 0;
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = (int) Math.floor(x + sx * HALF_WIDTH), cz = (int) Math.floor(z + sz * HALF_WIDTH);
                boolean seen = false;
                for (int k = 0; k < n; k++) if (out[k * 3] == cx && out[k * 3 + 2] == cz) { seen = true; break; }
                if (seen) continue;
                out[n * 3] = cx;
                out[n * 3 + 1] = y;
                out[n * 3 + 2] = cz;
                n++;
            }
        return n;
    }

    /** May one more cell be placed for a caster with {@code casterLive} cells in a world with {@code globalLive}? */
    public static boolean cellAllowed(int casterLive, int globalLive) { return casterLive < MAX_CELLS_PER_CASTER && globalLive < MAX_CELLS_GLOBAL; }

    /** Is a cell last needed at {@code lastNeeded} past its life at {@code now}? */
    public static boolean cellExpired(long now, long lastNeeded) { return now - lastNeeded >= CELL_TTL; }

    // ---------------------------------------------------------------- dash
    public static final double DEFAULT_STRENGTH = 0.9, MIN_STRENGTH = 0.3, MAX_STRENGTH = 2.0, MAX_SPEED = 2.0;
    public static final int DASH_COOLDOWN = 30;
    public static final double FALL_GRACE = 3.0;
    /** A dash from the ground gets a little lift so the ground's friction does not eat it. */
    public static final double GROUND_LIFT = 0.2;
    /** The hold scaling: a tap pushes x{@value #TAP_MULT}, a full hold x{@value #FULL_MULT}; no hold information (the old cast) pushes x1. */
    public static final double TAP_MULT = 0.55, FULL_MULT = 1.5;

    public static double strength(double configured) {
        if (Double.isNaN(configured)) return DEFAULT_STRENGTH;
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, configured));
    }

    /** The push of a charge: the configured strength times {@link #TAP_MULT} .. {@link #FULL_MULT} by the ticks held (1 without a measured hold), never above {@link #MAX_SPEED}. */
    public static double dashSpeed(double configured, Charge c, int maxHold) {
        double mult = 1.0;
        if (c != null && !c.legacy()) {
            double f = maxHold <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, c.held() / (double) maxHold));
            mult = TAP_MULT + (FULL_MULT - TAP_MULT) * f;
        }
        return Math.min(MAX_SPEED, strength(configured) * mult);
    }

    /**
     * The velocity of a dash: the unit look vector times {@code speed}; from the ground a small lift is added when the look is nearly level. Writes (x, y, z) to {@code out}; a zero or NaN
     * look gives a zero velocity. The result is never faster than {@link #MAX_SPEED}.
     */
    public static void dashVelocity(double lx, double ly, double lz, double speed, boolean onGround, double[] out) {
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (!(len > 1e-9) || Double.isNaN(speed)) { out[0] = out[1] = out[2] = 0.0; return; }
        double s = Math.max(0.0, Math.min(MAX_SPEED, speed));
        double vx = lx / len * s, vy = ly / len * s, vz = lz / len * s;
        if (onGround && ly / len < 0.2) vy = Math.max(vy, GROUND_LIFT);
        double v = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (v > MAX_SPEED) { double k = MAX_SPEED / v; vx *= k; vy *= k; vz *= k; }
        out[0] = vx;
        out[1] = vy;
        out[2] = vz;
    }

    public static boolean dashReady(long now, long lastDash) { return now - lastDash >= DASH_COOLDOWN; }

    /** The fall distance after a dash: {@link #FALL_GRACE} blocks forgiven, never below zero. */
    public static float fallAfterDash(float fallDistance) { return (float) Math.max(0.0, fallDistance - FALL_GRACE); }
}
