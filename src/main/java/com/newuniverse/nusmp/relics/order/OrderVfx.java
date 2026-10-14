package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: the God of Order's effects, drawn by the client's {@code OrderBossLayer}: white-gold law sigils on the floor, the ledger's ruled grid, the
 * correction beam (the staff's line, the Writ, the pillar of the Judgement) and the halo of ledger glyphs. They cover both the fight's moves and the
 * visit (arrival, census while it reads, the correction beam that burns the duplicate, the farewell). Only drawn: nothing here hurts, pushes or
 * touches a block; the gameplay stays in {@link OrderMoves} and {@link OrderDirector}.
 */
final class OrderVfx {
    private OrderVfx() {}

    static final int GOLD = 0xFFE8B923, WHITE_GOLD = 0xFFFFF0B8;

    static int tint(GodOfOrder g) { return g != null && g.gatesOpen() ? WHITE_GOLD : GOLD; }

    /** When the census halo and the correction beam were last sent (game time), so a beat that comes twice draws one. */
    private static long lastCensus = Long.MIN_VALUE / 2, lastCorrection = Long.MIN_VALUE / 2;

    private static Vec3 sky(Vec3 at) { return at.add(0, 16, 0); }

    // ---- Decree of Binding
    static void decreeTell(GodOfOrder g, Vec3 at, double r, int windUp) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, at, at.add(0, 1, 0), tint(g), windUp, r);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_GRID, at, at.add(1, 0, 0), tint(g), windUp, r * 0.9);
    }

    static void decreeStrike(GodOfOrder g, Vec3 at, double r) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, at, at.add(0, 1, 0), tint(g), r);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_GRID, at, at.add(1, 0, 0), tint(g), r * 0.9);
    }

    // ---- Writ of Light
    static void writTell(GodOfOrder g, Vec3 from, Vec3 dir, double length, int windUp) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 a = from.add(0, 1.0, 0), b = from.add(dir.scale(length)).add(0, 1.0, 0);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_BEAM, a, b, tint(g), windUp, OrderTable.WRIT_HALF_WIDTH * 1.4);
    }

    static void writStrike(GodOfOrder g, Vec3 from, Vec3 dir, double length) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 a = from.add(0, 1.0, 0), b = from.add(dir.scale(length)).add(0, 1.0, 0);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, a, b, tint(g), 14, OrderTable.WRIT_HALF_WIDTH * 1.4);
    }

    // ---- Circle of Judgement
    static void judgementTell(GodOfOrder g, Vec3 at, double r, int windUp) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, at, at.add(0, 1, 0), tint(g), windUp, r);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_BEAM, sky(at), at, tint(g), windUp, r * 0.5);
    }

    static void judgementStrike(GodOfOrder g, Vec3 at, double r) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, sky(at), at, tint(g), 14, r * 0.8);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, at, at.add(0, 1, 0), tint(g), r);
    }

    // ---- Erasure
    static void erasureTell(GodOfOrder g, int ticks) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 c = g.position();
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_LEDGER, c, c.add(0, 3.4, 0), tint(g), ticks, 7.0);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_GRID, c, c.add(1, 0, 0), tint(g), ticks, 14.0);
    }

    static void erasureStrike(GodOfOrder g) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 c = g.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_LEDGER, c, c.add(0, 3.4, 0), tint(g), 40, 7.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_GRID, c, c.add(1, 0, 0), tint(g), 14.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, c, c.add(0, 1, 0), tint(g), 9.0);
    }

    // ---- the Open Ledger (the Gates)
    static void gates(GodOfOrder g) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 c = g.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_LEDGER, c, c.add(0, 3.4, 0), WHITE_GOLD, 60, 7.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, c, c.add(0, 1, 0), WHITE_GOLD, 8.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_GRID, c, c.add(1, 0, 0), WHITE_GOLD, 16.0);
    }

    // ---- the visit (called by OrderFx)
    static void arrival(ServerLevel sl, Vec3 spot) {
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, spot, spot.add(0, 1, 0), GOLD, OrderPlan.TELEGRAPH_TICKS, 4.0);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_GRID, spot, spot.add(1, 0, 0), GOLD, OrderPlan.TELEGRAPH_TICKS, 8.0);
    }

    static void stepOut(ServerLevel sl, Vec3 spot) {
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, spot, spot.add(0, 1, 0), WHITE_GOLD, 4.0);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, sky(spot), spot, WHITE_GOLD, 16, 1.4);
    }

    /** The census: the halo of ledger script turns round the God while it reads. */
    static void census(ServerLevel sl, Vec3 godAt) {
        if (sl.getGameTime() - lastCensus < 40) return;                      // the caller's first beats can come twice: one halo is enough
        lastCensus = sl.getGameTime();
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_LEDGER, godAt, godAt.add(0, 2.9, 0), GOLD, OrderPlan.READ_TICKS, 4.5);
    }

    /** The correction beam: the staff burns the duplicate. */
    static void correction(ServerLevel sl, Vec3 staff, Vec3 target) {
        if (sl.getGameTime() - lastCorrection < 30) return;
        lastCorrection = sl.getGameTime();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, staff, target, WHITE_GOLD, OrderPlan.BEAM_TICKS, 1.0);
    }

    static void burned(ServerLevel sl, Vec3 at) {
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, at.add(0, -1.0, 0), at, GOLD, 2.5);
    }

    static void farewell(ServerLevel sl, Vec3 at) {
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_LEDGER, at, at.add(0, 2.9, 0), WHITE_GOLD, 30, 4.0);
    }
}
