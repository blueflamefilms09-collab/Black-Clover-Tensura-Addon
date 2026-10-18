package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the look of the Spear's techniques (the red gem, the white-gold light, the pink of the Lust forms) is decided in one place, so the colours stay the
 * Spear's and a form that is cursed turns every effect magenta without each technique asking. Effects go only through the mod's own effect system
 * ({@code VfxSpawn} shapes), never vanilla particles; this class only chooses which shape and which colour, the techniques say where and when.
 */
final class SpearFx {
    private SpearFx() {}

    /** White-gold light, the gem's red, silver steel, the Lust magenta (0xAARRGGBB), and the same two sin and gem colours as 0xRRGGBB for the ground rings. */
    static final int GOLD = 0xFFFFE8A0, RED = 0xFFFF5C4D, SILVER = 0xFFE4E8F0, LUST = 0xFFD94A8C;
    static final int RGB_RED = 0xFF5C4D, RGB_LUST = 0xD94A8C;

    static boolean lust(HeroCtx c) { return c.form.curse(); }

    /** The strong colour of the weapon now: the gem's red, or the Lust magenta in a curse form. */
    static int main(HeroCtx c) { return lust(c) ? LUST : RED; }

    /** The light colour: white-gold, or the Lust magenta. */
    static int light(HeroCtx c) { return lust(c) ? LUST : GOLD; }

    /** The ground ring colour (0xRRGGBB). */
    static int rgb(HeroCtx c) { return lust(c) ? RGB_LUST : RGB_RED; }

    static Vec3 mid(LivingEntity e) { return e.getBoundingBox().getCenter(); }

    /** Where the spear's tip is, roughly: in front of the eye, a little low, along {@code dir}. */
    static Vec3 hand(HeroCtx c, Vec3 dir) { return c.eye().add(dir.scale(0.7)).add(0.0, -0.3, 0.0); }

    /** The wielder's arm swings (a thrust is seen to be thrown), for everyone watching. */
    static void swing(HeroCtx c) {
        try {
            c.player.swing(InteractionHand.MAIN_HAND, true);
        } catch (RuntimeException ignored) {
            // a stand-in player has no connection to tell
        }
    }

    static void flare(HeroCtx c, Vec3 at, float size) { c.fx(VfxShape.LIGHT_FLARE, at, at, light(c), 10, size); }

    /** A sound at a place in the wielder's world, for everyone near. */
    static void sound(HeroCtx c, Vec3 where, SoundEvent s, float volume, float pitch) {
        c.level.playSound(null, BlockPos.containing(where.x, where.y, where.z), s, SoundSource.PLAYERS, volume, pitch);
    }
}
