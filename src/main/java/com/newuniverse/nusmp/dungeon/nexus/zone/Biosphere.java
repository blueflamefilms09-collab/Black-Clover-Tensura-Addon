package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import com.newuniverse.nusmp.dungeon.nexus.NexusStates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The glue of the Golden City's living garden (docs/living_dungeon.md, "The Golden City"): every few seconds, near each player inside it,
 * a handful of places are looked at and brought to the state {@link BiosphereCycle} says they belong in at this time. Flowers bloom and
 * wilt, grass is grazed bare and returns, moss spreads and recedes, the shores of the pools rise and fall, vines grow down and are drawn
 * up with their berries ripening, and mini-groves sprout, grow and are pruned. It only ever replaces the garden's own plants (air,
 * flowers, grass, carpets, the cells of a grove, water and mud at a shore, the cells of a vine), never a path, a pool, a tree, a block of the
 * city or anything a player put there; at most {@link #MAX_WRITES} blocks are written a pass, and the whole pass runs inside
 * {@code DungeonGuard.internal} because the Dungeon's guard holds even flowers. Nothing is saved: the cycle is a function of the time.
 */
final class Biosphere {
    private Biosphere() {}

    private static final int PASS_TICKS = BiosphereCycle.PASS_TICKS, RADIUS = BiosphereCycle.RADIUS, SAMPLES = BiosphereCycle.SAMPLES, VINE_TRIES = BiosphereCycle.VINE_TRIES,
            MAX_WRITES = BiosphereCycle.MAX_WRITES, MAX_PLAYERS = BiosphereCycle.MAX_PLAYERS;
    private static final Map<Long, Long> LAST = new HashMap<>();
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static Set<Block> flora;

    static void tick(ServerLevel level, GoldenGrid g, List<ServerPlayer> players) {
        long now = level.getGameTime();
        long key = (((long) g.cellX) << 32) ^ (g.cellZ & 0xFFFFFFFFL);
        Long last = LAST.get(key);
        if (last != null && now >= last && now - last < PASS_TICKS) return;
        LAST.put(key, now);
        final int[] writes = {0};
        DungeonGuard.internal(() -> {
            int served = 0;
            for (ServerPlayer p : players) {
                if (served >= MAX_PLAYERS || writes[0] >= MAX_WRITES) break;
                around(level, g, p, now, 7L * (served + 1), writes);
                served++;
            }
        });
        ambience(level, g, players);
    }

    /** One player's share of a pass. */
    private static void around(ServerLevel level, GoldenGrid g, ServerPlayer p, long now, long salt, int[] writes) {
        int px = p.getBlockX(), pz = p.getBlockZ();
        long pass = now / PASS_TICKS;
        for (int k = 0; k < SAMPLES && writes[0] < MAX_WRITES; k++) {
            int[] d = BiosphereCycle.sample(g.seed ^ salt, pass, k, RADIUS);
            int x = px + d[0], z = pz + d[1], u = x - g.cx, v = z - g.cz;
            if (!g.g(u, v) || level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            byte kind = g.coverAt(u, v);
            if (BiosphereCycle.mayWrite(kind)) {
                if (cover(level, g, kind, x, z, u, v, now)) writes[0]++;
            } else if (kind == GoldenGrid.SHORE) {
                if (shore(level, g, x, z, u, v, now)) writes[0]++;
            }
        }
        List<GoldenGrid.Vine> vines = g.vines;
        for (int k = 0; k < VINE_TRIES && !vines.isEmpty() && writes[0] < MAX_WRITES; k++) {
            GoldenGrid.Vine vi = vines.get(GoldenRng.hash(g.seed ^ salt, (int) pass, k, 83) % vines.size());
            int x = g.cx + vi.u(), z = g.cz + vi.v();
            if (Math.abs(x - px) > RADIUS + 6 || Math.abs(z - pz) > RADIUS + 6 || level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            if (vine(level, g, vi, x, z, now)) writes[0]++;
        }
        List<GoldenGrid.Grove> groves = g.groves;
        if (!groves.isEmpty() && BiosphereCycle.mayRebuildGrove(writes[0])) {
            GoldenGrid.Grove gr = groves.get(GoldenRng.hash(g.seed ^ salt, (int) pass, 0, 84) % groves.size());
            int x = g.cx + gr.u(), z = g.cz + gr.v();
            if (Math.abs(x - px) <= RADIUS + 6 && Math.abs(z - pz) <= RADIUS + 6) writes[0] += grove(level, g, gr, x, z, now);
        }
    }

    // ---------------------------------------------------------------- the pieces
    private static Set<Block> flora() {
        if (flora == null) {
            Set<Block> set = new HashSet<>();
            for (String id : BiosphereCycle.FLORA) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(set::add);
            }
            flora = set;
        }
        return flora;
    }

    private static String idOf(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(); }

    /** Ground cover: the plant of the column's kind at this time, if what stands there now is air or one of the garden's own plants. */
    private static boolean cover(ServerLevel level, GoldenGrid g, byte kind, int x, int z, int u, int v, long now) {
        BlockPos pos = new BlockPos(x, g.floorY + g.groundAt(u, v) + 1, z);
        BlockState cur = level.getBlockState(pos);
        BiosphereCycle.Cover want = BiosphereCycle.cover(g.seed, kind, u, v, now);
        BlockState target = NexusStates.of(want.block(), want.props());
        if (cur == target || !(cur.isAir() || flora().contains(cur.getBlock()))) return false;
        level.setBlock(pos, target, NexusSink.FLAGS);
        return true;
    }

    /** The shore of a pool: mud rises out of the water and sinks back, but only where no water could run off and nobody swims. */
    private static boolean shore(ServerLevel level, GoldenGrid g, int x, int z, int u, int v, long now) {
        BlockPos pos = new BlockPos(x, g.floorY, z);
        BlockState cur = level.getBlockState(pos);
        if (BiosphereCycle.shoreWet(g.seed, u, v, now)) {
            if (!cur.is(Blocks.MUD)) return false;
            for (Direction d : SIDES) if (level.getBlockState(pos.relative(d)).isAir()) return false;
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), NexusSink.FLAGS);
            return true;
        }
        if (!cur.is(Blocks.WATER) || !cur.getFluidState().isSource()) return false;
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return false;
        level.setBlock(pos, Blocks.MUD.defaultBlockState(), NexusSink.FLAGS);
        return true;
    }

    /** A vine under a canopy: one cell more or one less each time it is looked at, then its berries ripen or drop. */
    private static boolean vine(ServerLevel level, GoldenGrid g, GoldenGrid.Vine vi, int x, int z, long now) {
        final int top = g.floorY + vi.anchor();
        if (level.getBlockState(new BlockPos(x, top, z)).isAir()) return false;               // the canopy is gone: nothing to hang from
        int have = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (have < vi.max() + 1) {
            BlockState s = level.getBlockState(p.set(x, top - 1 - have, z));
            if (s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)) have++;
            else break;
        }
        int want = BiosphereCycle.vineLength(g.seed, vi.u(), vi.v(), vi.max(), now);
        boolean ripe = BiosphereCycle.ripe(g.seed, vi.u(), vi.v(), now);
        BlockState plant = NexusStates.of("minecraft:cave_vines_plant", ripe ? "berries=true" : "berries=false");
        BlockState head = NexusStates.of("minecraft:cave_vines", ripe ? "age=25,berries=true" : "age=25,berries=false");
        if (have < want) {
            BlockPos next = new BlockPos(x, top - 1 - have, z);
            if (!level.getBlockState(next).isAir()) return false;
            if (have > 0) level.setBlock(next.above(), plant, NexusSink.FLAGS);
            level.setBlock(next, head, NexusSink.FLAGS);
            return true;
        }
        if (have > want) {
            BlockPos lowest = new BlockPos(x, top - have, z);
            level.setBlock(lowest, Blocks.AIR.defaultBlockState(), NexusSink.FLAGS);
            if (have > 1) level.setBlock(lowest.above(), head, NexusSink.FLAGS);
            return true;
        }
        for (int i = 0; i < have; i++) {                                                       // the berries: one cell a look
            BlockPos c = new BlockPos(x, top - 1 - i, z);
            BlockState wanted = i == have - 1 ? head : plant;
            if (level.getBlockState(c) != wanted) {
                level.setBlock(c, wanted, NexusSink.FLAGS);
                return true;
            }
        }
        return false;
    }

    /** A mini-grove: brought to the stage the time says, unless somebody stands in it. Cells that hold anything but the grove's own blocks are left as they are. Returns the blocks written. */
    private static int grove(ServerLevel level, GoldenGrid g, GoldenGrid.Grove gr, int x, int z, long now) {
        if (level.getChunkSource().getChunkNow((x - 1) >> 4, (z - 1) >> 4) == null || level.getChunkSource().getChunkNow((x + 1) >> 4, (z + 1) >> 4) == null) return 0;
        int stage = BiosphereCycle.groveStage(gr.phase(), now);
        int baseY = g.floorY + g.groundAt(gr.u(), gr.v());
        if (idOf(level.getBlockState(new BlockPos(x, baseY + GoldenFlora.GROVE_LOW, z))).equals(GoldenFlora.groveMarker(stage))) return 0;
        AABB box = new AABB(x - 1, baseY + GoldenFlora.GROVE_LOW, z - 1, x + 2, baseY + GoldenFlora.GROVE_HIGH + 1, z + 2);
        if (!level.getEntitiesOfClass(LivingEntity.class, box).isEmpty()) return 0;
        BlockState air = Blocks.AIR.defaultBlockState();
        int written = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = GoldenFlora.GROVE_LOW; dy <= GoldenFlora.GROVE_HIGH; dy++) {
                    BlockPos pos = new BlockPos(x + dx, baseY + dy, z + dz);
                    BlockState cur = level.getBlockState(pos);
                    if (!BiosphereCycle.GROVE_BLOCKS.contains(idOf(cur))) continue;
                    String[] want = GoldenFlora.groveCell(stage, dx, dy, dz);
                    BlockState target = want == null ? air : NexusStates.of(want[0], want[1]);
                    if (cur != target) {
                        level.setBlock(pos, target, NexusSink.FLAGS);
                        written++;
                    }
                }
            }
        }
        return written;
    }

    /** The waterfalls throw spray where they land, for whoever is within earshot. */
    private static void ambience(ServerLevel level, GoldenGrid g, List<ServerPlayer> players) {
        for (GoldenGrid.Fall w : g.falls) {
            double x = g.cx + w.u() + 0.5, z = g.cz + w.v() + 0.5;
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(x, p.getY(), z) < 48.0 * 48.0) {
                    level.sendParticles(ParticleTypes.SPLASH, x, g.floorY + 1.3, z, 10, 0.5, 0.1, 0.5, 0.1);
                    break;
                }
            }
        }
    }
}
