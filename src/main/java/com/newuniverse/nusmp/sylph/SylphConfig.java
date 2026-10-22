package com.newuniverse.nusmp.sylph;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Sylph applies ({@code magic.sylph}, default true; false = Yuno's wind pages leave the caster the old way, there is no link and no powered form, and the
 * exclusive Sylph Spirit Lord keeps the older fairy look) and how many Sylphs may exist across the whole server at once. These change gameplay, so they are SERVER settings (NeoForge sends a server
 * config to the clients, so the Spirit Lord's renderer reads the same switch). File: nusmp-sylph-server.toml (registered by {@link SylphBoot}).
 */
public final class SylphConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SYLPH, POSES, VOICES;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_POWERED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        SYLPH = b.comment("The Sylph, Yuno's wind Spirit Lord as a small winged girl: she floats beside the caster, every wind page leaves her hands and a bigger powered form joins the big pages.",
                "false = the old look (the exclusive Sylph keeps its earlier fairy model) and the generic casting; the pages, their numbers and their cooldowns are the same either way.")
                .define("sylph", true);
        b.pop();
        b.push("moods");
        POSES = b.comment("Sylph's moods: she takes a pose and a face for what happens (a wave when linked, a flinch when you are hit, worry when you are low, a cheer when you kill, a nap when you stand still).",
                "false = the old behaviour: her clip follows the clock and her casts only.").define("poses", true);
        VOICES = b.comment("Sylph speaks short spoken lines for those moods to players within 32 blocks (at most one line per listener every 4 seconds; also needs the voice system's own settings: voice.maxPerSecond above 0, and the",
                "client's voice switch and volume). false = she stays silent apart from her chimes, gusts and giggle sounds.").define("voices", true);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most small Sylphs linked across the whole server (one per caster).").defineInRange("maxSpirits", SylphRules.DEFAULT_MAX_SPIRITS, 1, 256);
        MAX_POWERED = b.comment("Most powered Sylphs out across the whole server (one per caster, a few seconds each).").defineInRange("maxPowered", SylphRules.DEFAULT_MAX_POWERED, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    private SylphConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(SYLPH); }
    public static boolean poses() { return get(POSES); }
    public static boolean voices() { return get(VOICES); }
    public static int maxSpirits() { return get(MAX_SPIRITS); }
    public static int maxPowered() { return get(MAX_POWERED); }
}
