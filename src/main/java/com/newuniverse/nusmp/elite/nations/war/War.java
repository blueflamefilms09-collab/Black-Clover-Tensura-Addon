package com.newuniverse.nusmp.elite.nations.war;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: one war between two nations, pure data. {@code attacker} declared it. All times are server game ticks. The scores are those of the two sides; {@code lastKill} is the anti-farming memory (a victim
 * scores for the other side once per cooldown); {@code siege} counts the consecutive hold intervals of each side at the enemy capital. The state machine that moves it is {@link WarRules}.
 */
public final class War {
    public enum Outcome { NONE, VICTORY, DRAW, CEASEFIRE, SURRENDER, CANCELLED, FORCED }

    public final long id;
    public final String attacker, defender;
    public WarPhase phase = WarPhase.DECLARED;
    public long declaredAt, phaseEnds, activeSince;
    public int scoreAttacker, scoreDefender;
    public int killsAttacker, killsDefender, capturesAttacker, capturesDefender;
    public int siegeAttacker, siegeDefender;
    public Outcome outcome = Outcome.NONE;
    public String winner = "";
    /** Nation that proposed a ceasefire (empty = none pending). */
    public String ceasefireFrom = "";
    public String reason = "";
    public long spoilsCoins;
    public final Map<String, Long> lastKill = new HashMap<>();

    public War(long id, String attacker, String defender) {
        this.id = id;
        this.attacker = attacker;
        this.defender = defender;
    }

    public boolean involves(String nation) { return attacker.equals(nation) || defender.equals(nation); }

    public boolean isPair(String a, String b) { return (attacker.equals(a) && defender.equals(b)) || (attacker.equals(b) && defender.equals(a)); }

    public String other(String nation) { return attacker.equals(nation) ? defender : attacker; }

    public int score(String nation) { return attacker.equals(nation) ? scoreAttacker : scoreDefender; }

    public boolean hostile() { return phase.hostile(); }

    public boolean fighting() { return phase.fighting(); }

    /** Still tracked (not archived). */
    public boolean open() { return phase != WarPhase.ENDED; }

    public static String pairKey(String a, String b) { return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a; }

    public String pairKey() { return pairKey(attacker, defender); }
}
