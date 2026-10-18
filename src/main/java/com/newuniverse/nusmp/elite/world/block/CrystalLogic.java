package com.newuniverse.nusmp.elite.world.block;

import com.newuniverse.nusmp.elite.world.MagiculeApi;
import com.newuniverse.nusmp.elite.world.WorldBlocks;
import com.newuniverse.nusmp.elite.world.WorldConfig;
import com.newuniverse.nusmp.elite.world.WorldGuard;
import com.newuniverse.nusmp.elite.world.rules.CrystalGrowth;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the Minecraft side of the growth rules in {@link CrystalGrowth}, kept out of the block classes so the two blocks stay small and the self-test can drive each step by hand. Every entry
 * point checks the switches first ({@link WorldConfig#crystals}, {@link WorldGuard#allowed}), so a crystal placed in the Dungeon or with the engine off simply never changes.
 */
public final class CrystalLogic {
    private CrystalLogic() {}

    /** How far round a cluster another crystal counts as "its grove". */
    public static final int GROVE_RADIUS = 3;

    /** A bud ripens, or a cluster matures into a Mother Rock (random tick). Returns true when the block changed. */
    public static boolean ripen(CrystalBudBlock self, BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (!WorldConfig.crystals() || !WorldGuard.allowed(level)) return false;
        CrystalStage st = self.stage();
        double density = MagiculeApi.effective(level, pos);
        if (st == CrystalStage.CLUSTER) return mature(state, level, pos, rnd, density);
        double chance = CrystalGrowth.growChance(st, density, WorldConfig.growthChance());
        if (chance <= 0.0 || rnd.nextDouble() >= chance) return false;
        CrystalStage next = st.next();
        if (next == null) return false;
        level.setBlock(pos, carry(WorldBlocks.blockOf(next), state), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.2f + 0.2f * st.ordinal());
        return true;
    }

    private static boolean mature(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd, double density) {
        int grove = groveNeighbours(level, pos);
        int rocks = MagiculeApi.mothersAt(level, pos);
        if (!CrystalGrowth.motherReady(density, grove, rocks, WorldConfig.motherCap(), WorldConfig.motherChance(), rnd.nextDouble())) return false;
        level.setBlock(pos, WorldBlocks.MOTHER_ROCK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.7f);
        MagiculeApi.bump(level, pos, 0.05, 1);
        return true;
    }

    /** The same facing and water as {@code from}, on the block {@code to}. */
    public static BlockState carry(Block to, BlockState from) {
        return to.defaultBlockState().setValue(AmethystClusterBlock.FACING, from.getValue(AmethystClusterBlock.FACING)).setValue(AmethystClusterBlock.WATERLOGGED, from.getValue(AmethystClusterBlock.WATERLOGGED));
    }

    /** Buds, clusters and Mother Rocks within {@value #GROVE_RADIUS} blocks (not counting the block itself). */
    public static int groveNeighbours(ServerLevel level, BlockPos pos) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-GROVE_RADIUS, -GROVE_RADIUS, -GROVE_RADIUS), pos.offset(GROVE_RADIUS, GROVE_RADIUS, GROVE_RADIUS))) {
            if (p.equals(pos)) continue;
            Block b = level.getBlockState(p).getBlock();
            if (b instanceof CrystalBudBlock || b instanceof MotherRockBlock) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- the Mother Rock

    /** A Mother Rock's random tick: it re-registers itself in its chunk, gains charge with the density, and spends a charge to seed a bud. */
    public static void motherTick(MotherRockBlock self, BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (!WorldConfig.crystals() || !WorldGuard.allowed(level)) return;
        MagiculeApi.addMother(level, pos);                       // idempotent: a rock placed before the chunk had data is found again
        double density = MagiculeApi.effective(level, pos);
        int charge = state.getValue(MotherRockBlock.CHARGE);
        if (charge < CrystalGrowth.MAX_CHARGE && rnd.nextDouble() < CrystalGrowth.chargeChance(charge, density)) {
            charge++;
            state = state.setValue(MotherRockBlock.CHARGE, charge);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        if (charge > 0 && rnd.nextDouble() < CrystalGrowth.seedChance(charge, density, WorldConfig.seedChance()) && seed(level, pos, rnd)) {
            level.setBlock(pos, state.setValue(MotherRockBlock.CHARGE, charge - 1), Block.UPDATE_CLIENTS);
        }
    }

    /** Tries a few times to sprout a Small Bud on a free face within the seed radius. Returns true when one was placed. */
    public static boolean seed(ServerLevel level, BlockPos origin, RandomSource rnd) {
        int r = WorldConfig.seedRadius();
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos spot = origin.offset(rnd.nextIntBetweenInclusive(-r, r), rnd.nextIntBetweenInclusive(-r, r), rnd.nextIntBetweenInclusive(-r, r));
            if (spot.equals(origin)) continue;
            BlockState at = level.getBlockState(spot);
            boolean water = at.is(Blocks.WATER) && at.getFluidState().isSource();
            if (!(at.isAir() || water)) continue;
            Direction face = Direction.getRandom(rnd);
            BlockPos support = spot.relative(face.getOpposite());
            BlockState sup = level.getBlockState(support);
            if (!CrystalGrowth.canSeed(true, sup.isFaceSturdy(level, support, face))) continue;
            if (crowded(level, spot)) continue;
            BlockState bud = WorldBlocks.BUD_SMALL.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, face).setValue(AmethystClusterBlock.WATERLOGGED, water);
            if (!bud.canSurvive(level, spot)) continue;
            level.setBlock(spot, bud, Block.UPDATE_ALL);
            level.playSound(null, spot, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 1.0f);
            return true;
        }
        return false;
    }

    /** More than three crystals within two blocks of a spot: the grove is full there. */
    private static boolean crowded(ServerLevel level, BlockPos spot) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(spot.offset(-2, -2, -2), spot.offset(2, 2, 2))) if (level.getBlockState(p).getBlock() instanceof CrystalBudBlock && ++n > 3) return true;
        return false;
    }
}
