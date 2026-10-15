package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;
import java.util.Random;
import java.util.function.IntUnaryOperator;

/**
 * Why: the cards are tabletop (charges that come back "at dawn", dice, spell levels, "advantage", feet, damage that is "doubled after the dice").
 * This is where each of those becomes a Minecraft number, in one place, with the same scale as the earlier waves: a long rest is a Minecraft day,
 * a short rest a few minutes, one round 6 seconds, 5 feet one block.
 * <p>
 * Relics of Many Worlds, part 4, wave 22 (twelve staves and wands): the pure rules, with no Minecraft types, so they are unit-tested.
 */
public final class Wave22Rules {
    private Wave22Rules() {}

    private static final long DAY = 24000L;

    /** The Minecraft day a day-time falls in (the "dawn" counter every charge wand runs on). */
    public static long day(long dayTime) { return Math.floorDiv(dayTime, DAY); }

    // ================================================================ charges that come back at dawn
    /** The five wands with charges: how many they hold and how they refill. */
    public enum Kind {
        SERPENT(10), EMBER(3), APPRENTICE(5), SPIDER(8), WISP(7);

        private final int max;

        Kind(int max) { this.max = max; }

        public int max() { return max; }
    }

    /** What a wand remembers: charges left, the day it was last brought up to date, and whether it sulks until the next dawn. */
    public record Store(int charges, long day, boolean locked) {}

    /** A new wand is full. */
    public static Store fresh(Kind k, long today) { return new Store(k.max(), today, false); }

    /** The result of a dawn: the new store and whether the wisp wand's refill roll came up 0 (it sulks). */
    public record Dawn(Store store, boolean sulked, int gained) {}

    /**
     * Brings a store up to the given day. Each elapsed dawn (at most 7 are counted) unlocks the wand and refills it by the card's rule:
     * Serpent Head Staff 1d6+4, Staff of Ember Visions back to full, Staff of the Apprentice 1d4, Wand of Spider Summoning 1d6+1, and the Wand-o-Wisp
     * only when it is empty: d8-1 (a 0 means it sulks that day and refills nothing). {@code roll.applyAsInt(n)} returns 1..n.
     */
    public static Dawn dawn(Kind k, Store s, long today, IntUnaryOperator roll) {
        if (today <= s.day()) return new Dawn(s, false, 0);
        int c = s.charges();
        int gained = 0;
        boolean sulked = false;
        long nights = Math.min(7L, today - s.day());
        for (long i = 0; i < nights; i++) {
            int add = switch (k) {
                case SERPENT -> roll.applyAsInt(6) + 4;
                case EMBER -> k.max();
                case APPRENTICE -> roll.applyAsInt(4);
                case SPIDER -> roll.applyAsInt(6) + 1;
                case WISP -> {
                    if (c > 0) yield 0;
                    int r = roll.applyAsInt(8) - 1;
                    if (r == 0) sulked = true;
                    yield r;
                }
            };
            int next = Math.min(k.max(), c + add);
            gained += next - c;
            c = next;
        }
        return new Dawn(new Store(c, today, false), sulked, gained);
    }

    public static boolean canSpend(Store s, int n) { return !s.locked() && n > 0 && s.charges() >= n; }

    /** The store after spending n charges (unchanged when it cannot). */
    public static Store spend(Store s, int n) { return canSpend(s, n) ? new Store(s.charges() - n, s.day(), false) : s; }

    /** Spending the last charge of the Serpent Head Staff or the Spider wand risks the card's d20: a 1 puts the wand to sleep until the next dawn. */
    public static boolean lastChargeLocks(int d20) { return d20 == 1; }

    public static Store lock(Store s) { return new Store(s.charges(), s.day(), true); }

    // ================================================================ the Scepter of the Double Moon
    public enum Element { FIRE, COLD, LIGHTNING, RADIANT, NECROTIC, THUNDER, FORCE, OTHER }

    public enum Sky { DAY, NIGHT, TWILIGHT }

    /**
     * The element of a damage type id ("minecraft:in_fire", "tensura:holy_damage", "tensura:darkness_elemental" ...), by the words in its path.
     * Lightning is read before light, so a thunderbolt is never counted as radiant.
     */
    public static Element element(String damageTypeId) {
        if (damageTypeId == null) return Element.OTHER;
        String s = damageTypeId.toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        if (has(s, "lightning", "electr", "shock", "spark", "plasma")) return Element.LIGHTNING;
        if (has(s, "sonic", "sound", "thunder", "voice")) return Element.THUNDER;
        if (has(s, "fire", "flame", "lava", "burn", "inferno", "hell", "magma", "heat", "explosion")) return Element.FIRE;
        if (has(s, "freez", "ice", "cold", "frost", "blizzard", "snow", "chill")) return Element.COLD;
        if (has(s, "holy", "radiant", "light", "solar", "sun")) return Element.RADIANT;
        if (has(s, "dark", "wither", "necro", "shadow", "curse", "soul", "drain", "death")) return Element.NECROTIC;
        if (has(s, "magic", "force", "arcane", "mana")) return Element.FORCE;
        return Element.OTHER;
    }

    private static boolean has(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }

    /** Day or night by the time of day (0 sunrise ... 12999 day, 13000 to 22999 night); a place with no clear day and night is Twilight. */
    public static Sky sky(boolean noClearDayNight, long dayTime) {
        if (noClearDayNight) return Sky.TWILIGHT;
        long t = Math.floorMod(dayTime, DAY);
        return t < 13000L || t >= 23000L ? Sky.DAY : Sky.NIGHT;
    }

    /** The card's multiplier: 2.0 doubled, 0.5 halved, 1.0 untouched. */
    public static double moonScale(Sky sky, Element e) {
        return switch (sky) {
            case DAY -> switch (e) {
                case COLD -> 0.5;
                case FIRE, RADIANT, THUNDER -> 2.0;
                default -> 1.0;
            };
            case NIGHT -> switch (e) {
                case FIRE -> 0.5;
                case COLD, NECROTIC, LIGHTNING -> 2.0;
                default -> 1.0;
            };
            case TWILIGHT -> switch (e) {
                case COLD, NECROTIC, RADIANT, THUNDER -> 2.0;
                case FIRE, FORCE, LIGHTNING -> 0.5;
                default -> 1.0;
            };
        };
    }

    /** "Double the damage after all damage dice are rolled. Round up." */
    public static float moonScaled(float amount, double scale) { return (float) Math.ceil(amount * scale); }

    /** Against a player the doubled blow may take at most this share of their maximum health (the relics' PvP cap). */
    public static float capVsPlayer(float amount, float maxHealth) { return Math.min(amount, maxHealth * 0.45f); }

    // ================================================================ the Lich Wand
    public static final int LICH_POOL = 8, LICH_SLOTS = 3;

    /**
     * The three necromancy spells stored in the wand at dawn: three different indices of the pool of eight, drawn from the owner and the day
     * (so they do not change if the world is reloaded the same day).
     */
    public static int[] dailySpells(long seed, long day) {
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + day * 0xBF58476D1CE4E5B9L);
        int[] out = new int[LICH_SLOTS];
        boolean[] taken = new boolean[LICH_POOL];
        for (int i = 0; i < LICH_SLOTS; i++) {
            int pick;
            do pick = r.nextInt(LICH_POOL); while (taken[pick]);
            taken[pick] = true;
            out[i] = pick;
        }
        return out;
    }

    public static boolean slotUsed(int mask, int slot) { return (mask >> slot & 1) != 0; }

    /** The first slot not yet cast today, or -1 when the wand is spent. */
    public static int nextSlot(int mask) {
        for (int i = 0; i < LICH_SLOTS; i++) if (!slotUsed(mask, i)) return i;
        return -1;
    }

    /** Healing a Lich Wand wielder receives grows back stronger (flesh regrows) but gaunt: at most once per this many ticks it leaves them weak. */
    public static final float LICH_HEAL_BONUS = 1.15f;
    public static final int LICH_GAUNT_GAP = 200, LICH_GAUNT_TICKS = 100, LICH_FOOD_FLOOR = 8;

    // ================================================================ the Staff of Blood Magic
    /** Spell levels of the three casts. */
    public static final int[] BLOOD_LEVELS = {1, 3, 5};
    public static final float[] BLOOD_DAMAGE = {5.0f, 11.0f, 20.0f};

    /**
     * What a cast costs in health: "1d10 HP per spell level" is {@code level} rolls of a d10, each worth a quarter heart (scaled to the maximum
     * health so a 40 HP wielder pays the same share), never more than 60% of the maximum health and never enough to kill: at least 2 health is left.
     */
    public static float bloodCost(int level, float maxHealth, float health, IntUnaryOperator roll) {
        int pips = 0;
        for (int i = 0; i < Math.max(1, level); i++) pips += roll.applyAsInt(10);
        float cost = pips * 0.25f * (maxHealth / 20.0f);
        cost = Math.min(cost, maxHealth * 0.6f);
        return Math.max(0f, Math.min(cost, health - 2.0f));
    }

    // ================================================================ the Staff of Ember Visions
    public enum Form {
        HUMANOID(3.5, 3.0f), BEAST(6.0, 2.0f), BIRD(14.0, 0.0f);

        private final double radius;
        private final float damage;

        Form(double radius, float damage) { this.radius = radius; this.damage = damage; }

        public double radius() { return radius; }

        public float damage() { return damage; }

        /** Reshaping goes humanoid, beast, bird, and round again. */
        public Form next() { return values()[(ordinal() + 1) % values().length]; }

        public static Form of(int ordinal) { return values()[Math.floorMod(ordinal, values().length)]; }
    }

    /** The flame lasts 10 minutes. */
    public static final int EMBER_TICKS = 12000, EMBER_RATE = 20;

    // ================================================================ the Staff of the Apprentice
    public static final String[] SCHOOLS = {"Abjuration", "Conjuration", "Divination", "Enchantment", "Evocation", "Illusion", "Necromancy", "Transmutation"};
    public static final String[][] SPELLS = {
            {"Shield", "Protection from Good and Evil"}, {"Fog Cloud", "Grease"}, {"Detect Magic", "Comprehend Languages"}, {"Sleep", "Charm Person"},
            {"Magic Missile", "Burning Hands"}, {"Silent Image", "Color Spray"}, {"Ray of Sickness", "Cause Fear"}, {"Feather Fall", "Jump"}};

    /** The school of a d8 roll (1..8; other numbers wrap). */
    public static int school(int d8) { return Math.floorMod(d8 - 1, SCHOOLS.length); }

    public static final int WEEK = 7;

    /** Once a week the staff may be re-studied (its school rolled again). {@code last} is the day of the last study, or a negative number for never. */
    public static boolean weekReady(long last, long today) { return last < 0 || today - last >= WEEK; }

    // ================================================================ the Wand-o-Wisp
    public static final int WISP_TICKS = 1200, WISP_MAX = 2, WISP_COST = 3, WISP_RATE = 20;
    public static final float WISP_DAMAGE = 3.0f, FAERIE_BONUS = 1.15f;
    public static final int FAERIE_TICKS = 400;

    /** The sum of wisps after a summon: at most WISP_MAX (the oldest is replaced). */
    public static int wispCount(int current) { return Math.min(WISP_MAX, Math.max(0, current) + 1); }

    // ================================================================ the Wand of Spider Summoning
    /** A summon: the entity type, a vanilla fallback, how many, the charge cost. */
    public record Brood(String type, String fallback, int count, int cost) {}

    public static final Brood SPIDER = new Brood("minecraft:spider", "minecraft:spider", 1, 1);
    public static final Brood SWARM = new Brood("minecraft:cave_spider", "minecraft:cave_spider", 4, 2);
    public static final Brood LARGE = new Brood("tensura:black_spider", "minecraft:spider", 1, 3);
    public static final int BROOD_TICKS = 1200;

    // ================================================================ the Dreamcore Woven Wand
    public static final int DREAMS_PER_DAY = 3;

    public enum Dream { NIGHTMARE, NEUTRAL, SWEET }

    /** A creature's dream by what is on it: harmful effects only is a nightmare, helpful only a sweet dream, anything else neutral. */
    public static Dream dreamOf(int harmful, int helpful) {
        if (harmful > 0 && helpful == 0) return Dream.NIGHTMARE;
        if (helpful > 0 && harmful == 0) return Dream.SWEET;
        if (harmful > helpful) return Dream.NIGHTMARE;
        if (helpful > harmful) return Dream.SWEET;
        return Dream.NEUTRAL;
    }

    // ================================================================ the Superlunary Staff
    public static final int PROJECTION_PLACES = 3;
    public static final double SUNBURST_RADIUS = 8.0;
    public static final int SUNBURST_TICKS = 120;

    /** Blindness the Superlunary Staff causes lasts half again as long (the card's disadvantage on every save, as a longer hold). */
    public static int blindTicks(int base) { return (int) Math.min(20 * 20, Math.round(base * 1.5)); }

    /** An eight-point compass word for an offset (x east, z south). */
    public static String bearing(double dx, double dz) {
        String[] names = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        double ang = Math.toDegrees(Math.atan2(dz, dx));
        int idx = (int) Math.round(ang / 45.0);
        return names[Math.floorMod(idx, 8)];
    }

    // ================================================================ the Bifrost
    public static final int GATE_WEEK = 7;

    /** The Gate may be opened once a week. */
    public static boolean gateReady(long last, long today) { return last < 0 || today - last >= GATE_WEEK; }

    /** Days (rounded up) until the Gate answers again. */
    public static int gateDaysLeft(long last, long today) { return last < 0 ? 0 : (int) Math.max(0, GATE_WEEK - (today - last)); }

    // ================================================================ the Windwheel Staff
    /** Charges last one minute from the last one gained; at most this many are stored. */
    public static final int WIND_TICKS = 1200, WIND_MAX = 10;
    /** A "spell" is a drop of the magicule pool of at least this share of the maximum; its level is one per two percent spent (1 to 5). */
    public static final double WIND_MIN_SPEND = 0.01;

    /** The charges a spell stores: its level, one per two percent of the maximum magicules spent, 1 to 5; 0 when the drop is too small to be a spell. */
    public static int windStored(double spentFraction) {
        if (spentFraction < WIND_MIN_SPEND) return 0;
        return (int) Math.max(1, Math.min(5, Math.ceil(spentFraction / 0.02)));
    }

    /** The new total after a spell: capped. */
    public static int windTotal(int current, int stored) { return Math.min(WIND_MAX, Math.max(0, current) + Math.max(0, stored)); }

    /** 1d6 of lightning per charge (an average of 3.5 each), halved when the target saves, never more than 40. */
    public static float windDamage(int charges, boolean saved) {
        float d = Math.min(40f, Math.max(0, charges) * 3.5f);
        return saved ? d / 2.0f : d;
    }

    /** Are the stored charges still there? */
    public static boolean windAlive(long now, long until) { return until > now; }
}
