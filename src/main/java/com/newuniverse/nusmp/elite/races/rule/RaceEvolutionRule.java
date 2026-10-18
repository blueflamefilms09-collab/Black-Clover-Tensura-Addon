package com.newuniverse.nusmp.elite.races.rule;

/**
 * Why: the owner wants each requirement of an evolution as a small, separate rule (EP gate, kills, mastery, item, record), so a race file can list them, the screen can show them one
 * by one with their own progress, and a test can drive each one alone. A rule is pure: it answers a progress in 0..1 from a {@link RaceCtx}, carries a weight (its share of the bar,
 * 0..100) and says in words what it asks. {@link #commit} is the one place a rule may take something (an item) and is only called after EVERY rule of the set is met.
 */
public interface RaceEvolutionRule {
    /** The key used in the race file ({@code ep}, {@code kills}, {@code mastery}, {@code item}, {@code record}, {@code any_of}). */
    String type();

    /** This rule's share of the progress bar; the weights of a set are normalised to 100. */
    double weight();

    /** 0..1, 1 = satisfied. */
    double progress(RaceCtx ctx);

    /** One English line: what the rule asks (the screen and {@code /multiverse elite race info} print it). */
    String describe();

    default boolean met(RaceCtx ctx) { return progress(ctx) >= 1.0 - 1e-9; }

    /** Called once, after every rule of the set is met, when the evolution really happens: take the item, spend the resource. */
    default void commit(RaceCtx ctx) {}
}
