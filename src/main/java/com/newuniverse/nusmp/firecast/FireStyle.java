package com.newuniverse.nusmp.firecast;

/**
 * Why: the four Crimson Lion characters who got their own way of casting the Fire grimoire pages (docs/magic/fire_casting.md). The style is decided by the canon book bound to the caster
 * (the {@code Canon} id of the grimoire in the Grimoire Slot); every other caster is {@link #NONE} and keeps the generic casting. Pure: no game classes, so the rule is unit tested.
 */
public enum FireStyle {
    /** The generic casting of every other grimoire (key hold, chant, release). */
    NONE,
    /** Magna Swing: the page needs Magna's Soul Bat in the main hand; the spell is a ball tossed in front and the swing launches it. */
    MAGNA,
    /** Mereoleona Vermillion: the key readies the page, a finisher of an unarmed punch combo releases it from the fist. */
    MEREOLEONA,
    /** Fuegoleon Vermillion: the Salamander spirit link; the spells leave the spirit's mouth. */
    FUEGOLEON,
    /** Leopold Vermillion: hold the key to wind a spiral; damage and size grow with the hold (bounded), release fires it. */
    LEOPOLD;

    /** The style of a canon book id ("magna", "mereoleona", "fuegoleon", "leopold": the ids of grimoire.CanonBook), NONE for anything else (also null and the empty id). */
    public static FireStyle ofCanon(String canonId) {
        if (canonId == null) return NONE;
        return switch (canonId.toLowerCase(java.util.Locale.ROOT)) {
            case "magna" -> MAGNA;
            case "mereoleona" -> MEREOLEONA;
            case "fuegoleon" -> FUEGOLEON;
            case "leopold" -> LEOPOLD;
            default -> NONE;
        };
    }
}
