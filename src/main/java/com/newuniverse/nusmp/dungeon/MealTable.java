package com.newuniverse.nusmp.dungeon;

import java.util.List;

/**
 * What each cooked monster meal adds beside its spell buff ({@link MealBuffs}), per docs/living_dungeon.md section 4: a Tensura skill
 * lent for a while ({@link MealSkills}) and a second Black Clover magic its eater's spells are attuned to ({@link MealAttunement}). The
 * seared monster meat stays plain. Pure: skills and magics are named as text and looked up at runtime, so a name the game does not know
 * does nothing instead of breaking the meal; this class also words the tooltip and the message on eating.
 */
public final class MealTable {
    private MealTable() {}

    /**
     * {@code skill}: a Tensura skill id (lent for {@code skillTicks}, its display name {@code skillName}); {@code magic}: the name of a
     * {@code blackclover.MagicType} constant (the eater's spells carry its rider for {@code magicTicks}).
     */
    public record Perk(String meal, String skill, String skillName, int skillTicks, String magic, int magicTicks) {}

    /** Jelly of a magicule-fed slime: it opens the senses to magicules, and its gel slows what it touches. */
    public static final Perk CANDIED_SLIME_JELLY = new Perk("candied_slime_jelly", "tensura:magic_sense", "Magic Sense", 6000, "GEL", 6000);
    /** The Living Armor's shell: a skin that turns blows, and iron that shoves. */
    public static final Perk GRILLED_ARMOR_FLANK = new Perk("grilled_armor_flank", "tensura:physical_attack_resistance", "Physical Attack Resistance", 4800, "IRON", 4800);
    /** The Basilisk's venom, borne without harm and lent on. */
    public static final Perk ROAST_BASILISK_FILLET = new Perk("roast_basilisk_fillet", "tensura:poison_resistance", "Poison Resistance", 3600, "POISON", 3600);
    /** The Direwolf's pack instinct, and a hunger that feeds on every bite. */
    public static final Perk ROAST_DIREWOLF_HAUNCH = new Perk("roast_direwolf_haunch", "tensura:danger_sense", "Danger Sense", 3600, "BLOOD", 3600);

    public static final List<Perk> ALL = List.of(CANDIED_SLIME_JELLY, GRILLED_ARMOR_FLANK, ROAST_BASILISK_FILLET, ROAST_DIREWOLF_HAUNCH);

    /** The perk of the meal with this item id, or null. */
    public static Perk forMeal(String meal) {
        for (Perk p : ALL) if (p.meal().equals(meal)) return p;
        return null;
    }

    // ---------------------------------------------------------------- wording
    /** "5 min" for whole minutes, "45 s" otherwise. */
    public static String duration(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return seconds >= 60 && seconds % 60 == 0 ? seconds / 60 + " min" : seconds + " s";
    }

    /** What a rider does, from the name of the {@code MagicType.HitEffect} behind it: the words that follow "your spells also". */
    public static String verb(String hit) {
        return switch (hit) {
            case "BURN" -> "set what they hit alight";
            case "FREEZE" -> "chill what they hit to a crawl";
            case "SHOCK" -> "jolt what they hit weak";
            case "SLOW" -> "slow what they hit";
            case "POISON" -> "poison what they hit";
            case "WITHER" -> "wither what they hit";
            case "BLIND" -> "blind what they hit";
            case "LEVITATE" -> "lift what they hit off its feet";
            case "PUSH", "PULL" -> "knock what they hit back";
            case "WEAKEN" -> "weaken what they hit";
            case "DRAIN" -> "give a little life back to you";
            default -> "carry its touch";
        };
    }

    /** The tooltip line of the attunement. */
    public static String attuneLine(String magicLabel, String hit, int ticks) {
        return "Attuned to " + magicLabel + " (" + duration(ticks) + "): your spells also " + verb(hit);
    }

    /** The tooltip line of the lent skill. */
    public static String skillLine(String skillName, int ticks) {
        return "Lends " + skillName + " (" + duration(ticks) + ")";
    }

    /** The message on eating, short enough for the action bar: either part may be null; "" if both are. */
    public static String eatLine(String magicLabel, String hit, String skillName) {
        String attune = magicLabel == null ? "" : magicLabel + ": your spells also " + verb(hit) + ".";
        String lent = skillName == null ? "" : skillName + " lent.";
        return attune.isEmpty() ? lent : lent.isEmpty() ? attune : attune + " " + lent;
    }
}
