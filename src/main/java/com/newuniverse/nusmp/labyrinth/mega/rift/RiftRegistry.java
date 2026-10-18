package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registration entry point of the dimensional rifts and the waypoint (1.72.0), the pattern of every package of the Mega-Dungeon: its own deferred registers and one {@link #init}
 * (mod bus: the block, its block entity, the in-game self-tests) and one {@link #register} (game bus: the spawner, the waypoint sampler, the pocket realm's events), called from
 * {@code MegaSystem}. The block has no block item on purpose: a rift is made by the dungeon, a relic or an admin command, never placed from the inventory.
 */
public final class RiftRegistry {
    private RiftRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    /** Unbreakable, immovable, no collision, light 11: the tear gives light. */
    public static final DeferredBlock<DimensionalRiftBlock> RIFT = BLOCKS.register("dimensional_rift", () -> new DimensionalRiftBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.AMETHYST).strength(-1f, 3600000f).pushReaction(PushReaction.BLOCK).noOcclusion().noCollission()
                    .noLootTable().lightLevel(s -> 11)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalRiftBlockEntity>> RIFT_BE = BLOCK_ENTITIES.register("dimensional_rift",
            () -> BlockEntityType.Builder.of(DimensionalRiftBlockEntity::new, RIFT.get()).build(null));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RiftSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        RiftService.register(gameBus);
        PocketEvents.register(gameBus);
    }

    /** The {@code rift} admin node ({@code open | list | close | pocket | waypoint | status}); attached under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return RiftCommand.command(); }
}
