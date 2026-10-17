package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.arena.ArenaShapes;
import com.newuniverse.nusmp.arc.story.StoryRules;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the seven old bosses enter the roster as plain records, and the rest of the arcs lean on them (the Spade Kingdom's characters follow "the Triad", the
 * story gate reads their ids), so their records must agree with what the rest of the mod already says about them: the same ids as the boss registry, the
 * same Charter Grimoire as the Charter table, entity paths that are the registered ones, a story chain with no loop, floors that make sense, and arenas that
 * exist. Pure: nothing here starts a game.
 */
class ExistingBossesTest {
    /** The theme ids of the contract (docs/labyrinth/mega_v2.md): the eleven that exist and the sixteen the theme builders add. */
    private static final Set<String> THEMES = Set.of("gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral", "radiant", "abyss",
            "holy_church", "celestial_court", "sky_citadel", "mountain_monastery", "feywild_grove", "dwarven_forgehold", "crystal_geode", "sunken_temple",
            "cursed_archive", "necropolis", "vampire_castle", "haunted_manor", "fungal_underdark", "infernal_foundry", "drowned_crypt", "spider_warrens");

    @Test
    void sevenBossesInStoryOrderWithDistinctIds() {
        List<ArcCharacter> all = ExistingBosses.all();
        List<String> order = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (ArcCharacter c : all) {
            assertTrue(ids.add(c.id()), "duplicate " + c.id());
            assertTrue(c.isBoss(), c.id());
            order.add(c.id());
        }
        assertEquals(List.of("goliath", "charybdis_spawn", "devil_avatar", "dante", "vanica", "zenon", "lucius"), order);
    }

    @Test
    void theIdsAreTheBossRegistrysAndTheEntityPathsAreTheRegisteredOnes() {
        for (ArcCharacter c : ExistingBosses.all()) {
            assertTrue(ExistingBosses.owns(c.id()), c.id());
            String path = ExistingBosses.entityPath(c.id());
            assertNotNull(path, c.id());
            assertEquals(c.id(), BossRegistry.idOf("nusmp", path).orElse(null), c.id() + ": the boss registry knows its entity under another id");
            assertEquals(c.id(), ExistingBosses.idOfPath(path));
        }
        assertFalse(ExistingBosses.owns("zagred"), "Zagred is the ZAG builder's");
        assertFalse(ExistingBosses.owns("god_of_order"));
        assertFalse(ExistingBosses.owns(null));
        assertNull(ExistingBosses.entityPath("revchi_salik"));
        assertNull(ExistingBosses.idOfPath("nope"));
    }

    @Test
    void theCharterGrimoireIsTheOneTheTableAlreadyHas() {
        for (ArcCharacter c : ExistingBosses.all()) {
            CharterTable.Boss row = CharterTable.byId(c.id());
            assertNotNull(row, c.id() + " has no Charter row");
            assertTrue(c.hasCharter(), c.id());
            assertEquals(row.skills(), c.charterSkills(), c.id() + ": the record and the table disagree on the skills");
            assertEquals(row.chance(), c.charterChance(), 1e-9, c.id());
            assertEquals(row.entityPath(), ExistingBosses.entityPath(c.id()), c.id());
        }
    }

    @Test
    void theStoryChainHasNoLoopAndEveryKnownPrerequisiteIsOneOfTheSeven() {
        ArcRoster.clearForTests();
        try {
            ExistingBosses.registerAll();
            ExistingBosses.registerAll();                                                       // twice is harmless
            assertEquals(7, ArcRoster.all().size());
            for (ArcCharacter c : ExistingBosses.all()) {
                assertFalse(StoryRules.cyclic(c, ArcRoster::byId), c.id());
                for (String p : c.prerequisites()) {
                    if (ExistingBosses.owns(p)) assertTrue(ExistingBosses.all().indexOf(ArcRoster.byId(p)) < ExistingBosses.all().indexOf(c), c.id() + " follows " + p + " which comes later");
                    else assertEquals("zagred", p, c.id() + " follows an id of nobody we know: " + p);
                }
            }
            assertEquals(List.of("dante", "vanica", "zenon"), ArcRoster.byId("lucius").prerequisites());
            assertEquals(List.of("goliath", "charybdis_spawn"), ArcRoster.byId("devil_avatar").prerequisites());
            assertTrue(ArcRoster.byId("goliath").prerequisites().isEmpty() && ArcRoster.byId("charybdis_spawn").prerequisites().isEmpty(), "the old dungeon's pair is open from the start");
        } finally {
            ArcRoster.clearForTests();
        }
    }

    @Test
    void sagasTiersAndFloorsMakeSense() {
        assertEquals(ArcSaga.DANMACHI, ExistingBosses.GOLIATH.saga());
        assertEquals(ArcSaga.DANMACHI, ExistingBosses.CHARYBDIS_SPAWN.saga());
        assertEquals(ArcSaga.SPADE_RAID, ExistingBosses.DANTE.saga());
        assertEquals(ArcSaga.SPADE_RAID, ExistingBosses.VANICA.saga());
        assertEquals(ArcSaga.SPADE_RAID, ExistingBosses.ZENON.saga());
        assertEquals(ArcSaga.WIZARD_KING, ExistingBosses.LUCIUS.saga());
        assertEquals(5, ExistingBosses.LUCIUS.tier());
        for (ArcCharacter c : List.of(ExistingBosses.DANTE, ExistingBosses.VANICA, ExistingBosses.ZENON)) assertEquals(4, c.tier(), c.id());
        for (ArcCharacter c : List.of(ExistingBosses.GOLIATH, ExistingBosses.CHARYBDIS_SPAWN)) {
            assertEquals(2, c.tier(), c.id());
            assertTrue(c.floorMin() == 10 && c.floorMax() == 30, "the old dungeon's bosses stand on floors 10 to 30");
            assertNull(c.road(), c.id() + " belongs to both roads");
        }
        assertEquals(0, ExistingBosses.LUCIUS.floorMax(), "Lucius answers only in his arena: never a floor boss");
        assertFalse(ExistingBosses.LUCIUS.standsOn(50, Polarity.HOLY));
        assertEquals(Polarity.ABYSSAL, ExistingBosses.VANICA.road());
        assertEquals(Polarity.HOLY, ExistingBosses.ZENON.road());
        assertTrue(ExistingBosses.DANTE.standsOn(60, Polarity.HOLY) && ExistingBosses.DANTE.standsOn(-60, Polarity.ABYSSAL));
        assertFalse(ExistingBosses.VANICA.standsOn(60, Polarity.HOLY));
        assertFalse(ExistingBosses.GOLIATH.standsOn(40, Polarity.HOLY), "above the window");
        for (ArcCharacter c : ExistingBosses.all()) {
            assertTrue(c.health() > 0 && c.attack() > 0 && c.speed() > 0, c.id());
            for (String t : c.themes()) assertTrue(THEMES.contains(t), c.id() + " names the unknown theme " + t);
        }
    }

    @Test
    void everyArenaTheRecordsNameIsKnownOrTheFinalHour() {
        for (ArcCharacter c : ExistingBosses.all()) {
            assertFalse(c.arena().isEmpty(), c.id());
            if (ExistingBosses.LUCIUS.equals(c)) assertEquals(ExistingBosses.FINAL_HOUR_ARENA, c.arena());
            else assertNotNull(ArenaShapes.byId(c.arena()), c.id() + " names an arena nobody builds: " + c.arena());
        }
        assertEquals("orario_pit", ArenaShapes.forCharacter(ExistingBosses.GOLIATH).id());
        assertEquals("spade_throne", ArenaShapes.forCharacter(ExistingBosses.DANTE).id());
        assertTrue(ArenaShapes.usesFinalHour(ExistingBosses.LUCIUS));
    }
}
