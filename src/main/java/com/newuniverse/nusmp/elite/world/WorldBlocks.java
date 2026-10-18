package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.world.block.CrystalBudBlock;
import com.newuniverse.nusmp.elite.world.block.MotherRockBlock;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the five blocks of the Magic Ore growth, in the owner's stage order: {@code magic_bud_small}, {@code magic_bud_medium}, {@code magic_bud_large}, {@code magic_crystal_cluster} and
 * {@code mother_rock}. The first four are vanilla-style directional crystals (sizes after the amethyst's), the Mother Rock is a full block with a charge. Light grows with the stage.
 * Their block items are registered in {@link WorldItems}.
 */
public final class WorldBlocks {
    private WorldBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    public static final DeferredBlock<CrystalBudBlock> BUD_SMALL = BLOCKS.register(CrystalStage.SMALL_BUD.blockId,
            () -> new CrystalBudBlock(CrystalStage.SMALL_BUD, 3.0f, 4.0f, crystal(3, 1.0f)));
    public static final DeferredBlock<CrystalBudBlock> BUD_MEDIUM = BLOCKS.register(CrystalStage.MEDIUM_BUD.blockId,
            () -> new CrystalBudBlock(CrystalStage.MEDIUM_BUD, 4.0f, 3.0f, crystal(6, 1.2f)));
    public static final DeferredBlock<CrystalBudBlock> BUD_LARGE = BLOCKS.register(CrystalStage.LARGE_BUD.blockId,
            () -> new CrystalBudBlock(CrystalStage.LARGE_BUD, 5.0f, 3.0f, crystal(9, 1.4f)));
    public static final DeferredBlock<CrystalBudBlock> CLUSTER = BLOCKS.register(CrystalStage.CLUSTER.blockId,
            () -> new CrystalBudBlock(CrystalStage.CLUSTER, 7.0f, 3.0f, crystal(12, 1.5f)));
    public static final DeferredBlock<MotherRockBlock> MOTHER_ROCK = BLOCKS.register(CrystalStage.MOTHER_ROCK.blockId,
            () -> new MotherRockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .randomTicks().lightLevel(s -> 6 + 3 * s.getValue(MotherRockBlock.CHARGE))));

    private static BlockBehaviour.Properties crystal(int light, float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().sound(SoundType.AMETHYST_CLUSTER).strength(strength)
                .lightLevel(s -> light).pushReaction(PushReaction.DESTROY).randomTicks();
    }

    /** The block of a stage. */
    public static Block blockOf(CrystalStage stage) {
        return switch (stage) {
            case SMALL_BUD -> BUD_SMALL.get();
            case MEDIUM_BUD -> BUD_MEDIUM.get();
            case LARGE_BUD -> BUD_LARGE.get();
            case CLUSTER -> CLUSTER.get();
            case MOTHER_ROCK -> MOTHER_ROCK.get();
        };
    }

    /** Touch the class so its entries exist before the register is attached. */
    public static void init() {}
}
