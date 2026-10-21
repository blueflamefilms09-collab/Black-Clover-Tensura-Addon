package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryAbility;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryArchetype;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryLibrary;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the telegraph table is data the owner will edit, and the floor warning must be the ground the attack hits. This pins (1) the shipped file: it parses with no problem, it passes the audit
 * against the foundry's own library (every override is a real ability, every archetype of the contract has an ultimate that warns), every cleave, slam, leap and dash of the library has a warning
 * and every telegraphed ability wind-up is at least readable; (2) the geometry: the outline stays inside the point cap, the circle and the cone points lie on the ground the hit test accepts, a
 * line is as long as asked; (3) the parser's strictness: a bad row is dropped and reported, never thrown.
 */
public class TelegraphTest {
    private static FoundryLibrary library() throws Exception {
        String a = Files.readString(Path.of("src/main/resources/data/nusmp/mega/foundry/_abilities.json"));
        String b = Files.readString(Path.of("src/main/resources/data/nusmp/mega/foundry/_archetypes.json"));
        return new FoundryLibrary(FoundryLibrary.parseAbilities(a).rows(), FoundryLibrary.parseArchetypes(b).rows());
    }

    private static TelegraphLibrary shipped() throws Exception {
        TelegraphLibrary.Parsed p = TelegraphLibrary.parse(Files.readString(Path.of("src/main/resources/data/nusmp/mega/poise/telegraphs.json")));
        assertEquals(List.of(), p.problems(), "the shipped telegraph file has problems");
        return p.library();
    }

    @Test
    public void theShippedFileParsesAndPassesTheAuditAgainstTheFoundryLibrary() throws Exception {
        TelegraphLibrary t = shipped();
        assertEquals(List.of(), t.check(library()));
        assertEquals(FoundryArchetype.CONTRACT.size(), t.ultimateIds().size());
        for (String arch : FoundryArchetype.CONTRACT) {
            FoundryAbility u = t.ultimate(arch);
            assertNotNull(u, arch + " has no ultimate");
            assertNotNull(t.def(u), u.id() + " does not warn");
            assertTrue(u.cd() > 0 && !u.passive());
        }
    }

    @Test
    public void everyCleaveSlamLeapAndDashOfTheLibraryWarnsAndNoPassiveDoes() throws Exception {
        TelegraphLibrary t = shipped();
        int warned = 0;
        for (FoundryAbility a : library().abilities().values()) {
            TelegraphDef d = t.def(a);
            switch (a.kind()) {
                case CLEAVE, SLAM, LEAP, DASH, DIVE, BASH -> assertNotNull(d, a.id() + " should warn");
                case BURST -> assertNull(d, "a passive is never telegraphed");
                case HEAL, GUARD, REGEN, THORNS, MARK, RALLY, WARD, ENRAGE -> assertNull(d, a.id() + " is a self buff and must not draw a danger zone");
                default -> { }
            }
            if (d != null) {
                warned++;
                assertTrue(d.shape() != TelegraphShape.NONE);
                int wind = PoiseMath.wind(a.wind(), d.minWind(), false, PoiseTuning.DEFAULT);
                assertTrue(wind >= d.minWind() && wind >= a.wind());
                assertTrue(Desperation.wind(wind, true, PoiseTuning.DEFAULT) >= Math.min(wind, Desperation.MIN_WIND));
                assertTrue(d.radiusFor(a) > 0.0 && d.lengthFor(a) >= 3.0 || d.shape() != TelegraphShape.LINE);
            }
        }
        assertTrue(warned >= 15, "only " + warned + " abilities warn");
    }

    @Test
    public void aCleaveDrawsItsOwnArcAndADashItsOwnReach() throws Exception {
        TelegraphLibrary t = shipped();
        FoundryAbility cleave = library().ability("cleave"), whirl = library().ability("whirlwind"), dash = library().ability("dash"), slam = library().ability("ground_slam");
        assertEquals(120.0, t.def(cleave).angleFor(cleave), 1e-9);
        assertEquals(360.0, t.def(whirl).angleFor(whirl), 1e-9);
        assertEquals(cleave.r(), t.def(cleave).radiusFor(cleave), 1e-9);
        assertEquals(slam.r(), t.def(slam).radiusFor(slam), 1e-9);
        assertEquals(Math.min(dash.dur() * 1.05, dash.max()), t.def(dash).lengthFor(dash), 1e-9);
        assertTrue(t.def(library().ability("leap_slam")).atTarget(), "a leap warns where it lands");
        assertFalse(t.def(slam).atTarget());
    }

    @Test
    public void theOutlineRespectsTheCapAndLiesOnTheGroundTheHitTestAccepts() {
        for (TelegraphShape s : TelegraphShape.values()) {
            List<double[]> pts = TelegraphGeometry.outline(s, 4.0, 120.0, 9.0, 1.0, 1000);
            assertTrue(pts.size() <= TelegraphGeometry.MAX_POINTS * 2, s + " has " + pts.size() + " points");
            if (s == TelegraphShape.NONE) assertTrue(pts.isEmpty());
            else assertFalse(pts.isEmpty(), s + " draws nothing");
            for (double[] p : pts) {
                assertTrue(TelegraphGeometry.inside(s, p[0], p[1], 4.0 + 1e-6, 120.0 + 1e-6, 9.0 + 1e-6, 1.0 + 1e-6), s + " point " + p[0] + "," + p[1] + " is outside its own hit area");
            }
        }
        assertTrue(TelegraphGeometry.outline(null, 3, 90, 3, 1, 12).isEmpty());
    }

    @Test
    public void theHitTestMatchesTheCleaveAndTheLine() {
        assertTrue(TelegraphGeometry.inside(TelegraphShape.CONE, 3.0, 0.0, 3.4, 120, 0, 0));
        assertFalse(TelegraphGeometry.inside(TelegraphShape.CONE, -3.0, 0.0, 3.4, 120, 0, 0), "behind the mob is outside a 120 degree cone");
        assertTrue(TelegraphGeometry.inside(TelegraphShape.CONE, 1.5, 2.5, 3.4, 120, 0, 0), "59 degrees is inside, 60 is the edge");
        assertFalse(TelegraphGeometry.inside(TelegraphShape.CONE, 0.2, 3.3, 3.4, 120, 0, 0), "almost sideways is outside");
        assertTrue(TelegraphGeometry.inside(TelegraphShape.CONE, -3.0, 0.0, 3.4, 360, 0, 0), "a whirlwind hits all round");
        assertFalse(TelegraphGeometry.inside(TelegraphShape.CONE, 4.0, 0.0, 3.4, 120, 0, 0), "beyond the radius");
        assertTrue(TelegraphGeometry.inside(TelegraphShape.LINE, 5.0, 0.8, 0, 0, 8.0, 1.0));
        assertFalse(TelegraphGeometry.inside(TelegraphShape.LINE, 5.0, 1.2, 0, 0, 8.0, 1.0));
        assertFalse(TelegraphGeometry.inside(TelegraphShape.LINE, -0.5, 0.0, 0, 0, 8.0, 1.0));
        assertFalse(TelegraphGeometry.inside(TelegraphShape.NONE, 0, 0, 5, 360, 5, 5));
    }

    @Test
    public void aLocalPointRotatesToTheWorldAlongTheAttackDirection() {
        double[] a = TelegraphGeometry.toWorld(10, 20, 0.0, 3, 1);
        assertEquals(13.0, a[0], 1e-9);
        assertEquals(21.0, a[1], 1e-9);
        double[] b = TelegraphGeometry.toWorld(10, 20, Math.PI / 2, 3, 1);
        assertEquals(9.0, b[0], 1e-9, "left of a +z attack is -x");
        assertEquals(23.0, b[1], 1e-9);
    }

    @Test
    public void theParserDropsBadRowsAndReportsThemWithoutThrowing() {
        TelegraphLibrary.Parsed p = TelegraphLibrary.parse("{\"kinds\":{\"NOPE\":{\"shape\":\"circle\"},\"SLAM\":{\"shape\":\"hexagon\"},\"CLEAVE\":{\"shape\":\"cone\",\"minWind\":9999,\"r\":-3}},"
                + "\"ultimates\":{\"brute\":\"ghost\"},\"abilities\":[]}");
        assertEquals(3, p.problems().size(), p.problems().toString());
        assertEquals(1, p.library().kinds().size());
        TelegraphDef d = p.library().kinds().get(FoundryAbility.Kind.CLEAVE);
        assertEquals(FoundryAbility.MAX_WIND, d.minWind(), "an absurd wind-up is clamped");
        assertTrue(d.radius() >= 0.5);
        assertFalse(TelegraphLibrary.parse("not json").problems().isEmpty());
        assertFalse(TelegraphLibrary.parse("[]").problems().isEmpty());
        assertEquals(0, TelegraphLibrary.parse("not json").library().kinds().size());
        assertNull(TelegraphLibrary.EMPTY.def(library0()));
    }

    private static FoundryAbility library0() {
        return FoundryAbility.clamped("x", FoundryAbility.Kind.CLEAVE, 10, 0, 3, 8, 1, 0, 0, 3, 120, 0, "road", "");
    }
}
