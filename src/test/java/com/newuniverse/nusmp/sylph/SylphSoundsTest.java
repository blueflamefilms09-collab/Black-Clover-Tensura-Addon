package com.newuniverse.nusmp.sylph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Sylph's sound set (docs/magic/sylph.md): the catalogue ({@link SylphSoundSet}, from which {@link SylphSounds} registers one sound event per cue), the {@code sylph.*} entries of
 * {@code assets/nusmp/sounds.json} and the {@code .ogg} files {@code tools/gen_sylph_sounds.py} synthesises must say the same: every cue has an entry with its variants, whose files exist and are
 * short mono Vorbis; no entry or file is an orphan; every subtitle has its line; the set is small; and none of it points at the Tensura spirits' sounds.
 */
class SylphSoundsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path SOUNDS = ASSETS.resolve("sounds");
    private static final Path DIR = SOUNDS.resolve("sylph");

    private record Ogg(int channels, int rate, double seconds) {}

    private static Ogg readOgg(Path file) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = -1;
        for (int i = 0; i < d.length - 7; i++) if (d[i] == 1 && d[i + 1] == 'v' && d[i + 2] == 'o' && d[i + 3] == 'r' && d[i + 4] == 'b' && d[i + 5] == 'i' && d[i + 6] == 's') { at = i; break; }
        assertTrue(at > 0, file + " has no Vorbis identification header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int channels = d[at + 11] & 0xff, rate = b.getInt(at + 12);
        int last = -1;
        for (int i = d.length - 4; i >= 0; i--) if (d[i] == 'O' && d[i + 1] == 'g' && d[i + 2] == 'g' && d[i + 3] == 'S') { last = i; break; }
        return new Ogg(channels, rate, b.getLong(last + 6) / (double) rate);
    }

    private static JsonObject soundsJson() throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject(); }

    private static String nameOf(JsonElement s) { return s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString(); }

    @Test
    void theCatalogueIsThirteenCuesWithRealParameters() {
        assertEquals(13, SylphSoundSet.cues().size(), "the first eight, and the five wordless stand-ins of the moods");
        Set<String> ids = new HashSet<>();
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) {
            assertTrue(ids.add(c.id()), "unique " + c.id());
            assertEquals("sylph." + c.id(), c.event());
            assertTrue(c.variants() >= 2, c.id() + " has variants");
            assertTrue(c.volume() > 0f && c.volume() <= 1.2f);
            assertTrue(c.pitchMin() >= 0.5f && c.pitchMax() <= 2.0f && c.pitchMin() <= c.pitchMax());
            assertTrue(c.cooldown() >= 0);
            assertEquals("subtitles.nusmp.sylph." + c.id(), c.subtitleKey());
            assertEquals("sylph/" + c.id() + "_1", c.file(1));
            assertTrue(SylphSoundSet.has(c.id()));
        }
        assertFalse(SylphSoundSet.has("nope"));
        assertNull(SylphSoundSet.cue("nope"));
        for (String id : new String[]{"link", "unlink", "chime", "flutter", "giggle", "gust", "surge", "gale", "hum", "gasp", "whimper", "cheer", "pout"}) assertTrue(SylphSoundSet.has(id), id);
        SylphSoundSet.Cue chime = SylphSoundSet.cue("chime");
        assertEquals(chime.pitchMin(), SylphSoundSet.pitch(chime, 0.0), 1e-6);
        assertEquals(chime.pitchMax(), SylphSoundSet.pitch(chime, 1.0), 1e-6);
        assertEquals(chime.pitchMin(), SylphSoundSet.pitch(chime, -3.0), 1e-6, "out-of-range numbers are clamped");
        assertTrue(SylphSoundSet.MAX_PER_SECOND >= 1 && SylphSoundSet.MAX_PER_SECOND <= 5, "a few sounds a second per listener");
    }

    @Test
    void everyCueHasItsEntryItsFilesAndTheyAreShortMonoVorbis() throws IOException {
        JsonObject doc = soundsJson();
        long total = 0;
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) {
            assertTrue(doc.has(c.event()), "sounds.json has an entry for " + c.event());
            JsonObject e = doc.getAsJsonObject(c.event());
            assertEquals(c.subtitleKey(), e.get("subtitle").getAsString());
            assertEquals(c.variants(), e.getAsJsonArray("sounds").size(), c.id() + " lists every variant");
            int v = 1;
            for (JsonElement s : e.getAsJsonArray("sounds")) {
                assertEquals("nusmp:" + c.file(v), nameOf(s));
                Path f = SOUNDS.resolve(c.file(v) + ".ogg");
                assertTrue(Files.exists(f), "file " + f);
                Ogg o = readOgg(f);
                assertEquals(1, o.channels(), f + " is mono (a sound that is positioned in the world must be)");
                assertTrue(o.rate() == 44100 || o.rate() == 32000, f + " sample rate " + o.rate());
                assertTrue(o.seconds() >= 0.29 && o.seconds() <= 3.05, f + " lasts " + o.seconds() + " s");
                total += Files.size(f);
                v++;
            }
        }
        assertTrue(total < 700_000, "the whole set is small: " + total + " bytes");
    }

    @Test
    void noEntryAndNoFileIsAnOrphanAndNothingPointsAtTheTensuraSpirits() throws IOException {
        Set<String> events = new HashSet<>();
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) events.add(c.event());
        for (var e : soundsJson().entrySet()) {
            if (!e.getKey().startsWith("sylph.")) continue;
            assertTrue(events.contains(e.getKey()), "an entry no cue names: " + e.getKey());
            for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                assertTrue(nameOf(s).startsWith("nusmp:sylph/"), "her entries use her own files only: " + nameOf(s));
                assertFalse(nameOf(s).startsWith("tensura:") || nameOf(s).contains("minecraft:"), "never the basic spirit's sounds: " + nameOf(s));
            }
        }
        Set<String> named = new HashSet<>();
        for (var e : soundsJson().entrySet()) if (e.getKey().startsWith("sylph.")) for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) named.add(nameOf(s).substring("nusmp:".length()));
        try (Stream<Path> w = Files.list(DIR)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String n = "sylph/" + p.getFileName().toString().replace(".ogg", "");
                assertTrue(named.contains(n), "a file no entry names: " + p);
            }
        }
    }

    @Test
    void theOlderEntriesOfSoundsJsonAreStillThere() throws IOException {
        JsonObject doc = soundsJson();
        assertEquals(200, doc.keySet().stream().filter(k -> k.startsWith("mob.")).count(), "the 200 mob voices are untouched");
        assertEquals(120, doc.keySet().stream().filter(k -> k.startsWith("voice.") && !k.startsWith("voice.sylph_") && !k.startsWith("voice.undine_") && !k.startsWith("voice.gnome_") && !k.startsWith("voice.shishou_")).count(), "the 120 spoken lines are untouched (the own lines of Sylph, the Undine, the Gnome and Shishou are voice.sylph_*, voice.undine_*, voice.gnome_*, voice.shishou_*)");
        assertTrue(doc.size() >= 352 + SylphSoundSet.cues().size(), "nothing was removed");
    }
}
