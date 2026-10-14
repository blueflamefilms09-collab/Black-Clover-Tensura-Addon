package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: the Goliath's move effects, drawn by the client's {@code StoneBossLayer}: stone shockwave rings for every wave, columns of dust for the
 * fist and the waking, boulder trails for the rubble and the fist's shadow, fissure lines for the Seismic Line and the cracks of the seal. Nothing
 * here places, moves or breaks a block; the gameplay stays in {@link GoliathMoves}. The colour is the amber of the stone's cracks, red once the
 * Gates are open.
 */
final class GoliathVfx {
    private GoliathVfx() {}

    static final int AMBER = 0xFFFFAA33, HOT = 0xFFFF5A2E;

    static int tint(GoliathBoss b) { return b != null && b.gatesOpen() ? HOT : AMBER; }

    // ---- ring waves (Ground Slam, Twin Stomp, Roaring Shockwave, Bedrock Quake)
    static void ringTell(GoliathBoss b, double reach, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, b.position(), b.position().add(0, 1, 0), tint(b), windUp, reach);
    }

    /** The wave itself, lasting as long as the wave takes to cross its reach. */
    static void ringStrike(GoliathBoss b, Vec3 centre, double reach, double speed) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_RING, centre, centre.add(0, 1, 0), tint(b), GoliathMath.waveTicks(reach, speed), reach);
    }

    // ---- Bedrock Fist
    static void fistTell(GoliathBoss b, Vec3 mark, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_TRAIL, mark.add(0, 9, 0), mark, tint(b), windUp, Math.max(1.0, radius * 0.6));
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, mark, mark.add(0, 1, 0), tint(b), windUp, radius);
    }

    static void fistStrike(GoliathBoss b, Vec3 mark, double radius) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_TRAIL, mark.add(0, 9, 0), mark, tint(b), 8, Math.max(1.0, radius * 0.6));
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, mark, mark.add(0, 1, 0), tint(b), Math.max(1.0, radius * 0.5));
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, mark, mark.add(0, 1, 0), tint(b), radius + 1.0);
    }

    /** The aftershock ring of the fist at the Gates. */
    static void aftershockTell(GoliathBoss b, Vec3 mark, double reach, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, mark, mark.add(0, 1, 0), HOT, windUp, reach);
    }

    // ---- Seismic Line
    static void lineTell(GoliathBoss b, Vec3 from, Vec3 to, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_FISSURE, from, to, tint(b), windUp, 1.2);
    }

    static void lineStrike(GoliathBoss b, Vec3 from, Vec3 to) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_FISSURE, from, to, tint(b), 40, 1.2);
    }

    // ---- Rubble Fall
    static void rubbleTell(GoliathBoss b, Vec3 spot, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_TRAIL, spot.add(0, 9, 0), spot, tint(b), windUp, Math.max(0.8, radius * 0.5));
    }

    static void rubbleStrike(GoliathBoss b, Vec3 spot, double radius) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_TRAIL, spot.add(0, 9, 0), spot, tint(b), GoliathTable.RUBBLE_LAND + 6, Math.max(0.8, radius * 0.5));
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, spot, spot.add(0, 1, 0), tint(b), radius + 0.8);
    }

    // ---- the waking, the Gates, the fall
    static void waking(GoliathBoss b, Vec3 seal) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.4;
            BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_FISSURE, seal.add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), seal.add(Math.cos(a) * 9.0, 0, Math.sin(a) * 9.0), AMBER, 60, 1.0);
        }
    }

    static void emerged(GoliathBoss b, Vec3 seal) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, seal, seal.add(0, 1, 0), AMBER, 2.2);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, seal, seal.add(0, 1, 0), AMBER, 9.0);
    }

    static void gates(GoliathBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_RING, c, c.add(0, 1, 0), HOT, 30, 12.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, c, c.add(0, 1, 0), HOT, 2.4);
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI * 2 / 3 + 0.5;
            BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_FISSURE, c.add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), c.add(Math.cos(a) * 11.0, 0, Math.sin(a) * 11.0), HOT, 50, 1.2);
        }
    }

    static void fall(GoliathBoss b, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, at, at.add(0, 1, 0), tint(b), 3.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, at, at.add(0, 1, 0), tint(b), 14.0);
    }
}
