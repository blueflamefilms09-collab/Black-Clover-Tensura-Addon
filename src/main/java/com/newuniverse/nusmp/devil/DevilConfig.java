package com.newuniverse.nusmp.devil;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Devil Contract system ({@code serverconfig/nusmp-devil-server.toml}, 3.48.0). Everything here changes gameplay, so it is a server setting (rule 9); every value is
 * clamped again when it is read, so a hand-edited file can not go past a bound. {@code enabled = false} restores the game as it was before the feature: the key does nothing, no stat is added, the union can
 * not begin, a level in progress ends, and the new sacrifices (half health, a lost eye) stop having an effect (they stay on the record). What only changes looks (the markings, the horns and wings, the eye
 * shade) is a CLIENT setting: {@code client.devil.DevilClientConfig}.
 */
public final class DevilConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, ALLOW_FLIGHT, HALF_HEALTH, EYE_REACH, DEATH_COOLDOWN, WORLDS, EFFECTS;
    private static final ModConfigSpec.DoubleValue DRAIN_SCALE, STAT_SCALE, EFFECT_SCALE;
    private static final ModConfigSpec.IntValue UNION_SECONDS, COOLDOWN_HOURS;
    private static final ModConfigSpec.BooleanValue NPC_ENABLED, CRAFTABLE, DARK_RULE;
    private static final ModConfigSpec.IntValue DARK_LIGHT, DARK_RADIUS;
    private static final ModConfigSpec.ConfigValue<?> DARK_DIMS;   // a config value, not state (the scan test bans static collections)
    private static final ModConfigSpec.IntValue NPC_MAX, NPC_LIFE;
    private static final ModConfigSpec.BooleanValue LEGACY, M_ENABLED, M_ARTS, M_PASSIVES, M_FUSIONS;
    private static final ModConfigSpec.IntValue M_START, M_MAX, M_DAILY;
    private static final ModConfigSpec.DoubleValue M_RATE, M_BOOST, CALL_PERCENT;
    private static final ModConfigSpec.BooleanValue SCROLL_ON, CALL_ON;
    private static final ModConfigSpec.IntValue CALL_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("devil");
        ENABLED = b.comment("The Devil Contract system: assimilation key, the Devil Union and the new sacrifices. false = off: the key does nothing, no stat is added, a level in progress ends, the new sacrifices have no effect.")
                .define("enabled", true);
        b.push("union");
        UNION_SECONDS = b.comment("How long the Devil Union lasts, in seconds. Strict: it ends then, whatever the player does (default 5 minutes).")
                .defineInRange("unionSeconds", DevilRules.DEFAULT_UNION_SECONDS, 20, 3600);
        COOLDOWN_HOURS = b.comment("How long after the union before it can begin again, in in-game hours (1000 ticks each; the default 24 is one Minecraft day, 24 000 ticks). Counted in game time, so sleeping does not shorten it.")
                .defineInRange("cooldownHours", DevilRules.DEFAULT_COOLDOWN_HOURS, 0, 240);
        DEATH_COOLDOWN = b.comment("Dying inside the union starts the cooldown. false = a death ends the union without a cooldown.").define("deathStartsCooldown", true);
        b.pop();
        b.push("balance");
        DRAIN_SCALE = b.comment("Multiplier on the magicules assimilation and the union drain every second.").defineInRange("drainScale", 1.0, 0.0, 4.0);
        STAT_SCALE = b.comment("Multiplier on the attack damage and movement speed bonuses (each also has a hard cap in the rules).").defineInRange("statScale", 1.0, 0.0, 2.0);
        ALLOW_FLIGHT = b.comment("From 50 % the wings give creative-style flight. false = the wings are only for show.").define("allowFlight", true);
        b.pop();
        b.push("sacrifices");
        HALF_HEALTH = b.comment("The 'half' price halves the maximum health for good (it stays through death). false = it is only recorded.").define("halfHealth", true);
        EYE_REACH = b.comment("A lost eye also shortens how far the player can reach (depth perception). false = a lost eye only changes the screen (client side).").define("eyeReachPenalty", true);
        b.pop();
        b.push("worlds");
        WORLDS = b.comment("3.76.0: devils of eight anime worlds can be called with /devil worlds (Black Clover, Chainsaw Man, Black Butler, Blue Exorcist, Tensura, Dororo, Berserk, Fairy Tail), each by its own ritual.",
                "false = the rituals do not begin (contracts already signed stay); the old grimoire devils are not touched either way.").define("enabled", true);
        b.pop();
        b.push("attribute");
        EFFECTS = b.comment("3.76.0: the devil's attribute acts in a fight while the devil is let in (from 20 % assimilation): extra damage and its own effect on the host's blows. false = the attribute is a label, as before 3.76.0.")
                .define("combatEffects", true);
        EFFECT_SCALE = b.comment("Multiplier on the extra damage of the attribute (the bonus is never more than three times the blow).").defineInRange("effectScale", 1.0, 0.0, 10.0);
        b.pop();
        b.push("npc");
        NPC_ENABLED = b.comment("The devil's figure: while a ritual runs and the offer is open, a figure of the devil stands three blocks in front of the caller and speaks to them (docs/devil_forms.md).",
                "false = exactly the old behaviour: the ritual's pictures only, no entity.").define("enabled", true);
        NPC_MAX = b.comment("The most figures on the whole server at once (one per player at most). Over the cap a ritual plays the old pictures-only shadow instead.")
                .defineInRange("max", DevilNpcRules.DEFAULT_MAX, 0, DevilNpcRules.MAX_MAX);
        NPC_LIFE = b.comment("The longest a figure may live, in seconds, whatever else happens (the ritual and the 30 second offer fit inside the default 60).")
                .defineInRange("lifetimeSeconds", DevilNpcRules.DEFAULT_LIFE_SECONDS, DevilNpcRules.MIN_LIFE_SECONDS, DevilNpcRules.MAX_LIFE_SECONDS);
        b.pop();
        CRAFTABLE = b.comment("4.40.0: the Devil Contract item can be crafted (very expensive: a Tensura Royal Blood or Zane Blood, or the fallback Gneiss Crystal, with a nether star and more). false = only the old way (relic loot). Read when data packs load (world start, /reload); old copies work either way.")
                .define("contractCraftable", true);
        b.push("ritual");
        DARK_RULE = b.comment("4.40.0: a contract ritual needs a dark magic at the circle (devil_dark_magic.json) or a dark place (a dark dimension or biome, or light at or under maxLight). false = the old rituals, in any light.")
                .define("darkRule", true);
        DARK_LIGHT = b.comment("The highest light level (block and sky together, 0 to 15) that counts as a dark place; the default 4 is a night in the open or a cave.").defineInRange("maxLight", 4, 0, 15);
        DARK_RADIUS = b.comment("How far from the caller a player counts as a participant of the circle, in blocks (their dark magic lets the ritual begin).").defineInRange("participantRadius", 8, 0, 32);
        DARK_DIMS = b.comment("Dimensions that are dark enough for a ritual in any light (the End by default).").defineListAllowEmpty("darkDimensions", java.util.List.of("minecraft:the_end"), () -> "minecraft:the_end", o -> o instanceof String);
        b.pop();
        LEGACY = b.comment("The old assimilation: the key climbs the level to 100 percent at once, no mastery ceiling, no time limit, no rest after. false (default) = the new Devil Awakening: the level can not pass the skill's mastery.")
                .define("legacyAssimilation", false);
        b.push("mastery");
        M_ENABLED = b.comment("The Devil Awakening skill (docs/devil_mastery.md): the extra skill a contract gives, with a mastery that starts low and grows by use. false = no skill, no arts, no passives, no mastery ceiling.").define("enabled", true);
        M_START = b.comment("The mastery (percent of the devil's power) a new contract starts with.").defineInRange("startPercent", 10, 1, 50);
        M_MAX = b.comment("The highest mastery that can be reached (100 = the Devil Union can open).").defineInRange("maxPercent", 100, 10, 100);
        M_RATE = b.comment("Multiplier on how fast mastery grows (1 = about 30 hours of active play to reach 100 percent).").defineInRange("gainRate", 1.0, 0.1, 10.0);
        M_BOOST = b.comment("5.7.0: a second multiplier on how fast mastery grows, on top of gainRate (it is the owner's 'faster devil mastery'). 3 = three times as fast as before 5.7.0 (about 10 hours of active play to 100 percent instead of 30). 1 = the old speed.")
                .defineInRange("gainBoost", 3.0, 1.0, 10.0);
        M_DAILY = b.comment("The most mastery-earning use one player can count a real (UTC) day, in minutes of effective use.").defineInRange("dailyCapMinutes", 360, 10, 1440);
        M_ARTS = b.comment("The arts of the devil's kit (skill modes 2 to 5).").define("arts", true);
        M_PASSIVES = b.comment("The passives of the devil's kit while the devil is let in.").define("passives", true);
        M_FUSIONS = b.comment("Double unions: in the Devil Union, a second devil you hold adds its fusion passives (kits file, 'fusions').").define("fusions", true);
        b.pop();
        b.push("scroll");
        SCROLL_ON = b.comment("5.7.0: a player who gets a devil receives a Scroll of the Devil (a written book) that explains how the devil works and what each step of mastery needs. One per devil; /devil scroll gives it again. false = no scroll.")
                .define("enabled", true);
        b.pop();
        b.push("calling");
        CALL_ON = b.comment("5.7.0: now and then a devil speaks to a player who has no contract and offers one (the offer lasts 30 seconds; /devil sign or /devil refuse answers it). The ritual's cost items are not taken, the contract's price is. false = devils only come when called.")
                .define("enabled", true);
        CALL_PERCENT = b.comment("The chance of one offer, in percent, each time a player is rolled (2 = the owner's number).").defineInRange("chancePercent", DevilCallingRules.DEFAULT_PERCENT, 0.0, 100.0);
        CALL_MINUTES = b.comment("How many real minutes pass between two rolls for the same player. One roll is a few comparisons, once a minute for the whole server.")
                .defineInRange("intervalMinutes", DevilCallingRules.DEFAULT_INTERVAL_MINUTES, 1, DevilCallingRules.MAX_INTERVAL_MINUTES);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private DevilConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return DevilRules.clampD(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return DevilRules.clampInt(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean enabled() { return on(ENABLED, true); }
    public static long unionTicks() { return DevilRules.unionTicks(whole(UNION_SECONDS, DevilRules.DEFAULT_UNION_SECONDS, 20, 3600)); }
    public static long cooldownTicks() { return DevilRules.cooldownTicks(whole(COOLDOWN_HOURS, DevilRules.DEFAULT_COOLDOWN_HOURS, 0, 240)); }
    public static boolean deathStartsCooldown() { return on(DEATH_COOLDOWN, true); }
    public static double drainScale() { return num(DRAIN_SCALE, 1.0, 0, 4); }
    public static double statScale() { return num(STAT_SCALE, 1.0, 0, 2); }
    public static boolean allowFlight() { return on(ALLOW_FLIGHT, true); }
    public static boolean halfHealth() { return on(HALF_HEALTH, true); }
    public static boolean eyeReach() { return on(EYE_REACH, true); }
    public static boolean worlds() { return on(WORLDS, true); }
    public static boolean attributeEffects() { return on(EFFECTS, true); }
    public static double effectScale() { return num(EFFECT_SCALE, 1.0, 0, 10); }
    public static boolean contractCraftable() { return on(CRAFTABLE, true); }
    public static boolean darkRule() { return on(DARK_RULE, true); }
    public static int darkMaxLight() { return whole(DARK_LIGHT, 4, 0, 15); }
    public static int darkRadius() { return whole(DARK_RADIUS, 8, 0, 32); }
    public static boolean darkDimension(String id) {
        try { return id != null && DARK_DIMS.get() instanceof java.util.Collection<?> dims && dims.contains(id); } catch (RuntimeException notLoaded) { return "minecraft:the_end".equals(id); }
    }
    public static boolean legacyAssimilation() { return on(LEGACY, false); }
    public static boolean masteryOn() { return on(M_ENABLED, true); }
    public static float masteryStart() { return whole(M_START, 10, 1, 50); }
    public static float masteryMax() { return whole(M_MAX, 100, 10, 100); }
    public static double masteryRate() { return num(M_RATE, 1.0, 0.1, 10.0); }
    public static double masteryBoost() { return num(M_BOOST, 3.0, 1.0, 10.0); }
    /** What the curve uses: gainRate times gainBoost, kept in the curve's own 0.1 to 10 range. */
    public static double masteryRateEffective() { return DevilRules.clampD(masteryRate() * masteryBoost(), 0.1, 10.0); }
    public static boolean scrollOn() { return on(SCROLL_ON, true); }
    public static boolean callingOn() { return on(CALL_ON, true); }
    public static double callingPercent() { return num(CALL_PERCENT, DevilCallingRules.DEFAULT_PERCENT, 0.0, 100.0); }
    public static int callingMinutes() { return whole(CALL_MINUTES, DevilCallingRules.DEFAULT_INTERVAL_MINUTES, 1, DevilCallingRules.MAX_INTERVAL_MINUTES); }
    public static int masteryDailyMinutes() { return whole(M_DAILY, 360, 10, 1440); }
    public static boolean kitArts() { return on(M_ARTS, true); }
    public static boolean kitPassives() { return on(M_PASSIVES, true); }
    public static boolean kitFusions() { return on(M_FUSIONS, true); }
    public static boolean npcEnabled() { return on(NPC_ENABLED, true); }
    public static int npcMax() { return DevilNpcRules.clampMax(whole(NPC_MAX, DevilNpcRules.DEFAULT_MAX, 0, DevilNpcRules.MAX_MAX)); }
    public static long npcLifetimeTicks() { return DevilNpcRules.lifetimeTicks(whole(NPC_LIFE, DevilNpcRules.DEFAULT_LIFE_SECONDS, DevilNpcRules.MIN_LIFE_SECONDS, DevilNpcRules.MAX_LIFE_SECONDS)); }
}
