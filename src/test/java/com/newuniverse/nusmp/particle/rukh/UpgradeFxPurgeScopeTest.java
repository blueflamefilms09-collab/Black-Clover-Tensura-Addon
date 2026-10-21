package com.newuniverse.nusmp.particle.rukh;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Since 1.108.0 the two classes of the God worn upgrade effects ({@code client/relics/fx/UpgradeFxClient} and {@code relics/upgrade/UpgradeActFx}) spawn the mod's own particles, so their two
 * exclusion lines are gone from {@code docs/rukh/scope.txt} and the purge scan ({@link RukhPurgeScopeTest}) covers them again. This pins both halves: the files are in the scope, and the scan
 * patterns find nothing in them.
 */
class UpgradeFxPurgeScopeTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String[] FILES = {"client/relics/fx/UpgradeFxClient.java", "relics/upgrade/UpgradeActFx.java"};
    // the two patterns of the scan, as RukhPurgeScopeTest has them
    private static final java.util.regex.Pattern BAD = java.util.regex.Pattern.compile("ParticleTypes\\.|DustParticleOptions|new\\s+DustColorTransitionOptions|ItemParticleOption|BlockParticleOption");
    private static final java.util.regex.Pattern CALL = java.util.regex.Pattern.compile("\\b(?:addParticle|sendParticles)\\(\\s*(?!(?:$|[a-z_][A-Za-z0-9_]*\\s*,|(?:com\\.newuniverse\\.nusmp\\.particle\\.rukh\\.)?RukhParticles\\.))");

    @Test
    void theTwoFilesAreInThePurgeScopeAgain() throws IOException {
        RukhPurgeScopeTest.Scope s = RukhPurgeScopeTest.parse(Files.readAllLines(ROOT.resolve("docs/rukh/scope.txt"), StandardCharsets.UTF_8));
        for (String f : FILES) assertTrue(s.has(f), f + " must be in the purge scope (its exclusion line was removed in 1.108.0)");
        assertTrue(Files.exists(ROOT.resolve("docs/rukh/purge_enforced")), "the purge is enforced, so the scan really runs over them");
    }

    @Test
    void theScanPatternsFindNothingInThem() throws IOException {
        for (String f : FILES) {
            for (String line : Files.readAllLines(ROOT.resolve("src/main/java/com/newuniverse/nusmp/" + f), StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*") || line.contains("rukh-allow:")) continue;
                assertFalse(BAD.matcher(line).find(), f + ": " + t);
                assertFalse(CALL.matcher(line).find(), f + ": " + t);
            }
        }
    }
}
