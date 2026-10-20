package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.ManaCoreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Why: the few places where the game tells the Decennial feature something happened, in one class so the registry has one line to call. The data reload (the roster), a person breaking a block (an
 * element of a hall: the vanilla break is cancelled and the hall removes the element itself, with no drops and no tool wear), the death of one of a boss's adds (no loot, no experience, and the
 * boss's signature may feed on it), the server tick (the hall builds in steps, once a second the halls near people wake and sync), the start and stop of the server, and the Mana Core that a floor's
 * altar forges when a Decennial boss falls (it is named after the boss). Every handler leaves at once when the event is not about the feature, and every handler is wrapped so a mistake here lets
 * the game's own rule stand and never reaches the server loop.
 */
final class DecennialEvents {
    private DecennialEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    static void register(IEventBus bus) {
        bus.addListener(DecennialEvents::onReload);
        bus.addListener(DecennialEvents::onBreak);
        bus.addListener(EventPriority.HIGH, DecennialEvents::onAddDrops);
        bus.addListener(EventPriority.HIGH, DecennialEvents::onAddXp);
        bus.addListener(DecennialEvents::onAddDeath);
        bus.addListener(DecennialEvents::onTick);
        bus.addListener(DecennialEvents::onStarted);
        bus.addListener(DecennialEvents::onStopping);
        bus.addListener(DecennialEvents::onJoin);
    }

    private static void onReload(AddReloadListenerEvent e) { e.addListener(new DecennialReload()); }

    // ================================================================ an element is broken
    /** Runs after the Dungeon Guard's own handler (which lets an element through, see {@code DungeonGuard.allowBreakWhere}). */
    private static void onBreak(BlockEvent.BreakEvent e) {
        try {
            if (!(e.getLevel() instanceof ServerLevel sl)) return;
            BlockPos pos = e.getPos();
            if (!BreakableCells.serverHas(pos.asLong())) return;
            e.setCanceled(true);                                                   // no drops, no tool wear: the hall removes the element itself
            if (DecennialArenas.breakAt(sl, pos, e.getPlayer() instanceof ServerPlayer sp ? sp : null)) return;
            BreakableCells.serverRemove(pos.asLong());                             // a stale cell (its hall is gone): forget it
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] An element of a hall could not be broken: {}", ex.toString());
        }
    }

    // ================================================================ the adds
    private static boolean isAdd(Entity e) { return e instanceof Mob m && m.getTags().contains(AddKinds.TAG); }

    private static void onAddDrops(LivingDropsEvent e) {
        if (isAdd(e.getEntity())) e.setCanceled(true);
    }

    private static void onAddXp(LivingExperienceDropEvent e) {
        if (isAdd(e.getEntity())) e.setDroppedExperience(0);
    }

    private static void onAddDeath(LivingDeathEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob m) || !isAdd(m) || !(m.level() instanceof ServerLevel sl)) return;
            UUID owner = AddKinds.owner(m);
            if (owner == null) return;
            Entity boss = sl.getEntity(owner);
            if (boss instanceof LabyrinthGuardianEntity g && g.decennial().active()) g.decennial().addDied(g);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The death of an add could not be told to its boss: {}", ex.toString());
        }
    }

    // ================================================================ time and the server
    private static void onTick(ServerTickEvent.Post e) {
        try {
            MinecraftServer server = e.getServer();
            DecennialArenas.stepBuilds(server);
            if (server.getTickCount() % 20 == 7) DecennialArenas.pulse(server);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The Decennial halls could not be advanced: {}", ex.toString());
        }
    }

    private static void onStarted(ServerStartedEvent e) {
        try {
            DecennialArenas.recount(e.getServer());
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The Decennial halls could not be counted at start: {}", ex.toString());
        }
    }

    private static void onStopping(ServerStoppingEvent e) { DecennialArenas.shutdown(); }

    // ================================================================ the core of a Decennial boss carries its name
    private static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity item) || !(item.getItem().getItem() instanceof ManaCoreItem)) return;
            ItemStack stack = item.getItem();
            int sector = ManaCoreItem.sectorOf(stack);
            MinecraftServer server = e.getLevel().getServer();
            if (sector < 0 || server == null || stack.has(DataComponents.CUSTOM_NAME)) return;
            DecennialData.Site s = DecennialData.get(server).siteOfSector(sector);
            DecennialProfile p = s == null ? null : DecennialRoster.forLevel(s.level);
            if (p == null) return;
            ItemStack named = stack.copy();
            named.set(DataComponents.CUSTOM_NAME, Component.translatableWithFallback("message.nusmp.mega.decennial.core", "Core of %1$s",
                    Component.translatableWithFallback("decennial.boss." + p.id() + ".name", p.name())).withStyle(st -> st.withItalic(false).withColor(0xE0B0FF)));
            item.setItem(named);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] A Mana Core could not be named: {}", ex.toString());
        }
    }
}
