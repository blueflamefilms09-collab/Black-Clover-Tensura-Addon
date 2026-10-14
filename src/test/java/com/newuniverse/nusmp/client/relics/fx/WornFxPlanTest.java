package com.newuniverse.nusmp.client.relics.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The grade tiers of the worn effects, the quality option, the distance rule, the budget and the themes: pure rules, so they are tests. */
class WornFxPlanTest {
    static WornFxPlan near(int tier, FxQuality q) { return WornFxPlan.of(tier, q, 4.0); }

    @Test
    void normalAndSpecialGradesAddNothing() {
        for (FxQuality q : FxQuality.values()) {
            assertEquals(WornFxPlan.NONE, near(1, q));
            assertEquals(WornFxPlan.NONE, near(2, q));
        }
    }

    @Test
    void theEffectsGrowWithTheGrade() {
        FxQuality q = FxQuality.HIGH;
        assertTrue(near(3, q).rim() > 0f && near(3, q).shimmer() == 0f && near(3, q).motes() == 0, "RARE: a faint rim only");
        assertTrue(near(4, q).shimmer() > 0f && near(4, q).motes() == 0, "UNIQUE: a soft shimmer");
        assertTrue(near(5, q).motes() > 0 && near(5, q).runeRing() && !near(5, q).halo(), "LEGEND: motes and the rune ring");
        assertTrue(near(6, q).halo() && near(7, q).halo(), "GOD and GENESIS: the halo");
        for (int t = 4; t <= 7; t++) {
            assertTrue(near(t, q).rim() >= near(t - 1, q).rim(), "the rim never fades with the grade at tier " + t);
            assertTrue(near(t, q).motes() >= near(t - 1, q).motes());
            assertTrue(near(t, q).pulse() >= near(t - 1, q).pulse());
        }
        assertTrue(near(7, q).motes() > near(6, q).motes());
    }

    @Test
    void lowQualityIsTheCheapestAndHighTheRichest() {
        for (int t = 3; t <= 7; t++) {
            WornFxPlan lo = near(t, FxQuality.LOW), mid = near(t, FxQuality.MEDIUM), hi = near(t, FxQuality.HIGH);
            assertTrue(lo.fxVertices() <= mid.fxVertices() && mid.fxVertices() <= hi.fxVertices(), "vertices grow with the quality at tier " + t);
            assertEquals(0f, lo.shimmer(), 0f, "low quality has no scrolling shimmer");
            assertFalse(lo.runeRing() || lo.halo());
            assertTrue(lo.motes() <= 2);
            assertTrue(mid.motes() <= 4);
            assertTrue(lo.particleEvery() >= mid.particleEvery() && mid.particleEvery() >= hi.particleEvery(), "particles spawn less often at lower quality");
        }
    }

    @Test
    void farWearersGetTheModelAlone() {
        WornFxPlan far = WornFxPlan.of(7, FxQuality.HIGH, 17.0 * 17.0);
        assertEquals(0f, far.rim(), 0f);
        assertEquals(0, far.motes());
        assertFalse(far.runeRing() || far.halo() || far.usesShell());
        assertTrue(far.particleEvery() > 0, "the particle rule does its own culling");
        assertTrue(WornFxPlan.of(7, FxQuality.HIGH, 15.0 * 15.0).motes() > 0);
        assertTrue(WornFxPlan.CULL_RANGE >= 32.0);
    }

    @Test
    void theWorstCaseEffectCostStaysInsideTheCaps() {
        for (FxQuality q : FxQuality.values()) {
            WornFxPlan p = near(7, q);
            assertTrue(p.fxVertices() <= 7 * 32 + 128 + 128);
            assertTrue(p.fxVertices() < q.vertexCap / 3, "the effects of one relic are a small part of the player's budget");
        }
        assertTrue(FxQuality.HIGH.vertexCap <= 6000, "the owner's worst case");
        assertTrue(FxQuality.LOW.vertexCap < FxQuality.MEDIUM.vertexCap && FxQuality.MEDIUM.vertexCap < FxQuality.HIGH.vertexCap);
    }

    @Test
    void qualityNamesParseInAnyCase() {
        assertEquals(FxQuality.LOW, FxQuality.byName("low", FxQuality.HIGH));
        assertEquals(FxQuality.MEDIUM, FxQuality.byName(" Medium ", FxQuality.HIGH));
        assertEquals(FxQuality.HIGH, FxQuality.byName("HIGH", FxQuality.LOW));
        assertEquals(FxQuality.MEDIUM, FxQuality.byName("ultra", FxQuality.MEDIUM));
        assertEquals(FxQuality.MEDIUM, FxQuality.byName(null, FxQuality.MEDIUM));
    }

    @Test
    void budgetDropsTheLowestGradesFirstAndAddsExtrasInOrder() {
        int[] base = {1500, 1500, 1500, 1500};
        int[] extra = {400, 400, 400, 400};
        boolean[] drawn = new boolean[4], extras = new boolean[4];
        int used = WornBudget.allocate(base, extra, 4200, drawn, extras);
        assertTrue(drawn[0] && drawn[1] && !drawn[2] && !drawn[3], "two models fit in 4200, the best two");
        assertTrue(extras[0] && extras[1], "and their extras: 3000 + 800 = 3800");
        assertEquals(3800, used);
        used = WornBudget.allocate(base, extra, 3200, drawn, extras);
        assertTrue(drawn[0] && drawn[1] && !drawn[2]);
        assertFalse(extras[0] || extras[1], "after both models 200 are left, not enough for any extras");
        assertEquals(3000, used);
        used = WornBudget.allocate(base, extra, 3500, drawn, extras);
        assertTrue(extras[0] && !extras[1], "after both models 500 are left: one set of extras (3400)");
        assertEquals(3400, used);
        used = WornBudget.allocate(base, extra, 1000, drawn, extras);
        assertFalse(drawn[0], "a model bigger than the whole budget is not drawn");
        assertEquals(0, used);
    }

    @Test
    void budgetNeverGrantsExtrasWithoutTheModelAndNeverExceedsTheCap() {
        java.util.Random r = new java.util.Random(5);
        for (int trial = 0; trial < 300; trial++) {
            int n = 1 + r.nextInt(8);
            int[] b = new int[n], e = new int[n];
            for (int i = 0; i < n; i++) { b[i] = 100 + r.nextInt(2500); e[i] = r.nextInt(900); }
            boolean[] d = new boolean[n], x = new boolean[n];
            int cap = 500 + r.nextInt(6000);
            int used = WornBudget.allocate(b, e, cap, d, x);
            assertTrue(used <= cap, "used " + used + " cap " + cap);
            int sum = 0;
            for (int i = 0; i < n; i++) {
                assertFalse(x[i] && !d[i]);
                if (d[i]) sum += b[i];
                if (x[i]) sum += e[i];
            }
            assertEquals(sum, used);
        }
    }

    @Test
    void themesFollowTheHueOfTheGlow() {
        assertEquals(FxTheme.EMBER, FxTheme.fromGlow(0xFF5A1A));
        assertEquals(FxTheme.EMBER, FxTheme.fromGlow(0xE0364A));
        assertEquals(FxTheme.STAR, FxTheme.fromGlow(0xFFE9A0));
        assertEquals(FxTheme.STAR, FxTheme.fromGlow(0xFFFFFF));
        assertEquals(FxTheme.LEAF, FxTheme.fromGlow(0x3CC85A));
        assertEquals(FxTheme.SNOW, FxTheme.fromGlow(0x9FE0FF));
        assertEquals(FxTheme.SPARK, FxTheme.fromGlow(0x58A8FF));
        assertEquals(FxTheme.WISP, FxTheme.fromGlow(0xB05CFF));
        assertEquals(FxTheme.PETAL, FxTheme.fromGlow(0xFF8AD0));
        assertEquals(FxTheme.WISP, FxTheme.fromGlow(0x101018));
        assertEquals(FxTheme.BEE, FxTheme.byName("Bee"));
        assertNull(FxTheme.byName("lava"));
        assertNull(FxTheme.byName(null));
    }
}
