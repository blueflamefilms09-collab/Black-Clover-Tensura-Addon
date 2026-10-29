package com.newuniverse.nusmp.audio.voice;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: which line is said for which happening is a decision, so it lives in one pure class (no Minecraft class) that the tests drive: the boss FAMILY comes from the boss-bar style id the
 * boss already wears ({@code boss/bars/BarStyleResolver}, so the voice follows the same archetype resolution as the bar: a frozen keep's boss speaks like ice, a lich like the undead, a
 * Zogratis devil like a devil), the NPC line from what the player did, the Djinn line from the Djinn's name. Every method answers a line id that exists in {@link VoiceCatalog}, or null
 * when no line fits (the caller then says nothing; the text of the message is shown either way).
 */
public final class VoicePicker {
    private VoicePicker() {}

    /** boss-bar style id -> voice family. A style that is not listed falls back to {@link #DEFAULT_FAMILY}. */
    private static final Map<String, String> FAMILY = new HashMap<>();
    /** The family of a boss whose style is unknown or has no voice of its own. */
    public static final String DEFAULT_FAMILY = "evil";

    private static void put(String family, String... styles) {
        for (String s : styles) FAMILY.put(s, family);
    }

    static {
        put("holy", "holy", "archon", "magic_knights");
        put("evil", "evil", "midnight_sun", "god_of_order", "lucius", "warden", "spade_kingdom", "seven_sins");
        put("undead", "undead", "lich", "fallen_king", "pocket_tomb", "plague", "blood");
        put("fire", "fire");
        put("ice", "ice");
        put("storm", "storm");
        put("void", "void", "mirror", "clockwork", "underworld", "qliphoth_gate", "charybdis", "zagred", "water", "forest");
        put("dragon", "dragon", "goliath", "danmachi");
        put("devil", "devil", "supreme_devil", "zogratis");
        put("angel", "angel");
    }

    /** The voice family of a boss-bar style id ({@code null} or an unknown id answers the default family). */
    public static String bossFamily(String barStyleId) {
        if (barStyleId == null) return DEFAULT_FAMILY;
        return FAMILY.getOrDefault(barStyleId, DEFAULT_FAMILY);
    }

    /** The boss line for a style and an event ({@code intro}, {@code phase2}, {@code death}), or null for an event that has no line. */
    public static String boss(String barStyleId, String event) {
        String id = VoiceCatalog.bossId(bossFamily(barStyleId), event);
        return VoiceCatalog.has(id) ? id : null;
    }

    /** The boss event of a phase change: moving to phase 2 or later is {@code phase2}; a phase that did not go up is nothing. */
    public static String bossPhaseEvent(int oldPhase, int newPhase) {
        return newPhase > oldPhase && newPhase >= 1 ? "phase2" : null;
    }

    /** The Djinn line for a Djinn name ({@code "Glasya-Labolas"}, {@code "glasya_labolas"}) and an event ({@code summon}, {@code attack}), or null for a name without a voice. */
    public static String djinn(String name, String event) {
        String id = VoiceCatalog.djinnId(name, event);
        return VoiceCatalog.has(id) ? id : null;
    }

    /** The System announcer's line for a suffix ({@code floor_enter}), or null. */
    public static String system(String line) {
        String id = VoiceCatalog.systemId(line);
        return VoiceCatalog.has(id) ? id : null;
    }

    /** What an NPC does with the player. */
    public enum NpcAct { GREET, SELL, BUY, FAREWELL, QUEST_OFFER, QUEST_DONE, QUEST_FAREWELL, GUIDE }

    /** The NPC line for an act; {@code merchant} selects the merchant's set for GREET / FAREWELL. */
    public static String npc(NpcAct act, boolean merchant) {
        String line = switch (act) {
            case GREET -> merchant ? "merchant_greet" : "guide_greet";
            case SELL -> "merchant_sell";
            case BUY -> "merchant_buy";
            case FAREWELL -> merchant ? "merchant_farewell" : "quest_farewell";
            case QUEST_OFFER -> "quest_offer";
            case QUEST_DONE -> "quest_done";
            case QUEST_FAREWELL -> "quest_farewell";
            case GUIDE -> "guide_greet";
        };
        String id = VoiceCatalog.npcId(line);
        return VoiceCatalog.has(id) ? id : null;
    }

    /** Whether a Djinn's first attack of a cast should speak: only the first attack of a cast, never a repeat inside the same cast ({@code castId} is any number that changes per cast). */
    public static boolean firstAttackOfCast(long lastSpokenCast, long castId) {
        return castId != lastSpokenCast;
    }

    /** Hit points at or under this fraction (and above zero) are "low health" for the System warning. */
    public static final float LOW_HEALTH_FRACTION = 0.25f;

    public static boolean lowHealth(float health, float maxHealth) {
        return maxHealth > 0f && health > 0f && health / maxHealth <= LOW_HEALTH_FRACTION;
    }
}
