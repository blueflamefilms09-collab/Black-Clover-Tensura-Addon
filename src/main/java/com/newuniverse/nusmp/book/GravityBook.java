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

/** Gravity Magic (Dante). Singularity is a forbidden ritual, not a page. */
public class GravityBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("heavy_infighting", "Heavy Infighting", GravityBook::infighting),
            BookPage.signature("demon_king", "Presence of the Demon King", GravityBook::presence),
            BookPage.mid("gravity_sphere", "Gravity Sphere", TensuraShots.shot(TensuraShots.Shot.GRAVITY_SPHERE, 11, 1.2f, 0.2f, 0)));

    public GravityBook() { super(MagicType.GRAVITY, 0xFF7A3CC8); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.GRAVITY_ELEMENTAL; }

    /** Slam one target into the ground, then a short air lock (shares the control lock). */
    static boolean infighting(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 6);
        if (t == null) { fail(p, "Too far to slam."); return false; }
        t.setDeltaMovement(0, -2.0, 0);
        t.hurtMarked = true;
        b.hurt(i, p, t, mode, 10f);
        if (BalanceLaw.beginControl(p)) TimeStop.freeze(t, BalanceLaw.controlTicks(t, 20));
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.EARTH_SPIKES, t.position(), t.position().add(p.getViewVector(1f)), 14, 0.6f);
        return true;
    }

    /** An 8-block domain for 6 s: enemies are dragged down and slowed. You are slowed too. */
    static boolean presence(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position(), p.position().add(0, 1, 0), 30, 2.0f);
        SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
            Vec3 c = p.position();
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0));
            for (LivingEntity t : around(p, c, 8)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                t.setDeltaMovement(t.getDeltaMovement().add(0, -0.5, 0));
                t.hurtMarked = true;
            }
            if (age % 40 == 0) b.vfx(p, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), 40, 8 / 1.6f);
        });
        return true;
    }
}
