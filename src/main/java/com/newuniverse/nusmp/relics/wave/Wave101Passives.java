package com.newuniverse.nusmp.relics.wave;

import java.util.Map;

/**
 * The passive rule of each weapon of wave 101 (the Millennium Rod, Ea and the Requiem Arrow). The rules and their arithmetic are {@link Wave71Rules} (the same rules as the weapons of
 * wave 71); {@link Wave101Events} applies them to the blows of the weapon in the main hand.
 */
final class Wave101Passives {
    private Wave101Passives() {}

    static final Map<String, Wave71Rules.Rule> RULES = Map.ofEntries(
            Map.entry("millennium_rod", Wave71Rules.Rule.of(Wave71Rules.Cond.LOW_HP_FOE, 0.7).status("minecraft:weakness", 6, 1)),
            Map.entry("ea_sword_of_rupture", Wave71Rules.Rule.of(Wave71Rules.Cond.BIG_FOE, 0.6)),
            Map.entry("requiem_stand_arrow", Wave71Rules.Rule.of(Wave71Rules.Cond.LOW_HP_SELF, 0.8).steal(0.05))
    );
}
