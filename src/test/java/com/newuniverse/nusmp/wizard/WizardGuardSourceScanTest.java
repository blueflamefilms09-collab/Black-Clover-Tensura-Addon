package com.newuniverse.nusmp.wizard;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: 3.59.1 fixed a Tower Wizard / library-book pile-up after restarts. These lines are what keeps it fixed; losing one brings the crash back. Reads the sources as files. */
class WizardGuardSourceScanTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of(p), StandardCharsets.UTF_8); }

    @Test
    void stalesBooksAndSecondWizardsAreRefusedOnJoin() throws Exception {
        String lib = read("src/main/java/com/newuniverse/nusmp/wizard/WizardLibrary.java");
        assertTrue(lib.contains("e.loadedFromDisk() && it.getTags().contains(BOOK_TAG)"), "a saved library book must be dropped on load");
        assertTrue(lib.contains("TowerWizardEntity.class, w.getBoundingBox().inflate(24)"), "a second wizard within 24 blocks must be refused");
        assertTrue(lib.contains("books.get(i).discard()"), "more than BOOKS books must be trimmed");
        assertTrue(lib.contains("SETTLE_TICKS") && lib.contains("MAX_NEW_BOOKS_PER_RUN"), "new books wait for saved entities and are added a few at a time");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("WizardLibrary::onJoin"), "the join guard must be registered");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/wizard/WizardSystem.java").contains("level.isLoaded(at)"), "no wizard is added into a chunk that is still loading");
    }
}
