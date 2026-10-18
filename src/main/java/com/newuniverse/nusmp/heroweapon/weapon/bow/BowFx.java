package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the look of the Bow's techniques (the yellow gem, the white-gold light, the violet of the Pride forms) is decided in one place, so the colours stay
 * the Bow's and a cursed form turns every effect violet without each technique asking. Effects go only through the mod's own effect system
 * ({@code VfxSpawn} shapes) and the ground rings of {@code Tells}, never vanilla particles; this class only chooses the colour, the techniques say
 * which shape, where and when.
 */
final class BowFx {
    private BowFx() {}

    /** Yellow-gold of the gem, white-gold light, the Pride violet, and the red of the sniper's laser sight (0xAARRGGBB); the two ground-ring colours as 0xRRGGBB. */
    static final int GOLD = 0xFFFFD84A, WHITE = 0xFFFFE8A0, PRIDE = 0xFF7A3FB5, SIGHT = 0xFFFF5A4A;
    static final int RGB_GOLD = 0xFFD84A, RGB_PRIDE = 0x7A3FB5;

    static boolean pride(HeroCtx c) { return c.form.curse(); }

    /** The strong colour of the weapon now: the gem's yellow, or the Pride violet in a curse form. */
    static int main(HeroCtx c) { return pride(c) ? PRIDE : GOLD; }

    /** The light colour: white-gold, or the Pride violet. */
    static int light(HeroCtx c) { return pride(c) ? PRIDE : WHITE; }

    /** The ground ring colour (0xRRGGBB). */
    static int rgb(HeroCtx c) { return pride(c) ? RGB_PRIDE : RGB_GOLD; }

    static Vec3 mid(LivingEntity e) { return e.getBoundingBox().getCenter(); }

    /** Where the arrow leaves the bow, roughly: in front of the eye, a little low, along {@code dir}. */
    static Vec3 muzzle(HeroCtx c, Vec3 dir) { return c.eye().add(dir.scale(0.8)).add(0.0, -0.25, 0.0); }

    /** The wielder's arm moves (a shot is seen to be loosed), for everyone watching. */
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
