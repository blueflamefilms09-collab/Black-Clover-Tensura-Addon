package com.newuniverse.nusmp.star;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the star nodes change what a Star mage can do and how much the server holds, so every switch and number is a SERVER setting (NeoForge syncs a server config to the clients). File
 * {@code nusmp-star-server.toml}, registered by {@link StarBoot}. Every value is clamped again by {@link StarRules} when it is read, so a hand-edited file cannot break a bound.
 *
 * <p>{@code star.nodes} (default true): false restores today's behaviour exactly: no star is placed, Conjunction is the 12-block dash, the Quartile Flagellum is the cone, the new pages are not in the book.
 * Changing it needs the grimoire lists rebuilt (a server restart); the casts follow it at once.
 */
public final class StarConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue NODES, AUTO_QUARTET, CONJUNCTION_FALLBACK, NEVERLAND_CANON_GATE;
    public static final ModConfigSpec.IntValue PER_PLAYER, HEIR_PER_PLAYER, SERVER_CAP, LIFE_TICKS, PLACE_RANGE, SEED_COOLDOWN, CONJUNCTION_ALLIES;
    public static final ModConfigSpec.DoubleValue HEIR_MASTERY, DISPEL_CHANCE;
    public static final ModConfigSpec.EnumValue<StarRules.DualMode> DUAL_MODE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("star");
        NODES = b.comment("Star nodes: a Star mage hangs stars in the world (Star Seed), Conjunction teleports to beneath one, the Quartile pages use four. false = Star Magic exactly as before (Conjunction is a dash, the Quartile Flagellum a cone, no new page).")
                .define("nodes", StarRules.DEFAULT_NODES);
        PER_PLAYER = b.comment("Stars one mage can have at once (the oldest goes first).").defineInRange("maxPerPlayer", StarRules.DEFAULT_PER_PLAYER, StarRules.MIN_PER_PLAYER, StarRules.MAX_PER_PLAYER);
        HEIR_PER_PLAYER = b.comment("Stars a Grinberryall heir can have at once.").defineInRange("maxPerHeir", StarRules.DEFAULT_HEIR_PER_PLAYER, StarRules.MIN_PER_PLAYER, StarRules.MAX_PER_PLAYER);
        SERVER_CAP = b.comment("Stars on the whole server at once (the oldest of anybody goes first): the hard bound of the entities this feature can ever hold.")
                .defineInRange("maxOnServer", StarRules.DEFAULT_SERVER, StarRules.MIN_SERVER, StarRules.MAX_SERVER);
        LIFE_TICKS = b.comment("How long a star hangs (ticks; 800 = 40 seconds).").defineInRange("lifeTicks", StarRules.DEFAULT_LIFE_TICKS, StarRules.MIN_LIFE, StarRules.MAX_LIFE);
        PLACE_RANGE = b.comment("How far from the mage a star can be placed (blocks).").defineInRange("placeRange", StarRules.DEFAULT_RANGE, StarRules.MIN_RANGE, StarRules.MAX_RANGE);
        SEED_COOLDOWN = b.comment("Ticks between two placements of one mage.").defineInRange("seedCooldown", StarRules.DEFAULT_SEED_COOLDOWN, StarRules.MIN_SEED_COOLDOWN, StarRules.MAX_SEED_COOLDOWN);
        AUTO_QUARTET = b.comment("A Quartile cast with fewer than four stars places the missing ones on a ring in front of the mage (true), or fails with 'Four stars are needed' (false). Quintile pages always need five real stars.")
                .define("autoQuartet", StarRules.DEFAULT_AUTO_QUARTET);
        CONJUNCTION_FALLBACK = b.comment("Conjunction with no star in the look direction: the old 12-block dash (true) or 'No star to go to' (false).").define("conjunctionFallback", StarRules.DEFAULT_FALLBACK);
        CONJUNCTION_ALLIES = b.comment("How many allies (party, team, tamed pets) within 3 blocks one Conjunction brings along (0 = none; never more than 5).")
                .defineInRange("conjunctionMaxAllies", StarRules.DEFAULT_ALLIES, 0, StarRules.MAX_ALLIES);
        HEIR_MASTERY = b.comment("The mastery (share of the Star grimoire's bar) the Grinberryall heir needs besides a Spade cover and the cleared Spade Kingdom Raid. 1.0 = the signature tier.")
                .defineInRange("heirMastery", StarRules.DEFAULT_HEIR_MASTERY, StarRules.MIN_HEIR_MASTERY, 1.0);
        DUAL_MODE = b.comment("What the Grimoire Tower does when the questionnaire ties Star with another attribute: FUSION (default, as before: one fused grimoire) or SECOND_GRIMOIRE (two real grimoires, the other attribute's and a Star one).")
                .defineEnum("dualMode", StarRules.DualMode.FUSION);
        DISPEL_CHANCE = b.comment("Chance (0 to 1) that a star or a Quartile Scutum pane breaks when the mod's Anti-Magic dispel reaches it (an Anti-Magic slash, the Nihility zone, the other dispel arts). A pane and the stars holding it use about 1.7 times this (0.35 -> 0.6), a Grinberryall heir's use half. 0 = stars never die to Anti-Magic (as before). Allies' and the caster's own stars are never hit.")
                .defineInRange(StarDispelRules.CONFIG_KEY, StarDispelRules.DEFAULT_CHANCE, StarDispelRules.MIN_CHANCE, StarDispelRules.MAX_CHANCE);
        NEVERLAND_CANON_GATE = b.comment("Black Neverland as the manga has it: besides the Grinberryall heir mark the caster must be bonded to the wind spirit Sylph (flag sylph_bond) and hold a Demon-Dweller-family sword or be the Anti-Magic Lord (flag anti_magic_source); the dome then cancels magic (silence, jammed skills, shattered barriers) for an Aura price. false = only the heir mark is needed (the dome without the anti-magic part, unless the caster has the anti-magic source anyway).")
                .define(StarNeverlandRules.CONFIG_KEY, StarNeverlandRules.DEFAULT_CANON_GATE);
        b.pop();
        SPEC = b.build();
    }

    private StarConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean nodes() { return get(NODES); }
    public static int perPlayer(boolean heir) { return StarRules.perPlayerCap(heir, get(PER_PLAYER), get(HEIR_PER_PLAYER)); }
    public static int serverCap() { return StarRules.serverCap(get(SERVER_CAP)); }
    public static int lifeTicks() { return StarRules.lifeTicks(get(LIFE_TICKS)); }
    public static int placeRange() { return StarRules.placeRange(get(PLACE_RANGE)); }
    public static int seedCooldown() { return StarRules.seedCooldown(get(SEED_COOLDOWN)); }
    public static boolean autoQuartet() { return get(AUTO_QUARTET); }
    public static boolean conjunctionFallback() { return get(CONJUNCTION_FALLBACK); }
    public static int conjunctionAllies() { return StarRules.maxAllies(get(CONJUNCTION_ALLIES)); }
    public static double heirMastery() { return StarRules.heirMastery(get(HEIR_MASTERY)); }
    public static boolean neverlandCanonGate() { return get(NEVERLAND_CANON_GATE); }
    public static double dispelChance() { return StarDispelRules.clampChance(get(DISPEL_CHANCE)); }
    public static StarRules.DualMode dualMode() { return get(DUAL_MODE); }
}
