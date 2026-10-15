package com.newuniverse.nusmp.relics.wave;

import java.util.List;
import java.util.Locale;

/**
 * Why: wave 27 (headgear and gloves) turns a dozen tabletop rules (daily and weekly uses, charges that come back at dawn, ability checks, a
 * critical fumble, a compliment, a cursed crown) into plain numbers. They live here, free of Minecraft, so a unit test can pin them down; the glue
 * ({@link Wave27}, {@link Wave27Util}, {@link Wave27Head}, {@link Wave27Hands}, {@link Wave27Events}) only reads and writes what these produce.
 * <p>
 * The time scale is the one of the earlier waves: a long rest is a Minecraft day (24000 ticks), a short rest a few minutes, one round 6 seconds,
 * five feet one block.
 */
final class Wave27Rules {
    private Wave27Rules() {}

    static final int DAY = 24000;

    // The named switches the cards carry (Passive.flag) and the handlers ask for. Every card also carries F_ANY, so a handler can leave early.
    static final String F_ANY = "w27_any", F_SOUL = "w27_soul", F_CROWN = "w27_crown", F_ORB = "w27_orb", F_MASK = "w27_mask", F_TRICKSTER = "w27_trickster",
            F_BEAR = "w27_bear", F_DRAGON = "w27_dragon", F_MIMIC = "w27_mimic", F_STORM = "w27_storm", F_GLOVE = "w27_glove", F_OPPORTUNITY = "w27_opportunity",
            F_GORISTRO = "w27_goristro";

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ------------------------------------------------------------------ days
    /** The day number of a world clock reading (turns over at dawn, like the sun). */
    static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    /** What is stored for "used on this day": the day plus one, so that a never-used 0 is never today. */
    static long dailyMark(long today) { return today + 1; }

    /** True if the once-per-day use has not been spent today (the Trickster Mask: after it ends it cannot be used until the next dawn). */
    static boolean dailyReady(long mark, long today) { return mark != dailyMark(today); }

    /** The Soul Searchers' week: seven days; the mark is the game time when they may be used again. */
    static final long WEEK = 7L * DAY;

    static boolean weekReady(long readyAt, long now) { return now >= readyAt; }

    // ------------------------------------------------------------------ charges that come back at dawn
    /**
     * A pool of charges that comes back at dawn. {@code day} is the world day of the last change plus one (0 for a pool never touched, which reads as
     * full). A dawn that has passed since adds {@code gain} charges for each day gone, up to the maximum.
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
    }

    static final int DRAGON_MAX = 3, STORM_MAX = 9, THUNDERWAVE_COST = 1, LIGHTNING_COST = 3;

    /** The 1d3 of the Dragon's Fire Gauntlet, from a roll in [0, 2]. */
    static int dawnGainD3(int roll0to2) { return 1 + clamp(roll0to2, 0, 2); }

    /** The 1d4+1 of the Stormcaller, from a roll in [0, 3]. */
    static int dawnGainD4p1(int roll0to3) { return 2 + clamp(roll0to3, 0, 3); }

    // ------------------------------------------------------------------ Soul Searchers
    enum Order { LAWFUL, NEUTRAL, CHAOTIC }

    enum Moral { GOOD, NEUTRAL, EVIL }

    /** What a soul reads as. {@code kills} is a player's count of player kills (0 for anything else). */
    record Alignment(Order order, Moral moral) {
        String label() {
            if (order == Order.NEUTRAL && moral == Moral.NEUTRAL) return "True Neutral";
            return cap(order.name()) + " " + cap(moral.name());
        }

        private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT); }
    }

    /**
     * The nine alignments from what the game knows of a creature: a pet, villager or golem is lawful and good; an undead is lawful and evil (it obeys);
     * any other monster is chaotic and evil; a player is good, neutral or evil by the number of players they have killed (0, 1 to 2, 3 or more) and
     * lawful unless they carry a curse; everything else is neutral.
     */
    static Alignment alignment(boolean player, boolean tameOrVillager, boolean undead, boolean monster, int kills, boolean cursed) {
        if (player) {
            Moral m = cursed || kills >= 3 ? Moral.EVIL : kills >= 1 ? Moral.NEUTRAL : Moral.GOOD;
            return new Alignment(cursed ? Order.CHAOTIC : Order.LAWFUL, m);
        }
        if (tameOrVillager) return new Alignment(Order.LAWFUL, Moral.GOOD);
        if (undead) return new Alignment(Order.LAWFUL, Moral.EVIL);
        if (monster) return new Alignment(Order.CHAOTIC, Moral.EVIL);
        return new Alignment(Order.NEUTRAL, Moral.NEUTRAL);
    }

    /** The religion check against Charisma, as existence: a soul whose maximum EP is over twice the wearer's is too deep to read. */
    static boolean tooDeep(double theirMaxEp, double myMaxEp) { return theirMaxEp > myMaxEp * 2.0; }

    static final int MAX_READ = 24;

    // ------------------------------------------------------------------ The Mad King's Crown
    /** Continuous wear that starts the nightmares: two days. Past six days the exhaustion is heavier. */
    static final long NIGHTMARE_AFTER = 2L * DAY, EXHAUSTED_AFTER = 6L * DAY;

    /**
     * The wear counter after one more second worn: it restarts from nothing if the crown was off for more than a day ("you must wear it constantly").
     * {@code lastWorn} is the game time of the last second it was worn (0 if never).
     */
    static long wornAfter(long worn, long lastWorn, long now) {
        if (lastWorn > 0 && now - lastWorn > DAY) worn = 0;
        return worn + 20;
    }

    /** 0 none, 1 nightmares and a weakness, 2 the heavy exhaustion: never more, and it never kills. */
    static int exhaustion(long worn) { return worn >= EXHAUSTED_AFTER ? 2 : worn >= NIGHTMARE_AFTER ? 1 : 0; }

    /** The extra maximum magicules the crown gives ("an additional spell slot"): a fifth of the base, as a multiplier. */
    static final double CROWN_MAGICULE = 0.20;

    // ------------------------------------------------------------------ Orb of Ancient Sight
    static final double ORB_RADIUS = 24.0;      // 120 ft
    static final int ORB_LINES = 8, ORB_GLOW = 400;

    /** The roman numeral an effect level is written with (1 is not written, 0 is level one). */
    static String level(int amplifier) {
        if (amplifier <= 0) return "";
        String[] r = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        return amplifier < r.length ? r[amplifier] : " " + (amplifier + 1);
    }

    /** One line of the aura report: "Name: Speed II, Strength". */
    static String auraLine(String name, List<String> effects) { return name + ": " + String.join(", ", effects); }

    // ------------------------------------------------------------------ Trickster Mask
    static final int MASK_TICKS = 3000;      // 3 hours of a 24-hour day

    // ------------------------------------------------------------------ Gloves
    /** A pickpocket's chance: weaker creatures are easier; never certain, never hopeless. */
    static double stealChance(double maxHealth) { return clamp(0.8 - maxHealth / 100.0, 0.25, 0.8); }

    static boolean steals(double chance, int roll0to99) { return roll0to99 < (int) Math.round(chance * 100.0); }

    /** The critical 1 of the Gloves of Opportunity: five in a hundred melee blows from a creature that holds something go wide. */
    static final int FUMBLE_PERCENT = 5, FUMBLE_GAP = 100;

    static boolean fumbles(int roll0to99) { return roll0to99 < FUMBLE_PERCENT; }

    /** Is a point inside a cone from an origin along a unit direction (dot product test), up to a range? */
    static boolean inCone(double dx, double dy, double dz, double dirX, double dirY, double dirZ, double range, double minDot) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6) return true;
        if (len > range) return false;
        return (dx * dirX + dy * dirY + dz * dirZ) / len >= minDot;
    }

    /** The Gauntlet Mimic's three shapes. */
    enum Shape { BLUDGEONING, PIERCING, SLASHING;
        Shape next() { return values()[(ordinal() + 1) % values().length]; }

        static Shape of(int i) { return values()[clamp(i, 0, values().length - 1)]; }
    }

    static final float MIMIC_BONUS = 3.5f, MIMIC_PIERCE_EXTRA = 2.0f;
    static final int MIMIC_SAP_FLOOR = 6, ADHESIVE_GAP = 60, ADHESIVE_TICKS = 60;

    /** The extra damage of a bare-handed blow with the mimic: 1d6 average, piercing adds a little against heavy armour (8 points or more). */
    static float mimicDamage(Shape s, int victimArmor) { return MIMIC_BONUS + (s == Shape.PIERCING && victimArmor >= 8 ? MIMIC_PIERCE_EXTRA : 0f); }

    /** The Hungry feature saps two hit points, but never when that would leave the wearer under {@link #MIMIC_SAP_FLOOR}. */
    static boolean canSap(float health) { return health > MIMIC_SAP_FLOOR; }

    /** Goristro's unarmed 1d8: average 4.5. */
    static final float GORISTRO_BONUS = 4.5f, GORISTRO_CHARGE_DAMAGE = 16.0f;
    static final double GORISTRO_RANGE = 7.0;     // 20 ft is four blocks; stretched so a charge is worth making

    // ------------------------------------------------------------------ Glove of Appreciation
    static final int GLOVE_MAX = 5, GLOVE_GAP = 12000;      // five charges (invented: the card gives no limit); one charge per giver per ten minutes

    private static final String[] NICE = {"thank", "thanks", "nice", "great", "awesome", "amazing", "well done", "good job", "gg", "brilliant", "love",
            "wonderful", "beautiful", "genius", "impressive", "legend", "hero", "kind", "lovely", "good work", "fantastic", "cool"};

    /** Is this chat line a compliment for that name: it names the player and holds a kind word. */
    static boolean compliment(String line, String name) {
        if (line == null || name == null || name.isBlank()) return false;
        String l = line.toLowerCase(Locale.ROOT);
        if (!l.contains(name.toLowerCase(Locale.ROOT))) return false;
        for (String w : NICE) if (l.contains(w)) return true;
        return false;
    }

    /** May a giver add a charge: not before the gap has passed since their last one, and never past the cap. */
    static boolean giftAccepted(int charges, long lastFromGiver, long now) { return charges < GLOVE_MAX && (lastFromGiver <= 0 || now - lastFromGiver >= GLOVE_GAP); }

    // ------------------------------------------------------------------ stunned
    /** The stun check of the Stormcaller's Shocking Grasp: paralysed, turned to stone, webbed or held nearly still by Slowness V or more. */
    static boolean stunned(boolean paralysed, boolean petrified, boolean webbed, int slownessAmp) { return paralysed || petrified || webbed || slownessAmp >= 4; }
}
