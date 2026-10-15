package com.newuniverse.nusmp.relics.forge;

import com.newuniverse.nusmp.relics.forge.ForgeRules.Button;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Msg;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The synced numbers of the Relic Forge and the lines the screen prints from them. */
class ForgeViewTest {
    private static void assertArgs(Object[] expected, Object[] actual) {
        assertTrue(java.util.Arrays.equals(expected, actual), java.util.Arrays.toString(expected) + " vs " + java.util.Arrays.toString(actual));
    }

    private static int[] data() { return new int[ForgeView.COUNT]; }

    @Test
    void anEmptyOrShortArrayDecodesToTheNoRelicView() {
        ForgeView.View v = ForgeView.decode(new int[0]);
        assertFalse(v.hasSubject());
        assertFalse(v.isRelic());
        assertEquals(0f, v.tierFraction());
        assertEquals("nusmp.forge.tier.none", v.tierLine().key());
        assertEquals("nusmp.forge.sockets.none", v.socketLine().key());
        assertEquals("nusmp.forge.cost.none", v.costLine().key());
        assertEquals("nusmp.forge.chance.none", v.chanceLine().key());
        assertNull(v.cooldownLine());
        assertSame(Msg.OK, v.msg());
        assertNotNull(ForgeView.decode(null));
    }

    @Test
    void aRelicShowsTierSocketsCostAndChance() {
        int[] d = data();
        d[ForgeView.D_SUBJECT] = ForgeView.subjectCode(Subject.RELIC);
        d[ForgeView.D_TIER] = 3;
        d[ForgeView.D_MAX] = 6;
        d[ForgeView.D_USED] = 1;
        d[ForgeView.D_TOTAL] = 2;
        d[ForgeView.D_TARGET] = 4;
        d[ForgeView.D_COST] = 10;
        d[ForgeView.D_CHANCE] = 70;
        d[ForgeView.D_COOLDOWN_SECONDS] = 5;
        ForgeView.View v = ForgeView.decode(d);
        assertTrue(v.isRelic());
        assertEquals(0.5f, v.tierFraction(), 1e-6f);
        assertArgs(new Object[]{3, 6}, v.tierLine().args());
        assertArgs(new Object[]{1, 2}, v.socketLine().args());
        assertArgs(new Object[]{10}, v.costLine().args());
        assertArgs(new Object[]{70}, v.chanceLine().args());
        assertArgs(new Object[]{5}, v.cooldownLine().args());
    }

    @Test
    void atTheTopNothingIsPaidAndARelicWithoutSocketsSaysSo() {
        int[] d = data();
        d[ForgeView.D_SUBJECT] = ForgeView.subjectCode(Subject.RELIC);
        d[ForgeView.D_TIER] = 6;
        d[ForgeView.D_MAX] = 6;
        d[ForgeView.D_TARGET] = -1;
        ForgeView.View v = ForgeView.decode(d);
        assertEquals(1f, v.tierFraction());
        assertEquals("nusmp.forge.cost.none", v.costLine().key());
        assertEquals("nusmp.forge.sockets.zero", v.socketLine().key());
        assertEquals("nusmp.forge.cycle.none", v.cycleLine().key());
    }

    @Test
    void theCycleLineIsOneBasedAndStaysInRange() {
        int[] d = data();
        d[ForgeView.D_SUBJECT] = ForgeView.subjectCode(Subject.RELIC);
        d[ForgeView.D_USED] = 2;
        d[ForgeView.D_TOTAL] = 2;
        d[ForgeView.D_CYCLE] = 1;
        assertArgs(new Object[]{2, 2}, ForgeView.decode(d).cycleLine().args());
        d[ForgeView.D_CYCLE] = 9;
        assertArgs(new Object[]{2, 2}, ForgeView.decode(d).cycleLine().args());   // a stale index is clamped
    }

    @Test
    void theButtonMaskAndTheMessageAreRead() {
        int[] d = data();
        d[ForgeView.D_MASK] = (1 << Button.SOCKET.id()) | (1 << Button.ENGRAVE.id());
        d[ForgeView.D_MSG] = Msg.SOCKETS_FULL.ordinal();
        ForgeView.View v = ForgeView.decode(d);
        assertTrue(v.enabled(Button.SOCKET));
        assertTrue(v.enabled(Button.ENGRAVE));
        assertFalse(v.enabled(Button.UPGRADE));
        assertEquals("nusmp.forge.msg.sockets_full", v.messageLine().key());
    }

    @Test
    void theEngraveLineShowsTheCap() {
        int[] d = data();
        d[ForgeView.D_FORGE_ENGRAVINGS] = 2;
        assertArgs(new Object[]{2, ForgeRules.ENGRAVING_CAP}, ForgeView.decode(d).engraveLine().args());
    }

    @Test
    void theSubjectCodeRoundTripsAndABadCodeIsNone() {
        for (Subject s : Subject.values()) {
            int[] d = data();
            d[ForgeView.D_SUBJECT] = ForgeView.subjectCode(s);
            assertSame(s, ForgeView.decode(d).subject());
        }
        int[] d = data();
        d[ForgeView.D_SUBJECT] = 77;
        assertSame(Subject.NONE, ForgeView.decode(d).subject());
    }
}
