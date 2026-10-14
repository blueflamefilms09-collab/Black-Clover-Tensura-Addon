package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: the Charybdis Spawn's move effects, drawn by the client's {@code TideBossLayer}: whirlpool spirals for the Whirlpool and the Maelstrom,
 * water spouts for the megalodons' walls, the surfacing and the lunge, tentacle sweeps for the Tentacle Sweep, and tide rings for the crush, the
 * volley and the Frenzy. Only drawn: nothing here moves a foe or touches a block; the gameplay stays in {@link CharybdisMoves}. Teal normally,
 * a colder white-blue in the Frenzy of the Gates.
 */
final class CharybdisVfx {
    private CharybdisVfx() {}

    static final int TEAL = 0xFF2FD6C8, FRENZY = 0xFF8DF6FF;

    static int tint(CharybdisSpawnBoss b) { return b != null && b.gatesOpen() ? FRENZY : TEAL; }

    // ---- Magicule Scale Volley
    static void volleyTell(CharybdisSpawnBoss b, Vec3 mouth, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_VORTEX, mouth, mouth.add(0, 1, 0), tint(b), windUp, 2.2);
    }

    static void volleyStrike(CharybdisSpawnBoss b, Vec3 mouth) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_RING, mouth, mouth.add(0, 1, 0), tint(b), 3.5);
    }

    // ---- Tentacle Sweep (and its rebound at the Gates)
    static void sweepTell(CharybdisSpawnBoss b, Vec3 centre, Vec3 dir, double reach, int windUp, boolean rebound) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_SWEEP, centre, centre.add(dir.scale(3)), tint(b), windUp, reach);
        if (rebound) BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_SWEEP, centre, centre.subtract(dir.scale(3)), FRENZY, windUp, reach);
    }

    static void sweepStrike(CharybdisSpawnBoss b, Vec3 centre, Vec3 dir, double reach) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SWEEP, centre, centre.add(dir.scale(3)), tint(b), reach);
    }

    // ---- Whirlpool / Maelstrom
    static void whirlTell(CharybdisSpawnBoss b, Vec3 centre, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_VORTEX, centre, centre.add(0, 1, 0), tint(b), windUp, radius);
    }

    /** The whirlpool itself, lasting as long as the zone does. */
    static void whirlStrike(CharybdisSpawnBoss b, Vec3 centre, double radius, int ticks) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_VORTEX, centre, centre.add(0, 1, 0), tint(b), ticks, radius);
    }

    // ---- Beak Lunge
    static void lungeTell(CharybdisSpawnBoss b, Vec3 end, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_RING, end, end.add(0, 1, 0), tint(b), windUp, 3.0);
    }

    static void lungeStrike(CharybdisSpawnBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position().add(0, b.getBbHeight() / 2, 0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, c, c.add(0, 1, 0), tint(b), 4.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_RING, c, c.add(0, 1, 0), tint(b), 5.0);
    }

    // ---- Summon Megalodons
    static void summonTell(CharybdisSpawnBoss b, Vec3 spot, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_SPOUT, spot, spot.add(0, 1, 0), tint(b), windUp, 3.5);
    }

    static void summonStrike(CharybdisSpawnBoss b, Vec3 spot) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, spot, spot.add(0, 1, 0), tint(b), 5.0);
    }

    // ---- Tidal Crush
    static void crushTell(CharybdisSpawnBoss b, Vec3 floor, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_RING, floor, floor.add(0, 1, 0), tint(b), windUp, radius);
    }

    static void crushStrike(CharybdisSpawnBoss b, Vec3 floor, double radius) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_RING, floor, floor.add(0, 1, 0), tint(b), radius + 1.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, floor, floor.add(0, 1, 0), tint(b), radius * 1.4);
    }

    // ---- surfacing, the Frenzy, the fall
    static void surfacing(CharybdisSpawnBoss b, int ticks) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_VORTEX, c.add(0, -1, 0), c.add(0, 0, 0), TEAL, ticks, 6.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, c.add(0, -2, 0), c.add(0, 1, 0), TEAL, 7.0);
    }

    static void frenzy(CharybdisSpawnBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_RING, c, c.add(0, 1, 0), FRENZY, 32, 14.0);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_VORTEX, c, c.add(0, 1, 0), FRENZY, 60, 8.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, c, c.add(0, 1, 0), FRENZY, 8.0);
    }

    static void burst(CharybdisSpawnBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, c, c.add(0, 1, 0), tint(b), 9.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_RING, c, c.add(0, 1, 0), tint(b), 12.0);
    }
}
