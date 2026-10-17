package com.newuniverse.nusmp.arc.midnight;

import java.util.List;
import java.util.Locale;

/**
 * Why: in the story the Third Eye is not one person. It is the trio at the head of the Eye of the Midnight Sun, Vetto (Despair), Rhya (the Disloyal) and
 * Fana (Hatred), the three strongest, each an elf soul in a human body marked by a third eye. The brief for this builder asks for the Third Eye as a
 * multi-body encounter if the story supports it, and it does, so the three are separate bosses of the roster (Vetto belongs to another builder, Fana and
 * Rhya to this one) that FIGHT AS ONE: when one of them falls, the others of the trio that are still standing rage. This pure class holds who is in the
 * trio and who answers whom, so the rule is tested and the glue ({@link ThirdEyeBond}) only has to ask.
 */
public final class EyeBondRules {
    private EyeBondRules() {}

    /** The ids of the roster characters that are the Third Eye, in the order the story meets them. */
    public static final List<String> THIRD_EYE = List.of("vetto", "fana", "rhya");

    /** How far (blocks) a member must be from the one that fell to answer its fall: an arena's worth, not the whole dimension. */
    public static final double RAGE_RADIUS = 128.0;

    public static boolean isMember(String id) { return id != null && THIRD_EYE.contains(id.toLowerCase(Locale.ROOT)); }

    /** Does {@code other} rage when {@code fallen} falls? Both must be in the trio and they must be two different people. */
    public static boolean ragesOn(String fallen, String other) {
        return isMember(fallen) && isMember(other) && !fallen.equalsIgnoreCase(other);
    }

    /** The members still to be fought once {@code fallen} is down, in story order. */
    public static List<String> remaining(String fallen) {
        return THIRD_EYE.stream().filter(id -> !id.equalsIgnoreCase(fallen)).toList();
    }
}
