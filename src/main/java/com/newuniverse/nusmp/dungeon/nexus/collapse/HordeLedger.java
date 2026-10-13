package com.newuniverse.nusmp.dungeon.nexus.collapse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The books of the frenzied horde of one collapse ({@link CollapseGuards} keeps them, with no world in them): which creatures are alive and
 * for which slot (an exit, or a player) each was made, which are on their way (shares of a wave not yet handed to the walls, wall births still
 * cracking, rings of light still burning), and how many have been made in all (the collapse's saved counter). Every ceiling of
 * {@link GuardPlan} is held here, at the moment a creature is counted: {@link #admit} refuses the one that would pass any of them, so the
 * horde can never grow past MAX_ALIVE, PER_EXIT_ALIVE at one exit or MAX_SPAWNED in all, whatever the walls hand over and whenever. Pure.
 */
final class HordeLedger {
    /** The slot of a creature found again after a restart: counted alive, but it belongs to no exit. */
    static final int FOUND = -2;

    /** A creature announced by a ring of light that will stand at (x, y, z) from tick `due` on (it waits a little if a player is in the way, `waits` times at most). */
    record Ring(int slot, int x, int y, int z, long due, int tier, int waits) {}

    /** One slot's share of a wave, waiting its turn: `k` births round the block (x, y, z). */
    record Job(int slot, int x, int y, int z, int k, int wave) {}

    /** The births handed to the walls for one slot of one wave: how many have still to come out, and the tick after which they are given up. */
    static final class Batch {
        final int slot;
        final long expires;
        int left;

        Batch(int slot, long expires) {
            this.slot = slot;
            this.expires = expires;
        }
    }

    private final CollapseState s;
    private final Map<UUID, Integer> alive = new LinkedHashMap<>();
    private final List<Ring> rings = new ArrayList<>();
    private final List<Batch> batches = new ArrayList<>();
    private final ArrayDeque<Job> jobs = new ArrayDeque<>();

    HordeLedger(CollapseState s) { this.s = s; }

    // ---------------------------------------------------------------- who is alive
    int alive() { return alive.size(); }

    boolean has(UUID id) { return alive.containsKey(id); }

    /** The creatures alive, in the order they were counted (a copy: a blow that kills can set other things off). */
    List<UUID> ids() { return new ArrayList<>(alive.keySet()); }

    /** How many alive were made for this slot. */
    int at(int slot) {
        int n = 0;
        for (int e : alive.values()) if (e == slot) n++;
        return n;
    }

    /** Is the horde at a ceiling that holds for every slot (MAX_ALIVE alive or MAX_SPAWNED made)? */
    boolean full() { return !room(FOUND); }

    /** May one more creature for this slot be counted: under MAX_ALIVE alive, MAX_SPAWNED made, and (for an exit) under PER_EXIT_ALIVE there? */
    boolean room(int slot) {
        return s.spawned < GuardPlan.MAX_SPAWNED && alive.size() < GuardPlan.MAX_ALIVE && (slot < 0 || at(slot) < GuardPlan.PER_EXIT_ALIVE);
    }

    /** Counts a creature that has come into the world for this slot; false (and nothing counted) if that would pass a ceiling. */
    boolean admit(UUID id, int slot) {
        if (alive.containsKey(id)) {                                                          // counted already (the join handler found it first): only its slot is known now
            alive.put(id, slot);
            return true;
        }
        if (!room(slot)) return false;
        alive.put(id, slot);
        s.spawned++;
        return true;
    }

    /** A creature of this collapse loaded from disk (or found again): it counts alive again, without being counted as made. A few more than MAX_ALIVE are taken, no more. */
    void adopt(UUID id) {
        if (alive.size() < GuardPlan.MAX_ALIVE + 4) alive.putIfAbsent(id, FOUND);
    }

    /** Keeps only the creatures for which `there` is true (the others died, were removed or are out of reach). */
    void retain(Predicate<UUID> there) { alive.keySet().removeIf(id -> !there.test(id)); }

    // ---------------------------------------------------------------- what is on its way
    void addRing(Ring r) { rings.add(r); }

    /** The rings waiting (a copy). */
    List<Ring> rings() { return new ArrayList<>(rings); }

    void removeRing(Ring r) { rings.remove(r); }

    void addBatch(Batch b) { batches.add(b); }

    /** Gives up the batches whose time is past: the births that never came out are forgotten. */
    void expire(long now) { batches.removeIf(b -> b.expires < now); }

    void queue(Job j) { jobs.add(j); }

    /** The next share of a wave to hand to the walls, or null. */
    Job nextJob() { return jobs.poll(); }

    boolean hasJobs() { return !jobs.isEmpty(); }

    /** Creatures on the way in all: shares not yet handed over, wall births still cracking, rings still burning. */
    int coming() {
        int n = rings.size();
        for (Job j : jobs) n += j.k();
        for (Batch b : batches) n += Math.max(0, b.left);
        return n;
    }

    /** Of those, the ones that are for a player and not an exit. */
    int comingPlayer() {
        int n = 0;
        for (Ring r : rings) if (r.slot() == GuardPlan.PLAYER) n++;
        for (Job j : jobs) if (j.slot() == GuardPlan.PLAYER) n += j.k();
        for (Batch b : batches) if (b.slot == GuardPlan.PLAYER) n += Math.max(0, b.left);
        return n;
    }

    /** For each of `exits` exits, the creatures alive for it plus those on their way to it (what {@link GuardPlan#assign} counts against PER_EXIT_ALIVE). */
    int[] load(int exits) {
        int[] load = new int[Math.max(exits, 0)];
        for (int e : alive.values()) if (e >= 0 && e < load.length) load[e]++;
        for (Ring r : rings) if (r.slot() >= 0 && r.slot() < load.length) load[r.slot()]++;
        for (Job j : jobs) if (j.slot() >= 0 && j.slot() < load.length) load[j.slot()] += j.k();
        for (Batch b : batches) if (b.slot >= 0 && b.slot < load.length) load[b.slot] += Math.max(0, b.left);
        return load;
    }

    /** Forgets everything on its way and everything alive (the collapse is over). */
    void clear() {
        alive.clear();
        rings.clear();
        batches.clear();
        jobs.clear();
    }

    /** Forgets what is on its way and keeps who is alive (nothing more is born; the creatures are removed by the caller). */
    void closeBirths() {
        rings.clear();
        batches.clear();
        jobs.clear();
    }
}
