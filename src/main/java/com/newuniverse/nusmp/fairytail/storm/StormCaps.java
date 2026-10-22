package com.newuniverse.nusmp.fairytail.storm;

import java.util.List;

/**
 * Why: a storm lasts several seconds and costs the server work every tick, so the number of storms alive is capped twice: per player (casting a third storm ends your oldest one)
 * and for the whole server (a full sky refuses a new storm instead of piling on). The decision is a pure function of the live list, so it is unit tested and cannot drift from what
 * the manager does. Players are keyed by UUID (a string here, so the class needs no game type); the live list is oldest first.
 */
public final class StormCaps {
    private StormCaps() {}

    /** One live storm: who owns it. */
    public record Live(String owner, long startTick) {}

    /** What to do with a new storm of {@code owner}. */
    public record Verdict(boolean admit, int evictIndex) {
        public static Verdict refuse() { return new Verdict(false, -1); }

        public static Verdict admit(int evict) { return new Verdict(true, evict); }

        public boolean evicts() { return admit && evictIndex >= 0; }
    }

    public static final int MIN_PER_PLAYER = 1, MAX_PER_PLAYER = 2, MIN_GLOBAL = 1, MAX_GLOBAL = 64;

    /**
     * @param live      the storms alive now, in the order they were started (oldest first)
     * @param owner     the caster
     * @param perPlayer most storms one player may have alive (clamped to 1..2)
     * @param global    most storms the server may have alive (clamped to 1..64)
     * @return admit (and which live one to end first, or -1) or refuse (the sky is full of other people's storms)
     */
    public static Verdict decide(List<Live> live, String owner, int perPlayer, int global) {
        int per = Math.max(MIN_PER_PLAYER, Math.min(MAX_PER_PLAYER, perPlayer));
        int all = Math.max(MIN_GLOBAL, Math.min(MAX_GLOBAL, global));
        int mine = 0, oldestMine = -1;
        for (int i = 0; i < live.size(); i++) {
            if (live.get(i).owner().equals(owner)) {
                mine++;
                if (oldestMine < 0) oldestMine = i;
            }
        }
        if (mine >= per) {
            // the caster's own oldest storm is ended to make room: the total does not grow
            return Verdict.admit(oldestMine);
        }
        if (live.size() >= all) {
            // the server is full: the caster has room of their own but not in the sky; a caster with a storm of their own may swap it, otherwise the cast is refused
            return oldestMine >= 0 ? Verdict.admit(oldestMine) : Verdict.refuse();
        }
        return Verdict.admit(-1);
    }
}
