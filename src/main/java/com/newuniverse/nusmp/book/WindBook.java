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
            BookPage.signature("spirit_storm", "Spirit Storm", WindBook::storm));

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
        b.vfx(p, VfxShape.FX_WIND_GUST, p.position(), p.position().add(dir.scale(10 * size(i, p))), 0, 1f);
        return true;
    }

    /** A tornado around you for 5 s: enemies are lifted and spun. */
    static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5 * size(i, p);
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.WIND_RING, p.position(), p.position().add(0, 1, 0), 30, 1.4f);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            Vec3 c = p.position();
            for (LivingEntity t : around(p, c, r)) {
                Vec3 to = t.position().subtract(c);
                Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.4);
                t.setDeltaMovement(swirl.add(0, 0.35, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 4f);
            }
            if (age % 20 == 0 && age > 0) b.vfx(p, VfxShape.WIND_RING, c, c.add(0, 1, 0), 22, r / 4f);
        });
        return true;
    }
}
