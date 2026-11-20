package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.FormsFileCheck;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Why: the owner's rule is that EVERY bow form gives its real arrows a magic effect and a look; a form added to the forms file later must not silently shoot a
 * plain arrow. This test reads the real forms file and the real texture folder and fails when a form has no row in {@link BowArrowForms}, no effect, a
 * magic share outside its range or no texture, and pins the table (the row order is what the arrow tells the client), the technique kinds and the
 * form-to-effect list of docs/heroweapon/bow_arrows.md.
 */
class BowArrowFormsTest {
    private static final String FORMS = "src/main/resources/data/anime_multiverse/hero/forms/legendary_bow.json";
    private static final Path TEXTURES = Path.of("src/main/resources/assets/anime_multiverse/textures/entity/bow_arrow");

    private static List<String> fileForms() throws Exception {
        List<String> out = new ArrayList<>();
        for (JsonElement e : FormsFileCheck.read(FORMS).getAsJsonArray("forms")) out.add(FormsFileCheck.str(e.getAsJsonObject(), "id"));
        return out;
    }

    @Test
    void everyFormOfTheFileHasAnArrowRowAnEffectAndATexture() throws Exception {
        List<String> bad = new ArrayList<>();
        for (JsonElement e : FormsFileCheck.read(FORMS).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            String id = FormsFileCheck.str(f, "id");
            if (!BowArrowForms.has(id)) {
                bad.add(id + " has no row in BowArrowForms");
                continue;
            }
            BowArrowForms.Look l = BowArrowForms.of(id);
            if (l.kind().armedOnly()) bad.add(id + " uses the technique-only kind " + l.kind());
            if (l.magicShare() <= 0.0 || l.magicShare() > 0.6) bad.add(id + " has a magic share of " + l.magicShare());
            if (FormsFileCheck.bool(f, "curse") && l.kind() != ArrowKind.PRIDE) bad.add(id + " is a Pride form but its arrows are " + l.kind());
            if (!FormsFileCheck.bool(f, "curse") && l.kind() == ArrowKind.PRIDE) bad.add(id + " is not a Pride form but its arrows carry the pride curse");
            Path tex = Path.of("src/main/resources/assets/anime_multiverse/" + BowArrowForms.texturePath(l));
            if (!Files.isRegularFile(tex)) bad.add(id + " has no texture " + tex);
        }
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void theTableHasNoRowForAFormThatDoesNotExist() throws Exception {
        Set<String> file = new HashSet<>(fileForms());
        for (String id : BowArrowForms.ids()) assertTrue(file.contains(id), "the table names the form " + id + " which the forms file does not have");
        assertEquals(file.size(), BowArrowForms.count());
    }

    @Test
    void theTexturesAreRealPngsOfTheVanillaArrowLayout() throws Exception {
        for (String id : BowArrowForms.ids()) {
            BufferedImage im = ImageIO.read(TEXTURES.resolve(BowArrowForms.of(id).texture() + ".png").toFile());
            assertNotNull(im, id);
            assertEquals(32, im.getWidth(), id);
            assertEquals(32, im.getHeight(), id);
            int opaqueShaft = 0;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 5; y++) if ((im.getRGB(x, y) >>> 24) > 0) opaqueShaft++;
            assertTrue(opaqueShaft >= 30, id + " paints too little of the shaft region");
        }
        BufferedImage glow = ImageIO.read(TEXTURES.resolve("glow.png").toFile());
        assertEquals(16, glow.getWidth());
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            int argb = glow.getRGB(x, y);
            if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, "colour under a transparent pixel of glow.png");
        }
    }

    @Test
    void theRowOrderIsPinnedBecauseTheArrowTellsTheClientTheNumber() {
        List<String> expected = List.of("base", "hunters_bow", "scouts_bow", "bind_arrow", "bird_hunting", "arrow_shower", "meteor_bow", "frozen_rain", "eagle_piercer",
                "sagittarius", "bow_of_pride_1", "bow_of_pride_2", "bow_of_pride_3");
        assertEquals(expected, BowArrowForms.ids().subList(0, expected.size()), "rows are only ever appended");
        for (int i = 0; i < BowArrowForms.count(); i++) assertEquals(BowArrowForms.ids().get(i), BowArrowForms.byIndex(i).form());
        assertEquals("base", BowArrowForms.byIndex(-1).form());
        assertEquals("base", BowArrowForms.byIndex(999).form());
        assertEquals(0, BowArrowForms.index("no_such_form"));
        assertEquals("base", BowArrowForms.of("no_such_form").form());
        assertEquals("base", BowArrowForms.of(null).form());
    }

    @Test
    void theFormToEffectListOfTheDocIsWhatTheTableSays() {
        assertEquals(ArrowKind.LIGHT, BowArrowForms.of("base").kind());
        assertEquals(ArrowKind.SPLIT, BowArrowForms.of("arrow_shower").kind());
        assertEquals(ArrowKind.BIND, BowArrowForms.of("bind_arrow").kind());
        assertEquals(ArrowKind.PIERCE, BowArrowForms.of("bird_hunting").kind());
        assertEquals(ArrowKind.PIERCE, BowArrowForms.of("eagle_piercer").kind());
        assertEquals(ArrowKind.FROST, BowArrowForms.of("frozen_rain").kind());
        assertEquals(ArrowKind.BLEED, BowArrowForms.of("hunters_bow").kind());
        assertEquals(ArrowKind.FIRE, BowArrowForms.of("meteor_bow").kind());
        assertEquals(ArrowKind.STAR, BowArrowForms.of("sagittarius").kind());
        assertEquals(ArrowKind.MARK, BowArrowForms.of("scouts_bow").kind());
        for (String p : List.of("bow_of_pride_1", "bow_of_pride_2", "bow_of_pride_3")) assertEquals(ArrowKind.PRIDE, BowArrowForms.of(p).kind());
    }

    @Test
    void everyFormKindIsUsedAndTheTechniqueKindsBelongToNoForm() {
        Set<ArrowKind> used = EnumSet.noneOf(ArrowKind.class);
        for (String id : BowArrowForms.ids()) used.add(BowArrowForms.of(id).kind());
        for (ArrowKind k : ArrowKind.values()) {
            if (k.armedOnly()) assertFalse(used.contains(k), k + " is a technique kind");
            else assertTrue(used.contains(k), "no form uses " + k);
        }
    }

    @Test
    void theArmedModesPickTheirOwnKindAndTheRestKeepTheForm() {
        assertEquals(ArrowKind.SNIPER, ArrowKind.of(ArmedMode.SNIPER, ArrowKind.FIRE));
        assertEquals(ArrowKind.BIND, ArrowKind.of(ArmedMode.NET, ArrowKind.FIRE));
        assertEquals(ArrowKind.MARK, ArrowKind.of(ArmedMode.MARK, ArrowKind.FIRE));
        assertEquals(ArrowKind.BURST, ArrowKind.of(ArmedMode.BURST, ArrowKind.FIRE));
        assertEquals(ArrowKind.SEEK, ArrowKind.of(ArmedMode.SEEK, ArrowKind.FIRE));
        assertEquals(ArrowKind.COMMAND, ArrowKind.of(ArmedMode.COMMAND, ArrowKind.FIRE));
        assertEquals(ArrowKind.FIRE, ArrowKind.of(ArmedMode.SECOND, ArrowKind.FIRE), "Second Arrow is the form's own arrow, twice");
        assertEquals(ArrowKind.FROST, ArrowKind.of(ArmedMode.NONE, ArrowKind.FROST));
        for (ArmedMode m : ArmedMode.values()) assertNotNull(ArrowKind.of(m, ArrowKind.LIGHT), m.id());
    }

    @Test
    void theKindOrderIsPinned() {
        assertEquals(List.of("LIGHT", "SPLIT", "BIND", "PIERCE", "FROST", "BLEED", "FIRE", "STAR", "MARK", "PRIDE", "SNIPER", "BURST", "SEEK", "COMMAND"),
                java.util.Arrays.stream(ArrowKind.values()).map(Enum::name).toList(), "kinds are only ever appended");
    }
}
