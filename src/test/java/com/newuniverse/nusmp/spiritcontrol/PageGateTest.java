package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the grimoire's spirit pages are appended at the END of every book and shown only to a player a spirit answers, so the gating is pinned as pure data: six pages in a fixed order with unique ids, the two
 * toggles act on the press, an ability page shows only while the spirit has that many abilities, nothing shows without a spirit, and the labels read as the doc says.
 */
class PageGateTest {
    @Test
    void sixPagesInAFixedOrderWithUniqueIds() {
        List<String> ids = PageGate.pageIds();
        assertEquals(List.of("spirit_call", "spirit_combat", "spirit_ability_1", "spirit_ability_2", "spirit_ability_3", "spirit_ability_4"), ids);
        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (String id : ids) assertTrue(PageGate.isPage(id), id);
    }

    @Test
    void theNumbersOfAbilityPages() {
        assertEquals(0, PageGate.abilityIndex("spirit_ability_1"));
        assertEquals(3, PageGate.abilityIndex("spirit_ability_4"));
        assertEquals(-1, PageGate.abilityIndex("spirit_ability_5"));
        assertEquals(-1, PageGate.abilityIndex("spirit_ability_0"));
        assertEquals(-1, PageGate.abilityIndex("spirit_ability_12"));
        assertEquals(-1, PageGate.abilityIndex("spirit_call"));
        assertEquals(-1, PageGate.abilityIndex(null));
        for (int i = 0; i < PageGate.MAX_ABILITIES; i++) assertEquals(i, PageGate.abilityIndex(PageGate.abilityId(i)));
        assertFalse(PageGate.isPage("spirit_dive"), "the old Spirit Dive page is not ours");
        assertFalse(PageGate.isPage("spirit_channeling"));
    }

    @Test
    void onlyTheTwoTogglesActOnThePress() {
        assertTrue(PageGate.isInstant(PageGate.CALL));
        assertTrue(PageGate.isInstant(PageGate.COMBAT));
        for (int i = 0; i < PageGate.MAX_ABILITIES; i++) assertFalse(PageGate.isInstant(PageGate.abilityId(i)));
    }

    @Test
    void aPageIsShownOnlyWhileASpiritAnswersAndAnAbilityPageOnlyWhileTheSpiritHasThatAbility() {
        for (String id : PageGate.pageIds()) assertFalse(PageGate.visible(id, 0, 4), "no spirit answers: " + id + " is hidden");
        assertTrue(PageGate.visible(PageGate.CALL, 1, 0));
        assertTrue(PageGate.visible(PageGate.COMBAT, 2, 0));
        assertFalse(PageGate.visible(PageGate.abilityId(0), 1, 0), "a spirit with no ability shows no ability page");
        assertTrue(PageGate.visible(PageGate.abilityId(0), 1, 1));
        assertFalse(PageGate.visible(PageGate.abilityId(1), 1, 1));
        Set<String> shownForSpace = new HashSet<>();
        for (String id : PageGate.pageIds()) if (PageGate.visible(id, 1, 4)) shownForSpace.add(id);
        assertEquals(6, shownForSpace.size(), "the Space spirit with four abilities shows all six");
        int two = 0;
        for (String id : PageGate.pageIds()) if (PageGate.visible(id, 1, 2)) two++;
        assertEquals(4, two, "a spirit with two abilities: two toggles and two ability pages");
        assertFalse(PageGate.visible("mana_strike", 1, 4), "a page that is not ours is never shown by this gate");
    }

    @Test
    void anAvatarNeverCastsASpiritPage() {
        for (String id : PageGate.pageIds()) {
            assertEquals(com.newuniverse.nusmp.avatar.cast.AvatarSkillList.Kind.NONE, com.newuniverse.nusmp.avatar.cast.AvatarSkillList.kind(id, 0), id);
            assertFalse(com.newuniverse.nusmp.avatar.cast.AvatarSkillList.allowedPage(id, 3.0), id);
        }
    }

    @Test
    void theLabels() {
        assertEquals("Summon Sylph", PageGate.callLabel("Sylph", false));
        assertEquals("Release Sylph", PageGate.callLabel("Sylph", true));
        assertEquals("Spirit Combat Mode: ON", PageGate.combatLabel(true));
        assertEquals("Spirit Combat Mode: OFF", PageGate.combatLabel(false));
    }
}
