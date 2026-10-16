package com.newuniverse.nusmp.client.relics.hand;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the hold styles of the hand renderer are a data file nobody can see run without the game, so a typo in it (a style that does not exist, a context with no transform, a
 * number that is not a number) would show up as a relic held wrongly or as a sprite. This test parses sample text and the real transforms.txt and checks the rules the renderer
 * relies on: inheritance, the lookup order id, look, slot, the overrides of a use line, and that every use line names a style that exists.
 */
class HandTransformsTest {
    static final Path FILE = Path.of("src/main/resources/assets/nusmp/relic_hand/transforms.txt");

    static final List<String> SAMPLE = List.of(
            "# a sample", "",
            "style default size=0.9",
            "xf default ground 0 0 0 0 2 0 0.5",
            "xf default fixed 0 180 0 0 0 0 1",
            "xf default tp_right 90 180 0 0 0 0 0.6",
            "xf default tp_left 90 180 0 0 0 0 0.6",
            "xf default fp_right -20 -38 0 1 2 0 0.55",
            "xf default fp_left -20 38 0 -1 2 0 0.55",
            "xf default head 0 180 0 0 13 7 1",
            "style sprite",
            "xf sprite ground 0 0 0 0 2 0 0.5", "xf sprite fixed 0 180 0 0 0 0 1", "xf sprite tp_right 0 0 0 0 3 1 0.55", "xf sprite tp_left 0 0 0 0 3 1 0.55",
            "xf sprite fp_right 0 -90 25 1.13 3.2 1.13 0.68", "xf sprite fp_left 0 90 -25 -1.13 3.2 1.13 0.68", "xf sprite head 0 180 0 0 13 7 1",
            "style hang from=default hold=0,0.9,0 sway=7",
            "style small from=hang size=0.5 hold=0,0,0",
            "xf small fixed 0 180 0 0 0 0 1.4",
            "use slot:NECK hang",
            "use look:NECK:PENDANT small only=1",
            "use id:the_ring,id:other_ring small sway=2");

    @Test
    void inheritsEverythingThatAStyleDoesNotSay() {
        HandTransforms t = HandTransforms.parse(SAMPLE);
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        assertTrue(t.complete());
        HandTransforms.Style hang = t.style("hang");
        assertEquals(0.9f, hang.fit().size(), 0f, "size comes from default");
        assertEquals(0.9f, hang.fit().hy(), 0f);
        assertEquals(7f, hang.fit().sway(), 0f);
        assertEquals(90f, hang.xf(HandTransforms.Ctx.TP_RIGHT).rx(), 0f, "the transforms come from default");
        HandTransforms.Style small = t.style("small");
        assertEquals(0.5f, small.fit().size(), 0f);
        assertEquals(0f, small.fit().hy(), 0f, "a style's own hold replaces the inherited one");
        assertEquals(7f, small.fit().sway(), 0f, "the sway is inherited through two levels");
        assertEquals(1.4f, small.xf(HandTransforms.Ctx.FIXED).s(), 0f, "an xf of its own wins");
        assertEquals(0.5f, small.xf(HandTransforms.Ctx.GROUND).s(), 0f, "the others fall through to default");
        assertTrue(small.has(HandTransforms.Ctx.GROUND));
    }

    @Test
    void looksUpIdThenLookThenSlotThenDefault() {
        HandTransforms t = HandTransforms.parse(SAMPLE);
        assertEquals("id:the_ring", t.keyFor("the_ring", "NECK", "PENDANT"));
        assertEquals("small", t.styleFor("the_ring", "NECK", "PENDANT").name);
        assertEquals(2f, t.styleFor("the_ring", "NECK", "PENDANT").fit().sway(), 0f, "the use line overrides");
        assertEquals("look:NECK:PENDANT", t.keyFor("x", "neck", "pendant"), "keys are case-insensitive in slot and look");
        assertEquals(1, t.styleFor("x", "NECK", "PENDANT").fit().only());
        assertEquals("hang", t.styleFor("x", "NECK", "AMULET").name);
        assertEquals("default", t.styleFor("x", "CHARM", "ORB").name);
        assertNull(t.keyFor("x", "CHARM", "ORB"));
        assertEquals("small", t.styleFor("other_ring", "CHARM", "CHARM").name, "a comma list gives every key the style");
    }

    @Test
    void reportsBrokenLinesAndKeepsTheRest() {
        HandTransforms t = HandTransforms.parse(List.of("style default", "xf default ground 0 0 0 0 0 0 1", "xf nowhere ground 0 0 0 0 0 0 1", "xf default sideways 0 0 0 0 0 0 1",
                "xf default fixed 0 0 x 0 0 0 1", "xf default head 0 0 0 0 0 1", "style a size=big", "style b weight=3", "style c from=c", "style d from=missing", "use id:q ghost"));
        assertEquals(8, t.problems().size(), t.problems().toString());
        assertNotNull(t.style("default"));
        assertNull(t.style("a"));
        assertNull(t.style("c"));
        assertFalse(t.complete(), "no sprite style");
        assertEquals("default", t.styleFor("q", "CHARM", "ORB").name, "a use of a missing style falls back to default");
    }

    @Test
    void contextsAreFoundByName() {
        assertEquals(HandTransforms.Ctx.TP_LEFT, HandTransforms.Ctx.byName("TP_left"));
        assertNull(HandTransforms.Ctx.byName("sideways"));
        assertNull(HandTransforms.Ctx.byName(null));
    }

    // ================================================================ the real file
    static HandTransforms real() throws IOException { return HandTransforms.parse(Files.readAllLines(FILE)); }

    @Test
    void theRealFileParsesCleanlyAndIsComplete() throws IOException {
        HandTransforms t = real();
        assertTrue(t.problems().isEmpty(), "transforms.txt has bad lines: " + t.problems());
        assertTrue(t.complete(), "the default and sprite styles must give every context");
        assertTrue(t.styleCount() >= 10, "styles read: " + t.styleCount());
        assertTrue(t.useCount() >= 40, "use lines read: " + t.useCount());
    }

    @Test
    void everyStyleGivesEveryContextAndEveryUseNamesAStyle() throws IOException {
        HandTransforms t = real();
        for (String name : List.of("default", "sprite", "dangle", "hang", "palm", "small", "tool", "flask", "brim", "cuff", "cloak", "garment", "coil", "boots")) {
            HandTransforms.Style s = t.style(name);
            assertNotNull(s, "style " + name);
            for (HandTransforms.Ctx c : HandTransforms.Ctx.values()) assertTrue(s.has(c), name + " has no transform for " + c);
            assertTrue(s.fit().size() > 0.2f && s.fit().size() <= 1.2f, name + " size " + s.fit().size());
            for (float f : new float[]{s.fit().hx(), s.fit().hy(), s.fit().hz()}) assertTrue(Math.abs(f) <= 1.5f, name + " hold " + f);
        }
        for (String key : t.useKeys()) {
            assertTrue(key.startsWith("id:") || key.startsWith("look:") || key.startsWith("slot:"), "key " + key);
            String style = t.useStyleName(key);
            assertNotNull(t.style(style), key + " names the style " + style + " that does not exist");
        }
    }

    @Test
    void theHandStylesAreFiniteAndScaledSensibly() throws IOException {
        HandTransforms t = real();
        for (String name : List.of("default", "sprite", "dangle", "hang", "palm", "small", "tool", "flask", "brim", "cuff", "cloak", "garment", "coil", "boots")) {
            for (HandTransforms.Ctx c : HandTransforms.Ctx.values()) {
                HandTransforms.Xf x = t.style(name).xf(c);
                for (float v : new float[]{x.rx(), x.ry(), x.rz(), x.tx(), x.ty(), x.tz()}) assertTrue(Float.isFinite(v) && Math.abs(v) <= 360f, name + " " + c);
                assertTrue(x.s() > 0.2f && x.s() <= 2f, name + " " + c + " scale " + x.s());
            }
        }
    }

    /** An angle in degrees brought into -180..180, so a sum that is a whole number of turns is 0 (180 + 180 and 180 - 180 both are). */
    static double turn(double a) { return ((a + 180.0) % 360.0 + 360.0) % 360.0 - 180.0; }

    @Test
    void theLeftHandIsTheRightHandMirrored() throws IOException {
        HandTransforms t = real();
        for (String name : List.of("default", "sprite", "dangle", "hang", "palm", "small", "tool", "flask", "brim", "cuff", "cloak", "garment", "coil", "boots")) {
            for (HandTransforms.Ctx[] pair : new HandTransforms.Ctx[][]{{HandTransforms.Ctx.TP_RIGHT, HandTransforms.Ctx.TP_LEFT}, {HandTransforms.Ctx.FP_RIGHT, HandTransforms.Ctx.FP_LEFT}}) {
                HandTransforms.Xf r = t.style(name).xf(pair[0]), l = t.style(name).xf(pair[1]);
                String at = name + " " + pair[1];
                assertEquals(r.rx(), l.rx(), 1e-4, at + " pitch");
                assertEquals(0.0, turn(r.ry() + l.ry()), 1e-4, at + " yaw is mirrored");
                assertEquals(0.0, turn(r.rz() + l.rz()), 1e-4, at + " roll is mirrored");
                assertEquals(r.tx(), -l.tx(), 1e-4, at + " sideways offset is mirrored");
                assertEquals(r.ty(), l.ty(), 1e-4, at + " height");
                assertEquals(r.tz(), l.tz(), 1e-4, at + " depth");
                assertEquals(r.s(), l.s(), 1e-4, at + " scale");
            }
        }
    }
}
