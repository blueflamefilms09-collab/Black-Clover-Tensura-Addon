package com.newuniverse.nusmp.skilltree.rarity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Why: the owner wants the WHOLE franchises Tensura, Black Clover and Magi to stop dropping and being sold as Skill Books (their own skills, the Grimoire items and the djinn vessels are not books and stay),
 * behind a server setting. The setting is a comma separated STRING ({@code blockedFranchises}), because a list value in a NeoForge config throws while the file is still loading; this class turns that string
 * into a set and answers "is this franchise blocked", with no Minecraft type and no config class, so the parser is unit tested on every kind of bad input (empty, spaces, upper case, duplicates, junk
 * characters, an absurdly long list) and every door of the loot (the weight tables, the Guardian pick, the shop's stock) asks the same question the same way. A franchise key is a snake-case id
 * ({@code tensura}, {@code god_of_high_school}); an entry that is not one is ignored, the set is capped, and an empty string blocks nothing.
 *
 * <p>The facade at the bottom is how the game reaches the setting without this class knowing the config: the loot wiring installs a supplier of the EFFECTIVE setting string once at start-up
 * ({@link #setSource}; empty when {@code hideBlockedFranchises} is off), {@link #setting()} reads it and {@link #current()} parses it, keeping the last parse so that a drop never parses a string.
 * With no source installed (every unit test, the client) nothing is blocked. Held books are never touched by any of this: only what the loot, the pity picks and the shop CHOOSE.
 */
public final class BlockedFranchises {
    private BlockedFranchises() {}

    /** The default of the setting: the three franchises the owner named. */
    public static final String DEFAULT = "tensura,black_clover,magi";
    /** The most entries read from the setting. */
    public static final int MAX_ENTRIES = 64;
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]{1,40}");
    private static final Pattern SPLIT = Pattern.compile("[,;\\s]+");

    /** The franchise keys of a setting string: lower-cased, trimmed, split on commas, semicolons and spaces, junk and duplicates dropped, at most {@link #MAX_ENTRIES}. Never null, never throws. */
    public static Set<String> parse(String setting) {
        if (setting == null || setting.isBlank()) return Set.of();
        String text = setting.length() > 4096 ? setting.substring(0, 4096) : setting;
        Set<String> out = new LinkedHashSet<>();
        for (String part : SPLIT.split(text.toLowerCase(java.util.Locale.ROOT))) {
            if (out.size() >= MAX_ENTRIES) break;
            if (KEY.matcher(part).matches()) out.add(part);
        }
        return Collections.unmodifiableSet(out);
    }

    /** Is this franchise key in the set (case-insensitive)? A null or empty key (a classic route with no franchise) is never blocked. */
    public static boolean blocked(Set<String> set, String franchise) {
        if (set == null || set.isEmpty() || franchise == null || franchise.isEmpty()) return false;
        return set.contains(franchise.toLowerCase(java.util.Locale.ROOT));
    }

    // ------------------------------------------------------------------ the facade
    private static volatile Supplier<String> source = () -> "";
    private record Parsed(String text, Set<String> set) {}

    private static volatile Parsed last = new Parsed("", Set.of());

    /** Installs the supplier of the effective setting string (the loot wiring does this once); null removes it (nothing is blocked). */
    public static void setSource(Supplier<String> supplier) { source = supplier == null ? () -> "" : supplier; }

    /** The supplier in force (the self-test swaps it for a fixed string and puts this back). */
    public static Supplier<String> source() { return source; }

    /** The effective setting string right now ("" when nothing is blocked or the setting cannot be read). */
    public static String setting() {
        try {
            String s = source.get();
            return s == null ? "" : s;
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    /** The blocked set of the setting in force; parsed once per distinct text. */
    public static Set<String> current() {
        String text = setting();
        if (text.isEmpty()) return Set.of();
        Parsed p = last;
        if (text.equals(p.text())) return p.set();
        p = new Parsed(text, parse(text));
        last = p;
        return p.set();
    }

    /** Is this franchise blocked by the setting in force? */
    public static boolean isBlocked(String franchise) { return blocked(current(), franchise); }
}
