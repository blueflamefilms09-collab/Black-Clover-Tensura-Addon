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
        float[] place = {0.1f, 0.1f, 45f, 0.005f, 0f};                  // 60 units = 0.30 blocks * 0.85 = 0.255 apparent: below 0.6 * 0.70
        WeaponClasses.Cls cls = CLASSES.classOf("x_hand_axe");
        float ox = place[0], oy = place[1];
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(body), place, "handaxe", cls, 0.85f);
        assertTrue(r.changed());
        assertEquals(0.70f / 0.255f, r.scaleFactor(), 1e-3);
        assertEquals(0.005f * r.scaleFactor(), place[3], 1e-6);
        float apparent = 60f * place[3] * 0.85f;
        assertEquals(0.70f, apparent, 1e-3, "the class minimum");
        // the grip stays: the point 25% up the original length keeps its position
        double a = Math.toRadians(45);
        double gripX0 = ox + Math.cos(a) * cls.grip() * 0.30, gripX1 = place[0] + Math.cos(a) * cls.grip() * 60 * place[3];
        assertEquals(gripX0, gripX1, 1e-4);
        assertTrue(r.log().contains("too small"));
    }

    @Test
    void aWeaponInsideSixtyPercentOfTheMinimumIsLeftAlone() {
        List<float[]> body = WeaponOrientationTest.axe(-14, 0);
        float[] place = {0.1f, 0.1f, 45f, 0.009f, 0f};                  // 0.54 * 0.85 = 0.459 >= 0.42
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(body), place, "handaxe", CLASSES.classOf("x_hand_axe"), 0.85f);
        assertFalse(r.changed());
        assertEquals(0.009f, place[3], 0f);
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
    void aHeldItemBelowSixtyPercentOfTheItemMinimumIsRaised() {
        List<float[]> v = new ArrayList<>();
        WeaponOrientationTest.box(v, -1, 1, 0, 2, -1, 1);                 // 2 units
        float[] place = {0.5f, 0.5f, 90f, 0.1f, 0f};                      // 0.2 blocks * 0.55 = 0.11 apparent: below 0.6 * 0.4
        WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers(v), place, "item", CLASSES.classOf("fortune_die"), 0.55f);
        assertTrue(r.changed());
        assertEquals(WeaponClasses.ITEM_MIN_EXTENT, 2 * place[3] * 0.55f, 1e-3, "the largest dimension now reaches the item minimum");
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
