package com.newuniverse.nusmp.labyrinth.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the Labyrinth Gate ({@code nusmp:labyrinth_gate}) is a block you walk into, not one you click, so a party runs through the arch together
 * the way they would run into a nether portal; {@code entityInside} is the hook the game already fires for every entity whose box overlaps a
 * no-collision block, so no ticking block entity is needed. Only a {@link ServerPlayer} travels (mobs and items stay), and only once per gate
 * use: {@link LabyrinthTravel#justTravelled} reads the game-time stamp the travel code leaves in the player's persistent data, so the player
 * who is set down INSIDE a gate on the way back is not sent straight through again (the same guard {@code DungeonBlocks.PortalBlock} uses).
 * The outline shape is the full cube so the block can be looked at and targeted while the collision shape stays empty. The client swirl is
 * drawn in {@code animateTick} (the game's random-tick hook for cosmetic particles) and the real animation is the texture itself (a 16 x 128
 * frame sheet with a {@code .png.mcmeta}), which costs nothing per block. Which way the gate sends you is {@link #travel}, overridden by the
 * Exit Gate.
 */
public class GateBlock extends Block {
    public GateBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.block(); }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || LabyrinthTravel.justTravelled(p)) return;
        travel(p, pos);
    }

    /** What the gate does with a player who stepped into it: this one leads into the Labyrinth (or the legacy caves, by the engine switch). */
    protected void travel(ServerPlayer p, BlockPos pos) { LabyrinthTravel.enter(p, pos); }

    /** Motes wind inward and down into the gate; now and then a white spark and the portal's hum. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        double t = (level.getGameTime() % 80) / 80.0 * Math.PI * 2.0;
        for (int i = 0; i < 3; i++) {
            double a = t + i * 2.0944 + rnd.nextDouble() * 0.3, r = 0.65 - i * 0.06;
            double x = pos.getX() + 0.5 + Math.cos(a) * r, z = pos.getZ() + 0.5 + Math.sin(a) * r;
            double y = pos.getY() + 0.15 + rnd.nextDouble() * 0.7;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, -Math.cos(a) * 0.1, -0.05, -Math.sin(a) * 0.1);
        }
        if (rnd.nextInt(4) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 0.2 + rnd.nextDouble() * 0.6, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        if (rnd.nextInt(100) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3f, 0.6f + rnd.nextFloat() * 0.3f, false);
    }
}
