package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: the owner asked that no hit in the mod passes 10,000 (15,000 for the one strongest weapon) and that the Djinn weapons hit 4,000 to 8,000 by rank (docs/damage_scale_down.md). The last word is
 * said here, on the damage event, after every multiplier of the mod has been applied (the power raise, the aura, the Djinn riders, the balance law): LOWEST priority, only for hits that come from
 * the mod (a player's hit, a mod entity, a mod damage type). All the rules are {@link DamageScale}'s; this class only reads the facts. Cost: a handful of instanceof checks and two registry lookups per damage event,
 * nothing per tick, no packet, no state (the Extreme Magic flag is set and cleared around one {@code hurt} call on the server thread).
 */
public final class DamageScaleEvents {
    private DamageScaleEvents() {}

    public static void register(IEventBus gameBus) { gameBus.addListener(EventPriority.LOWEST, DamageScaleEvents::onIncoming); }

    static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide) return;
        float amount = e.getAmount();
        if (!(amount > 0f)) return;
        DamageScale.Limits l = DamageScaleConfig.limits();
        if (!l.enabled()) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity(), direct = src.getDirectEntity();
        boolean extreme = DamageScale.extremeActive();
        if (!extreme && !fromMod(src, attacker, direct)) return;
        DamageScale.Kind kind = DamageScale.Kind.GENERAL;
        int rank = 0, count = Rankings.djinnCount();
        double charge = 1.0;
        if (extreme) {
            kind = DamageScale.Kind.EXTREME;
            rank = DamageScale.extremeRank();
            count = DamageScale.extremeCount();
        } else if (attacker instanceof ServerPlayer p) {
            ItemStack main = p.getMainHandItem();
            boolean melee = direct == p && src.is(DamageTypes.PLAYER_ATTACK);
            if (isApex(main)) {
                kind = DamageScale.Kind.APEX;
            } else if (melee && main.getItem() instanceof DjinnWeaponItem w) {
                rank = Rankings.djinnRank(w.djinn().id());
                if (rank > 0) {
                    kind = DamageScale.Kind.DJINN_WEAPON;
                    charge = DamageScale.charge(p.getAttackStrengthScale(0.5f));
                }
            } else if (melee) {
                int bearer = bearerRank(p);
                if (bearer > 0) { kind = DamageScale.Kind.DJINN_BEARER; rank = bearer; }
                else if (isModItem(main)) kind = DamageScale.Kind.WEAPON;
            }
        }
        float out = (float) DamageScale.apply(l, kind, rank, count, amount, charge, e.getEntity() instanceof Player);
        if (out != amount && out > 0f) e.setAmount(out);
    }

    /** A hit "from the mod": a player's hit, a hit by (or through) an entity of the mod, or a damage type of the mod. */
    static boolean fromMod(DamageSource src, Entity attacker, Entity direct) {
        if (attacker instanceof Player || direct instanceof Player) return true;
        if (isModEntity(attacker) || isModEntity(direct)) return true;
        return src.typeHolder().unwrapKey().map(k -> DamageScaleConfig.NAMESPACE.equals(k.location().getNamespace())).orElse(false);
    }

    private static boolean isModEntity(Entity e) {
        if (e == null) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && DamageScaleConfig.NAMESPACE.equals(id.getNamespace());
    }

    private static boolean isModItem(ItemStack s) {
        if (s.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id != null && DamageScaleConfig.NAMESPACE.equals(id.getNamespace());
    }

    /** The configured apex weapon (default Elsdocia) is the main-hand item. */
    static boolean isApex(ItemStack s) {
        String apex = DamageScaleConfig.apexId();
        if (apex == null || apex.isBlank() || s.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id != null && id.toString().equals(apex.trim());
    }

    /** The rank (1 = strongest) of the Djinn the player has out in a Half Equip or beyond, else 0. */
    static int bearerRank(ServerPlayer p) {
        try {
            if (!p.hasData(com.newuniverse.nusmp.relics.djinn.DjinnAttachments.STATE)) return 0;
            DjinnState s = DjinnState.of(p);
            Djinn d = s.djinn();
            return d != null && s.phase() >= DjinnPhaseRules.HALF ? Rankings.djinnRank(d.id()) : 0;
        } catch (RuntimeException none) {
            return 0;
        }
    }
}
