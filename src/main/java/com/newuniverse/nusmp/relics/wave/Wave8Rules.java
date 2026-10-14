package com.newuniverse.nusmp.relics.wave;

/**
 * Relics of Many Worlds, wave 8 (the ten blades and axes): the pure rules, with no Minecraft types so they are unit-tested.
 * Soul-fragment milestones of the Soul Shatter Blade, its per-day charges, the two-hits-in-a-row combo of the Dragon Tooth Blade,
 * the "saving throw" rolls, cones, lifesteal overflow and the small tables the other blades read.
 */
public final class Wave8Rules {
    private Wave8Rules() {}

    public static final int DAY = 24000;

    // ---------------------------------------------------------------- Soul Shatter Blade
    /** Fragments at which each reward comes (the card: 5, 10, 15, 20, 25, 30). */
    public static final int M_CHARGES = 5, M_DARK = 10, M_THREE = 15, M_RAISE = 20, M_VOICE = 25, M_CALL = 30;
    public static final int MAX_SOULS = 999;

    /** Charges of Inflict Wounds / Animate Dead a day: none before 5 fragments, 2 from 5, 3 from 15. */
    public static int maxCharges(int souls) {
        if (souls >= M_THREE) return 3;
        if (souls >= M_CHARGES) return 2;
        return 0;
    }

    /** Charges left today given what was stored (day number and how many were used that day). */
    public static int chargesLeft(int souls, long storedDay, int storedUsed, long today) {
        int used = storedDay == today ? Math.max(0, storedUsed) : 0;
        return Math.max(0, maxCharges(souls) - used);
    }

    public static int usedAfter(long storedDay, int storedUsed, long today) { return (storedDay == today ? Math.max(0, storedUsed) : 0) + 1; }

    public static long dayOf(long gameTime) { return Math.floorDiv(gameTime, (long) DAY); }

    public static boolean darkAura(int souls) { return souls >= M_DARK; }
    public static boolean canRaise(int souls) { return souls >= M_RAISE; }
    public static boolean lichSpeaks(int souls) { return souls >= M_VOICE; }
    public static boolean lichCalls(int souls) { return souls >= M_CALL; }

    public static int addSoul(int souls) { return Math.min(MAX_SOULS, Math.max(0, souls) + 1); }

    /**
     * What the cleansing does: a golden apple lifts the curse only while the lich is silent (under 25 fragments); an enchanted golden apple
     * ("greater restoration") takes all of them. Returns the fragments left.
     */
    public static int cleanse(int souls, boolean enchantedApple) {
        if (enchantedApple) return 0;
        return souls < M_VOICE ? 0 : souls;
    }

    /** Lifesteal on a kill: {heal, overflow} where the overflow (what would pass the maximum) becomes temporary hit points, at most {@code cap}. */
    public static float[] lifesteal(float dealt, float health, float maxHealth, float cap) {
        float d = Math.max(0f, dealt);
        float room = Math.max(0f, maxHealth - health);
        float heal = Math.min(d, room);
        float over = Math.min(Math.max(0f, d - heal), Math.max(0f, cap));
        return new float[]{heal, over};
    }

    // ---------------------------------------------------------------- Dragon Tooth Blade
    /** The window, in ticks, in which two hits count as "two strikes in a row on the same turn". */
    public static final int COMBO_WINDOW = 60;

    /**
     * The combo: the same target hit again within the window bursts into flame. Returns the new count of strikes (1 = a first strike,
     * 2 = the burst; the caller resets to 0 after a burst).
     */
    public static int comboNext(int lastTarget, long lastTick, int count, int target, long now) {
        if (target == lastTarget && count > 0 && now - lastTick <= COMBO_WINDOW && now >= lastTick) return count + 1;
        return 1;
    }

    // ---------------------------------------------------------------- saving throws
    /** A saving throw as a roll of 0..99 against a percentage chance to resist: true = the target saves. */
    public static boolean saves(int roll0to99, int savePercent) { return roll0to99 < clamp(savePercent, 0, 100); }

    /** The chance (percent) that a target saves: mobs by their toughness, players a little less, bosses always half-save at least. */
    public static int savePercent(boolean player, boolean boss, float maxHealth, int base) {
        int p = base + (maxHealth >= 60f ? 15 : maxHealth >= 30f ? 8 : 0);
        if (player) p -= 10;
        if (boss) p = Math.max(p, 75);
        return clamp(p, 5, 95);
    }

    // ---------------------------------------------------------------- aiming
    /** Is the point (dx,dy,dz) from the user within {@code range} and inside the cone of the look vector (lx,ly,lz) with cosine {@code cosHalf}? */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double range, double cosHalf) {
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > range * range) return false;
        if (d2 < 1.0e-6) return true;
        double l = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (l < 1.0e-9) return false;
        return (dx * lx + dy * ly + dz * lz) / (Math.sqrt(d2) * l) >= cosHalf;
    }

    /** Squared distance from the point (px,py,pz) to the segment a-b (the Leviathan Axe's flight line). */
    public static double segDist2(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double abx = bx - ax, aby = by - ay, abz = bz - az;
        double len2 = abx * abx + aby * aby + abz * abz;
        double t = len2 < 1.0e-9 ? 0.0 : ((px - ax) * abx + (py - ay) * aby + (pz - az) * abz) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        double cx = ax + abx * t - px, cy = ay + aby * t - py, cz = az + abz * t - pz;
        return cx * cx + cy * cy + cz * cz;
    }

    // ---------------------------------------------------------------- the small tables
    /** The Random Axe of Charity: 2d4 copper pieces become this many ingots (about one per three pieces, 1 to 3). */
    public static int copperIngots(int d4a, int d4b) {
        int pieces = clamp(d4a, 1, 4) + clamp(d4b, 1, 4);
        return Math.max(1, (pieces + 1) / 3);
    }

    /** A bonus that may not exceed {@code cap} against a player (the relics' PvP-safe rule), whole against anything else. */
    public static float bonusVs(float bonus, boolean victimIsPlayer, float playerCap) { return victimIsPlayer ? Math.min(bonus, playerCap) : bonus; }

    /** The Buster Sword is a two-handed weapon: with the off hand full the blow is a fifth weaker. */
    public static float twoHanded(float damage, boolean offHandBusy) { return offHandBusy ? damage * 0.8f : damage; }

    /** Feeding Frenzy: the level after one more death (current -1 = not in a frenzy, so the first is level 0; capped at 2 = level III). */
    public static int frenzyLevel(int current) { return Math.min(2, Math.max(-1, current) + 1); }

    /** Is the light level "dim light or darkness" (the Duality Blade's blade of darkness)? */
    public static boolean dim(int rawBrightness) { return rawBrightness <= 7; }

    /** Duality Blade bonus damage: {light mode, dim and dark mode, otherwise} = 1d4+2 radiant / 1d4+2 necrotic / plain 1d4+1 each. */
    public static float dualityBonus(boolean streamers, boolean dim) { return streamers ? 3.5f : dim ? 3.5f : 2.0f; }

    /** Shadow Stalk lands behind the target: the point {@code dist} blocks behind (tx,tz) looking along (fx,fz). */
    public static double[] behind(double tx, double tz, double fx, double fz, double dist) {
        double l = Math.sqrt(fx * fx + fz * fz);
        if (l < 1.0e-6) return new double[]{tx, tz + dist};
        return new double[]{tx - fx / l * dist, tz - fz / l * dist};
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
