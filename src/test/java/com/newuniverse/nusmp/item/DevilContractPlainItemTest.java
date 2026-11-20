package com.newuniverse.nusmp.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 4.65.0: the Infernal Contract is a plain craftable item (not a relic), still crafted by its recipe, and held by its rod. */
class DevilContractPlainItemTest {
    private static String read(String f) throws Exception { return Files.readString(Path.of(f)); }

    @Test
    void notARelicAnymoreButStillCraftable() throws Exception {
        assertTrue(read("src/main/java/com/newuniverse/nusmp/item/NUItems.java").contains("DeferredItem<Item> DEVIL_CONTRACT = ITEMS.register(\"devil_contract\""));
        assertFalse(read("src/main/java/com/newuniverse/nusmp/relics/wave/Wave0.java").contains("RelicDef.of(\"devil_contract\""));
        assertTrue(Files.exists(Path.of("src/main/resources/data/anime_multiverse/recipe/devil_contract.json")));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/relics/wave/Wave0Hooks.java").contains("s.is(NUItems.DEVIL_CONTRACT.get())"), "the four contracts are still offered");
    }

    @Test
    void theMeshIsHeldByItsRod() throws Exception {
        String m = read("src/main/resources/assets/anime_multiverse/weapon_meshes/devil_contract.txt");
        assertTrue(m.lines().anyMatch(l -> l.startsWith("gripxyz -3.400 30.000 0.000")), "the hand holds the rod, not the middle of the scroll");
    }
}
