package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: somebody in the Sanctuary has to tell the story as a whole, and in the series that is the court: Julius Novachrono, the Wizard King, who sees the
 * big picture and whose will already surfaces in the mod's Lucius fight (his Time grimoire, {@code book/TimeBook}, and the "Two Souls" windows of
 * {@code LuciusBoss}), and Marx Francois, his aide, who keeps the records and so explains the rules of this mod's dungeon (parties, the System, the Guardian
 * floors, the Charter Grimoires, the Hourglass). Neither waits for a gate: they are the first two faces a new delver meets and the ones whose hints cover
 * every saga.
 */
public final class CourtCast {
    private CourtCast() {}

    /** Julius Novachrono: the Wizard King; Time Magic; playful, humble, curious about everything, quietly sure of the delvers. */
    public static final ArcCharacter JULIUS_NOVACHRONO = ArcCharacter.npc("julius_novachrono", "Julius Novachrono").title("the Wizard King")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE8DCC0, 0xD8B050).magic("time").stats(0, 0, 0.25, 0).build();

    /** Marx Francois: aide to the Wizard King; Memory Magic; formal, exact, long-suffering about his employer's schedule. */
    public static final ArcCharacter MARX_FRANCOIS = ArcCharacter.npc("marx_francois", "Marx Francois").title("Aide to the Wizard King")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x8AA0D0, 0xD8E0F0).magic("memory").stats(0, 0, 0.25, 0).build();
}
