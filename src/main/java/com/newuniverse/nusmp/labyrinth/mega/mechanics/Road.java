package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.Locale;

/**
 * Why: "road affinity (holy / abyssal / both)" of the brief. A mechanic may belong to one road only (a Rukh blessing is Holy, a soul-eating glyph Abyssal) or be at home on
 * both; the Director never draws a mechanic for the wrong road. Pure.
 */
public enum Road {
    BOTH, HOLY, ABYSSAL;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Is a mechanic with this affinity allowed on the road? A null road (unknown floor) allows everything. */
    public boolean allows(Polarity p) {
        return p == null || this == BOTH || (this == HOLY && p == Polarity.HOLY) || (this == ABYSSAL && p == Polarity.ABYSSAL);
    }

    public static Road fromId(String id) {
        if (id == null) return null;
        for (Road r : values()) if (r.id().equalsIgnoreCase(id)) return r;
        return null;
    }
}
