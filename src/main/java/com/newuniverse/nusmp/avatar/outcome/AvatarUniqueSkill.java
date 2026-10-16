package com.newuniverse.nusmp.avatar.outcome;

import java.util.List;
import java.util.Random;

/**
 * Why: Soul Awakening gives the Avatar "a custom Unique Skill", but registering a new Tensura skill for every awakening is not possible at runtime and
 * would pollute the skill registry. So a Unique Skill of an Avatar is data: a name, an effect id and numbers drawn from this table. The entity turns the
 * attribute part into modifiers and the signature part into a move of its own. Pure (no Minecraft), seeded by the caller, unit tested.
 */
public final class AvatarUniqueSkill {
    private AvatarUniqueSkill() {}

    /** What the skill does passively. The entity maps the kind to an attribute. */
    public enum Passive { ATTACK, HEALTH, SPEED, ARMOR, REGEN }

    /** The move the skill adds, cast against enemies now and then. */
    public enum Signature { SHOCKWAVE, DASH_STRIKE, SOUL_FLAME }

    /** One generated skill. {@code passiveAmount} is a fraction (0.25 = +25%), {@code signaturePower} the base damage in HP before the balance caps. */
    public record Perk(String id, String name, Passive passive, double passiveAmount, Signature signature, double signaturePower, int cooldownTicks) {
        public String describe() {
            return name + ": +" + Math.round(passiveAmount * 100) + "% " + passive.name().toLowerCase() + ", signature " + signature.name().toLowerCase().replace('_', ' ');
        }
    }

    private record Entry(String id, String name, Passive passive, Signature signature) {}

    private static final List<Entry> TABLE = List.of(
            new Entry("edge_of_the_horizon", "Edge of the Horizon", Passive.ATTACK, Signature.DASH_STRIKE),
            new Entry("heart_of_the_labyrinth", "Heart of the Labyrinth", Passive.HEALTH, Signature.SHOCKWAVE),
            new Entry("wind_that_remembers", "Wind That Remembers", Passive.SPEED, Signature.DASH_STRIKE),
            new Entry("wall_of_names", "Wall of Names", Passive.ARMOR, Signature.SHOCKWAVE),
            new Entry("ember_of_the_first_god", "Ember of the First God", Passive.ATTACK, Signature.SOUL_FLAME),
            new Entry("unbroken_resolve", "Unbroken Resolve", Passive.REGEN, Signature.SHOCKWAVE),
            new Entry("star_forged_soul", "Star-Forged Soul", Passive.HEALTH, Signature.SOUL_FLAME),
            new Entry("dancer_of_the_thousand_steps", "Dancer of the Thousand Steps", Passive.SPEED, Signature.DASH_STRIKE),
            new Entry("mirror_of_the_void", "Mirror of the Void", Passive.ARMOR, Signature.SOUL_FLAME),
            new Entry("lifeline_of_the_dungeon", "Lifeline of the Dungeon", Passive.REGEN, Signature.DASH_STRIKE));

    public static int tableSize() { return TABLE.size(); }

    /** Draws a skill. Numbers vary a little around the table's baseline so two awakenings of the same entry are not identical. */
    public static Perk generate(Random rng) {
        Entry e = TABLE.get(rng.nextInt(TABLE.size()));
        double amount = 0.15 + rng.nextInt(16) / 100.0;                  // +15% .. +30%
        double power = 6.0 + rng.nextInt(7);                              // 6 .. 12 HP before the caps
        int cooldown = 160 + rng.nextInt(9) * 20;                         // 8 .. 12 seconds
        return new Perk(e.id(), e.name(), e.passive(), amount, e.signature(), power, cooldown);
    }

    /** Rebuilds a stored perk (from NBT strings); null when the id is not in the table (a perk removed by a later version is simply ignored). */
    public static Perk restore(String id, double amount, double power, int cooldown) {
        for (Entry e : TABLE) {
            if (e.id().equals(id)) return new Perk(e.id(), e.name(), e.passive(), amount, e.signature(), power, Math.max(40, cooldown));
        }
        return null;
    }
}
