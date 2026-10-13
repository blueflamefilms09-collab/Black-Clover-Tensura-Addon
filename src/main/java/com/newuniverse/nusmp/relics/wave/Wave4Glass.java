package com.newuniverse.nusmp.relics.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 4: the Arcane Glass of Seeing's d6 as game rules. A look into the gem rolls a d6: a 1 or 2 clouds the next attack
 * ("disadvantage": it lands for 30 percent less), a 3 to 6 sharpens it ("advantage": 50 percent more, a clean strike). The mark
 * lasts ten seconds or one blow, whichever comes first. Pure Java.
 */
final class Wave4Glass {
    private Wave4Glass() {}

    static final int KEEN = 1;
    static final int CLOUDED = -1;
    static final int WINDOW = 200;
    static final float KEEN_MULT = 1.5f;
    static final float CLOUDED_MULT = 0.7f;
    private static final int CAP = 256;

    private record Pending(int kind, long until) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** The d6: 1 and 2 cloud the next attack, 3 to 6 sharpen it. */
    static int kindOf(int d6) { return d6 <= 2 ? CLOUDED : KEEN; }

    static float multiplier(int kind) { return kind == KEEN ? KEEN_MULT : kind == CLOUDED ? CLOUDED_MULT : 1f; }

    static void arm(UUID player, int kind, long now) {
        if (PENDING.size() >= CAP) PENDING.values().removeIf(p -> p.until <= now);
        if (PENDING.size() >= CAP) PENDING.clear();
        PENDING.put(player, new Pending(kind, now + WINDOW));
    }

    /** Spends the pending mark: its kind, or 0 when there is none (or it ran out). */
    static int take(UUID player, long now) {
        Pending p = PENDING.remove(player);
        return p == null || p.until <= now ? 0 : p.kind;
    }

    static boolean armed(UUID player, long now) {
        Pending p = PENDING.get(player);
        return p != null && p.until > now;
    }

    static void forget(UUID player) { PENDING.remove(player); }

    static void clear() { PENDING.clear(); }
}
