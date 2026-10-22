package com.newuniverse.nusmp.spiritdark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The spoken lines of the Spirit of Darkness: the plan (tools/audio/spirit_dark_lines.json), the recordings that exist, the data file the game reads (only lines whose file exists), the sounds.json
 * entries, the budget, and the per listener limits. A line without a recording must never be registered; a recording must always be registered.
 */
class SpiritDarkVoiceTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path VOICE_DIR = ASSETS.resolve("sounds/spirit/dark/voice");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static List<String> recordings() throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(VOICE_DIR)) return out;
        try (Stream<Path> s = Files.list(VOICE_DIR)) {
            for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".ogg")).sorted()::iterator) out.add(f.getFileName().toString().replace(".ogg", ""));
        }
        return out;
    }

    @Test
    void thePlanHasThirtyToFiftyShortLinesInTheGroupsTheCodeKnows() throws IOException {
        JsonObject m = json(Path.of("tools/audio/spirit_dark_lines.json"));
        JsonArray lines = m.getAsJsonArray("lines");
        assertTrue(lines.size() >= 30 && lines.size() <= 50, "30 to 50 lines, is " + lines.size());
        Set<String> ids = new HashSet<>();
        Map<String, Integer> perGroup = new LinkedHashMap<>();
        for (JsonElement e : lines) {
            JsonObject l = e.getAsJsonObject();
            String id = l.get("id").getAsString(), group = l.get("group").getAsString(), text = l.get("text").getAsString();
            assertTrue(ids.add(id), "unique id " + id);
            assertEquals(group + "_" + (perGroup.merge(group, 1, Integer::sum)), id, "ids count up inside their group");
            assertEquals(SpiritDarkVoiceCatalog.groupOf(id), group);
            assertTrue(text.length() >= 8 && text.length() <= 90, "short: " + text);
            assertTrue(l.get("prompt").getAsString().endsWith(text.substring(text.indexOf(' ') + 1)) || l.get("prompt").getAsString().contains(text.split("[.!?]")[0]), "the prompt speaks the text: " + id);
            assertEquals("lo1O1Dz1Tu4FWVEPaxuT", l.get("voice").getAsString());
            String low = text.toLowerCase();
            assertFalse(low.contains("hunger") || low.contains("starv") || low.contains("food"), id);
        }
        assertEquals(SpiritDarkVoiceCatalog.PLANNED, perGroup, "the code's planned counts are the manifest's");
        assertEquals("eleven_v4", m.get("model").getAsString());
    }

    @Test
    void everyRecordingIsPlannedRegisteredAndNamedInTheDataFile() throws IOException {
        JsonObject data = json(ASSETS.resolve("spirit_dark_voice.json"));
        JsonObject lines = data.getAsJsonObject("lines");
        JsonObject sounds = json(ASSETS.resolve("sounds.json"));
        List<String> files = recordings();
        for (String id : files) {
            assertTrue(lines.has(id), id + " has a recording but the game's data file does not list it (run tools/audio/spirit_dark_voice.py sync)");
            assertTrue(sounds.has("spirit.dark.voice." + id), id + " has no sounds.json entry");
            JsonObject s = sounds.getAsJsonObject("spirit.dark.voice." + id).getAsJsonArray("sounds").get(0).getAsJsonObject();
            assertEquals("nusmp:spirit/dark/voice/" + id, s.get("name").getAsString());
            assertEquals(32, s.get("attenuation_distance").getAsInt(), "carries 32 blocks and no farther");
        }
        for (var e : lines.entrySet()) assertTrue(files.contains(e.getKey()), "the data file lists " + e.getKey() + " but no recording exists: a line without a recording must never be registered");
        for (String k : sounds.keySet()) if (k.startsWith("spirit.dark.voice.")) assertTrue(files.contains(k.substring("spirit.dark.voice.".length())), k + " has no file");
        Map<String, String> parsed = SpiritDarkVoiceCatalog.parse(Files.readString(ASSETS.resolve("spirit_dark_voice.json")));
        assertEquals(lines.size(), parsed.size(), "every listed line is a planned one");
    }

    @Test
    void theRecordingsAreModestAndShortOggFiles() throws IOException {
        long total = 0;
        for (String id : recordings()) {
            Path f = VOICE_DIR.resolve(id + ".ogg");
            byte[] head = Files.readAllBytes(f);
            assertTrue(head.length > 1000 && head[0] == 'O' && head[1] == 'g' && head[2] == 'g' && head[3] == 'S', id + " is an Ogg file");
            total += head.length;
        }
        assertTrue(total < 3L * 1024 * 1024, "under about 3 MB for all the spoken lines: " + total);
        Path report = Path.of("tools/audio/spirit_dark_voice_report.json");
        if (Files.exists(report)) {
            JsonObject r = json(report);
            for (var e : r.entrySet()) {
                double secs = e.getValue().getAsJsonObject().get("seconds").getAsDouble();
                assertTrue(secs >= 0.8 && secs <= 9.0, e.getKey() + " is " + secs + " s");
            }
        }
    }

    @Test
    void theCatalogOnlyKnowsPlannedLinesAndPicksWithoutRepeating() {
        Map<String, String> m = SpiritDarkVoiceCatalog.parse("{\"lines\": {\"cast_1\": \"A\", \"cast_2\": \"B\", \"cast_99\": \"no\", \"bogus_1\": \"no\", \"hurt_1\": \"C\", \"cast_0\": \"no\"}}");
        assertEquals(3, m.size(), "only planned groups and numbers: " + m);
        SpiritDarkVoiceCatalog.setForTest(m);
        assertEquals(List.of("cast_1", "cast_2"), SpiritDarkVoiceCatalog.inGroup("cast"));
        assertTrue(SpiritDarkVoiceCatalog.has("hurt_1") && !SpiritDarkVoiceCatalog.has("link_1"));
        assertNull(SpiritDarkVoiceCatalog.pick("link", 0.3f, null), "a group without a line gives none: the game then uses the synthesised cue");
        assertEquals("cast_1", SpiritDarkVoiceCatalog.pick("cast", 0.0f, null));
        assertEquals("cast_2", SpiritDarkVoiceCatalog.pick("cast", 0.0f, "cast_1"), "never the line just spoken when there is another");
        assertEquals("hurt_1", SpiritDarkVoiceCatalog.pick("hurt", 0.9f, "hurt_1"), "a lone line may repeat");
        SpiritDarkVoiceCatalog.setForTest(new LinkedHashMap<>());
        assertEquals(0, SpiritDarkVoiceCatalog.count());
        for (String g : SpiritDarkVoiceCatalog.PLANNED.keySet()) assertNotNull(SpiritDarkVoiceCatalog.fallback(g), "every group has a synthesised stand-in: " + g);
        assertEquals("power_end", SpiritDarkVoiceCatalog.groupOf("power_end_2"));
        assertNull(SpiritDarkVoiceCatalog.groupOf("nounderscore"));
    }

    @Test
    void theListenerHearsAtMostOneLineEveryFourSecondsAndTheServerFewPerTick() {
        SpiritDarkVoiceCatalog.ListenerGate g = new SpiritDarkVoiceCatalog.ListenerGate();
        UUID a = UUID.randomUUID();
        assertTrue(g.tryHear(a, "cast_1", 1000));
        assertFalse(g.tryHear(a, "hurt_1", 1000 + SpiritDarkVoiceCatalog.LISTENER_GAP - 1), "one line per listener per four seconds, whoever speaks");
        assertEquals(80, SpiritDarkVoiceCatalog.LISTENER_GAP, "four seconds");
        assertTrue(g.tryHear(a, "hurt_1", 1000 + SpiritDarkVoiceCatalog.LISTENER_GAP));
        assertFalse(g.tryHear(a, "cast_1", 1000 + 2 * SpiritDarkVoiceCatalog.LISTENER_GAP), "the same line not again for thirty seconds");
        assertTrue(g.tryHear(a, "cast_1", 1000 + SpiritDarkVoiceCatalog.LINE_COOLDOWN + 1));
        int heard = 0;
        for (int i = 0; i < 30; i++) if (g.tryHear(UUID.randomUUID(), "kill_1", 9000)) heard++;
        assertEquals(SpiritDarkVoiceCatalog.PER_TICK, heard, "the server starts at most " + SpiritDarkVoiceCatalog.PER_TICK + " lines a tick");
        g.forget(a);
        assertFalse(g.tryHear(null, "x", 1));
        assertEquals(32.0, SpiritDarkVoiceCatalog.RANGE, "players within 32 blocks only");
    }
}
