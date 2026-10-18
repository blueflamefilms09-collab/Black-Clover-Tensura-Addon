package com.newuniverse.nusmp.elite.nations.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: the rules of roles and permissions, pure so they can be tested without the game. A new nation starts with four roles (leader, officer, member, recruit); the leader role always holds every
 * permission and cannot be edited or deleted. A custom role is created below the actor's own rank and an actor can only touch players and roles ranked strictly below them (so an officer cannot kick the
 * leader or promote themselves).
 */
public final class Roles {
    private Roles() {}

    public static final String LEADER = "leader", OFFICER = "officer", MEMBER = "member", RECRUIT = "recruit";
    public static final int LEADER_RANK = 100, OFFICER_RANK = 60, MEMBER_RANK = 30, RECRUIT_RANK = 10;
    public static final int MAX_ROLE_NAME = 24;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,24}");

    public static List<Role> defaults() {
        List<Role> out = new ArrayList<>();
        out.add(new Role(LEADER, "Leader", LEADER_RANK, Permission.ALL_MASK));
        out.add(new Role(OFFICER, "Officer", OFFICER_RANK, Permission.INVITE.bit() | Permission.KICK.bit() | Permission.TREASURY.bit() | Permission.CONTRACTS.bit() | Permission.BUILD.bit() | Permission.DIPLOMACY.bit()));
        out.add(new Role(MEMBER, "Member", MEMBER_RANK, Permission.CONTRACTS.bit() | Permission.BUILD.bit()));
        out.add(new Role(RECRUIT, "Recruit", RECRUIT_RANK, 0));
        return out;
    }

    public static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }

    public static boolean validName(String name) {
        if (name == null) return false;
        String t = name.trim();
        return !t.isEmpty() && t.length() <= MAX_ROLE_NAME && t.chars().allMatch(c -> c >= 32 && c != 127);
    }

    public static boolean isProtected(String roleId) { return LEADER.equals(roleId); }

    /** May the actor act on (kick, re-role) a player who holds {@code target}? Strictly higher rank only. */
    public static boolean canActOn(Role actor, Role target) { return actor != null && target != null && actor.rank() > target.rank(); }

    /** May the actor give out {@code newRole}? Needs ROLES and a strictly higher rank than the role handed out. */
    public static boolean canAssign(Role actor, Role newRole) { return actor != null && newRole != null && actor.has(Permission.ROLES) && actor.rank() > newRole.rank(); }

    /** The rank a custom role gets when created by {@code actor}: just under the actor, never above the officer rank and never below recruit. */
    public static int rankForNewRole(Role actor) {
        if (actor == null) return RECRUIT_RANK;
        return Math.max(RECRUIT_RANK, Math.min(actor.rank() - 1, OFFICER_RANK - 1));
    }

    /** May the actor edit the permissions / name of {@code target}? Not the leader role, and only a role strictly below their own. */
    public static boolean canEdit(Role actor, Role target) {
        return actor != null && target != null && !isProtected(target.id()) && actor.has(Permission.ROLES) && actor.rank() > target.rank();
    }

    /** May the actor grant {@code p} to a role? An actor can only hand out what they hold themselves. */
    public static boolean canGrant(Role actor, Permission p) { return actor != null && actor.has(p); }
}
