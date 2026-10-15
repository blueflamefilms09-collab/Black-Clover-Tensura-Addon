package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * What the Order looks like from afar: white-gold rings, columns of glyphs (the enchanting table's letters rise as the Order's script), the beam that
 * burns a duplicate to gold dust. Everything is server-sent particles and sounds, bounded per call, so no client code is involved.
 */
final class OrderFx {
    private OrderFx() {}

    static final int GOLD = 0xE8B923, WHITE_GOLD = 0xFFF0B8, PALE = 0xFFFAE6;

    /** The start of a telegraph: a spinning circle on the ground and a note that rises. */
    static void telegraphStart(ServerLevel sl, Vec3 spot) {
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, spot.add(0, 0.05, 0), spot.add(0, 1, 0), 0xFF000000 | GOLD, OrderPlan.TELEGRAPH_TICKS, 3.0f);
        OrderVfx.arrival(sl, spot);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(spot), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.5f, 0.6f);
    }

    /** One beat of the telegraph (call every few ticks): a ring that closes in and a column of glyphs. 'progress' is 0..1. */
    static void telegraph(ServerLevel sl, Vec3 spot, float progress) {
        double r = 4.0 - 3.0 * progress;
        Tells.ring(sl, spot, r, GOLD, 1.2f, 40);
        Tells.column(sl, spot, 4.0 + 2.0 * progress, WHITE_GOLD);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, spot.x, spot.y + 1.2, spot.z, 14, 0.8, 1.1, 0.8, 0.6);
        if (progress > 0.7f) com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, spot.x, spot.y + 0.3, spot.z, 4, 0.3, 0.1, 0.3, 0.12);
    }

    /** The God steps out of the column. */
    static void appear(ServerLevel sl, Vec3 spot) {
        OrderVfx.stepOut(sl, spot);
        Tells.burst(sl, spot.add(0, 1.4, 0), WHITE_GOLD, 40);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, spot.x, spot.y + 1.6, spot.z, 30, 0.4, 1.4, 0.4, 0.08);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(spot), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    /** While it reads: a ring of glyphs turning round it. */
    static void reading(ServerLevel sl, Vec3 godAt, int tick) {
        if (tick < 4) OrderVfx.census(sl, godAt);
        double a0 = tick * 0.12;
        for (int i = 0; i < 6; i++) {
            double a = a0 + i * Math.PI / 3.0;
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, godAt.x + Math.cos(a) * 1.6, godAt.y + 2.4 + 0.2 * Math.sin(a * 2), godAt.z + Math.sin(a) * 1.6, 1, 0, 0, 0, 0);
        }
        if (tick % 10 == 0) Tells.ring(sl, godAt, 2.2, GOLD, 1.0f, 24);
    }

    /** The beam from the staff to the duplicate (call every couple of ticks). */
    static void beam(ServerLevel sl, Vec3 from, Vec3 to, float progress) {
        if (progress < 0.06f) OrderVfx.correction(sl, from, to);
        Tells.line(sl, from, to, WHITE_GOLD);
        Vec3 head = from.add(to.subtract(from).scale(Math.min(1.0, 0.3 + progress)));
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, head.x, head.y, head.z, 3, 0.1, 0.1, 0.1, 0.02);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, to.x, to.y, to.z, 2, 0.3, 0.3, 0.3, 0.1);
    }

    /** The duplicate burns to gold dust. */
    static void dust(ServerLevel sl, Vec3 at) {
        OrderVfx.burned(sl, at);
        Tells.burst(sl, at, GOLD, 50);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 24, 0.4, 0.5, 0.4, 0.3);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 16, 0.3, 0.4, 0.3, 0.12);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0f, 0.7f);
    }

    /** The God goes. */
    static void leave(ServerLevel sl, Vec3 at) {
        OrderVfx.farewell(sl, at);
        Tells.burst(sl, at.add(0, 1.4, 0), PALE, 30);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, at.x, at.y + 1.6, at.z, 20, 0.3, 1.2, 0.3, 0.06);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0f, 0.5f);
    }
}
