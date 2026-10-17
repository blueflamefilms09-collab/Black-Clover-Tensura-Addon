package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: the registration entry point of the Safe-Zone builder of the Mega-Dungeon (1.54.0): one structure processor type
 * ({@code nusmp:mod_compat}, which swaps marked vanilla furniture for Farmer's Delight and Refurbished Furniture blocks when those mods are
 * loaded), one player attachment ({@code nusmp:mega_rest}, the resting capability), the game-bus handlers of resting and of the end of easy
 * regeneration, and the reload listener of the compat tables. {@code MegaSystem} calls {@link #init} (mod bus: the two DeferredRegisters, which
 * must be handed to the bus while the mod is still being constructed) and {@link #register} (game bus: handlers added with {@code addListener},
 * no {@code @SubscribeEvent} scanning, like the rest of the mod). The register NAMED {@code PROCESSORS} is a contract: the template checker
 * greps {@code PROCESSORS.register("mod_compat"} to learn which processor types exist. Nothing here registers a block or an item, so there is
 * no ordering constraint against {@code NUItems.ITEMS}.
 */
public final class SafeRegistry {
    private SafeRegistry() {}

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, NUSMP.MODID);
    /** {@code nusmp:mod_compat}: swaps the blocks under {@code as=} markers for the compat mods' blocks (see {@link ModCompatProcessor}). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<ModCompatProcessor>> MOD_COMPAT =
            PROCESSORS.register("mod_compat", () -> (StructureProcessorType<ModCompatProcessor>) () -> ModCompatProcessor.CODEC);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    /** {@code nusmp:mega_rest}: the player's resting state (see {@link RestState}); a default state is not saved, and it does not survive death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RestState>> REST_STATE =
            ATTACHMENTS.register("mega_rest", () -> AttachmentType.builder(() -> new RestState()).serialize(RestState.CODEC, s -> !s.isDefault()).build());

    public static void init(IEventBus modBus) {
        PROCESSORS.register(modBus);
        ATTACHMENTS.register(modBus);
        SafeSelfTests.register();                                                           // the in-game self-tests of /multiverse mega selftest (safe.*)
    }

    /** Pass {@code NeoForge.EVENT_BUS}. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGH, RestEvents::onRightClick);                  // first, so a bed is never left to explode and a rest is not stolen by a later handler
        gameBus.addListener(RestEvents::onPlayerTick);
        gameBus.addListener(RestEvents::onDamage);
        gameBus.addListener(RestEvents::onDimensionChange);
        gameBus.addListener(RegenThrottle::onHeal);
        gameBus.addListener(RegenThrottle::onPlayerTick);
        gameBus.addListener(RegenThrottle::onDimensionChange);
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new CompatLoader()));
        gameBus.addListener((ServerStoppingEvent e) -> RestAnchors.clear());
    }

    /** The admin sub-command {@code rest} (status, refill, anchors); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return SafeCommand.command(); }

    /**
     * Lets the Dungeon Master (or anyone) hear about the ambush a long rest rolled; nothing spawns unless a listener does it. Call from the
     * listener's own registry, once.
     */
    public static void addAmbushListener(RestAmbushListener listener) { RestEvents.addAmbushListener(listener); }
}
