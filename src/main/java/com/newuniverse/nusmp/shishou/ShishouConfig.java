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
    public static final ModConfigSpec.BooleanValue ENABLED, VOICE, NATURAL, MINION_EATS_LOOT, MINION_AUTOCAST, HELD_AUTOCAST, ALLOW_BROADCAST, SENSE_NEUTRALS;
    public static final ModConfigSpec.DoubleValue NATURAL_CHANCE, STRIKE_CAP, VIBRATION_PCT, FLAME_CAP, LIGHTNING_CAP, DIVINE_CAP;
    public static final ModConfigSpec.IntValue DORMANT_KILLS, DORMANT_DAYS, RESERVE_DAYS, STEAL_SCALE, STORAGE_ROWS, MINION_LEASH;
    private static final Map<ShishouSkill, ModConfigSpec.BooleanValue> SKILLS = new EnumMap<>(ShishouSkill.class);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relic");
        ENABLED = b.comment("Shishou, the talking sword (Akashic Relic, Grade: EX-Singularity). false = nothing spawns, the item is inert, no skill is cast, nothing is sent. The world's record is kept.")
                .define("shishouEnabled", true);
        VOICE = b.comment("Shishou's spoken lines (only lines with a recording exist; none are recorded yet). false = Thought Communication text only. Lines go to the wielder and players within 32 blocks, one per listener per 4 seconds.")
                .define("shishouVoice", true);
        b.pop();
        b.push("wild");
        NATURAL = b.comment("A very rare natural appearance: one roll per day near a random online player, only while no Shishou exists, never loading a chunk.").define("naturalSpawn", true);
        NATURAL_CHANCE = b.comment("Chance of the daily roll (0.004 = about once in 250 days).").defineInRange("naturalChancePerDay", ShishouNatural.DEFAULT_CHANCE, 0.0, 1.0);
        DORMANT_KILLS = b.comment("Kills after which the wild sword settles into its pedestal and waits.").defineInRange("dormantAfterKills", ShishouRules.DORMANT_KILLS, 1, 10000);
        DORMANT_DAYS = b.comment("Days of wandering after which the wild sword settles into its pedestal.").defineInRange("dormantAfterDays", ShishouRules.DORMANT_DAYS, 1, 1000);
        RESERVE_DAYS = b.comment("Days a recovered, dormant sword is kept for its old wielder before anyone may claim it.").defineInRange("reserveDays", ShishouRules.RESERVE_DAYS, 0, 1000);
        b.pop();
        b.push("combat");
        STRIKE_CAP = b.comment("Most damage of one telekinetic strike.").defineInRange("strikeCap", ShishouRules.STRIKE_CAP, 1.0, 1000.0);
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
        MINION_LEASH = b.comment("Blocks around the wielder in which a thrown Shishou hunts.").defineInRange("minionLeash", ShishouRules.MINION_LEASH, 4, 48);
        b.pop();
        b.push("telepathy");
        ALLOW_BROADCAST = b.comment("Thought Communication may be switched to nearby players by the wielder (/multiverse shishou broadcast on). Off by default for each wielder either way.").define("allowBroadcast", true);
        b.pop();
        b.push("sense");
        SENSE_NEUTRALS = b.comment("Magic Sense also marks neutral mobs (default: hostile and neutral). false = hostile only. Players are never marked.").define("markNeutral", true);
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
    public static int storageRows() { return ShishouStorageRules.rows(get(STORAGE_ROWS)); }

    /** Is the skill switched on (and Shishou enabled at all)? */
    public static boolean skillOn(ShishouSkill s) { return enabled() && get(SKILLS.get(s)); }
}
