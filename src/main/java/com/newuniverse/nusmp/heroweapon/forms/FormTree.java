package com.newuniverse.nusmp.heroweapon.forms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: "which forms can the weapon learn now?" is asked in three places (right after the gem takes something in, when the player presses Learn, and by the Weapon
 * Book to colour its list) and must give the same answer in all three, on the server and on the client. It is a small piece of arithmetic over four inputs (the
 * forms, the learned set, what the gem has absorbed, the curse series unlocked), so it lives here with no Minecraft types and a JUnit test. The one thing the
 * arithmetic cannot know is which items belong to a tag; the caller supplies that as a {@link TagMatcher} (the game answers from the item registry, a test from a
 * map). Nothing here changes any state: the absorbed counts are never consumed by learning, they only accumulate (design.md section 6).
 */
public final class FormTree {
    private FormTree() {}

    /** Answers "does this item belong to this tag?" (both as {@code namespace:path}). */
    @FunctionalInterface
    public interface TagMatcher {
        boolean inTag(String tagId, String itemId);
    }

    /** A matcher that knows no tags (item requirements only). */
    public static final TagMatcher NO_TAGS = (tag, item) -> false;

    /** What a row of the Weapon Book shows about a form. */
    public enum Status {
        /** The form in use. */
        ACTIVE,
        /** Learned, not in use. */
        LEARNED,
        /** Not learned yet, but the parents are learned and the gem has absorbed enough: it is learnable now. */
        AVAILABLE,
        /** Not learned and something is missing (a parent or an item). */
        LOCKED,
        /** A curse form whose series is not unlocked: nothing the gem eats can open it. */
        SEALED
    }

    /** One line of the form list: the form, how far it is indented, and whether the "Curse Series" heading is drawn above it. */
    public record Row(HeroForm form, int indent, boolean curseHeading) {}

    // ------------------------------------------------------------------------------------------------ requirements
    /** True when the requirement is met by this absorbed item id (an item requirement by equality, a tag requirement through the matcher). */
    public static boolean matches(HeroForm.Requirement r, String itemId, TagMatcher tags) {
        if (itemId == null || itemId.isEmpty()) return false;
        return r.isTag() ? tags.inTag(r.tag(), itemId) : r.item().equals(itemId);
    }

    /** How many of the absorbed items count toward the requirement (not capped). */
    public static int raw(HeroForm.Requirement r, Map<String, Integer> absorbed, TagMatcher tags) {
        long sum = 0;
        for (Map.Entry<String, Integer> e : absorbed.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (!r.isTag()) {
                if (r.item().equals(e.getKey())) return e.getValue();
            } else if (tags.inTag(r.tag(), e.getKey())) sum += e.getValue();
        }
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    /** The progress toward the requirement, 0 to its count. */
    public static int progress(HeroForm.Requirement r, Map<String, Integer> absorbed, TagMatcher tags) {
        return Math.min(r.count(), raw(r, absorbed, tags));
    }

    public static boolean met(HeroForm.Requirement r, Map<String, Integer> absorbed, TagMatcher tags) {
        return raw(r, absorbed, tags) >= r.count();
    }

    public static boolean requirementsMet(HeroForm f, Map<String, Integer> absorbed, TagMatcher tags) {
        for (HeroForm.Requirement r : f.requires()) if (!met(r, absorbed, tags)) return false;
        return true;
    }

    /** Overall progress of a form's requirements, 0 to 1 (1 for a form that asks for nothing). */
    public static double share(HeroForm f, Map<String, Integer> absorbed, TagMatcher tags) {
        if (f.requires().isEmpty()) return 1.0;
        double sum = 0;
        for (HeroForm.Requirement r : f.requires()) sum += progress(r, absorbed, tags) / (double) r.count();
        return sum / f.requires().size();
    }

    // ------------------------------------------------------------------------------------------------ availability
    public static boolean parentsLearned(HeroForm f, Set<String> learned) {
        for (String p : f.parents()) if (!learned.contains(p)) return false;
        return true;
    }

    /** True for a curse form whose sin is not among the unlocked series. */
    public static boolean sealed(HeroForm f, Set<String> series) {
        return f.curse() && (f.sin() == null || !series.contains(f.sin().id()));
    }

    /** True when the form is not learned yet and nothing stands in the way: parents learned, requirements met, series open. */
    public static boolean available(HeroForm f, Set<String> learned, Map<String, Integer> absorbed, TagMatcher tags, Set<String> series) {
        return !learned.contains(f.id()) && parentsLearned(f, learned) && !sealed(f, series) && requirementsMet(f, absorbed, tags);
    }

    public static Status status(HeroForm f, String activeId, Set<String> learned, Map<String, Integer> absorbed, TagMatcher tags, Set<String> series) {
        if (learned.contains(f.id())) return f.id().equals(activeId) ? Status.ACTIVE : Status.LEARNED;
        if (sealed(f, series)) return Status.SEALED;
        return available(f, learned, absorbed, tags, series) ? Status.AVAILABLE : Status.LOCKED;
    }

    /**
     * The ids that would be learned if everything available were learned now, in the order they open: a form learned in this run can be the parent that opens the
     * next one, so the pass repeats until nothing changes. Forms already learned are not listed.
     */
    public static List<String> learnable(List<HeroForm> forms, Set<String> learned, Map<String, Integer> absorbed, TagMatcher tags, Set<String> series) {
        Set<String> have = new LinkedHashSet<>(learned);
        List<String> out = new ArrayList<>();
        boolean changed = true;
        for (int pass = 0; changed && pass <= forms.size(); pass++) {
            changed = false;
            for (HeroForm f : forms) {
                if (!available(f, have, absorbed, tags, series)) continue;
                have.add(f.id());
                out.add(f.id());
                changed = true;
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ display order
    /**
     * The list order of the Weapon Book: the ordinary forms depth-first from the base form (a form sits under its first parent), then the Curse Series grouped by
     * sin. {@code indent} is the tier for an ordinary form and the depth inside the series for a curse form; the first curse row asks for a heading.
     */
    public static List<Row> rows(List<HeroForm> forms) {
        List<HeroForm> ordinary = new ArrayList<>(), curse = new ArrayList<>();
        for (HeroForm f : forms) (f.curse() ? curse : ordinary).add(f);
        Set<String> known = new HashSet<>();
        for (HeroForm f : ordinary) known.add(f.id());
        Map<String, List<HeroForm>> children = new LinkedHashMap<>();
        List<HeroForm> roots = new ArrayList<>();
        for (HeroForm f : ordinary) {
            if (f.parents().isEmpty() || !known.contains(f.parents().get(0))) roots.add(f);
            else children.computeIfAbsent(f.parents().get(0), k -> new ArrayList<>()).add(f);
        }
        List<Row> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (HeroForm root : roots) walk(root, children, seen, out);
        for (HeroForm f : ordinary) if (seen.add(f.id())) out.add(new Row(f, f.tier(), false));      // unreachable forms (a broken parent chain) still show
        if (!curse.isEmpty()) {
            int minTier = Integer.MAX_VALUE;
            for (HeroForm f : curse) minTier = Math.min(minTier, f.tier());
            List<String> sinOrder = new ArrayList<>();
            for (HeroForm f : curse) {
                String s = f.sin() == null ? "" : f.sin().id();
                if (!sinOrder.contains(s)) sinOrder.add(s);
            }
            List<HeroForm> sorted = new ArrayList<>(curse);
            sorted.sort(Comparator.<HeroForm>comparingInt(f -> sinOrder.indexOf(f.sin() == null ? "" : f.sin().id())).thenComparingInt(HeroForm::tier));
            boolean first = true;
            for (HeroForm f : sorted) {
                out.add(new Row(f, Math.max(0, f.tier() - minTier), first));
                first = false;
            }
        }
        return out;
    }

    private static void walk(HeroForm f, Map<String, List<HeroForm>> children, Set<String> seen, List<Row> out) {
        if (!seen.add(f.id())) return;
        out.add(new Row(f, f.tier(), false));
        for (HeroForm c : children.getOrDefault(f.id(), List.of())) walk(c, children, seen, out);
    }
}
