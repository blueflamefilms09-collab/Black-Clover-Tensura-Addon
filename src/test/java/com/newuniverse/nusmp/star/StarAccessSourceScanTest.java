package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's report was "the new Star effects only show for Spade users". The cause was a server-side page gate, not the look; this test keeps the Spade / heir check OUT of the effect path for good: the
 * effect packet, the client layer, the prop painter, the VFX manager and the shape registry never name a kingdom, a cover or the heir mark, and inside the {@code star} package the kingdom is read in ONE place
 * ({@code StarGame.isHeir}), the heir mark is asked in exactly the places the doc lists, and every gate on a page goes through {@link StarAccess} and the {@code star.vfxForAll} setting.
 */
class StarAccessSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) {
        try {
            return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
        } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static final Pattern SPADE_OR_HEIR = Pattern.compile("Kingdom|SPADE|Spade|coverOf|GrimoireCover|GrimoirePages|isHeir|StarAccess|vfxForAll|SagaRewards");

    @Test
    void theEffectPathNeverNamesAKingdomACoverOrTheHeirMark() {
        for (String f : new String[]{"star/StarFx.java", "star/StarProps.java", "vfx/StarV2.java", "vfx/VfxSpawn.java", "vfx/VfxPayload.java", "vfx/VfxShape.java", "vfx/client/layer/StarMagicLayer.java",
                "vfx/client/StarV2Config.java", "vfx/client/VfxManager.java", "client/prop/StarPropPainter.java", "client/prop/PropPainterRegistry.java", "perf/PerfBudgets.java"}) {
            Matcher m = SPADE_OR_HEIR.matcher(code(f));
            boolean found = m.find();
            assertFalse(found, f + " is on the path of a Star effect and must not look at the kingdom, the cover or the heir mark: found '" + (found ? m.group() : "") + "'");
        }
        String fx = code("star/StarFx.java");
        assertTrue(fx.contains("VfxSpawn.send(level, new VfxPayload("), "every Star effect packet still goes through VfxSpawn (nearby players, the per-tick budget)");
        assertFalse(fx.contains("ServerPlayer"), "the packet does not depend on a player at all, only on the level and the points");
    }

    @Test
    void theKingdomIsReadInOnePlaceAndTheHeirMarkOnlyWhereTheDocSaysSo() throws IOException {
        List<String> kingdomReaders = new ArrayList<>();
        List<String> heirAskers = new ArrayList<>();
        try (Stream<Path> s = Files.walk(JAVA)) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java")).toList()) {
                String rel = JAVA.relativize(f).toString().replace('\\', '/');
                String c = code(rel);
                if (rel.startsWith("star/") && !rel.equals("star/StarGame.java") && Pattern.compile("Kingdom\\.|import [\\w.]*\\.Kingdom;|coverOf|\\.kingdom\\b").matcher(c).find()) kingdomReaders.add(rel);
                if (c.contains("StarGame.isHeir(")) heirAskers.add(rel);
            }
        }
        assertEquals(List.of(), kingdomReaders, "inside the star package only StarGame.isHeir reads the kingdom (StarAccess takes the kingdom's NAME as an argument for the tests)");
        heirAskers.sort(String::compareTo);
        assertEquals(List.of("star/StarCasts.java", "star/StarDispel.java", "star/StarPages.java"), heirAskers.stream().filter(x -> !x.equals("star/StarAccessSelfTests.java")).toList(),
                "the heir mark is asked by the casts (Black Neverland gate, the whip while the setting is off, the Hasta bonus), the dispel (the halved chance) and the page block, nowhere else");
        String game = code("star/StarGame.java");
        assertEquals(1, game.split("Kingdom\\.SPADE", -1).length - 1, "one Spade check in the whole mod's Star code");
        assertTrue(game.contains("GrimoirePages.coverOf(inst.get()).kingdom == Kingdom.SPADE"), "the heir facts stay: a Spade Star grimoire, the Raid, the mastery");
    }

    @Test
    void everyPageGateGoesThroughStarAccessAndTheSetting() {
        String casts = code("star/StarCasts.java");
        assertTrue(casts.contains("StarAccess.needsHeir(id, StarConfig.vfxForAll()) && !StarGame.isHeir(c.caster)"), "the cast gate");
        assertFalse(casts.contains("StarRules.needsHeir("), "no cast asks the 3.7.0 table directly any more");
        assertTrue(casts.contains("(StarConfig.vfxForAll() || StarGame.isHeir(p)) ? whip.cast(b, i, p, mode) : cone.cast(b, i, p, mode)"), "the whip for everybody, the cone otherwise");
        assertEquals(3, casts.split("isHeir\\(", -1).length - 1, "three heir questions in the casts: the gate, the whip switch, the Hasta bonus");
        assertTrue(casts.contains("c.book.masteryFrac(c.inst), StarGame.isHeir(p))"), "the Hasta's 1.15 stays the heir's");
        String pages = code("star/StarPages.java");
        assertTrue(pages.contains("StarRules.HEIR_PAGES.contains(page.id()) && StarAccess.needsHeir(page.id(), StarConfig.vfxForAll()) && !StarGame.isHeir(p)"), "the page block");
        String access = code("star/StarAccess.java");
        assertFalse(access.contains("net.minecraft") || access.contains("net.neoforged"), "StarAccess is pure");
        assertTrue(access.contains("ALWAYS_HEIR = Set.of(StarRules.BLACK_NEVERLAND)"), "Black Neverland stays heir-only");
        String boot = code("star/StarBoot.java");
        assertTrue(boot.contains("StarAccessSelfTests.register()") && code("star/StarAccessSelfTests.java").contains("\"magic.star_nodes_all\""), "the self-test key starts with the smoke prefix magic.star_nodes");
    }

    @Test
    void theHeirPerksAreStillTheHeirsAndNothingAboutStarIsGatedOnTheCoverAnywhereElse() {
        String game = code("star/StarGame.java");
        assertTrue(game.contains("StarConfig.perPlayer(heir)"), "the 8th star is the heir's");
        assertTrue(code("star/StarDispel.java").contains("StarDispelRules.chance(base"), "the halved dispel chance is the heir's");
        assertTrue(code("star/StarRules.java").contains("return heir ? raw * 1.15f : raw;"), "the Hasta bonus is the heir's");
        assertTrue(code("book/GrimoireBook.java").contains("StarPages.block(this, page(mode), p)"), "the page block hook is still there");
        for (String f : new String[]{"book/pages15/Pages15.java", "book/pages15/Batch1.java", "charset/CharacterSets.java", "charset/CharacterSetRegistry.java"}) {
            String c = code(f);
            for (String line : c.split("\n")) if (line.toLowerCase().contains("star")) assertFalse(line.contains("SPADE") || line.contains("Kingdom."), f + ": a Star line that looks at the kingdom: " + line.strip());
        }
    }
}
