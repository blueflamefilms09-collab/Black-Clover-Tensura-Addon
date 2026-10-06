package com.newuniverse.nusmp.world;

import com.newuniverse.nusmp.block.NUBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A super-rare Grimoire Tower: a round stone library tower with windows, floors of bookshelves,
 * a wall ladder, a treasure chest, and a Grimoire Altar on the top floor.
 */
public class GrimoireTowerFeature extends Feature<NoneFeatureConfiguration> {
    static final ResourceKey<LootTable> LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("nusmp", "chests/grimoire_tower"));

    public GrimoireTowerFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        // 0.39: the towers belong to the Clover Kingdom; they only start appearing in new land once the Convergence reaches it
        if (com.newuniverse.nusmp.multiverse.Convergence.enabled()
                && !com.newuniverse.nusmp.multiverse.Convergence.cachedStage().atLeast(com.newuniverse.nusmp.multiverse.Convergence.Stage.CLOVER)) return false;
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos o = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, ctx.origin());
        if (!level.getBlockState(o.below()).isSolid() || level.getBlockState(o.below()).getFluidState().isSource()) return false;
        int r = 5, height = 20 + rand.nextInt(6);

        // foundation: fill down to solid ground
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            for (int y = -1; y > -8; y--) {
                BlockPos p = o.offset(dx, y, dz);
                if (level.getBlockState(p).isSolid() && y < -1) break;
                level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
        }
        // walls, windows, air inside
        for (int y = 0; y < height; y++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > r * r) continue;
            BlockPos p = o.offset(dx, y, dz);
            if (d2 >= (r - 1) * (r - 1)) {
                boolean window = y % 5 == 2 && (dx == 0 || dz == 0);
                boolean door = y < 2 && dz == -r && dx == 0;
                level.setBlock(p, door ? Blocks.AIR.defaultBlockState() : window ? Blocks.GLASS_PANE.defaultBlockState()
                        : rand.nextInt(7) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), 2);
            } else {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // floors every 5 blocks (with a ladder hole), bookshelves around each floor
        for (int fy = 0; fy < height; fy += 5) {
            for (int dx = -r + 1; dx <= r - 1; dx++) for (int dz = -r + 1; dz <= r - 1; dz++) {
                if (dx * dx + dz * dz >= (r - 1) * (r - 1)) continue;
                if (fy > 0 && dx == 0 && dz == r - 2) continue;   // ladder hole
                level.setBlock(o.offset(dx, fy, dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
            }
            for (int a = 0; a < 16; a++) {
                double ang = a * Math.PI / 8;
                int dx = (int) Math.round(Math.cos(ang) * (r - 2)), dz = (int) Math.round(Math.sin(ang) * (r - 2));
                if (dz >= r - 2 && Math.abs(dx) <= 1) continue;   // keep the ladder side clear
                if (dz == -(r - 2) && dx == 0) continue;           // keep the door clear
                for (int h = 1; h <= 2; h++) level.setBlock(o.offset(dx, fy + h, dz), Blocks.BOOKSHELF.defaultBlockState(), 2);
            }
        }
        // ladder against the north wall, all the way up
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = 1; y < height; y++) level.setBlock(o.offset(0, y, r - 2), ladder, 2);
        // lecterns on the ground floor
        level.setBlock(o.offset(2, 1, 0), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST), 2);
        level.setBlock(o.offset(-2, 1, 0), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST), 2);
        // top floor: the Grimoire Altar and a treasure chest
        int top = (height - 1) / 5 * 5;
        level.setBlock(o.offset(0, top + 1, 0), NUBlocks.GRIMOIRE_ALTAR.get().defaultBlockState(), 2);
        BlockPos chest = o.offset(2, top + 1, -1);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(level, rand, chest, LOOT);
        // roof
        for (int rr = r + 1; rr >= 0; rr--) {
            int y = height + (r + 1 - rr);
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++)
                if (dx * dx + dz * dz <= rr * rr && dx * dx + dz * dz >= (rr - 1) * (rr - 1))
                    level.setBlock(o.offset(dx, y, dz), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
        }
        com.newuniverse.nusmp.multiverse.WorldSites.record(com.newuniverse.nusmp.multiverse.WorldSites.Kind.TOWER, level.getLevel().dimension(), o);
        return true;
    }
}
