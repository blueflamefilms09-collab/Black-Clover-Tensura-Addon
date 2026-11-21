package com.newuniverse.nusmp.boss.rules;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: Goku and Kakashi lose health "by themselves" in their arena and the log never said which damage it was (the blows that carry an attacker are not caught by the anonymous-damage rule, so nothing
 * was written). This is the bounded memory behind the diagnostic line of {@code bossRules.logDamageBosses}: the first time a boss of the list takes a NEW kind of hit (damage type, attacker type,
 * striking entity type) one line is written; the same kind is never written twice and at most {@link #MAX_LINES} lines are written per run, so the log can never flood. It changes no gameplay.
 */
public final class DamageProbe {
    private DamageProbe() {}

    public static final int MAX_LINES = 24;
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    /** True the first time this kind of hit is met (and the cap is not reached); false for a repeat or once the cap is full. */
    public static boolean first(String boss, String msgId, String attacker, String striker) {
        if (SEEN.size() >= MAX_LINES) return false;
        return SEEN.add(boss + "|" + msgId + "|" + attacker + "|" + striker);
    }

    /** Forgets everything (a test, or a server restart in the same process). */
    public static void reset() { SEEN.clear(); }
}
