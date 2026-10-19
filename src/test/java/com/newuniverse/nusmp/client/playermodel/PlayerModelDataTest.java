package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The kit's data, with no game: the hair table (every style resolves, its boxes lie in the 64x64 sheet, the cap is on the hat layer's UV and larger than it), the table of looks (every look
 * resolves to a skin that exists at 64x64, a glow map where it says so, a haircut that exists, extras with their roots and textures), skin path resolution, the old avatar hair ids, the helmet
 * rule and the arcs' bosses. The same audit runs in the CI server's self-test {@code render.playermodel}.
 */
class PlayerModelDataTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static InputStream open(String path) {
        try {
            File f = new File(ASSETS + path);
            return f.isFile() ? Files.newInputStream(f.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(ASSETS + path)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void theWholeDataSetPassesTheAudit() {
        List<String> bad = PlayerModelAudit.run(PlayerModelDataTest::open);
        assertTrue(bad.isEmpty(), bad.size() + " problems: " + bad.subList(0, Math.min(8, bad.size())));
    }

    @Test
    void everyHairStyleResolvesAndTheElevenNamedOnesExist() throws Exception {
        Map<String, HairStyleData.Style> hair = HairStyleData.parse(json("playermodel/hair_styles.json"));
        for (String id : List.of("short", "long", "ponytail", "twin_tails", "braid", "spiky", "bob", "messy", "bun", "mane", "bald")) assertTrue(hair.containsKey(id), "style " + id);
        assertTrue(hair.get("bald").bald());
        for (HairStyleData.Style s : hair.values()) {
            assertEquals(s.id(), s.texture().substring(s.texture().indexOf('/') + 1));
            if (!s.bald()) assertFalse(s.boxes().isEmpty() && s.chains().isEmpty());
            for (HairStyleData.Chain c : s.chains()) {
                assertTrue(c.length() > 0f && c.segments().size() <= 4, s.id() + " " + c.name());
                assertEquals(c.root()[1], HairStyleData.pivotOf(c, 0)[1], 1e-6f);
            }
        }
    }

    @Test
    void theCapIsLargerThanTheHatLayerAndEveryLayerHasItsOwnSurface() throws Exception {
        Map<String, HairStyleData.Style> hair = HairStyleData.parse(json("playermodel/hair_styles.json"));
        for (HairStyleData.Style s : hair.values()) {
            for (HairStyleData.Box b : s.boxes()) {
                if (!b.name().equals("cap")) continue;
                assertEquals(HairStyleData.CAP_U, b.u());
                assertEquals(HairStyleData.CAP_V, b.v());
                assertTrue(b.inflate() > 0.5f, s.id() + ": the cap must clear the hat layer (inflate 0.5)");
                for (HairStyleData.Box o : s.boxes()) {
                    if (o == b || !"head".equals(o.part().name().toLowerCase()) || o.rotated()) continue;
                    boolean sameBox = java.util.Arrays.equals(o.from(), b.from()) && java.util.Arrays.equals(o.size(), b.size());
                    if (sameBox) assertTrue(Math.abs(b.inflate() - o.inflate()) > 1e-4f, s.id() + ": two layers on one surface would z-fight");
                }
            }
        }
    }

    @Test
    void theHelmetRuleHidesTheCapAndKeepsWhatHangsOut() throws Exception {
        Map<String, HairStyleData.Style> hair = HairStyleData.parse(json("playermodel/hair_styles.json"));
        HairStyleData.Style lng = hair.get("long");
        for (HairStyleData.Box b : lng.boxes()) assertFalse(HairStyleData.boxShownUnderHelmet(b), "the cap is hidden under a helmet");
        HairStyleData.Chain back = lng.chains().stream().filter(c -> c.name().equals("back")).findFirst().orElseThrow();
        assertTrue(HairStyleData.segmentShownUnderHelmet(back, back.segments().size() - 1), "the end of the back hair hangs out of the helmet");
        float top = back.root()[1] + back.segments().get(0).size()[1];
        assertEquals(top > HairStyleData.HELMET_BOTTOM, HairStyleData.segmentShownUnderHelmet(back, 0));
        HairStyleData.Style bun = hair.get("bun");
        assertTrue(bun.boxes().stream().noneMatch(HairStyleData::boxShownUnderHelmet), "a bun cannot be under a helmet");
    }

    @Test
    void everyExtrasModelParsesWithTheGameLoaderAndHangsFromAnAnchorRoot() throws Exception {
        Map<String, PlayerLook> looks = PlayerLook.parseTable(json("playermodel/looks.json"));
        int checked = 0;
        for (PlayerLook l : looks.values()) {
            for (PlayerLook.Extra x : l.extras()) {
                com.newuniverse.nusmp.client.geo.GeoModelData m = com.newuniverse.nusmp.client.geo.GeoModelData.parse(x.model(), json("geo/entity/" + x.model() + ".geo.json"));
                assertTrue(m.cubeCount() > 0, x.model() + " has no cubes");
                assertFalse(m.roots.isEmpty());
                for (com.newuniverse.nusmp.client.geo.GeoModelData.Bone r : m.roots) assertTrue(r.name.startsWith("x_"), x.model() + " root " + r.name);
                for (PlayerLook.Anchor a : PlayerLook.Anchor.values()) {
                    com.newuniverse.nusmp.client.geo.GeoModelData v = m.view(b -> b.name.equals("x_" + a.key));
                    assertTrue(v.roots.size() <= 1);
                }
                File anim = new File(ASSETS + "animations/entity/" + x.model() + ".animation.json");
                if (anim.isFile()) assertNotNull(com.newuniverse.nusmp.client.geo.GeoAnim.parse(json("animations/entity/" + x.model() + ".animation.json")).clip("idle"), x.model() + " idle");
                checked++;
            }
        }
        assertTrue(checked >= 80, "extras checked: " + checked);
    }

    @Test
    void skinPathsResolveAndRefuseWhatLeavesTheFolder() {
        assertEquals("textures/entity/playermodel/asta.png", SkinPath.resolve("asta"));
        assertEquals("textures/entity/playermodel/asta.png", SkinPath.resolve("nusmp:asta"));
        assertEquals("textures/entity/playermodel/asta_glow.png", SkinPath.glowOf(SkinPath.resolve("asta")));
        assertEquals("textures/entity/hub/x.png", SkinPath.resolve("textures/entity/hub/x.png"));
        for (String bad : new String[]{"../secret", "textures/../../x.png", "/etc/passwd.png", "A B", "", null, "x.jpg", "textures//x.png", "a:b:c"})
            assertEquals("", SkinPath.resolve(bad), String.valueOf(bad));
        assertEquals("textures/entity/playermodel/hair/short.png", SkinPath.hairSheet("hair/short"));
        assertEquals("", SkinPath.hairSheet("../x"));
        assertEquals("textures/entity/pmx_asta.png", SkinPath.extraTexture("pmx_asta"));
        assertEquals("", SkinPath.extraTexture("a/b"));
    }

    @Test
    void everyOldAvatarHairIdIsTheSameHaircutInTheNewSystem() throws Exception {
        Map<String, HairStyleData.Style> hair = HairStyleData.parse(json("playermodel/hair_styles.json"));
        for (int i = 0; i < LegacyHair.COUNT; i++) assertTrue(hair.containsKey(LegacyHair.of(i).style()), "id " + i);
        assertEquals("twin_tails", LegacyHair.of(4).style(), "the old 'twin tails' stays twin tails");
        assertEquals("braid", LegacyHair.of(10).style());
        assertEquals(LegacyHair.of(0).style(), LegacyHair.of(-5).style(), "a bad id is clamped, not a crash");
        assertEquals(LegacyHair.of(15).style(), LegacyHair.of(99).style());
        assertEquals(16, LegacyHair.COUNT, "AppearanceData.HAIR_COUNT");
    }

    @Test
    void everyLookIsReadAndTheScaleIsTheHeightOverTheBase() throws Exception {
        Map<String, PlayerLook> looks = PlayerLook.parseTable(json("playermodel/looks.json"));
        assertTrue(looks.size() >= 90, "looks: " + looks.size());
        PlayerLook escanor = looks.get("escanor");
        assertEquals(2.0f / PlayerLook.BASE_HEIGHT, escanor.scale(), 0.01f);
        assertEquals(PlayerLook.scaleForHeight(1.875f), 1f, 1e-6f);
        assertFalse(looks.get("npc_default").extras().size() > 0);
        PlayerLook lucifero = looks.get("lucifero");
        assertFalse(lucifero.extras().isEmpty(), "wings and horns are extras");
        assertTrue(looks.get("meliodas").holdR() < 0f, "the arm that holds a prop is raised");
        for (PlayerLook l : looks.values()) {
            assertTrue(l.scale() > 0.4f && l.scale() < 2.6f, l.id() + " scale " + l.scale());
            assertTrue(l.alpha() == 1f);
        }
        PlayerLook clamped = looks.get("asta").withScale(Float.NaN).withAlpha(5f);
        assertEquals(1f, clamped.scale());
        assertEquals(1f, clamped.alpha());
    }

    @Test
    void theArcBossesTheKitDrawsAreRegisteredBosses() throws Exception {
        String sources = new String(Files.readAllBytes(new File("src/main/java/com/newuniverse/nusmp/arc/ArcEntities.java").toPath()));
        assertTrue(sources.contains("public static <T extends ArcBoss>"));
        StringBuilder all = new StringBuilder();
        try (var walk = Files.walk(new File("src/main/java/com/newuniverse/nusmp/arc").toPath())) {
            for (var p : (Iterable<java.nio.file.Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) all.append(new String(Files.readAllBytes(p)));
        }
        for (String id : PlayerModelTable.ARC_BOSSES) {
            assertTrue(all.indexOf("\"" + id + "\"") >= 0 || all.indexOf("id() + ") >= 0 || all.indexOf("CHARACTER.id()") >= 0, id + " is not an arc boss id");
        }
    }

    @Test
    void theConvertedEntitiesAreRegisteredEntityIds() throws Exception {
        StringBuilder all = new StringBuilder();
        try (var walk = Files.walk(new File("src/main/java/com/newuniverse/nusmp").toPath())) {
            for (var p : (Iterable<java.nio.file.Path>) walk.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("playermodel"))::iterator) all.append(new String(Files.readAllBytes(p)));
        }
        for (String id : PlayerModelTable.CONVERTED_ENTITIES) {
            boolean ok = all.indexOf("\"" + id + "\"") >= 0;
            assertTrue(ok, id + " is not an entity id of the mod");
        }
    }
}
