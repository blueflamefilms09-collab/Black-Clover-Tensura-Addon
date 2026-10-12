package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The words the Nexus says to the server, and when it says them. */
class NexusTextTest {
    @Test
    void theSealBroadcastIsTheOwnersLineThenWhoWalksWhat() {
        assertEquals("An immense Mana presence is felt as a Spatial Nexus seal is breached. Steve walks the Monarch's Vault.",
                NexusText.sealBreached("Steve", NexusZone.MONARCH_VAULT.label));
        assertEquals("An immense Mana presence is felt as a Spatial Nexus seal is breached. Alex walks the Golden City.", NexusText.sealBreached("Alex", NexusZone.GOLDEN_CITY.label));
        for (NexusZone z : NexusZone.values()) {
            String text = NexusText.sealBreached("Someone", z.label);
            assertTrue(text.startsWith(NexusText.SEAL_LINE + " Someone walks the "), text);
            assertFalse(text.contains("the The"), "no doubled article: " + text);
            assertTrue(text.endsWith(z.label.substring(4) + "."), text);
        }
    }

    @Test
    void anArticleIsAddedOnlyOnce() {
        assertEquals("the Underworld Gate", NexusText.the("The Underworld Gate"));
        assertEquals("the Variable Core", NexusText.the("Variable Core"));
        assertEquals("the Variable Core", NexusText.the(""));
        assertEquals("the Variable Core", NexusText.the(null));
        assertEquals("the Tempest Mirage", NexusText.the("The Tempest Mirage"));
    }

    @Test
    void theNexusStaysQuietOnlyWhenTheOlderFlowAnnouncedTheFirstEntrance() {
        assertTrue(NexusText.announceSeal(false, false, 0), "a fresh world, the flag not set: the Nexus announces and sets the flag");
        assertFalse(NexusText.announceSeal(false, true, 0), "a fresh world, the flag set and no Nexus seal yet: the older flow just announced it");
        assertTrue(NexusText.announceSeal(false, true, 1), "every later seal is announced");
        assertTrue(NexusText.announceSeal(false, false, 4));
        assertTrue(NexusText.announceSeal(true, true, 0), "a world from before the Nexus: every new seal is announced");
        assertTrue(NexusText.announceSeal(true, true, 3));
    }

    @Test
    void zoneIdsAreLowerCaseNames() {
        assertEquals("monarch_vault", NexusText.id(NexusZone.MONARCH_VAULT));
        assertEquals("golden_city", NexusText.id(NexusZone.GOLDEN_CITY));
    }
}
