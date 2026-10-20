package com.newuniverse.nusmp.soul;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the purification has to read as a soul being washed clean, in the same white-gold and teal the Soul grimoire and the Paladin's
 * halo use, and it has to cost little: server-sent particles in a rising helix whose size follows the creature, plus one existing
 * circle effect (the generic magic circle, no new shape) that follows the target while the channel runs. Everything here is
 * one-shot calls with a handful of particles; nothing is kept between ticks.
 */
final class PaladinFx {
    private PaladinFx() {}

    static final int GOLD = 0xFFFFE9A8, TEAL = 0xFF72D9CC;

    /** The circle that follows the target for the whole channel (the book's magic circle in the purification colour). */
    static void channelStart(ServerLevel sl, Entity target) {
        float size = (float) Math.max(0.8, Math.min(3.0, (target.getBbWidth() + target.getBbHeight()) * 0.5));
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, target, target.position().add(0, 0.1, 0), GOLD, PaladinRules.CHANNEL_TICKS + 10, size);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.6f);
    }

    /** Every other tick of the channel: a helix of light rising off the creature, tighter and brighter as the soul comes clean. */
    static void channelTick(ServerLevel sl, Entity target, int tick) {
        double h = Math.max(0.6, target.getBbHeight());
        double r = Math.max(0.4, target.getBbWidth() * 0.75) * (1.15 - 0.5 * tick / (double) PaladinRules.CHANNEL_TICKS);
        double phase = tick * 0.35;
        for (int arm = 0; arm < 2; arm++) {
            double a = phase + arm * Math.PI;
            double y = ((tick * 0.045) + arm * 0.5) % 1.0;
            Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.END_ROD, target.getX() + Math.cos(a) * r, target.getY() + y * h, target.getZ() + Math.sin(a) * r, 1, 0, 0.02, 0, 0.0);
        }
        if (tick % 6 == 0) Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.SOUL, target.getX(), target.getY() + h * 0.5, target.getZ(), 2, target.getBbWidth() * 0.3, h * 0.3, target.getBbWidth() * 0.3, 0.02);
        if (tick % 20 == 10) sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.7f, 0.8f + tick / 120f);
    }

    /** The soul comes clean: a column of light, a ring of sparks and the circle bursting. */
    static void success(ServerLevel sl, Entity target) {
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double h = target.getBbHeight();
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.END_ROD, c.x, c.y, c.z, 36, target.getBbWidth() * 0.5, h * 0.6, target.getBbWidth() * 0.5, 0.12);
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.GLOW, c.x, c.y, c.z, 14, target.getBbWidth() * 0.5, h * 0.5, target.getBbWidth() * 0.5, 0.05);
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.FIREWORK, c.x, c.y, c.z, 12, target.getBbWidth() * 0.4, h * 0.4, target.getBbWidth() * 0.4, 0.08);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, target.position(), target.position().add(0, 1, 0), GOLD, 22, (float) Math.max(1.0, Math.min(3.0, target.getBbWidth() * 1.4)));
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.0f, 1.3f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.0f, 0.7f);
    }

    /** The purification slips: the light gutters out in grey. */
    static void failure(ServerLevel sl, Entity target) {
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.SMOKE, c.x, c.y, c.z, 18, target.getBbWidth() * 0.4, target.getBbHeight() * 0.4, target.getBbWidth() * 0.4, 0.03);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    /** A Paladin lets go (or comes back to its owner): a short fall of light. */
    static void released(ServerLevel sl, Entity paladin) {
        Vec3 c = paladin.position().add(0, paladin.getBbHeight() * 0.5, 0);
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.END_ROD, c.x, c.y, c.z, 14, paladin.getBbWidth() * 0.4, paladin.getBbHeight() * 0.4, paladin.getBbWidth() * 0.4, 0.04);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 0.9f);
    }

    /** The recall: a flash at the place a Paladin left and at the place it arrives. */
    static void recall(ServerLevel sl, Vec3 at, double height) {
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.END_ROD, at.x, at.y + height * 0.5, at.z, 10, 0.3, height * 0.4, 0.3, 0.05);
        Fx.spray(sl, FxThemes.SKILL_PALADIN, FxOld.ENCHANT, at.x, at.y + height * 0.5, at.z, 10, 0.4, height * 0.4, 0.4, 0.3);
    }
}
