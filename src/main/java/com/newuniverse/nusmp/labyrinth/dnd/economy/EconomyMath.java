package com.newuniverse.nusmp.labyrinth.dnd.economy;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.dungeon.sentient.StoneTable;

import java.util.Locale;
import java.util.Set;

/**
 * Why: the Labyrinth's monster economy ("the dungeon is an ecosystem, loot is physical": DanMachi's Magic Stones, Delicious in Dungeon's
 * monster meat) is decided here as plain numbers with no Minecraft types, so the whole drop table is unit-tested and the event handler
 * ({@link EconomyDrops}) only turns the answer into item entities. The stone haul is NOT re-invented: it calls the Living Dungeon's own
 * {@link StoneTable} (same grades, same ranges, same Looting rule), so a stone from the Labyrinth and one from the old caves are worth the
 * same and the two economies can be balanced in one place. What is new is the chance gate (a monster can die without leaving a stone, which
 * is what makes a stone an event instead of a guarantee) scaled by toughness, and the meat table, which maps an entity id to the raw cut that
 * {@code DungeonFood} already registers (raw stays toxic: purifying or cooking it is the survival loop that the dungeon hunger clock feeds).
 * Pure, no randomness of its own: it takes a {@link Dice} so a seeded generator can replay any roll.
 */
public final class EconomyMath {
    private EconomyMath() {}

    /** Raw cut kinds, the same numbers as {@code DungeonFood.rawFor}: 0 slime jelly, 1 armor flank, 2 basilisk fillet, 3 direwolf haunch, 4 plain monster meat. */
    public static final int JELLY = 0, FLANK = 1, FILLET = 2, HAUNCH = 3, PLAIN = 4;

    /** What one death leaves as meat: the cut kind and how many. */
    public record Meat(int kind, int count) {
        public static final Meat NONE = new Meat(PLAIN, 0);

        public boolean isEmpty() { return count <= 0; }
    }

    /** The whole physical haul of one death. */
    public record Loot(StoneTable.Haul stones, Meat meat) {
        public boolean isEmpty() { return stones.isEmpty() && meat.isEmpty(); }
    }

    /** Creatures with no flesh to cut: the meat table leaves them out (they still drop stones). */
    private static final Set<String> FLESHLESS = Set.of("skeleton", "stray", "bogged", "blaze", "vex", "allay", "golem", "shulker", "breeze",
            "wraith", "ghost", "spirit", "phantom", "wisp", "elemental", "armor", "living_armor", "guardian_statue", "ghast");

    /** How much the toughness of a monster scales the stone chance: a weak one is a little less likely to leave a stone than a strong one. */
    public static double toughnessFactor(StoneTable.Grade grade) {
        return switch (grade) {
            case TIER1 -> 0.80;
            case TIER2 -> 0.90;
            case TIER3 -> 1.00;
            case JUGGERNAUT -> 1.10;
            case BOSS -> 1.10;
        };
    }

    /** The chance that this death leaves stones at all: the configured base times the toughness factor, clamped to [0, 1]. */
    public static double stoneChance(StoneTable.Grade grade, double base) {
        return clamp01(base * toughnessFactor(grade));
    }

    /** The chance of a meat drop: the configured base, and 5 points more per Looting level, clamped to [0, 1]. */
    public static double meatChance(double base, int looting) {
        return clamp01(base + 0.05 * Math.max(0, looting));
    }

    /**
     * The stones of one death. A player kill gates on {@link #stoneChance} and then rolls {@link StoneTable#roll} (looting adds to the top of the
     * range); a death that no player caused is {@link StoneTable#ambient} (one in four, one stone), so a monster that fell into the void or was
     * burnt by a trap is never a free farm.
     */
    public static StoneTable.Haul stones(StoneTable.Grade grade, boolean playerKill, int looting, double baseChance, Dice dice) {
        if (baseChance <= 0.0) return StoneTable.Haul.NONE;
        if (!playerKill) return StoneTable.ambient(1.0, dice);
        if (!dice.chance(stoneChance(grade, baseChance))) return StoneTable.Haul.NONE;
        return StoneTable.roll(grade, Math.max(0, looting), 1.0, dice);
    }

    /**
     * The meat of one death (player kills only: a corpse nobody butchered is not a ration). One cut, two from a tier-3 or heavier monster, one
     * more with luck from Looting; nothing for a fleshless creature.
     */
    public static Meat meat(String entityPath, StoneTable.Grade grade, boolean playerKill, int looting, double baseChance, Dice dice) {
        if (!playerKill || baseChance <= 0.0 || fleshless(entityPath)) return Meat.NONE;
        if (!dice.chance(meatChance(baseChance, looting))) return Meat.NONE;
        int count = 1;
        if (grade == StoneTable.Grade.TIER3 || grade == StoneTable.Grade.JUGGERNAUT || grade == StoneTable.Grade.BOSS) count++;
        if (looting > 0 && dice.chance(Math.min(0.9, 0.25 * looting))) count++;
        return new Meat(kindOf(entityPath), count);
    }

    /** The whole table for one death, the shape {@link EconomyDrops} consumes. */
    public static Loot roll(String entityPath, StoneTable.Grade grade, boolean playerKill, int looting, double stoneBase, double meatBase, Dice dice) {
        return new Loot(stones(grade, playerKill, looting, stoneBase, dice), meat(entityPath, grade, playerKill, looting, meatBase, dice));
    }

    /** Which raw cut a creature leaves, by words in its entity id path (the first match wins; anything else is plain monster meat). */
    public static int kindOf(String entityPath) {
        String p = norm(entityPath);
        if (has(p, "slime", "jelly", "ooze")) return JELLY;
        if (has(p, "armor", "saurus", "turtle", "shell", "crab", "beetle")) return FLANK;
        if (has(p, "basilisk", "snake", "serpent", "lizard", "spider", "scorpion", "naga")) return FILLET;
        if (has(p, "wolf", "hound", "dog", "boar", "hog", "bear", "lion", "tiger")) return HAUNCH;
        return PLAIN;
    }

    /** True for a creature that is made of bone, fire, spirit or stone and leaves no meat. */
    public static boolean fleshless(String entityPath) {
        String p = norm(entityPath);
        for (String f : FLESHLESS) if (p.contains(f)) return !has(p, "armorsaurus", "armor_saurus");
        return false;
    }

    private static boolean has(String p, String... words) {
        for (String w : words) if (p.contains(w)) return true;
        return false;
    }

    private static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }

    private static double clamp01(double v) { return v < 0.0 ? 0.0 : Math.min(1.0, v); }
}
