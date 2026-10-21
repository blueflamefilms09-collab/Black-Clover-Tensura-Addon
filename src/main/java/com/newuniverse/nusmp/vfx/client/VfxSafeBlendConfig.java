package com.newuniverse.nusmp.vfx.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why (docs/vfx_square_bug.md): the safe path of the square-bug fix can be forced from a file, without a new build. {@code vfx.safeBlend} in {@code config/nusmp-vfx-client.toml} (a CLIENT config).
 * Off (the default): only a texture that carries a colour under its transparent texels is drawn with the alpha-weighted additive blend, everything else exactly as before. On: every additive
 * aura sprite and glow map uses the alpha-weighted blend (the vertex alpha then fades it, as the painters were written to assume), and the VFX vertex buffer also swaps any additive flat sprite that
 * came out opaque for the generated soft circle. 1.110.0: the emissive passes of the weapons and relics ({@code SafeAdditive#emissive}) then also take the alpha-weighted additive type.
 */
public final class VfxSafeBlendConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SAFE_BLEND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        SAFE_BLEND = b.comment("Force the safe blend path for every additive aura sprite and glow (alpha-weighted, so a transparent corner can never show as a solid square), and swap opaque flat sprites of the "
                        + "effect engine for a generated soft circle. Off = only the textures that need it take the safe path.")
                .define("safeBlend", false);
        b.pop();
        SPEC = b.build();
    }

    private VfxSafeBlendConfig() {}

    /** True when the file asks for the safe path; false before the file is read. */
    public static boolean safeBlend() {
        try { return SAFE_BLEND.get(); } catch (IllegalStateException notLoaded) { return false; }
    }
}
