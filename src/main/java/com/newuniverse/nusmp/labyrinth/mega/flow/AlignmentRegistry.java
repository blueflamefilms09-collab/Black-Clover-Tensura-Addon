package com.newuniverse.nusmp.labyrinth.mega.flow;

/**
 * Why: the classification table is loaded by a data reload and read by every cast, from the hook in {@code QuestHooks} to the self-tests, so there has to
 * be exactly one place that holds the table the last reload produced and hands it out. It is a single volatile reference swapped in one assignment, so
 * a reload in progress never exposes a half-built table to a cast that happens on the same tick, and before the first reload (or when the file is
 * missing) it holds {@link MagicAlignments#EMPTY}, which classifies everything as NEUTRAL: the flow then boosts nothing and rejects nothing, which is
 * the safe way to be wrong. No other state lives here.
 */
public final class AlignmentRegistry {
    private AlignmentRegistry() {}

    private static volatile MagicAlignments current = MagicAlignments.EMPTY;

    /** The table the last data reload installed ({@link MagicAlignments#EMPTY} before the first one). */
    public static MagicAlignments current() { return current; }

    /** Replaces the table (called by the reload listener and by tests); null installs the empty table. */
    static void install(MagicAlignments table) { current = table == null ? MagicAlignments.EMPTY : table; }

    /** The alignment of a cast named by these ids, most specific first (see {@link MagicAlignments#classify}). */
    public static Alignment classify(String... ids) { return current.classify(ids); }
}
