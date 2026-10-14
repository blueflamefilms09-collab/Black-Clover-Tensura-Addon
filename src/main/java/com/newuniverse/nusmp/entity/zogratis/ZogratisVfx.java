package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * 1.35.0: how the four Zogratis bosses send their own effects (the shapes of ZogratisGravityLayer, ZogratisBloodLayer, ZogratisSpaceLayer and
 * ZogratisDivineLayer). Every call is one small packet; a level sends at most {@link FxMath#MAX_PER_TICK} of them per tick, so a boss with many marks never floods
 * the clients (the extra ones are dropped, the dust telegraphs still show). Colours are 0xRRGGBB. Telegraph forms last the wind-up and
 * show the time left; strike forms are short. Server thread only.
 */
public final class ZogratisVfx {
    private ZogratisVfx() {}

    private static final Map<ServerLevel, FxMath.Throttle> THROTTLES = new java.util.WeakHashMap<>();       // levels are compared by identity; a stopped server's levels are let go

    /** The level's throttle; forgotten when the server stops (the boss classes call {@link #forget}). */
    private static FxMath.Throttle throttle(ServerLevel sl) { return THROTTLES.computeIfAbsent(sl, k -> new FxMath.Throttle()); }

    public static void forget() { THROTTLES.clear(); }

    private static boolean send(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, float power, boolean tell, boolean variant, int follow) {
        if (sl == null || !throttle(sl).allow(sl.getGameTime())) return false;
        long seed = FxMath.seed(sl.random.nextLong(), tell, variant);
        VfxSpawn.send(sl, new VfxPayload(shape.ordinal(), from, to, 0xFF000000 | rgb, Math.max(1, ticks), power, follow, seed));
        return true;
    }

    private static boolean send(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, float power, boolean tell) {
        return send(sl, shape, from, to, rgb, ticks, power, tell, false, -1);
    }

    private static Vec3 up(Vec3 p, double dy) { return p.add(0, dy, 0); }

    // ---------------------------------------------------------------- Dante
    /** A well of warped space on the ground: telegraph (rim closes over 'ticks') or the live field. */
    public static void well(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb, boolean tell) {
        send(sl, VfxShape.GRAVITY_WELL, c, up(c, 1), rgb, ticks, (float) radius, tell);
    }
    public static void gravityImpact(ServerLevel sl, Vec3 c, double radius, int rgb) { send(sl, VfxShape.GRAVITY_IMPACT, c, up(c, 1), rgb, 22, (float) radius, false); }
    /** A lane of chevrons on the ground from a to b. */
    public static void lane(ServerLevel sl, Vec3 a, Vec3 b, double width, int ticks, int rgb, boolean tell) {
        send(sl, VfxShape.GRAVITY_LANES, up(a, 0.05), up(b, 0.05), rgb, ticks, (float) width, tell);
    }
    public static void singularity(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb) { send(sl, VfxShape.GRAVITY_SINGULARITY, c, up(c, 1), rgb, ticks, (float) radius, false); }
    public static void gravityAura(ServerLevel sl, Entity body, double size, int ticks, int rgb) {
        send(sl, VfxShape.GRAVITY_AURA, body.position(), up(body.position(), 1), rgb, ticks, (float) size, false, false, body.getId());
    }

    // ---------------------------------------------------------------- Vanica
    /** A scythe crescent round 'c' opening toward 'toward'. */
    public static void crescent(ServerLevel sl, Vec3 c, Vec3 toward, double radius, int ticks, int rgb, boolean tell) {
        send(sl, VfxShape.BLOOD_CRESCENT, c, toward, rgb, ticks, (float) radius, tell);
    }
    public static void bloodLance(ServerLevel sl, Vec3 a, Vec3 b, double width, int ticks, int rgb, boolean tell) { send(sl, VfxShape.BLOOD_LANCE, a, b, rgb, ticks, (float) width, tell); }
    public static void bloodSigil(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb, boolean tell) { send(sl, VfxShape.BLOOD_SIGIL, c, up(c, 1), rgb, ticks, (float) radius, tell); }
    public static void bloodArcs(ServerLevel sl, Vec3 a, Vec3 b, double size, int ticks, int rgb) { send(sl, VfxShape.BLOOD_ARCS, a, b, rgb, ticks, (float) size, false); }
    public static void downpour(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb) { send(sl, VfxShape.BLOOD_DOWNPOUR, c, up(c, 1), rgb, ticks, (float) radius, false); }

    // ---------------------------------------------------------------- Zenon
    /** A cracked pane of space at c facing 'facing' (flat on the ground when 'ground'). */
    public static void crack(ServerLevel sl, Vec3 c, Vec3 facing, double size, int ticks, int rgb, boolean ground) { send(sl, VfxShape.SPACE_CRACK, c, facing, rgb, ticks, (float) size, false, ground, -1); }
    public static void tear(ServerLevel sl, Vec3 a, Vec3 b, double width, int ticks, int rgb) { send(sl, VfxShape.SPACE_TEAR, a, b, rgb, ticks, (float) width, false); }
    public static void gate(ServerLevel sl, Vec3 c, Vec3 facing, double radius, int ticks, int rgb) { send(sl, VfxShape.SPACE_RINGGATE, c, facing, rgb, ticks, (float) radius, false); }
    public static void spaceShatter(ServerLevel sl, Vec3 c, double radius, int rgb) { send(sl, VfxShape.SPACE_SHATTER, c, up(c, 1), rgb, 20, (float) radius, false); }

    // ---------------------------------------------------------------- Lucius (rgb from FxMath.divine(power))
    public static void pillar(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb, boolean tell) { send(sl, VfxShape.DIVINE_PILLAR, c, up(c, 1), rgb, ticks, (float) radius, tell); }
    public static void halo(ServerLevel sl, Entity body, double size, int ticks, int rgb) {
        Vec3 head = body.position().add(0, body.getBbHeight() * 0.95, 0);
        send(sl, VfxShape.DIVINE_HALO, head, head.add(0, 0, 1), rgb, ticks, (float) size, false, false, body.getId());
    }
    public static void soulVortex(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb) { send(sl, VfxShape.SOUL_VORTEX, c, up(c, 1), rgb, ticks, (float) radius, false); }
    public static void ray(ServerLevel sl, Vec3 a, Vec3 b, double width, int ticks, int rgb, boolean tell) { send(sl, VfxShape.DIVINE_RAY, a, b, rgb, ticks, (float) width, tell); }
    public static void divineSigil(ServerLevel sl, Vec3 c, double radius, int ticks, int rgb, boolean tell) { send(sl, VfxShape.DIVINE_SIGIL, c, up(c, 1), rgb, ticks, (float) radius, tell); }
}
