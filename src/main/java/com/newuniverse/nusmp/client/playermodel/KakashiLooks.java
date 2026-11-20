package com.newuniverse.nusmp.client.playermodel;

import java.util.List;
import java.util.Locale;

/**
 * Why: Kakashi Hatake's boss body on the Black Clover player-model kit (docs/bosses/kakashi.md, tools/bc_playermodels/gen_kakashi.py). Four looks in {@code looks_bc_kakashi.json}, one per state of
 * the eye and the Susanoo, and this pure table (no game class, so the unit test and the CI self-test read it) says which one a state wears, which particle theme its shimmer uses and which pose a
 * move gives the body. {@link PlayerBodies.Kakashi} only asks these functions. Table lookups and a switch on a string: nothing is allocated per frame.
 */
public final class KakashiLooks {
    private KakashiLooks() {}

    /** The hitai-ate over the left eye (idle, phases 1 and 2), the band pushed up (a move of phases 1 and 2), the Mangekyo (phases 3 and 4), the Perfect Susanoo wrapped round the Mangekyo look. */
    public static final String COVERED = "bc_kakashi_hatake", OPEN = "bc_kakashi_hatake_open", MANGEKYO = "bc_kakashi_hatake_mangekyo", SUSANOO = "bc_kakashi_hatake_susanoo";
    private static final List<String> IDS = List.of(COVERED, OPEN, MANGEKYO, SUSANOO);

    /** The ids of the four looks (for the audit, the tests and the docs). */
    public static List<String> ids() { return IDS; }

    public static boolean isKakashi(String lookId) { return lookId != null && IDS.contains(lookId); }

    /** The names without the {@code bc_} prefix (the old-look ids the audit of the tables accepts, since Kakashi's old body is a mesh, not a look). */
    public static boolean knowsName(String nameWithoutPrefix) { return nameWithoutPrefix != null && IDS.contains(BcLooks.PREFIX + nameWithoutPrefix); }

    /**
     * Which look a state wears: the Susanoo while it stands, the Mangekyo look from phase 3 (the eye is the point of the phase), the open eye while a move is in hand (the band is pushed up to
     * use it), else the covered eye.
     */
    public static String pick(boolean susanoo, int phase, boolean acting) {
        if (susanoo) return SUSANOO;
        if (phase >= 2) return MANGEKYO;
        return acting ? OPEN : COVERED;
    }

    /** The particle theme of the shimmer of each look (an id of particle_themes.json), or null for any other id. */
    public static String theme(String lookId) {
        if (lookId == null) return null;
        return switch (lookId) {
            case COVERED, OPEN -> "boss.kakashi.raikiri";
            case MANGEKYO -> "boss.kakashi.kamui";
            case SUSANOO -> "boss.kakashi.susanoo";
            default -> null;
        };
    }

    /** The poses the body can strike (pure ids; {@link PosePatterns} draws them). */
    public static final int NONE = 0, RAIKIRI = 1, SEALS = 2, THROW = 3, KAMUI_EYE = 4, SUSANOO_POSE = 5, SLASH = 6;

    /** What a move looks like in the body: the Raikiri hand drawn back, seals, a throw, the hand at the eye, the arms spread, a sword cut. Unknown moves: nothing (the kit's cast pose). */
    public static int actionOf(String moveId) {
        if (moveId == null) return NONE;
        String id = moveId.toLowerCase(Locale.ROOT);
        if (id.contains("kamui_step") || id.contains("kamui_vortex") || id.contains("kamui_warp") || id.contains("sharingan")) return KAMUI_EYE;
        if (id.contains("raikiri") || id.contains("cutter") || id.contains("beast_fang") || id.equals("purple_lightning") || id.equals("purple_strike") || id.equals("purple_chain")) return RAIKIRI;
        if (id.contains("susanoo") || id.contains("six_paths") || id.equals("purple_burst") || id.contains("eight_gates")) return SUSANOO_POSE;
        if (id.contains("kunai") || id.contains("shuriken") || id.contains("thunder_god")) return THROW;
        if (id.contains("kubikiri") || id.contains("sabre")) return SLASH;
        if (id.contains("water_dragon") || id.contains("clone") || id.contains("mist") || id.contains("earth") || id.contains("ninken") || id.contains("fireball")) return SEALS;
        return NONE;
    }
}
