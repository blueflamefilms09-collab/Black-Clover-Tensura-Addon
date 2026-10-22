package com.newuniverse.nusmp.charset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a Black Bulls member's grimoire should look richer than the basic one (docs/magic/character_spells_bulls.md, "the flair"). Every successful cast of a grimoire bound to one of these
 * characters adds a small signature layer in his colour: a ground ring for a mid page, a burst where he aims for a zone page, an aura for a signature page. This class is the pure part: the
 * palette (a colour and the names of the effect shapes per character) and the bounded decisions (how many extra packets a page tier gets, how often a player may get one). No game class, so
 * it is unit tested; {@code book.CharacterFlairBulls} turns the names into {@code VfxShape}s and sends them.
 */
public final class BullsFlairPalette {
    private BullsFlairPalette() {}

    /** One character's look: the colour and the shape names (VfxShape constants) of the ring (ground), the burst (aim point) and the aura (around him). */
    public record Look(int color, String ring, String burst, String aura) {}

    private static final Map<String, Look> LOOKS = new LinkedHashMap<>();

    static {
        LOOKS.put("vanessa", new Look(0xFF4060, "THREAD_WEB", "THREAD_STRINGS", "ROUGE_FATE_TAP"));
        LOOKS.put("charmy", new Look(0xFFF4FA, "MANA_CHARGE", "COTTON_SHEEP", "SPIRIT_AURA"));
        LOOKS.put("luck", new Look(0xFFE14A, "LIGHTNINGFX_ZONE", "LIGHTNINGFX_BURST", "FX_LIGHTNING_ARC"));
        LOOKS.put("magna", new Look(0xFF5A1A, "FLAMEFX_ZONE", "FLAMEFX_BURST", "FIRE_PILLAR"));
        LOOKS.put("gauche", new Look(0x8E86FF, "MIRROR_FRAME", "MIRROR_SHATTER", "MIRROR_DOUBLE"));
        LOOKS.put("gordon", new Look(0xB4E040, "POISON_CURTAIN", "POISON_BREATH", "CURSE_FX1"));
        LOOKS.put("zora", new Look(0xB0AAA2, "ASH_FORMATION", "ASH_FX3", "ASH_FX2"));
        LOOKS.put("grey", new Look(0x7AF0D8, "ALCHEMY_CIRCLE", "TRANSMUTATION_FX1", "SPIRIT_AURA"));
        LOOKS.put("henry", new Look(0xFF9A3C, "RECOMBINATION_FX1", "RECOMBINE_CONSTRUCT", "SPIRIT_AURA"));
        LOOKS.put("finral", new Look(0xB088FF, "SPACE_RINGGATE", "SPACE_CRACK", "SPACE_PORTAL"));
        LOOKS.put("nacht", new Look(0x6A5AA8, "SHADOW_POOL", "SHADOW_UNITE", "DARKFX_ZONE"));
        LOOKS.put("secre", new Look(0x8AE8FF, "SEAL_ORBIT", "SEAL_BASIC", "SEAL_BRANCH"));
    }

    /** The look of a character, or null for anyone else (nothing is added then). */
    public static Look of(String canon) { return canon == null ? null : LOOKS.get(canon); }

    public static List<String> canons() { return List.copyOf(LOOKS.keySet()); }

    /** At most this many extra packets per cast, whatever the page. */
    public static final int MAX_PACKETS = 3;

    /** A player gets a flair at most once in this many ticks (a spammed page cannot flood the network). */
    public static final int MIN_GAP_TICKS = 6;

    /** How many extra packets a page of this cost tier (percent of mana) gets: none below a mid page, 1 for a mid page, 2 for a zone page, 3 for a signature page. */
    public static int packetsFor(double costPercent) {
        if (costPercent >= 30) return MAX_PACKETS;
        if (costPercent >= 18) return 2;
        if (costPercent >= 10) return 1;
        return 0;
    }

    /** May this player get a flair now? ({@code last} = the game tick of his previous one, 0 = never.) */
    public static boolean gapOk(long now, long last) { return last <= 0 || now - last >= MIN_GAP_TICKS || now < last; }
}
