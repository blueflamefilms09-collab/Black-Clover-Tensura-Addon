package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: a spear is a pole, and a pole defends: it keeps foes at its length, sweeps the space round its wielder, and meets a rush with the point. The Windmill
 * Sweep is the sweep, the Counter Thrust is the parry (the owner's "answer" to being hit: cut the blow and give a thrust back), and the Minute Hand, the
 * sweep of the Dragon Minute Hand, turns round the wielder like the hand of a clock so that its danger is something a person can watch and move against.
 * The counter is bounded: it cuts one blow, answers once for the Spear's own damage (not the blow's), and never answers a foe the techniques may not strike.
 */
final class SpearGuard {
    private SpearGuard() {}

    // ================================================================ Windmill Sweep
    static boolean whirl(HeroCtx c) {
        Vec3 centre = c.player.position();
        c.fx(VfxShape.WIND_RING, centre.add(0.0, 0.2, 0.0), centre.add(0.0, 1.0, 0.0), SpearFx.light(c), 12, 2.0f);
        Tells.ring(c.level, centre, SpearNumbers.WHIRL_RADIUS, SpearFx.rgb(c));
        SpearFx.swing(c);
        SpearFx.sound(c, centre, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        Tasks.later(c, SpearNumbers.WHIRL_DELAY, () -> {
            Vec3 o = c.player.position();
            Hits hits = new Hits();
            for (LivingEntity t : Foes.around(c, o, SpearNumbers.WHIRL_RADIUS, Foes.MAX)) hits.add(t, (float) SpearNumbers.WHIRL_RAW);
            for (int i = 0; i < 4; i++) {
                double[] a = Geo.direction(i * 90.0 + 20.0), b = Geo.direction(i * 90.0 + 70.0);
                c.fx(VfxShape.WIND_SLASH, o.add(a[0] * 3.6, 1.0, a[1] * 3.6), o.add(b[0] * 3.6, 1.0, b[1] * 3.6), SpearFx.light(c), 8, 1.3f);
            }
            for (LivingEntity t : hits.apply(c, Foes.MAX)) {
                SpearFx.flare(c, SpearFx.mid(t), 0.5f);
                c.shove(t, o, SpearNumbers.WHIRL_PUSH);
            }
            SpearFx.sound(c, o, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.6f);
        });
        return true;
    }

    // ================================================================ Counter Thrust
    static boolean riposte(HeroCtx c) {
        SpearState.raiseStance(c, c.level.getGameTime() + SpearNumbers.RIPOSTE_TICKS);
        Vec3 chest = c.player.position().add(0.0, 1.2, 0.0);
        SpearFx.flare(c, chest, 0.8f);
        c.fx(VfxShape.WIND_RING, c.player.position().add(0.0, 0.2, 0.0), chest, SpearFx.light(c), SpearNumbers.RIPOSTE_TICKS, 0.8f);
        c.say("Counter stance.");
        SpearFx.sound(c, chest, SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
        return true;
    }

    /**
     * The wielder is about to take {@code amount}: with the counter stance up, a blow from a creature is cut to a third and answered with a thrust when
     * the attacker is within the spear's reach; a projectile is only halved. Falls and fire and the void are not blows and pass untouched. The stance
     * is spent either way. With the Featherlight passive a fall does nothing at all.
     */
    static float onDamaged(HeroCtx c, DamageSource source, float amount) {
        if (c.summoned() && c.form.has(SpearIds.FEATHERLIGHT) && source.is(DamageTypeTags.IS_FALL)) return 0f;
        long now = c.level.getGameTime();
        if (!SpearState.stanceUp(c, now)) return amount;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity foe) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL)) return amount;
        SpearState.dropStance(c);
        Vec3 chest = c.player.position().add(0.0, 1.2, 0.0);
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            SpearFx.flare(c, chest, 0.9f);
            SpearFx.sound(c, chest, SoundEvents.TRIDENT_HIT, 1.0f, 1.6f);
            return amount * (float) SpearNumbers.RIPOSTE_PROJECTILE_KEEP;
        }
        if (Foes.valid(c, foe) && foe.distanceTo(c.player) <= SpearNumbers.RIPOSTE_REACH) {
            Vec3 at = SpearFx.mid(foe);
            c.fx(VfxShape.STAR_MAGIC, SpearFx.hand(c, at.subtract(c.eye()).normalize()), at, SpearFx.main(c), 4, 1.0f);
            if (Foes.hit(c, foe, (float) SpearNumbers.RIPOSTE_RAW)) {
                SpearFx.flare(c, at, 0.9f);
                c.shove(foe, c.player.position(), 0.6);
            }
        }
        SpearFx.sound(c, chest, SoundEvents.ANVIL_LAND, 0.9f, 1.4f);
        return amount * (float) SpearNumbers.RIPOSTE_KEEP;
    }

    // ================================================================ Minute Hand
    static boolean minuteHand(HeroCtx c) {
        Vec3 look = c.look();
        double start = Geo.bearing(look.x, look.z);
        Vec3 centre = c.player.position();
        VfxSpawn.sendFollowing(c.level, VfxShape.TIME_ACCEL, c.player, centre.add(0.0, 1.0, 0.0), SpearFx.light(c), 40, 1.3f);
        Tells.ring(c.level, centre, SpearNumbers.MINUTE_RADIUS, SpearFx.rgb(c));
        double[] d0 = Geo.direction(start);
        c.fx(VfxShape.THREAD_LINE, centre.add(0.0, 0.9, 0.0), centre.add(d0[0] * SpearNumbers.MINUTE_RADIUS, 0.9, d0[1] * SpearNumbers.MINUTE_RADIUS),
                SpearFx.light(c), SpearNumbers.MINUTE_DELAY + 2, 0.7f);
        SpearFx.swing(c);
        SpearFx.sound(c, centre, SoundEvents.BELL_BLOCK, 1.2f, 0.6f);
        Set<UUID> struck = new HashSet<>();
        double per = 360.0 / SpearNumbers.MINUTE_STEPS;
        Tasks.pulses(c, SpearNumbers.MINUTE_DELAY, SpearNumbers.MINUTE_STEPS, SpearNumbers.MINUTE_STEP_TICKS, s -> {
            double prev = start + s * per, cur = start + (s + 1) * per;
            Vec3 o = c.player.position();
            double[] dir = Geo.direction(cur);
            c.fx(VfxShape.THREAD_LINE, o.add(0.0, 0.9, 0.0), o.add(dir[0] * SpearNumbers.MINUTE_RADIUS, 0.9, dir[1] * SpearNumbers.MINUTE_RADIUS),
                    SpearFx.light(c), 5, 0.9f);
            for (LivingEntity e : Foes.around(c, o, SpearNumbers.MINUTE_RADIUS + 1.0, Foes.MAX)) {
                if (struck.contains(e.getUUID())) continue;
                Vec3 m = e.getBoundingBox().getCenter();
                double dx = m.x - o.x, dz = m.z - o.z;
                if (Math.sqrt(dx * dx + dz * dz) > SpearNumbers.MINUTE_RADIUS + e.getBbWidth() * 0.5) continue;
                if (!Geo.swept(prev, cur, Geo.bearing(dx, dz), SpearNumbers.MINUTE_HALF_WIDTH)) continue;
                if (!Foes.sees(c, o.add(0.0, 1.2, 0.0), e)) continue;
                struck.add(e.getUUID());
                if (Foes.hit(c, e, (float) SpearNumbers.MINUTE_RAW)) {
                    SpearFx.flare(c, m, 0.7f);
                    c.shove(e, o, 0.4);
                }
            }
            if (s % 5 == 0) SpearFx.sound(c, o, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.8f + 0.1f * s / 3.0f);
        });
        return true;
    }
}
