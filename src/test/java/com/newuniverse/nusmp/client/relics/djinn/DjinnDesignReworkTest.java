package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnAssets;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the design audit (docs/djinn_design_audit.md) reworked the Djinn designs that were copies. The new files must exist for every Djinn that points at them, the old files must still be there
 * (owner rule 2) and {@code djinnLookClassic} must bring the old paths back. Reads the repo's files (the working directory is the project root).
 */
class DjinnDesignReworkTest {
    private static final Path AM = Path.of("src/main/resources/assets/anime_multiverse");

    @Test
    void everyDarkBustAndEveryReworkedLightBustHasItsV2FileAndKeepsTheOldOne() {
        for (Djinn d : Djinn.values()) {
            String dark = DjinnLook.bustFile(d.id(), true, true, false);
            assertTrue(dark.endsWith("_d_bust2.png"), d.id());
            assertTrue(Files.exists(AM.resolve(dark)), dark + " is missing");
            assertTrue(Files.exists(AM.resolve(DjinnLook.bustPath(d.id(), true, true))), d.id() + ": the old dark bust was removed");
            assertEquals(DjinnLook.bustPath(d.id(), true, true), DjinnLook.bustFile(d.id(), true, true, true), "classic restores the old dark bust");
            String light = DjinnLook.bustFile(d.id(), false, true, false);
            assertTrue(Files.exists(AM.resolve(light)), light + " is missing");
            assertEquals(DjinnLook.BUST2_LIGHT.contains(d.id()), light.endsWith("_bust2.png"), d.id());
            assertEquals(DjinnLook.bustPath(d.id(), false, true), DjinnLook.bustFile(d.id(), false, true, true), "classic restores the old light bust");
        }
    }

    @Test
    void theReworkedTinyDjinnHaveAllTheirFilesAndTheOldOnesStay() {
        assertFalse(TinyDjinnAssets.REWORKED.isEmpty());
        for (String id : TinyDjinnAssets.REWORKED) {
            for (String stem : new String[]{"tinydjinn_" + id, "tinydjinn_" + id + "_v2"}) {
                assertTrue(Files.exists(AM.resolve("geo/entity/" + stem + ".geo.json")), stem + " geo");
                for (String suffix : new String[]{".png", "_glow.png", "_dark.png", "_dark_glow.png"}) assertTrue(Files.exists(AM.resolve("textures/entity/" + stem + suffix)), stem + suffix);
            }
        }
    }

    @Test
    void theClassicSwitchIsOffByDefault() {
        assertFalse(DjinnLookConfig.classic());
    }
}
