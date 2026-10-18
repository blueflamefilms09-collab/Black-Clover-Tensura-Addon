package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every technique of the Shield shows itself the same few ways (a floating pane, a shattering barrier, a cage of chains, a dome of iron, a seal, a brand of
 * blood), and the colours of the Shield (cold steel and the emerald of its jewel; red and black for the Wrath series) must be the same everywhere. Keeping the
 * calls here, as named helpers over the mod's effect system ({@link VfxSpawn}, {@link Tells}), means a technique reads as what it does and the look of the whole
 * weapon can be tuned in one file. Nothing here changes the world; it only asks clients to draw and play. Colours are 0xAARRGGBB for the effect system and
 * 0xRRGGBB for {@link Tells}.
 */
final class ShieldFx {
    private ShieldFx() {}

    static final int STEEL = 0xFFBFD8E8, JEWEL = 0xFF2EE6A0, SILVER = 0xFFE8F0FA, WRATH = 0xFFC2181B, EMBER = 0xFFFF6A1A, SOUL = 0xFF8A5CFF, HEAL = 0xFF7CFF9C;
    static final int TELL_STEEL = 0x9FD8E8, TELL_JEWEL = 0x2EE6A0, TELL_WRATH = 0xC2181B, TELL_EMBER = 0xFF6A1A;

    /** The upright floating pane of the Air Strike Shield: centred on {@code centre}, facing the way {@code facing} lies from it. */
    static void pane(ServerLevel l, Vec3 centre, Vec3 facing, int ticks) { VfxSpawn.send(l, VfxShape.MIRROR_PANE, centre, facing, JEWEL, ticks, 1.6f); }

    /** A barrier breaking or a blow turned aside (glass shards, a pulse ring). */
    static void shatter(ServerLevel l, Vec3 at, Vec3 lean, float size, int argb) { VfxSpawn.send(l, VfxShape.BARRIER_FX3, at, lean, argb, 22, size); }

    /** A dome of hexagon panes of radius {@code radius}. */
    static void dome(ServerLevel l, Vec3 at, double radius, int ticks, int argb) { VfxSpawn.send(l, VfxShape.BARRIER_FX2, at, at.add(0, 1, 0), argb, ticks, (float) radius); }

    /** The glass lance of the Shooting Star Shield. */
    static void lance(ServerLevel l, Vec3 from, Vec3 to, int flight) { VfxSpawn.send(l, VfxShape.BARRIER_FX1, from, to, JEWEL, flight, 1.3f); }

    /** A cage of chains round {@code at}: a dome-like field of the given radius. */
    static void cage(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.CHAIN_FX2, at, at.add(0, 1, 0), STEEL, ticks, (float) radius); }

    /** Chains thrown from {@code from} to {@code to}. */
    static void chains(ServerLevel l, Vec3 from, Vec3 to, int flight) { VfxSpawn.send(l, VfxShape.CHAIN_FX1, from, to, STEEL, flight, 1.0f); }

    /** The manacle slamming shut: the chains' burst at a point. */
    static void manacle(ServerLevel l, Vec3 at, Vec3 lean, float size) { VfxSpawn.send(l, VfxShape.CHAIN_FX3, at, lean, STEEL, 24, size); }

    /** The Immovable Guardian bell: a dome of iron over {@code at}. */
    static void guardian(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.IRON_FX2, at, at.add(0, 1, 0), STEEL, ticks, (float) radius); }

    /** The gauntlet slamming down: the Iron God's fist at {@code at}, leaning {@code lean}. */
    static void slam(ServerLevel l, Vec3 at, Vec3 lean, float size) { VfxSpawn.send(l, VfxShape.IRON_FX3, at, lean, STEEL, 28, size); }

    /** A seal field on the ground (the cover of Protect). */
    static void seal(ServerLevel l, Vec3 at, double radius, int ticks, int argb) { VfxSpawn.send(l, VfxShape.SEALING_FX2, at, at.add(0, 1, 0), argb, ticks, (float) radius); }

    /** A green rune circle: the herbs of the Leaf Shield. */
    static void herbs(ServerLevel l, Vec3 at, double radius) { VfxSpawn.send(l, VfxShape.ELF_CIRCLE, at, at.add(0, 1, 0), HEAL, 30, (float) radius / 2.5f); }

    /** Spectral shields orbiting {@code follow}: the Second Shield. */
    static void orbit(ServerLevel l, Entity follow, int ticks, int argb) { VfxSpawn.sendFollowing(l, VfxShape.WEAPON_CONSTRUCTS, follow, follow.position().add(0, 1, 0), argb, ticks, 1.0f); }

    /** Soul wisps spiralling in to {@code at}: the Soul Eater Shield. */
    static void souls(ServerLevel l, Vec3 at, double radius) { VfxSpawn.send(l, VfxShape.SOUL_VORTEX, at, at.add(0, 1, 0), SOUL, 40, (float) radius); }

    /** A slash of wind: the arc of a bash or a sweep. */
    static void arc(ServerLevel l, Vec3 from, Vec3 to, int argb, float size) { VfxSpawn.send(l, VfxShape.WIND_SLASH, from, to, argb, 8, size); }

    /** A blood sigil on the ground and its thorns: Blood Sacrifice. */
    static void bloodSigil(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.BLOOD_SIGIL, at, at.add(0, 1, 0), WRATH, ticks, (float) radius); }

    /** A blood cage over {@code at}. */
    static void bloodCage(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.BLOOD_FX2, at, at.add(0, 1, 0), WRATH, ticks, (float) radius); }

    /** A burst of blood: the trap closing. */
    static void bloodBurst(ServerLevel l, Vec3 at, Vec3 lean, float size) { VfxSpawn.send(l, VfxShape.BLOOD_FX3, at, lean, WRATH, 28, size); }

    /** The brand of the curse stamped in the air: the Wrath series' mark. */
    static void brand(ServerLevel l, Vec3 at, Vec3 lean, float size) { VfxSpawn.send(l, VfxShape.CURSE_FX3, at, lean, WRATH, 28, size); }

    /** A burst of flame at {@code at}. */
    static void flame(ServerLevel l, Vec3 at, float size) { VfxSpawn.send(l, VfxShape.FIRE_BURST, at, at.add(0, 1, 0), EMBER, 20, size); }

    /** Flame bursting out of {@code at} in every direction, in waves: Megiddo. */
    static void wildfire(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.FIRE_WILD, at, at.add(0, 1, 0), EMBER, ticks, (float) radius); }

    /** A column of flame (a fire pillar) at {@code at}. */
    static void pillar(ServerLevel l, Vec3 at, double radius) { VfxSpawn.send(l, VfxShape.FIRE_PILLAR, at, at.add(0, 1, 0), WRATH, 30, (float) radius); }

    /** A magic circle of the given radius under {@code at} (the wind-up of a big technique). */
    static void circle(ServerLevel l, Vec3 at, double radius, int ticks, int argb) { VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE, at, at.add(0, 1, 0), argb, ticks, (float) radius); }

    // ------------------------------------------------------------------ tells (the dust of the Zogratis telegraphs: visible to everyone near)
    static void tellRing(ServerLevel l, Vec3 at, double radius, int rgb) { Tells.ring(l, at, radius, rgb); }

    static void tellLine(ServerLevel l, Vec3 from, Vec3 to, int rgb) { Tells.line(l, from, to, rgb); }

    static void tellColumn(ServerLevel l, Vec3 at, double height, int rgb) { Tells.column(l, at, height, rgb); }

    static void tellGather(ServerLevel l, Vec3 at, double radius, int rgb, int count) { Tells.gather(l, at, radius, rgb, count); }

    /** A cone drawn in dust: two edge lines and an arc, so the player sees the width of the blow that is coming. */
    static void tellCone(ServerLevel l, Vec3 from, Vec3 dir, double range, double halfAngleDeg, int rgb) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1.0E-6) return;
        flat = flat.normalize();
        for (int side = -1; side <= 1; side += 2) {
            double a = Math.toRadians(halfAngleDeg) * side;
            Vec3 edge = new Vec3(flat.x * Math.cos(a) - flat.z * Math.sin(a), 0, flat.x * Math.sin(a) + flat.z * Math.cos(a));
            Tells.line(l, from, from.add(edge.scale(range)), rgb);
        }
    }

    // ------------------------------------------------------------------ sound
    static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) { l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch); }
}
