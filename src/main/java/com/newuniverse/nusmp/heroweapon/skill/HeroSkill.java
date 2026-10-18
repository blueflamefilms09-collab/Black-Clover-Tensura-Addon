package com.newuniverse.nusmp.heroweapon.skill;

/**
 * Why: every technique of every hero weapon (about 150 of them, written by six different builders) must look the same to the code that fires it: the input
 * handler finds the skill the active form lists for the input, checks the cooldown and the cost, asks the skill to cast, and starts the cooldown. A skill is
 * therefore a small object with an id, text and numbers, and one server-side {@link #cast}. It never starts its own cooldown and never pays its own cost
 * (the dispatcher does, so the rules cannot be skipped by forgetting a call); it returns false to refuse (nothing paid, no cooldown). The ids are global
 * strings of the form {@code <kind>.<name>} (for example {@code shield.air_strike}) so the forms JSON of any weapon can name them and a test can check them.
 */
public interface HeroSkill {
    /** Global id {@code <kind>.<name>}, lower case, e.g. {@code shield.air_strike}. */
    String id();

    /** The display name. */
    String name();

    /** One or two sentences for the tooltip and the Weapon Book: what it does, its range, its counter or cost. */
    String desc();

    /** Cooldown in ticks before the level scaling (the dispatcher shortens it a little with the bond level). At least 40 for an ordinary skill, at least 600 for an ultimate. */
    int cooldownTicks(HeroCtx c);

    /** Cost as a share of the wielder's maximum magicules (0.02 is two percent); 0 for none. The dispatcher pays it through {@link HeroCtx#pay}. */
    double magoiShare();

    /** Ticks of wind-up shown before the effect (0 for instant). The dispatcher waits that long; {@link #cast} runs at the end. */
    default int windUpTicks() { return 0; }

    /** True for the strongest technique of a form (the Weapon Book marks it; a form has at most one). */
    default boolean ultimate() { return false; }

    /** Server side. Do the effect; return true when it was used, false to refuse (for example no target: say why with {@link HeroCtx#say}). */
    boolean cast(HeroCtx c);
}
