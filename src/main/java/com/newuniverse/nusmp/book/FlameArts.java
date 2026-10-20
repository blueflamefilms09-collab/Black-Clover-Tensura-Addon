package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.FlameProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Flame Magic, rebuilt after the Vermillion family (Mereoleona's Leo Rugiens and Calidos Brachium, Fuegoleon's Crimson Lion King and flame armour, the Spiral Flame, the Flame Armor).
 * The eight pages that existed keep their ids, order and cost tier and are re-implemented with their numbers kept (FireBook's old static methods and WikiSpells are untouched). The two first
 * pages stay Tensura shots (their projectile entities carry Tensura's own fire and block rules) and gain the armour; the rest use the new bodies: the lion and the phoenix are models
 * ({@link FlameProps}), every page draws the rebuilt Flame VFX ({@code FLAMEFX_*}), and every successful cast puts the Flame armour (the Crimson Lion set) on the caster for 5 s
 * ({@link WaterArmour}); Flame Armour keeps it for 30 s. Three pages are appended.
 * <pre>
 *   id                  what it does (numbers as before)
 *   exploding_fireball  (kept, a Tensura fire ball)
 *   sol_linea           (kept, a Tensura fire lance)
 *   calderos            five flame pillars in a cross at the aim point, 12 damage, a lift, 5 s of burning
 *   leo_rugiens         the Flame Lion (model) bounds from you to the aim point (up to 20 blocks): 18 damage on its run, 12 where it lands
 *   spiral_flame        a corkscrew of flame drilling 22 blocks, 13 damage, a burst at the end (6)
 *   wild_bursting_flame flame bursts out in every direction, three waves, 7 each and a push
 *   ignis_columna       a column of flame at the aim point for 2.5 s: 12 first, 3.5 a half second after, lifting what stands in it
 *   calidos_brachium    arms of flame smash everything in a 120 degree cone within 8 blocks: 20 damage, 8 s of burning
 *   crimson_lion_king   (new) Fuegoleon's lion at 2.4 times the size runs 24 blocks: 26 damage on the way, an explosion where it ends (16 in 5 blocks)
 *   flame_armour        (new) 30 s of the Flame armour: Fire Resistance and Strength, and every blow sets its target alight with 4 extra damage (FireBook.onDamageEntity)
 *   phoenix_flare       (new) a phoenix flies 20 blocks: 12 damage and burning on its line, allies near you are healed, a sunburst where it ends (8 in 4 blocks)
 * </pre>
 */
public final class FlameArts {
    private FlameArts() {}

    private static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    // ---------------------------------------------------------------- the pages that existed (re-implemented)
    static boolean calderos(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        Vec3[] pts = {c, c.add(2.5 * s, 0, 0), c.add(-2.5 * s, 0, 0), c.add(0, 0, 2.5 * s), c.add(0, 0, -2.5 * s)};
        b.castCircle(p, 1.1f);
        SpellRuntime.later(p.serverLevel(), 10, () -> {
            for (Vec3 pt : pts) {
                for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pt, pt).inflate(1.3, 0, 1.3).expandTowards(0, 4, 0),
                        e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
                    b.hurt(i, p, t, mode, 12f);
                    t.igniteForSeconds(5);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.6, 0));
                    t.hurtMarked = true;
                }
                b.vfx(p, VfxShape.FLAMEFX_ZONE, pt, pt.add(0, 1, 0), 34, 1.5f * s);
                b.vfx(p, VfxShape.FLAMEFX_BURST, pt, pt.add(0, 1, 0), 24, 0.7f * s);
            }
        });
        return true;
    }

    /** Leo Rugiens: the lion (a model) runs from you to the aim point; the hits are timed to when it reaches each foe. */
    static boolean leoRugiens(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 look = p.getViewVector(1f);
        Vec3 flat = look.multiply(1, 0, 1).normalize();
        Vec3 feet = p.position().add(flat.scale(1.5));
        Vec3 end = GrimoireBook.aim(p, 20);
        Vec3 start = p.position().add(0, 1.2, 0).add(flat.scale(1.5)), endC = end.add(0, 0.6, 0);
        double len = Math.max(1, start.distanceTo(endC));
        int charge = 22;
        b.castCircle(p, 1.4f);
        FlameProps.lion(p.serverLevel(), p, feet, end, charge, 12, 1.1f * s);
        b.vfx(p, VfxShape.FLAMEFX_CAST, start, endC, charge, 1.0f * s);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 1.3f);
        for (LivingEntity t : GrimoireBook.along(p, start, endC, 1.8 * s)) {
            double f = Math.min(1, t.position().add(0, t.getBbHeight() / 2, 0).distanceTo(start) / len);
            int delay = (int) Math.round(charge * (1 - Math.cbrt(1 - f)));
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                if (!t.isAlive()) return;
                b.hurt(i, p, t, mode, 18f);
                t.igniteForSeconds(6);
                t.knockback(1.2, -look.x, -look.z);
            });
        }
        SpellRuntime.later(p.serverLevel(), charge, () -> {
            for (LivingEntity t : GrimoireBook.around(p, end, 3 * s)) { b.hurt(i, p, t, mode, 12f); t.igniteForSeconds(4); }
            b.vfx(p, VfxShape.FLAMEFX_BURST, end, end, 26, 1.5f * s);
        });
        return true;
    }

    static boolean spiralFlame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.FLAMEFX_CAST, start, start.add(dir.scale(22)), 22, 1.2f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.7), 1.0 * s, 13, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 13f);
            t.igniteForSeconds(5);
            t.knockback(0.8, -dir.x, -dir.z);
        }, (bolt, at) -> {
            for (LivingEntity t : GrimoireBook.around(p, at, 2.5 * s)) { b.hurt(i, p, t, mode, 6f); t.igniteForSeconds(3); }
            b.vfx(p, VfxShape.FLAMEFX_BURST, at, at, 24, 1.1f * s);
        });
        return true;
    }

    static boolean wildBurst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5.5f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.FLAMEFX_ZONE, c, c.add(0, 1, 0), 34, r);
        for (int w = 0; w < 3; w++) {
            float reach = r * (0.55f + 0.225f * w);
            int wave = w;
            SpellRuntime.later(p.serverLevel(), w * 6, () -> {
                b.vfx(p, VfxShape.FLAMEFX_BURST, p.position(), p.position().add(0, 1, 0), 24, reach / 2.4f);
                for (LivingEntity t : GrimoireBook.around(p, p.position(), reach)) {
                    b.hurt(i, p, t, mode, 7f);
                    t.igniteForSeconds(4);
                    Vec3 away = t.position().subtract(p.position()).normalize();
                    t.knockback(0.9, -away.x, -away.z);
                }
            });
        }
        return true;
    }

    static boolean ignisColumna(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 2.2 * s;
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.FLAMEFX_ZONE, c, c.add(0, 1, 0), 50, (float) r * 1.2f);
        b.vfx(p, VfxShape.FLAMEFX_BURST, c, c.add(0, 1, 0), 26, 1.6f * s);
        SpellRuntime.zone(p.serverLevel(), 50, 5, age -> {
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 0, r).expandTowards(0, 10, 0),
                    e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
                b.hurt(i, p, t, mode, age == 0 ? 12f : 3.5f);
                t.igniteForSeconds(6);
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.5, 0, 0.5).add(0, 0.35, 0));
                t.hurtMarked = true;
            }
        });
        return true;
    }

    /** Calidos Brachium: two arms of flame, one each side, swing from your shoulders; everything in the cone is smashed. */
    static boolean calidosBrachium(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.4f);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(1.2 * s);
            b.vfx(p, VfxShape.FLAMEFX_CAST, eye.add(side), eye.add(side).add(look.scale(8)), 16, 1.5f);
        }
        SpellRuntime.later(p.serverLevel(), 6, () -> {
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
                if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5) continue;
                b.hurt(i, p, t, mode, 20f);
                t.igniteForSeconds(8);
                t.knockback(1.6, -look.x, -look.z);
                b.vfx(p, VfxShape.FLAMEFX_BURST, t.position().add(0, 1, 0), t.position(), 20, 0.9f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- appended
    /** Crimson Lion King: the lion at 2.4 times the size, 24 blocks: 26 damage on the way and an explosion where it ends. */
    static boolean crimsonLionKing(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 look = p.getViewVector(1f), flat = look.multiply(1, 0, 1).normalize();
        Vec3 feet = p.position().add(flat.scale(2.0));
        Vec3 end = feet.add(flat.scale(24));
        Vec3 start = feet.add(0, 1.6, 0), endC = end.add(0, 1.6, 0);
        int run = 26;
        b.castCircle(p, 1.8f);
        FlameProps.lion(p.serverLevel(), p, feet, end, run, 16, 2.4f * s);
        b.vfx(p, VfxShape.FLAMEFX_CAST, start, endC, run, 2.2f * s);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.8f);
        for (LivingEntity t : GrimoireBook.along(p, start, endC, 2.6 * s)) {
            double f = Math.min(1, t.position().distanceTo(feet) / 24.0);
            int delay = (int) Math.round(run * (1 - Math.cbrt(1 - f)));
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                if (!t.isAlive() || ally(p, t)) return;
                b.hurt(i, p, t, mode, 26f);
                t.igniteForSeconds(8);
                t.knockback(1.6, -flat.x, -flat.z);
            });
        }
        SpellRuntime.later(p.serverLevel(), run, () -> {
            for (LivingEntity t : GrimoireBook.around(p, end, 5 * s)) { if (!ally(p, t)) { b.hurt(i, p, t, mode, 16f); t.igniteForSeconds(6); } }
            b.vfx(p, VfxShape.FLAMEFX_BURST, end, end, 30, 2.6f * s);
            b.vfx(p, VfxShape.FLAMEFX_ZONE, end, end.add(0, 1, 0), 40, 4f * s);
        });
        return true;
    }

    /** Flame Armour: 30 s of the armour: Fire Resistance and Strength; while it is on every blow sets its target alight and adds 4 damage (see armourBlow). */
    static boolean flameArmour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.FLAMEFX_BURST, p.position(), p.position().add(0, 1, 0), 26, 1.5f);
        b.vfx(p, VfxShape.FLAMEFX_ZONE, p.position(), p.position().add(0, 1, 0), 40, 2.2f);
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
        p.clearFire();
        return true;
    }

    /** Called by FireBook when the caster strikes something: while the Flame armour is worn the target burns and takes 4 more (balance-law scaled). */
    static void armourBlow(GrimoireBook book, ManasSkillInstance i, ServerPlayer p, LivingEntity target, Changeable<Float> amount) {
        if (!WaterArmour.worn(p, Aura.FLAME) || target == p) return;
        target.igniteForSeconds(4);
        amount.set(amount.get() + BalanceLaw.damage(target, 4f, book.masteryFrac(i)));
    }

    /** Phoenix Flare: a phoenix flies 20 blocks: 12 damage and burning along its line; allies within 8 blocks of you are healed; a sunburst where it ends. */
    static boolean phoenixFlare(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        Vec3 end = start.add(dir.scale(22));
        b.castCircle(p, 1.4f);
        FlameProps.phoenix(p.serverLevel(), p, start.add(dir.scale(1.5)).add(0, -0.2, 0), dir, 1.1, 20, 1.2f * s);
        b.vfx(p, VfxShape.FLAMEFX_CAST, start, end, 20, 1.3f * s);
        for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8))) {
            if (ally != p && ally.isAlliedTo(p)) BalanceLaw.heal(ally, 4f);
        }
        SpellRuntime.bolt(p, start, dir.scale(1.1), 1.3 * s, 20, true, null, (bolt, t) -> {
            if (ally(p, t)) return;
            b.hurt(i, p, t, mode, 12f);
            t.igniteForSeconds(5);
        }, (bolt, at) -> {
            for (LivingEntity t : GrimoireBook.around(p, at, 4 * s)) { if (!ally(p, t)) { b.hurt(i, p, t, mode, 8f); t.igniteForSeconds(4); } }
            b.vfx(p, VfxShape.FLAMEFX_BURST, at, at, 28, 1.8f * s);
        });
        return true;
    }
}
