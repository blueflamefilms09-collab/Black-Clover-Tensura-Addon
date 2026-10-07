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

/** Ice Magic: stop, then shatter. */
public class IceBook extends GrimoireBook {
    private final List<BookPage> pages = com.newuniverse.nusmp.book.ext.Ext.join(List.of(
            BookPage.starter("frost_lance", "Frost Lance", TensuraShots.shot(TensuraShots.Shot.ICE_LANCE, 9, 2.0f, 0.4f, 0)),
            BookPage.signature("ice_prison", "Ice Prison", IceBook::prison),
            // 0.49: more spells for a thin magic (appended)
            BookPage.starter("frost_ball", "Frost Ball", TensuraShots.shot(TensuraShots.Shot.FROST_BALL, 8, 1.6f, 0.6f, 0)),
            BookPage.zone("frozen_ground", "Frozen Ground", ElementBook.field(4, 4.5, 80, 20, true, VfxShape.WATER_RING,
                    ElementBook.all(ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)), (t, p) -> t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40)))),
            BookPage.mid("blizzard_crown", "Blizzard Crown", ElementBook.nova(10, 6, false, VfxShape.WATER_BURST,
                    ElementBook.all(ElementBook.knock(1.0), (t, p) -> t.setTicksFrozen(t.getTicksRequiredToFreeze() + 80))))), com.newuniverse.nusmp.book.ext.IceExt.pages());

    public IceBook() { super(MagicType.ICE, 0xFF9FE6FF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.ICE_ELEMENTAL; }

    static boolean lance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.LIGHTNING_SPEAR, start, start.add(dir.scale(24)), 10, 0.7f);
        SpellRuntime.bolt(p, start, dir.scale(1.6), 0.5, 15, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 8f);
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + 60);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }, (bolt, at) -> b.vfx(p, VfxShape.WATER_SPLASH, at, at, 14, 0.5f));
        return true;
    }

    /** Encase one target in ice (hard control), then it shatters for damage. */
    static boolean prison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        if (t == null) { fail(p, "No target to encase."); return false; }
        if (!control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        TimeStop.freeze(t, ticks);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.WATER_RING, t.position(), t.position().add(0, 1, 0), ticks, 0.8f);
        b.vfx(p, VfxShape.MIRROR_PANE, t.getBoundingBox().getCenter(), p.getEyePosition(), ticks, 1f);
        SpellRuntime.later(p.serverLevel(), ticks, () -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, 12f);
            b.vfx(p, VfxShape.WATER_SPLASH, t.position(), t.position(), 18, 1.2f);
        });
        return true;
    }
}
