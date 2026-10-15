package com.newuniverse.nusmp.labyrinth.gate;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the two portal blocks of the Labyrinth (docs/labyrinth/c_gate.md). They live in their own DeferredRegister so the gate package registers
 * itself in one line from {@link com.newuniverse.nusmp.labyrinth.LabyrinthSystem}, and their block items go on {@code NUItems.ITEMS} like every
 * other block of the mod ({@link com.newuniverse.nusmp.dungeon.DungeonBlocks}), which is why the system init must run before
 * {@code NUItems.ITEMS.register}. Both blocks are built like bedrock: {@code strength(-1, 3600000)} is "cannot be mined in survival" plus "no
 * explosion reaches it", {@code pushReaction(BLOCK)} stops pistons moving them, {@code noLootTable} because nothing should ever drop one. They
 * have NO collision so a player walks INTO them the way they walk into a nether portal, and {@code noOcclusion} because their texture is
 * translucent (the model declares {@code render_type: translucent}) and the faces behind them must still draw.
 */
public final class GateRegistry {
    private GateRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** The Labyrinth Gate: the floating gate of the Overworld arch. Walking into it takes you to the Spire's arrival pad. */
    public static final DeferredBlock<GateBlock> LABYRINTH_GATE = BLOCKS.register("labyrinth_gate", () -> new GateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollission().noOcclusion().strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 11).noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)));

    /** The Exit Gate on the arrival pad: golden-white, walking into it takes you back to where you came in. */
    public static final DeferredBlock<ExitGateBlock> LABYRINTH_EXIT = BLOCKS.register("labyrinth_exit", () -> new ExitGateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().noOcclusion().strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 11).noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)));

    public static final DeferredItem<BlockItem> LABYRINTH_GATE_ITEM = NUItems.ITEMS.register("labyrinth_gate", () -> new BlockItem(LABYRINTH_GATE.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> LABYRINTH_EXIT_ITEM = NUItems.ITEMS.register("labyrinth_exit", () -> new BlockItem(LABYRINTH_EXIT.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
