package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.forms.FormMastery;
import com.newuniverse.nusmp.heroweapon.rules.HeroXp;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Legendary Bow is a DRAWN weapon: the owner's rule is that the draw time sets the power, and its techniques on the sneak and hero keys change
 * the next shot instead of replacing it (a sniper arrow with a visible laser sight, a net that pins, a mark that lets friends hurt more, a burst, a seeker
 * that homes, a second arrow, Pride's command). The bow needs no arrows in the pack: it looses arrows of light made of the wielder's magicules, a small
 * cost per draw (real arrows are not used: the bow stays independent of the inventory and of tipped-arrow edge cases). This class is the release of the
 * string and the seven kinds of arrow. Arrows are hitscan lines that stop at the first wall, or a short-lived homing bolt for the seeker; no entity is
 * spawned; damage goes through the balance law, once per foe per arrow, never on a creature the techniques may not strike.
 */
final class BowShots {
    private BowShots() {}

    // ================================================================ the string is let go
    /** The core hands the release of the drawn bow here. Always handles it (returns true) while the weapon is in the hand. */
    static boolean release(HeroCtx c, int chargeTicks) {
        if (!c.summoned()) return false;
        double f = DrawMath.fraction(chargeTicks, c.form.has(BowIds.QUICKDRAW));
        if (!DrawMath.enough(f)) return true;
        long now = c.level.getGameTime();
        if (BowState.shotLocked(c, now)) return true;
        ArmedMode mode = BowState.armed(c, now);
        if (mode != ArmedMode.NONE && f < mode.minFraction() - 1e-9) mode = ArmedMode.NONE;          // too weak for the change: a plain arrow, the arming waits
        if (!c.pay(DrawMath.cost(f))) {
            c.say("The string goes slack: no magicules to make the arrow.");
            return true;
        }
        BowState.markShot(c, now);
        if (mode != ArmedMode.NONE) BowState.disarm(c);
        BowFx.swing(c);
        Vec3 eye = c.eye();
        BowFx.sound(c, eye, SoundEvents.ARROW_SHOOT, 1.0f, 1.2f + 0.4f * (float) f);
        switch (mode) {
            case SNIPER -> sniper(c);
            case NET -> net(c);
            case MARK -> mark(c);
            case BURST -> burst(c);
            case SEEK -> seek(c);
            case SECOND -> second(c, f);
            case COMMAND -> command(c);
            default -> plain(c, f, plainRaw(c, f));
        }
        return true;
    }

    // ================================================================ every few ticks: the laser sight and the fading of an arming
    static void tick(HeroCtx c) {
        if (!c.bond.hasExtra("bow")) return;
        long now = c.level.getGameTime();
        ArmedMode stored = BowState.stored(c);
        if (stored == ArmedMode.NONE) return;
        if (now > BowState.armedUntil(c)) {
            BowState.disarm(c);
            c.say(stored.label() + " fades.");
            return;
        }
        if (stored == ArmedMode.SNIPER) {                                    // everyone can see the lane of the sniper arrow
            Vec3 eye = c.eye(), dir = c.look();
            Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(BowNumbers.SNIPER_RANGE)));
            c.fx(VfxShape.THREAD_LINE, BowFx.muzzle(c, dir), end, BowFx.SIGHT, 7, 0.45f);
        }
        if ((now / 5) % 4 == 0) c.say(stored.label() + " ready: a draw of " + Math.round(stored.minFraction() * 100) + "% or more (" + (BowState.armedUntil(c) - now + 19) / 20 + "s).");
    }

    // ================================================================ the plain arrow of light
    private static double plainRaw(HeroCtx c, double f) {
        long now = c.level.getGameTime();
        return DrawMath.raw(f, c.form.stat("attack_damage"), BowState.lawUp(c, now) ? BowNumbers.LAW_BONUS : 1.0, c.form.has(BowIds.PRIDE_ARROWS) ? BowNumbers.PRIDE_BONUS : 1.0);
    }

    /** One arrow of light along the look: {@code raw} for the first foe, {@code PIERCE_FACTOR} of it for a second with Piercing Arrows. Returns the foes struck. */
    private static List<LivingEntity> plain(HeroCtx c, double f, double raw) {
        Vec3 eye = c.eye(), dir = c.look();
        double range = DrawMath.range(f, c.form.has(BowIds.FARSIGHT));
        double width = c.form.has(BowIds.PRIDE_ARROWS) ? BowNumbers.PRIDE_WIDTH : BowNumbers.BASIC_WIDTH;
        Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(range)));
        int pierce = c.form.has(BowIds.PIERCING_ARROWS) ? 2 : 1;
        Hits hits = new Hits();
        double r = raw;
        for (LivingEntity t : Foes.along(c, eye, end, width, pierce)) {
            double each = r;
            if (c.form.has(BowIds.EAGLE_EYE) && t.distanceTo(c.player) > BowNumbers.EAGLE_EYE_DISTANCE) each = Math.min(BowNumbers.BASIC_MAX_RAW, each * BowNumbers.EAGLE_EYE_BONUS);
            hits.add(t, (float) each);
            r *= BowNumbers.PIERCE_FACTOR;
        }
        Vec3 muzzle = BowFx.muzzle(c, dir);
        c.fx(VfxShape.STAR_MAGIC, muzzle, end, BowFx.main(c), Math.max(3, (int) (muzzle.distanceTo(end) / 10.0) + 2), (float) (0.5 + 0.5 * f + (width > 0.6 ? 0.6 : 0.0)));
        List<LivingEntity> struck = hits.apply(c, pierce);
        for (LivingEntity t : struck) BowFx.flare(c, BowFx.mid(t), 0.4f);
        reward(c, struck);
        return struck;
    }

    /** A hit with an arrow is a hit of the weapon: a point of mastery for the form and a little experience, as a melee blow would give. */
    private static void reward(HeroCtx c, List<LivingEntity> struck) {
        if (struck.isEmpty()) return;
        FormMastery.hit(c.player);
        HeroXp.gain(c.player, 1L);
    }

    // ================================================================ the armed arrows
    /** The first foe along the look within {@code range} (width {@code width}), or null. */
    private static LivingEntity first(HeroCtx c, double range, double width) {
        Vec3 eye = c.eye();
        Vec3 end = Rays.clipEnd(c, eye, eye.add(c.look().scale(range)));
        List<LivingEntity> in = Foes.along(c, eye, end, width, 1);
        return in.isEmpty() ? null : in.get(0);
    }

    private static void line(HeroCtx c, double range, int ticks, float size) {
        Vec3 dir = c.look(), eye = c.eye();
        c.fx(VfxShape.STAR_MAGIC, BowFx.muzzle(c, dir), Rays.clipEnd(c, eye, eye.add(dir.scale(range))), BowFx.main(c), ticks, size);
    }

    /** Eagle Piercing Shot: a sniper arrow along the whole lane, through up to six foes. */
    private static void sniper(HeroCtx c) {
        Vec3 eye = c.eye(), dir = c.look();
        Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(BowNumbers.SNIPER_RANGE)));
        Vec3 muzzle = BowFx.muzzle(c, dir);
        Hits hits = new Hits();
        for (LivingEntity t : Foes.along(c, eye, end, BowNumbers.SNIPER_WIDTH, BowNumbers.SNIPER_TARGETS)) hits.add(t, (float) BowNumbers.SNIPER_RAW);
        c.fx(VfxShape.LIGHT_BLADE, muzzle, end, BowFx.main(c), 10, 1.0f);
        c.fx(VfxShape.STAR_MAGIC, muzzle, end, BowFx.main(c), 6, 1.2f);
        List<LivingEntity> struck = hits.apply(c, BowNumbers.SNIPER_TARGETS);
        for (LivingEntity t : struck) {
            BowFx.flare(c, BowFx.mid(t), 0.8f);
            c.shove(t, eye, 0.3);
        }
        BowFx.flare(c, end, 1.0f);
        BowFx.sound(c, end, SoundEvents.CROSSBOW_SHOOT, 1.2f, 0.5f);
        BowFx.sound(c, end, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.4f);
        reward(c, struck);
    }

    /** Bind Arrow: the first foe is hit and pinned (through the shared control lock and the law's limits on how long). */
    private static void net(HeroCtx c) {
        line(c, BowNumbers.NET_RANGE, 5, 0.9f);
        LivingEntity t = first(c, BowNumbers.NET_RANGE, BowNumbers.BASIC_WIDTH);
        if (t == null) return;
        List<LivingEntity> struck = new ArrayList<>();
        if (Foes.hit(c, t, (float) BowNumbers.NET_RAW)) struck.add(t);
        if (t.isAlive()) {
            if (c.control()) {
                int ticks = BalanceLaw.controlTicks(t, BowNumbers.NET_ROOT);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
                c.fx(VfxShape.THREAD_WEB, t.position(), t.position(), BowFx.light(c), ticks + 10, 1.8f);
                BowFx.sound(c, t.position(), SoundEvents.CHAIN_PLACE, 1.0f, 1.4f);
            } else {
                c.say("The net finds no hold: the control lock is still on.");
            }
        }
        reward(c, struck);
    }

    /** Marking Arrow: the first foe glows through walls and is frail, so friends hurt it more. */
    private static void mark(HeroCtx c) {
        line(c, BowNumbers.MARK_RANGE, 5, 0.7f);
        LivingEntity t = first(c, BowNumbers.MARK_RANGE, BowNumbers.BASIC_WIDTH);
        if (t == null) return;
        List<LivingEntity> struck = new ArrayList<>();
        if (Foes.hit(c, t, (float) BowNumbers.MARK_RAW)) struck.add(t);
        if (t.isAlive()) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, BowNumbers.MARK_TICKS, 0));
            Fx.addEffect(t, "tensura:fragility", BowNumbers.MARK_TICKS, 0);
            c.fx(VfxShape.COMPASS_LOCK, BowFx.mid(t), BowFx.mid(t), BowFx.main(c), 30, 1.2f);
            BowFx.sound(c, t.position(), SoundEvents.LODESTONE_COMPASS_LOCK, 1.0f, 1.0f);
        }
        reward(c, struck);
    }

    /** Burst Arrow: it sticks, a ring shows where, and half a second later it bursts. */
    private static void burst(HeroCtx c) {
        Vec3 eye = c.eye(), dir = c.look();
        Vec3 end = Rays.clipEnd(c, eye, eye.add(dir.scale(BowNumbers.BURST_RANGE)));
        List<LivingEntity> in = Foes.along(c, eye, end, BowNumbers.BASIC_WIDTH, 1);
        Vec3 impact = in.isEmpty() ? end : BowFx.mid(in.get(0));
        c.fx(VfxShape.STAR_MAGIC, BowFx.muzzle(c, dir), impact, BowFx.main(c), 4, 1.0f);
        Tells.ring(c.level, impact, BowNumbers.BURST_RADIUS, BowFx.rgb(c));
        BowFx.flare(c, impact, 0.6f);
        BowFx.sound(c, impact, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
        Tasks.later(c, BowNumbers.BURST_FUSE, () -> {
            Hits hits = new Hits();
            for (LivingEntity t : Foes.around(c, impact, BowNumbers.BURST_RADIUS, Foes.MAX)) hits.add(t, (float) BowNumbers.BURST_RAW);
            c.fx(VfxShape.FIRE_BURST, impact, impact, BowFx.main(c), 16, 1.6f);
            List<LivingEntity> struck = hits.apply(c, BowNumbers.BURST_TARGETS);
            for (LivingEntity t : struck) {
                BowFx.flare(c, BowFx.mid(t), 0.6f);
                c.shove(t, impact, 0.8);
            }
            BowFx.flare(c, impact, 1.5f);
            BowFx.sound(c, impact, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4f, 0.7f);
            reward(c, struck);
        });
    }

    /** Seeker Arrow: a short-lived bolt that bends towards the foe nearest the crosshair. */
    private static void seek(HeroCtx c) {
        Vec3 eye = c.eye(), dir = c.look();
        LivingEntity prey = Foes.mostAligned(c, eye, dir, BowNumbers.SEEK_RANGE, BowNumbers.SEEK_CONE);
        Vec3 start = BowFx.muzzle(c, dir);
        Vec3[] last = {start};
        SpellRuntime.Bolt bolt = SpellRuntime.bolt(c.player, start, dir.scale(BowNumbers.SEEK_SPEED), BowNumbers.SEEK_RADIUS, BowNumbers.SEEK_LIFE, false, prey,
                (b, target) -> {
                    if (!Foes.valid(c, target)) return;
                    List<LivingEntity> struck = new ArrayList<>();
                    if (Foes.hit(c, target, (float) BowNumbers.SEEK_RAW)) struck.add(target);
                    BowFx.flare(c, BowFx.mid(target), 0.7f);
                    reward(c, struck);
                },
                (b, at) -> BowFx.flare(c, at, 0.5f));
        SpellRuntime.zone(c.level, BowNumbers.SEEK_LIFE, 2, age -> {
            Vec3 now = bolt.pos;
            if (now.distanceToSqr(last[0]) > 0.04) c.fx(VfxShape.COMPASS_NEEDLE, last[0], now, BowFx.light(c), 4, 0.6f);
            last[0] = now;
        });
    }

    /** Second Arrow: the plain arrow, and a second one a moment later at a share of its power, never more than the ordinary budget together. */
    private static void second(HeroCtx c, double f) {
        double first = plainRaw(c, f);
        plain(c, f, first);
        Tasks.later(c, BowNumbers.SECOND_DELAY, () -> {
            BowFx.swing(c);
            BowFx.sound(c, c.eye(), SoundEvents.ARROW_SHOOT, 1.0f, 1.5f);
            plain(c, f, BowNumbers.secondRaw(first));
        });
    }

    /** Pride's Command: the first monster hit turns on its own side, and the nearest ones turn on it. Never a boss, never a player. */
    private static void command(HeroCtx c) {
        line(c, BowNumbers.COMMAND_RANGE_SHOT, 5, 0.9f);
        LivingEntity t = first(c, BowNumbers.COMMAND_RANGE_SHOT, BowNumbers.BASIC_WIDTH);
        if (t == null) return;
        List<LivingEntity> struck = new ArrayList<>();
        if (Foes.hit(c, t, (float) BowNumbers.COMMAND_RAW)) struck.add(t);
        reward(c, struck);
        if (!(t instanceof Mob victim) || t instanceof Player || BalanceLaw.isBoss(t) || !t.isAlive()) return;
        List<Mob> near = new ArrayList<>(c.level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(BowNumbers.COMMAND_RANGE),
                m -> m != victim && m.isAlive() && Foes.valid(c, m) && !BalanceLaw.isBoss(m)));
        if (near.isEmpty()) return;                                          // no one to turn on it: the lock is not spent
        if (!c.control()) {
            c.say("Your command needs a moment to gather again.");
            return;
        }
        near.sort((a, b) -> Double.compare(a.distanceToSqr(victim), b.distanceToSqr(victim)));
        int turned = 0;
        for (Mob m : near) {
            if (turned == 0) victim.setTarget(m);
            m.setTarget(victim);
            c.fx(VfxShape.THREAD_LINE, BowFx.mid(m), BowFx.mid(victim), BowFx.PRIDE, 30, 0.7f);
            if (++turned >= BowNumbers.COMMAND_RECRUITS) break;
        }
        BowFx.flare(c, BowFx.mid(victim), 1.0f);
        BowFx.sound(c, victim.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.6f);
    }
}
