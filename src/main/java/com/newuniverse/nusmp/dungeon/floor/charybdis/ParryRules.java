package com.newuniverse.nusmp.dungeon.floor.charybdis;

/**
 * The rule behind the Charybdis Spawn's signature: its magicule scales "must be parried or deflected" (pure, unit tested). A scale that reaches
 * a player does NOT hurt them when
 * <ul>
 *   <li>(a) they are holding up a shield and the scale comes from the front ({@link Deflect#SHIELD}),</li>
 *   <li>(b) they are inside the Magebane Shield's Spellbreak Parry window and the scale is in front of them or beside them ({@link Deflect#PARRY},
 *       the window is wider than a plain shield: it turns missiles round at a distance), or</li>
 *   <li>(c) they strike the scale itself with a melee blow ({@link #strikeDeflects}).</li>
 * </ul>
 * Instead the scale is sent back at the boss at {@link #SPEED_FACTOR} times its speed, and wounds it for {@link #BONUS_FRACTION} of its maximum
 * health, at most {@link #VOLLEY_CAP_FRACTION} of it for the whole volley ({@link Pool}). Anything else is an ordinary hit, bounded by the boss's
 * damage law and the volley's hits-per-victim budget. A scale already sent back is never deflected again, and never hurts a player.
 */
public final class ParryRules {
    private ParryRules() {}

    public enum Deflect { NONE, SHIELD, PARRY }

    /** How much faster a scale flies back, the least and the most it flies (blocks a tick) so it neither crawls nor tunnels through a wall. */
    public static final double SPEED_FACTOR = 1.5, MIN_RETURN_SPEED = 0.6, MAX_RETURN_SPEED = 1.4;
    /** The bonus wound of one returned scale and the most one volley can add, as shares of the boss's maximum health. */
    public static final double BONUS_FRACTION = 0.04, VOLLEY_CAP_FRACTION = 0.12;
    /** The dot product of the way a player looks and the way to the scale above which a shield covers it (the front half), and the parry's wider cover. */
    public static final double SHIELD_FRONT = 0.0, PARRY_FRONT = -0.35;

    /**
     * What happens to a scale that has reached a player.
     * @param blocking  the player is blocking with a shield ({@code isBlocking()})
     * @param parryOpen the Magebane Shield's parry window is open for the player
     * @param facing    the dot product of the player's look vector and the unit vector from the player to the scale (1 dead ahead, -1 behind)
     */
    public static Deflect onImpact(boolean blocking, boolean parryOpen, double facing) {
        if (parryOpen && facing > PARRY_FRONT) return Deflect.PARRY;
        if (blocking && facing > SHIELD_FRONT) return Deflect.SHIELD;
        return Deflect.NONE;
    }

    /** Does a blow at the scale itself turn it? Only a player's, and only once. */
    public static boolean strikeDeflects(boolean byPlayer, boolean alreadyReturned) { return byPlayer && !alreadyReturned; }

    /** The speed of a scale on its way back: half as fast again, never under {@link #MIN_RETURN_SPEED} nor over {@link #MAX_RETURN_SPEED}. */
    public static double returnSpeed(double speed) { return Math.max(MIN_RETURN_SPEED, Math.min(MAX_RETURN_SPEED, speed * SPEED_FACTOR)); }

    /** The wound one returned scale means to deal. */
    public static double bonus(double bossMaxHealth) { return Math.max(0, bossMaxHealth) * BONUS_FRACTION; }

    /** The dot product of a look vector and the vector to a point (neither needs to be a unit vector; 1 = ahead, -1 = behind, 0 if either is zero). */
    public static double facing(double lookX, double lookY, double lookZ, double toX, double toY, double toZ) {
        double a = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ), b = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
        if (a < 1e-9 || b < 1e-9) return 0;
        return (lookX * toX + lookY * toY + lookZ * toZ) / (a * b);
    }

    /** What a whole volley may take from the boss in bonus wounds: one pool per volley. */
    public static final class Pool {
        private final double cap;
        private double paid;

        public Pool(double bossMaxHealth) { this.cap = Math.max(0, bossMaxHealth) * VOLLEY_CAP_FRACTION; }

        /** Takes up to 'want' from the pool; returns what was granted (0 once the pool is empty). */
        public double take(double want) {
            double give = Math.max(0, Math.min(want, cap - paid));
            paid += give;
            return give;
        }

        public double paid() { return paid; }

        public double cap() { return cap; }

        public boolean empty() { return paid >= cap - 1e-9; }
    }
}
