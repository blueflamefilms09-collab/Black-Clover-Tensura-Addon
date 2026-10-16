package com.newuniverse.nusmp.quests.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Why: the installer touches the owner's disk, so install, refuse, force, update and uninstall are proven on a temp directory. */
class PackInstallerTest {
    private static void put(Path root, String rel, String text) throws IOException {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text, StandardCharsets.UTF_8);
    }

    private static String get(Path root, String rel) throws IOException {
        return Files.readString(root.resolve(rel), StandardCharsets.UTF_8);
    }

    private static void rm(Path root) throws IOException {
        try (Stream<Path> w = Files.walk(root)) {
            for (Path p : (Iterable<Path>) w.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
        }
    }

    private static Path pack(Path base, String chapterB) throws IOException {
        Path pack = base.resolve("pack");
        if (Files.exists(pack)) rm(pack);
        put(pack, "data.snbt", "{ version: 13 }");
        put(pack, "chapter_groups.snbt", "{ }");
        put(pack, "chapters/a.snbt", "{ a: 1 }");
        if (chapterB != null) put(pack, "chapters/" + chapterB + ".snbt", "{ b: 1 }");
        put(pack, "README.txt", "docs, not copied");
        return pack;
    }

    @Test
    void freshInstallCopiesFilesAndWritesMarker() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q1");
        try {
            Path quests = base.resolve("ftbquests/quests");
            PackInstaller.Result r = PackInstaller.install(pack(base, "b"), quests, "1.49.0", false, 0);
            assertTrue(r.ok(), r.message());
            assertEquals("{ a: 1 }", get(quests, "chapters/a.snbt"));
            assertFalse(Files.exists(quests.resolve("README.txt")));
            PackManifest m = PackInstaller.readMarker(quests);
            assertNotNull(m);
            assertEquals("1.49.0", m.version());
            assertEquals(4, m.files().size());
        } finally { rm(base); }
    }

    @Test
    void refusesOverForeignQuestsUnlessForced() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q2");
        try {
            Path quests = base.resolve("ftbquests/quests");
            put(quests, "chapters/mine.snbt", "{ mine: 1 }");
            Path pack = pack(base, null);
            PackInstaller.Result r = PackInstaller.install(pack, quests, "v", false, 0);
            assertFalse(r.ok());
            assertEquals("{ mine: 1 }", get(quests, "chapters/mine.snbt"));
            assertNull(PackInstaller.readMarker(quests));

            PackInstaller.Result f = PackInstaller.install(pack, quests, "v", true, 1792152000000L);
            assertTrue(f.ok(), f.message());
            Path backup = base.resolve("ftbquests/quests_backup_20261016_120000");
            assertEquals("{ mine: 1 }", get(backup, "chapters/mine.snbt"));
            assertFalse(Files.exists(quests.resolve("chapters/mine.snbt")));
            assertNotNull(PackInstaller.readMarker(quests));
        } finally { rm(base); }
    }

    @Test
    void updateKeepsOwnerChaptersAndRetiresOldOnes() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q3");
        try {
            Path quests = base.resolve("ftbquests/quests");
            assertTrue(PackInstaller.install(pack(base, "old"), quests, "1", false, 0).ok());
            put(quests, "chapters/mine.snbt", "{ mine: 1 }");
            Files.writeString(quests.resolve("chapters/a.snbt"), "{ edited: 1 }");
            Path next = pack(base, "new");
            PackInstaller.Result r = PackInstaller.update(next, quests, "2", 5000);
            assertTrue(r.ok(), r.message());
            assertEquals("{ a: 1 }", get(quests, "chapters/a.snbt"));
            assertTrue(Files.exists(quests.resolve("chapters/new.snbt")));
            assertFalse(Files.exists(quests.resolve("chapters/old.snbt")));
            assertEquals("{ mine: 1 }", get(quests, "chapters/mine.snbt"));
            assertEquals("2", PackInstaller.readMarker(quests).version());
            assertTrue(Files.exists(base.resolve("ftbquests/quests_backup_19700101_000005/chapters/old.snbt")));
            assertTrue(PackInstaller.update(next, quests, "2", 9000).message().startsWith("Already up to date"));
        } finally { rm(base); }
    }

    @Test
    void updateNeedsOurMarker() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q4");
        try {
            Path quests = base.resolve("ftbquests/quests");
            put(quests, "chapters/mine.snbt", "x");
            assertFalse(PackInstaller.update(pack(base, null), quests, "1", 0).ok());
            assertFalse(PackInstaller.uninstall(quests, 0).ok());
            assertEquals("x", get(quests, "chapters/mine.snbt"));
        } finally { rm(base); }
    }

    @Test
    void uninstallRemovesOnlyOurFiles() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q5");
        try {
            Path quests = base.resolve("ftbquests/quests");
            assertTrue(PackInstaller.install(pack(base, "b"), quests, "1", false, 0).ok());
            put(quests, "chapters/mine.snbt", "{ mine: 1 }");
            PackInstaller.Result r = PackInstaller.uninstall(quests, 7000);
            assertTrue(r.ok(), r.message());
            assertEquals("{ mine: 1 }", get(quests, "chapters/mine.snbt"));
            assertFalse(Files.exists(quests.resolve("data.snbt")));
            assertFalse(Files.exists(quests.resolve("chapters/a.snbt")));
            assertNull(PackInstaller.readMarker(quests));
            assertTrue(Files.exists(base.resolve("ftbquests/quests_backup_19700101_000007/data.snbt")));
        } finally { rm(base); }
    }

    @Test
    void nestedQuestsFolderIsTheContentRoot() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q6");
        try {
            Path pack = base.resolve("pack");
            put(pack, "quests/data.snbt", "{ }");
            put(pack, "quests/chapters/a.snbt", "{ }");
            Path quests = base.resolve("ftbquests/quests");
            assertTrue(PackInstaller.install(pack, quests, "1", false, 0).ok());
            assertTrue(Files.exists(quests.resolve("chapters/a.snbt")));
        } finally { rm(base); }
    }

    @Test
    void missingPackIsAClearFailure() throws IOException {
        Path base = Files.createTempDirectory("nusmp-q7");
        try {
            assertFalse(PackInstaller.install(base.resolve("nope"), base.resolve("q"), "1", false, 0).ok());
            assertFalse(Files.exists(base.resolve("q")));
        } finally { rm(base); }
    }
}
