package com.newuniverse.nusmp.avatar.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Why: the race files name their requirements as strings ("has_item:nusmp:genesis_core", "black_clover:devil_contract") and the checker must turn each into a
 * yes or no for one player. The mapping from tag to predicate is a registry map (the owner asked for exactly that); this class is its pure core, working on a
 * small {@link Ctx} instead of a ServerPlayer, so the parsing, the negation and the fail-closed rule for unknown tags are unit tested with a fake context.
 *
 * <p>Tag grammar: {@code key} or {@code key:argument}. The key may itself contain a namespace ({@code black_clover:devil_contract}, {@code danmachi:has_falna},
 * {@code relics:requires_curios_attunement}); the longest registered key wins. A leading {@code !} negates. An unknown tag is false and is reported through
 * the warning callback (fail closed: a typo can never open a race).
 */
public final class ConditionEvaluator {
    private ConditionEvaluator() {}

    /** What the conditions can ask about a player. The real implementation reads a ServerPlayer; tests use a fake. */
    public interface Ctx {
        boolean hasItem(String itemId);
        boolean hasSkill(String skillId);
        boolean hasGrimoire();
        boolean hasMagic(String magicId);
        boolean hasDevilContract();
        boolean hasFalna();
        boolean hasAttunedRelic();
        double ep();
        String dimension();
    }

    public record Result(boolean ok, List<String> failed) {}

    /** The registry: tag key to predicate over (context, argument). Mutable on purpose so an add-on can register its own tag at startup. */
    private static final Map<String, BiPredicate<Ctx, String>> REGISTRY = new HashMap<>();

    static {
        register("has_item", (c, a) -> c.hasItem(a));
        register("has_skill", (c, a) -> c.hasSkill(a));
        register("has_grimoire", (c, a) -> c.hasGrimoire());
        register("has_magic", (c, a) -> c.hasMagic(a));
        register("black_clover:devil_contract", (c, a) -> c.hasDevilContract());
        register("danmachi:has_falna", (c, a) -> c.hasFalna());
        register("relics:requires_curios_attunement", (c, a) -> c.hasAttunedRelic());
        register("min_ep", (c, a) -> c.ep() >= parse(a));
        register("dimension", (c, a) -> c.dimension().equals(a));
    }

    public static void register(String key, BiPredicate<Ctx, String> predicate) { REGISTRY.put(key, predicate); }

    public static boolean known(String key) { return REGISTRY.containsKey(key); }

    private static double parse(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.POSITIVE_INFINITY; }
    }

    public static boolean test(Ctx ctx, String tag, Consumer<String> warn) {
        if (tag == null) return false;
        String t = tag.trim();
        boolean negate = false;
        while (t.startsWith("!")) { negate = !negate; t = t.substring(1).trim(); }
        if (t.isEmpty()) return false;
        String key = t, arg = "";
        BiPredicate<Ctx, String> p = REGISTRY.get(t);
        if (p == null) {
            // longest registered key first: split at the last colon, then the one before it, ...
            int cut = t.lastIndexOf(':');
            while (cut > 0 && p == null) {
                String k = t.substring(0, cut);
                p = REGISTRY.get(k);
                if (p != null) { key = k; arg = t.substring(cut + 1); } else cut = t.lastIndexOf(':', cut - 1);
            }
        }
        if (p == null) {
            if (warn != null) warn.accept("unknown avatar race condition '" + tag + "' (treated as not satisfied)");
            return false;
        }
        boolean r;
        try { r = p.test(ctx, arg); } catch (RuntimeException e) {
            if (warn != null) warn.accept("condition '" + tag + "' threw " + e + " (treated as not satisfied)");
            return false;
        }
        return negate != r;
    }

    public static Result evaluate(Ctx ctx, List<String> conditions, Consumer<String> warn) {
        List<String> failed = new ArrayList<>();
        if (conditions != null) for (String c : conditions) if (!test(ctx, c, warn)) failed.add(c);
        return new Result(failed.isEmpty(), failed);
    }
}
