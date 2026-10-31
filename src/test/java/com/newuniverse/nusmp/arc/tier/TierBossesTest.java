package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 3.33.0: the Tier List bosses of tiers F and D obey the house rules, stand on their floors, are all different and have their skins and names. */
class TierBossesTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");

    private static List<TierRow> all() {
        List<TierRow> out = new java.util.ArrayList<>(TierRosterF.rows());
        out.addAll(TierRosterD.rows());
        return out;
    }

    @Test
    void sixteenCharactersTheFirstBatch() {
        List<TierRow> all = all();
        assertEquals(6, TierRosterF.rows().size());
        assertEquals(10, TierRosterD.rows().size());
        assertEquals(16, all.size());
    }

    @Test
    void everyRowObeysTheHouseRules() {
        for (TierRow row : all()) assertEquals(List.of(), TierRules.problems(row), row.id());
    }

    @Test
    void idsAreUniqueAndTheNamesToo() {
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (TierRow row : all()) {
            assertTrue(ids.add(row.id()), "repeated id " + row.id());
            assertTrue(names.add(row.character().name()), "repeated name " + row.character().name());
            assertTrue(row.id().matches("[a-z][a-z0-9_]*"), row.id());
        }
    }

    @Test
    void lowTiersStandOnTheEarlyFloors() {
        for (TierRow row : all()) {
            assertTrue(row.character().floorMin() >= 1 && row.character().floorMax() <= 20, row.id());
        }
    }

    @Test
    void theLadderOnlyGoesUp() {
        TierLetter[] l = TierLetter.values();
        for (int i = 1; i < l.length; i++) {
            assertTrue(l[i].health() > l[i - 1].health(), l[i].label());
            assertTrue(l[i].attack() >= l[i - 1].attack(), l[i].label());
            assertTrue(l[i].floorMax() >= l[i - 1].floorMax(), l[i].label());
        }
        assertEquals(TierLetter.TOP, TierLetter.fromLabel("SSS+++"));
        assertTrue(TierLetter.A.light() && !TierLetter.S.light());
    }

    /** 3.33.0: the tier bosses wear the mod's player-model kit like the other humanoids: a painted skin and a look in looks_tier.json. */
    @Test
    void everyCharacterHasItsPlayerModelSkinAndLook() throws Exception {
        String looks = Files.readString(RES.resolve("playermodel/looks_tier.json"));
        for (TierRow row : all()) {
            assertTrue(Files.isRegularFile(RES.resolve("textures/entity/playermodel/" + row.id() + ".png")), "missing player-model skin " + row.id() + ".png");
            assertTrue(looks.contains("\"" + row.id() + "\""), "no look for " + row.id());
        }
    }

    @Test
    void thePlayerModelKitDrawsExactlyTheseBosses() {
        assertEquals(TierRoster.ids().size(), all().size() + TierRosterGag.rows().size());          // 3.36.0: the gag boss is appended after the sixteen
        for (String id : TierRoster.ids()) assertTrue(com.newuniverse.nusmp.client.playermodel.PlayerModelTable.ARC_BOSSES.contains(id), id);
        assertTrue(com.newuniverse.nusmp.client.playermodel.PlayerLookTables.FILES.contains("playermodel/looks_tier.json"));
    }

    /**
     * 3.37: anime skin style v2 (replaces the 3.33.1 rule "the hair is painted flat on the skin"): every tier boss, now and in every later tier, wears the kit's volumetric hair (an anime_* cut of
     * hair_styles.json, with a hair colour), the scalp is painted on the base head only (the hat layer's top face stays free, so no flat hair under the 3D hair), and the sleeves, jacket and
     * legs carry overlay-layer details (the overlay layers are not empty). The old flat mode stays reachable with gen_tier_looks.py --flat.
     */
    @Test
    void everyTierLookIsInTheAnimeStyleV2WithVolumetricHairAndOverlayDetails() throws Exception {
        com.google.gson.JsonObject looks = com.google.gson.JsonParser.parseString(Files.readString(RES.resolve("playermodel/looks_tier.json"))).getAsJsonObject().getAsJsonObject("looks");
        com.google.gson.JsonObject styles = com.google.gson.JsonParser.parseString(Files.readString(RES.resolve("playermodel/hair_styles.json"))).getAsJsonObject().getAsJsonObject("styles");
        for (TierRow row : all()) {
            assertTrue(looks.has(row.id()), "no look for " + row.id());
            com.google.gson.JsonObject look = looks.getAsJsonObject(row.id());
            assertTrue(look.has("hair"), row.id() + " has no kit haircut: anime skin style v2 uses the volumetric hair");
            String hair = look.get("hair").getAsString();
            assertTrue(hair.startsWith("anime_") && styles.has(hair), row.id() + ": " + hair + " is not an anime_* cut of hair_styles.json");
            assertTrue(look.has("hair_color") && look.get("hair_color").getAsString().matches("#[0-9a-fA-F]{6}"), row.id() + " needs a hair colour");
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(RES.resolve("textures/entity/playermodel/" + row.id() + ".png").toFile());
            int k = img.getWidth() / 64;                                                       // 3.38.0 HD outfits: a skin may be 64, 128 or 256 square; every model pixel is a k x k block of texels
            assertTrue(k >= 1, row.id());
            int hatTop = 0;
            for (int y = 0; y < 8 * k; y++) for (int x = 40 * k; x < 48 * k; x++) if ((img.getRGB(x, y) >>> 24) != 0) hatTop++;
            assertEquals(0, hatTop, row.id() + ": hair painted flat on the hat layer's top (the volumetric hair covers it)");
            int overlay = 0;
            for (int y = 32 * k; y < 48 * k; y++) for (int x = 16 * k; x < 40 * k; x++) if ((img.getRGB(x, y) >>> 24) != 0) overlay++;          // the jacket layer of the body
            for (int y = 32 * k; y < 48 * k; y++) for (int x = 40 * k; x < 56 * k; x++) if ((img.getRGB(x, y) >>> 24) != 0) overlay++;          // the sleeve layer of the right arm
            assertTrue(overlay > 120 * k * k, row.id() + ": the overlay layers carry no outfit details (" + overlay + " texels)");
        }
    }

    /**
     * 3.38.0 (HD outfits): a tier boss skin may be 64, 128 or 256 square (the UVs are fractions of the 64x64 layout, so the same layout simply gets sampled finer): equal width and height, a
     * power-of-two multiple of 64, a glow map of the same size when the look glows, and the face rectangles of the base layer (head, body, arms, legs: front and back) have no hole.
     */
    @Test
    void everyTierSkinAndGlowMapIsSquareAMultipleOf64AndTheGlowMatchesItsSkin() throws Exception {
        com.google.gson.JsonObject looks = com.google.gson.JsonParser.parseString(Files.readString(RES.resolve("playermodel/looks_tier.json"))).getAsJsonObject().getAsJsonObject("looks");
        for (TierRow row : all()) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(RES.resolve("textures/entity/playermodel/" + row.id() + ".png").toFile());
            assertEquals(img.getWidth(), img.getHeight(), row.id() + ": a skin is square");
            assertEquals(0, img.getWidth() % 64, row.id() + ": a multiple of 64");
            int k = img.getWidth() / 64;
            assertTrue(k >= 1 && (k & (k - 1)) == 0 && img.getWidth() <= 1024, row.id() + ": a power-of-two multiple of 64 up to 1024, got " + img.getWidth());
            assertTrue(com.newuniverse.nusmp.client.playermodel.SkinPath.validSkinSize(img.getWidth(), img.getHeight()), row.id());
            boolean glows = looks.getAsJsonObject(row.id()).has("glow") && looks.getAsJsonObject(row.id()).get("glow").getAsBoolean();
            java.nio.file.Path gp = RES.resolve("textures/entity/playermodel/" + row.id() + "_glow.png");
            if (glows) assertTrue(Files.isRegularFile(gp), row.id() + " says it glows but has no glow map");
            if (Files.isRegularFile(gp)) {
                java.awt.image.BufferedImage g = javax.imageio.ImageIO.read(gp.toFile());
                assertEquals(img.getWidth(), g.getWidth(), row.id() + ": the glow map has the size of its skin");
                assertEquals(img.getHeight(), g.getHeight(), row.id() + ": the glow map has the size of its skin");
            }
            int[][] fronts = {{8, 8, 8, 8}, {20, 20, 8, 12}, {32, 20, 8, 12}, {4, 20, 4, 12}, {12, 20, 4, 12}};   // head front, body front and back, right leg front and back (arms: wide or slim, left to the arm tests of the kit)
            for (int[] r : fronts)
                for (int y = r[1] * k; y < (r[1] + r[3]) * k; y++)
                    for (int x = r[0] * k; x < (r[0] + r[2]) * k; x++)
                        assertTrue((img.getRGB(x, y) >>> 24) != 0, row.id() + ": a hole in the base layer at " + x + "," + y);
        }
    }

    /** The old shared-body recolours stay in the jar as the fallback if the kit is switched off. */
    @Test
    void theLegacySharedBodySkinsAreKept() {
        for (TierRow row : all()) assertTrue(Files.isRegularFile(RES.resolve("textures/entity/arc_" + row.id() + ".png")), "kept skin missing " + row.id());
    }

    @Test
    void everyCharacterHasANameInTheLanguageFile() throws Exception {
        String lang = Files.readString(RES.resolve("lang/en_us.json"));
        for (TierRow row : all()) assertTrue(lang.contains("\"entity.nusmp." + row.id() + "\""), "no name for " + row.id());
    }

    @Test
    void theRulesCatchABrokenRow() {
        TierMove cheap = TierMove.of("tap", "Tap", TierMove.Kind.CLEAVE).wind(4).cd(10).range(0, 3).dmg(9).build();
        assertFalse(TierRules.moveProblems("x", List.of(cheap)).isEmpty());
    }
}
