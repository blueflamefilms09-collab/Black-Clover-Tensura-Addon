package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Why: the registration entry point of the System (1.55.0, docs/labyrinth/mega_system.md): the Solo Leveling layer of the Holy road, with its holographic window,
 * floor quests, permanent stat rewards and the Shadow Core that raises slain creatures as shadows. {@code MegaSystem} calls {@link #init} and {@link #register}
 * (the lead adds no other line for the server side), so the package wires itself: {@code init} runs BEFORE {@code NUItems.ITEMS.register} (so the Shadow Core
 * can go on the shared item register), registers the data component of the core and the attachment {@code nusmp:system_state} on the mod bus, the two payloads
 * ({@code nusmp:system_sync}, {@code nusmp:system_toast}), the one {@link com.newuniverse.nusmp.labyrinth.mega.FloorListener} that runs the quests, and the five
 * in-game self-tests ({@code system.*}) with the shared registry the CI server smoke test runs. {@code register} puts the data reload of the quests and stats
 * and every game-bus handler on the game bus with {@code addListener} (no annotation scanning, like the rest of the mod). The client half is NOT here: it is
 * {@code client/mega/system/SystemClient}, which the lead initialises in the client block of the mod class.
 */
public final class SystemRegistry {
    private SystemRegistry() {}

    /** {@code nusmp:shadow_core}, for the creative tab list and the held-model registration of {@code WeaponRenderer}. */
    public static final DeferredItem<ShadowCoreItem> SHADOW_CORE = ShadowItems.SHADOW_CORE;

    public static void init(IEventBus modBus) {
        ShadowItems.init();
        ShadowItems.COMPONENTS.register(modBus);
        SystemAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(SystemNet::register);
        MegaDungeon.addFloorListener(SystemQuests.LISTENER);
        SystemSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new SystemReload()));
        SystemEvents.register(gameBus);
    }

    /** The {@code system} admin node ({@code status | quest | stat | core | minions}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return SystemCommand.command();
    }
}
