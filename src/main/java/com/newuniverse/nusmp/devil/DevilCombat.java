package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: until 3.76.0 a devil's attribute was a label. Here it acts (the missing piece the 3.48.0 notes listed): while the host has let the devil in (20 % assimilation and up), the host's own
 * blows deal extra damage and carry the attribute's effect ({@link DevilRules#effect}: Gravity crushes, Anti-Magic strips protections, Soul feeds the host, and so on), at most once a second.
 * The same hook makes the Brand of Sacrifice real: a monster's blow on a branded host is a fifth harder. On the damage event only: no tick, no packet, no state beyond one game time in the
 * contract. Cost for a player with no contract: one attachment lookup per blow dealt or taken.
 */
public final class DevilCombat {
    private DevilCombat() {}

    public static void register(IEventBus bus) { bus.addListener(DevilCombat::onDamage); }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide || e.getAmount() <= 0f || !DevilConfig.enabled()) return;
        SafeCast.tick("devil.combat", () -> {
            LivingEntity target = e.getEntity();
            if (target instanceof ServerPlayer victim && e.getSource().getEntity() instanceof Enemy) {
                DevilContract vc = DevilStore.peek(victim);
                if (vc != null && vc.sacrifices.contains(Sacrifice.BRANDED)) e.setAmount(DevilRules.branded(e.getAmount()));
            }
            if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p || p == target || !DevilConfig.attributeEffects()) return;
            DevilContract c = DevilStore.peek(p);
            if (c == null || !c.hasContract()) return;
            long now = DevilContracts.time(p);
            if (!DevilRules.effectReady(c.lastEffect, now)) return;
            double power = DevilRules.effectPower(c.tier, c.assimilation, c.inUnion());
            DevilRules.Effect fx = DevilRules.effect(c.attribute);
            if (power <= 0 || (fx.bonus() <= 0 && fx.effectId().isEmpty() && !fx.strip() && fx.heal() <= 0)) return;
            c.lastEffect = now;
            float blow = e.getAmount(), bonus = DevilRules.effectBonus(blow, fx, power, DevilConfig.effectScale());
            if (target instanceof Player) bonus = (float) Math.min(bonus, target.getMaxHealth() * 0.15);       // no one-shots between players
            e.setAmount(blow + bonus);
            if (!fx.effectId().isEmpty()) {
                if (fx.effectId().equals("nusmp:burn")) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), Math.min(fx.ticks(), 200)));
                else com.newuniverse.nusmp.relics.Fx.addEffect(target, fx.effectId(), fx.amp() >= 2 ? BalanceLaw.controlTicks(target, fx.ticks()) : fx.ticks(), fx.amp());
            }
            if (fx.strip()) {
                target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                target.removeEffect(MobEffects.ABSORPTION);
                target.removeEffect(MobEffects.REGENERATION);
                target.removeEffect(MobEffects.FIRE_RESISTANCE);
            }
            if (fx.heal() > 0) com.newuniverse.nusmp.relics.Fx.heal(p, (float) Math.min(6.0, (blow + bonus) * fx.heal() * power));
            WorldDevil d = WorldDevil.byId(c.devil);
            Fx.sprayTinted(p.level(), FxThemes.SKILL, FxSlot.MOTE, d != null ? d.color() : 0xC81E2D, 1.1f, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.4, 0.3, 0.03);
        });
    }
}
