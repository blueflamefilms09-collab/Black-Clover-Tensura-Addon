package com.newuniverse.nusmp.elite.items.weapon;

import java.util.List;

/**
 * Why: everything the data says about ONE weapon: its right-click technique, its sneak-right-click technique, the bonus strike of a full swing, and (for the Void Edge) the id of the skill another
 * subsystem makes that the sword is paired with and how much stronger the sword is for someone who has it. Pure.
 *
 * @param item            the item id, {@code nusmp:void_edge}
 * @param primary         right-click
 * @param alt             sneak + right-click (never null; a weapon without one has none listed and the loader supplies a copy of the primary)
 * @param swingDamage     extra damage of a hit with the attack bar at 90% or more
 * @param swingEffects    effects that full swing puts on the foe
 * @param swingHeal       health the wielder gets back from a full swing
 * @param swingStrip      beneficial effects the full swing strips
 * @param skill           the id of a paired skill ({@code nusmp:void_edge}), "" for none. The sword only ASKS for it through {@code EliteItemsApi.hasSkill}; it never imports the skill
 * @param skillMultiplier damage multiplier while the wielder has the paired skill (1 = no change)
 */
public record WeaponAbilities(String item, AbilitySpec primary, AbilitySpec alt, float swingDamage, List<AbilitySpec.EffectSpec> swingEffects, float swingHeal, int swingStrip,
                              String skill, double skillMultiplier) {
    public WeaponAbilities {
        swingEffects = swingEffects == null ? List.of() : List.copyOf(swingEffects);
        skill = skill == null ? "" : skill;
        if (skillMultiplier <= 0) skillMultiplier = 1.0;
    }
}
