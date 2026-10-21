package com.newuniverse.nusmp.particle.fx;

import net.minecraft.core.particles.ParticleOptions;

/**
 * Why: the one builder of the particle options of the upgraded items' effect tables, used by every loader (the God melee tables, the Genesis weapon tables, the worn relic tables, the act
 * bursts, the tiny Djinn): a table token ({@code fx}, {@code dust}, {@code minecraft:flame}, {@code nusmp:ember}, ...) becomes options of one of the mod's own particles, dyed with the layer's
 * colour where the particle is tintable. The decision itself is the pure {@link UpgradeParticleAlias}; this class only turns the answer into options. It never answers a vanilla particle and
 * never reads the vanilla particle registry. Called once per layer when a table is loaded (the options are cached by the caller), never per particle.
 */
public final class UpgradeParticleOptions {
    private UpgradeParticleOptions() {}

    /** The largest scale an upgrade effect gives its particle (the tables' sizes run up to 2; a sprite that big is what filled the screen). */
    public static final float MAX_SCALE = 1.0f;
    public static final float MIN_SCALE = 0.4f;

    /** The scale a table size becomes: 0.4 to 1.0. */
    public static float scale(float tableSize) {
        if (Float.isNaN(tableSize)) return 0.7f;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, tableSize));
    }

    /** The options for a table token, a layer colour (0xRRGGBB, or -1) and a table size; never null (a registry that is not bound yet gives the facade's default custom mote). */
    public static ParticleOptions of(String token, int rgb, float size) {
        float scale = scale(size);
        String id = UpgradeParticleAlias.resolve(token);
        if (id == null) return FxRelic.dust(rgb < 0 ? 0xFFFFFF : rgb & 0xFFFFFF, scale);
        TintedParticleType type = FxTypes.type(id);
        FxCatalog.Def def = FxCatalog.byId(id);
        if (type == null || def == null) return FxRelic.dust(rgb < 0 ? 0xFFFFFF : rgb & 0xFFFFFF, scale);
        return new TintedParticleOptions(type, def.tint() ? UpgradeParticleAlias.dye(token, rgb) : -1, scale, false, def.tint());
    }

    /** The same for a colour and no token (a hard-coded dust of the old code): the hue-themed custom mote dyed with {@code rgb}. */
    public static ParticleOptions dust(int rgb, float size) {
        return of(UpgradeParticleAlias.FX, rgb, size);
    }
}
