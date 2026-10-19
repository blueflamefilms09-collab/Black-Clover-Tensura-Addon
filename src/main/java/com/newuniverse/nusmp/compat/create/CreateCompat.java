package com.newuniverse.nusmp.compat.create;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the one entry point of the Create compatibility (docs/compat/create.md), so the mod class gains a single line. It always registers the server config
 * ({@code nusmp-labyrinth-create-compat-server.toml}), the operators' command and the self-test {@code compat.create}; it registers the parts that act on the dungeon (the
 * sector listener that plans traps, the tick that arms and runs them, the contraption guard) ONLY when the Create mod is installed. Soft by design: nothing here references a
 * Create class at compile time, every Create block, damage type and method is found by id or by name at run time, and every one of them is optional. With Create absent the
 * dungeon is exactly as it was.
 */
public final class CreateCompat {
    private CreateCompat() {}

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, CreateConfig.SPEC, "nusmp-labyrinth-create-compat-server.toml");
        NeoForge.EVENT_BUS.addListener(CreateCommand::register);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> PartResolver.clear());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            CreateTrapDirector.reset();
            PartResolver.clear();
        });
        CreateSelfTests.register();
        if (PartResolver.createLoaded()) {
            MegaDungeon.addListener(new CreateTrapDirector());
            NeoForge.EVENT_BUS.addListener(CreateTrapDirector::onServerTick);
            ContraptionGuard.register(NeoForge.EVENT_BUS);
        }
    }
}
