package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The runtime safety net: scale up to the class minimum, turn tip-down weapons over, mirror a head that faces the player; never touch a good one. */
class WeaponNormaliserTest {
    @SuppressWarnings("unchecked")
    static List<float[]>[] layers(List<float[]> body) {
        List<float[]>[] l = new List[]{body, new ArrayList<float[]>(), new ArrayList<float[]>()};
        for (float[] v : body) l[2].add(v.clone());               // the glow layer repeats the geometry
        return l;
    }

    static final WeaponClasses CLASSES = WeaponClasses.defaults();

    @Test
    void aTinyHandAxeIsScaledUpToItsClassMinimumAboutTheGrip() {
        List<float[]> body = WeaponOrientationTest.axe(-14, 0);
        float[] place = {0.1f, 0.1f, 45f, 0.008f, 0f};                  // 60 units = 0.48 blocks * 0.85 = 0.408 apparent: below 0.75 * 1.10
        WeaponClasses.Cls cls = CLASSES.classOf("x_hand_axe");
        float ox = place[0], oy = place[1];
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(body), place, "handaxe", cls, 0.85f);
        assertTrue(r.changed());
        assertEquals(1.10f / 0.408f, r.scaleFactor(), 1e-3);
        assertEquals(0.008f * r.scaleFactor(), place[3], 1e-6);
        float apparent = 60f * place[3] * 0.85f;
        assertEquals(1.10f, apparent, 1e-3, "the class minimum");
        // the grip stays: the point 25% up the original length keeps its position
        double a = Math.toRadians(45);
        double gripX0 = ox + Math.cos(a) * cls.grip() * 0.48, gripX1 = place[0] + Math.cos(a) * cls.grip() * 60 * place[3];
        assertEquals(gripX0, gripX1, 1e-4);
        assertTrue(r.log().contains("too small"));
    }

    @Test
    void aWeaponAtSeventyFivePercentOfTheMinimumOrMoreIsLeftAlone() {
        List<float[]> body = WeaponOrientationTest.axe(-14, 0);
        float[] place = {0.1f, 0.1f, 45f, 0.017f, 0f};                  // 1.02 * 0.85 = 0.867 >= 0.825 = 0.75 * 1.10
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(body), place, "handaxe", CLASSES.classOf("x_hand_axe"), 0.85f);
        assertFalse(r.changed());
        assertEquals(0.017f, place[3], 0f);
        assertEquals("", r.log());
    }

    @Test
    void aHeadFacingThePlayerIsMirroredAndTheSecondRunChangesNothing() {
        float[] place = {0.1f, 0.1f, 45f, 0.02f, 0f};
        List<float[]>[] l = layers(WeaponOrientationTest.axe(0, 14));
        WeaponClasses.Cls cls = CLASSES.classOf("x_hand_axe");
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(l, place, "handaxe", cls, 0.85f);
        assertTrue(r.flipped());
        assertFalse(r.rotated());
        WeaponOrientation.Measures m = WeaponOrientation.measure(l[0], place[3]);
        assertTrue(m.imbalance < -0.03);
        WeaponOrientation.Measures g = WeaponOrientation.measure(l[2], place[3]);
        assertTrue(g.imbalance < -0.03, "the glow layer is mirrored with the body");
        WeaponNormaliser.Result again = WeaponNormaliser.normalise(l, place, "handaxe", cls, 0.85f);
        assertFalse(again.changed(), "a corrected mesh is stable");
    }

    @Test
    void aPlaceLineThatPointsDownIsTurnedOver() {
        float[] place = {0.9f, 0.9f, -135f, 0.02f, 0f};
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(WeaponOrientationTest.axe(-14, 0)), place, "longsword", CLASSES.classOf("x_sword"), 1.0f);
        assertTrue(r.rotated());
        assertEquals(45f, place[2], 1e-4);
    }

    @Test
    void aHeadAtTheButtIsTurnedOver() {
        List<float[]> v = new ArrayList<>();
        WeaponOrientationTest.box(v, -1, 1, 0, 60, -1, 1);
        WeaponOrientationTest.box(v, -20, 0, 2, 22, -2, 2);
        float[] place = {0.1f, 0.1f, 45f, 0.02f, 0f};
        List<float[]>[] l = layers(v);
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(l, place, "battleaxe", CLASSES.classOf("x_axe"), 0.85f);
        assertTrue(r.rotated());
        WeaponOrientation.Measures m = WeaponOrientation.measure(l[0], place[3]);
        assertTrue(m.centroid > 0.5, "head now at the top: " + m.centroid);
        assertTrue(m.imbalance < 0.012, "and not toward the player: " + m.imbalance);
    }

    @Test
    void aHeldItemBelowSeventyFivePercentOfTheItemMinimumIsRaised() {
        List<float[]> v = new ArrayList<>();
        WeaponOrientationTest.box(v, -1, 1, 0, 2, -1, 1);                 // 2 units
        float[] place = {0.5f, 0.5f, 90f, 0.2f, 0f};                      // 0.4 blocks * 0.55 = 0.22 apparent: below 0.75 * 0.5
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(v), place, "handitem", new WeaponClasses(null, java.util.Map.of("fortune_die", "handitem")).classOf("fortune_die"), 0.55f);
        assertTrue(r.changed());
        assertEquals(WeaponClasses.ITEM_MIN_EXTENT, 2 * place[3] * 0.55f, 1e-3, "the largest dimension now reaches the item minimum");
    }

    @Test
    void aSwordBuiltPointFirstIsTurnedOverAlongItsLengthAndTheEdgeKeepsItsSide() {
        List<float[]> v = reversedSword();
        float[] place = {0.1f, 0.1f, 45f, 0.025f, 0f};                    // 60 units = 1.5 blocks * 0.85 = 1.275 apparent: inside the shortsword class, so only the flip happens
        List<float[]>[] l = layers(v);
        float xmin = Float.MAX_VALUE, xmax = -Float.MAX_VALUE;
        for (float[] p : l[0]) { xmin = Math.min(xmin, p[0]); xmax = Math.max(xmax, p[0]); }
        WeaponClasses.Cls cls = CLASSES.classOf("x_shortsword");
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(l, place, "shortsword", cls, 0.85f);
        assertTrue(r.changed());
        assertTrue(r.gripFlipped());
        assertFalse(r.rotated());
        assertFalse(r.flipped());
        assertTrue(r.log().contains("grip at the far end"), r.log());
        WeaponOrientation.Measures m = WeaponOrientation.measure(l[0], place[3]);
        assertTrue(m.widestAt < 0.4, "the guard is low now: " + m.widestAt);
        assertFalse(WeaponOrientation.detect(m, "shortsword", 45).contains(WeaponOrientation.Flag.GRIP_FAR_END));
        float x2min = Float.MAX_VALUE, x2max = -Float.MAX_VALUE;
        for (float[] p : l[0]) { x2min = Math.min(x2min, p[0]); x2max = Math.max(x2max, p[0]); }
        assertEquals(xmin, x2min, 0f, "no x moved: the edge keeps its side");
        assertEquals(xmax, x2max, 0f);
        assertFalse(WeaponNormaliser.normalise(l, place, "shortsword", cls, 0.85f).changed(), "a corrected mesh is stable");
        // the glow layer turned with the body
        WeaponOrientation.Measures g = WeaponOrientation.measure(l[2], place[3]);
        assertTrue(g.widestAt < 0.4, "the glow layer follows: " + g.widestAt);
    }

    @Test
    void aGripDirectiveIsTrustedBothWays() {
        float[] place = {0.1f, 0.1f, 45f, 0.025f, 0f};
        WeaponClasses.Cls cls = CLASSES.classOf("x_shortsword");
        assertFalse(WeaponNormaliser.normalise(layers(reversedSword()), place.clone(), "shortsword", cls, 0.85f, 0.12f).changed(),
                "`grip 0.12` says the hilt is at the butt: no geometry guess");
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(WeaponOrientationTest.axe(-14, 0)), place.clone(), "shortsword", cls, 0.85f, 0.9f);
        assertTrue(r.gripFlipped(), "`grip 0.9` says the hilt is at the far end: turned over whatever the shape");
    }

    /** A blade with its point at y = 0, an ornate guard at 40 to 52, a thin grip and a pommel on top: the Windcutter's shape. */
    static List<float[]> reversedSword() {
        List<float[]> v = new ArrayList<>();
        WeaponOrientationTest.box(v, -3, 3, 4, 44, -0.4f, 0.4f);
        WeaponOrientationTest.box(v, -0.5f, 0.5f, 0, 4, -0.2f, 0.2f);
        WeaponOrientationTest.box(v, -9, 9, 40, 52, -3, 3);
        WeaponOrientationTest.box(v, -0.8f, 0.8f, 52, 58, -0.8f, 0.8f);
        WeaponOrientationTest.box(v, -1.2f, 1.2f, 58, 60, -1.2f, 1.2f);
        return v;
    }

    @Test
    void anAbsurdlySmallMeshIsCappedNotBlownUp() {
        List<float[]> v = WeaponOrientationTest.axe(-14, 0);
        float[] place = {0.1f, 0.1f, 45f, 0.0001f, 0f};
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(v), place, "greatsword", CLASSES.classOf("x_greatsword"), 0.85f);
        assertEquals(WeaponNormaliser.MAX_SCALE_UP, r.scaleFactor(), 1e-6);
    }

    @Test
    void nothingHappensWithoutAClassOrWithAnEmptyMesh() {
        float[] place = {0.1f, 0.1f, 45f, 0.02f, 0f};
        assertFalse(WeaponNormaliser.normalise(layers(WeaponOrientationTest.axe(0, 14)), place, "handaxe", null, 0.85f).changed());
        assertFalse(WeaponNormaliser.normalise(layers(new ArrayList<>()), place, "handaxe", CLASSES.classOf("x_hand_axe"), 0.85f).changed());
    }
}
