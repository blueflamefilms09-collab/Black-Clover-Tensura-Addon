package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.List;

/**
 * Why: the one plain-data shape of a mechanic, shared by the parser, the picker, the planner, the signature guard and the engine, with no Minecraft type in it so all of that is
 * JUnit-testable. A mechanic = who it is (id, base design, category, source in the series or rulebook it was inspired by), where it may appear (scope, tier range, road, theme
 * affinity), what it costs the floor's threat budget and how likely it is, WHEN it fires (a trigger), WHAT it does (effects from the primitive library), and how a player beats it
 * (a counter). Name, description and flavour are the English fallbacks of the lang keys {@code mechanic.nusmp.<id>.name / .desc / .flavor}. Immutable; built by
 * {@link MechanicParser}.
 */
public record MechanicDef(String id, String name, String desc, String flavor, MechCategory category, String source, String base, Scope scope, int tierMin, int tierMax,
                          Road road, List<String> themes, int cost, int weight, Trigger trigger, List<Effect> effects, Counter counter, String reward, String sig) {

    /** One primitive call. {@code delay}: ticks after the trigger; {@code chance}: 0..1; {@code times}/{@code every}: repeat count and gap in ticks. */
    public record Effect(String op, Args args, int delay, double chance, int times, int every) {}

    /** What sets the mechanic off. */
    public record Trigger(String kind, Args args) {}

    /** How a player beats it ({@code none} = they cannot, the mechanic is survived or left behind). */
    public record Counter(String kind, Args args) {
        public boolean none() { return "none".equals(kind); }
    }

    /** Does the mechanic fit the floor's tier? */
    public boolean fitsTier(int tier) { return tier >= tierMin && tier <= tierMax; }

    /** Does the mechanic prefer this theme (or have no preference)? */
    public boolean likesTheme(String theme) { return themes.isEmpty() || themes.contains(theme); }

    /** The lang keys, one place so the generator, the command and the tests agree. */
    public String nameKey() { return "mechanic.nusmp." + id + ".name"; }

    public String descKey() { return "mechanic.nusmp." + id + ".desc"; }

    public String flavorKey() { return "mechanic.nusmp." + id + ".flavor"; }
}
