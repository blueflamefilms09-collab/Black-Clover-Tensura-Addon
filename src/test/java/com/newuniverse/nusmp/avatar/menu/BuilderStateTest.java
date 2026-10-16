package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The creation screen's page rules: what has to be done before each page can be left, and when Create is lit. */
class BuilderStateTest {
    @Test
    void youCannotLeaveTheStatsBeforeRolling() {
        BuilderState s = new BuilderState();
        assertFalse(s.canNext());
        assertFalse(s.next());
        s.onRolled();
        assertTrue(s.next());
        assertEquals(BuilderState.PAGE_RACE, s.page());
    }

    @Test
    void theRacePageNeedsAnAvailableRaceAndAPrimaryPersonality() {
        BuilderState s = new BuilderState();
        s.onRolled(); s.next();
        assertFalse(s.canNext());
        s.chooseRace("elf", false);
        s.choosePrimary(AvatarPersonality.LOYAL);
        assertFalse(s.canNext(), "a locked race cannot be taken");
        s.chooseRace("elf", true);
        assertTrue(s.canNext());
        s.chooseRace("elf", true); s.primary();
        s.choosePrimary(null);
        assertFalse(s.canNext());
    }

    @Test
    void theSecondaryNeverEqualsThePrimary() {
        BuilderState s = new BuilderState();
        s.choosePrimary(AvatarPersonality.TSUNDERE);
        s.chooseSecondary(AvatarPersonality.TSUNDERE);
        assertNull(s.secondary());
        s.chooseSecondary(AvatarPersonality.HEROIC);
        assertEquals("tsundere_heroic", s.hybridId());
        s.choosePrimary(AvatarPersonality.HEROIC);
        assertNull(s.secondary(), "taking the secondary as primary clears it");
        assertEquals("", s.hybridId());
    }

    @Test
    void createNeedsEverythingAndAValidName() {
        BuilderState s = ready();
        assertTrue(s.canCreate());
        s.setName("");
        assertTrue(s.canCreate(), "the name is optional");
        s.setName("A");
        assertFalse(s.canCreate());
        s.setName("Rimuru-sama's Pet 2");
        assertTrue(s.canCreate());
        s.setName("bad<>name");
        assertFalse(s.canCreate());
        s.setName("x".repeat(25));
        assertFalse(s.canCreate());
        s.setName("Okay");
        s.setBusy(true);
        assertFalse(s.canCreate(), "no second request while one is on its way");
    }

    @Test
    void tabsOnlyOpenPagesAlreadyEarned() {
        BuilderState s = new BuilderState();
        assertFalse(s.goTo(2));
        assertEquals(0, s.page());
        s = ready();
        assertEquals(BuilderState.PAGE_STATS, s.page());
        assertTrue(s.goTo(BuilderState.PAGE_CONFIRM));
        assertEquals(BuilderState.PAGE_CONFIRM, s.page());
        assertTrue(s.goTo(0));
        assertFalse(s.back());
        assertFalse(s.goTo(9));
        assertFalse(s.goTo(-1));
    }

    @Test
    void nothingMovesWhileBusy() {
        BuilderState s = ready();
        s.setBusy(true);
        assertFalse(s.next());
        assertFalse(s.goTo(2));
    }

    private static BuilderState ready() {
        BuilderState s = new BuilderState();
        s.onRolled();
        s.next();
        s.chooseRace("human", true);
        s.choosePrimary(AvatarPersonality.LOYAL);
        s.next(); s.next();
        s.goTo(0);
        return s;
    }
}
