package com.newuniverse.nusmp.relics.scaling;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.scaling.AuraScaling.Aura;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Applies the Aura to a player's melee hit with a relic weapon in the main hand. {@link LivingIncomingDamageEvent} (NeoForge 21.1, the successor of
 * LivingHurtEvent) is where the incoming amount can still be changed and armour reductions can still be modified. Only a plain melee attack of the
 * player counts (the damage type is {@code player_attack} and the direct attacker is the player): relic abilities already scale through the
 * balance law, and arrows are not "the weapon in the hand", so nothing is scaled twice.
 */
public final class AuraEvents {
    private AuraEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(AuraEvents::onHit);
        gameBus.addListener(AuraEvents::onLogout);
        gameBus.addListener(AuraEvents::onStopping);
    }

    static void onHit(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!src.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p) return;
        LivingEntity target = e.getEntity();
        if (target == p) return;
        ItemStack main = p.getMainHandItem();
        RelicDef def = Relics.defOf(main);
        if (def == null || !def.slot().isHand() || def.look() == Look.SHIELD || !Attune.works(main, def, p)) return;
        Aura a = BalanceLaw.isBoss(target) ? AuraCache.of(p).boss() : AuraCache.of(p).normal();
        if (a.none()) return;
        float original = e.getAmount();
        float limit = target instanceof Player ? BalanceLaw.cap(target, Float.MAX_VALUE) : 0f;       // a player target keeps the per-hit cap of the balance law
        int effOrdinal = com.newuniverse.nusmp.relics.tier.Tiers.effectiveGrade(main, def).ordinal();
        double gradeMax = com.newuniverse.nusmp.relics.GradeDamage.exemptId(def.id()) ? 0.0 : com.newuniverse.nusmp.relics.GradeDamage.range(effOrdinal)[1];
        float scaled = AuraScaling.apply(original, a.damageMult(), limit, gradeMax);
        if (scaled != original) e.setAmount(scaled);
        if (a.armourIgnore() > 0) {
            float keep = AuraScaling.armourKeep(a.armourIgnore());
            e.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { AuraCache.forget(e.getEntity().getUUID()); }

    private static void onStopping(ServerStoppingEvent e) { AuraCache.clear(); }
}
