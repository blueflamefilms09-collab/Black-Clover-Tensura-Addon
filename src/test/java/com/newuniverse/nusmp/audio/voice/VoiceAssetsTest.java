package com.newuniverse.nusmp.audio.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: four things must say the same about the spoken lines: the manifest ({@code tools/audio/voice_lines.json}, the prompts the voices were made from), the catalogue ({@link VoiceCatalog},
 * from which {@link VoiceSounds} registers one sound event per line), the entries of {@code assets/nusmp/sounds.json} and the {@code .ogg} files under {@code sounds/voice/<role>/}. A line
 * without a file logs an error in the game, an entry without a file the same, a file nothing names is dead weight. This test walks all of it: every manifest id is in the catalogue (same ids,
 * same order), has a file that is a mono Ogg Vorbis between 0.5 and 12 seconds and a sane size, has its {@code voice.<id>} entry with the subtitle off, no entry or file is an orphan, the
 * registry really loops over the catalogue, and the whole set stays small.
 */
class VoiceAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path VOICE = ASSETS.resolve("sounds/voice");
    private static final Path MANIFEST = Path.of("tools/audio/voice_lines.json");
    private static final double MIN_SECONDS = 0.5, MAX_SECONDS = 12.0;
    private static final long MIN_BYTES = 3_000, MAX_BYTES = 250_000, BUDGET = 12L * 1024 * 1024;

    record Ogg(int channels, int sampleRate, double seconds) {}

    static Ogg readOgg(Path file) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = indexOf(d, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'});
        assertTrue(at > 0, file + " has no Vorbis identification header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int channels = d[at + 11] & 0xff;
        int rate = b.getInt(at + 12);
        int last = lastIndexOf(d, new byte[]{'O', 'g', 'g', 'S'});
        long granule = b.getLong(last + 6);
        return new Ogg(channels, rate, granule / (double) rate);
    }

    private static int indexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = 0; i <= d.length - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = d.length - pat.length; i >= 0; i--) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    private static JsonObject soundsJson() throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
    }

    @Test
    void theCatalogueIsTheManifestInTheSameOrder() throws IOException {
        JsonArray lines = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonArray();
        List<String> ids = new ArrayList<>();
        for (JsonElement e : lines) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            ids.add(id);
            assertTrue(id.startsWith(o.get("role").getAsString() + "_"), id + " does not carry its role as a prefix");
            assertFalse(o.get("prompt").getAsString().isBlank(), id + " has no prompt");
            assertFalse(o.get("voice").getAsString().isBlank(), id + " has no voice id");
        }
        assertEquals(120 + VoiceCatalog.GNOME_LINES.size(), ids.size(), "the owner asked for all 120 lines; the Gnome's lines are appended after them");
        assertEquals(ids, VoiceCatalog.baseIds(), "VoiceCatalog and tools/audio/voice_lines.json must list the same lines (the Undine's lines come after them)");
        assertEquals(new HashSet<>(ids).size(), ids.size(), "an id is listed twice");
    }

    @Test
    void everyLineHasAnEntryAndAShortMonoOggFile() throws IOException {
        JsonObject doc = soundsJson();
        List<String> bad = new ArrayList<>();
        long total = 0;
        for (String id : VoiceCatalog.ids()) {
            String key = VoiceCatalog.eventId(id);
            JsonElement entry = doc.get(key);
            if (entry == null || !entry.isJsonObject()) {
                bad.add("no sounds.json entry " + key);
                continue;
            }
            JsonObject e = entry.getAsJsonObject();
            if (e.has("subtitle") && !VoiceCatalog.UNDINE.equals(VoiceCatalog.role(id))) bad.add(key + ": subtitles must be off (the text is shown by the message itself)");        // the Undine's words are written nowhere else: her lines have a subtitle (UndineVoiceAssetsTest)
            JsonArray sounds = e.getAsJsonArray("sounds");
            if (sounds == null || sounds.size() != 1) {
                bad.add(key + ": exactly one file expected");
                continue;
            }
            JsonElement s = sounds.get(0);
            String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
            if (!name.equals("nusmp:" + VoiceCatalog.filePath(id))) bad.add(key + ": names " + name + ", expected nusmp:" + VoiceCatalog.filePath(id));
            Path f = ASSETS.resolve("sounds").resolve(VoiceCatalog.filePath(id) + ".ogg");
            if (!Files.exists(f)) {
                bad.add(key + ": missing file " + f);
                continue;
            }
            long size = Files.size(f);
            total += size;
            if (size < MIN_BYTES || size > MAX_BYTES) bad.add(f + ": " + size + " bytes");
            Ogg ogg = readOgg(f);
            if (ogg.channels() != 1) bad.add(f + ": " + ogg.channels() + " channels (mono only)");
            if (ogg.sampleRate() != 32000) bad.add(f + ": sample rate " + ogg.sampleRate());
            if (ogg.seconds() < MIN_SECONDS || ogg.seconds() > MAX_SECONDS) bad.add(f + ": " + String.format("%.2f", ogg.seconds()) + " s");
        }
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad.subList(0, Math.min(12, bad.size())));
        assertTrue(total < BUDGET, "the voices take " + total + " bytes, the budget is " + BUDGET);
    }

    @Test
    void noVoiceEntryAndNoVoiceFileIsAnOrphan() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> events = new HashSet<>();
        for (String id : VoiceCatalog.ids()) events.add(VoiceCatalog.eventId(id));
        List<String> bad = new ArrayList<>();
        for (String key : doc.keySet()) if (key.startsWith("voice.") && !events.contains(key)) bad.add("sounds.json entry " + key + " is not in the catalogue");
        Set<String> expected = new HashSet<>();
        for (String id : VoiceCatalog.ids()) expected.add(VoiceCatalog.filePath(id) + ".ogg");
        try (Stream<Path> files = Files.walk(VOICE)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String rel = ASSETS.resolve("sounds").relativize(f).toString().replace('\\', '/');
                if (!expected.contains(rel)) bad.add("no line names the file " + rel);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theRegistryLoopsOverTheCatalogueSoNoLineCanBeUnregistered() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/audio/voice/VoiceSounds.java"));
        assertTrue(src.contains("for (String line : VoiceCatalog.ids())"), "VoiceSounds must register every catalogue line in a loop");
        assertTrue(src.contains("SOUNDS.register(id,"), "VoiceSounds must register the event under the catalogue's event id");
    }

    @Test
    void theOlderSoundEntriesAreKept() throws IOException {
        JsonObject doc = soundsJson();
        for (String old : new String[]{"zagred_word", "zagred_seal", "music.hub", "music.rift", "mob.goblin.ambient"}) assertTrue(doc.has(old), "the older entry " + old + " must stay");
    }
}
