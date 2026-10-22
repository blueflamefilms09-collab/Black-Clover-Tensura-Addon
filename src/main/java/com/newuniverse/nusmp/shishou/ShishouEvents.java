package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the game events Shishou listens to. Each one is cheap for everybody else by construction: a kill is looked at only when its killer is Shishou's body or a linked wielder (a null check and a set test), the
 * barrier, the invulnerability frames and Stealth are looked at only while one of those windows is open anywhere ({@link ShishouWindows#anyActive}: an int compare), a heal costs one effect lookup, the
 * attribute event returns at once for any other item. Cleanup at logout, death, respawn, dimension change and server stop drops every transient thing of the player. Server side only (the attribute event
 * also runs on the client, with the stack's own mirror of the level).
 */
public final class ShishouEvents {
    private ShishouEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(ShishouEvents::onStarted);
        bus.addListener(ShishouEvents::onStopping);
        bus.addListener(ShishouEvents::onDeath);
        bus.addListener(ShishouEvents::onDrops);
        bus.addListener(ShishouEvents::onIncomingDamage);
        bus.addListener(ShishouEvents::onProjectile);
        bus.addListener(ShishouEvents::onTarget);
        bus.addListener(ShishouEvents::onHeal);
        bus.addListener(ShishouEvents::onAttributes);
        bus.addListener(ShishouEvents::onLogout);
        bus.addListener(ShishouEvents::onRespawn);
        bus.addListener(ShishouEvents::onDimension);
        bus.addListener(ShishouEvents::onServerTick);
        bus.addListener(ShishouEvents::onLogin);
        bus.addListener(ShishouEvents::onPlayerTick);
    }

    /** The Tensura skill follows the sword: a player who logs in gets (or loses) the Ultimate skill Shishou by the record. */
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) SafeCast.tick("shishou.grant", () -> ShishouGrantService.sync(p)); }

    /** Every 100 ticks per player (spread by id): one owner compare and, only when the list is wrong, a grant or a revoke. */
    static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && ShishouGrant.due(p.tickCount, p.getId())) SafeCast.tick("shishou.grant", () -> ShishouGrantService.sync(p));
    }

    static void onStarted(ServerStartedEvent e) { ShishouData.bind(e.getServer()); }

    static void onStopping(ServerStoppingEvent e) {
        ShishouLive.clearAll();
        ShishouData.bind(null);
    }

    /** A kill: Skill Eater, only when the killer is Shishou's body or a linked wielder. */
    static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v instanceof ServerPlayer p) {
            ShishouLive.forget(p.getUUID());
            return;
        }
        Entity k = e.getSource().getEntity();
        if (k == null) return;
        if (k instanceof ShishouEntity || ShishouLive.anyLinked() && k instanceof ServerPlayer) SafeCast.tick("shishou.kill", () -> ShishouService.onKill(v, e.getSource()));
    }

    static void onDrops(LivingDropsEvent e) {
        if (e.getSource().getEntity() instanceof ShishouEntity) SafeCast.tick("shishou.drops", () -> ShishouService.onDrops(e));
    }

    /** The invulnerability frames of Spatial Motion. */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!ShishouLive.WINDOWS.anyActive(ShishouWindows.Kind.IFRAMES)) return;
        if (e.getEntity() instanceof ServerPlayer p && ShishouLive.WINDOWS.active(p.getUUID(), ShishouWindows.Kind.IFRAMES, p.level().getGameTime())) e.setCanceled(true);
    }

    /** Multilayer Barrier: an arrow, a fireball, a thrown trident that would hit the wielder is turned back at its shooter; at most three a second, each costs mana, and an empty pool breaks the barrier. */
    static void onProjectile(ProjectileImpactEvent e) {
        if (!ShishouLive.WINDOWS.anyActive(ShishouWindows.Kind.BARRIER)) return;
        if (!(e.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer p)) return;
        long now = p.level().getGameTime();
        if (!ShishouLive.WINDOWS.active(p.getUUID(), ShishouWindows.Kind.BARRIER, now) || !ShishouService.holding(p)) return;
        if (!ShishouLive.WINDOWS.deflectAllowed(p.getUUID(), now, ShishouRules.BARRIER_DEFLECT_PER_SECOND)) return;
        SafeCast.tick("shishou.barrier", () -> {
            ShishouData d = ShishouData.get();
            if (!d.record().spend(now, ShishouRules.BARRIER_DEFLECT_MANA)) {                    // no mana left: the barrier breaks and rests for thirty seconds
                ShishouSpells.stopBarrier(p);
                ShishouLive.COOLDOWNS.start(p.getUUID(), ShishouSkill.MULTILAYER_BARRIER, now, ShishouRules.BARRIER_COOLDOWN);
                ShishouSpeech.say(p, p.serverLevel(), ShishouVoiceLines.Group.BARRIER_OFF);
                return;
            }
            Projectile proj = e.getProjectile();
            Entity shooter = proj.getOwner();
            Vec3 v = proj.getDeltaMovement();
            double speed = Math.max(0.8, v.length());
            Vec3 dir = shooter != null && shooter != p ? shooter.getEyePosition().subtract(proj.position()).normalize() : v.scale(-1.0).normalize();
            e.setCanceled(true);
            proj.setDeltaMovement(dir.scale(speed));
            proj.setOwner(p);
            proj.hasImpulse = true;
            PerfParticles.send(p.serverLevel(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.CLOUD), proj.getX(), proj.getY(), proj.getZ(), 4, 0.2, 0.2, 0.2, 0.05);     // the small wind burst
            d.touch();
        });
    }

    /** Stealth: hostile mobs do not pick the veiled thrown sword, and do not newly pick a veiled wielder beyond four blocks. */
    static void onTarget(LivingChangeTargetEvent e) {
        if (!ShishouLive.WINDOWS.anyActive(ShishouWindows.Kind.STEALTH)) return;
        LivingEntity nt = e.getNewAboutToBeSetTarget();
        if (nt instanceof ShishouEntity se) {
            if (se.isVeiled()) e.setCanceled(true);
        } else if (nt instanceof ServerPlayer p && ShishouLive.WINDOWS.active(p.getUUID(), ShishouWindows.Kind.STEALTH, p.level().getGameTime()) && e.getEntity().distanceToSqr(p) > 16.0) e.setCanceled(true);
    }

    /** Black Flame blocks natural health regeneration of its victim. */
    static void onHeal(LivingHealEvent e) { if (ShishouEffects.burning(e.getEntity())) e.setCanceled(true); }

    private static final ResourceLocation LEVEL_ATTACK = ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_level_attack"), ARTS = ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_arts"),
            ARTS_SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_arts_speed"), SHARING = ResourceLocation.fromNamespaceAndPath("nusmp", "shishou_sharing");

    /** The stats the held sword gives: attack by level (capped), Sword Arts Mastery (a little speed and damage), Skill Sharing (a little walking speed); main hand only. Computed from the stack's own mirror. */
    static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() != ShishouItems.SHISHOU.get() || !ShishouConfig.enabled()) return;
        int level = ShishouComponents.level(s);
        e.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(LEVEL_ATTACK, ShishouRules.attackBonus(level, ShishouConfig.damagePercent()), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (ShishouConfig.skillOn(ShishouSkill.SWORD_ARTS_MASTERY) && ShishouSkill.SWORD_ARTS_MASTERY.available(level, 0L)) {
            e.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(ARTS_SPEED, ShishouRules.SPEED_ARTS_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            e.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(ARTS, ShishouRules.ARTS_DAMAGE_SHARE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        }
        if (ShishouConfig.skillOn(ShishouSkill.SKILL_SHARING))
            e.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(SHARING, ShishouRules.sharingSpeed(level / 5), AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) ShishouLive.forget(p.getUUID()); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) ShishouLive.forget(p.getUUID()); }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) ShishouLive.forget(p.getUUID()); }

    /** Once a second (and the clones every half second while any live): the server-wide upkeep; closes the windows of players who no longer hold him. */
    static void onServerTick(ServerTickEvent.Post e) {
        var server = e.getServer();
        if (!ShishouData.bound()) return;
        SafeCast.tick("shishou.tick", () -> {
            ShishouService.tickServer(server);
            if (server.getTickCount() % 20 == 0 && ShishouLive.WINDOWS.anyOpen()) {
                long now = server.overworld().getGameTime();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) if (!ShishouLive.isLinked(p.getUUID(), now)) ShishouLive.WINDOWS.forget(p.getUUID());
            }
        });
    }

    /** The item-handler capability of the stack: Stomach for hoppers and other mods, only for THE valid stack, only the usable rows, through the same acceptance rules. */
    public static void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.registerItem(Capabilities.ItemHandler.ITEM, (stack, ctx) -> {
            if (!ShishouData.bound() || !ShishouConfig.enabled() || !ShishouData.get().cap().validStack(ShishouComponents.token(stack))) return null;
            return new RangedWrapper(new InvWrapper(ShishouData.get().storage()), 0, ShishouStorage.usable());
        }, ShishouItems.SHISHOU.get());
    }
}
