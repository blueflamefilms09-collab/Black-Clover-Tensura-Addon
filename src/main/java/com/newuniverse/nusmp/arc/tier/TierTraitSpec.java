package com.newuniverse.nusmp.arc.tier;

/**
 * Why: a trait on a row. Most traits stand alone ({@code moveId} is null); the ones that hang on one move of the row (Balance, Mirror, Pull, Karma, Echo, Untouchable: see
 * {@link TierTrait#needsMove()}) name the move whose use starts them or whose target they act on. {@link TierRules} checks that the named move exists.
 */
public record TierTraitSpec(TierTrait trait, String moveId) {
    public TierTraitSpec {
        if (trait == null) throw new IllegalArgumentException("a trait spec needs a trait");
    }

    public static TierTraitSpec of(TierTrait t) { return new TierTraitSpec(t, null); }

    public static TierTraitSpec on(TierTrait t, String moveId) { return new TierTraitSpec(t, moveId); }
}
