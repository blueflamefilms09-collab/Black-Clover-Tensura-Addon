package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import com.newuniverse.nusmp.heroweapon.weapon.harpoon.ReelRule.Reel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: reeling in must stay fair against the biggest foes, so who is pulled to whom is proved exactly: light creatures come to the wielder, heavy ones and bosses pull the wielder, the close ones are left alone. */
class ReelRuleTest {
    @Test
    void aZombieIsPulledToTheWielder() {
        assertEquals(Reel.PULL_TARGET, ReelRule.mode(false, 0.6, 1.95, 12.0, 20.0));
    }

    @Test
    void anIronGolemIsTooHeavyAndPullsTheWielderInstead() {
        assertEquals(Reel.PULL_SELF, ReelRule.mode(false, 1.4, 2.7, 12.0, 100.0));
    }

    @Test
    void aBossAlwaysPullsTheWielderWhateverItsSize() {
        assertEquals(Reel.PULL_SELF, ReelRule.mode(true, 0.5, 1.0, 12.0, 20.0));
    }

    @Test
    void aCreatureWithPlentyOfHealthIsHeavyEvenIfSmall() {
        assertEquals(Reel.PULL_SELF, ReelRule.mode(false, 0.6, 1.8, 12.0, 150.0));
        assertEquals(Reel.PULL_TARGET, ReelRule.mode(false, 0.6, 1.8, 12.0, 149.0));
    }

    @Test
    void anythingWithinThreeBlocksIsLeftAlone() {
        assertEquals(Reel.REFUSE, ReelRule.mode(false, 0.6, 1.8, 2.9, 20.0));
        assertEquals(Reel.REFUSE, ReelRule.mode(true, 3.0, 3.0, 1.0, 900.0));
        assertEquals(Reel.PULL_TARGET, ReelRule.mode(false, 0.6, 1.8, 3.0, 20.0));
    }

    @Test
    void theVolumeThresholdIsFourCubicBlocks() {
        assertEquals(Reel.PULL_SELF, ReelRule.mode(false, 2.0, 1.0, 10.0, 10.0), "4.0 exactly is heavy");
        assertEquals(Reel.PULL_TARGET, ReelRule.mode(false, 1.9, 1.0, 10.0, 10.0));
    }
}
