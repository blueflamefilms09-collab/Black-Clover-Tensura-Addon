package com.newuniverse.nusmp.undine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: five things must say the same about the Undine's spoken lines: the manifest ({@code tools/audio/undine_lines.json}: what the voice was asked to say), the catalogue ({@link UndineLines}: which lines
 * are REGISTERED), the entries of {@code sounds.json} ({@code voice.undine_*}, with a subtitle), the {@code .ogg} files under {@code sounds/voice/undine/} and the subtitle texts of {@code tools/lang/undine.json}.
 * A registered line without a file is a log error in the game; a file nobody registers is dead weight; a line whose file does not exist must NOT be registered (the group then falls back to the hum), which is exactly
 * what {@code tools/audio/undine_voice_convert.py} keeps true. The set stays far under 3 MB.
 */
class UndineVoiceAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path VOICE = ASSETS.resolve("sounds/voice/undine");
    private static final Path MANIFEST = Path.of("tools/audio/undine_lines.json"), REPORT = Path.of("tools/audio/undine_voice_report.json"), LANG = Path.of("tools/lang/undine.json");
    private static final long BUDGET = 3L * 1024 * 1024;

    private static JsonObject sounds() throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject(); }

    private static double seconds(Path file, int[] channelsAndRate) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = -1;
        for (int i = 0; i < d.length - 7 && at < 0; i++) if (d[i] == 1 && d[i + 1] == 'v' && d[i + 2] == 'o' && d[i + 3] == 'r' && d[i + 4] == 'b' && d[i + 5] == 'i' && d[i + 6] == 's') at = i;
        assertTrue(at > 0, "no Vorbis header in " + file);
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        channelsAndRate[0] = d[at + 11] & 0xff;
        channelsAndRate[1] = b.getInt(at + 12);
        int last = -1;
        for (int i = d.length - 4; i >= 0 && last < 0; i--) if (d[i] == 'O' && d[i + 1] == 'g' && d[i + 2] == 'g' && d[i + 3] == 'S') last = i;
        return b.getLong(last + 6) / (double) channelsAndRate[1];
    }

    @Test
    void theManifestIsTheCatalogueOfFortyLinesWithOneLibraryVoice() throws IOException {
        JsonArray lines = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonArray();
        List<String> ids = new ArrayList<>();
        Set<String> voices = new HashSet<>();
        for (JsonElement e : lines) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            ids.add(id);
            assertEquals(UndineLines.groupOf(id).key(), o.get("group").getAsString(), id + " is in its group");
            assertFalse(o.get("prompt").getAsString().isBlank(), id + " has a prompt");
            assertTrue(o.get("prompt").getAsString().replaceAll("\\[[^\\]]*\\]\\s*", "").trim().length() >= 3, id + " has words to say");
            assertFalse(o.get("model").getAsString().isBlank());
            voices.add(o.get("voice").getAsString());
            assertEquals("Julie I", o.get("voice_name").getAsString(), "one distinct library voice, named in the manifest (never a clone)");
        }
        assertEquals(40, ids.size(), "the owner asked for 30 to 50 lines");
        assertEquals(UndineLines.manifestIds(), ids, "UndineLines and tools/audio/undine_lines.json list the same lines in the same order");
        assertEquals(1, voices.size(), "one voice for all of her lines");
    }

    @Test
    void registeredMeansHasAFileAnEntryASubtitleAndAShortMonoOgg() throws IOException {
        JsonObject doc = sounds();
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        long total = 0;
        List<String> bad = new ArrayList<>();
        for (String id : UndineLines.ids()) {
            assertTrue(VoiceCatalog.has(id), id + " is in the voice catalogue (so the registry, the packet check and the client know it)");
            assertEquals("undine", VoiceCatalog.role(id));
            assertEquals("voice/undine/" + id, VoiceCatalog.filePath(id));
            JsonObject e = doc.getAsJsonObject(VoiceCatalog.eventId(id));
            if (e == null) { bad.add("no sounds.json entry for " + id); continue; }
            if (!e.has("subtitle") || !e.get("subtitle").getAsString().equals("subtitles.nusmp.voice." + id)) bad.add(id + ": subtitle key");
            if (!lang.has("subtitles.nusmp.voice." + id) || !lang.get("subtitles.nusmp.voice." + id).getAsString().startsWith("Undine:")) bad.add(id + ": no subtitle text in tools/lang/undine.json");
            JsonArray s = e.getAsJsonArray("sounds");
            JsonElement first = s.get(0);
            String name = first.isJsonObject() ? first.getAsJsonObject().get("name").getAsString() : first.getAsString();
            if (s.size() != 1 || !name.equals("nusmp:voice/undine/" + id)) bad.add(id + ": names " + name);
            Path f = VOICE.resolve(id + ".ogg");
            if (!Files.exists(f)) { bad.add("missing file " + f); continue; }
            long size = Files.size(f);
            total += size;
            int[] cr = new int[2];
            double sec = seconds(f, cr);
            if (cr[0] != 1) bad.add(id + ": " + cr[0] + " channels");
            if (cr[1] != 32000) bad.add(id + ": rate " + cr[1]);
            if (sec < 0.5 || sec > 12.0) bad.add(id + ": " + sec + " s");
            if (size < 3_000 || size > 250_000) bad.add(id + ": " + size + " bytes");
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertTrue(total < BUDGET, "the spoken lines take " + total + " bytes, the budget is " + BUDGET);
    }

    @Test
    void noEntryNoFileNoSubtitleIsAnOrphanAndTheReportIsTrue() throws IOException {
        Set<String> registered = new HashSet<>(UndineLines.ids());
        List<String> bad = new ArrayList<>();
        for (String key : sounds().keySet()) if (key.startsWith("voice.undine_") && !registered.contains(key.substring("voice.".length()))) bad.add("sounds.json " + key + " names a line that is not registered");
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        for (String key : lang.keySet()) if (key.startsWith("subtitles.nusmp.voice.undine_") && !registered.contains(key.substring("subtitles.nusmp.voice.".length()))) bad.add("lang " + key + " is an orphan");
        if (Files.isDirectory(VOICE)) try (Stream<Path> files = Files.list(VOICE)) {
            files.forEach(f -> { String n = f.getFileName().toString(); if (!n.endsWith(".ogg") || !registered.contains(n.substring(0, n.length() - 4))) bad.add("file " + n + " is not registered"); });
        }
        assertTrue(bad.isEmpty(), bad.toString());
        // the report the converter wrote says exactly this: converted = registered, converted + missing + rejected = all
        JsonObject report = JsonParser.parseString(Files.readString(REPORT)).getAsJsonObject();
        Set<String> converted = new HashSet<>(report.getAsJsonObject("converted").keySet());
        assertEquals(registered, converted, "UndineLines registers exactly the lines the converter converted (run tools/audio/undine_voice_convert.py again)");
        int missing = report.getAsJsonArray("missing").size(), rejected = report.getAsJsonArray("rejected").size();
        assertEquals(UndineLines.manifestIds().size(), converted.size() + missing + rejected);
        for (JsonElement m : report.getAsJsonArray("missing")) assertFalse(registered.contains(m.getAsString()), m + " has no take, so it must not be registered");
        for (String id : UndineLines.manifestIds()) if (!registered.contains(id)) assertFalse(Files.exists(VOICE.resolve(id + ".ogg")), id + " is not registered, so its file must not be there");
    }

    @Test
    void theHumFallbackIsARegisteredSoundSoAGroupWithoutRecordedLinesIsNeverSilent() throws IOException {
        JsonObject doc = sounds();
        assertTrue(doc.has(UndineSoundSet.Cue.HUM.event()));
        assertEquals(3, UndineSoundSet.Cue.HUM.variants());
        for (int i = 1; i <= 3; i++) assertTrue(Files.exists(ASSETS.resolve("sounds/undine/hum_" + i + ".ogg")));
    }
}
