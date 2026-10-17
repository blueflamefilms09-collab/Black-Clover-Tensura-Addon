package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;

import java.util.List;

/**
 * Why: the lead wires a builder's characters into {@code ArcCharacters.bootstrap()} with as few lines as possible, and the Hub, the tests and the docs all
 * want one answer to "who is in this cast, in what order". This class is that answer for the other Magic Knight squads and the court: sixteen friendly
 * characters (no bosses, so no entity class, no moves and no Charter Grimoire), listed in the order the Sanctuary meets them (the court first, then the
 * juniors, then the captains as their gates open). One line, {@code KnightsCast.registerAll();}, puts them all in the roster; registering twice is harmless.
 */
public final class KnightsCast {
    private KnightsCast() {}

    /** Everybody of this cast, the Sanctuary's order. */
    public static final List<ArcCharacter> ALL = List.of(
            CourtCast.JULIUS_NOVACHRONO, CourtCast.MARX_FRANCOIS,
            GoldenDawnCast.YUNO, GoldenDawnCast.MIMOSA_VERMILLION, GoldenDawnCast.KLAUS_LUNETTES,
            CrimsonLionCast.LEOPOLD_VERMILLION, CrimsonLionCast.FUEGOLEON_VERMILLION,
            CaptainsCast.CHARLOTTE_ROSELEI,
            SilverEagleCast.NOZEL_SILVA, SilverEagleCast.SOLID_SILVA,
            CaptainsCast.JACK_THE_RIPPER, CaptainsCast.DOROTHY_UNSWORTH,
            GoldenDawnCast.WILLIAM_VANGEANCE,
            CrimsonLionCast.MEREOLEONA_VERMILLION,
            SeabedTempleCast.KAHONO, SeabedTempleCast.KIATO);

    /** Registers every character of the cast in the roster. */
    public static void registerAll() {
        for (ArcCharacter c : ALL) ArcRoster.register(c);
    }
}
