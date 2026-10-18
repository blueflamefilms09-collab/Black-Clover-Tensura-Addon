package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the Book's numbers must stay inside the owner's balance budget (docs/heroweapon/design.md section 5): this holds every technique's row to {@link Budget},
 * checks the weapon has the seven techniques the design promises (at least six are required) with the ultimate where it belongs, and pins what a page can do
 * even revised: no offence page passes the ordinary limit of 12 on one foe, the Life Force Burst the heavy limit of 20, Libraria the ultimate limit of 32, and
 * the Healing Verse never passes a quarter of a friend's health however the numbers are scaled.
 */
class BookNumbersTest {
    @Test
    void everyTechniqueObeysTheBudget() {
        List<String> bad = Budget.violations(BookNumbers.table());
        assertTrue(bad.isEmpty(), "budget violations: " + bad);
    }

    @Test
    void theWeaponHasSevenTechniquesAndOneUltimate() {
        assertEquals(7, BookNumbers.table().size());
        int ultimates = 0;
        List<String> ids = new ArrayList<>();
        for (Budget.Spec s : BookNumbers.table()) {
            assertTrue(s.id().startsWith("book."), "every id starts with book.");
            if (s.kind() == Budget.Kind.ULTIMATE) ultimates++;
            ids.add(s.id());
        }
        assertEquals(1, ultimates);
        assertEquals(Budget.Kind.ULTIMATE, BookNumbers.spec(BookNumbers.LIBRARIA).kind());
        assertEquals(BookText.skillIds(), ids, "the text table and the number table list the same skills in the same order");
    }

    @Test
    void anUnknownSkillIsAnErrorNotADefault() {
        boolean threw = false;
        try {
            BookNumbers.spec("book.nope");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    void noOffencePageEvenRevisedPassesTheOrdinaryLimit() {
        double limit = Budget.Kind.ORDINARY.maxTotal();
        for (boolean revised : new boolean[] {false, true}) {
            assertTrue(BookNumbers.pageRaw(BookNumbers.BIRD_RAW, 1, revised) <= limit);
            assertTrue(BookNumbers.pageRaw(BookNumbers.NEEDLE_RAW, BookNumbers.NEEDLE_PULSES, revised) * BookNumbers.NEEDLE_PULSES <= limit + 1e-9, "needles revised=" + revised);
            assertTrue(BookNumbers.pageRaw(BookNumbers.FEATHER_RAW, 1, revised) <= limit);
            assertTrue(BookNumbers.pageRaw(BookNumbers.BURST_RAW, 1, revised) <= limit);
            assertTrue(BookNumbers.pageRaw(BookNumbers.CAGE_CEILING_RAW, 1, revised) <= limit);
        }
        assertEquals(6.0, BookNumbers.pageRaw(BookNumbers.BIRD_RAW, 1, false), 1e-9);
        assertEquals(7.5, BookNumbers.pageRaw(BookNumbers.BIRD_RAW, 1, true), 1e-9);
        assertTrue(BookNumbers.pageRaw(BookNumbers.BIRD_RAW, 1, true) <= 8.0, "the bird flies without a tell, so even revised it stays at 8 raw or less");
        assertEquals(3.0, BookNumbers.pageRaw(BookNumbers.NEEDLE_RAW, 4, true), 1e-9, "3.125 would make the rain 12.5, so it is held to 3 a volley");
        assertEquals(2.5, BookNumbers.pageRaw(BookNumbers.NEEDLE_RAW, 4, false), 1e-9);
        assertEquals(11.25, BookNumbers.pageRaw(BookNumbers.FEATHER_RAW, 1, true), 1e-9);
    }

    @Test
    void theHeavyBurstAndTheUltimateStayUnderTheirLimits() {
        assertEquals(14.0, BookNumbers.heavyRaw(BookNumbers.LIFE_RAW, false), 1e-9);
        assertEquals(17.5, BookNumbers.heavyRaw(BookNumbers.LIFE_RAW, true), 1e-9);
        assertEquals(20.0, BookNumbers.heavyRaw(100.0, true), 1e-9, "never past 20");
        assertEquals(28.0, BookNumbers.volleyRaw(false) * BookNumbers.volleys(false), 1e-9);
        assertEquals(32.0, BookNumbers.volleyRaw(true) * BookNumbers.volleys(true), 1e-9);
        assertEquals(32.0, BookNumbers.spec(BookNumbers.LIBRARIA).total(), 1e-9);
        assertTrue(BookNumbers.volleys(true) * BookNumbers.LIB_INTERVAL + BookNumbers.LIB_DELAY <= Budget.MAX_DURATION_TICKS);
    }

    @Test
    void theHealingVerseNeverPassesAQuarterOfAFriendsHealth() {
        assertEquals(3.6, BookNumbers.versePulse(100.0, false), 1e-9, "1.6 and 2 percent of 100");
        assertEquals(4.5, BookNumbers.versePulse(100.0, true), 1e-9, "25 percent more when revised");
        assertEquals(20.0 * 0.25 / 3, BookNumbers.versePulse(20.0, false), 1e-9, "for a 20 health friend the quarter cap already binds");
        assertEquals(20.0 * 0.25 / 3, BookNumbers.versePulse(20.0, true), 1e-9);
        for (double hp = 1.0; hp < 3000.0; hp += 11.0) {
            for (boolean revised : new boolean[] {false, true}) {
                double pulse = BookNumbers.versePulse(hp, revised);
                assertTrue(pulse >= 0.0);
                assertTrue(pulse * BookNumbers.VERSE_PULSES <= 0.25 * hp + 1e-9, "three pulses never pass a quarter at " + hp + " revised=" + revised);
            }
        }
        assertEquals(0.0, BookNumbers.versePulse(0.0, false), 1e-9);
        assertEquals(0.4, BookNumbers.featherHeal(20.0), 1e-9);
    }
}
