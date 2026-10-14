package com.newuniverse.nusmp.charter;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The non-destructive law: a held grimoire only ever adds, on loan. */
class MirrorPolicyTest {
    private static List<String> ids(List<MirrorPolicy.Lease> l) {
        List<String> out = new ArrayList<>();
        for (MirrorPolicy.Lease x : l) out.add(x.skillId());
        return out;
    }

    @Test
    void onlyAdditionsAndEveryOneIsALease() {
        List<MirrorPolicy.Lease> plan = MirrorPolicy.plan(Set.of(), CharterTable.byId("dante").skills());
        assertEquals(CharterTable.byId("dante").skills(), ids(plan));
        for (MirrorPolicy.Lease l : plan) assertEquals(MirrorPolicy.LEASE_TICKS, l.ticks());
        assertTrue(MirrorPolicy.LEASE_TICKS >= 40 && MirrorPolicy.LEASE_TICKS <= 100, "about three seconds after it is put away");
    }

    @Test
    void aSkillThePlayerOwnsIsLeftAlone() {
        List<MirrorPolicy.Lease> plan = MirrorPolicy.plan(Set.of("tensura:gravity_field", "tensura:burden"), CharterTable.byId("dante").skills());
        assertFalse(ids(plan).contains("tensura:gravity_field"));
        assertFalse(ids(plan).contains("tensura:burden"));
        assertEquals(CharterTable.byId("dante").skills().size() - 2, plan.size());
        assertTrue(MirrorPolicy.plan(new ArrayList<>(CharterTable.byId("dante").skills()), CharterTable.byId("dante").skills()).isEmpty(), "a player who owns the whole set is lent nothing");
    }

    @Test
    void theThiefsSkillIsNeverLent() {
        assertTrue(MirrorPolicy.plan(Set.of(), List.of("tensura:usurper", "usurper", "TENSURA:USURPER", "tensura:tuner")).stream().noneMatch(l -> l.skillId().contains("usurper")));
        assertEquals(List.of("tensura:tuner"), ids(MirrorPolicy.plan(Set.of(), List.of("tensura:usurper", "tensura:tuner"))));
    }

    @Test
    void duplicatesBlanksAndBareIds() {
        assertEquals(List.of("tensura:sage", "tensura:burden"), ids(MirrorPolicy.plan(Set.of(), java.util.Arrays.asList("sage", "tensura:sage", null, " ", "Burden"))));
        assertTrue(MirrorPolicy.plan(null, null).isEmpty());
        assertTrue(MirrorPolicy.plan(Set.of(), List.of("tensura:sage"), 0).isEmpty());
        assertEquals(List.of("tensura:sage"), ids(MirrorPolicy.plan(Set.of("TENSURA:BURDEN"), List.of("tensura:burden", "tensura:sage"))));
    }

    @Test
    void severalHeldBooksMergeInOrderWithoutRepeats() {
        List<String> u = MirrorPolicy.union(List.of(CharterTable.byId("lucius"), CharterTable.byId("zenon")));
        assertEquals(new java.util.HashSet<>(u).size(), u.size());
        assertTrue(u.contains("tensura:anti_skill") && u.contains("tensura:spatial_motion"));
        assertEquals(1, u.stream().filter("tensura:dimension_cutter"::equals).count(), "both books lend dimension cutter: once");
        assertEquals("tensura:anti_skill", u.get(0));
    }

    @Test
    void theAbsoluteCapHolds() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 200; i++) many.add("tensura:skill_" + i);
        assertEquals(MirrorPolicy.MAX_LENT, MirrorPolicy.plan(Set.of(), many).size());
    }

    @Test
    void thePolicyCannotExpressRemovingAnything() {
        for (Method m : MirrorPolicy.class.getDeclaredMethods()) {
            String n = m.getName().toLowerCase();
            assertFalse(n.contains("remove") || n.contains("forget") || n.contains("reset") || n.contains("replace") || n.contains("steal"), m.getName());
        }
    }
}
