package com.newuniverse.nusmp.labyrinth.squad;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the squads, {@code nusmp-labyrinth-squad-server.toml}, section {@code [squads]}. The switch {@code enabled} (default ON) is what keeps the
 * old behaviour available: with it false the Dungeon Master spawns exactly the groups its tables and budget give, with no squad, no roles and no extra goals, which is
 * the path that existed before squads. The other keys are the numbers the brief calls guesses (the morale reaction, how many survivors break the nerve, how fast healers
 * heal) and are kept here so a server can retune them. Every value is a plain {@code define} / {@code defineInRange} and every reader goes through {@link #get}, which
 * answers the default while the spec is not loaded yet (a unit test, an early server tick).
 */
public final class SquadConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ANNOUNCE;
    public static final ModConfigSpec.ConfigValue<String> MORALE_BREAK;
    public static final ModConfigSpec.DoubleValue MORALE_FRACTION, HEAL_FRACTION;
    public static final ModConfigSpec.IntValue TICK_EVERY, HEAL_COOLDOWN, BUFF_COOLDOWN, MAX_SQUADS, ENRAGE_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("squads");
        ENABLED = b.comment("Coordinated 5 to 8 member squads (tanks, damage, flankers, healers) replace the random horde of an encounter. false = the Dungeon Master spawns the groups its tables give, with no squad at all (the old behaviour).")
                .define("enabled", true);
        MORALE_BREAK = b.comment("What the survivors do when the leader falls, the squad is down to its last member or loses too many: flee, enrage, or mixed (front-line and melee enrage, shooters and healers flee).")
                .define("moraleBreak", "mixed");
        MORALE_FRACTION = b.comment("A squad whose survivors are fewer than this fraction of its size loses its nerve.")
                .defineInRange("moraleFraction", 0.34, 0.0, 1.0);
        TICK_EVERY = b.comment("Ticks between two thinking passes of one squad (target, ward, leader, morale). The goals of its members read the result each tick, so this is the whole cost of a squad.")
                .defineInRange("thinkEveryTicks", 10, 2, 100);
        HEAL_COOLDOWN = b.comment("Ticks between two heals of one healer.")
                .defineInRange("healCooldownTicks", 80, 20, 1200);
        BUFF_COOLDOWN = b.comment("Ticks between two buffs of one healer.")
                .defineInRange("buffCooldownTicks", 200, 40, 2400);
        HEAL_FRACTION = b.comment("The share of an ally's maximum health one heal restores (before the 50 percent cap per cast and the decay of repeated heals in BalanceLaw). Bosses are never healed.")
                .defineInRange("healFraction", 0.15, 0.01, 0.5);
        ENRAGE_TICKS = b.comment("How long an enraged survivor keeps its bonus.")
                .defineInRange("enrageTicks", 600, 40, 12000);
        MAX_SQUADS = b.comment("The most squads alive at once (the oldest idle ones are forgotten beyond it).")
                .defineInRange("maxSquads", 96, 4, 1000);
        ANNOUNCE = b.comment("The System panel tells the party 'Squad: N: roles' when a squad appears.")
                .define("announce", true);
        b.pop();
        SPEC = b.build();
    }

    private SquadConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }
}
