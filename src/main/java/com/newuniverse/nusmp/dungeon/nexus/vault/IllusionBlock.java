package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Illusion Stone and Illusion Deepslate (docs/living_dungeon.md, "Hidden Mana Vaults"): they look exactly like the rock they sit in and
 * have no collision, so players and creatures walk straight through them. They are the wall of a hidden vault. To keep the secret they
 * also copy what rock does to light (they block it, so a lit vault does not glow through its wall) and to shading (the corners of the
 * blocks beside them are darkened as beside rock), and they count as solid for fluids, so water can neither wash them away nor seep through.
 */
public class IllusionBlock extends Block {
    public IllusionBlock(BlockBehaviour.Properties props) { super(props); }

    /** A full cube to look at and to click on, though it has nothing to bump into. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 15; }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 0.2f; }
}
