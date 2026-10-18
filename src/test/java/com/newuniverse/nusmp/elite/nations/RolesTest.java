package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Role;
import com.newuniverse.nusmp.elite.nations.core.Roles;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Why: roles and permissions decide who may kick, spend and declare war; the hierarchy rules must hold for every pair. */
class RolesTest {
    private static UUID id(String s) { return UUID.nameUUIDFromBytes(s.getBytes()); }

    private static Nation nation() { return new Nation("n", "N", 0xFFAA00, id("lead"), "Lead", 0, 100); }

    @Test
    void maskRoundTrip() {
        EnumSet<Permission> s = EnumSet.of(Permission.INVITE, Permission.WAR, Permission.ROLES);
        assertEquals(s, Permission.fromMask(Permission.mask(s)));
        assertEquals(0, Permission.mask(null));
        assertEquals(Permission.values().length, Integer.bitCount(Permission.ALL_MASK));
        assertEquals(Permission.TREASURY, Permission.parse(" Treasury "));
        assertNull(Permission.parse("fly"));
        assertNull(Permission.parse(null));
    }

    @Test
    void defaultsHaveLeaderWithEverything() {
        var d = Roles.defaults();
        assertEquals(4, d.size());
        Role leader = d.get(0);
        assertEquals(Roles.LEADER, leader.id());
        assertEquals(Permission.ALL_MASK, leader.permMask());
        assertFalse(d.get(3).has(Permission.INVITE));
        assertTrue(d.get(1).has(Permission.TREASURY));
        assertFalse(d.get(1).has(Permission.WAR), "officers may not declare war by default");
        assertFalse(d.get(1).has(Permission.ROLES));
    }

    @Test
    void hierarchy() {
        var d = Roles.defaults();
        Role leader = d.get(0), officer = d.get(1), member = d.get(2), recruit = d.get(3);
        assertTrue(Roles.canActOn(officer, member));
        assertFalse(Roles.canActOn(officer, officer));
        assertFalse(Roles.canActOn(officer, leader));
        assertTrue(Roles.canActOn(leader, officer));
        assertFalse(Roles.canActOn(null, member));
        assertTrue(Roles.canAssign(leader, officer));
        assertFalse(Roles.canAssign(officer, member), "officer lacks ROLES");
        assertFalse(Roles.canAssign(leader, leader), "nobody hands out their own rank");
        assertTrue(Roles.canEdit(leader, member));
        assertFalse(Roles.canEdit(leader, leader), "the leader role is protected");
        assertFalse(Roles.canEdit(member, recruit));
        assertTrue(Roles.canGrant(leader, Permission.WAR));
        assertFalse(Roles.canGrant(officer, Permission.WAR));
    }

    @Test
    void newRoleRankIsBelowActor() {
        Role leader = Roles.defaults().get(0), officer = Roles.defaults().get(1);
        assertEquals(Roles.OFFICER_RANK - 1, Roles.rankForNewRole(leader));
        assertEquals(Roles.OFFICER_RANK - 1, Roles.rankForNewRole(officer));
        assertEquals(Roles.RECRUIT_RANK, Roles.rankForNewRole(new Role("x", "x", 5, 0)));
    }

    @Test
    void validation() {
        assertTrue(Roles.validId("scout_2"));
        assertFalse(Roles.validId("Scout"));
        assertFalse(Roles.validId(""));
        assertFalse(Roles.validId(null));
        assertTrue(Roles.validName("Master of Coin"));
        assertFalse(Roles.validName("   "));
        assertFalse(Roles.validName("x".repeat(25)));
    }

    @Test
    void membershipAndPermissions() {
        Nation n = nation();
        UUID lead = id("lead"), bob = id("bob"), eve = id("eve");
        assertTrue(n.can(lead, Permission.WAR));
        assertTrue(n.addMember(bob, "Bob", Roles.MEMBER));
        assertFalse(n.addMember(bob, "Bob", Roles.MEMBER), "already a member");
        assertTrue(n.can(bob, Permission.BUILD));
        assertFalse(n.can(bob, Permission.KICK));
        assertFalse(n.can(eve, Permission.BUILD), "not a member");
        assertTrue(n.setRole(bob, Roles.OFFICER));
        assertTrue(n.can(bob, Permission.KICK));
        assertFalse(n.setRole(bob, Roles.LEADER), "the leader role is only handed over by transfer");
        assertFalse(n.setRole(lead, Roles.MEMBER), "the leader keeps the leader role");
        assertFalse(n.removeMember(lead));
        assertTrue(n.removeMember(bob));
        assertEquals(1, n.memberCount());
    }

    @Test
    void customRoleChangesPermissions() {
        Nation n = nation();
        UUID bob = id("bob");
        n.addMember(bob, "Bob", Roles.MEMBER);
        n.roles.put("banker", new Role("banker", "Banker", 40, Permission.TREASURY.bit()));
        assertTrue(n.setRole(bob, "banker"));
        assertTrue(n.can(bob, Permission.TREASURY));
        assertFalse(n.can(bob, Permission.BUILD));
        n.roles.put("banker", n.role("banker").withPerm(Permission.TREASURY, false).withPerm(Permission.WAR, true));
        assertFalse(n.can(bob, Permission.TREASURY));
        assertTrue(n.can(bob, Permission.WAR));
        // a deleted role falls back to recruit instead of breaking the member
        n.roles.remove("banker");
        assertNotNull(n.roleOf(bob));
        assertEquals(Roles.RECRUIT, n.roleOf(bob).id());
    }

    @Test
    void leadershipTransfer() {
        Nation n = nation();
        UUID lead = id("lead"), bob = id("bob");
        n.addMember(bob, "Bob", Roles.MEMBER);
        assertFalse(n.transferLeadership(lead));
        assertFalse(n.transferLeadership(id("nobody")));
        assertTrue(n.transferLeadership(bob));
        assertEquals(bob, n.leader);
        assertEquals(Roles.LEADER, n.roleOf(bob).id());
        assertEquals(Roles.OFFICER, n.roleOf(lead).id());
        assertEquals(1, n.countRole(Roles.LEADER));
    }

    @Test
    void namesAndSlugs() {
        assertEquals("black_clover", Nation.slug("  Black Clover! "));
        assertTrue(Nation.validName("Spade Kingdom", 3, 24));
        assertFalse(Nation.validName("ab", 3, 24));
        assertFalse(Nation.validName("Bad<name>", 3, 24));
        assertFalse(Nation.validName("!!!", 3, 24), "a slug must remain");
    }
}
