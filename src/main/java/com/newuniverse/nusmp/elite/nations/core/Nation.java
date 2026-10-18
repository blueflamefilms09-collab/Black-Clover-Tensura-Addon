package com.newuniverse.nusmp.elite.nations.core;

import com.newuniverse.nusmp.elite.nations.econ.Ledger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the state of one nation, with no Minecraft types so the rules around it (roles, membership, metrics) can be tested. A nation is a persistent political entity BIGGER than a dungeon party or an FTB
 * squad: it has a name, a banner colour, a founder/leader, members each holding a role, custom roles, a capital position, a treasury ledger, nation XP and the metrics the level is computed from.
 * Mutated only by the server-side services; saved by {@code NationNbt}. The id is a stable slug (lower-case name at founding) that never changes on rename.
 */
public final class Nation {
    public final String id;
    public String name;
    public int color;
    public UUID leader;
    public long createdAt;

    public String capitalDim = "";
    public int cx, cy, cz;
    public boolean hasCapital;

    public final Map<UUID, String> members = new LinkedHashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    public final Map<String, Role> roles = new LinkedHashMap<>();
    public final Ledger ledger;

    public long xp;
    public int level = 1, peakLevel = 1;
    public int completedContracts, warWins, warLosses, areaScore;
    public int taxPercent;
    public long lastInterest, lastGuardTick;
    /** Pending invitations: player id to expiry (game ticks). Not saved: an invitation does not survive a restart. */
    public final Map<UUID, Long> invites = new HashMap<>();

    public Nation(String id, String name, int color, UUID leader, String leaderName, long createdAt, int ledgerCap) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.leader = leader;
        this.createdAt = createdAt;
        this.ledger = new Ledger(ledgerCap);
        for (Role r : Roles.defaults()) roles.put(r.id(), r);
        members.put(leader, Roles.LEADER);
        if (leaderName != null) names.put(leader, leaderName);
    }

    public int memberCount() { return members.size(); }

    public boolean isMember(UUID p) { return p != null && members.containsKey(p); }

    public Role roleOf(UUID p) {
        String rid = p == null ? null : members.get(p);
        if (rid == null) return null;
        Role r = roles.get(rid);
        return r != null ? r : roles.get(Roles.RECRUIT);
    }

    /** True when the player is a member whose role holds the permission. The leader always passes. */
    public boolean can(UUID p, Permission perm) {
        if (p == null || !members.containsKey(p)) return false;
        if (p.equals(leader)) return true;
        Role r = roleOf(p);
        return r != null && r.has(perm);
    }

    public Role role(String roleId) { return roleId == null ? null : roles.get(roleId); }

    public String nameOf(UUID p) {
        String n = names.get(p);
        return n != null ? n : (p == null ? "?" : p.toString().substring(0, 8));
    }

    /** Adds a member with the default entry role. Returns false if already a member. */
    public boolean addMember(UUID p, String playerName, String roleId) {
        if (p == null || members.containsKey(p)) return false;
        members.put(p, roles.containsKey(roleId) ? roleId : Roles.RECRUIT);
        if (playerName != null) names.put(p, playerName);
        invites.remove(p);
        return true;
    }

    /** Removes a member. The leader cannot be removed this way (disband or hand over first). */
    public boolean removeMember(UUID p) {
        if (p == null || p.equals(leader) || !members.containsKey(p)) return false;
        members.remove(p);
        return true;
    }

    public boolean setRole(UUID p, String roleId) {
        if (!members.containsKey(p) || !roles.containsKey(roleId)) return false;
        if (p.equals(leader)) return false;
        if (Roles.LEADER.equals(roleId)) return false;
        members.put(p, roleId);
        return true;
    }

    /** Hands the leadership to another member: the old leader becomes an officer. */
    public boolean transferLeadership(UUID to) {
        if (to == null || !members.containsKey(to) || to.equals(leader)) return false;
        UUID old = leader;
        leader = to;
        members.put(to, Roles.LEADER);
        members.put(old, Roles.OFFICER);
        return true;
    }

    public List<UUID> memberIds() { return List.copyOf(members.keySet()); }

    /** Number of members holding the given role. */
    public int countRole(String roleId) {
        int n = 0;
        for (String r : members.values()) if (r.equals(roleId)) n++;
        return n;
    }

    public NationLevels.Metrics metrics() {
        return new NationLevels.Metrics(members.size(), ledger.balance(), completedContracts, areaScore, warWins, xp);
    }

    public static String slug(String name) {
        if (name == null) return "";
        String s = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    public static boolean validName(String name, int minLen, int maxLen) {
        if (name == null) return false;
        String t = name.trim();
        if (t.length() < minLen || t.length() > maxLen) return false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_' || c == '\'')) return false;
        }
        return !slug(t).isEmpty();
    }
}
