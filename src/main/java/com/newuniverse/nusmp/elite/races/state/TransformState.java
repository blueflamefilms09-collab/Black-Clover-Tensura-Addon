package com.newuniverse.nusmp.elite.races.state;

import java.util.Locale;

/**
 * Why: the five states a race can be in. BASE is the race as it is; BOOSTED is the ki boost (the Kaioken-like multiplier that coexists with the mod's own Kaioken skill);
 * ASCENDED is the awakened form (anger, a near-death rage or a great EP); GREAT_APE is the moon-born rage form (huge, strong, slow); DIVINE is the top form of the highest tier.
 * The id is what the files, the commands and the save use, never the ordinal; ONLY APPEND new states.
 */
public enum TransformState {
    BASE("base"),
    BOOSTED("boosted"),
    ASCENDED("ascended"),
    GREAT_APE("great_ape"),
    DIVINE("divine");

    public final String id;

    TransformState(String id) { this.id = id; }

    public static TransformState byId(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (TransformState v : values()) if (v.id.equals(t)) return v;
        return null;
    }
}
