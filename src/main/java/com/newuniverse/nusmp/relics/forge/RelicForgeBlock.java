package com.newuniverse.nusmp.relics.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: a table block that opens {@link RelicForgeMenu}, built like a vanilla crafting table: no block entity (the menu's own container holds the items
 * while the window is open and hands every one back when it closes), so there is no stored state to duplicate, lose or scan, and breaking the table
 * can never swallow a relic. {@code useWithoutItem} is used rather than the item-aware use, so the table opens whatever the player holds, and a
 * right click with a block in hand still places it when sneaking (vanilla's rule).
 */
public class RelicForgeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);
    private static final Component TITLE = Component.translatable("container.nusmp.relic_forge");

    public RelicForgeBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new RelicForgeMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
