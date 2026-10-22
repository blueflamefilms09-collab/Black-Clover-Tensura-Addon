package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.FuegoleonArmRules.Reason;
import com.newuniverse.nusmp.book.FuegoleonArmRules.View;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Fuegoleon's flaming arm is the server's decision (which player has it, from when, until when) and a wrong rule is easy to miss in play: an arm on a mage who is not Fuegoleon, an arm that stays
 * after the spirit was released, a lease renewed every tick for every player. This test pins the pure rules of {@link FuegoleonArmRules}: the reason matrix, the styles, the renewal cadence and its
 * packet cost, and what the client draws for each of the viewer's settings. Pure (no game classes).
 */
class FuegoleonArmRulesTest {
    // ------------------------------------------------------------------ the server's rules
    @Test
    void theArmNeedsFuegoleonsGrimoireTheSwitchAndALiveWearer() {
        for (boolean contract : new boolean[]{false, true})
            for (boolean linked : new boolean[]{false, true})
                for (boolean dive : new boolean[]{false, true}) {
                    assertEquals(Reason.NONE, FuegoleonArmRules.reason(false, true, contract, linked, dive, true), "the server switch is off");
                    assertEquals(Reason.NONE, FuegoleonArmRules.reason(true, false, contract, linked, dive, true), "not Fuegoleon's grimoire");
                    assertEquals(Reason.NONE, FuegoleonArmRules.reason(true, true, contract, linked, dive, false), "a dead wearer");
                }
    }

    @Test
    void theArmComesWithTheContractTheSpiritOutOrTheFireDive() {
        assertEquals(Reason.NONE, FuegoleonArmRules.reason(true, true, false, false, false, true), "no spirit: no arm");
        assertEquals(Reason.CONTRACT, FuegoleonArmRules.reason(true, true, true, false, false, true));
        assertEquals(Reason.LINK, FuegoleonArmRules.reason(true, true, false, true, false, true), "the Salamander is out");
        assertEquals(Reason.DIVE, FuegoleonArmRules.reason(true, true, false, false, true, true));
        assertEquals(Reason.DIVE, FuegoleonArmRules.reason(true, true, true, true, true, true), "the dive is the strongest reason");
        assertEquals(Reason.LINK, FuegoleonArmRules.reason(true, true, true, true, false, true));
        assertTrue(FuegoleonArmRules.on(true, true, true, false, false, true));
        assertFalse(FuegoleonArmRules.on(true, true, false, false, false, true));
    }

    @Test
    void theArmGoesWhenTheSpiritIsReleased() {
        assertTrue(FuegoleonArmRules.on(true, true, true, false, false, true));
        assertFalse(FuegoleonArmRules.on(true, true, false, false, false, true), "the contract is gone");
    }

    @Test
    void onlyTheFireSpiritIsTheSalamander() {
        assertTrue(FuegoleonArmRules.salamander("Salamander"));
        assertTrue(FuegoleonArmRules.salamander("salamander"));
        assertFalse(FuegoleonArmRules.salamander("Undine"));
        assertFalse(FuegoleonArmRules.salamander("Sylph"));
        assertFalse(FuegoleonArmRules.salamander("Gnome"));
        assertFalse(FuegoleonArmRules.salamander("-"));
        assertFalse(FuegoleonArmRules.salamander(""));
        assertFalse(FuegoleonArmRules.salamander(null));
    }

    @Test
    void theStyleOfTheLeaseSaysHowHighTheArmBurns() {
        assertEquals(FuegoleonArmRules.STYLE, FuegoleonArmRules.styleOf(Reason.CONTRACT));
        assertEquals(FuegoleonArmRules.STYLE_LINKED, FuegoleonArmRules.styleOf(Reason.LINK));
        assertEquals(FuegoleonArmRules.STYLE_LINKED, FuegoleonArmRules.styleOf(Reason.DIVE));
        assertTrue(FuegoleonArmRules.valid(FuegoleonArmRules.STYLE) && FuegoleonArmRules.valid(FuegoleonArmRules.STYLE_LINKED));
        for (int bad : new int[]{-1, 0, 3, 99, 127}) assertFalse(FuegoleonArmRules.valid(bad), "a bad style byte " + bad);
    }

    // ------------------------------------------------------------------ cadence and cost
    @Test
    void everyWearerIsCheckedOnceEveryRenewPeriod() {
        for (int id : new int[]{0, 1, 17, 39, 40, 41, 123456, -5}) {
            int hits = 0;
            for (long t = 0; t < FuegoleonArmRules.RENEW_TICKS * 5L; t++) if (FuegoleonArmRules.due(t, id)) hits++;
            assertEquals(5, hits, "entity " + id);
        }
    }

    @Test
    void aCrowdIsSpreadOverThePeriod() {
        int[] perTick = new int[FuegoleonArmRules.RENEW_TICKS];
        for (int id = 0; id < 400; id++) for (int t = 0; t < FuegoleonArmRules.RENEW_TICKS; t++) if (FuegoleonArmRules.due(t, id)) perTick[t]++;
        for (int n : perTick) assertEquals(10, n, "400 players spread evenly: ten a tick");
    }

    @Test
    void theArmCostsAtMostHalfAPacketASecondPerWearer() {
        assertTrue(FuegoleonArmRules.packetsPerSecond() <= 0.5, "" + FuegoleonArmRules.packetsPerSecond());
        assertTrue(FuegoleonArmRules.LEASE_TICKS > FuegoleonArmRules.RENEW_TICKS * 2, "the lease survives a missed renewal");
        assertTrue(FuegoleonArmRules.LEASE_TICKS <= 200, "the arm disappears within ten seconds when the server stops renewing it");
    }

    // ------------------------------------------------------------------ what the client draws
    @Test
    void theViewerCanTurnTheArmOff() {
        for (boolean c : new boolean[]{false, true})
            for (boolean d : new boolean[]{false, true})
                for (boolean l : new boolean[]{false, true}) assertEquals(0f, FuegoleonArmRules.strength(View.OFF, c, d, l));
    }

    @Test
    void flareModeShowsTheArmOnlyWhileItBurns() {
        assertEquals(0f, FuegoleonArmRules.strength(View.FLARE, false, false, false), "at rest: nothing");
        assertTrue(FuegoleonArmRules.strength(View.FLARE, false, false, true) > 0f, "the Salamander is out");
        assertTrue(FuegoleonArmRules.strength(View.FLARE, true, false, false) > FuegoleonArmRules.strength(View.FLARE, false, false, true), "a cast burns higher than the spirit being out");
        assertEquals(1f, FuegoleonArmRules.strength(View.FLARE, false, true, false), "the dive is the highest");
    }

    @Test
    void alwaysModeShowsAQuietArmAtRestAndTheSameFlamesInAction() {
        float rest = FuegoleonArmRules.strength(View.ALWAYS, false, false, false);
        assertTrue(rest >= 0.5f && rest < 1f, "a quiet flame at rest: " + rest);
        for (boolean c : new boolean[]{false, true})
            for (boolean d : new boolean[]{false, true})
                for (boolean l : new boolean[]{false, true}) {
                    float always = FuegoleonArmRules.strength(View.ALWAYS, c, d, l), flare = FuegoleonArmRules.strength(View.FLARE, c, d, l);
                    assertTrue(always >= flare && always >= rest && always <= 1f);
                }
        assertEquals(FuegoleonArmRules.strength(View.FLARE, true, true, true), FuegoleonArmRules.strength(View.ALWAYS, true, true, true));
    }

    @Test
    void theFlareFollowsTheCastAndTheDive() {
        assertEquals(0f, FuegoleonArmRules.flare(false, false));
        assertTrue(FuegoleonArmRules.flare(true, false) > 0f);
        assertEquals(1f, FuegoleonArmRules.flare(false, true));
        assertEquals(1f, FuegoleonArmRules.flare(true, true));
    }

    @Test
    void theArmDrawsAtMostAFewDozenSprites() {
        assertTrue(FuegoleonArmRules.worstQuads() <= 64, "" + FuegoleonArmRules.worstQuads());
        assertTrue(FuegoleonArmRules.MAX_SHEETS <= 8 && FuegoleonArmRules.MAX_EMBERS <= 8 && FuegoleonArmRules.MAX_STRANDS <= 4);
    }
}
