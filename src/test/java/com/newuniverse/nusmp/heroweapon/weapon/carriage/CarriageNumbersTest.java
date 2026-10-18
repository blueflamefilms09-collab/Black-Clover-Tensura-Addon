package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Vassal Carriage must stay inside the balance budget whatever anybody edits later, and the vehicle's numbers must stay sensible: every technique
 * passes the budget, the ids are the carriage's and unique, the ultimate's row covers both of its blows, the chassis follows the passives, there is a seat
 * for every place the entity promises, the steering turns faster when slow, and the boarding and absence rules behave at their edges.
 */
class CarriageNumbersTest {
    @Test
    void everyTechniqueIsWithinTheBudget() {
        for (Budget.Spec s : CarriageNumbers.ALL) {
            List<String> bad = Budget.violations(s);
            assertTrue(bad.isEmpty(), bad.toString());
        }
        assertEquals(9, CarriageNumbers.ALL.size(), "the carriage has nine techniques (at least six are required)");
    }

    @Test
    void idsAreTheCarriagesAndUnique() {
        Set<String> seen = new HashSet<>();
        for (Budget.Spec s : CarriageNumbers.ALL) {
            assertTrue(s.id().startsWith("carriage."), s.id());
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
        }
        assertEquals(1L, CarriageNumbers.ALL.stream().filter(Budget.Spec::ultimate).count(), "exactly one ultimate");
    }

    @Test
    void theChargeRowCoversBothBlows() {
        assertEquals(14.0, CarriageNumbers.CRASH_CHARGE.rawMax(), 1e-9, "the crash");
        assertEquals(8.0, CarriageNumbers.CRASH_CHARGE.rawBase(), 1e-9, "the stomp");
        assertEquals(2, CarriageNumbers.CRASH_CHARGE.hitsPerTarget());
        assertTrue(CarriageNumbers.CRASH_CHARGE.totalPerTarget() <= 32.0);
        assertTrue(CarriageNumbers.CHARGE_WARNING >= 10, "the charge shows a warning before it starts");
        assertTrue(CarriageNumbers.BOOST_TICKS < 200 && CarriageNumbers.CHARGE_TICKS < 200, "a boost never lasts twenty seconds");
    }

    @Test
    void theChassisFollowsThePassives() {
        CarriageNumbers.Chassis plain = CarriageNumbers.chassis(false, false, false, false);
        assertEquals(40.0, plain.health(), 1e-9);
        assertEquals(0.22, plain.speed(), 1e-9);
        assertEquals(2, plain.seats());
        CarriageNumbers.Chassis swift = CarriageNumbers.chassis(true, true, false, false);
        assertEquals(0.22 * 1.15, swift.speed(), 1e-9);
        assertEquals(3, swift.seats());
        CarriageNumbers.Chassis queen = CarriageNumbers.chassis(true, true, true, true);
        assertEquals(90.0, queen.health(), 1e-9);
        assertEquals(0.22 * 1.3, queen.speed(), 1e-9, "the Queen's Pull replaces the swift wheels, it does not add to them");
    }

    @Test
    void thereIsASeatForEveryPlaceTheEntityPromises() {
        assertTrue(CarriageNumbers.SEATS.length >= CarriageNumbers.SEATS_ROOMY);
        for (double[] seat : CarriageNumbers.SEATS) {
            assertEquals(3, seat.length);
            assertTrue(seat[1] > 0.3 && seat[1] < CarriageNumbers.HEIGHT, "a bench surface is above the floor and under the roof: " + seat[1]);
            assertTrue(Math.abs(seat[0]) < CarriageNumbers.WIDTH / 2.0 && Math.abs(seat[2]) < CarriageNumbers.MODEL_LENGTH / 2.0, "and inside the carriage");
            double head = seat[1] - 0.6 + 1.8;
            assertTrue(head < CarriageNumbers.HEIGHT + 0.3, "a seated rider's head (feet 0.6 below the seat point) is not far above the roof line: " + head);
        }
        assertTrue(CarriageNumbers.SEATS[0][2] > CarriageNumbers.SEATS[1][2] + 0.8, "the driver sits well ahead of the passengers");
        assertEquals(CarriageNumbers.SEATS[1][2], CarriageNumbers.SEATS[2][2], 1e-9, "the two passengers sit side by side");
        assertEquals(-CarriageNumbers.SEATS[1][0], CarriageNumbers.SEATS[2][0], 1e-9, "on either side of the middle");
        assertEquals(0.0, CarriageNumbers.SEATS[0][0], 1e-9, "the driver sits in the middle of the width");
    }

    @Test
    void steeringTurnsFasterWhenSlowAndTheSpeedFactorsRank() {
        assertEquals(9.0, CarriageRules.turnRate(0.0), 1e-9);
        assertEquals(3.0, CarriageRules.turnRate(2.0), 1e-9);
        assertTrue(CarriageRules.turnRate(0.2) > CarriageRules.turnRate(0.5));
        assertEquals(353.0, CarriageRules.steer(350.0, 10.0, 0.5), 1e-9, "at half a block per tick it turns 3 degrees towards the target, the short way round");
        assertEquals(359.0, CarriageRules.steer(350.0, 10.0, 0.0), 1e-9, "standing still it turns 9");
        assertEquals(5.0, CarriageRules.steer(0.0, 5.0, 0.0), 1e-9, "a small turn arrives exactly");
    }

    @Test
    void boardingAndAbsenceBehaveAtTheirEdges() {
        assertTrue(CarriageRules.mayBoard(true, false, false, 0, 2), "the owner boards an empty carriage");
        assertFalse(CarriageRules.mayBoard(false, false, true, 0, 2), "an ally may not board before the owner");
        assertTrue(CarriageRules.mayBoard(false, true, true, 1, 2), "an ally boards once the owner is aboard");
        assertFalse(CarriageRules.mayBoard(false, true, false, 1, 2), "a stranger never boards");
        assertFalse(CarriageRules.mayBoard(true, true, true, 2, 2), "a full carriage takes nobody, the owner included");
        CarriageRules.Absence a = new CarriageRules.Absence();
        for (int i = 0; i < CarriageNumbers.ABSENT_LIMIT; i++) a.tick(false);
        assertFalse(a.expired(CarriageNumbers.ABSENT_LIMIT), "exactly at the limit it still stands");
        a.tick(false);
        assertTrue(a.expired(CarriageNumbers.ABSENT_LIMIT));
        a.tick(true);
        assertFalse(a.expired(CarriageNumbers.ABSENT_LIMIT), "coming back resets the clock");
        assertEquals(1.0, CarriageRules.forwardInput(1.0), 1e-9);
        assertEquals(-0.3, CarriageRules.forwardInput(-1.0), 1e-9);
        assertEquals(1.0, CarriageRules.forwardInput(5.0), 1e-9);
        assertEquals(1.0, CarriageRules.speedFactor(false, false), 1e-9);
        assertTrue(CarriageRules.speedFactor(true, false) > 1.0 && CarriageRules.speedFactor(true, true) > CarriageRules.speedFactor(true, false));
    }

    @Test
    void haikuikkuMultipliesWhateverTheCarriageIsDoing() {
        assertEquals(1.25, CarriageRules.speedFactor(false, false, true), 1e-9);
        assertEquals(1.0, CarriageRules.speedFactor(false, false, false), 1e-9);
        assertEquals(CarriageNumbers.CHARGE_FACTOR * CarriageNumbers.HASTE_FACTOR, CarriageRules.speedFactor(true, true, true), 1e-9, "a charge outranks a boost, haste multiplies it");
        double fastest = CarriageNumbers.BASE_SPEED * CarriageNumbers.QUEEN_SPEED * CarriageRules.speedFactor(false, true, true);
        assertTrue(fastest < 1.0, "even the swiftest carriage stays well under a block per tick of attribute speed: " + fastest);
    }

    @Test
    void thePortalGateExplainsWhyItWillNotOpen() {
        assertEquals("No waypoint is marked yet", CarriageRules.portalProblem(false, true, 100.0));
        assertEquals("The waypoint is in another world", CarriageRules.portalProblem(true, false, 100.0));
        assertTrue(CarriageRules.portalProblem(true, true, 15.9).contains("too near"));
        assertTrue(CarriageRules.portalProblem(true, true, 1000.1).contains("too far"));
        assertEquals(null, CarriageRules.portalProblem(true, true, 16.0), "exactly the minimum is allowed");
        assertEquals(null, CarriageRules.portalProblem(true, true, 1000.0), "exactly the maximum is allowed");
    }
}
