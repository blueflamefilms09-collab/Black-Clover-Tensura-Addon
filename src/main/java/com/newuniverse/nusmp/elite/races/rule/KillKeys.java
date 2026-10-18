package com.newuniverse.nusmp.elite.races.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: which of the kill counters ({@code any}, {@code hostile}, {@code boss}, {@code player}) a death feeds is a rule worth testing on its own, away from the game: a player or a
 * boss always counts, a creature counts only above a minimum maximum health (so a farm of chickens cannot buy an evolution), {@code hostile} is a monster above that minimum, and a
 * boss also counts as a hostile and as any.
 */
public final class KillKeys {
    private KillKeys() {}

    public static List<String> classify(boolean isPlayer, boolean isHostile, boolean isBoss, double maxHealth, double minHealth) {
        List<String> keys = new ArrayList<>(3);
        boolean big = maxHealth >= minHealth;
        if (isPlayer) keys.add("player");
        if (isBoss) keys.add("boss");
        if (isPlayer || isBoss || big) keys.add("any");
        if ((isHostile && big) || isBoss) keys.add("hostile");
        return keys;
    }
}
