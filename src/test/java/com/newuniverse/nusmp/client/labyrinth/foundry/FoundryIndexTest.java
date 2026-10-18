package com.newuniverse.nusmp.client.labyrinth.foundry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the asset index is the ONLY thing a client knows about the 400 foundry mobs (it never sees the data packs), so its parser must read the shipped file and survive a broken
 * one: bad ids and bad animation names are skipped, sizes are clamped, a file that is not JSON gives an empty table and never an exception.
 */
public class FoundryIndexTest {
    @Test
    public void parsesTheShape() {
        Map<String, FoundryIndex.Entry> m = FoundryIndex.parse("{\"mobs\":{\"tunnel_kobold\":{\"anim\":\"small__dagger-none__auto-auto-auto\",\"glow\":16711680,\"size\":1.2,\"height\":1.5,\"tier\":2}}}");
        FoundryIndex.Entry e = m.get("tunnel_kobold");
        assertEquals("small__dagger-none__auto-auto-auto", e.anim());
        assertEquals(0xFF0000, e.glow());
        assertEquals(1.2f, e.size(), 1e-6);
        assertEquals(2, e.tier());
        assertEquals(1.5f, e.height(), 1e-6);
    }

    @Test
    public void skipsAndClampsWhatIsBroken() {
        Map<String, FoundryIndex.Entry> m = FoundryIndex.parse("{\"mobs\":{\"Bad Id\":{\"anim\":\"ok_anim\"},\"no_anim\":{\"glow\":1},\"weird\":{\"anim\":\"../../etc\"},\"big\":{\"anim\":\"ok-anim\",\"size\":99,\"tier\":9}}}");
        assertEquals(1, m.size());
        assertEquals(2.5f, m.get("big").size(), 1e-6);
        assertEquals(5, m.get("big").tier());
        assertTrue(FoundryIndex.parse("not json").isEmpty());
        assertTrue(FoundryIndex.parse("{\"mobs\":3}").isEmpty());
        FoundryIndex.install(m);
        assertNull(FoundryIndex.get("nothing"));
        assertNull(FoundryIndex.get(null));
        assertEquals(1, FoundryIndex.size());
    }

    @Test
    public void readsTheShippedIndex() throws Exception {
        String text = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/nusmp/foundry/index.json"));
        Map<String, FoundryIndex.Entry> m = FoundryIndex.parse(text);
        assertTrue(m.size() >= 8);
        for (Map.Entry<String, FoundryIndex.Entry> e : m.entrySet()) {
            assertTrue(e.getValue().height() > 0.5f, e.getKey());
            assertTrue(e.getValue().size() >= 0.6f, e.getKey());
        }
    }
}
