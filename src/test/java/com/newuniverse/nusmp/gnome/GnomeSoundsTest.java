package com.newuniverse.nusmp.gnome;

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
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Gnome's own sound set: the catalogue ({@link GnomeSoundRules}), the {@code gnome.*} entries of sounds.json, the {@code .ogg} files that tools/gen_gnome_sounds.py writes, the subtitles and the
 * generator's own table must say the same; every file is a short mono Ogg Vorbis; the sound gate holds its cooldowns and rates; no entry or file of the set is an orphan.
 */
class GnomeSoundsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DIR = ASSETS.resolve("sounds/gnome");

    private static JsonObject sounds() throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject(); }

    private static String nameOf(JsonElement s) { return s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString(); }

    private record Ogg(int channels, int rate, double seconds) {}

    private static Ogg read(Path f) throws IOException {
        byte[] d = Files.readAllBytes(f);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', f + " is not an Ogg file");
        int at = -1;
        byte[] pat = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        outer:
        for (int i = 0; i <= d.length - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            at = i;
            break;
        }
        assertTrue(at > 0, f + " has no Vorbis header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int rate = b.getInt(at + 12);
        int last = -1;
        for (int i = d.length - 4; i >= 0; i--) if (d[i] == 'O' && d[i + 1] == 'g' && d[i + 2] == 'g' && d[i + 3] == 'S') { last = i; break; }
        return new Ogg(d[at + 11] & 0xff, rate, b.getLong(last + 6) / (double) rate);
    }

    @Test
    void everyCueHasItsEntryItsVariantsItsFilesAndItsSubtitle() throws IOException {
        JsonObject doc = sounds();
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/gnome.json"))).getAsJsonObject();
        List<String> bad = new ArrayList<>();
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) {
            JsonObject e = doc.has(c.event()) ? doc.getAsJsonObject(c.event()) : null;
            if (e == null) { bad.add("no sounds.json entry " + c.event()); continue; }
            if (!c.subtitleKey().equals(e.get("subtitle").getAsString())) bad.add(c.event() + " subtitle key");
            if (!lang.has(c.subtitleKey()) || !lang.get(c.subtitleKey()).getAsString().equals(c.subtitle())) bad.add(c.event() + " subtitle line");
            JsonArray list = e.getAsJsonArray("sounds");
            if (list.size() != c.variants() || list.size() < 2) bad.add(c.event() + " variants " + list.size() + " (catalogue " + c.variants() + ")");
            for (JsonElement s : list) {
                String name = nameOf(s);
                Path f = DIR.resolve(name.substring(name.indexOf("gnome/") + 6) + ".ogg");
                if (!name.startsWith("nusmp:gnome/") || !Files.exists(f)) { bad.add("missing " + name); continue; }
                Ogg o = read(f);
                if (o.channels() != 1) bad.add(f + " not mono");
                if (o.rate() != 32000 && o.rate() != 44100) bad.add(f + " rate " + o.rate());
                if (o.seconds() < 0.29 || o.seconds() > 3.05) bad.add(f + " " + o.seconds() + " s");
                boolean streamed = s.isJsonObject() && s.getAsJsonObject().get("stream").getAsBoolean();
                if ((o.seconds() >= 2.4) != streamed) bad.add(f + " stream flag vs length " + o.seconds());
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noGnomeEntryOrFileIsAnOrphan() throws IOException {
        Set<String> events = new HashSet<>();
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) events.add(c.event());
        Set<String> named = new HashSet<>();
        List<String> bad = new ArrayList<>();
        JsonObject doc = sounds();
        for (String k : doc.keySet()) {
            if (!k.startsWith("gnome.")) continue;
            if (!events.contains(k)) bad.add("entry " + k + " is not in the catalogue");
            for (JsonElement s : doc.getAsJsonObject(k).getAsJsonArray("sounds")) named.add(nameOf(s).substring(nameOf(s).indexOf("gnome/") + 6));
        }
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String n = f.getFileName().toString().replace(".ogg", "");
                if (!named.contains(n)) bad.add("no entry names " + f);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertTrue(Files.readString(ASSETS.resolve("sounds.json")).contains("\"zagred_word\""), "the older entries are still there");
    }

    @Test
    void theSetIsSmallAndNotTheBasicSpiritSounds() throws IOException {
        long bytes = 0;
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) files::iterator) bytes += Files.size(f);
        }
        assertTrue(bytes < 1_000_000, "the Gnome's sounds are under 1 MB: " + bytes);
        assertEquals(11, GnomeSoundRules.CUES.size());
        for (String cue : new String[]{"link", "unlink", "idle", "cast", "rise", "slam", "sink", "grunt", "chuckle", "snore", "huff"}) assertNotNull(GnomeSoundRules.cue(cue), cue);
        String[] first = {"link", "unlink", "idle", "cast", "rise", "slam", "sink"};
        for (int i = 0; i < first.length; i++) assertEquals(first[i], GnomeSoundRules.CUES.get(i).id(), "the first seven cues keep their order (append only)");
        for (String voice : new String[]{"grunt", "chuckle", "snore", "huff"}) {
            GnomeSoundRules.Cue c = GnomeSoundRules.cue(voice);
            assertTrue(c.volume() <= 0.8f && c.range() == 16, voice + " is a quiet sound of the golem's voice, heard within 16 blocks");
        }
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) {
            assertTrue(c.event().startsWith("gnome."), "its own namespace of events, not spirit.* or the Tensura ones: " + c.event());
            assertTrue(c.pitchLow() >= 0.5f && c.pitchHigh() <= 2.0f && c.pitchLow() <= c.pitchHigh(), c.event() + " pitch inside the game's range");
            assertTrue(c.pitchHigh() <= 1.12f, c.event() + ": a deep set, never pitched up");
        }
    }

    @Test
    void theGeneratorTableIsTheCatalogue() throws IOException {
        String py = Files.readString(Path.of("tools/gen_gnome_sounds.py"));
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES)
            assertTrue(py.contains("\"" + c.id() + "\": (" + c.variants() + ", \"" + c.subtitle() + "\""), "tools/gen_gnome_sounds.py describes " + c.id() + " differently");
    }

    @Test
    void theRangeOfACueIsTheGamesRuleForAVariableRangeEvent() {
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) {
            int want = c.volume() > 1f ? Math.round(16f * c.volume()) : 16;
            assertEquals(want, c.range(), c.event());
        }
        assertTrue(GnomeSoundRules.cue("slam").range() >= 32 && GnomeSoundRules.cue("slam").range() <= 48, "the slam carries a few dozen blocks, not the whole server");
        assertTrue(GnomeSoundRules.cue("idle").range() == 16 && GnomeSoundRules.cue("idle").volume() < 0.7f, "the idle rumble is quiet");
    }

    @Test
    void theGateHoldsTheCooldownTheOwnersRateAndTheServersRate() {
        GnomeSoundRules.Gate g = new GnomeSoundRules.Gate();
        UUID a = UUID.randomUUID();
        assertTrue(g.allow(a, "rise", 1000));
        assertFalse(g.allow(a, "rise", 1010), "the cue's own cooldown (40 ticks for the rise)");
        assertFalse(g.allow(a, "slam", 1002), "never two sounds of one owner on top of each other (4 ticks)");
        assertTrue(g.allow(a, "slam", 1005));
        assertTrue(g.allow(a, "cast", 1010));
        assertFalse(g.allow(a, "link", 1015), "three a second at most for one owner");
        assertTrue(g.allow(a, "link", 1025), "the next second is free again");
        assertTrue(g.allow(a, "rise", 1041));
        assertFalse(g.allow(a, "nosuchcue", 2000), "an unknown cue never plays");
        assertFalse(g.allow(null, "cast", 2000));
        GnomeSoundRules.Gate s = new GnomeSoundRules.Gate();
        int ok = 0;
        for (int i = 0; i < 40; i++) if (s.allow(UUID.randomUUID(), "slam", 5000 + i % 10)) ok++;
        assertEquals(GnomeSoundRules.SERVER_PER_SECOND, ok, "the server-wide rate: 12 a second however many mages there are");
        assertTrue(s.tracked() >= 1, "owners are tracked");
        s.clear();
        assertEquals(0, s.tracked());
        g.forget(a);
        assertEquals(0, g.tracked(), "a logout drops the owner");
    }
}
