package com.newuniverse.nusmp.relics.wave;

/**
 * The arithmetic of wave 12 (ten hand-held weapons), kept free of Minecraft types so it is unit-tested: the Giant Jawblade's charge multiplier,
 * the Lumberjack's falling-tree damage, the Warmage's daily missiles, the Sword Crystal's arm and moonlit recharge, the hook swords' "prone"
 * rule, the Stabshroom's natural 1 and 20. Every D&amp;D die of the cards is turned into a fixed or short random number here.
 */
final class Wave12Rules {
    private Wave12Rules() {}

    // ---------------------------------------------------------------- Giant Jawblade
    static final int JAW_MAX_CHARGES = 3;
    /** Ticks a charge lasts after the last one was added. */
    static final int JAW_WINDOW = 200;

    /** 3d6 becomes 4d6, 5d6, 6d6 at one, two, three charges: the damage is multiplied by (3 + charges) / 3. */
    static double jawMultiplier(int charges) {
        int c = Math.max(0, Math.min(JAW_MAX_CHARGES, charges));
        return (3.0 + c) / 3.0;
    }

    /** The charged blow on a player is limited: it may add at most this share of the victim's maximum health on top of the plain blow. */
    static final double PLAYER_BONUS_SHARE = 0.15;

    /** The amount after a bonus, with the bonus held to {@code share} of {@code maxHealth} when the victim is a player. */
    static float boundedBonus(float base, float boosted, float maxHealth, boolean victimIsPlayer, double share) {
        if (boosted <= base || !victimIsPlayer) return Math.max(base, boosted);
        float room = (float) (maxHealth * share);
        return base + Math.min(boosted - base, room);
    }

    // ---------------------------------------------------------------- The Lumberjack
    static final int TREE_MAX_LOGS = 96, TREE_RADIUS = 8, TREE_UP = 40, TREE_DOWN = 2;

    /** 2d6 (7) where the trunk stood, 1d6 (3.5) more for every 20 feet (6 blocks) away from the stump, at most 24. */
    static float treeDamage(double blocksFromStump) {
        double steps = Math.floor(Math.max(0.0, blocksFromStump) / 6.0);
        return (float) Math.min(24.0, 7.0 + 3.5 * steps);
    }

    /** Is {@code leavesTouching} (natural leaves next to the logs) enough for the logs to be a tree and not somebody's house? */
    static boolean looksLikeTree(int logs, int naturalLeaves) { return logs >= 2 && naturalLeaves >= 3; }

    // ---------------------------------------------------------------- Whirling Blades
    /** Even blows are the cold blade's, odd the hot one's. */
    static boolean coldBlade(int blow) { return (blow & 1) == 0; }

    // ---------------------------------------------------------------- Warmage's Longsword
    static final int MISSILES_PER_DAY = 5;

    /** Missiles left: the stored count if it was written today, a full sword at a new dawn. */
    static int missilesLeft(int stored, long storedDay, long today, boolean hasStored) {
        if (!hasStored || storedDay != today) return MISSILES_PER_DAY;
        return Math.max(0, Math.min(MISSILES_PER_DAY, stored));
    }

    /** One missile: 1d4 + 1 with the given die roll (1..4). */
    static float missileDamage(int d4) { return Math.max(1, Math.min(4, d4)) + 1.0f; }

    /** Deals {@code count} missiles round-robin over {@code targets} foes: the number each one gets. */
    static int[] spread(int count, int targets) {
        int[] out = new int[Math.max(0, targets)];
        if (out.length == 0) return out;
        for (int i = 0; i < count; i++) out[i % out.length]++;
        return out;
    }

    // ---------------------------------------------------------------- Sword Crystal
    static final int CRYSTAL_CHARGES = 4;
    static final float CRYSTAL_HP = 273f;
    static final float CRYSTAL_BLOW_COST = 11f;       // 273 / 11: about 25 blows before the arm shatters
    static final int CRYSTAL_TICKS = 1200;

    static float crystalAfterBlow(float hp) { return Math.max(0f, hp - CRYSTAL_BLOW_COST); }

    /** 2d4 of cold on the wielder when the arm breaks on its last hit (2..8). */
    static float shatterBackfire(int d4a, int d4b) { return Math.max(1, Math.min(4, d4a)) + Math.max(1, Math.min(4, d4b)); }

    /** The backfire never kills: it leaves at least one point of health. */
    static float safeBackfire(float amount, float health) { return Math.max(0f, Math.min(amount, health - 1f)); }

    /** Night is from 13000 to 23000 of the day (the moon is up). */
    static boolean moonlit(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t <= 23000;
    }

    /** The number of the night a time belongs to (the evening and the early morning that follows are one night). */
    static long nightIndex(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        long day = Math.floorDiv(dayTime, 24000L);
        return t >= 13000 ? day : day - 1;
    }

    /** One charge per night in the moonlight, never above the maximum. */
    static boolean regains(int charges, long lastNight, long thisNight) { return charges < CRYSTAL_CHARGES && thisNight != lastNight; }

    // ---------------------------------------------------------------- Twin Hook Swords
    /** The kind of foe the hook can take off its feet: a tall narrow body (two legs) or something that flies. */
    static boolean hookable(double width, double height, boolean flies) {
        if (flies) return true;
        return height >= 1.2 && height >= width * 1.8;
    }

    /** The hook's "Strength check": it fails against something far bigger than the wielder. */
    static boolean hookHolds(float victimMaxHealth, boolean boss) { return !boss && victimMaxHealth <= 80f; }

    static final int CLIMB_MAX_TICKS = 80;

    // ---------------------------------------------------------------- The Stabshroom
    /** A d20 of the card: 1 is the spore burst, 20 the double blow. */
    static boolean fumble(int d20) { return d20 == 1; }

    static boolean critical(int d20) { return d20 == 20; }

    // ---------------------------------------------------------------- Disintegration Dagger
    static final double BLAST_RADIUS = 4.0;                // 10 feet is three blocks; four blocks leaves room for the corpse's own width
    static final int BLAST_DELAY = 12;

    /** 6d4 fire (15) shrunk to 6, and half of it for those who "make the save" by standing back. */
    static float blastDamage(double distance) { return distance <= 2.0 ? 6.0f : 3.0f; }

    // ---------------------------------------------------------------- Bottle Blade
    /** An empty bottle is a club: its blow is this share of a full one. */
    static final float EMPTY_BLOW = 0.4f;
}
