package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.MagicRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Static Chain (Aspectual magic of the lightning family, designed for this mod; the original addon's source was not available): release the key on the foe you look at (20 blocks) and a bolt
 * strikes it, then jumps to the nearest foe within 7 blocks of it, and on, up to four jumps (six when mastered), losing 15% of its force at every link. The struck foes are then tethered
 * to each other for three seconds: any that strays more than five blocks from the middle of the chain is yanked back and all of them are slowed. The tether is a hard control, so it obeys
 * the shared 12-second control lock of the balance law (without it the chain still strikes and slows). Never touches allies, pets, summons or you. Numbers: {@link MagicRules.StaticChain}.
 */
public class StaticChainMagic extends EliteAspectual {
    static final SkillText TEXT = SkillText.of("static_chain", "Static Chain",
            "Aspectual magic (lightning). A bolt strikes the foe you look at and jumps from foe to foe, then tethers them together so they cannot scatter.");

    private static final int COLOR = 0xFFFFF29A;

    public StaticChainMagic() { super(AspectualType.LIGHTNING, TEXT); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("static_chain.cast", "Static Chain", MagicRules.StaticChain.COST, MagicRules.StaticChain.COOLDOWN));
    }

    @Override protected int color() { return COLOR; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks) {
        LivingEntity first = EliteBridge.lookFoe(player, MagicRules.StaticChain.CAST_RANGE);
        if (first == null) {
            EliteBridge.fail(player, SkillMsg.t("static_chain.no_target", "No foe in your sights."));
            return false;
        }
        boolean mastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        // candidates: the first victim, then the foes around it (capped), as positions for the pure chain order
        List<LivingEntity> pool = new ArrayList<>();
        pool.add(first);
        for (LivingEntity e : EliteBridge.foes(player, first.position(), MagicRules.StaticChain.JUMP_RANGE * (MagicRules.StaticChain.maxJumps(mastered) + 1))) if (e != first) pool.add(e);
        double[][] pos = new double[pool.size()][];
        for (int i = 0; i < pos.length; i++) {
            Vec3 c = pool.get(i).getBoundingBox().getCenter();
            pos[i] = new double[]{c.x, c.y, c.z};
        }
        int[] order = MagicRules.StaticChain.order(pos, 0, MagicRules.StaticChain.maxJumps(mastered), MagicRules.StaticChain.JUMP_RANGE);

        double mastery = masteryFrac(instance);
        Vec3 from = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.5)).add(0, -0.2, 0);
        List<UUID> chained = new ArrayList<>();
        for (int link = 0; link < order.length; link++) {
            LivingEntity t = pool.get(order[link]);
            Vec3 to = t.getBoundingBox().getCenter();
            EliteBridge.hit(player, t, MagicRules.StaticChain.linkDamage(link, mastered) * EliteSkillsConfig.damageScale(), mastery, true);
            EliteBridge.shape(level, link == 0 ? VfxShape.LIGHTNING_SPEAR : VfxShape.FX_LIGHTNING_ARC, from, to, COLOR, 10, 0.8f);
            EliteBridge.beam(level, from, to, false);
            EliteBridge.spray(level, false, to, 5, 0.3, 0.4, 0.3, 0.05);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MagicRules.StaticChain.bindTicks(mastered), 1, true, false));
            chained.add(t.getUUID());
            from = to;
        }
        EliteBridge.sound(level, first.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
        EliteBridge.actionbar(player, SkillMsg.t("static_chain.struck", "Static Chain: %s foes linked.", chained.size()).withStyle(ChatFormatting.YELLOW));
        if (chained.size() > 1 && BalanceLaw.beginControl(player)) tether(player, chained, MagicRules.StaticChain.bindTicks(mastered));
        return true;
    }

    /** The tether: every half second the chain's centre is found again and any victim too far from it is dragged back. */
    private static void tether(ServerPlayer owner, List<UUID> chained, int ticks) {
        int pulses = Math.max(1, ticks / 5);
        EliteBridge.startZone(owner, 5, pulses, (o, level, index) -> {
            List<LivingEntity> alive = new ArrayList<>();
            Vec3 sum = Vec3.ZERO;
            for (UUID id : chained) {
                Entity e = level.getEntity(id);
                if (e instanceof LivingEntity le && le.isAlive()) {
                    alive.add(le);
                    sum = sum.add(le.getBoundingBox().getCenter());
                }
            }
            if (alive.size() < 2) return;
            Vec3 centre = sum.scale(1.0 / alive.size());
            for (LivingEntity le : alive) {
                Vec3 c = le.getBoundingBox().getCenter();
                if (!MagicRules.StaticChain.yank(c.distanceTo(centre))) continue;
                Vec3 back = centre.subtract(c).normalize().scale(0.5);
                le.setDeltaMovement(le.getDeltaMovement().scale(0.5).add(back));
                le.hurtMarked = true;
                EliteBridge.shape(level, VfxShape.FX_LIGHTNING_ARC, c, centre, COLOR, 6, 0.5f);
            }
        });
    }
}
