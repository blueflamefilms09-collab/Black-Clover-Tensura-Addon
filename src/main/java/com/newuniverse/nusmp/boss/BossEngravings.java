package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;

/**
 * The attack profile of the boss roster: Tensura's engravings Barrier Piercing and Soul Eater are enchantments of weapons, which a mob
 * does not carry, so the boss's blows carry their effect instead (documented in docs/boss_overhaul.md):
 * <ul>
 *   <li><b>Barrier Piercing</b>: a blow of a registry-listed boss shaves a few ticks off the Tensura barrier effects of the player it
 *       hits (never removes them, never touches an infinite one).</li>
 *   <li><b>Soul Eater</b>: the same blow takes a few points of the player's spiritual health (never below 1; Tensura regenerates it).</li>
 * </ul>
 * Both are throttled to once a second per player, only read the boss rows of {@link BossBuffTable}, and only ever act on players.
 */
public final class BossEngravings {
    private BossEngravings() {}

    private static final String LAST = "nusmp_boss_engrave_at";
    private static final String[] BARRIERS = {"tensura:magic_barrier", "tensura:physical_barrier", "tensura:multilayer_barrier"};

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || e.getAmount() <= 0f || victim.level().isClientSide) return;
        if (victim.isCreative() || victim.isSpectator()) return;                  // admins and spectators are never worked on
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        String id = BossEntities.bossId(attacker);
        if (id == null || ZogratisEntities.isFamily(victim)) return;
        BossBuffTable.Profile p = BossBuffTable.profile(id);
        if (p == null || BossBuffConfig.multiplier() <= 0 || !BossBuffConfig.globalBuffs()) return;
        long now = victim.level().getGameTime();
        CompoundTag data = victim.getPersistentData();
        if (now - data.getLong(LAST) < 20 && now >= data.getLong(LAST)) return;
        data.putLong(LAST, now);
        if (p.pierceTicks() > 0) pierce(victim, p.pierceTicks());
        if (p.soulPoints() > 0) EnergyBridge.spirit(victim, p.soulPoints());
    }

    /** Shortens the player's finite Tensura barrier effects by 'ticks' (not below 1 tick, infinite ones are left alone). */
    static void pierce(ServerPlayer victim, int ticks) {
        for (String bid : BARRIERS) {
            ResourceLocation rl = ResourceLocation.tryParse(bid);
            if (rl == null) continue;
            Optional<Holder.Reference<MobEffect>> h = BuiltInRegistries.MOB_EFFECT.getHolder(rl);
            if (h.isEmpty()) continue;
            MobEffectInstance cur = victim.getEffect(h.get());
            if (cur == null || cur.isInfiniteDuration() || cur.getDuration() <= ticks + 1) continue;
            victim.removeEffect(h.get());
            victim.addEffect(new MobEffectInstance(h.get(), cur.getDuration() - ticks, cur.getAmplifier(), cur.isAmbient(), cur.isVisible(), cur.showIcon()));
        }
    }
}
