package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the late-game scaling (3.90) must reach a hit once and only once, and must be a SERVER setting, and nothing here can be played, so it is pinned by reading the source: the factor is multiplied
 * in exactly one place (BalanceLaw.damage with a caster), no other file calls it, the grimoire buff is applied once in each of GrimoireBook.hurtAs / hurtDirect, the settings live in the SERVER config,
 * the pure curve names no game class, and the self-test prefix is in the smoke list before boss.stats. Pure (file reads); the working directory is the project root.
 */
class LateGameScalingSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    @Test
    void theFactorIsAppliedOnceAtTheChokePoint() throws IOException {
        String law = code(SRC.resolve("balance/BalanceLaw.java"));
        assertEquals(2, count(law, "lateGameScale("), "the definition and one use");
        assertTrue(law.contains("raw * epScale(caster) * lateGameScale(caster) *"), "multiplied into BalanceLaw.damage(caster, ...)");
        assertTrue(law.contains("return cap(target, (float) r);"), "players still end at the pvp hit cap");
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (f.endsWith("BalanceLaw.java") || f.endsWith("ScalingSelfTests.java")) continue;
                assertFalse(code(f).contains("lateGameScale("), f + " applies the late-game scaling a second time");
            }
        }
    }

    @Test
    void theGrimoireBuffIsAppliedOncePerHitPath() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        assertEquals(2, count(gb, "r *= BalanceLaw.grimoireMult();"), "hurtAs and hurtDirect");
        assertTrue(gb.contains("BalanceLaw.damage(caster, target, r, masteryFrac(i))"), "then the balance law and its caps");
    }

    @Test
    void theSettingsAreServerSettingsAndTheCurveIsPure() throws IOException {
        String cfg = code(SRC.resolve("NUConfig.java"));
        for (String k : new String[]{"\"lateGameScaling\", true", "\"lateGameScalingBaseline\"", "\"lateGameScalingCap\", 6.0", "\"grimoireDamageMultiplier\", 1.75"}) assertTrue(cfg.contains(k), k);
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("registerConfig(ModConfig.Type.SERVER, NUConfig.SPEC)"), "NUConfig is the SERVER config");
        String curve = code(SRC.resolve("balance/LateGameScaling.java"));
        assertFalse(curve.contains("net.minecraft") || curve.contains("net.neoforged"), "the curve is pure");
    }

    @Test
    void theSelfTestIsRegisteredAndExpected() throws IOException {
        assertTrue(code(SRC.resolve("balance/ScalingSelfTests.java")).contains("\"scaling.rules\""));
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("ScalingSelfTests.register()"));
        assertTrue(Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8).contains(",scaling,boss.stats,tier\""));
    }
}
