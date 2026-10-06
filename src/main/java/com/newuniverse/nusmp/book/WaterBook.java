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

/** Water Magic. Undine spirit. */
public class WaterBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("waterball", "Sea Dragon's Waterball", TensuraShots.shot(TensuraShots.Shot.WATER_BALL, 9, 1.4f, 1.2f, 0)),
            BookPage.signature("roar", "Sea Dragon's Roar", WaterBook::roar),
            BookPage.zone("cradle", "Sea Dragon's Cradle", WaterBook::cradle));

    public WaterBook() { super(MagicType.WATER, 0xFF4FA8FF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.WATER_ELEMENTAL; }
    @Override public Element spiritElement() { return Element.WATER; }
    @Override public String spiritName() { return "Undine"; }

    static boolean waterball(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        SpellRuntime.bolt(p, start, dir.scale(1.1), 0.7 * s, 28, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 9f);
            t.clearFire();
            t.knockback(1.2, -bolt.vel.x, -bolt.vel.z);
        }, (bolt, at) -> b.vfx(p, VfxShape.WATER_SPLASH, at, at, 18, 0.7f * s));
        return true;
    }

    /** A homing water dragon. Big knockback, splash. Signature (screen shake). */
    static boolean roar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = size(i, p);
        LivingEntity target = target(p, 32);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.WATER_RING, p.position(), p.position().add(0, 1, 0), 24, 1.2f);
        SpellRuntime.bolt(p, start, dir.scale(0.9), 1.4 * s, 70, false, target, (bolt, t) -> {}, (bolt, at) -> {
            for (LivingEntity t : around(p, at, 2.5 * s)) {
                b.hurt(i, p, t, mode, 20f);
                t.clearFire();
                Vec3 away = t.position().subtract(at).normalize();
                t.knockback(2.5, -away.x, -away.z);
            }
            b.vfx(p, VfxShape.FX_WATER_CRASH, at, at, 0, 1.4f * s);
        });
        return true;
    }

    /** A water sphere: you and nearby players regenerate, projectiles inside crawl. */
    static boolean cradle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 4 * size(i, p);
        b.castCircle(p, 1f);
        SpellRuntime.zone(p.serverLevel(), 160, 1, age -> {
            Vec3 c = p.position();
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r))) {
                if (pr.getOwner() != p) { pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6)); pr.hurtMarked = true; }
            }
            if (age % 20 == 0) {
                for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r))) {
                    if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, 2f);
                }
            }
            if (age % 40 == 0) b.vfx(p, VfxShape.WATER_RING, c, c.add(0, 1, 0), 40, r / 4.5f);
        });
        return true;
    }
}
