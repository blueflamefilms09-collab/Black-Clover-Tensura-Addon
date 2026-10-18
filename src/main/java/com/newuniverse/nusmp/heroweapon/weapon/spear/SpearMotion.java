package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SafeSpot;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: "high mobility" is half of what the owner asked of the Legendary Spear, so it moves in five different ways, each with a different use: the Dragoon
 * Lunge (a straight dash through a line of foes), Pole Vault (up and over), Portal Spear (a blink to where you aim), Filo Rush (a sprint that tramples) and
 * Hooked Pierce (haul a foe in, or haul yourself to a boss). Every move that puts the wielder somewhere new goes through {@link Moves} so it only ever
 * lands on a safe place; the damage is the Spear's, through the balance law; nothing here breaks or places a block.
 */
final class SpearMotion {
    private SpearMotion() {}

    // ================================================================ Dragoon Lunge
    static boolean lunge(HeroCtx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        if (p.isPassenger()) {                                              // a mount would drag the wielder straight back
            c.say("Get down first.");
            return false;
        }
        Vec3 dir = SpearAim.dir(c);
        Vec3 flat = Moves.flat(dir, Moves.flat(c.look(), new Vec3(0.0, 0.0, 1.0)));
        Vec3 start = p.position();
        double free = SpearNumbers.LUNGE_DISTANCE;
        for (double h : new double[]{0.4, 1.6}) {                          // a wall at the knee or at the head stops the dash
            Vec3 a = start.add(0.0, h, 0.0), b = a.add(flat.scale(SpearNumbers.LUNGE_DISTANCE + 0.5));
            free = Math.min(free, Rays.clipEnd(c, a, b).distanceTo(a) - 0.6);
        }
        Vec3 dest = null;
        for (double d = free; d >= 2.0; d -= 0.5) {
            double x = start.x + flat.x * d, z = start.z + flat.z * d;
            int y = Moves.surfaceBelow(level, x, start.y + 1.0, z, 4);
            if (y == Integer.MIN_VALUE) continue;
            if (Moves.standable(level, p, (int) Math.floor(x), y, (int) Math.floor(z))) {
                dest = Moves.feet(level, x, y, z);
                break;
            }
        }
        if (dest == null) {
            c.say("No room to lunge.");
            return false;
        }
        Vec3 a = start.add(0.0, 1.0, 0.0), b = dest.add(0.0, 1.0, 0.0);
        Hits hits = new Hits();
        for (LivingEntity t : Foes.along(c, a, b, SpearNumbers.LUNGE_WIDTH, SpearNumbers.LUNGE_TARGETS)) hits.add(t, (float) SpearNumbers.LUNGE_RAW);
        c.fx(VfxShape.LIGHT_BLADE, a, b, SpearFx.light(c), 8, 0.9f);
        c.fx(VfxShape.STAR_MAGIC, a, b, SpearFx.main(c), 6, 0.9f);
        SpearFx.swing(c);
        for (LivingEntity t : hits.apply(c, SpearNumbers.LUNGE_TARGETS)) SpearFx.flare(c, SpearFx.mid(t), 0.5f);
        Moves.teleport(p, dest);
        SpearFx.sound(c, dest, SoundEvents.TRIDENT_RETURN, 1.0f, 1.6f);
        return true;
    }

    // ================================================================ Pole Vault
    static boolean vault(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 flat = Moves.flat(c.look(), new Vec3(0.0, 0.0, 1.0));
        Vec3 feet = p.position();
        Moves.launch(p, flat.x * SpearNumbers.VAULT_FORWARD, SpearNumbers.VAULT_UP, flat.z * SpearNumbers.VAULT_FORWARD);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SpearNumbers.VAULT_SLOW_FALL, 0, false, false, true));
        c.fx(VfxShape.WIND_RING, feet.add(0.0, 0.2, 0.0), feet.add(0.0, 1.0, 0.0), SpearFx.light(c), 12, 1.2f);
        SpearFx.swing(c);
        SpearFx.sound(c, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);
        return true;
    }

    // ================================================================ Portal Spear
    static boolean portal(HeroCtx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        if (p.isPassenger()) {
            c.say("Get down first.");
            return false;
        }
        Vec3 eye = c.eye(), aim = c.aim(SpearNumbers.PORTAL_RANGE), toAim = aim.subtract(eye);
        Vec3 target = toAim.length() > 1.0 ? aim.subtract(toAim.normalize().scale(0.5)) : aim;       // step back off a wall so we aim at open air
        int floor = Moves.surfaceBelow(level, target.x, target.y + 0.5, target.z, 12);
        int fy = floor == Integer.MIN_VALUE ? (int) Math.floor(target.y) : floor;
        SafeSpot.Spot spot = SafeSpot.nearest(Moves.probe(level, p), (int) Math.floor(target.x), fy, (int) Math.floor(target.z), SpearNumbers.PORTAL_SEARCH);
        if (spot == null) {
            c.say("The portal finds no footing there.");
            return false;
        }
        Vec3 here = p.position().add(0.0, 1.0, 0.0), dest = Moves.feet(level, spot.x() + 0.5, spot.y(), spot.z() + 0.5);
        Vec3 face = c.look();
        c.fx(VfxShape.SPACE_RINGGATE, here, here.add(face), SpearFx.light(c), 18, 1.2f);
        c.fx(VfxShape.SPACE_RINGGATE, dest.add(0.0, 1.0, 0.0), dest.add(0.0, 1.0, 0.0).add(face), SpearFx.light(c), 18, 1.2f);
        SpearFx.swing(c);
        SpearFx.sound(c, here, SoundEvents.PORTAL_TRIGGER, 0.5f, 1.8f);
        Tasks.later(c, SpearNumbers.PORTAL_DELAY, () -> {
            if (p.isPassenger() || !Moves.standable(level, p, spot.x(), spot.y(), spot.z())) return;      // mounted, or the place changed in the meantime
            Vec3 left = p.position().add(0.0, 1.0, 0.0);
            if (!Moves.teleport(p, dest)) return;
            c.fx(VfxShape.THREAD_LINE, left, dest.add(0.0, 1.0, 0.0), SpearFx.light(c), 6, 0.8f);
            Hits hits = new Hits();
            for (LivingEntity t : Foes.around(c, dest, SpearNumbers.PORTAL_EXIT_RADIUS, Foes.MAX)) hits.add(t, (float) SpearNumbers.PORTAL_RAW);
            for (LivingEntity t : hits.apply(c, Foes.MAX)) {
                SpearFx.flare(c, SpearFx.mid(t), 0.5f);
                c.shove(t, dest, 0.6);
            }
            if (c.form.has(SpearIds.PORTAL_ECHO)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SpearNumbers.PORTAL_ECHO_TICKS, 1, false, false, true));
            SpearFx.sound(c, dest, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.4f);
        });
        return true;
    }

    // ================================================================ Filo Rush
    static boolean rush(HeroCtx c) {
        ServerPlayer p = c.player;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SpearNumbers.RUSH_TICKS, SpearNumbers.RUSH_AMP, false, false, true));
        Set<UUID> struck = new HashSet<>();
        Vec3[] last = {p.position()};
        SpearFx.swing(c);
        SpearFx.sound(c, p.position(), SoundEvents.PHANTOM_FLAP, 1.0f, 1.6f);
        SpellRuntime.zone(c.level, SpearNumbers.RUSH_TICKS, SpearNumbers.RUSH_INTERVAL, age -> {
            if (!Tasks.live(c)) return;
            Vec3 now = p.position();
            for (LivingEntity e : Foes.around(c, now.add(0.0, 0.9, 0.0), SpearNumbers.RUSH_RADIUS, Foes.MAX)) {
                if (struck.size() >= SpearNumbers.RUSH_TARGETS || !struck.add(e.getUUID())) continue;
                if (Foes.hit(c, e, (float) SpearNumbers.RUSH_RAW)) {
                    c.shove(e, now, 0.7);
                    SpearFx.flare(c, SpearFx.mid(e), 0.5f);
                }
            }
            if (now.distanceToSqr(last[0]) > 1.0) c.fx(VfxShape.WIND_GALE, last[0].add(0.0, 0.8, 0.0), now.add(0.0, 0.8, 0.0), SpearFx.light(c), 8, 0.8f);
            last[0] = now;
        });
        return true;
    }

    // ================================================================ Hooked Pierce
    static boolean hook(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity target = Foes.aimed(c, SpearNumbers.HOOK_RANGE, 0.9);
        if (target == null) {
            c.say("Nothing to hook.");
            return false;
        }
        if (!c.control()) {
            c.say("You cannot bind another foe so soon.");
            return false;
        }
        Vec3 dir = c.look(), hand = SpearFx.hand(c, dir);
        SpearFx.swing(c);
        c.fx(VfxShape.THREAD_LINE, hand, SpearFx.mid(target), SpearFx.light(c), SpearNumbers.HOOK_DELAY + 3, 0.9f);
        SpearFx.sound(c, hand, SoundEvents.CHAIN_PLACE, 1.0f, 1.2f);
        Tasks.later(c, SpearNumbers.HOOK_DELAY, () -> {
            Vec3 eye = c.eye();
            if (!target.isAlive() || eye.distanceTo(SpearFx.mid(target)) > SpearNumbers.HOOK_RANGE + 3.0 || !Foes.sees(c, eye, target)) return;
            Foes.hit(c, target, (float) SpearNumbers.HOOK_RAW);
            Vec3 toTarget = target.position().subtract(p.position()).multiply(1.0, 0.0, 1.0);
            Vec3 away = toTarget.lengthSqr() < 1.0E-6 ? Moves.flat(c.look(), new Vec3(0.0, 0.0, 1.0)) : toTarget.normalize();
            if (BalanceLaw.isBoss(target)) {                                                      // a boss is not moved: the wielder is pulled to it
                Vec3 goal = target.position().subtract(away.scale(SpearNumbers.HOOK_STOP + target.getBbWidth() * 0.5));
                int y = Moves.surfaceBelow(c.level, goal.x, target.getY() + 1.0, goal.z, 4);
                if (y != Integer.MIN_VALUE && Moves.standable(c.level, p, (int) Math.floor(goal.x), y, (int) Math.floor(goal.z))) Moves.teleport(p, Moves.feet(c.level, goal.x, y, goal.z));
                return;
            }
            Vec3 goal = p.position().add(away.scale(SpearNumbers.HOOK_STOP));
            Vec3 pull = goal.subtract(target.position());
            double dist = pull.horizontalDistance();
            if (dist > 0.3) {
                Vec3 v = pull.multiply(1.0, 0.0, 1.0).normalize().scale(Math.min(1.6, 0.3 + dist * 0.18));
                target.setDeltaMovement(v.x, Math.max(0.2, target.getDeltaMovement().y), v.z);
                target.hurtMarked = true;
            }
            SpearFx.flare(c, SpearFx.mid(target), 0.6f);
        });
        return true;
    }
}
