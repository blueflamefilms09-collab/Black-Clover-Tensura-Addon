package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Rankings;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: every devil of docs/devil_worlds.md has a rank in the relics' own ranking language (docs/devil_double_magic.md), no ranking text is invented, and the dark-magic list is real data. */
class DevilRanksTest {

    @Test
    void theFourTiersMapOntoFourRelicGrades() {
        assertEquals(Grade.RARE, DevilRanks.gradeOf(DevilTier.LOW));
        assertEquals(Grade.UNIQUE, DevilRanks.gradeOf(DevilTier.MID));
        assertEquals(Grade.LEGEND, DevilRanks.gradeOf(DevilTier.HIGH));
        assertEquals(Grade.GOD, DevilRanks.gradeOf(DevilTier.SUPREME));
    }

    @Test
    void everyDevilIsRankedInItsTiersGradeWithAPlaceOfItsOwn() {
        Set<String> used = new HashSet<>();
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            Rankings.Rank r = DevilRanks.of(d);
            assertEquals(DevilRanks.gradeOf(d.tier()), r.grade(), d.id() + " is ranked outside its tier's grade");
            assertTrue(r.place() > 0, d.id() + " has no place");
            assertEquals("item", r.source(), d.id() + " fell back to the tier");
            assertTrue(used.add(r.grade() + "#" + r.place()), d.id() + " shares its place");
        }
    }

    @Test
    void theFourOldPactDevilsAreRankedToo() {
        for (String pact : new String[] {"ignivar", "verbael", "gravemaw", "kairovore"}) {
            Rankings.Rank r = DevilRanks.ofPact(pact, DevilTier.MID);
            assertTrue(r.place() > 0, pact + " has no place");
            assertEquals(Grade.UNIQUE, r.grade());
        }
    }

    @Test
    void aDevilAddedLaterIsNeverUnranked() {
        Rankings.Rank r = DevilRanks.of("a_devil_from_a_later_version", DevilTier.HIGH);
        assertEquals(Grade.LEGEND, r.grade());
        assertEquals(0, r.place());
        assertFalse(DevilRanks.line(r).isEmpty());
        assertEquals(Grade.RARE, DevilRanks.of(null, DevilTier.LOW).grade());
    }

    @Test
    void theDarkMagicListIsRealData() {
        DarkRitualRules.Dark d = DarkRitualRules.table();
        assertTrue(d.magics().contains("DARK") && d.magics().contains("SHADOW") && d.magics().contains("CURSE"));
        assertTrue(d.attributes().contains("CURSE") && d.attributes().contains("GHOST"));
        assertTrue(d.skills().size() > 5 && d.skills().size() <= DarkRitualRules.MAX_SKILLS);
        for (String s : d.skills()) assertTrue(s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), s + " is not a skill id");
        assertTrue(DarkRitualRules.isDark(d, "dark", null) && DarkRitualRules.isDark(d, null, "ghost"));
        assertFalse(DarkRitualRules.isDark(d, "FLAME", "FLAME") || DarkRitualRules.isDark(d, null, null));
    }

    @Test
    void theDarkRuleHasFourGroundsAndAnOffSwitch() {
        assertEquals(DarkRitualRules.Why.NONE, DarkRitualRules.allow(true, false, false, false, 15, 4));
        assertEquals(DarkRitualRules.Why.MAGIC, DarkRitualRules.allow(true, true, false, false, 15, 4));
        assertEquals(DarkRitualRules.Why.DIMENSION, DarkRitualRules.allow(true, false, true, false, 15, 4));
        assertEquals(DarkRitualRules.Why.BIOME, DarkRitualRules.allow(true, false, false, true, 15, 4));
        assertEquals(DarkRitualRules.Why.LIGHT, DarkRitualRules.allow(true, false, false, false, 4, 4));
        assertEquals(DarkRitualRules.Why.NONE, DarkRitualRules.allow(true, false, false, false, 5, 4));
        assertEquals(DarkRitualRules.Why.OFF, DarkRitualRules.allow(false, false, false, false, 15, 4));
        assertTrue(DarkRitualRules.needsLightCheck(DarkRitualRules.Why.LIGHT));
        assertFalse(DarkRitualRules.needsLightCheck(DarkRitualRules.Why.MAGIC));
        assertFalse(DarkRitualRules.lightBroken(4 + DarkRitualRules.LIGHT_GRACE, 4));
        assertTrue(DarkRitualRules.lightBroken(5 + DarkRitualRules.LIGHT_GRACE, 4));
    }
}
