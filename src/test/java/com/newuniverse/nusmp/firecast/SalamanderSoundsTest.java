package com.newuniverse.nusmp.firecast;

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
 * The Salamander's creature sounds (docs/magic/fire_casting.md, 4.2): the catalogue ({@link SalamanderSoundSet}, from which {@link SalamanderSounds} registers one sound event per cue), the
 * {@code salamander.*} entries of {@code assets/nusmp/sounds.json}, the {@code .ogg} files {@code tools/gen_salamander_sounds.py} synthesises and the subtitle lines of
 * {@code tools/lang/salamander_moods.json} must say the same: every cue has an entry with its variants, whose files exist and are short mono Vorbis; no entry or file is an orphan; every
 * subtitle has its line; the set is 15 to 25 files and under 2 MB; nothing points at the Tensura spirits' sounds; the older entries are still there.
 */
class SalamanderSoundsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path SOUNDS = ASSETS.resolve("sounds");
    private static final Path DIR = SOUNDS.resolve("salamander");
    private static final Path LANG = Path.of("tools/lang/salamander_moods.json");

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
    void theCatalogueIsElevenCuesOfTwentyFiveVariantsWithRealParameters() {
        assertEquals(11, SalamanderSoundSet.cues().size());
        assertEquals(25, SalamanderSoundSet.totalVariants());
        assertTrue(SalamanderSoundSet.totalVariants() >= 15 && SalamanderSoundSet.totalVariants() <= 25, "15 to 25 variants");
        Set<String> ids = new HashSet<>();
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) {
            assertTrue(ids.add(c.id()), "unique " + c.id());
            assertEquals("salamander." + c.id(), c.event());
            assertTrue(c.variants() >= 2, c.id() + " has variants");
            assertTrue(c.volume() > 0f && c.volume() <= 1.2f);
            assertTrue(c.pitchMin() >= 0.5f && c.pitchMax() <= 2.0f && c.pitchMin() <= c.pitchMax());
            assertTrue(c.cooldown() >= 0);
            assertEquals("subtitles.nusmp.salamander." + c.id(), c.subtitleKey());
            assertEquals("salamander/" + c.id() + "_1", c.file(1));
        }
        for (String id : new String[]{"trill", "chirp", "growl", "hiss", "whoosh", "purr", "yawn", "whimper", "flap", "roar", "dragon_flap"}) assertTrue(SalamanderSoundSet.has(id), id);
        assertFalse(SalamanderSoundSet.has("nope"));
        assertNull(SalamanderSoundSet.cue(null));
        SalamanderSoundSet.Cue trill = SalamanderSoundSet.cue("trill");
        assertEquals(trill.pitchMin(), SalamanderSoundSet.pitch(trill, 0.0), 1e-6);
        assertEquals(trill.pitchMax(), SalamanderSoundSet.pitch(trill, 1.0), 1e-6);
        assertEquals(trill.pitchMin(), SalamanderSoundSet.pitch(trill, -3.0), 1e-6, "out-of-range numbers are clamped");
        assertEquals(1, SalamanderSoundSet.variant(trill, 0.0));
        assertEquals(trill.variants(), SalamanderSoundSet.variant(trill, 0.9999));
        assertEquals(trill.variants(), SalamanderSoundSet.variant(trill, 5.0));
        assertEquals(32.0, SalamanderSoundSet.RADIUS, "heard within 32 blocks only");
        assertEquals(60, SalamanderSoundSet.LISTENER_GAP, "one sound per three seconds for a listener");
        assertTrue(SalamanderSoundSet.cue("roar").volume() > SalamanderSoundSet.cue("trill").volume(), "the big form is louder than the small one");
    }

    @Test
    void everyCueHasItsEntryItsFilesAndTheyAreShortMonoVorbisAndTheWholeSetIsSmall() throws IOException {
        JsonObject doc = soundsJson();
        long total = 0;
        int files = 0;
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) {
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
                boolean streamed = s.isJsonObject() && s.getAsJsonObject().has("stream") && s.getAsJsonObject().get("stream").getAsBoolean();
                assertEquals(o.seconds() >= 2.4, streamed, f + ": a file of 2.4 s or more is streamed, a shorter one is not");
                total += Files.size(f);
                files++;
                v++;
            }
        }
        assertEquals(SalamanderSoundSet.totalVariants(), files);
        assertTrue(total < 2_000_000, "mono ogg, under 2 MB in all: " + total + " bytes");
    }

    @Test
    void noEntryAndNoFileIsAnOrphanAndNothingPointsAtTheTensuraSpirits() throws IOException {
        Set<String> events = new HashSet<>();
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) events.add(c.event());
        Set<String> named = new HashSet<>();
        for (var e : soundsJson().entrySet()) {
            if (!e.getKey().startsWith("salamander.")) continue;
            assertTrue(events.contains(e.getKey()), "an entry no cue names: " + e.getKey());
            for (JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                assertTrue(nameOf(s).startsWith("nusmp:salamander/"), "its entries use its own files only: " + nameOf(s));
                assertFalse(nameOf(s).startsWith("tensura:") || nameOf(s).contains("minecraft:"), "never the basic spirit's sounds: " + nameOf(s));
                named.add(nameOf(s).substring("nusmp:".length()));
            }
        }
        try (Stream<Path> w = Files.list(DIR)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String n = "salamander/" + p.getFileName().toString().replace(".ogg", "");
                assertTrue(named.contains(n), "a file no entry names: " + p);
            }
        }
    }

    @Test
    void everySubtitleHasItsLineAndNoLineIsAnOrphan() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        Set<String> keys = new HashSet<>();
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) {
            assertTrue(lang.has(c.subtitleKey()), "a subtitle line for " + c.id());
            assertFalse(lang.get(c.subtitleKey()).getAsString().isBlank());
            keys.add(c.subtitleKey());
        }
        for (var e : lang.entrySet()) if (e.getKey().startsWith("subtitles.nusmp.salamander.")) assertTrue(keys.contains(e.getKey()), "a subtitle no cue names: " + e.getKey());
    }

    @Test
    void theCreatureSoundsHaveNoWordsAndTheOlderEntriesOfSoundsJsonAreStillThere() throws IOException {
        JsonObject doc = soundsJson();
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) assertFalse(c.what().isBlank());
        assertEquals(200, doc.keySet().stream().filter(k -> k.startsWith("mob.")).count(), "the 200 mob voices are untouched");
        assertEquals(120, doc.keySet().stream().filter(k -> k.startsWith("voice.") && !k.startsWith("voice.sylph_") && !k.startsWith("voice.undine_") && !k.startsWith("voice.gnome_") && !k.startsWith("voice.shishou_")).count(), "the 120 spoken lines are untouched (the own lines of Sylph, the Undine, the Gnome and Shishou are appended)");
        assertTrue(doc.keySet().stream().filter(k -> k.startsWith("sylph.")).count() >= 8, "the Sylph's sounds are untouched");
        // our entries are appended at the end of the file, after every older one
        String text = Files.readString(ASSETS.resolve("sounds.json"));
        int firstOurs = text.indexOf("\n  \"salamander.");
        assertTrue(firstOurs > 0);
        assertTrue(text.lastIndexOf("\n  \"voice.djinn_murmur_attack") < firstOurs, "appended after the older entries (the 120 voice lines)");
    }

    @Test
    void theSoundEventsAreRegisteredFromTheCatalogueAndPlayedToNearbyPlayersOnlyThroughTheGate() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/firecast/SalamanderSounds.java")).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
        assertTrue(src.contains("for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues())") && src.contains("SOUNDS.register(c.event()"), "registered in a loop from the catalogue");
        assertTrue(src.contains("SalamanderSoundSet.RADIUS") && src.contains("continue;") && src.contains("GATE.tryAcquire(p.getUUID(), c.id(), now)"), "players further than 32 blocks are skipped, the rest go through the gate");
        assertTrue(src.contains("p.connection.send("), "a sound packet to the allowed listener only (no level-wide playSound)");
        assertFalse(src.contains("playSound("), "never the world-wide playSound");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/firecast/FireCastBoot.java")).contains("SalamanderSounds.SOUNDS.register(modBus)"), "the register is on the mod bus");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/firecast/FireCastEvents.java")).contains("SalamanderSounds.forget(p.getUUID())"), "a listener's gate record goes on logout");
    }
}
