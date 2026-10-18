package com.newuniverse.nusmp.elite.skills;

import java.util.Collection;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Why: Gaia is the mother of the earth, not a weapon for one side of a war. Who stands in a domain decides what the domain does for them: the owner and the owner's nation are
 * blessed, strangers are only protected from (the block guard keeps them from breaking the ground), and enemies of the owner's nation get none of it and make the domain more
 * expensive to hold. The nations system is built by another worker, so the answer comes through the late-bound {@code EliteHooks.nationOf / atWar} functions, handed in as plain
 * functions (so this class has no Minecraft types and no import of another subsystem, and answers "neutral" for everyone while nations are not built or are switched off).
 */
public final class GaiaWarCheck {
    private GaiaWarCheck() {}

    public enum Relation { SELF, ALLY, NEUTRAL, ENEMY }

    /** Pure core: the relation of {@code other} to the owner, from their nation ids ("" = none) and whether those nations are at war. */
    public static Relation classify(boolean sameEntity, String ownerNation, String otherNation, boolean atWar) {
        if (sameEntity) return Relation.SELF;
        String a = ownerNation == null ? "" : ownerNation, b = otherNation == null ? "" : otherNation;
        if (!a.isEmpty() && a.equals(b)) return Relation.ALLY;
        if (!a.isEmpty() && !b.isEmpty() && atWar) return Relation.ENEMY;
        return Relation.NEUTRAL;
    }

    /** The same through the hook functions (null functions answer neutral). */
    public static Relation relation(UUID owner, UUID other, Function<UUID, String> nationOf, BiPredicate<String, String> atWar) {
        if (owner == null || other == null) return Relation.NEUTRAL;
        if (owner.equals(other)) return Relation.SELF;
        String a = nationOf == null ? "" : nationOf.apply(owner);
        String b = nationOf == null ? "" : nationOf.apply(other);
        boolean war = atWar != null && a != null && b != null && !a.isEmpty() && !b.isEmpty() && atWar.test(a, b);
        return classify(false, a, b, war);
    }

    /** True if any of the relations is an enemy: the domain is held "on a war footing" (costs more). */
    public static boolean warFooting(Collection<Relation> inside) {
        if (inside == null) return false;
        for (Relation r : inside) if (r == Relation.ENEMY) return true;
        return false;
    }

    /** Does the domain bless this visitor (regeneration and resistance)? */
    public static boolean blesses(Relation r) { return r == Relation.SELF || r == Relation.ALLY; }

    /** Is this visitor kept from breaking the ground inside the domain? Everyone but the owner and the owner's nation. */
    public static boolean guardsAgainst(Relation r) { return r != Relation.SELF && r != Relation.ALLY; }

    /** Is this visitor burdened (slowed) by the domain? Only enemies. */
    public static boolean burdens(Relation r) { return r == Relation.ENEMY; }
}
