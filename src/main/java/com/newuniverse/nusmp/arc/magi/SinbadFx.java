package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Why (4.105.0): the one place where Sinbad sends the Magi layer's five shapes (MAGI_EQUIP, MAGI_AURA, MAGI_RUKH, MAGI_EXTREME, MAGI_PHASE, drawn by {@code vfx.client.layer.MagoiLayer}),
 * each behind {@code sinbad_boss.magiVfx}: with it off every call answers {@code false} and the caller sends the old effect it always sent. One packet per call, to the players near, through
 * the effect budget of {@link VfxSpawn}. Worst case per Sinbad: an aura packet every 25 ticks, an equip packet per switch (at most one per 3 s outside the finale's seven), one Extreme and one
 * phase packet per cast, one Rukh flock per ranged move.
 */
final class SinbadFx {
    private SinbadFx() {}

    /** The duration of an aura packet and how often it is re-sent (a little longer than the gap, so it never blinks). */
    static final int AURA_TICKS = 30, AURA_EVERY = 25;

    /** The Djinn Equip forging: from his feet, following him. */
    static boolean equip(ServerLevel sl, SinbadBoss b, SinbadDjinn d) {
        if (!SinbadBossConfig.magiVfx() || d == SinbadDjinn.NONE) return false;
        VfxSpawn.sendFollowing(sl, VfxShape.MAGI_EQUIP, b, b.position(), d.argb, 40, 1.0F);
        return true;
    }

    /** The steady Magoi aura while a djinn is worn. */
    static boolean aura(ServerLevel sl, SinbadBoss b, SinbadDjinn d) {
        if (!SinbadBossConfig.magiVfx() || d == SinbadDjinn.NONE) return false;
        VfxSpawn.sendFollowing(sl, VfxShape.MAGI_AURA, b, b.position(), d.argb, AURA_TICKS, 1.0F);
        return true;
    }

    /** A flock of Rukh from a point to a point (a spell's path). */
    static boolean rukh(ServerLevel sl, Vec3 from, Vec3 to, SinbadDjinn d, float size) {
        if (!SinbadBossConfig.magiVfx()) return false;
        VfxSpawn.send(sl, VfxShape.MAGI_RUKH, from, to, d.argb, 24, size);
        return true;
    }

    /** The Extreme Magic: a sigil in the sky and on the ground, shafts, a pillar and a shockwave of the given radius. */
    static boolean extreme(ServerLevel sl, Vec3 centre, SinbadDjinn d, float radius) {
        if (!SinbadBossConfig.magiVfx()) return false;
        VfxSpawn.send(sl, VfxShape.MAGI_EXTREME, centre, centre, d.argb, 60, radius);
        return true;
    }

    /** The break of a phase: rays, a ring and a burst of feathers from his body. */
    static boolean phase(ServerLevel sl, SinbadBoss b, SinbadDjinn d, float size) {
        if (!SinbadBossConfig.magiVfx()) return false;
        VfxSpawn.sendFollowing(sl, VfxShape.MAGI_PHASE, b, b.position().add(0.0, b.getBbHeight() * 0.5, 0.0), d.argb, 36, size);
        return true;
    }
}
