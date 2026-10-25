package com.newuniverse.nusmp.relics;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.12.0: the head and feet relics live in the Curios slots {@code relic_head} / {@code relic_feet} (the armour slot never drew them on the body). The WHEN and WHICH of that move are
 * decided here, pure, so they can be tested without a game:
 * <ul>
 *   <li>a right-click with a HEAD or FEET relic goes to its Curios slot, not while sneaking (sneak-use stays the attunement ritual), and only while the server setting is on;</li>
 *   <li>a head relic found in the HEAD armour slot (or a feet relic in the FEET armour slot) is moved to its Curios slot, looked at once a second per player with a back-off of
 *       {@link #RETRY_TICKS} after a try that found no place;</li>
 *   <li>a head or feet relic worn in a Curios slot keeps the armour points and toughness its armour form would have given.</li>
 * </ul>
 * Nothing here knows a Minecraft type: slots are named by their {@link RelicSlot} / {@code EquipmentSlot} names.
 */
public final class HeadFeetMoveRules {
    private HeadFeetMoveRules() {}

    /** After a look that found no free place (or no Curios), the next look at the same player waits this many ticks (10 s). */
    public static final int RETRY_TICKS = 200;

    /** The Curios slot id of a HEAD or FEET relic slot name ({@code relic_head}, {@code relic_feet}), or null for any other slot. */
    public static String curiosSlot(String relicSlotName) {
        RelicSlot s = RelicSlot.byName(relicSlotName);
        if (s == RelicSlot.HEAD) return "relic_head";
        if (s == RelicSlot.FEET) return "relic_feet";
        return null;
    }

    /** Should a right-click with a relic of this slot be tried on its Curios slot? (setting on, a head or feet relic, not sneaking, something in the hand). */
    public static boolean redirectOnUse(boolean enabled, String relicSlotName, boolean sneaking, boolean stackEmpty) {
        return enabled && !sneaking && !stackEmpty && curiosSlot(relicSlotName) != null;
    }

    /** Is this relic (by its relic slot name) the kind that belongs in this armour slot (by its equipment slot name)? A head relic in HEAD, a feet relic in FEET; never a chest relic. */
    public static boolean belongsTo(String relicSlotName, String equipmentSlotName) {
        RelicSlot s = RelicSlot.byName(relicSlotName);
        return (s == RelicSlot.HEAD && "HEAD".equalsIgnoreCase(equipmentSlotName)) || (s == RelicSlot.FEET && "FEET".equalsIgnoreCase(equipmentSlotName));
    }

    /** Is a look at the armour slots due now? Off with the setting, for a spectator, or while the back-off of the last empty-handed try has not run out. */
    public static boolean due(boolean enabled, boolean spectator, long now, long notBefore) { return enabled && !spectator && now >= notBefore; }

    /** The game time before which the next look must not happen after a try that found no place. */
    public static long retryAt(long now) { return now + RETRY_TICKS; }

    /** A hard cap on the changes one player can get from this (another mod could add places to the slots; the normal case is two relics, four changes). */
    public static final int MAX_SHARES = 8;

    /** One attribute change a Curios-worn head or feet relic makes: the key (the end of its modifier id), whether it is toughness (else armour points) and the amount. */
    public record Share(String key, boolean toughness, double amount) {}

    /**
     * The armour points and toughness that the head or feet relics worn in Curios slots add: what their armour form gives (def armour points, def toughness, only when above zero),
     * one entry per kind per relic. At most one head and one feet relic can be in the slots, so this is at most four entries.
     * @param ids      relic ids
     * @param slots    their {@link RelicSlot} names
     * @param defence  their armour points ({@link RelicDef#armorDefence})
     * @param toughness their toughness ({@link RelicDef#armorToughness})
     */
    public static List<Share> shares(List<String> ids, List<String> slots, List<Integer> defence, List<Double> toughness) {
        List<Share> out = new ArrayList<>(4);
        for (int i = 0; i < ids.size() && out.size() < MAX_SHARES; i++) {
            if (curiosSlot(slots.get(i)) == null) continue;
            if (defence.get(i) > 0) out.add(new Share(ids.get(i) + "/armor", false, defence.get(i)));
            if (toughness.get(i) > 0.0) out.add(new Share(ids.get(i) + "/toughness", true, toughness.get(i)));
        }
        return out;
    }
}
