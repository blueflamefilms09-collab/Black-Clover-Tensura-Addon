package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Legendary Spear is the longest-reaching melee weapon of the four, and its identity is the straight line: a thrust that runs through a row of foes,
 * a javelin, a volley of lances, a falling star and the two beams that grow into Brionac (the series' Triaina is the beam that evolves into Brionac). They
 * share one shape (find the line, show it, wait, strike everything in it once) and one rule for fairness: the line is shown first, a wall stops it, and the
 * damage is the sum per foe through the balance law. This class holds those six techniques; the numbers are {@link SpearNumbers}'s.
 */
final class SpearStrikes {
    private SpearStrikes() {}

    private static double stretch(HeroCtx c) { return c.form.has(SpearIds.LONGSHAFT) ? SpearNumbers.LONGSHAFT : 1.0; }

    // ================================================================ Piercing Thrust
    static boolean thrust(HeroCtx c) {
        Vec3 eye = c.eye(), dir = SpearAim.dir(c);
        Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(SpearNumbers.THRUST_LENGTH * stretch(c))));
        Vec3 hand = SpearFx.hand(c, dir);
        SpearFx.swing(c);
        c.fx(VfxShape.THREAD_LINE, hand, end, SpearFx.light(c), SpearNumbers.THRUST_DELAY + 4, 0.8f);
        SpearFx.sound(c, eye, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
        Tasks.later(c, SpearNumbers.THRUST_DELAY, () -> {
            Hits hits = new Hits();
            for (LivingEntity t : Foes.along(c, eye, end, SpearNumbers.THRUST_WIDTH, SpearNumbers.THRUST_TARGETS)) hits.add(t, (float) SpearNumbers.THRUST_RAW);
            c.fx(VfxShape.STAR_MAGIC, hand, end, SpearFx.main(c), 4, 0.9f);
            for (LivingEntity t : hits.apply(c, SpearNumbers.THRUST_TARGETS)) {
                SpearFx.flare(c, SpearFx.mid(t), 0.45f);
                c.shove(t, eye, 0.25);
            }
            SpearFx.sound(c, end, SoundEvents.TRIDENT_HIT, 0.9f, 1.2f);
        });
        return true;
    }

    // ================================================================ Javelin Throw
    static boolean javelin(HeroCtx c) {
        Vec3 dir = SpearAim.dir(c);
        Vec3 hand = SpearFx.hand(c, dir);
        Vec3 reach = hand.add(dir.scale(SpearNumbers.JAVELIN_SPEED * SpearNumbers.JAVELIN_LIFE));
        SpearFx.swing(c);
        SpearFx.flare(c, hand, 0.6f);
        c.fx(VfxShape.THREAD_LINE, hand, Rays.clipEnd(c, hand, reach), SpearFx.light(c), SpearNumbers.JAVELIN_DELAY + 2, 0.5f);       // the path, shown before the throw
        SpearFx.sound(c, hand, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 1.3f);
        Tasks.later(c, SpearNumbers.JAVELIN_DELAY, () -> {
            Vec3 end = Rays.clipEnd(c, hand, reach);
            int flight = Math.max(2, (int) Math.ceil(hand.distanceTo(end) / SpearNumbers.JAVELIN_SPEED));
            c.fx(VfxShape.COMPASS_NEEDLE, hand, end, SpearFx.light(c), flight, 1.8f);
            int[] struck = {0};
            SpellRuntime.bolt(c.player, hand, dir.scale(SpearNumbers.JAVELIN_SPEED), SpearNumbers.JAVELIN_RADIUS, SpearNumbers.JAVELIN_LIFE, true, null,
                    (bolt, target) -> {
                        if (struck[0] >= SpearNumbers.JAVELIN_TARGETS || !Foes.valid(c, target)) return;
                        if (Foes.hit(c, target, (float) SpearNumbers.JAVELIN_RAW)) {
                            struck[0]++;
                            SpearFx.flare(c, SpearFx.mid(target), 0.5f);
                        }
                        if (struck[0] >= SpearNumbers.JAVELIN_TARGETS) bolt.life = 1;
                    },
                    (bolt, at) -> {
                        SpearFx.flare(c, at, 0.8f);
                        SpearFx.sound(c, at, SoundEvents.TRIDENT_HIT, 1.0f, 0.9f);
                    });
        });
        return true;
    }

    // ================================================================ Chaos Spear
    static boolean chaos(HeroCtx c) {
        Vec3 eye = c.eye(), look = c.look();
        List<LivingEntity> seen = new ArrayList<>();
        for (LivingEntity e : Foes.around(c, c.player.position(), SpearNumbers.CHAOS_RANGE, Foes.MAX)) if (Foes.sees(c, eye, e)) seen.add(e);
        if (seen.isEmpty()) {
            c.say("No prey in sight.");
            return false;
        }
        List<LivingEntity> picks = seen.size() > SpearNumbers.CHAOS_LANCES ? new ArrayList<>(seen.subList(0, SpearNumbers.CHAOS_LANCES)) : seen;
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        SpearFx.swing(c);
        for (int i = 0; i < SpearNumbers.CHAOS_LANCES; i++) {
            LivingEntity t = picks.get(i % picks.size());
            double side = (i - (SpearNumbers.CHAOS_LANCES - 1) / 2.0) * 0.45;
            Vec3 from = eye.add(right.scale(side)).add(0.0, 0.25 + 0.1 * (i % 2), 0.0).add(look.scale(0.6));
            Vec3 to = SpearFx.mid(t);
            Tasks.later(c, i, () -> c.fx(VfxShape.COMPASS_NEEDLE, from, to, SpearFx.light(c), 5, 0.7f));
        }
        SpearFx.sound(c, eye, SoundEvents.ENDER_EYE_LAUNCH, 0.9f, 1.4f);
        Tasks.later(c, SpearNumbers.CHAOS_DELAY + SpearNumbers.CHAOS_LANCES, () -> {
            Vec3 now = c.eye();
            Hits hits = new Hits();                                                                    // a lance lands only on a foe still in sight: cover turns it
            for (int i = 0; i < SpearNumbers.CHAOS_LANCES; i++) {
                LivingEntity t = picks.get(i % picks.size());
                if (t.isAlive() && Foes.sees(c, now, t)) hits.add(t, (float) SpearNumbers.CHAOS_PER_LANCE);
            }
            for (LivingEntity t : hits.apply(c, SpearNumbers.CHAOS_LANCES)) SpearFx.flare(c, SpearFx.mid(t), 0.5f);
        });
        return true;
    }

    // ================================================================ Meteor Spear
    static boolean meteor(HeroCtx c) {
        Vec3 eye = c.eye(), dir = SpearAim.dir(c);
        Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(SpearNumbers.METEOR_LENGTH * stretch(c))));
        Vec3 hand = SpearFx.hand(c, dir);
        SpearFx.swing(c);
        SpearFx.flare(c, hand, 0.9f);
        c.fx(VfxShape.THREAD_LINE, hand, end, SpearFx.light(c), SpearNumbers.METEOR_DELAY + 2, 1.0f);
        Tells.ring(c.level, end, SpearNumbers.METEOR_SPLASH_RADIUS, SpearFx.rgb(c));
        SpearFx.sound(c, eye, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 0.9f);
        Tasks.later(c, SpearNumbers.METEOR_DELAY, () -> {
            Hits hits = new Hits();
            for (LivingEntity t : Foes.along(c, eye, end, SpearNumbers.METEOR_WIDTH, SpearNumbers.METEOR_TARGETS)) hits.add(t, (float) SpearNumbers.METEOR_RAW);
            for (LivingEntity t : Foes.around(c, end, SpearNumbers.METEOR_SPLASH_RADIUS, Foes.MAX)) hits.add(t, (float) SpearNumbers.METEOR_SPLASH);
            c.fx(VfxShape.LIGHT_BLADE, hand, end, SpearFx.light(c), 8, 1.1f);
            c.fx(VfxShape.STAR_MAGIC, hand, end, SpearFx.main(c), 6, 1.0f);
            for (LivingEntity t : hits.apply(c, SpearNumbers.METEOR_TARGETS)) {
                SpearFx.flare(c, SpearFx.mid(t), 0.5f);
                c.shove(t, eye, 0.5);
            }
            SpearFx.flare(c, end, 1.4f);
            SpearFx.sound(c, end, SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f, 0.8f);
        });
        return true;
    }

    // ================================================================ Triaina and Brionac
    static boolean triaina(HeroCtx c) {
        return beam(c, SpearNumbers.TRIAINA_LENGTH * stretch(c), SpearNumbers.TRIAINA_WIDTH, SpearNumbers.TRIAINA_RAW, SpearNumbers.TRIAINA_DELAY,
                SpearNumbers.TRIAINA_TARGETS, 1.1f, false);
    }

    static boolean brionac(HeroCtx c) {
        return beam(c, SpearNumbers.BRIONAC_LENGTH, SpearNumbers.BRIONAC_WIDTH, SpearNumbers.BRIONAC_RAW, SpearNumbers.BRIONAC_DELAY, 12, 2.0f, true);
    }

    /**
     * A beam of the gem's light. The gem gathers light for {@code delay} ticks while a line shows where the beam will go (the wielder may still turn,
     * and the line follows, so everyone sees it coming and can leave it); at the end the beam strikes everything in its lane once, walls stop it.
     */
    private static boolean beam(HeroCtx c, double length, double width, double raw, int delay, int targets, float size, boolean grand) {
        SpearFx.swing(c);
        for (int step = 0; step < delay; step += 4) {
            int when = step;
            Tasks.later(c, when, () -> {
                Vec3 dir = SpearAim.dir(c), eye = c.eye();
                Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(length)));
                Vec3 hand = SpearFx.hand(c, dir);
                c.fx(VfxShape.THREAD_LINE, hand, end, SpearFx.light(c), 6, grand ? 1.1f : 0.8f);
                SpearFx.flare(c, hand, (grand ? 0.5f : 0.35f) + 0.04f * when);
            });
        }
        if (grand) VfxSpawn.sendFollowing(c.level, VfxShape.MAGIC_CIRCLE, c.player, c.player.position().add(0.0, 0.1, 0.0), SpearFx.light(c), delay + 6, 2.6f);
        SpearFx.sound(c, c.eye(), grand ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_POWER_SELECT, 1.2f, grand ? 1.3f : 1.6f);
        Tasks.later(c, delay, () -> {
            Vec3 eye = c.eye(), dir = SpearAim.dir(c);
            Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(length)));
            Vec3 hand = SpearFx.hand(c, dir);
            Hits hits = new Hits();
            for (LivingEntity t : Foes.along(c, eye, end, width, targets)) hits.add(t, (float) raw);
            c.fx(VfxShape.LIGHT_BLADE, hand, end, SpearFx.light(c), 10, size);
            c.fx(VfxShape.STAR_MAGIC, hand, end, SpearFx.main(c), 6, size);
            for (LivingEntity t : hits.apply(c, targets)) {
                SpearFx.flare(c, SpearFx.mid(t), grand ? 0.9f : 0.6f);
                c.shove(t, eye, grand ? 0.9 : 0.5);
                if (grand) c.fx(VfxShape.FIRE_BURST, SpearFx.mid(t), SpearFx.mid(t), SpearFx.RED, 14, 0.9f);
            }
            SpearFx.flare(c, end, grand ? 1.8f : 1.0f);
            SpearFx.sound(c, end, SoundEvents.LIGHTNING_BOLT_IMPACT, grand ? 1.4f : 1.0f, grand ? 1.3f : 1.7f);
        });
        return true;
    }
}
