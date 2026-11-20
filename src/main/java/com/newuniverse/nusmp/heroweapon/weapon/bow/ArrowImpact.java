package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
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
 * Why: this is the magic of the arrows. Each {@link ArrowKind} is one small effect that happens when a hero arrow strikes a foe ({@link #onHit}) or lands
 * ({@link #impact}); the form-independent techniques (net, mark, burst, seeker, command, sniper) reuse the effects and numbers of the old hitscan arrows
 * ({@link BowNumbers}), now delivered by a real arrow. Only the mod's own effect and damage helpers are used: hard control goes through the shared lock and
 * the control law (a boss is only staggered), damage through the balance law, targets through {@link Foes}; nothing breaks, places or burns a block, and a
 * burst touches at most {@link #BURST_TARGETS} foes. The costs: one effect packet per hit at most (the effect system's own budgets apply), one entity query
 * per landing of a burst kind, and none for an arrow of a rain (a volley arrow never bursts).
 */
final class ArrowImpact {
    private ArrowImpact() {}

    /** The most foes one burst of fire, star or burst arrow touches. */
    static final int BURST_TARGETS = 6;
    /** Radius and damage share (of the arrow's raw damage) of the fire and star bursts. */
    static final double FIRE_RADIUS = 3.0, STAR_RADIUS = 2.5, AREA_SHARE = 0.6;
    /** Frost slows for this long (ticks) at this level; the bleed and the curse last this long. */
    static final int FROST_TICKS = 50, BLEED_TICKS = 60, MARK_TICKS = 100, CURSE_TICKS = 160;

    // ================================================================ on a foe
    static void onHit(HeroArrow a, HeroCtx c, LivingEntity t) {
        try {
            Vec3 mid = BowFx.mid(t);
            switch (a.kind()) {
                case LIGHT, SPLIT, PIERCE -> BowFx.flare(c, mid, 0.4f);
                case BIND -> bind(c, t);
                case FROST -> {
                    if (t.isAlive()) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROST_TICKS, 1));
                    c.fx(VfxShape.ICE_FX2, mid, mid, BowFx.light(c), 12, 0.9f);
                }
                case BLEED -> {
                    Fx.addEffect(t, "minecraft:poison", BLEED_TICKS, 0);
                    Fx.addEffect(t, "tensura:fragility", MARK_TICKS, 0);
                    BowFx.flare(c, mid, 0.4f);
                }
                case FIRE -> {
                    if (t.isAlive()) t.igniteForSeconds(2.0f);
                    BowFx.flare(c, mid, 0.5f);
                }
                case STAR -> c.fx(VfxShape.STAR_MAGIC, mid, mid, BowFx.light(c), 10, 0.6f);
                case MARK -> mark(c, t);
                case PRIDE -> {
                    Fx.addEffect(t, "tensura:curse", CURSE_TICKS, 0);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CURSE_TICKS / 2, 0));
                    BowFx.flare(c, mid, 0.6f);
                }
                case SNIPER -> {
                    BowFx.flare(c, mid, 0.8f);
                    c.shove(t, c.eye(), 0.3);
                }
                case SEEK -> BowFx.flare(c, mid, 0.7f);
                case COMMAND -> command(c, t);
                case BURST -> { }
            }
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow arrow effect", e);
        }
    }

    /** Bind Arrow: the foe is pinned (through the shared control lock and the law's limits on how long). */
    private static void bind(HeroCtx c, LivingEntity t) {
        if (!t.isAlive()) return;
        if (c.control()) {
            int ticks = BalanceLaw.controlTicks(t, BowNumbers.NET_ROOT);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
            c.fx(VfxShape.THREAD_WEB, t.position(), t.position(), BowFx.light(c), ticks + 10, 1.8f);
            BowFx.sound(c, t.position(), SoundEvents.CHAIN_PLACE, 1.0f, 1.4f);
        } else {
            c.say("The net finds no hold: the control lock is still on.");
        }
    }

    /** Marking Arrow: the foe glows through walls and is frail, so friends hurt it more. */
    private static void mark(HeroCtx c, LivingEntity t) {
        if (!t.isAlive()) return;
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, BowNumbers.MARK_TICKS, 0));
        Fx.addEffect(t, "tensura:fragility", BowNumbers.MARK_TICKS, 0);
        c.fx(VfxShape.COMPASS_LOCK, BowFx.mid(t), BowFx.mid(t), BowFx.main(c), 30, 1.2f);
        BowFx.sound(c, t.position(), SoundEvents.LODESTONE_COMPASS_LOCK, 1.0f, 1.0f);
    }

    /** Pride's Command: the monster hit turns on its own side, and the nearest ones turn on it. Never a boss, never a player. */
    private static void command(HeroCtx c, LivingEntity t) {
        if (!(t instanceof Mob victim) || t instanceof Player || BalanceLaw.isBoss(t) || !t.isAlive()) return;
        List<Mob> near = new ArrayList<>(c.level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(BowNumbers.COMMAND_RANGE),
                m -> m != victim && m.isAlive() && Foes.valid(c, m) && !BalanceLaw.isBoss(m)));
        if (near.isEmpty()) return;                                          // no one to turn on it: the lock is not spent
        if (!c.control()) {
            c.say("Your command needs a moment to gather again.");
            return;
        }
        near.sort((x, y) -> Double.compare(x.distanceToSqr(victim), y.distanceToSqr(victim)));
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

    // ================================================================ where it lands
    static void impact(HeroArrow a, HeroCtx c, Vec3 at) {
        try {
            switch (a.kind()) {
                case SPLIT -> {
                    if (!a.isChild()) split(a, c, at);
                }
                case FIRE -> {
                    if (!a.isVolley()) area(a, c, at, FIRE_RADIUS, VfxShape.FIRE_BURST, 1.2f, true);
                }
                case STAR -> {
                    if (!a.isVolley()) area(a, c, at, STAR_RADIUS, VfxShape.STAR_MAGIC, 1.3f, false);
                }
                case BURST -> burst(a, c, at);
                default -> { }
            }
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow arrow impact fx", e);
        }
    }

    /** Arrow Shower: at most {@link ArrowSplit#SPLIT_CHILDREN} small arrows leave the landing, each at a foe nearby when there is one, else spread round. */
    private static void split(HeroArrow a, HeroCtx c, Vec3 at) {
        List<LivingEntity> foes = Foes.around(c, at, 8.0, ArrowSplit.SPLIT_CHILDREN);
        Vec3 from = at.add(0.0, 0.6, 0.0);
        for (int i = 0; i < ArrowSplit.SPLIT_CHILDREN; i++) {
            Vec3 dir;
            if (i < foes.size()) {
                dir = BowFx.mid(foes.get(i)).subtract(from);
            } else {
                double ang = i * (Math.PI * 2.0 / ArrowSplit.SPLIT_CHILDREN) + a.tickCount * 0.5;
                dir = new Vec3(Math.cos(ang), 0.5, Math.sin(ang));
            }
            if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(0.0, 1.0, 0.0);
            HeroArrow k = BowArrows.fire(c, from, dir, 1.6f, 0.1f, ArrowSplit.child(a.raw()), ArrowKind.LIGHT, BowArrows.VOLLEY | BowArrows.CHILD);
            if (k != null) k.setLife(30);
        }
        BowFx.flare(c, at, 0.6f);
    }

    /** A fire or star burst where the arrow landed: the foes within the radius take a magic hit of a share of the arrow's damage (fire also sets them alight for a moment). */
    private static void area(HeroArrow a, HeroCtx c, Vec3 at, double radius, VfxShape shape, float size, boolean fire) {
        c.fx(shape, at, at, fire ? BowFx.main(c) : BowFx.light(c), 14, size);
        BowFx.sound(c, at, fire ? SoundEvents.FIRECHARGE_USE : SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, fire ? 0.9f : 1.6f);
        double dmg = ArrowSplit.magic(a.raw(), AREA_SHARE, HeroConfig.bowMagicShareScale());
        for (LivingEntity t : Foes.around(c, at, radius, BURST_TARGETS)) {
            magic(a, c, t, dmg);
            if (fire && t.isAlive()) t.igniteForSeconds(2.0f);
            c.shove(t, at, 0.4);
        }
    }

    /** Burst Arrow: a ring shows where, and half a second later it bursts (the old numbers). */
    private static void burst(HeroArrow a, HeroCtx c, Vec3 at) {
        Tells.ring(c.level, at, BowNumbers.BURST_RADIUS, BowFx.rgb(c));
        BowFx.flare(c, at, 0.6f);
        BowFx.sound(c, at, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
        Tasks.later(c, BowNumbers.BURST_FUSE, () -> {
            c.fx(VfxShape.FIRE_BURST, at, at, BowFx.main(c), 16, 1.6f);
            List<LivingEntity> struck = new ArrayList<>();
            for (LivingEntity t : Foes.around(c, at, BowNumbers.BURST_RADIUS, BowNumbers.BURST_TARGETS)) {
                if (magic(a, c, t, BowNumbers.BURST_RAW)) struck.add(t);
                BowFx.flare(c, BowFx.mid(t), 0.6f);
                c.shove(t, at, 0.8);
            }
            BowFx.flare(c, at, 1.5f);
            BowFx.sound(c, at, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4f, 0.7f);
            BowShots.reward(c, struck);
        });
    }

    /** One magic hit (indirect magic of the wielder, through the balance law) on a foe the wielder may strike; its own hurt immunity applies. */
    private static boolean magic(HeroArrow a, HeroCtx c, LivingEntity t, double raw) {
        if (raw <= 0.0 || !Foes.valid(c, t)) return false;
        float d = BalanceLaw.damage(c.player, t, (float) (raw * c.power), c.masteryShare());
        return t.hurt(a.damageSources().indirectMagic(a, c.player), d);
    }
}
