package com.newuniverse.nusmp.arc.danmachi;

import java.util.List;

/**
 * Why: Udaeus's moves are numbers first (range windows, telegraphs, shapes, damage) and the table must be checkable without a game: every move has a
 * telegraph of at least a second at the Gates, an attack exists at every distance, the ultimate is rare and leaves it exposed. This pure table is that;
 * {@code UdaeusBoss} turns each row into a framework {@code Move}.
 *
 * <p>The shape of the fight (the skeletal giant of the 37th floor's White Palace, see {@code docs/arc/udaeus.md}): a great black sword up close (the slash,
 * the cleave, the whirl), bone at range (the rib volley, the pillars), a roar that raises Spartoi, a leap, a nova, a cry that shuts the rib cage tight, and the
 * Ossuary Rise that fills the throne room with bone. The leveled form adds the Black Sword Wave. New rows are only appended.
 */
public final class UdaeusTable {
    private UdaeusTable() {}

    public static final String BLACK_SWORD_SLASH = "black_sword_slash", OVERHEAD_CLEAVE = "overhead_cleave", BONE_WHIRL = "bone_whirl", RIB_VOLLEY = "rib_volley",
            OSSUARY_PILLARS = "ossuary_pillars", SPARTOI_CALL = "spartoi_call", GRAVE_LEAP = "grave_leap", BONE_NOVA = "bone_nova", RIB_CAGE_CRY = "rib_cage_cry",
            OSSUARY_RISE = "ossuary_rise", BLACK_SWORD_WAVE = "black_sword_wave";

    /** Base damages of the custom moves. */
    public static final float CLEAVE_HIT = 22f, CLEAVE_SHOCK = 12f, CRY_PULSE = 4f, RISE_PILLAR = 14f;

    private static final List<DmSpec> SPECS = List.of(
            DmSpec.move(BLACK_SWORD_SLASH, "Black Sword Slash").power(20).wind(20).cd(90).range(0, 7.5).weight(6).recovery(10).cone(6.8, 70, 18, 1.5, true),
            DmSpec.move(OVERHEAD_CLEAVE, "Overhead Cleave").power(20).wind(26).cd(150).range(0, 9).weight(4).recovery(14).custom(9, CLEAVE_HIT),
            DmSpec.move(BONE_WHIRL, "Bone Whirl").power(20).wind(22).cd(140).range(0, 7).weight(3).crowd().recovery(12).spin(6.2, 13, 1.4),
            DmSpec.move(RIB_VOLLEY, "Rib Volley").power(20).wind(24).cd(150).range(6, 32).weight(4).recovery(10).volley(3, 36, 6.5, 32, 1.3),
            DmSpec.move(OSSUARY_PILLARS, "Ossuary Pillars").power(20).wind(26).cd(210).range(4, 34).weight(4).crowd().recovery(10).marks(3, 2.3, 11, 1.0, 0),
            DmSpec.move(SPARTOI_CALL, "Call of the Spartoi").power(20).wind(30).cd(720).range(0, 48).weight(2).recovery(10).custom(0, 0),
            DmSpec.move(GRAVE_LEAP, "Grave Leap").power(40).wind(26).cd(230).range(8, 36).weight(3).recovery(12).leap(7, 16, 1.2, 9),
            DmSpec.move(BONE_NOVA, "Bone Nova").power(40).wind(24).cd(220).range(0, 12).weight(3).crowd().recovery(12).wave(11, 12, 1.1, 1, 0),
            DmSpec.move(RIB_CAGE_CRY, "Rib Cage Cry").power(40).wind(30).cd(520).range(0, 40).weight(2).crowd().recovery(12).custom(8, CRY_PULSE),
            DmSpec.move(OSSUARY_RISE, "Ossuary Rise").power(60).wind(64).cd(1500).range(0, 40).weight(5).crowd().ultimate().custom(30, RISE_PILLAR),
            // the leveled form: appended, never reordered
            DmSpec.move(BLACK_SWORD_WAVE, "Black Sword Wave").power(100).wind(28).cd(220).range(6, 34).weight(4).recovery(12).leveled().lane(1, 30, 3.0, 21, 1.4, 0.7));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<DmSpec> specs() { return SPECS; }

    public static DmSpec byId(String id) {
        for (DmSpec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
