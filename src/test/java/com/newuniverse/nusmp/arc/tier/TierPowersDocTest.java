package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3.39.0: a guard against docs/bosses/tier_powers.md and the code drifting apart. It is not a parser: it checks that the doc still names every boss and every trait, explains the three name
 * tags and the budgets, prices every trait the way {@link TierTrait} does, and gives each of the sixteen bosses the same traits (and prices) as its roster row. Fix the doc OR the row, whichever
 * is wrong; the messages say which line.
 */
class TierPowersDocTest {
    private static final Path DOC = Path.of("docs/bosses/tier_powers.md");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String doc() throws Exception { return Files.readString(DOC); }

    private static List<TierRow> sixteen() {
        List<TierRow> out = new ArrayList<>(TierRosterF.rows());
        out.addAll(TierRosterD.rows());
        return out;
    }

    /** The text of a boss the doc can be expected to contain: the nickname in quotes if the row has one ("Big Mom" Charlotte Linlin -> Big Mom), else the whole name. */
    private static String docKey(TierRow row) {
        String n = row.character().name();
        int a = n.indexOf('"'), b = a < 0 ? -1 : n.indexOf('"', a + 1);
        return a >= 0 && b > a ? n.substring(a + 1, b) : n;
    }

    /** The lines of one "## N." section (from its heading to the next "## " heading). */
    private static String section(String doc, String heading) {
        int a = doc.indexOf("\n" + heading);
        assertTrue(a >= 0, "the doc has no section '" + heading + "'");
        int b = doc.indexOf("\n## ", a + heading.length());
        return b < 0 ? doc.substring(a) : doc.substring(a, b);
    }

    private static String[] cells(String line) {
        String[] raw = line.split("\\|", -1);
        String[] out = new String[Math.max(0, raw.length - 2)];
        for (int i = 1; i < raw.length - 1; i++) out[i - 1] = raw[i].trim();
        return out;
    }

    // ---------------------------------------------------------------- the doc exists and says the basics

    @Test
    void theDocExistsAndIsNotAStub() throws Exception {
        assertTrue(Files.isRegularFile(DOC), "docs/bosses/tier_powers.md is missing");
        assertTrue(doc().length() > 4000, "the doc is only " + doc().length() + " characters");
        assertTrue(doc().contains("Light Yagami"), "the doc must say Light Yagami keeps his own scene and is not touched");
    }

    @Test
    void theDocExplainsTheThreeNameTags() throws Exception {
        String d = doc().toLowerCase(Locale.ROOT);
        for (String tag : new String[] {"named", "theme", "filler"}) assertTrue(d.contains(tag), "the doc never explains the tag '" + tag + "' (rule 10: say where each name comes from)");
    }

    @Test
    void theDocStatesTheBudgetsOfTheLetters() throws Exception {
        String d = doc();
        for (TierLetter l : new TierLetter[] {TierLetter.F, TierLetter.D, TierLetter.C, TierLetter.B, TierLetter.A}) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(l.label()) + "\\s+" + l.traitBudget() + "\\b");
            assertTrue(p.matcher(d).find(), "the doc does not state the budget of tier " + l.label() + " (" + l.traitBudget() + " points) as '" + l.label() + " " + l.traitBudget() + "'");
        }
    }

    @Test
    void theDocStatesTheCapsAndTheBand() throws Exception {
        String d = doc();
        assertTrue(d.contains(String.format(Locale.ROOT, "%.1fx", TierBalance.MAX_ORDINARY)) || d.contains("1.6x"), "the doc does not state the 1.6x cap of an ordinary hit");
        assertTrue(d.contains(String.format(Locale.ROOT, "%.1fx", TierBalance.MAX_ULTIMATE)) || d.contains("2.0x"), "the doc does not state the 2.0x cap of an ultimate");
        assertTrue(d.contains("25%") || d.contains("±25"), "the doc does not state the +-25% band");
        assertTrue(d.contains(TierBalance.REF_POWER + "% power"), "the doc does not say the band is measured at " + TierBalance.REF_POWER + "% power");
    }

    @Test
    void theDocNamesTheTwoSettingsAndTheConfigDefinesThem() throws Exception {
        String d = doc();
        assertTrue(d.contains("tierPowers") && d.contains("tierPowerScale"), "the doc must name the two server settings");
        String cfg = Files.readString(SRC.resolve("arc/ArcConfig.java"));
        assertTrue(cfg.contains("\"tierPowers\""), "ArcConfig does not define tierPowers");
        assertTrue(cfg.contains("\"tierPowerScale\""), "ArcConfig does not define tierPowerScale");
        Matcher range = Pattern.compile("defineInRange\\(\"tierPowerScale\",\\s*1\\.0,\\s*0\\.5,\\s*2\\.0\\)").matcher(cfg);
        assertTrue(range.find(), "tierPowerScale must be 0.5 to 2.0 with the default 1.0, as the doc says");
    }

    @Test
    void theFilesTheDocPointsAtExist() throws Exception {
        String d = doc();
        for (String f : new String[] {"tools/tier_bosses/tier_pressure.py", "docs/bosses/RULES.md"}) {
            String shortName = f.substring(f.lastIndexOf('/') + 1);
            if (d.contains(shortName)) assertTrue(Files.isRegularFile(Path.of(f)), "the doc points at " + f + " which does not exist");
        }
    }

    // ---------------------------------------------------------------- every boss

    @Test
    void theDocMentionsEveryOneOfTheSixteenBosses() throws Exception {
        String d = doc();
        List<TierRow> rows = sixteen();
        assertEquals(16, rows.size());
        for (TierRow row : rows) {
            String name = row.character().name();
            assertTrue(d.contains(name) || d.contains(docKey(row)), "the doc never mentions " + row.id() + " (" + name + ")");
        }
    }

    /** ROSTER-DEPENDENT: every boss of the first batch carries at least one trait (the roster tuning adds them to the rows). */
    @Test
    void everyBossRowHasAtLeastOneTrait() {
        for (TierRow row : sixteen()) assertFalse(row.traits().isEmpty(), row.id() + " has no trait: the doc gives every boss a signature power");
    }

    // ---------------------------------------------------------------- every trait

    @Test
    void theDocMentionsEveryTraitByNameOrLabel() throws Exception {
        String d = doc();
        for (TierTrait t : TierTrait.values())
            assertTrue(d.contains(t.name()) || d.toLowerCase(Locale.ROOT).contains(t.label().toLowerCase(Locale.ROOT)), "the doc never mentions the trait " + t.name() + " (" + t.label() + ")");
    }

    @Test
    void theTraitTableOfSectionTwoListsEveryTraitWithItsPriceAndNothingElse() throws Exception {
        String s = section(doc(), "## 2.");
        Map<TierTrait, Integer> price = new EnumMap<>(TierTrait.class);
        for (String line : s.split("\\R")) {
            if (!line.startsWith("|")) continue;
            String[] c = cells(line);
            if (c.length < 3) continue;
            Matcher m = Pattern.compile("^([A-Z][A-Z_]+)\\b").matcher(c[0]);
            if (!m.find()) continue;                                                      // the header and the separator line
            TierTrait t;
            try {
                t = TierTrait.valueOf(m.group(1));
            } catch (IllegalArgumentException e) {
                fail("the trait table lists " + m.group(1) + " which is not in TierTrait");
                return;
            }
            assertNull(price.put(t, Integer.parseInt(c[1].replaceAll("[^0-9]", ""))), "the trait table lists " + t + " twice");
        }
        for (TierTrait t : TierTrait.values()) {
            assertTrue(price.containsKey(t), "the trait table of section 2 has no row for " + t);
            assertEquals(t.price(), price.get(t), "the doc prices " + t + " at " + price.get(t) + " but TierTrait at " + t.price());
        }
    }

    // ---------------------------------------------------------------- the sixteen lines of section 4

    /** ROSTER-DEPENDENT: each boss line of section 4 lists exactly the traits (and prices) of its roster row. */
    @Test
    void eachBossLineOfSectionFourMatchesItsRosterRow() throws Exception {
        String s = section(doc(), "## 4.");
        List<String[]> lines = new ArrayList<>();
        for (String line : s.split("\\R")) {
            if (!line.startsWith("|")) continue;
            String[] c = cells(line);
            if (c.length >= 3 && c[0].startsWith("**")) lines.add(c);                    // a boss line starts with the bold name
        }
        assertEquals(16, lines.size(), "section 4 must have one line per boss");
        for (TierRow row : sixteen()) {
            String key = docKey(row);
            List<String[]> hit = new ArrayList<>();
            for (String[] c : lines) {
                String shown = c[0].replace("*", "");
                int paren = shown.indexOf(" (");
                if (paren > 0) shown = shown.substring(0, paren);                         // "Charlos (One Piece)" -> "Charlos"
                if (shown.contains(key)) hit.add(c);
            }
            assertEquals(1, hit.size(), "section 4 must have exactly one line for " + row.id() + " (" + key + "), found " + hit.size());
            String traitCell = hit.get(0)[1];
            Map<TierTrait, Integer> docPrice = new EnumMap<>(TierTrait.class);
            Matcher m = Pattern.compile("\\b([A-Z][A-Z_]+)\\b[^()+]*\\((\\d+)").matcher(traitCell);
            while (m.find()) {
                try {
                    docPrice.put(TierTrait.valueOf(m.group(1)), Integer.parseInt(m.group(2)));
                } catch (IllegalArgumentException ignored) {
                    // a word in capitals that is not a trait (e.g. "F")
                }
            }
            Set<TierTrait> rowTraits = EnumSet.noneOf(TierTrait.class);
            for (TierTraitSpec spec : row.traits()) rowTraits.add(spec.trait());
            assertEquals(rowTraits, docPrice.keySet(), row.id() + ": the doc line says '" + traitCell + "' but the roster row carries " + rowTraits);
            for (TierTrait t : rowTraits) assertEquals(t.price(), docPrice.get(t), row.id() + ": the doc prices " + t + " at " + docPrice.get(t));
        }
    }

    @Test
    void theBudgetSentenceOfSectionFourIsTrue() throws Exception {
        String s = section(doc(), "## 4.");
        assertTrue(s.contains("### Tier F (budget " + TierLetter.F.traitBudget() + ")"), "the Tier F heading must state the budget " + TierLetter.F.traitBudget());
        assertTrue(s.contains("### Tier D (budget " + TierLetter.D.traitBudget() + ")"), "the Tier D heading must state the budget " + TierLetter.D.traitBudget());
    }
}
