package com.newuniverse.nusmp.labyrinth.mega.eco;

import java.util.Collection;
import java.util.Set;

/**
 * Why: the turf wars and the faction drops must leave two kinds of creature alone, and this mod already marks both in several places that no
 * single API covers: BOSSES (the Peak Altar's boss, the Wandering Boss of the encounter director, the Floor Warden and the old floor bosses
 * and Juggernauts) are balanced one by one and a faction quarrel must not start or end their fights, and SERVANTS (a Paladin, a Soul Paladin, a
 * Monarch's shadow soldier, a relic's summon, a Wizard's bull, an Underworld Gate servant) belong to a player and are driven by that
 * player, so a goblin's grudge must not take them over and the target guard must not shield them from the monsters that ought to hunt them.
 * Each of those systems sets its own marker (a scoreboard tag or a key in the entity's persistent data); this class is the single list
 * of the marker strings, copied from where they are set, so a new servant system adds ONE string here and every faction rule respects
 * it. The strings are plain text because the markers live in NBT and several of them are package-private in their own packages. Pure (no
 * Minecraft types).
 */
public final class TurfMarks {
    private TurfMarks() {}

    /** Scoreboard tags that mark a boss: {@code PEAK_BOSS_TAG} of the Peak Altar, the encounter director's wanderer, the old floor boss, Juggernaut and Charybdis tags. */
    public static final Set<String> BOSS_TAGS = Set.of("nusmp_peak_boss", "nusmp_wanderer", "nusmp_floor_boss", "nusmp_juggernaut", "nusmp_charybdis");
    /** The Floor Warden's entity id (the Labyrinth Guardian is a subclass and is caught by its class instead). */
    public static final String WARDEN_ID = "nusmp:floor_warden";
    /** Scoreboard tags that mark a servant: the Underworld Gate's, and {@code nusmp_servant}, the one tag any future servant system can set. */
    public static final Set<String> SERVANT_TAGS = Set.of("nusmp_underworld", "nusmp_servant");
    /** Keys in the entity's persistent data that mark a servant: Paladin, Soul Paladin (and the turned Paladin), shadow soldier, relic summon, Wizard's bull. */
    public static final Set<String> SERVANT_KEYS = Set.of("nusmp_paladin", "nusmp_paladin_of", "nusmp_paladin_turn", "nusmp_shadow", "nusmp_relic_owner", "nusmp_bull_owner");

    /** Does any of {@code have} appear in {@code marks}? (An empty or null collection never matches; no allocation.) */
    public static boolean anyOf(Collection<String> have, Set<String> marks) {
        if (have == null || have.isEmpty()) return false;
        for (String s : have) if (marks.contains(s)) return true;
        return false;
    }
}
