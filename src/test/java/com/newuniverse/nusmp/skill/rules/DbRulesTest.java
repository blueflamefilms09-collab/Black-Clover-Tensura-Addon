package com.newuniverse.nusmp.skill.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DbRulesTest {
    @Test void fifteenCharactersWithUniqueIds() {
        assertEquals(15, DbRules.SPECS.size());
        Set<String> ids = new HashSet<>();
        for (Spec s : DbRules.SPECS) {
            assertTrue(s.id().matches("db_[a-z0-9_]+"), s.id());
            assertTrue(ids.add(s.id()) && ids.add(s.uniqueId()), "duplicate id " + s.id());
            assertSame(s, DbRules.spec(s.id()));
            assertSame(s, DbRules.spec(s.uniqueId()));
        }
    }

    @Test void everySkillHasPassiveTwoOrMoreActivesAndOneFinisher() {
        for (Spec s : DbRules.SPECS) {
            assertFalse(s.passives().isEmpty(), s.id() + " passive");
            assertEquals(2, s.uniqueActives().size(), s.id() + " unique actives");
            assertNotNull(s.finisher(), s.id() + " finisher");
            long fins = s.modes().stream().filter(Mode::mastery).count();
            assertEquals(1, fins, s.id());
            Set<String> modeIds = new HashSet<>();
            for (Mode m : s.modes()) {
                assertTrue(modeIds.add(m.id()), s.id() + " duplicate mode " + m.id());
                assertTrue(m.move().dmg() >= 0 && m.cost() >= 0 && m.masteredCost() <= m.cost() && m.masteredCooldown() <= m.cooldown(), s.id() + "." + m.id());
                if (!m.passive()) {
                    assertTrue(m.cooldown() > 0, s.id() + "." + m.id() + " cooldown");
                    assertTrue(m.move().range() >= 0 && m.move().radius() >= 0 && m.move().n() >= 0, m.id());
                    assertTrue(m.move().n() <= DbRules.MAX_TARGETS || m.move().prim() == DbRules.Prim.BARRAGE, m.id() + " n");
                }
            }
        }
    }

    @Test void gohanBeastIsTheTopAndBulmaTheLowest() {
        double max = 0, min = 9;
        for (Spec s : DbRules.SPECS) { max = Math.max(max, s.scale()); min = Math.min(min, s.scale()); }
        assertEquals(DbRules.GOHAN.scale(), max, 1e-9);
        assertEquals(DbRules.BULMA.scale(), min, 1e-9);
    }

    @Test void uniqueIsHalfAnUltimateAndLocksAreCapped() {
        assertEquals(DbRules.power(DbRules.GOKU, true) * DbRules.UNIQUE_FACTOR, DbRules.power(DbRules.GOKU, false), 1e-9);
        assertEquals(DbRules.MAX_LOCK_TICKS, DbRules.lock(new DbRules.Move(DbRules.Prim.SEAL, 0, 0, 0, 0, 0, 99999, 0, 0)));
        assertEquals(0.0, DbRules.stored(-5, 60), 1e-9);
        assertEquals(60.0, DbRules.stored(1000, 60), 1e-9);
        assertTrue(DbRules.lowHealth(6, 20) && !DbRules.lowHealth(8, 20));
    }

    @Test void everySkillHasItsIcon() {
        for (Spec s : DbRules.SPECS) for (String id : new String[] {s.id(), s.uniqueId()})
            assertTrue(new File("src/main/resources/assets/nusmp/textures/skill/" + id + ".png").isFile(), "icon " + id);
    }
}
