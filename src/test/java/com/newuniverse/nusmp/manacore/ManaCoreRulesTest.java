package com.newuniverse.nusmp.manacore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rank Mana Cores (3.51.0): the pure table is sound and bounded, every lang key exists, and the package stays server safe. */
class ManaCoreRulesTest {
    @Test
    void theTableValidatesAndHasTwentyFourUniqueCores() {
        assertNull(ManaCoreRules.validate(), ManaCoreRules.validate());
        Set<String> ids = new HashSet<>();
        for (ManaCoreRules.Core c : ManaCoreRules.all()) assertTrue(ids.add(c.id()), c.id());
        assertEquals(24, ids.size());
        assertNotNull(ManaCoreRules.byId("core_ss_ancient"));
        assertEquals("World Line Core / Dragon Fragment", ManaCoreRules.byId("core_ss_ancient").name());
    }

    @Test
    void beastCoresFromATowardPlugIntoTheBeastWillSystem() {
        for (int rank = 4; rank < 8; rank++) {
            ManaCoreRules.Core c = ManaCoreRules.of(rank, 1);
            assertNotNull(c.will(), c.id());
            assertTrue(c.beastForm(), c.id());
            assertTrue(new File("src/main/resources/data/nusmp/beast_will/" + c.will() + ".json").isFile(), c.will());
        }
        for (int rank = 0; rank < 4; rank++) assertFalse(ManaCoreRules.of(rank, 1).beastForm());
    }

    @Test
    void theSsAncientCoreHoldsTheTwoOwnerNumbers() {
        ManaCoreRules.Core c = ManaCoreRules.byId("core_ss_ancient");
        assertEquals(75_000, c.a1().mp());
        assertEquals(120_000, c.a2().mp());
        assertEquals(12_000, c.a2().cd());                       // 600 s
        assertEquals(600, c.a2().dur());                          // 30 s countdown
        assertEquals(200, c.a1().dur());                          // 10 s stopped world
        assertEquals(1.0, c.passive().flag("time_immune"));
    }

    @Test
    void everyActiveStaysInsideTheHardCaps() {
        for (ManaCoreRules.Core c : ManaCoreRules.all())
            for (int n = 1; n <= 2; n++) {
                ManaCoreRules.Active a = c.active(n);
                assertTrue(a.targets() >= 1 && a.targets() <= ManaCoreRules.MAX_TARGETS, a.key());
                assertTrue(a.radius() <= ManaCoreRules.MAX_RADIUS && a.dur() <= ManaCoreRules.MAX_DURATION && a.delay() <= ManaCoreRules.MAX_DELAY, a.key());
                if (a.kind() != ManaCoreRules.Kind.BEAST_FORM) assertTrue(a.cd() >= ManaCoreRules.MIN_COOLDOWN && a.mp() > 0, a.key());
            }
    }

    @Test
    void costsAndGatesRiseWithTheRank() {
        for (int r = 1; r < 8; r++) {
            assertTrue(ManaCoreRules.EP_GATE[r] > ManaCoreRules.EP_GATE[r - 1]);
            assertTrue(ManaCoreRules.POWER[r] > ManaCoreRules.POWER[r - 1]);
            assertTrue(ManaCoreRules.DROP[r] < ManaCoreRules.DROP[r - 1]);
            assertTrue(ManaCoreRules.of(r, 0).a1().mp() > ManaCoreRules.of(r - 1, 0).a1().mp());
        }
        assertTrue(ManaCoreRules.mayUse(0, 0, true));
        assertFalse(ManaCoreRules.mayUse(7, 499_999, true));
        assertTrue(ManaCoreRules.mayUse(7, 0, false));
    }

    @Test
    void cooldownIsClampedAndTheMatrixGivesEveryThirdCastFree() {
        assertEquals(ManaCoreRules.MIN_COOLDOWN, ManaCoreRules.cooldown(1, 0.0, 0.0));
        assertEquals(700, ManaCoreRules.cooldown(1000, 1.0, 0.7));
        assertEquals(2000, ManaCoreRules.cooldown(1000, 99.0, 0.0));
        assertTrue(ManaCoreRules.freeCast(3, 3.0) && ManaCoreRules.freeCast(6, 3.0));
        assertFalse(ManaCoreRules.freeCast(1, 3.0) || ManaCoreRules.freeCast(4, 3.0) || ManaCoreRules.freeCast(3, 0.0));
        assertEquals(2, ManaCoreRules.historyIndex(4, 1));
        assertEquals(0, ManaCoreRules.historyIndex(4, 3));
    }

    @Test
    void victimRankFollowsTheBands() {
        assertEquals(-1, ManaCoreRules.victimRank(50));
        assertEquals(0, ManaCoreRules.victimRank(100));
        assertEquals(4, ManaCoreRules.victimRank(20_000));
        assertEquals(7, ManaCoreRules.victimRank(1e12));
    }

    @Test
    void everyItemAndTooltipLangKeyExists() throws Exception {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"))).getAsJsonObject();
        for (ManaCoreRules.Core c : ManaCoreRules.all()) {
            for (String k : new String[]{"item.nusmp." + c.id(), "manacore." + c.id() + ".passive", "manacore." + c.id() + ".a1", "manacore." + c.id() + ".a2"})
                assertTrue(lang.has(k), "missing lang key " + k);
            assertFalse(lang.get("item.nusmp." + c.id()).getAsString().toLowerCase().contains("istaroth"));
        }
        for (String v : ManaCoreRules.VARIANTS) assertTrue(lang.has("manacore.variant." + v));
        for (String k : new String[]{"too_weak", "cooldown", "no_mp", "control_cd", "no_target", "no_history", "busy", "safeguard", "last_second", "countdown", "will_swap", "dropped"})
            assertTrue(lang.has("manacore.msg." + k), k);
    }

    @Test
    void thePackageIsServerSafeAndItsRulesAreMinecraftFree() throws Exception {
        File dir = new File("src/main/java/com/newuniverse/nusmp/manacore");
        for (File f : dir.listFiles((d, n) -> n.endsWith(".java"))) {
            String s = Files.readString(f.toPath());
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client") || s.contains("Minecraft.getInstance"), f.getName());
            assertFalse(s.toLowerCase().contains("istaroth"), f.getName());
            assertFalse(s.contains("addFreshEntity") && !f.getName().equals("ManaCoreDrops.java"), f.getName() + " spawns an entity");
        }
        String rules = Files.readString(new File(dir, "ManaCoreRules.java").toPath());
        assertFalse(rules.contains("import net.minecraft") || rules.contains("import net.neoforged"), "the rules class must stay pure");
    }
}
