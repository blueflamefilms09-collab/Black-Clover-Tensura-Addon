package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the forward stairwell of a floor belongs at its far end, where the party has had to explore the whole floor to find it, and "the far end" has to be a
 * decision the tests can check, not a feeling. The decision is a ranking: every free doorway of every room is a candidate host, its rank is the walking distance of
 * its room from the arrival stairwell ({@link WalkGraph}), the farthest comes first, and ties are broken by a number the assembler draws from the seeded generator
 * (so the same seed gives the same floor and two equally far doorways are not always resolved the same way). The assembler then tries the candidates in this
 * order until the stairwell fits on one: the winner is, by construction, the farthest doorway where a stairwell can stand. Pure so the order is proved on plain
 * numbers, including that it never depends on the order the candidates are listed in.
 */
public final class StairSearch {
    private StairSearch() {}

    /**
     * One free doorway.
     *
     * @param piece  the index of the room that owns the doorway
     * @param socket the index of the doorway in that room
     * @param dist   the walking distance of the room from the arrival stairwell, in doorways
     * @param tie    a seeded number that orders equally far doorways
     */
    public record Candidate(int piece, int socket, int dist, int tie) {}

    /** The candidates, farthest first; equal distances by {@code tie}, then by piece and socket so the order is total and never depends on how the input was listed. */
    public static List<Candidate> rank(List<Candidate> candidates) {
        List<Candidate> out = new ArrayList<>(candidates == null ? List.of() : candidates);
        out.sort(Comparator.comparingInt(Candidate::dist).reversed().thenComparingInt(Candidate::tie).thenComparingInt(Candidate::piece).thenComparingInt(Candidate::socket));
        return out;
    }

    /**
     * The first candidate in rank order that {@code fits} accepts, or null. This is the search the assembler performs with real placements; it is written here with
     * a predicate so a test can compare it with a brute-force maximum over the candidates that fit.
     */
    public static Candidate pick(List<Candidate> candidates, java.util.function.Predicate<Candidate> fits) {
        for (Candidate c : rank(candidates)) if (fits.test(c)) return c;
        return null;
    }
}
