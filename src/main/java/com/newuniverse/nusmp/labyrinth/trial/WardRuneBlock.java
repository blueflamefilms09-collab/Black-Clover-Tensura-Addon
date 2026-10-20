package com.newuniverse.nusmp.labyrinth.trial;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: a floor tile that MARKS where a barrier column rises, instead of the archway storing door offsets in its NBT. Jigsaw rotates every
 * piece it places; an offset written into the keystone's tag would point into a wall three times out of four, while a tile in the floor
 * is rotated with the rest of the piece by the structure placer and the archway simply scans its box for it. It is a plain, ordinary
 * block on purpose (no block entity, no ticker): a chamber may hold a dozen of them and they must cost nothing until a trial starts.
 * Its faint light and rune face tell a player where the doors are before they close.
 */
public final class WardRuneBlock extends Block {
    public WardRuneBlock(Properties props) { super(props); }

    /** A rare spark over the tile: enough to catch the eye in a dim corridor, not enough to fill a doorway with particles. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(12) != 0) return;
        Fx.add(level, FxThemes.DUNGEON_TRIAL, FxOld.ELECTRIC_SPARK, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0.0, 0.02, 0.0);
    }
}
