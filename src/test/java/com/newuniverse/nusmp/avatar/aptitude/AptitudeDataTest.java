package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
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
 * The shipped aptitude files and the parser that reads them: every file parses, the ids are the file names, the tax ladder rises with the rarity, Anti-Magic
 * and Time are among the heaviest, every magic name is a real MagicType, every skill id is in the owner's Tensura id reference, and bad files are refused
 * with a clear reason instead of being half-read.
 */
class AptitudeDataTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/avatar_aptitudes");
    private static final Path IDS = Path.of("src/test/resources/com/newuniverse/nusmp/charter/tensura_skill_ids.txt");
    private static final Path MAGIC = Path.of("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");

    private static List<AptitudeParser.Parsed> shipped() throws Exception {
        List<AptitudeParser.Parsed> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String id = f.getFileName().toString().replace(".json", "");
                out.add(AptitudeParser.parse(id, JsonParser.parseString(Files.readString(f)).getAsJsonObject()));
            }
        }
        return out;
    }

    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    private static String badReason(String id, String body) {
        try { AptitudeParser.parse(id, json(body)); return null; } catch (IllegalArgumentException e) { return e.getMessage(); }
    }

    @Test
    void atLeastTwentyFourShipAndEveryFileParses() throws Exception {
        List<AptitudeParser.Parsed> all = shipped();
        assertTrue(all.size() >= 24, "the owner asked for at least 24, found " + all.size());
        Set<String> ids = new HashSet<>();
        for (AptitudeParser.Parsed p : all) assertTrue(ids.add(p.aptitude().id()), "duplicate " + p.aptitude().id());
    }

    @Test
    void everyRarityHasAnAptitudeAndTheTaxLadderRises() throws Exception {
        double prevMax = -1;
        for (Aptitude.Rarity r : Aptitude.Rarity.values()) {
            double lo = 1e9, hi = -1;
            int n = 0;
            for (AptitudeParser.Parsed p : shipped()) {
                if (p.aptitude().rarity() != r) continue;
                n++;
                lo = Math.min(lo, p.aptitude().taxPercent());
                hi = Math.max(hi, p.aptitude().taxPercent());
            }
            assertTrue(n > 0, "no aptitude of rarity " + r);
            assertTrue(lo >= prevMax, r + " is cheaper than the rarity below it");
            prevMax = hi;
            if (r == Aptitude.Rarity.COMMON) assertEquals(0.0, hi, 1e-9, "a common aptitude is free to pick");
            else assertTrue(lo > 0.0, r + " must cost something");
        }
    }

    @Test
    void antiMagicAndTimeAreAmongTheHeaviest() throws Exception {
        Aptitude anti = null, time = null;
        for (AptitudeParser.Parsed p : shipped()) {
            if (p.aptitude().id().equals("anti_magic")) anti = p.aptitude();
            if (p.aptitude().id().equals("time")) time = p.aptitude();
        }
        assertNotNull(anti, "anti_magic ships");
        assertNotNull(time, "time ships");
        assertTrue(anti.rarity().ordinal() >= Aptitude.Rarity.EPIC.ordinal(), "Anti-Magic is EPIC or LEGENDARY");
        assertTrue(time.rarity().ordinal() >= Aptitude.Rarity.EPIC.ordinal(), "Time is EPIC or LEGENDARY");
        assertEquals("anti_magic", anti.magic());
        assertEquals("time", time.magic());
    }

    @Test
    void everyMagicNameIsARealMagicTypeAndEverySkillIsInTheTensuraReference() throws Exception {
        Set<String> magic = new HashSet<>();
        Matcher m = Pattern.compile("^\\s{4}([A-Z][A-Z_]*)\\(\"", Pattern.MULTILINE).matcher(Files.readString(MAGIC));
        while (m.find()) magic.add(m.group(1).toLowerCase());
        assertTrue(magic.size() > 50, "read the MagicType names");
        Set<String> ids = new HashSet<>();
        for (String l : Files.readAllLines(IDS)) if (!l.isBlank() && !l.startsWith("#")) ids.add("tensura:" + l.trim());
        assertTrue(ids.size() > 300, "read the id reference");
        for (AptitudeParser.Parsed p : shipped()) {
            Aptitude a = p.aptitude();
            if (!a.magic().isEmpty()) assertTrue(magic.contains(a.magic()), a.id() + ": '" + a.magic() + "' is not a MagicType");
            assertFalse(a.skills().isEmpty(), a.id() + " lends no skill");
            for (String s : a.skills()) assertTrue(ids.contains(s), a.id() + ": " + s + " is not a Tensura skill id");
        }
    }

    @Test
    void everyShippedAptitudeHasItsIconAndALook() throws Exception {
        for (AptitudeParser.Parsed p : shipped()) {
            String icon = p.look().icon();
            assertTrue(icon.startsWith("nusmp:textures/gui/soulforge/apt/"), p.aptitude().id() + " icon " + icon);
            File f = new File("src/main/resources/assets/nusmp/" + icon.substring("nusmp:".length()));
            assertTrue(f.isFile(), "missing icon file " + f);
            assertTrue(p.look().tint() >= 0 && p.look().tint() <= 0xFFFFFF);
            assertFalse(p.look().description().isEmpty(), p.aptitude().id() + " has no flavour line");
        }
    }

    @Test
    void aMinimalFileTakesTheDefaults() {
        AptitudeParser.Parsed p = AptitudeParser.parse("plain", json("{}"));
        assertEquals("plain", p.aptitude().name());
        assertEquals(Aptitude.Rarity.COMMON, p.aptitude().rarity());
        assertEquals(0.0, p.aptitude().taxPercent(), 1e-9);
        assertEquals("", p.aptitude().magic());
        assertEquals(List.of(), p.aptitude().skills());
        assertEquals(AptitudeLook.rarityColour(Aptitude.Rarity.COMMON), p.look().tint());
        assertEquals("nusmp:textures/gui/soulforge/apt/plain.png", p.look().icon());
    }

    @Test
    void colourAcceptsHashHexAndNumbers() {
        assertEquals(0xB04A4A, AptitudeParser.parse("a", json("{\"tint\":\"#B04A4A\"}")).look().tint());
        assertEquals(0xB04A4A, AptitudeParser.parse("a", json("{\"tint\":\"0xb04a4a\"}")).look().tint());
        assertEquals(0x00FF00, AptitudeParser.parse("a", json("{\"tint\":65280}")).look().tint());
    }

    @Test
    void badFilesAreRefusedWithAReason() {
        assertNotNull(badReason("a", "{\"rarity\":\"MYTHIC\"}"), "unknown rarity");
        assertNotNull(badReason("a", "{\"tax_percent\":120}"), "tax above 90");
        assertNotNull(badReason("a", "{\"tax_percent\":-1}"), "negative tax");
        assertNotNull(badReason("a", "{\"tax_percent\":\"lots\"}"), "tax not a number");
        assertNotNull(badReason("a", "{\"skills\":\"tensura:fire\"}"), "skills not a list");
        assertNotNull(badReason("a", "{\"skills\":[\"Fire Ball\"]}"), "skill id not a resource id");
        assertNotNull(badReason("a", "{\"skills\":[\"a:b\",\"a:c\",\"a:d\",\"a:e\",\"a:f\",\"a:g\",\"a:h\",\"a:i\",\"a:j\"]}"), "too many skills");
        assertNotNull(badReason("a", "{\"magic\":\"Fire Magic\"}"), "magic is a name, not a sentence");
        assertNotNull(badReason("a", "{\"tint\":\"red\"}"), "tint not a colour");
        assertNotNull(badReason("a", "{\"name\":\"" + "x".repeat(60) + "\"}"), "name too long");
        assertNotNull(badReason("a", "{\"icon\":\"not an id\"}"), "icon not a location");
        assertNotNull(badReason("Bad Id", "{}"), "id not snake_case");
        assertNotNull(badReason("1abc", "{}"), "id starts with a digit");
    }

    @Test
    void theLookTableFallsBackToTheRarityColour() {
        Aptitude a = new Aptitude("unseen", "Unseen", Aptitude.Rarity.EPIC, 5, "", List.of());
        AptitudeLook.replaceAll(null);
        assertEquals(AptitudeLook.rarityColour(Aptitude.Rarity.EPIC), AptitudeLook.of(a).tint());
        assertEquals("nusmp:textures/gui/soulforge/apt/unseen.png", AptitudeLook.of(a).icon());
    }
}
