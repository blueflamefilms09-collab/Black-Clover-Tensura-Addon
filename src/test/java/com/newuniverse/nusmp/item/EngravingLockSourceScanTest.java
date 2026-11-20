package com.newuniverse.nusmp.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the engraving lock (docs/engraving_lock.md) is wired in four places that must stay together: the two events, the restore pass of both engraving classes,
 * the server setting and the self-test. Source scan, no Minecraft needed.
 */
class EngravingLockSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String p) throws IOException { return Files.readString(SRC.resolve(p)); }

    @Test
    void lockHandlesAnvilAndGrindstoneAndNoClientClass() throws IOException {
        String s = read("item/EngravingLock.java");
        assertTrue(s.contains("AnvilUpdateEvent") && s.contains("GrindstoneEvent.OnPlaceItem"));
        assertTrue(s.contains("setCanceled(true)") && s.contains("locked(e.getRight())"));
        assertTrue(s.contains("NUConfig.ENGRAVING_LOCK.get()"));
        assertFalse(s.contains("net.minecraft.client") || s.contains("com.newuniverse.nusmp.client"));
    }

    @Test
    void bothEngravingClassesPutAStrippedEngravingBack() throws IOException {
        for (String f : new String[]{"item/WeaponEngravings.java", "franchise/FranchiseEngravings.java"}) {
            String s = read(f);
            assertTrue(s.contains("EngravingLock.on()"), f);
            assertTrue(s.contains("if (done) return changed;"), f);
        }
    }

    @Test
    void settingIsServerSideAndDefaultsOn() throws IOException {
        String s = read("NUConfig.java");
        assertTrue(s.contains("define(\"engravingLock\", true)"));
    }

    @Test
    void wiredAndSelfTested() throws IOException {
        String n = read("NUSMP.java");
        assertTrue(n.contains("EngravingLock::onAnvil") && n.contains("EngravingLock::onGrindstone") && n.contains("EngravingLockSelfTests.register()"));
        assertTrue(read("item/EngravingLockSelfTests.java").contains("\"engraving.lock\""));
    }
}
