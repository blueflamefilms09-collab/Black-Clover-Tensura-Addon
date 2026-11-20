package com.newuniverse.nusmp.arc.kakashi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The voice set, the boss bar style and the Blender body of the Kakashi boss agree with each other and with the code that reads them. */
class KakashiAssetsTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final String[] REQUIRED_GROUPS = {"intro", "phase_pack", "phase_mangekyo", "phase_susanoo", "hurt", "low", "defeat", "victory", "kill", "idle"};

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theLineFileCoversTheWholeFightAndEveryMoveCallOut() throws IOException {
        JsonObject groups = json(RES.resolve("data/anime_multiverse/kakashi/lines.json")).getAsJsonObject("groups");
        for (String g : REQUIRED_GROUPS) assertTrue(groups.has(g) && groups.getAsJsonArray(g).size() > 0, "group " + g);
        for (String m : KakashiMath.BOOK) assertTrue(groups.has(KakashiMoves.voiceGroup(m)), "the call-out group of " + m);
        Set<String> ids = new HashSet<>();
        for (var e : groups.entrySet()) {
            for (JsonElement l : e.getValue().getAsJsonArray()) {
                JsonObject o = l.getAsJsonObject();
                assertTrue(ids.add(o.get("id").getAsString()), "duplicate id " + o.get("id"));
                String text = o.get("text").getAsString();
                assertFalse(text.isBlank());
                assertTrue(text.length() <= 160, "short phrases only: " + o.get("id"));
            }
        }
        assertTrue(ids.size() >= 50, "a real set, not a token one: " + ids.size());
    }

    @Test
    void theManifestMatchesTheShippedLinesAndNamesASourceForEach() throws IOException {
        JsonObject shipped = json(RES.resolve("data/anime_multiverse/kakashi/lines.json")).getAsJsonObject("groups");
        Set<String> shippedIds = new HashSet<>();
        shipped.entrySet().forEach(e -> e.getValue().getAsJsonArray().forEach(l -> shippedIds.add(l.getAsJsonObject().get("id").getAsString())));
        JsonArray manifest = json(Path.of("tools/audio/kakashi_lines.json")).getAsJsonArray("lines");
        Set<String> manifestIds = new HashSet<>();
        for (JsonElement l : manifest) {
            JsonObject o = l.getAsJsonObject();
            manifestIds.add(o.get("id").getAsString());
            assertTrue(o.has("source") && !o.get("source").getAsString().isBlank(), "source of " + o.get("id"));
            assertTrue(o.has("file"), "file path of " + o.get("id"));
        }
        assertEquals(shippedIds, manifestIds);
    }

    @Test
    void everyLineHasASubtitleAndTheRecordedListOnlyNamesRealIds() throws IOException {
        String lang = Files.readString(RES.resolve("assets/anime_multiverse/lang/en_us.json"), StandardCharsets.UTF_8);
        JsonObject groups = json(RES.resolve("data/anime_multiverse/kakashi/lines.json")).getAsJsonObject("groups");
        Set<String> ids = new HashSet<>();
        groups.entrySet().forEach(e -> e.getValue().getAsJsonArray().forEach(l -> ids.add(l.getAsJsonObject().get("id").getAsString())));
        for (String id : ids) assertTrue(lang.contains("\"" + "subtitles.anime_multiverse.kakashi." + id + "\""), "subtitle of " + id);
        assertTrue(lang.contains("\"entity.anime_multiverse.kakashi_hatake\": \"Kakashi Hatake\""));
        JsonElement rec = JsonParser.parseString(Files.readString(RES.resolve("data/anime_multiverse/kakashi/recorded.json"), StandardCharsets.UTF_8));
        for (JsonElement r : rec.isJsonArray() ? rec.getAsJsonArray() : rec.getAsJsonObject().getAsJsonArray("recorded")) assertTrue(ids.contains(r.getAsString()), "recorded id " + r);
        assertEquals("kakashi.voice.kakashi_intro_1", KakashiLines.eventPath("kakashi_intro_1"));
        assertEquals("subtitles.anime_multiverse.kakashi.kakashi_intro_1", KakashiLines.subtitleKey("kakashi_intro_1"));
    }

    @Test
    void theNarutoBarStyleIsRegisteredAndItsArtExists() throws IOException {
        JsonObject s = json(RES.resolve("assets/anime_multiverse/boss_bars/naruto.json"));
        assertEquals("naruto", s.get("id").getAsString());
        assertEquals("sharingan", s.get("emblem").getAsString());
        assertTrue(Files.readString(RES.resolve("assets/anime_multiverse/boss_bars/_index.json")).contains("naruto"));
        assertTrue(Files.size(RES.resolve("assets/anime_multiverse/textures/gui/boss_bar/frame_naruto.png")) > 100);
    }

    @Test
    void theBlenderBodyAndItsTexturesShip() throws IOException {
        for (String f : List.of("entity_meshes/kakashi.txt", "entity_meshes/kakashi_lod.txt", "textures/entity/boss_mesh/kakashi.png", "textures/entity/boss_mesh/kakashi_glow.png")) {
            assertTrue(Files.size(RES.resolve("assets/anime_multiverse").resolve(f)) > 100, f);
        }
        long full = Files.readAllLines(RES.resolve("assets/anime_multiverse/entity_meshes/kakashi.txt")).size();
        long lod = Files.readAllLines(RES.resolve("assets/anime_multiverse/entity_meshes/kakashi_lod.txt")).size();
        assertTrue(lod < full, "the distant body is the lighter one");
    }

    @Test
    void theThirteenParticleThemesAreGenerated() throws IOException {
        String themes = Files.readString(RES.resolve("data/anime_multiverse/particle_themes.json"), StandardCharsets.UTF_8);
        for (String t : new String[]{"kunai", "copy", "raikiri", "fang", "purple", "water", "ninken", "clone", "mist", "earth", "kamui", "susanoo", "log"}) {
            assertTrue(themes.contains("boss.kakashi." + t), "theme boss.kakashi." + t);
        }
    }
}
