package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Why: the rules of the wave 25 relics that can be written without Minecraft (nourishment of rocks, saving throws turned into chances, the
 * Corpse Mask's five faces, the Witch's eight beasts, the Death Knight's daily curse) live here, so they are unit tested without a game. Dice are passed
 * in as {@code roll(sides) -> 0..sides-1}, like RandomSource.nextInt.
 */
final class Wave25Pure {
    private Wave25Pure() {}

    // ---------------------------------------------------------------- dice and saves
    static int dice(int count, int sides, IntUnaryOperator roll) {
        int t = 0;
        for (int i = 0; i < count; i++) t += 1 + Math.max(0, Math.min(sides - 1, roll.applyAsInt(sides)));
        return t;
    }

    /** The chance that a d20 plus the bonus reaches the DC (a saving throw as a probability). */
    static double saveChance(int dc, int bonus) {
        int needed = dc - bonus;
        int hits = 21 - needed;
        return Math.max(0, Math.min(20, hits)) / 20.0;
    }

    /** Does a rolled d20 face (0..19 for 1..20) plus the bonus reach the DC? */
    static boolean saves(int face0to19, int dc, int bonus) { return face0to19 + 1 + bonus >= dc; }

    /** Disadvantage on a save: two rolls, the worse counts, so a chance p becomes p squared. */
    static double withDisadvantage(double p) { return p * p; }

    /** Damage a creature takes to itself from a curse: capped by a fraction of maximum health and never the last point of health. */
    static float selfHurt(float wanted, float health, float maxHealth, double cap) {
        float limit = Math.min((float) (maxHealth * cap), health - 1.0f);
        return Math.max(0f, Math.min(wanted, limit));
    }

    // ---------------------------------------------------------------- Ring of Rock Eating
    private static final Set<String> STONES = Set.of("stone", "cobblestone", "andesite", "diorite", "granite", "tuff", "deepslate", "cobbled_deepslate",
            "blackstone", "basalt", "smooth_basalt", "calcite", "dripstone_block", "polished_andesite", "polished_diorite", "polished_granite",
            "polished_deepslate", "polished_blackstone", "mossy_cobblestone", "sandstone", "red_sandstone", "terracotta", "end_stone", "netherrack");

    /** Food points one item of this id feeds a rock-eater (0 if it is no rock). {@code itemId} is a registry path with or without the namespace. */
    static double rockNutrition(String itemId) {
        if (itemId == null) return 0;
        String p = itemId.toLowerCase(Locale.ROOT);
        int c = p.indexOf(':');
        String ns = c >= 0 ? p.substring(0, c) : "minecraft";
        if (c >= 0) p = p.substring(c + 1);
        if (!ns.equals("minecraft")) return 0;
        if (p.equals("gravel") || p.equals("flint")) return 0.5;
        if (p.equals("amethyst_shard") || p.equals("quartz")) return 1.5;
        return STONES.contains(p) ? 1.0 : 0;
    }

    /** How many whole items of a stack to eat: enough to fill {@code missingFood} points, at most {@code maxItems} and at most the stack. */
    static int itemsToEat(double perItem, int stackSize, int maxItems, double missingFood) {
        if (perItem <= 0 || stackSize <= 0 || maxItems <= 0 || missingFood <= 0) return 0;
        int need = (int) Math.ceil(missingFood / perItem);
        return Math.max(0, Math.min(need, Math.min(stackSize, maxItems)));
    }

    // ---------------------------------------------------------------- Ring of the Mimic
    static boolean isMimicId(String entityId) {
        if (entityId == null) return false;
        String s = entityId.toLowerCase(Locale.ROOT);
        return s.contains("mimic") && !s.contains("mimicry_ring");
    }

    /** A natural 1 on the d20 face (0 is the lowest face) is the critical failure that bites. */
    static boolean fumble(int face0to19) { return face0to19 == 0; }

    // ---------------------------------------------------------------- Rose Petal
    static int dawnGain(IntUnaryOperator roll) { return dice(2, 6, roll) + 2; }

    // ---------------------------------------------------------------- Silver Tongued Lyre
    static boolean isCelestialId(String entityId) {
        if (entityId == null) return false;
        String s = entityId.toLowerCase(Locale.ROOT);
        return s.contains("angel") || s.contains("seraph") || s.contains("celestial") || s.contains("archon") || s.contains("valkyrie");
    }

    private static final String[] HUMANOID = {"player", "villager", "pillager", "vindicator", "evoker", "illusioner", "witch", "piglin", "zombie", "skeleton",
            "drowned", "husk", "stray", "goblin", "hobgoblin", "orc", "ogre", "lizardman", "dwarf", "elf", "human", "kijin", "vampire", "oni", "bandit",
            "knight", "mage", "wizard", "cultist", "warrior", "priest"};

    /** Does this entity id look humanoid (people, illagers, piglins, the walking dead, Tensura's goblins, orcs, lizardmen...)? */
    static boolean isHumanoidId(String entityId) {
        if (entityId == null) return false;
        String s = entityId.toLowerCase(Locale.ROOT);
        for (String h : HUMANOID) if (s.contains(h)) return true;
        return false;
    }

    static boolean hearsLyre(String entityId) { return isHumanoidId(entityId) || isCelestialId(entityId); }

    /** 4d6 necrotic damage: halved (rounded down, at least 1) on a saved throw. */
    static float lyreDamage(int fourD6, boolean saved) {
        int d = Math.max(1, fourD6);
        return saved ? Math.max(1, d / 2) : d;
    }

    // ---------------------------------------------------------------- Soup Spoon of Improbability
    private static final Set<String> DISADVANTAGE = Set.of("minecraft:weakness", "minecraft:slowness", "minecraft:mining_fatigue", "minecraft:blindness",
            "minecraft:nausea", "minecraft:darkness", "tensura:confusion", "tensura:fear");

    /** The effects that stand for "disadvantage": the steady hand of a good sip keeps them from lasting (at amplifier 1 and under). */
    static boolean isDisadvantage(String effectId) { return effectId != null && DISADVANTAGE.contains(effectId); }

    // ---------------------------------------------------------------- Witch's Ring
    /** The eight beasts of the d8, in the card's order; each has the effects and the build of its form while it is worn. */
    enum Form {
        CAT("Cat", -0.55, 0, 0, false, "minecraft:night_vision:0", "minecraft:jump_boost:1", "minecraft:speed:0"),
        FROG("Frog", -0.6, 0, 0, false, "minecraft:jump_boost:2", "minecraft:water_breathing:0", "minecraft:dolphins_grace:0"),
        BAT("Bat", -0.65, 0, 0, false, "minecraft:night_vision:0", "minecraft:slow_falling:0", "minecraft:jump_boost:1"),
        OWL("Owl", -0.45, 0, 0, false, "minecraft:night_vision:0", "minecraft:slow_falling:0", "minecraft:luck:0"),
        RAT("Rat", -0.65, 0, 0, false, "minecraft:speed:1", "minecraft:night_vision:0", "minecraft:jump_boost:0"),
        RAVEN("Raven", -0.55, 0, 0, false, "minecraft:slow_falling:0", "minecraft:jump_boost:2", "minecraft:luck:0"),
        SPIDER("Spider", -0.5, 0, 0, true, "minecraft:night_vision:0", "minecraft:jump_boost:2", "minecraft:speed:0"),
        GOAT("Goat", -0.15, 2.0, 0.5, false, "minecraft:jump_boost:3", "minecraft:strength:0");

        private final String label;
        private final double scale, attack, knock;
        private final boolean poison;
        private final String[] effects;

        Form(String label, double scale, double attack, double knock, boolean poison, String... effects) {
            this.label = label;
            this.scale = scale;
            this.attack = attack;
            this.knock = knock;
            this.poison = poison;
            this.effects = effects;
        }

        String label() { return label; }
        /** Added to the size (a multiplied-base modifier): -0.5 is half size. */
        double scale() { return scale; }
        double attackBonus() { return attack; }
        double knockbackResistance() { return knock; }
        boolean poisonBite() { return poison; }
        /** "namespace:effect:amplifier" strings, applied while the form lasts. */
        String[] effects() { return effects.clone(); }

        static Form of(int index) {
            Form[] all = values();
            return all[Math.floorMod(index, all.length)];
        }
    }

    static int formAfter(int index) { return Math.floorMod(index + 1, Form.values().length); }

    /** Rolls the d8 of the ring (an index 0..7). */
    static int rollForm(IntUnaryOperator roll) { return Math.floorMod(roll.applyAsInt(Form.values().length), Form.values().length); }

    /** Seconds of form left after a tick of {@code spent} ticks, never under zero. */
    static int allowanceAfter(int left, int spent) { return Math.max(0, left - Math.max(0, spent)); }

    // ---------------------------------------------------------------- Corpse Mask
    /**
     * Stores a witnessed face ("typeId|name") in the mask: it keeps the {@code cap} newest. While there is room a face is stored at once; once full, a
     * new one replaces the oldest only if {@code gap} ticks have passed since the last one was stored. The same face seen again moves to the front
     * (newest) without costing a slot. Returns the new list (the input is untouched).
     */
    static List<String> storeFace(List<String> faces, String face, long now, long lastStoredAt, int cap, int gap) {
        List<String> out = new ArrayList<>(faces);
        if (face == null || face.isBlank()) return out;
        if (out.remove(face)) { out.add(face); return out; }
        if (out.size() < cap) { out.add(face); return out; }
        if (now - lastStoredAt >= gap) {
            out.remove(0);
            out.add(face);
        }
        return out;
    }

    static String faceType(String face) {
        int i = face == null ? -1 : face.indexOf('|');
        return i < 0 ? (face == null ? "" : face) : face.substring(0, i);
    }

    static String faceName(String face) {
        int i = face == null ? -1 : face.indexOf('|');
        return i < 0 || i + 1 >= face.length() ? faceType(face) : face.substring(i + 1);
    }

    /** The next face index in a cycle of {@code size} faces (0 for an empty mask). */
    static int nextFace(int current, int size) { return size <= 0 ? 0 : Math.floorMod(current + 1, size); }

    // ---------------------------------------------------------------- Death Knight's Helmet
    static int helmetDc(int daysWorn) { return Wave25Tune.DKH_BASE_DC + Wave25Tune.DKH_DC_STEP * Math.max(0, daysWorn - 1); }

    /** The Constitution bonus the helmet has granted after this many days without a failed save in between (one a day, up to four). */
    static int conAfterDay(int con) { return Math.min(Wave25Tune.DKH_CON_MAX, Math.max(0, con) + 1); }

    /** The curse hurts 2d8 psychic damage, one more 2d8 for every earlier failure: stage 1 is 2d8, stage 2 is 4d8. */
    static int curseDamage(int stage, IntUnaryOperator roll) { return dice(2 * Math.max(1, stage), 8, roll); }

    /** Days between the last day worn and today: more than one means the helmet was off for a whole day and its history starts again. */
    static boolean helmetLapsed(long lastDay, long today) { return today - lastDay > 1; }

    // ---------------------------------------------------------------- Bucket of Disguise
    private static final String[][] THOUGHTS = {
            {"zombie", "Brains... or a nap. Brains."},
            {"skeleton", "Why do I never find an arrow when I need one?"},
            {"creeper", "Hsss... it is so quiet. I wonder if it would like a hug."},
            {"spider", "Eight legs, no plan. Mostly ceilings."},
            {"enderman", "Do not look. Do not look. Why is it looking?"},
            {"villager", "Hrm. Hmm. Hrmmm. Perhaps a trade."},
            {"pig", "Mud. Mud is good. Is that a carrot?"},
            {"cow", "Grass. Grass. More grass. Moo."},
            {"sheep", "Soft. I am soft. We are all soft."},
            {"chicken", "BAWK. (I forgot what I was about to say.)"},
            {"wolf", "Pack. Bone. Pack. Is that my friend?"},
            {"goblin", "Shiny! Mine! Hm, is that one a big one?"},
            {"orc", "Hungry. Always hungry. Who is the strongest here?"},
            {"slime", "Blorp. (It thinks nothing at all, very warmly.)"}};
    private static final String[] GENERIC = {"It is thinking about lunch.", "It is wondering who that is under the bucket.", "It is thinking about a nap.",
            "It is trying not to laugh at the bucket.", "It is thinking of home."};

    /** A thought for an entity id (substring match on the table), or a generic one; {@code pick} chooses among the generic lines. */
    static String thought(String entityId, int pick) {
        String s = entityId == null ? "" : entityId.toLowerCase(Locale.ROOT);
        for (String[] t : THOUGHTS) if (s.contains(t[0])) return t[1];
        return GENERIC[Math.floorMod(pick, GENERIC.length)];
    }

    // ---------------------------------------------------------------- Spirit Lantern
    /** A plain age for a replay line: "moments", "N minutes" or "N days" of game time (1200 ticks a minute, 24000 a day). */
    static String ageText(long ticks) {
        if (ticks < 600) return "moments";
        if (ticks < 24000) { long m = Math.max(1, ticks / 1200); return m + (m == 1 ? " minute" : " minutes"); }
        long d = ticks / 24000;
        return d + (d == 1 ? " day" : " days");
    }
}
