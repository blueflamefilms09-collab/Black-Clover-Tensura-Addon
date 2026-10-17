package com.newuniverse.nusmp.arc.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: a story arena is a flat void until somebody is carried to it, and its floor, walls, pillars and lights have to exist before the first traveller is set
 * down. The first arrival builds it here, once, from the pure formula of its {@link ArenaShape}: procedural building needs no structure file to ship and no
 * template to keep in step with the formula, and the formula is unit tested (stacks, support, sealing, the palette against the game's own table of states).
 * Blocks are placed inside {@link DungeonGuard#internal(Runnable)}, because the guard that makes the dimension unbreakable would otherwise refuse the builder's
 * own writes; the chunks are made first, so nothing is placed into a chunk that is not there. A column is written bottom to top, so a lantern finds its post
 * and a hanging one its ceiling by the time the neighbour updates run. {@link #repair} lays back anything missing (an operator's {@code /fill}, a bug, a formula
 * that changed with the build version), which is cheap and safe to call on every arrival. Vanilla blocks only: nothing here can be missing from a resource pack.
 */
public final class ArcArenaBuilder {
    private ArcArenaBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The block state a palette text means: the block, then each named property set (a name or value the game does not know is ignored, never thrown). A block that does not exist becomes stone, so a typo is visible and never a hole. */
    public static BlockState stateOf(String text) {
        BlockSpec spec = BlockSpec.parse(text);
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(spec.namespace(), spec.path())).orElse(Blocks.STONE);
        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> p : spec.properties().entrySet()) {
            Property<?> prop = block.getStateDefinition().getProperty(p.getKey());
            if (prop != null) state = withValue(state, prop, p.getValue());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(state);
    }

    /** The block state of every part of a shape, made once. */
    static Map<ArenaPart, BlockState> states(ArenaShape shape) {
        Map<ArenaPart, BlockState> out = new EnumMap<>(ArenaPart.class);
        for (Map.Entry<ArenaPart, String> e : shape.palette().entrySet()) out.put(e.getKey(), stateOf(e.getValue()));
        return out;
    }

    /** Builds the arena of {@code slot} if the save says it is not built (or was built by an older formula). Returns whether anything was built now. */
    public static boolean ensureBuilt(ServerLevel level, ArcArenaSave save, ArenaShape shape, int slot) {
        if (save.builtVersion(slot) >= ArcArenaSave.BUILD_VERSION) return false;
        place(level, shape, slot, save.builtVersion(slot) > 0);
        save.markBuilt(slot, ArcArenaSave.BUILD_VERSION);
        return true;
    }

    /** Lays back every block of the formula that is not what it should be. Returns how many were set. */
    public static int repair(ServerLevel level, ArenaShape shape, int slot) { return place(level, shape, slot, true); }

    private static int place(ServerLevel level, ArenaShape shape, int slot, boolean onlyMissing) {
        int R = shape.radius() + 1, cx = ArenaGrid.centreX(slot), cz = ArenaGrid.centreZ(slot);
        for (int x = (cx - R) >> 4; x <= (cx + R) >> 4; x++) for (int z = (cz - R) >> 4; z <= (cz + R) >> 4; z++) level.getChunk(x, z);       // generate and load first
        Map<ArenaPart, BlockState> states = states(shape);
        int[] set = {0};
        DungeonGuard.internal(() -> {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    for (ArenaCell c : shape.column(dx, dz)) {
                        BlockState want = states.get(c.part());
                        if (want == null) continue;
                        pos.set(cx + dx, ArenaShape.FLOOR_Y + c.dy(), cz + dz);
                        if (onlyMissing && level.getBlockState(pos).is(want.getBlock())) continue;
                        level.setBlock(pos, want, 2);
                        set[0]++;
                    }
                }
            }
        });
        LOG.info("[nusmp] The story arena {} (slot {}) {}: {} blocks set", shape.id(), slot, onlyMissing ? "repaired" : "built", set[0]);
        return set[0];
    }
}
