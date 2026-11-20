package com.newuniverse.nusmp.arc.magi;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers and the one-djinn rule of the Sinbad boss, with no game: the table is sane, every djinn has moves, the filter and the pick follow the rule. */
class SinbadTableTest {
    private static SinbadTable.Spec spec(String id) {
        SinbadTable.Spec s = SinbadTable.byId(id);
        assertNotNull(s, id);
        return s;
    }

    @Test
    void theTableHasUniqueIdsAndSaneNumbers() {
        Set<String> ids = new HashSet<>();
        for (SinbadTable.Spec s : SinbadTable.specs()) {
            assertTrue(ids.add(s.id()), "twice: " + s.id());
            assertFalse(s.label().isBlank(), s.id());
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange(), s.id() + " range");
            assertTrue(s.windUpTicks() > 0 && s.cooldownTicks() >= 40 && s.weight() >= 1 && s.minPower() >= 0 && s.minPower() <= 100, s.id() + " numbers");
            assertTrue(s.manaCost() >= 0 && s.recovery() >= 0, s.id() + " mana/recovery");
        }
        assertEquals(17, SinbadTable.specs().size(), "the move list is append-only (the client reads move names by index): update this number when a move is appended");
    }

    @Test
    void theMoveOrderIsKeptAppendOnly() {
        String[] first = {"sword_slash", "magoi_thrust", "djinn_equip", "bararaq", "bararaq_saiqa", "bararaq_inqerad_saiqa", "garufor_zairu", "garufor_kirestal", "stagnation",
                "command_scream", "rukh_intrusion", "foraz_zora", "gale_step", "vepar_istella", "saber_flurry", "rending_darkness", "seven_seas_sovereign"};
        for (int i = 0; i < first.length; i++) assertEquals(first[i], SinbadTable.specs().get(i).id(), "slot " + i);
    }

    @Test
    void everyDjinnHasMovesAndTheTwoUltimatesAreTheOnlyOnes() {
        for (SinbadDjinn d : SinbadDjinn.values()) {
            if (d == SinbadDjinn.NONE) continue;
            assertTrue(SinbadTable.specs().stream().anyMatch(s -> s.djinn() == d), d + " has no move");
            assertTrue(SinbadTable.unlockPower(d) >= 10 && SinbadTable.unlockPower(d) <= 60, d + " unlock");
        }
        List<String> ult = SinbadTable.specs().stream().filter(SinbadTable.Spec::ultimate).map(SinbadTable.Spec::id).toList();
        assertEquals(List.of(SinbadTable.INQERAD_SAIQA, SinbadTable.SEVEN_SEAS), ult);
        assertTrue(spec(SinbadTable.INQERAD_SAIQA).minPower() >= SinbadTable.unlockPower(SinbadDjinn.BAAL), "Extreme Magic needs Baal");
        assertTrue(spec(SinbadTable.RENDING_DARKNESS).minPower() >= SinbadTable.unlockPower(SinbadDjinn.FURFUR), "no Furfur move before Furfur unlocks");
        assertTrue(spec(SinbadTable.VEPAR_ISTELLA).minPower() >= SinbadTable.unlockPower(SinbadDjinn.VEPAR), "no Vepar move before Vepar unlocks");
    }

    @Test
    void theFinaleKeepsHimExposedUntilItsLastStrike() {
        int sequence = 6 * 20 + 10;          // the seventh step starts at tick 120 and strikes 10 ticks later (SinbadMoves.sevenSeas)
        assertTrue(spec(SinbadTable.SEVEN_SEAS).recovery() >= sequence, "the move's recovery must outlast the sequence");
        assertTrue(spec(SinbadTable.SEVEN_SEAS).recovery() >= 40, "an ultimate's recovery exposes him");
        assertTrue(spec(SinbadTable.SEVEN_SEAS).minPower() >= 90);
    }

    @Test
    void oneMoveNeverMarksMoreThanTheCapOfSpots() {
        assertEquals(12, SinbadTable.MAX_SPOTS);
        assertTrue(SinbadMoves.RAIN_PER_VOLLEY * 3 >= SinbadTable.MAX_SPOTS, "three volleys of the rain cover the cap");
    }

    @Test
    void onlyTheEquippedDjinnsMovesAndTheSwordMovesPassTheFilter() {
        long plenty = SinbadTable.EQUIP_TICKS;
        for (SinbadTable.Spec s : SinbadTable.specs()) {
            if (s.djinn() == SinbadDjinn.NONE) continue;
            assertFalse(SinbadEquip.allowed(s, SinbadDjinn.NONE, true, 0), s.id() + " passes with nothing on");
            assertTrue(SinbadEquip.allowed(s, s.djinn(), false, plenty), s.id() + " does not pass with its own djinn");
            for (SinbadDjinn other : SinbadDjinn.values()) if (other != s.djinn()) assertFalse(SinbadEquip.allowed(s, other, true, plenty) && s.djinn() != SinbadDjinn.NONE, s.id() + " passes with " + other);
        }
        for (SinbadDjinn d : SinbadDjinn.values()) assertTrue(SinbadEquip.allowed(spec(SinbadTable.SWORD_SLASH), d, false, plenty), "the sword is always there");
    }

    @Test
    void theEquipMoveComesWhenNothingIsOnOrTheEquipIsAboutToEnd() {
        SinbadTable.Spec equip = spec(SinbadTable.DJINN_EQUIP);
        assertTrue(SinbadEquip.allowed(equip, SinbadDjinn.NONE, false, 0));
        assertFalse(SinbadEquip.allowed(equip, SinbadDjinn.BAAL, false, SinbadTable.EQUIP_TICKS));
        assertFalse(SinbadEquip.allowed(equip, SinbadDjinn.BAAL, false, SinbadTable.SWITCH_WINDOW));
        assertTrue(SinbadEquip.allowed(equip, SinbadDjinn.BAAL, false, SinbadTable.SWITCH_WINDOW - 1));
    }

    @Test
    void theFinaleOnlyExistsOnceTheGatesAreOpen() {
        SinbadTable.Spec finale = spec(SinbadTable.SEVEN_SEAS);
        assertFalse(SinbadEquip.allowed(finale, SinbadDjinn.NONE, false, 0));
        assertTrue(SinbadEquip.allowed(finale, SinbadDjinn.NONE, true, 0));
        assertTrue(SinbadEquip.allowed(null, SinbadDjinn.NONE, false, 0), "a move that is not in the table is a plain move");
    }

    @Test
    void theGatesShortenTheEquip() {
        assertEquals(400, SinbadEquip.equipTicks(false));
        assertEquals(240, SinbadEquip.equipTicks(true));
    }

    @Test
    void thePickNeverAnswersNoneOrTheCurrentDjinnAndRespectsTheUnlocks() {
        for (int power = 0; power <= 100; power += 5) {
            for (SinbadDjinn cur : SinbadDjinn.values()) {
                for (double dist : new double[]{2, 15}) {
                    for (int foes : new int[]{1, 4}) {
                        SinbadDjinn d = SinbadEquip.pick(dist, foes, power, cur);
                        assertNotEquals(SinbadDjinn.NONE, d);
                        if (power >= 10) {
                            assertNotEquals(cur, d, "power " + power + " from " + cur);
                            assertTrue(power >= SinbadTable.unlockPower(d), d + " is not unlocked at " + power);
                        }
                    }
                }
            }
        }
        assertEquals(SinbadDjinn.VEPAR, SinbadEquip.pick(3, 4, 100, SinbadDjinn.NONE), "a crowd gets Vepar");
        assertEquals(SinbadDjinn.BAAL, SinbadEquip.pick(20, 1, 100, SinbadDjinn.NONE), "a far target gets Baal");
        assertEquals(SinbadDjinn.CROCELL, SinbadEquip.pick(3, 1, 100, SinbadDjinn.NONE), "a close target gets Crocell");
    }

    @Test
    void theDjinnIdsRoundTripAndAnUnknownOneIsNone() {
        for (SinbadDjinn d : SinbadDjinn.values()) assertEquals(d, SinbadDjinn.byId(d.id));
        assertEquals(SinbadDjinn.NONE, SinbadDjinn.byId("sitri"));
        assertEquals(SinbadDjinn.NONE, SinbadDjinn.byId(""));
        for (SinbadDjinn d : SinbadDjinn.values()) assertEquals(0xFF000000, d.argb & 0xFF000000, d + " is opaque");
    }

    @Test
    void theNumbersAreInTheBalanceRange() {
        assertTrue(SinbadTable.MAGOI_MAGIC_TAKEN > 0f && SinbadTable.MAGOI_MAGIC_TAKEN < 1f);
        assertTrue(SinbadTable.FORESIGHT_CHANCE > 0f && SinbadTable.FORESIGHT_CHANCE <= 0.25f, "the sidestep stays rare");
        assertTrue(SinbadTable.FORESIGHT_COOLDOWN >= 100, "and has a cooldown");
        assertTrue(SinbadTable.INQERAD_DAMAGE >= SinbadTable.SAIQA_DAMAGE && SinbadTable.SAIQA_DAMAGE >= SinbadTable.BOLT_DAMAGE, "Extreme Magic hits hardest");
    }
}
