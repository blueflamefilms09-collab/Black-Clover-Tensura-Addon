package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.client.aura.EarthArmorStyles;
import com.newuniverse.nusmp.prop.EarthConstruct;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 1.93.0 rebuild of Sandstone, Rock, Red Ochre, Stone, Earth and Mineral Magic, headless: the pure tables the server spells and the client painters share (prop param word,
 * effect palettes, armour style numbers), the page ids the books must keep, and the assets every piece, construct and sprite needs.
 */
class EarthFamilyRebuildTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final String[] KEYS = {"sandstone", "rock", "red_ochre", "stone", "earth", "mineral"};
    private static final String[] PIECES = {"helm", "chest", "belt", "mantle", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l", "greave_r", "greave_l", "back", "extra"};
    private static final String[] CONSTRUCTS = {"earth_golem", "rock_titan", "stone_sentinel", "sandstone_sphinx", "sandstone_cage", "red_ochre_totem", "mineral_spire", "rock_boulder", "mineral_lance"};

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    // ------------------------------------------------------------------ the construct param word
    @Test
    void constructWordRoundTrips() {
        for (int type = 0; type < EarthConstruct.TYPES; type++)
            for (int state = 0; state < 3; state++)
                for (long t : new long[]{0L, 1L, 65535L, 65536L, 123456789L}) {
                    int p = EarthConstruct.pack(type, state, t);
                    assertEquals(type, EarthConstruct.type(p));
                    assertEquals(state, EarthConstruct.state(p));
                    assertEquals(0, EarthConstruct.since(p, t));
                    assertEquals(7, EarthConstruct.since(p, t + 7));
                    assertTrue(p >= 0, "the packed word stays positive");
                }
        int p = EarthConstruct.pack(EarthConstruct.TITAN, EarthConstruct.IDLE, 100);
        int q = EarthConstruct.withState(p, EarthConstruct.ACT, 130);
        assertEquals(EarthConstruct.TITAN, EarthConstruct.type(q));
        assertEquals(EarthConstruct.ACT, EarthConstruct.state(q));
        assertEquals(5, EarthConstruct.since(q, 135));
    }

    @Test
    void constructStylesAndNames() {
        assertEquals(EarthFxStyle.EARTH, EarthConstruct.style(EarthConstruct.GOLEM));
        assertEquals(EarthFxStyle.ROCK, EarthConstruct.style(EarthConstruct.TITAN));
        assertEquals(EarthFxStyle.ROCK, EarthConstruct.style(EarthConstruct.BOULDER));
        assertEquals(EarthFxStyle.STONE, EarthConstruct.style(EarthConstruct.SENTINEL));
        assertEquals(EarthFxStyle.SANDSTONE, EarthConstruct.style(EarthConstruct.SPHINX));
        assertEquals(EarthFxStyle.SANDSTONE, EarthConstruct.style(EarthConstruct.CAGE));
        assertEquals(EarthFxStyle.RED_OCHRE, EarthConstruct.style(EarthConstruct.TOTEM));
        assertEquals(EarthFxStyle.MINERAL, EarthConstruct.style(EarthConstruct.SPIRE));
        assertEquals(EarthFxStyle.MINERAL, EarthConstruct.style(EarthConstruct.LANCE));
        assertTrue(EarthConstruct.valid(8));
        assertFalse(EarthConstruct.valid(9));
        assertFalse(EarthConstruct.valid(-1));
    }

    // ------------------------------------------------------------------ the effect identities
    @Test
    void everyColourFindsItsOwnMagic() {
        for (int s = 0; s < KEYS.length; s++) {
            assertEquals(s, EarthFxStyle.of(EarthFxStyle.color(s)), KEYS[s]);
            assertTrue(EarthFxStyle.exact(EarthFxStyle.color(s)));
            assertEquals(s, EarthFxStyle.style(KEYS[s].toUpperCase()));
            assertTrue(EarthFxStyle.color(s) != EarthFxStyle.glow(s), "the glow differs from the body colour of " + KEYS[s]);
        }
        assertEquals(-1, EarthFxStyle.style("FIRE"));
        assertEquals(EarthFxStyle.STONE, EarthFxStyle.of(0xFF8C8A84));          // a near colour still reads as the nearest magic
    }

    @Test
    void colourTableMatchesTheBooks() throws Exception {
        String wiki = read("src/main/java/com/newuniverse/nusmp/book/WikiBooks.java");
        for (int s = 0; s < KEYS.length; s++) {
            if (s == EarthFxStyle.EARTH) continue;
            Matcher m = Pattern.compile("case " + KEYS[s].toUpperCase() + " -> 0xFF([0-9A-F]{6});").matcher(wiki);
            assertTrue(m.find(), "WikiBooks colour of " + KEYS[s]);
            assertEquals(Integer.parseInt(m.group(1), 16), EarthFxStyle.color(s) & 0xFFFFFF, KEYS[s]);
        }
        String earth = read("src/main/java/com/newuniverse/nusmp/book/EarthBook.java");
        Matcher m = Pattern.compile("super\\(MagicType\\.EARTH, 0xFF([0-9A-F]{6})\\)").matcher(earth);
        assertTrue(m.find());
        assertEquals(Integer.parseInt(m.group(1), 16), EarthFxStyle.color(EarthFxStyle.EARTH) & 0xFFFFFF);
    }

    @Test
    void armourStylesAgreeEverywhere() {
        for (int s = 0; s < KEYS.length; s++) {
            String name = KEYS[s].toUpperCase();
            int style = EarthFxStyle.armourStyle(s);
            assertEquals(style, EarthArmorStyles.of(name), name);
            assertTrue(EarthArmorStyles.owns(style));
            if (s != EarthFxStyle.EARTH) assertEquals(WikiStyles.styleOf(name), style, name + " keeps its WikiStyles index");
        }
        assertEquals(100, EarthArmorStyles.EARTH);
        assertFalse(WikiStyles.valid(EarthArmorStyles.EARTH), "Earth is not a wiki magic: its style must be outside the wiki table");
        assertFalse(EarthArmorStyles.owns(WikiStyles.styleOf("AIR")));
        assertEquals(-1, EarthArmorStyles.of("AIR"));
    }

    // ------------------------------------------------------------------ the books keep what saves depend on
    @Test
    void wikiBooksKeepTheFiveGenericIdsFirst() throws Exception {
        String src = read("src/main/java/com/newuniverse/nusmp/book/EarthFamilyBooks.java");
        String[] order = {"attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"};
        int from = src.indexOf("case SANDSTONE -> List.of(");
        assertTrue(from > 0);
        String[] cases = {"case SANDSTONE -> List.of(", "case ROCK -> List.of(", "case RED_OCHRE -> List.of(", "case STONE -> List.of(", "default -> List.of("};
        for (int c = 0; c < cases.length; c++) {
            int start = src.indexOf(cases[c]);
            int end = c + 1 < cases.length ? src.indexOf(cases[c + 1]) : src.indexOf("return new EarthFamilyBook");
            String block = src.substring(start, end);
            int at = 0;
            for (String id : order) {
                int k = block.indexOf("\"" + id + "\"", at);
                assertTrue(k >= at, cases[c] + " lost or reordered " + id);
                at = k;
            }
            List<String> ids = new ArrayList<>();
            Matcher m = Pattern.compile("(?:starter|mid|zone|signature)\\(\"([a-z_]+)\"").matcher(block);
            while (m.find()) ids.add(m.group(1));
            assertEquals(ids.size(), ids.stream().distinct().count(), "page ids are unique in " + cases[c]);
            assertTrue(ids.size() >= 7 && ids.size() <= 32, "page count of " + cases[c]);
            assertEquals(List.of(order), ids.subList(0, 5), cases[c] + ": the five generic ids come first, in order");
        }
        String wiki = read("src/main/java/com/newuniverse/nusmp/book/WikiBooks.java");
        assertTrue(wiki.contains("EarthFamilyBooks.owns(magic)"));
    }

    @Test
    void earthBookKeepsItsSixPagesFirst() throws Exception {
        String src = read("src/main/java/com/newuniverse/nusmp/book/EarthBook.java");
        String[] old = {"spike_line", "wall", "mother_earth_split", "boulder_shot", "witch_hunter_claws", "rampaging_mother_earth"};
        String[] added = {"earth_golem", "earth_armor", "sinkhole"};
        int at = src.indexOf("private final List<BookPage> pages");
        assertTrue(at > 0);
        for (String id : old) { int k = src.indexOf("\"" + id + "\"", at); assertTrue(k >= at, "Earth lost or reordered " + id); at = k; }
        for (String id : added) { int k = src.indexOf("\"" + id + "\"", at); assertTrue(k >= at, "Earth's new page " + id + " must come after the old ones"); at = k; }
    }

    // ------------------------------------------------------------------ assets
    @Test
    void everyArmourPieceExists() {
        for (String key : KEYS)
            for (String piece : PIECES) {
                String id = key + "_" + piece;
                assertTrue(new File(ASSETS, "geo/entity/" + id + ".geo.json").isFile(), "model " + id);
                assertTrue(new File(ASSETS, "textures/entity/" + id + ".png").isFile(), "texture " + id);
                assertTrue(new File(ASSETS, "textures/entity/" + id + "_glow.png").isFile(), "glow map " + id);
            }
        for (String id : new String[]{"rock_back", "sandstone_back", "red_ochre_back", "stone_back", "earth_back", "mineral_back", "sandstone_extra", "red_ochre_extra", "stone_extra", "earth_extra", "mineral_extra"})
            assertTrue(new File(ASSETS, "animations/entity/" + id + ".animation.json").isFile(), "the animated piece " + id + " needs its clip");
    }

    @Test
    void everyConstructExists() {
        for (String id : CONSTRUCTS) {
            assertTrue(new File(ASSETS, "geo/entity/" + id + ".geo.json").isFile(), "model " + id);
            assertTrue(new File(ASSETS, "animations/entity/" + id + ".animation.json").isFile(), "animations " + id);
            assertTrue(new File(ASSETS, "textures/entity/" + id + ".png").isFile(), "texture " + id);
            assertTrue(new File(ASSETS, "textures/entity/" + id + "_glow.png").isFile(), "glow map " + id);
        }
    }

    @Test
    void everySpriteTheLayerNamesExists() throws Exception {
        String src = read("src/main/java/com/newuniverse/nusmp/vfx/client/layer/EarthFamilyFx.java");
        Matcher m = Pattern.compile("t\\(\"([a-z_]+)\", \"([a-z_]+)\"\\)").matcher(src);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(new File(ASSETS, "textures/particle/earthfam_" + m.group(1) + "_" + m.group(2) + ".png").isFile(), "sprite " + m.group(1) + "_" + m.group(2));
        }
        assertTrue(n >= 30, "the sprite tables were read (" + n + ")");
        for (String key : KEYS)
            assertTrue(new File(ASSETS, "textures/particle/earthfam_" + key + "_" + (key.equals("sandstone") ? "sigil" : key.equals("rock") ? "sigil" : key.equals("red_ochre") ? "sun" : "circle") + ".png").isFile());
    }

    @Test
    void painterClipsExistInTheModels() throws Exception {
        String painter = read("src/main/java/com/newuniverse/nusmp/client/prop/EarthPropPainter.java");
        for (String clip : new String[]{"idle", "walk", "attack", "roar", "block", "form", "crumble", "grow", "pulse"})
            assertTrue(painter.contains("\"" + clip + "\""), "painter plays " + clip);
        String[][] need = {{"earth_golem", "idle", "walk", "attack"}, {"rock_titan", "idle", "walk", "attack", "roar"}, {"stone_sentinel", "idle", "walk", "attack", "block"},
                {"sandstone_sphinx", "idle", "walk", "attack"}, {"sandstone_cage", "idle", "form", "crumble"}, {"red_ochre_totem", "idle", "attack"}, {"mineral_spire", "idle", "grow", "pulse"},
                {"rock_boulder", "idle"}, {"mineral_lance", "idle"}};
        for (String[] row : need) {
            String json = read("src/main/resources/assets/nusmp/animations/entity/" + row[0] + ".animation.json");
            for (int k = 1; k < row.length; k++) assertTrue(json.contains("\"" + row[k] + "\""), row[0] + " has the clip " + row[k]);
        }
    }
}
