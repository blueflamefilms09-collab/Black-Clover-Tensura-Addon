package com.newuniverse.nusmp.blackclover;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Real time stop. A frozen entity does not tick at all: no movement, AI, gravity or attacks.
 * Projectiles hang in the air and finish their path afterward. Frozen targets are NOT invulnerable:
 * taking more than 4 hearts breaks the freeze (balance law). Players are pinned in place.
 */
public final class TimeStop {
    private static final class Frozen {
        long until;
        final Vec3 pos;
        float stored;
        Frozen(long until, Vec3 pos) { this.until = until; this.pos = pos; }
    }

    private static final Map<Entity, Frozen> FROZEN = new IdentityHashMap<>();

    private TimeStop() {}

    /** Freezes (or extends) an entity's stopped time. */
    public static void freeze(Entity e, int ticks) {
        if (isImmune(e)) return;                                                   // 0.48: the Demon-Slayer's field
        long until = e.level().getGameTime() + ticks;
        Frozen f = FROZEN.get(e);
        if (f == null) FROZEN.put(e, new Frozen(until, e.position()));
        else f.until = Math.max(f.until, until);
    }

    public static boolean isFrozen(Entity e) { return FROZEN.containsKey(e); }

    static final String K_IMMUNE = "nusmp_timestop_immune_until";

    /**
     * 0.48: 'e' can't be caught in stopped time for the next 'ticks' (the Genesis Demon-Slayer's Conceptual Nullification Field
     * keeps its wielder and allies outside it); a freeze already on 'e' breaks at once.
     */
    public static void immune(Entity e, int ticks) {
        long until = e.level().getGameTime() + ticks;
        if (e.getPersistentData().getLong(K_IMMUNE) < until) e.getPersistentData().putLong(K_IMMUNE, until);
        FROZEN.remove(e);
    }

    public static boolean isImmune(Entity e) { return e.level().getGameTime() < e.getPersistentData().getLong(K_IMMUNE); }

    /** Frozen entities skip their whole tick. */
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide && FROZEN.containsKey(event.getEntity())) event.setCanceled(true);
    }

    /** Hits land normally; 4 hearts (8 HP) of damage shatter the freeze. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Frozen f = FROZEN.get(event.getEntity());
        if (f == null) return;
        f.stored += event.getAmount();
        if (f.stored > 8f) FROZEN.remove(event.getEntity());
    }

    public static void onAttack(AttackEntityEvent event) { if (FROZEN.containsKey(event.getEntity())) event.setCanceled(true); }

    public static void onUseItem(PlayerInteractEvent.RightClickItem event) { if (FROZEN.containsKey(event.getEntity())) event.setCanceled(true); }

    /** Pins frozen players and releases expired freezes. */
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<Entity, Frozen>> it = FROZEN.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Entity e = en.getKey();
            Frozen f = en.getValue();
            if (e.isRemoved()) { it.remove(); continue; }
            if (e instanceof ServerPlayer p) {
                if (p.position().distanceToSqr(f.pos) > 0.0025) p.teleportTo(f.pos.x, f.pos.y, f.pos.z);
                p.setDeltaMovement(Vec3.ZERO);
                p.fallDistance = 0;
            }
            if (e.level().getGameTime() >= f.until) it.remove();
        }
    }

    public static void clear() { FROZEN.clear(); }
}
