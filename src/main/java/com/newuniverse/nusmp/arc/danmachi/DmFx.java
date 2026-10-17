package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the four monsters share a handful of shapes on the floor (a ring, a lane, a spot, a cone) and each draws them in its own material: stone and ember
 * for the Minotaur and the Black Goliath, bone for Udaeus, water and flame for the Amphisbaena. This is the one place that knows which of the mod's own
 * effect shapes ({@code BOSS_STONE_*}, {@code BONE_FX*}, {@code BOSS_TIDE_*} ...) draws which, so a move says "a ring tell here" and never "this packet".
 * Everything goes out through {@link BossMoveVfx}, {@link VfxSpawn} and {@link Tells} (never a vanilla particle), changes no block, and is sent only to the
 * players near. A telegraph is drawn once for the whole wind-up; a strike is short and bright.
 */
final class DmFx {
    private DmFx() {}

    /** What an effect is made of. */
    enum Look { STONE, BONE, TIDE }

    /** A boss's palette: the material, the colour (0xRRGGBB) and the hotter colour it takes at the Gates. */
    record Style(Look look, int rgb, int hot) {
        int tint(ZogratisBoss b) { return b != null && b.gatesOpen() ? hot : rgb; }
    }

    private static ServerLevel server(ZogratisBoss b) { return b.level() instanceof ServerLevel sl ? sl : null; }

    private static int argb(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

    // ================================================================ a ring on the floor (a stomp, a wave, a howl, a quake)
    static void ringTell(ZogratisBoss b, Style st, Vec3 centre, double reach, int windUp) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        switch (st.look()) {
            case BONE -> VfxSpawn.send(sl, VfxShape.BONE_FX2, centre, centre.add(0, 1, 0), argb(rgb), Math.max(12, windUp + 6), (float) reach);
            case TIDE -> BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_RING, centre, centre.add(0, 1, 0), rgb, windUp, reach);
            default -> BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, centre, centre.add(0, 1, 0), rgb, windUp, reach);
        }
    }

    /** The wave itself, lasting as long as it takes to cross {@code reach}. */
    static void ringStrike(ZogratisBoss b, Style st, Vec3 centre, double reach, int ticks) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        switch (st.look()) {
            case BONE -> VfxSpawn.send(sl, VfxShape.BONE_FX3, centre, centre.add(0, 1, 0), argb(rgb), Math.max(14, ticks), (float) Math.max(1.0, reach / 5.0));
            case TIDE -> BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_RING, centre, centre.add(0, 1, 0), rgb, ticks, reach);
            default -> BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_RING, centre, centre.add(0, 1, 0), rgb, ticks, reach);
        }
    }

    // ================================================================ a spot (a fist, a boulder, a pillar, a spout)
    static void spotTell(ZogratisBoss b, Style st, Vec3 spot, double radius, int windUp) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        switch (st.look()) {
            case BONE -> VfxSpawn.send(sl, VfxShape.BONE_FX2, spot, spot.add(0, 1, 0), argb(rgb), Math.max(12, windUp + 6), (float) radius);
            case TIDE -> BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_SPOUT, spot, spot.add(0, 1, 0), rgb, windUp, Math.max(2.0, radius * 1.4));
            default -> {
                BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_TRAIL, spot.add(0, 9, 0), spot, rgb, windUp, Math.max(0.8, radius * 0.5));
                BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, spot, spot.add(0, 1, 0), rgb, windUp, radius);
            }
        }
        Tells.ring(sl, spot, radius, rgb, 1.1f, (int) Math.max(12, Math.min(40, radius * 8)));
    }

    static void spotStrike(ZogratisBoss b, Style st, Vec3 spot, double radius) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        switch (st.look()) {
            case BONE -> VfxSpawn.send(sl, VfxShape.BONE_FX3, spot, spot.add(0, 1, 0), argb(rgb), 20, (float) Math.max(0.6, radius * 0.5));
            case TIDE -> BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_SPOUT, spot, spot.add(0, 1, 0), rgb, Math.max(2.0, radius * 1.4));
            default -> {
                BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_TRAIL, spot.add(0, 9, 0), spot, rgb, 8, Math.max(0.8, radius * 0.5));
                BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, spot, spot.add(0, 1, 0), rgb, Math.max(1.0, radius * 0.5));
                BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, spot, spot.add(0, 1, 0), rgb, radius + 1.0);
            }
        }
    }

    // ================================================================ a lane (a charge, a lash, a spoke, a wave of the sword)
    static void laneTell(ZogratisBoss b, Style st, Vec3 from, Vec3 to, double width, int windUp) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        if (st.look() == Look.STONE) BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_FISSURE, from, to, rgb, windUp, Math.max(1.0, width * 0.6));
        edges(sl, from, to, width, rgb);                          // the dust edges read the width from the floor, whatever the material
    }

    static void laneStrike(ZogratisBoss b, Style st, Vec3 from, Vec3 to, double width, int ticks) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        switch (st.look()) {
            case BONE -> VfxSpawn.send(sl, VfxShape.BONE_FX1, from, to, argb(rgb), Math.max(8, ticks), (float) Math.max(0.8, width * 0.4));
            case TIDE -> VfxSpawn.send(sl, VfxShape.WATER_JAVELIN, from, to, argb(rgb), Math.max(8, ticks), (float) Math.max(0.8, width * 0.5));
            default -> BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_FISSURE, from, to, rgb, Math.max(12, ticks), Math.max(1.0, width * 0.6));
        }
    }

    /** A breath of flame from the mouth to a point (a spiral of fire in the style's colour as its glow: the mod has no pure blue fire, so the flames keep their own colours). */
    static void breathStrike(ZogratisBoss b, Style st, Vec3 from, Vec3 to, int ticks) {
        ServerLevel sl = server(b);
        if (sl != null) VfxSpawn.send(sl, VfxShape.FIRE_SPIRAL, from, to, argb(st.tint(b)), Math.max(8, ticks), 1.0f);
    }

    /** The two sides of a lane in dust (a lane's width must be readable from the floor, whatever the material). */
    static void edges(ServerLevel sl, Vec3 from, Vec3 to, double width, int rgb) {
        Vec3 d = to.subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-6) return;
        Vec3 n = new Vec3(-d.z, 0, d.x).normalize().scale(width / 2.0);
        Tells.line(sl, from.add(n), to.add(n), rgb);
        Tells.line(sl, from.subtract(n), to.subtract(n), rgb);
    }

    // ================================================================ a cone (a gore, a sweep, a breath)
    /** The edges and the rim of a cone that opens {@code halfDeg} each side of the unit vector (fx, fz), in dust (and one stroke of the material along its middle). */
    static void coneTell(ZogratisBoss b, Style st, Vec3 from, double fx, double fz, double halfDeg, double reach, int windUp) {
        ServerLevel sl = server(b);
        if (sl == null) return;
        int rgb = st.tint(b);
        double half = Math.toRadians(Math.min(180.0, halfDeg));
        int steps = Math.max(3, (int) Math.min(10, Math.ceil(Math.toDegrees(half) / 12.0)));
        Vec3 prev = null;
        for (int i = -steps; i <= steps; i++) {
            double[] r = DmGeometry.rotate(fx, fz, half * i / steps);
            Vec3 p = from.add(r[0] * reach, 0, r[1] * reach);
            if (prev != null) Tells.line(sl, prev, p, rgb);
            if (i == -steps || i == steps || i == 0) Tells.line(sl, from, p, rgb);
            prev = p;
        }
        if (windUp > 0 && st.look() == Look.STONE) BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_FISSURE, from, from.add(fx * reach, 0, fz * reach), rgb, windUp, 1.0);
    }

    static void coneStrike(ZogratisBoss b, Style st, Vec3 from, double fx, double fz, double reach) {
        laneStrike(b, st, from, from.add(fx * reach, 0, fz * reach), 2.0, 14);
    }

    // ================================================================ small things
    /** A burst of the style's colour at a point (an impact, a crash, a break). */
    static void burst(ZogratisBoss b, Style st, Vec3 at, int count) {
        ServerLevel sl = server(b);
        if (sl != null) Tells.burst(sl, at, st.tint(b), count);
    }

    /** Motes gathering round a point (a roar, a charge-up). */
    static void gather(ZogratisBoss b, Style st, Vec3 at, double radius, int count) {
        ServerLevel sl = server(b);
        if (sl != null) Tells.gather(sl, at, radius, st.tint(b), count);
    }

    /** A line of dust (a breath's axis, a volley's path). */
    static void line(ZogratisBoss b, Style st, Vec3 from, Vec3 to) {
        ServerLevel sl = server(b);
        if (sl != null) Tells.line(sl, from, to, st.tint(b));
    }
}
