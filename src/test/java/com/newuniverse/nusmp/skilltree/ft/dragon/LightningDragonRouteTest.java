package com.newuniverse.nusmp.skilltree.ft.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.lightning.LightningDragonNumbers;
import org.junit.jupiter.api.Test;

/**
 * Why: the Lightning Dragon Slayer route is the second route of the Fairy Tail set and the proof that the engine takes a second tree with a different play style from the
 * first (speed, numbing and a stun instead of burning and area): the same questions are asked of its file as of the Fire route's, because both are read by the same loader and
 * balanced by the same budget, and the game cannot be run here.
 */
class LightningDragonRouteTest {
    private static SkillRoute route() throws Exception { return DragonRouteJson.read("lightning_dragon_slayer"); }

    @Test
    void theFileObeysTheSchemaAndTheShapeRules() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.structure(route(), DragonRouteJson.colorText("lightning_dragon_slayer")));
    }

    @Test
    void theNamesAndTheLootWeightAreThere() throws Exception {
        SkillRoute r = route();
        assertEquals("nusmp:lightning_dragon_slayer", r.id());
        assertEquals("Lightning Dragon Slayer", r.name());
        assertEquals("nusmp:lightning_dragon_slayer_book", r.book());
        assertEquals("lightning_dragon_slayer", r.classId());
        assertEquals(2, r.lootWeight(), "rarer than the fire book");
        assertEquals(0xF5D63B, r.color());
        assertTrue(!r.bookName().isBlank() && !r.bookLore().isBlank());
        assertTrue(!"Fang of Smoldering Insanity".equals(r.bookName()), "its own book");
    }

    @Test
    void theRootBranchesAndTheSecretArtNeedsTwoOfTheBranches() throws Exception {
        SkillRoute r = route();
        assertTrue(r.node(LightningDragonNumbers.IRON_FIST).isRoot());
        assertEquals("iron_fist", r.node(LightningDragonNumbers.ROAR).parents().get(0).node());
        assertEquals("iron_fist", r.node(LightningDragonNumbers.BREAKDOWN_FIST).parents().get(0).node());
        assertEquals("iron_fist", r.node(LightningDragonNumbers.LIGHTNING_BODY).parents().get(0).node());
        SkillNode secret = r.node(LightningDragonNumbers.ROARING_THUNDER);
        assertEquals(2, secret.parents().size());
        assertTrue(!DragonRouteChecks.branchOf(r, secret.parents().get(0).node()).equals(DragonRouteChecks.branchOf(r, secret.parents().get(1).node())), "two different branches");
        assertEquals("nusmp:ft_roaring_thunder", secret.skill());
        assertEquals(java.util.List.of("lightning_body"), r.node(LightningDragonNumbers.LIGHTNING_BODY).passives());
        assertEquals(java.util.List.of("thunder_eater"), r.node(LightningDragonNumbers.THUNDER_EATER).passives());
    }

    @Test
    void theFileAndTheCodeTableSayTheSameNumbersAndTheyObeyTheBudget() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.consistency(route(), LightningDragonNumbers.TABLES));
        DragonRouteChecks.noProblems(DragonRouteChecks.tableBudget(LightningDragonNumbers.TABLES));
    }

    @Test
    void everyNumberTheCodeReadsExistsAndEveryNumberIsRead() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.sourceKeys(DragonRouteChecks.dir("lightning"), LightningDragonNumbers.class, LightningDragonNumbers.TABLES, "LightningDragonPassives.java"));
    }

    @Test
    void everySkillHasItsIconPicture() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.icons(DragonRouteChecks.dir("lightning")));
    }

    @Test
    void theRegisterClassRegistersExactlyTheSkillsTheFileNames() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.registrations(route(), java.nio.file.Path.of("src/main/java/com/newuniverse/nusmp/skilltree/ft/LightningDragonSkills.java"), DragonRouteChecks.dir("lightning"), LightningDragonNumbers.class));
    }
}
