package com.newuniverse.nusmp.elite.records.rule;

import java.util.Locale;

/**
 * Why: the owner's inventory names exactly twenty ways a record can unlock; this enum is that list, one entry per way, with the data id used in the JSON files ({@code "type": "kill_entity"}) and the
 * outside SOURCE the number comes from. A source other than the empty string is something that may not exist on a server (FTB Chunks is another mod; Tensura's storage can fail to answer): the evaluator
 * answers UNAVAILABLE for such a type while the source is missing, instead of throwing or quietly reporting zero. Pure.
 */
public enum CondType {
    BIOME_DISCOVERY(""), CALAMITY_DAMAGE(""), CALAMITY_KILL(""), DEATH_COUNT(""), DISTANCE_TRAVELED(""),
    EP_REACHED("tensura"), FORGE_QUALITY(""), FORGE_COUNT(""), FTB_CHUNKS_CLAIMED("ftbchunks"),
    DEMON_LORD_STATUS("tensura"), HIGHER_BEING_STATUS("tensura"), TRUE_HERO_STATUS("tensura"),
    KILL_ENTITY(""), KILL_COUNT(""), SINGLE_HIT_DAMAGE(""), SKILL_LEARNED("skills"),
    STRUCTURE_DISCOVERY(""), WALPURGIS_ATTENDED(""), WAR_KILLS(""), WAR_WINS("");

    private final String source;

    CondType(String source) { this.source = source; }

    /** The outside source this type needs ("tensura", "ftbchunks", "skills") or "" when the numbers are the mod's own. */
    public String source() { return source; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The type with data id {@code s} (any case, '-' or ' ' accepted for '_'), or null. */
    public static CondType parse(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (CondType c : values()) if (c.name().equals(t)) return c;
        return null;
    }
}
