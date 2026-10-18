package com.newuniverse.nusmp.heroweapon.weapon.sword;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.sword.SwordNumbers.*;

/**
 * Why: this is what each technique of the Legendary Sword DOES, one static method per technique, kept apart from the list of skills (which only names them and gives
 * their texts) so the part that touches the world is in one place that can be read top to bottom. The rules every body keeps: damage goes only through
 * {@link HeroCtx#hit} (after shortening the previous blow's frames of grace, so the many small blows of a barrage each land); a heal goes only through
 * {@link HeroCtx#heal}; anything that holds a foe asks {@link HeroCtx#control()} first and takes its length from the balance law; no block is placed or broken; nothing
 * runs longer than 20 seconds; a pulse that can overlap another is limited by a per-target cap so no target takes more than the technique's budget; a technique with a
 * wind-up shows its tell at once and acts after it, and the delayed part is skipped if the wielder died, left or put the weapon away in between; the numbers are
 * {@link SwordNumbers}'s.
 */
final class SwordOps {
    private SwordOps() {}

    // ================================================================ shared helpers
    /** True while a strike of this class is inside the damage code: the damage events it raises must not be taken for the wielder's own melee blows (server thread only). */
    private static boolean striking;

    static boolean isStriking() { return striking; }

    /**
     * A hit through the balance law, after the previous blow's frames of grace are cut to the point where a new blow lands in full. The hit is marked as ours while
     * it runs, so that the weapon's melee hook does not answer it with a bonus strike of its own (that would be a chain of blows with no end).
     */
    static boolean strike(HeroCtx c, LivingEntity t, double raw) {
        if (!(raw > 0)) return false;
        if (t.invulnerableTime > 10) t.invulnerableTime = 10;
        boolean was = striking;
        striking = true;
        try {
            return c.hit(t, (float) raw);
        } finally {
            striking = was;
        }
    }

    /** The same, limited by a per-target cap so overlapping pulses cannot pass the technique's budget. */
    static boolean strike(HeroCtx c, LivingEntity t, double raw, HitCap cap) {
        double allowed = cap.take(t.getUUID(), raw);
        return allowed > 0 && strike(c, t, allowed);
    }

    /** The horizontal part of a direction, normalised; south when there is none. */
    static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static String seconds(ServerPlayer p) { return Integer.toString((BalanceLaw.controlCooldownLeft(p) + 19) / 20); }

    private static boolean hostileProjectile(ServerPlayer w, Projectile p) {
        if (p.isRemoved()) return false;
        Entity owner = p.getOwner();
        return !(owner instanceof LivingEntity le && SwordTargets.isFriend(w, le));
    }

    /**
     * Moves the wielder up to {@code max} blocks along {@code dir} (horizontal), stopping short of a wall and at the farthest free spot, and returns how far it moved
     * (0 when it could not move: no room, or no connection to move a player on).
     */
    static double dash(HeroCtx c, Vec3 dir, double max) {
        ServerPlayer p = c.player;
        Vec3 from = p.position(), lift = new Vec3(0, p.getBbHeight() * 0.6, 0);
        Vec3 far = from.add(dir.scale(max));
        HitResult wall = c.level.clip(new ClipContext(from.add(lift), far.add(lift), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double blockedAt = wall.getType() == HitResult.Type.MISS ? -1.0 : wall.getLocation().distanceTo(from.add(lift));
        double len = SwordGeo.dashLength(max, blockedAt, 0.6);
        for (double d = len; d >= 1.0; d -= 0.5) {
            Vec3 dest = from.add(dir.scale(d));
            if (!c.level.noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) continue;
            try {
                p.teleportTo(dest.x, dest.y, dest.z);
                p.fallDistance = 0;
                return d;
            } catch (RuntimeException noConnection) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /** Takes a share of a foe's magicules and gives the wielder a smaller share of their own maximum; returns what the wielder got. */
    static double plunder(HeroCtx c, LivingEntity from, double takeShare, double keepShare) {
        try {
            var theirs = TensuraStorages.getExistenceFrom(from);
            if (theirs == null) return 0.0;
            double take = Math.min(theirs.getMagicule(), EnergyHelper.getMaxMagicule(from) * takeShare);
            if (!(take > 0)) return 0.0;
            theirs.setMagicule(theirs.getMagicule() - take);
            theirs.markDirty();
            var mine = TensuraStorages.getExistenceFrom(c.player);
            if (mine == null) return 0.0;
            double max = EnergyHelper.getMaxMagicule(c.player), gain = Math.min(take, max * keepShare);
            mine.setMagicule(Math.min(max, mine.getMagicule() + gain));
            mine.markDirty();
            return gain;
        } catch (RuntimeException noStorage) {
            return 0.0;
        }
    }

    // ================================================================ the ordinary tree
    /** Blitz Strike: a dash of seven blocks that cuts everything on the way. */
    static boolean blitz(HeroCtx c) {
        Vec3 dir = flat(c.look());
        Vec3 chest = c.player.position().add(0, 1.0, 0);
        SwordFx.tellLine(c.level, chest, chest.add(dir.scale(BLITZ_DASH)), SwordFx.TELL_SKY);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.8f);
        SwordRuntime.after(c, BLITZ_DELAY, () -> {
            Vec3 start = c.player.position().add(0, 1.0, 0);
            double moved = dash(c, dir, BLITZ_DASH);
            Vec3 end = c.player.position().add(0, 1.0, 0);
            List<LivingEntity> hit = moved > 0 ? SwordTargets.foesAlong(c, start, end, BLITZ_WIDTH) : SwordTargets.foesInCone(c, dir, 3.0, 50.0);
            for (LivingEntity t : hit) strike(c, t, BLITZ.raw());
            SwordFx.streak(c.level, start, end, SwordFx.SKY, 1.0f);
            SwordFx.crescent(c.level, start, end, SwordFx.STEEL, 1.1f);
            SwordFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
            if (moved > 0 || !hit.isEmpty()) SwordRuntime.state(c.player).ledger.armGhost(c.level.getGameTime(), GHOST_TICKS);
        });
        return true;
    }

    /** Tempest Dance: two sweeps of the blade round you. */
    static boolean tempest(HeroCtx c) {
        if (SwordTargets.foesAround(c, c.player.position(), TEMPEST_RADIUS).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        SwordFx.tellRing(c.level, c.player.position(), TEMPEST_RADIUS, SwordFx.TELL_SKY);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        HitCap cap = new HitCap(TEMPEST.total());
        for (int i = 0; i < 2; i++) {
            int sweep = i;
            SwordRuntime.after(c, TEMPEST_DELAY + i * TEMPEST_GAP, () -> {
                Vec3 at = c.player.position();
                for (LivingEntity t : SwordTargets.foesAround(c, at, TEMPEST_RADIUS)) {
                    strike(c, t, TEMPEST.raw(), cap);
                    c.shove(t, at, TEMPEST_PUSH);
                }
                SwordFx.gust(c.level, at, TEMPEST_RADIUS);
                Vec3 o = at.add(0, 1.0, 0);
                for (double[] p : SwordGeo.ring(o.x, o.z, TEMPEST_RADIUS * 0.8, 4, sweep * 45.0 + 20.0))
                    SwordFx.crescent(c.level, o, new Vec3(p[0], o.y, p[1]), SwordFx.STEEL, 1.2f);
                SwordFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 0.9f + sweep * 0.3f);
            });
        }
        return true;
    }

    /** Afterimage Step: a quick step aside that leaves a double behind and gives speed; the next blow leaves a ghost strike. */
    static boolean afterimage(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 motion = new Vec3(p.getX() - p.xo, 0, p.getZ() - p.zo);                  // a player's velocity is the client's: what moved them since last tick is on the server
        Vec3 dir = motion.lengthSqr() > 0.0025 ? motion.normalize() : flat(c.look()).scale(-1.0);
        Vec3 from = p.position();
        double moved = dash(c, dir, STEP_DASH);
        if (moved <= 0) {
            c.say("No room to step");
            return false;
        }
        SwordFx.afterimage(c.level, from, 1.8f);
        SwordFx.streak(c.level, from.add(0, 1, 0), p.position().add(0, 1, 0), SwordFx.SKY, 0.8f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, STEP_SPEED_TICKS, STEP_SPEED_LEVEL, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, STEP_GUARD_TICKS, 0, true, false));
        SwordRuntime.state(p).ledger.armGhost(c.level.getGameTime(), GHOST_TICKS);
        SwordFx.sound(c.level, from, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.8f);
        return true;
    }

    /** Flashing Sword: the blade flashes; foes in front are blinded for a moment (players are only lit up and slowed) and you run a little faster. */
    static boolean flashing(HeroCtx c) {
        Vec3 dir = c.look();
        Vec3 at = c.eye().add(dir.scale(1.2));
        SwordFx.flare(c.level, at, 1.4f);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.6f);
        for (LivingEntity t : SwordTargets.foesInCone(c, dir, FLASH_RANGE, FLASH_HALF_ANGLE)) {
            strike(c, t, FLASHING.raw());
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, FLASH_GLOW_TICKS, 0));
            if (t instanceof Player) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0));
            else t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLASH_BLIND_TICKS, 0));
        }
        c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FLASH_SPEED_TICKS, 0, true, true));
        return true;
    }

    /** Wind Cutter Whistle: three whistling crescents fly out in a fan. */
    static boolean windCutter(HeroCtx c) {
        Vec3 dir = c.look(), eye = c.eye();
        double h = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        HitCap cap = new HitCap(WIND_CUTTER.total());
        for (double[] d : SwordGeo.fan(dir.x, dir.z, CUTTER_COUNT, CUTTER_SPREAD)) {
            Vec3 v = new Vec3(d[0] * h, dir.y, d[1] * h);
            v = v.lengthSqr() < 1.0E-6 ? dir : v.normalize();
            Vec3 start = eye.add(v.scale(0.8)).add(0, -0.3, 0);
            SwordFx.crescent(c.level, start, start.add(v.scale(CUTTER_SPEED * CUTTER_LIFE)), SwordFx.SKY, 1.0f);
            SpellRuntime.bolt(c.player, start, v.scale(CUTTER_SPEED), CUTTER_RADIUS, CUTTER_LIFE, true, null,
                    (bolt, target) -> {
                        if (SwordTargets.isFoe(c.player, target)) strike(c, target, WIND_CUTTER.raw(), cap);
                    }, null);
        }
        SwordFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 2.0f);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.6f, 1.9f);
        return true;
    }

    /** Meteor Sword: stars fall in a ring where you point. */
    static boolean meteor(HeroCtx c) {
        Vec3 at = c.aim(METEOR_RANGE);
        SwordFx.tellRing(c.level, at, METEOR_RING + METEOR_RADIUS, SwordFx.TELL_THUNDER);
        SwordFx.tellColumn(c.level, at, 4.0, SwordFx.TELL_THUNDER);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
        HitCap cap = new HitCap(METEOR.total());
        double[][] ring = SwordGeo.ring(at.x, at.z, METEOR_RING, METEOR_STARS, 45.0);
        for (int i = 0; i < METEOR_STARS; i++) {
            Vec3 spot = new Vec3(ring[i][0], at.y, ring[i][1]);
            SwordRuntime.after(c, METEOR_DELAY + i * METEOR_GAP, () -> {
                SwordFx.star(c.level, spot);
                for (LivingEntity t : SwordTargets.foesAround(c, spot, METEOR_RADIUS)) strike(c, t, METEOR.raw(), cap);
                SwordFx.sound(c.level, spot, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.2f);
            });
        }
        return true;
    }

    /** Assassin Sword: blink behind the foe you look at and stab; twice as deep in the back of a foe that is not facing you. */
    static boolean assassin(HeroCtx c) {
        LivingEntity t = SwordTargets.aimFoe(c, ASSASSIN_RANGE);
        if (t == null) {
            List<LivingEntity> near = SwordTargets.foesAround(c, c.player.position(), ASSASSIN_RANGE);
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        LivingEntity victim = t;
        SwordFx.afterimage(c.level, c.player.position(), 1.8f);
        SwordFx.tellLine(c.level, c.player.getBoundingBox().getCenter(), victim.getBoundingBox().getCenter(), SwordFx.TELL_SKY);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.6f);
        SwordRuntime.after(c, ASSASSIN_DELAY, () -> {
            if (!victim.isAlive() || victim.isRemoved()) return;
            ServerPlayer p = c.player;
            double reach = ASSASSIN_RANGE + ASSASSIN_SLACK;
            if (victim.level() != c.level || victim.distanceToSqr(p) > reach * reach) {                    // it teleported or fled far: the blink fizzles, it never drags the wielder after it
                c.say("The target slipped away");
                return;
            }
            Vec3 face = flat(victim.getViewVector(1f));
            Vec3 spot = null;
            for (double side : new double[]{0.0, 1.2, -1.2}) {
                Vec3 across = new Vec3(-face.z, 0, face.x).scale(side);
                Vec3 cand = victim.position().subtract(face.scale(ASSASSIN_BEHIND)).add(across);
                if (c.level.noCollision(p, p.getBoundingBox().move(cand.subtract(p.position())))) {
                    spot = cand;
                    break;
                }
            }
            if (spot == null) return;
            try {
                p.teleportTo(c.level, spot.x, spot.y, spot.z, SwordGeo.yawOf(victim.getX() - spot.x, victim.getZ() - spot.z), 0f);
                p.fallDistance = 0;
            } catch (RuntimeException noConnection) {
                return;
            }
            boolean back = SwordGeo.behind(face.x, face.z, p.getX() - victim.getX(), p.getZ() - victim.getZ());
            strike(c, victim, ASSASSIN_BASE + (back ? ASSASSIN_BACKSTAB : 0.0));
            SwordFx.streak(c.level, spot.add(0, 1, 0), victim.getBoundingBox().getCenter(), SwordFx.SKY, 1.0f);
            SwordFx.flare(c.level, victim.getBoundingBox().getCenter(), 0.8f);
            SwordFx.sound(c.level, victim.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, back ? 0.8f : 1.3f);
            SwordRuntime.state(p).ledger.armGhost(c.level.getGameTime(), GHOST_TICKS);
        });
        return true;
    }

    /** Eagle Blade: a lunge whose point goes through armour. */
    static boolean eagleBlade(HeroCtx c) {
        Vec3 dir = flat(c.look());
        Vec3 chest = c.player.position().add(0, 1.0, 0);
        if (SwordTargets.foesAlong(c, chest, chest.add(dir.scale(EAGLE_DASH + 1.5)), 1.0).isEmpty()) {
            c.say("No foe in the line");
            return false;
        }
        SwordFx.tellLine(c.level, chest, chest.add(dir.scale(EAGLE_DASH + 1.5)), SwordFx.TELL_THUNDER);
        SwordFx.flare(c.level, c.eye().add(c.look().scale(1.0)), 0.8f);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.8f, 1.6f);
        SwordRuntime.after(c, EAGLE_DELAY, () -> {
            Vec3 start = c.player.position().add(0, 1.0, 0);
            List<LivingEntity> line = SwordTargets.foesAlong(c, start, start.add(dir.scale(EAGLE_DASH + 1.5)), 1.0);
            LivingEntity t = line.isEmpty() ? null : line.get(0);
            double reach = t == null ? EAGLE_DASH : Math.max(0.0, Math.min(EAGLE_DASH, t.distanceTo(c.player) - 1.2));
            if (reach >= 1.0) dash(c, dir, reach);
            SwordFx.streak(c.level, start, c.player.position().add(0, 1.0, 0), SwordFx.THUNDER, 1.1f);
            if (t == null || !t.isAlive()) return;
            double raw = EAGLE_BASE * EnergyBridge.armourBypass(t, c.player.damageSources().playerAttack(c.player), (float) EAGLE_BASE, (float) EAGLE_PIERCE);
            strike(c, t, Math.min(raw, EAGLE_BLADE.raw()));
            SwordFx.flare(c.level, t.getBoundingBox().getCenter(), 1.0f);
            SwordFx.sound(c.level, t.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.9f);
        });
        return true;
    }

    /** Gravity Sword: the weight round a foe grows; it and what stands near it are slowed, pulled down and cut. */
    static boolean gravity(HeroCtx c) {
        LivingEntity t = SwordTargets.aimFoe(c, GRAVITY_RANGE);
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        if (!c.control()) {
            c.say("Control is not ready for " + seconds(c.player) + " s");
            return false;
        }
        Vec3 centre = t.position();
        SwordFx.well(c.level, centre, GRAVITY_RADIUS, GRAVITY_PULSES * GRAVITY_GAP + 10);
        SwordFx.sound(c.level, centre, SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 0.7f);
        HitCap cap = new HitCap(GRAVITY.total());
        Map<UUID, Integer> held = new HashMap<>();
        SwordRuntime.every(c, (GRAVITY_PULSES - 1) * GRAVITY_GAP, GRAVITY_GAP, age -> {
            for (LivingEntity e : SwordTargets.foesAround(c, centre, GRAVITY_RADIUS)) {
                strike(c, e, GRAVITY.raw(), cap);
                int ticks = held.computeIfAbsent(e.getUUID(), k -> BalanceLaw.controlTicks(e, GRAVITY_SLOW_TICKS));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.min(ticks, GRAVITY_GAP + 5), 2));
                Vec3 vm = e.getDeltaMovement();
                e.setDeltaMovement(vm.x * 0.3, Math.min(vm.y, -0.55), vm.z * 0.3);
                e.hurtMarked = true;
            }
            SwordFx.sound(c.level, centre, SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
        });
        return true;
    }

    /** Skyfall Cleave: a leap and a heavy slam. */
    static boolean cleave(HeroCtx c) {
        Vec3 dir = flat(c.look());
        if (SwordTargets.foesAround(c, c.player.position().add(dir.scale(CLEAVE_LEAP * 0.5)), CLEAVE_LEAP + CLEAVE_RADIUS).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        Vec3 landing = c.player.position().add(dir.scale(CLEAVE_LEAP));
        SwordFx.tellRing(c.level, landing, CLEAVE_RADIUS, SwordFx.TELL_SKY);
        SwordFx.tellColumn(c.level, landing, 3.0, SwordFx.TELL_SKY);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
        SwordRuntime.after(c, CLEAVE_DELAY, () -> {
            Vec3 from = c.player.position();
            double need = Math.hypot(landing.x - from.x, landing.z - from.z);                  // the leap goes to the ring that was drawn, even if the wielder moved meanwhile
            if (need >= 1.0) dash(c, flat(landing.subtract(from)), Math.min(need, CLEAVE_LEAP + 1.5));
            Vec3 land = c.player.position();
            for (LivingEntity t : SwordTargets.foesAround(c, land, CLEAVE_RADIUS)) {
                strike(c, t, CLEAVE.raw());
                c.shove(t, land, CLEAVE_PUSH);
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.3, 0));
                t.hurtMarked = true;
            }
            SwordFx.streak(c.level, from.add(0, 1, 0), land.add(0, 0.5, 0), SwordFx.STEEL, 1.2f);
            SwordFx.crush(c.level, land, CLEAVE_RADIUS);
            SwordFx.sound(c.level, land, SoundEvents.MACE_SMASH_GROUND, 1.3f, 0.9f);
        });
        return true;
    }

    /** Phoenix Gale Blade: the sword takes red energy, then a storm of flame and a bird of fire fly at the foe. */
    static boolean phoenixGale(HeroCtx c) {
        Vec3 dir = c.look();
        SwordFx.tellGather(c.level, c.eye().add(dir.scale(1.0)), 1.2, SwordFx.TELL_FLAME, 16);
        SwordFx.tellCone(c.level, c.player.position().add(0, 0.2, 0), dir, PHOENIX_RANGE, PHOENIX_HALF_ANGLE, SwordFx.TELL_FLAME);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.6f);
        SwordRuntime.after(c, PHOENIX_DELAY, () -> {
            HitCap cap = new HitCap(PHOENIX_GALE.total());
            Vec3 eye = c.eye();
            for (LivingEntity t : SwordTargets.foesInCone(c, dir, PHOENIX_RANGE, PHOENIX_HALF_ANGLE)) {
                strike(c, t, PHOENIX_STORM, cap);
                t.igniteForSeconds(PHOENIX_FIRE_SECONDS);
            }
            SwordFx.storm(c.level, eye.add(dir.scale(0.6)), eye.add(dir.scale(PHOENIX_RANGE)));
            Vec3 start = eye.add(dir.scale(1.2));
            SwordFx.bird(c.level, start, start.add(dir.scale(PHOENIX_BIRD_SPEED * PHOENIX_BIRD_LIFE)), PHOENIX_BIRD_LIFE);
            SpellRuntime.bolt(c.player, start, dir.scale(PHOENIX_BIRD_SPEED), PHOENIX_BIRD_RADIUS, PHOENIX_BIRD_LIFE, false, null,
                    (bolt, target) -> {
                        if (!SwordTargets.isFoe(c.player, target)) return;
                        strike(c, target, PHOENIX_BIRD, cap);
                        target.igniteForSeconds(PHOENIX_FIRE_SECONDS);
                    }, (bolt, end) -> SwordFx.flame(c.level, end, 1.2f));
            SwordFx.sound(c.level, c.player.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 0.8f);
        });
        return true;
    }

    /** Float Sword: four swords circle you for eight seconds, each cutting a different foe that comes near once a second, and stop a few shots. */
    static boolean floatBlades(HeroCtx c) {
        SwordState st = SwordRuntime.state(c.player);
        long now = c.level.getGameTime();
        st.floatUntil = now + FLOAT_TICKS;
        int mine = ++st.castNumber;
        SwordFx.orbit(c.level, c.player, FLOAT_TICKS, SwordFx.STEEL);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f, 1.0f);
        HitCap cap = new HitCap(FLOAT_BLADES.total());
        int[] stops = {FLOAT_BLOCKS};
        SwordRuntime.every(c, FLOAT_TICKS, 2, age -> {
            if (st.castNumber != mine || c.level.getGameTime() >= st.floatUntil) return;
            Vec3 pos = c.player.position();
            if (stops[0] > 0) {
                for (Projectile pr : c.level.getEntitiesOfClass(Projectile.class, SwordTargets.box(pos, FLOAT_GUARD_RADIUS),
                        pr -> hostileProjectile(c.player, pr) && pr.distanceToSqr(pos) <= FLOAT_GUARD_RADIUS * FLOAT_GUARD_RADIUS)) {
                    SwordFx.flare(c.level, pr.position(), 0.5f);
                    pr.discard();
                    if (--stops[0] <= 0) break;
                }
            }
            if (age % FLOAT_GAP != 0) return;
            List<LivingEntity> foes = SwordTargets.foesAround(c, pos, FLOAT_RADIUS);
            int round = age / FLOAT_GAP;                                                              // each blade cuts a different foe; with more foes than blades the cuts rotate
            for (int i = 0; i < FLOAT_COUNT && i < foes.size(); i++) {
                LivingEntity t = foes.get((i + round * FLOAT_COUNT) % foes.size());
                if (strike(c, t, FLOAT_BLADES.raw(), cap)) SwordFx.crescent(c.level, pos.add(0, 1.4, 0), t.getBoundingBox().getCenter(), SwordFx.STEEL, 0.7f);
            }
        });
        return true;
    }

    /** Hundred Swords: the blade splits into a hundred and they fall where you point. */
    static boolean hundredSwords(HeroCtx c) {
        Vec3 at = c.aim(HUNDRED_RANGE);
        SwordFx.tellRing(c.level, at, HUNDRED_RADIUS, SwordFx.TELL_SKY);
        SwordFx.circle(c.level, at, HUNDRED_RADIUS * 0.6, HUNDRED_DELAY + 10, SwordFx.SKY);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f, 0.8f);
        HitCap cap = new HitCap(HUNDRED_SWORDS.total());
        SwordRuntime.after(c, HUNDRED_DELAY, () -> SwordRuntime.every(c, (HUNDRED_PULSES - 1) * HUNDRED_GAP, HUNDRED_GAP, age -> {
            for (LivingEntity t : SwordTargets.foesAround(c, at, HUNDRED_RADIUS)) strike(c, t, HUNDRED_SWORDS.raw(), cap);
            for (double[] p : SwordGeo.scatter(at.x, at.z, HUNDRED_RADIUS, 8, c.level.getGameTime() + age)) SwordFx.falling(c.level, new Vec3(p[0], at.y, p[1]), SwordFx.SKY);
            SwordFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.6f);
        }));
        return true;
    }

    /** Lightning Hundred Swords: after a long gathering, a hundred blades of lightning fall over a wide ring. */
    static boolean lightningHundred(HeroCtx c) {
        Vec3 at = c.aim(LIGHTNING_RANGE);
        SwordFx.circle(c.level, at, LIGHTNING_RADIUS * 0.8, LIGHTNING_DELAY + 10, SwordFx.THUNDER);
        SwordFx.tellRing(c.level, at, LIGHTNING_RADIUS, SwordFx.TELL_THUNDER);
        SwordFx.tellGather(c.level, at.add(0, 3, 0), LIGHTNING_RADIUS * 0.5, SwordFx.TELL_THUNDER, 24);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.WARDEN_SONIC_CHARGE, 1.4f, 0.9f);
        HitCap cap = new HitCap(LIGHTNING_HUNDRED.total());
        SwordRuntime.after(c, LIGHTNING_DELAY, () -> SwordRuntime.every(c, (LIGHTNING_PULSES - 1) * LIGHTNING_GAP, LIGHTNING_GAP, age -> {
            for (LivingEntity t : SwordTargets.foesAround(c, at, LIGHTNING_RADIUS)) strike(c, t, LIGHTNING_HUNDRED.raw(), cap);
            long seed = c.level.getGameTime() * 31L + age;
            for (double[] p : SwordGeo.scatter(at.x, at.z, LIGHTNING_RADIUS, 6, seed)) SwordFx.bolt(c.level, new Vec3(p[0], at.y, p[1]), SwordFx.THUNDER);
            for (double[] p : SwordGeo.scatter(at.x, at.z, LIGHTNING_RADIUS, 4, seed + 7)) SwordFx.falling(c.level, new Vec3(p[0], at.y, p[1]), SwordFx.WHITE);
            if (age == 0) SwordFx.sound(c.level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f, 1.0f);
            else SwordFx.sound(c.level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.3f);
        }));
        return true;
    }

    // ================================================================ the Greed and Gluttony series
    /** Guillotine: a great blade of gold falls on the spot where the foe stands, and what it cuts through gives up magicules. */
    static boolean guillotine(HeroCtx c) {
        LivingEntity t = SwordTargets.aimFoe(c, GUILLOTINE_RANGE);
        if (t == null) {
            c.say("No foe in sight");
            return false;
        }
        Vec3 spot = t.position();
        boolean cursed = c.form.curse();
        SwordFx.tellColumn(c.level, spot, 6.0, SwordFx.TELL_GOLD);
        SwordFx.tellRing(c.level, spot, 1.8, SwordFx.TELL_GOLD);
        SwordFx.falling(c.level, spot, SwordFx.GOLD);
        SwordFx.sound(c.level, spot, SoundEvents.ANVIL_PLACE, 1.0f, 0.6f);
        SwordRuntime.after(c, GUILLOTINE_DELAY, () -> {
            boolean took = false;
            for (LivingEntity v : SwordTargets.foesAround(c, spot, 1.8)) {
                if (strike(c, v, GUILLOTINE.raw()) && !took) took = plunder(c, v, STEAL_TAKE, STEAL_KEEP) > 0;
            }
            SwordFx.brand(c.level, spot.add(0, 1, 0), spot.add(0, 2, 0), 1.2f, SwordFx.GOLD);
            SwordFx.sound(c.level, spot, SoundEvents.ANVIL_LAND, 1.4f, 0.7f);
            if (cursed) HeroCurse.addCorruption(c.player, GUILLOTINE_CORRUPTION);
        });
        return true;
    }

    /** Gold Rebellion: a ring of golden blades; what it cuts is gilded, and your next blow on it takes a little more. */
    static boolean goldRebellion(HeroCtx c) {
        if (SwordTargets.foesAround(c, c.player.position(), REBELLION_RADIUS).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        SwordFx.tellRing(c.level, c.player.position(), REBELLION_RADIUS, SwordFx.TELL_GOLD);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        SwordRuntime.after(c, REBELLION_DELAY, () -> {
            SwordState st = SwordRuntime.state(c.player);
            long now = c.level.getGameTime();
            Vec3 at = c.player.position();
            for (LivingEntity t : SwordTargets.foesAround(c, at, REBELLION_RADIUS)) {
                if (strike(c, t, GOLD_REBELLION.raw())) st.marks.mark(t.getUUID(), now, GILDED_TICKS);
                c.shove(t, at, 0.6);
            }
            Vec3 o = at.add(0, 1.0, 0);
            for (double[] p : SwordGeo.ring(o.x, o.z, REBELLION_RADIUS * 0.9, 8, 0.0)) SwordFx.darkSlash(c.level, o, new Vec3(p[0], o.y, p[1]), 1.0f);
            SwordFx.brand(c.level, at.add(0, 1, 0), at.add(0, 2, 0), 1.4f, SwordFx.GOLD);
            SwordFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        });
        return true;
    }

    /** Maneater: a maw closes on up to three foes in front of you and feeds you. */
    static boolean maneater(HeroCtx c) {
        Vec3 dir = c.look();
        if (SwordTargets.foesInCone(c, dir, MANEATER_RANGE + 0.5, MANEATER_HALF_ANGLE).isEmpty()) {
            c.say("No foe in reach");
            return false;
        }
        SwordFx.tellCone(c.level, c.player.position().add(0, 0.2, 0), dir, MANEATER_RANGE, MANEATER_HALF_ANGLE, SwordFx.TELL_BROWN);
        SwordFx.sound(c.level, c.player.position(), SoundEvents.PHANTOM_BITE, 1.2f, 0.5f);
        SwordRuntime.after(c, MANEATER_DELAY, () -> {
            List<LivingEntity> in = SwordTargets.foesInCone(c, dir, MANEATER_RANGE, MANEATER_HALF_ANGLE);
            if (in.size() > MANEATER_BITES) in = new ArrayList<>(in.subList(0, MANEATER_BITES));
            int bitten = 0;
            for (LivingEntity t : in) if (strike(c, t, MANEATER.raw())) bitten++;
            if (bitten > 0) c.heal(c.player, (float) (c.player.getMaxHealth() * MANEATER_HEAL * bitten));
            Vec3 jaws = c.player.position().add(dir.scale(2.0)).add(0, 1.0, 0);
            SwordFx.maw(c.level, jaws, jaws.add(dir), 1.4f);
            SwordFx.sound(c.level, jaws, SoundEvents.FOX_BITE, 1.3f, 0.5f);
        });
        return true;
    }

    /** Strong Decline: the ground splits in a line in front of you and fangs rise out of it. */
    static boolean strongDecline(HeroCtx c) {
        Vec3 dir = flat(c.look());
        Vec3 from = c.player.position(), to = from.add(dir.scale(DECLINE_LENGTH));
        boolean cursed = c.form.curse();
        SwordFx.tellLine(c.level, from.add(0, 0.2, 0), to.add(0, 0.2, 0), SwordFx.TELL_BROWN);
        SwordFx.tellRing(c.level, to, 1.8, SwordFx.TELL_BROWN);
        SwordFx.sound(c.level, from, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.8f, 0.7f);
        SwordRuntime.after(c, DECLINE_DELAY, () -> {
            for (LivingEntity t : SwordTargets.foesAlong(c, from.add(0, 0.5, 0), to.add(0, 0.5, 0), DECLINE_HALF_WIDTH)) {
                strike(c, t, STRONG_DECLINE.raw());
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DECLINE_SLOW_TICKS, 1));
            }
            SwordFx.fissure(c.level, from, to);
            SwordFx.sound(c.level, from.add(dir.scale(DECLINE_LENGTH * 0.5)), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4f, 0.6f);
            if (cursed) HeroCurse.addCorruption(c.player, DECLINE_CORRUPTION);
        });
        return true;
    }

    /**
     * Insatiable Maw: the ultimate of the series; three great maws close over a wide ring and each wave feeds the wielder. The ring stays where it is drawn (the
     * spot the wielder stood on at the cast), so a foe that leaves it in the wind-up is missed and what the eye sees is what the rules apply to.
     */
    static boolean insatiableMaw(HeroCtx c) {
        Vec3 at = c.player.position();
        boolean cursed = c.form.curse();
        SwordFx.banquet(c.level, at, MAW_RADIUS, MAW_DELAY + MAW_WAVES * MAW_GAP);
        SwordFx.tellRing(c.level, at, MAW_RADIUS, SwordFx.TELL_BROWN);
        SwordFx.tellGather(c.level, at.add(0, 1, 0), MAW_RADIUS * 0.5, SwordFx.TELL_GOLD, 24);
        SwordFx.sound(c.level, at, SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.5f);
        HitCap cap = new HitCap(INSATIABLE_MAW.total());
        SwordRuntime.after(c, MAW_DELAY, () -> SwordRuntime.every(c, (MAW_WAVES - 1) * MAW_GAP, MAW_GAP, age -> {
            int hit = 0;
            for (LivingEntity t : SwordTargets.foesAround(c, at, MAW_RADIUS)) if (strike(c, t, INSATIABLE_MAW.raw(), cap)) hit++;
            if (hit > 0) c.heal(c.player, (float) (c.player.getMaxHealth() * MAW_HEAL));
            SwordFx.maw(c.level, at.add(0, 1, 0), at.add(0, 2, 0), 2.4f);
            SwordFx.brand(c.level, at.add(0, 1, 0), at.add(0, 2, 0), 1.6f, SwordFx.GOLD);
            SwordFx.sound(c.level, at, SoundEvents.PHANTOM_BITE, 1.6f, 0.4f);
            if (age == 0 && cursed) HeroCurse.addCorruption(c.player, MAW_CORRUPTION);
        }));
        return true;
    }
}
