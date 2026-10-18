package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the registry decides which of two sets every caller sees, and getting that wrong has two ugly faces: a client on a remote server showing the integrated server's old trees, or a
 * single-player client lagging behind its own server's reload. These tests pin the rule (the server's snapshot is in force while a server runs in this JVM, the client's copy otherwise, and
 * clearing each side returns to the other), the lookups through it, and the two helpers the abilities use to read their numbers without a null check. The registry is static state, so every test
 * ends by clearing both sides: a test that leaked a set would change the answer of the next one.
 */
class RouteRegistryTest {
    private static RouteIndex serverSet() {
        Map<String, JsonElement> files = new LinkedHashMap<>();
        files.put(RouteSchemaTest.ID, JsonParser.parseString(RouteSchemaTest.GOOD));
        return RouteIndex.build(files, List.of());
    }

    private static List<SkillRoute> clientRoutes() {
        SkillRoute r = RouteSchema.parse("nusmp:ice_make", "{\"name\":\"Ice Make\",\"book\":\"nusmp:ice_book\",\"class_id\":\"ice_make\",\"color\":\"#66CCFF\",\"nodes\":[{\"id\":\"static_ice\"}]}").route();
        assertNotNull(r);
        return List.of(r);
    }

    private static void reset() {
        RouteRegistry.clear();
        RouteRegistry.clearClient();
    }

    @Test
    void nothingIsLoadedToStartWith() {
        reset();
        assertTrue(RouteRegistry.all().isEmpty());
        assertNull(RouteRegistry.byId(RouteSchemaTest.ID));
        assertNull(RouteRegistry.byBook("nusmp:fire_dragon_slayer_book"));
        assertNull(RouteRegistry.byClass("fire_dragon_slayer"));
        assertNull(RouteRegistry.bySkill("nusmp:ft_fire_dragons_roar"));
        assertNull(RouteRegistry.node(RouteSchemaTest.ID, "roar"));
        assertTrue(RouteRegistry.report().isEmpty());
    }

    @Test
    void theServerSetIsInForceWhileAServerRunsAndTheClientCopyOtherwise() {
        reset();
        try {
            RouteRegistry.setClient(clientRoutes());
            assertEquals(List.of("nusmp:ice_make"), RouteRegistry.all().stream().map(SkillRoute::id).toList(), "a client on a remote server sees what the server sent");

            RouteIndex server = serverSet();
            RouteRegistry.publish(server);
            assertSame(server, RouteRegistry.snapshot(), "once a server runs in this JVM its set is the one in force, even though a client copy exists");
            assertEquals(List.of(RouteSchemaTest.ID), RouteRegistry.all().stream().map(SkillRoute::id).toList());
            assertNotNull(RouteRegistry.byBook("nusmp:fire_dragon_slayer_book"));
            assertNull(RouteRegistry.byId("nusmp:ice_make"), "the client copy is hidden while the server's set is in force");

            RouteRegistry.clear();
            assertEquals(List.of("nusmp:ice_make"), RouteRegistry.all().stream().map(SkillRoute::id).toList(), "after the server stops the client copy is what is left");
            RouteRegistry.clearClient();
            assertTrue(RouteRegistry.all().isEmpty(), "and after leaving the world, nothing");
        } finally {
            reset();
        }
    }

    @Test
    void aServerWithNoRoutesStillHidesAStaleClientCopy() {
        reset();
        try {
            RouteRegistry.setClient(clientRoutes());
            RouteRegistry.publish(RouteIndex.EMPTY);
            assertTrue(RouteRegistry.all().isEmpty(), "a loaded but empty server set is not the same as no server: the stale copy must not show through");
            RouteRegistry.publish(null);
            assertTrue(RouteRegistry.all().isEmpty(), "publishing null is an empty set");
        } finally {
            reset();
        }
    }

    @Test
    void aReloadReplacesTheWholeSetInOneStep() {
        reset();
        try {
            RouteIndex first = serverSet();
            RouteRegistry.publish(first);
            List<SkillRoute> seen = RouteRegistry.all();
            RouteRegistry.publish(RouteIndex.EMPTY);
            assertEquals(1, seen.size(), "a list a caller already holds is not changed by the reload");
            assertTrue(RouteRegistry.all().isEmpty());
        } finally {
            reset();
        }
    }

    @Test
    void theHelpersReadNodesAndNumbersAndFallBackInsteadOfThrowing() {
        reset();
        try {
            RouteRegistry.publish(serverSet());
            assertEquals("roar", RouteRegistry.node(RouteSchemaTest.ID, "roar").id());
            assertNull(RouteRegistry.node(RouteSchemaTest.ID, "ghost"));
            assertNull(RouteRegistry.node("nusmp:ghost", "roar"));
            assertEquals(13.0, RouteRegistry.value(RouteSchemaTest.ID, "roar", "damage", 2, -1), 1e-9);
            assertEquals(16.0, RouteRegistry.value(RouteSchemaTest.ID, "roar", "damage", 9, -1), 1e-9, "past the table the last entry repeats");
            assertEquals(7.5, RouteRegistry.value(RouteSchemaTest.ID, "roar", "no_such_table", 1, 7.5), 1e-9, "an unknown table answers the fallback");
            assertEquals(7.5, RouteRegistry.value(RouteSchemaTest.ID, "ghost", "damage", 1, 7.5), 1e-9, "an unknown node answers the fallback");
            assertEquals(7.5, RouteRegistry.value("nusmp:ghost", "roar", "damage", 1, 7.5), 1e-9, "an unknown route answers the fallback");
            assertEquals(7.5, RouteRegistry.value(null, null, "damage", 1, 7.5), 1e-9);
        } finally {
            reset();
        }
    }
}
