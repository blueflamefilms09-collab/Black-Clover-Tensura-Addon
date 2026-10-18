package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.List;
import java.util.Map;

/**
 * Why: a form is the unit of progress of a hero weapon (the Weapon Copy of the series: the gem takes in materials and the weapon becomes the Leaf Shield, the
 * Chimera Viper Shield, the Shield of Rage ...). Every system reads forms: the loadout (stats, skills, passives), the Weapon Book (requirements, mastery), the
 * curse (curse forms), the renderer (the look) and the weapon logic (its passives). One immutable record, loaded from {@code data/nusmp/hero/forms/<itemId>.json}
 * by the forms system, keeps them agreeing. The JSON schema and its rules are in {@code docs/heroweapon/design.md} section 6.
 *
 * @param kind            the weapon
 * @param id              the form id, unique within the weapon, lower case ({@code small_shield})
 * @param name            display name
 * @param desc            one or two sentences
 * @param tier            depth in the tree, 0 for the base form; used for sorting and balance only
 * @param parents         ids of forms that must be unlocked first (all of them); empty for a root
 * @param requires        what the gem must have absorbed (all of them), counted over everything absorbed so far
 * @param mastery         mastery points (uses) needed to finish the form and receive its {@code abilityBonus}; 0 for none
 * @param stats           attribute bonuses while this form is the active one (keys: {@link #STAT_KEYS})
 * @param abilityBonus    permanent attribute bonuses given once when the form's mastery is complete (same keys)
 * @param skills          slot word ({@link HeroTrigger#slot}) to global skill id
 * @param passives        ids of passives the weapon's logic implements for this form
 * @param look            {@code base} or {@code curse}: which mesh and glow set the item shows
 * @param curse           true for a Curse Series form (needs the curse unlocked, adds corruption)
 * @param sin             the sin of a curse form, else null
 * @param corruptionPerSecond corruption points per second while a curse form is active (0 for none)
 */
public record HeroForm(HeroKind kind, String id, String name, String desc, int tier, List<String> parents, List<Requirement> requires, int mastery,
                       Map<String, Double> stats, Map<String, Double> abilityBonus, Map<String, String> skills, List<String> passives, String look,
                       boolean curse, Sin sin, int corruptionPerSecond) {

    /** The closed set of keys of {@code stats} and {@code abilityBonus}: item attack damage and speed, and the player attributes the forms system applies. */
    public static final List<String> STAT_KEYS = List.of("attack_damage", "attack_speed", "armor", "armor_toughness", "max_health", "movement_speed",
            "knockback_resistance", "reach", "luck");

    /** The look names an art builder must provide for every weapon that has a form using them. */
    public static final String LOOK_BASE = "base", LOOK_CURSE = "curse";

    /** What the gem must have absorbed: {@code count} of an item id ({@code item}) or of any item in a tag ({@code tag}); exactly one of the two is non-empty. */
    public record Requirement(String item, String tag, int count) {
        public boolean isTag() { return tag != null && !tag.isEmpty(); }
    }

    /** True when the form lists the passive. */
    public boolean has(String passive) { return passives.contains(passive); }

    /** The skill id on a trigger, or null. */
    public String skill(HeroTrigger trigger) { return skills.get(trigger.slot()); }

    /** Bonus value of a key (0 when absent). */
    public double stat(String key) { return stats.getOrDefault(key, 0.0); }

    /** The id of the form every bond starts with. */
    public static final String BASE_ID = "base";

    /** A synthetic base form used when a weapon's data has none: no bonuses, no skills, base look. The forms system replaces it with the real data. */
    public static HeroForm base(HeroKind kind) {
        return new HeroForm(kind, BASE_ID, kind.display(), "", 0, List.of(), List.of(), 0, Map.of(), Map.of(), Map.of(), List.of(), LOOK_BASE, false, null, 0);
    }
}
