package com.newuniverse.nusmp.elite.nations.core;

import java.util.Set;

/**
 * Why: one role of a nation: an id (stable), a display name, a rank (higher rank may act on lower rank: kick, promote) and a permission mask. Immutable; the "with" methods return a changed copy.
 */
public record Role(String id, String name, int rank, int permMask) {
    public boolean has(Permission p) { return (permMask & p.bit()) != 0; }

    public Role withPerm(Permission p, boolean on) { return new Role(id, name, rank, on ? permMask | p.bit() : permMask & ~p.bit()); }

    public Role withName(String newName) { return new Role(id, newName, rank, permMask); }

    public Role withRank(int newRank) { return new Role(id, name, newRank, permMask); }

    public Set<Permission> permissions() { return Permission.fromMask(permMask); }
}
