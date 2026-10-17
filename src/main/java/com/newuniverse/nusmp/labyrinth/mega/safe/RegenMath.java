package com.newuniverse.nusmp.labyrinth.mega.safe;

/**
 * Why: the brief ends easy regeneration in the Mega-Dungeon: outside a Safe Zone natural health regeneration and the magicule / aura
 * regeneration are scaled by {@code passiveRegenScale} (default 0.25, 0 = none, 1 = vanilla), so the only fast way to recover is to secure a
 * room and rest. Two hooks need two small decisions and both are here, pure, so the balance is tested without a server. (a) The HEALTH hook
 * has no event for "natural regeneration", only {@code LivingHealEvent}, so it uses a heuristic: vanilla's {@code FoodData} heals in steps of
 * at most 1.0 ({@code heal(1.0f)} at full food, {@code heal(saturation / 6)} while saturated), and so does the Regeneration effect, which is
 * excluded; anything bigger (a potion, a golden apple's instant part, a spell, a rest) is not natural and is left alone. It is a heuristic and
 * says so: a mod that heals in small steps is throttled too. (b) The MANA hook cannot intercept Tensura's regeneration, so it scales the
 * regeneration attribute instead with a transient {@code ADD_MULTIPLIED_TOTAL} modifier of {@code scale - 1}, which multiplies the final value by
 * exactly {@code scale} and cleanly reaches zero at scale 0.
 */
public final class RegenMath {
    private RegenMath() {}

    /** Vanilla natural regeneration never heals more than this in one call; larger heals are not natural. */
    public static final float NATURAL_STEP = 1.0f;

    /**
     * The amount a heal should actually give: natural-sized heals of a player without the Regeneration effect are multiplied by {@code scale};
     * everything else comes back unchanged. A result below half a hundredth of a heart is rounded to 0 (the caller cancels the heal).
     */
    public static float scaleHeal(float amount, double scale, boolean hasRegenerationEffect) {
        if (!(amount > 0f)) return amount;
        if (Double.isNaN(scale) || scale >= 1.0 || hasRegenerationEffect || amount > NATURAL_STEP) return amount;
        float out = (float) (amount * Math.max(0.0, scale));
        return out < 0.005f ? 0f : out;
    }

    /** The {@code ADD_MULTIPLIED_TOTAL} amount that multiplies a regeneration attribute by {@code scale}: {@code scale - 1}, kept within -1..0. */
    public static double modifierAmount(double scale) {
        if (Double.isNaN(scale)) return 0.0;
        return Math.max(-1.0, Math.min(0.0, scale - 1.0));
    }

    /** Does the throttle apply right now: in the Mega-Dungeon, outside a safe zone, to a player who is not exempt, with a scale below 1? */
    public static boolean applies(boolean inMega, boolean inSafeZone, boolean exempt, double scale) {
        return inMega && !inSafeZone && !exempt && !Double.isNaN(scale) && scale < 1.0;
    }
}
