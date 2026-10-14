package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Usurper may only lend the boss a temporary copy: it never removes, steals or changes a player's skills. */
class UsurperPolicyTest {
    private static final List<UsurperPolicy.Seen> SKILLS = List.of(
            new UsurperPolicy.Seen("tensura:heat_wave", UsurperPolicy.Kind.COMMON),
            new UsurperPolicy.Seen("tensura:poison_resistance", UsurperPolicy.Kind.RESISTANCE),
            new UsurperPolicy.Seen("tensura:sage", UsurperPolicy.Kind.UNIQUE),
            new UsurperPolicy.Seen("tensura:raphael", UsurperPolicy.Kind.ULTIMATE),
            new UsurperPolicy.Seen("tensura:body_armor", UsurperPolicy.Kind.INTRINSIC),
            new UsurperPolicy.Seen("tensura:magic_nullification", UsurperPolicy.Kind.RESISTANCE),
            new UsurperPolicy.Seen("tensura:usurper", UsurperPolicy.Kind.UNIQUE),
            new UsurperPolicy.Seen("nusmp:book_time", UsurperPolicy.Kind.OTHER));

    @Test
    void thePlanHasNothingThatRemovesAnything() {
        // the plan names what the BOSS gets and how long; there is no field for anything taken from the target
        List<String> parts = new ArrayList<>();
        for (RecordComponent c : UsurperPolicy.Plan.class.getRecordComponents()) parts.add(c.getName());
        assertEquals(List.of("copyId", "ticks", "drainFraction"), parts);
        assertEquals(400, LuciusNumbers.USURPER_LEND_TICKS, "20 seconds");
        assertTrue(LuciusNumbers.USURPER_DRAIN > 0 && LuciusNumbers.USURPER_DRAIN <= 0.05, "a small drain of regenerating magicules");
    }

    @Test
    void theTargetsSkillsAreTheSameBeforeAndAfter() {
        Set<String> before = new HashSet<>();
        for (UsurperPolicy.Seen s : SKILLS) before.add(s.id());
        Set<String> snapshot = new HashSet<>(before);
        List<UsurperPolicy.Seen> frozen = Collections.unmodifiableList(new ArrayList<>(SKILLS));
        for (int roll = -20; roll < 20; roll++) {
            Optional<UsurperPolicy.Plan> plan = UsurperPolicy.choose(frozen, Set.of(), false, 1000, 0, roll);
            assertEquals(snapshot, UsurperPolicy.playerAfter(before, plan), "roll " + roll);
            assertEquals(snapshot, before, "the input set is never touched");
            assertEquals(SKILLS.size(), frozen.size());
        }
    }

    @Test
    void onlyHarmlessKindsAreCopiedAndNeverTheDeniedOnes() {
        Set<String> picked = new HashSet<>();
        for (int roll = 0; roll < 50; roll++) UsurperPolicy.choose(SKILLS, Set.of(), false, 1000, 0, roll).ifPresent(p -> picked.add(p.copyId()));
        assertEquals(Set.of("tensura:heat_wave", "tensura:poison_resistance", "tensura:sage"), picked);
        assertFalse(UsurperPolicy.copyable(new UsurperPolicy.Seen("tensura:magic_nullification", UsurperPolicy.Kind.RESISTANCE)));
        assertFalse(UsurperPolicy.copyable(new UsurperPolicy.Seen("tensura:anti_skill", UsurperPolicy.Kind.UNIQUE)));
        assertFalse(UsurperPolicy.copyable(new UsurperPolicy.Seen("tensura:tuner", UsurperPolicy.Kind.UNIQUE)));
        assertFalse(UsurperPolicy.copyable(new UsurperPolicy.Seen("tensura:raphael", UsurperPolicy.Kind.ULTIMATE)));
        assertFalse(UsurperPolicy.copyable(null));
        assertFalse(UsurperPolicy.copyable(new UsurperPolicy.Seen("", UsurperPolicy.Kind.COMMON)));
    }

    @Test
    void cooldownLeaseAndOwnedSkillsAreRespected() {
        assertTrue(UsurperPolicy.choose(SKILLS, Set.of(), true, 1000, 0, 0).isEmpty(), "one copy at a time");
        assertTrue(UsurperPolicy.choose(SKILLS, Set.of(), false, 100, 600, 0).isEmpty(), "cooling down");
        assertTrue(UsurperPolicy.choose(SKILLS, Set.of(), false, 600, 600, 0).isPresent());
        Set<String> all = new HashSet<>();
        for (UsurperPolicy.Seen s : SKILLS) all.add(s.id());
        assertTrue(UsurperPolicy.choose(SKILLS, all, false, 1000, 0, 0).isEmpty(), "never a copy over the boss's own skill");
        assertTrue(UsurperPolicy.choose(List.of(), Set.of(), false, 1000, 0, 0).isEmpty());
        assertTrue(UsurperPolicy.choose(null, Set.of(), false, 1000, 0, 0).isEmpty());
        assertEquals(1000 + LuciusNumbers.USURPER_COOLDOWN_TICKS, UsurperPolicy.nextAllowed(1000));
    }

    @Test
    void thePolicyHasNoRemovalApi() {
        for (var m : UsurperPolicy.class.getDeclaredMethods()) {
            String n = m.getName().toLowerCase();
            assertFalse(n.contains("remove") || n.contains("forget") || n.contains("steal") || n.contains("strip"), n);
        }
        assertTrue(Arrays.stream(UsurperPolicy.class.getDeclaredMethods()).anyMatch(m -> m.getName().equals("playerAfter")));
    }
}
