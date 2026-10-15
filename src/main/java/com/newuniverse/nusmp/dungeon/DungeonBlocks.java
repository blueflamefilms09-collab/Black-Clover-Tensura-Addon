package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The blocks of the Living Dungeon: the Maw and its roots in the Overworld, the Root Gate and the Mycelium veil below. */
public final class DungeonBlocks {
    private DungeonBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** The mouth of a Dungeon Maw: dense mana torn open. Cannot be broken; it closes when its roots are gone or its time is out. */
    public static final DeferredBlock<PortalBlock> DUNGEON_MAW = BLOCKS.register("dungeon_maw", () -> new PortalBlock(false,
            BlockBehaviour.Properties.of().noCollission().strength(-1f, 3600000f).lightLevel(s -> 12).noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.SCULK)));

    /** The Root Gate in the Dungeon that takes you back up. */
    public static final DeferredBlock<PortalBlock> DUNGEON_GATE = BLOCKS.register("dungeon_gate", () -> new PortalBlock(true,
            BlockBehaviour.Properties.of().noCollission().strength(-1f, 3600000f).lightLevel(s -> 10).noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.SCULK)));

    /** A root wrapped round a Maw: tough, and best chopped with an axe. */
    public static final DeferredBlock<Block> MAW_ROOT = BLOCKS.register("maw_root", () -> new Block(
            BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.MANGROVE_ROOTS).lightLevel(s -> 3).noLootTable()));

    /** A veil of glowing mycelium: where the Dungeon is growing a mined block back, and where a carcass has returned to the soil. */
    public static final DeferredBlock<Block> DUNGEON_MYCELIUM = BLOCKS.register("dungeon_mycelium", () -> new MyceliumVeilBlock(
            BlockBehaviour.Properties.of().noCollission().replaceable().instabreak().lightLevel(s -> 5).noLootTable().sound(SoundType.MOSS).pushReaction(PushReaction.DESTROY)));

    /** What a creature of the Dungeon leaves behind (see CarcassBlock). No item: it is not carried. */
    public static final DeferredBlock<CarcassBlock> CARCASS = BLOCKS.register("carcass", () -> new CarcassBlock(
            BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> DUNGEON_MAW_ITEM = NUItems.ITEMS.register("dungeon_maw", () -> new BlockItem(DUNGEON_MAW.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DUNGEON_GATE_ITEM = NUItems.ITEMS.register("dungeon_gate", () -> new BlockItem(DUNGEON_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MAW_ROOT_ITEM = NUItems.ITEMS.register("maw_root", () -> new BlockItem(MAW_ROOT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DUNGEON_MYCELIUM_ITEM = NUItems.ITEMS.register("dungeon_mycelium", () -> new BlockItem(DUNGEON_MYCELIUM.get(), new Item.Properties()));

    public static void init() {}

    /** A mouth you walk into: a Maw takes you down, a Gate takes you up. */
    public static final class PortalBlock extends Block {
        private final boolean exit;

        PortalBlock(boolean exit, BlockBehaviour.Properties props) {
            super(props);
            this.exit = exit;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (level.isClientSide || !(entity instanceof ServerPlayer p) || LivingDungeon.justTravelled(p)) return;
            if (com.newuniverse.nusmp.labyrinth.Labyrinth.engineIsLabyrinth()) {                                   // 1.35.0: the Labyrinth engine takes the Maws
                if (exit) com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel.leaveAny(p); else com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel.enterFromMaw(p, pos);
                return;
            }
            if (exit) LivingDungeon.leave(p); else LivingDungeon.enter(p, pos);                                     // legacy: the Living Dungeon caves
        }
    }

    /** The mycelium veil: a thin mat that never blocks anything. */
    public static final class MyceliumVeilBlock extends Block {
        MyceliumVeilBlock(BlockBehaviour.Properties props) { super(props); }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Block.box(1, 0, 1, 15, 2, 15); }
    }
}
