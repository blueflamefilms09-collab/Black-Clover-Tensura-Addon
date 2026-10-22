package com.newuniverse.nusmp.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Elsdocia and the Demon Dweller used to take 2 and 1.5 percent of the TARGET's maximum magicule on every hit (the owner found it in the bytecode of 1.112.0). The
 * drain now only runs when the server config {@code weaponsDrainTargetMagicule} is on (default off); the weapon still charges. Source scan: every {@code setMagicule} of
 * the two absorb methods sits inside that guard, and the config exists with default false.
 */
class WeaponDrainGuardTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    @Test
    void everyTargetDrainOfTheTwoWeaponsIsBehindTheConfig() throws IOException {
        String s = Files.readString(SRC.resolve("item/MagicWeaponItem.java"));
        for (String method : new String[]{"private static void absorbDweller", "private static void absorbElsdocia"}) {
            int at = s.indexOf(method);
            assertTrue(at > 0, method);
            int end = s.indexOf("\n    }\n", at);
            String body = s.substring(at, end);
            int guard = body.indexOf("NUConfig.WEAPON_DRAINS_TARGET.get()");
            int drain = body.indexOf("existence.setMagicule(");
            assertTrue(guard > 0 && drain > guard, method + ": the drain must come after the config check");
            assertEquals(1, body.split("existence\\.setMagicule\\(", -1).length - 1, method + ": exactly one drain");
        }
    }

    @Test
    void theConfigDefaultsToNoDrain() throws IOException {
        String s = Files.readString(SRC.resolve("NUConfig.java"));
        assertTrue(s.contains("\"weaponsDrainTargetMagicule\", false"), "default must be false");
    }

    @Test
    void theChargeStillRisesWithoutTheDrain() throws IOException {
        String s = Files.readString(SRC.resolve("item/MagicWeaponItem.java"));
        assertTrue(s.contains("\"ElsdociaCharge\"") && s.contains("\"DwellerCharge\""), "the charge tags are still written");
    }
}
