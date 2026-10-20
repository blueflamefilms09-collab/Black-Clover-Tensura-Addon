package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the codemod moved the mod's call sites from vanilla particles to the facade; this keeps them moved. No source file outside the allow-list may use one of the vanilla particles
 * the map replaces (END_ROD above all, the old workhorse): not as a call, not as a value. The allow-list is the particle library itself (the generated {@code FxOld} and the facade's
 * fallbacks), the lanes the codemod skips until the other builders merged (they are removed from the list by running the codemod with {@code --include-lanes}), and a line carrying
 * {@code fx-allow: reason}. Data-carrying options (block, item, falling dust) are not in the map and are not checked. The facade itself must keep the budget calls the performance pass
 * asked for, and the two generated files must say that they are generated.
 */
class FxSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    /** Paths allowed to name a mapped vanilla particle. */
    private static final List<String> ALLOW = List.of("particle/fx/", "client/particle/fx/", "particle/rukh/", "client/particle/rukh/",
            "labyrinth/mega/rift/");   // the rift pockets: another builder's lane, rewritten by 'codemod.py --include-lanes' after it merges

    private static final Pattern LINE_COMMENT = Pattern.compile("^\\s*(//|\\*|/\\*)");

    private static List<String> mappedNames() throws IOException {
        List<String> out = new ArrayList<>();
        for (java.util.Map.Entry<String, JsonElement> e : JsonParser.parseString(Files.readString(ROOT.resolve("tools/particles/map.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("particles").entrySet())
            if (!e.getKey().equals("comment")) out.add(e.getKey());
        return out;
    }

    private static boolean allowed(String rel) {
        for (String a : ALLOW) if (rel.startsWith(a)) return true;
        return false;
    }

    @Test
    void noSourceOutsideTheAllowListUsesAMappedVanillaParticle() throws IOException {
        Pattern p = Pattern.compile("\\bParticleTypes\\s*\\.\\s*(" + String.join("|", mappedNames()) + ")\\b");
        List<String> problems = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(f).toString().replace('\\', '/');
                if (allowed(rel)) continue;
                List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (LINE_COMMENT.matcher(line).find() || line.contains("fx-allow:") || line.stripLeading().startsWith("import ")) continue;
                    if (p.matcher(line).find()) problems.add(rel + ":" + (i + 1) + ": " + line.strip());
                }
            }
        }
        assertTrue(problems.isEmpty(), "vanilla particles that the codemod replaces are back (run tools/particles/codemod.py):\n" + String.join("\n", problems));
    }

    @Test
    void endRodInParticularIsGoneFromTheCodemoddedPackages() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(f).toString().replace('\\', '/');
                if (allowed(rel)) continue;
                String text = Files.readString(f, StandardCharsets.UTF_8);
                for (String line : text.split("\n")) {
                    if (LINE_COMMENT.matcher(line).find() || line.contains("fx-allow:")) continue;
                    if (line.contains("ParticleTypes.END_ROD")) problems.add(rel);
                }
            }
        }
        assertTrue(problems.isEmpty(), "ParticleTypes.END_ROD in: " + problems);
    }

    @Test
    void theFacadeGoesThroughTheBudgetsAndTheQualityLevels() throws IOException {
        String fx = Files.readString(SRC.resolve("particle/fx/Fx.java"), StandardCharsets.UTF_8);
        assertTrue(fx.contains("PerfBudgets.particles("), "the server half of the facade lost its per-tick budget");
        assertTrue(fx.contains("PerfBudgets.clientParticles("), "the client half of the facade lost its per-tick budget");
        assertTrue(fx.contains("PerfSettings.commonQuality()") && fx.contains("PerfSettings.vfxQuality()"), "the facade no longer scales by the quality level");
        assertTrue(fx.contains("FxClientLod.distanceDetail("), "the client half no longer scales by the distance");
        assertTrue(fx.contains("FxConfig.serverCustom()") && fx.contains("FxConfig.clientCustom()"), "the facade ignores the particles.custom switches");
    }

    @Test
    void theGeneratedFilesAreMarkedAsGenerated() throws IOException {
        for (String f : new String[]{"particle/fx/FxOld.java", "particle/fx/FxThemes.java"})
            assertTrue(Files.readString(SRC.resolve(f), StandardCharsets.UTF_8).contains("GENERATED by tools/particles/codemod.py"), f);
    }

    @Test
    void theProvidersAllocateNothingPerParticleBeyondTheParticle() throws IOException {
        String client = Files.readString(SRC.resolve("client/particle/fx/FxClient.java"), StandardCharsets.UTF_8);
        int at = client.indexOf("createParticle(");
        assertTrue(at > 0);
        String body = client.substring(at, client.indexOf('}', at));
        assertEquals(1, body.split("new ", -1).length - 1, "createParticle must only build the particle: " + body);
        String particle = Files.readString(SRC.resolve("client/particle/fx/FxParticle.java"), StandardCharsets.UTF_8);
        String tick = particle.substring(particle.indexOf("public void tick()"), particle.indexOf("public float getQuadSize"));
        assertFalse(tick.contains("new "), "FxParticle.tick allocates");
    }
}
