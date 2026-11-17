package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** The canon Belial (owner notes) is a replacement behind {@code belialCanon}: on it is the pale scythe Djinn, off it the old Belial is back unchanged. */
class BelialCanonTest {
    private static final Path MAIN = Path.of("src", "main");
    private static final Path DJINN = MAIN.resolve("java/com/newuniverse/nusmp/relics/djinn");

    private static void withCanon(Boolean on, Runnable body) {
        try {
            BelialCanon.force(on);
            body.run();
        } finally {
            BelialCanon.force(null);
        }
    }

    @Test
    void canonOffRestoresTheOldBelialRowExactly() {
        withCanon(false, () -> {
            assertEquals("Lord of Hollow Thrones", Djinn.BELIAL.title());
            assertSame(Affinity.HEAT, Djinn.BELIAL.affinity());
            assertEquals(0x7A0F2E, Djinn.BELIAL.color());
            assertEquals(0xFF7096, Djinn.BELIAL.glow());
            assertTrue(Djinn.BELIAL.extreme().name().contains("Belial Inanis"));
            assertEquals("hollow_cleave", DjinnWeaponRules.spec(Djinn.BELIAL, false).one().key());
            assertEquals("greatsword", DjinnWeaponRules.spec(Djinn.BELIAL).kind());
        });
        assertFalse(BelialCanon.on(), "the config is not loaded in a unit test: the old Belial is what a test sees");
    }

    @Test
    void canonOnIsThePaleScytheDjinnAndNothingElseChanges() {
        withCanon(true, () -> {
            assertEquals(BelialCanon.TITLE, Djinn.BELIAL.title());
            assertEquals(BelialCanon.COLOR, Djinn.BELIAL.color());
            assertEquals(BelialCanon.GLOW, Djinn.BELIAL.glow());
            assertNotEquals(Djinn.BELIAL.color(), Djinn.BELIAL.glow());
            assertSame(Djinn.Look.Tail.NONE, Djinn.BELIAL.look().tail());
            assertSame(Djinn.Look.Wings.MEMBRANE, Djinn.BELIAL.look().wings(), "bat wings");
            assertTrue(Djinn.BELIAL.extreme().name().contains("Belior Zauto"));
            assertFalse(Djinn.BELIAL.extreme().name().contains("Inanis"));
            // the same bounds as DjinnTableTest: the Extreme stays inside the fairness ceiling
            assertTrue(Djinn.BELIAL.extreme().damage() <= 48.0 && Djinn.BELIAL.extreme().radius() <= 24.0);
            assertTrue(Djinn.BELIAL.trait().bonusFrac() <= 0.40 && Djinn.BELIAL.trait().bonusCap() <= 9.0);
            assertTrue(Djinn.BELIAL.trait().effectTicks() <= 180 && Djinn.BELIAL.extreme().effectTicks() <= 180);
            // every other Djinn is untouched
            assertEquals("King of the First Throne", Djinn.BAEL.title());
            assertSame(Affinity.WATER, Djinn.AGARES.affinity());
        });
    }

    @Test
    void theCanonScytheHasItsOwnIdsAndTheOldWeaponStays() {
        assertEquals("djinn_weapon_belial", DjinnWeaponRules.itemPath(Djinn.BELIAL, false));
        assertEquals("djinn_weapon_belial_scythe", DjinnWeaponRules.itemPath(Djinn.BELIAL, false, true));
        assertEquals("djinn_weapon_belial_scythe_dark", DjinnWeaponRules.itemPath(Djinn.BELIAL, true, true));
        assertEquals("djinn_weapon_baal", DjinnWeaponRules.itemPath(Djinn.BAAL, false, true), "only Belial has a canon variant");
        assertSame(Djinn.BELIAL, DjinnWeaponRules.djinnOfPath("djinn_weapon_belial_scythe"));
        assertSame(Djinn.BELIAL, DjinnWeaponRules.djinnOfPath("djinn_weapon_belial_scythe_dark"));
        assertTrue(DjinnWeaponRules.isDarkPath("djinn_weapon_belial_scythe_dark"));
        assertTrue(DjinnWeaponRules.isCanonPath("djinn_weapon_belial_scythe"));
        assertFalse(DjinnWeaponRules.isCanonPath("djinn_weapon_belial"));
    }

    @Test
    void theScytheRowIsAScytheWithTheTwoActsAndInsideTheFairnessBounds() {
        DjinnWeaponRules.Spec s = DjinnWeaponRules.canonSpec();
        assertEquals("scythe", s.kind());
        assertEquals("spirit_sever", s.one().key());
        assertEquals("exile", s.two().key());
        assertTrue(s.two().cooldown() > s.one().cooldown(), "the exile is the slower act");
        assertTrue(s.one().range() <= 16 && s.two().range() <= 16);
        assertSame(s, DjinnWeaponRules.spec(Djinn.BELIAL, true));
        assertNotSame(s, DjinnWeaponRules.spec(Djinn.BELIAL, false));
        Set<String> eng = DjinnWeaponRules.engravings(Djinn.BELIAL, false, true).stream().map(DjinnWeaponRules.Engraving::id).collect(Collectors.toSet());
        assertTrue(eng.contains("severance") && eng.contains("soul_eater"), "rule 6: Tensura engravings that fit the lore");
        assertTrue(DjinnWeaponRules.engravings(Djinn.BELIAL, true, true).stream().anyMatch(g -> g.id().equals("ruination")));
    }

    @Test
    void theDataFilesExist() throws IOException {
        Path data = MAIN.resolve("resources/data/anime_multiverse");
        assertTrue(Files.exists(data.resolve("damage_type/spirit_sever.json")));
        for (String tag : new String[]{"bypasses_armor", "bypasses_cooldown", "bypasses_effects", "bypasses_enchantments", "bypasses_invulnerability", "bypasses_resistance", "bypasses_shield"})
            assertTrue(Files.readString(MAIN.resolve("resources/data/minecraft/tags/damage_type/" + tag + ".json")).contains("anime_multiverse:spirit_sever"), tag);
        assertTrue(Files.exists(data.resolve("weapon_attributes/djinn_weapon_belial_scythe.json")));
        assertTrue(Files.exists(data.resolve("weapon_attributes/djinn_weapon_belial_scythe_dark.json")));
        String types = Files.readString(MAIN.resolve("resources/assets/anime_multiverse/djinn_extreme_types.json"));
        assertTrue(types.contains("\"belial_canon\"") && types.contains("anime_multiverse:spirit_sever"));
        String lang = Files.readString(MAIN.resolve("resources/assets/anime_multiverse/lang/en_us.json"));
        for (String key : new String[]{"djinn_weapon.act.spirit_sever", "djinn_weapon.act.exile", "item.anime_multiverse.djinn_weapon_belial_scythe", "death.attack.anime_multiverse.spirit_sever"})
            assertTrue(lang.contains("\"" + key + "\""), key);
    }

    @Test
    void theSeverAndExileCodeHasNoClientClassAndNoPerTickWorldScan() throws IOException {
        for (String f : new String[]{"BelialSever.java", "BelialExile.java", "BelialSeverRules.java", "BelialCanon.java", "BelialConfig.java"}) {
            String src = Files.readString(DJINN.resolve(f));
            assertFalse(src.contains("net.minecraft.client") || src.contains("com.mojang.blaze3d") || src.contains("nusmp.client") || src.contains("Minecraft.getInstance"), f + ": server code only");
            assertFalse(src.contains("ChunkSource.getChunk(") && src.contains("true)"), f + ": no chunk loading");
        }
        String exile = Files.readString(DJINN.resolve("BelialExile.java"));
        assertTrue(exile.contains("onLogout") && exile.contains("onDimension") && exile.contains("onDeath") && exile.contains("onServerStopping"), "cleanup on logout, dimension change, death and stop");
    }
}
