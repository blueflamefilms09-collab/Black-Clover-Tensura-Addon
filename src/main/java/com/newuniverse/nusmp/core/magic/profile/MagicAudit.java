package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The audit of the magic framework: for every profile it asserts that the profile is complete and that everything a magic needs outside
 * its profile exists (its Unique book, a page list, its icon, its lang keys), and REPORTS what is missing, magic by magic. It is pure
 * (the outside world is a {@link Probe}), so it runs as a unit test over the real enum and at game start over the real registries
 * ({@link MagicAuditRuntime}).
 *
 * <p>Severity: ERROR = something that would throw or misbehave at runtime (a null part, a duplicate id, a restricted name that is not a
 * magic, no fallback icon); WARN = something that works through a fallback but should be filled in (no book, no icon, no lang name);
 * INFO = a note (the magic draws the generic effect).</p>
 */
public final class MagicAudit {
    private MagicAudit() {}

    public enum Severity { ERROR, WARN, INFO }

    public record Finding(Severity severity, String magic, String code, String message) {
        @Override public String toString() { return severity + " [" + magic + "] " + code + ": " + message; }
    }

    /** What the audit asks the world. Resource paths are inside assets/nusmp. */
    public interface Probe {
        boolean hasResource(String assetPath);
        boolean hasLang(String key);
        /** Registry path of the magic's Unique book ("book_fire"), or null if no book is registered for it. */
        String bookPath(String magicId);
        /** Number of pages of the magic's book (all modes), or -1 when it has no book. */
        int pageCount(String magicId);
        /** Is the legacy per-magic grimoire skill registered (nusmp:grimoire_&lt;name&gt;)? */
        default boolean legacySkill(String magicId) { return true; }
        /** The ids of the book's own (family) pages, for the page description lang keys; empty = not checked. */
        default java.util.List<String> familyPageIds(String magicId) { return java.util.List.of(); }
    }

    public record Report(List<Finding> findings, int checked) {
        public List<Finding> of(Severity s) {
            List<Finding> l = new ArrayList<>();
            for (Finding f : findings) if (f.severity() == s) l.add(f);
            return l;
        }
        public int errors() { return of(Severity.ERROR).size(); }
        public int warnings() { return of(Severity.WARN).size(); }
        public boolean clean() { return errors() == 0; }

        /** Count of findings with this code. */
        public int count(String code) {
            int n = 0;
            for (Finding f : findings) if (f.code().equals(code)) n++;
            return n;
        }

        public boolean has(String magic, String code) {
            for (Finding f : findings) if (f.magic().equalsIgnoreCase(magic) && f.code().equals(code)) return true;
            return false;
        }

        /** "NO_LANG_NAME x31, NO_ICON x2": how many findings of each code, most frequent first. */
        public String summary() {
            java.util.Map<String, Integer> m = new java.util.TreeMap<>();
            for (Finding f : findings) if (f.severity() != Severity.INFO) m.merge(f.code(), 1, Integer::sum);
            StringBuilder sb = new StringBuilder();
            m.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey()).append(" x").append(e.getValue()));
            return sb.length() == 0 ? "nothing missing" : sb.toString();
        }

        /** This report plus more findings. */
        public Report plus(List<Finding> more) {
            List<Finding> l = new ArrayList<>(findings);
            l.addAll(more);
            return new Report(l, checked);
        }

        /** Human lines, errors first. */
        public List<String> lines() {
            List<String> l = new ArrayList<>();
            l.add("magic audit: " + checked + " magics, " + errors() + " errors, " + warnings() + " warnings");
            for (Severity s : Severity.values()) for (Finding f : of(s)) l.add(f.toString());
            return l;
        }
    }

    /** SpellArchetype.packSeed stores the magic ordinal in two decimal digits: more constants than this decode wrongly. */
    public static final int CARD_SEED_LIMIT = 100;

    /**
     * Audits the profiles.
     * @param profiles one per magic (enum order); null entries are reported, not thrown on
     * @param probe    the world; may be null (then only the profile data is checked)
     * @param constants the number of enum constants (for the card-seed limit), 0 to skip that check
     */
    public static Report run(Collection<? extends MagicProfile> profiles, Probe probe, int constants) {
        List<Finding> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int checked = 0;
        boolean fallbackIcon = probe == null || probe.hasResource(MagicKeys.FALLBACK_ICON);
        if (!fallbackIcon) add(out, Severity.ERROR, "*", "NO_FALLBACK_ICON", "the fallback icon " + MagicKeys.FALLBACK_ICON + " is missing (run tools/magic/gen_merged_icon.py)");
        if (profiles != null) {
            for (MagicProfile p : profiles) {
                if (p == null) { add(out, Severity.ERROR, "?", "NULL_PROFILE", "a magic resolved no profile at all"); continue; }
                checked++;
                checkOne(out, p, probe, seen);
            }
            boolean anyPlain = false;
            for (MagicProfile p : profiles) if (p != null && !p.isMerged()) { anyPlain = true; break; }
            for (String r : anyPlain ? MagicKeys.RESTRICTED : Set.<String>of()) {      // a list of merged magics alone says nothing about the enum
                boolean found = false;
                for (MagicProfile p : profiles) if (p != null && MagicKeys.enumKey(p.id()).equals(r)) { found = true; break; }
                if (!found) add(out, Severity.ERROR, r, "RESTRICTED_UNKNOWN", "MagicKeys.RESTRICTED lists '" + r + "' but no such magic exists (typo or removed constant)");
            }
        }
        if (constants >= CARD_SEED_LIMIT)
            add(out, Severity.WARN, "*", "CARD_SEED_LIMIT", constants + " magics exist but SpellArchetype.packSeed keeps the magic ordinal in two digits (limit "
                    + CARD_SEED_LIMIT + "): spell cards of magics with an ordinal of 100 or more decode as another magic. Unused in production today.");
        return new Report(out, checked);
    }

    private static void checkOne(List<Finding> out, MagicProfile p, Probe probe, Set<String> seen) {
        String id;
        try { id = MagicKeys.canonical(p.id()); } catch (RuntimeException e) { id = ""; }
        if (id.isEmpty()) { add(out, Severity.ERROR, "?", "NO_ID", "a profile has a blank id"); return; }
        if (!seen.add(id)) add(out, Severity.ERROR, id, "DUPLICATE_ID", "two profiles share the id " + id);

        // ---- the profile itself: every part present
        String name = safe(p::displayName), word = safe(p::word), soul = safe(p::soulType);
        if (name.isBlank()) add(out, Severity.ERROR, id, "NO_NAME", "display name is blank");
        if (word.isBlank()) add(out, Severity.ERROR, id, "NO_WORD", "spell-name word is blank");
        if (soul.isBlank()) add(out, Severity.ERROR, id, "NO_SOUL", "soul type is blank");
        HitEffect hit = guard(p::hit);
        if (hit == null) add(out, Severity.ERROR, id, "NO_HIT", "hit effect is null");
        VfxShape cast = guard(p::castShape), field = guard(p::fieldShape), burst = guard(p::burstShape);
        if (cast == null || field == null || burst == null) add(out, Severity.ERROR, id, "NO_VFX_SHAPE", "cast / field / burst shape is null");
        VfxStyle style = guard(p::vfxStyle);
        if (style == null) add(out, Severity.ERROR, id, "NO_VFX_STYLE", "vfx style is null");
        else if (style == VfxStyle.GENERIC) add(out, Severity.INFO, id, "GENERIC_VFX", "no effects of its own: it draws the generic wiki-style effect");
        Integer color = guard(p::color);
        if (color == null || (color >>> 24) == 0) add(out, Severity.ERROR, id, "NO_COLOR", "colour is missing or fully transparent");
        String icon = safe(p::iconPath);
        if (icon.isBlank()) add(out, Severity.ERROR, id, "NO_ICON_PATH", "icon path is blank");
        Double cp = guard(p::safeCostPercent);
        Integer cd = guard(p::safeCooldownTicks);
        if (cp == null || cp.isNaN() || cp < 0 || cd == null || cd < 0) add(out, Severity.ERROR, id, "BAD_SAFE_NUMBERS", "safe cost / cooldown defaults are missing or negative");

        if (probe == null || p.isMerged()) return;

        // ---- everything outside the profile
        if (!icon.isBlank() && !probe.hasResource(icon))
            add(out, Severity.WARN, id, "NO_ICON", "icon " + icon + " is missing (the skill list shows the fallback " + MagicKeys.FALLBACK_ICON + ")");
        String book = probe.bookPath(id);
        if (!probe.legacySkill(id)) add(out, Severity.ERROR, id, "NO_LEGACY_SKILL", "nusmp:grimoire_" + id.toLowerCase(Locale.ROOT) + " is not registered: NUSkills.grimoireSkill() would throw");
        if (book == null) {
            add(out, Severity.WARN, id, "NO_BOOK", "no Unique book is registered (NUSkills.BOOKS / WIKI_ATTRIBUTE_BOOKS): the grimoire falls back to the legacy skill with no pages");
        } else {
            if (probe.pageCount(id) <= 0) add(out, Severity.WARN, id, "NO_PAGES", "the book '" + book + "' has no pages");
            if (!probe.hasLang(MagicKeys.bookNameKey(book))) add(out, Severity.WARN, id, "NO_LANG_NAME", "lang key " + MagicKeys.bookNameKey(book) + " is missing (the skill shows its raw key)");
            if (!probe.hasLang(MagicKeys.bookDescriptionKey(book))) add(out, Severity.WARN, id, "NO_LANG_DESC", "lang key " + MagicKeys.bookDescriptionKey(book) + " is missing");
            List<String> pages = probe.familyPageIds(id), missing = new ArrayList<>();
            if (pages != null) for (String page : pages) if (!probe.hasLang(MagicKeys.pageKey(book, page))) missing.add(page);
            if (!missing.isEmpty())
                add(out, Severity.WARN, id, "NO_LANG_PAGES", missing.size() + " of " + pages.size() + " pages have no lang description (" + MagicKeys.pageKey(book, missing.get(0)) + (missing.size() > 1 ? ", ..." : "") + ")");
        }
    }

    private static void add(List<Finding> out, Severity s, String magic, String code, String msg) { out.add(new Finding(s, magic, code, msg)); }

    private static <T> T guard(java.util.function.Supplier<T> s) {
        try { return s.get(); } catch (RuntimeException | LinkageError e) { return null; }
    }

    private static String safe(java.util.function.Supplier<String> s) {
        String v = guard(s);
        return v == null ? "" : v;
    }
}
