package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.List;

/**
 * Why: the roller decides WHAT a drop is (grade, slot, which vanilla item, how much stronger, which enchantments and engravings, how much magic
 * resistance) and the factory turns that decision into an {@code ItemStack}; the two are separated by this plain record so the decision can be seeded,
 * replayed, counted in the tests and printed by the admin command without ever touching the game, and so the factory has one thing to read instead of
 * a dozen arguments. Ids are strings (the game resolves them at the last moment and skips any that does not exist, so an absent optional mod or an old
 * Tensura costs one enchantment, never a drop). Percentages are whole numbers: {@code powerPercent} is the bonus to attack damage or armour (0 when the
 * slot has no such stat), {@code durabilityPercent} the bonus to the maximum durability, {@code resistPercent} the magic resistance of this one piece.
 * The lists are copied on construction so a spec is immutable.
 */
public record GearSpec(GearGrade grade, GearSlot slot, String item, int powerPercent, int durabilityPercent,
                       List<Ench> enchants, List<Ench> engravings, int resistPercent) {

    /** One enchantment or engraving: a namespaced id and a level (at least 1; the factory caps it at the enchantment's own maximum). */
    public record Ench(String id, int level) {
        public Ench {
            level = Math.max(1, level);
        }
    }

    public GearSpec {
        enchants = enchants == null ? List.of() : List.copyOf(enchants);
        engravings = engravings == null ? List.of() : List.copyOf(engravings);
    }

    /** What the data component of the finished stack holds. */
    public GearInfo info() { return new GearInfo(grade, slot, resistPercent); }
}
