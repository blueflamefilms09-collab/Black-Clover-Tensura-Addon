package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: effects can be gated on the instance's named counters ({@code "wrong>=3"}), which is what lets a mechanic be a small state machine out of the same primitives (count the
 * wrong presses with {@code counter_add}, spawn the wave only after the third) without a scripting language. The syntax is {@code name op number} with op one of
 * {@code >= <= > < == !=}; a counter that was never set reads 0; text that does not parse is "true" (a typo in a datapack must not silently disable an effect), and the
 * generator rejects it before it ever ships. Pure.
 */
public final class Cond {
    private Cond() {}

    private static final Pattern P = Pattern.compile("\\s*([a-z0-9_]+)\\s*(>=|<=|==|!=|>|<)\\s*(-?\\d+)\\s*");

    public static boolean valid(String s) { return s != null && P.matcher(s).matches(); }

    public static boolean test(String s, Map<String, Integer> counters) {
        Matcher m = P.matcher(s == null ? "" : s);
        if (!m.matches()) return true;
        int have = counters.getOrDefault(m.group(1), 0), want = Integer.parseInt(m.group(3));
        return switch (m.group(2)) {
            case ">=" -> have >= want;
            case "<=" -> have <= want;
            case ">" -> have > want;
            case "<" -> have < want;
            case "==" -> have == want;
            default -> have != want;
        };
    }
}
