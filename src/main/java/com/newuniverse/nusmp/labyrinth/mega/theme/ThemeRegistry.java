package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRegistry;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleSelfTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registration entry point of the themes and the Crucible of Choice, the one class {@code MegaSystem} calls for them (it was a contract stub
 * with two empty methods; the bodies are filled in now and nothing else changes). {@link #init} runs on the mod bus before {@code NUItems.ITEMS.register}
 * and does three things: it registers the structure processor type {@code nusmp:region_theme} (the processor list {@code nusmp:mega_interior} names it,
 * so it MUST be registered before the datapack is read), it puts the Crucible in front of the Relic Pedestal's pick and drop ({@link CrucibleRegistry}),
 * and it registers the in-game self-tests the CI server smoke test runs. {@link #register} receives the game bus and registers the reload listeners
 * that read the theme files, the Cursed Spirit roster and the relic-to-theme table, the ambient particles of a theme and every handler of the
 * Crucible. The server config {@link ThemeConfig#SPEC} is registered by the lead (one line in the mod class, see the report), not here, so that the
 * mod's single place for config registration stays the only one.
 */
public final class ThemeRegistry {
    private ThemeRegistry() {}

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, NUSMP.MODID);

    /** {@code nusmp:region_theme}: swaps the deepslate and dark oak palette blocks for the family of the region's theme (see {@link RegionThemeProcessor}). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<RegionThemeProcessor>> REGION_THEME =
            PROCESSORS.register("region_theme", () -> (StructureProcessorType<RegionThemeProcessor>) () -> RegionThemeProcessor.CODEC);

    public static void init(IEventBus modBus) {
        PROCESSORS.register(modBus);
        CrucibleRegistry.installHooks();
        ThemeSelfTests.register();
        CrucibleSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new ThemeDefs());
            e.addListener(new RelicThemeMap());
        });
        gameBus.addListener(ThemeAmbient::onServerTick);
        CrucibleRegistry.register(gameBus);
    }
}
