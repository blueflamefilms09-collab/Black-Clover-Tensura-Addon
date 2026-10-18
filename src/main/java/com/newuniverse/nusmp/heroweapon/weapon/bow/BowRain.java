package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: "ranged artillery" is the first thing the owner asked of the Legendary Bow, and artillery has one honest shape: a place is marked, a moment passes,
 * and then it is struck. Everyone in the marked circle sees the ring on the ground and has time to leave it; that is the answer to every technique here. The
 * Arrow Rain, the Frozen Rain (which slows), the Starfall Volley (the ultimate) and Autocracy Justice (the Pride ultimate, a lion of light) are the same
 * rain with different numbers; the Meteor Bow is one heavy blow instead of waves. The numbers are {@link BowNumbers}'s, the circle is always drawn first,
 * waves are counted exactly (see {@link Tasks#pulses}), and nothing here breaks or places a block.
 */
final class BowRain {
    private BowRain() {}

    /** One rain: reach, circle, waves, ticks between waves, damage of one wave, wait before the first, arrows drawn per wave, and its flavour. */
    private record Spec(double range, double radius, int waves, int interval, double rawWave, int delay, int arrows, boolean frost, boolean grand, boolean lion) {}

    static boolean rain(HeroCtx c) {
        return cast(c, new Spec(BowNumbers.RAIN_RANGE, BowNumbers.RAIN_RADIUS, BowNumbers.RAIN_WAVES, BowNumbers.RAIN_INTERVAL, BowNumbers.RAIN_RAW_WAVE,
                BowNumbers.RAIN_DELAY, 6, false, false, false));
    }

    static boolean frozen(HeroCtx c) {
        return cast(c, new Spec(BowNumbers.FROZEN_RANGE, BowNumbers.FROZEN_RADIUS, BowNumbers.FROZEN_WAVES, BowNumbers.FROZEN_INTERVAL, BowNumbers.FROZEN_RAW_WAVE,
                BowNumbers.FROZEN_DELAY, 8, true, false, false));
    }

    static boolean starfall(HeroCtx c) {
        return cast(c, new Spec(BowNumbers.STARFALL_RANGE, BowNumbers.STARFALL_RADIUS, BowNumbers.STARFALL_WAVES, BowNumbers.STARFALL_INTERVAL, BowNumbers.STARFALL_RAW_WAVE,
                BowNumbers.STARFALL_DELAY, 10, false, true, false));
    }

    static boolean autocracy(HeroCtx c) {
        boolean ok = cast(c, new Spec(BowNumbers.AUTOCRACY_RANGE, BowNumbers.AUTOCRACY_RADIUS, BowNumbers.AUTOCRACY_WAVES, BowNumbers.AUTOCRACY_INTERVAL,
                BowNumbers.AUTOCRACY_RAW_WAVE, BowNumbers.AUTOCRACY_DELAY, 10, false, true, true));
        if (ok) {
            try {
                HeroCurse.addCorruption(c.player, BowNumbers.AUTOCRACY_CORRUPTION);
            } catch (RuntimeException ignored) {
                // the curse engine must never stop a technique
            }
        }
        return ok;
    }

    private static boolean cast(HeroCtx c, Spec s) {
        ServerLevel level = c.level;
        Vec3 aim = c.aim(s.range());
        int y = Moves.surfaceBelow(level, aim.x, aim.y + 0.5, aim.z, 10);
        Vec3 centre = new Vec3(aim.x, y == Integer.MIN_VALUE ? aim.y : y, aim.z);
        if (centre.distanceTo(c.player.position()) < 2.5) {
            c.say("Aim farther away.");
            return false;
        }
        int lasts = s.delay() + (s.waves() - 1) * s.interval() + 10;
        Tells.ring(level, centre, s.radius(), BowFx.rgb(c));
        for (int t = 8; t < lasts - 10; t += 8) Tasks.later(c, t, () -> Tells.ring(level, centre, s.radius(), BowFx.rgb(c)));       // a ring of dust fades in a second or two: draw it again until the last wave
        if (s.frost()) c.fx(VfxShape.ICE_FX2, centre, centre, BowFx.light(c), lasts, (float) s.radius());
        else if (s.grand()) c.fx(VfxShape.MAGIC_CIRCLE, centre.add(0.0, 0.1, 0.0), centre.add(0.0, 1.0, 0.0), BowFx.main(c), lasts, (float) (s.radius() * 0.7));
        Vec3 dir = c.look(), muzzle = BowFx.muzzle(c, dir);
        c.fx(VfxShape.STAR_MAGIC, muzzle, centre.add(0.0, 18.0, 0.0), BowFx.main(c), 6, 1.0f);
        if (s.lion()) c.fx(VfxShape.FIRE_LION, muzzle, centre.add(0.0, 1.0, 0.0), BowFx.PRIDE, s.delay() + 10, 2.4f);
        BowFx.swing(c);
        BowFx.sound(c, muzzle, SoundEvents.ARROW_SHOOT, 1.2f, 0.7f);
        Tasks.pulses(c, s.delay(), s.waves(), s.interval(), k -> {
            Hits hits = new Hits();
            for (LivingEntity e : Foes.around(c, centre, s.radius(), Foes.MAX)) hits.add(e, (float) s.rawWave());
            for (double[] pt : Volley.scatter(s.arrows(), s.radius(), k)) {
                c.fx(VfxShape.STAR_MAGIC, centre.add(pt[0], 16.0, pt[1]), centre.add(pt[0], 0.2, pt[1]), BowFx.main(c), 4, 0.5f);
            }
            for (LivingEntity e : hits.apply(c, Foes.MAX)) {
                if (s.frost()) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BowNumbers.FROZEN_SLOW_TICKS, 1));
                BowFx.flare(c, BowFx.mid(e), 0.4f);
            }
            if (k % 2 == 0) BowFx.sound(c, centre, SoundEvents.ARROW_HIT, 0.8f, 1.3f);
        });
        return true;
    }

    // ================================================================ Meteor Bow
    static boolean meteor(HeroCtx c) {
        ServerLevel level = c.level;
        Vec3 aim = c.aim(BowNumbers.METEOR_RANGE);
        int y = Moves.surfaceBelow(level, aim.x, aim.y + 0.5, aim.z, 10);
        Vec3 centre = new Vec3(aim.x, y == Integer.MIN_VALUE ? aim.y : y, aim.z);
        if (centre.distanceTo(c.player.position()) < 3.0) {
            c.say("Aim farther away.");
            return false;
        }
        Vec3 dir = c.look(), muzzle = BowFx.muzzle(c, dir);
        Tells.ring(level, centre, BowNumbers.METEOR_RADIUS, BowFx.rgb(c));
        c.fx(VfxShape.STAR_MAGIC, muzzle, centre.add(0.0, 40.0, 0.0), BowFx.main(c), 8, 1.2f);
        BowFx.swing(c);
        BowFx.sound(c, muzzle, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.8f);
        Tasks.later(c, BowNumbers.METEOR_DELAY / 2, () -> Tells.ring(level, centre, BowNumbers.METEOR_RADIUS, BowFx.rgb(c)));
        Tasks.later(c, BowNumbers.METEOR_DELAY - 6, () -> {
            Tells.ring(level, centre, BowNumbers.METEOR_RADIUS, BowFx.rgb(c));
            c.fx(VfxShape.STAR_MAGIC, centre.add(3.0, 40.0, 2.0), centre, BowFx.main(c), 6, 2.2f);
        });
        Tasks.later(c, BowNumbers.METEOR_DELAY, () -> {
            Hits hits = new Hits();
            for (LivingEntity e : Foes.around(c, centre, BowNumbers.METEOR_RADIUS, Foes.MAX)) hits.add(e, (float) BowNumbers.METEOR_RAW);
            c.fx(VfxShape.FIRE_BURST, centre, centre, BowFx.main(c), 18, 2.2f);
            for (LivingEntity e : hits.apply(c, Foes.MAX)) {
                BowFx.flare(c, BowFx.mid(e), 0.7f);
                c.shove(e, centre, 0.9);
            }
            BowFx.flare(c, centre.add(0.0, 0.5, 0.0), 2.0f);
            BowFx.sound(c, centre, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6f, 0.6f);
        });
        return true;
    }
}
