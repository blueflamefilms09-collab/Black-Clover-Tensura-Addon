package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the look table and the charge-up maths of 3.69.0 are plain arithmetic, so the budget, the bounds and the timeline are pinned here with no game. */
class SaiyanLookTest {
    @Test
    void everyFormHasALookAndTheCodeFindsIt() {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            SaiyanLook.Look k = SaiyanLook.of(f.id());
            assertNotNull(k, f.id());
            assertEquals(k, SaiyanLook.ofCode(SaiyanForms.code(f.id())), f.id());
            assertTrue(k.glow() != 0 && k.flames() >= 1 && k.flames() <= SaiyanLook.MAX_FLAMES && k.bolts() <= SaiyanLook.MAX_BOLTS, f.id());
            assertTrue(k.burst() > 0f && k.burst() <= 2f && k.auraH() > 1f && k.auraH() < 4f, f.id());
        }
        assertEquals(SaiyanForms.ALL.size(), SaiyanLook.all().size(), "no look without a form");
        assertNull(SaiyanLook.of(null));
        assertNull(SaiyanLook.ofCode(0));
        assertNull(SaiyanLook.ofCode(SaiyanForms.ALL.size() + 1));
    }

    @Test
    void everyLookStaysUnderTheQuadBudgetAndFarIsLighter() {
        for (SaiyanLook.Look k : SaiyanLook.all()) {
            assertTrue(SaiyanLook.quads(k, false) <= SaiyanLook.MAX_QUADS, k.id() + " " + SaiyanLook.quads(k, false));
            assertTrue(SaiyanLook.quads(k, true) < SaiyanLook.quads(k, false), k.id());
            assertTrue(SaiyanLook.quads(k, true) <= 40, k.id() + " far");
        }
    }

    @Test
    void onlyTheSuperSaiyanFourFormsHaveFurAndATailAndNoArmour() {
        for (SaiyanLook.Look k : SaiyanLook.all()) {
            boolean ss4 = k.id().contains("super_saiyan_4") || k.id().contains("full_power_super_saiyan_4");
            assertEquals(ss4, k.hasFur(), k.id());
            assertEquals(ss4, k.tail(), k.id());
            assertEquals(!ss4, k.armour(), k.id() + ": armour on every form but the furred ones");
        }
    }

    @Test
    void theChargeUpHasTheOrderGatherThenBurst() {
        assertEquals(0f, SaiyanLook.gather(0f), 1e-6);
        assertEquals(1f, SaiyanLook.gather(SaiyanLook.GATHER_TICKS), 1e-6);
        assertTrue(SaiyanLook.flash(SaiyanLook.GATHER_TICKS) > SaiyanLook.flash(SaiyanLook.GATHER_TICKS / 2f), "the flash peaks at the burst");
        assertEquals(0f, SaiyanLook.flash(SaiyanLook.GATHER_TICKS + 11f), 1e-6, "and is gone ten ticks later");
        assertEquals(0f, SaiyanLook.pillar(0f), 1e-6, "no column while gathering");
        assertEquals(1f, SaiyanLook.pillar(SaiyanLook.GATHER_TICKS + 3f), 1e-6, "the column is full after the burst");
        assertEquals(0f, SaiyanLook.pillar(SaiyanLook.BURST_TICKS), 1e-6, "and gone when the charge-up ends");
        assertEquals(0f, SaiyanLook.ringRadius(SaiyanLook.GATHER_TICKS - 1f, 0, 1f), 1e-6, "no ring before the burst");
        assertTrue(SaiyanLook.ringRadius(SaiyanLook.GATHER_TICKS + 4f, 0, 1f) > 0.6f);
        assertTrue(SaiyanLook.ringRadius(SaiyanLook.GATHER_TICKS + 10f, 0, 1f) > SaiyanLook.ringRadius(SaiyanLook.GATHER_TICKS + 4f, 0, 1f), "a ring only grows");
        assertEquals(0f, SaiyanLook.ringRadius(SaiyanLook.BURST_TICKS + 1f, 1, 1f), 1e-6);
        assertTrue(SaiyanLook.crackRadius(SaiyanLook.GATHER_TICKS, 1f) > SaiyanLook.crackRadius(2f, 1f), "the cracks spread");
        assertEquals(0f, SaiyanLook.crackRadius(SaiyanLook.CRACK_TICKS, 1f), 1e-6);
        assertFalse(SaiyanLook.crackAlpha(SaiyanLook.CRACK_TICKS - 1f) > SaiyanLook.crackAlpha(SaiyanLook.GATHER_TICKS), "they cool off");
    }

    @Test
    void rocksStreaksAndRingsStayInBoundsForTheWholeCharge() {
        float[] o = new float[7];
        for (SaiyanLook.Look k : SaiyanLook.all()) {
            for (float t = 0f; t <= SaiyanLook.CRACK_TICKS + 4f; t += 0.5f) {
                for (int i = 0; i < SaiyanLook.MAX_CHIPS; i++) {
                    SaiyanLook.chip(i, t, k.burst(), o);
                    assertTrue(o[1] >= 0f && o[1] <= 6f && o[2] >= 0f && o[2] <= 3f && o[3] > 0f && o[3] < 0.2f, k.id() + " chip " + i + " t=" + t);
                }
                for (int i = 0; i < SaiyanLook.MAX_STREAKS; i++) {
                    SaiyanLook.streak(i, t, o);
                    assertTrue(o[3] >= 0f && o[3] <= 1f && Math.hypot(o[0], o[2]) <= 3.6f && o[1] >= 0f && o[1] <= 2.5f, k.id() + " streak " + i + " t=" + t);
                }
                assertTrue(SaiyanLook.ringRadius(t, 0, k.burst()) <= 9.5f && SaiyanLook.ringRadius(t, 1, k.burst()) <= 9.5f);
            }
        }
    }

    @Test
    void streaksOnlyFlowWhileTheEnergyGathers() {
        float[] o = new float[7];
        float seen = 0f;
        for (int i = 0; i < SaiyanLook.MAX_STREAKS; i++) { SaiyanLook.streak(i, SaiyanLook.GATHER_TICKS / 2f, o); seen = Math.max(seen, o[3]); }
        assertTrue(seen > 0.5f, "several streaks are in flight mid-gather");
        for (int i = 0; i < SaiyanLook.MAX_STREAKS; i++) { SaiyanLook.streak(i, SaiyanLook.GATHER_TICKS + 12f, o); assertEquals(0f, o[3], 1e-6, "none after the burst"); }
    }

    @Test
    void theHashIsDeterministicAndInRange() {
        for (int i = -50; i < 500; i++) {
            float h = SaiyanLook.hash(i);
            assertTrue(h >= 0f && h < 1f);
            assertEquals(h, SaiyanLook.hash(i), 0f);
        }
    }
}
