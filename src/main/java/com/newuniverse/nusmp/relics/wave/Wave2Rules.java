package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;
import java.util.Set;

/**
 * Wave 2 of the Relics of Many Worlds (the charms, boots and the cloaks, capes and mantles): the pure rules, no Minecraft imports, so
 * they can be tested without the game.
 * <p>
 * Tabletop becomes Minecraft here: a foot is a third of a block, a saving throw is a chance read off the target's size (maximum health,
 * bosses save most), "advantage" is two rolls and the better one, a short or long rest is the Minecraft day.
 */
public final class Wave2Rules {
    private Wave2Rules() {}

    /** One block is three feet: 30 ft is 10 blocks, 60 ft is 20 blocks, 5 ft is the next block over. */
    public static final double FEET_PER_BLOCK = 3.0;

    public static double blocks(double feet) { return feet / FEET_PER_BLOCK; }

    // ---------------------------------------------------------------- saving throws
    /**
     * The chance that a creature shrugs an effect off ("saving throw"): small creatures almost never do, a 100 HP monster does half of the
     * time, a boss (or anything with 300 HP) about three times in five, nothing is ever immune and nothing always saves.
     */
    public static double saveChance(double maxHealth, boolean boss) {
        double c = boss ? 0.6 : (maxHealth - 20.0) / 160.0;
        return Math.max(0.05, Math.min(0.6, c));
    }

    /** {@code roll} is a uniform number in [0, 1): it saves when the roll is under the chance. */
    public static boolean saved(double chance, double roll) { return roll < chance; }

    // ---------------------------------------------------------------- geometry
    /**
     * Is the offset (dx, dy, dz) from the caster within {@code range} blocks and inside a cone around the look direction (lx, ly, lz) whose
     * half angle has the cosine {@code cosHalf}? Something standing on the caster counts as inside.
     */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double range, double cosHalf) {
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > range) return false;
        if (dist < 1.0E-6) return true;
        double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (ll < 1.0E-6) return false;
        return (dx * lx + dy * ly + dz * lz) / (dist * ll) >= cosHalf;
    }

    // ---------------------------------------------------------------- Sand of the Pocket
    public static final double SAND_RANGE = 3.5;
    public static final double SAND_COS_HALF = 0.5;

    /** One round: three seconds of blindness (weakness is the "disadvantage on attacks"), halved against a boss. */
    public static int sandTicks(boolean boss) { return boss ? 30 : 60; }

    private static final Set<String> EYELESS = Set.of("minecraft:warden");

    /** "The creature must have eyes": the Warden has none, so the sand does nothing to it. */
    public static boolean eyeless(String entityTypeId) { return entityTypeId != null && EYELESS.contains(entityTypeId); }

    // ---------------------------------------------------------------- Holy Chains
    private static final Set<String> EVIL_WORDS = Set.of("daemon", "devil", "demon", "lich", "vampire", "wraith", "ghoul", "imp", "undead",
            "zombie", "skeleton", "wither", "revenant", "shade", "specter", "spectre", "ghost", "wight", "banshee", "fiend", "hell", "necromancer");

    /** An entity type path (the part after the colon) that names something evil: undead, daemons, devils and the like. */
    public static boolean evilPath(String path) {
        if (path == null) return false;
        for (String word : path.toLowerCase(Locale.ROOT).split("_")) if (EVIL_WORDS.contains(word)) return true;
        return false;
    }

    // ---------------------------------------------------------------- Cloak of the Depths
    /** One tentacle, and one more at every ten levels of experience (the card's character levels 5, 11 and 17). */
    public static int tentacles(int xpLevel) {
        return 1 + (xpLevel >= 10 ? 1 : 0) + (xpLevel >= 20 ? 1 : 0) + (xpLevel >= 30 ? 1 : 0);
    }

    // ---------------------------------------------------------------- Nightstalker's Mantle
    /**
     * The Shriek: 10 damage, half on a successful save, and against another player never more than 6 (30% of 20 HP) or 3 on a save. The card
     * says 6d6; the Mantle pays for it in existence points and in a long cooldown.
     */
    public static double shriekDamage(boolean targetIsPlayer, boolean saved) {
        double d = targetIsPlayer ? 6.0 : 10.0;
        return saved ? d / 2.0 : d;
    }

    /** The wing-beat Dash: further while the wings are open. */
    public static double wingDashBlocks(boolean flying) { return flying ? 9.0 : 4.0; }

    // ---------------------------------------------------------------- Cloak of the Moth Rider
    /** The Fearsome Visage pattern makes one monster in five unwilling to pick the wearer as its first target. */
    public static boolean intimidated(int mobHash) { return Math.floorMod(mobHash, 5) == 0; }

    /** The moth ally stays for five minutes (the card says an hour; the summon helper allows five minutes at most). */
    public static final int MOTH_LIFE_TICKS = 6000;

    // ---------------------------------------------------------------- Cloak of Dichotomy
    public enum Hit { MELEE, SPELL, OTHER }

    /**
     * Sorts a blow by the damage type that dealt it. A blow struck by hand ("minecraft:player_attack", dealt directly) is the paladin's
     * smite; magic and everything a spell mod adds (another namespace) is a spell; arrows, fire, falls and the rest are neither.
     */
    public static Hit classify(String damageTypeId, boolean direct) {
        if (damageTypeId == null) return Hit.OTHER;
        if (direct && damageTypeId.equals("minecraft:player_attack")) return Hit.MELEE;
        if (damageTypeId.equals("minecraft:magic") || damageTypeId.equals("minecraft:indirect_magic")) return Hit.SPELL;
        return damageTypeId.startsWith("minecraft:") ? Hit.OTHER : Hit.SPELL;       // another namespace (tensura, nusmp, a spell mod) is a spell
    }

    /** The smite's 1d8 extra radiant damage, at 0.6 per pip, half as much again against evil. */
    public static double smiteBonus(int d8, boolean evil) {
        double b = Math.max(1, Math.min(8, d8)) * 0.6;
        return evil ? b * 1.5 : b;
    }

    /** A spell after a smite hits 20% harder (its save DC went up by 2). */
    public static final double SPELL_BOOST = 1.2;

    /** How long a boost waits for the other half of the pair: ten seconds. */
    public static final int BOOST_TICKS = 200;

    // ---------------------------------------------------------------- Cloak Mimic, tame
    /** What the Hungry cloak takes at the turn of the day. */
    public record Tax(int food, float health, boolean exhausted) {}

    /**
     * It feeds on its wearer once a day: four points of food from a full stomach; from a nearly empty one all that is left and a spell of
     * exhaustion; from an empty one two hearts of blood (never the last of it) and the exhaustion.
     */
    public static Tax hungerTax(int foodLevel) {
        if (foodLevel >= 6) return new Tax(4, 0f, false);
        if (foodLevel >= 1) return new Tax(foodLevel, 0f, true);
        return new Tax(0, 4f, true);
    }

    /** The cloak would rather feed on a caster's magicules: a tenth of the maximum at the turn of the day, if the wearer has that much. */
    public static final double MIMIC_MANA_TAX = 0.10;

    /** Acid and corrosion are halved for the wearer. */
    public static boolean acid(String damageTypePath) {
        if (damageTypePath == null) return false;
        String p = damageTypePath.toLowerCase(Locale.ROOT);
        return p.contains("acid") || p.contains("corro");
    }

    public static int nextStyle(int current, int count) { return count <= 0 ? 0 : Math.floorMod(current + 1, count); }

    // ---------------------------------------------------------------- Cloak of Comfort
    /** A camp needs quiet: nothing hostile this close. */
    public static final double CAMP_QUIET_RADIUS = 14.0;
}
