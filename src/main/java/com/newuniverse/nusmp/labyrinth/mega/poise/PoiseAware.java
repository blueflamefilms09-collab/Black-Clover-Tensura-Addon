package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: a mob that has its own brain (the Mob Foundry's one entity type) must be told when its poise breaks and when it turns desperate, so it can cancel the cast it was winding up and
 * rebuild its ability list with the ultimate; every other mob is handled from outside (an effect, a goal). The poise layer calls these two methods and nothing else on the mob, so it does
 * not depend on the foundry's internals.
 */
public interface PoiseAware {
    /** The poise bar broke: the bearer is helpless for {@code ticks}; whatever it was winding up is lost. */
    void poiseStaggered(int ticks);

    /** The desperation phase started or ended (also called once when a desperate bearer comes back from a saved chunk). */
    void poiseDesperate(boolean on);
}
