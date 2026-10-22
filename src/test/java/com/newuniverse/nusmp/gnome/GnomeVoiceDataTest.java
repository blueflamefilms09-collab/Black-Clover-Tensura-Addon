package com.newuniverse.nusmp.gnome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The Gnome's spoken lines as data: the script (tools/audio/gnome_voice.py), the manifest, the catalogue, sounds.json and the files must say the same, and the audio must stay small. */
class GnomeVoiceDataTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final String VOICE_ID = "xHxp1c5pQOzhWjBqV78M";

    private static List<String> planned() {
        List<String> all = new ArrayList<>();
        for (String g : GnomeMoodRules.groups()) all.addAll(GnomeMoodRules.planned(g));
        return all;
    }

    @Test
    void theScriptPlansBetweenFifteenAndTwentyFiveLinesAndTheGroupsAreItsLines() throws IOException {
        String py = Files.readString(Path.of("tools/audio/gnome_voice.py"));
        Set<String> suffixes = new HashSet<>(planned());
        assertEquals(planned().size(), suffixes.size(), "a line is in one group only");
        assertTrue(suffixes.size() >= 15 && suffixes.size() <= 25, "the owner asked for 15 to 25 lines: " + suffixes.size());
        for (String s : suffixes) assertTrue(py.contains("\"" + s + "\": \"["), "the script has the prompt of " + s);
        assertTrue(py.contains(VOICE_ID), "one distinct library voice");
        assertFalse(py.toLowerCase().contains("clone"), "no cloning: the script only says it does not clone");
    }

    @Test
    void theRecordedLinesAreAtLeastFifteenAndEveryOneIsRegisteredEverywhere() throws IOException {
        List<String> recorded = VoiceCatalog.GNOME_LINES;
        assertTrue(recorded.size() >= 15, "at least fifteen lines have a recording: " + recorded.size());
        Set<String> plan = new HashSet<>(planned());
        for (String s : recorded) assertTrue(plan.contains(s), s + " belongs to a group");
        JsonArray manifest = JsonParser.parseString(Files.readString(Path.of("tools/audio/voice_lines.json"))).getAsJsonArray();
        List<String> inManifest = new ArrayList<>();
        for (JsonElement e : manifest) {
            JsonObject o = e.getAsJsonObject();
            if (!"gnome".equals(o.get("role").getAsString())) continue;
            inManifest.add(o.get("id").getAsString());
            assertEquals(VOICE_ID, o.get("voice").getAsString());
            assertFalse(o.get("prompt").getAsString().isBlank());
        }
        List<String> want = new ArrayList<>();
        for (String s : recorded) want.add(VoiceCatalog.gnomeId(s));
        assertEquals(want, inManifest, "the manifest lists the recorded lines in the catalogue's order");
        JsonObject sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        JsonObject report = JsonParser.parseString(Files.readString(Path.of("tools/audio/voice_report.json"))).getAsJsonObject();
        for (String id : want) {
            JsonObject e = sounds.getAsJsonObject("voice." + id);
            assertNotNull(e, "sounds.json entry for " + id);
            assertFalse(e.has("subtitle"), id + ": a spoken line has no subtitle (the text is shown by the game's own message)");
            assertEquals("nusmp:voice/gnome/" + id, e.getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString());
            assertTrue(report.has(id), "voice_report.json has " + id);
        }
        for (String key : sounds.keySet()) if (key.startsWith("voice.gnome_")) assertTrue(want.contains(key.substring("voice.".length())), "no entry without a recording: " + key);
    }

    @Test
    void theFilesAreShortAndTheWholeGnomeAudioIsUnderThreeMegabytes() throws IOException {
        long total = 0;
        for (String s : VoiceCatalog.GNOME_LINES) {
            Path f = ASSETS.resolve("sounds/voice/gnome/" + VoiceCatalog.gnomeId(s) + ".ogg");
            assertTrue(Files.exists(f), "the recording of " + s);
            long size = Files.size(f);
            assertTrue(size >= 3_000 && size <= 250_000, f + " is " + size + " bytes");
            byte[] d = Files.readAllBytes(f);
            assertTrue(d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', f + " is Ogg");
            total += size;
        }
        try (Stream<Path> files = Files.list(ASSETS.resolve("sounds/voice/gnome"))) {
            for (Path f : (Iterable<Path>) files::iterator) assertTrue(VoiceCatalog.has(f.getFileName().toString().replace(".ogg", "")), "no file without a catalogue line: " + f);
        }
        try (Stream<Path> files = Files.list(ASSETS.resolve("sounds/gnome"))) {
            for (Path f : (Iterable<Path>) files::iterator) total += Files.size(f);
        }
        assertTrue(total < 3_000_000, "the Gnome's audio (cues and voice) is under 3 MB: " + total);
    }

    @Test
    void theVoiceIsOnlyHeardNearbyAndLimitedByConstants() {
        assertEquals(32.0, GnomeMoodRules.LINE_RADIUS);
        assertTrue(GnomeMoodRules.LINE_GAP_TICKS >= 80, "at most one line every four seconds to a listener");
        assertTrue(GnomeMoodRules.LINE_REPEAT_TICKS >= 400, "the same line is not repeated to a listener at once");
        for (GnomeMoodRules.Situation s : GnomeMoodRules.Situation.values())
            if (GnomeMoodRules.of(s).lineChance() > 0 && s != GnomeMoodRules.Situation.OWNER_DEATH) assertTrue(GnomeMoodRules.of(s).cooldown() >= 6, s + " has a cooldown");
    }
}
