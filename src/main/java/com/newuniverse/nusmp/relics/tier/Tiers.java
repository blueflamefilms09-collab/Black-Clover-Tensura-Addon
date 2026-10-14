package com.newuniverse.nusmp.relics.tier;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.tier.TierRules.Stat;
import net.minecraft.world.item.ItemStack;

/**
 * The read/write side of tier ascension for stacks. Other systems (socketing, visual evolution, tooltips, the engine) ask only these methods.
 * Every read is a component lookup, so it is safe in hot paths; an item that was never raised returns its base grade untouched.
 */
public final class Tiers {
    private Tiers() {}

    private static final Grade[] GRADES = Grade.values();

    /** The stored tier (0..6) of the stack; 0 if it was never raised or is not a relic. */
    public static int tierOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Integer t = stack.get(TierComponents.TIER.get());
        return t == null ? 0 : TierRules.clampTier(t);
    }

    /** The def's own grade, or null if the stack is not a relic. */
    public static Grade baseGrade(ItemStack stack) {
        RelicDef d = Relics.defOf(stack);
        return d == null ? null : d.grade();
    }

    /** max(base grade, tier): what the item counts as now. Null if the stack is not a relic. */
    public static Grade effectiveGrade(ItemStack stack) { return effectiveGrade(stack, Relics.defOf(stack)); }

    /** The same when the def is already known (no registry lookup). Null def gives null. */
    public static Grade effectiveGrade(ItemStack stack, RelicDef def) {
        if (def == null) return null;
        int t = tierOf(stack);
        return t <= def.grade().ordinal() ? def.grade() : GRADES[t];
    }

    /** How many grades the item stands above its base (0 for an unraised or non-relic stack). */
    public static int steps(ItemStack stack, RelicDef def) {
        return def == null ? 0 : TierRules.steps(def.grade().ordinal(), tierOf(stack));
    }

    /** The multiplier of a stat for this stack (1.0 for an unraised or non-relic stack). */
    public static double multiplier(ItemStack stack, Stat stat) { return multiplier(stack, Relics.defOf(stack), stat); }

    public static double multiplier(ItemStack stack, RelicDef def, Stat stat) { return TierRules.multiplier(stat, steps(stack, def)); }

    /** True if the stack has been raised above its base grade. */
    public static boolean isRaised(ItemStack stack) { return steps(stack, Relics.defOf(stack)) > 0; }

    /**
     * Writes the tier onto this very stack object (serial, attunement and every other component stay). A tier at or under the base grade removes the
     * component. Returns false if the stack is not a relic.
     */
    public static boolean setTier(ItemStack stack, int tier) {
        RelicDef d = Relics.defOf(stack);
        if (d == null || stack.isEmpty()) return false;
        int t = TierRules.clampTier(tier);
        if (t <= d.grade().ordinal()) stack.remove(TierComponents.TIER.get());
        else stack.set(TierComponents.TIER.get(), t);
        return true;
    }
}
