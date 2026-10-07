package com.newuniverse.nusmp.block;

import com.newuniverse.nusmp.book.UnderworldMatter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 0.47 Kotodama: underworld matter (Zagred's purple-black sludge). Placed only by {@code book.UnderworldMatter}, which records
 * what it devoured and puts it back when the spell ends. It cannot be broken or pushed while it lasts (so the restore is exact),
 * drops nothing, drags at whatever wades through it and glows faintly. The life drain is done by the spreading session, which
 * knows whose sludge it is. Matter that no session owns (left behind by a crash) crumbles to dirt on a random tick.
 */
public class UnderworldMatterBlock extends Block {
    public UnderworldMatterBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable().sound(SoundType.HONEY_BLOCK)
                .speedFactor(0.45f).jumpFactor(0.6f).lightLevel(s -> 4).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false)
                .randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!UnderworldMatter.owns(level, pos)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
    }
}
