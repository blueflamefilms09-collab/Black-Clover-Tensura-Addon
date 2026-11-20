package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.relics.Rankings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: one place that turns one of the mod's own ranks (beast rank, devil tier, Saiyan rank, Magic Knight rank, loot and aptitude rarity) into the relic card line the owner asked for: the stars,
 * the grade name and the place, in the grade's colour (docs/rank_audit.md, the table is {@code rankings.json "legacy"}). Every display site asks here and falls back to its old text when this
 * answers null (the {@code rankDisplayClassic} setting is on, or the rank has no mapping). Plain common code (no client class), so a GUI and a server-side message can both use it.
 */
public final class RankCards {
    private RankCards() {}

    /** The relic rank of an old rank (null if unmapped). The key is the enum constant's name. */
    public static Rankings.Rank of(String kind, Enum<?> constant) { return constant == null ? null : Rankings.legacy(kind, constant.name()); }

    /** The card as a coloured line for a rank that is a letter or a name and no enum (a Mana Core's "SS"), or null when classic or unmapped. */
    public static MutableComponent line(String kind, String key) {
        if (RankDisplayConfig.classic()) return null;
        Rankings.Rank r = Rankings.legacy(kind, key);
        return r == null ? null : Component.literal(r.card()).withStyle(r.grade().colour());
    }

    /** The card as text ("★★★★★☆☆  God Grade"), or null when the classic display is on or there is no mapping. */
    public static String text(String kind, Enum<?> constant) {
        if (RankDisplayConfig.classic()) return null;
        Rankings.Rank r = of(kind, constant);
        return r == null ? null : r.card();
    }

    /** The card of a described Magic Knight rank ("3rd Class Senior Magic Knight"; the class does not change the grade), or null in classic / when no rank name is in the text. */
    public static String knightText(String described) {
        if (described == null) return null;
        for (com.newuniverse.nusmp.multiverse.KnightRank k : com.newuniverse.nusmp.multiverse.KnightRank.values()) {
            if (described.contains(k.displayName)) return text("knight", k);
        }
        return null;
    }

    /** The card as a coloured line (the grade's colour), or null like {@link #text}. */
    public static MutableComponent line(String kind, Enum<?> constant) {
        if (RankDisplayConfig.classic()) return null;
        Rankings.Rank r = of(kind, constant);
        return r == null ? null : Component.literal(r.card()).withStyle(r.grade().colour());
    }
}
