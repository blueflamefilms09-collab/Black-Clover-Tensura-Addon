package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Geometry;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Timed;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.newuniverse.nusmp.heroweapon.weapon.gauntlet.GauntletNumbers.*;

/**
 * Why: the bodies of the Gauntlet's nine techniques. They are close-range and fast, so most of them resolve in a few ticks, but every one that does more than
 * 8 raw damage shows its area first (a ring, a lane, an aura on the fist) and lands a moment later through {@link Timed}, which re-checks the wielder when it
 * runs. A technique that has nothing to hit is refused (no cost, no cooldown, the points stay). Points are spent when the technique is certain to happen and
 * are paid back by the points it earns when it hits. All damage goes through {@code HeroCtx.hit}, all healing through {@code HeroCtx.heal}, nothing breaks or
 * places a block, and the wielder is only moved by {@link Moves} (a lunge or a blink to a spot with room, ground and no lava).
 */
final class GauntletSpells {
    private GauntletSpells() {}

    /** The gauntlet's colours: 0xRRGGBB for the ground rings, 0xAARRGGBB for the effects. */
    static final int ORANGE_RGB = 0xFFA43A, ORANGE = 0xFFFFA43A, FIRE = 0xFFFF6A1E;

    // ================================================================ shared pieces
    private static PointPool pool(HeroCtx c) { return GauntletState.pool(c.player.getUUID()); }

    private static long now(HeroCtx c) { return c.level.getGameTime(); }

    /** Gives the wielder skill points (not above the cap); only a form that lists the Skill Power-up Points passive keeps a pool. */
    static void gain(HeroCtx c, int n) {
        if (c.form.has("rank_points")) pool(c).gain(n, now(c));
    }

    private static int spend(HeroCtx c, Rank rank) { return pool(c).spend(rank.maxSpend(), now(c)); }

    private static float raw(HeroCtx c, Rank rank, int spent, LivingEntity t) {
        boolean undead = c.form.has("spirit_bane") && t.getType().is(EntityTypeTags.UNDEAD);
        return (float) rank.raw(spent, undead);
    }

    private static void swing(HeroCtx c) { c.player.swing(InteractionHand.MAIN_HAND); }

    /** The horizontal part of the look direction, normalised (south when the wielder looks straight up or down). */
    private static Vec3 flatLook(HeroCtx c) {
        Vec3 look = c.look();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    /** Knocks a raised shield out of the foe's hands (a player's shield goes on cooldown, anything else stops using its item). */
    static void breakGuard(LivingEntity t) {
        if (!t.isBlocking()) return;
        if (t instanceof Player p) p.disableShield();
        else t.stopUsingItem();
    }

    /** Throws a foe upwards; a boss only stumbles. */
    private static void launch(LivingEntity t, double lift) {
        double v = BalanceLaw.isBoss(t) ? lift * 0.2 : lift;
        t.setDeltaMovement(t.getDeltaMovement().add(0.0, v, 0.0));
        t.hurtMarked = true;
    }

    // ================================================================ Air Strike Rush
    static boolean rush(HeroCtx c) {
        if (c.player.isPassenger()) {
            c.say("You cannot rush while riding");
            return false;
        }
        int spent = spend(c, RUSH);
        Vec3 start = c.player.position();
        Vec3 dir = flatLook(c);
        swing(c);
        Moves.dash(c.player, dir, RUSH_BLOCKS, 0.1);
        c.fx(VfxShape.FX_SPIRIT_OVERDRIVE, start.add(0.0, 0.3, 0.0), start.add(dir.scale(RUSH_BLOCKS)).add(0.0, 0.3, 0.0), ORANGE, 12, 1.0f);
        c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        Timed.later(c, RUSH_DELAY, live -> {
            Vec3 end = live.player.position().add(dir.scale(1.5));
            int n = 0;
            for (LivingEntity t : Foes.along(live, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), RUSH_WIDTH, RUSH.spec().maxTargets())) {
                if (live.hit(t, raw(live, RUSH, spent, t))) {
                    live.shove(t, start, 0.4);
                    live.fx(VfxShape.GRAVITY_IMPACT, t.position(), t.position(), ORANGE, 12, 1.2f);
                    n++;
                }
            }
            if (n > 0) gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }

    // ================================================================ Moonlight Kick
    static boolean moonlightKick(HeroCtx c) {
        if (Foes.cone(c, KICK_RANGE, KICK_HALF_ANGLE, 1).isEmpty()) {
            c.say("Nothing in reach of the kick");
            return false;
        }
        int spent = spend(c, MOONLIGHT_KICK);
        swing(c);
        Vec3 flat = flatLook(c);
        Vec3 from = c.player.position().add(0.0, 0.3, 0.0);
        // the tell: the two edges of the arc and the crescent that sweeps it; the kick lands a moment later on whoever is still inside
        for (double edge : new double[]{KICK_HALF_ANGLE, -KICK_HALF_ANGLE}) {
            double[] h = Geometry.turnYaw(flat.x, flat.z, edge);
            Tells.line(c.level, from.add(0.0, 0.6, 0.0), from.add(0.0, 0.6, 0.0).add(h[0] * KICK_RANGE, 0.0, h[1] * KICK_RANGE), ORANGE_RGB);
        }
        c.fx(VfxShape.SLASH_WAVE, from.add(flat.scale(0.4)), from.add(flat.scale(KICK_RANGE)).add(0.0, 1.5, 0.0), ORANGE, 10, 1.0f);
        c.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.4f);
        Timed.later(c, KICK_DELAY, live -> {
            Vec3 here = live.player.position();
            int n = 0;
            for (LivingEntity t : Foes.around(live, here, KICK_RANGE, MOONLIGHT_KICK.spec().maxTargets())) {
                Vec3 to = t.position().subtract(here);
                if (!Geometry.inCone(to.x, 0.0, to.z, flat.x, 0.0, flat.z, KICK_HALF_ANGLE)) continue;
                if (live.hit(t, raw(live, MOONLIGHT_KICK, spent, t))) {
                    launch(t, KICK_LIFT + KICK_LIFT_PER_POINT * spent);
                    n++;
                }
            }
            if (n > 0) gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }

    // ================================================================ Afterimage Palm
    static boolean afterimagePalm(HeroCtx c) {
        List<LivingEntity> near = Foes.cone(c, PALM_RANGE, PALM_HALF_ANGLE, 1);
        if (near.isEmpty()) {
            c.say("No foe to slip behind");
            return false;
        }
        LivingEntity t = near.get(0);
        Vec3 origin = c.player.position();
        Vec3 face = new Vec3(t.getLookAngle().x, 0.0, t.getLookAngle().z);
        Vec3 behind = face.lengthSqr() < 1.0e-6 ? t.position().subtract(origin.subtract(t.position()).normalize().scale(PALM_BEHIND))
                : t.position().subtract(face.normalize().scale(PALM_BEHIND));
        float yaw = (float) Geometry.yawOf(t.getX() - behind.x, t.getZ() - behind.z);
        if (!Moves.blinkTo(c.level, c.player, behind.x, t.getY(), behind.z, yaw)) {
            c.say("There is no room behind that foe");
            return false;
        }
        int spent = spend(c, AFTERIMAGE_PALM);
        c.fx(VfxShape.MIRROR_STEP, origin, origin, ORANGE, 24, 1.8f);
        c.fx(VfxShape.WIND_RING, c.player.position().add(0.0, 0.2, 0.0), c.player.position().add(0.0, 1.0, 0.0), ORANGE, 14, 1.4f);
        c.sound(SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.7f);
        swing(c);
        Timed.later(c, PALM_DELAY, live -> {
            if (!t.isAlive()) return;
            breakGuard(t);
            if (live.hit(t, raw(live, AFTERIMAGE_PALM, spent, t))) {
                live.shove(t, live.player.position(), 0.5);
                live.fx(VfxShape.GRAVITY_IMPACT, t.position(), t.position(), ORANGE, 14, 1.5f);
                live.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                gain(live, POINTS_TECHNIQUE);
            }
        });
        return true;
    }

    // ================================================================ Shaking Quaking Fist
    static boolean quakingFist(HeroCtx c) {
        Vec3 dir = flatLook(c);
        Vec3 from = c.player.position().add(0.0, 1.0, 0.0);
        Vec3 to = Rays.reach(c, from, dir, FIST_LENGTH).end();
        int spent = spend(c, QUAKING_FIST);
        swing(c);
        Tells.line(c.level, from, to, ORANGE_RGB);
        VfxSpawn.sendFollowing(c.level, VfxShape.GRAVITY_AURA, c.player, c.player.position(), ORANGE, FIST_DELAY + 4, 0.9f);
        c.sound(SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 0.7f);
        Timed.later(c, FIST_DELAY, live -> {
            int n = 0;
            for (LivingEntity t : Foes.along(live, from, to, FIST_WIDTH, QUAKING_FIST.spec().maxTargets())) {
                if (live.hit(t, raw(live, QUAKING_FIST, spent, t))) {
                    live.shove(t, from.subtract(dir), FIST_PUSH);
                    Status.slow(t, FIST_SLOW_TICKS, 1);
                    n++;
                }
            }
            live.fx(VfxShape.GRAVITY_LANES, from, to, ORANGE, 12, (float) FIST_WIDTH);
            live.fx(VfxShape.GRAVITY_IMPACT, to, to, ORANGE, 14, 1.6f);
            live.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.5f);
            if (n > 0) gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }

    // ================================================================ Tiger Leap
    static boolean tigerLeap(HeroCtx c) {
        if (c.player.isPassenger()) {
            c.say("You cannot leap while riding");
            return false;
        }
        int spent = spend(c, TIGER_LEAP);
        Vec3 dir = flatLook(c);
        Vec3 start = c.player.position();
        Vec3 landing = Rays.reach(c, start.add(0.0, 1.0, 0.0), dir, LEAP_BLOCKS).end().subtract(0.0, 1.0, 0.0);
        swing(c);
        Moves.dash(c.player, dir, LEAP_BLOCKS, LEAP_LIFT);
        Tells.ring(c.level, landing, LEAP_RADIUS, ORANGE_RGB);
        c.fx(VfxShape.FX_SPIRIT_OVERDRIVE, start.add(0.0, 0.3, 0.0), landing.add(0.0, 0.3, 0.0), ORANGE, 14, 1.0f);
        c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
        Timed.later(c, LEAP_DELAY, live -> {
            Vec3 here = live.player.position();
            int n = 0;
            for (LivingEntity t : Foes.around(live, here, LEAP_RADIUS, TIGER_LEAP.spec().maxTargets())) {
                if (live.hit(t, raw(live, TIGER_LEAP, spent, t))) {
                    live.shove(t, here, LEAP_PUSH);
                    n++;
                }
            }
            Tells.ring(live.level, here, LEAP_RADIUS, ORANGE_RGB);
            live.fx(VfxShape.GRAVITY_IMPACT, here, here, ORANGE, 16, (float) LEAP_RADIUS * 0.6f);
            live.sound(SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 0.8f);
            if (n > 0) gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }

    // ================================================================ Turtle Crush (heavy)
    static boolean turtleCrush(HeroCtx c) {
        Vec3 center = c.player.position().add(flatLook(c).scale(CRUSH_AHEAD));
        int spent = spend(c, TURTLE_CRUSH);
        swing(c);
        Tells.ring(c.level, center, CRUSH_RADIUS, ORANGE_RGB);
        c.fx(VfxShape.GRAVITY_WELL, center, center, ORANGE, CRUSH_DELAY + 6, (float) CRUSH_RADIUS);
        VfxSpawn.sendFollowing(c.level, VfxShape.GRAVITY_AURA, c.player, c.player.position(), ORANGE, CRUSH_DELAY + 4, 1.0f);
        c.sound(SoundEvents.ANVIL_USE, 0.8f, 0.6f);
        Timed.later(c, CRUSH_DELAY, live -> {
            int n = 0;
            for (LivingEntity t : Foes.around(live, center, CRUSH_RADIUS, TURTLE_CRUSH.spec().maxTargets())) {
                breakGuard(t);
                if (live.hit(t, raw(live, TURTLE_CRUSH, spent, t))) {
                    Status.weaken(t, CRUSH_WEAK_TICKS);
                    live.shove(t, center, 0.5);
                    n++;
                }
            }
            live.fx(VfxShape.GRAVITY_IMPACT, center, center, ORANGE, 18, (float) CRUSH_RADIUS);
            live.sound(SoundEvents.ANVIL_LAND, 1.0f, 0.7f);
            if (n > 0) gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }

    // ================================================================ Iron Guard
    static boolean ironGuard(HeroCtx c) {
        long now = now(c);
        GauntletState.setGuard(c.player.getUUID(), new GauntletState.Guard(now + GUARD_TICKS));
        VfxSpawn.sendFollowing(c.level, VfxShape.GRAVITY_AURA, c.player, c.player.position(), ORANGE, GUARD_TICKS, 0.8f);
        c.sound(SoundEvents.SHIELD_BLOCK, 0.9f, 0.6f);
        c.say("Iron Guard");
        return true;
    }

    /**
     * What the wielder takes from a blow: Hardened Fist trims it, and inside the guard it is cut again, gives a skill point, and the first blow of the stance is
     * answered with a counter that never exceeds the damage that was taken.
     */
    static float onDamaged(HeroCtx c, DamageSource source, float amount) {
        float out = amount;
        if (c.form.has("hardened_fist")) out *= (float) HARDENED_TAKEN;
        GauntletState.Guard g = GauntletState.guard(c.player.getUUID(), now(c));
        if (g != null && (source.getEntity() != null || source.getDirectEntity() != null)) {
            out *= (float) GUARD_TAKEN;
            if (g.absorbed < GUARD_POINTS_MAX) {
                g.absorbed++;
                gain(c, POINTS_GUARD);
            }
            if (!g.countered && source.getEntity() instanceof LivingEntity foe && Foes.isFoe(c.player, foe) && foe.distanceToSqr(c.player) <= 36.0) {
                g.countered = true;
                if (c.hit(foe, (float) Math.min(COUNTER_RAW, amount))) {
                    c.shove(foe, c.player.position(), 0.6);
                    c.fx(VfxShape.GRAVITY_IMPACT, foe.position(), foe.position(), ORANGE, 12, 1.2f);
                }
            }
        }
        return out;
    }

    // ================================================================ Second Wind
    static boolean secondWind(HeroCtx c) {
        c.heal(c.player, (float) (c.player.getMaxHealth() * SECOND_WIND.heal()));
        Status.cleanse(c.player);
        gain(c, POINTS_WIND);
        VfxSpawn.sendFollowing(c.level, VfxShape.SPIRIT_AURA, c.player, c.player.position(), ORANGE, 30, 0.8f);
        c.sound(SoundEvents.PLAYER_BURP, 0.5f, 1.4f);
        return true;
    }

    // ================================================================ Dragon Slaying Conflagration Fist (ultimate)
    static boolean dragonSlayer(HeroCtx c) {
        boolean heart = c.form.has("dragon_heart");
        int held = pool(c).points(now(c));
        int counted = dragonPoints(held, heart);
        if (counted < DRAGON_MIN) {
            c.say("The Conflagration Fist needs " + DRAGON_MIN + " skill points (you hold " + counted + ")");
            return false;
        }
        LivingEntity first = Foes.aimed(c, DRAGON_LENGTH, DRAGON_WIDTH);
        if (first == null) {
            c.say("No foe in the line of the Conflagration Fist");
            return false;
        }
        pool(c).spend(PointPool.MAX, now(c));
        swing(c);
        Vec3 from = c.player.position().add(0.0, 1.0, 0.0);
        Tells.line(c.level, from, first.getBoundingBox().getCenter(), ORANGE_RGB);
        Tells.ring(c.level, first.position(), DRAGON_SPLASH_RADIUS, ORANGE_RGB);
        VfxSpawn.sendFollowing(c.level, VfxShape.GRAVITY_AURA, c.player, c.player.position(), FIRE, DRAGON_DELAY + 4, 1.4f);
        c.fx(VfxShape.FIRE_BURST, from, from, FIRE, DRAGON_DELAY, 1.0f);
        c.sound(SoundEvents.BLAZE_SHOOT, 1.0f, 0.5f);
        Timed.later(c, DRAGON_DELAY, live -> {
            if (!first.isAlive() || first.distanceTo(live.player) > DRAGON_LENGTH + 3.0) {
                live.say("The Conflagration Fist missed");
                return;
            }
            Vec3 at = first.getBoundingBox().getCenter();
            breakGuard(first);
            boolean undead = live.form.has("spirit_bane") && first.getType().is(EntityTypeTags.UNDEAD);
            live.hit(first, (float) dragonRaw(counted, undead));
            live.shove(first, live.player.position(), 0.9);
            for (LivingEntity t : Foes.around(live, at, DRAGON_SPLASH_RADIUS, DRAGON_SLAYER.spec().maxTargets())) {
                if (t == first) continue;
                live.hit(t, (float) DRAGON_SPLASH_RAW);
                live.shove(t, at, 0.7);
            }
            live.fx(VfxShape.FX_SPIRIT_OVERDRIVE, live.player.position().add(0.0, 1.0, 0.0), at, FIRE, 12, 1.2f);
            live.fx(VfxShape.FIRE_PILLAR, first.position(), first.position(), FIRE, 30, 2.0f);
            live.fx(VfxShape.FLAME_EXPLOSION, at, at, FIRE, 26, 2.0f);
            live.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.8f);
            gain(live, POINTS_TECHNIQUE);
        });
        return true;
    }
}
