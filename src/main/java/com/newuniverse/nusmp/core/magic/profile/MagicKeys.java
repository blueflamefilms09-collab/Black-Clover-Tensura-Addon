package com.newuniverse.nusmp.core.magic.profile;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pure string helpers that every magic profile, the audit and the merge generator share: ids, restricted names, resource and lang keys.
 * Nothing here knows a game class, so it is unit tested. A magic id is either an enum constant name ("FLAME", "ICE_WEDGE") or a merged id
 * ("merged:flame+water").
 */
public final class MagicKeys {
    private MagicKeys() {}

    /** The prefix of every merged magic id. */
    public static final String MERGED_PREFIX = "merged:";

    /**
     * Magics that are never rolled for a soul (devil, command or admin only). By NAME, not by ordinal or by switch: appending enum
     * constants can never shift it. Anything listed here that is not an enum constant is caught by the audit.
     */
    public static final Set<String> RESTRICTED = Collections.unmodifiableSet(new LinkedHashSet<>(java.util.List.of(
            "ANTI_MAGIC", "KOTODAMA", "DEMON_BEAST", "CURSE", "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT", "DEMON_WATER")));

    /** The one book whose registry id is not "book_" + the lower-case constant name (the Flame book is "book_fire"). */
    private static String bookException(String key) { return "FLAME".equals(key) ? "book_fire" : null; }

    public static boolean isRestricted(String id) { return RESTRICTED.contains(enumKey(id)); }

    public static boolean isMerged(String id) { return id != null && id.trim().toLowerCase(Locale.ROOT).startsWith(MERGED_PREFIX); }

    /** Trimmed, never null. */
    public static String clean(String id) { return id == null ? "" : id.trim(); }

    /** The enum constant spelling of an id: upper case, spaces and dashes as underscores ("ice wedge" -> "ICE_WEDGE"). */
    public static String enumKey(String id) {
        return clean(id).toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    /** The canonical key of any id: enum names upper case, merged ids lower case. This is what registries key by. */
    public static String canonical(String id) {
        String c = clean(id);
        return isMerged(c) ? c.toLowerCase(Locale.ROOT) : enumKey(c);
    }

    /** "ICE_WEDGE" -> "Ice Wedge"; "merged:flame+water" -> "Flame-Water"; blank -> "Unknown Magic". */
    public static String prettify(String id) {
        String c = clean(id);
        if (c.isEmpty()) return "Unknown Magic";
        boolean merged = isMerged(c);
        if (merged) c = c.substring(MERGED_PREFIX.length());
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if (ch == '_' || ch == ' ') { sb.append(' '); cap = true; }
            else if (ch == '+') { sb.append('-'); cap = true; }
            else { sb.append(cap ? Character.toUpperCase(ch) : Character.toLowerCase(ch)); cap = false; }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "Unknown Magic" : out;
    }

    /** Texture path (inside assets/nusmp) of the magic's grimoire / skill icon. */
    public static String iconPath(String id) {
        return "textures/skill/grimoire/" + enumKey(id).toLowerCase(Locale.ROOT) + ".png";
    }

    /** The icon every magic falls back to when its own is missing (a plain rune-circle glyph; see tools/magic/gen_merged_icon.py). */
    public static final String FALLBACK_ICON = "textures/skill/grimoire/merged.png";

    /** The icons of the Buff / rune and Ultimate cards (see core.magic.grimoire.SpellArchetype.iconPath). */
    public static String buffIconPath(String id) { return "textures/skill/icons/" + enumKey(id).toLowerCase(Locale.ROOT) + "_buff.png"; }
    public static String ultimateIconPath(String id) { return "textures/skill/icons/" + enumKey(id).toLowerCase(Locale.ROOT) + "_ultimate.png"; }

    /** Registry path of the magic's Unique book ("book_fire" for Flame, "book_<name>" for every other constant). */
    public static String bookPath(String id) {
        String k = enumKey(id);
        String ex = bookException(k);
        return ex != null ? ex : "book_" + k.toLowerCase(Locale.ROOT);
    }

    /** The name of the Aura slot (aura.Aura) a magic uses: the constant of the same name, except the ones whose slot is named differently. */
    public static String auraName(String id) {
        String k = enumKey(id);
        return "COPY".equals(k) ? "IMITATION" : k;
    }

    /** Lang key of the book's skill name, as ManasCore builds it from the registry id. */
    public static String bookNameKey(String bookPath) { return "nusmp.skill." + bookPath; }

    public static String bookDescriptionKey(String bookPath) { return "nusmp.skill." + bookPath + ".description"; }

    /** Lang key of one page's description (multiverse.MasteryPages shows it when the page opens). */
    public static String pageKey(String bookPath, String pageId) { return "nusmp.page." + bookPath + "." + pageId; }
}
