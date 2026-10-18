package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the registration entry point of the dungeon mechanics library, the one class {@code MegaSystem} calls (the lead adds a single {@code init} and a single {@code register}
 * line, as for every other module). It registers no block, item or entity: {@code init} adds the in-game self-tests ({@link MechanicsSelfTests}, keys {@code mech.*}, which the CI
 * smoke test runs) and hooks the two dungeon listeners; {@code register} wires the game-bus handlers with {@code addListener} like the rest of the mod: the Director's tick, the
 * damage / heal / death / interaction hooks, the reload listeners that read the mechanics and the monster groups from the data packs, and the server-stopping cleanup. The lead
 * registers {@link MechanicsConfig#SPEC} as {@code nusmp-labyrinth-mega-mechanics-server.toml} and attaches {@link #command()} under {@code /multiverse mega}.
 */
public final class MechanicsRegistry {
    private MechanicsRegistry() {}

    public static void init(IEventBus modBus) {
        MechanicsSelfTests.register();
        MegaDungeon.addFloorListener(MechanicsEvents.LISTENER);
        MegaDungeon.addListener(MechanicsEvents.LISTENER);
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MechanicsDirector::onServerTick);
        gameBus.addListener(MechanicsEvents::onIncoming);
        gameBus.addListener(MechanicsEvents::onHeal);
        gameBus.addListener(MechanicsEvents::onDeath);
        gameBus.addListener(MechanicsEvents::onRightBlock);
        gameBus.addListener(MechanicsEvents::onRightItem);
        gameBus.addListener(MechanicsEvents::onStopping);
        gameBus.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new MechanicLibrary.Reload());
            e.addListener(new MobGroupsReload());
        });
    }

    /** The {@code mechanic} admin node ({@code list [category] | info <id> | apply <id> here | clear | count | verify}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return MechanicsCommand.command();
    }
}
