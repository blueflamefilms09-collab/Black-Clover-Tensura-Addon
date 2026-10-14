package com.newuniverse.nusmp.core.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The pure rules of the discovery system (no Minecraft types, so it is unit-tested): what a key looks like, which things are tracked, when
 * an entry is shown and how an undiscovered one is labelled, how the slot scan finds what is new and how a sync is cut into packets.
 * <p>
 * A key is a short string with a kind prefix: {@code item:nusmp:heart_of_words}, {@code page:fire_book:attribute_bolt},
 * {@code skill:nusmp:grimoire_fire}, {@code magic:flame}, {@code mob:nusmp:zagred}, {@code spiritlord:salamander}, {@code boss:dante}.
 * The one question every UI asks is {@link #visible}: is this key known to the player (or does the player see everything)?
 */
public final class DiscoveryRules {
    private DiscoveryRules() {}

    /** What an undiscovered entry is called on screen. A fixed string, so its length gives nothing away. */
    public static final String QUESTION = "???";
    /** The data version written after the grandfathering pass; a player below it gets the pass on the next login. */
    public static final int VERSION = 1;
    /** The most keys one player can hold (a guard against runaway growth; the mod has far fewer things than this). */
    public static final int MAX_KEYS = 8192;
    public static final int MAX_KEY_LENGTH = 120;
    /** Keys per sync packet. */
    public static final int CHUNK = 800;

    public static final String ITEM = "item:", PAGE = "page:", SKILL = "skill:", MAGIC = "magic:", MOB = "mob:", SPIRIT_LORD = "spiritlord:", BOSS = "boss:";

    /** Only things of these namespaces are tracked and hidden (the mod's own); everything else is always shown. */
    private static final Set<String> TRACKED = Set.of("nusmp");

    public static boolean tracksNamespace(String namespace) { return namespace != null && TRACKED.contains(namespace); }

    // ---------------------------------------------------------------- keys
    public static String itemKey(String namespace, String path) { return ITEM + namespace + ":" + path; }

    public static String skillKey(String namespace, String path) { return SKILL + namespace + ":" + path; }

    public static String pageKey(String bookPath, String pageId) { return PAGE + bookPath + ":" + pageId; }

    public static String magicKey(String magicName) { return MAGIC + magicName.toLowerCase(Locale.ROOT); }

    public static String mobKey(String namespace, String path) { return MOB + namespace + ":" + path; }

    public static String spiritLordKey(String kind) { return SPIRIT_LORD + kind.toLowerCase(Locale.ROOT); }

    public static String bossKey(String id) { return BOSS + id.toLowerCase(Locale.ROOT); }

    /** A key a player may hold: non-empty, short, one of the known prefixes, no whitespace or control characters. */
    public static boolean validKey(String key) {
        if (key == null || key.length() < 6 || key.length() > MAX_KEY_LENGTH) return false;
        boolean prefixed = false;
        for (String p : new String[]{ITEM, PAGE, SKILL, MAGIC, MOB, SPIRIT_LORD, BOSS}) if (key.startsWith(p) && key.length() > p.length()) prefixed = true;
        if (!prefixed) return false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c <= ' ' || c == 127) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- showing and hiding
    /** True when the entry may be drawn with its real name: the viewer sees everything, or the key is known. */
    public static boolean visible(Set<String> known, boolean showAll, String key) {
        if (showAll) return true;
        return key != null && known != null && known.contains(key);
    }

    /** An entry gated by several keys (any one is enough). No keys at all = nothing gates it = always visible. */
    public static boolean visibleAny(Set<String> known, boolean showAll, Collection<String> keys) {
        if (showAll || keys == null || keys.isEmpty()) return true;
        for (String k : keys) if (visible(known, false, k)) return true;
        return false;
    }

    /** "a|b|c" (the form a page entry carries its keys in) as a list; blanks dropped. */
    public static List<String> splitKeys(String joined) {
        List<String> out = new ArrayList<>();
        if (joined == null) return out;
        for (String s : joined.split("\\|")) {
            String t = s.strip();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** The real name for a discovered entry, {@link #QUESTION} for an undiscovered one. */
    public static String label(Set<String> known, boolean showAll, String key, String realName) {
        return visible(known, showAll, key) ? realName : QUESTION;
    }

    // ---------------------------------------------------------------- the slot scan
    /**
     * Compares the previous scan with the current one slot by slot (by identity), stores the current one into {@code prev} and hands every
     * slot that now holds something different (and not empty) to {@code onNew}. Returns how many slots changed. An unchanged inventory costs
     * one pass of reference comparisons and nothing else.
     */
    public static <T> int diffSlots(T[] prev, T[] now, Consumer<T> onNew) {
        int changed = 0;
        int n = Math.min(prev.length, now.length);
        for (int i = 0; i < n; i++) {
            if (prev[i] == now[i]) continue;
            prev[i] = now[i];
            changed++;
            if (now[i] != null) onNew.accept(now[i]);
        }
        return changed;
    }

    /** The modes (page numbers) a grimoire's unlock bitmask opens, among the first {@code familyCount}: mode 0 is always open. */
    public static List<Integer> unlockedModes(int mask, int familyCount) {
        List<Integer> out = new ArrayList<>();
        int n = Math.min(Math.max(familyCount, 0), 31);
        for (int m = 0; m < n; m++) if (m == 0 || (mask & (1 << m)) != 0) out.add(m);
        return out;
    }

    // ---------------------------------------------------------------- sync and migration
    /** Cuts a key list into packet-sized chunks (at least one, possibly empty, so a full sync always sends something). */
    public static List<List<String>> chunks(List<String> keys) {
        List<List<String>> out = new ArrayList<>();
        if (keys.isEmpty()) { out.add(List.of()); return out; }
        for (int i = 0; i < keys.size(); i += CHUNK) out.add(new ArrayList<>(keys.subList(i, Math.min(keys.size(), i + CHUNK))));
        return out;
    }

    /** True when a player's data is older than the current version: the grandfathering pass must run. */
    public static boolean needsMigration(int storedVersion) { return storedVersion < VERSION; }
}
