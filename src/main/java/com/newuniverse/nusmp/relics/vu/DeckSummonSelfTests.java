package com.newuniverse.nusmp.relics.vu;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.deck.DeckSummonGeometry;
import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline;
import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline.Phase;

import java.io.IOException;
import java.io.InputStream;

/**
 * The self-test {@code visual.deck_summon}: the card summon of the Deck of Many Things has its shape, its texture atlas in the jar, a timeline of about two seconds whose phases run in order and
 * whose progress never goes back, and a payload seed that reads back the card, the zone and the alignment. Pure arithmetic and one jar file: runs on a dedicated server.
 */
public final class DeckSummonSelfTests {
    private DeckSummonSelfTests() {}

    public static void register() { SelfTests.register("visual.deck_summon", server -> run()); }

    static SelfTests.Result run() throws IOException {
        if (VfxShape.valueOf("DECK_SUMMON").ordinal() != 439) return SelfTests.Result.fail("DECK_SUMMON moved from its shipped ordinal 439 (ordinals are saved; shapes are only ever appended after it)");   // 439 = its place when it shipped; later shapes (the Storm Magic ones) are appended behind it
        try (InputStream in = DeckSummonSelfTests.class.getResourceAsStream("/assets/nusmp/textures/particle/deck_summon.png")) {
            if (in == null) return SelfTests.Result.fail("the summon atlas textures/particle/deck_summon.png is not in the jar");
        }
        if (DeckSummonTimeline.TOTAL < 30 || DeckSummonTimeline.TOTAL > 50) return SelfTests.Result.fail("the summon is not about two seconds: " + DeckSummonTimeline.TOTAL + " ticks");
        Phase[] ph = Phase.values();
        for (int i = 0; i < ph.length; i++) {
            if (DeckSummonTimeline.length(ph[i]) <= 0) return SelfTests.Result.fail(ph[i] + " has no length");
            if (i > 0 && DeckSummonTimeline.start(ph[i]) < DeckSummonTimeline.start(ph[i - 1])) return SelfTests.Result.fail(ph[i] + " starts before " + ph[i - 1]);
        }
        float last = -1f;
        for (int k = 0; k <= DeckSummonTimeline.TOTAL * 4; k++) {
            float t = k / 4f, f = DeckSummonTimeline.flipDegrees(t);
            if (f < last) return SelfTests.Result.fail("the flip goes back at " + t);
            last = f;
        }
        if (DeckSummonTimeline.cardAlpha(DeckSummonTimeline.TOTAL) > 0.001f) return SelfTests.Result.fail("the card does not dissolve");
        for (int card = 0; card < 22; card++) {
            for (int zone = 0; zone < DeckSummonGeometry.ZONES; zone++) {
                for (int mood = 0; mood < 3; mood++) {
                    long s = DeckSummonGeometry.pack(card, zone, mood);
                    if (DeckSummonGeometry.cardOf(s) != card || DeckSummonGeometry.zoneOf(s) != zone || DeckSummonGeometry.moodOf(s) != mood) return SelfTests.Result.fail("the payload seed does not read back");
                }
            }
        }
        return SelfTests.Result.ok(DeckSummonTimeline.TOTAL + " ticks, " + ph.length + " phases, atlas in the jar, seed round trip");
    }
}
