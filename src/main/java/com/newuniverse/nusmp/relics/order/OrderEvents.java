package com.newuniverse.nusmp.relics.order;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.order.LedgerRules.Verdict;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * The game-bus handlers of the Order. Call {@link #register} once from the mod constructor with {@code NeoForge.EVENT_BUS}.
 * <ul>
 *   <li><b>Players</b>: every player's belongings are looked at every 100 ticks (staggered by entity id), at most sixteen stacks a tick
 *       ({@link RelicScanner}); a container that is opened or closed is looked at the same way, which catches chests, shulker boxes, ender chests, backpack
 *       mods that use menus and Lootr's per-player chests.</li>
 *   <li><b>Item entities</b> that join a level are looked at at once; a valid copy whose item entity is destroyed is marked lost.</li>
 *   <li><b>Mobs and armour stands</b> that equip a relic are looked at at once.</li>
 *   <li><b>The server</b>: the director's tick; the ledger and the God are let go when the server stops.</li>
 * </ul>
 */
public final class OrderEvents {
    private OrderEvents() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static boolean registered;

    public static void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(OrderEvents::onServerStarted);
        bus.addListener(OrderEvents::onServerStopping);
        bus.addListener(OrderEvents::onServerTick);
        bus.addListener(OrderEvents::onPlayerTick);
        bus.addListener(OrderEvents::onContainerOpen);
        bus.addListener(OrderEvents::onContainerClose);
        bus.addListener(EventPriority.LOWEST, OrderEvents::onEntityJoin);        // after anybody who may cancel it: only a join that happened is looked at
        bus.addListener(OrderEvents::onEntityLeave);
        bus.addListener(OrderEvents::onEquipmentChange);
        bus.addListener(OrderEvents::onLogin);
        bus.addListener(OrderEvents::onLogout);
    }

    // ================================================================ the server
    static void onServerStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        UniqueLedger.bind(server);
        UniqueRules.bind(server);                                                          // 1.68.0: the God of Order's list of one-world-only relics
        UniqueLedger l = UniqueLedger.get(server);
        LOG.info("[nusmp] The Order's ledger: {} relics written ({} alive), mode {}", l.entries().size(), l.liveCount(), l.mode(server.overworld().getGameTime()));
    }

    static void onServerStopping(ServerStoppingEvent e) {
        OrderDirector.clear();
        RelicScanner.clear();
        UniqueLedger.unbind();
        UniqueRules.unbind();
    }

    static void onServerTick(ServerTickEvent.Post e) { OrderDirector.onServerTick(e.getServer()); }

    // ================================================================ players
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        RelicScanner.onPlayerTick(server, p, server.overworld().getGameTime());
    }

    static void onContainerOpen(PlayerContainerEvent.Open e) {
        if (e.getEntity() instanceof ServerPlayer p) RelicScanner.onMenu(p, e.getContainer());
    }

    static void onContainerClose(PlayerContainerEvent.Close e) {
        if (e.getEntity() instanceof ServerPlayer p) RelicScanner.onMenu(p, e.getContainer());
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) RelicScanner.requestAll(p.getServer());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        RelicScanner.forget(id);
    }

    // ================================================================ item entities and mobs
    static void onEntityJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel sl) || !(e.getEntity() instanceof ItemEntity ie)) return;
        MinecraftServer server = sl.getServer();
        if (server == null || RelicScanner.costOf(ie.getItem()) <= 0) return;
        RelicScanner.now(server, RelicScanner.forItemEntity(RelicScanner.Run.SCAN, ie, sl), server.overworld().getGameTime());
    }

    static void onEquipmentChange(LivingEquipmentChangeEvent e) {
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide() || who instanceof Player || !(who.level() instanceof ServerLevel sl)) return;
        MinecraftServer server = sl.getServer();
        if (server == null || RelicScanner.costOf(e.getTo()) <= 0) return;
        RelicScanner.now(server, RelicScanner.forMob(RelicScanner.Run.SCAN, who, sl), server.overworld().getGameTime());
    }

    /**
     * An item entity left a level. If it was destroyed (burned, lost in the void, despawned) with the valid copy in it, the relic is marked lost, so
     * the world can have it again. Picking up, merging, unloading and changing dimension leave the item entity empty or are no destruction; an item
     * entity that was taken by a hopper or a container below it is not counted either (the relic is in that container now).
     */
    static void onEntityLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel sl) || !(e.getEntity() instanceof ItemEntity ie)) return;
        Entity.RemovalReason why = ie.getRemovalReason();
        ItemStack s = ie.getItem();
        if (why == null || !why.shouldDestroy() || s.isEmpty()) return;
        String id = OrderRelics.uniqueId(s);
        MinecraftServer server = sl.getServer();
        if (id == null || server == null) return;
        UniqueLedger l = UniqueLedger.get(server);
        if (LedgerRules.judge(true, l.known(id), RelicSerial.get(s), false) != Verdict.CANONICAL) return;
        if (collectedBelow(sl, ie)) return;
        if (l.lose(id)) LOG.info("[nusmp] {} was destroyed ({}) at {}: the Order marks it lost", OrderText.titleCase(id), why, ie.blockPosition().toShortString());
    }

    /** Is there a container where the item entity was (a hopper takes an item entity by copying its stack, leaving the entity's stack as it was)? */
    private static boolean collectedBelow(Level level, ItemEntity ie) {
        BlockPos at = ie.blockPosition();
        for (BlockPos p : new BlockPos[]{at, at.below()}) {
            if (!level.isLoaded(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof Container) return true;
        }
        return false;
    }
}
