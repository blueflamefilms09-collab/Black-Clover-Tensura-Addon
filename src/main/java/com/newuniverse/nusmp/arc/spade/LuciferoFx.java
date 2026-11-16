package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): Lucifero, the gravity devil of the Spade Kingdom, used to end his biggest moves in clouds of vanilla dust. Every one of
 * those now goes through here and, while the server setting {@code bossRework1Vfx} is on, is a shaped gravity effect of the mod's own layer (ZogratisGravityLayer and the
 * divine pillar, through {@link ZogratisVfx}'s per-level budget, players near only): a pillar of falling heavens on each mark of the rain, a crushing well when the room
 * kneels, the event horizon's sphere when it collapses, a warped-space impact when his shroud or shell breaks, a gravity lane per limb in the Six Limbs telegraph (sent once,
 * for the whole wind-up). Off, the old dust comes back exactly as it was. One effect per call; the dust telegraphs ({@link Tells#ring} and kin) are untouched.
 */
final class LuciferoFx {
    private LuciferoFx() {}

    private static final int VIOLET = LuciferoMoves.VIOLET_RGB, ACCENT = LuciferoMoves.ACCENT_RGB, WARN = LuciferoMoves.WARN_RGB;

    /** A piece of the Falling Heavens lands on {@code m}: a falling-heavens pillar. */
    static void heaven(ServerLevel sl, Vec3 m) {
        if (ReworkFx.on()) ZogratisVfx.pillar(sl, m, LuciferoMoves.HEAVEN_RADIUS, 16, VIOLET, false);
        else Tells.burst(sl, m.add(0, 0.8, 0), ACCENT, 22);
    }

    /** Kneel Before the King lands round {@code c}: the gravity crush ring closing on the room. */
    static void kneel(ServerLevel sl, Vec3 c) {
        if (ReworkFx.on()) ZogratisVfx.well(sl, c, 26, 30, VIOLET, false);
        else Tells.burst(sl, c.add(0, 1.2, 0), ACCENT, 90);
    }

    /** The Event Horizon collapses on {@code c}: its black sphere swells and falls in. */
    static void horizon(ServerLevel sl, Vec3 c) {
        if (ReworkFx.on()) ZogratisVfx.singularity(sl, c.add(0, 3.0, 0), WeightMath.CORE_RADIUS * 1.2, 24, VIOLET);
        else Tells.burst(sl, c.add(0, 1.2, 0), ACCENT, 80);
    }

    /** Something of his breaks at {@code at} (the shroud torn, the shell shattered, a swallowed devil): a small warped-space impact. */
    static void burst(ServerLevel sl, Vec3 at, int rgb, int count) {
        if (ReworkFx.on()) ZogratisVfx.gravityImpact(sl, at, Math.max(1.2, Math.min(4.0, count / 15.0)), rgb);
        else Tells.burst(sl, at, rgb, count);
    }

    /** One limb of the Six Limbs telegraph from {@code c} at {@code angle}: a gravity lane for the whole wind-up (sent once), a dust line on the floor every call. */
    static void armTell(ServerLevel sl, Vec3 c, double angle, double len, boolean first, int windUp) {
        if (!ReworkFx.on()) {
            SpadeFx.spoke(sl, c, angle, len, WARN);
            return;
        }
        Vec3 end = c.add(Math.cos(angle) * len, 0, Math.sin(angle) * len);
        if (first) ZogratisVfx.lane(sl, c.add(0, 0.1, 0), end.add(0, 0.1, 0), 2.2, windUp, WARN, true);
        Tells.line(sl, c.add(0, 0.15, 0), end.add(0, 0.15, 0), WARN);
    }
}
