package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Skills lent by meals: a skill the player owns is never touched, a lent one is only ever extended, and every lease ends. */
class SkillLeaseTest {
    @Test
    void aMissingSkillIsLearnedForTheMealsTime() {
        SkillLease.Plan p = SkillLease.grant(false, false, 0L, 1000L, 6000);
        assertEquals(SkillLease.Action.LEARN, p.action());
        assertEquals(7000L, p.until());
    }

    @Test
    void aSkillTheyOwnIsLeftAloneAndNoLeaseIsWritten() {
        SkillLease.Plan p = SkillLease.grant(true, false, 0L, 1000L, 6000);
        assertEquals(SkillLease.Action.NONE, p.action());
    }

    @Test
    void aSecondMealExtendsTheLeaseAndNeverShortensIt() {
        SkillLease.Plan longer = SkillLease.grant(true, true, 7000L, 2000L, 6000);
        assertEquals(SkillLease.Action.EXTEND, longer.action());
        assertEquals(8000L, longer.until());
        SkillLease.Plan shorter = SkillLease.grant(true, true, 7000L, 2000L, 1000);
        assertEquals(SkillLease.Action.EXTEND, shorter.action());
        assertEquals(7000L, shorter.until(), "a short meal cannot cut a long lease");
    }

    @Test
    void aLeaseWhoseSkillIsGoneTeachesItAgain() {
        SkillLease.Plan p = SkillLease.grant(false, true, 7000L, 2000L, 1000);
        assertEquals(SkillLease.Action.LEARN, p.action());
        assertEquals(7000L, p.until());
    }

    @Test
    void anExpiredLeaseThatWasNotSweptYetRunsFromNowWhenRenewed() {
        SkillLease.Plan p = SkillLease.grant(true, true, 500L, 9000L, 3600);
        assertEquals(SkillLease.Action.EXTEND, p.action());
        assertEquals(12600L, p.until());
    }

    @Test
    void aMealWithNoTimeDoesNothing() {
        assertEquals(SkillLease.Action.NONE, SkillLease.grant(false, false, 0L, 1000L, 0).action());
        assertEquals(SkillLease.Action.NONE, SkillLease.grant(false, false, 0L, 1000L, -5).action());
    }

    @Test
    void onlyLeasesThatHaveRunOutExpire() {
        Map<String, Long> until = new LinkedHashMap<>();
        until.put("tensura:magic_sense", 900L);
        until.put("tensura:poison_resistance", 1000L);
        until.put("tensura:danger_sense", 1001L);
        assertEquals(List.of("tensura:magic_sense", "tensura:poison_resistance"), SkillLease.expired(until, 1000L));
        assertEquals(List.of(), SkillLease.expired(until, 100L));
        assertEquals(List.of(), SkillLease.expired(new LinkedHashMap<>(), 100L));
        assertEquals(3, SkillLease.expired(until, 5000L).size());
    }
}
