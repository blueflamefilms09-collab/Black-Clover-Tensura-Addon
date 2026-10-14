package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.blackclover.Devil;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The table of Spirits and Devils: complete, unique, and only verified ids. */
class SocketCatalogTest {
    /** Tensura skill ids checked against the Tensura id dump (scratchpad/tensura_ids/clean.json) when the table was written. */
    private static final Set<String> VERIFIED_SKILLS = Set.of("flame_attack_resistance", "water_attack_resistance", "wind_attack_resistance", "earth_attack_resistance",
            "light_attack_resistance", "darkness_attack_resistance", "gravity_attack_resistance", "spatial_attack_resistance", "cold_resistance");

    @Test
    void sixSpiritsAndTheNineDevilsOfTheMod() {
        assertEquals(6, SocketCatalog.spirits().size());
        assertEquals(9, SocketCatalog.devils().size());
        assertEquals(15, SocketCatalog.all().size());
        for (Devil d : Devil.values()) {
            SocketDef def = SocketCatalog.byId("devil:" + d.name().toLowerCase());
            assertNotNull(def, "devil " + d + " has a core");
            assertEquals(SocketKind.DEVIL, def.kind());
        }
        for (SocketElement e : SocketElement.values()) assertNotNull(SocketCatalog.byId("spirit:" + e.key()), "spirit " + e);
    }

    @Test
    void idsItemsAndActKeysAreUnique() {
        Set<String> ids = new HashSet<>(), items = new HashSet<>(), acts = new HashSet<>();
        for (SocketDef d : SocketCatalog.all()) {
            assertTrue(ids.add(d.id()), d.id());
            assertTrue(items.add(d.coreItemId()), d.coreItemId());
            assertTrue(acts.add(d.act().key()), d.act().key());
            assertTrue(d.coreItemId().matches("[a-z0-9_]+"));
            assertNotEquals(0, d.rgb() & 0xFFFFFF, "a black colour would read as no socket: " + d.id());
        }
    }

    @Test
    void everyBoonIsSane() {
        for (SocketDef d : SocketCatalog.all()) {
            assertFalse(d.boons().isEmpty(), d.id() + " gives something");
            assertTrue(d.act().cooldownTicks() >= 200 && d.act().manaPercent() > 0 && d.act().manaPercent() <= 10, d.id() + " act numbers");
            for (Boon b : d.boons()) {
                assertTrue(b.key().contains(":") || b.type() == Boon.Type.LIFESTEAL, b.key());
                if (b.type() == Boon.Type.SKILL) {
                    assertTrue(b.key().startsWith("tensura:") && VERIFIED_SKILLS.contains(b.key().substring(8)), "unverified skill " + b.key());
                }
                if (b.type() == Boon.Type.RESIST) assertTrue(b.amount() > 0 && b.amount() <= 0.9);
                if (b.type() == Boon.Type.LIFESTEAL) assertTrue(b.amount() > 0 && b.amount() <= 0.25);
                if (b.type() == Boon.Type.ATTR) assertTrue(b.amp() >= 0 && b.amp() <= 2);
            }
        }
    }

    @Test
    void lookupByKeyAndId() {
        assertSame(SocketCatalog.byId("spirit:fire"), SocketCatalog.byKey("fire"));
        assertSame(SocketCatalog.byId("devil:megicula"), SocketCatalog.byKey("megicula"));
        assertSame(SocketCatalog.byId("devil:megicula"), SocketCatalog.byKey("devil:megicula"));
        assertNull(SocketCatalog.byId("spirit:nothing"));
        assertNull(SocketCatalog.byKey(null));
        assertEquals("spirit_core_fire", SocketCatalog.byId("spirit:fire").coreItemId());
        assertEquals("devil_core_walgner", SocketCatalog.byId("devil:walgner").coreItemId());
    }

    @Test
    void theNamedAbilitiesExist() {
        assertEquals("Hellflare", SocketCatalog.byId("spirit:fire").act().name());
        assertEquals("Curse-Warding Magic", SocketCatalog.byId("devil:megicula").act().name());
        assertTrue(SocketCatalog.byId("devil:walgner").boons().stream().anyMatch(b -> b.type() == Boon.Type.LIFESTEAL));
    }

    @Test
    void optInList() {
        assertFalse(SocketCatalog.isOptIn("some_curio_relic"));
        SocketCatalog.allowSocket("some_curio_relic");
        assertTrue(SocketCatalog.isOptIn("some_curio_relic"));
        assertFalse(SocketCatalog.isOptIn(null));
    }

    @Test
    void socketDataIsImmutableAndBounded() {
        SocketData a = SocketData.EMPTY.with(new SocketData.Entry("spirit:fire", 3L));
        assertEquals(0, SocketData.EMPTY.size());
        assertTrue(a.has("spirit:fire"));
        assertEquals(0, a.without(0).size());
        assertEquals(1, a.without(5).size());
        assertEquals(1, a.without(-1).size());
        assertThrows(UnsupportedOperationException.class, () -> a.entries().add(new SocketData.Entry("x", 0L)));
    }
}
