package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Locale;

/**
 * Why: the owner's brief is "allow the player to pick where each skill point goes", but the first System (1.55.0) had the server pick the stat by dice and some
 * servers will want that surprise to stay. So where a cleared floor quest's stat reward goes is a SERVER setting ({@code system.pointMode}): {@link #CHOOSE}
 * (the default) pays the reward into the player's free pool and the player decides on the stat screen, {@link #RANDOM} is the 1.55.0 behaviour (the roller picks
 * the stat). The config holds a plain string (never a list-validated value, which throws while the server is still loading), so this reads it leniently: anything
 * that is not "random" is the default. Pure.
 */
public enum PointMode {
    /** The reward goes to the free pool; the player picks the stat. The default. */
    CHOOSE,
    /** The reward is placed at random by {@link RewardRoller} (the 1.55.0 behaviour). */
    RANDOM;

    /** The lower-case id used in the config file. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The mode for a config word: "random" (any case, spaces ignored) is {@link #RANDOM}, everything else (also null) is {@link #CHOOSE}. */
    public static PointMode of(String word) {
        return word != null && word.trim().equalsIgnoreCase("random") ? RANDOM : CHOOSE;
    }

    /** The mode for its wire code (the ordinal); an unknown code is the default. */
    public static PointMode ofCode(int code) {
        PointMode[] all = values();
        return code >= 0 && code < all.length ? all[code] : CHOOSE;
    }
}
