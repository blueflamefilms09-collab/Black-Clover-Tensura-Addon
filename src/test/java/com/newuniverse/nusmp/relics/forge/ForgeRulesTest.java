package com.newuniverse.nusmp.relics.forge;

import com.newuniverse.nusmp.relics.forge.ForgeRules.Button;
import com.newuniverse.nusmp.relics.forge.ForgeRules.CoreKind;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Msg;
import com.newuniverse.nusmp.relics.forge.ForgeRules.State;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The decisions of the Relic Forge table: slot acceptance, button availability, refusals and the engraving cap. */
class ForgeRulesTest {
    private static State relic(boolean atTop, int cats, int used, int total, CoreKind core) {
        return new State(Subject.RELIC, atTop, cats, used, total, core, 0, 0, 0, false);
    }

    @Test
    void slotsTakeOnlyWhatTheyShould() {
        assertTrue(ForgeRules.acceptsSubject(true, false));
        assertTrue(ForgeRules.acceptsSubject(false, true), "a mod weapon that is not a relic still fits (engraving)");
        assertFalse(ForgeRules.acceptsSubject(false, false));
        assertTrue(ForgeRules.acceptsCatalyst(true));
        assertFalse(ForgeRules.acceptsCatalyst(false));
        assertTrue(ForgeRules.acceptsCore(CoreKind.SOCKET_CORE));
        assertTrue(ForgeRules.acceptsCore(CoreKind.EMPTY_CORE));
        assertTrue(ForgeRules.acceptsCore(CoreKind.ENGRAVING_BOOK));
        assertFalse(ForgeRules.acceptsCore(CoreKind.OTHER));
        assertFalse(ForgeRules.acceptsCore(CoreKind.NONE));
    }

    @Test
    void buttonIdsRoundTripAndUnknownIdsAreNull() {
        for (Button b : Button.values()) assertSame(b, Button.of(b.id()));
        assertNull(Button.of(-1));
        assertNull(Button.of(Button.values().length));
        assertEquals(5, Button.values().length, "UPGRADE, SOCKET, DRAW_OUT, CYCLE, ENGRAVE and nothing else");
    }

    @Test
    void upgradeNeedsARelicBelowTheTopWithACatalyst() {
        assertEquals(Msg.NO_ITEM, ForgeRules.check(Button.UPGRADE, new State(Subject.NONE, false, 1, 0, 0, CoreKind.NONE, 0, 0, 0, false)));
        assertEquals(Msg.NOT_ALLOWED, ForgeRules.check(Button.UPGRADE, new State(Subject.WEAPON, false, 1, 0, 0, CoreKind.NONE, 0, 0, 0, true)));
        assertEquals(Msg.AT_TOP, ForgeRules.check(Button.UPGRADE, relic(true, 1, 0, 0, CoreKind.NONE)));
        assertEquals(Msg.NO_CATALYST, ForgeRules.check(Button.UPGRADE, relic(false, 0, 0, 0, CoreKind.NONE)));
        assertEquals(Msg.OK, ForgeRules.check(Button.UPGRADE, relic(false, 2, 0, 0, CoreKind.NONE)));
    }

    @Test
    void socketRefusalsComeInTheOrderThePlayerShouldFixThem() {
        assertEquals(Msg.NO_SOCKETS, ForgeRules.check(Button.SOCKET, relic(false, 0, 0, 0, CoreKind.SOCKET_CORE)));
        assertEquals(Msg.NEED_SOCKET_CORE, ForgeRules.check(Button.SOCKET, relic(false, 0, 0, 2, CoreKind.NONE)));
        assertEquals(Msg.NEED_SOCKET_CORE, ForgeRules.check(Button.SOCKET, relic(false, 0, 0, 2, CoreKind.EMPTY_CORE)));
        assertEquals(Msg.SOCKETS_FULL, ForgeRules.check(Button.SOCKET, relic(false, 0, 2, 2, CoreKind.SOCKET_CORE)));
        assertEquals(Msg.OK, ForgeRules.check(Button.SOCKET, relic(false, 0, 1, 2, CoreKind.SOCKET_CORE)));
    }

    @Test
    void drawOutNeedsTheEmptyCoreAndSomethingBound() {
        assertEquals(Msg.NEED_EMPTY_CORE, ForgeRules.check(Button.DRAW_OUT, relic(false, 0, 1, 2, CoreKind.SOCKET_CORE)));
        assertEquals(Msg.NOTHING_BOUND, ForgeRules.check(Button.DRAW_OUT, relic(false, 0, 0, 2, CoreKind.EMPTY_CORE)));
        assertEquals(Msg.OK, ForgeRules.check(Button.DRAW_OUT, relic(false, 0, 1, 2, CoreKind.EMPTY_CORE)));
    }

    @Test
    void cycleWorksWhenAnythingIsBound() {
        assertEquals(Msg.NOTHING_BOUND, ForgeRules.check(Button.CYCLE, relic(false, 0, 0, 2, CoreKind.NONE)));
        assertEquals(Msg.OK, ForgeRules.check(Button.CYCLE, relic(false, 0, 1, 2, CoreKind.NONE)));
        assertEquals(1, ForgeRules.cycleNext(0, 2));
        assertEquals(0, ForgeRules.cycleNext(1, 2), "wraps");
        assertEquals(0, ForgeRules.cycleNext(5, 0));
        assertEquals(1, ForgeRules.clampCycle(7, 2));
        assertEquals(0, ForgeRules.clampCycle(-3, 2));
        assertEquals(0, ForgeRules.clampCycle(3, 0));
    }

    @Test
    void engraveNeedsAWeaponAndABookAndRoom() {
        assertEquals(Msg.NO_ITEM, ForgeRules.check(Button.ENGRAVE, new State(Subject.NONE, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 0, 1, false)));
        assertEquals(Msg.NOT_A_WEAPON, ForgeRules.check(Button.ENGRAVE, new State(Subject.RELIC, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 0, 1, false)), "a ring or armour relic");
        assertEquals(Msg.NEED_BOOK, ForgeRules.check(Button.ENGRAVE, new State(Subject.WEAPON, false, 0, 0, 0, CoreKind.SOCKET_CORE, 0, 0, 0, true)));
        assertEquals(Msg.NOT_AN_ENGRAVING, ForgeRules.check(Button.ENGRAVE, new State(Subject.WEAPON, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 0, 0, true)));
        assertEquals(Msg.OK, ForgeRules.check(Button.ENGRAVE, new State(Subject.WEAPON, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 0, 1, true)));
        assertEquals(Msg.OK, ForgeRules.check(Button.ENGRAVE, new State(Subject.RELIC, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 2, 1, true)), "a relic weapon");
        assertEquals(Msg.ENGRAVING_CAP, ForgeRules.check(Button.ENGRAVE, new State(Subject.WEAPON, false, 0, 0, 0, CoreKind.ENGRAVING_BOOK, 0, 3, 1, true)));
    }

    @Test
    void theEngravingCapCountsOnlyWhatTheForgeAdds() {
        assertEquals(3, ForgeRules.ENGRAVING_CAP);
        assertEquals(Msg.OK, ForgeRules.engraveCap(0, 3));
        assertEquals(Msg.OK, ForgeRules.engraveCap(2, 1));
        assertEquals(Msg.ENGRAVING_CAP, ForgeRules.engraveCap(2, 2));
        assertEquals(Msg.ENGRAVING_CAP, ForgeRules.engraveCap(3, 1));
    }

    @Test
    void anEngravingIsNeverReplacedAndConflictsAreRefused() {
        assertEquals(Msg.ENGRAVING_PRESENT, ForgeRules.engraveEntry(true, true));
        assertEquals(Msg.ENGRAVING_PRESENT, ForgeRules.engraveEntry(true, false));
        assertEquals(Msg.ENGRAVING_CONFLICT, ForgeRules.engraveEntry(false, false));
        assertEquals(Msg.OK, ForgeRules.engraveEntry(false, true));
        assertEquals(1, ForgeRules.engraveLevel(0, 3));
        assertEquals(3, ForgeRules.engraveLevel(9, 3));
        assertEquals(2, ForgeRules.engraveLevel(2, 3));
        assertEquals(1, ForgeRules.engraveLevel(5, 0));
    }

    @Test
    void theMaskHasOneBitPerEnabledButton() {
        int none = ForgeRules.enabledMask(new State(Subject.NONE, false, 0, 0, 0, CoreKind.NONE, 0, 0, 0, false));
        assertEquals(0, none);
        int m = ForgeRules.enabledMask(relic(false, 1, 1, 2, CoreKind.SOCKET_CORE));
        assertTrue((m & (1 << Button.UPGRADE.id())) != 0);
        assertTrue((m & (1 << Button.SOCKET.id())) != 0);
        assertTrue((m & (1 << Button.CYCLE.id())) != 0);
        assertTrue((m & (1 << Button.DRAW_OUT.id())) == 0);
        assertTrue((m & (1 << Button.ENGRAVE.id())) == 0);
    }

    @Test
    void everyMessageHasALangKeyAndAnUnknownOrdinalIsOk() {
        for (Msg m : Msg.values()) assertEquals("nusmp.forge.msg." + m.name().toLowerCase(), m.langKey());
        assertSame(Msg.OK, Msg.of(-1));
        assertSame(Msg.OK, Msg.of(999));
        assertTrue(Msg.UPGRADE_OK.good());
        assertTrue(Msg.ENGRAVED.good());
        assertFalse(Msg.NO_ITEM.good());
        assertFalse(Msg.UPGRADE_FAILED_KEPT.good());
    }
}
