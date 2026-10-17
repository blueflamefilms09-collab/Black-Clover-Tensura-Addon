package com.newuniverse.nusmp.arc.spade;

import java.util.Locale;

/**
 * Why: both bosses of the Spade Kingdom Raid treat a hit by what KIND of hit it is. Morris tunes his body against the kind that hurts him most (and
 * is open to the others), Lucifero's gravity shreds what flies at him but not what is swung at him. The same question must get the same answer in both
 * fights, and the game side can only read three plain facts off a damage source (is there a living attacker, is it a projectile, what is the registry
 * path of its type) plus whether it is an anti-magic blow. This class takes those facts and decides, so the table is unit tested without a game, and a
 * mod's own damage type that nobody listed still lands somewhere sensible (a living attacker and no projectile means a blow).
 */
public enum DamageKind {
    /** A blow swung or thrown by hand: swords, fists, claws, anything physical that is not a projectile. */
    MELEE,
    /** Arrows, bolts, thrown things and every projectile the game tags as one. */
    RANGED,
    /** Spells and skills: magic, elemental, holy, gravity, spatial ... anything the registry path calls non-physical. */
    MAGIC,
    /** A demon sword, the Anti-Magic Lord's blows or a floating Anti-Magic grimoire: modification and gravity cannot touch it. */
    ANTI_MAGIC,
    /** No living attacker (fire on the ground, a fall, the void, a field left behind): not a blow, so no ledger, no tuning, no shroud. */
    OTHER;

    /** Words in a damage type's registry path that make a hit a spell and not a blow. */
    private static final String[] MAGIC_WORDS = {"magic", "elemental", "holy", "spirit", "darkness", "light", "lightning", "dragon_breath", "sonic", "soul",
            "gravity", "space", "spatial", "dimension", "explosion", "fire", "burn", "frost", "freez", "ice", "wither", "poison", "thunder", "wind", "water", "earth"};

    /**
     * Classifies one hit.
     *
     * @param hasAttacker a living entity is behind the hit
     * @param projectile  the game tags the damage type as a projectile
     * @param typePath    the registry path of the damage type ("player_attack", "indirect_magic"), may be null
     * @param antiMagic   the blow is anti-magic (decided by the caller from the weapon or the Lord)
     */
    public static DamageKind classify(boolean hasAttacker, boolean projectile, String typePath, boolean antiMagic) {
        if (antiMagic) return ANTI_MAGIC;
        if (!hasAttacker) return OTHER;
        if (projectile) return RANGED;
        String p = typePath == null ? "" : typePath.toLowerCase(Locale.ROOT);
        for (String w : MAGIC_WORDS) if (p.contains(w)) return MAGIC;
        return MELEE;
    }

    /** The three kinds a body can be tuned against; anti-magic and the rest are never tuned. */
    public boolean tunable() { return this == MELEE || this == RANGED || this == MAGIC; }

    /** The word for chat and the action bar ("melee", "ranged", "magic"). */
    public String word() { return name().toLowerCase(Locale.ROOT).replace('_', '-'); }
}
