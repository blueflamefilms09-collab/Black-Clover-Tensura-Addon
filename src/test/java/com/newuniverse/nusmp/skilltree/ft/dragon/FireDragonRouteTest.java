package com.newuniverse.nusmp.skilltree.ft.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.fire.FireDragonNumbers;
import org.junit.jupiter.api.Test;

/**
 * Why: the Fire Dragon Slayer route is the one the owner named (the Fang of Smoldering Insanity, Iron Fist then Roar then a Secret Art), and it can only be checked by
 * reading its files, because the game cannot be run here. These tests read the shipped route file as the game's loader will: it obeys the schema and the price bands, it has the
 * shape the owner asked for, it says exactly the numbers of the code table, the table obeys the budget of design.md section 6, and every number the skill classes read exists.
 */
class FireDragonRouteTest {
    private static SkillRoute route() throws Exception { return DragonRouteJson.read("fire_dragon_slayer"); }

    @Test
    void theFileObeysTheSchemaAndTheShapeRules() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.structure(route(), DragonRouteJson.colorText("fire_dragon_slayer")));
    }

    @Test
    void theOwnersNamesAndTheLootWeightAreThere() throws Exception {
        SkillRoute r = route();
        assertEquals("nusmp:fire_dragon_slayer", r.id());
        assertEquals("Fire Dragon Slayer", r.name());
        assertEquals("nusmp:fire_dragon_slayer_book", r.book());
        assertEquals("Heart of the Fire Dragon", r.bookName());      // 1.81.0: the Fang of Smoldering Insanity is its own Heavy Knight route now
        assertEquals("fire_dragon_slayer", r.classId());
        assertEquals(3, r.lootWeight(), "the common first book");
        assertEquals(0xE8541C, r.color());
        assertTrue(!r.bookLore().isBlank());
    }

    @Test
    void ironFistLeadsToRoarAndRoarLeadsToTheSecretArt() throws Exception {
        SkillRoute r = route();
        SkillNode fist = r.node(FireDragonNumbers.IRON_FIST), roar = r.node(FireDragonNumbers.ROAR), lotus = r.node(FireDragonNumbers.CRIMSON_LOTUS);
        assertNotNull(fist);
        assertTrue(fist.isRoot(), "the Iron Fist is the root");
        assertEquals("iron_fist", roar.parents().get(0).node(), "Roar follows the Iron Fist");
        assertTrue(lotus.parents().stream().anyMatch(p -> p.node().equals("roar")), "the Secret Art follows Roar");
        assertEquals("Crimson Lotus: Exploding Flame Blade", lotus.name());
        assertEquals("nusmp:ft_crimson_lotus_exploding_flame_blade", lotus.skill());
    }

    @Test
    void theSecretArtNeedsTwoBranchesAndTheBranchesAreDifferent() throws Exception {
        SkillRoute r = route();
        SkillNode lotus = r.node(FireDragonNumbers.CRIMSON_LOTUS);
        assertEquals(2, lotus.parents().size());
        assertTrue(!DragonRouteChecks.branchOf(r, lotus.parents().get(0).node()).equals(DragonRouteChecks.branchOf(r, lotus.parents().get(1).node())), "two different branches of the root");
        assertTrue(r.node(FireDragonNumbers.FLAME_EATER).skill().isEmpty() && r.node(FireDragonNumbers.BLAZING_SCALES).skill().isEmpty(), "the two passives grant no skill");
        assertEquals(java.util.List.of("flame_eater"), r.node(FireDragonNumbers.FLAME_EATER).passives());
        assertEquals(java.util.List.of("blazing_scales"), r.node(FireDragonNumbers.BLAZING_SCALES).passives());
    }

    @Test
    void theFileAndTheCodeTableSayTheSameNumbersAndTheyObeyTheBudget() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.consistency(route(), FireDragonNumbers.TABLES));
        DragonRouteChecks.noProblems(DragonRouteChecks.tableBudget(FireDragonNumbers.TABLES));
    }

    @Test
    void everyNumberTheCodeReadsExistsAndEveryNumberIsRead() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.sourceKeys(DragonRouteChecks.dir("fire"), FireDragonNumbers.class, FireDragonNumbers.TABLES, "FireDragonPassives.java"));
    }

    @Test
    void everySkillHasItsIconPicture() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.icons(DragonRouteChecks.dir("fire")));
    }

    @Test
    void theRegisterClassRegistersExactlyTheSkillsTheFileNames() throws Exception {
        DragonRouteChecks.noProblems(DragonRouteChecks.registrations(route(), java.nio.file.Path.of("src/main/java/com/newuniverse/nusmp/skilltree/ft/FireDragonSkills.java"), DragonRouteChecks.dir("fire"), FireDragonNumbers.class));
    }
}
