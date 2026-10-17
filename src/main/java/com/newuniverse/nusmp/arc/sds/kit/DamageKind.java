package com.newuniverse.nusmp.arc.sds.kit;

import java.util.Locale;

/**
 * Why: three of the four Sins answer a player's blow by WHAT it is, not only by how big it is (Gowther reads the kind of damage you keep dealing
 * and shrugs it off, King's Guardian parries blades and arrows but not spells and is weak to ice, Diane's metal skin laughs at blades and melts in
 * fire). They all need the same question answered the same way: "is this a blade, an arrow, a spell, fire, ice, a blast, lightning or something else?".
 * This pure enum is that one answer. The game glue only reads a handful of facts off the damage source (its vanilla tags, its type id, whether it is a spell
 * by the mod's own definition, whether a living thing struck it by hand) and passes them in as {@link Facts}, so the rule can be unit tested without a world and
 * a damage type from another mod is still sorted by the words in its id. The order of the checks is the design: an element wins over how it travelled (a
 * fire arrow is fire, because that is what a Guardian's moss reacts to), a spell wins over a plain hit, and a blow that is nothing else is {@link #OTHER}.
 */
public enum DamageKind {
    MELEE("blades"), PROJECTILE("arrows"), SPELL("spells"), FIRE("fire"), ICE("ice"), EXPLOSION("blasts"), LIGHTNING("lightning"), OTHER("other blows");

    private final String label;

    DamageKind(String label) { this.label = label; }

    /** What a player is told, in the plural: "blades", "spells" ... */
    public String label() { return label; }

    /** Blades, arrows and blasts: what a body of moss or metal can turn aside. */
    public boolean physical() { return this == MELEE || this == PROJECTILE || this == EXPLOSION; }

    /**
     * The facts the glue reads off a damage source.
     *
     * @param freezingTag    the vanilla tag {@code is_freezing}
     * @param fireTag        the vanilla tag {@code is_fire}
     * @param explosionTag   the vanilla tag {@code is_explosion}
     * @param lightningTag   the vanilla tag {@code is_lightning}
     * @param projectileTag  the vanilla tag {@code is_projectile}
     * @param spell          the mod's definition of a spell (vanilla magic, or a Tensura damage type that is not physical)
     * @param melee          a living thing struck it directly (no projectile in between)
     * @param typePath       the path of the damage type's id ("player_attack", "tensura:ice_spear" gives "ice_spear"), may be empty
     */
    public record Facts(boolean freezingTag, boolean fireTag, boolean explosionTag, boolean lightningTag, boolean projectileTag, boolean spell,
                        boolean melee, String typePath) {
        public Facts {
            typePath = typePath == null ? "" : typePath.toLowerCase(Locale.ROOT);
        }
    }

    /** True when a word of the id (split on anything that is not a letter) starts with one of {@code words}: "icicle_lance" has "icicle", "slice" does not have "ice". */
    private static boolean any(String path, String... words) {
        for (String token : path.split("[^a-z]+")) {
            if (token.isEmpty()) continue;
            for (String w : words) if (token.startsWith(w)) return true;
        }
        return false;
    }

    /** The kind of a blow. Elements first (tag, then the words of the type id), then how it travelled, then spell, then melee, then other. */
    public static DamageKind of(Facts f) {
        String p = f.typePath();
        if (f.freezingTag() || any(p, "ice", "icicle", "frost", "freez", "cold", "blizzard", "snow")) return ICE;
        if (f.fireTag() || any(p, "fire", "flame", "heat", "burn", "lava", "hellfire", "magma", "blaze")) return FIRE;
        if (f.explosionTag() || any(p, "explo", "blast", "bomb")) return EXPLOSION;
        if (f.lightningTag() || any(p, "lightning", "thunder", "electr")) return LIGHTNING;
        if (f.projectileTag() || any(p, "arrow", "projectile", "thrown", "bullet", "trident")) return PROJECTILE;
        if (f.spell()) return SPELL;
        if (f.melee()) return MELEE;
        return OTHER;
    }
}
