package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the table that turns a {@link BlockKind} into a block of the game is plain strings, and the game only complains about a wrong one when a chunk is
 * generated (a missing block becomes air, a wrong property is ignored with a log line), which nobody would see until the Hub looked wrong. So every row is
 * checked here against the game's own block-state table for 1.21.1 ({@code tools/labyrinth/vanilla_block_states_1_21_1.json}, from the 1.21.1 data): the block exists,
 * every property named exists on it, every value is one the game allows. The same test pins the vocabulary rules (only one block of this mod, no lava, no flowing
 * water) and the connection rules of the three railing blocks.
 */
public class KindBlocksTest {
    private static final Path TABLE = Path.of("tools/labyrinth/vanilla_block_states_1_21_1.json");

    /** block name (no namespace) -> property -> allowed values, read from the flat JSON the mega tools use. */
    private static Map<String, Map<String, Set<String>>> vanilla() throws IOException {
        Map<String, Map<String, Set<String>>> out = new HashMap<>();
        Pattern line = Pattern.compile("^\\s*\"(\\w+)\"\\s*:\\s*\\{(.*)\\}\\s*,?\\s*$");
        Pattern prop = Pattern.compile("\"(\\w+)\"\\s*:\\s*\\[([^\\]]*)\\]");
        for (String l : Files.readAllLines(TABLE)) {
            Matcher m = line.matcher(l);
            if (!m.matches()) continue;
            Map<String, Set<String>> props = new HashMap<>();
            Matcher pm = prop.matcher(m.group(2));
            while (pm.find()) {
                Set<String> values = new HashSet<>();
                for (String v : pm.group(2).split(",")) values.add(v.trim().replace("\"", ""));
                props.put(pm.group(1), values);
            }
            out.put(m.group(1), props);
        }
        return out;
    }

    @Test
    public void everyRowIsARealBlockWithRealPropertiesAndValues() throws IOException {
        Map<String, Map<String, Set<String>>> table = vanilla();
        assertTrue(table.size() > 1000, "the vanilla table was read: " + table.size() + " blocks");
        for (BlockKind k : BlockKind.values()) {
            KindBlocks.Spec spec = KindBlocks.of(k);
            if (spec.block().equals(KindBlocks.GATE_ID)) {
                assertEquals(BlockKind.GATE, k, "the mod's own block is only ever the gate");
                continue;
            }
            assertTrue(spec.block().startsWith("minecraft:"), "kind " + k + " uses " + spec.block());
            Map<String, Set<String>> props = table.get(spec.block().substring("minecraft:".length()));
            assertTrue(props != null, "kind " + k + " names a block that does not exist in 1.21.1: " + spec.block());
            for (String nv : spec.props()) {
                String[] kv = nv.split("=");
                assertTrue(props.containsKey(kv[0]), spec.block() + " has no property " + kv[0] + " (kind " + k + ")");
                assertTrue(props.get(kv[0]).contains(kv[1]), spec.block() + " does not allow " + nv + " (kind " + k + "), only " + props.get(kv[0]));
            }
        }
    }

    @Test
    public void theVocabularyIsClosedAndSafe() {
        int ours = 0;
        for (BlockKind k : BlockKind.values()) {
            String id = KindBlocks.of(k).block();
            assertFalse(id.contains("lava") || id.contains("fire") || id.contains("tnt") || id.contains("magma") || id.contains("cactus") || id.contains("spawner") || id.contains("barrier"), "nothing dangerous: " + id);
            if (!id.startsWith("minecraft:")) ours++;
        }
        assertEquals(1, ours, "the only block of this mod in the Hub's vocabulary is the Labyrinth Gate");
        assertEquals("minecraft:water", KindBlocks.of(BlockKind.WATER).block());
        assertTrue(KindBlocks.of(BlockKind.WATER).props().length == 1 && KindBlocks.of(BlockKind.WATER).props()[0].equals("level=0"), "water is a source block");
        assertFalse(KindBlocks.of(BlockKind.GOLD_TRIM).block().contains("gold"), "the Spire's bands are not real gold: a buildable world must not have a gold mine in its wall");
    }

    @Test
    public void everyKindHasItsFlagsRightForTheWalkabilitySearch() {
        assertTrue(BlockKind.AIR.passable && BlockKind.GATE.passable && BlockKind.MOSS_CARPET.passable && BlockKind.GLOW_LICHEN.passable);
        assertFalse(BlockKind.RAIL_LIGHT.passable || BlockKind.RAIL_DARK.passable || BlockKind.RAIL_CHAIN.passable || BlockKind.WATER.passable || BlockKind.POINTED_DRIPSTONE_UP.passable);
        assertTrue(BlockKind.SMOOTH_QUARTZ_SLAB.slab && BlockKind.RAMP_SLAB.slab && BlockKind.QUARTZ_SLAB.slab);
        assertTrue(BlockKind.SMOOTH_QUARTZ_SLAB.supportsStanding() && BlockKind.RAMP_BLOCK.supportsStanding());
        assertFalse(BlockKind.RAIL_DARK.supportsStanding() || BlockKind.LANTERN.supportsStanding() || BlockKind.AIR.supportsStanding());
        assertTrue(BlockKind.RAIL_CHAIN.isRail() && !BlockKind.SOUL_LANTERN.isRail());
        assertTrue(BlockKind.QUARTZ_STAIR_E.isStair() && !BlockKind.QUARTZ_SLAB.isStair());
    }

    @Test
    public void railingConnectionsFollowTheNeighbours() {
        assertEquals(List.of("north=true", "east=false", "south=true", "west=false"), KindBlocks.fenceProps(true, false, true, false));
        assertEquals(List.of("north=low", "east=none", "south=low", "west=none", "up=false"), KindBlocks.wallProps(true, false, true, false, false), "a straight wall has no post");
        assertEquals(List.of("north=low", "east=none", "south=low", "west=none", "up=true"), KindBlocks.wallProps(true, false, true, false, true), "unless something stands on it");
        assertEquals("up=true", KindBlocks.wallProps(true, true, false, false, false).get(4), "a corner has its post");
        assertEquals("up=true", KindBlocks.wallProps(false, false, false, false, false).get(4), "a lone wall is a post");
        assertEquals("x", KindBlocks.chainAxis(true, false, false, false));
        assertEquals("z", KindBlocks.chainAxis(false, false, true, false));
        assertEquals("x", KindBlocks.chainAxis(false, false, false, false));
        assertTrue(KindBlocks.joinsRail(BlockKind.RAIL_DARK) && KindBlocks.joinsRail(BlockKind.QUARTZ_BRICKS) && KindBlocks.joinsRail(BlockKind.POST));
        assertFalse(KindBlocks.joinsRail(BlockKind.AIR) || KindBlocks.joinsRail(BlockKind.LANTERN) || KindBlocks.joinsRail(BlockKind.GATE));
    }
}
