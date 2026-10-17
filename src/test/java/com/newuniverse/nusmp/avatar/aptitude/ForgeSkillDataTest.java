package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shipped Soul Forge skill files and their parser: every id is real (a Tensura id from the owner's reference, or a skill registered in NUSkills), the two
 * slots both have entries, the skills that steal or erase other skills are not offered, and bad files are refused with a reason.
 */
class ForgeSkillDataTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/avatar_skills");
    private static final Path IDS = Path.of("src/test/resources/com/newuniverse/nusmp/charter/tensura_skill_ids.txt");
    private static final Path NUSKILLS = Path.of("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java");

    private static List<ForgeSkill> shipped() throws Exception {
        List<ForgeSkill> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                out.add(ForgeSkillParser.parse(f.getFileName().toString().replace(".json", ""), JsonParser.parseString(Files.readString(f)).getAsJsonObject()));
            }
        }
        return out;
    }

    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    private static String badReason(String body) {
        try { ForgeSkillParser.parse("x", json(body)); return null; } catch (IllegalArgumentException e) { return e.getMessage(); }
    }

    @Test
    void everyShippedSkillIdIsRealAndUnique() throws Exception {
        Set<String> tensura = new HashSet<>();
        for (String l : Files.readAllLines(IDS)) if (!l.isBlank() && !l.startsWith("#")) tensura.add(l.trim());
        Set<String> nusmp = new HashSet<>();
        Matcher m = Pattern.compile("SKILLS\\.register\\(\"([a-z_]+)\"").matcher(Files.readString(NUSKILLS));
        while (m.find()) nusmp.add(m.group(1));
        assertTrue(tensura.size() > 300 && nusmp.size() > 20, "read both id sources");
        Set<String> seen = new HashSet<>();
        for (ForgeSkill s : shipped()) {
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
            String ns = s.id().substring(0, s.id().indexOf(':')), path = s.id().substring(s.id().indexOf(':') + 1);
            if (ns.equals("tensura")) assertTrue(tensura.contains(path), s.id() + " is not in the Tensura id reference");
            else if (ns.equals("nusmp")) assertTrue(nusmp.contains(path), s.id() + " is not registered in NUSkills");
            else fail("unexpected namespace in " + s.id());
        }
    }

    private static void fail(String m) { assertTrue(false, m); }

    @Test
    void bothSlotsHaveEntriesAndTheTaxesRiseWithTheKind() throws Exception {
        int unique = 0, ultimate = 0;
        double maxUniqueTax = 0, minUltimateTax = 1000, maxUniqueEp = 0, minUltimateEp = 1e9;
        for (ForgeSkill s : shipped()) {
            if (s.kind() == ForgeSkill.Kind.UNIQUE) {
                unique++;
                maxUniqueTax = Math.max(maxUniqueTax, s.epTaxPercent());
                maxUniqueEp = Math.max(maxUniqueEp, s.minEp());
            } else {
                ultimate++;
                minUltimateTax = Math.min(minUltimateTax, s.epTaxPercent());
                minUltimateEp = Math.min(minUltimateEp, s.minEp());
            }
            assertTrue(s.epTaxPercent() > 0.0, s.id() + " must cost something");
        }
        assertTrue(unique >= 20, "unique skills: " + unique);
        assertTrue(ultimate >= 5, "ultimate skills: " + ultimate);
        assertTrue(minUltimateTax > maxUniqueTax, "an Ultimate costs more than any Unique");
        assertTrue(minUltimateEp > maxUniqueEp, "an Ultimate needs more EP than any Unique");
    }

    @Test
    void skillsThatStealOrEraseOtherSkillsAreNeverOffered() throws Exception {
        for (ForgeSkill s : shipped()) {
            assertNotEquals("tensura:usurper", s.id());
            assertNotEquals("tensura:anti_skill", s.id());
        }
    }

    @Test
    void aMinimalFileNeedsAnIdAndAKind() {
        ForgeSkill s = ForgeSkillParser.parse("f", json("{\"id\":\"tensura:predator\",\"kind\":\"Unique\"}"));
        assertEquals("predator", s.name());
        assertEquals(ForgeSkill.Kind.UNIQUE, s.kind());
        assertEquals(0.0, s.epTaxPercent(), 1e-9);
        assertEquals(ForgeSkillParser.UNIQUE_TINT, s.tint());
        assertEquals(ForgeSkillParser.ULTIMATE_TINT, ForgeSkillParser.parse("f", json("{\"id\":\"a:b\",\"kind\":\"ultimate\"}")).tint());
    }

    @Test
    void badFilesAreRefusedWithAReason() {
        assertNotNull(badReason("{}"), "no id");
        assertNotNull(badReason("{\"id\":\"predator\",\"kind\":\"unique\"}"), "id without a namespace");
        assertNull(badReason("{\"id\":\"Tensura:Predator\",\"kind\":\"unique\"}"), "upper case ids are lowered, not refused");
        assertNotNull(badReason("{\"id\":\"tensura:predator\"}"), "no kind");
        assertNotNull(badReason("{\"id\":\"tensura:predator\",\"kind\":\"common\"}"), "kind is unique or ultimate");
        assertNotNull(badReason("{\"id\":\"tensura:predator\",\"kind\":\"unique\",\"ep_tax\":95}"), "tax above 90");
        assertNotNull(badReason("{\"id\":\"tensura:predator\",\"kind\":\"unique\",\"ep_tax\":\"x\"}"), "tax not a number");
        assertNotNull(badReason("{\"id\":\"tensura:predator\",\"kind\":\"unique\",\"min_ep\":-5}"), "negative EP gate");
        assertNotNull(badReason("{\"id\":\"tensura:predator\",\"kind\":\"unique\",\"icon\":\"not an id\"}"), "icon");
    }

    @Test
    void theCatalogIsReplacedWholeAndFirstWins() {
        ForgeSkill a = new ForgeSkill("tensura:predator", ForgeSkill.Kind.UNIQUE, "A", 1, 0, 0, "", ""), b = new ForgeSkill("tensura:predator", ForgeSkill.Kind.ULTIMATE, "B", 1, 0, 0, "", "");
        ForgeSkills.replaceAll(List.of(a, b));
        assertEquals(1, ForgeSkills.size());
        assertEquals("A", ForgeSkills.byId("TENSURA:PREDATOR").name());
        assertEquals(1, ForgeSkills.ofKind(ForgeSkill.Kind.UNIQUE).size());
        assertEquals(0, ForgeSkills.ofKind(ForgeSkill.Kind.ULTIMATE).size());
        ForgeSkills.replaceAll(null);
        assertEquals(0, ForgeSkills.size());
    }
}
