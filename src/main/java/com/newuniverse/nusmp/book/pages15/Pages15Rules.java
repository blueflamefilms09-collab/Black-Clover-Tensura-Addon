package com.newuniverse.nusmp.book.pages15;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the "15 spells for every grimoire" pages are written by eight builders at once, so the rules every page has to obey live in ONE pure place (no Minecraft class, unit tested, also used by the
 * in-game self-test {@code magic.pages15}): readable snake_case spell ids (never numbered), unique ids, the four tiers with their cost and cooldown bounds and the damage the kit hands out per tier, at most one
 * signature page per book, the page limits (64 modes in the unlock mask, 32 pages visible in one grimoire, at least 15 in a basic grimoire), and the "canon: source" or "original" mark every page carries in
 * its batch doc (rule 10).
 *
 * <p>Worst case of one page (rule 11, the project-wide figure): one server cast; at most {@link Tier#maxTargets} hit targets, one zone of at most {@link #MAX_ZONE_TICKS} ticks at {@link #MIN_ZONE_STEP}+ tick
 * steps, at most {@link #MAX_PACKETS} effect packets to nearby players, nothing per tick after it ends.
 */
public final class Pages15Rules {
    private Pages15Rules() {}

    /** The goal: every basic grimoire shows at least this many pages. */
    public static final int GOAL = 15;
    /** Pages one grimoire may SHOW (its basic grimoire, or a character's): the budget that used to be 24. */
    public static final int MAX_VISIBLE = 32;
    /** Family pages one book may hold in all, hidden ones included: the unlock mask has 64 bits (book.UnlockMask). */
    public static final int MAX_FAMILY = 64;
    /** New pages one book may get from this project (the largest plan is 14; the rest is margin). */
    public static final int MAX_NEW_PER_BOOK = 20;
    /** At most one signature page (35 % cost, the full mastery bar and Senior rank) per book. */
    public static final int MAX_SIGNATURE_PER_BOOK = 1;
    /** The bounds of a standing effect (the project-wide figure of docs/magic/page_counts.md section 8). */
    public static final int MAX_ZONE_TICKS = 100, MIN_ZONE_STEP = 4, MAX_PACKETS = 10;

    /** The four page tiers (BookPage.starter / mid / zone / signature) with the numbers a new page uses. */
    public enum Tier {
        //          cost %  floor  cooldown  min   max   hit  pulse  targets
        STARTER(8, 150, 80, 80, 400, 7f, 2f, 4),
        MID(12, 300, 160, 160, 700, 12f, 3f, 6),
        ZONE(20, 600, 300, 300, 1400, 16f, 5f, 8),
        SIGNATURE(35, 1200, 600, 600, 3000, 26f, 7f, 10);

        public final double costPercent, costFloor;
        public final int cooldown, minCooldown, maxCooldown, maxTargets;
        /** Raw damage of one hit of a page of this tier / of one pulse of a zone (before the balance law scales it, like every other page of the book). */
        public final float hit, pulse;

        Tier(double costPercent, double costFloor, int cooldown, int minCooldown, int maxCooldown, float hit, float pulse, int maxTargets) {
            this.costPercent = costPercent;
            this.costFloor = costFloor;
            this.cooldown = cooldown;
            this.minCooldown = minCooldown;
            this.maxCooldown = maxCooldown;
            this.hit = hit;
            this.pulse = pulse;
            this.maxTargets = maxTargets;
        }

        /** The tier whose cost percent this is, or null (a page of another cost is not a tier page). */
        public static Tier ofCost(double costPercent) {
            for (Tier t : values()) if (Math.abs(t.costPercent - costPercent) < 1e-9) return t;
            return null;
        }

        /** Most raw damage a single hit of this tier may deal (a zone pulse is {@link #pulse}); the self-test and the kit stay below it. */
        public float maxHit() { return hit * 1.5f; }

        /** Mastery needed to open a page of this tier on its own cost (cost % / 35 of the bar, docs/magic/page_counts.md 4.3). */
        public double masteryByCost() { return Math.min(1.0, costPercent / 35.0); }
    }

    // ------------------------------------------------------------------ ids and names
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(_[a-z0-9]+)*");
    private static final Pattern NUMBERED = Pattern.compile(".*_x\\d+$|^(page|spell|skill|magic|new)_?\\d+$|.*_\\d+$");
    /** Ids that already mean something else in a grimoire (the universal pages). */
    private static final Set<String> RESERVED = Set.of("spirit_dive", "spirit_channeling", "devil_union", "summon_grimoire", "store_weapon", "draw_weapon", "mana_strike", "mana_zone");

    /** Null when the id is a readable snake_case spell name, else why not. */
    public static String idProblem(String id) {
        if (id == null || id.isEmpty()) return "empty id";
        if (id.length() < 3 || id.length() > 48) return "id length " + id.length() + " is not 3..48";
        if (!ID.matcher(id).matches()) return "id is not lower snake_case: " + id;
        if (NUMBERED.matcher(id).matches()) return "id is numbered, not a readable spell name: " + id;
        if (RESERVED.contains(id)) return "id is a universal page: " + id;
        return null;
    }

    /** Null when the name is fit to show in the grimoire, else why not. */
    public static String nameProblem(String name) {
        if (name == null || name.isBlank()) return "empty name";
        if (!name.equals(name.strip())) return "name has leading or trailing space: '" + name + "'";
        if (name.length() < 3 || name.length() > 64) return "name length " + name.length() + " is not 3..64";
        for (int i = 0; i < name.length(); i++) if (Character.isISOControl(name.charAt(i))) return "name has a control character";
        return null;
    }

    /** The lang key of a page's description. */
    public static String langKey(String bookPath, String id) { return "nusmp.page." + bookPath + "." + id; }

    // ------------------------------------------------------------------ the mark of every page: canon or original
    /** {@code original} = made up for this project (marked as such in the docs); otherwise the wiki / manga / anime place the spell comes from. */
    public record Origin(boolean original, String source) {}

    private static final Pattern CANON = Pattern.compile("(?i)^canon\\s*:\\s*(\\S.{2,})$");

    /** Parses {@code original} or {@code canon: <source>} (the source names the English or Japanese wiki page, or the chapter / episode); null for anything else. */
    public static Origin parseOrigin(String text) {
        if (text == null) return null;
        String t = text.strip();
        if (t.equalsIgnoreCase("original")) return new Origin(true, "");
        Matcher m = CANON.matcher(t);
        return m.matches() ? new Origin(false, m.group(1).strip()) : null;
    }

    /** A row of a batch doc: {@code | `book_x` | `page_id` | Name | tier | origin | ... |}. */
    public record DocRow(String book, String id, String name, String tier, Origin origin) {}

    private static final Pattern ROW = Pattern.compile("^\\|\\s*`(book_[a-z0-9_]+|forbidden_magic)`\\s*\\|\\s*`([a-z0-9_]+)`\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([a-z]+)\\s*\\|\\s*([^|]+?)\\s*(?:\\|.*)?$");

    /** Parses one table row of {@code docs/magic/pages15_batchN.md}, or returns null for any other line (headers, text). A row with an origin that is neither original nor canon gets origin null. */
    public static DocRow parseDocRow(String line) {
        if (line == null) return null;
        Matcher m = ROW.matcher(line.strip());
        if (!m.matches()) return null;
        return new DocRow(m.group(1), m.group(2), m.group(3), m.group(4).toLowerCase(Locale.ROOT), parseOrigin(m.group(5)));
    }

    // ------------------------------------------------------------------ the checks over a set of pages
    /** What the rules need to know about one page: the book, id, name, cost percent and cooldown ticks. */
    public record Spec(String book, String id, String name, double costPercent, int cooldown) {}

    /**
     * Every problem of a set of new pages (empty = fine).
     *
     * @param specs        all new pages of all books
     * @param baseFamily   book -> family pages the book already had before these pages (hidden ones included)
     * @param existingIds  ids of the pages that existed before (any book): a new id never repeats one
     */
    public static List<String> problems(List<Spec> specs, Map<String, Integer> baseFamily, Set<String> existingIds) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Integer> perBook = new HashMap<>(), signatures = new HashMap<>();
        for (Spec s : specs) {
            String where = s.book() + "/" + s.id();
            String p = idProblem(s.id());
            if (p != null) out.add(where + ": " + p);
            p = nameProblem(s.name());
            if (p != null) out.add(where + ": " + p);
            if (!seen.add(s.id())) out.add(where + ": id used twice in the new pages (ids are unique across all books)");
            if (existingIds != null && existingIds.contains(s.id())) out.add(where + ": id already exists as an older page");
            Tier t = Tier.ofCost(s.costPercent());
            if (t == null) out.add(where + ": cost " + s.costPercent() + " % is not a tier (8 / 12 / 20 / 35)");
            else {
                if (s.cooldown() < t.minCooldown || s.cooldown() > t.maxCooldown)
                    out.add(where + ": cooldown " + s.cooldown() + " ticks is outside " + t.minCooldown + ".." + t.maxCooldown + " of a " + t.name().toLowerCase(Locale.ROOT) + " page");
                if (t == Tier.SIGNATURE) signatures.merge(s.book(), 1, Integer::sum);
            }
            perBook.merge(s.book(), 1, Integer::sum);
            if (Pages15Plan.batchOf(s.book()) == 0) out.add(where + ": " + s.book() + " is not in the plan");
        }
        for (var e : perBook.entrySet()) {
            if (e.getValue() > MAX_NEW_PER_BOOK) out.add(e.getKey() + ": " + e.getValue() + " new pages, over " + MAX_NEW_PER_BOOK);
            int base = baseFamily == null ? 0 : baseFamily.getOrDefault(e.getKey(), 0);
            if (base + e.getValue() > MAX_FAMILY) out.add(e.getKey() + ": " + (base + e.getValue()) + " family pages, over the " + MAX_FAMILY + " the unlock mask holds");
        }
        for (var e : signatures.entrySet())
            if (e.getValue() > MAX_SIGNATURE_PER_BOOK) out.add(e.getKey() + ": " + e.getValue() + " signature pages, at most " + MAX_SIGNATURE_PER_BOOK);
        return out;
    }

    /** Null when a grimoire that shows {@code visible} pages is within the budget (and, for a basic grimoire of a claimed book, reaches the goal), else why not. */
    public static String visibleProblem(String book, int visible, boolean claimed) {
        if (visible > MAX_VISIBLE) return book + " shows " + visible + " pages, over the " + MAX_VISIBLE + " one grimoire may show";
        if (claimed && visible < GOAL) return book + " shows only " + visible + " pages in the basic grimoire, the goal is " + GOAL;
        return null;
    }
}
