package com.newuniverse.nusmp.audio.music;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: three things must say the same: the catalogue ({@link MusicCatalog}, from which {@link MusicSounds} registers one event per track), the {@code music.*} entries of
 * {@code assets/nusmp/sounds.json} and the {@code .ogg} loops that {@code tools/audio/gen_music.py} synthesises. An event without an entry plays nothing, an entry without a file
 * logs an error in the game, a file nothing names is dead weight. This walks all of it: each track has an entry that streams one existing mono Ogg Vorbis file of 60 to 90 seconds,
 * no music entry or file is an orphan, the whole set is within the 25 MB budget, the older entries (the mob voices, the Zagred voice lines) are still there, and none of the music
 * leaked into the mob voice folder the other audio test guards.
 */
class MusicAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path MUSIC = ASSETS.resolve("sounds/music");
    private static final long BUDGET = 25L * 1024 * 1024;

    private static JsonObject soundsJson() throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
    }

    private static String nameOf(JsonElement s) { return s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString(); }

    private record Ogg(int channels, int sampleRate, double seconds) {}

    private static Ogg readOgg(Path file) throws IOException {
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

    @Test
    void everyTrackHasAnEntryAndALoopOfTheRightShape() throws IOException {
        JsonObject doc = soundsJson();
        List<String> bad = new ArrayList<>();
        for (String track : MusicCatalog.trackNames()) {
            String id = MusicCatalog.eventId(track);
            JsonElement entry = doc.get(id);
            if (entry == null || !entry.isJsonObject()) {
                bad.add("no sounds.json entry " + id);
                continue;
            }
            JsonArray sounds = entry.getAsJsonObject().getAsJsonArray("sounds");
            if (sounds == null || sounds.size() != 1) {
                bad.add(id + ": expects exactly one sound");
                continue;
            }
            JsonElement s = sounds.get(0);
            if (!("nusmp:music/" + track).equals(nameOf(s))) bad.add(id + ": names " + nameOf(s));
            if (!s.isJsonObject() || !s.getAsJsonObject().has("stream") || !s.getAsJsonObject().get("stream").getAsBoolean()) bad.add(id + ": a loop this long must be streamed");
            Path f = MUSIC.resolve(track + ".ogg");
            if (!Files.exists(f)) {
                bad.add(id + ": missing file " + f);
                continue;
            }
            Ogg ogg = readOgg(f);
            if (ogg.channels() != 1) bad.add(f + ": " + ogg.channels() + " channels (mono only)");
            if (ogg.seconds() < 60.0 || ogg.seconds() > 90.0) bad.add(f + ": " + String.format("%.1f", ogg.seconds()) + " s (60 to 90)");
        }
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad.subList(0, Math.min(10, bad.size())));
    }

    @Test
    void noMusicEntryAndNoMusicFileIsAnOrphan() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> events = new HashSet<>(MusicCatalog.eventIds());
        Set<String> files = new HashSet<>();
        for (String t : MusicCatalog.trackNames()) files.add(t + ".ogg");
        List<String> bad = new ArrayList<>();
        for (String key : doc.keySet()) if (key.startsWith("music.") && !events.contains(key)) bad.add("sounds.json entry " + key + " is not in the catalogue");
        try (Stream<Path> walk = Files.list(MUSIC)) {
            for (Path f : (Iterable<Path>) walk::iterator) if (!files.contains(f.getFileName().toString())) bad.add("no track is named " + f.getFileName());
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyTrackHasATitleInTheLangFragment() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/themed_music.json"))).getAsJsonObject();
        List<String> bad = new ArrayList<>();
        for (String track : MusicCatalog.trackNames()) {
            String key = MusicCatalog.titleKey(track);
            if (!lang.has(key) || lang.get(key).getAsString().isBlank()) bad.add("no title for " + key);
        }
        assertTrue(lang.size() == MusicCatalog.trackNames().size(), "the fragment has keys that are not tracks");
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theMusicStaysWithinItsBudgetAndTheOlderEntriesAreKept() throws IOException {
        long total = 0;
        try (Stream<Path> walk = Files.list(MUSIC)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".ogg"))::iterator) total += Files.size(f);
        }
        assertTrue(total > 1024 * 1024, "the music takes only " + total + " bytes: were the files generated?");
        assertTrue(total <= BUDGET, "the music takes " + total + " bytes, the budget is " + BUDGET);
        JsonObject doc = soundsJson();
        for (String old : new String[]{"zagred_word", "zagred_seal", "zagred_fall", "zagred_shatter", "zagred_heal", "zagred_overwrite", "zagred_phase", "mob.goblin.ambient", "mob.npc.taunt"}) {
            assertTrue(doc.has(old), "the older entry " + old + " must stay");
        }
    }
}
