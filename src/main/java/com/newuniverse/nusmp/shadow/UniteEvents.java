package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the passive parts of the Devil Unite modes live in game events, not in a cast: Equus takes less physical damage and is not knocked back,
 * Felis dodges hits and is forgotten by mobs further than 8 blocks, and every mode ends cleanly when its wearer dies or leaves. They are kept
 * together here with one {@link #register} for the lead to call, and every rule they apply comes from {@link UniteKind} so the numbers stay in one table.
 */
public final class UniteEvents {
    private UniteEvents() {}

    /** Registers the handlers on the game bus ({@code NeoForge.EVENT_BUS}), including the watcher tick. */
    public static void register(IEventBus bus) {
        bus.addListener(UniteEvents::onIncomingDamage);
        bus.addListener(UniteEvents::onKnockBack);
        bus.addListener(UniteEvents::onChangeTarget);
        bus.addListener(UniteEvents::onDeath);
        bus.addListener(UniteEvents::onLogout);
        bus.addListener(UniteWatcher::onServerTick);
    }

    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        UniteKind kind = UniteState.active(p);
        if (kind == null) return;
        DamageSource src = e.getSource();
        boolean bypassInvuln = src.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (kind.dodge > 0 && !bypassInvuln) {
            boolean boss = src.getEntity() instanceof LivingEntity attacker && BalanceLaw.isBoss(attacker);
            if (kind.dodges(p.getRandom().nextDouble(), boss, ShadowConfig.dodgeVsBoss())) {
                e.setCanceled(true);
                var at = p.position().add(0, 0.2, 0);
                VfxSpawn.send(p.serverLevel(), VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), 0xFF3A3A55, 12, 0.6f);
                return;
            }
        }
        if (kind.physicalReduction > 0) {
            String path = src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
            boolean physical = DamageClass.isPhysical(path, src.is(DamageTypeTags.BYPASSES_ARMOR), bypassInvuln);
            e.setAmount((float) kind.afterReduction(e.getAmount(), physical));
        }
    }

    static void onKnockBack(LivingKnockBackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && UniteState.active(p) == UniteKind.EQUUS) e.setCanceled(true);
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !(e.getEntity() instanceof Mob mob)) return;
        UniteKind kind = UniteState.active(p);
        if (kind == null || BalanceLaw.isBoss(mob)) return;
        if (kind.mobLosesTarget(mob.distanceTo(p))) e.setNewAboutToBeSetTarget(null);
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) UniteState.stop(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ShadowSight.forget(e.getEntity().getUUID());
    }
}
