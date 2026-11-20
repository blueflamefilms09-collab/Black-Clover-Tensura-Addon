package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.hub.HubNodes.Facts;
import com.newuniverse.nusmp.client.hub.HubNodes.Tab;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the enum order is the order on the ring and is append-only (rule 2): a test pins the first eight so a reorder or a removal fails the build, and checks the names and lock rules stay sane. Pure.
 */
class HubNodesTest {
    @Test
    void theFirstEightTabsKeepTheirOrder() {
        String[] expected = {"STATUS", "WEAPONS", "SKILLS", "QUESTS", "ATTUNEMENT", "MULTIVERSE", "CODEX", "SETTINGS"};
        assertTrue(Tab.values().length >= expected.length, "a tab was removed");
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], Tab.values()[i].name(), "tab " + i + " moved");
        assertEquals(Tab.values().length, HubNodes.COUNT);
    }

    @Test
    void keysAndGlyphsAreUniqueAndLangSafe() {
        Set<String> keys = new HashSet<>(), glyphs = new HashSet<>();
        for (Tab t : Tab.values()) {
            assertTrue(t.key.matches("[a-z_]+"), t.key);
            assertTrue(keys.add(t.key), "duplicate key " + t.key);
            assertTrue(glyphs.add(t.glyph) && t.glyph.length() == 2, "glyph " + t.glyph);
            assertTrue(t.rgb >= 0 && t.rgb <= 0xFFFFFF);
        }
    }

    @Test
    void onlySkillsAndAttunementStillOpenOldScreens() {
        // Hub v2: the Codex is a native page of the hub (HubCodexBody), no longer a launcher of the old book
        assertTrue(Tab.SKILLS.launcher && Tab.ATTUNEMENT.launcher);
        assertFalse(Tab.CODEX.launcher, "the Codex is drawn by the hub");
        assertFalse(Tab.STATUS.launcher || Tab.WEAPONS.launcher || Tab.QUESTS.launcher || Tab.MULTIVERSE.launcher || Tab.SETTINGS.launcher);
    }

    @Test
    void lockRulesUseOnlyWhatTheClientKnows() {
        assertFalse(HubNodes.locked(Tab.WEAPONS, Facts.NONE), "before the server answered the page is not locked");
        assertTrue(HubNodes.locked(Tab.WEAPONS, new Facts(true, 0, 3, true)));
        assertFalse(HubNodes.locked(Tab.WEAPONS, new Facts(true, 2, 0, false)));
        assertTrue(HubNodes.locked(Tab.QUESTS, Facts.NONE));
        assertFalse(HubNodes.locked(Tab.QUESTS, new Facts(false, 0, 1, false)));
        assertFalse(HubNodes.locked(Tab.QUESTS, new Facts(false, 0, 0, true)));
        for (Tab t : Tab.values()) if (t != Tab.WEAPONS && t != Tab.QUESTS) assertFalse(HubNodes.locked(t, Facts.NONE), t + " must never be locked");
    }

    @Test
    void atWrapsAround() {
        assertEquals(Tab.STATUS, HubNodes.at(0));
        assertEquals(Tab.STATUS, HubNodes.at(HubNodes.COUNT));
        assertEquals(Tab.values()[HubNodes.COUNT - 1], HubNodes.at(-1));
    }
}
