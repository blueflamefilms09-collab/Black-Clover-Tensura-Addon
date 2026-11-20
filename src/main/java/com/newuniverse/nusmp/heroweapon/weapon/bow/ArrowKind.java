package com.newuniverse.nusmp.heroweapon.weapon.bow;

/**
 * Why: a real arrow of the Legendary Bow carries ONE magic effect, chosen by the bow form in the hand (the first ten kinds) or by the armed technique (the
 * last four, which are the form-independent special arrows: the sniper lane, the burst, the seeker, Pride's command; the net and the mark reuse BIND and MARK).
 * The kind is a pure list so the form table, the arrow entity and the tests all name the same effects. New kinds are only ever APPENDED.
 */
public enum ArrowKind {
    /** Base form: a light (holy) arrow, nothing more than a flash. */
    LIGHT,
    /** Arrow Shower: the first impact splits the arrow into a small bounded volley of child arrows. */
    SPLIT,
    /** Bind Arrow: roots the foe through the shared control lock and the control law. */
    BIND,
    /** Bird Hunting and Eagle Piercer: one more foe pierced and a faster, flatter flight. */
    PIERCE,
    /** Frozen Rain: slows the foe and chills the spot. */
    FROST,
    /** Hunter's Bow: a bleed (poison) and a hunter's mark. */
    BLEED,
    /** Meteor Bow: a fire burst at the impact point (no block is ever changed). */
    FIRE,
    /** Sagittarius: a zodiac star burst at the impact point. */
    STAR,
    /** Scout's Bow: the foe glows through walls and is left frail. */
    MARK,
    /** The Pride forms: the pride curse. */
    PRIDE,
    // ---- armed-only kinds: the techniques, whatever the form
    /** Eagle Piercing Shot: a straight, fast arrow through up to six foes. */
    SNIPER,
    /** Burst Arrow: sticks, and bursts after a fuse. */
    BURST,
    /** Seeker Arrow: bends towards the foe nearest the crosshair. */
    SEEK,
    /** Pride's Command: turns the first monster hit against its own side. */
    COMMAND;

    /** True for the kinds only a technique can give (no form owns them). */
    public boolean armedOnly() { return ordinal() >= SNIPER.ordinal(); }

    /** The kind of the arrow loosed with {@code mode} armed from a bow of form kind {@code form}: the technique's own kind, or the form's. */
    public static ArrowKind of(ArmedMode mode, ArrowKind form) {
        return switch (mode) {
            case SNIPER -> SNIPER;
            case NET -> BIND;
            case MARK -> MARK;
            case BURST -> BURST;
            case SEEK -> SEEK;
            case COMMAND -> COMMAND;
            default -> form;                                   // NONE and SECOND: the form's own arrow
        };
    }
}
