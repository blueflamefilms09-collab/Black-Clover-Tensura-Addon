package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules.Category;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The EX-Singularity grade (above Genesis, {@link Grade#AKASHIC}): the order, "never rolled", the star run, the pure prismatic palette (bounded, moving, saturated, a glint), the shard orbit,
 * the Shishou tooltip style, the damage table, the lang fragment, the texture, the wiring of the hooks, and the "nothing is allocated per frame" source scan of the draw code.
 */
class ExSingularityTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    // ---------------------------------------------------------------- the grade
    @Test
    void akashicIsTheTopGradeRightAboveGenesis() {
        Grade[] g = Grade.values();
        assertEquals(8, g.length);
        assertEquals(Grade.GENESIS, g[6]);
        assertEquals(Grade.AKASHIC, g[7]);
        assertEquals(8, Grade.AKASHIC.tier());
        assertTrue(Grade.AKASHIC.atLeast(Grade.GENESIS));
        assertFalse(Grade.GENESIS.atLeast(Grade.AKASHIC));
        assertEquals(Grade.AKASHIC, Grade.max(Grade.GENESIS, Grade.AKASHIC));
        assertEquals("EX-Singularity Grade", Grade.AKASHIC.label());
        assertEquals("EX-Singularity", Grade.AKASHIC.shortName());
        assertEquals(Grade.AKASHIC, Grade.byName("EX-Singularity"));
        assertEquals(Grade.AKASHIC, Grade.byName("ex-singularity grade"));
        assertEquals(Grade.AKASHIC, Grade.byName("akashic"));
        assertTrue(Grade.AKASHIC.epBonus() >= Grade.GENESIS.epBonus());
        for (int i = 0; i < 7; i++) assertEquals(i, g[i].ordinal(), "the saved ordinals of the old grades are unchanged");
    }

    @Test
    void akashicIsNeverRolled() {
        assertFalse(Grade.AKASHIC.rollable());
        assertEquals(0.0, Grade.AKASHIC.weight(), 1e-12);
        for (Grade g : Grade.values()) if (g != Grade.AKASHIC) {
            assertTrue(g.rollable(), g.name());
            assertTrue(g.weight() > 0.0, g.name());
        }
        assertEquals(Grade.RARE, Grade.ofCard("ex-singularity"), "a printed card rarity never becomes EX-Singularity");
    }

    @Test
    void noRelicDefinitionAndNoLootTableNamesTheGrade() throws IOException {
        // the relic catalogue (every wave) and the datapack loot tables never name it: only an item that is handed over by hand does
        try (Stream<Path> files = Files.walk(SRC.resolve("relics"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String name = p.getFileName().toString();
                if (name.startsWith("ExSingularity") || name.equals("Grade.java") || name.equals("GradeDamage.java") || name.equals("RelicLoot.java")) continue;
                String text = Files.readString(p);
                assertFalse(text.contains("Grade.AKASHIC"), name + " names Grade.AKASHIC");
            }
        }
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json") && !f.toString().contains("particle_themes"))::iterator) {
                String text = Files.readString(p).toLowerCase();
                assertFalse(text.contains("\"max_grade\": \"akashic\"") || text.contains("\"max_grade\":\"akashic\"") || text.contains("\"max_grade\": \"ex-singularity\""), p + " lets a loot entry roll EX-Singularity");
            }
        }
        String loot = Files.readString(SRC.resolve("relics/RelicLoot.java"));
        assertTrue(loot.contains("!d.grade().rollable()"), "the relic roll skips unrollable grades");
        assertTrue(Files.readString(SRC.resolve("labyrinth/mega/flow/DecennialReward.java")).contains("!d.grade().rollable()"));
        assertTrue(Files.readString(SRC.resolve("labyrinth/mega/decennial/DecennialRewards.java")).contains("!d.grade().rollable()"));
    }

    @Test
    void theStarRunIsLongerThanGenesisAndItsOwn() {
        String s = Grade.AKASHIC.stars();
        assertEquals(ExSingularity.STARS, s);
        assertTrue(s.length() > Grade.GENESIS.stars().length());
        assertEquals(9, s.chars().filter(c -> c == '★').count(), "nine filled stars");
        assertEquals('✦', s.charAt(0));
        assertEquals('✦', s.charAt(s.length() - 1));
        assertEquals(7, Grade.GENESIS.stars().length(), "Genesis keeps its seven");
    }

    @Test
    void damageTableLiesAboveGenesis() {
        assertTrue(GradeDamage.range(Grade.AKASHIC.ordinal())[0] > GradeDamage.range(Grade.GENESIS.ordinal())[1]);
        assertTrue(GradeDamage.range(Grade.AKASHIC.ordinal())[0] > 400, "above Elsdocia's own 400");
        assertEquals(Grade.AKASHIC.ordinal(), GradeDamage.ordinal("EX-Singularity"));
        assertEquals(Grade.AKASHIC.ordinal(), GradeDamage.ordinal("akashic"));
        assertEquals(Grade.AKASHIC.ordinal(), GradeDamage.ordinal("ex singularity grade"));
        assertEquals(GradeDamage.range(7)[1], GradeDamage.range(99)[1], "ordinals above the table clamp to the top");
        assertTrue(GradeDamage.damage(7, "longsword") > GradeDamage.damage(6, "greatsword"));
    }

    // ---------------------------------------------------------------- the palette
    @Test
    void hsvKnownColours() {
        assertEquals(0xFF0000, ExSingularity.hsv(0f, 1f, 1f));
        assertEquals(0xFF0000, ExSingularity.hsv(1f, 1f, 1f), "a whole turn is the same colour");
        assertEquals(0xFFFF00, ExSingularity.hsv(1f / 6f, 1f, 1f));
        assertEquals(0x00FFFF, ExSingularity.hsv(0.5f, 1f, 1f));
        assertEquals(0xFFFFFF, ExSingularity.hsv(0.3f, 0f, 1f));
        assertEquals(0x000000, ExSingularity.hsv(0.3f, 1f, 0f));
        assertEquals(ExSingularity.hsv(0.25f, 0.8f, 1f), ExSingularity.hsv(-0.75f, 0.8f, 1f), "negative hues wrap");
        assertEquals(0xFFFFFF, ExSingularity.towardWhite(0x336699, 1f));
        assertEquals(0x336699, ExSingularity.towardWhite(0x336699, 0f));
    }

    @Test
    void textColourIsBoundedMovingFullSpectrumWithAGlint() {
        Set<Integer> seen = new HashSet<>();
        int lightest = 0;
        for (long ms = 0; ms < 12000L; ms += 41L) {
            for (int i = 0; i < 24; i++) {
                int c = ExSingularity.textRgb(i, 24, ms);
                assertEquals(c & 0xFFFFFF, c, "0xRRGGBB only");
                seen.add(c);
            }
        }
        assertTrue(seen.size() > 400, "the colours really change: " + seen.size());
        // at one instant the row shows most of the spectrum: at least 5 of the 6 hue sectors
        boolean[] sector = new boolean[6];
        for (int i = 0; i < 24; i++) sector[(int) Math.floor(hueOf(ExSingularity.textRgb(i, 24, 100L)) * 6) % 6] = true;
        int n = 0;
        for (boolean b : sector) if (b) n++;
        assertTrue(n >= 5, "the row spans the spectrum: " + n + " of 6 sectors");
        // the glint: somewhere along the row a glyph is much paler than the saturated rest
        for (long ms = 0; ms < 1600L; ms += 50L) for (int i = 0; i < 24; i++) lightest = Math.max(lightest, ExSingularity.textRgb(i, 24, ms) & 0xFF);
        assertTrue(lightest >= 200, "a white glint runs along the row");
        // brighter and more saturated than Genesis's pale rainbow (HSB saturation 0.35)
        double sat = 0;
        for (int i = 0; i < 24; i++) sat += satOf(ExSingularity.textRgb(i, 24, 3000L));
        assertTrue(sat / 24 > 0.6, "full-colour, not pastel: " + sat / 24);
    }

    @Test
    void glowWalksTheWholeSpectrumInTenSecondsAndRepeats() {
        assertEquals(ExSingularity.glowRgb(0f), ExSingularity.glowRgb(ExSingularity.GLOW_PERIOD_TICKS));
        boolean[] sector = new boolean[6];
        for (float t = 0; t < ExSingularity.GLOW_PERIOD_TICKS; t += 5f) sector[(int) Math.floor(hueOf(ExSingularity.glowRgb(t)) * 6) % 6] = true;
        for (boolean b : sector) assertTrue(b, "every hue sector is visited");
        assertEquals(10f, ExSingularity.GLOW_PERIOD_TICKS / 20f, 1e-6, "ten seconds a turn: slow, never a flash");
        // the veil: the spectrum along the blade, dim with a bright band running up it
        for (float t = 0; t < 600f; t += 11f) {
            Set<Integer> along = new HashSet<>();
            int brightest = 0, dimmest = 255;
            for (float y = 0f; y <= 1.0001f; y += 0.02f) {
                int c = ExSingularity.veilRgb(y, t);
                assertEquals(c & 0xFFFFFF, c);
                along.add(c);
                int v = Math.max(c >> 16 & 255, Math.max(c >> 8 & 255, c & 255));
                brightest = Math.max(brightest, v);
                dimmest = Math.min(dimmest, v);
            }
            assertTrue(along.size() > 25, "the veil is a gradient along the blade: " + along.size());
            assertTrue(dimmest <= 50 && dimmest >= 38, "dim base " + dimmest);
            assertTrue(brightest <= 170, "never white-hot: " + brightest);
        }
        float bestAt0 = -1, bestAt30 = -1;
        int vmax0 = 0, vmax30 = 0;
        for (float y = 0f; y <= 1f; y += 0.01f) {
            int a = ExSingularity.veilRgb(y, 0f), b = ExSingularity.veilRgb(y, 30f);
            int va = Math.max(a >> 16 & 255, Math.max(a >> 8 & 255, a & 255)), vb = Math.max(b >> 16 & 255, Math.max(b >> 8 & 255, b & 255));
            if (va > vmax0) { vmax0 = va; bestAt0 = y; }
            if (vb > vmax30) { vmax30 = vb; bestAt30 = y; }
        }
        assertTrue(bestAt30 > bestAt0 + 0.3f, "the bright band runs up the blade: " + bestAt0 + " -> " + bestAt30);
        for (float t = 0; t < 400f; t += 3f) {
            float p = ExSingularity.pulse(t);
            assertTrue(p >= 0.6f && p <= 1.0f, "pulse " + p);
        }
    }

    @Test
    void frameAndBorderColoursAreBounded() {
        Set<Integer> seen = new HashSet<>();
        for (long ms = 0; ms < 6000L; ms += 100L) {
            for (int k = 0; k < 8; k++) {
                int c = ExSingularity.frameRgb(k, 8, ms, false), d = ExSingularity.frameRgb(k, 8, ms, true);
                assertEquals(c & 0xFFFFFF, c);
                assertEquals(d & 0xFFFFFF, d);
                seen.add(c);
            }
            int a = ExSingularity.frameAlpha(false, ms), b = ExSingularity.frameAlpha(true, ms);
            assertTrue(a >= 200 && a <= 255 && b >= 100 && b <= 150, a + " " + b);
            assertEquals(ExSingularity.borderRgb(ms, false) & 0xFFFFFF, ExSingularity.borderRgb(ms, false));
            float tw = 0;
            for (int c = 0; c < 4; c++) { tw += ExSingularity.twinkle(c, ms) > 0.05f ? 1 : 0; }
            assertTrue(tw <= 1, "at most one corner twinkles at a time (the draw budget relies on it)");
        }
        assertTrue(seen.size() > 100);
        assertEquals(ExSingularity.frameRgb(3, 8, 0L, false), ExSingularity.frameRgb(3, 8, ExSingularity.FRAME_PERIOD_MS, false), "the frame loops");
        assertEquals(23, ExSingularity.FRAME_FILLS, "16 outer (6 + 6 pieces, 4 gradients) + 4 inner + 1 sheen + 2 twinkle");
        assertTrue(ExSingularity.FRAME_SLOTS_PER_FRAME <= 8);
    }

    @Test
    void shardsStayOnTheBlade() {
        float[] o = new float[4];
        for (int n : new int[]{1, 4, ExSingularity.MAX_SHARDS}) for (int i = 0; i < n; i++) for (float t = 0; t < 2000f; t += 7.3f) {
            ExSingularity.shard(i, n, t, 0.13f, o);
            assertTrue(Math.hypot(o[0], o[2]) <= 0.13f * 1.15f + 1e-4, "radius " + Math.hypot(o[0], o[2]));
            assertTrue(o[1] >= 0.12f - 1e-4 && o[1] <= 0.88f + 1e-4, "height " + o[1]);
            assertTrue(o[3] >= 0f && o[3] <= 1f);
        }
        float[] a = new float[4], b = new float[4];
        ExSingularity.shard(2, 8, 123f, 0.13f, a);
        ExSingularity.shard(2, 8, 123f, 0.13f, b);
        assertArrayEquals(a, b);
        ExSingularity.shard(2, 8, 124f, 0.13f, b);
        assertTrue(a[0] != b[0] || a[2] != b[2], "they orbit");
        assertEquals(8, ExSingularity.MAX_SHARDS, "32 quads: 8 shards x 2 planes x 2 faces");
    }

    // ---------------------------------------------------------------- Shishou's tooltip style
    @Test
    void shishouResolvesToTheAkashicRelicStyleAtEveryRarity() {
        assertTrue(ExSingularity.isAkashicPath("shishou"));
        assertFalse(ExSingularity.isAkashicPath("shishou_fragment"));
        assertFalse(ExSingularity.isAkashicPath(null));
        UnifiedTooltipRules.Hints h = new UnifiedTooltipRules.Hints("shishou", List.of("ShishouItem"), false, false, false, false, false);
        assertEquals(Category.AKASHIC_RELIC, UnifiedTooltipRules.classify(h));
        for (int rarity = 0; rarity < 5; rarity++) {
            UnifiedTooltipRules.Style s = UnifiedTooltipRules.style(h, rarity, null);
            assertEquals(Grade.AKASHIC, s.grade());
            assertTrue(s.akashic());
            assertTrue(s.banner().startsWith(ExSingularity.STARS));
            assertTrue(s.banner().endsWith("EX-Singularity Grade"));
            assertEquals("Type: Akashic Relic", s.typeLine());
        }
        // nothing else changes: an epic sword of another id is still Unique
        UnifiedTooltipRules.Hints other = new UnifiedTooltipRules.Hints("some_sword", List.of("SwordItem"), false, false, false, true, false);
        assertEquals(Grade.UNIQUE, UnifiedTooltipRules.style(other, 3, null).grade());
    }

    @Test
    void langFragmentAndTextureExist() throws IOException {
        String lang = Files.readString(Path.of("tools/lang/ex_singularity.json"));
        assertTrue(lang.contains("\"nusmp.ex_singularity.name\": \"EX-Singularity\""));
        assertTrue(lang.contains("Akashic Relic - Grade: EX-Singularity"));
        assertTrue(Files.readString(Path.of("tools/lang/unified_tooltips.json")).contains("\"tooltip.nusmp.unified.cat.akashic_relic\""));
        Path tex = Path.of("src/main/resources/assets/nusmp/textures/entity/weapon/ex_singularity_star.png");
        assertTrue(Files.exists(tex), "the star texture");
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(tex.toFile());
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
        int visible = 0;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            int argb = img.getRGB(x, y);
            int a = argb >>> 24, r = argb >> 16 & 255;
            if (a == 0) assertEquals(0, argb & 0xFFFFFF, "no colour under alpha at " + x + "," + y);
            else visible++;
            assertEquals(a, r, "premultiplied white: the colour equals the alpha");
        }
        assertTrue(visible > 400 && visible < 2400, "a star, not a card: " + visible);
        assertEquals(0, img.getRGB(0, 0) >>> 24, "the border is clear");
    }

    @Test
    void particleThemeExists() throws IOException {
        String themes = Files.readString(Path.of("src/main/resources/data/nusmp/particle_themes.json"));
        assertTrue(themes.contains("\"relic.grade.AKASHIC\""));
        assertTrue(Files.readString(Path.of("tools/particles/gen_themes.py")).contains("\"relic.grade.AKASHIC\""));
    }

    // ---------------------------------------------------------------- the wiring
    @Test
    void theHooksAreWired() throws IOException {
        String wr = Files.readString(SRC.resolve("client/WeaponRenderer.java"));
        assertTrue(wr.contains("ExSingularityFx.glowRgb(time)") && wr.contains("ExSingularityFx.extras(pose, buffers, mesh, time)"), "the hand");
        String sd = Files.readString(SRC.resolve("client/ShishouMeshDraw.java"));
        assertTrue(sd.contains("ExSingularityFx.glowRgb(time)") && sd.contains("ExSingularityFx.extras(pose, buffers, mesh, time)"), "the world body");
        assertTrue(Files.readString(SRC.resolve("client/relics/RelicClient.java")).contains("ExSingularityClient.init(modBus)"));
        assertTrue(Files.readString(SRC.resolve("relics/RelicSystem.java")).contains("ExSingularitySelfTests.register()"));
        String ut = Files.readString(SRC.resolve("client/ui/tooltip/UnifiedTooltip.java"));
        assertTrue(ut.contains("HEAD_PRISM") && ut.contains("PRISM_LINE") && ut.contains("PrismText.name("));
    }

    // ---------------------------------------------------------------- cheap: nothing allocated per frame
    @Test
    void thePureClassAllocatesNothing() throws IOException {
        String text = strip(Files.readString(SRC.resolve("relics/ExSingularity.java")));
        for (String bad : new String[]{" new ", "String.format", ".stream(", "ArrayList", "HashMap", "List.of", "StringBuilder", "java.awt"})
            assertFalse(text.replace("Set.of(", "").contains(bad), "ExSingularity must not use " + bad);
    }

    @Test
    void theDrawPathsAllocateNothing() throws IOException {
        String fx = strip(Files.readString(SRC.resolve("client/ExSingularityFx.java")));
        for (String m : new String[]{"extras", "veil", "shards", "plane", "vert"}) assertNoAlloc("ExSingularityFx." + m, body(fx, m));
        String cl = strip(Files.readString(SRC.resolve("client/ex/ExSingularityClient.java")));
        for (String m : new String[]{"frame", "paint", "onClientTick", "onEntityTick", "mote"}) assertNoAlloc("ExSingularityClient." + m, body(cl, m));
        String pt = strip(Files.readString(SRC.resolve("client/ui/tooltip/PrismText.java")));
        assertNoAlloc("PrismText.draw", body(pt, "draw"));
        for (String f : new String[]{fx, cl, pt}) {
            assertFalse(f.contains("String.format"));
            assertFalse(f.contains("java.awt"));
            assertFalse(f.contains("Minecraft.getInstance().level.getEntities"), "no per-tick world scan");
        }
    }

    private static void assertNoAlloc(String what, String body) {
        assertFalse(body.isEmpty(), what + " was not found");
        assertFalse(body.contains(" new ") || body.contains("(new ") || body.contains("=new "), what + " allocates");
        assertFalse(body.contains(".stream(") || body.contains("List.of") || body.contains("String.valueOf") || body.contains("Integer.toString") || body.contains("+ \"") || body.contains("\" +"), what + " builds objects or strings");
    }

    /** The text of the first method named {@code name} (declaration to its closing brace); empty if there is none. */
    static String body(String src, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^\\s*(?:public |private |protected |static |final )*[\\w<>\\[\\], .?]+\\s+" + name + "\\s*\\([^)]*\\)\\s*(?:throws [\\w, ]+)?\\{").matcher(src);
        if (!m.find()) return "";
        int i = m.end(), depth = 1;
        while (i < src.length() && depth > 0) {
            char c = src.charAt(i++);
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        return src.substring(m.start(), i);
    }

    /** The source without comments and string-free of line comments, so a scan only sees code. */
    static String strip(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    private static double hueOf(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        if (d < 1e-6) return 0;
        double h = max == r ? ((g - b) / d) % 6 : max == g ? (b - r) / d + 2 : (r - g) / d + 4;
        h /= 6;
        return h < 0 ? h + 1 : h;
    }

    private static double satOf(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        return max < 1e-6 ? 0 : (max - min) / max;
    }
}
