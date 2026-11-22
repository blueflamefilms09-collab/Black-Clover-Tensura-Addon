package com.newuniverse.nusmp.arc.goku;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why (4.103.0): the owner wants Goku to have answers to mods whose skills attack a fighter's strength itself, the Devil x Slime (High School DxD) mod above all: Divine Dividing halves strength once per
 * level, Albion's poison, Compression Divider, the Sea Dragon's pressure and the like. Goku is a Saiyan who powers up to answer a debuff, so: harmful effects of the listed mods do not take hold on him,
 * attribute changes those mods put on him are purged, their damage is cut, and each answered debuff makes him flare his ki (a shove, never a hurt). This class is the pure part: which namespaces count,
 * the damage factor, the cooldown. No game types; {@link GokuBoss} applies it. Everything is a server setting and the list is open (any mod's namespace can be added).
 */
public final class GokuCounters {
    private GokuCounters() {}

    public static final String DEFAULT_MODS = "dxdtensura";
    /** Ticks between two flares (5 s): the shove is a show of strength, not a weapon. */
    public static final int FLARE_GAP = 100;

    /** The namespaces of a comma separated setting: trimmed, lower case, empty parts dropped, at most 16. */
    public static List<String> parse(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && out.size() < 16 && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** Does an id such as {@code dxdtensura:divided} (or a bare namespace) belong to one of the listed mods? */
    public static boolean listed(List<String> mods, String id) {
        if (id == null || mods.isEmpty()) return false;
        int c = id.indexOf(':');
        String ns = (c < 0 ? id : id.substring(0, c)).toLowerCase(Locale.ROOT);
        return mods.contains(ns);
    }

    /** The factor on damage from a listed mod's damage type: the setting when listed (0.05 to 1), else 1. Bad numbers read as 1. */
    public static float damageFactor(boolean isListed, double setting) {
        if (!isListed || Double.isNaN(setting)) return 1f;
        return (float) Math.max(0.05, Math.min(1.0, setting));
    }

    /** May he flare now? Once per {@link #FLARE_GAP} ticks. */
    public static boolean mayFlare(long now, long lastFlare) {
        return now - lastFlare >= FLARE_GAP;
    }
}
