package com.newuniverse.nusmp.shishou;

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

/**
 * Why: five things must say the same about Shishou's spoken lines: the manifest ({@code tools/audio/shishou_lines.json}: what the voice is to say, with the voice id in its header, null until chosen), the
 * catalogue ({@link ShishouVoiceLines}: which lines are REGISTERED, and the words of each), the entries of {@code sounds.json}, the {@code .ogg} files under {@code sounds/voice/shishou/} and the lang text
 * ({@code tools/lang/shishou_voice.json}). No audio exists yet, so nothing is registered, no file and no sounds.json entry exist, and every moment is Thought Communication text; the day lines are recorded the
 * converter registers exactly those and this test keeps them honest. The whole set stays under 3 MB.
 */
class ShishouVoiceAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path VOICE = ASSETS.resolve("sounds/voice/shishou");
    private static final Path MANIFEST = Path.of("tools/audio/shishou_lines.json"), REPORT = Path.of("tools/audio/shishou_voice_report.json"), LANG = Path.of("tools/lang/shishou_voice.json");
    private static final long BUDGET = 3L * 1024 * 1024;

    private static JsonObject manifest() throws IOException { return JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject(); }

    @Test
    void theManifestIsTheCatalogueOfAboutFortyLinesWithAVoiceFieldInTheHeader() throws IOException {
        JsonObject doc = manifest();
        assertTrue(doc.has("voice") && doc.has("voice_name") && doc.has("model"), "the header names the voice (null until chosen), its name and the model");
        boolean chosen = !doc.get("voice").isJsonNull();
        if (!chosen) assertTrue(ShishouVoiceLines.ids().isEmpty(), "no voice is chosen, so no line may be registered");
        JsonArray lines = doc.getAsJsonArray("lines");
        List<String> ids = new ArrayList<>();
        for (JsonElement e : lines) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            ids.add(id);
            assertEquals(ShishouVoiceLines.groupOf(id).key(), o.get("group").getAsString(), id + " is in its group");
            String prompt = o.get("prompt").getAsString();
            assertTrue(prompt.startsWith("["), id + " starts with a delivery tag");
            String words = prompt.replaceAll("\\[[^\\]]*\\]\\s*", "").trim();
            assertTrue(words.length() >= 3, id + " has words to say");
            assertEquals(words, o.get("text").getAsString(), id + ": the text is the prompt without its tags");
            assertEquals(words, ShishouVoiceLines.text(id), id + ": the Java text block follows the manifest (run tools/audio/shishou_lines.py)");
        }
        assertTrue(ids.size() >= 36 && ids.size() <= 50, "about forty lines: " + ids.size());
        assertEquals(ShishouVoiceLines.manifestIds(), ids, "ShishouVoiceLines and tools/audio/shishou_lines.json list the same lines in the same order");
        assertEquals(ids.size(), ShishouVoiceLines.textCount());
        assertEquals("Let go of me, you aren't my wielder.", ShishouVoiceLines.text("shishou_thief_1"), "the thief line is the owner's sentence");
        assertEquals("So you are my wielder.", ShishouVoiceLines.text("shishou_claim_1").substring(0, 22));
    }

    @Test
    void everyManifestLineHasLangTextAndRegisteredMeansFileEntrySubtitle() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        JsonObject sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        for (String id : ShishouVoiceLines.manifestIds()) {
            assertTrue(lang.has(ShishouVoiceLines.textKey(id)), id + " has no lang text");
            assertEquals(ShishouVoiceLines.text(id), lang.get(ShishouVoiceLines.textKey(id)).getAsString());
        }
        long total = 0;
        List<String> bad = new ArrayList<>();
        for (String id : ShishouVoiceLines.ids()) {
            assertTrue(VoiceCatalog.has(id), id + " is in the voice catalogue (so the registry, the packet check and the client know it)");
            assertEquals("shishou", VoiceCatalog.role(id));
            assertEquals("voice/shishou/" + id, VoiceCatalog.filePath(id));
            JsonObject e = sounds.getAsJsonObject(VoiceCatalog.eventId(id));
            if (e == null) { bad.add("no sounds.json entry for " + id); continue; }
            if (!e.has("subtitle") || !e.get("subtitle").getAsString().equals("subtitles.nusmp.voice." + id)) bad.add(id + ": subtitle key");
            if (!lang.has("subtitles.nusmp.voice." + id)) bad.add(id + ": no subtitle text");
            Path f = VOICE.resolve(id + ".ogg");
            if (!Files.exists(f)) { bad.add("missing file " + f); continue; }
            long size = Files.size(f);
            total += size;
            if (size < 3_000 || size > 250_000) bad.add(id + ": " + size + " bytes");
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertTrue(total < BUDGET, "the spoken lines take " + total + " bytes, the budget is " + BUDGET);
    }

    @Test
    void noEntryNoFileNoSubtitleIsAnOrphanAndTheReportIsTrue() throws IOException {
        Set<String> registered = new HashSet<>(ShishouVoiceLines.ids());
        List<String> bad = new ArrayList<>();
        JsonObject sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        for (String key : sounds.keySet()) if (key.startsWith("voice.shishou_") && !registered.contains(key.substring("voice.".length()))) bad.add("sounds.json " + key + " names a line that is not registered");
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        for (String key : lang.keySet()) if (key.startsWith("subtitles.nusmp.voice.shishou_") && !registered.contains(key.substring("subtitles.nusmp.voice.".length()))) bad.add("lang " + key + " is an orphan");
        if (Files.isDirectory(VOICE)) try (Stream<Path> files = Files.list(VOICE)) {
            files.forEach(f -> { String n = f.getFileName().toString(); if (!n.endsWith(".ogg") || !registered.contains(n.substring(0, n.length() - 4))) bad.add("file " + n + " is not registered"); });
        }
        assertTrue(bad.isEmpty(), bad.toString());
        JsonObject report = JsonParser.parseString(Files.readString(REPORT)).getAsJsonObject();
        Set<String> converted = new HashSet<>(report.getAsJsonObject("converted").keySet());
        assertEquals(registered, converted, "ShishouVoiceLines registers exactly the lines the converter converted (run tools/audio/shishou_voice_convert.py again)");
        int missing = report.getAsJsonArray("missing").size(), rejected = report.getAsJsonArray("rejected").size();
        assertEquals(ShishouVoiceLines.manifestIds().size(), converted.size() + missing + rejected);
        for (String id : ShishouVoiceLines.manifestIds()) if (!registered.contains(id)) assertFalse(Files.exists(VOICE.resolve(id + ".ogg")), id + " is not registered, so its file must not be there");
    }

    @Test
    void theScriptsExistAndRefuseToSpendWithoutAVoice() throws IOException {
        for (String f : new String[]{"shishou_lines.py", "shishou_voice_generate.py", "shishou_voice_convert.py"}) assertTrue(Files.exists(Path.of("tools/audio").resolve(f)), f);
        String gen = Files.readString(Path.of("tools/audio/shishou_voice_generate.py"));
        assertTrue(gen.contains("not chosen yet") && gen.contains("doc.get(\"voice\")"), "the generator refuses while the header's voice is null");
        assertTrue(gen.contains("ELEVENLABS_API_KEY"));
    }
}
