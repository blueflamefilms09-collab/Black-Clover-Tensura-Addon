package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "400 different mobs, 200 for each road" is the owner's promise, and the files that SHIP are the proof: this test reads {@code data/nusmp/mega/foundry/*.json} from the source
 * tree exactly as the loader does and runs the same pure check the admin command and the in-game self-test run ({@link FoundryVerify}): 200 + 200 mobs, 40 per tier per road, every
 * file parses with no problem, every ability and archetype named exists, unique names and ids, a source on every row, at least 30 sources and none above 8 percent. It also checks
 * the files against the asset index the client reads ({@code assets/nusmp/foundry/index.json}: every mob has an entry whose animation class file exists, and the model, texture and
 * glow files exist), so a mob that would be invisible in game fails the build instead. (The LOOK uniqueness of the 400 is checked by the generator's guard, tools/foundry/guard.py.)
 */
public class FoundryRosterTest {
    private static final Path DATA = Path.of("src/main/resources/data/nusmp/mega/foundry");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");

    private static FoundryLibrary library() throws IOException {
        return new FoundryLibrary(FoundryLibrary.parseAbilities(Files.readString(DATA.resolve("_abilities.json"))).rows(),
                FoundryLibrary.parseArchetypes(Files.readString(DATA.resolve("_archetypes.json"))).rows());
    }

    private static List<FoundryDef> defs(FoundryLibrary lib, List<String> problems) throws IOException {
        List<FoundryDef> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA)) {
            for (Path p : s.sorted().toList()) {
                String file = p.getFileName().toString();
                if (file.startsWith("_") || !file.endsWith(".json")) continue;
                FoundryDef.Parsed parsed = FoundryDef.parse(file, Files.readString(p), lib);
                problems.addAll(parsed.problems());
                if (parsed.def() != null) out.add(parsed.def());
                else problems.add(file + ": dropped");
                if (parsed.def() != null && !file.equals(parsed.def().id() + ".json")) problems.add(file + ": the file name is not the id " + parsed.def().id());
            }
        }
        return out;
    }

    @Test
    public void everyShippedMobFileParsesWithoutProblems() throws IOException {
        List<String> problems = new ArrayList<>();
        defs(library(), problems);
        assertEquals(List.of(), problems);
    }

    @Test
    public void theRosterKeepsTheOwnersPromise() throws IOException {
        FoundryLibrary lib = library();
        List<String> problems = new ArrayList<>();
        FoundryRoster roster = new FoundryRoster(defs(lib, problems));
        assertEquals(List.of(), FoundryVerify.check(roster, lib, true));
        assertEquals(200, roster.count(Polarity.HOLY));
        assertEquals(200, roster.count(Polarity.ABYSSAL));
        assertEquals(400, roster.size());
        for (Polarity p : Polarity.values()) for (int t = 1; t <= 5; t++) assertEquals(40, roster.of(p, t).size(), p.id() + " tier " + t);
    }

    @Test
    public void everyMobHasItsModelItsTexturesAndAnAnimationClassTheClientCanFind() throws IOException {
        List<String> problems = new ArrayList<>();
        List<FoundryDef> defs = defs(library(), problems);
        String index = Files.readString(ASSETS.resolve("foundry/index.json"));
        com.google.gson.JsonObject mobs = com.google.gson.JsonParser.parseString(index).getAsJsonObject().getAsJsonObject("mobs");
        Set<String> classes = new HashSet<>();
        for (FoundryDef d : defs) {
            assertTrue(mobs.has(d.id()), d.id() + " has no entry in the asset index");
            String anim = mobs.getAsJsonObject(d.id()).get("anim").getAsString();
            classes.add(anim);
            assertTrue(Files.exists(ASSETS.resolve("geo/entity/foundry_" + d.id() + ".geo.json")), d.id() + ": no model");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/foundry_" + d.id() + ".png")), d.id() + ": no texture");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/foundry_" + d.id() + "_glow.png")), d.id() + ": no glow texture");
            assertTrue(Files.exists(ASSETS.resolve("animations/entity/foundry_anim_" + anim + ".animation.json")), d.id() + ": the animation class " + anim + " has no file");
            assertEquals(mobs.getAsJsonObject(d.id()).get("tier").getAsInt(), d.tier(), d.id() + ": tier differs between the data and the index");
        }
        assertEquals(defs.size(), mobs.size(), "the index holds mobs the data does not know");
        assertTrue(classes.size() < defs.size() / 3 || defs.size() < 40, "the clips must be shared: " + classes.size() + " classes for " + defs.size() + " mobs");
    }

    @Test
    public void aBadDefinitionIsRefusedOrRepaired() {
        FoundryLibrary lib = new FoundryLibrary(java.util.Map.of("dash", FoundryAbility.clamped("dash", FoundryAbility.Kind.DASH, 100, 2, 9, 5, 1, 5, 0, 0, 0, 0, "road", "")),
                java.util.Map.of("brute", FoundryArchetype.FALLBACK));
        String ok = "{\"id\":\"x_one\",\"name\":\"X\",\"road\":\"holy\",\"tier\":2,\"faction\":\"undead\",\"archetype\":\"brute\",\"abilities\":[\"dash\"],\"size\":1.1,\"height\":2.0,\"source\":\"S\",\"glow\":\"#ff0000\"}";
        assertEquals(null, FoundryDef.parse("a", ok.replace("\"holy\"", "\"sideways\""), lib).def());
        assertEquals(null, FoundryDef.parse("a", ok.replace("\"tier\":2", "\"tier\":9"), lib).def());
        assertEquals(null, FoundryDef.parse("a", ok.replace("undead", "plasma"), lib).def());
        assertEquals(null, FoundryDef.parse("a", ok.replace("[\"dash\"]", "[\"nope\"]"), lib).def());
        assertEquals(null, FoundryDef.parse("a", "{oops", lib).def());
        assertEquals(null, FoundryDef.parse("a", ok.replace("x_one", "Bad Id"), lib).def());
        FoundryDef.Parsed fine = FoundryDef.parse("a", ok, lib);
        assertEquals(List.of(), fine.problems());
        assertEquals(0xFF0000, fine.def().glow());
        FoundryDef.Parsed big = FoundryDef.parse("a", ok.replace("\"size\":1.1", "\"size\":9"), lib);
        assertEquals(2.0, big.def().size(), 1e-9);
        assertTrue(big.problems().size() == 1);
        FoundryDef.Parsed ghost = FoundryDef.parse("a", ok.replace("[\"dash\"]", "[\"dash\",\"ghost\"]"), lib);
        assertEquals(List.of("dash"), ghost.def().abilities());
        assertTrue(ghost.problems().size() == 1);
    }

    @Test
    public void theVerifierSeesWhatIsWrong() {
        FoundryLibrary lib = new FoundryLibrary(java.util.Map.of("dash", FoundryAbility.clamped("dash", FoundryAbility.Kind.DASH, 100, 2, 9, 5, 1, 5, 0, 0, 0, 0, "road", ""),
                "slam", FoundryAbility.clamped("slam", FoundryAbility.Kind.SLAM, 100, 0, 5, 5, 1, 5, 0, 3, 0, 0, "road", "")), java.util.Map.of("brute", FoundryArchetype.FALLBACK));
        FoundryDef a = new FoundryDef("a", "Same", Polarity.HOLY, 1, "undead", "brute", List.of("dash", "slam"), 1.0, 2.0, "S", 0xFFFFFF);
        FoundryDef b = new FoundryDef("b", "same", Polarity.HOLY, 1, "undead", "brute", List.of("dash"), 1.0, 2.0, "", 0xFFFFFF);
        List<String> bad = FoundryVerify.check(new FoundryRoster(List.of(a, b)), lib, false);
        assertTrue(bad.stream().anyMatch(s -> s.contains("are called")), bad.toString());
        assertTrue(bad.stream().anyMatch(s -> s.contains("abilities, 2 or 3")), bad.toString());
        assertTrue(bad.stream().anyMatch(s -> s.contains("no source")), bad.toString());
        assertTrue(bad.stream().anyMatch(s -> s.contains("missing from the library")), bad.toString());
        assertTrue(FoundryVerify.check(new FoundryRoster(List.of(a)), lib, true).stream().anyMatch(s -> s.contains("needs 200")));
    }
}
