package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the pure model speaks in {@link BlockKind}s; this is the one place where a kind becomes a real {@link BlockState}, from the string table of
 * {@link KindBlocks}. The states are built once, lazily (the block registry is frozen by the time a chunk is generated, not when this class loads), and then
 * read by every generator thread. A block id that does not exist (a typo, or the mod's own gate block missing in a stripped build) never crashes generation:
 * that kind becomes air and ONE warning names it. Chunk generation places blocks without neighbour updates, so the three railing blocks, whose shape
 * depends on their neighbours (a fence's four arms, a wall's four sides, a chain's axis), get their properties computed here from the model.
 */
public final class MacroBlocks {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static volatile BlockState[] states;

    private MacroBlocks() {}

    private static BlockState[] table() {
        BlockState[] t = states;
        if (t == null) {
            synchronized (MacroBlocks.class) {
                t = states;
                if (t == null) {
                    BlockKind[] kinds = BlockKind.values();
                    t = new BlockState[kinds.length];
                    for (BlockKind k : kinds) t[k.ordinal()] = resolve(KindBlocks.of(k));
                    states = t;
                }
            }
        }
        return t;
    }

    /** Forgets the built table (a test that changes the registry; the game never needs it). */
    static void reset() { states = null; }

    private static BlockState resolve(KindBlocks.Spec spec) {
        ResourceLocation id = ResourceLocation.tryParse(spec.block());
        Optional<Block> block = id == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty()) {
            warnOnce("block:" + spec.block(), "the block " + spec.block() + " does not exist in this game; the Hub draws air there");
            return Blocks.AIR.defaultBlockState();
        }
        BlockState s = block.get().defaultBlockState();
        for (String p : spec.props()) s = with(s, p);
        return s;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String nameValue) {
        int eq = nameValue.indexOf('=');
        if (eq <= 0) return s;
        String name = nameValue.substring(0, eq), value = nameValue.substring(eq + 1);
        Property<?> prop = s.getBlock().getStateDefinition().getProperty(name);
        if (prop == null) {
            warnOnce("prop:" + s.getBlock() + name, "the block " + BuiltInRegistries.BLOCK.getKey(s.getBlock()) + " has no property " + name + "; left at its default");
            return s;
        }
        Optional<?> v = prop.getValue(value);
        if (v.isEmpty()) {
            warnOnce("val:" + s.getBlock() + name + value, "the property " + name + " of " + BuiltInRegistries.BLOCK.getKey(s.getBlock()) + " has no value " + value + "; left at its default");
            return s;
        }
        return s.setValue((Property) prop, (Comparable) v.get());
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) LOG.warn("[nusmp] Sanctuary generator: {}", message);
    }

    /** The state of a kind with default railing connections (no neighbour information). */
    public static BlockState state(BlockKind kind) { return table()[kind.ordinal()]; }

    /** Is this one of the kinds whose state depends on the neighbours? */
    public static boolean needsNeighbours(BlockKind kind) { return kind == BlockKind.RAIL_DARK || kind == BlockKind.RAIL_LIGHT || kind == BlockKind.RAIL_CHAIN; }

    /** The state of a kind at a position, with the connections of the railing blocks worked out from the model's neighbours. */
    public static BlockState stateAt(HubModel model, BlockKind kind, int x, int y, int z) {
        if (!needsNeighbours(kind)) return state(kind);
        boolean n = KindBlocks.joinsRail(model.kindAt(x, y, z - 1)), s = KindBlocks.joinsRail(model.kindAt(x, y, z + 1));
        boolean e = KindBlocks.joinsRail(model.kindAt(x + 1, y, z)), w = KindBlocks.joinsRail(model.kindAt(x - 1, y, z));
        BlockState base = state(kind);
        switch (kind) {
            case RAIL_DARK -> {
                for (String p : KindBlocks.fenceProps(n, e, s, w)) base = with(base, p);
                return base;
            }
            case RAIL_LIGHT -> {
                List<String> props = KindBlocks.wallProps(n, e, s, w, model.kindAt(x, y + 1, z).solid);
                for (String p : props) base = with(base, p);
                return base;
            }
            default -> {
                return with(base, "axis=" + KindBlocks.chainAxis(e, w, n, s));
            }
        }
    }
}
