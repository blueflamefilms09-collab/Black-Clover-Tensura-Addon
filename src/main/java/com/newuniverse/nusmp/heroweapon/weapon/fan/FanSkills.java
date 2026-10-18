package com.newuniverse.nusmp.heroweapon.weapon.fan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the nine techniques of the Vassal Fan, one small method each, so the whole weapon reads as a list: a wide sweep of cutting wind (Flower Wind), a
 * crescent flung along the aim (Moon Break), a push that also turns arrows back (Repelling Gust), a pull (Gale Pull), a gliding dash (Wind Dance), a veil of
 * mist (Mist Veil), a blessing for friends (Snow Plum), one thrust that cracks armour (Tortoise Shell Cracker) and the red petal tornado (Reverse Snow Moon
 * Flower). Every number comes from {@link FanNumbers}, every word from {@link FanText}; damage, heals and hard control go through {@code HeroCtx} (the balance
 * law), effects through the mod's own effect system, and nothing changes a block or spawns an entity. A technique with nothing to act on refuses (returns false)
 * so it costs nothing.
 */
final class FanSkills {
    private FanSkills() {}

    // the palette: the wind is pale blue-white, the petals snow, the tornado of the ultimate red (the series' Reverse Snow Moon Flower is a red tornado of petals)
    private static final int WIND = 0xFFCFEFFF, PETAL = 0xFFF3F8FF, MIST = 0xFFDDEFFF, CRIMSON = 0xFFE0405A, SPIRIT = 0xFF6FE3FF;
    private static final int WIND_RGB = 0xBFE6FF, CRIMSON_RGB = 0xE0405A;

    static List<HeroSkill> all() {
        List<HeroSkill> l = new ArrayList<>();
        l.add(flowerWind());
        l.add(moonBreak());
        l.add(repelGust());
        l.add(galePull());
        l.add(windDance());
        l.add(mistVeil());
        l.add(snowPlum());
        l.add(tortoiseShellCracker());
        l.add(reverseSnowMoonFlower());
        return List.copyOf(l);
    }

    private static SimpleSkill make(String id, SimpleSkill.Body body) {
        Budget.Spec s = FanNumbers.spec(id);
        return SimpleSkill.of(id, FanText.skillName(id), FanText.skillDesc(id), s.cooldown(), s.cost(), body);
    }

    /** Gathers {@code n} stacks of magic residue (when the form has the passive) and tells the wielder. */
    static void gain(HeroCtx c, int n) {
        if (!c.form.has(FanNumbers.P_RESIDUE)) return;
        ResidueLedger res = FanState.RESIDUE.of(c.player.getUUID());
        if (res.add(n)) c.say("Residue " + res.stacks() + "/" + FanNumbers.RESIDUE_MAX);
    }

    /** Attacking ends the Mist Veil: the mist is lifted from the wielder and the invisibility with it. */
    static void unveil(HeroCtx c) {
        UUID id = c.player.getUUID();
        if (!FanState.VEIL.active(id, c.level.getGameTime())) return;
        FanState.VEIL.clear(id);
        c.player.removeEffect(MobEffects.INVISIBILITY);
    }

    /** Counts a full melee swing toward the next stack. */
    static void swung(HeroCtx c) {
        if (!c.form.has(FanNumbers.P_RESIDUE)) return;
        ResidueLedger res = FanState.RESIDUE.of(c.player.getUUID());
        if (res.swing()) c.say("Residue " + res.stacks() + "/" + FanNumbers.RESIDUE_MAX);
    }

    // ------------------------------------------------------------------ Flower Wind
    private static HeroSkill flowerWind() {
        return make(FanNumbers.FLOWER_WIND, c -> {
            ResidueLedger res = FanState.RESIDUE.of(c.player.getUUID());
            boolean surge = c.form.has(FanNumbers.P_DRAGON_SURGE) && res.full();
            double range = FanNumbers.flowerRange(surge), raw = FanNumbers.flowerRaw(surge);
            List<LivingEntity> foes = GlassKit.foesInCone(c, range, FanNumbers.FLOWER_HALF_ANGLE, FanNumbers.FLOWER_TARGETS);
            if (foes.isEmpty()) {
                c.say("The wind finds no foe in front of you.");
                return false;
            }
            Vec3 eye = c.eye(), look = c.look(), feet = c.player.position();
            unveil(c);
            for (LivingEntity e : foes) if (GlassKit.strike(c, e, raw)) GlassKit.push(c, e, feet, FanNumbers.FLOWER_SHOVE);
            c.fx(VfxShape.WIND_SLASH, eye.add(look.scale(0.6)), eye.add(look.scale(range)), WIND, 14, surge ? 2.0f : 1.5f);
            Vec3 bloom = eye.add(look.scale(1.8)).subtract(0.0, 0.4, 0.0);          // the petals open a little way ahead of you, not in your face
            c.fx(VfxShape.CHERRY_BLOSSOM_FX3, bloom, bloom.add(look.scale(range * 0.6)), PETAL, 18, surge ? 0.9f : 0.6f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
            GlassKit.swing(c);
            if (foes.size() >= FanNumbers.SWEEP_RESIDUE_FOES) gain(c, 1);
            return true;
        });
    }

    // ------------------------------------------------------------------ Moon Break
    private static HeroSkill moonBreak() {
        return make(FanNumbers.MOON_BREAK, c -> {
            Vec3 eye = c.eye(), look = c.look();
            Vec3 end = GlassKit.clipEnd(c, eye, eye.add(look.scale(FanNumbers.MOON_RANGE)));
            List<LivingEntity> foes = GlassKit.foesAlong(c, eye, end, FanNumbers.MOON_WIDTH, FanNumbers.MOON_TARGETS);
            if (foes.isEmpty()) {
                c.say("The crescent has nothing in its path.");
                return false;
            }
            unveil(c);
            for (LivingEntity e : foes) GlassKit.strike(c, e, FanNumbers.MOON_RAW);
            c.fx(VfxShape.WIND_EMPEROR, eye.add(look.scale(0.8)), end, WIND, 16, 0.9f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
            c.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.6f);
            GlassKit.swing(c);
            if (foes.size() >= FanNumbers.SWEEP_RESIDUE_FOES) gain(c, 1);
            return true;
        });
    }

    // ------------------------------------------------------------------ Repelling Gust
    private static boolean deflectable(HeroCtx c, Projectile pr) {
        if (!pr.isAlive() || pr.getDeltaMovement().lengthSqr() < 0.01) return false;
        var owner = pr.getOwner();
        return owner == null || (owner != c.player && !owner.isAlliedTo(c.player));
    }

    private static HeroSkill repelGust() {
        return make(FanNumbers.REPEL_GUST, c -> {
            double r = FanNumbers.GUST_RADIUS + (c.form.has(FanNumbers.P_SEALING_TIDE) ? FanNumbers.TIDE_EXTRA : 0.0);
            Vec3 feet = c.player.position();
            List<LivingEntity> foes = GlassKit.foes(c, feet, r, FanNumbers.GUST_TARGETS);
            List<Projectile> shots = c.level.getEntitiesOfClass(Projectile.class, new AABB(feet, feet).inflate(r), pr -> deflectable(c, pr));
            if (foes.isEmpty() && shots.isEmpty()) {
                c.say("The gust has nothing to push.");
                return false;
            }
            for (LivingEntity e : foes) GlassKit.push(c, e, feet, FanNumbers.GUST_SHOVE);
            for (int i = 0; i < Math.min(shots.size(), FanNumbers.GUST_PROJECTILES); i++) shots.get(i).deflect(ProjectileDeflection.REVERSE, c.player, c.player, true);
            c.fx(VfxShape.WIND_RING, feet, feet.add(0.0, 1.0, 0.0), WIND, 24, (float) (r * 0.36));
            c.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.9f, 1.4f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Gale Pull
    private static HeroSkill galePull() {
        return make(FanNumbers.GALE_PULL, c -> {
            double range = FanNumbers.PULL_RANGE + (c.form.has(FanNumbers.P_SEALING_TIDE) ? FanNumbers.TIDE_EXTRA : 0.0);
            List<LivingEntity> foes = new ArrayList<>();
            for (LivingEntity e : GlassKit.foesInCone(c, range, FanNumbers.PULL_HALF_ANGLE, FanNumbers.PULL_TARGETS)) if (!BalanceLaw.isBoss(e)) foes.add(e);
            if (foes.isEmpty()) {
                c.say("The gale finds nothing it can draw in.");
                return false;
            }
            if (!GlassKit.control(c)) return false;
            Vec3 look = c.look(), feet = c.player.position();
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            flat = flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            Vec3 point = feet.add(flat.scale(FanNumbers.PULL_POINT_AHEAD)).add(0.0, 0.9, 0.0);
            for (LivingEntity e : foes) GlassKit.drawToward(e, point, FanNumbers.PULL_GAIN, FanNumbers.PULL_MAX_SPEED);
            GlassKit.later(c, FanNumbers.PULL_SECOND_AFTER, () -> {
                for (LivingEntity e : foes) if (e.isAlive()) GlassKit.drawToward(e, point, FanNumbers.PULL_GAIN, FanNumbers.PULL_MAX_SPEED);
            });
            c.fx(VfxShape.WIND_TORNADO, point.subtract(0.0, 0.9, 0.0), point, WIND, 24, 1.3f);
            for (int i = 0; i < Math.min(3, foes.size()); i++) c.fx(VfxShape.WIND_GALE, foes.get(i).getBoundingBox().getCenter(), point, WIND, 12, 0.8f);
            c.sound(SoundEvents.PHANTOM_FLAP, 1.0f, 0.6f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Wind Dance
    private static HeroSkill windDance() {
        return make(FanNumbers.WIND_DANCE, c -> {
            Vec3 look = c.look();
            double speed = FanNumbers.DANCE_SPEED * (c.form.has(FanNumbers.P_TIGER_POUNCE) ? FanNumbers.POUNCE_FACTOR : 1.0);
            Geo.Impulse v = Geo.dash(look.x, look.y, look.z, speed, FanNumbers.DANCE_UP, FanNumbers.DANCE_DOWN, FanNumbers.DANCE_LIFT, c.player.onGround());
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            flat = flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            Vec3 start = GlassKit.chest(c.player);
            Vec3 end = GlassKit.clipEnd(c, start, start.add(flat.scale(FanNumbers.DANCE_LENGTH)));
            List<LivingEntity> brushed = GlassKit.foesAlong(c, start, end, FanNumbers.DANCE_WIDTH, FanNumbers.DANCE_TARGETS);
            if (!brushed.isEmpty()) unveil(c);
            for (LivingEntity e : brushed) {
                if (GlassKit.strike(c, e, FanNumbers.DANCE_RAW)) GlassKit.push(c, e, start, FanNumbers.DANCE_SHOVE);
            }
            GlassKit.impulse(c.player, new Vec3(v.x(), v.y(), v.z()), false);
            c.player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FanNumbers.DANCE_SLOW_FALL_TICKS, 0, false, false, true));
            GlassKit.follow(c, VfxShape.WIND_ZEPHYR, WIND, 36, 0.9f);
            c.fx(VfxShape.WIND_SLASH, start, end, PETAL, 12, 1.0f);
            c.sound(SoundEvents.ELYTRA_FLYING, 0.8f, 1.5f);
            return true;
        });
    }

    // ------------------------------------------------------------------ Mist Veil
    private static HeroSkill mistVeil() {
        return make(FanNumbers.MIST_VEIL, c -> {
            UUID id = c.player.getUUID();
            FanState.VEIL.set(id, c.level.getGameTime() + FanNumbers.VEIL_TICKS);
            c.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, FanNumbers.VEIL_TICKS, 0, false, false, true));
            Vec3 feet = c.player.position();
            int lost = 0;
            for (Mob m : c.level.getEntitiesOfClass(Mob.class, new AABB(feet, feet).inflate(FanNumbers.VEIL_RADIUS), m -> m.getTarget() == c.player)) {
                m.setTarget(null);
                lost++;
            }
            GlassKit.follow(c, VfxShape.MIST_MAGIC, MIST, FanNumbers.VEIL_TICKS, 1.2f);
            c.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f, 1.6f);
            c.say(lost > 0 ? "The mist closes round you; " + lost + (lost == 1 ? " hunter loses" : " hunters lose") + " you." : "The mist closes round you.");
            return true;
        });
    }

    // ------------------------------------------------------------------ Snow Plum
    private static HeroSkill snowPlum() {
        return make(FanNumbers.SNOW_PLUM, c -> {
            Vec3 feet = c.player.position();
            List<LivingEntity> friends = GlassKit.friends(c, FanNumbers.PLUM_RADIUS, FanNumbers.PLUM_TARGETS);
            for (LivingEntity f : friends) {
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FanNumbers.PLUM_TICKS, 0, false, true, true));
                if (f.getHealth() < f.getMaxHealth()) c.heal(f, (float) FanNumbers.plumHeal(f.getMaxHealth()));
            }
            c.fx(VfxShape.CHERRY_BLOSSOM_FX2, feet, feet, PETAL, 60, (float) FanNumbers.PLUM_RADIUS);
            for (int i = 1; i < Math.min(5, friends.size()); i++) c.fx(VfxShape.SEAL_WOUND, GlassKit.chest(c.player), friends.get(i).getBoundingBox().getCenter(), SPIRIT, 20, 0.8f);
            c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            return true;
        });
    }

    // ------------------------------------------------------------------ Tortoise Shell Cracker
    private static HeroSkill tortoiseShellCracker() {
        return make(FanNumbers.TORTOISE_SHELL_CRACKER, c -> {
            LivingEntity target = GlassKit.focus(c, FanNumbers.TORTOISE_RANGE);
            if (target == null) {
                c.say("No foe on your crosshair.");
                return false;
            }
            unveil(c);
            Vec3 from = c.eye().add(c.look().scale(0.8));
            // the tell: a thin line to the mark and the wind coiling at the fan tip
            GlassKit.line(c, from, target.getBoundingBox().getCenter(), WIND_RGB);
            c.fx(VfxShape.STAR_MAGIC, from, from.add(c.look().scale(0.7)), WIND, FanNumbers.TORTOISE_DELAY + 2, 0.9f);
            c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.8f);
            GlassKit.swing(c);
            GlassKit.later(c, FanNumbers.TORTOISE_DELAY, () -> {
                if (!target.isAlive() || target.distanceTo(c.player) > FanNumbers.TORTOISE_RANGE + 3.0 || !c.player.hasLineOfSight(target)) {
                    c.say("The thrust lost its mark.");
                    return;
                }
                double raw = FanNumbers.tortoiseRaw(target.getArmorValue());
                Vec3 centre = target.getBoundingBox().getCenter();
                if (GlassKit.strike(c, target, raw)) GlassKit.push(c, target, c.player.position(), FanNumbers.TORTOISE_SHOVE);
                c.fx(VfxShape.WIND_GALE, c.eye().add(c.look().scale(0.8)), centre, WIND, 12, 1.0f);
                c.fx(VfxShape.STAR_MAGIC, centre.add(0.0, 0.6, 0.0), centre, PETAL, 14, 1.2f);
                c.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
                gain(c, 1);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Reverse Snow Moon Flower
    private static HeroSkill reverseSnowMoonFlower() {
        return make(FanNumbers.SNOW_MOON_FLOWER, c -> {
            unveil(c);
            Vec3 aimed = c.aim(FanNumbers.SNOW_MOON_AIM);
            Vec3 centre = GlassKit.ground(c, aimed, 8.0);
            // the tell: a red ring on the ground where the tornado will stand and the petals gathering there
            GlassKit.ring(c, centre, FanNumbers.SNOW_MOON_RADIUS, CRIMSON_RGB);
            c.fx(VfxShape.CHERRY_BLOSSOM_FX2, centre, centre, CRIMSON, FanNumbers.SNOW_MOON_DELAY + 6, (float) FanNumbers.SNOW_MOON_RADIUS);
            c.sound(SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.7f);
            GlassKit.swing(c);
            GlassKit.later(c, FanNumbers.SNOW_MOON_DELAY, () -> {
                int stacks = FanState.RESIDUE.of(c.player.getUUID()).spend();
                if (stacks > 0) c.say("Residue burned: " + stacks);
                double raw = FanNumbers.snowMoonPulse(stacks);
                int span = FanNumbers.SNOW_MOON_PULSES * FanNumbers.SNOW_MOON_INTERVAL;
                c.fx(VfxShape.WIND_TORNADO, centre, centre, CRIMSON, span + 10, (float) FanNumbers.SNOW_MOON_RADIUS);
                c.fx(VfxShape.CHERRY_BLOSSOM_FX3, centre.add(0.0, 0.4, 0.0), centre.add(0.0, 2.5, 0.0), CRIMSON, 24, 1.4f);
                c.sound(SoundEvents.WIND_CHARGE_THROW, 1.0f, 0.6f);
                GlassKit.pulses(c, FanNumbers.SNOW_MOON_PULSES, FanNumbers.SNOW_MOON_INTERVAL, i -> {
                    for (LivingEntity e : GlassKit.foes(c, centre, FanNumbers.SNOW_MOON_RADIUS, FanNumbers.SNOW_MOON_TARGETS)) {
                        GlassKit.strike(c, e, raw);
                        GlassKit.drawToward(e, centre.add(0.0, 1.0, 0.0), FanNumbers.SNOW_MOON_DRAW, 0.25);
                    }
                    if (i == FanNumbers.SNOW_MOON_PULSES - 1) c.fx(VfxShape.CHERRY_BLOSSOM_FX3, centre.add(0.0, 0.4, 0.0), centre.add(0.0, 3.0, 0.0), CRIMSON, 28, 2.0f);
                });
            });
            return true;
        }).asUltimate();
    }
}
