package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;

import java.util.List;

/**
 * Why: which arena a character is fought in is a question several systems ask (the builder that places it, the travel that carries the party into it, the
 * director that keeps its fight, the Chronicle that names it, the tests), and each must get the same answer. The rule is short and written once: a character that
 * names an arena ({@code ArcCharacter.arena()}) gets that arena if this registry knows the id; every other character gets the arena of its SAGA (the ruins for the
 * Eye of the Midnight Sun, the throne room for the Spade Kingdom, the court for the Wizard King, the plain for the Seven Deadly Sins, the pit for DanMachi, the
 * plain ring for the allies); and an unknown id or saga falls back to the ring, so nobody is ever left without a place to fight. The shapes are immutable and
 * shared. The one arena the registry does NOT hold is the Final Hour's courtyard ({@code zogratis_arena}): that dimension has its own builder, director and
 * guard (1.46.0), and the Chronicle sends the party there through the Hourglass's own travel.
 */
public final class ArenaShapes {
    private ArenaShapes() {}

    public static final ArenaShape MIDNIGHT_SUN_RUINS = new MidnightSunRuinsShape();
    public static final ArenaShape SPADE_THRONE = new SpadeThroneShape();
    public static final ArenaShape WIZARD_COURT = new WizardCourtShape();
    public static final ArenaShape SUNLIT_PLAIN = new SunlitPlainShape();
    public static final ArenaShape ORARIO_PIT = new OrarioPitShape();
    public static final ArenaShape STONE_RING = new StoneRingShape();

    private static final List<ArenaShape> ALL = List.of(MIDNIGHT_SUN_RUINS, SPADE_THRONE, WIZARD_COURT, SUNLIT_PLAIN, ORARIO_PIT, STONE_RING);

    /** Every arena, the generic ring last. */
    public static List<ArenaShape> all() { return ALL; }

    /** The arena with this id, or null (the Final Hour's id, an empty id and a typo all answer null). */
    public static ArenaShape byId(String id) {
        if (id == null) return null;
        for (ArenaShape s : ALL) if (s.id().equalsIgnoreCase(id.trim())) return s;
        return null;
    }

    /** The arena a character is fought in: the one it names, else its saga's, else the plain ring. */
    public static ArenaShape forCharacter(ArcCharacter c) {
        if (c == null) return STONE_RING;
        ArenaShape named = byId(c.arena());
        if (named != null) return named;
        return switch (c.saga()) {
            case ELF_REINCARNATION -> MIDNIGHT_SUN_RUINS;
            case SPADE_RAID -> SPADE_THRONE;
            case WIZARD_KING -> WIZARD_COURT;
            case SEVEN_DEADLY_SINS -> SUNLIT_PLAIN;
            case DANMACHI -> ORARIO_PIT;
            case ALLIES, TIER_LIST -> STONE_RING;
        };
    }

    /** Is the character fought in the Final Hour's own arena (the Hourglass's dimension) instead of a story arena? */
    public static boolean usesFinalHour(ArcCharacter c) { return c != null && "zogratis_arena".equalsIgnoreCase(c.arena()); }
}
