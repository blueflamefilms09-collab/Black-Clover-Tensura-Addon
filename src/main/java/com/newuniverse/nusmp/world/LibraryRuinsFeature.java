package com.newuniverse.nusmp.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/** Rare ruined library: broken walls, toppled bookshelves, cobwebs and a relic chest. */
public class LibraryRuinsFeature extends Feature<NoneFeatureConfiguration> {
    static final ResourceKey<LootTable> LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("nusmp", "chests/library_ruins"));

    public LibraryRuinsFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos o = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, ctx.origin());
        if (!level.getBlockState(o.below()).isSolid()) return false;
        int w = 4;
        for (int dx = -w; dx <= w; dx++) for (int dz = -w; dz <= w; dz++) {
            level.setBlock(o.offset(dx, -1, dz), rand.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 2);
            boolean edge = Math.abs(dx) == w || Math.abs(dz) == w;
            if (edge) {
                int h = rand.nextInt(4);   // broken, uneven walls
                for (int y = 0; y < h; y++)
                    level.setBlock(o.offset(dx, y, dz), rand.nextInt(3) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), 2);
            } else if (Math.abs(dx) == w - 1 && rand.nextInt(3) > 0) {
                level.setBlock(o.offset(dx, 0, dz), Blocks.BOOKSHELF.defaultBlockState(), 2);
                if (rand.nextBoolean()) level.setBlock(o.offset(dx, 1, dz), Blocks.BOOKSHELF.defaultBlockState(), 2);
            } else if (rand.nextInt(14) == 0) {
                level.setBlock(o.offset(dx, 0, dz), Blocks.COBWEB.defaultBlockState(), 2);
            }
        }
        BlockPos chest = o.offset(0, 0, 0);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(level, rand, chest, LOOT);
        com.newuniverse.nusmp.multiverse.WorldSites.record(com.newuniverse.nusmp.multiverse.WorldSites.Kind.RUINS, level.getLevel().dimension(), o);
        return true;
    }
}
