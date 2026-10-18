package com.newuniverse.nusmp.heroweapon.weapon.strike;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the owner purged vanilla particle spam from the abilities (1.36.0): an effect is a {@link VfxShape} sent through {@link VfxSpawn}, or one of the telegraph helpers of
 * {@link Tells}. The four striking weapons draw a warning first and the blow after, and the mod already has the two doors that know how: {@link BossMoveVfx} for the stone
 * effects (a dim telegraph that closes in, a bright strike) and {@link ZogratisVfx} for the gravity, blood and light effects, both of which also keep a per-tick budget so a crowd
 * of casts never floods the clients. This class gives the weapons one short name for each, plus a ring or an arc of dust on the ground and a sound. Colours are 0xRRGGBB
 * everywhere in this class. Nothing in this class changes a block.
 */
public final class StrikeFx {
    private StrikeFx() {}

    private static int argb(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

    // ------------------------------------------------------------------ the mod's effects, the plain way
    /** One effect of the mod's own, sent as it is (no telegraph voice). */
    public static void shape(ServerLevel l, VfxShape s, Vec3 from, Vec3 to, int rgb, int ticks, float power) { VfxSpawn.send(l, s, from, to, argb(rgb), ticks, power); }

    /** An effect that sticks to a body (an aura, a tornado round the wielder). */
    public static void follow(ServerLevel l, VfxShape s, Entity e, Vec3 to, int rgb, int ticks, float power) { VfxSpawn.sendFollowing(l, s, e, to, argb(rgb), ticks, power); }

    // ------------------------------------------------------------------ the stone effects: a telegraph, then a strike
    /** The dim ring that closes in on the ground while a blow winds up (lasts {@code windUp} ticks, at least 6). */
    public static void tellRing(ServerLevel l, Vec3 center, double radius, int rgb, int windUp) { BossMoveVfx.telegraph(l, VfxShape.BOSS_STONE_RING, center, center.add(0, 1, 0), rgb, windUp, radius); }

    /** The bright ring that runs out when the blow lands. */
    public static void strikeRing(ServerLevel l, Vec3 center, double radius, int rgb) { BossMoveVfx.strike(l, VfxShape.BOSS_STONE_RING, center, center.add(0, 1, 0), rgb, radius); }

    /** The dotted line a crack in the floor will follow, drawn while the blow winds up. */
    public static void tellCrack(ServerLevel l, Vec3 from, Vec3 to, int rgb, int windUp) { BossMoveVfx.telegraph(l, VfxShape.BOSS_STONE_FISSURE, from, to, rgb, windUp, 1.2); }

    /** The glowing crack that runs along the line when the blow lands ({@code ticks} long). */
    public static void strikeCrack(ServerLevel l, Vec3 from, Vec3 to, int rgb, int ticks) { BossMoveVfx.strikeFor(l, VfxShape.BOSS_STONE_FISSURE, from, to, rgb, ticks, 1.2); }

    /** A column of dust that shoots up out of the ground. */
    public static void dustColumn(ServerLevel l, Vec3 base, int rgb, double size) { BossMoveVfx.strike(l, VfxShape.BOSS_STONE_COLUMN, base, base.add(0, 1, 0), rgb, size); }

    /** The crushing impact of a heavy blow on the ground: warped-space rings, cracks, a ring of dust. */
    public static void impact(ServerLevel l, Vec3 center, double radius, int rgb) { ZogratisVfx.gravityImpact(l, center, radius, rgb); }

    // ------------------------------------------------------------------ dust on the ground (Tells)
    public static void ring(ServerLevel l, Vec3 center, double radius, int rgb) { Tells.ring(l, center, radius, rgb); }

    public static void line(ServerLevel l, Vec3 from, Vec3 to, int rgb) { Tells.line(l, from, to, rgb); }

    public static void column(ServerLevel l, Vec3 base, double height, int rgb) { Tells.column(l, base, height, rgb); }

    public static void gather(ServerLevel l, Vec3 at, double radius, int rgb, int count) { Tells.gather(l, at, radius, rgb, count); }

    public static void burst(ServerLevel l, Vec3 at, int rgb, int count) { Tells.burst(l, at, rgb, count); }

    /**
     * The outline of a fan on the ground: {@code radius} blocks out from {@code origin}, {@code halfAngleDeg} either side of the flat direction {@code dir}, drawn as a chain of
     * short dust lines (seven of them), so a player sees which side of the wielder a sweep will cover.
     */
    public static void arc(ServerLevel l, Vec3 origin, Vec3 dir, double radius, double halfAngleDeg, int rgb) {
        int n = 7;
        Vec3 last = null;
        for (int i = 0; i <= n; i++) {
            double a = -halfAngleDeg + 2 * halfAngleDeg * i / n;
            double[] v = WeaponGeo.turn(dir.x, dir.z, a);
            Vec3 p = new Vec3(origin.x + v[0] * radius, origin.y + 0.15, origin.z + v[1] * radius);
            if (last != null) Tells.line(l, last, p, rgb);
            last = p;
        }
    }

    // ------------------------------------------------------------------ sound
    public static void sound(ServerLevel l, Vec3 at, SoundEvent sound, float volume, float pitch) { l.playSound(null, BlockPos.containing(at), sound, SoundSource.PLAYERS, volume, pitch); }
}
