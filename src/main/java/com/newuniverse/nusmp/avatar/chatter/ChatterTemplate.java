package com.newuniverse.nusmp.avatar.chatter;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: dictionary lines carry placeholders ({name}, {creator}, {target}, {skill}, {grimoire}, {dungeon}, {relic}, {magicule}); one tiny class
 * fills them and validates them (the schema test uses {@link #placeholders}) so a typo in a data file can never print a raw "{tagret}" in chat.
 */
public final class ChatterTemplate {
    private ChatterTemplate() {}

    public static final Set<String> KNOWN = Set.of("name", "creator", "target", "skill", "grimoire", "dungeon", "relic", "magicule");

    private static final Pattern HOLE = Pattern.compile("\\{([a-zA-Z_]+)}");

    /** The placeholder names a template uses, in order of first appearance. */
    public static Set<String> placeholders(String template) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = HOLE.matcher(template);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** Fills every known placeholder from {@code values}; unknown or missing ones become an empty string, never a raw brace. */
    public static String fill(String template, Map<String, String> values) {
        Matcher m = HOLE.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = values.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v));
        }
        m.appendTail(sb);
        return sb.toString().replaceAll("\\s{2,}", " ").strip();
    }
}
