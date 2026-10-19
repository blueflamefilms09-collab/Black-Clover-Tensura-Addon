package com.newuniverse.nusmp.fairytail;

/**
 * Why: every Fairy Tail spell is one of a small number of shapes (a breath down a lane, a blow in a cone, a dash, a ring, a rain on a
 * point, a buff, a heal ...). The spell tables name the shape and its numbers; {@link FtCast} owns one routine per shape. Minecraft-free.
 */
public enum FtKind {
    /** A lane in front of the caster: everything in the capsule is hit (a roar, a cannon, a slicer). */
    BREATH,
    /** A close blow: everything in a cone in front of the caster. */
    CONE,
    /** The caster rushes forward and hits everything on the way. */
    DASH,
    /** A burst round the caster. */
    RING,
    /** A burst at the point the caster looks at. */
    TARGET_AREA,
    /** Several pulses on the point the caster looks at (a rain of ice, a thunder palace). */
    RAIN,
    /** Several lanes one after another (a volley of lances). */
    BARRAGE,
    /** A rush to the nearest foe, a chain of blows and a last explosion (Crimson Lotus). */
    COMBO,
    /** Effects on the caster only. */
    BUFF,
    /** Effects on the allies round the caster (and the caster). */
    ENCHANT,
    /** Heals the allies round the caster and cleanses them. */
    HEAL,
    /** Absorption hearts and resistance on the caster. */
    SHIELD,
    /** The element is eaten: restores the caster and charges the next spells (fire, iron). */
    EAT,
    /** A timed form: temporary armour, effects and a damage bonus for every spell. */
    FORM,
    /** Foes round the caster are lifted and then slammed down. */
    LIFT,
    /** The foe under the crosshair is marked and explodes a little later. */
    BOMB,
    /** Strips the good effects from the foes in the cone and hits them. */
    DISPEL,
    /** Traps the foe under the crosshair (slow, weakness), through the shared control limits. */
    LOCK
}
