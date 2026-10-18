package com.newuniverse.nusmp.elite.nations.war;

/**
 * Why: the protection rules of ("no friendly fire between allied nations, war PvP only while active and only between warring members"), pure. A player who belongs to NO nation is never touched by these
 * rules (vanilla PvP settings apply). Between two members of nations: the same nation is protected (config), allied nations are protected, other pairs may fight ONLY while a war between them is ACTIVE
 * (config {@code warOnlyPvp}; when it is off the rules only add the two friendly-fire protections). Never applied inside the dungeon dimensions (the caller checks, because the dimension test needs the game).
 */
public final class PvpRules {
    private PvpRules() {}

    public enum Verdict { ALLOW_VANILLA, ALLOW_WAR, DENY_SAME_NATION, DENY_ALLIED, DENY_NOT_AT_WAR }

    public static Verdict check(String attackerNation, String victimNation, boolean sameNationProtected, boolean allied, boolean warActiveBetween, boolean warOnlyPvp) {
        if (attackerNation == null || attackerNation.isEmpty() || victimNation == null || victimNation.isEmpty()) return Verdict.ALLOW_VANILLA;
        if (attackerNation.equals(victimNation)) return sameNationProtected ? Verdict.DENY_SAME_NATION : Verdict.ALLOW_VANILLA;
        if (allied) return Verdict.DENY_ALLIED;
        if (warActiveBetween) return Verdict.ALLOW_WAR;
        return warOnlyPvp ? Verdict.DENY_NOT_AT_WAR : Verdict.ALLOW_VANILLA;
    }

    public static boolean denies(Verdict v) { return v == Verdict.DENY_SAME_NATION || v == Verdict.DENY_ALLIED || v == Verdict.DENY_NOT_AT_WAR; }
}
