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

/** Wind Magic. Sylph spirit. */
public class WindBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("kamaitachi", "Crescent Kamaitachi", TensuraShots.shot(TensuraShots.Shot.WIND_BLADE, 9, 2.0f, 1.0f, 0)),
            BookPage.mid("gust_lane", "Gust Lane", WindBook::lane),
            BookPage.signature("spirit_storm", "Spirit Storm", WindBook::storm),
            // 0.31: wiki spells, appended
            BookPage.zone("towering_tornado", "Towering Tornado", WindBook::toweringTornado),
            BookPage.signature("slicing_wind_emperor", "Slicing Wind Emperor", WindBook::windEmperor),
            BookPage.signature("spirit_of_zephyr", "Spirit of Zephyr", WindBook::zephyr).withCooldown(1800));

    public WindBook() { super(MagicType.WIND, 0xFF8CFFC2); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.WIND_ELEMENTAL; }
    @Override public Element spiritElement() { return Element.WIND; }
    @Override public String spiritName() { return "Sylph"; }

    static boolean crescent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.WIND_SLASH, start, start.add(dir.scale(24)), 16, 1f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.6), 1.2 * s, 15, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 9f);
            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
            t.hurtMarked = true;
        }, null);
        return true;
    }

    /** A 10-block lane: enemies are blown down it, projectiles in it are shredded, allies speed up. */
    static boolean lane(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.position().add(0, 1, 0), dir = p.getViewVector(1f).multiply(1, 0.2, 1).normalize(), end = a.add(dir.scale(10 * size(i, p)));
        for (LivingEntity t : along(p, a, end, 1.5)) {
            b.hurt(i, p, t, mode, 4f);
            t.setDeltaMovement(dir.scale(1.5).add(0, 0.3, 0));
            t.hurtMarked = true;
        }
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(a, end).inflate(1.5))) if (pr.getOwner() != p) pr.discard();
        for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(a, end).inflate(1.5))) {
            if (ally == p || ally.isAlliedTo(p)) ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
        }
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WIND_GALE, a, end, 0, 24, 1f);
        return true;
    }

    /** A tornado around you for 5 s: enemies are lifted and spun. */
    static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5 * size(i, p);
        b.castCircle(p, 1.5f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_TORNADO, p, p.position().add(0, 1, 0), 0, 100, r);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            Vec3 c = p.position();
            for (LivingEntity t : around(p, c, r)) {
                Vec3 to = t.position().subtract(c);
                Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.4);
                t.setDeltaMovement(swirl.add(0, 0.35, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 4f);
            }
        });
        return true;
    }

    /** Towering Tornado: a huge tornado where you aim for 5 s: it drags foes in and up, shreds them and swallows spells. */
    static boolean toweringTornado(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 3.5f * size(i, p);
        Vec3 c = aim(p, 22);
        b.castCircle(p, 1.3f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WIND_TORNADO, c, c.add(0, 1, 0), 0, 100, r);
        SpellRuntime.zone(p.serverLevel(), 100, 4, age -> {
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r * 1.6, 0, r * 1.6).expandTowards(0, r * 2.8, 0),
                    e -> e != p && e.isAlive() && !e.isAlliedTo(p))) {
                Vec3 to = t.position().subtract(c);
                Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.35), pull = to.multiply(-0.08, 0, -0.08);
                t.setDeltaMovement(swirl.add(pull).add(0, t.getY() - c.y < r * 2 ? 0.3 : 0.02, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 4f);
            }
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r, 0, r).expandTowards(0, r * 2.8, 0)))
                if (pr.getOwner() != p) pr.discard();
        });
        return true;
    }

    /** Slicing Wind Emperor: a huge crescent of many wind blades sweeps 28 blocks ahead, cutting spells and everything else. */
    static boolean windEmperor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(28));
        b.castCircle(p, 1.5f);
        com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.WIND_EMPEROR, a, end, 0, 20, 1.8f * s);
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(a, end).inflate(3 * s))) if (pr.getOwner() != p) pr.discard();
        for (LivingEntity t : along(p, a, end, 3.0 * s)) {
            double f = Math.min(1, t.position().distanceTo(a) / 28);
            int delay = (int) Math.round(15 * (1 - Math.cbrt(1 - f)));
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                if (!t.isAlive()) return;
                b.hurt(i, p, t, mode, 18f);
                t.knockback(0.8, -dir.x, -dir.z);
            });
        }
        return true;
    }

    /** Spirit of Zephyr: the wind spirit wraps you for 30 s: very fast, light as air, enemy spells blown away before they touch you. */
    static boolean zephyr(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.5f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_ZEPHYR, p, p.position().add(0, 1, 0), 0, 600, 1f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1));
        SpellRuntime.zone(p.serverLevel(), 600, 4, age -> {
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                Vec3 away = pr.position().subtract(p.position()).normalize().scale(1.2);
                pr.setDeltaMovement(away);
                pr.hurtMarked = true;
            }
        });
        return true;
    }
}
