package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the theme table is what gives every magic, relic grade and boss its own particles, so it must be complete and consistent: it names only registered library particles (light and
 * dark flavour), has 1 to 4 per theme, covers all 105 magic types (read from the enum's source, so a magic added later without a theme fails here), the seven relic grades, the relic
 * elements, and every theme the codemod's map names. The slots a call site uses most often (MOTE and AMBIENT) must be small particles, because they replace the old end-rod and smoke
 * particles that are spawned by the dozen. The resolution rules (exact id, nearest dotted parent, default) and the key interning are tested on a small table of their own.
 */
class ParticleThemesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static ParticleThemes.Table real() throws IOException {
        return ParticleThemes.parse(JsonParser.parseString(Files.readString(ROOT.resolve("src/main/resources/data/nusmp/particle_themes.json"), StandardCharsets.UTF_8)));
    }

    private static List<String> enumNames(String file, String regex) throws IOException {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(Files.readString(SRC.resolve(file), StandardCharsets.UTF_8));
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void everyThemeNamesOnlyRegisteredParticlesInOneToFourSlots() throws IOException {
        for (ParticleThemes.Theme t : real().themes().values()) {
            assertTrue(t.particles().size() >= 1 && t.particles().size() <= 4, t.id() + ": " + t.particles().size() + " particles");
            assertTrue(t.darkParticles().size() >= 1 && t.darkParticles().size() <= 4, t.id() + " dark");
            for (String p : t.particles()) assertTrue(FxCatalog.has(p), t.id() + " names the unknown particle " + p);
            for (String p : t.darkParticles()) assertTrue(FxCatalog.has(p), t.id() + " (dark) names the unknown particle " + p);
            assertTrue(t.rgb() >= 0 && t.darkRgb() >= 0, t.id() + " colour");
        }
        assertTrue(FxCatalog.has("mote") && real().fallback().particles().stream().allMatch(FxCatalog::has), "the default theme is broken");
    }

    @Test
    void allHundredAndFiveMagicTypesHaveTheirOwnTheme() throws IOException {
        List<String> magics = enumNames("blackclover/MagicType.java", "^    ([A-Z][A-Z0-9_]*)\\(\"");
        assertEquals(105, magics.size(), "MagicType changed: update the themes (tools/particles/gen_themes.py) and this number");
        ParticleThemes.Table t = real();
        Set<String> identities = new TreeSet<>();
        for (String m : magics) {
            assertTrue(t.has("magic." + m), "no theme for magic " + m);
            ParticleThemes.Theme th = t.themes().get("magic." + m);
            identities.add(String.join(",", th.particles()) + "#" + th.rgb());
        }
        assertTrue(identities.size() >= 100, "the magics should not share looks: only " + identities.size() + " distinct (particles, colour) pairs");
    }

    @Test
    void theRelicGradesAndTheDocumentedFamiliesHaveThemes() throws IOException {
        ParticleThemes.Table t = real();
        for (String g : enumNames("relics/Grade.java", "^    ([A-Z]+)\\(\"")) assertTrue(t.has("relic.grade." + g), "no theme for the relic grade " + g);
        for (String e : new String[]{"fire", "water", "ice", "wind", "earth", "storm", "nature", "light", "holy", "dark", "demon", "arcane", "blood", "metal", "time", "space", "soul", "music", "poison", "dream", "gem"})
            assertTrue(t.has("relic.element." + e), "no theme for the relic element " + e);
        for (String k : new String[]{"skill", "skill.god", "skill.sword", "skill.requip", "boss.lucius", "boss.zenon", "boss.dante", "boss.vanica", "boss.charybdis", "boss.goliath", "dungeon", "dungeon.gate", "dungeon.vault", "corpse"})
            assertTrue(t.has(k), "no theme " + k);
        // a god relic is gold-white glints, a genesis relic is stars and nebula
        assertTrue(t.themes().get("relic.grade.GOD").particles().contains("glint"));
        assertTrue(t.themes().get("relic.grade.GENESIS").particles().contains("nebula_puff"));
    }

    @Test
    void theSlotsTheOldCodeUsesByTheDozenAreSmallParticles() throws IOException {
        for (ParticleThemes.Theme t : real().themes().values()) {
            for (FxSlot s : FxSlot.values()) {
                for (boolean dark : new boolean[]{false, true}) {
                    FxCatalog.Def d = FxCatalog.byId(t.particle(s, dark));
                    assertNotNull(d, t.id());
                    float w = Math.max(d.profile().sizeStart(), d.profile().sizeEnd());
                    assertTrue(w <= 1.05f, t.id() + " slot " + s + (dark ? " (dark)" : "") + " is " + d.id() + ", " + w + " blocks wide: shapes belong to the fixed particles, not to slots");
                }
            }
        }
    }

    @Test
    void everyThemeTheCodemodMapNamesExistsExactly() throws IOException {
        JsonObject map = JsonParser.parseString(Files.readString(ROOT.resolve("tools/particles/map.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        ParticleThemes.Table t = real();
        map.getAsJsonArray("themes").forEach(e -> {
            String id = e.getAsJsonArray().get(1).getAsString();
            if (id.contains("$")) {                                     // a template (relic.wave.$1): every number the wave files can have must exist
                try (java.util.stream.Stream<Path> waves = Files.list(SRC.resolve("relics/wave"))) {
                    for (Path w : (Iterable<Path>) waves::iterator) {
                        Matcher wm = Pattern.compile("^Wave(\\d+)").matcher(w.getFileName().toString());
                        if (wm.find()) assertTrue(t.has(id.replace("$1", wm.group(1))), "map.json template " + id + " has no theme for wave " + wm.group(1));
                    }
                } catch (IOException ex) {
                    throw new AssertionError(ex);
                }
            } else {
                assertTrue(t.has(id), "map.json names the unknown theme " + e);
            }
        });
        assertTrue(t.has(map.get("default_theme").getAsString()));
    }

    @Test
    void resolutionGoesExactThenParentThenDefault() {
        JsonObject doc = JsonParser.parseString("{\"default\":{\"particles\":[\"mote\"],\"color\":\"#102030\"},\"themes\":{"
                + "\"a\":{\"particles\":[\"ember\",\"spark\"],\"color\":\"#FF0000\"},\"a.b.c\":{\"particles\":[\"glint\"],\"color\":\"#00FF00\",\"dark\":{\"particles\":[\"void_speck\"],\"color\":\"#010203\"}}}}").getAsJsonObject();
        ParticleThemes.Table t = ParticleThemes.parse(doc);
        assertEquals("a.b.c", t.resolve("a.b.c").id());
        assertEquals("a", t.resolve("a.b").id(), "a missing child resolves to its nearest parent");
        assertEquals("a", t.resolve("a.zzz.q").id());
        assertEquals("default", t.resolve("zzz").id());
        assertEquals("default", t.resolve(null).id());
        assertEquals(0x102030, t.resolve("zzz").rgb());
        ParticleThemes.Theme c = t.resolve("a.b.c");
        assertEquals(0x010203, c.darkRgb());
        assertEquals("void_speck", c.particle(FxSlot.SPARK, true), "a one-particle theme answers every slot with it");
        ParticleThemes.Theme a = t.resolve("a");
        assertEquals("ember", a.particle(FxSlot.MOTE, false));
        assertEquals("spark", a.particle(FxSlot.ACCENT, false));
        assertEquals("ember", a.particle(FxSlot.SPARK, false), "slots wrap around a short list");
        assertEquals(ParticleThemes.darken(0xFF0000), a.darkRgb(), "a theme without a dark colour gets the derived one");
    }

    @Test
    void aBrokenDocumentFallsBackToTheBuiltInDefault() {
        assertEquals("default", ParticleThemes.parse(null).resolve("magic.FLAME").id());
        assertEquals("default", ParticleThemes.parse(JsonParser.parseString("[1,2]")).resolve("x").id());
        ParticleThemes.Table t = ParticleThemes.parse(JsonParser.parseString("{\"themes\":{\"x\":{\"particles\":[],\"color\":\"nonsense\"},\"y\":5}}"));
        assertEquals(ParticleThemes.BUILT_IN.particles(), t.resolve("x").particles(), "an empty list takes the default's particles");
        assertEquals(ParticleThemes.BUILT_IN.rgb(), t.resolve("x").rgb(), "an unreadable colour takes the default's");
        assertFalse(t.has("y"));
    }

    @Test
    void keysAreInternedAndCacheTheirTheme() {
        ThemeKey a = ThemeKey.of("magic.FLAME");
        assertSame(a, ThemeKey.of("magic.FLAME"));
        assertEquals("magic.FLAME.x", a.child("x").id());
        assertEquals("default", ThemeKey.of(null).id());
        ParticleThemes.Theme first = a.theme();
        assertSame(first, a.theme());
        ParticleThemes.reload(null);
        assertNotNull(a.theme(), "after a reload the key resolves again");
    }
}
