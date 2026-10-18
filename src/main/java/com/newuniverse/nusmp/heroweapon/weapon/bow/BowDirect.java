package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Bow's "tactical versatility" is more than arrows of different kinds: it has to be able to shoot a crowd (the Arrow Squall fan), to lock onto one
 * foe that will not stand still (Bird Hunting, the five shafts that strike together), to get out of reach (the Backstep Shot springs back and shoots on the
 * way), and, for the Pride series, to armour its wielder (Law Fanatic) and to roast one foe in a brass bull. Each of them shows what it will do before it
 * does it (the fan is drawn, the shafts fly, the bull's furnace appears) and is answered by cover, by leaving the line, or by being out of reach.
 */
final class BowDirect {
    private BowDirect() {}

    // ================================================================ Arrow Squall
    static boolean squall(HeroCtx c) {
        Vec3 eye = c.eye(), look = c.look(), muzzle = BowFx.muzzle(c, look);
        double[] offsets = Geo.fan(BowNumbers.SQUALL_ARROWS, BowNumbers.SQUALL_SPREAD);
        List<Vec3> ends = new ArrayList<>();
        for (double off : offsets) {
            double[] r = Geo.rotate(look.x, look.z, off);
            Vec3 d = new Vec3(r[0], look.y, r[1]);
            Vec3 end = Rays.clipEnd(c, eye, eye.add(d.scale(BowNumbers.SQUALL_RANGE)));
            ends.add(end);
            c.fx(VfxShape.THREAD_LINE, muzzle, end, BowFx.light(c), BowNumbers.SQUALL_DELAY + 2, 0.5f);
        }
        BowFx.swing(c);
        BowFx.sound(c, eye, SoundEvents.CROSSBOW_SHOOT, 0.8f, 1.5f);
        Tasks.later(c, BowNumbers.SQUALL_DELAY, () -> {
            Hits hits = new Hits();
            for (Vec3 end : ends) {
                List<LivingEntity> in = Foes.along(c, eye, end, BowNumbers.SQUALL_WIDTH, 1);
                c.fx(VfxShape.STAR_MAGIC, muzzle, end, BowFx.main(c), 3, 0.6f);
                if (!in.isEmpty()) hits.add(in.get(0), (float) BowNumbers.SQUALL_PER_ARROW);
            }
            for (LivingEntity t : hits.apply(c, Foes.MAX)) {
                BowFx.flare(c, BowFx.mid(t), 0.5f);
                c.shove(t, eye, 0.3);
            }
            BowFx.sound(c, eye, SoundEvents.ARROW_SHOOT, 1.0f, 1.8f);
        });
        return true;
    }

    // ================================================================ Bird Hunting
    static boolean bird(HeroCtx c) {
        Vec3 eye = c.eye(), look = c.look();
        LivingEntity t = Foes.mostAligned(c, eye, look, BowNumbers.BIRD_RANGE, BowNumbers.BIRD_CONE);
        if (t == null) {
            c.say("Nothing to hunt.");
            return false;
        }
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 to = BowFx.mid(t);
        for (int i = 0; i < BowNumbers.BIRD_SHAFTS; i++) {
            double side = (i - (BowNumbers.BIRD_SHAFTS - 1) / 2.0) * 0.5;
            Vec3 from = eye.add(right.scale(side)).add(0.0, 0.3 - 0.15 * Math.abs(side), 0.0).add(look.scale(0.6));
            Tasks.later(c, i, () -> c.fx(VfxShape.STAR_MAGIC, from, to, BowFx.main(c), BowNumbers.BIRD_DELAY, 0.6f));
        }
        BowFx.swing(c);
        BowFx.sound(c, eye, SoundEvents.ARROW_SHOOT, 1.0f, 1.6f);
        Tasks.later(c, BowNumbers.BIRD_DELAY + BowNumbers.BIRD_SHAFTS - 1, () -> {
            if (!t.isAlive() || !Foes.sees(c, c.eye(), t)) return;
            if (Foes.hit(c, t, (float) BowNumbers.BIRD_RAW)) {
                BowFx.flare(c, BowFx.mid(t), 0.9f);
                BowFx.sound(c, t.position(), SoundEvents.ARROW_HIT_PLAYER, 1.0f, 1.3f);
            }
        });
        return true;
    }

    // ================================================================ Backstep Shot
    static boolean retreat(HeroCtx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        if (p.isPassenger()) {                                              // a mount would drag the wielder straight back
            c.say("Get down first.");
            return false;
        }
        Vec3 eye = c.eye(), look = c.look(), start = p.position();
        Vec3 back = Moves.flat(look, new Vec3(0.0, 0.0, 1.0)).scale(-1.0);
        LivingEntity prey = Foes.nearestInCone(c, eye, look, BowNumbers.RETREAT_SEEK, 70.0);
        double free = BowNumbers.RETREAT_DISTANCE;
        for (double h : new double[]{0.4, 1.6}) {                          // a wall at the knee or at the head stops the spring
            Vec3 a = start.add(0.0, h, 0.0), b = a.add(back.scale(BowNumbers.RETREAT_DISTANCE + 0.5));
            free = Math.min(free, Rays.clipEnd(c, a, b).distanceTo(a) - 0.6);
        }
        Vec3 dest = null;
        for (double d = free; d >= 2.0 && dest == null; d -= 0.5) {
            double x = start.x + back.x * d, z = start.z + back.z * d;
            int y = Moves.surfaceBelow(level, x, start.y + 1.0, z, 4);
            if (y != Integer.MIN_VALUE && Moves.standable(level, p, (int) Math.floor(x), y, (int) Math.floor(z))) dest = Moves.feet(level, x, y, z);
        }
        if (dest == null) {
            c.say("No room to spring back.");
            return false;
        }
        c.fx(VfxShape.STAR_MAGIC, start.add(0.0, 1.0, 0.0), dest.add(0.0, 1.0, 0.0), BowFx.light(c), 6, 0.9f);
        Moves.teleport(p, dest);
        BowFx.swing(c);
        BowFx.sound(c, dest, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
        if (prey != null && prey.isAlive()) {
            Vec3 muzzle = BowFx.muzzle(c, Rays.dir(c.eye(), BowFx.mid(prey), look));
            c.fx(VfxShape.STAR_MAGIC, muzzle, BowFx.mid(prey), BowFx.main(c), 4, 0.6f);
            if (Foes.hit(c, prey, (float) BowNumbers.RETREAT_RAW)) BowFx.flare(c, BowFx.mid(prey), 0.5f);
        }
        return true;
    }

    // ================================================================ Law Fanatic
    static boolean law(HeroCtx c) {
        ServerPlayer p = c.player;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BowNumbers.LAW_TICKS, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BowNumbers.LAW_TICKS, 0, false, false, true));
        BowState.raiseLaw(c, c.level.getGameTime() + BowNumbers.LAW_TICKS);
        VfxSpawn.sendFollowing(c.level, VfxShape.SHADOW_UNITE, p, p.position().add(0.0, 1.0, 0.0), BowFx.PRIDE, 60, 1.2f);
        c.say("Law Fanatic.");
        BowFx.sound(c, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.5f);
        return true;
    }

    // ================================================================ Bull of Phalaris
    static boolean bull(HeroCtx c) {
        LivingEntity t = Foes.aimed(c, BowNumbers.BULL_RANGE, 1.0);
        if (t == null) {
            c.say("Nothing to put in the bull.");
            return false;
        }
        if (!c.control()) {
            c.say("The bull needs a moment to be forged again.");
            return false;
        }
        Vec3 at = t.position();
        c.fx(VfxShape.BRONZE_FX2, at, at, BowFx.PRIDE, BowNumbers.BULL_DELAY + BowNumbers.BULL_ROOT + 10, 2.2f);
        Tells.ring(c.level, at, 2.2, BowFx.rgb(c));
        BowFx.swing(c);
        BowFx.sound(c, at, SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
        Tasks.later(c, BowNumbers.BULL_DELAY, () -> {
            if (!t.isAlive()) return;
            Foes.hit(c, t, (float) BowNumbers.BULL_RAW);
            if (!t.isAlive()) return;
            int ticks = BalanceLaw.controlTicks(t, BowNumbers.BULL_ROOT);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
            c.fx(VfxShape.FIRE_PILLAR, t.position(), t.position(), BowFx.main(c), 24, 1.6f);
            BowFx.flare(c, BowFx.mid(t), 1.0f);
            BowFx.sound(c, t.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
        });
        return true;
    }
}
