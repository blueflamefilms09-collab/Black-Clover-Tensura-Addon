package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Fire Magic. Salamander spirit. */
public class FireBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("exploding_fireball", "Exploding Fireball", TensuraShots.shot(TensuraShots.Shot.FIRE_BALL, 10, 1.6f, 0.6f, 80)),
            BookPage.mid("sol_linea", "Sol Linea", TensuraShots.shot(TensuraShots.Shot.FIRE_LANCE, 14, 2.4f, 0.8f, 100)),
            BookPage.zone("calderos", "Calderos", FireBook::calderos),
            // appended last so the existing pages keep their mode numbers and unlock bits
            BookPage.signature("leo_rugiens", "Leo Rugiens", FireBook::leoRugiens).withCooldown(600),
            // 0.31: wiki spells, appended
            BookPage.mid("spiral_flame", "Spiral Flame", FireBook::spiralFlame),
            BookPage.zone("wild_bursting_flame", "Wild Bursting Flame", FireBook::wildBurst),
            BookPage.signature("ignis_columna", "Ignis Columna", FireBook::ignisColumna).withCooldown(600));

    public FireBook() { super(MagicType.FLAME, 0xFFFF6A1E); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.FIRE_ELEMENTAL; }
    @Override public Element spiritElement() { return Element.FLAME; }
    @Override public String spiritName() { return "Salamander"; }

    static boolean fireball(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.FLAME_TRAIL, start, start.add(dir.scale(30)), 25, 0.8f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.2), 0.6, 25, false, null, (bolt, t) -> {}, (bolt, at) -> {
            for (LivingEntity t : around(p, at, 3 * s)) { b.hurt(i, p, t, mode, 10f); t.igniteForSeconds(4); }
            if (com.newuniverse.nusmp.NUConfig.GRIEF.get()) p.serverLevel().explode(p, at.x, at.y, at.z, 1.5f, false, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
            b.vfx(p, VfxShape.FLAME_EXPLOSION, at, at, 22, 0.8f * s);
            b.impact(p, at, 0.6f);
        });
        return true;
    }

    /** A spiraling spear of flame that pierces everything in its line. */
    static boolean solLinea(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.9f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_SPEAR, start, start.add(dir.scale(28)), 0, 16, s);
        SpellRuntime.bolt(p, start, dir.scale(2.2), 0.8 * s, 13, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 14f);
            t.igniteForSeconds(5);
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_BURST, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), 0, 16, 0.45f);
        }, null);
        return true;
    }

    /**
     * Leo Rugiens: a lion of flame bounds from you to where you aim (up to 20 blocks), burning everything it runs through, and roars
     * out in a blast where it lands.
     */
    static boolean leoRugiens(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 look = p.getViewVector(1f);
        Vec3 start = p.position().add(0, 1.2, 0).add(look.multiply(1, 0, 1).normalize().scale(1.5));
        Vec3 end = aim(p, 20).add(0, 0.6, 0);
        double len = Math.max(1, start.distanceTo(end));
        int duration = 34, charge = (int) (duration * 0.65f);      // matches FireSpellLayer: the lion arrives at 65% of the effect
        b.castCircle(p, 1.4f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_LION, start, end, 0, duration, 1.2f * s);
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.3f);
        for (LivingEntity t : along(p, start, end, 1.8 * s)) {
            double f = Math.min(1, t.position().add(0, t.getBbHeight() / 2, 0).distanceTo(start) / len);
            int delay = (int) Math.round(charge * (1 - Math.cbrt(1 - f)));   // when the lion (ease-out run) reaches it
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                if (!t.isAlive()) return;
                b.hurt(i, p, t, mode, 18f);
                t.igniteForSeconds(6);
                t.knockback(1.2, -look.x, -look.z);
            });
        }
        SpellRuntime.later(p.serverLevel(), charge, () -> {
            for (LivingEntity t : around(p, end, 3 * s)) { b.hurt(i, p, t, mode, 12f); t.igniteForSeconds(4); }
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_BURST, end, end, 0, 22, 1.3f * s);
        });
        return true;
    }

    /** Five flame pillars erupt in a cross at the target point. */
    static boolean calderos(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 c = aim(p, 24);
        Vec3[] pts = {c, c.add(2.5 * s, 0, 0), c.add(-2.5 * s, 0, 0), c.add(0, 0, 2.5 * s), c.add(0, 0, -2.5 * s)};
        SpellRuntime.later(p.serverLevel(), 10, () -> {
            for (Vec3 pt : pts) {
                for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pt, pt).inflate(1.3, 0, 1.3).expandTowards(0, 4, 0),
                        e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
                    b.hurt(i, p, t, mode, 12f);
                    t.igniteForSeconds(5);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.6, 0));
                    t.hurtMarked = true;
                }
                com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_PILLAR, pt, pt.add(0, 1, 0), 0, 30, s);
            }
        });
        return true;
    }

    /** Spiral Flame: a vortex of flame drills forward, piercing and burning everything in its path, and bursts at the end. */
    static boolean spiralFlame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_SPIRAL, start, start.add(dir.scale(22)), 0, 22, 1.1f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.7), 1.0 * s, 13, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 13f);
            t.igniteForSeconds(5);
            t.knockback(0.8, -dir.x, -dir.z);
        }, (bolt, at) -> {
            for (LivingEntity t : around(p, at, 2.5 * s)) { b.hurt(i, p, t, mode, 6f); t.igniteForSeconds(3); }
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_BURST, at, at, 0, 20, 0.9f * s);
        });
        return true;
    }

    /** Wild Bursting Flame: flame bursts out of you in every direction, three waves, each throwing foes back. */
    static boolean wildBurst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5.5f * size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_WILD, c, c.add(0, 1, 0), 0, 30, r);
        for (int w = 0; w < 3; w++) {
            float reach = r * (0.55f + 0.225f * w);
            SpellRuntime.later(p.serverLevel(), w * 6, () -> {
                for (LivingEntity t : around(p, p.position(), reach)) {
                    b.hurt(i, p, t, mode, 7f);
                    t.igniteForSeconds(4);
                    Vec3 away = t.position().subtract(p.position()).normalize();
                    t.knockback(0.9, -away.x, -away.z);
                }
            });
        }
        return true;
    }

    /** Ignis Columna: a towering column of flame erupts where you aim and keeps burning for 2.5 s, lifting what stands in it. */
    static boolean ignisColumna(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 c = aim(p, 24);
        double r = 2.2 * s;
        b.castCircle(p, 1.5f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.FIRE_PILLAR, c, c.add(0, 1, 0), 0, 50, 2.6f * s);
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
}
