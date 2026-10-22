package com.newuniverse.nusmp.spiritlight;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: four things must say the same about the Spirit of Light's sounds: the kinds of {@link SpiritLightRules#SOUND_IDS} (the events {@link SpiritLightSounds} registers), the entries
 * {@code spirit_light.*} of {@code assets/nusmp/sounds.json}, the {@code .ogg} files under {@code sounds/spirit_light/}, and the spoken lines (the manifest tools/audio/spirit_light_lines.json, the
 * {@link SpiritLightVoiceLines} data). A sound event without an entry plays nothing, an entry without a file logs an error in the game, a file nothing names is dead weight, a subtitle without a line shows its
 * key, and a spoken line without a file must never be registered at all (nothing is faked). Mono Ogg Vorbis only, short, and the whole set small.
 */
class SpiritLightSoundsDataTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp"), SOUNDS = ASSETS.resolve("sounds"), SL = SOUNDS.resolve("spirit_light");
    private static final long BUDGET = 3L * 1024 * 1024;

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

    private static JsonObject soundsJson() throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject(); }

    private static String nameOf(JsonElement s) { return s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString(); }

    private static Path fileOf(String name) { return SOUNDS.resolve(name.substring(name.indexOf(':') + 1) + ".ogg"); }

    @Test
    void everyKindOfSoundHasAnEntryWithSubtitleVariantsAndShortMonoFiles() throws IOException {
        JsonObject doc = soundsJson();
        List<String> bad = new ArrayList<>();
        for (String id : SpiritLightRules.SOUND_IDS) {
            JsonElement entry = doc.get(id);
            if (entry == null || !entry.isJsonObject()) { bad.add("no sounds.json entry " + id); continue; }
            JsonObject e = entry.getAsJsonObject();
            if (!e.has("subtitle") || !e.get("subtitle").getAsString().equals("subtitles.nusmp." + id)) bad.add(id + ": subtitle key");
            JsonArray sounds = e.getAsJsonArray("sounds");
            if (sounds == null || sounds.size() < 2) { bad.add(id + ": needs at least two variants"); continue; }
            for (JsonElement s : sounds) {
                String name = nameOf(s);
                if (!name.startsWith("nusmp:spirit_light/") || name.startsWith("nusmp:spirit_light/voice/")) bad.add(id + ": odd sound name " + name);
                Path f = fileOf(name);
                if (!Files.exists(f)) { bad.add(id + ": missing file " + f); continue; }
                Ogg ogg = readOgg(f);
                if (ogg.channels() != 1) bad.add(f + ": " + ogg.channels() + " channels (mono only)");
                if (ogg.sampleRate() != 32000 && ogg.sampleRate() != 44100) bad.add(f + ": sample rate " + ogg.sampleRate());
                if (ogg.seconds() < 0.25 || ogg.seconds() > 3.0) bad.add(f + ": " + String.format("%.2f", ogg.seconds()) + " s");
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theCubHasItsCreatureSoundsAndNoSoundIsAWord() {
        List<String> ids = List.of(SpiritLightRules.SOUND_IDS);
        for (String c : new String[]{"cub_mew", "cub_roar", "cub_yawn", "cub_chirp", "cub_whimper", "purr"}) assertTrue(ids.contains("spirit_light." + c), c);
        for (String c : new String[]{"link", "unlink", "beam", "pulse", "power"}) assertTrue(ids.contains("spirit_light." + c), c);
        assertEquals(11, SpiritLightRules.SOUND_KINDS);
    }

    @Test
    void everySpokenLineThatIsRegisteredHasAFileAndNoRegisteredEntryIsAnOrphan() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> expectedFiles = new HashSet<>();
        for (String id : SpiritLightVoiceLines.AVAILABLE) {
            String key = "spirit_light.voice." + id;
            JsonElement entry = doc.get(key);
            assertNotNull(entry, key + " has an entry");
            JsonObject e = entry.getAsJsonObject();
            assertFalse(e.has("subtitle"), key + ": no subtitle (the text goes on the owner's action bar, like every spoken line of the mod)");
            JsonArray sounds = e.getAsJsonArray("sounds");
            assertEquals(1, sounds.size(), key);
            String name = nameOf(sounds.get(0));
            assertEquals("nusmp:spirit_light/voice/" + id, name);
            assertEquals(32, sounds.get(0).getAsJsonObject().get("attenuation_distance").getAsInt(), key + " carries 32 blocks, as far as the seraph speaks");
            Path f = fileOf(name);
            assertTrue(Files.exists(f), key + " has its file " + f);
            expectedFiles.add(f.toString().replace('\\', '/'));
            Ogg ogg = readOgg(f);
            assertEquals(1, ogg.channels(), f + " mono");
            assertEquals(32000, ogg.sampleRate(), f + " 32 kHz like every spoken line");
            assertTrue(ogg.seconds() >= 0.5 && ogg.seconds() <= 12.0, f + " " + ogg.seconds() + " s");
            long size = Files.size(f);
            assertTrue(size >= 3_000 && size <= 250_000, f + " " + size + " bytes");
        }
        List<String> bad = new ArrayList<>();
        for (String key : doc.keySet()) {
            if (!key.startsWith("spirit_light.voice.")) continue;
            String id = key.substring("spirit_light.voice.".length());
            if (!SpiritLightVoiceLines.has(id)) bad.add("sounds.json entry " + key + " is a line without a file in the catalogue (nothing is faked)");
        }
        Path voiceDir = SL.resolve("voice");
        if (Files.isDirectory(voiceDir))
            try (Stream<Path> files = Files.list(voiceDir)) {
                for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator)
                    if (!expectedFiles.contains(f.toString().replace('\\', '/'))) bad.add("no registered line names the file " + f);
            }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noSfxFileIsAnOrphanAndTheWholeSetStaysSmall() throws IOException {
        JsonObject doc = soundsJson();
        Set<String> named = new HashSet<>();
        for (String id : SpiritLightRules.SOUND_IDS) for (JsonElement s : doc.getAsJsonObject(id).getAsJsonArray("sounds")) named.add(fileOf(nameOf(s)).toString().replace('\\', '/'));
        long total = 0;
        List<String> bad = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SL)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                total += Files.size(f);
                String rel = f.toString().replace('\\', '/');
                if (!rel.contains("/spirit_light/voice/") && !named.contains(rel)) bad.add("no sounds.json entry names the file " + f);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertTrue(total < BUDGET, "the Spirit of Light's sounds take " + total + " bytes, the budget is 3 MB");
    }

    @Test
    void theManifestListsThirtyFiveShortLinesWithOneVoiceAndTheCatalogueIsASubsetOfIt() throws IOException {
        JsonObject doc = JsonParser.parseString(Files.readString(Path.of("tools/audio/spirit_light_lines.json"))).getAsJsonObject();
        JsonArray lines = doc.getAsJsonArray("lines");
        assertTrue(lines.size() >= 25 && lines.size() <= 40, "25 to 40 lines, has " + lines.size());
        String voice = doc.get("voice_id").getAsString();
        assertFalse(voice.isBlank());
        Set<String> ids = new HashSet<>();
        Set<String> cues = new HashSet<>();
        for (JsonElement e : lines) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            assertTrue(ids.add(id), id + " listed twice");
            assertTrue(id.startsWith("spiritlight_" + o.get("cue").getAsString() + "_"), id);
            assertEquals(voice, o.get("voice").getAsString(), "one voice for the whole seraph");
            String text = o.get("text").getAsString();
            assertTrue(text.length() <= 60 && text.length() >= 8, id + ": a short line");
            assertTrue(o.get("prompt").getAsString().endsWith(text), id + ": the prompt carries the text");
            cues.add(o.get("cue").getAsString());
        }
        for (LightMoodRules.Cue c : LightMoodRules.Cue.values()) assertTrue(cues.contains(c.key()), "the cue " + c + " has lines");
        assertEquals(lines.size(), SpiritLightVoiceLines.ALL.size(), "the generated list names every manifest line");
        for (String[] l : SpiritLightVoiceLines.ALL) assertTrue(ids.contains(l[0]), l[0]);
        for (String id : SpiritLightVoiceLines.AVAILABLE) assertTrue(ids.contains(id), id + " is in the manifest");
        long voiceBytes = 0;
        for (String id : SpiritLightVoiceLines.AVAILABLE) voiceBytes += Files.size(SL.resolve("voice").resolve(id + ".ogg"));
        assertTrue(voiceBytes < BUDGET, "the spoken lines stay under 3 MB: " + voiceBytes);
    }

    @Test
    void theOlderSoundEntriesAreKept() throws IOException {
        JsonObject doc = soundsJson();
        for (String old : new String[]{"zagred_word", "zagred_seal", "music.hub", "music.rift", "mob.goblin.ambient", "voice.system_floor_enter"}) assertTrue(doc.has(old), "the older entry " + old + " must stay");
    }
}
