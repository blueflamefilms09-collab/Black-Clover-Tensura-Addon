package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Why: everything the party SEES and HEARS of the poise layer is here, in one place, drawn only with the mod's own particle facade ({@code Fx}: the themed, budgeted, quality-scaled
 * particles; nothing vanilla, no Rukh): the outline of the ground a telegraphed attack will hit (traced from the same geometry the hit test uses, with a smaller copy growing inside it so the
 * party can read how long is left), the flash and the shield-break sound when a bar breaks, and the cue of the desperation phase (a dark burst and ring when it starts, embers around the
 * bearer while it lasts). A short action-bar line tells the players nearby what happened. Server side only; every call goes through the facade's budget, so a room full of telegraphs never
 * floods the client.
 */
public final class PoiseCue {
    private PoiseCue() {}

    /** The theme of the warnings: the squad's battle sparks (gold) for the light tone, ash and blood for the dark one. */
    public static final ThemeKey THEME = ThemeKey.of("dungeon.squad");
    /** The hit area is a little bigger than the radius the ability states (the runner counts a target within sqrt(r*r+4)), so the outline is drawn that much wider and the warning is never smaller than the hit. */
    public static final double PAD = 0.5;
    private static final double NOTICE_RANGE = 32.0;

    /**
     * Traces the warning of one telegraphed attack: the outline of the shape at its full size and a copy of {@code progress} (0 to 1) of that size inside it.
     *
     * @param yawRad direction of the attack, 0 = +x, counter-clockwise (see {@link TelegraphGeometry#toWorld})
     */
    public static void shape(ServerLevel level, TelegraphShape shape, double radius, double angleDeg, double length, double halfWidth, boolean dark, double ox, double oy, double oz, double yawRad,
                             double progress) {
        if (level == null || shape == null || shape == TelegraphShape.NONE) return;
        FxTone tone = FxTone.of(dark);
        trace(level, tone, TelegraphGeometry.outline(shape, radius + PAD, angleDeg, length + PAD, halfWidth + 0.2, 24), ox, oy, oz, yawRad);
        double p = Math.max(0.15, Math.min(1.0, progress));
        trace(level, tone, TelegraphGeometry.outline(shape, (radius + PAD) * p, angleDeg, (length + PAD) * p, halfWidth * p + 0.1, 10), ox, oy, oz, yawRad);
    }

    private static void trace(ServerLevel level, FxTone tone, List<double[]> points, double ox, double oy, double oz, double yawRad) {
        for (int i = 0, n = points.size(); i < n; i++) {
            double[] p = points.get(i);
            double[] w = TelegraphGeometry.toWorld(ox, oz, yawRad, p[0], p[1]);
            Fx.spray(level, THEME, tone, w[0], oy + 0.12, w[1], 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The bar broke: a flash, a ring, the sound of a shield giving way, and a line for the players near. */
    public static void broken(ServerLevel level, Mob m) {
        Fx.burst(level, THEME, m, FxTone.LIGHT, 12);
        Fx.ring(level, THEME, FxTone.LIGHT, m.getX(), m.getY() + 0.1, m.getZ(), 1.6, 16);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.9f, 0.8f);
        notice(level, m, Component.translatableWithFallback("message.nusmp.poise.broken", "%s loses its footing!", m.getDisplayName()).withStyle(ChatFormatting.GOLD));
    }

    /** The desperation phase begins: a dark burst and ring, a low roar, a line for the players near. */
    public static void desperate(ServerLevel level, Mob m) {
        Fx.burst(level, THEME, m, FxTone.DARK, 12);
        Fx.ring(level, THEME, FxTone.DARK, m.getX(), m.getY() + 0.1, m.getZ(), 2.5, 20);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 0.7f);
        notice(level, m, Component.translatableWithFallback("message.nusmp.poise.desperate", "%s is desperate!", m.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
    }

    /** The embers around a desperate bearer (called every half second while it lasts). */
    public static void aura(ServerLevel level, Mob m) {
        Fx.aura(level, THEME, m, FxTone.DARK);
    }

    private static void notice(ServerLevel level, Mob m, Component text) {
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(m) <= NOTICE_RANGE * NOTICE_RANGE) p.displayClientMessage(text, true);
        }
    }
}
