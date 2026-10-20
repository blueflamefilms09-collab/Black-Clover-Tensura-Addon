package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.mana.zone.GrandZoneMath.Exposure;
import com.newuniverse.nusmp.mana.zone.GrandZoneMath.PulseKind;
import com.newuniverse.nusmp.mana.zone.GrandZoneRegion.Effect;
import com.newuniverse.nusmp.mana.zone.GrandZoneRegion.Theme;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Grand Magic Zones as places: the shipped data, the shapes, the exposure of a player against a zone, and the capped, deterministic effects. */
class GrandZoneTest {
    private static final File DIR = new File("src/main/resources/data/nusmp/grand_magic_zones");

    private static List<GrandZoneRegion> shipped() throws Exception {
        List<GrandZoneRegion> out = new ArrayList<>();
        File[] files = DIR.listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files, "the zone folder exists: " + DIR);
        for (File f : files) out.add(GrandZoneRegion.parse(f.getName().replace(".json", ""), Files.readString(f.toPath())));
        out.sort((a, b) -> a.id().compareTo(b.id()));
        return out;
    }

    private static void refused(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isBlank(), "a refusal gives a reason");
            return;
        }
        fail("the file was accepted");
    }

    private static void fail(String m) { throw new AssertionError(m); }

    private static GrandZoneRegion byId(List<GrandZoneRegion> all, String id) {
        return all.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow(() -> new AssertionError("missing zone " + id));
    }

    @Test
    void theFiveNamedZonesShipAndFollowTheCanon() throws Exception {
        List<GrandZoneRegion> all = shipped();
        assertTrue(all.size() >= 5, "five named zones: " + all.size());
        GrandZoneRegion seabed = byId(all, "seabed_temple"), forest = byId(all, "witches_forest"), volcano = byId(all, "ultime_volcano_trail"),
                gravito = byId(all, "gravito_rock"), elysia = byId(all, "elysia");
        assertTrue(seabed.has(Effect.VIOLENT_CURRENTS) && seabed.theme() == Theme.SEABED, "violent swirling currents under the sea");
        assertTrue(forest.has(Effect.WITCH_GLOW) && forest.theme() == Theme.FOREST, "the witches' forest glows and confuses");
        assertTrue(volcano.has(Effect.MAGMA_HEAT) && volcano.theme() == Theme.VOLCANO, "the volcano trail burns");
        assertTrue(gravito.has(Effect.CHAOTIC_GRAVITY) && gravito.has(Effect.FLOATING_BOULDERS) && gravito.theme() == Theme.GRAVITO, "chaotic gravity and floating boulders");
        assertTrue(elysia.has(Effect.CALM) && elysia.theme() == Theme.HAVEN, "Elysia is a safe haven");
        for (GrandZoneRegion r : all) {
            assertEquals(List.of(), r.problems(), r.id());
            assertTrue(r.density() >= 1.0 && r.density() <= 3.0 && r.recommendedEp() >= 1000);
            assertEquals("minecraft:overworld", r.dimension());
        }
        assertTrue(seabed.recommendedEp() > elysia.recommendedEp(), "the haven asks the least");
        assertTrue(gravito.recommendedEp() >= volcano.recommendedEp() && gravito.density() > elysia.density());
    }

    @Test
    void shapesContainAndMeasureDepth() {
        GrandZoneRegion s = GrandZoneRegion.parse("s", "{\"shape\":\"sphere\",\"centre\":[100,64,-50],\"radius\":50,\"effects\":[\"pressure\"]}");
        assertTrue(s.contains(100, 64, -50) && s.contains(149.9, 64, -50) && !s.contains(150.1, 64, -50) && !s.contains(100, 120, -50));
        assertEquals(50.0, s.depth(100, 64, -50), 1e-9);
        assertEquals(0.0, s.depth(150, 64, -50), 1e-9);
        assertTrue(s.depth(160, 64, -50) < 0);
        GrandZoneRegion b = GrandZoneRegion.parse("b", "{\"shape\":\"box\",\"min\":[0,0,0],\"max\":[100,40,60],\"effects\":[\"mana_storm\"]}");
        assertTrue(b.box() && b.contains(50, 20, 30) && !b.contains(101, 20, 30) && !b.contains(50, 41, 30));
        assertEquals(10.0, b.depth(10, 20, 30), 1e-9);
        assertEquals(50.0, b.effectiveRadius(), 1e-9);
        GrandZoneRegion floor = s.withBox("floor_3", "nusmp:mega_dungeon", 0, 0, 0, 64, 32, 64);
        assertTrue(floor.box() && floor.theme() == s.theme() && floor.has(Effect.PRESSURE) && floor.dimension().equals("nusmp:mega_dungeon") && floor.id().equals("floor_3"));
        assertTrue(floor.contains(32, 16, 32) && !floor.contains(100, 16, 32), "the dungeon's hook keeps a zone's theme and gives it a floor's box");
    }

    @Test
    void badFilesAreRefusedWithAReason() {
        refused(() -> GrandZoneRegion.parse("x", "{\"shape\":\"sphere\",\"radius\":0}"));
        refused(() -> GrandZoneRegion.parse("x", "{\"shape\":\"box\",\"min\":[0,0,0],\"max\":[0,5,5]}"));
        refused(() -> GrandZoneRegion.parse("x", "{\"radius\":50,\"effects\":[\"no_such_effect\"]}"));
        refused(() -> GrandZoneRegion.parse("x", "nope"));
        assertEquals(0xFF2A7BFF, GrandZoneRegion.colour("#2A7BFF"));
        assertEquals(0x80112233, GrandZoneRegion.colour("0x80112233"));
        assertEquals(0xFF8C6BFF, GrandZoneRegion.colour("garbage"));
        GrandZoneRegion calmAndHot = GrandZoneRegion.parse("c", "{\"radius\":60,\"effects\":[\"calm\",\"magma_heat\"]}");
        assertFalse(calmAndHot.problems().isEmpty(), "a safe haven cannot also hurt");
    }

    @Test
    void theWeakAreHarmedTheStrongAreFedAndAManaZoneResists() {
        double rec = 1_000_000;
        Exposure none = GrandZoneMath.exposure(0, rec, 2.4, false, false);
        Exposure half = GrandZoneMath.exposure(rec * 0.5, rec, 2.4, false, false);
        Exposure near = GrandZoneMath.exposure(rec * 0.95, rec, 2.4, false, false);
        Exposure strong = GrandZoneMath.exposure(rec * 8, rec, 2.4, false, false);
        assertTrue(none.harm() > half.harm() && half.harm() > near.harm(), "harm falls as the player nears the recommendation");
        assertTrue(none.harm() >= 0.9, "no EP at all is crushed: " + none.harm());
        assertEquals(0.0, near.harm(), 0.25);
        assertEquals(0.0, strong.harm(), 0.0);
        assertTrue(strong.boon() > 0.9 && strong.boon() <= 1.0, "eight times the recommendation is fed in full");
        assertEquals(0.0, none.boon(), 0.0);
        assertTrue(GrandZoneMath.exposure(rec * 2, rec, 2.4, false, false).boon() > 0.2);
        Exposure resisted = GrandZoneMath.exposure(0, rec, 2.4, false, true);
        assertEquals(none.harm() * 0.5, resisted.harm(), 1e-9, "a Mana Zone user resists half");
        Exposure haven = GrandZoneMath.exposure(0, rec, 2.0, true, false);
        assertEquals(0.0, haven.harm(), 0.0);
        assertTrue(haven.boon() >= 0.3, "a safe haven always feeds a little");
        assertTrue(GrandZoneMath.exposure(0, rec, 3.0, false, false).harm() >= GrandZoneMath.exposure(0, rec, 1.2, false, false).harm(), "denser mana is harder");
        double prev = 2;
        for (double ep = 0; ep <= rec; ep += rec / 50) {
            double h = GrandZoneMath.exposure(ep, rec, 2.4, false, false).harm();
            assertTrue(h <= prev + 1e-12, "monotonic at " + ep);
            prev = h;
        }
    }

    @Test
    void theBorderIsNotACliff() {
        assertEquals(0.0, GrandZoneMath.edgeFade(-5), 0.0);
        assertEquals(0.0, GrandZoneMath.edgeFade(0), 0.0);
        assertEquals(0.5, GrandZoneMath.edgeFade(12), 1e-9);
        assertEquals(1.0, GrandZoneMath.edgeFade(500), 0.0);
        assertEquals(0, GrandZoneMath.bucket(0));
        assertEquals(20, GrandZoneMath.bucket(1.5));
        assertEquals(10, GrandZoneMath.bucket(0.5));
    }

    @Test
    void gravityPulsesAreCappedDeterministicAndRideableByTheStrong() {
        double[] a = new double[3], b = new double[3];
        boolean sawLift = false, sawPush = false, sawSlam = false;
        for (long seed = 1; seed <= 400; seed++) {
            for (long n = 1; n <= 20; n++) {
                for (double harm : new double[]{0.0, 0.5, 1.0}) {
                    PulseKind k = GrandZoneMath.gravityPulse(seed, n, harm, 0, false, a);
                    double len = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
                    assertTrue(len <= GrandZoneMath.MAX_GRAVITY_SPEED + 1e-9, "capped: " + len);
                    if (k == PulseKind.SLAM) { assertTrue(harm >= 0.4, "a slam only happens to a player being harmed"); assertTrue(a[1] < 0); sawSlam = true; }
                    if (k == PulseKind.LIFT) { assertTrue(a[1] > 0); sawLift = true; }
                    if (k == PulseKind.PUSH) sawPush = true;
                }
            }
        }
        assertTrue(sawLift && sawPush && sawSlam, "all three kinds occur");
        GrandZoneMath.gravityPulse(77, 5, 0.8, 0, false, a);
        GrandZoneMath.gravityPulse(77, 5, 0.8, 0, false, b);
        assertArrayEquals(new float[]{(float) a[0], (float) a[1], (float) a[2]}, new float[]{(float) b[0], (float) b[1], (float) b[2]});
        double[] strong = new double[3], user = new double[3];
        GrandZoneMath.gravityPulse(77, 5, 0.8, 1.0, false, strong);
        GrandZoneMath.gravityPulse(77, 5, 0.8, 0.0, true, user);
        double lenWeak = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]), lenStrong = Math.sqrt(strong[0] * strong[0] + strong[1] * strong[1] + strong[2] * strong[2]);
        assertTrue(lenStrong < lenWeak, "a strong player rides the zone");
        assertEquals(lenWeak * 0.5, Math.sqrt(user[0] * user[0] + user[1] * user[1] + user[2] * user[2]), 1e-9, "a Mana Zone halves the pulse");
        for (long n = 0; n < 200; n++) {
            int gap = GrandZoneMath.pulseGap(5, n);
            assertTrue(gap >= 60 && gap < 160, "gap " + gap);
        }
    }

    @Test
    void currentsAreCappedAndSwirlRoundTheCentre() {
        double[] v = new double[3];
        for (long t = 0; t < 2000; t += 7) {
            GrandZoneMath.currentPush(t, 40, 25, 0, 0, 1.0, 0.0, false, v);
            assertTrue(Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) <= GrandZoneMath.MAX_CURRENT + 1e-9);
        }
        GrandZoneMath.currentPush(0, 100, 0, 0, 0, 0.5, 0, false, v);
        assertTrue(Math.abs(v[2]) > Math.abs(v[0]) * 0.9, "on the +x axis the current runs along z: it swirls");
        double[] weak = new double[3], strong = new double[3];
        GrandZoneMath.currentPush(10, 30, 30, 0, 0, 1.0, 0.0, false, weak);
        GrandZoneMath.currentPush(10, 30, 30, 0, 0, 1.0, 1.0, false, strong);
        assertTrue(Math.abs(strong[0]) + Math.abs(strong[2]) < Math.abs(weak[0]) + Math.abs(weak[2]));
    }

    @Test
    void pressureHeatAndBoltsNeverKillOnTheirOwn() {
        assertEquals(0.0, GrandZoneMath.pressureDamage(0.2, 20), 0.0, "under 0.35 harm there is no pressure damage");
        for (double h = 0; h <= 1.0001; h += 0.05) {
            assertTrue(GrandZoneMath.pressureDamage(h, 20) <= 20 * GrandZoneMath.PRESSURE_MAX_SHARE + 1e-9);
            assertTrue(GrandZoneMath.heatDamage(h, 20) <= 20 * 0.08 + 1e-9);
        }
        assertEquals(-1, GrandZoneMath.slownessAmplifier(0.1));
        assertEquals(0, GrandZoneMath.slownessAmplifier(0.4));
        assertEquals(1, GrandZoneMath.slownessAmplifier(0.9));
        assertEquals(0, GrandZoneMath.heatFireSeconds(0.1));
        assertTrue(GrandZoneMath.heatFireSeconds(1.0) >= 3);
        assertEquals(0.0, GrandZoneMath.boltHitChance(0.2), 0.0);
        assertTrue(GrandZoneMath.boltHitChance(1.0) <= 0.12);
        assertTrue(GrandZoneMath.boltDamage(100) <= 6.0 && GrandZoneMath.boltDamage(20) <= 2.0 + 1e-9);
        int bolts = 0;
        for (long s = 0; s < 7000; s++) if (GrandZoneMath.stormBolt(s, 4242)) bolts++;
        assertTrue(bolts > 800 && bolts < 1200, "about one in seven seconds: " + bolts);
    }

    @Test
    void theBoonFeedsAndTheLookStaysInRange() {
        assertEquals(0.0, GrandZoneMath.manaRegenPercent(0, 2.4), 0.0);
        assertTrue(GrandZoneMath.manaRegenPercent(1, 3.0) <= 0.4, "regeneration stays a fraction of a percent a second");
        assertEquals(1.0, GrandZoneMath.powerMult(0), 0.0);
        assertEquals(1.10, GrandZoneMath.powerMult(1), 1e-9);
        for (double f = 0; f <= 1.0; f += 0.1) {
            assertTrue(GrandZoneMath.skyTint(f, true, 3.0) <= 0.75 && GrandZoneMath.skyTint(f, false, 1.0) >= 0);
            assertTrue(GrandZoneMath.fogScale(f, true, true) >= 0.35 && GrandZoneMath.fogScale(f, false, false) == 1.0);
        }
        assertTrue(GrandZoneMath.skyTint(1.0, true, 2.0) > GrandZoneMath.skyTint(1.0, false, 2.0), "a storm tints the sky more");
    }

    @Test
    void theMixIsStableAndSpread() {
        assertEquals(GrandZoneMath.mix(1, 2), GrandZoneMath.mix(1, 2));
        assertNotEquals(GrandZoneMath.mix(1, 2), GrandZoneMath.mix(1, 3));
        assertNotEquals(GrandZoneMath.mix(1, 2), GrandZoneMath.mix(2, 2));
        int[] bins = new int[8];
        for (long i = 0; i < 8000; i++) bins[(int) Math.floorMod(GrandZoneMath.mix(9, i), 8L)]++;
        for (int b : bins) assertTrue(b > 800 && b < 1200, "uniform enough: " + b);
    }

    @Test
    void effectBitsAndMasksRoundTrip() {
        GrandZoneRegion g = GrandZoneRegion.parse("g", "{\"radius\":100,\"effects\":[\"chaotic_gravity\",\"floating_boulders\",\"mana_storm\"]}");
        assertEquals(GrandZoneRegion.fromMask(g.effectMask()), g.effects());
        assertEquals(1 | 2 | 16, g.effectMask());
        assertEquals(1, Effect.CHAOTIC_GRAVITY.bit());
        assertNull(Effect.byName("nope"));
        assertEquals(Theme.WILD, GrandZoneRegion.themeByName("unknown"));
    }
}
