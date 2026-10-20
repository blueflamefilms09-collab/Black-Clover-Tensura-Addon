package com.newuniverse.nusmp.boss.bars;

import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBossController;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialProfile;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialRoster;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the lair action warning of the bar reads {@link DecennialBossController#nextLair()}, a read-only look at the controller's clocks. It must say nothing while no fight runs or a phase shift is
 * on, name a lair action of the boss that is due no earlier than the clocks say, move on after one fires, and never change what the controller does (a run with and without looking is the same).
 */
class NextLairTest {
    private static DecennialProfile boss() {
        DecennialRoster.loadShipped();
        return DecennialRoster.forLevel(5);
    }

    @Test
    void thereIsNothingToWarnAboutBeforeTheFight() {
        DecennialBossController c = new DecennialBossController(boss());
        assertNull(c.nextLair());
        c.tick(1000, 1.0, false, 0);
        assertNull(c.nextLair(), "a party that is not in the hall");
    }

    @Test
    void theFirstLairActionIsNamedAndDueOnTheClock() {
        DecennialProfile p = boss();
        DecennialBossController c = new DecennialBossController(p);
        c.tick(1000, 1.0, true, 0);
        DecennialBossController.NextLair n = c.nextLair();
        assertNotNull(n);
        assertTrue(p.lairs().stream().anyMatch(l -> l.id().equals(n.id())), "one of the boss's own lair actions");
        assertTrue(n.at() >= 1000 + DecennialBossController.FIRST_LAIR_DELAY / 2, "not before the first delay: " + n.at());
        assertTrue(n.at() <= 1000 + DecennialBossController.FIRST_LAIR_DELAY + DecennialBossController.LAIR_STAGGER * p.lairs().size(), "and not absurdly late: " + n.at());
    }

    @Test
    void itMovesOnAfterTheActionFiresAndGoesQuietInAShift() {
        DecennialProfile p = boss();
        DecennialBossController c = new DecennialBossController(p);
        c.tick(1000, 1.0, true, 0);
        DecennialBossController.NextLair first = c.nextLair();
        long t = first.at();
        boolean fired = false;
        for (long now = 1001; now <= t + 5 && !fired; now++) {
            List<DecennialBossController.Event> ev = c.tick(now, 1.0, true, 0);
            for (DecennialBossController.Event e : ev) if (e.kind() == DecennialBossController.Kind.LAIR) fired = true;
        }
        assertTrue(fired, "the lair action fired by its time");
        DecennialBossController.NextLair second = c.nextLair();
        assertNotNull(second);
        assertTrue(second.at() > t, "the next one is later than the one that fired: " + second.at() + " vs " + t);
        c.tick(t + 10, 0.01, true, 0);                                     // a big drop: a phase shift begins
        assertNull(c.nextLair(), "nothing is announced during the shift");
    }
}
