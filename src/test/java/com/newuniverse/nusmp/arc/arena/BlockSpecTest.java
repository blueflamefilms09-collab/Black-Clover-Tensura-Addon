package com.newuniverse.nusmp.arc.arena;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every block of every arena is named by this text form, and a mistake in the parser would make the game place the wrong block (or none) on the first
 * arrival; these tests pin the forms the palettes use, the default namespace, the order of properties and what is refused.
 */
class BlockSpecTest {
    @Test
    void aPlainIdMeansMinecraft() {
        BlockSpec s = BlockSpec.parse("stone_bricks");
        assertEquals("minecraft", s.namespace());
        assertEquals("stone_bricks", s.path());
        assertTrue(s.properties().isEmpty());
        assertEquals("minecraft:stone_bricks", s.id());
        assertEquals("minecraft:stone_bricks", s.toString());
    }

    @Test
    void propertiesKeepTheirWrittenOrder() {
        BlockSpec s = BlockSpec.parse("minecraft:stone_brick_stairs[facing=north,half=bottom]");
        assertEquals(Map.of("facing", "north", "half", "bottom"), s.properties());
        assertEquals("minecraft:stone_brick_stairs[facing=north,half=bottom]", s.toString());
        assertEquals("minecraft:oak_leaves[persistent=true]", BlockSpec.parse(" oak_leaves[persistent=true] ").toString());
        assertEquals("nusmp:labyrinth_gate", BlockSpec.parse("nusmp:labyrinth_gate").id());
        assertEquals(0, BlockSpec.parse("lantern[]").properties().size());
    }

    @Test
    void textThatIsNotABlockIsRefused() {
        for (String bad : new String[] {"", "Stone", "stone[", "stone[a]", "stone[=1]", "stone[a=]", "stone[a=1,a=2]", "stone bricks", "minecraft:", ":stone"}) {
            boolean refused;
            try {
                BlockSpec.parse(bad);
                refused = false;
            } catch (IllegalArgumentException e) {
                refused = true;
            }
            assertTrue(refused, "'" + bad + "' must not parse");
        }
        boolean nullRefused;
        try {
            BlockSpec.parse(null);
            nullRefused = false;
        } catch (IllegalArgumentException e) {
            nullRefused = true;
        }
        assertTrue(nullRefused);
    }
}
