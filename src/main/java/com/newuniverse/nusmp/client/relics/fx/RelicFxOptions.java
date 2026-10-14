package com.newuniverse.nusmp.client.relics.fx;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the worn relic visuals: config/nusmp-relicfx-client.toml. {@code relicfx.quality} (low, medium, high) sets the vertex budget of the worn
 * models and their grade effects and how often the ambient particles spawn ({@link FxQuality}); {@code relicfx.particles} off stops the ambient particles
 * (the models and their glow stay); {@code relicfx.models} off hands every worn relic back to the old plain boxes.
 * Registered once from the client section of the mod constructor:
 * {@code modContainer.registerConfig(ModConfig.Type.CLIENT, RelicFxOptions.SPEC, "nusmp-relicfx-client.toml")}.
 */
public final class RelicFxOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> QUALITY;
    public static final ModConfigSpec.BooleanValue PARTICLES;
    public static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relicfx");
        QUALITY = b.comment("How much the worn relics show: 'low' (models and a faint rim only, at most about 2400 vertices per player), 'medium' (the default: rim, shimmer, orbiting motes, about 4200), "
                        + "'high' (rune rings, halos and denser particles, about 6000).")
                .define("quality", "medium", o -> o instanceof String s && (s.equals("low") || s.equals("medium") || s.equals("high")));   // not defineInList(List.of(..)): List.of(..).contains(null) throws when the file does not exist yet and aborts every config load
        PARTICLES = b.comment("Ambient particles drifting off worn relics (embers, snow, sparks, leaves, wisps, stars). Off = none.")
                .define("particles", true);
        MODELS = b.comment("Draw the worn relics as 3D models. Off = the plain boxes the mod had before.")
                .define("models", true);
        b.pop();
        SPEC = b.build();
    }

    private RelicFxOptions() {}

    public static FxQuality quality() {
        try { return FxQuality.byName(QUALITY.get(), FxQuality.MEDIUM); } catch (IllegalStateException notLoaded) { return FxQuality.MEDIUM; }
    }

    public static boolean particles() { return get(PARTICLES); }

    public static boolean models() { return get(MODELS); }

    private static boolean get(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
