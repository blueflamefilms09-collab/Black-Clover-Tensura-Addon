package com.newuniverse.nusmp.spiritlight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Spirit of Light exists at all ({@code magic.spiritLight}, default true; false = nobody can link it, a linked one is dropped at the next upkeep and
 * Swiftness III goes with it) and how many of the things it puts in the world may exist across the whole server at once. These change gameplay, so they are SERVER settings, in their own file:
 * nusmp-spiritlight-server.toml (registered by {@link SpiritLightBoot}). The damage scale only ever turns the beam DOWN (0..1): no setting lifts a Light Magic cap.
 */
public final class SpiritLightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPIRIT_LIGHT;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_POWERED, MAX_EFFECT_PROPS;
    public static final ModConfigSpec.DoubleValue BEAM_DAMAGE_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        SPIRIT_LIGHT = b.comment("The Spirit of Light: a glowing lion cub (and, powered, a seraph) that a Light Magic caster links. While linked: Swiftness III, a piercing light beam and a Glowing pulse that marks hostile mobs through walls.",
                "false = nobody can link it (and a linked one is dropped); nothing else in Light Magic changes.")
                .define("spiritLight", true);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most Spirits of Light linked across the whole server (one per caster).").defineInRange("maxSpirits", SpiritLightRules.DEFAULT_MAX_SPIRITS, 1, 256);
        MAX_POWERED = b.comment("Most seraph (powered) forms risen across the whole server at once (one per caster).").defineInRange("maxPowered", SpiritLightRules.DEFAULT_MAX_POWERED, 1, 64);
        MAX_EFFECT_PROPS = b.comment("Most beams and pulse rings alive across the whole server at once (each lives a few ticks).").defineInRange("maxEffectProps", SpiritLightRules.DEFAULT_MAX_EFFECT_PROPS, 1, 256);
        BEAM_DAMAGE_SCALE = b.comment("Multiplies the raw damage of the spirit's beam (0 = harmless, 1 = as designed). It can only turn the beam down: the Light Magic caps stay.")
                .defineInRange("beamDamageScale", 1.0, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private SpiritLightConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(SPIRIT_LIGHT); }
    public static int maxSpirits() { return get(MAX_SPIRITS); }
    public static int maxPowered() { return get(MAX_POWERED); }
    public static int maxEffectProps() { return get(MAX_EFFECT_PROPS); }
    public static double beamDamageScale() { return get(BEAM_DAMAGE_SCALE); }
}
