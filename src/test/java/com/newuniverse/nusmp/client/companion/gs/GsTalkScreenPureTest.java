package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.ui.gs.GsPalette;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Great Spirit talk screen is drawn by pure code (layout, portrait plan, icons, painter) so it can be checked without a game: every window size the GUI scale can produce must fit, every
 * rectangle must stay inside the panel, a click must find the widget that is drawn, a frame must cost a bounded number of draw calls, and the portrait's expression rectangles must stay on the face.
 */
class GsTalkScreenPureTest {
    private static final int[][] WINDOWS = {{200, 150}, {240, 135}, {260, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {1000, 600}};

    // ---------------------------------------------------------------- the portrait
    @Test
    void everyExpressionRectangleStaysOnTheFace() {
        for (Mood m : Mood.values()) {
            for (int[] r : PortraitPlan.rects(m)) {
                assertEquals(5, r.length);
                assertTrue(r[0] >= 0 && r[1] >= 0 && r[2] > 0 && r[3] > 0, m + " " + java.util.Arrays.toString(r));
                assertTrue(r[0] + r[2] <= 16, m + " x " + java.util.Arrays.toString(r));
                assertTrue(r[1] + r[3] <= 18, m + " y (a drop may hang under the chin) " + java.util.Arrays.toString(r));
            }
        }
        for (int[] r : PortraitPlan.blinkRects()) assertTrue(r[0] + r[2] <= 16 && r[1] + r[3] <= 16);
        assertTrue(PortraitPlan.maxRects() >= PortraitPlan.blinkRects().length);
        assertEquals(0, PortraitPlan.rects(Mood.NEUTRAL).length, "the neutral face is the skin's own");
        assertEquals(0, PortraitPlan.rects(null).length);
    }

    @Test
    void blinkBreathAndShakeAreSmallAndPeriodic() {
        int blinks = 0;
        for (int t = 0; t < 103; t++) if (PortraitPlan.blinking(t, Mood.NEUTRAL)) blinks++;
        assertTrue(blinks > 0 && blinks < 20);
        for (int t = 0; t < 400; t++) assertFalse(PortraitPlan.blinking(t, Mood.HAPPY), "a smiling face already has closed eyes");
        for (float t = -50; t < 500; t += 3) {
            int b = PortraitPlan.breath(t);
            assertTrue(b == 0 || b == 1);
        }
        for (float a = 0; a < 60; a += 1) {
            int s = PortraitPlan.shake(Mood.ANGRY, a);
            assertTrue(Math.abs(s) <= 1);
            if (a >= 24) assertEquals(0, s);
        }
        assertEquals(0, PortraitPlan.shake(Mood.HAPPY, 3f));
    }

    @Test
    void theScaleFitsTheBoxAndTheSparklesStayInside() {
        for (int w = 20; w < 130; w += 7) for (int h = 20; h < 130; h += 9) {
            int s = PortraitPlan.scaleFor(w, h, 7);
            assertTrue(s >= 1 && s <= 7);
            if (s > 1) assertTrue(15 * s <= w + 1 && 13 * s <= h + 1, w + "x" + h + " scale " + s);
        }
        int[] out = new int[3];
        for (Mood m : Mood.values()) {
            assertTrue(PortraitPlan.auraCount(m) <= PortraitPlan.MAX_AURA);
            for (int i = 0; i < PortraitPlan.auraCount(m); i++) for (float t = 0; t < 240; t += 5) {
                assertTrue(PortraitPlan.aura(m, i, t, 94, 90, out));
                assertTrue(out[2] >= 0 && out[2] <= 255, "alpha " + out[2]);
                assertTrue(out[0] > -10 && out[0] < 104 && out[1] > -10 && out[1] < 100, m + " " + out[0] + "," + out[1]);
            }
            assertFalse(PortraitPlan.aura(m, PortraitPlan.auraCount(m), 0f, 94, 90, out));
            assertEquals(0, PortraitPlan.auraColor(m) >>> 24);
        }
        assertEquals(3, PortraitPlan.px(2, 3));
        assertEquals(1, PortraitPlan.pxSize(1, 1));
    }

    @Test
    void theSkinIsReadOnlyWhenItIsAStandardOne() {
        int[] skin = new int[64 * 64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) skin[y * 64 + x] = 0xFFC8A078;
        FaceInfo wide = FaceInfo.from(skin, 64, 64);
        assertTrue(wide.has());
        assertFalse(wide.slim());
        assertFalse(wide.legacy());
        assertEquals(4, wide.armW());
        assertEquals(0xFFC8A078, wide.tone());
        skin[24 * 64 + 47] = 0;
        FaceInfo slim = FaceInfo.from(skin, 64, 64);
        assertTrue(slim.slim());
        assertEquals(3, slim.armW());
        FaceInfo old = FaceInfo.from(new int[64 * 32], 64, 32);
        assertTrue(old.has() && old.legacy());
        assertEquals(FaceInfo.DEFAULT_TONE, old.tone(), "a transparent face keeps the default tone");
        for (int[] bad : new int[][]{{64, 48}, {32, 32}, {128, 128}, {64, 16}}) assertSame(FaceInfo.NONE, FaceInfo.from(new int[bad[0] * bad[1]], bad[0], bad[1]));
        assertSame(FaceInfo.NONE, FaceInfo.from(null, 64, 64));
        assertSame(FaceInfo.NONE, FaceInfo.from(new int[10], 64, 64));
        assertFalse(FaceInfo.NONE.has());
    }

    // ---------------------------------------------------------------- the icons and the palette
    @Test
    void everyIconIsNineByNineAndCheap() {
        for (GsIcons.Icon i : GsIcons.Icon.values()) {
            String[] rows = GsIcons.rows(i);
            assertEquals(GsIcons.SIZE, rows.length, i.name());
            for (String r : rows) {
                assertEquals(GsIcons.SIZE, r.length(), i.name());
                for (char c : r.toCharArray()) assertTrue(c == '#' || c == '+' || c == '.', i.name());
            }
            assertTrue(GsIcons.rects(i) > 0 && GsIcons.rects(i) <= 20, i.name() + " " + GsIcons.rects(i));
        }
    }

    @Test
    void thePaletteHasAColourPerSkin() {
        for (GsSkin s : GsSkin.values()) {
            int k = s.ordinal();
            assertNotEquals(GsPalette.PANEL_TOP[k], GsPalette.TEXT[k]);
            assertTrue(GsPalette.NEON[k] >= 0 && GsPalette.NEON[k] <= 0xFFFFFF);
        }
    }

    // ---------------------------------------------------------------- the layout
    @Test
    void everyRectangleFitsThePanelAtEveryWindowSize() {
        for (int[] win : WINDOWS) for (int tab = 0; tab < GsTalkLayout.TABS; tab++) {
            GsTalkLayout L = GsTalkLayout.compute(win[0], win[1], tab);
            String where = win[0] + "x" + win[1] + " tab " + tab;
            int[] p = L.panel;
            assertTrue(p[0] >= 0 && p[1] >= 0 && p[0] + p[2] <= win[0] && p[1] + p[3] <= win[1] || win[0] < 184 || win[1] < 138, "panel in window " + where);
            inside(p, L.portrait, "portrait " + where);
            inside(p, L.plate, "plate " + where);
            inside(p, L.meter, "meter " + where);
            inside(p, L.moodChip, "mood " + where);
            inside(p, L.tabs, "tabs " + where);
            inside(p, L.dialogue, "dialogue " + where);
            inside(p, L.close, "close " + where);
            for (int i = 0; i < GsTalkLayout.TABS; i++) inside(p, L.tabRect(i), "tab " + i + " " + where);
            assertTrue(L.dialogue[1] >= L.tabs[1] + L.tabs[3], "dialogue under tabs " + where);
            assertTrue(L.dialogue[1] + L.dialogue[3] <= L.close[1] + 1, "dialogue above the footer " + where);
            if (tab == 0 || tab == 1) {
                for (int s = 0; s < L.visibleRows; s++) inside(p, L.rowRect(s), "row " + s + " " + where);
                if (tab == 0) assertTrue(L.list[1] >= L.dialogue[1] + L.dialogue[3], "rows under the dialogue " + where);
            }
            if (tab == 1) for (int[] t : L.tile) inside(p, t, "tile " + where);
            if (tab == 2) {
                inside(p, L.giftSlot, "gift slot " + where);
                inside(p, L.give, "give " + where);
                inside(p, L.likes, "likes " + where);
                for (int[] sb : L.stanceBtn) inside(p, sb, "stance " + where);
                inside(p, L.picker, "picker " + where);
                for (int i = 0; i < 36; i++) inside(L.picker, L.pickerSlot(i), "picker slot " + i + " " + where);
                for (int i = 0; i < L.tasteCells(); i++) inside(L.tastes, L.tasteSlot(i), "taste " + i + " " + where);
                if (L.storyLines > 0) inside(p, L.storyArea, "story " + where);
            }
            if (tab == 3) {
                for (int i = 0; i < 9; i++) inside(p, L.pouchSlot(i), "pouch " + i + " " + where);
                if (L.stripShown) for (int i = 0; i < 9; i++) inside(p, L.hotbarSlot(i), "hotbar " + i + " " + where);
                else {
                    GsTalkFrame hf = frame(GsSkin.B, 3);
                    for (int i = 0; i < 9; i++) {
                        int[] hr = L.hotbarSlot(i);
                        assertFalse(GsHit.isHotbar(L.hit(hr[0] + 2, hr[1] + 2, hf)), "no hidden hotbar hit " + where);
                    }
                }
                inside(p, L.use, "use " + where);
                inside(p, L.put, "put " + where);
                inside(p, L.takeAll, "take all " + where);
                if (L.ringShown) inside(p, L.ring, "ring " + where);
            }
        }
    }

    private static void inside(int[] outer, int[] r, String what) {
        assertTrue(r[0] >= outer[0] && r[1] >= outer[1] && r[0] + r[2] <= outer[0] + outer[2] && r[1] + r[3] <= outer[1] + outer[3], what + " " + java.util.Arrays.toString(r) + " not in " + java.util.Arrays.toString(outer));
    }

    @Test
    void theHotbarStripAndTheBigSlotsAreThereAtTheCommonGuiSizes() {
        for (int[] win : new int[][]{{320, 180}, {384, 216}, {427, 240}, {480, 270}, {640, 360}}) {
            GsTalkLayout L = GsTalkLayout.compute(win[0], win[1], 3);
            assertTrue(L.stripShown, win[0] + "x" + win[1]);
            assertTrue(L.pouchPitch >= 20, win[0] + "x" + win[1] + " pitch " + L.pouchPitch);
            assertTrue(L.ringShown, win[0] + "x" + win[1]);
        }
        GsTalkLayout big = GsTalkLayout.compute(640, 360, 3);
        assertEquals(GsTalkLayout.BIG_SLOT, big.pouchPitch);
        GsTalkLayout small = GsTalkLayout.compute(200, 150, 3);
        assertFalse(small.stripShown, "a small window drops the strip and Put opens the picker instead");
    }

    @Test
    void theLeftColumnStacksWithoutOverlap() {
        for (int[] win : WINDOWS) {
            GsTalkLayout L = GsTalkLayout.compute(win[0], win[1], 0);
            assertTrue(L.plate[1] >= L.portrait[1] + L.portrait[3], "plate under the portrait " + win[0]);
            assertTrue(L.meter[1] >= L.plate[1] + L.plate[3]);
            assertTrue(L.moodChip[1] >= L.meter[1] + L.meter[3] - 1);
            assertTrue(L.portraitScale >= 1 && L.portraitScale <= 7);
            assertTrue(L.tabs[0] >= L.portrait[0] + L.portrait[2], "the right column starts after the left one");
        }
    }

    @Test
    void aClickFindsTheWidgetThatIsDrawnThere() {
        GsTalkFrame f = frame(GsSkin.B, 2);
        GsTalkLayout L = GsTalkLayout.compute(427, 240, 2);
        assertEquals(GsHit.CLOSE, L.hit(L.close[0] + 2, L.close[1] + 2, f));
        for (int i = 0; i < GsTalkLayout.TABS; i++) {
            int[] r = L.tabRect(i);
            assertEquals(GsHit.TAB + i, L.hit(r[0] + 1, r[1] + 1, f));
        }
        assertEquals(GsHit.PORTRAIT, L.hit(L.portrait[0] + 5, L.portrait[1] + 5, f));
        assertEquals(GsHit.METER, L.hit(L.meter[0] + 5, L.meter[1] + 5, f));
        assertEquals(GsHit.DIALOGUE, L.hit(L.dialogue[0] + 5, L.dialogue[1] + 5, f));
        assertEquals(GsHit.GIFT_SLOT, L.hit(L.giftSlot[0] + 3, L.giftSlot[1] + 3, f));
        assertEquals(GsHit.GIVE, L.hit(L.give[0] + 3, L.give[1] + 3, f));
        assertEquals(GsHit.LIKES, L.hit(L.likes[0] + 3, L.likes[1] + 3, f));
        for (int i = 0; i < 3; i++) assertEquals(GsHit.STANCE + i, L.hit(L.stanceBtn[i][0] + 2, L.stanceBtn[i][1] + 2, f));
        f.hasGift = false;
        assertTrue(GsHit.GIFT_SLOT != L.hit(L.giftSlot[0] + 3, L.giftSlot[1] + 3, f), "a widget the server did not offer cannot be hit");
        f.stanceOffered = false;
        assertFalse(GsHit.isStance(L.hit(L.stanceBtn[0][0] + 2, L.stanceBtn[0][1] + 2, f)));
        assertEquals(GsHit.NONE, L.hit(0, 0, f));
        assertEquals(GsHit.NONE, L.hit(-5, 9999, f));

        f.tab = 3;
        GsTalkLayout P = GsTalkLayout.compute(427, 240, 3);
        for (int i = 0; i < 9; i++) {
            int[] r = P.pouchSlot(i);
            assertEquals(GsHit.POUCH + i, P.hit(r[0] + 2, r[1] + 2, f));
            int[] hb = P.hotbarSlot(i);
            assertEquals(GsHit.HOTBAR + i, P.hit(hb[0] + 2, hb[1] + 2, f));
        }
        assertEquals(GsHit.USE, P.hit(P.use[0] + 2, P.use[1] + 2, f));
        assertEquals(GsHit.PUT, P.hit(P.put[0] + 2, P.put[1] + 2, f));
        assertEquals(GsHit.TAKE_ALL, P.hit(P.takeAll[0] + 2, P.takeAll[1] + 2, f));
        assertEquals(GsHit.RING, P.hit(P.ring[0] + 2, P.ring[1] + 2, f));
    }

    @Test
    void theOpenPickerTakesTheClickBeforeAnythingUnderIt() {
        GsTalkFrame f = frame(GsSkin.B, 2);
        f.pickerOpen = true;
        GsTalkLayout L = GsTalkLayout.compute(427, 240, 2);
        for (int i = 0; i < 36; i++) {
            int[] r = L.pickerSlot(i);
            assertEquals(GsHit.PICKER + i, L.hit(r[0] + 2, r[1] + 2, f), "picker slot " + i);
        }
        assertTrue(GsHit.isPicker(GsHit.PICKER) && GsHit.isPicker(GsHit.PICKER + 35) && !GsHit.isPicker(GsHit.PICKER + 36));
        int x = L.picker[0] + 1, y = L.picker[1] + 1;
        assertEquals(GsHit.DIALOGUE, L.hit(x, y, f), "the popup's own surface swallows the click");
        f.pickerOpen = false;
        assertFalse(GsHit.isPicker(L.hit(L.picker[0] + 6, L.picker[1] + 14, f)));
    }

    @Test
    void theRowsScrollAndTheLastRowIsReachable() {
        GsTalkLayout L = GsTalkLayout.compute(320, 240, 0);
        assertEquals(0, L.maxScroll(1));
        assertEquals(0, L.maxScroll(L.visibleRows));
        assertEquals(5, L.maxScroll(L.visibleRows + 5));
        GsTalkFrame f = frame(GsSkin.B, 0);
        for (int i = 0; i < L.visibleRows + 4; i++) f.rows.add(new GsTalkFrame.Row("row " + i, false));
        f.scroll = L.maxScroll(f.rows.size());
        int[] last = L.rowRect(L.visibleRows - 1);
        assertEquals(GsHit.ROW + f.rows.size() - 1, L.hit(last[0] + 3, last[1] + 3, f));
        assertTrue(GsHit.isRow(GsHit.ROW) && GsHit.rowIndex(GsHit.ROW + 7) == 7);
    }

    @Test
    void everyHitCodeIsDifferentAndTheRangesDoNotOverlap() {
        int[][] ranges = {{GsHit.TAB, 4}, {GsHit.CLOSE, 1}, {GsHit.DIALOGUE, 1}, {GsHit.PORTRAIT, 1}, {GsHit.METER, 1}, {GsHit.MOOD, 1}, {GsHit.GIFT_SLOT, 1}, {GsHit.GIVE, 1}, {GsHit.LIKES, 1},
                {GsHit.FIVE, 1}, {GsHit.FOLLOW, 1}, {GsHit.PANEL, 1}, {GsHit.STANCE, 3}, {GsHit.USE, 1}, {GsHit.PUT, 1}, {GsHit.TAKE_ALL, 1}, {GsHit.RING, 1}, {GsHit.POUCH, 9}, {GsHit.HOTBAR, 9},
                {GsHit.PICKER, 36}, {GsHit.TASTE, 8}};
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int[] r : ranges) for (int i = 0; i < r[1]; i++) assertTrue(seen.add(r[0] + i), "code " + (r[0] + i) + " is used twice");
        assertTrue(seen.stream().allMatch(c -> c >= 0 && c < GsHit.ROW));
    }

    // ---------------------------------------------------------------- the painter
    /** A canvas that only counts (and checks nothing is drawn far outside the window). */
    private static final class Counter implements GsCanvas {
        int fills, gradients, sprites, texts, items, compiled, clips, unclips, outside;
        final int w, h;

        Counter(int w, int h) {
            this.w = w;
            this.h = h;
        }

        private void box(int x1, int y1, int x2, int y2) {
            if (x2 < x1 || y2 < y1 || x1 < -2 || y1 < -2 || x2 > w + 2 || y2 > h + 2) outside++;
        }

        @Override public void fill(int x1, int y1, int x2, int y2, int argb) { fills++; box(x1, y1, x2, y2); }
        @Override public void text(String s, int x, int y, int argb) { texts++; assertTrue(s != null); }
        @Override public int textWidth(String s) { return s == null ? 0 : s.length() * 6; }
        @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { gradients++; box(x1, y1, x2, y2); }
        @Override public void clip(int x1, int y1, int x2, int y2) { clips++; }
        @Override public void unclip() { unclips++; }
        @Override public void sprite(GsSprite s, GsSkin skin, int x, int y, int sw, int sh, int u, int v, int uw, int vh, int tint, boolean additive) { sprites++; }
        @Override public void item(int ref, int x, int y) { items++; }
        @Override public void compiled(String[] lines, int count, int x, int y, int lineH, float age, int ink, int settled, int data) { compiled++; }

        int calls() { return fills + gradients + sprites + texts + items + compiled; }
    }

    private static GsTalkFrame frame(GsSkin skin, int tab) {
        GsTalkFrame f = new GsTalkFrame();
        f.skin = skin;
        f.tab = tab;
        f.name = "Charmy Pappitson";
        f.sub = "Cotton Magic user";
        f.tierName = "a friend";
        f.tierLine = "a friend";
        f.rapport = 14;
        f.rapportShown = 14;
        f.rapportText = "14";
        f.nextText = "+11";
        f.toNext = 11;
        f.moodName = "Happy";
        f.mood = Mood.HAPPY;
        f.moodAge = 5f;
        f.face = new FaceInfo(true, false, false, 0xFFC8A078, 64);
        f.time = 37f;
        f.dialogue = new String[]{"Hello! Want to share a snack?", "I know a good place to eat.", "Third line of her words.", "Fourth line."};
        f.typed = 40;
        f.dialogueDone = false;
        for (int i = 0; i < GsTalkFrame.L_COUNT; i++) f.labels[i] = "label " + i;
        for (int i = 0; i < 4; i++) f.tabNames[i] = "Tab " + i;
        f.hasFive = f.hasPanel = f.hasFollow = true;
        f.following = true;
        f.hasGift = f.hasLikes = true;
        f.heldIndex = 3;
        f.heldCount = 5;
        f.heldReaction = GiftMemory.Reaction.LOVED;
        for (int i = 0; i < 5; i++) f.tasteReactions.add(GiftMemory.Reaction.values()[i % 4]);
        f.inventoryFilled = 0xFFFFFFFFFL;
        f.inventoryReactions[3] = GiftMemory.Reaction.LIKED;
        f.stance = 1;
        f.stanceOffered = true;
        f.story = new String[]{"Rapport +2", "She loved Cake", "A high five"};
        f.storyKinds = new BondStory.Kind[]{BondStory.Kind.RAPPORT, BondStory.Kind.LOVED, BondStory.Kind.FIVE};
        f.storyAge = 10f;
        for (int i = 0; i < 9; i++) f.pouchCounts[i] = i % 2 == 0 ? "3" : "";
        f.useIt = 1;
        f.ring = PouchRing.State.READY;
        f.ringSlot = 2;
        f.hasPut = f.hasTakeAll = f.hasUse = true;
        f.hint = "1-3 / 5";
        f.litLeaf = 1;
        f.sinceRise = 4f;
        f.rapportGlow = 0.5f;
        return f;
    }

    @Test
    void aFrameIsBoundedOnEveryTabSkinAndWindow() {
        int worst = 0;
        for (GsSkin skin : GsSkin.values()) for (int[] win : WINDOWS) for (int tab = 0; tab < GsTalkLayout.TABS; tab++) for (boolean picker : new boolean[]{false, true}) {
            GsTalkLayout L = GsTalkLayout.compute(win[0], win[1], tab);
            GsTalkFrame f = frame(skin, tab);
            f.pickerOpen = picker && tab == 2;
            if (tab == 0 || tab == 1) for (int i = 0; i < 40; i++) f.rows.add(new GsTalkFrame.Row("A choice number " + i, i % 5 == 0));
            f.hover = GsHit.TAB + 1;
            f.rippleAt = f.time - 3f;
            f.rippleX = L.tabs[0] + 20;
            f.rippleY = L.tabs[1] + 5;
            Counter c = new Counter(win[0], win[1]);
            GsTalkPainter.paint(c, L, f);
            String where = skin + " " + win[0] + "x" + win[1] + " tab " + tab + (f.pickerOpen ? " picker" : "");
            assertTrue(c.calls() > 40, "something was drawn " + where);
            assertTrue(c.calls() <= 700, "draw calls " + c.calls() + " " + where);
            assertTrue(c.items <= 36 + 9 + 8 + 2, "items " + c.items + " " + where);
            assertTrue(c.compiled <= 2, "compile blocks " + c.compiled + " " + where);
            assertEquals(c.clips, c.unclips, "every clip is released " + where);
            assertEquals(0, c.outside, "fills far outside the window " + where);
            worst = Math.max(worst, c.calls());
        }
        assertTrue(worst > 0);
    }

    @Test
    void aMissingSkinAndAnEmptyViewStillDraw() {
        GsTalkFrame f = new GsTalkFrame();
        for (int tab = 0; tab < 4; tab++) {
            f.tab = tab;
            f.skin = GsSkin.C;
            GsTalkLayout L = GsTalkLayout.compute(320, 240, tab);
            Counter c = new Counter(320, 240);
            GsTalkPainter.paint(c, L, f);
            assertTrue(c.calls() > 10);
        }
    }

    @Test
    void theHighQualityFramesAreNotEmptierThanTheLowOnes() {
        GsTalkLayout L = GsTalkLayout.compute(427, 240, 0);
        GsTalkFrame hi = frame(GsSkin.B, 0), lo = frame(GsSkin.B, 0);
        hi.fancy = true;
        lo.fancy = false;
        Counter a = new Counter(427, 240), b = new Counter(427, 240);
        GsTalkPainter.paint(a, L, hi);
        GsTalkPainter.paint(b, L, lo);
        assertTrue(a.calls() >= b.calls(), a.calls() + " vs " + b.calls());
        assertTrue(a.sprites >= b.sprites);
    }

    @Test
    void theFrameLabelsHaveSafeDefaults() {
        GsTalkFrame f = new GsTalkFrame();
        for (int i = 0; i < GsTalkFrame.L_COUNT; i++) assertEquals("", f.label(i));
        assertEquals("", f.label(-1));
        assertEquals("", f.label(GsTalkFrame.L_COUNT));
        f.labels[GsTalkFrame.L_GIVE] = "Give";
        assertEquals("Give", f.label(GsTalkFrame.L_GIVE));
    }
}
