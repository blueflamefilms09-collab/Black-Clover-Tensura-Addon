package com.newuniverse.nusmp.arc.zagred;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Why: Overwrite, his ultimate, writes ONE rule over the arena for twelve seconds and punishes whoever breaks it. The rules are a short, always
 * announced list (so nobody is ever hit by something they were not told), each with an obvious way to obey it, and the party's other answer is to break
 * three of the five stones that hold the sentence. This is the pure part: the list, the text shown on the screen, the pick (never the same rule twice
 * in a row) and the test "does this state break the rule". Enforcement against real players lives in the boss class.
 */
public enum ZagredRule {
    NO_FIRE("No fire", "Do not strike him with fire", "fire"),
    NO_ICE("No ice", "Do not strike him with ice", "ice"),
    NO_LIGHTNING("No lightning", "Do not strike him with lightning", "lightning"),
    NO_FLYING("No flying", "Keep your feet on the ground", null),
    NO_STILL("No standing still", "Never stand still for a second", null),
    NO_HEALING("No healing", "Do not mend yourself", null);

    /** Seconds of airborne time after which a player counts as flying (a jump, even a long fall, is shorter than this). */
    public static final int AIRBORNE_LIMIT = 40;
    /** Ticks of standing still after which a player counts as standing still. */
    public static final int STILL_LIMIT = 20;

    private final String text, hint, element;

    ZagredRule(String text, String hint, String element) {
        this.text = text;
        this.hint = hint;
        this.element = element;
    }

    /** The rule as written ("No fire"). */
    public String text() { return text; }

    /** How to obey it. */
    public String hint() { return hint; }

    /** The element whose blows on him are forbidden ("fire"), or null for a rule about movement or healing. */
    public String element() { return element; }

    public String textKey() { return "message.nusmp.zagred.rule." + name().toLowerCase(Locale.ROOT); }

    public String hintKey() { return textKey() + ".hint"; }

    /** The next rule: any but {@code last}, drawn by {@code rng}. */
    public static ZagredRule next(Random rng, ZagredRule last) {
        List<ZagredRule> pool = new ArrayList<>(List.of(values()));
        if (last != null && pool.size() > 1) pool.remove(last);
        return pool.get(rng.nextInt(pool.size()));
    }

    /**
     * Does this state break the movement rules? Only {@link #NO_FLYING} and {@link #NO_STILL} are about movement; the others are enforced where the
     * blow lands (the element rules) or where health goes up (no healing).
     *
     * @param flying         the player is flying by an ability, an elytra or a levitation effect
     * @param airborneTicks  ticks in a row off the ground
     * @param stillTicks     ticks in a row standing still on the ground
     */
    public boolean breaks(boolean flying, int airborneTicks, int stillTicks) {
        return switch (this) {
            case NO_FLYING -> flying || airborneTicks > AIRBORNE_LIMIT;
            case NO_STILL -> stillTicks >= STILL_LIMIT;
            default -> false;
        };
    }
}
