package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.blackclover.Devil;

/**
 * Why: the two bosses of the Spade Kingdom Raid (Morris Libardirt and Lucifero, the King of Devils) are defined as plain records here, in a class that
 * knows no game type, and the boss classes only point at them. That keeps the story data (tier, health band, floors, themes, the chain that unlocks
 * them, the Charter Grimoire, the lines) testable on its own (every skill id is checked against the Tensura id reference, the chain is checked against
 * the Triad), and it means the lead's roster line is the same {@code ArcRoster.register(MorrisBoss.CHARACTER)} as for every other character.
 * <p>
 * Story order: the Dark Triad (Dante, Vanica, Zenon) falls first, then Morris, who is the vessel the King uses to open the second gate, then Lucifero
 * himself. Health stays under Lucius's 3600 (he is the top of the ladder) and inside the tier-4 band. Floors: Morris on the boss floors 70 to 90 of
 * either road, Lucifero on 90 and 100; the themes he favours are the dark, infernal ones, so on the Abyssal road's last floor he is the likely guardian.
 */
public final class SpadeRoster {
    private SpadeRoster() {}

    /** The colour of Modification Magic in the mod's own books (0xFF9B58), so Morris reads as what he is; the lighter accent for rims and sparks. */
    public static final int MORRIS_COLOR = 0xFF9B58, MORRIS_ACCENT = 0xFFD9B0;
    /** The gravity violet and accent of Lucifero (the same as the Zogratis devil style, so the family of gravity reads as one). */
    public static final int LUCIFERO_COLOR = 0x5B2A8C, LUCIFERO_ACCENT = 0xB58CFF;

    /**
     * Morris Libardirt, the Magic Scholar of the Diamond Kingdom who joined the Dark Triad: Modification Magic (he rewrites the structure of things,
     * and with a devil's power of ideas), the host of Lucifero's contract (the gravity is borrowed). The Charter Grimoire lends skills that rewrite,
     * analyse and cancel, plus the borrowed gravity.
     */
    public static final ArcCharacter MORRIS = ArcCharacter.boss("morris_libardirt", "Morris Libardirt")
            .title("the Magic Scholar").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(4)
            .colors(MORRIS_COLOR, MORRIS_ACCENT)
            .devil(Devil.LUCIFERO).magic("modification")
            .stats(3000, 14, 0.30, 14)
            .floors(null, 70, 90)
            .themes("cursed_archive", "infernal_foundry", "shadow", "gravity", "astral")
            .after("dante", "vanica", "zenon")
            .charter(0.30, "molecular_manipulation", "analytical_appraisal", "mana_manipulation", "chant_annulment", "thought_acceleration",
                    "gravity_field", "gravity_manipulation", "earth_manipulation")
            .lines("Specimens that walk in on their own. How considerate. Hold still: this will only take the rest of your life.",
                    "Lucifero? That is far more of the contract than we agreed... no. No, let it come. I want to see what is inside it!",
                    "The gate... it opens regardless. How magnificent. My notes... someone, save my notes...")
            .build();

    /**
     * Lucifero, the King of Devils: Gravity Magic, the devil of the Dark Triad's eldest, the one the Magic Knight captains could not stand up against.
     * The Charter Grimoire lends the gravity set of a king (domination, field, flight, the nullification of gravity), the presence that bows a room,
     * and the cutting of spells.
     */
    public static final ArcCharacter LUCIFERO = ArcCharacter.boss("lucifero", "Lucifero")
            .title("King of Devils").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(4)
            .colors(LUCIFERO_COLOR, LUCIFERO_ACCENT)
            .devil(Devil.LUCIFERO).magic("gravity")
            .stats(3500, 20, 0.34, 20)
            .floors(null, 90, 100)
            .themes("infernal_foundry", "abyss", "gravity", "flame", "shadow", "necropolis")
            .after("dante", "vanica", "zenon", "morris_libardirt")
            .charter(0.35, "gravity_domination", "gravity_field", "gravity_flight", "gravity_attack_nullification", "majesty", "demon_lord_haki",
                    "magic_jamming", "spatial_motion", "mortal_fear")
            .lines("Kneel. Is it so hard to understand? You stand in the presence of the King of Devils.",
                    "Enough. I have let you stand long enough. Feel what the weight of a king is, when he stops holding it back.",
                    "A king... pressed into the dust by insects. Remember it: you have only broken the part of me that was lent to this world.")
            .build();
}
