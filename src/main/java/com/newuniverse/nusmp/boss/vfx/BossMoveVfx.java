package com.newuniverse.nusmp.boss.vfx;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: the one door through which the boss moves (Devil Avatar, Goliath, Charybdis Spawn, God of Order) send their {@code VfxShape.BOSS_*}
 * effects. It marks an effect as telegraph or strike (see {@link BossVfxRules#seed}), keeps the radius sane and spends from the per-tick budget
 * ({@link BossVfxRules.Throttle}), so the callers can just say what they want. Server side only; never touches a block.
 */
public final class BossMoveVfx {
    private BossMoveVfx() {}

    private static final BossVfxRules.Throttle THROTTLE = new BossVfxRules.Throttle();

    /** The wind-up of a move: dim, steady and closing, lasting as long as the wind-up. */
    public static boolean telegraph(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int windUpTicks, double size) {
        return send(sl, shape, from, to, rgb, BossVfxRules.telegraphTicks(windUpTicks), size, false);
    }

    /** The move landing: bright and fast. */
    public static boolean strike(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, double size) {
        return send(sl, shape, from, to, rgb, BossVfxRules.strikeTicks(size), size, true);
    }

    /** A strike that lasts 'ticks' (a field or a whirlpool that stays). */
    public static boolean strikeFor(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, double size) {
        return send(sl, shape, from, to, rgb, Math.max(4, Math.min(240, ticks)), size, true);
    }

    private static boolean send(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, double size, boolean strike) {
        if (sl == null || shape == null || !THROTTLE.allow(sl.getGameTime())) return false;
        int argb = (rgb & 0xFF000000) == 0 ? 0xFF000000 | rgb : rgb;
        VfxSpawn.send(sl, new VfxPayload(shape.ordinal(), from, to == null ? from.add(0, 1, 0) : to, argb, ticks, BossVfxRules.clampRadius(size), -1,
                BossVfxRules.seed(sl.random.nextLong(), strike)));
        return true;
    }
}
