package com.newuniverse.nusmp.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: three things must say the same: the catalogue of mob voices ({@link MobSoundSet}, from which {@link ModSounds} registers one sound event per family and cue), the entries of
 * {@code assets/nusmp/sounds.json} (several variants per event, a subtitle key, {@code stream} for the long ones) and the {@code .ogg} files that {@code tools/gen_mob_sounds.py}
 * synthesises. A sound event without an entry plays nothing, an entry without a file logs an error in the game, a file nothing names is dead weight, a subtitle without a line shows its key.
 * This test walks all of it: every event has an entry whose files exist and are mono Ogg Vorbis between 0.3 and 3.0 seconds, no entry or file is an orphan, every subtitle has its line in
 * {@code tools/lang/mob_audio.json} (or in {@code en_us.json}), long files are streamed, the older entries (the Zagred voice lines) are still there, and the whole voice set is under 8 MB.
 */
class SoundsJsonTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path SOUNDS = ASSETS.resolve("sounds");
    private static final Path LANG_FRAGMENT = Path.of("tools/lang/mob_audio.json");
    private static final Path EN_US = ASSETS.resolve("lang/en_us.json");
    private static final double MIN_SECONDS = 0.29, MAX_SECONDS = 3.05, STREAM_OVER = 2.4;

    private static JsonObject soundsJson() throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
    }

    private static String nameOf(JsonElement s) { return s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString(); }

    private static Path fileOf(String name) { return SOUNDS.resolve(name.substring(name.indexOf(':') + 1) + ".ogg"); }

    /** What the identification header and the last page of an Ogg Vorbis file say. */
    record Ogg(int channels, int sampleRate, double seconds) {}

    static Ogg readOgg(Path file) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = indexOf(d, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'}, 0);
        assertTrue(at > 0, file + " has no Vorbis identification header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int channels = d[at + 11] & 0xff;
        int rate = b.getInt(at + 12);
        int last = lastIndexOf(d, new byte[]{'O', 'g', 'g', 'S'});
        long granule = b.getLong(last + 6);
        return new Ogg(channels, rate, granule / (double) rate);
    }

    private static int indexOf(byte[] d, byte[] pat, int from) {
        outer:
        for (int i = from; i <= d.length - pat.length; i++) {
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

    @Test
    void everyEventOfTheCatalogueHasAnEntryAndEveryFileExistsAndIsAShortMonoOgg() throws IOException {
        JsonObject doc = soundsJson();
        List<String> bad = new ArrayList<>();
        int events = 0;
        for (String id : MobSoundSet.allEventIds()) {
            events++;
            JsonElement entry = doc.get(id);
            if (entry == null || !entry.isJsonObject()) {
                bad.add("no sounds.json entry " + id);
                continue;
            }
            JsonObject e = entry.getAsJsonObject();
            if (!e.has("subtitle") || e.get("subtitle").getAsString().isBlank()) bad.add(id + ": no subtitle key");
            JsonArray sounds = e.getAsJsonArray("sounds");
            if (sounds == null || sounds.size() < 2) bad.add(id + ": needs at least two variants, has " + (sounds == null ? 0 : sounds.size()));
            if (sounds == null) continue;
            for (JsonElement s : sounds) {
                String name = nameOf(s);
                if (!name.startsWith("nusmp:mob/")) bad.add(id + ": odd sound name " + name);
                Path f = fileOf(name);
                if (!Files.exists(f)) {
                    bad.add(id + ": missing file " + f);
                    continue;
                }
                Ogg ogg = readOgg(f);
                if (ogg.channels() != 1) bad.add(f + ": " + ogg.channels() + " channels (mono only)");
                if (ogg.sampleRate() != 32000 && ogg.sampleRate() != 44100) bad.add(f + ": sample rate " + ogg.sampleRate());
                if (ogg.seconds() < MIN_SECONDS || ogg.seconds() > MAX_SECONDS) bad.add(f + ": " + String.format("%.2f", ogg.seconds()) + " s");
                boolean streamed = s.isJsonObject() && s.getAsJsonObject().has("stream") && s.getAsJsonObject().get("stream").getAsBoolean();
                if (ogg.seconds() >= STREAM_OVER && !streamed) bad.add(f + ": " + String.format("%.2f", ogg.seconds()) + " s but not streamed");
                if (streamed && ogg.seconds() < STREAM_OVER - 0.3) bad.add(f + ": streamed but only " + String.format("%.2f", ogg.seconds()) + " s");
            }
        }
        assertTrue(events >= 160, "the spec asks for at least 8 cues x 20 families, the catalogue has " + events);
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad.subList(0, Math.min(12, bad.size())));
    }

    @Test
    void noMobEntryAndNoMobFileIsAnOrphan() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> events = new HashSet<>(MobSoundSet.allEventIds());
        Set<String> named = new HashSet<>();
        List<String> bad = new ArrayList<>();
        for (String key : doc.keySet()) {
            if (key.startsWith("mob.") && !events.contains(key)) bad.add("sounds.json entry " + key + " is not in the catalogue");
            if (key.startsWith("mob.")) for (JsonElement s : doc.getAsJsonObject(key).getAsJsonArray("sounds")) named.add(fileOf(nameOf(s)).toString().replace('\\', '/'));
        }
        try (Stream<Path> files = Files.walk(SOUNDS.resolve("mob"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                if (!named.contains(f.toString().replace('\\', '/'))) bad.add("no entry names the file " + f);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everySubtitleHasItsLine() throws IOException {
        JsonObject doc = soundsJson();
        JsonObject lang = JsonParser.parseString(Files.readString(LANG_FRAGMENT)).getAsJsonObject();
        JsonObject en = JsonParser.parseString(Files.readString(EN_US)).getAsJsonObject();
        List<String> bad = new ArrayList<>();
        for (MobSoundSet.Family f : MobSoundSet.families()) {
            for (Cue c : Cue.values()) {
                String key = MobSoundSet.subtitleKey(f.id(), c);
                String declared = doc.getAsJsonObject(f.event(c)).get("subtitle").getAsString();
                if (!declared.equals(key)) bad.add(f.event(c) + " names the subtitle " + declared + ", the catalogue says " + key);
                String line = lang.has(key) ? lang.get(key).getAsString() : en.has(key) ? en.get(key).getAsString() : "";
                if (line.isBlank()) bad.add("no lang line for " + key);
            }
        }
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad.subList(0, Math.min(10, bad.size())));
    }

    @Test
    void theOlderEntriesAreKeptAndTheVoiceSetStaysModest() throws IOException {
        JsonObject doc = soundsJson();
        for (String old : new String[]{"zagred_word", "zagred_seal", "zagred_fall", "zagred_shatter", "zagred_heal", "zagred_overwrite", "zagred_phase"}) {
            assertTrue(doc.has(old), "the older entry " + old + " must stay");
        }
        long total = 0;
        int files = 0;
        try (Stream<Path> walk = Files.walk(SOUNDS.resolve("mob"))) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                total += Files.size(f);
                files++;
            }
        }
        assertTrue(total < 8L * 1024 * 1024, "the mob voices take " + total + " bytes, the budget is 8 MB");
        assertTrue(files >= 160, "only " + files + " files");
    }

    @Test
    void theHeaderParserReadsAnOggFromBytes() {
        // a hand-made minimal Vorbis identification page prefix: only the fields the reader uses
        byte[] d = new byte[200];
        d[0] = 'O'; d[1] = 'g'; d[2] = 'g'; d[3] = 'S';
        byte[] id = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        System.arraycopy(id, 0, d, 28, id.length);
        d[28 + 11] = 1;
        ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN).putInt(28 + 12, 44100);
        System.arraycopy("OggS".getBytes(StandardCharsets.US_ASCII), 0, d, 120, 4);
        ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN).putLong(120 + 6, 44100L);
        assertEquals(28, indexOf(d, id, 0));
        assertEquals(120, lastIndexOf(d, new byte[]{'O', 'g', 'g', 'S'}));
    }
}
