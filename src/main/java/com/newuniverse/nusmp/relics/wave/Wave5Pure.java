package com.newuniverse.nusmp.relics.wave;

import java.util.HashMap;
import java.util.Map;

/**
 * Wave 5 (the seven neck relics): every rule that needs no Minecraft type, so it can be tested alone. Charge pools and their daily refill
 * (Amulet of the Warmage, The Amulet of Lost Souls), the eight souls and the roll that frees one, the three tiers of Hemlock's Amulet, the
 * lunar calendar and the inventory trim of the Amulet of Safeguarding, the lethal-hit rule of the Dragon Rage form, the Huntress' bonus.
 * Dice are rolled by the caller and passed in, so nothing here is random.
 */
public final class Wave5Pure {
    private Wave5Pure() {}

    public static final long DAY = 24000L;
    /** One lunar cycle of the Overworld sky (the moon has eight phases, one per day). */
    public static final long MONTH_DAYS = 8L;
    /** The most one spell of these relics may do to one target (30% of a 20 HP player); only an EP-priced card may go above it. */
    public static final float DAMAGE_CAP = 6.0f;

    // ---------------------------------------------------------------- charge pools
    public static final int WARMAGE_MAX = 4;
    public static final int WARMAGE_DIE = 4;
    public static final int SOULS_MAX = 10;
    public static final int SOULS_DIE = 10;
    public static final int SOUL_COUNT = 8;
    public static final int SOUL_FREE_DIE = 20;

    public static long day(long dayTime) { return Math.max(0L, dayTime) / DAY; }

    /** The charges after the world clock may have rolled into a new day: a new day gives back {@code roll} (1..die) charges, up to the cap. */
    public static int regen(int charges, int max, long lastDay, long today, int roll) {
        int c = Math.max(0, Math.min(max, charges));
        if (today <= lastDay) return c;
        return Math.min(max, c + Math.max(1, roll));
    }

    public static boolean canAfford(int charges, int cost) { return cost > 0 && charges >= cost; }

    /** The last charge of the Amulet of Lost Souls was spent: a natural 1 on the d20 frees the soul that was being used. */
    public static boolean freesSoul(int chargesAfter, int d20) { return chargesAfter <= 0 && d20 == 1; }

    // ---------------------------------------------------------------- the eight souls
    /** Comos, Helbi, Hurod, Ilia, Klaroven, Mantra, Prammac, Vagan: the order of the card. */
    public static final String[] SOUL_NAMES = {
            "Comos Parolath", "Helbi Finemane", "Hurod", "Ilia", "Klaroven of the Torch", "Mantra Tarlem", "Prammac Caerkul", "Vagan Nightfist"};
    /** Who they were (the card's own words). */
    public static final String[] SOUL_KINDS = {
            "Male Triton", "Female Gnome", "Male Half-Orc", "Female Yuan-Ti", "Male Elf", "Female Tiefling", "Female Dragonborn", "Male Goliath"};
    /** Each soul's three spells, for a cost of 1, 2 and 3 charges. */
    public static final String[][] SOUL_SPELLS = {
            {"Witch Bolt", "Misty Step", "Lightning Bolt"},
            {"Healing Word", "Lesser Restoration", "Spirit Guardians"},
            {"Thunderwave", "Shatter", "Thunder Step"},
            {"Ray of Sickness", "Dragon's Breath", "Aura of Vitality"},
            {"Inflict Wounds", "Ray of Enfeeblement", "Vampiric Touch"},
            {"Burning Hands", "Scorching Ray", "Fireball"},
            {"Tasha's Caustic Brew", "Melf's Acid Arrow", "Vitriolic Sphere"},
            {"Ice Knife", "Snilloc's Snowball Swarm", "Spirit Shroud"}};

    public static boolean isFreed(int mask, int soul) { return soul >= 0 && soul < SOUL_COUNT && (mask & (1 << soul)) != 0; }

    public static int free(int mask, int soul) { return soul < 0 || soul >= SOUL_COUNT ? mask : mask | (1 << soul); }

    public static boolean allFreed(int mask) { return (mask & ((1 << SOUL_COUNT) - 1)) == (1 << SOUL_COUNT) - 1; }

    /** The next soul after {@code from} that is still in the amulet (wrapping round), or -1 when every one has been freed. */
    public static int nextSoul(int mask, int from) {
        for (int i = 1; i <= SOUL_COUNT; i++) {
            int s = Math.floorMod(from + i, SOUL_COUNT);
            if (!isFreed(mask, s)) return s;
        }
        return -1;
    }

    // ---------------------------------------------------------------- Hemlock's Amulet
    public static final int AWAKEN_AT = 60;
    public static final int EXALT_AT = 240;
    public static final int ENERGY_CAP = 999;

    /** 0 dormant, 1 Awakened, 2 Exalted. */
    public static int tier(int energy) { return energy >= EXALT_AT ? 2 : energy >= AWAKEN_AT ? 1 : 0; }

    public static int addEnergy(int energy, int gain) { return Math.max(0, Math.min(ENERGY_CAP, energy + Math.max(0, gain))); }

    /** How long the Mask of the Living lasts. */
    public static int veilTicks(int tier) { return tier >= 2 ? 12000 : tier == 1 ? 9600 : 6000; }

    public static double healthBonus(int tier) { return tier >= 2 ? 8.0 : tier == 1 ? 4.0 : 0.0; }

    public static double toughnessBonus(int tier) { return tier >= 2 ? 4.0 : tier == 1 ? 2.0 : 0.0; }

    /** Tensura's spiritual health (the soul): the Reliquary holds more of it (a fraction of the wearer's base). */
    public static double soulBonus(int tier) { return tier >= 2 ? 0.5 : tier == 1 ? 0.25 : 0.0; }

    /** The Reliquary saves the wearer from one lethal blow a day. */
    public static final long RELIQUARY_COOLDOWN = DAY;

    // ---------------------------------------------------------------- the Amulet of Safeguarding
    public static final int MEDITATE_TICKS = 600;
    public static final double MEDITATE_STILL = 1.6;
    /** After it breaks the amulet needs this many full moons to mend (counted in cycles). */
    public static final long MEND_CYCLES = 2L;

    public static int moonPhase(long dayTime) { return (int) Math.floorMod(day(dayTime), MONTH_DAYS); }

    /** The full moon (phase 0) and the dark hours of its night (13000 to 23000 of its day). */
    public static boolean fullMoonNight(long dayTime) {
        long t = Math.floorMod(Math.max(0L, dayTime), DAY);
        return moonPhase(dayTime) == 0 && t >= 13000L && t <= 23000L;
    }

    /** Which lunar month the clock is in. */
    public static long cycle(long dayTime) { return day(dayTime) / MONTH_DAYS; }

    /** Once each month during the full moon: not yet recorded this month, and not still mending. */
    public static boolean canRecord(long dayTime, long lastCycle, long mendCycle) {
        return fullMoonNight(dayTime) && cycle(dayTime) > lastCycle && cycle(dayTime) >= mendCycle;
    }

    public static long mendsAt(long dayTime) { return cycle(dayTime) + MEND_CYCLES; }

    public static boolean heldStill(double dx, double dy, double dz) { return dx * dx + dy * dy + dz * dz <= MEDITATE_STILL * MEDITATE_STILL; }

    /**
     * What the recorded self no longer owns: for each slot in order, how many of its items were not on the record (so they stay behind).
     * The record only ever takes items away from the survivor, never adds one, so it can not be used to copy items.
     */
    public static int[] excess(String[] ids, int[] counts, boolean[] exempt, Map<String, Integer> recorded) {
        int[] out = new int[ids.length];
        Map<String, Integer> left = new HashMap<>(recorded);
        for (int i = 0; i < ids.length; i++) {
            if (counts[i] <= 0 || ids[i] == null) continue;
            if (exempt[i]) continue;
            int have = left.getOrDefault(ids[i], 0);
            int keep = Math.min(counts[i], have);
            left.put(ids[i], have - keep);
            out[i] = counts[i] - keep;
        }
        return out;
    }

    // ---------------------------------------------------------------- the Amulet of Dragon Rage
    public static final int FORM_TICKS = 2400;
    public static final long FORM_COOLDOWN = DAY;
    /** The dragon's own hit points (as absorption). */
    public static final float DRAGON_POOL = 40.0f;

    public enum Lethal { NONE, REVERT, UNCONSCIOUS, DIE }

    /**
     * A hit on the dragon: it first spends the dragon's pool; what is left is the "remaining damage". Nothing left: the form holds. Some left: the
     * form ends; if the remainder is more than the dragon's whole pool it is instant death, if it would take the survivor to 0 they are left
     * unconscious (1 HP, stunned), otherwise they simply take it.
     */
    public static Lethal dragonHit(float newDamage, float absorption, float health, float pool) {
        float remaining = newDamage - Math.max(0f, absorption);
        if (remaining <= 0f) return Lethal.NONE;
        if (remaining > pool) return Lethal.DIE;
        if (remaining >= health) return Lethal.UNCONSCIOUS;
        return Lethal.REVERT;
    }

    /** The damage that leaves exactly 1 HP. */
    public static float leaveOneHp(float absorption, float health) { return Math.max(0f, Math.max(0f, absorption) + health - 1.0f); }

    // ---------------------------------------------------------------- the Amulet of the Huntress
    /** Slashing / piercing bonus: +1, doubled for claws and teeth; the unseen pounce adds more. */
    public static float huntressBonus(boolean natural, boolean pounce) { return (natural ? 2.0f : 1.0f) + (pounce ? 2.0f : 0.0f); }

    /** Stealth: a sneaking wearer is only noticed within this many blocks. */
    public static final double HUNTRESS_SEEN_WITHIN = 8.0;

    // ---------------------------------------------------------------- the cantrips of the Warmage
    /** Cantrips scale with level as usual: +1 damage per 10 levels up to +2, never above the cap. */
    public static float cantrip(float base, int experienceLevel) { return Math.min(DAMAGE_CAP, base + Math.min(2, Math.max(0, experienceLevel) / 10)); }

    public static float cap(float damage) { return Math.max(0f, Math.min(DAMAGE_CAP, damage)); }
}
