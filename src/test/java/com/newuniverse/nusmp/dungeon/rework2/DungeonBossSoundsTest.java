package com.newuniverse.nusmp.dungeon.rework2;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dungeon bosses' sound set (docs/dungeon_bosses_rework2.md, part 2): the catalogue ({@link DungeonBossSoundRules}), the registry's source, the {@code dboss.*} entries of sounds.json, the {@code .ogg}
 * files tools/gen_dungeon_boss_sounds.py writes, the subtitles of the lang fragment and the generator's own table must say the same; every file is a short, small, mono Ogg Vorbis; every registered sound has its
 * file and its entry; no entry or file is an orphan; the per-boss limiter holds its cooldowns and its rate.
 */
class DungeonBossSoundsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path DIR = ASSETS.resolve("sounds/dboss");

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
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/dungeon_bosses2.json"))).getAsJsonObject();
        List<String> bad = new ArrayList<>();
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) {
            JsonObject e = doc.has(c.event()) ? doc.getAsJsonObject(c.event()) : null;
            if (e == null) { bad.add("no sounds.json entry " + c.event()); continue; }
            if (!c.subtitleKey().equals(e.get("subtitle").getAsString())) bad.add(c.event() + " subtitle key");
            if (!lang.has(c.subtitleKey()) || !lang.get(c.subtitleKey()).getAsString().equals(c.subtitle())) bad.add(c.event() + " subtitle line");
            JsonArray list = e.getAsJsonArray("sounds");
            if (list.size() != c.variants() || list.size() < 2) bad.add(c.event() + " variants " + list.size() + " (catalogue " + c.variants() + ")");
            for (JsonElement s : list) {
                String name = nameOf(s);
                Path f = DIR.resolve(name.substring(name.indexOf("dboss/") + 6) + ".ogg");
                if (!name.startsWith("anime_multiverse:dboss/") || !Files.exists(f)) { bad.add("missing " + name); continue; }
                if (Files.size(f) >= 200_000) bad.add(f + " is " + Files.size(f) + " bytes (the limit is 200 KB)");
                Ogg o = read(f);
                if (o.channels() != 1) bad.add(f + " not mono");
                if (o.rate() != 32000 && o.rate() != 44100) bad.add(f + " rate " + o.rate());
                if (o.seconds() < 0.29 || o.seconds() > 2.45) bad.add(f + " " + o.seconds() + " s");
                if (s.isJsonObject() && s.getAsJsonObject().get("stream").getAsBoolean()) bad.add(f + " is streamed (a short cue is loaded whole)");
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyRegisteredSoundHasItsFileAndItsEntry() throws IOException {
        // the registry registers one event per catalogue cue ("dboss." + id); the scan reads the source so it needs no running game
        String reg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/dungeon/rework2/DungeonBossSounds.java"));
        assertTrue(reg.contains("createVariableRangeEvent"), "the events are variable-range events");
        assertTrue(reg.contains("DungeonBossSoundRules.CUES"), "the registry is driven by the catalogue, so a cue cannot be registered without an entry here");
        JsonObject doc = sounds();
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) {
            assertTrue(doc.has(c.event()), c.event() + " is registered but has no sounds.json entry");
            for (int i = 1; i <= c.variants(); i++) assertTrue(Files.exists(DIR.resolve(c.id() + "_" + i + ".ogg")), c.id() + "_" + i + ".ogg is missing");
        }
        assertTrue(Files.readString(ASSETS.resolve("sounds.json")).contains("\"zagred_word\""), "the older entries are still there");
    }

    @Test
    void noDbossEntryOrFileIsAnOrphan() throws IOException {
        Set<String> events = new HashSet<>();
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) events.add(c.event());
        Set<String> named = new HashSet<>();
        List<String> bad = new ArrayList<>();
        JsonObject doc = sounds();
        for (String k : doc.keySet()) {
            if (!k.startsWith("dboss.")) continue;
            if (!events.contains(k)) bad.add("entry " + k + " is not in the catalogue");
            for (JsonElement s : doc.getAsJsonObject(k).getAsJsonArray("sounds")) named.add(nameOf(s).substring(nameOf(s).indexOf("dboss/") + 6));
        }
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) {
                String n = f.getFileName().toString().replace(".ogg", "");
                if (!named.contains(n)) bad.add("no entry names " + f);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theSetIsSmallAndTheCatalogueIsAppendOnly() throws IOException {
        long bytes = 0;
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) files::iterator) bytes += Files.size(f);
        }
        assertTrue(bytes < 1_000_000, "the set is under 1 MB: " + bytes);
        String[] ids = {"rise", "slam", "chain", "roar", "howl", "hiss", "gong", "charge", "blast", "toll", "clang", "spit", "shimmer", "crackle", "step", "growl"};
        assertTrue(DungeonBossSoundRules.CUES.size() >= ids.length);
        for (int i = 0; i < ids.length; i++) assertEquals(ids[i], DungeonBossSoundRules.CUES.get(i).id(), "the first sixteen cues keep their order (append only)");
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) {
            assertTrue(c.event().startsWith("dboss."), "its own namespace of events: " + c.event());
            assertTrue(c.pitchLow() >= 0.5f && c.pitchHigh() <= 2.0f && c.pitchLow() <= c.pitchHigh(), c.event() + " pitch inside the game's range");
            assertTrue(c.cooldown() >= 1 && c.volume() > 0f && c.volume() <= 3f, c.event() + " has a cooldown and a sane volume");
        }
    }

    @Test
    void theGeneratorTableIsTheCatalogue() throws IOException {
        String py = Files.readString(Path.of("tools/gen_dungeon_boss_sounds.py"));
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES)
            assertTrue(py.contains("\"" + c.id() + "\": (" + c.variants() + ", \"" + c.subtitle() + "\""), "tools/gen_dungeon_boss_sounds.py describes " + c.id() + " differently");
    }

    @Test
    void theRangeOfACueIsTheGamesRuleForAVariableRangeEvent() {
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) {
            int want = c.volume() > 1f ? Math.round(16f * c.volume()) : 16;
            assertEquals(want, c.range(), c.event());
            assertTrue(c.range() <= 48, c.event() + " carries a few dozen blocks, not the whole server");
        }
    }

    @Test
    void theLimiterHoldsTheCooldownAndTheRateOfOneBoss() {
        DungeonBossSoundRules.Limiter l = new DungeonBossSoundRules.Limiter();
        assertTrue(l.allow("slam", 1000));
        assertFalse(l.allow("slam", 1003), "the cue's own cooldown (6 ticks for the slam)");
        assertTrue(l.allow("chain", 1004));
        assertTrue(l.allow("clang", 1005));
        assertTrue(l.allow("shimmer", 1006));
        assertFalse(l.allow("crackle", 1007), "four cues a second at most for one boss");
        assertTrue(l.allow("crackle", 1021), "the next second is free again");
        assertTrue(l.allow("slam", 1030), "the cooldown is over");
        assertFalse(l.allow("nosuchcue", 2000), "an unknown cue never plays");
        assertNotNull(DungeonBossSoundRules.cue("toll"));
        assertTrue(DungeonBossSoundRules.cue("toll").cooldown() <= 20, "the two bells of the Warden's toll, 20 ticks apart, both play");
    }
}
