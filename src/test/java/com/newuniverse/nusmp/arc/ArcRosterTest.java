package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The roster: ids are checked, a clash is refused at once, floor bosses are found by road and floor, NPCs never stand on a floor. */
class ArcRosterTest {
    private static boolean threw(Runnable r) {
        try {
            r.run();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    @Test
    void aCharacterIsRegisteredOnceAndFoundById() {
        ArcRoster.clearForTests();
        ArcCharacter c = ArcCharacter.boss("heath_grice", "Heath Grice").title("the Death Knight").saga(ArcSaga.ELF_REINCARNATION).tier(1).build();
        assertSame(c, ArcRoster.register(c));
        assertSame(c, ArcRoster.register(c), "the same character twice is a reload, not a clash");
        assertSame(c, ArcRoster.byId("HEATH_GRICE"));
        assertEquals("Heath Grice, the Death Knight", c.fullName());
        assertTrue(threw(() -> ArcRoster.register(ArcCharacter.boss("heath_grice", "Someone Else").build())), "a different character under a taken id is refused");
    }

    @Test
    void anIdMustBeSnakeCase() {
        assertTrue(threw(() -> ArcCharacter.boss("Heath Grice", "x").build()));
        assertTrue(threw(() -> ArcCharacter.boss("1x", "x").build()));
    }

    @Test
    void floorBossesAreFoundByRoadAndFloorAndNpcsNeverStandOnAFloor() {
        ArcRoster.clearForTests();
        ArcCharacter holy = ArcRoster.register(ArcCharacter.boss("licht", "Licht").floors(Polarity.HOLY, 10, 30).build());
        ArcCharacter abyss = ArcRoster.register(ArcCharacter.boss("zagred", "Zagred").floors(Polarity.ABYSSAL, 10, 40).build());
        ArcCharacter any = ArcRoster.register(ArcCharacter.boss("goliath_arc", "Goliath").floors(null, 20, 20).build());
        ArcCharacter friend = ArcRoster.register(ArcCharacter.npc("asta", "Asta").floors(null, 1, 100).build());
        assertEquals(java.util.List.of(holy, any), ArcRoster.floorBosses(20, Polarity.HOLY));
        assertEquals(java.util.List.of(abyss, any), ArcRoster.floorBosses(-20, Polarity.ABYSSAL));
        assertEquals(java.util.List.of(), ArcRoster.floorBosses(50, Polarity.HOLY));
        assertFalse(friend.standsOn(20, Polarity.HOLY));
        assertEquals(3, ArcRoster.bosses().size());
        assertEquals(1, ArcRoster.npcs().size());
    }

    @Test
    void charterAndPrerequisitesAreReadFromTheRecord() {
        ArcRoster.clearForTests();
        ArcCharacter a = ArcRoster.register(ArcCharacter.boss("vetto", "Vetto").charter(0.4, "earth_manipulation", "tensura:strengthen_body").build());
        ArcCharacter b = ArcRoster.register(ArcCharacter.boss("patolli", "Patolli").after("vetto", "nobody").build());
        assertTrue(a.hasCharter());
        assertEquals(java.util.List.of("tensura:earth_manipulation", "tensura:strengthen_body"), a.charterSkills());
        assertFalse(b.hasCharter());
        assertEquals(java.util.List.of(a), ArcRoster.prerequisitesOf(b), "an id that is not registered is skipped");
    }
}
