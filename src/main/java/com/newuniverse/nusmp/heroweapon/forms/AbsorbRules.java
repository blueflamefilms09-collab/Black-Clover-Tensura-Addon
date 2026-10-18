package com.newuniverse.nusmp.heroweapon.forms;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: what the gem takes in, and how much of it, is a rule worth getting right and testing: a player who offers a stack of sixty-four nether stars to a form that
 * asks for four should not lose sixty, and a gem that swallows anything would make the forms' requirements meaningless. The rules are pure counting over the forms,
 * the learned set and the absorbed counts, so they live here with no Minecraft types (the game supplies the tag question as a {@link FormTree.TagMatcher}) and the
 * game classes only move items. The default is generous but not wasteful: an item is accepted when some form of the weapon asks for it (by id or by tag), and only as
 * many are taken as the forms that are still to be learned can use (the config can switch both rules off).
 */
public final class AbsorbRules {
    private AbsorbRules() {}

    /** True when some form of the weapon asks for this item (by id, or through a tag the item belongs to). */
    public static boolean accepts(String itemId, List<HeroForm> forms, FormTree.TagMatcher tags) {
        for (HeroForm f : forms) for (HeroForm.Requirement r : f.requires()) if (FormTree.matches(r, itemId, tags)) return true;
        return false;
    }

    /**
     * How many more of this item the forms still to be learned could use: for each unlearned form and each of its requirements this item satisfies, what is still
     * missing; the largest of those. 0 when every requirement it counts for is already met (or belongs to a learned form).
     */
    public static int needed(String itemId, List<HeroForm> forms, Set<String> learned, Map<String, Integer> absorbed, FormTree.TagMatcher tags) {
        int most = 0;
        for (HeroForm f : forms) {
            if (learned.contains(f.id())) continue;
            for (HeroForm.Requirement r : f.requires()) {
                if (!FormTree.matches(r, itemId, tags)) continue;
                most = Math.max(most, r.count() - FormTree.raw(r, absorbed, tags));
            }
        }
        return Math.max(0, most);
    }

    /**
     * How many of a stack to take.
     *
     * @param stack        the stack size on offer
     * @param needed       {@link #needed} for the item
     * @param onlyNeeded   the config {@code absorbOnlyWhatIsNeeded}
     * @param whole        the player asked for the whole stack (shift-click in the Weapon Book)
     * @param acceptsAny   the config {@code absorbAnything}: the gem takes anything and cannot tell what is needed
     */
    public static int take(int stack, int needed, boolean onlyNeeded, boolean whole, boolean acceptsAny) {
        if (stack <= 0) return 0;
        if (acceptsAny || whole || !onlyNeeded) return stack;
        return Math.min(stack, Math.max(0, needed));
    }

    /**
     * Allows at most {@code perSecond} operations in any window of 20 game ticks, per player (the config {@code absorbPerSecondLimit}). One instance per player; it is
     * a counter of the current window, so it allocates nothing and needs no clean-up beyond forgetting the instance.
     */
    public static final class Limiter {
        private final int perSecond;
        private long windowStart = Long.MIN_VALUE / 2;
        private int used;

        public Limiter(int perSecond) { this.perSecond = Math.max(1, perSecond); }

        /** True when this operation, at game time {@code now}, is within the limit (and is counted). */
        public boolean tryAcquire(long now) {
            if (now - windowStart >= 20 || now < windowStart) {
                windowStart = now;
                used = 0;
            }
            if (used >= perSecond) return false;
            used++;
            return true;
        }
    }
}
