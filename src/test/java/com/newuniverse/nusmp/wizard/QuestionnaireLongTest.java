package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.multiverse.SocialClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The long questionnaire (docs/wizard_long_questionnaire.md): every question parses, nothing gives a magic away, every flavor is reachable, the sections come in order, the summary and the steering work. */
class QuestionnaireLongTest {
    private static String[][][] oldPools() throws Exception {
        Field f = Questionnaire.class.getDeclaredField("POOLS");
        f.setAccessible(true);
        return (String[][][]) f.get(null);
    }

    private static List<String[][]> newPools() { return List.of(LongQuestions.NATURE, LongQuestions.HEARTH, LongQuestions.BLOOD, LongQuestions.RESOLVE, LongQuestions.SOCIAL); }

    private static final List<String> LETTER = List.of("F", "K", "T", "R", "S");

    @Test
    void newQuestionCountsAreAsPromised() {
        assertTrue(LongQuestions.NATURE.length >= 30, "nature: " + LongQuestions.NATURE.length);
        assertEquals(6, LongQuestions.HEARTH.length);
        assertEquals(6, LongQuestions.BLOOD.length);
        assertEquals(10, LongQuestions.SOCIAL.length);
        assertEquals(12, LongQuestions.RESOLVE.length);
        String[][][] lp = Questionnaire.longPools();                                  // nature, hearth, blood, resolve, social: the old ones first, the new appended
        assertEquals(20 + LongQuestions.NATURE.length, lp[0].length);
        assertEquals(4 + 6, lp[1].length);
        assertEquals(5 + 6, lp[2].length);
        assertEquals(2 + 12, lp[3].length);
        assertEquals(2 + 10, lp[4].length);
    }

    @Test
    void everyQuestionParsesWithFourOptionsAndValidTokens() throws Exception {
        String[] want = {"F", "K", "R", "T", "S"};
        String[][][] lp = Questionnaire.longPools();
        for (int p = 0; p < lp.length; p++) {
            for (String[] q : lp[p]) {
                assertEquals(5, q.length, "text + 4 options: " + q[0]);
                for (int k = 1; k < q.length; k++) {
                    String[] o = q[k].split("\\|", 2);
                    assertEquals(2, o.length, "option has tokens: " + q[k]);
                    assertFalse(o[0].isBlank());
                    boolean ok = false;
                    for (String tok : o[1].split(",")) {
                        String[] t = tok.split(":");
                        Integer.parseInt(t[t.length - 1]);
                        switch (t[0]) {
                            case "F" -> MagicFlavors.Flavor.valueOf(t[1]);
                            case "K" -> Kingdom.valueOf(t[1]);
                            case "R" -> Race.valueOf(t[1]);
                            case "S" -> SocialClass.valueOf(t[1]);
                            case "T" -> { assertEquals(2, t.length); }
                            default -> throw new AssertionError("unknown token " + tok);
                        }
                        assertTrue(LETTER.contains(t[0]));
                        ok = true;
                    }
                    assertTrue(ok);
                }
            }
        }
        // the new pools carry only their own kind of token (the old nature pool still uses a few kinds too, so only the new ones are checked)
        String[] kinds = {"F", "K", "R", "T", "S"};
        List<String[][]> np = newPools();
        for (int p = 0; p < np.size(); p++)
            for (String[] q : np.get(p)) for (int k = 1; k < 5; k++) for (String tok : q[k].split("\\|", 2)[1].split(","))
                assertEquals(kinds[p], tok.split(":")[0], "wrong token kind in " + q[0]);
        for (String[][] pool : np) for (String[] q : pool) for (int k = 1; k < 5; k++) assertTrue(q[k].split("\\|", 2)[0].length() <= 60, "fits the answer button: " + q[k]);
        assertEquals(5, want.length);
    }

    @Test
    void questionTextsAreUniqueAcrossAllPools() throws Exception {
        Set<String> seen = new HashSet<>();
        for (String[][] pool : Questionnaire.longPools()) for (String[] q : pool) assertTrue(seen.add(q[0]), "duplicate question: " + q[0]);
        for (String[][] pool : oldPools()) for (String[] q : pool) assertTrue(seen.contains(q[0]), "an old question was changed or removed: " + q[0]);
    }

    @Test
    void everyFlavorIsReachableFromManyNewQuestions() {
        for (MagicFlavors.Flavor f : MagicFlavors.Flavor.values()) {
            int n = 0;
            for (String[] q : LongQuestions.NATURE) {
                boolean in = false;
                for (int k = 1; k < 5; k++) if (q[k].contains("F:" + f.name() + ":")) in = true;
                if (in) n++;
            }
            assertTrue(n >= 6, f + " appears in only " + n + " new nature questions");
        }
    }

    @Test
    void noNewOptionNamesAMagic() {
        Set<String> banned = new HashSet<>(List.of("magic", "grimoire", "attribute"));
        for (MagicType m : MagicType.values()) banned.add(m.displayName.toLowerCase());
        for (String[][] pool : newPools()) for (String[] q : pool) for (int k = 0; k < 5; k++) {
            String text = (k == 0 ? q[0] : q[k].split("\\|", 2)[0]).toLowerCase();
            for (String b : banned) assertFalse(text.contains(b), "'" + b + "' gives a magic away in: " + text);
            if (k > 0) for (MagicType m : MagicType.values()) assertFalse(text.equals(m.name().toLowerCase().replace('_', ' ')), "an option that is just a magic name: " + text);
        }
    }

    @Test
    void newResolveQuestionsMixBestMiddlingAndArrogantAnswers() {
        int minus = 0, middling = 0, tied = 0;
        for (String[] q : LongQuestions.RESOLVE) {
            int best = Integer.MIN_VALUE, worst = Integer.MAX_VALUE, count = 0;
            List<Integer> v = new ArrayList<>();
            for (int k = 1; k < 5; k++) v.add(Integer.parseInt(q[k].split("\\|", 2)[1].split(":")[1]));
            for (int x : v) { best = Math.max(best, x); worst = Math.min(worst, x); }
            for (int x : v) if (x == best) count++;
            assertTrue(best == 2 || best == 3, "the best answer is worth 2 or 3: " + q[0]);
            assertTrue(worst <= 0 && worst >= -1, "there is a lazy or arrogant answer: " + q[0]);
            if (worst == -1) minus++;
            if (v.contains(1) || (best == 3 && v.contains(2))) middling++;
            if (count > 1) tied++;
        }
        assertTrue(minus >= 4 && middling >= 4 && tied >= 4, minus + " / " + middling + " / " + tied);
    }

    // ---------------------------------------------------------------- sections
    @Test
    void theLongSheetComesInOrderedSections() {
        RandomSource rng = RandomSource.create(3);
        for (int i = 0; i < 50; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            assertTrue(s.longMode());
            assertEquals(30, s.texts().size());
            assertEquals(s.texts().size(), s.cats().size());
            int[] count = new int[5];
            int last = -1;
            for (String c : s.cats()) {
                int at = SheetSections.ORDER.indexOf(c);
                assertTrue(at >= last, "sections out of order");
                last = at;
                count[at]++;
            }
            assertEquals(List.of(12, 4, 4, 4, 6), List.of(count[0], count[1], count[2], count[3], count[4]));
            for (int q = 0; q < s.texts().size(); q++) assertEquals(4, s.shownOptions().get(q).size());
            Set<String> texts = new HashSet<>(s.texts());
            assertEquals(s.texts().size(), texts.size(), "no question twice");
        }
    }

    @Test
    void answerOrderIsStillShuffledAndQuestionsInsideASectionAreRandom() {
        Set<List<String>> firstNature = new HashSet<>(), orders = new HashSet<>();
        RandomSource rng = RandomSource.create(5);
        for (int i = 0; i < 40; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            firstNature.add(List.of(s.texts().get(0), s.texts().get(1)));
            orders.add(s.shownOptions().get(0));
        }
        assertTrue(firstNature.size() > 10 && orders.size() > 10);
    }

    @Test
    void theOldSheetIsUnchangedTwelveQuestionsNoSections() {
        Questionnaire.Sheet s = Questionnaire.draw(RandomSource.create(9));
        assertEquals(12, s.texts().size());
        assertTrue(s.cats().isEmpty());
        assertFalse(s.longMode());
        CompoundTag t = s.toTag();
        ListTag qs = t.getList("Q", Tag.TAG_COMPOUND);
        for (int i = 0; i < qs.size(); i++) assertFalse(qs.getCompound(i).contains("C"), "an old sheet has no section ids");
        // the old sheet is read with the old rule: a 12-question sheet, results in range
        var r = Questionnaire.result(s, new int[12], MagicFlavors.pool(), true, 0.5, RandomSource.create(1));
        assertNotNull(r);
        assertTrue(r.leaves() >= 3 && r.leaves() <= 5);
    }

    @Test
    void theSheetTagCarriesTheSectionsAndStaysSmall() {
        Questionnaire.Sheet s = Questionnaire.draw(RandomSource.create(4), true);
        CompoundTag t = s.toTag();
        ListTag qs = t.getList("Q", Tag.TAG_COMPOUND);
        assertEquals(30, qs.size());
        for (int i = 0; i < qs.size(); i++) assertEquals(s.cats().get(i), qs.getCompound(i).getString("C"));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try { net.minecraft.nbt.NbtIo.write(t, new java.io.DataOutputStream(out)); } catch (java.io.IOException e) { throw new AssertionError(e); }
        System.out.println("long sheet packet: " + out.size() + " bytes");
        assertTrue(out.size() < 12000, "the packet is a few KB: " + out.size());
    }

    @Test
    void theProgressLineReadsPartOfFiveAndTheQuestionInThePart() {
        List<String> cats = new ArrayList<>();
        int[] n = {12, 4, 4, 4, 6};
        for (int i = 0; i < 5; i++) for (int k = 0; k < n[i]; k++) cats.add(SheetSections.ORDER.get(i));
        assertEquals("Part 1 of 5: Nature (1/12)", SheetSections.progress(cats, 0));
        assertEquals("Part 2 of 5: Hearth (3/4)", SheetSections.progress(cats, 14));
        assertEquals("Part 4 of 5: Origin (4/4)", SheetSections.progress(cats, 23));
        assertEquals("Part 5 of 5: Resolve (6/6)", SheetSections.progress(cats, 29));
        assertEquals("", SheetSections.progress(List.of(), 0), "an old sheet has no sections");
        assertEquals("", SheetSections.progress(List.of("", ""), 1));
    }

    // ---------------------------------------------------------------- the summary
    @Test
    void theSummaryNamesTheWinnerOfEachCategoryAndNeverNumbersOfOptionsNotChosen() {
        RandomSource rng = RandomSource.create(21);
        for (int i = 0; i < 40; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            int[] a = Questionnaire.bestResolveAnswers(s);
            var r = Questionnaire.result(s, a, MagicFlavors.pool(), true, 0.5, rng);
            List<String> lines = Questionnaire.summary(r);
            assertEquals(5, lines.size());
            assertTrue(lines.get(0).startsWith("Nature: "), lines.get(0));
            assertTrue(lines.get(1).startsWith("Hearth: " + r.kingdom().displayName.replace(" Kingdom", "") + " ("), lines.get(1));
            assertTrue(lines.get(2).startsWith("Blood: " + r.blood().displayName + " ("), lines.get(2));
            assertTrue(lines.get(3).startsWith("Origin: " + r.social().displayName + " ("), lines.get(3));
            assertTrue(lines.get(4).matches("Resolve: -?\\d+ of \\d+, a (three|four|five)-leaf grimoire"), lines.get(4));
            for (int k = 1; k <= 3; k++) assertTrue(lines.get(k).matches("[A-Za-z]+: [^()\\d]+ \\((clear|leaning|narrow)\\)"), lines.get(k));
            int want = r.leaves();
            assertTrue(lines.get(4).contains(want == 3 ? "three" : want == 4 ? "four" : "five"));
        }
        assertEquals(List.of(), Questionnaire.summary(new Questionnaire.Result(MagicType.FLAME, null, null, false, Kingdom.CLOVER, Race.HUMAN, 3, SocialClass.COMMONER)), "an old result has no detail and no lines");
    }

    @Test
    void theSummaryWordsFollowTheGap() {
        assertEquals("clear", Questionnaire.word(8, 0));
        assertEquals("leaning", Questionnaire.word(8, 6));
        assertEquals("narrow", Questionnaire.word(8, 7.5));
        assertEquals("narrow", Questionnaire.word(0, 0));
    }

    // ---------------------------------------------------------------- the answers matter
    @Test
    void aSteeredFirePathGetsAFireFamilyMagic() {
        // a sheet whose twelve nature questions all offer a hot answer: always taking it must give a magic of the fire family
        List<String[]> hotQs = new ArrayList<>();
        for (String[] q : Questionnaire.longPools()[0]) { int best = 0; for (int k = 1; k < 5; k++) best = Math.max(best, hotPoints(q[k].split("\\|", 2)[1])); if (best >= 2) hotQs.add(q); }
        assertTrue(hotQs.size() >= 12, "enough hot answers in the pool: " + hotQs.size());
        for (int seed = 0; seed < 40; seed++) {
            List<String[]> pick = new ArrayList<>(hotQs);
            java.util.Collections.shuffle(pick, new java.util.Random(seed));
            List<String> texts = new ArrayList<>(), cats = new ArrayList<>();
            List<List<String>> shown = new ArrayList<>(), toks = new ArrayList<>();
            int[] a = new int[12];
            for (int q = 0; q < 12; q++) {
                String[] src = pick.get(q);
                texts.add(src[0]);
                cats.add(SheetSections.NATURE);
                List<String> s1 = new ArrayList<>(), t1 = new ArrayList<>();
                int best = -1;
                for (int k = 1; k < 5; k++) {
                    String[] o = src[k].split("\\|", 2);
                    s1.add(o[0]);
                    t1.add(o[1]);
                    if (hotPoints(o[1]) > (best < 0 ? 0 : hotPoints(t1.get(best)))) best = k - 1;
                }
                shown.add(s1);
                toks.add(t1);
                a[q] = best;
            }
            var r = Questionnaire.result(new Questionnaire.Sheet(texts, shown, toks, cats, true), a, MagicFlavors.pool(), true, 0.0, RandomSource.create(seed));
            assertTrue(hasHot(r.primary()), "steered towards the hot nature, got " + r.primary());
        }
    }

    private static boolean hasHot(MagicType m) {
        for (MagicFlavors.Flavor f : MagicFlavors.flavors(m)) if (f == MagicFlavors.Flavor.HOT) return true;
        return false;
    }

    @Test
    void takingTheHotAnswerWheneverOfferedBeatsRandomAnswersOnRealSheets() {
        RandomSource rng = RandomSource.create(77);
        int steeredHot = 0, randomHot = 0, n = 300;
        for (int i = 0; i < n; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            int[] a = new int[s.texts().size()], b = new int[a.length];
            for (int q = 0; q < a.length; q++) {
                b[q] = rng.nextInt(4);
                int bestPts = 0;
                for (int k = 0; k < 4; k++) { int pts = hotPoints(s.tokens().get(q).get(k)); if (pts > bestPts) { bestPts = pts; a[q] = k; } }
            }
            if (hasHot(Questionnaire.result(s, a, MagicFlavors.pool(), true, 0.0, rng).primary())) steeredHot++;
            if (hasHot(Questionnaire.result(s, b, MagicFlavors.pool(), true, 0.0, rng).primary())) randomHot++;
        }
        System.out.println("fire-family magic: steered " + steeredHot + "/" + n + ", random " + randomHot + "/" + n);
        assertTrue(steeredHot > 2 * randomHot, "steering must show: " + steeredHot + " vs " + randomHot);
    }

    private static int hotPoints(String tokens) {
        int pts = 0;
        for (String t : tokens.split(",")) if (t.startsWith("F:HOT:")) pts += Integer.parseInt(t.substring(6));
        return pts;
    }

    @Test
    void aTieInKingdomBloodAndClassIsBrokenByTheLaterAnswers() {
        for (int seed = 0; seed < 30; seed++) {
            Questionnaire.Sheet s = new Questionnaire.Sheet(List.of("a", "b", "c", "d", "e", "f"),
                    List.of(List.of("x"), List.of("x"), List.of("x"), List.of("x"), List.of("x"), List.of("x")),
                    List.of(List.of("K:CLOVER:2"), List.of("K:SPADE:2"), List.of("R:ELF:2"), List.of("R:DWARF:2"), List.of("S:NOBLE:2"), List.of("S:PEASANT:2")),
                    List.of("HEARTH", "HEARTH", "BLOOD", "BLOOD", "ORIGIN", "ORIGIN"), true);
            var r = Questionnaire.result(s, new int[6], List.of(MagicType.FLAME, MagicType.WATER), true, 0.0, RandomSource.create(seed));
            assertEquals(Kingdom.SPADE, r.kingdom());
            assertEquals(Race.DWARF, r.blood());
            assertEquals(SocialClass.PEASANT, r.social());
            // and the old rule (not a long sheet) still breaks a tie at random: both outcomes show up
        }
        Set<Kingdom> seen = EnumSet.noneOf(Kingdom.class);
        for (int seed = 0; seed < 60; seed++) {
            Questionnaire.Sheet s = new Questionnaire.Sheet(List.of("a", "b"), List.of(List.of("x"), List.of("x")), List.of(List.of("K:CLOVER:2"), List.of("K:SPADE:2")));
            seen.add(Questionnaire.result(s, new int[2], List.of(MagicType.FLAME), true, 0.0, RandomSource.create(seed)).kingdom());
        }
        assertTrue(seen.contains(Kingdom.CLOVER) && seen.contains(Kingdom.SPADE), "the old sheet still breaks ties at random");
    }

    @Test
    void aWrongSizedOrOutOfRangeAnswerIsRefused() {
        Questionnaire.Sheet s = Questionnaire.draw(RandomSource.create(2), true);
        assertNull(Questionnaire.result(s, new int[3], MagicFlavors.pool(), true, 0.5, RandomSource.create(1)));
        int[] bad = new int[30];
        bad[4] = 4;
        assertNull(Questionnaire.result(s, bad, MagicFlavors.pool(), true, 0.5, RandomSource.create(1)));
        bad[4] = -1;
        assertNull(Questionnaire.result(s, bad, MagicFlavors.pool(), true, 0.5, RandomSource.create(1)));
    }

    @Test
    void fiveLeavesStillNeedTheForbiddenStartRule() {
        RandomSource rng = RandomSource.create(13);
        for (int i = 0; i < 400; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            var r = Questionnaire.result(s, Questionnaire.bestResolveAnswers(s), MagicFlavors.pool(), false, 0.5, rng);
            assertTrue(r.leaves() <= 4);
        }
    }
}
