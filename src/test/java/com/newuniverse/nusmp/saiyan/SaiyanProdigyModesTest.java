package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.skill.modes.ModeSpec;
import com.newuniverse.nusmp.skill.rules.DbRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one data table of the Saiyan Prodigy (docs/saiyan_prodigy.md): ids, order, ops, unlocks and the Dragon Ball art formula. Pure. */
class SaiyanProdigyModesTest {
    @Test
    void theTableHasTheFixedModesInTheFixedOrder() {
        assertEquals(18, SaiyanProdigyModes.BUNDLE.size());
        assertEquals("ki_blast", SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.KI_BLAST).id());
        assertEquals("art", SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.ART).id());
        assertEquals("kamehameha", SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.KAMEHAMEHA).id());
        assertEquals("god_bind", SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.GOD_BIND).id());
        assertEquals("style", SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.STYLE).id());
        assertEquals(SaiyanProdigyModes.STYLE, SaiyanProdigyModes.BUNDLE.size() - 1, "the last mode is the newest: modes are only appended");
    }

    @Test
    void everySaiyanOpIsReachableExceptTheTwoNamed() {
        Set<Integer> unreached = new HashSet<>();
        for (int u : SaiyanProdigyModes.UNREACHED) unreached.add(u);
        assertEquals(Set.of(SaiyanNet.OP_FORM_PREV, SaiyanNet2.OP_BRANCH), unreached);
        for (int op = SaiyanNet.OP_BLAST; op <= SaiyanNet2.LAST; op++) {
            assertEquals(!unreached.contains(op), SaiyanProdigyModes.reaches(op), "op " + op);
        }
    }

    @Test
    void everyActionIsAKnownOpOrAProdigyAction() {
        for (ModeSpec m : SaiyanProdigyModes.BUNDLE.modes()) {
            for (int a : new int[] {m.action(), m.sneakAction(), m.releaseAction()}) {
                if (a == 0) continue;
                boolean saiyanOp = a >= SaiyanNet.OP_BLAST && a <= SaiyanNet2.LAST;
                boolean own = a >= SaiyanProdigyModes.ACT_ART && a <= SaiyanProdigyModes.ACT_STYLE_NEXT;
                assertTrue(saiyanOp || own, m.id() + " has an unknown action " + a);
            }
            assertTrue(m.action() != 0, m.id() + " does nothing");
            if (m.isHold()) assertTrue(m.releaseAction() != m.action(), m.id() + " releases with the op it starts with");
        }
    }

    @Test
    void theHoldsAreTheThreeChargedTechniques() {
        Set<String> holds = new HashSet<>();
        for (ModeSpec m : SaiyanProdigyModes.BUNDLE.modes()) if (m.isHold()) holds.add(m.id());
        assertEquals(Set.of("ki_wave", "kamehameha", "spirit_bomb"), holds);
    }

    @Test
    void theGokuLockIsKept() {
        for (String id : new String[] {"kamehameha", "rsp", "instant_transmission", "spirit_bomb", "meteor_combination", "god_bind"}) {
            ModeSpec m = SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.BUNDLE.index(id));
            assertEquals(SaiyanProdigyModes.NEED_GOKU, m.need(), id);
            assertFalse(SaiyanProdigyModes.open(m, true, false), id + " stays closed without the Goku line");
            assertFalse(SaiyanProdigyModes.open(m, false, true), id + " stays closed before the awakening");
            assertTrue(SaiyanProdigyModes.open(m, true, true), id);
        }
        ModeSpec blast = SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.KI_BLAST);
        assertFalse(SaiyanProdigyModes.open(blast, false, false));
        assertTrue(SaiyanProdigyModes.open(blast, true, false));
        assertTrue(SaiyanProdigyModes.open(SaiyanProdigyModes.BUNDLE.at(SaiyanProdigyModes.SENSES), false, false), "senses are open before the awakening, as the old key was");
    }

    @Test
    void theArtsAreTheElevenReworkTechniquesInOrder() {
        assertEquals(SaiyanNet2.CYCLE.length, SaiyanProdigyModes.SAIYAN_ARTS.size());
        for (int i = 0; i < SaiyanNet2.CYCLE.length; i++) assertEquals(SaiyanNet2.CYCLE[i], SaiyanProdigyModes.SAIYAN_ARTS.get(i).op());
        Set<String> ids = new HashSet<>();
        for (SaiyanProdigyModes.Art a : SaiyanProdigyModes.SAIYAN_ARTS) assertTrue(ids.add(a.id()), a.id());
        assertEquals(SaiyanNet2.OP_AVATAR, SaiyanProdigyModes.saiyanArt("giant_ki_avatar").op());
    }

    @Test
    void theDragonBallUnlockGrowsWithTheCharacterAndEveryCharacterIsReachable() {
        assertEquals(1000.0, SaiyanProdigyModes.dbUnlockBp(0.30, 1.0), 1.0e-6);
        double goku = SaiyanProdigyModes.dbUnlockBp(DbRules.GOKU.scale(), 1.0), gohan = SaiyanProdigyModes.dbUnlockBp(DbRules.GOHAN.scale(), 1.0);
        assertTrue(goku > 1.0e6 && goku < 2.0e6, "Goku about 1.44 million: " + goku);
        assertTrue(gohan > goku);
        double prev = 0;
        for (DbRules.Spec s : DbRules.SPECS) {
            double u = SaiyanProdigyModes.dbUnlockBp(s.scale(), 1.0);
            assertTrue(u >= 1000.0 && u <= 2.5e6, s.id() + " " + u);
            assertTrue(u < SaiyanRules.BP_HARD_CAP, s.id() + " opens below the Battle Power cap");
            prev = Math.max(prev, u);
        }
        assertEquals(0.0, SaiyanProdigyModes.dbUnlockBp(1.0, 0.0), "a scale of 0 opens every character at once");
        assertTrue(SaiyanProdigyModes.dbUnlockBp(2.0, 1.0) <= SaiyanProdigyModes.dbUnlockBp(1.0, 1.0) + 1e-9, "a scale above 1 does not open later");
    }

    @Test
    void ownershipOpensAndBattlePowerStrengthens() {
        double unlock = SaiyanProdigyModes.dbUnlockBp(0.97, 1.0);
        assertFalse(SaiyanProdigyModes.dbOpen(0, unlock, false));
        assertTrue(SaiyanProdigyModes.dbOpen(0, unlock, true), "an owner of the old skill keeps it");
        assertTrue(SaiyanProdigyModes.dbOpen(unlock, unlock, false));
        assertFalse(SaiyanProdigyModes.dbUltimateStrength(unlock * 3.9, unlock, false, false));
        assertTrue(SaiyanProdigyModes.dbUltimateStrength(unlock * 4.0, unlock, false, false));
        assertTrue(SaiyanProdigyModes.dbUltimateStrength(0, unlock, true, false), "mastered");
        assertTrue(SaiyanProdigyModes.dbUltimateStrength(0, unlock, false, true), "the old Ultimate is owned");
        assertFalse(SaiyanProdigyModes.dbFinisher(unlock * 15.9, unlock, false, false));
        assertTrue(SaiyanProdigyModes.dbFinisher(unlock * 16.0, unlock, false, false));
        assertTrue(SaiyanProdigyModes.dbFinisher(0, unlock, false, true));
    }

    @Test
    void theWheelOfArtsIsBounded() {
        int most = SaiyanProdigyModes.SAIYAN_ARTS.size();
        for (DbRules.Spec s : DbRules.SPECS) most += s.actives().size();
        assertTrue(most <= SaiyanProdigyModes.MAX_ARTS, "all arts of everyone (" + most + ") fit under the cap");
    }
}
