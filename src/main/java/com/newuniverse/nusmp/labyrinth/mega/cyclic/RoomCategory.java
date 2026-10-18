package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.Locale;

/**
 * Why: the owner asks for five FUNCTIONAL room categories to be balanced (Combat Chambers, Puzzles and Hazards, Lore / Scriptoriums, Transitions, Safe Rooms),
 * while the generator only knows {@link RoomKind}, which is the folder a template lives in (a "hall" can be a barracks or a library). The balance of the dungeon
 * is therefore stated here once, as a pure function from (kind, template id) to a category, so the loop pass, the tests and the in-game self-test all count the
 * same way. The rules are deliberately simple and readable:
 * <ul>
 * <li><b>SAFE</b>: Landing, Safe Zone (the stairwells are Transitions: they join floors, even though nobody is ambushed in them).</li>
 * <li><b>TRANSITION</b>: corridors, junctions, tunnel links, the Way Onward, the antechamber, the stairwells and every hallway room the loop pass adds.</li>
 * <li><b>PUZZLE_HAZARD</b>: puzzle and trap rooms, and halls whose name says water, lava or a chasm (cistern, lava hall, flooded nave ...).</li>
 * <li><b>LORE</b>: treasure closets, vaults, set pieces, and halls that are libraries, archives, scriptoriums, shrines, chapels and the like.</li>
 * <li><b>COMBAT</b>: trial halls, the arena, caverns and every other hall (barracks, armoury, crypts, dens ...): where things fight.</li>
 * </ul>
 * A hall is read by words in its template id, so a new library dropped into {@code structure/mega/hall/} is Lore without a line of Java.
 */
public enum RoomCategory {
    COMBAT, PUZZLE_HAZARD, LORE, TRANSITION, SAFE;

    private static final String[] LORE_WORDS = {"library", "archive", "scriptorium", "stacks", "scroll", "reading", "catalogue", "shrine", "chapel", "nave", "cloister",
            "study", "reliquary", "ledger", "counting_house", "temple", "court", "terrace", "sundial", "colonnade", "mirror", "zen_garden", "prayer", "baptistery", "confessional"};
    private static final String[] HAZARD_WORDS = {"cistern", "lava", "chasm", "flooded", "pit", "darklake", "burning", "torture", "geode_echo", "web", "spore"};

    /** The category of a room of this kind and template ({@code templateId} may be null or empty for a made-up room). */
    public static RoomCategory of(RoomKind kind, String templateId) {
        if (kind == null) return TRANSITION;
        switch (kind) {
            case LANDING, SAFE:
                return SAFE;
            case CORRIDOR, JUNCTION, LINK, ONWARD, ANTECHAMBER, STAIRWELL, CAP, OTHER:
                return TRANSITION;
            case TRAP, PUZZLE:
                return PUZZLE_HAZARD;
            case TRIAL, ARENA, CAVERN:
                return COMBAT;
            case TREASURE, VAULT, SET_PIECE:
                return LORE;
            case HALL:
            default:
                break;
        }
        String id = templateId == null ? "" : templateId.toLowerCase(Locale.ROOT);
        int slash = id.lastIndexOf('/');
        String name = slash >= 0 ? id.substring(slash + 1) : id;
        for (String w : HAZARD_WORDS) if (name.contains(w)) return PUZZLE_HAZARD;
        for (String w : LORE_WORDS) if (name.contains(w)) return LORE;
        return COMBAT;
    }
}
