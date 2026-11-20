package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: nothing of the real arrows can be profiled or played here, so the rules that keep them cheap, server-safe and optional are pinned by reading the
 * source (comments and strings aside): the arrow entity extends the vanilla arrow, is never saved, never picked up, gives its place in the budget back
 * on every removal, ends by itself, checks its owner on a timer, and its per-tick method does no world scan and loads no chunk; the budget is taken before an
 * entity is made; every hitscan path that real arrows replace is still there behind the setting {@code heroweapon.bow.realArrows}; the setting is a server
 * config that defaults to on; the bow package names no client class and no hunger. If a rule is replaced by a better one on purpose, change the pin in the
 * same commit and say why in docs/heroweapon/bow_arrows.md.
 */
class BowArrowsSourceScanTest {
    private static final Path BOW = Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/weapon/bow");

    private static String raw(String file) throws IOException { return Files.readString(BOW.resolve(file)); }

    /** The source without comments and string literals, so a word in a doc comment never counts. */
    private static String code(String file) throws IOException {
        String s = raw(file);
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
        return s.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    /** The body of the first method with this header text, braces matched. */
    private static String body(String src, String header) {
        int h = src.indexOf(header);
        assertTrue(h >= 0, "no method " + header);
        int open = src.indexOf('{', h), depth = 0;
        for (int i = open; i < src.length(); i++) {
            if (src.charAt(i) == '{') depth++;
            else if (src.charAt(i) == '}' && --depth == 0) return src.substring(open, i + 1);
        }
        throw new AssertionError("unbalanced " + header);
    }

    @Test
    void theArrowExtendsTheVanillaArrowIsNeverSavedNorPickedUp() throws IOException {
        String arrow = code("HeroArrow.java"), reg = code("HeroArrowEntities.java");
        assertTrue(arrow.contains("class HeroArrow extends AbstractArrow"));
        assertTrue(reg.contains(".noSave()"), "a hero arrow is never saved with the world");
        assertTrue(reg.contains(".noSummon()"));
        assertTrue(reg.contains("clientTrackingRange(4)") && reg.contains("updateInterval(20)"), "tracked like the vanilla arrow: only nearby players are told");
        assertTrue(arrow.contains("Pickup.DISALLOWED"));
        assertTrue(body(arrow, "boolean tryPickup(").contains("return false"));
        assertTrue(arrow.contains("ItemStack.EMPTY"), "nothing to drop or pick up");
        assertTrue(!arrow.contains("spawnAtLocation") && !arrow.contains("ItemEntity"), "a hero arrow drops nothing");
    }

    @Test
    void theArrowGivesItsPlaceBackOnEveryRemovalAndEndsByItself() throws IOException {
        String arrow = code("HeroArrow.java");
        assertTrue(body(arrow, "void onRemovedFromLevel()").contains("releaseBudget()"));
        assertTrue(body(arrow, "void remove(").contains("releaseBudget()"));
        assertTrue(body(arrow, "void releaseBudget()").contains("if (!reserved) return"), "a place is given back once");
        String tick = body(arrow, "public void tick()");
        assertTrue(tick.contains("maxLife") && tick.contains("linger"), "a flight and a stuck time are both bounded");
        assertTrue(tick.contains("ownerLive()"), "an owner who died, left or changed world takes the arrows away");
        assertTrue(tick.contains("tickCount % 5"), "the owner check is on a timer, not every tick");
        assertTrue(tick.contains("ctx == null"), "an unconfigured (summoned) arrow is discarded");
    }

    @Test
    void thePerTickMethodsScanNothingAndLoadNoChunk() throws IOException {
        String arrow = code("HeroArrow.java");
        for (String m : List.of("public void tick()", "void steer()")) {
            String b = body(arrow, m);
            for (String banned : List.of("getEntit", "Foes.", "around(", "along(", "hasChunk", "getChunk", "forceLoad", "ChunkPos", "getBlockStates", "clip(", "getNearest"))
                assertTrue(!b.contains(banned), m + " must not use " + banned);
        }
        assertTrue(arrow.contains("tickCount % 2"), "the seeker steers every other tick");
    }

    @Test
    void theBudgetIsTakenBeforeAnEntityIsMade() throws IOException {
        String fire = body(code("BowArrows.java"), "static HeroArrow fire(");
        int acquire = fire.indexOf("tryAcquire"), create = fire.indexOf(".create(");
        assertTrue(acquire >= 0 && create > acquire, "ArrowBudget.tryAcquire must come before create()");
        assertTrue(fire.contains("HeroConfig.realArrows()"));
        assertTrue(fire.contains("releaseBudget()"), "an arrow the world refused gives its place back");
        assertTrue(fire.contains("bowArrowsPerPlayer()") && fire.contains("bowArrowsGlobal()"));
    }

    @Test
    void everyHitscanPathARealArrowReplacesIsStillThereBehindTheSetting() throws IOException {
        String shots = code("BowShots.java"), rain = code("BowRain.java"), direct = code("BowDirect.java");
        assertTrue(shots.contains("HeroConfig.realArrows() && BowArrows.release("));
        assertTrue(shots.contains("static void sniper(") || shots.contains("private static void sniper("), "the hitscan sniper arrow is kept");
        for (String old : List.of("Foes.along(", "private static void net(", "private static void mark(", "private static void burst(", "private static void seek(", "private static void command(", "private static void second("))
            assertTrue(shots.contains(old), "BowShots lost " + old);
        assertTrue(rain.contains("HeroConfig.realArrows()") && rain.contains("BowArrows.rainWave("));
        assertTrue(rain.contains("VfxShape.HERO_ARROW, centre.add(pt[0], 16.0"), "the old instant wave is kept");
        assertTrue(direct.contains("HeroConfig.realArrows()") && direct.contains("BowArrows.squall("));
        assertTrue(direct.contains("hits.add(in.get(0), (float) BowNumbers.SQUALL_PER_ARROW)"), "the old instant fan is kept");
    }

    @Test
    void theMeteorTechniqueGoesThroughOneSeam() throws IOException {
        String skills = code("BowSkills.java");
        assertTrue(skills.contains("BowNumbers.METEOR_BOW, BowSkills::meteor"));
        assertTrue(body(skills, "static boolean meteor(").contains("BowRain.meteor(c)"));
        assertEquals(1, count(skills, "BowRain\\.meteor\\("), "BowRain.meteor is called from the seam only");
    }

    private static int count(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    @Test
    void theSettingsAreServerConfigsAndTheDefaultIsOn() throws IOException {
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/HeroConfig.java"));
        assertTrue(cfg.contains("define(\"realArrows\", true)"), "the real arrows are on by default");
        assertTrue(cfg.contains("\"maxArrowsPerPlayer\", 24") && cfg.contains("\"maxArrowsGlobal\", 256") && cfg.contains("\"magicShareScale\", 1.0"));
        assertTrue(cfg.contains("b.push(\"bow\")"), "the section is heroweapon.bow");
        String core = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/heroweapon/HeroCore.java"));
        assertTrue(core.contains("ModConfig.Type.SERVER, HeroConfig.SPEC"), "HeroConfig is a SERVER config");
        String nusmp = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        assertTrue(nusmp.contains("HeroArrowEntities.ENTITIES.register(modEventBus)"));
        assertTrue(nusmp.contains("BowArrowClient::register"));
        assertTrue(nusmp.contains("ArrowBudget.clear()"), "a stopped server forgets the counts");
    }

    @Test
    void theArrowIsInTheArrowsTagHasALangNameAndASelfTest() throws IOException {
        String tag = Files.readString(Path.of("src/main/resources/data/minecraft/tags/entity_type/arrows.json"));
        assertTrue(tag.contains("anime_multiverse:hero_arrow") && tag.contains("\"replace\": false"));
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        assertTrue(lang.contains("\"entity.anime_multiverse.hero_arrow\""));
        assertTrue(raw("BowLogic.java").contains("\"hero.bow.arrows\""));
        assertTrue(raw("HeroArrowEntities.java").contains("\"hero_arrow\""));
    }

    @Test
    void theBowPackageNamesNoClientClassAndNoHunger() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.list(BOW)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(p.getFileName().toString());
                if (c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance")) bad.add(p + " names a client class");
                if (c.contains("HUNGER") || c.contains("FoodData") || c.contains("causeFoodExhaustion") || c.contains("addExhaustion")) bad.add(p + " touches hunger");
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theRendererIsClientOnlyCullsByDistanceAndGoesThroughSafeAdditive() throws IOException {
        String r = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/heroweapon/bow/HeroArrowRenderer.java"));
        assertTrue(r.contains("extends ArrowRenderer<HeroArrow>"), "the vanilla arrow renderer is reused");
        assertTrue(r.contains("SafeAdditive.additive(") && !r.contains("RenderType.eyes("));
        assertTrue(r.contains("TRAIL_DISTANCE_SQ") && r.contains("speed < 0.3"), "far or stuck arrows draw no trail");
        String render = body(r, "public void render(");
        assertTrue(!render.contains(" new ") || !render.contains("new Vec3"), "no allocation per frame in render()");
    }

    @Test
    void theTexturePainterAndItsTableAgree() throws IOException {
        String py = Files.readString(Path.of("tools/gen_bow_arrow_textures.py"));
        for (String id : BowArrowForms.ids()) assertTrue(py.contains("\"" + id + "\""), "tools/gen_bow_arrow_textures.py does not paint " + id);
    }
}
