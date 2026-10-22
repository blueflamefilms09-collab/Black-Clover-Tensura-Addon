package com.newuniverse.nusmp.spiritdark;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Spirit of Darkness exists at all ({@code magic.spiritDark}, default true; false = the spirit's gameplay is off: nothing can be linked, a linked spirit is
 * dismissed at the next upkeep) and how many of the things it puts in the world may exist across the whole server at once. These change gameplay, so they are SERVER settings. File:
 * nusmp-spiritdark-server.toml (registered by {@link SpiritDarkBoot}).
 */
public final class SpiritDarkConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPIRIT_DARK, SPIRIT_DARK_VOICE;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_POWERED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        SPIRIT_DARK = b.comment("The Spirit of Darkness: a shadow-wisp companion a Dark Magic caster links with its key; it steals a little life on melee strikes, marks a struck foe with Wither or Blindness",
                "(short, once per foe per five seconds) and Shadow Steps its owner a short validated distance. A powered demon form may be called for fifteen seconds.",
                "false = the spirit's gameplay is off (nothing can be linked; a linked spirit is dismissed).")
                .define("spiritDark", true);
        SPIRIT_DARK_VOICE = b.comment("The Spirit of Darkness speaks (short spoken lines, to players within 32 blocks, at most one line per four seconds per listener). false = silent apart from its sound cues.").define("spiritDarkVoice", true);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most Spirit of Darkness companions linked across the whole server (one per caster).").defineInRange("maxSpirits", SpiritDarkRules.DEFAULT_MAX_SPIRITS, 1, 256);
        MAX_POWERED = b.comment("Most powered demon forms out across the whole server (one per caster, fifteen seconds each).").defineInRange("maxPowered", SpiritDarkRules.DEFAULT_MAX_POWERED, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    private SpiritDarkConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(SPIRIT_DARK); }
    public static boolean voice() { return get(SPIRIT_DARK_VOICE); }
    public static int maxSpirits() { return get(MAX_SPIRITS); }
    public static int maxPowered() { return get(MAX_POWERED); }
}
