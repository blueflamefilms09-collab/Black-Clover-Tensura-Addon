package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the stat-point screen, the movable-window screen, the badge on the window and the new messages need words, and the owner's rule is that new lang strings
 * go in their own fragment ({@code tools/system/lang_points.json}) and the big {@code en_us.json} is not edited. So the new texts live here, in ONE pure map,
 * under the System's usual key prefix: the code reads its English fallback from this map through {@link SystemText#english} (so a key that is not merged into the
 * game's lang file yet still shows real words, never a raw key), and a unit test pins that the fragment file and this map say exactly the same thing, so they cannot
 * drift. It also holds the one piece of formatting the client and the tests share: how an attribute amount is written ({@code +0.2}, {@code +1%}). The older texts
 * stay in {@link SystemText}, untouched. Pure.
 */
public final class PointsText {
    private PointsText() {}

    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String shortKey, String english) {
        String full = SystemText.PREFIX + shortKey;
        EN.put(full, english);
        return full;
    }

    // ---- the pop-up, the window badge and the chat lines
    public static final String TOAST_FREE = put("points.toast_free", "REWARD: +%1$s STAT POINT(S) TO SPEND");
    public static final String BADGE = put("points.badge", "+%1$s PTS [%2$s]");
    public static final String BADGE_SHORT = put("points.badge_short", "+%1$s");
    public static final String MSG_NO_RESPEC = put("points.msg.no_respec", "Taking stat points back is switched off on this server.");
    public static final String MSG_RESET_DONE = put("points.msg.reset_done", "%1$s stat point(s) are back in your free pool.");

    /** The one-time hint on entry (replaces the use of {@link SystemText#MSG_WINDOW_HINT}, which talked about nudge keys that no longer exist). */
    public static final String MSG_WINDOW_HINT_MOUSE = put("points.msg.window_hint",
            "The System window can be moved and resized with the mouse: press %1$s, then drag it, drag a corner or scroll. Press %2$s to place your stat points.");

    // ---- the admin command (permission 2)
    public static final String CMD_STATUS_FREE = put("points.cmd.status_free", "Free stat points: %1$s (point mode %2$s, taking points back: %3$s)");
    public static final String CMD_FREE_CHANGED = put("points.cmd.free_changed", "%1$s now has %2$s free stat point(s) (%3$s changed).");

    // ---- the stat screen
    public static final String TITLE = put("points.title", "[ SYSTEM ] STAT POINTS");
    public static final String FREE = put("points.free", "FREE POINTS");
    public static final String FREE_NONE = put("points.free_none", "No free points. Clear floor quests to earn more.");
    public static final String FREE_HINT = put("points.free_hint", "Choose where each point goes.");
    public static final String MODE_CHOOSE = put("points.mode_choose", "Quest rewards go to your free pool: you decide.");
    public static final String MODE_RANDOM = put("points.mode_random", "Quest rewards are placed at random by the System.");
    public static final String CAP = put("points.cap", "At most %1$s points per stat");
    public static final String VALUE = put("points.value", "%1$s / %2$s");
    public static final String BTN_ADD1 = put("points.btn.add1", "+1");
    public static final String BTN_ADD5 = put("points.btn.add5", "+5");
    public static final String BTN_MAX = put("points.btn.max", "MAX");
    public static final String BTN_RESET = put("points.btn.reset", "RESET");
    public static final String BTN_MOVE = put("points.btn.move", "MOVE WINDOW");
    public static final String BTN_CLOSE = put("points.btn.close", "CLOSE");
    public static final String TIP_ADD1 = put("points.tip.add1", "Spend 1 free point on %1$s (Shift: 5).");
    public static final String TIP_ADD5 = put("points.tip.add5", "Spend up to 5 free points on %1$s.");
    public static final String TIP_MAX = put("points.tip.max", "Spend as many free points as fit on %1$s.");
    public static final String TIP_ROW = put("points.tip.row", "Scroll: +1 / -1 point (Shift: 5).");
    public static final String TIP_RESET = put("points.tip.reset", "Take every placed point back into the free pool. Free of charge.");
    public static final String TIP_RESET_OFF = put("points.tip.reset_off", "Taking points back is switched off on this server.");
    public static final String TIP_MOVE = put("points.tip.move", "Move and resize the System window with the mouse.");
    public static final String TIP_FULL = put("points.tip.full", "%1$s is at its limit.");
    public static final String TIP_NOFREE = put("points.tip.nofree", "You have no free points.");
    public static final String EFFECT = put("points.effect", "%1$s %2$s per point (now %3$s, at most %4$s)");
    public static final String EFFECT_NOCAP = put("points.effect_nocap", "%1$s %2$s per point (now %3$s)");
    public static final String STAT_STR_FULL = put("points.stat.str", "Strength");
    public static final String STAT_AGI_FULL = put("points.stat.agi", "Agility");
    public static final String STAT_VIT_FULL = put("points.stat.vit", "Vitality");
    public static final String STAT_INT_FULL = put("points.stat.int", "Intelligence");
    public static final String STAT_SENSE_FULL = put("points.stat.sense", "Sense");

    // ---- the move and resize screen
    public static final String EDIT_TITLE = put("points.edit.title", "MOVE AND RESIZE THE SYSTEM WINDOW");
    public static final String EDIT_HINT = put("points.edit.hint", "Drag the window to move it. Drag a corner, or scroll, to resize it.");
    public static final String EDIT_SIZE = put("points.edit.size", "SIZE %1$s");
    public static final String EDIT_RESET = put("points.edit.reset", "RESET");
    public static final String EDIT_DONE = put("points.edit.done", "DONE");
    public static final String EDIT_STATS = put("points.edit.stats", "STAT POINTS");
    public static final String EDIT_SAVED = put("points.edit.saved", "System window: place and size saved");

    // ---- keys
    public static final String KEY_POINTS = "key.nusmp.system_points";

    static {
        EN.put(KEY_POINTS, "Open the stat point screen");
    }

    /** The full key of the full name of a stat ({@code STR} gives the Strength key). */
    public static String statFullKey(Stat s) {
        return switch (s) {
            case STR -> STAT_STR_FULL;
            case AGI -> STAT_AGI_FULL;
            case VIT -> STAT_VIT_FULL;
            case INT -> STAT_INT_FULL;
            case SENSE -> STAT_SENSE_FULL;
        };
    }

    /** The English text of one of these keys, or null when the key is not one of ours. */
    public static String englishOrNull(String key) { return key == null ? null : EN.get(key); }

    /** Every key with its English text, in declaration order (the lang fragment {@code tools/system/lang_points.json} says exactly this). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }

    // ================================================================ formatting
    /**
     * An attribute amount as the player reads it: a plain add is {@code +0.2} (two decimals at most, no trailing zeros), a multiplier is a percentage
     * ({@code perPoint 0.01} is {@code +1%}). The sign is always shown ({@code +} or {@code -}); zero is {@code +0}.
     *
     * @param op the {@link StatTable.Op} of the effect
     */
    public static String amount(StatTable.Op op, double value) {
        boolean percent = op != StatTable.Op.ADD_VALUE;
        double v = percent ? value * 100.0 : value;
        String body = trim(Math.abs(v));
        String sign = v < 0 && !body.equals("0") ? "-" : "+";
        return sign + body + (percent ? "%" : "");
    }

    /** A number with at most two decimals and no trailing zeros ({@code 0.25} stays, {@code 6.0} becomes {@code 6}). */
    static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) {
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') end--;
            if (end > 0 && s.charAt(end - 1) == '.') end--;
            s = s.substring(0, end);
        }
        return s.isEmpty() ? "0" : s;
    }
}
