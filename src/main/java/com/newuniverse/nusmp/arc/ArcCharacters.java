package com.newuniverse.nusmp.arc;

/**
 * Why: the single list of characters of the arcs, in story order. Every character is a file of its own (a boss class with a {@code CHARACTER} constant, or an
 * NPC definition) and this class is the one place that names them all, so the roster, the registry and the saved ids agree on one order. The lead adds one line
 * per character here when a builder's work is merged; a character that is not listed here does not exist in the game.
 */
public final class ArcCharacters {
    private ArcCharacters() {}

    private static boolean done;

    /** Registers every character and loads every boss class (so its entity type is registered before the registry events). Safe to call twice. */
    public static synchronized void bootstrap() {
        if (done) return;
        done = true;
        // Saga 1 ... (lines are appended here as the builders are merged, e.g. ArcRoster.register(HeathGriceBoss.CHARACTER);)
        com.newuniverse.nusmp.arc.world.ExistingBosses.registerAll();   // 1.61.0: the seven older bosses (Goliath ... Lucius) as roster characters, first in the story order (ARC-WORLD)
        // Saga 1, the five openers in the order of the anime (S1A); each CHARACTER constant loads its boss class, which registers its entity type
        ArcRoster.register(com.newuniverse.nusmp.arc.early.RevchiSalikBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.early.HeathGriceBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.early.LotusWhomaltBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.early.MarsBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.early.VettoBoss.CHARACTER);
        // Saga 1, the Eye of the Midnight Sun (S1B): Rades first, then Fana, Rhya (the Third Eye trio with Vetto) and Patolli
        ArcRoster.register(com.newuniverse.nusmp.arc.midnight.RadesBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.midnight.FanaBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.midnight.RhyaBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.midnight.PatolliBoss.CHARACTER);
        // Zagred, remade (ZAG): the same entity id as before, after Patolli and Rades Spirito whom the story names as his prerequisites
        ArcRoster.register(com.newuniverse.nusmp.arc.zagred.ZagredBoss.CHARACTER);
        // Saga 2, the Spade Kingdom Raid: Morris first, Lucifero names him as a prerequisite (S2)
        ArcRoster.register(com.newuniverse.nusmp.arc.spade.MorrisBoss.CHARACTER);
        ArcRoster.register(com.newuniverse.nusmp.arc.spade.LuciferoBoss.CHARACTER);
        com.newuniverse.nusmp.arc.npc.BlackBullsNpcs.registerAll();   // 1.61.0: fourteen Black Bulls and friends of the saga Allies (NPC-A); the NPC lines stay together because the roster order decides the Hub posts
        com.newuniverse.nusmp.arc.knights.KnightsCast.registerAll();   // 1.61.0: sixteen friendly Magic Knights, captains and court members of the Hub (NPC-B)
    }
}
