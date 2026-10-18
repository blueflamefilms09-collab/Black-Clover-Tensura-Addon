package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the Katana's numbers must stay inside the owner's balance budget (docs/heroweapon/design.md section 5): this holds every technique's row to
 * {@link Budget}, checks the weapon has the nine techniques the design promises (at least six are required) with the ultimate where it belongs, and pins the
 * tempo arithmetic: the most tempo adds only 1.5 to a cut and 0.75 to a thrust, and the dance of sakura at full tempo is exactly 30, under the ultimate limit.
 */
class KatanaNumbersTest {
    @Test
    void everyTechniqueObeysTheBudget() {
        List<String> bad = Budget.violations(KatanaNumbers.table());
        assertTrue(bad.isEmpty(), "budget violations: " + bad);
    }

    @Test
    void theWeaponHasNineTechniquesAndOneUltimate() {
        assertEquals(9, KatanaNumbers.table().size());
        int ultimates = 0;
        List<String> ids = new ArrayList<>();
        for (Budget.Spec s : KatanaNumbers.table()) {
            assertTrue(s.id().startsWith("katana."), "every id starts with katana.");
            if (s.kind() == Budget.Kind.ULTIMATE) ultimates++;
            ids.add(s.id());
        }
        assertEquals(1, ultimates);
        assertEquals(Budget.Kind.ULTIMATE, KatanaNumbers.spec(KatanaNumbers.SAKURA_DANCE).kind());
        assertEquals(KatanaText.skillIds(), ids, "the text table and the number table list the same skills in the same order");
    }

    @Test
    void anUnknownSkillIsAnErrorNotADefault() {
        boolean threw = false;
        try {
            KatanaNumbers.spec("katana.nope");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    void tempoAddsALittleAndIsCappedAtThree() {
        assertEquals(6.5, KatanaNumbers.crescentRaw(0), 1e-9);
        assertEquals(8.0, KatanaNumbers.crescentRaw(3), 1e-9);
        assertEquals(8.0, KatanaNumbers.crescentRaw(9), 1e-9, "tempo above 3 counts as 3");
        assertEquals(6.5, KatanaNumbers.crescentRaw(-2), 1e-9);
        assertEquals(10.5, KatanaNumbers.instantRaw(3), 1e-9);
        assertEquals(5.75, KatanaNumbers.thrustRaw(3), 1e-9);
        assertEquals(5.0, KatanaNumbers.thrustRaw(0), 1e-9);
        assertTrue(KatanaNumbers.crescentRaw(3) <= Budget.Kind.ORDINARY.maxHit());
        assertTrue(KatanaNumbers.crescentRaw(3) <= 8.0, "the plain cut never passes 8 raw, so it needs no tell");
        assertTrue(KatanaNumbers.instantRaw(3) <= Budget.Kind.ORDINARY.maxHit());
        assertTrue(3 * KatanaNumbers.thrustRaw(3) <= Budget.Kind.HEAVY.maxTotal());
    }

    @Test
    void theDanceOfSakuraGrowsWithTempoAndStaysUnderTheUltimateLimit() {
        assertEquals(18.0, KatanaNumbers.sakuraTotal(0), 1e-9);
        assertEquals(30.0, KatanaNumbers.sakuraTotal(3), 1e-9);
        assertEquals(30.0, KatanaNumbers.sakuraTotal(8), 1e-9);
        assertEquals(10.0, KatanaNumbers.sakuraFormation(3), 1e-9);
        assertEquals(6.0, KatanaNumbers.sakuraFormation(0), 1e-9);
        assertEquals(30.0, KatanaNumbers.spec(KatanaNumbers.SAKURA_DANCE).total(), 1e-9);
        assertTrue(KatanaNumbers.sakuraTotal(KatanaNumbers.TEMPO_MAX) <= Budget.Kind.ULTIMATE.maxTotal());
    }

    @Test
    void thePhantomBladeIsBoundedByItsCutsNotByTime() {
        Budget.Spec s = KatanaNumbers.spec(KatanaNumbers.DOUBLE_SWORD);
        assertEquals(4, s.hitsPerTarget());
        assertEquals(10.0, s.total(), 1e-9);
        assertTrue(KatanaNumbers.OPENING_RAW <= Budget.Kind.ORDINARY.maxHit());
        assertTrue(KatanaNumbers.RIPOSTE_RAW <= Budget.Kind.ORDINARY.maxHit());
        assertTrue(KatanaNumbers.PARRY_NEGATE_SHARE < 1.0, "a parry never makes the wielder immune");
    }
}
