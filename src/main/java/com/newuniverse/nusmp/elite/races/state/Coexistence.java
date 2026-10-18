package com.newuniverse.nusmp.elite.races.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the ki boost of a race and the Kaioken skill are both "multiply the body for a while". Run together unchecked they would multiply into a one-shot machine, and the
 * owner wants them to COEXIST: either can be on alone at full strength, and when both are on the race state's multiplier on the two stats Kaioken also touches (attack and
 * speed) is scaled by the server's overlap factor (config {@code kaiokenOverlapFactor}, default 0.5). Nothing is switched off and nothing is read from the Kaioken skill's
 * internals: the runtime only asks whether the skill is toggled on. Flat bonuses (health, armour, scale) are never reduced. Pure.
 */
public final class Coexistence {
    private Coexistence() {}

    /** The attributes the Kaioken skill and the ki states both multiply. */
    public static boolean isKiAttribute(String key) { return "attack_damage".equals(key) || "movement_speed".equals(key); }

    /** The modifiers of a state as they should be applied now. */
    public static List<StateModifier> effective(TransformSpec spec, boolean kaiokenActive, double overlapFactor) {
        if (!kaiokenActive || !spec.kiScaled()) return spec.modifiers();
        double f = Math.max(0.0, Math.min(1.0, overlapFactor));
        List<StateModifier> out = new ArrayList<>(spec.modifiers().size());
        for (StateModifier m : spec.modifiers()) {
            boolean multiplies = m.op() != StateModifier.Op.ADD;
            out.add(multiplies && isKiAttribute(m.attribute()) ? m.scaled(f) : m);
        }
        return out;
    }
}
