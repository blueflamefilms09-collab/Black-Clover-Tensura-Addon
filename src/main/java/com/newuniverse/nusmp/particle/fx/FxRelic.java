package com.newuniverse.nusmp.particle.fx;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the relic helpers {@code relics.Fx.ring / burst / dust / align} take a colour (0xRRGGBB) and used to answer with Light or Dark Rukh. The Djinn code shares those helpers and keeps
 * its Rukh, so they stay as they are; every other relic (the waves, the sockets, the order, the skills) is moved by the codemod to this class, which does the same job with the custom
 * particles: the colour picks the theme by its hue ({@link FxHue}: red embers, blue bubbles, green leaves, gold glints, ...) AND dyes the tintable particles, so two relics with two colours
 * look different without their code knowing anything about particles. Same geometry and caps as the helpers it replaces.
 */
public final class FxRelic {
    private FxRelic() {}

    private static final double MAX_RADIUS = 32.0;

    /** The theme of a colour. */
    public static ThemeKey theme(int rgb) {
        return ThemeKey.of(FxHue.themeId(rgb));
    }

    /** The theme of a relic grade by its enum name ({@code GOD} gives {@code relic.grade.GOD}: gold-white glints; {@code GENESIS}: stars and nebula). */
    public static ThemeKey grade(String gradeName) {
        return ThemeKey.of("relic.grade." + (gradeName == null ? "NORMAL" : gradeName.toUpperCase(java.util.Locale.ROOT)));
    }

    /** Dark colours take the dark flavour, the rest the light one (replaces {@code relics.Fx.align}). */
    public static FxTone tone(int rgb) {
        return FxHue.tone(rgb);
    }

    /** The particle for a coloured dust of the old code: the colour's theme, dyed with the colour (replaces {@code relics.Fx.dust}). */
    public static ParticleOptions dust(int rgb, float size) {
        ParticleOptions o = Fx.options(theme(rgb), FxSlot.MOTE, tone(rgb));
        if (o instanceof TintedParticleOptions t) return t.tintable() ? t.with(rgb & 0xFFFFFF, size) : t.with(-1, size);
        return Fx.options(theme(rgb), tone(rgb));
    }

    /** A ring of the colour's particles on the ground plus a grimoire circle (replaces {@code relics.Fx.ring}). */
    public static void ring(ServerLevel l, Vec3 at, double radius, int rgb) {
        if (l == null || at == null) return;
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        ThemeKey k = theme(rgb);
        FxTone tone = tone(rgb);
        int n = Mth.clamp((int) (r * 8), 12, 64);
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            Fx.sprayTinted(l, k, FxSlot.MOTE, tone, rgb, 1.0f, at.x + Math.cos(a) * r, at.y + 0.1, at.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
        }
        VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE, at.add(0, 0.08, 0), at.add(0, 1, 0), 0xFF000000 | (rgb & 0xFFFFFF), 24, (float) Math.min(6.0, r * 0.7));
    }

    /** A puff of the colour's particles and sparks (replaces {@code relics.Fx.burst}). */
    public static void burst(ServerLevel l, Vec3 at, int rgb) {
        if (l == null || at == null) return;
        ThemeKey k = theme(rgb);
        Fx.sprayTinted(l, k, FxSlot.MOTE, tone(rgb), rgb, 1.0f, at.x, at.y + 0.6, at.z, FxGeo.MAX_BURST, 0.5, 0.5, 0.5, 0.05);
        Fx.sprayTinted(l, k, FxSlot.SPARK, FxTone.LIGHT, rgb, 1.0f, at.x, at.y + 0.6, at.z, FxGeo.MAX_BURST, 0.4, 0.4, 0.4, 0.08);
    }
}
