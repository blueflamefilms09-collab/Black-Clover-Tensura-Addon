package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: three more squad captains who each answer a different question a delver has about the dungeon: Charlotte Roselei of the Blue Rose (traps and
 * binding), Jack the Ripper of the Green Mantis (cutting through barriers and rules) and Dorothy Unsworth of the Coral Peacock (rest and dreams). The
 * owner's brief put Jack in the Purple Orca and Dorothy in the Green Mantis; the series (and the mod's own {@code grimoire/CanonBook}) has Jack leading the
 * Green Mantis and Dorothy the Coral Peacock, so the titles follow the series. They are in one class because a captain with no squad around them is a
 * single-record file; their gates differ (see {@link KnightsGates}).
 */
public final class CaptainsCast {
    private CaptainsCast() {}

    /** Charlotte Roselei: captain of the Blue Rose; Briar Magic; cold on the surface, fond of roses and of one Black Bull. */
    public static final ArcCharacter CHARLOTTE_ROSELEI = ArcCharacter.npc("charlotte_roselei", "Charlotte Roselei").title("Captain of the Blue Rose")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x4A90C8, 0xC8D8E8).magic("briar").stats(0, 0, 0.25, 0)
            .after(KnightsGates.REVCHI).build();

    /** Jack the Ripper: captain of the Green Mantis; Slash Magic; a grinning commoner who cuts through anything. */
    public static final ArcCharacter JACK_THE_RIPPER = ArcCharacter.npc("jack_the_ripper", "Jack the Ripper").title("Captain of the Green Mantis")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0x3CB074, 0xC8E0C8).magic("slash").stats(0, 0, 0.25, 0)
            .after(KnightsGates.HEATH).build();

    /** Dorothy Unsworth: captain of the Coral Peacock; Dream Magic; almost always asleep, never unaware. */
    public static final ArcCharacter DOROTHY_UNSWORTH = ArcCharacter.npc("dorothy_unsworth", "Dorothy Unsworth").title("Captain of the Coral Peacock")
            .source("Black Clover").saga(ArcSaga.ALLIES).colors(0xE88AA0, 0xF0D8E0).magic("dream").stats(0, 0, 0.25, 0)
            .after(KnightsGates.LOTUS).build();
}
