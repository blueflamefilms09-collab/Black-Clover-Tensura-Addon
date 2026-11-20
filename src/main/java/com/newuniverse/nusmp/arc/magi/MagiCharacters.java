package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: the Magi roster, one record per character (same pattern as {@code SdsCharacters}). Sinbad stands in the saga Allies (like the Goku boss: no new saga is added, so the exhaustive
 * saga switches of the arena shapes and rewards stay untouched), is NOT a floor boss (summoned by the command, the spawn egg or the arc summon only), and has no Charter Grimoire (that
 * needs generated grimoire art; the skill ids that were planned for it are listed in docs/sinbad_boss.md).
 */
public final class MagiCharacters {
    private MagiCharacters() {}

    /** Sinbad, King of the Seven Seas: tier 3 (the Seven Deadly Sins' tier), stats sized against Escanor (3000 / 24 / 0.30 / 16). */
    public static final ArcCharacter SINBAD = ArcCharacter.boss("sinbad_boss", "Sinbad")
            .title("King of the Seven Seas").source("Magi").saga(ArcSaga.ALLIES).tier(3)
            .colors(0x5B3A8C, 0xFFD54F).magic("lightning")
            .stats(3200, 24, 0.32, 15)
            .lines("I am Sinbad, King of Sindria and master of seven djinn. Let's see what kind of adventurer you are.",
                    "Then I won't hold back. Seven Seas - lend me all of your power!",
                    "Ha... You'd have made a fine member of my Household.")
            .build();
}
