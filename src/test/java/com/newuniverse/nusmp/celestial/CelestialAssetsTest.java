package com.newuniverse.nusmp.celestial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The files Celestial Spirit Magic needs, checked against the key table: every key has its icon, its item models, its 3D mesh with skin and glow, its constellation sprite and
 * its geo model with the clips the props play; the king has three phase models with the clips the renderer asks for; the weapon class table, the engravings, the Curios tag, the
 * loot modifier, the dimension and the lang fragment all know the keys; and the asset scripts' own table (tools/celestial/celestial_data.py) is the same list as the code's.
 */
class CelestialAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp"), DATA = Path.of("src", "main", "resources", "data");

    private static List<String> itemIds() {
        List<String> ids = new ArrayList<>();
        for (CelestialKey k : CelestialKey.values()) ids.add(k.itemId());
        ids.add("spirit_king_sceptre");
        return ids;
    }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theAssetScriptsHaveTheSameKeysAsTheCode() throws IOException {
        String py = Files.readString(Path.of("tools", "celestial", "celestial_data.py"));
        Matcher m = Pattern.compile("\\(\"(\\w+)\", \"([\\w ]+)\", \"(gold|silver|gate)\", 0x([0-9A-Fa-f]{6})\\)").matcher(py);
        List<String> seen = new ArrayList<>();
        while (m.find()) {
            CelestialKey k = CelestialKey.byId(m.group(1));
            assertNotNull(k, "the script has a key the code does not: " + m.group(1));
            assertEquals(seen.size(), k.ordinal(), "same order: " + k.id);
            assertEquals(k.rank.name().toLowerCase(), m.group(3), k.id);
            assertEquals(k.rgb, Integer.parseInt(m.group(4), 16), "colour of " + k.id);
            seen.add(k.id);
        }
        assertEquals(CelestialKey.values().length, seen.size());
    }

    @Test
    void everyItemHasItsIconModelsMeshAndSkins() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : itemIds()) {
            for (String f : new String[]{"textures/item/" + id + ".png", "models/item/" + id + ".json", "models/item/" + id + "_icon.json", "weapon_meshes/" + id + ".txt",
                    "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png"})
                if (!Files.exists(ASSETS.resolve(f))) bad.add(f);
            Path model = ASSETS.resolve("models/item/" + id + ".json");
            if (Files.exists(model)) {
                JsonObject o = json(model);
                if (!"minecraft:builtin/entity".equals(o.get("parent").getAsString())) bad.add(id + " model is not builtin/entity (WeaponRenderer draws it)");
                if (!o.has("display")) bad.add(id + " has no display transforms");
            }
            Path mesh = ASSETS.resolve("weapon_meshes/" + id + ".txt");
            if (Files.exists(mesh)) {
                List<String> lines = Files.readAllLines(mesh);
                if (lines.stream().noneMatch(l -> l.startsWith("place "))) bad.add(id + " mesh has no place line");
                if (lines.stream().noneMatch(l -> l.trim().equals("orient manual"))) bad.add(id + " mesh is not marked orient manual");
                if (lines.stream().noneMatch(l -> l.startsWith("glow "))) bad.add(id + " mesh has no glow layer");
                long quads = lines.stream().filter(l -> l.startsWith("qn ") || l.startsWith("q ")).count();
                if (quads < 100 || quads > 5000) bad.add(id + " has " + quads + " quads");
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyKeyIsInTheWeaponClassTableTheEngravingsAndTheCuriosSlot() throws IOException {
        String classes = Files.readString(ASSETS.resolve("weapon_classes.json")), classesTools = Files.readString(Path.of("tools", "relics", "weapon_classes.json"));
        String engravings = Files.readString(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java"));
        JsonArray tag = json(DATA.resolve("curios/tags/item/celestial_key.json")).getAsJsonArray("values");
        List<String> inTag = new ArrayList<>();
        for (JsonElement e : tag) inTag.add(e.getAsString());
        List<String> bad = new ArrayList<>();
        for (String id : itemIds()) {
            if (!classes.contains("\"" + id + "\"")) bad.add(id + " class row (assets)");
            if (!classesTools.contains("\"" + id + "\"")) bad.add(id + " class row (tools)");
            if (id.endsWith("_key") && !engravings.contains("\"" + id + "\"")) bad.add(id + " engravings");
            if (id.endsWith("_key") && !inTag.contains("nusmp:" + id)) bad.add(id + " curios tag");
        }
        if (!engravings.contains("\"spirit_king_sceptre\"")) bad.add("sceptre engravings");
        assertEquals(List.of(), bad);
        JsonObject slot = json(DATA.resolve("nusmp/curios/slots/celestial_key.json"));
        assertTrue(slot.get("size").getAsInt() >= 12, "room for the twelve gold keys");
        assertEquals("celestial_key", json(DATA.resolve("nusmp/curios/entities/celestial_key.json")).getAsJsonArray("slots").get(0).getAsString());
    }

    @Test
    void everySpiritHasItsModelClipsAndConstellation() throws IOException {
        List<String> bad = new ArrayList<>();
        for (CelestialKey k : CelestialKey.values()) {
            if (!k.summons()) continue;
            String id = "celestial_" + k.id;
            checkGeo(id, List.of("idle", "summon", "cast", "leave"), bad);
            if (!Files.exists(ASSETS.resolve("textures/particle/celestial_sign_" + k.id + ".png"))) bad.add("sign of " + k.id);
        }
        for (String f : new String[]{"celestial_circle", "celestial_ring", "celestial_glint", "celestial_shaft", "celestial_door", "celestial_sign_king"})
            if (!Files.exists(ASSETS.resolve("textures/particle/" + f + ".png"))) bad.add(f);
        assertEquals(List.of(), bad);
    }

    @Test
    void theKingAndTheGateHaveTheirModelsAndClips() throws IOException {
        List<String> bad = new ArrayList<>();
        for (int p = 1; p <= 3; p++) checkGeo("celestial_king_p" + p, List.of("idle", "raise", "slam", "point", "judge", "death"), bad);
        checkGeo("celestial_gate", List.of("idle", "open", "open_hold", "close"), bad);
        assertEquals(List.of(), bad);
    }

    private static void checkGeo(String id, List<String> clips, List<String> bad) throws IOException {
        Path geo = ASSETS.resolve("geo/entity/" + id + ".geo.json"), anim = ASSETS.resolve("animations/entity/" + id + ".animation.json");
        for (Path p : new Path[]{geo, anim, ASSETS.resolve("textures/entity/" + id + ".png"), ASSETS.resolve("textures/entity/" + id + "_glow.png")})
            if (!Files.exists(p)) bad.add("missing " + p);
        if (!Files.exists(geo) || !Files.exists(anim)) return;
        JsonObject g = json(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        java.util.Set<String> bones = new java.util.HashSet<>();
        int cubes = 0;
        for (JsonElement b : g.getAsJsonArray("bones")) {
            JsonObject o = b.getAsJsonObject();
            bones.add(o.get("name").getAsString());
            cubes += o.getAsJsonArray("cubes").size();
        }
        int tw = g.getAsJsonObject("description").get("texture_width").getAsInt(), th = g.getAsJsonObject("description").get("texture_height").getAsInt();
        if (cubes < 10) bad.add(id + " has only " + cubes + " cubes");
        for (JsonElement b : g.getAsJsonArray("bones")) {
            JsonObject o = b.getAsJsonObject();
            if (o.has("parent") && !bones.contains(o.get("parent").getAsString())) bad.add(id + " bone " + o.get("name").getAsString() + " has a missing parent");
            for (JsonElement c : o.getAsJsonArray("cubes")) {
                JsonArray uv = c.getAsJsonObject().getAsJsonArray("uv"), sz = c.getAsJsonObject().getAsJsonArray("size");
                int u = uv.get(0).getAsInt(), v = uv.get(1).getAsInt(), sx = sz.get(0).getAsInt(), sy = sz.get(1).getAsInt(), sa = sz.get(2).getAsInt();
                if (u + 2 * (sx + sa) > tw || v + sa + sy > th) bad.add(id + " cube outside the sheet in " + o.get("name").getAsString());
            }
        }
        JsonObject animations = json(anim).getAsJsonObject("animations");
        for (String clip : clips) {
            if (!animations.has(clip)) { bad.add(id + " has no clip " + clip); continue; }
            for (String bone : animations.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) if (!bones.contains(bone)) bad.add(id + " clip " + clip + " animates a missing bone " + bone);
        }
        if (animations.has("idle") && !animations.getAsJsonObject("idle").get("loop").getAsBoolean()) bad.add(id + " idle does not loop");
    }

    @Test
    void theDataFilesAreConsistent() throws IOException {
        assertEquals("nusmp:celestial_keys", json(DATA.resolve("nusmp/loot_modifiers/celestial_keys.json")).get("type").getAsString());
        JsonArray entries = json(DATA.resolve("neoforge/loot_modifiers/global_loot_modifiers.json")).getAsJsonArray("entries");
        boolean listed = false;
        for (JsonElement e : entries) listed |= e.getAsString().equals("nusmp:celestial_keys");
        assertTrue(listed, "global_loot_modifiers.json lists the key modifier");
        JsonObject type = json(DATA.resolve("nusmp/dimension_type/spirit_world.json"));
        assertEquals("nusmp:spirit_world", json(DATA.resolve("nusmp/dimension/spirit_world.json")).get("type").getAsString());
        assertTrue(type.get("height").getAsInt() >= 160, "room for the floor at 100 and the roof at 130");
        assertEquals(0, type.get("min_y").getAsInt());
        assertEquals("minecraft:the_end", type.get("effects").getAsString());
    }

    @Test
    void theLangFragmentNamesEveryItem() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "celestial_keys.json"));
        List<String> bad = new ArrayList<>();
        for (String id : itemIds()) if (!lang.has("item.nusmp." + id)) bad.add("item." + id);
        for (String k : new String[]{"entity.nusmp.spirit_king", "curios.identifier.celestial_key"})
            if (!lang.has(k)) bad.add(k);
        assertEquals(List.of(), bad);
    }
}
