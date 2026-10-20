package com.newuniverse.nusmp.particle.fx;

/**
 * Why: a theme names two to four particles, and the call sites need to say WHICH of them they mean without naming a texture: the {@code MOTE} is the signature particle of the theme
 * (the one the old code drew as an end-rod or a glow), the {@code ACCENT} its second voice (the old enchant or portal glyph), the {@code SPARK} the hard bright one (crit, electric spark),
 * the {@code AMBIENT} the soft one (smoke, poof, cloud). A theme with fewer than four particles wraps around, so every slot always has an answer. Pure.
 */
public enum FxSlot {
    MOTE, ACCENT, SPARK, AMBIENT;

    public int index() {
        return ordinal();
    }
}
