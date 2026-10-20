package com.newuniverse.nusmp.particle.rukh;

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
 * Why: the owner's rule ("no vanilla particles from relics, equipment or active abilities") must not rot after the purge, so CI enforces it:
 * every Java file in the scope of {@code docs/rukh/scope.txt} (the same file {@code tools/rukh/purge.py} reads, so the script and the test
 * cannot disagree) is scanned for vanilla particle types, Dust/Item/Block particle options and for addParticle/sendParticles calls whose first
 * argument is not a variable or a Rukh type. A deliberate exception carries {@code // rukh-allow: <reason>} on its line. While the purge is
 * still running the enforcing part is switched off: it only runs when the marker file {@code docs/rukh/purge_enforced} exists (the lead
 * creates it when the purge is done) or {@code -Dnusmp.rukh.enforce=true} is given, so CI stays green meanwhile. The scope-file parsing and the
 * glob rules are tested unconditionally.
 * <p>
 * 1.103.0: the purge itself stands (the scope still spawns no vanilla particle), but the allowed Rukh scope shrank: the Rukh now belong to the Djinn users and the dungeons ONLY, and every
 * other effect in the purge scope (the relic waves, the worn relic FX, the magics, the songs, the skills, the corpses, the grimoires, the sword draws, the hero weapons) spawns the mod's own
 * custom particles through {@code particle.fx.Fx} instead. {@link RukhExclusiveScopeTest} enforces that; {@code docs/rukh/exclusive.md} says why; the call-pattern test below is extended to accept
 * the facade, which is not a raw {@code sendParticles}/{@code addParticle} call.
 */
class RukhPurgeScopeTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path SCOPE = ROOT.resolve("docs/rukh/scope.txt");

    private static final Pattern BAD = Pattern.compile("ParticleTypes\\.|DustParticleOptions|new\\s+DustColorTransitionOptions|ItemParticleOption|BlockParticleOption");
    // first argument of addParticle( / sendParticles( must be a plain variable (then a comma) or a RukhParticles type
    private static final Pattern CALL = Pattern.compile("\\b(?:addParticle|sendParticles)\\(\\s*(?!(?:$|[a-z_][A-Za-z0-9_]*\\s*,|(?:com\\.newuniverse\\.nusmp\\.particle\\.rukh\\.)?RukhParticles\\.))");

    /** Glob to regex: {@code **} crosses folders, {@code *} and {@code ?} stay in one, {@code [..]} / {@code [!..]} are classes. */
    static Pattern glob(String g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            if (c == '*') {
                if (i + 1 < g.length() && g.charAt(i + 1) == '*') {
                    i++;
                    if (i + 1 < g.length() && g.charAt(i + 1) == '/') {
                        i++;
                        sb.append("(?:.*/)?");
                    } else {
                        sb.append(".*");
                    }
                } else {
                    sb.append("[^/]*");
                }
            } else if (c == '?') {
                sb.append("[^/]");
            } else if (c == '[') {
                int j = g.indexOf(']', i);
                String cls = g.substring(i + 1, j);
                sb.append('[').append(cls.startsWith("!") ? "^" + cls.substring(1) : cls).append(']');
                i = j;
            } else if ("\\.^$|(){}+".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return Pattern.compile(sb.toString());
    }

    record Scope(List<Pattern> includes, List<Pattern> excludes) {
        boolean has(String rel) {
            return includes.stream().anyMatch(p -> p.matcher(rel).matches()) && excludes.stream().noneMatch(p -> p.matcher(rel).matches());
        }
    }

    static Scope parse(List<String> lines) {
        List<Pattern> inc = new ArrayList<>(), exc = new ArrayList<>();
        for (String raw : lines) {
            String s = raw.strip();
            if (s.isEmpty() || s.startsWith("#")) continue;
            s = s.replaceFirst("\\s+@p\\d+$", "");
            if (s.startsWith("!")) exc.add(glob(s.substring(1).strip()));
            else inc.add(glob(s));
        }
        return new Scope(inc, exc);
    }

    @Test
    void globsBehave() {
        assertTrue(glob("relics/**").matcher("relics/wave/Wave3.java").matches());
        assertTrue(glob("relics/**").matcher("relics/Relics.java").matches());
        assertFalse(glob("relics/*").matcher("relics/wave/Wave3.java").matches());
        assertTrue(glob("relics/wave/Wave9[!0-9]*").matcher("relics/wave/Wave9Acts.java").matches());
        assertFalse(glob("relics/wave/Wave9[!0-9]*").matcher("relics/wave/Wave90.java").matches());
        assertTrue(glob("relics/wave/Wave1[0-9]*").matcher("relics/wave/Wave12Acts.java").matches());
        assertFalse(glob("relics/wave/Wave1[0-9]*").matcher("relics/wave/Wave1.java").matches());
        assertTrue(glob("client/SwordDrawClient.java").matcher("client/SwordDrawClient.java").matches());
        assertFalse(glob("client/SwordDrawClient.java").matcher("client/SwordDrawClientX.java").matches());
    }

    @Test
    void theScopeFileKeepsTheTimeStandardAndTheBossesOut() throws IOException {
        Scope s = parse(Files.readAllLines(SCOPE, StandardCharsets.UTF_8));
        assertFalse(s.has("book/TimeBook.java"));
        assertTrue(s.has("book/MagicBook.java"));
        assertTrue(s.has("relics/wave/Wave3.java"));
        assertTrue(s.has("dungeon/nexus/relic/NexusRelic.java"));
        assertFalse(s.has("entity/zogratis/ZenonBoss.java"));
        assertFalse(s.has("boss/budget/BudgetedBoss.java"));
        assertFalse(s.has("dungeon/DungeonBlocks.java"));
        assertFalse(s.has("labyrinth/trial/TrialRegistry.java"));
        assertFalse(s.has("vfx/client/layer/TimeMagicLayer.java"));
    }

    @Test
    void theCallPatternAcceptsVariablesAndRukhAndRejectsEverythingElse() {
        assertFalse(CALL.matcher("level.sendParticles(type, x, y, z, 1, 0, 0, 0, 0);").find());
        assertFalse(CALL.matcher("level.addParticle(com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), x, y, z, 0, 0, 0);").find());
        assertFalse(CALL.matcher("level.sendParticles(player, type, true, x, y, z, 1, 0, 0, 0, 0);").find());
        assertTrue(CALL.matcher("level.addParticle(new DustParticleOptions(c, 1f), x, y, z, 0, 0, 0);").find());
        assertTrue(CALL.matcher("level.sendParticles(FX.get(), x, y, z, 1, 0, 0, 0, 0);").find());
        assertTrue(BAD.matcher("level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);").find());
    }

    @Test
    void theFacadeCallsOfTheCustomParticlesAreNotRawParticleCalls() {
        assertFalse(CALL.matcher("Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, 0, 0, 0, 0);").find());
        assertFalse(BAD.matcher("Fx.options(FxThemes.RELIC, FxTone.DARK)").find());
        assertTrue(Files.exists(ROOT.resolve("docs/rukh/exclusive.md")), "docs/rukh/exclusive.md (the 1.103.0 owner decision) is missing");
    }

    @Test
    void noScopedFileSpawnsAVanillaParticleOnceThePurgeIsEnforced() throws IOException {
        boolean enforced = Files.exists(ROOT.resolve("docs/rukh/purge_enforced")) || Boolean.getBoolean("nusmp.rukh.enforce");
        if (!enforced) {
            System.out.println("RukhPurgeScopeTest: purge not enforced yet (docs/rukh/purge_enforced missing); skipping the file scan");
            return;
        }
        Scope s = parse(Files.readAllLines(SCOPE, StandardCharsets.UTF_8));
        List<String> problems = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(f).toString().replace('\\', '/');
                if (!s.has(rel)) continue;
                List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String t = line.strip();
                    if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
                    if (line.contains("rukh-allow:")) continue;
                    if (BAD.matcher(line).find() || CALL.matcher(line).find()) problems.add(rel + ":" + (i + 1) + ": " + t);
                }
            }
        }
        assertTrue(problems.isEmpty(), "vanilla particles left in the Rukh purge scope:\n" + String.join("\n", problems));
    }
}
