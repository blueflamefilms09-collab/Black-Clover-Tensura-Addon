package com.newuniverse.nusmp.sylph;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sylph's spoken lines (docs/magic/sylph.md, "Voice lines"): the manifest ({@code tools/audio/sylph_lines.json}, every line she is meant to have), the list the game registers
 * ({@link SylphVoiceLines}, only lines whose file exists), the files under {@code sounds/voice/sylph/}, the {@code voice.sylph_*} entries of {@code sounds.json}, the subtitles
 * ({@code tools/lang/sylph_voice.json}) and the report must say the same; a line that was not generated is not registered; the whole set stays small.
 */
class SylphVoiceDataTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DIR = ASSETS.resolve("sounds/voice/sylph");
    private static final Path MANIFEST = Path.of("tools/audio/sylph_lines.json");
    private static final Path LANG = Path.of("tools/lang/sylph_voice.json");
    private static final Path REPORT = Path.of("tools/audio/sylph_voice_report.json");

    private static JsonArray manifest() throws IOException { return JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonArray(); }

    private static JsonObject obj(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theManifestListsEveryLineSheIsMeantToHaveWithAVoiceAndATextAndNoTags() throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> groups = new LinkedHashSet<>();
        for (JsonElement e : manifest()) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            assertTrue(ids.add(id), "unique " + id);
            assertEquals("sylph", o.get("role").getAsString());
            assertTrue(id.matches("sylph_[a-z]+_[0-9]+"), id);
            String group = o.get("group").getAsString();
            groups.add(group);
            assertTrue(id.startsWith("sylph_" + group + "_"), id + " carries its group");
            assertFalse(o.get("voice").getAsString().isBlank(), id + " has a voice id");
            String text = o.get("text").getAsString(), prompt = o.get("prompt").getAsString();
            assertFalse(text.isBlank());
            assertFalse(text.contains("[") || text.contains("]"), "the subtitle text has no audio tags: " + id);
            assertTrue(prompt.endsWith(text) && prompt.startsWith("["), id + ": the prompt is the tags and then the text");
            String low = text.toLowerCase();
            assertFalse(low.contains("hunger") || low.contains("starv") || low.contains("food") || low.contains("eat "), "no hunger talk: " + id);
            assertTrue(text.length() <= 70, "short lines: " + id);
        }
        assertEquals(56, ids.size(), "about 40 to 60 lines were asked for");
        assertTrue(groups.containsAll(Set.of("link", "unlink", "cast", "big", "shout", "hurt", "startle", "idle", "worry", "cheer", "sad", "giggle", "pout", "smug", "sleepy", "victory")));
        for (SylphMood.Event e : SylphMood.Event.values()) {
            String g = SylphMood.spec(e).voice();
            if (g != null) assertTrue(groups.contains(g), "the mood table names the group " + g);
        }
    }

    @Test
    void onlyLinesWhoseFileExistsAreRegisteredAndEveryFileIsRegisteredWithItsSubtitle() throws IOException {
        Set<String> inManifest = new HashSet<>();
        for (JsonElement e : manifest()) inManifest.add(e.getAsJsonObject().get("id").getAsString());
        JsonObject sounds = obj(ASSETS.resolve("sounds.json")), lang = obj(LANG), report = obj(REPORT);
        List<String> bad = new ArrayList<>();
        long total = 0;
        for (String id : SylphVoiceLines.ids()) {
            if (!inManifest.contains(id)) bad.add("registered but not in the manifest: " + id);
            Path f = DIR.resolve(id + ".ogg");
            if (!Files.exists(f)) { bad.add("registered but no file: " + f); continue; }
            total += Files.size(f);
            JsonObject e = sounds.has("voice." + id) ? sounds.getAsJsonObject("voice." + id) : null;
            if (e == null) { bad.add("no sounds.json entry voice." + id); continue; }
            if (!SylphVoiceLines.subtitleKey(id).equals(e.has("subtitle") ? e.get("subtitle").getAsString() : null)) bad.add(id + ": subtitle key");
            if (!lang.has(SylphVoiceLines.subtitleKey(id))) bad.add(id + ": no subtitle text in tools/lang/sylph_voice.json");
            if (!report.has(id)) bad.add(id + ": not in the report");
        }
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String id = f.getFileName().toString().replace(".ogg", "");
                if (!SylphVoiceLines.ids().contains(id)) bad.add("a file the list does not register (run tools/audio/sylph_voice_convert.py): " + id);
            }
        }
        for (String key : sounds.keySet()) if (key.startsWith("voice.sylph_") && !SylphVoiceLines.ids().contains(key.substring("voice.".length()))) bad.add("an entry for a line that is not registered: " + key);
        for (String key : lang.keySet()) assertTrue(key.startsWith("subtitles.nusmp.voice.sylph_"), "the fragment holds Sylph's subtitles only: " + key);
        assertTrue(bad.isEmpty(), bad.toString());
        assertTrue(total < 3_000_000, "her spoken lines stay under 3 MB: " + total);
        assertEquals(SylphVoiceLines.ids().size(), report.size(), "the report covers exactly the registered lines");
    }

    @Test
    void aGroupHoldsContiguousLinesFromOneSoPickingNeverHitsAGap() {
        for (var g : SylphVoiceLines.groups().entrySet())
            for (int i = 0; i < g.getValue().size(); i++) assertEquals(SylphVoiceLines.id(g.getKey(), i + 1), g.getValue().get(i));
    }

    @Test
    void theDocNamesTheVoiceItsLicenceAndWhatIsMissing() throws IOException {
        String doc = Files.readString(Path.of("docs/magic/sylph.md"));
        assertTrue(doc.contains("Candy - Girly and Sweet") && doc.contains("Nggzl2QAXh3OijoXD116"), "the voice is named");
        assertTrue(doc.toLowerCase().contains("licen"), "the licence question is stated");
        assertTrue(doc.contains("sylph_voice_fetch.py") && doc.contains("sylph_voice_convert.py"), "the one-command way to finish the set is documented");
    }
}
