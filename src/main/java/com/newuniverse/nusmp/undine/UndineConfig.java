package com.newuniverse.nusmp.undine;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Undine applies ({@code magic.undine}, default true; false = Noelle casts the Water grimoire the old generic way and the Spirit Lord of Water is drawn
 * and heard as before, nothing else changes) and how many of her may exist across the whole server at once. These change gameplay, so they are SERVER settings (NeoForge syncs a server config to
 * the clients, which is how the renderer knows). File: nusmp-undine-server.toml (registered by {@link UndineBoot}).
 */
public final class UndineConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue UNDINE;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_FULL, SOUNDS_PER_SECOND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        UNDINE = b.comment("The Undine: Noelle's water Spirit Lord as a tall woman of water (a companion beside the caster and a full form for the big pages, with a sound set of her own).",
                "false = the old Spirit Lord look and sound, and Noelle casts the Water grimoire the generic way; the pages, their numbers and their cooldowns are the same either way.")
                .define("undine", true);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most companion Undines linked across the whole server (one per caster).").defineInRange("maxSpirits", UndineRules.DEFAULT_MAX_SPIRITS, 1, 256);
        MAX_FULL = b.comment("Most full-form Undines out across the whole server (one per caster, a few seconds each).").defineInRange("maxFull", UndineRules.DEFAULT_MAX_FULL, 1, 64);
        SOUNDS_PER_SECOND = b.comment("Most Undine sounds the whole server may play in one second.").defineInRange("soundsPerSecond", UndineVoiceBudget.DEFAULT_PER_SECOND, 1, 100);
        b.pop();
        SPEC = b.build();
    }

    private UndineConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup, a client that has not synced yet) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(UNDINE); }

    public static int maxSpirits() { return get(MAX_SPIRITS); }

    public static int maxFull() { return get(MAX_FULL); }

    public static int soundsPerSecond() { return get(SOUNDS_PER_SECOND); }
}
