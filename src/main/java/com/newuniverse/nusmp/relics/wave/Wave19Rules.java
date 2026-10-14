package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

/**
 * Relics of Many Worlds, wave 19 (scrolls, potions and other consumables): the numbers and small tables, pure (no Minecraft imports) so they can be
 * tested. One long rest is a Minecraft day (24000 ticks); one tabletop hour is twenty minutes (24000 ticks) and a tabletop minute-count of
 * twenty is about seven (8000 ticks); a round is 6 seconds.
 */
public final class Wave19Rules {
    private Wave19Rules() {}

    public static final int DAY = 24000, HOUR = 24000, TWENTY_MIN = 8000, ROUND = 120;

    // ================================================================ Wild Magic (Unreadable Spells, Fey Honey)
    /** The outcomes of the wild magic table; the d100 ranges are in {@link #surge}. Every one is bounded and none can kill. */
    public enum Surge {
        GLITTER, LEVITATE, MENDING, FIRE_BURST, FEATHER_FALL, VANISH, SWIFT_FEET, MISTY_STEP, SWAP_PLACES, STINK_CLOUD, CHAIN_SPARK,
        SHEEP_FLOCK, DREAD, FLASH, MANA_SURGE, BLOOM, BACKFIRE, WISH
    }

    private static final int[] EDGES = {6, 12, 18, 26, 32, 38, 44, 50, 55, 62, 68, 73, 79, 84, 89, 93, 97, 100};

    /** The wild magic table: a d100 (1 to 100, clamped) read against the table. */
    public static Surge surge(int d100) {
        int r = Math.max(1, Math.min(100, d100));
        Surge[] all = Surge.values();
        for (int i = 0; i < EDGES.length; i++) if (r <= EDGES[i]) return all[i];
        return Surge.WISH;
    }

    /** A d100 from a bounded random source (1 to 100). */
    public static int d100(IntUnaryOperator rng) { return 1 + Math.floorMod(rng.applyAsInt(100), 100); }

    /** A d20 (1 to 20). */
    public static int d20(IntUnaryOperator rng) { return 1 + Math.floorMod(rng.applyAsInt(20), 20); }

    /** Did this saving throw succeed? d20 plus a bonus against a difficulty class; a natural 20 always does, a natural 1 never. */
    public static boolean saves(int d20, int bonus, int dc) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= dc;
    }

    // ---------------------------------------------------------------- Fey Honey
    /** The honey lasts an hour (twenty minutes). */
    public static final int HONEY_TICKS = HOUR;
    /** Constitution saving throw DC 10 when the honey wears off. */
    public static final int HONEY_DC = 10;
    /** A pool of magicules above this fraction counts as being of the Fey: it shrugs the aftermath off. */
    public static final double FEY_POOL = 0.80;

    /** The come-down: true if the player is spared the surge (a fey-full pool, or the save). */
    public static boolean honeySpared(double magiculeFraction, int d20) {
        return magiculeFraction >= FEY_POOL || saves(d20, 0, HONEY_DC);
    }

    // ---------------------------------------------------------------- Blood Chocolates
    public static final float CHOCOLATE_HEAL = 10f;
    /** Twenty tabletop minutes of vulnerability to radiant damage. */
    public static final int VULNERABLE_TICKS = TWENTY_MIN;
    public static final float VULNERABLE_FACTOR = 2.0f;

    /** Damage after the vulnerability: doubled while it lasts and the damage is radiant. */
    public static float radiantDamage(float amount, boolean vulnerable, boolean radiant) {
        return vulnerable && radiant ? amount * VULNERABLE_FACTOR : amount;
    }

    /** Is this damage type path radiant (holy / light / sun)? */
    public static boolean isRadiantPath(String damageTypePath) {
        if (damageTypePath == null) return false;
        String p = damageTypePath.toLowerCase(Locale.ROOT);
        return p.contains("holy") || p.contains("radiant") || p.contains("light_elemental") || p.contains("solar") || p.contains("sacred");
    }

    // ---------------------------------------------------------------- Holy Water
    /** 2d6 radiant against fiends and the undead: a fixed 8 here, doubled against a creature made vulnerable by blood chocolate. */
    public static final float HOLY_DAMAGE = 8f;
    public static final double HOLY_RANGE = 12.0, HOLY_SPLASH = 2.0;

    // ---------------------------------------------------------------- Scroll of Absorption
    /** The held scroll can take one spell; the stored damage is capped so a huge spell cannot be turned into a killing blow. */
    public static final float ABSORB_CAP = 24f;
    public static final float ABSORB_FALLBACK = 6f;
    public static final int ABSORB_RANGE = 24;

    /** What a released absorbed spell deals: what was absorbed (capped), or a small flat blast if it carried no damage. */
    public static float released(float absorbed) {
        if (absorbed <= 0.01f) return ABSORB_FALLBACK;
        return Math.min(ABSORB_CAP, absorbed);
    }

    /** Is this damage type path a spell (magic, an elemental bolt, a lance, a beam, a breath...)? A blade or an arrow is not. */
    public static boolean isSpellPath(String damageTypePath) {
        if (damageTypePath == null) return false;
        String p = damageTypePath.toLowerCase(Locale.ROOT);
        for (String key : SPELL_WORDS) if (p.contains(key)) return true;
        return false;
    }

    private static final String[] SPELL_WORDS = {"magic", "spell", "fire_ball", "fireball", "fire_bolt", "bolt", "lance", "ray", "blast", "breath", "storm",
            "flame", "lightning", "holy", "curse", "darkness", "frost", "ice", "wind_blade", "water_", "earth_", "sonic", "heat", "thunder", "light_", "solar"};

    // ---------------------------------------------------------------- Bargain Bin Scrolls
    /** Charm Parson: the DC 20 Wisdom save; a creature of this many health points or more shrugs it off (bosses, great beasts). */
    public static final float CHARM_RESISTS_FROM = 100f;
    public static final int CHARM_TICKS = HOUR / 4;                  // five minutes of the tabletop hour

    public static boolean charmTakes(float maxHealth) { return maxHealth < CHARM_RESISTS_FROM; }

    /** Firball: the needles burst for this much to a creature that fails the DC 15 Dexterity save, half to one that makes it. */
    public static final float FIRBALL_DAMAGE = 12f;
    public static final double FIRBALL_RADIUS = 3.5, FIRBALL_RANGE = 28.0;

    public static float firball(boolean dexSave) { return dexSave ? FIRBALL_DAMAGE / 2f : FIRBALL_DAMAGE; }

    /** Speak with Rocks: the rocks point out the ores within this radius, at most this many. */
    public static final int ROCKS_RADIUS = 10, ROCKS_MAX = 24;
    public static final String[] ROCK_QUIPS = {
            "It's a rock.", "Still a rock.", "Rock. Definitely rock.", "We have been here a long time. Rock.", "Have you tried not being a rock? We haven't.",
            "Nothing interesting to say.", "Gravity is the best part of the day.", "That one over there is a rock too."};

    public static String rockQuip(int roll) { return ROCK_QUIPS[Math.floorMod(roll, ROCK_QUIPS.length)]; }

    /** "3 east, 2 down, 5 south": where a block is from the reader (x grows east, z grows south, y up); zero parts are left out. */
    public static String offsetText(int dx, int dy, int dz) {
        StringBuilder sb = new StringBuilder();
        part(sb, dx, "east", "west");
        part(sb, dy, "up", "down");
        part(sb, dz, "south", "north");
        return sb.length() == 0 ? "right here" : sb.toString();
    }

    private static void part(StringBuilder sb, int d, String plus, String minus) {
        if (d == 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(Math.abs(d)).append(' ').append(d > 0 ? plus : minus);
    }

    /** Summon Greater Crab: hostile to everyone, after a telegraph, for this long; at most two alive per caster. */
    public static final int CRAB_WARN = 50, CRAB_TICKS = 20 * 20, CRAB_MAX = 2;
    public static final double CRAB_DAMAGE = 5.0, CRAB_SCALE = 1.6;

    /** Mace Hand: the hand is iron for an hour (twenty minutes), +1d6 plus a strength bonus on a bare fist. */
    public static final int MACE_TICKS = HOUR;
    public static final double MACE_BONUS = 5.0;

    // ---------------------------------------------------------------- Facsimile Copy
    public static final int FACSIMILE_MAX_GEN = 3, FACSIMILE_BASE_LIFE = 12000;

    /** How long the copy lasts before it crumbles: shorter for every copy of a copy (generation 1 is the first copy). */
    public static int facsimileLife(int generation) {
        int g = Math.max(1, generation);
        return Math.max(600, FACSIMILE_BASE_LIFE >> (g - 1));
    }

    /** The fraction of its durability the copy keeps: half, then a third, then a quarter... of what the source had. */
    public static double facsimileQuality(int generation) { return 1.0 / (Math.max(1, generation) + 1); }

    /** The new copy's generation, or -1 if the source is already as worn as a copy may get. */
    public static int nextGeneration(int sourceGeneration) {
        int g = Math.max(0, sourceGeneration) + 1;
        return g > FACSIMILE_MAX_GEN ? -1 : g;
    }

    /** The damage a copy of a damageable item starts with: it keeps only {@code quality} of the durability the source has left. */
    public static int facsimileDamage(int maxDamage, int sourceDamage, int generation) {
        if (maxDamage <= 0) return 0;
        double left = Math.max(0, maxDamage - sourceDamage) * facsimileQuality(generation);
        return Math.min(maxDamage - 1, (int) Math.round(maxDamage - Math.max(1.0, left)));
    }

    // ---------------------------------------------------------------- Mug of The Boss
    public static final String[] TITLES = {"Boss", "General", "Lich", "Chef", "Wizard"};
    public static final int MUG_TICKS = HOUR;
    public static final int MUG_AWE_TICKS = 1200;
    public static final double MUG_RANGE = 12.0;
    public static final float MUG_AWE_HEALTH = 60f;

    public static int nextTitle(int current) { return Math.floorMod(current + 1, TITLES.length); }

    public static String title(int index) { return TITLES[Math.floorMod(index, TITLES.length)]; }

    /** Once every 24 hours: how many ticks until the mug can be chugged again (0 if ready). */
    public static long mugWait(long now, long lastChug) {
        if (lastChug <= 0) return 0;
        return Math.max(0, lastChug + DAY - now);
    }

    public static boolean mugAwes(float maxHealth) { return maxHealth < MUG_AWE_HEALTH; }

    // ---------------------------------------------------------------- Spellbook of Concentration
    /** Three pages of two spells each; half the proficiency bonus (two) spells are prepared at a time. */
    public static final String[][] PAGES = {
            {"tensura:magic_bullet", "tensura:magic_barrier"},
            {"tensura:thought_acceleration", "tensura:magic_sense"},
            {"tensura:analytical_appraisal", "tensura:magic_aura"}};
    public static final String[] PAGE_NAMES = {"Bolt and Ward", "Mind and Sense", "Insight and Aura"};
    public static final int PREPARE_TICKS = DAY, REVISE_TICKS = 6000;

    public static int nextPage(int current) { return Math.floorMod(current + 1, PAGES.length); }

    public static String[] page(int index) { return PAGES[Math.floorMod(index, PAGES.length)]; }

    public static String pageName(int index) { return PAGE_NAMES[Math.floorMod(index, PAGE_NAMES.length)]; }
}
