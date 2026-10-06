package com.newuniverse.nusmp.core.magic.grimoire;

import java.util.Locale;

/**
 * A spell's archetype, shown as the small badge in the corner of its spell card (textures/skill/icons/mod_*.png): offensive
 * crosshair, defensive shield, buff up-arrow, debuff down-arrow. Pure: worked out from the page id, so every existing and
 * future page gets one without new data (anything not recognised is offensive).
 */
public enum SpellArchetype {
    OFFENSE, DEFENSE, BUFF, DEBUFF;

    private static final String[] DEFENSE_WORDS = {"shield", "wall", "cradle", "reflect", "barrier", "guard", "double", "armor", "dome"};
    private static final String[] BUFF_WORDS = {"acceleration", "boots", "infighting", "spirit_dive", "channeling", "union", "life_exchange",
            "reversal", "reincarnation", "guidepost", "domination", "heal", "boost", "rouge", "fallen_angel"};
    private static final String[] DEBUFF_WORDS = {"prison", "stasis", "grigora", "stolen", "seal", "curse", "devour", "gravity_sphere",
            "demon_king", "hundred_flowers", "red_thread", "whisper", "bind", "slow", "weaken", "blind"};

    public String texture() { return "textures/skill/icons/mod_" + name().toLowerCase(Locale.ROOT) + ".png"; }

    public static SpellArchetype of(String pageId) {
        String id = pageId == null ? "" : pageId.toLowerCase(Locale.ROOT);
        if (matches(id, DEBUFF_WORDS)) return DEBUFF;
        if (matches(id, DEFENSE_WORDS)) return DEFENSE;
        if (matches(id, BUFF_WORDS)) return BUFF;
        return OFFENSE;
    }

    private static boolean matches(String id, String[] words) {
        for (String w : words) if (id.contains(w)) return true;
        return false;
    }

    /** Packs magic ordinal + archetype into the spell card's seed (old seeds below 100 carry no archetype). */
    public static int packSeed(int magicOrdinal, SpellArchetype a) { return packSeed(magicOrdinal, a, false); }
    /** Same, plus whether it is the book's ultimate (signature-tier) spell, which shows the Ultimate icon (0.23). */
    public static int packSeed(int magicOrdinal, SpellArchetype a, boolean ultimate) { return magicOrdinal + 100 * (a.ordinal() + 1) + (ultimate ? 1000 : 0); }
    public static int magicOf(int seed) { return Math.floorMod(seed, 100); }
    public static SpellArchetype archetypeOf(int seed) {
        int a = Math.floorMod(seed, 1000) / 100 - 1;
        return a >= 0 && a < values().length ? values()[a] : null;
    }
    public static boolean isUltimate(int seed) { return seed >= 1000; }

    /**
     * Which of the magic's three icons (tools/gen_skill_icons.py) a spell card shows: the Ultimate icon for signature spells, the
     * Buff / rune icon for defensive, buff and debuff spells, otherwise the Active icon (the book's own skill icon).
     */
    public static String iconPath(String magic, int seed) {
        String m = magic.toLowerCase(Locale.ROOT);
        if (isUltimate(seed)) return "textures/skill/icons/" + m + "_ultimate.png";
        SpellArchetype a = archetypeOf(seed);
        if (a != null && a != OFFENSE) return "textures/skill/icons/" + m + "_buff.png";
        return "textures/skill/grimoire/" + m + ".png";
    }
}
