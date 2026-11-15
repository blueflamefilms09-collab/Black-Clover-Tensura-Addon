package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

/**
 * Why: the game events the Black Sword listens to, each cheap for everybody else by construction: the player tick is one int compare for every player but one tick in twenty (then a 41-slot class test), a death looks at
 * its killer and victim only, a spawn is looked at only when its maximum health is boss tier (an int compare), a heal costs one NBT-free map read, the damage event returns at once unless the victim is blocking with the
 * shield form. Cleanup at logout, respawn and server stop drops every transient thing of the player. Server side only.
 */
public final class GreedEvents {
    private GreedEvents() {}

    static final String CROWNED = "anime_multiverse_crowned_greed";
    private static final Random RND = new Random();

    public static void register(IEventBus bus) {
        bus.addListener(GreedEvents::onStarted);
        bus.addListener(GreedEvents::onStopping);
        bus.addListener(GreedEvents::onPlayerTick);
        bus.addListener(GreedEvents::onServerTick);
        bus.addListener(EventPriority.HIGHEST, GreedEvents::onDeath);
        bus.addListener(EventPriority.HIGHEST, GreedEvents::onDrops);
        bus.addListener(EventPriority.HIGH, GreedEvents::onIncomingDamage);
        bus.addListener(GreedEvents::onHeal);
        bus.addListener(GreedEvents::onPickup);
        bus.addListener(GreedEvents::onSpawn);
        bus.addListener(GreedEvents::onLogin);
        bus.addListener(GreedEvents::onRespawn);
        bus.addListener(GreedEvents::onLogout);
    }

    static void onStarted(ServerStartedEvent e) { GreedData.bind(e.getServer()); }

    static void onStopping(ServerStoppingEvent e) {
        GreedService.clearAll();
        GreedData.bind(null);
    }

    /** Once a second per player (spread by id): the scan of what they carry and, for the wielder, the hunger clock. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || !GreedConfig.enabled()) return;
        SafeCast.tick("greed.scan", () -> {
            GreedService.scan(p);
            GreedService.hungerTick(p);
            if ((p.tickCount / 20) % 5 == 0) GreedGrantService.sync(p);
        });
    }

    /** Every 6000 ticks: the 14 day release. */
    static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 6000 == 0) SafeCast.tick("greed.inactivity", () -> GreedService.inactivityCheck(e.getServer()));
    }

    /** A kill: the wielder devours the victim; a wielder killed by another player loses the sword to the place where they fell. */
    static void onDeath(LivingDeathEvent e) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return;
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        if (e.getSource().getEntity() instanceof ServerPlayer killer) SafeCast.tick("greed.kill", () -> GreedService.onKill(v, killer));
        if (v instanceof ServerPlayer vp && GreedData.get().isOwner(vp.getUUID())) {
            if (e.getSource().getEntity() instanceof ServerPlayer other && other != vp) {
                SafeCast.tick("greed.release", () -> {
                    GreedService.release(vp.getServer(), vp.getUUID(), vp.serverLevel(), vp.position());
                    other.sendSystemMessage(Component.translatableWithFallback("greed.msg.fallen", "The Mortal Sin lies where its host fell.").withStyle(ChatFormatting.RED));
                });
            } else {
                GreedService.markReturn(vp.getUUID());
            }
        }
    }

    /** The sword is never among a player's death drops (a plain death returns it at the respawn; a PvP death released it); a Crowned bearer drops it. */
    static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !GreedData.bound()) return;
        if (v instanceof ServerPlayer) e.getDrops().removeIf(i -> i.getItem().getItem() instanceof GreedItem);
        if (!GreedConfig.enabled() || !v.getPersistentData().getBoolean(CROWNED) || !(v.level() instanceof ServerLevel level)) return;
        GreedData d = GreedData.get();
        if (d.spawned()) return;
        long token = d.mint(RND);
        ItemStack s = GreedItems.creativeStack();
        GreedComponents.write(s, null, token);
        ItemEntity ie = new ItemEntity(level, v.getX(), v.getY() + 0.5, v.getZ(), s);
        ie.setInvulnerable(true);
        ie.setUnlimitedLifetime();
        e.getDrops().add(ie);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatableWithFallback("greed.msg.crowned_fall", "A Crowned monster has fallen, releasing a dormant Mortal Sin...").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
    }

    /** The shield form: while blocking, the hit is absorbed (stored for Reflection Fortress), projectiles are destroyed and the attacker is hit back. */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isBlocking()) return;
        ItemStack use = p.getUseItem();
        if (!(use.getItem() instanceof GreedItem g) || g.form() != GreedRules.Form.SHIELD || !GreedService.mayUse(p, use)) return;
        Vec3 from = e.getSource().getSourcePosition();
        if (from != null && p.getLookAngle().dot(from.subtract(p.position()).normalize()) <= 0.0) return;      // from behind: the shield does not help
        e.setCanceled(true);
        if (e.getSource().getDirectEntity() instanceof Projectile pr) pr.discard();
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity a ? a : null;
        GreedForms.shieldTook(p, e.getAmount(), attacker, p.serverLevel().getGameTime(), GreedData.get().tier());
    }

    /** Soul Sever: a severed creature does not heal. */
    static void onHeal(LivingHealEvent e) { if (GreedForms.severed(e.getEntity())) e.setCanceled(true); }

    /** Only the wielder picks the sword up from the ground. */
    static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        if (!(s.getItem() instanceof GreedItem) || !GreedData.bound()) return;
        var owner = GreedData.get().owner();
        if (owner != null && !owner.equals(e.getPlayer().getUUID())) e.setCanPickup(TriState.FALSE);
    }

    /** A Crowned bearer: a boss-tier monster may carry the dormant sword while it is not in the world. */
    static void onSpawn(FinalizeSpawnEvent e) {
        if (!GreedConfig.enabled() || !GreedConfig.CROWNED.get() || !GreedData.bound()) return;
        Mob mob = e.getEntity();
        if (mob.getMaxHealth() < 100.0f && TensuraBridge.ep(mob) < 50_000.0) return;
        if (GreedData.get().spawned() || mob.getRandom().nextDouble() >= GreedConfig.CROWNED_CHANCE.get()) return;
        mob.getPersistentData().putBoolean(CROWNED, true);
        mob.setCustomName(Component.translatableWithFallback("greed.crowned", "Crowned %s", mob.getName()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(mob.getMaxHealth() * 1.5);
            mob.setHealth(mob.getMaxHealth());
        }
        mob.setGlowingTag(true);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !GreedData.bound()) return;
        SafeCast.tick("greed.login", () -> {
            GreedState st = GreedStore.peek(p);
            if (st != null && !GreedData.get().isOwner(p.getUUID())) st.reset();                  // the sword left them while they were away
            GreedGrantService.sync(p);
            GreedService.applyAttributes(p, GreedStore.of(p));
            GreedService.hud(p, true);
        });
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SafeCast.tick("greed.respawn", () -> GreedService.onRespawn(p));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { GreedService.forget(e.getEntity().getUUID()); }
}
