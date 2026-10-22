package com.newuniverse.nusmp.spiritspace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The files of the Spirit of Space: the geo models (their quads are the numbers of {@link SpiritSpaceMesh}), their sheets and animations, the tileable starfield layers, and the sound data: every sound
 * event has its entry in sounds.json, every entry its files (mono Ogg Vorbis, short), every file an entry, every subtitle a line in the lang fragment, the older entries are still there.
 */
class SpiritSpaceAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp"), SOUNDS = ASSETS.resolve("sounds"), LANG = Path.of("tools/lang/spirit_space.json");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws IOException {
        return json(ASSETS.resolve("geo/entity/" + id + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static int quads(String id) throws IOException {
        int n = 0;
        for (JsonElement be : geo(id).getAsJsonArray("bones"))
            for (JsonElement ce : be.getAsJsonObject().getAsJsonArray("cubes")) n += ce.getAsJsonObject().getAsJsonObject("uv").size();
        return n;
    }

    private static Map<String, JsonObject> bones(JsonObject g) {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement b : g.getAsJsonArray("bones")) out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject());
        return out;
    }

    // ------------------------------------------------------------------ the geo files
    @Test
    void theGeneratedModelsHaveExactlyTheQuadCountsOfTheMeshTable() throws IOException {
        for (int lod = 0; lod < 3; lod++) {
            String suffix = lod == 0 ? "" : "_lod" + lod;
            assertEquals(SpiritSpaceMesh.ORB_GEO[lod], quads("spirit_space_orb" + suffix), "orb geo level " + lod);
            assertEquals(SpiritSpaceMesh.WISP_GEO[lod], quads("spirit_space_wisp" + suffix), "wisp geo level " + lod);
        }
        assertEquals(SpiritSpaceMesh.WISP_FX[0], quads("spirit_space_wisp_fx"));
    }

    @Test
    void everyModelIsWellFormedAndHasASheetOfTheSizeItDeclares() throws IOException {
        int orbW = ImageIO.read(ASSETS.resolve("textures/entity/spirit_space_orb.png").toFile()).getWidth();
        int wispW = ImageIO.read(ASSETS.resolve("textures/entity/spirit_space_wisp.png").toFile()).getWidth();
        for (String id : new String[]{"spirit_space_orb", "spirit_space_orb_lod1", "spirit_space_orb_lod2", "spirit_space_wisp", "spirit_space_wisp_lod1", "spirit_space_wisp_lod2", "spirit_space_wisp_fx"}) {
            JsonObject g = geo(id), d = g.getAsJsonObject("description");
            int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
            BufferedImage tex = ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
            assertEquals(tw, tex.getWidth(), id + " declares the size of its sheet");
            assertEquals(th, tex.getHeight(), id);
            int full = id.contains("orb") ? orbW : wispW;
            int want = id.endsWith("_lod1") || id.endsWith("_fx") ? full / 2 : id.endsWith("_lod2") ? full / 4 : full;
            assertEquals(want, tw, id + ": a lower level reads a smaller copy of the sheet");
            Map<String, JsonObject> bones = bones(g);
            for (JsonObject b : bones.values()) {
                if (b.has("parent")) assertTrue(bones.containsKey(b.get("parent").getAsString()), id + " bone " + b.get("name") + " has its parent");
                for (JsonElement ce : b.getAsJsonArray("cubes"))
                    for (var face : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                        JsonObject f = face.getValue().getAsJsonObject();
                        JsonArray uv = f.getAsJsonArray("uv"), sz = f.getAsJsonArray("uv_size");
                        double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble(), u1 = u0 + sz.get(0).getAsDouble(), v1 = v0 + sz.get(1).getAsDouble();
                        assertTrue(Math.min(u0, u1) >= -0.01 && Math.max(u0, u1) <= tw + 0.01 && Math.min(v0, v1) >= -0.01 && Math.max(v0, v1) <= th + 0.01, id + " uv inside the sheet");
                    }
            }
        }
    }

    @Test
    void theAnimationsHaveTheIdleAndCastClipsAndMoveRealBonesInsideTheirLength() throws IOException {
        for (String form : new String[]{"orb", "wisp"}) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/spirit_space_" + form + ".animation.json")).getAsJsonObject("animations");
            Map<String, JsonObject> bones = bones(geo("spirit_space_" + form));
            for (String clip : new String[]{"idle", "cast"}) {
                assertTrue(anim.has(clip), form + " clip " + clip);
                JsonObject c = anim.getAsJsonObject(clip);
                double len = c.get("animation_length").getAsDouble();
                assertTrue(len > 0);
                for (var be : c.getAsJsonObject("bones").entrySet()) {
                    assertTrue(bones.containsKey(be.getKey()), form + "/" + clip + " bone " + be.getKey());
                    for (var track : be.getValue().getAsJsonObject().entrySet())
                        for (var key : track.getValue().getAsJsonObject().entrySet()) assertTrue(Double.parseDouble(key.getKey()) <= len + 1e-6, form + "/" + clip + " keyframe " + key.getKey());
                }
            }
            assertTrue(anim.getAsJsonObject("idle").get("loop").getAsBoolean());
            assertFalse(anim.getAsJsonObject("cast").get("loop").getAsBoolean());
        }
    }

    @Test
    void theLodModelsUseBonesOfTheFullModelOnlySoOneClipFileDrivesThem() throws IOException {
        for (String form : new String[]{"orb", "wisp"}) {
            Map<String, JsonObject> full = bones(geo("spirit_space_" + form));
            for (String suffix : new String[]{"_lod1", "_lod2"})
                for (String name : bones(geo("spirit_space_" + form + suffix)).keySet()) assertTrue(full.containsKey(name), form + suffix + " bone " + name + " exists in the full model");
        }
        for (String name : bones(geo("spirit_space_wisp_fx")).keySet()) assertTrue(bones(geo("spirit_space_wisp")).containsKey(name), "fx bone " + name);
    }

    // ------------------------------------------------------------------ the layer textures
    private static BufferedImage tex(String name) throws IOException { return ImageIO.read(ASSETS.resolve("textures/entity/spirit_space/" + name + ".png").toFile()); }

    @Test
    void theStarfieldLayersAreTileableSquaresThatCarryNoColourUnderTransparency() throws IOException {
        for (String n : new String[]{"stars_far", "stars_mid", "stars_near", "fin", "portal_ring", "portal_disc", "halo"}) {
            BufferedImage im = tex(n);
            int bad = 0;
            for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) { int v = im.getRGB(x, y); if ((v >>> 24) == 0 && (v & 0xFFFFFF) != 0) bad++; }
            assertEquals(0, bad, n + " has colour under alpha 0 (run tools/safe_additive/scan_textures.py --fix)");
        }
        for (String n : new String[]{"stars_far", "stars_mid", "stars_near"}) {
            BufferedImage im = tex(n);
            assertEquals(128, im.getWidth(), n);
            assertEquals(128, im.getHeight(), n);
            assertTrue(im.getWidth() == im.getHeight(), n + " is square");
        }
        for (String n : new String[]{"core", "body"}) {
            BufferedImage im = tex(n);
            for (int y = 0; y < im.getHeight(); y += 7) for (int x = 0; x < im.getWidth(); x += 7) assertEquals(255, im.getRGB(x, y) >>> 24, n + " is opaque");
        }
    }

    @Test
    void theStarfieldsTileByConstructionTheirPaintersWrapEveryStampAndEveryNoiseLattice() throws IOException {
        String art = Files.readString(Path.of("tools/gen_spirit_space_art.py"));
        assertTrue(art.contains("self.px[y % self.h, x % self.w") && art.contains("(y0 + 1) % n") && art.contains("(x0 + 1) % n"), "stamps and noise wrap, so a UV scroll never shows a seam");
    }

    @Test
    void theOrbAndWispSheetsCarryNoColourUnderTransparency() throws IOException {
        for (String n : new String[]{"spirit_space_orb", "spirit_space_orb_lod1", "spirit_space_orb_lod2", "spirit_space_wisp_fx"}) {
            BufferedImage im = ImageIO.read(ASSETS.resolve("textures/entity/" + n + ".png").toFile());
            for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) { int v = im.getRGB(x, y); assertFalse((v >>> 24) == 0 && (v & 0xFFFFFF) != 0, n + " colour under alpha 0 at " + x + "," + y); }
        }
    }

    // ------------------------------------------------------------------ the sound data
    record Ogg(int channels, int sampleRate, double seconds) {}

    static Ogg readOgg(Path file) throws IOException {
        byte[] d = Files.readAllBytes(file);
        assertTrue(d.length > 64 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S', file + " is not an Ogg file");
        int at = indexOf(d, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'});
        assertTrue(at > 0, file + " has no Vorbis identification header");
        ByteBuffer b = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
        int rate = b.getInt(at + 12), channels = d[at + 11] & 0xff;
        int last = d.length - 4;
        while (last >= 0 && !(d[last] == 'O' && d[last + 1] == 'g' && d[last + 2] == 'g' && d[last + 3] == 'S')) last--;
        return new Ogg(channels, rate, b.getLong(last + 6) / (double) rate);
    }

    private static int indexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = 0; i <= d.length - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    private static Set<String> expectedEvents() {
        Set<String> ids = new HashSet<>();
        for (SoundGate.Kind k : SoundGate.Kind.values()) if (k != SoundGate.Kind.VOICE) ids.add(k.eventId());
        for (SpaceMoodRules.Voice v : SpaceMoodRules.Voice.values()) if (v != SpaceMoodRules.Voice.NONE) ids.add(v.eventId());
        return ids;
    }

    @Test
    void everyEventHasAnEntryWithVariantsAndEveryFileIsAShortMonoOgg() throws IOException {
        JsonObject doc = json(ASSETS.resolve("sounds.json"));
        List<String> bad = new ArrayList<>();
        int voiceVariants = 0;
        for (String id : expectedEvents()) {
            JsonElement entry = doc.get(id);
            if (entry == null) { bad.add("no sounds.json entry " + id); continue; }
            JsonObject e = entry.getAsJsonObject();
            if (!e.has("subtitle")) bad.add(id + ": no subtitle");
            JsonArray sounds = e.getAsJsonArray("sounds");
            if (sounds.size() < 2) bad.add(id + ": needs at least two variants");
            if (id.contains(".voice_")) voiceVariants += sounds.size();
            for (JsonElement s : sounds) {
                String name = s.getAsJsonObject().get("name").getAsString();
                if (!name.startsWith("nusmp:spirit_space/")) bad.add(id + ": odd sound name " + name);
                Path f = SOUNDS.resolve(name.substring(name.indexOf(':') + 1) + ".ogg");
                if (!Files.exists(f)) { bad.add(id + ": missing file " + f); continue; }
                Ogg ogg = readOgg(f);
                if (ogg.channels() != 1) bad.add(f + ": " + ogg.channels() + " channels");
                if (ogg.sampleRate() != 44100) bad.add(f + ": sample rate " + ogg.sampleRate());
                if (ogg.seconds() < 0.3 || ogg.seconds() > 2.4) bad.add(f + ": " + ogg.seconds() + " s");
                if (!s.getAsJsonObject().has("attenuation_distance")) bad.add(f + ": no attenuation distance");
                else if (id.contains(".voice_") && s.getAsJsonObject().get("attenuation_distance").getAsInt() > 32) bad.add(f + ": a voice cue must not carry past 32 blocks");
            }
        }
        assertTrue(voiceVariants >= 10 && voiceVariants <= 20, "the owner asked for 10 to 20 voice variants, there are " + voiceVariants);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noSpiritSpaceEntryAndNoFileIsAnOrphanAndTheOlderEntriesAreKept() throws IOException {
        JsonObject doc = json(ASSETS.resolve("sounds.json"));
        Set<String> events = expectedEvents(), named = new HashSet<>();
        List<String> bad = new ArrayList<>();
        for (String key : doc.keySet()) {
            if (!key.startsWith("spirit_space.")) continue;
            if (!events.contains(key)) bad.add("entry " + key + " has no sound event");
            for (JsonElement s : doc.getAsJsonObject(key).getAsJsonArray("sounds")) named.add(SOUNDS.resolve(s.getAsJsonObject().get("name").getAsString().substring("nusmp:".length()) + ".ogg").toString().replace('\\', '/'));
        }
        try (Stream<Path> files = Files.walk(SOUNDS.resolve("spirit_space"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) if (!named.contains(f.toString().replace('\\', '/'))) bad.add("no entry names the file " + f);
        }
        assertTrue(bad.isEmpty(), bad.toString());
        for (String old : new String[]{"zagred_word", "zagred_seal", "zagred_phase", "mob.goblin.ambient"}) assertTrue(doc.has(old), "the older entry " + old + " must stay");
        long total = 0;
        try (Stream<Path> files = Files.walk(SOUNDS.resolve("spirit_space"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".ogg"))::iterator) total += Files.size(f);
        }
        assertTrue(total < 2L * 1024 * 1024, "the spirit's sounds take " + total + " bytes, the budget is 2 MB");
    }

    @Test
    void everySubtitleHasItsLineAndTheLangFragmentIsFlatJson() throws IOException {
        JsonObject doc = json(ASSETS.resolve("sounds.json")), lang = json(LANG);
        for (var e : lang.entrySet()) assertTrue(e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString() && !e.getValue().getAsString().isBlank(), "flat string lines only: " + e.getKey());
        for (String id : expectedEvents()) {
            String key = doc.getAsJsonObject(id).get("subtitle").getAsString();
            assertEquals("subtitles.nusmp." + id, key, id + " subtitle key");
            assertTrue(lang.has(key), "no lang line for " + key);
        }
        for (String key : new String[]{"key.nusmp.spirit_space_link", "key.nusmp.spirit_space_cycle", "key.nusmp.spirit_space_use", "nusmp.spirit_space.linked", "nusmp.spirit_space.dimension"}) assertTrue(lang.has(key), key);
    }
}
