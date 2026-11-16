package com.newuniverse.nusmp.arc.tier;

import java.util.List;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): the one signature move appended to each of the seven weakest Tier List kits (Bertholdt Hoover, Kurozumi Orochi, Shou
 * Tucker, Big Mom, Yammy Llargo, Code, Mr. 2 Bon Clay), built from the existing {@link TierMove.Kind}s (so the telegraph is the kind's own {@code Tells}). Pure (the rows and
 * the filter); {@code TierMoves#signatureLand} throws the one shaped effect each makes as it lands: the Colossal Titan's kick splitting the ground, the eight-headed
 * serpent's venom, a transmutation circle, the Ikoku thrust's crescent, a Cero beam, Code's claw marks, the swan's crescent kick. One effect per cast, through the boss effect budget, sent only while {@code bossRework1Vfx} is on; with
 * {@code bossRework1Moves} off the moves themselves leave the lists (they are the last row of each, so no index moves). Models are not part of this wave.
 */
public final class TierSignature {
    private TierSignature() {}

    public static final String WALL_BREACH_KICK = "wall_breach_kick", YAMATA_VENOM_FANGS = "yamata_venom_fangs", LIFE_SEWING = "life_sewing",
            IKOKU_SOVEREIGNTY = "ikoku_sovereignty", IRA_GIANT_CERO = "ira_giant_cero", CLAW_GRIME = "claw_grime", SWAN_ARABESQUE = "swan_arabesque";

    /** The appended move of each of the seven rows, by boss id (pure data, read by the tests). */
    public static final List<String[]> ROWS = List.of(
            new String[]{"bertholdt_hoover", WALL_BREACH_KICK}, new String[]{"kurozumi_orochi", YAMATA_VENOM_FANGS}, new String[]{"shou_tucker", LIFE_SEWING},
            new String[]{"big_mom", IKOKU_SOVEREIGNTY}, new String[]{"yammy_llargo", IRA_GIANT_CERO}, new String[]{"code", CLAW_GRIME},
            new String[]{"mr_2_bon_clay", SWAN_ARABESQUE});

    public static final List<String> IDS = List.of(WALL_BREACH_KICK, YAMATA_VENOM_FANGS, LIFE_SEWING, IKOKU_SOVEREIGNTY, IRA_GIANT_CERO, CLAW_GRIME, SWAN_ARABESQUE);

    /** Is this one of the appended moves (dropped from the lists while {@code bossRework1Moves} is off)? */
    public static boolean is(String moveId) { return IDS.contains(moveId); }

    /** The moves a row fights with: all of them, or with the setting off its rows without the appended ones (pure: the setting is passed in). */
    public static List<TierMove> moves(List<TierMove> all, boolean reworkOn) {
        if (reworkOn) return all;
        return all.stream().filter(m -> !is(m.id())).toList();
    }
}
