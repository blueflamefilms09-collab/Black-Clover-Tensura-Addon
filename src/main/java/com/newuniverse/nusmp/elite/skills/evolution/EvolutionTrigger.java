package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: a skill's evolution can require any combination of twelve kinds of thing (a mastered ability, EP, kills, a title, forged items, ...). Each kind is one small class behind this one
 * interface, so a requirement list in the data file is just a list of {id, params} pairs and the engine never knows what any of them mean. A trigger is pure: it asks the
 * {@link EvolutionContext} and answers; the only trigger with a side effect is the one that consumes items, and that side effect is a separate step ({@link #consume}) that the engine
 * runs only after EVERY trigger of the path said yes, so a failed requirement never costs the player anything.
 */
public interface EvolutionTrigger {
    /** The id used in the data file, snake_case. */
    String id();

    /** Does the player meet it now? {@code skillId} is the skill that would evolve (the default subject of the trigger). */
    boolean test(EvolutionContext ctx, Params params, String skillId);

    /** The side effect of satisfying it (default none). Called once, after all triggers passed. Return false if it could not be paid after all. */
    default boolean consume(EvolutionContext ctx, Params params, String skillId) { return true; }

    /** A short English line for "what is still missing", shown to the player now and then. */
    String describe(Params params, String skillId);
}
