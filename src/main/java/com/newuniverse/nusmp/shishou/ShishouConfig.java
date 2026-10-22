package com.newuniverse.nusmp.shishou;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the server owner decides whether Shishou exists at all ({@code relic.shishouEnabled}, default true: false = nothing spawns, the item is inert, nothing is cast), whether he may speak
 * ({@code relic.shishouVoice}), how rare his natural appearance is, every number that changes combat, and a switch per skill ({@code [skills]}, default true). These change gameplay, so they are SERVER
 * settings. File: nusmp-shishou-server.toml (registered by {@link ShishouBoot}); nothing of Shishou changes only looks, so there is no client config.
 */
public final class ShishouConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, VOICE, NATURAL, MINION_EATS_LOOT, MINION_AUTOCAST, HELD_AUTOCAST, ALLOW_BROADCAST, SENSE_NEUTRALS, PATROL, FEED, GRANT_SKILL;
    public static final ModConfigSpec.DoubleValue NATURAL_CHANCE, STRIKE_CAP, VIBRATION_PCT, FLAME_CAP, LIGHTNING_CAP, DIVINE_CAP;
    public static final ModConfigSpec.IntValue DORMANT_KILLS, DORMANT_DAYS, RESERVE_DAYS, STEAL_SCALE, STORAGE_ROWS, MINION_LEASH,
            HUNT_RADIUS, HUNT_SCAN_TICKS, HUNT_EXAMINED, CHARGE_PERCENT, START_LEVEL, EXP_PERCENT, DAMAGE_PERCENT, FEED_PER_CLICK, FEED_GAP;
    private static final Map<ShishouSkill, ModConfigSpec.BooleanValue> SKILLS = new EnumMap<>(ShishouSkill.class);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relic");
        ENABLED = b.comment("Shishou, the talking sword (Akashic Relic, Grade: EX-Singularity). false = nothing spawns, the item is inert, no skill is cast, nothing is sent. The world's record is kept.")
                .define("shishouEnabled", true);
        VOICE = b.comment("Shishou's spoken lines (only lines with a recording exist; none are recorded yet). false = Thought Communication text only. Lines go to the wielder and players within 32 blocks, one per listener per 4 seconds.")
                .define("shishouVoice", true);
        FEED = b.comment("Feeding from the inventory: put a stack (monster drops, magic stones, cores) on the Shishou item in an inventory screen to eat it as experience. Only the wielder. false = the click does nothing.").define("shishouFeed", true);
        b.pop();
        b.push("wild");
        NATURAL = b.comment("A very rare natural appearance: one roll per day near a random online player, only while no Shishou exists, never loading a chunk.").define("naturalSpawn", true);
        NATURAL_CHANCE = b.comment("Chance of the daily roll (0.004 = about once in 250 days).").defineInRange("naturalChancePerDay", ShishouNatural.DEFAULT_CHANCE, 0.0, 1.0);
        DORMANT_KILLS = b.comment("Base of the kills after which the wild sword settles into its pedestal and waits; 2 more are added per sword level (level 10: base + 20), so a fast hunter does not go dormant in minutes. 2.0.0 was a flat 25.").defineInRange("dormantAfterKills", ShishouRules.DORMANT_KILLS, 1, 10000);
        HUNT_RADIUS = b.comment("Blocks (in a cube, up to 24 up and down) in which the wild sword looks for prey. 2.0.0 was 24 blocks and 4 up and down.").defineInRange("huntRadius", ShishouHunt.WILD_RADIUS, ShishouHunt.RADIUS_MIN, ShishouHunt.RADIUS_MAX);
        HUNT_SCAN_TICKS = b.comment("Ticks between two looks for prey while he has no target (one box query each). Lower = quicker to find prey, a little more server work.").defineInRange("huntScanTicks", ShishouHunt.SCAN_TICKS, ShishouHunt.SCAN_TICKS_MIN, ShishouHunt.SCAN_TICKS_MAX);
        HUNT_EXAMINED = b.comment("The most creatures one look examines (a hard cap on the work of a scan).").defineInRange("huntMaxExamined", ShishouHunt.MAX_EXAMINED, ShishouHunt.EXAMINED_MIN, ShishouHunt.EXAMINED_MAX);
        CHARGE_PERCENT = b.comment("Speed of the telekinetic charge in percent (100 = about 23 blocks a second at level 10 and up to 38 at the top).").defineInRange("chargeSpeedPercent", 100, 10, 300);
        PATROL = b.comment("A wild sword with no prey in range flies toward the nearest player (that is where monsters spawn) instead of wandering at random. It never targets a player.").define("patrolToPlayers", true);
        DORMANT_DAYS = b.comment("Days of wandering after which the wild sword settles into its pedestal.").defineInRange("dormantAfterDays", ShishouRules.DORMANT_DAYS, 1, 1000);
        RESERVE_DAYS = b.comment("Days a recovered, dormant sword is kept for its old wielder before anyone may claim it.").defineInRange("reserveDays", ShishouRules.RESERVE_DAYS, 0, 1000);
        b.pop();
        b.push("progress");
        START_LEVEL = b.comment("The level Shishou is raised to at least (never lowered). The default 10 opens Tier 1 and the Multilayer Barrier from the start. 2.0.0 started at level 1.").defineInRange("startLevel", ShishouRules.START_LEVEL, 1, 60);
        EXP_PERCENT = b.comment("Experience he gains from kills, stones and feeding, in percent of the listed values (100 = as listed).").defineInRange("expPercent", 100, 10, 1000);
        b.pop();
        b.push("combat");
        DAMAGE_PERCENT = b.comment("How steeply his attack and strike grow with the level, in percent (100 = as listed: 8 at the start, about 38 at level 50, 78 at 100). The start (a low magisteel sword) does not change.").defineInRange("damageScalePercent", 100, 0, 500);
        STRIKE_CAP = b.comment("Most damage of one telekinetic strike (2.0.0: 60).").defineInRange("strikeCap", ShishouRules.STRIKE_CAP, 1.0, 1000.0);
        VIBRATION_PCT = b.comment("Vibration Magic: the share of a hit that bypasses armour as extra damage (never above this).").defineInRange("vibrationShare", 0.2, 0.0, 0.5);
        FLAME_CAP = b.comment("Black Flame: most damage of one pulse (every half second for five seconds).").defineInRange("blackFlamePulseCap", 5.0, 0.5, 50.0);
        LIGHTNING_CAP = b.comment("Black Lightning: most damage of the bolt.").defineInRange("blackLightningCap", 80.0, 1.0, 1000.0);
        DIVINE_CAP = b.comment("Divine Sword Release: most damage of the pillar.").defineInRange("divineCap", ShishouRules.DIVINE_CAP, 1.0, 1000.0);
        STEAL_SCALE = b.comment("Skill Eater: percent of the steal table's chances (100 = as listed, 0 = nothing is ever stolen).").defineInRange("stealChancePercent", 100, 0, 300);
        b.pop();
        b.push("minion");
        MINION_EATS_LOOT = b.comment("true = a thrown Shishou also eats the drops of the monsters he kills (like the wild one). Default false: the drops stay for the wielder.").define("minionEatsLoot", false);
        MINION_AUTOCAST = b.comment("The thrown Shishou fires his absorbed spells on his own, on his own cooldowns, with the sword's mana.").define("minionAutoCast", true);
        HELD_AUTOCAST = b.comment("While held, he also fires an absorbed spell on his own at what the wielder is fighting (sword mana).").define("heldAutoCast", false);
        MINION_LEASH = b.comment("Blocks around the wielder in which a thrown Shishou hunts (2.0.0: 16).").defineInRange("minionLeash", 24, 4, 48);
        b.pop();
        b.push("telepathy");
        ALLOW_BROADCAST = b.comment("Thought Communication may be switched to nearby players by the wielder (/multiverse shishou broadcast on). Off by default for each wielder either way.").define("allowBroadcast", true);
        b.pop();
        b.push("sense");
        SENSE_NEUTRALS = b.comment("Magic Sense also marks neutral mobs (default: hostile and neutral). false = hostile only. Players are never marked.").define("markNeutral", true);
        b.pop();
        b.push("feed");
        FEED_PER_CLICK = b.comment("The most items eaten by one click (a left click eats the whole stack up to this, a right click eats one).").defineInRange("feedPerClick", 64, 1, 64);
        FEED_GAP = b.comment("Ticks between two feeding clicks of one player.").defineInRange("feedGapTicks", 4, 1, 100);
        b.pop();
        b.push("skill");
        GRANT_SKILL = b.comment("The wielder is given the Tensura Ultimate skill Shishou when he claims the sword and loses it when the sword is no longer his. false = no skill is given (the /multiverse shishou cast route still works).").define("grantUltimateSkill", true);
        b.pop();
        b.push("storage");
        STORAGE_ROWS = b.comment("Rows of Stomach, the internal storage (3 = 27 slots, up to 6 = 54).").defineInRange("rows", ShishouRules.STORAGE_ROWS_DEFAULT, 3, 6);
        b.pop();
        b.push("skills");
        for (ShishouSkill s : ShishouSkill.values()) SKILLS.put(s, b.comment(s.english() + " (" + s.romaji() + "): false = the skill does nothing.").define(s.configKey(), true));
        b.pop();
        SPEC = b.build();
    }

    private ShishouConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean enabled() { return get(ENABLED); }
    public static boolean voice() { return get(VOICE); }
    public static boolean natural() { return get(NATURAL); }
    public static double naturalChance() { return get(NATURAL_CHANCE); }
    public static int dormantKills() { return get(DORMANT_KILLS); }
    public static int dormantDays() { return get(DORMANT_DAYS); }
    public static int reserveDays() { return get(RESERVE_DAYS); }
    public static double strikeCap() { return get(STRIKE_CAP); }
    public static double vibrationShare() { return get(VIBRATION_PCT); }
    public static double flameCap() { return get(FLAME_CAP); }
    public static double lightningCap() { return get(LIGHTNING_CAP); }
    public static double divineCap() { return get(DIVINE_CAP); }
    public static int stealScale() { return get(STEAL_SCALE); }
    public static boolean minionEatsLoot() { return get(MINION_EATS_LOOT); }
    public static boolean minionAutoCast() { return get(MINION_AUTOCAST); }
    public static boolean heldAutoCast() { return get(HELD_AUTOCAST); }
    public static int minionLeash() { return get(MINION_LEASH); }
    public static boolean allowBroadcast() { return get(ALLOW_BROADCAST); }
    public static boolean senseNeutrals() { return get(SENSE_NEUTRALS); }
    public static boolean patrol() { return get(PATROL); }
    public static boolean feed() { return enabled() && get(FEED); }
    public static boolean grantSkill() { return enabled() && get(GRANT_SKILL); }
    public static int huntRadius() { return get(HUNT_RADIUS); }
    public static int huntScanTicks() { return get(HUNT_SCAN_TICKS); }
    public static int huntExamined() { return get(HUNT_EXAMINED); }
    public static int chargePercent() { return get(CHARGE_PERCENT); }
    public static int startLevel() { return Math.max(ShishouRules.START_LEVEL, get(START_LEVEL)); }
    public static int expPercent() { return get(EXP_PERCENT); }
    public static int damagePercent() { return get(DAMAGE_PERCENT); }
    public static int feedPerClick() { return get(FEED_PER_CLICK); }
    public static int feedGap() { return get(FEED_GAP); }
    public static int storageRows() { return ShishouStorageRules.rows(get(STORAGE_ROWS)); }

    /** Is the skill switched on (and Shishou enabled at all)? */
    public static boolean skillOn(ShishouSkill s) { return enabled() && get(SKILLS.get(s)); }
}
