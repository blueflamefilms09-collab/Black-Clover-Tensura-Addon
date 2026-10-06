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
            BookPage.zone("calderos", "Calderos", FireBook::calderos));

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
        b.vfx(p, VfxShape.FLAME_TRAIL, start, start.add(dir.scale(28)), 14, 1.2f * s);
        SpellRuntime.bolt(p, start, dir.scale(2.2), 0.8 * s, 13, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 14f);
            t.igniteForSeconds(5);
            b.vfx(p, VfxShape.FLAME_EXPLOSION, t.position(), t.position(), 14, 0.4f);
        }, null);
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
                b.vfx(p, VfxShape.FX_FIRE_ERUPTION, pt, pt, 0, s);
            }
        });
        return true;
    }
}
