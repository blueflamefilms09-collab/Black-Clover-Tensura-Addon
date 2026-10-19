package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the id table of docs/compat/create.md is what a server owner reads when a Create version renames a block, so it must list every id the code tries, every family, and every config key. */
class CreateDocTest {
    private static final Path DOC = Paths.get("docs/compat/create.md"), HOOKS = Paths.get("src/main/java/com/newuniverse/nusmp/NUSMP.java"), COMPAT = Paths.get("src/main/java/com/newuniverse/nusmp/compat/create/CreateCompat.java");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @Test
    void theTableListsEveryIdTheCodeTries() throws IOException {
        String doc = read(DOC);
        for (Part p : Part.values()) {
            for (String id : CreateIds.candidates(p)) assertTrue(doc.contains("`" + id + "`"), "the doc does not list " + id);
            if (!p.vanilla()) assertTrue(doc.contains("`" + p.fallback() + "`"), "the doc does not list the stand-in " + p.fallback());
        }
        for (String forbidden : CreateIds.FORBIDDEN) if (forbidden.equals("create:schematicannon")) assertTrue(doc.contains(forbidden));
        for (HazardKind k : HazardKind.values()) assertTrue(doc.contains("`" + CreateIds.damageTypePath(k) + "`") || doc.contains(CreateIds.damageTypePath(k)), k.id());
    }

    @Test
    void everyFamilyAndEveryConfigKeyIsDocumented() throws IOException {
        String doc = read(DOC);
        for (TrapFamily f : TrapFamily.values()) assertTrue(doc.contains("`" + f.id() + "`"), f.id());
        for (String key : new String[] {"enabled", "densityMultiplier", "damageMultiplier", "sawRun", "press", "drillWall", "treadmill", "pour", "sequence", "refuseContraptions"})
            assertTrue(doc.contains("`" + key + "`"), key);
        assertTrue(doc.contains("nusmp-labyrinth-create-compat-server.toml"));
        assertTrue(doc.contains("compat.create"));
    }

    @Test
    void theHooksAreWiredAndTheConfigFileIsNamedAsAsked() throws IOException {
        assertTrue(read(HOOKS).contains("compat.create.CreateCompat.init(modContainer)"));
        String compat = read(COMPAT);
        assertTrue(compat.contains("\"nusmp-labyrinth-create-compat-server.toml\""));
        assertTrue(compat.contains("CreateSelfTests.register()"));
        assertTrue(read(Paths.get("src/main/java/com/newuniverse/nusmp/compat/create/CreateSelfTests.java")).contains("SelfTests.register(\"compat.create\""));
    }

    @Test
    void theDocSaysWhatIsUnverified() throws IOException {
        String doc = read(DOC);
        assertTrue(doc.contains("could not be verified"));
        assertTrue(doc.contains("from memory"));
    }
}
