package com.newuniverse.nusmp.relics.wave;

/**
 * Why: the tabletop rules of these cards are dice, saving throws and "per long rest" counts; this class holds, in one place, what each of them became
 * (a long rest is a Minecraft day of 24000 ticks, a short rest a few minutes, one round is 6 seconds, 5 ft is one block), so the code that touches the
 * world stays thin and every number can be checked by a plain unit test.
 * <p>
 * Wave 28 (gauntlets, gloves, collars, medallions, pantaloons, two potions): the numbers and small state machines behind the twelve cards, kept free
 * of Minecraft so they can be tested. The glue ({@link Wave28}, {@link Wave28Util}, {@link Wave28Hands}, {@link Wave28Neck}, {@link Wave28Potions},
 * {@link Wave28Events}) only reads and writes the values these produce.
 */
final class Wave28Rules {
    private Wave28Rules() {}

    static final int DAY = 24000;

    // The named switches the cards carry (Passive.flag) and the handlers ask for. Every card of the wave also carries F_ANY, so a handler can leave
    // early for a player who wears none of them.
    static final String F_ANY = "w28_any", F_HELL = "w28_hellfire", F_PROF = "w28_proficiency", F_GAMELON = "w28_gamelon", F_CHROMA = "w28_chromatic",
            F_PENITENT = "w28_penitent", F_ABOLETH = "w28_aboleth", F_FENRIR = "w28_fenrir", F_SUN = "w28_sunheart", F_WITCHER = "w28_witcher",
            F_PANTS = "w28_pantaloons";

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** The day number of a world clock reading (turns over at dawn, like the sun). */
    static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    // ------------------------------------------------------------------ a pool of charges (Hellfire Gauntlet, Sunheart Medallion)
    /**
     * A pool of charges. {@code day} is the world day of the last change plus one (0 for a pool never touched, which reads as full). A dawn that has
     * passed since adds {@code gain} charges for each day gone, up to the maximum (the Sunheart Medallion uses gain 0: it only fills by its own act).
     */
    record Charges(int max, int left, long day) {
        static Charges fresh(int max) { return new Charges(max, max, 0L); }

        Charges at(long today, int gain) {
            if (day <= 0L) return new Charges(max, max, today + 1);
            long gone = today - (day - 1);
            if (gone <= 0L) return this;
            long back = Math.min((long) max, gone * Math.max(0, gain));
            return new Charges(max, (int) Math.min((long) max, left + back), today + 1);
        }

        boolean canSpend(int n) { return n > 0 && left >= n; }

        Charges spend(int n) { return canSpend(n) ? new Charges(max, left - n, day) : this; }

        Charges add(int n) { return new Charges(max, Math.min(max, left + Math.max(0, n)), day); }
    }

    // ------------------------------------------------------------------ Hellfire Gauntlet
    static final int HELL_MAX_CHARGES = 3, HELL_AWAKEN_BLOWS = 60, HELL_FIREBALL = 1, HELL_QUAKE = 1, HELL_AURA = 1, HELL_AURA_TICKS = 1200;
    /** Constitution 18 became a hardy body: the gauntlet's flames only take to a wearer with at least this much maximum health. */
    static final double HELL_HARDY_HEALTH = 24.0;
    static final float FIREBALL_DAMAGE = 12f, QUAKE_FORCE = 7f, QUAKE_FIRE = 7f, AURA_RETURN = 7f;
    static final double QUAKE_RADIUS = 6.0, FIREBALL_RADIUS = 4.0, FIREBALL_RANGE = 24.0, FIREBOLT_RANGE = 20.0;

    /** 2d4 from two rolls in [0, 3]. */
    static int twoD4(int r0, int r1) { return clamp(r0, 0, 3) + clamp(r1, 0, 3) + 2; }

    /** 1d8 from a roll in [0, 7]. */
    static int d8(int r) { return clamp(r, 0, 7) + 1; }

    static boolean hardy(double maxHealth) { return maxHealth >= HELL_HARDY_HEALTH; }

    static boolean awakened(int blows) { return blows >= HELL_AWAKEN_BLOWS; }

    /** The firebolt cantrip grows with the wearer's level, like the cantrip does at 5th, 11th and 17th level: 1 to 4 dice. */
    static int fireboltDice(int level) { return 1 + (level >= 5 ? 1 : 0) + (level >= 11 ? 1 : 0) + (level >= 17 ? 1 : 0); }

    static float fireboltDamage(int level) { return 4f * fireboltDice(level); }

    /** A maximum roll on a die (the fire's 2d4 = 8, the bare hand's 1d8 = 8) pushes a medium or smaller target back a block. */
    static boolean shoves(int fireRoll, int bareRoll) { return fireRoll >= 8 || bareRoll >= 8; }

    /** "Medium or smaller": a target no more than half as large again as the wearer (by hit-box volume). */
    static boolean mediumOrSmaller(double targetVolume, double wearerVolume) { return targetVolume <= wearerVolume * 1.5; }

    // ------------------------------------------------------------------ Proficiency Gloves
    /** What the gloves lend for the kind of thing in the main hand; {@code hasteAmp} -1 means none, {@code skill} empty means none. */
    record Prof(String name, double attack, int hasteAmp, double armor, double knockback, double luck, String skill) {
        boolean any() { return attack > 0 || hasteAmp >= 0 || armor > 0 || knockback > 0 || luck > 0 || !skill.isEmpty(); }
    }

    enum Held { NOTHING, BLADE, AXE, DIGGER, RANGED, SHIELD, OTHER }

    static Prof proficiency(Held held) {
        return switch (held) {
            case BLADE -> new Prof("blade", 1.5, -1, 0, 0, 0, "");
            case AXE -> new Prof("axe", 1.0, 0, 0, 0, 0, "");
            case DIGGER -> new Prof("digger", 0, 1, 0, 0, 0, "");
            case RANGED -> new Prof("ranged", 0, -1, 0, 0, 1.0, "tensura:sniper");
            case SHIELD -> new Prof("shield", 0, -1, 1.0, 0.2, 0, "");
            case OTHER -> new Prof("other", 0.5, -1, 0, 0, 0.5, "");
            case NOTHING -> new Prof("none", 0, -1, 0, 0, 0, "");
        };
    }

    // ------------------------------------------------------------------ The Gauntlet of Gamelon
    static final String[] GEMS = {"Earth", "Fire", "Water", "Air"};
    static final String[][] SPELLS = {
            {"Levitate Stone", "Stone Skin", "Earth Spikes"},
            {"Flame Bolt", "Warding Flames", "Flame Cloak"},
            {"Ice Bridge", "Healing Spring", "Tidal Burst"},
            {"Gust Leap", "Wind Cutter", "Updraft"}};
    static final int GAMELON_SLOTS = 12;

    static int gamelonNext(int idx) { return Math.floorMod(idx + 1, GAMELON_SLOTS); }

    static int gem(int idx) { return Math.floorMod(idx, GAMELON_SLOTS) / 3; }

    static int spell(int idx) { return Math.floorMod(idx, GAMELON_SLOTS) % 3; }

    static String gamelonName(int idx) { return GEMS[gem(idx)] + " gem: " + SPELLS[gem(idx)][spell(idx)]; }

    static final int ICE_BRIDGE_LENGTH = 10, ICE_BRIDGE_WIDTH = 3;

    // ------------------------------------------------------------------ Chromatic Collar
    enum Ward { ACID, POISON, FIRE, LIGHTNING, COLD }

    /** The d6 of the card from a roll in [0, 5]: 1 to 6. */
    static int chromaRoll(int r0to5) { return clamp(r0to5, 0, 5) + 1; }

    /** Rolls 1 to 5 give that one ward; a 6 gives the wearer's chosen ward (1 to 5) and an armour point. */
    static Ward wardOf(int roll, int choice1to5) {
        int k = roll >= 6 ? clamp(choice1to5, 1, 5) : clamp(roll, 1, 5);
        return Ward.values()[k - 1];
    }

    static double chromaArmor(int roll) { return roll >= 6 ? 1.0 : 0.0; }

    static final double CHROMA_REDUCTION = 0.5;

    static boolean chromaActive(long now, long until) { return now < until; }

    // ------------------------------------------------------------------ Collar of the Penitent Dragon
    static final float BREATH_DAMAGE = 6f, BREATH_UNDEAD_BONUS = 3f;
    static final double BREATH_RANGE = 6.0, BREATH_CONE = 0.6;

    /** A creature of dragon kind (not a great boss) that knows a penitent when it sees one and will not pick them as a target. */
    static boolean dragonKin(String entityPath, double maxHealth) {
        if (entityPath == null || maxHealth >= 150.0) return false;
        String p = entityPath.toLowerCase(java.util.Locale.ROOT);
        return p.contains("dragon") || p.contains("wyvern") || p.contains("drake") || p.contains("wyrm");
    }

    // ------------------------------------------------------------------ Eye of the Aboleth
    static final double CHARM_RANGE = 10.0;
    static final int CHARM_TICKS = 1200, PLAYER_CHARM_TICKS = 160;

    /** The Wisdom save as existence: the target resists if its maximum EP is more than twice the caster's. */
    static boolean charmSaves(double targetEp, double casterEp) { return targetEp > casterEp * 2.0; }

    /** Great bosses and the like cannot be charmed. */
    static boolean charmable(double maxHealth) { return maxHealth < 150.0; }

    // ------------------------------------------------------------------ Fenrir's Collar
    static final int FORM_FREE_TICKS = 6000, FORM_CURSED_TICKS = 1800, FORM_PRICE_TICKS = 600, MOON_CYCLE_DAYS = 8;

    /** Is the wolf under the wearer's control: under the full moon at night, or while the vigil taken at a full moon still holds (until the next one). */
    static boolean controlled(int moonPhase, boolean night, long vigilCycle, long today) {
        return (moonPhase == 0 && night) || (vigilCycle >= 0 && vigilCycle == cycleOf(today));
    }

    static long cycleOf(long day) { return Math.floorDiv(day, (long) MOON_CYCLE_DAYS); }

    static int formTicks(boolean controlled) { return controlled ? FORM_FREE_TICKS : FORM_CURSED_TICKS; }

    // ------------------------------------------------------------------ Sunheart Medallion
    static final int SUN_MAX = 10, COST_VITALITY = 1, COST_SHIELD = 2, COST_MASS_CURE = 3, COST_RESTORATION = 5, COST_HEAL = 6, COST_REGENERATE = 7;
    static final int SUN_WARD_TICKS = 12000;
    static final float SACRED_FLAME = 6f, SACRED_UNDEAD_BONUS = 3f, EVIL_BURN = 4f;
    static final int EVIL_BURN_GAP = 120;
    static final float EVIL_BURN_FLOOR = 6f;

    /** 1d8+3 from a roll in [0, 7]. */
    static int dawnRegain(int r0to7) { return clamp(r0to7, 0, 7) + 4; }

    /** Dawn's first light: the last 1500 ticks of the night's end and the first 1500 of the day. */
    static boolean inDawn(long dayTime) {
        long t = Math.floorMod(dayTime, (long) DAY);
        return t >= DAY - 1500 || t < 1500;
    }

    /** The medallion takes the dawn once per world day. */
    static boolean canGreetDawn(long lastDawnDay, long today) { return lastDawnDay != today; }

    /** The burn of the evil wearer: a few points each round, never when it would leave them under three hearts. */
    static float evilBurn(float health) { return health > EVIL_BURN_FLOOR ? EVIL_BURN : 0f; }

    // ------------------------------------------------------------------ Witcher's Medallion
    static final double WITCHER_RANGE = 20.0;
    static final int WITCHER_HUM_GAP = 100, SENSE_TICKS = 300, SENSE_COOLDOWN = 1200;

    /** 0 nothing, 1 a few, 2 several, 3 many monsters near. */
    static int humLevel(int monsters) { return monsters <= 0 ? 0 : monsters <= 2 ? 1 : monsters <= 5 ? 2 : 3; }

    /** Where a point lies relative to the way the wearer faces: ahead, to the right, behind or to the left. {@code yawDeg} is Minecraft's yaw. */
    static String compass(double dx, double dz, double yawDeg) {
        double toward = Math.toDegrees(Math.atan2(-dx, dz));             // Minecraft yaw of the direction to the point
        double rel = ((toward - yawDeg) % 360.0 + 540.0) % 360.0 - 180.0;
        double a = Math.abs(rel);
        if (a <= 45) return "ahead";
        if (a >= 135) return "behind you";
        return rel > 0 ? "to your right" : "to your left";       // facing south (yaw 0), west (yaw 90) is on the right hand
    }

    // ------------------------------------------------------------------ Buffoon's Pantaloons
    static final int PANTS_DIE = 20;

    /** A natural 1 on a d20 rolled from [0, 19]. */
    static boolean naturalOne(int r0to19) { return clamp(r0to19, 0, PANTS_DIE - 1) == 0; }

    // ------------------------------------------------------------------ Executioner's Bane
    /** Three hours of the day: 3 of 24 Minecraft hours. */
    static final int HEAD_TICKS = 3000, HEAD_SAVE_GAP = 200, HEAD_GRACE_TICKS = 60;

    /**
     * What a blow does to someone who has been beheaded: a lethal blow is turned into one that leaves a single hit point, but only once every ten
     * seconds. A second lethal blow inside that gap is the head being destroyed (it goes through).
     */
    static float beheadedDamage(float health, float damage, long now, long nextSave) {
        if (damage < health || now < nextSave) return damage;
        return Math.max(0f, health - 1f);
    }

    static boolean beheadedSaved(float health, float damage, long now, long nextSave) { return damage >= health && now >= nextSave; }

    /** When the three hours end without the head being reattached the body gives out: it loses half what it has, and never the last point. */
    static float collapseDamage(float health) { return Math.max(0f, Math.min(health * 0.5f, health - 1f)); }

    // ------------------------------------------------------------------ Perculiar Puking Poison
    static final int PUKE_COUNT = 6, PUKE_GAP = 167, PUKE_DC = 15;

    /** The Constitution modifier read from maximum health: every four points above twenty is one. */
    static int conMod(double maxHealth) { return (int) Math.floor((maxHealth - 20.0) / 4.0); }

    /** The DC 15 Constitution save: a d20 (from a roll in [0, 19]) plus the modifier; the drinker fails under 15. */
    static boolean failsSave(int r0to19, int conMod) { return clamp(r0to19, 0, 19) + 1 + conMod < PUKE_DC; }

    /** The twenty things that can come up, in the card's order. */
    enum Puke {
        RAINBOW_SLIME, BROOM, ROCKS, ACID, FISH, BUBBLES, BOOK, BLOOD, CARROTS, ANCHOR,
        DUCKLINGS, CHAIR, NECROTIC_MUSH, GLASS_BALL, CACTUS, BUGS, ANTLERS, OWN_HEAD, PLASMA, COFFIN
    }

    static Puke puke(int r0to19) { return Puke.values()[clamp(r0to19, 0, 19)]; }

    /** The blood's 1d4 from a roll in [0, 3]. */
    static int bloodDamage(int r0to3) { return clamp(r0to3, 0, 3) + 1; }

    /** Carrots: 1d6 from a roll in [0, 5]. */
    static int carrots(int r0to5) { return clamp(r0to5, 0, 5) + 1; }
}
