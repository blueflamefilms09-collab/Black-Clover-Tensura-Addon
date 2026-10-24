package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.companion.CompanionRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Great Spirit companion screen keeps its rules in small pure classes (which screen, what a line means, the mood clock, the rapport meter, the option kinds, the gift memory, the bond story, the heal
 * ring, the typewriter, the bubble numbers) so the real screen is only drawing and asking. They are pinned here with no Minecraft class.
 */
class CompanionGsLogicTest {
    // ---------------------------------------------------------------- which screen
    @Test
    void autoFollowsTheThemeAndTheOtherTwoForceTheScreen() {
        assertEquals(CompanionStyle.GREAT_SPIRIT, CompanionStyle.choose(CompanionStyle.Choice.AUTO, true));
        assertEquals(CompanionStyle.CLASSIC, CompanionStyle.choose(CompanionStyle.Choice.AUTO, false));
        for (boolean theme : new boolean[]{true, false}) {
            assertEquals(CompanionStyle.GREAT_SPIRIT, CompanionStyle.choose(CompanionStyle.Choice.GREAT_SPIRIT, theme));
            assertEquals(CompanionStyle.CLASSIC, CompanionStyle.choose(CompanionStyle.Choice.CLASSIC, theme));
        }
        assertEquals(CompanionStyle.Choice.GREAT_SPIRIT, CompanionStyle.Choice.parse(" great-spirit "));
        assertEquals(CompanionStyle.Choice.CLASSIC, CompanionStyle.Choice.parse("classic"));
        assertEquals(CompanionStyle.Choice.AUTO, CompanionStyle.Choice.parse("nonsense"));
        assertEquals(CompanionStyle.Choice.AUTO, CompanionStyle.Choice.parse(null));
    }

    // ---------------------------------------------------------------- what a line means
    @Test
    void aCompanionKeyHasItsOwnPrefixAndACommonKeyHasNone() {
        assertEquals("nusmp.companion.charmy.", CompanionCues.prefixOf("nusmp.companion.charmy.gift.love"));
        assertEquals("nusmp.companion.charmy.", CompanionCues.prefixOf("nusmp.companion.charmy.sub"));
        assertNull(CompanionCues.prefixOf("nusmp.companion.bond.pouch.put"));
        assertNull(CompanionCues.prefixOf("nusmp.companion.tab.talk"));
        assertNull(CompanionCues.prefixOf("nusmp.companion.gs.give"));
        assertNull(CompanionCues.prefixOf("nusmp.companion.close"));
        assertNull(CompanionCues.prefixOf("something.else"));
        assertNull(CompanionCues.prefixOf(null));
        assertNull(CompanionCues.prefixOf("nusmp.companion." + "x".repeat(200)));
    }

    @Test
    void theCueIsReadFromTheKeyAndNeverFromTheWording() {
        String p = "nusmp.companion.charmy.";
        assertEquals(CompanionCues.Cue.GIFT_LOVE, CompanionCues.cueOf(p + "gift.love"));
        assertEquals(CompanionCues.Cue.GIFT_OK, CompanionCues.cueOf(p + "gift.ok"));
        assertEquals(CompanionCues.Cue.GIFT_HATE, CompanionCues.cueOf(p + "gift.hate"));
        assertEquals(CompanionCues.Cue.GIFT_NO, CompanionCues.cueOf(p + "gift.no"));
        assertEquals(CompanionCues.Cue.FIVE, CompanionCues.cueOf(p + "act.five"));
        assertEquals(CompanionCues.Cue.JOIN_OK, CompanionCues.cueOf(p + "a.join"));
        assertEquals(CompanionCues.Cue.WAIT, CompanionCues.cueOf(p + "a.wait"));
        assertEquals(CompanionCues.Cue.USE, CompanionCues.cueOf(p + "use"));
        assertEquals(CompanionCues.Cue.STANCE, CompanionCues.cueOf(p + "stance.guard"));
        assertEquals(CompanionCues.Cue.FOOD_TALK, CompanionCues.cueOf(p + "a.food"));
        assertEquals(CompanionCues.Cue.PUT, CompanionCues.cueOf("nusmp.companion.bond.pouch.put"));
        assertEquals(CompanionCues.Cue.TOOK, CompanionCues.cueOf("nusmp.companion.bond.pouch.tookall"));
        assertEquals(CompanionCues.Cue.REFUSED, CompanionCues.cueOf("nusmp.companion.bond.pouch.full"));
        assertEquals(CompanionCues.Cue.NONE, CompanionCues.cueOf("nusmp.companion.bond.five"));
        assertEquals(CompanionCues.Cue.NONE, CompanionCues.cueOf(null));
        assertEquals(CompanionCues.Cue.NONE, CompanionCues.cueOf("block.minecraft.stone"));
    }

    @Test
    void cuesMapToMoodsAndGiftReactions() {
        assertEquals(Mood.HAPPY, CompanionCues.moodOf(CompanionCues.Cue.GIFT_LOVE));
        assertEquals(Mood.ANGRY, CompanionCues.moodOf(CompanionCues.Cue.GIFT_HATE));
        assertEquals(Mood.SHY, CompanionCues.moodOf(CompanionCues.Cue.GIFT_NO));
        assertEquals(Mood.HUNGRY, CompanionCues.moodOf(CompanionCues.Cue.FOOD_TALK));
        assertNull(CompanionCues.moodOf(CompanionCues.Cue.NONE));
        assertNull(CompanionCues.moodOf(CompanionCues.Cue.GREET));
        assertEquals(GiftMemory.Reaction.LOVED, CompanionCues.reactionOf(CompanionCues.Cue.GIFT_LOVE));
        assertEquals(GiftMemory.Reaction.REFUSED, CompanionCues.reactionOf(CompanionCues.Cue.GIFT_NO));
        assertNull(CompanionCues.reactionOf(CompanionCues.Cue.FIVE));
        for (Mood m : Mood.values()) assertTrue(CompanionCues.holdTicks(m) > 0);
    }

    // ---------------------------------------------------------------- the mood clock
    @Test
    void anEventMoodLastsItsHoldTimeAndThenTheRestingFaceReturns() {
        MoodTimer t = new MoodTimer();
        assertEquals(Mood.SHY, t.at(0f, 0), "a stranger rests shy");
        assertEquals(Mood.NEUTRAL, t.at(0f, 2));
        assertEquals(Mood.HAPPY, t.at(0f, 3), "a close friend rests happy");
        t.set(Mood.ANGRY, 100f);
        assertTrue(t.active(110f));
        assertEquals(Mood.ANGRY, t.at(110f, 3));
        float end = 100f + CompanionCues.holdTicks(Mood.ANGRY);
        assertFalse(t.active(end));
        assertEquals(Mood.HAPPY, t.at(end, 3));
        assertEquals(10f, t.age(110f), 1e-4);
        t.set(null, 5f);
        assertFalse(t.active(5000f));
    }

    // ---------------------------------------------------------------- the rapport meter
    @Test
    void theLeavesLightAtTheServersTiers() {
        assertEquals(CompanionRules.ACQUAINTANCE_AT, RapportMeter.LEAF_AT[0]);
        assertEquals(CompanionRules.FRIEND_AT, RapportMeter.LEAF_AT[1]);
        assertEquals(CompanionRules.CLOSE_AT, RapportMeter.LEAF_AT[2]);
        for (int r = 0; r <= 99; r++) {
            int lit = 0;
            for (int i = 0; i < RapportMeter.LEAVES; i++) if (RapportMeter.isLit(r, i)) lit++;
            assertEquals(CompanionRules.tier(r), lit, "rapport " + r);
            assertEquals(lit, RapportMeter.lit(r));
        }
        assertEquals(0f, RapportMeter.bar(0), 1e-6);
        assertEquals(1f, RapportMeter.bar(CompanionRules.CLOSE_AT), 1e-6);
        assertEquals(1f, RapportMeter.bar(99), 1e-6);
        float last = -1f;
        for (int r = 0; r <= 99; r++) {
            float b = RapportMeter.bar(r);
            assertTrue(b >= last);
            last = b;
        }
        assertEquals(RapportMeter.LEAF_AT[0], RapportMeter.toNext(0));
        assertEquals(0, RapportMeter.toNext(99));
        assertTrue(RapportMeter.newlyLit(2, 3, 0));
        assertFalse(RapportMeter.newlyLit(3, 4, 0));
    }

    @Test
    void aRiseGlidesGlowsAndPopsAndAFallIsInstant() {
        assertEquals(10f, RapportMeter.shown(5, 10, 100f), 1e-4);
        assertEquals(5f, RapportMeter.shown(5, 10, 0f), 1e-4);
        assertEquals(3f, RapportMeter.shown(5, 3, 0f), 1e-4, "a fall is instant");
        assertEquals(0f, RapportMeter.glow(RapportMeter.GLOW + 1f));
        assertTrue(RapportMeter.glow(0f) > RapportMeter.glow(20f));
        assertEquals(1f, RapportMeter.pop(RapportMeter.POP + 1f));
        assertTrue(RapportMeter.pop(RapportMeter.POP / 2f) > 1f);

        RapportMeter.Track tr = new RapportMeter.Track();
        assertEquals(0, tr.update(2, 0f), "the first view is no change");
        assertEquals(2f, tr.shown(0f), 1e-4);
        assertEquals(2, tr.update(4, 10f));
        assertEquals(2f, tr.shown(10f), 1e-4, "the bar starts where it was");
        assertEquals(4f, tr.shown(10f + RapportMeter.GLIDE), 1e-4);
        assertTrue(tr.glow(10f) > 0f);
        assertEquals(-3, tr.update(1, 20f));
        assertEquals(1f, tr.shown(20f), 1e-4);
        assertEquals(0f, tr.glow(20f));
        assertEquals(0, tr.update(-1, 30f), "no rapport on the view changes nothing");
        assertEquals(1, tr.rapport());
        RapportMeter.Track lit = new RapportMeter.Track();
        lit.update(2, 0f);
        lit.update(3, 1f);
        assertEquals(0, lit.litLeaf(2f), "the first leaf lit with this rise");
        assertEquals(-1, lit.litLeaf(100f));
    }

    // ---------------------------------------------------------------- what an option is
    @Test
    void theBondIdsGetTheirWidgetsAndEverythingElseIsAPlainRow() {
        assertEquals(OptionKinds.Kind.FIVE, OptionKinds.of("k:five"));
        assertEquals(OptionKinds.Kind.GIFT, OptionKinds.of("k:gift"));
        assertEquals(OptionKinds.Kind.LIKES, OptionKinds.of("k:likes"));
        assertEquals(OptionKinds.Kind.PUT, OptionKinds.of("k:put"));
        assertEquals(OptionKinds.Kind.TAKE_ALL, OptionKinds.of("k:takeall"));
        assertEquals(OptionKinds.Kind.USE_ON, OptionKinds.of("k:use:on"));
        assertEquals(OptionKinds.Kind.USE_OFF, OptionKinds.of("k:use:off"));
        assertEquals(OptionKinds.Kind.PANEL, OptionKinds.of("g:panel"));
        assertEquals(OptionKinds.Kind.JOIN, OptionKinds.of("t:join"));
        assertEquals(OptionKinds.Kind.WAIT, OptionKinds.of("t:wait"));
        assertEquals(OptionKinds.Kind.STANCE, OptionKinds.of("k:stance:guard"));
        assertEquals("guard", OptionKinds.stanceOf("k:stance:guard"));
        assertEquals("", OptionKinds.stanceOf("k:five"));
        assertEquals(OptionKinds.Kind.TAKE, OptionKinds.of("k:take:4"));
        assertEquals(4, OptionKinds.takeSlot("k:take:4"));
        assertEquals(-1, OptionKinds.takeSlot("k:take:9"));
        assertEquals(-1, OptionKinds.takeSlot("k:take:x"));
        assertEquals(-1, OptionKinds.takeSlot("t:self"));
        assertEquals(OptionKinds.Kind.TOPIC, OptionKinds.of("t:self"));
        assertEquals(OptionKinds.Kind.TOPIC, OptionKinds.of("k:unknown"));
        assertEquals(OptionKinds.Kind.TOPIC, OptionKinds.of(null));
        assertFalse(OptionKinds.hasWidget(OptionKinds.Kind.TOPIC));
        assertTrue(OptionKinds.hasWidget(OptionKinds.Kind.GIFT));
    }

    // ---------------------------------------------------------------- the memories are bounded
    @Test
    void theGiftMemoryRemembersTheNewestAnswerAndStaysBounded() {
        GiftMemory g = new GiftMemory();
        g.record("a", "minecraft:apple", GiftMemory.Reaction.LIKED);
        assertEquals(GiftMemory.Reaction.LIKED, g.get("a", "minecraft:apple"));
        g.record("a", "minecraft:apple", GiftMemory.Reaction.LOVED);
        assertEquals(GiftMemory.Reaction.LOVED, g.get("a", "minecraft:apple"), "the newest answer wins");
        assertNull(g.get("a", "minecraft:stone"));
        assertNull(g.get("b", "minecraft:apple"));
        g.record(null, "x", GiftMemory.Reaction.LOVED);
        g.record("a", "", GiftMemory.Reaction.LOVED);
        g.record("a", "x", null);
        assertEquals(1, g.count("a"));
        for (int i = 0; i < 200; i++) g.record("a", "item" + i, GiftMemory.Reaction.values()[i % 4]);
        assertEquals(GiftMemory.MAX_ITEMS, g.count("a"), "one companion keeps at most 24 items");
        for (int c = 0; c < 50; c++) g.record("c" + c, "i", GiftMemory.Reaction.LIKED);
        assertEquals(GiftMemory.MAX_COMPANIONS, g.companions(), "at most 8 companions");
        g.record("k".repeat(500), "i".repeat(500), GiftMemory.Reaction.LOVED);
        assertEquals(GiftMemory.Reaction.LOVED, g.get("k".repeat(500), "i".repeat(500)));
        g.clear();
        assertEquals(0, g.companions());
    }

    @Test
    void theNotebookListsLovedFirstThenHatedThenLikedThenRefused() {
        GiftMemory g = new GiftMemory();
        g.record("a", "liked1", GiftMemory.Reaction.LIKED);
        g.record("a", "hated1", GiftMemory.Reaction.HATED);
        g.record("a", "refused1", GiftMemory.Reaction.REFUSED);
        g.record("a", "loved1", GiftMemory.Reaction.LOVED);
        g.record("a", "loved2", GiftMemory.Reaction.LOVED);
        List<String> items = new ArrayList<>();
        List<GiftMemory.Reaction> rs = new ArrayList<>();
        g.notebook("a", 8, items, rs);
        assertEquals(List.of("loved2", "loved1", "hated1", "liked1", "refused1"), items);
        assertEquals(GiftMemory.Reaction.LOVED, rs.get(0));
        assertEquals(GiftMemory.Reaction.REFUSED, rs.get(4));
        g.notebook("a", 2, items, rs);
        assertEquals(2, items.size());
        g.notebook("nobody", 8, items, rs);
        assertTrue(items.isEmpty() && rs.isEmpty());
    }

    @Test
    void theBondStoryKeepsTheLastSixNewestFirstAndStaysBounded() {
        BondStory s = new BondStory();
        assertEquals(0, s.size("a"));
        assertNull(s.get("a", 0));
        for (int i = 0; i < 20; i++) s.add("a", BondStory.Kind.RAPPORT, "event " + i, i * 100L);
        assertEquals(BondStory.MAX, s.size("a"));
        assertEquals("event 19", s.get("a", 0).text());
        assertEquals("event 14", s.get("a", BondStory.MAX - 1).text());
        assertNull(s.get("a", BondStory.MAX));
        s.add("a", BondStory.Kind.RAPPORT, "event 19", 1901L);
        assertEquals("event 19", s.get("a", 0).text());
        assertEquals("event 18", s.get("a", 1).text(), "the same line twice in a row is one line");
        s.add("a", BondStory.Kind.NOTE, "  ", 0L);
        s.add("a", BondStory.Kind.NOTE, null, 0L);
        s.add("", BondStory.Kind.NOTE, "x", 0L);
        s.add(null, BondStory.Kind.NOTE, "x", 0L);
        assertEquals("event 19", s.get("a", 0).text());
        s.add("a", BondStory.Kind.NOTE, "z".repeat(500), 5000L);
        assertEquals(BondStory.MAX_TEXT, s.get("a", 0).text().length());
        for (int c = 0; c < 40; c++) s.add("c" + c, BondStory.Kind.FIVE, "hi", 0L);
        assertEquals(BondStory.MAX_COMPANIONS, s.companions());
        s.clear();
        assertEquals(0, s.companions());
    }

    // ---------------------------------------------------------------- the heal ring
    @Test
    void theRingTellsOnlyWhatTheClientCanKnow() {
        int n = 9;
        int[] kinds = new int[n], values = new int[n];
        java.util.Arrays.fill(kinds, CompanionRules.NOT_USEFUL);
        assertEquals(PouchRing.State.LOCKED, PouchRing.of(-1, kinds, values));
        assertEquals(PouchRing.State.OFF, PouchRing.of(0, kinds, values));
        assertEquals(PouchRing.State.EMPTY, PouchRing.of(1, kinds, values));
        assertEquals(-1, PouchRing.bestSlot(kinds, values));
        kinds[2] = CompanionRules.FOOD_KIND;
        values[2] = 4;
        kinds[5] = CompanionRules.POTION_KIND;
        values[5] = 1;
        assertEquals(PouchRing.State.READY, PouchRing.of(1, kinds, values));
        assertEquals(CompanionRules.bestHeal(kinds, values), PouchRing.bestSlot(kinds, values), "the same rule as the server");
        assertEquals(5, PouchRing.bestSlot(kinds, values), "a healing potion comes before food");
        assertEquals(-1, PouchRing.bestSlot(null, values));
        for (PouchRing.State st : PouchRing.State.values()) for (float t = 0; t < 300; t += 7) {
            float b = PouchRing.brightness(st, t);
            assertTrue(b >= 0f && b <= 1f, st + " " + t);
        }
        assertTrue(PouchRing.brightness(PouchRing.State.READY, 10f) > PouchRing.brightness(PouchRing.State.LOCKED, 10f) - 1f);
        for (float t = 0; t < 500; t += 11) {
            float s = PouchRing.sweep(t);
            assertTrue(s >= 0f && s < 1f);
        }
    }

    // ---------------------------------------------------------------- the typewriter
    @Test
    void theTypewriterRevealsAndALongTextShowsAtOnce() {
        String[] lines = {"Hello there", "friend"};
        int total = TypeWriter.total(lines);
        assertEquals(17, total);
        assertEquals(0, TypeWriter.shown(total, 0f, true));
        assertTrue(TypeWriter.shown(total, 2f, true) > 0);
        assertFalse(TypeWriter.done(total, 2f, true));
        assertTrue(TypeWriter.done(total, TypeWriter.duration(total) + 1f, true));
        assertEquals(Integer.MAX_VALUE, TypeWriter.shown(total, 0f, false), "off shows everything");
        assertTrue(TypeWriter.done(total, 0f, false));
        assertEquals(Integer.MAX_VALUE, TypeWriter.shown(TypeWriter.MAX_TYPED + 1, 0f, true), "a page of text is not typed");
        assertEquals(5, TypeWriter.ofLine(lines, 0, 5));
        assertEquals(11, TypeWriter.ofLine(lines, 0, 50));
        assertEquals(0, TypeWriter.ofLine(lines, 1, 5));
        assertEquals(3, TypeWriter.ofLine(lines, 1, 14));
        assertEquals(6, TypeWriter.ofLine(lines, 1, Integer.MAX_VALUE));
        assertEquals(0, TypeWriter.ofLine(lines, 7, 5));
        assertEquals(0, TypeWriter.ofLine(null, 0, 5));
        assertEquals(0, TypeWriter.total(null));
    }

    // ---------------------------------------------------------------- the bubble numbers
    @Test
    void theBubblePopsFadesAndShowsFewerLinesWhenFar() {
        assertEquals(0.55f, BubbleStyle.pop(0f), 1e-4);
        assertEquals(1f, BubbleStyle.pop(BubbleStyle.POP), 1e-4);
        assertEquals(1f, BubbleStyle.pop(100f));
        boolean over = false;
        for (float a = 0; a < BubbleStyle.POP; a += 0.25f) {
            float s = BubbleStyle.pop(a);
            assertTrue(s >= 0.55f - 1e-4 && s < 1.25f);
            if (s > 1f) over = true;
        }
        assertTrue(over, "a little overshoot");
        assertEquals(1f, BubbleStyle.fade(BubbleStyle.FADE + 1f));
        assertEquals(0f, BubbleStyle.fade(-3f));
        assertEquals(0.5f, BubbleStyle.fade(BubbleStyle.FADE / 2f), 1e-4);
        assertTrue(BubbleStyle.invisible(0.01f));
        assertFalse(BubbleStyle.invisible(0.5f));
        assertEquals(BubbleStyle.NEAR_LINES, BubbleStyle.lines(10 * 10, 4));
        assertEquals(BubbleStyle.FAR_LINES, BubbleStyle.lines(20 * 20, 4));
        assertEquals(1, BubbleStyle.lines(20 * 20, 1));
        assertEquals(0x80, BubbleStyle.argb(0x123456, 255, 0.5f) >>> 24 & 0xFF, 1);
        assertEquals(0x123456, BubbleStyle.argb(0x123456, 255, 2f) & 0xFFFFFF);
        assertEquals(0, BubbleStyle.argb(0x123456, 255, -1f) >>> 24);
        for (com.newuniverse.nusmp.client.ui.gs.GsSkin sk : com.newuniverse.nusmp.client.ui.gs.GsSkin.values()) {
            assertNotNull(sk);
            assertTrue((BubbleStyle.textColor(sk, 1f) >>> 24) == 255);
            assertTrue((BubbleStyle.cardColor(sk, 1f) >>> 24) > 0);
            assertTrue((BubbleStyle.nameCardColor(sk, 1f) >>> 24) > 0);
            assertTrue((BubbleStyle.nameColor(sk, 1f) >>> 24) == 255 && (BubbleStyle.tailColor(sk, 1f) >>> 24) == 255);
        }
    }

    // ---------------------------------------------------------------- the itemrefs
    @Test
    void itemReferencesDoNotOverlap() {
        for (int i = 0; i < 9; i++) assertTrue(GsItemRef.isPouch(GsItemRef.pouch(i)));
        for (int i = 0; i < 36; i++) {
            int r = GsItemRef.inventory(i);
            assertTrue(GsItemRef.isInventory(r) && !GsItemRef.isPouch(r) && !GsItemRef.isTaste(r));
            assertEquals(i, GsItemRef.inventoryIndex(r));
        }
        for (int i = 0; i < 8; i++) {
            int r = GsItemRef.taste(i);
            assertTrue(GsItemRef.isTaste(r) && !GsItemRef.isInventory(r) && !GsItemRef.isPouch(r));
            assertEquals(i, GsItemRef.tasteIndex(r));
        }
        assertFalse(GsItemRef.isPouch(GsItemRef.GIFT) || GsItemRef.isInventory(GsItemRef.GIFT) || GsItemRef.isTaste(GsItemRef.GIFT));
    }
}
