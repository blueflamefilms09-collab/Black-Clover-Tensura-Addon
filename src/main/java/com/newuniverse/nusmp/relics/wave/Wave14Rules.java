package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/**
 * Wave 14 (heads and footwear): the numbers and small state machines behind the seventeen cards, kept free of Minecraft so they can be tested.
 * The glue ({@link Wave14}, {@link Wave14Head}, {@link Wave14Feet}, {@link Wave14Zones}) only reads and writes the values these produce.
 * <ul>
 *   <li>{@link Breath}: the three dragon circlets' breath (damage, element, cone) and the bonus for carrying more than one circlet.</li>
 *   <li>{@link Pool}: charge pools that come back at dawn (or at midnight) by a dice roll.</li>
 *   <li>{@link Flock}: the Tiny Duck Crown's morning roll and the ducks that assist.</li>
 *   <li>{@link Mark}: the places the Ruby Boots know; {@link HighGround}: the Boots of the High Ground's minute.</li>
 *   <li>{@link Zone} and {@link ZoneBook}: patches of mud, dust devils and wind walls that stay in the world for a while.</li>
 *   <li>{@link Spell}: the eight spells of the Greaves of the Wind Walker and what each costs.</li>
 * </ul>
 * Tabletop became Minecraft: a long rest is a Minecraft day (dawn), a saving throw is a chance or an existence (EP) check, a bonus action is a
 * short cooldown, feet become blocks (5 ft = 1.5 blocks), dice are rolled here with the caller's own random source.
 */
final class Wave14Rules {
    private Wave14Rules() {}

    static final int DAY = 24000, MIDNIGHT_SHIFT = 6000;

    // The named switches the cards carry (Passive.flag) and the handlers ask for. Every card also carries F_ANY, so a handler can leave early.
    static final String F_ANY = "w14_any", F_GOLD = "w14_gold", F_SILVER = "w14_silver", F_STEEL = "w14_steel", F_DUCK = "w14_duck",
            F_UNICORN = "w14_unicorn", F_DARK = "w14_dark_unicorn", F_PHOENIX = "w14_phoenix", F_CONJURER = "w14_conjurer", F_DIADEM = "w14_diadem",
            F_RUBY = "w14_ruby", F_INDOM = "w14_indomitable", F_MUD = "w14_mud", F_HIGH = "w14_high_ground", F_WIND = "w14_wind_walker",
            F_WATER = "w14_waterwalkers", F_SNEAK = "w14_sneaking", F_FIRE = "w14_dragon_fire_boots";

    // ------------------------------------------------------------------ the clock
    /** The day number of a world clock reading (turns over at dawn, like the sun). */
    static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    /** The same but turning over at midnight (18000), for the Dark Unicorn Crown. */
    static long midnightDayOf(long dayTime) { return Math.floorDiv(dayTime + MIDNIGHT_SHIFT, (long) DAY); }

    /** What is stored for a daily use: 0 for never, else the day it was last used plus one. */
    static long dailyMark(long today) { return today + 1; }

    static boolean dailyReady(long stored, long today) { return stored == 0 || stored - 1 < today; }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ------------------------------------------------------------------ dice
    /** {@code n} dice with {@code sides} sides; {@code rng.applyAsInt(bound)} gives 0 (inclusive) to bound (exclusive). */
    static int dice(IntUnaryOperator rng, int n, int sides) {
        int sum = 0;
        for (int i = 0; i < Math.max(0, n); i++) sum += 1 + rng.applyAsInt(Math.max(1, sides));
        return sum;
    }

    // ------------------------------------------------------------------ the dragon circlets
    enum Breath {
        FIRE("fire", 0xFF7A1C, "minecraft:in_fire"), COLD("cold", 0x9FE4FF, "minecraft:freeze"), THUNDER("thunder", 0xE8E27A, "minecraft:sonic_boom");

        final String label;
        final int rgb;
        final String damageType;

        Breath(String label, int rgb, String damageType) {
            this.label = label;
            this.rgb = rgb;
            this.damageType = damageType;
        }
    }

    static final double BREATH_RANGE = 9.0, BREATH_COS_HALF = 0.82;                // a cone of about 35 degrees either side of the look
    static final float BREATH_BASE = 8.0f;
    static final int BREATH_COOLDOWN = 900, BREATH_BURN_TICKS = 100;

    /** How many different dragon circlets (up to 3) the wearer carries; each one beyond the worn one is "combined" and strengthens the breath. */
    static int circletsCombined(boolean gold, boolean silver, boolean steel) { return (gold ? 1 : 0) + (silver ? 1 : 0) + (steel ? 1 : 0); }

    /** The breath's damage: 8, plus a quarter for every other circlet carried. */
    static float breathDamage(int combined) { return BREATH_BASE * (1.0f + 0.25f * clamp(combined - 1, 0, 2)); }

    /** With all three circlets together the breath carries the other two elements' sting as well. */
    static boolean trinity(int combined) { return combined >= 3; }

    /** Is a point at offset (dx,dy,dz) from the eyes inside the cone around the unit look vector (lx,ly,lz)? */
    static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double cosHalf, double range) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > range) return false;
        if (len < 1.0e-6) return true;
        return (dx * lx + dy * ly + dz * lz) / len >= cosHalf;
    }

    /** Winter's Fury: at or under half health. */
    static boolean wintersFury(float health, float max) { return max > 0f && health * 2f <= max; }

    /** The extra d8 of cold; against a player it is held to 6. */
    static float furyExtra(int d8, boolean targetIsPlayer) { return targetIsPlayer ? Math.min(6, d8) : d8; }

    // Siege Monster: grow to Large for three minutes, once a day; a single blow of 10 or more breaks the concentration
    static final double SIEGE_SCALE = 0.5, SIEGE_DAMAGE = 4.0, SIEGE_BREAK_SPEED = 1.0;
    static final int SIEGE_TICKS = 3600;
    static final float SIEGE_BREAK_HIT = 10.0f;

    static boolean siegeBreaks(float hit) { return hit >= SIEGE_BREAK_HIT; }

    // ------------------------------------------------------------------ charge pools
    /**
     * A stored charge count and the day it last recovered. A count is stored as -1 until first looked at; the pool then starts full. At a new day it
     * gains {@code gain} (never above the maximum); a clock set back owes nothing.
     */
    record Pool(int have, long day) {
        static Pool read(int storedHave, long storedDay, int max, long today) {
            int h = storedHave < 0 ? max : Math.min(storedHave, max);
            long d = storedHave < 0 ? today : storedDay;
            return new Pool(h, d);
        }

        /** The pool as it stands on {@code today}, after a recovery of {@code gain} if a new day has begun. */
        Pool at(long today, int gain, int max) {
            if (today <= day) return new Pool(Math.min(have, max), day);
            return new Pool(Math.min(max, have + Math.max(0, gain)), today);
        }

        boolean canSpend(int cost) { return cost > 0 && have >= cost; }

        Pool spend(int cost) { return new Pool(Math.max(0, have - Math.max(0, cost)), day); }
    }

    static final int UNICORN_CHARGES = 3, DARK_CHARGES = 3, CONJURER_CHARGES = 6, RUBY_CHARGES = 3, WIND_CHARGES = 5;

    /** 1d3 (the unicorn crowns and the Ruby Boots), 1d4+2 (the Conjurer), 1d4+3 (the Greaves). */
    static int regainD3(IntUnaryOperator rng) { return dice(rng, 1, 3); }

    static int regainConjurer(IntUnaryOperator rng) { return dice(rng, 1, 4) + 2; }

    static int regainWind(IntUnaryOperator rng) { return dice(rng, 1, 4) + 3; }

    /** Healing Touch: 2d8 + 2. */
    static int healingTouch(IntUnaryOperator rng) { return dice(rng, 2, 8) + 2; }

    /** Void Beam: 4d6 necrotic, half on a saving throw (rounded down). */
    static float voidBeam(IntUnaryOperator rng, boolean saved) {
        int d = dice(rng, 4, 6);
        return saved ? d / 2 : d;
    }

    static final double VOID_SAVE_CHANCE = 0.4, VOID_RANGE = 36.0;

    // ------------------------------------------------------------------ the Conjurer
    /** Charges the Conjurer pays: 1, plus 0 to 3 more for a stronger creature, plus 2 to skip the ritual. */
    static int conjureCost(int extra, boolean instant) { return 1 + clamp(extra, 0, 3) + (instant ? 2 : 0); }

    /** The strength of the conjured creature: 1 to 4. */
    static int conjurePower(int extra) { return 1 + clamp(extra, 0, 3); }

    static final int CONJURE_RITUAL = 60, CONJURE_LIFE_BASE = 1200, CONJURE_LIFE_PER_POWER = 300;

    /** The creature lives longer for the Conjurer's steady mind: a quarter longer than the base. */
    static int conjureLife(int power) {
        return (int) ((CONJURE_LIFE_BASE + CONJURE_LIFE_PER_POWER * (clamp(power, 1, 4) - 1)) * 1.25);
    }

    // ------------------------------------------------------------------ the Tiny Duck Crown
    enum Habitat {
        NATURAL(4), UNLIKELY(2), HIGHLY_UNLIKELY(1);

        final int dice;

        Habitat(int dice) { this.dice = dice; }
    }

    /** The morning roll (percentile): 50 or higher brings the ducks. */
    static boolean ducksCome(int d100) { return d100 >= 50; }

    /** How many ducks come: that many d8. */
    static int flockSize(Habitat h, IntUnaryOperator rng) { return dice(rng, h.dice, 8); }

    static final int DUCKS_PER_ASSIST = 5, DUCK_FOLLOW_CAP = 24, DUCK_BELL_DAY_END = 18000;

    /** How many times the command word can still be said: the ducks, five at a time. */
    static int assistsLeft(int flock) { return Math.max(0, flock) / DUCKS_PER_ASSIST; }

    static int flockAfterAssist(int flock) { return Math.max(0, flock - DUCKS_PER_ASSIST); }

    /** Is the day's following over? Midnight (18000) ends it. */
    static boolean followOver(long dayTime) { return Math.floorMod(dayTime, (long) DAY) >= DUCK_BELL_DAY_END; }

    // ------------------------------------------------------------------ Ruby Boots of Planestriding
    /** A place the boots know: a dimension id and block coordinates. */
    record Mark(String dim, int x, int y, int z) {}

    static final int MAX_MARKS = 4, PLANE_RITUAL = 60;

    /** Adds a mark, dropping the oldest when there are too many; a repeat of the last mark changes nothing. */
    static List<Mark> addMark(List<Mark> marks, Mark m) {
        List<Mark> out = new ArrayList<>(marks);
        out.removeIf(o -> o.equals(m));
        out.add(m);
        while (out.size() > MAX_MARKS) out.remove(0);
        return out;
    }

    /** The next place in the circle of marks after {@code at}; home is place 0 and the marks follow it. Always in 0..count. */
    static int nextPlace(int at, int marks) { return marks <= 0 ? 0 : Math.floorMod(at + 1, marks + 1); }

    // ------------------------------------------------------------------ Boots of the Indomitable
    static final double FALL_MIN = 6.0;

    /** The fall damage vanilla would deal for this distance (3 blocks are free). */
    static float fallDamage(double distance) { return (float) Math.max(0.0, Math.ceil(distance - 3.0)); }

    /** The boots take a fall over six blocks onto another creature once a day. */
    static boolean indomitableFall(double distance, boolean ready) { return ready && distance >= FALL_MIN; }

    /** The other creature's "dexterity save" against the fall: half of them step aside. */
    static boolean fallSaved(IntUnaryOperator rng) { return rng.applyAsInt(100) < 50; }

    // ------------------------------------------------------------------ Boots of the High Ground
    static final int HIGH_TICKS = 1200, HIGH_REACTION_GAP = 20;
    static final double HIGH_REACH = 3.0, HIGH_STEP = 1.5;
    static final float HIGH_ADVANTAGE = 1.3f, HIGH_REACTION_FACTOR = 0.8f;

    /** The minute on the high ground: it ends when the time is up or the wearer has come down more than a step from where it rose. */
    record HighGround(long until, double startY) {
        boolean over(long now, double y) { return now >= until || y < startY - HIGH_STEP; }
    }

    /** May the reaction strike this creature again (one per creature per second)? */
    static boolean reactionReady(long lastStrike, long now) { return now - lastStrike >= HIGH_REACTION_GAP; }

    // ------------------------------------------------------------------ Waterwalkers and Dragon Fire Boots
    static final int FLIGHT_TICKS = 600, FLIGHT_LAST_WARN = 100;
    static final double CRASH_CHANCE = 0.4;
    static final float CRASH_DAMAGE = 4.0f, FIREBALL_DAMAGE = 11.0f;
    static final double FIREBALL_RANGE = 40.0, FIREBALL_RADIUS = 5.0;

    /** A crash costs at most four health and never kills. */
    static float crashDamage(float health) { return Math.max(0f, Math.min(CRASH_DAMAGE, health - 1.0f)); }

    static boolean crashes(IntUnaryOperator rng) { return rng.applyAsInt(100) < (int) (CRASH_CHANCE * 100); }

    // ------------------------------------------------------------------ patches that stay in the world
    enum ZoneKind { GREASE, DEVIL, WALL }

    /** A patch in the world: mud (slippery), a moving dust devil or a wind wall; {@code dim} is the dimension id, the heading is for devils and walls. */
    record Zone(ZoneKind kind, String dim, double x, double y, double z, double hx, double hz, double radius, long until, UUID owner) {
        boolean live(long now) { return now < until; }

        /** A dust devil moves along its heading (blocks per second); other kinds stay. */
        Zone moved(double seconds) {
            if (kind != ZoneKind.DEVIL) return this;
            return new Zone(kind, dim, x + hx * seconds, y, z + hz * seconds, hx, hz, radius, until, owner);
        }
    }

    static final int MAX_ZONES = 24, ZONE_STEP = 10;
    static final int GREASE_TICKS = 1200, DEVIL_TICKS = 160, WALL_TICKS = 200, WARD_TICKS = 12000;

    /** A bounded list of zones: the oldest goes when it is full. */
    static final class ZoneBook {
        private final List<Zone> zones = new ArrayList<>();

        void add(Zone z) {
            zones.add(z);
            while (zones.size() > MAX_ZONES) zones.remove(0);
        }

        int size() { return zones.size(); }

        boolean isEmpty() { return zones.isEmpty(); }

        /** Drops the zones whose time is up. */
        void prune(long now) { zones.removeIf(z -> !z.live(now)); }

        List<Zone> snapshot() { return List.copyOf(zones); }

        void replace(Zone old, Zone fresh) {
            int i = zones.indexOf(old);
            if (i >= 0) zones.set(i, fresh);
        }

        void clear() { zones.clear(); }
    }

    /** Is a point inside a wind wall? The wall stands across its heading, three blocks thick either way, {@code radius} wide to either side and three blocks high (thick, because an arrow covers three blocks a tick). */
    static boolean inWall(Zone z, double px, double py, double pz) {
        double dx = px - z.x(), dz = pz - z.z();
        double along = dx * z.hx() + dz * z.hz();                                   // through the wall
        double across = -dx * z.hz() + dz * z.hx();                                  // along the wall
        return Math.abs(along) <= 3.0 && Math.abs(across) <= z.radius() && py >= z.y() - 0.5 && py <= z.y() + 3.0;
    }

    // ------------------------------------------------------------------ the Greaves of the Wind Walker
    /** The eight spells and what each costs from the five charges (Gust is a cantrip). */
    enum Spell {
        GUST(0), FEATHER_FALL(1), DUST_DEVIL(2), GUST_OF_WIND(2), WARDING_WIND(2), WIND_WALL(3), CONTROL_WINDS(3), WIND_WALK(5);

        final int cost;

        Spell(int cost) { this.cost = cost; }
    }

    /** Warding Wind softens arrows and bolts by 60%. */
    static final float WARD_KEEP = 0.4f;
    static final int FEATHER_TICKS = 600, WIND_WALK_TICKS = 2400, CONTROL_TICKS = 1200;

    static float wardedProjectile(float amount) { return amount * WARD_KEEP; }

    // ------------------------------------------------------------------ the unicorn crowns' hair
    static final int[] HAIR = {0xFF6FA8, 0xFFB35C, 0xFFF06A, 0x7BE495, 0x5CD6FF, 0x8A7BFF, 0xE08BFF, 0xFFFFFF};

    static int nextHair(int at) { return Math.floorMod(at + 1, HAIR.length); }
}
