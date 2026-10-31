package com.newuniverse.nusmp.arc;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the switches of the characters of the arcs, {@code nusmp-arc-server.toml}, section {@code [arc]}. A server owner must be able to turn the whole
 * layer off, open every boss without the story chain, make the friendly characters killable, and tune how often the dungeon puts an arc boss on a floor,
 * without editing data files. Its own file (not more keys in an older config) so the layer ships and is documented on its own. Every value is a plain
 * {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader
 * goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class ArcConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, STORY_GATE, NPCS_MORTAL, FLOOR_BOSSES, SHARE_CREDIT, TIER_BOSSES, LIGHT_DEATH_NOTE_LETHAL, TIER_POWERS;
    public static final ModConfigSpec.DoubleValue FLOOR_BOSS_CHANCE, TIER_POWER_SCALE;
    public static final ModConfigSpec.IntValue NPC_WANDER, TALK_COOLDOWN, TIER_BOSS_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("arc");
        ENABLED = b.comment("Master switch of the characters of the arcs: when off, nobody talks, no story credit is given, no character can be summoned or spawned from an egg.")
                .define("enabled", true);
        STORY_GATE = b.comment("A boss can be summoned at its arena only after the player (or someone in their party) has defeated the bosses it follows in the story. False opens every boss.")
                .define("storyGate", true);
        NPCS_MORTAL = b.comment("False (default): a friendly character cannot be hurt by anything except /kill, the void and creative players. True: they can be killed like any mob.")
                .define("npcsMortal", false);
        FLOOR_BOSSES = b.comment("The Mega-Dungeon may put a boss of the arcs on a floor (its roads, floors and themes are set per character).")
                .define("floorBosses", true);
        FLOOR_BOSS_CHANCE = b.comment("Chance (0 to 1) that a floor that can have an arc boss gets one.")
                .defineInRange("floorBossChance", 0.5, 0.0, 1.0);
        SHARE_CREDIT = b.comment("When a boss falls, the online FTB Teams members of the fighters (within the quests' share radius) get the story credit too.")
                .define("shareStoryCredit", true);
        NPC_WANDER = b.comment("How far (blocks) a friendly character wanders from the post it was placed at. 0 = it stands still.")
                .defineInRange("npcWanderRadius", 6, 0, 32);
        TALK_COOLDOWN = b.comment("Ticks between two conversations with the same character (stops a held right-click from flooding the chat).")
                .defineInRange("talkCooldownTicks", 20, 0, 600);
        TIER_BOSSES = b.comment("3.33.0: the Tier List characters (tiers F, D, ...) may stand as bosses of the Mega-Dungeon and be summoned. False restores the dungeon without them.")
                .define("tierBosses", true);
        TIER_BOSS_MAX = b.comment("3.33.0: how many Tier List bosses may be alive in the whole server at once (a hard cap on top of the floors' own limits).")
                .defineInRange("tierBossMax", 6, 1, 24);
        LIGHT_DEATH_NOTE_LETHAL = b.comment("3.36.0: Light Yagami's Death Note scene ends in a REAL death of the player it writes (a normal death: keepInventory, respawn and the Totem of Undying work as usual; creative and spectator players are skipped). False = the scene plays the same but ends in a harmless joke and nobody dies. A SERVER setting because it changes gameplay.")
                .define("lightDeathNoteLethal", true);
        TIER_POWERS = b.comment("3.39.0: the anime powers of the Tier List bosses (their trait: slow field, Hierro armour, the Balance, Karma marks ... and the new statuses poison, shield break, strip). False = the bosses fight with their plain moves only (a few move cooldowns were retuned and stay).")
                .define("tierPowers", true);
        TIER_POWER_SCALE = b.comment("3.39.0: multiplies the STRENGTH of every boss trait (radius, damage cut, percentages; never a duration). 1.0 = as designed (docs/bosses/tier_powers.md), 0.5 = half as strong, 2.0 = twice as strong.")
                .defineInRange("tierPowerScale", 1.0, 0.5, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private ArcConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean storyGate() { return get(STORY_GATE); }

    public static boolean npcsMortal() { return get(NPCS_MORTAL); }

    public static boolean floorBosses() { return enabled() && get(FLOOR_BOSSES); }

    public static double floorBossChance() { return get(FLOOR_BOSS_CHANCE); }

    public static boolean shareCredit() { return get(SHARE_CREDIT); }

    public static int npcWander() { return get(NPC_WANDER); }

    public static int talkCooldown() { return get(TALK_COOLDOWN); }

    public static boolean tierBosses() { return enabled() && get(TIER_BOSSES); }

    public static int tierBossMax() { return get(TIER_BOSS_MAX); }

    /** Test hook only (the server self-test forces the powers off to prove the old behaviour comes back); null in the real game. */
    public static volatile Boolean tierPowersOverride;

    /** 3.39.0: do the Tier List bosses use their anime powers (traits and the new statuses)? */
    public static boolean tierPowers() {
        Boolean o = tierPowersOverride;
        return o != null ? o && enabled() : enabled() && get(TIER_POWERS);
    }

    /** 3.39.0: the strength multiplier of every boss trait, 0.5 to 2.0 (1.0 while the config is not loaded). */
    public static double tierPowerScale() { return Math.max(0.5, Math.min(2.0, get(TIER_POWER_SCALE))); }

    /** 3.36.0: does Light's Death Note really kill (true, the default) or end in a harmless joke? */
    public static boolean lightDeathNoteLethal() { return get(LIGHT_DEATH_NOTE_LETHAL); }
}
