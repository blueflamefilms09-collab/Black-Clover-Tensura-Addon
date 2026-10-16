package com.newuniverse.nusmp.thread;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Vanessa's Thread Magic got a real Rouge, three new pages and a toggle, and the owner wants each of them tunable (the price of a
 * rewrite most of all) and the old Rouge (the cat on the head, a 12% dodge, one death undo) reachable again with one switch. Own spec
 * file ({@code nusmp-thread-server.toml}, SERVER); the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, ThreadConfig.SPEC, "nusmp-thread-server.toml")}. Plain
 * {@code define}/{@code defineInRange} only. Reads fall back to the defaults before the config is loaded.
 */
public final class ThreadConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ROUGE_ENTITY, ROUGE_ON_HEAD;
    public static final ModConfigSpec.IntValue ROUGE_MINUTES, ROUGE_MAX_ALLIES, ROUGE_BOSS_COOLDOWN_SECONDS, TRAP_MAX, TRAP_SECONDS, ARREST_SECONDS;
    public static final ModConfigSpec.DoubleValue ROUGE_BIG_HIT_FRACTION, ROUGE_MANA_FLAT, ROUGE_MANA_PERCENT, REINFORCE_DRAIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("thread");
        ROUGE_ENTITY = b.comment("true: the Rouge page calls a real red thread cat that watches your allies and rewrites deadly blows (costs a lot of mana each time).",
                        "false: the 1.34 to 0.49 Rouge untouched (a cat on your head, a 12% dodge, one death undo a day).")
                .define("rougeEntity", true);
        ROUGE_ON_HEAD = b.comment("true (1.46.0): Rouge rests ON THE CASTER'S HEAD when she has nothing to do, drawn as the old head cat.",
                        "false: the 1.44.0 shoulder perch.").define("rougeOnHead", true);
        ROUGE_MINUTES = b.comment("Minutes Rouge stays after the page is cast.").defineInRange("rougeMinutes", 10, 1, 120);
        ROUGE_MAX_ALLIES = b.comment("How many hand-picked allies one caster can name (Sneak + cast on a player or pet). Party members and pets are watched on top of this.")
                .defineInRange("rougeMaxAllies", 6, 0, 32);
        ROUGE_BIG_HIT_FRACTION = b.comment("A blow of at least this fraction of the victim's maximum health is rewritten even if it would not kill.")
                .defineInRange("rougeBigHitFraction", 0.35, 0.05, 1.0);
        ROUGE_MANA_FLAT = b.comment("Mana (magicule) a rewrite costs, flat part.").defineInRange("rougeManaFlat", 150.0, 0.0, 1.0e9);
        ROUGE_MANA_PERCENT = b.comment("Mana a rewrite costs, percent of the caster's maximum, added to the flat part. Doubled against a boss.")
                .defineInRange("rougeManaPercent", 20.0, 0.0, 100.0);
        ROUGE_BOSS_COOLDOWN_SECONDS = b.comment("After Rouge rewrites a boss's blow on someone, the next boss blow on that person lands for this many seconds.")
                .defineInRange("rougeBossCooldownSeconds", 8, 0, 600);
        TRAP_MAX = b.comment("Most thread traps one caster can have out at once (the oldest vanish first).").defineInRange("trapMax", 8, 1, 32);
        TRAP_SECONDS = b.comment("Seconds a thread trap lasts.").defineInRange("trapSeconds", 90, 5, 3600);
        REINFORCE_DRAIN = b.comment("Reinforcement Magic and Mana Skin: percent of your maximum mana burned each second while it is on.")
                .defineInRange("reinforceDrain", 1.5, 0.0, 50.0);
        ARREST_SECONDS = b.comment("Base seconds Arresting Dark Weave holds a target (the balance law shortens it for players and bosses).")
                .defineInRange("arrestSeconds", 4, 1, 30);
        b.pop();
        SPEC = b.build();
    }

    private ThreadConfig() {}

    public static boolean rougeEntity() { return get(ROUGE_ENTITY); }
    public static boolean rougeOnHead() { return get(ROUGE_ON_HEAD); }
    public static int rougeTicks() { return get(ROUGE_MINUTES) * 1200; }
    public static int rougeMaxAllies() { return get(ROUGE_MAX_ALLIES); }
    public static int trapMax() { return get(TRAP_MAX); }
    public static int trapTicks() { return get(TRAP_SECONDS) * 20; }
    public static int arrestTicks() { return get(ARREST_SECONDS) * 20; }
    public static double reinforceDrain() { return get(REINFORCE_DRAIN); }

    /** The fate rule settings as the pure rules want them. */
    public static FateRules.Params fateParams() {
        return new FateRules.Params(get(ROUGE_BIG_HIT_FRACTION), get(ROUGE_MANA_FLAT), get(ROUGE_MANA_PERCENT), get(ROUGE_BOSS_COOLDOWN_SECONDS) * 20, 10);
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
