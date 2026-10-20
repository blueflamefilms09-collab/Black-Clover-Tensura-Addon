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
 * Why: the floor tile that MARKS where a trial's mobs appear, for the same reason as {@link WardRuneBlock}: a marker rotates with its
 * Jigsaw piece, a stored offset does not. A builder lays glyphs where a mob can actually stand (the archway only ever spawns on the cell
 * above a glyph), which is also the only safe answer to "where is the floor" in a piece the mod has never seen; when a chamber has none,
 * the archway falls back to the floor round the keystone. Its violet circle is the one visual warning a player gets before stepping in.
 */
public final class SpawnGlyphBlock extends Block {
    public SpawnGlyphBlock(Properties props) { super(props); }

    /** Violet motes rising off the circle, and now and then a portal swirl: the summoning circle is never quite still. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(4) != 0) return;
        double x = pos.getX() + 0.15 + rnd.nextDouble() * 0.7, z = pos.getZ() + 0.15 + rnd.nextDouble() * 0.7;
        level.addParticle(rnd.nextInt(5) == 0 ? Fx.options(FxThemes.DUNGEON_TRIAL, FxOld.PORTAL) : Fx.options(FxThemes.DUNGEON_TRIAL, FxOld.WITCH), x, pos.getY() + 1.05, z, 0.0, 0.03 + rnd.nextDouble() * 0.02, 0.0);
    }
}
