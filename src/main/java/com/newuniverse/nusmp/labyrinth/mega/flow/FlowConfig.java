package com.newuniverse.nusmp.labyrinth.mega.flow;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Rukh flow of the Mega-Dungeon and the decennial Metal Vessel (1.55.0),
 * {@code world/serverconfig/nusmp-labyrinth-mega-flow-server.toml}, section {@code [flow]}. Its own file (the pattern of {@code MegaConfig} and
 * {@code EcoConfig}) so this builder's numbers never share a spec with another builder's, and a server owner who finds the multiplier too kind or the
 * rejection too harsh changes one number here, or turns the whole feature off with {@code enabled = false}, without touching anything else. Every value is
 * a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading); the two
 * flow texts get a Predicate validator, which NeoForge resolves to the default on a bad value, and {@link FlowRules#parseFlow} reads an unknown text as the
 * default again, so a typo can never switch the feature into something nobody asked for. Every reader goes through {@link #get}, which answers the
 * default while the spec is not loaded yet (a data reload and event registration run before SERVER configs are read). The defaults repeat the numbers of
 * {@link FlowRules} so the tests and the file cannot drift.
 */
public final class FlowConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, NON_LETHAL, AMBIENT, VESSEL_ENABLED, REJECT_CANCELS_DAMAGE, SKILL_HOOK;
    public static final ModConfigSpec.ConfigValue<String> HOLY_FLOW, ABYSSAL_FLOW;
    public static final ModConfigSpec.DoubleValue HOLY_MULTIPLIER, REJECT_PERCENT, REJECT_MIN_HEALTH;
    public static final ModConfigSpec.IntValue REJECT_COOLDOWN_TICKS, CAST_WINDOW_TICKS, AMBIENT_COUNT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("flow");
        ENABLED = b.comment("The Rukh flow of the Mega-Dungeon: matching magic is multiplied, opposing magic is rejected, and the decennial boss drops its Metal Vessel. false = none of it (the dungeon plays as in 1.54.0).")
                .define("enabled", true);
        HOLY_FLOW = b.comment("What the air of the Holy road is thick with: \"light\" (default: Light and holy magic is multiplied, dark and demonic magic is rejected), \"dark\" (the mirror) or \"none\" (no flow). An unknown value reads as \"light\".")
                .define("holyFlow", "light", o -> o instanceof String s && FlowRules.validFlow(s));
        ABYSSAL_FLOW = b.comment("The same for the Abyssal road. The owner's brief defines only the Holy side, so the default is \"none\"; \"dark\" mirrors the rule (dark magic multiplied, light magic rejected). An unknown value reads as \"none\".")
                .define("abyssalFlow", "none", o -> o instanceof String s && FlowRules.validFlow(s));
        HOLY_MULTIPLIER = b.comment("Damage multiplier of a cast that matches the flow (applied to the damage that cast deals within castWindowTicks). Bosses with a damage-per-second budget keep their cap: the extra damage is charged to it.")
                .defineInRange("holyMultiplier", FlowRules.DEFAULT_MULTIPLIER, FlowRules.MIN_MULTIPLIER, FlowRules.MAX_MULTIPLIER);
        REJECT_PERCENT = b.comment("Share of the caster's maximum health taken when the place rejects a cast (8 = 8 percent). 0 with rejectMinHealth 0 = a rejection costs nothing.")
                .defineInRange("rejectPercent", FlowRules.DEFAULT_REJECT_PERCENT, 0.0, 100.0);
        REJECT_MIN_HEALTH = b.comment("The least health one rejection takes, in health points (2 = one heart), whatever the percentage comes to.")
                .defineInRange("rejectMinHealth", FlowRules.DEFAULT_REJECT_MIN_HEALTH, 0.0, 100.0);
        REJECT_COOLDOWN_TICKS = b.comment("Ticks before the same player can be rejected again (20 = once a second): casting five dark spells in a row costs one rejection, not five.")
                .defineInRange("rejectCooldownTicks", FlowRules.DEFAULT_REJECT_COOLDOWN_TICKS, 0, 1200);
        NON_LETHAL = b.comment("true = a rejection never kills: it stops at 1 health point (the place punishes, it does not execute). false = it can kill, through the normal damage path (death message, drops).")
                .define("nonLethal", true);
        REJECT_CANCELS_DAMAGE = b.comment("true = the damage of a rejected cast is cancelled as well (the spell fizzles). false (default) = only the health drain is the price.")
                .define("rejectCancelsDamage", false);
        CAST_WINDOW_TICKS = b.comment("A hit counts as coming from a cast when it lands at most this many ticks after the cast (30 = a second and a half). A spell that hurts for longer is boosted for this long only.")
                .defineInRange("castWindowTicks", FlowRules.DEFAULT_CAST_WINDOW_TICKS, 1, 600);
        AMBIENT = b.comment("Every two seconds a few Rukh of the flow's colour drift around each player standing in a place that has a flow. false = no ambient particles (the boost and the rejection are unaffected).")
                .define("ambient", true);
        AMBIENT_COUNT = b.comment("How many Rukh drift around a player each time (the particle budget of RukhFx still applies). 0 = none.")
                .defineInRange("ambientCount", 3, 0, 12);
        SKILL_HOOK = b.comment("Also read Tensura skills and other ManasCore skills (not grimoires) as casts, by their skill id, when the player activates them. Grimoire casts are always read.")
                .define("skillHook", true);
        VESSEL_ENABLED = b.comment("The Guardian of every Holy sector (the tenth-floor boss) drops one Metal Vessel, once per sector, from the relics that can hold a Djinn and that nobody owns yet. OFF by default since the owner's fourth brief says the end-game output is exactly ONE Unique+ relic per ten floors (the Crucible's); the older brief's guaranteed Metal Vessel would be a second one. true = the 1.55.0 first-brief behaviour.")
                .define("vesselEnabled", false);
        b.pop();
        SPEC = b.build();
    }

    private FlowConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The flow of the Holy road: the config text read strictly, LIGHT when it is unreadable. */
    public static Alignment holyFlow() { return FlowRules.parseFlow(get(HOLY_FLOW), Alignment.LIGHT); }

    /** The flow of the Abyssal road: the config text read strictly, NEUTRAL when it is unreadable. */
    public static Alignment abyssalFlow() { return FlowRules.parseFlow(get(ABYSSAL_FLOW), Alignment.NEUTRAL); }
}
