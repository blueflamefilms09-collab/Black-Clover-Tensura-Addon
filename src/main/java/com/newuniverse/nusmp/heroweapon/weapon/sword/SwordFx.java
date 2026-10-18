package com.newuniverse.nusmp.heroweapon.weapon.sword;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every technique of the Sword shows itself the same few ways (a streak of light, a crescent of wind, blades falling, a flash, a ring of swords, a maw), and the
 * colours of the Sword (steel and the sapphire of its jewel; gold and brown for the Greed and Gluttony series) must be the same everywhere. Keeping the calls here,
 * as named helpers over the mod's effect system ({@link VfxSpawn}, {@link Tells}), means a technique reads as what it does and the look of the whole weapon can be
 * tuned in one file. Nothing here changes the world; it only asks clients to draw and play. Colours are 0xAARRGGBB for the effect system and 0xRRGGBB for
 * {@link Tells}.
 */
final class SwordFx {
    private SwordFx() {}

    static final int STEEL = 0xFFDCE6F2, SAPPHIRE = 0xFF2E7BE6, SKY = 0xFF6FA8E8, GOLD = 0xFFD9A520, BROWN = 0xFF8B4A1F, FLAME = 0xFFFF5A1A, THUNDER = 0xFFBFD8FF, WHITE = 0xFFFFFFFF;
    static final int TELL_SKY = 0x6FA8E8, TELL_GOLD = 0xD9A520, TELL_BROWN = 0x8B4A1F, TELL_FLAME = 0xFF5A1A, TELL_THUNDER = 0xBFD8FF;

    /** A streak of light crossing from {@code from} to {@code to} in a few ticks: the dash of a blade. */
    static void streak(ServerLevel l, Vec3 from, Vec3 to, int argb, float size) { VfxSpawn.send(l, VfxShape.LIGHT_BLADE, from, to, argb, 8, size); }

    /** A crescent of wind flying from {@code from} to {@code to}. */
    static void crescent(ServerLevel l, Vec3 from, Vec3 to, int argb, float size) { VfxSpawn.send(l, VfxShape.WIND_SLASH, from, to, argb, 10, size); }

    /** A lens flare at {@code at}: the Flashing Sword. */
    static void flare(ServerLevel l, Vec3 at, float size) { VfxSpawn.send(l, VfxShape.LIGHT_FLARE, at, at.add(0, 1, 0), WHITE, 14, size); }

    /** Spectral swords orbiting {@code follow} for {@code ticks}. */
    static void orbit(ServerLevel l, Entity follow, int ticks, int argb) { VfxSpawn.sendFollowing(l, VfxShape.WEAPON_CONSTRUCTS, follow, follow.position().add(0, 1, 0), argb, ticks, 1.0f); }

    /** A sword of light falling on {@code at} from above. */
    static void falling(ServerLevel l, Vec3 at, int argb) { VfxSpawn.send(l, VfxShape.LIGHT_BLADE, at.add(0, 9, 0), at, argb, 6, 0.7f); }

    /** A jagged bolt falling on {@code at}. */
    static void bolt(ServerLevel l, Vec3 at, int argb) { VfxSpawn.send(l, VfxShape.LIGHTNING_SPEAR, at.add(0, 12, 0), at, argb, 6, 0.9f); }

    /** Starlight falling on {@code at}. */
    static void star(ServerLevel l, Vec3 at) { VfxSpawn.send(l, VfxShape.STAR_MAGIC, at.add(0, 10, 0), at, 0xFFFFE8A0, 12, 1.0f); }

    /** A magic circle on the ground, the wind-up of a big technique. */
    static void circle(ServerLevel l, Vec3 at, double radius, int ticks, int argb) { VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE, at, at.add(0, 1, 0), argb, ticks, (float) radius); }

    /** Gust rings running out from {@code at}. */
    static void gust(ServerLevel l, Vec3 at, double radius) { VfxSpawn.send(l, VfxShape.WIND_RING, at, at.add(0, 1, 0), STEEL, 14, (float) radius / 3.0f); }

    /** The flame storm of the Phoenix Gale Blade, drilling from {@code from} to {@code to}. */
    static void storm(ServerLevel l, Vec3 from, Vec3 to) { VfxSpawn.send(l, VfxShape.FIRE_SPIRAL, from, to, FLAME, 16, 1.6f); }

    /** The fiery bird: a spear of flame flying from {@code from} to {@code to}. */
    static void bird(ServerLevel l, Vec3 from, Vec3 to, int flight) { VfxSpawn.send(l, VfxShape.FIRE_SPEAR, from, to, FLAME, flight, 1.8f); }

    /** A burst of flame at {@code at}. */
    static void flame(ServerLevel l, Vec3 at, float size) { VfxSpawn.send(l, VfxShape.FIRE_BURST, at, at.add(0, 1, 0), FLAME, 20, size); }

    /** A well of bent space on the ground: the Gravity Sword. */
    static void well(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.GRAVITY_WELL, at, at.add(0, 1, 0), SKY, ticks, (float) radius); }

    /** A crushing blow on the ground at {@code at}. */
    static void crush(ServerLevel l, Vec3 at, double radius) { VfxSpawn.send(l, VfxShape.GRAVITY_IMPACT, at, at.add(0, 1, 0), STEEL, 22, (float) radius); }

    /** A double stepping out of the glass: the afterimage a dash leaves behind. */
    static void afterimage(ServerLevel l, Vec3 at, float height) { VfxSpawn.send(l, VfxShape.MIRROR_STEP, at, at.add(0, 1, 0), SKY, 24, height); }

    /** A crack that runs along the ground from {@code from} to {@code to} with slabs pushed up: Strong Decline. */
    static void fissure(ServerLevel l, Vec3 from, Vec3 to) { VfxSpawn.send(l, VfxShape.EARTH_FISSURE, from, to, BROWN, 24, 1.4f); }

    /** A giant maw opening at {@code at} and snapping shut in the direction {@code facing}: Maneater. */
    static void maw(ServerLevel l, Vec3 at, Vec3 facing, float size) { VfxSpawn.send(l, VfxShape.FOOD_FX3, at, facing, GOLD, 26, size); }

    /** The banquet field: a ring of forks and plates round {@code at} with a maw looming over it. */
    static void banquet(ServerLevel l, Vec3 at, double radius, int ticks) { VfxSpawn.send(l, VfxShape.FOOD_FX2, at, at.add(0, 1, 0), GOLD, ticks, (float) radius); }

    /** A dark crescent thrown from {@code from} towards {@code to}: the curse forms' slash. */
    static void darkSlash(ServerLevel l, Vec3 from, Vec3 to, float size) { VfxSpawn.send(l, VfxShape.DARK_SLASH_AVIDYA, from, to, GOLD, 12, size); }

    /** The curse brand burning in the air at {@code at}. */
    static void brand(ServerLevel l, Vec3 at, Vec3 lean, float size, int argb) { VfxSpawn.send(l, VfxShape.CURSE_FX3, at, lean, argb, 28, size); }

    // ------------------------------------------------------------------ tells (the dust of the Zogratis telegraphs: visible to everyone near)
    static void tellRing(ServerLevel l, Vec3 at, double radius, int rgb) { Tells.ring(l, at, radius, rgb); }

    static void tellLine(ServerLevel l, Vec3 from, Vec3 to, int rgb) { Tells.line(l, from, to, rgb); }

    static void tellColumn(ServerLevel l, Vec3 at, double height, int rgb) { Tells.column(l, at, height, rgb); }

    static void tellGather(ServerLevel l, Vec3 at, double radius, int rgb, int count) { Tells.gather(l, at, radius, rgb, count); }

    /** A cone drawn in dust: two edge lines, so the player sees the width of the blow that is coming. */
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
