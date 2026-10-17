package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registration entry point of the Hub's terrain generator (the MACRO module of the bidirectional Mega-Dungeon, 1.55.0), the one class
 * {@code MegaSystem} calls. It registers exactly one thing in the game's registries: the CODEC of {@link SanctuaryGenerator} in the chunk-generator registry, under
 * the id {@code nusmp:sanctuary}, because the Hub's dimension JSON ({@code "generator": {"type": "nusmp:sanctuary", "biome_source": ...}}) names it and the
 * dimension registry is read from the datapack when the server starts, which is after every mod-bus registration has run (that is why this is on the MOD bus
 * in {@code init}, called from {@code MegaSystem.init}, and not a game-bus handler). It also adds the in-game self-tests and drops the cached per-seed models
 * when the server stops, so a long-running client that opens world after world does not keep them.
 */
public final class MacroRegistry {
    private MacroRegistry() {}

    /** The chunk generator codecs of this mod. */
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, NUSMP.MODID);

    /** {@code nusmp:sanctuary}: the Hub's generator. */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<SanctuaryGenerator>> SANCTUARY = GENERATORS.register("sanctuary", () -> SanctuaryGenerator.CODEC);

    public static void init(IEventBus modBus) {
        GENERATORS.register(modBus);
        MacroSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((ServerStoppedEvent e) -> HubModel.clearCache());
    }
}
