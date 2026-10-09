package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.book.SpellRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Temporary, arena-bounded Marquis effects. */
public final class MarquisStatus {
    private static final String COMPRESSED_TAG = "nusmpMarquisCompressed";
    private static final String SLOW_CASTS = "nusmpMarquisSlowCasts";
    private static final String SLOW_CASTS_UNTIL = "nusmpMarquisSlowCastsUntil";
    private static final Map<UUID, Long> EVIL_EYE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> HEAL_LOCK = new ConcurrentHashMap<>();
    private static final Map<UUID, Marble> COMPRESSED = new ConcurrentHashMap<>();
    private static final ResourceLocation HEAVY_WEAPON = ResourceLocation.fromNamespaceAndPath("nusmp", "marquis_new_order");
    private static final ResourceLocation GEARSHIFT = ResourceLocation.fromNamespaceAndPath("nusmp", "marquis_gearshift");

    private MarquisStatus() {}

    public static void markEvilEye(LivingEntity target, int duration) {
        EVIL_EYE.put(target.getUUID(), target.level().getGameTime() + duration);
    }

    public static boolean evilEyeActive(LivingEntity target) {
        Long until = EVIL_EYE.get(target.getUUID());
        if (until == null) return false;
        if (target.level().getGameTime() >= until) {
            EVIL_EYE.remove(target.getUUID(), until);
            return false;
        }
        return true;
    }

    public static void suppressHealingInArena(Iterable<? extends LivingEntity> entities, int ticks) {
        for (LivingEntity entity : entities) HEAL_LOCK.put(entity.getUUID(), entity.level().getGameTime() + ticks);
    }

    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        Long eyeUntil = EVIL_EYE.get(entity.getUUID());
        Long lockUntil = HEAL_LOCK.get(entity.getUUID());
        long now = entity.level().getGameTime();
        if (eyeUntil != null && now >= eyeUntil) EVIL_EYE.remove(entity.getUUID(), eyeUntil);
        if (lockUntil != null && now >= lockUntil) HEAL_LOCK.remove(entity.getUUID(), lockUntil);
        if (eyeUntil != null && now < eyeUntil || lockUntil != null && now < lockUntil) event.setCanceled(true);
    }

    public static void applyHeavyWeaponOrder(LivingEntity target, int ticks) {
        if (target.getAttribute(Attributes.ATTACK_SPEED) == null) return;
        AttributeInstance attackSpeed = target.getAttribute(Attributes.ATTACK_SPEED);
        attackSpeed.removeModifier(HEAVY_WEAPON);
        attackSpeed.addTransientModifier(new AttributeModifier(HEAVY_WEAPON, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (target.level() instanceof ServerLevel sl) SpellRuntime.later(sl, ticks, () -> attackSpeed.removeModifier(HEAVY_WEAPON));
    }

    public static void applyGearshift(LivingEntity target, boolean top, int ticks) {
        AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(GEARSHIFT);
        speed.addTransientModifier(new AttributeModifier(GEARSHIFT, top ? 1.0 : -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (target.level() instanceof ServerLevel sl) SpellRuntime.later(sl, ticks, () -> speed.removeModifier(GEARSHIFT));
    }

    public static void slowNextGrimoireCasts(Player target, int casts, int ticks) {
        target.getPersistentData().putInt(SLOW_CASTS, Math.max(0, casts));
        target.getPersistentData().putLong(SLOW_CASTS_UNTIL, target.level().getGameTime() + ticks);
    }

    public static boolean consumeSlowedGrimoireCast(ServerPlayer player) {
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (now >= data.getLong(SLOW_CASTS_UNTIL)) {
            data.remove(SLOW_CASTS);
            data.remove(SLOW_CASTS_UNTIL);
            return false;
        }
        int left = data.getInt(SLOW_CASTS);
        if (left <= 0) return false;
        data.putInt(SLOW_CASTS, left - 1);
        if (left == 1) {
            data.remove(SLOW_CASTS);
            data.remove(SLOW_CASTS_UNTIL);
        }
        return true;
    }

    public static boolean compress(ServerPlayer player, Vec3 anchor, int ticks) {
        if (player.isCreative() || player.isSpectator() || COMPRESSED.containsKey(player.getUUID())) return false;
        player.getPersistentData().putBoolean(COMPRESSED_TAG, true);
        player.getPersistentData().putBoolean("nusmpMarquisWasInvisible", player.isInvisible());
        player.getPersistentData().putBoolean("nusmpMarquisWasNoPhysics", player.noPhysics);
        COMPRESSED.put(player.getUUID(), new Marble(anchor, player.level().getGameTime() + ticks));
        player.noPhysics = true;
        player.setInvisible(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        return true;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Marble marble = COMPRESSED.get(player.getUUID());
        if (marble == null) return;
        if (serverPlayer.level().getGameTime() >= marble.until) {
            release(serverPlayer, marble);
            return;
        }
        serverPlayer.setDeltaMovement(Vec3.ZERO);
        serverPlayer.fallDistance = 0;
        if (serverPlayer.position().distanceToSqr(marble.anchor) > 0.01)
            serverPlayer.teleportTo(marble.anchor.x, marble.anchor.y, marble.anchor.z);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (COMPRESSED.containsKey(entity.getUUID())) {
            event.setCanceled(true);
            return;
        }
        long until = entity.level().getGameTime();
        if (entity.getHealth() <= event.getAmount()) {
            EVIL_EYE.remove(entity.getUUID());
            HEAL_LOCK.remove(entity.getUUID());
        } else {
            EVIL_EYE.computeIfPresent(entity.getUUID(), (id, expires) -> expires <= until ? null : expires);
            HEAL_LOCK.computeIfPresent(entity.getUUID(), (id, expires) -> expires <= until ? null : expires);
        }
    }

    public static void onAttack(AttackEntityEvent event) {
        if (COMPRESSED.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }

    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (COMPRESSED.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }

    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (COMPRESSED.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        Marble marble = COMPRESSED.remove(player.getUUID());
        if (marble == null) return;
        player.teleportTo(marble.anchor.x, marble.anchor.y, marble.anchor.z);
        restorePlayer(player);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean(COMPRESSED_TAG)) restorePlayer(player);
    }

    private static void release(ServerPlayer player, Marble marble) {
        COMPRESSED.remove(player.getUUID(), marble);
        player.teleportTo(marble.anchor.x, marble.anchor.y, marble.anchor.z);
        restorePlayer(player);
    }

    private static void restorePlayer(Player player) {
        player.noPhysics = player.getPersistentData().getBoolean("nusmpMarquisWasNoPhysics");
        player.setInvisible(player.getPersistentData().getBoolean("nusmpMarquisWasInvisible"));
        player.getPersistentData().remove(COMPRESSED_TAG);
        player.getPersistentData().remove("nusmpMarquisWasNoPhysics");
        player.getPersistentData().remove("nusmpMarquisWasInvisible");
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }

    private record Marble(Vec3 anchor, long until) {}
}
