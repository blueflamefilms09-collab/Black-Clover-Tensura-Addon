package com.newuniverse.nusmp.relics;

import java.util.List;

/**
 * Why: a relic that belongs in a Curios slot (necklace, back, belt, hands, charm) had no way onto the body except the Curios screen: right-clicking it in the hand did not put it on (it fired
 * the relic's first ability, or showed the attunement hint). Curios may or may not equip an item that is only listed in a slot tag when it is right-clicked (that is a feature of its item
 * interface, which this mod cannot compile against), so the mod does it itself ({@link RelicCurioEquip}), and the choice of WHEN and WHICH slot lives here, pure, so it can be tested:
 * right-click puts the relic on when it is a worn-in-Curios relic, the player is not sneaking (sneak-use stays the attunement ritual and the second ability) and a slot of its type has a free
 * place; otherwise nothing changes and the old behaviour (first ability, or the hint) runs as before.
 */
public final class CurioEquipRules {
    private CurioEquipRules() {}

    /** One slot type of the player's Curios inventory: its id and, per place, whether the place is free. */
    public record Slot(String id, List<Boolean> free) {}

    /** The place chosen: the slot type and the index inside it. */
    public record Pick(String slotId, int index) {}

    /** Should a right-click with this item try to put it on? */
    public static boolean tryOnUse(boolean isCurioRelic, boolean sneaking, boolean stackEmpty) { return isCurioRelic && !sneaking && !stackEmpty; }

    /** The first free place in the slot type {@code want}, or null when the type is absent from the player's inventory or all its places are taken. */
    public static Pick pick(String want, List<Slot> slots) {
        if (want == null) return null;
        for (Slot s : slots) {
            if (!want.equals(s.id())) continue;
            for (int i = 0; i < s.free().size(); i++) if (Boolean.TRUE.equals(s.free().get(i))) return new Pick(s.id(), i);
        }
        return null;
    }

    /** Why nothing was equipped, in words for the log and the action bar. */
    public enum Result {
        EQUIPPED("put on"),
        NOT_A_CURIO_RELIC("not a relic worn in a Curios slot"),
        SNEAKING("sneaking: the ritual and the second ability keep the right-click"),
        NO_CURIOS("Curios is not installed or its inventory could not be read"),
        NO_SLOT_TYPE("the player has no slot of this type (the slot is not given to the player)"),
        SLOT_FULL("every place of this slot is taken");

        public final String text;

        Result(String text) { this.text = text; }
    }
}
