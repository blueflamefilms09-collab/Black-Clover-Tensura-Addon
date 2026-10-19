package com.newuniverse.nusmp.labyrinth.mega.puzzle;

/**
 * Why: the three things that make one copy of a puzzle different from another and that the planner knows before anyone arrives: the seed (the same seed always gives the same board and the
 * same solution, so a reset puts back the board the party already studied and a test can replay it), the number of delvers (1 to 8: boards grow and timers tighten with the party, but
 * every board can still be beaten by one person) and the tier of the floor (1 to 5, 200 floors each: longer sequences, bigger boards). Pure.
 */
public record Params(long seed, int party, int tier) {
    public Params {
        party = Math.max(1, Math.min(8, party));
        tier = Math.max(1, Math.min(5, tier));
    }

    /** The tier counted from 0 (0 to 4): the number most size formulas use. */
    public int lvl() { return tier - 1; }

    public Params withParty(int p) { return new Params(seed, p, tier); }
}
