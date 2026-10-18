package com.newuniverse.nusmp.client.remodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a remodelled mob that has no model, no texture, no clips or no renderer mapping is drawn as nothing (the geo loader only logs it), and nobody sees that until a boss walks out invisible.
 * This guard reads the files (no game needed) and fails when: an entity of {@link RemodelTable} is not a registered entity, a model of the table lacks its geometry, animations, texture or glow
 * layer (or its second form, or a skin), a model lacks one of the clips the pose clock plays, a model with a special state lacks its clip (sleep, sit, read, beam, bow, spawn), the numbers of
 * {@link RemodelLooks} drifted from tools/arc/manifest.json, the kit reported a problem for a model, a character file declares an entity the table does not know, a model of the table has no
 * character file, a family has no renderer case or an entity has no look or probe in {@code RemodelRenderers}, or a mob that is meant to look like the player was remodelled.
 */
class RemodelTableTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";
    /** The clips the pose clock plays for every model (ZogratisPose). */
    private static final List<String> CLIPS = List.of("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death");

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static boolean has(String path) { return new File(path).isFile(); }

    @Test
    void everyEntityIdIsUniqueAndRegistered() throws Exception {
        Set<String> seen = new HashSet<>();
        String sources = allSources();
        for (RemodelTable.Entry en : RemodelTable.ENTRIES) {
            assertTrue(seen.add(en.entityId()), "entity listed twice: " + en.entityId());
            assertFalse(en.models().isEmpty(), en.entityId() + " has no model");
            assertTrue(sources.contains("register(\"" + en.entityId() + "\""), "nusmp:" + en.entityId() + " is not registered anywhere in src/main/java (a stale id in RemodelTable?)");
        }
    }

    @Test
    void theMobsMeantToLookLikeThePlayerWereLeftAlone() {
        for (String id : RemodelTable.LEFT_AS_THEY_ARE) assertNull(RemodelTable.of(id), id + " looks like the player on purpose and must not be remodelled");
    }

    @Test
    void everyModelHasItsFilesItsSecondFormAndItsSkins() {
        for (String model : RemodelTable.withSecondForms()) {
            String stem = RemodelTable.stem(model);
            assertTrue(has(ASSETS + "geo/entity/" + stem + ".geo.json"), "missing geometry of " + model + ": run python3 -B tools/gen_arc_characters.py " + model);
            assertTrue(has(ASSETS + "animations/entity/" + stem + ".animation.json"), "missing animations of " + model);
            assertTrue(has(ASSETS + "textures/entity/" + stem + ".png"), "missing texture of " + model);
            assertTrue(has(ASSETS + "textures/entity/" + stem + "_glow.png"), "missing glow layer of " + model);
        }
        for (var skins : RemodelTable.SKINS.entrySet()) {
            assertTrue(RemodelTable.models().contains(skins.getKey()), "skins of a model the table does not draw: " + skins.getKey());
            for (String skin : skins.getValue()) {
                String stem = RemodelTable.skinStem(skins.getKey(), skin);
                assertTrue(has(ASSETS + "textures/entity/" + stem + ".png"), "missing skin texture " + stem + ": run python3 -B tools/remodel/gen_remodel.py " + skins.getKey());
                assertTrue(has(ASSETS + "textures/entity/" + stem + "_glow.png"), "missing skin glow layer " + stem);
            }
        }
    }

    @Test
    void everyModelHasTheClipsThePoseClockPlaysAndItsOwn() throws Exception {
        for (String model : RemodelTable.withSecondForms()) {
            JsonObject clips = json(ASSETS + "animations/entity/" + RemodelTable.stem(model) + ".animation.json").getAsJsonObject("animations");
            for (String c : CLIPS) assertTrue(clips.has(c), model + " has no clip " + c);
        }
        for (String[] special : new String[][]{{"dungeon_direwolf", "sleep"}, {"basilisk", "sleep"}, {"rouge", "sit"}, {"rouge", "spawn"}, {"god_of_order", "read"}, {"god_of_order", "beam"},
                {"god_of_order", "bow"}, {"goliath", "spawn"}, {"blood_beast", "spawn"}}) {
            JsonObject clips = json(ASSETS + "animations/entity/" + RemodelTable.stem(special[0]) + ".animation.json").getAsJsonObject("animations");
            assertTrue(clips.has(special[1]), special[0] + " has no clip " + special[1] + " (a renderer state plays it)");
        }
    }

    @Test
    void theLooksAreTheManifestsNumbersAndTheKitFoundNoProblem() throws Exception {
        JsonObject models = json("tools/arc/manifest.json").getAsJsonObject("models");
        for (String model : RemodelTable.withSecondForms()) {
            assertTrue(models.has(model), "tools/arc/manifest.json has no entry for " + model);
            JsonObject m = models.getAsJsonObject(model);
            assertEquals(0, m.getAsJsonArray("problems").size(), "the kit reported problems for " + model + ": " + m.getAsJsonArray("problems"));
            JsonObject look = m.getAsJsonObject("arc_look");
            RemodelLooks.Look l = RemodelLooks.BY_MODEL.get(model);
            assertNotNull(l, "RemodelLooks has no entry for " + model + ": run python3 -B tools/remodel/gen_table.py");
            assertEquals(look.get("height").getAsFloat(), l.height(), 1e-4, model + " height");
            assertEquals(look.get("walkCycle").getAsFloat(), l.walkCycle(), 1e-4, model + " walkCycle");
            assertEquals(look.get("runCycle").getAsFloat(), l.runCycle(), 1e-4, model + " runCycle");
            assertEquals(look.get("shadow").getAsFloat(), l.shadow(), 1e-4, model + " shadow");
            assertEquals(look.get("growXz").getAsFloat(), l.growXz(), 1e-4, model + " growXz");
            assertEquals(look.get("growUp").getAsFloat(), l.growUp(), 1e-4, model + " growUp");
        }
    }

    @Test
    void everyCharacterFileAndEveryTableRowAgree() throws Exception {
        Pattern entities = Pattern.compile("(?m)^ENTITIES\\s*=\\s*\\[([^\\]]*)\\]");
        Set<String> declared = new HashSet<>();
        File[] chars = new File("tools/arc").listFiles((d, n) -> n.startsWith("char_") && n.endsWith(".py"));
        assertNotNull(chars);
        for (File f : chars) {
            String text = Files.readString(f.toPath());
            Matcher m = entities.matcher(text);
            if (!m.find()) continue;
            for (String id : m.group(1).split(",")) {
                String clean = id.replace("\"", "").replace("'", "").trim();
                if (clean.isEmpty()) continue;
                declared.add(clean);
                assertNotNull(RemodelTable.of(clean), f.getName() + " declares the entity " + clean + " but RemodelTable has no row for it");
            }
        }
        for (String model : RemodelTable.models())
            assertTrue(has("tools/arc/char_" + model + ".py"), "model " + model + " has no character file tools/arc/char_" + model + ".py");
        // the entities the character files name are the ones whose models carry the entity's own id (the rest are drawn through a kind, a tier or a role)
        for (RemodelTable.Entry en : RemodelTable.ENTRIES)
            if (en.models().contains(en.entityId()) || en.family() == RemodelTable.Family.ZAGRED)
                assertTrue(declared.contains(en.entityId()), "tools/arc/char_" + en.entityId() + ".py does not declare ENTITIES = [\"" + en.entityId() + "\"]");
    }

    @Test
    void everyFamilyAndEveryEntityHasARendererMapping() throws Exception {
        String src = Files.readString(Path.of(MAIN + "client/remodel/RemodelRenderers.java"));
        for (RemodelTable.Family fam : RemodelTable.Family.values()) assertTrue(src.contains("case " + fam.name() + " ->"), "RemodelRenderers has no case for the family " + fam);
        for (RemodelTable.Entry en : RemodelTable.ENTRIES) {
            if (en.family() == RemodelTable.Family.BOSS || en.family() == RemodelTable.Family.MOB)
                assertTrue(src.contains("\"" + en.entityId() + "\""), "RemodelRenderers draws nothing for " + en.entityId() + " (no look or probe case)");
        }
        String nusmp = Files.readString(Path.of(MAIN + "NUSMP.java"));
        assertTrue(nusmp.contains("RemodelRenderers.init(modEventBus)"), "NUSMP does not call RemodelRenderers.init: the old renderers would stay in use");
    }

    @Test
    void theModelsOfEveryEntityAreAllInTheTable() {
        Set<String> all = new HashSet<>(RemodelTable.models());
        for (RemodelTable.Entry en : RemodelTable.ENTRIES) assertTrue(all.containsAll(en.models()), en.entityId());
        for (String m : RemodelTable.SECOND_FORMS) assertTrue(all.contains(m), "second form of a model the table does not draw: " + m);
    }

    /** Every java source of the mod as one string (to find a registration by its id; ~10 MB of text is fine for a test). */
    private static String allSources() throws Exception {
        StringBuilder sb = new StringBuilder();
        try (var walk = Files.walk(Path.of(MAIN))) {
            for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) sb.append(Files.readString(p)).append('\n');
        }
        return sb.toString();
    }
}
