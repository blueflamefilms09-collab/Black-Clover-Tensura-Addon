package com.newuniverse.nusmp.labyrinth.mega.lock;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Why: the owner asked to find EVERY way the dungeon can lock a player in a room and, for each, to either remove the random locking or guarantee an exit. This is that
 * inventory as data: one entry per way found (by reading the whole of {@code labyrinth/**}, the templates and the thousand mechanics), with what it does and which
 * guarantees now cover it. A guarantee is one of: the lock can never close on a person (NEVER_LOCKS), it opens by itself after a time (TIMED_RELEASE), a visible rune opens it
 * (RUNE), the command {@code /dungeon unstuck} takes the player out of it (UNSTUCK), the generator proves it cannot be a closed pocket (GENERATOR_CHECK), or it already had an
 * exit the player can see (OWN_EXIT: a ladder, an exit rift, an open doorway). The self-test {@code lock.escape} and {@code LockCaseTest} demand that every case that CAN
 * hold a person has at least one of the timed or rune guarantees plus the command, so a new lock added to this list without a way out fails the build. Pure.
 */
public enum LockCase {
    TRIAL_ARCHWAY("trial.archway", true, "A trial chamber (a Sealed Archway) bars every doorway with Magic Barrier until its wave is dead.", Guarantee.TIMED_RELEASE, Guarantee.RUNE, Guarantee.UNSTUCK),
    MECHANIC_SEAL_DOORS("mechanic.seal_doors", true, "A room mechanic (214 of the 1000 use seal_doors or cage) bars the doorways until its puzzle or fight is beaten; some have no counter at all.", Guarantee.TIMED_RELEASE, Guarantee.RUNE, Guarantee.UNSTUCK),
    MECHANIC_CAGE("mechanic.cage", true, "A mechanic cages the party in a ring of bars (the Sure-Hit Domain and its variants).", Guarantee.TIMED_RELEASE, Guarantee.RUNE, Guarantee.UNSTUCK),
    MECHANIC_BOSS_CAGE("mechanic.boss_cage", true, "A boss-arena mechanic cages the arena while the boss lives.", Guarantee.TIMED_RELEASE, Guarantee.RUNE, Guarantee.UNSTUCK),
    MECHANIC_BLOCKS("mechanic.blocks", true, "Mechanics that place walls, ceilings or obstacles (place, wall_blink, ceiling) and may leave them in a doorway.", Guarantee.TIMED_RELEASE, Guarantee.UNSTUCK),
    UNSOLVABLE_COUNTER("mechanic.unsolvable", true, "A seal whose counter cannot be met by the party present (no counter, or a plate for more players than are inside).", Guarantee.TIMED_RELEASE, Guarantee.RUNE, Guarantee.UNSTUCK),
    GUARDIAN_ARENA("guardian.arena", false, "The Guardian's hall has no door that closes behind a player; the Gatekeeper slab only gates progress to the antechamber.", Guarantee.NEVER_LOCKS, Guarantee.OWN_EXIT, Guarantee.UNSTUCK),
    GATEKEEPER_SLAB("gatekeeper.sector", false, "The sector Gatekeeper is a gate between two rooms that are both open to the player's side; nobody is inside it.", Guarantee.NEVER_LOCKS, Guarantee.UNSTUCK),
    PUZZLE_DOOR("puzzle.door", false, "A sigil hall's puzzle door closes a treasure alcove only, never the hall; the hall has two open doors.", Guarantee.NEVER_LOCKS, Guarantee.UNSTUCK),
    PIT_CHAMBER("trap.pit", true, "A pitfall tile drops a player into a chamber under the floor; the floor closes again.", Guarantee.OWN_EXIT, Guarantee.UNSTUCK),
    RIFT_POCKET("rift.pocket", true, "A pocket realm is a sealed set of rooms entered through a rift.", Guarantee.OWN_EXIT, Guarantee.TIMED_RELEASE, Guarantee.UNSTUCK),
    CATASTROPHE("crucible.catastrophe", false, "The Crucible's collapse, flood and spirit change no block, so nothing can close.", Guarantee.NEVER_LOCKS, Guarantee.UNSTUCK),
    GLOOM("economy.gloom", false, "Darkness and gloom hurt but never block a doorway.", Guarantee.NEVER_LOCKS, Guarantee.UNSTUCK),
    DIRECTIONAL_LOCK("floors.directional_lock", false, "The Directional Lock closes a ROAD, not a room, and always leaves the way back to Floor 0.", Guarantee.NEVER_LOCKS, Guarantee.OWN_EXIT),
    GENERATOR_POCKET("generator.pocket", true, "The generator (old or cyclic WFC) leaving a room with no walkable path to the stairs.", Guarantee.GENERATOR_CHECK, Guarantee.UNSTUCK),
    UNREACHABLE_SPAWN("spawn.unreachable", true, "A player appears, respawns or is teleported into a room that has no way out.", Guarantee.GENERATOR_CHECK, Guarantee.UNSTUCK),
    UNBREAKABLE_WALLS("guard.unbreakable", false, "The dungeon guard makes every wall unbreakable, so a player cannot dig out of a closed place; the way out does not need a wall to break.", Guarantee.UNSTUCK);

    /** The ways out. */
    public enum Guarantee { NEVER_LOCKS, TIMED_RELEASE, RUNE, UNSTUCK, GENERATOR_CHECK, OWN_EXIT }

    private final String id;
    private final boolean canHold;
    private final String what;
    private final Set<Guarantee> guarantees;

    LockCase(String id, boolean canHold, String what, Guarantee first, Guarantee... rest) {
        this.id = id;
        this.canHold = canHold;
        this.what = what;
        this.guarantees = Collections.unmodifiableSet(EnumSet.of(first, rest));
    }

    public String id() { return id; }

    /** Can this hold a person at all (false = it was checked and cannot)? */
    public boolean canHold() { return canHold; }

    public String what() { return what; }

    public Set<Guarantee> guarantees() { return guarantees; }

    /** The rule the self-test enforces: a case that can hold someone has an automatic way out (a timer, a rune, an own exit or a generator proof) and the command. */
    public boolean covered() {
        if (!guarantees.contains(Guarantee.UNSTUCK) && canHold) return false;
        if (!canHold) return !guarantees.isEmpty();
        return guarantees.contains(Guarantee.TIMED_RELEASE) || guarantees.contains(Guarantee.RUNE) || guarantees.contains(Guarantee.OWN_EXIT) || guarantees.contains(Guarantee.GENERATOR_CHECK);
    }
}
