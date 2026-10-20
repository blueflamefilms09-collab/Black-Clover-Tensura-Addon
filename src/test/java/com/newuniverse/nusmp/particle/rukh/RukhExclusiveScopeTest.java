package com.newuniverse.nusmp.particle.rukh;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: since 1.103.0 the Rukh (the magoi) are exclusive: they belong to Djinn users and to the dungeons, and every other effect (relic waves, relic worn FX, magics, songs, skills, corpses,
 * grimoires, sword draws, hero weapons, bosses outside the dungeons) has its own custom particles. This test pins that. {@code docs/rukh/exclusive_scope.txt} lists the owners; the codemod's
 * map ({@code tools/particles/map.json "rukh_keep"}) must list exactly the same paths, so the script and the test cannot disagree (the same arrangement as {@code scope.txt} and the purge).
 * The scan then looks for any use of {@code RukhFx}, {@code RukhClientFx} or {@code RukhParticles} outside the owners and the infrastructure that registers and falls back to them.
 * Files in the lanes other builders were working in when the codemod ran ({@code lanes.relics} and {@code lanes.other} of the map) are tolerated until the marker file
 * {@code docs/rukh/exclusive_enforced} exists (the lead creates it after running the codemod with {@code --include-relics --include-lanes}) or {@code -Dnusmp.rukh.exclusive=true} is given.
 */
class RukhExclusiveScopeTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Pattern USE = Pattern.compile("\\b(RukhFx|RukhClientFx|RukhParticles)\\b");
    private static final List<String> INFRA = List.of("particle/rukh/**", "client/particle/rukh/**", "particle/fx/**", "NUSMP.java");

    private static List<String> owners() throws IOException {
        List<String> out = new ArrayList<>();
        for (String raw : Files.readAllLines(ROOT.resolve("docs/rukh/exclusive_scope.txt"), StandardCharsets.UTF_8)) {
            String s = raw.strip();
            if (!s.isEmpty() && !s.startsWith("#")) out.add(s);
        }
        return out;
    }

    private static boolean any(List<String> globs, String rel) {
        for (String g : globs) if (RukhPurgeScopeTest.glob(g).matcher(rel).matches()) return true;
        return false;
    }

    private static List<String> mapList(String section, String key) throws IOException {
        JsonElement e = JsonParser.parseString(Files.readString(ROOT.resolve("tools/particles/map.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(section).get(key);
        List<String> out = new ArrayList<>();
        e.getAsJsonArray().forEach(x -> out.add(x.getAsString()));
        return out;
    }

    @Test
    void theOwnersAreExactlyTheDjinnAndTheDungeons() throws IOException {
        List<String> o = owners();
        assertEquals(new LinkedHashSet<>(o), new LinkedHashSet<>(mapList("rukh_keep", "globs")), "docs/rukh/exclusive_scope.txt and tools/particles/map.json (rukh_keep) must list the same owners");
        for (String s : o) {
            boolean djinn = s.contains("djinn") || s.equals("relics/Fx.java");
            boolean dungeon = s.startsWith("labyrinth/") || s.startsWith("client/labyrinth/") || s.startsWith("dungeon/") || s.startsWith("client/dungeon/");
            assertTrue(djinn || dungeon, "an owner that is neither the Djinn nor a dungeon: " + s);
        }
        assertFalse(any(o, "relics/wave/Wave3.java"), "the relic waves are not owners");
        assertFalse(any(o, "client/relics/RelicGradeFx.java"), "the relic worn FX are not owners");
        assertFalse(any(o, "skill/GodTier.java"), "skills are not owners");
        assertFalse(any(o, "blackclover/MagicType.java"), "the magics are not owners");
        assertTrue(any(o, "relics/djinn/DjinnMagic.java") && any(o, "client/relics/djinn/DjinnLook.java") && any(o, "labyrinth/mega/flow/FlowCasts.java"));
    }

    @Test
    void noRukhTypeIsUsedOutsideTheDjinnAndTheDungeons() throws IOException {
        boolean enforced = Files.exists(ROOT.resolve("docs/rukh/exclusive_enforced")) || Boolean.getBoolean("nusmp.rukh.exclusive");
        List<String> owners = owners();
        List<String> lanes = new ArrayList<>(mapList("lanes", "relics"));
        lanes.addAll(mapList("lanes", "other"));
        List<String> problems = new ArrayList<>();
        List<String> tolerated = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SRC)) {
            for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(f).toString().replace('\\', '/');
                if (any(owners, rel) || any(INFRA, rel)) continue;
                List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String t = lines.get(i).strip();
                    if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*") || lines.get(i).contains("rukh-allow:")) continue;
                    if (USE.matcher(lines.get(i)).find()) (any(lanes, rel) ? tolerated : problems).add(rel + ":" + (i + 1) + ": " + t);
                }
            }
        }
        if (enforced) problems.addAll(tolerated);
        else if (!tolerated.isEmpty()) System.out.println("RukhExclusiveScopeTest: " + tolerated.size() + " Rukh uses in the lanes of other builders are tolerated until docs/rukh/exclusive_enforced exists (run tools/particles/codemod.py --include-relics --include-lanes)");
        assertTrue(problems.isEmpty(), "the Rukh are for the Djinn and the dungeons only; these call sites must use the custom particles (tools/particles/codemod.py):\n" + String.join("\n", problems));
    }

    @Test
    void theDjinnAndTheDungeonsStillUseTheirRukh() throws IOException {
        for (String dir : new String[]{"relics/djinn", "labyrinth"}) {
            int uses = 0;
            try (Stream<Path> walk = Files.walk(SRC.resolve(dir))) {
                for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    if (USE.matcher(Files.readString(f, StandardCharsets.UTF_8)).find()) uses++;
                }
            }
            assertTrue(uses >= 3, dir + " lost its Rukh (" + uses + " files left): the owner wants the Djinn and the dungeons to keep the magoi look");
        }
    }

    @Test
    void theRukhTypesAndTheirParticleJsonsAreStillRegistered() throws IOException {
        String reg = Files.readString(SRC.resolve("particle/rukh/RukhParticles.java"), StandardCharsets.UTF_8);
        assertTrue(reg.contains("\"dark_rukh\"") && reg.contains("\"light_rukh\""));
        assertTrue(Files.exists(ROOT.resolve("src/main/resources/assets/nusmp/particles/dark_rukh.json")));
        assertTrue(Files.exists(ROOT.resolve("src/main/resources/assets/nusmp/particles/light_rukh.json")));
    }
}
