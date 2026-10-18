package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import com.newuniverse.nusmp.elite.world.rules.OreConversion;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: ORE CONVERSION. In a chunk whose density stays above the threshold, ordinary ores slowly turn into the magic ore of the data map (Tensura's own {@code tensura:magic_ore} and
 * {@code tensura:deepslate_magic_ore}, and the silver ores, when Tensura is there; a target that is not registered is skipped). The pass is cheap: it asks each section's palette whether
 * any ore state exists ({@code maybeHas}), samples that section, and converts at most ONE block, under the chunk's budget ({@link OreConversion.Budget}: so many per window, so many in
 * the chunk's life). It is never run in the protected dimensions (the caller checks {@link WorldGuard#allowed}, which refuses every Dungeon dimension and the Hub).
 */
public final class OreConverter {
    private OreConverter() {}

    private static final Map<Block, String> IDS = new IdentityHashMap<>();

    private static String idOf(Block b) {
        String s = IDS.get(b);
        if (s == null) {
            s = BuiltInRegistries.BLOCK.getKey(b).toString();
            IDS.put(b, s);
        }
        return s;
    }

    private static boolean isOre(BlockState s) { return WorldData.ores().lookup(idOf(s.getBlock())) != null; }

    private static boolean exists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
    }

    /** One pass over one chunk. Returns the converted position or null. Public for the self-test. */
    public static BlockPos pass(ServerLevel level, LevelChunk chunk, ChunkMagicule d, RandomSource rnd) {
        if (!WorldConfig.ores() || WorldData.ores().isEmpty() || !WorldGuard.allowed(level)) return null;
        if (d.density < WorldConfig.oreThreshold()) return null;
        long now = level.getGameTime();
        OreConversion.Budget b = d.ore;
        boolean fresh = now < b.windowStart || now - b.windowStart >= WorldConfig.oreInterval();
        if (b.total >= WorldConfig.oreLifetime() || (!fresh && b.usedInWindow >= WorldConfig.orePerInterval())) return null;       // the budget is spent: no scan at all
        LevelChunkSection[] sections = chunk.getSections();
        int start = rnd.nextInt(Math.max(1, sections.length));
        for (int n = 0; n < sections.length; n++) {
            int si = (start + n) % sections.length;
            LevelChunkSection sec = sections[si];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(OreConverter::isOre)) continue;
            int baseY = (chunk.getMinSection() + si) << 4;
            int from = rnd.nextInt(4096);
            for (int k = 0; k < 4096; k++) {
                int idx = (from + k) & 4095;
                int lx = idx & 15, lz = (idx >> 4) & 15, ly = idx >> 8;
                BlockState s = sec.getBlockState(lx, ly, lz);
                if (!isOre(s)) continue;
                int y = baseY + ly;
                OreConversion.Entry e = WorldData.ores().lookup(idOf(s.getBlock()));
                if (e == null || !e.fits(MagiculeModel.effective(d.density, y, level.getSeaLevel()), y)) continue;
                String target = OreConversion.pick(e, rnd.nextDouble(), OreConverter::exists);
                if (target == null) continue;
                if (!b.tryTake(now, WorldConfig.oreInterval(), WorldConfig.orePerInterval(), WorldConfig.oreLifetime())) return null;
                BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + lx, y, chunk.getPos().getMinBlockZ() + lz);
                Block to = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(target));
                level.setBlock(pos, to.defaultBlockState(), Block.UPDATE_ALL);
                chunk.setUnsaved(true);
                RukhFx.burst(level, Vec3.atCenterOf(pos), RukhFx.Alignment.LIGHT, 6);
                return pos;
            }
        }
        return null;
    }

    /** The id a block is looked up by in the ore map (for the self-test). */
    public static Optional<String> targetFor(String fromId, double density, int y) {
        OreConversion.Entry e = WorldData.ores().lookup(fromId);
        if (e == null || !e.fits(density, y)) return Optional.empty();
        return Optional.ofNullable(OreConversion.pick(e, 0.0, OreConverter::exists));
    }
}
