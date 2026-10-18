package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.LineGate;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.Collection;

/**
 * Why: the owner's rule is "own many lines, EQUIP and use only ONE at a time". The engine already enforces it (the Skill Tree's single active class, read by {@link LineGate#equipped} before any art or
 * passive acts, and changed by the existing {@code SetClassPayload}, whose server handler checks ownership and the class-switch cooldown), so the new menu must not invent a second rule; it must only
 * ask the same question and send the same request. This class is that question for the menu, pure: what the Equip button of a line should say and do. A line the player does not own cannot be equipped
 * (an operator who inspects it sees "not owned"); the line that is equipped says so; any other owned line may be equipped, which sends its class id. {@link #equippedCount} exists so the rule can be proved
 * on the real route files: because every line has its own class id, at most one line is ever equipped, whatever the player owns.
 */
public final class EquipRule {
    private EquipRule() {}

    public enum State { EQUIPPED, CAN_EQUIP, NOT_OWNED, NO_CLASS }

    public static State of(TreeState player, SkillRoute route) {
        if (route == null || !player.hasRoute(route.id())) return State.NOT_OWNED;
        if (LineGate.equipped(player, route)) return State.EQUIPPED;
        return route.classId().isEmpty() ? State.NO_CLASS : State.CAN_EQUIP;
    }

    /** The class id the Equip button sends, or an empty string when it must send nothing. */
    public static String classToSend(State state, SkillRoute route) { return state == State.CAN_EQUIP && route != null ? route.classId() : ""; }

    /** How many of these routes are equipped for this player (the rule says: never more than one). */
    public static int equippedCount(TreeState player, Collection<SkillRoute> routes) {
        int n = 0;
        for (SkillRoute r : routes) if (LineGate.equipped(player, r)) n++;
        return n;
    }
}
