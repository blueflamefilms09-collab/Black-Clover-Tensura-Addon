package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: the Golden Dawn is the squad the owner's story follows most closely, and four of its knights are friends the delvers meet in the Sanctuary: Yuno,
 * Mimosa and Klaus, the young knights who stand in the plaza from the first day, and William Vangeance, their captain, who comes once the third champion
 * of the first saga has fallen (the squad's captain has a long shadow of his own with the Eye of the Midnight Sun, see {@code docs/arc/npcb.md}). The four
 * records live together because they share a squad, a palette (the mod's own canon grimoires of the same people, {@code grimoire/CanonBook}, supply the
 * colours; a cover that is too dark to read as a name in chat is lightened) and a gate rule.
 *
 * <p>Each record is pure data ({@link ArcCharacter#npc}); the words they speak are {@code data/nusmp/arc/dialogue/<id>.json}.
 */
public final class GoldenDawnCast {
    private GoldenDawnCast() {}

    /** Yuno: Wind Magic and the wind spirit Sylph; the calm, competitive rival of the Black Bulls' Asta. */
    public static final ArcCharacter YUNO = ArcCharacter.npc("yuno", "Yuno").title("the Wind Prodigy").source("Black Clover")
            .saga(ArcSaga.ALLIES).colors(0x7FD6A0, 0xE8D890).magic("wind").stats(0, 0, 0.25, 0).build();

    /** William Vangeance: captain of the Golden Dawn; World Tree Magic; courteous, composed, carrying an old debt. */
    public static final ArcCharacter WILLIAM_VANGEANCE = ArcCharacter.npc("william_vangeance", "William Vangeance").title("Captain of the Golden Dawn")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE0D0A0, 0xC8A050).magic("world_tree").stats(0, 0, 0.25, 0)
            .after(KnightsGates.LOTUS).build();

    /** Mimosa Vermillion: Plant Magic and healing; gentle, earnest, the Vermillion who chose another squad on her own. */
    public static final ArcCharacter MIMOSA_VERMILLION = ArcCharacter.npc("mimosa_vermillion", "Mimosa Vermillion").title("the Golden Dawn's Healer")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE0D468, 0x8CC86A).magic("plant").stats(0, 0, 0.25, 0).build();

    /** Klaus Lunettes: Steel Magic; a proud noble and careful strategist who has learned that rank is a debt. */
    public static final ArcCharacter KLAUS_LUNETTES = ArcCharacter.npc("klaus_lunettes", "Klaus Lunettes").title("the Golden Dawn's Strategist")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x9AB0C0, 0xD8E0E8).magic("steel").stats(0, 0, 0.25, 0).build();
}
