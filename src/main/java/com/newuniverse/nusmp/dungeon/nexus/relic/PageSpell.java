package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.function.IntUnaryOperator;

/**
 * The curated list of spells the Gambler's Grimoire Page can cast (docs/living_dungeon.md section 7). The page does not open a real
 * grimoire page: each spell is a direct effect, so it works for a mage of any magic and cannot be blocked by a sealed grimoire or a missing
 * book. Each has a wind-up (a telegraph) before it lands, a reach and a size; damage is the raw value the balance law then scales. Pure.
 */
public enum PageSpell {
    DARK_CLOAKED_DIMENSION_SLASH("Dark Cloaked Dimension Slash", 55f, 2.2, 36.0, 20),
    GRAVITY_CRUSH("Gravity Crush", 42f, 8.0, 24.0, 24),
    SPATIAL_RIFT_BARRAGE("Spatial Rift Barrage", 20f, 3.0, 24.0, 16),
    METEOR_OF_BONES("Meteor of Bones", 62f, 5.5, 30.0, 30),
    JUDGEMENT_PILLAR("Judgement Pillar", 72f, 4.0, 26.0, 20);

    public final String label;
    /** Raw damage before the balance law; for the barrage it is the damage of one bolt. */
    public final float raw;
    /** Radius of the blast, or half-width of the slash, in blocks. */
    public final double size;
    /** How far from the caster it reaches. */
    public final double range;
    /** Ticks between the wind-up showing the place and the spell landing. */
    public final int telegraph;

    PageSpell(String label, float raw, double size, double range, int telegraph) {
        this.label = label;
        this.raw = raw;
        this.size = size;
        this.range = range;
        this.telegraph = telegraph;
    }

    public static PageSpell pick(IntUnaryOperator below) { return values()[WildSurge.pick(below, values().length)]; }
}
