package com.newuniverse.nusmp.manacore;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the rank Mana Cores ({@code serverconfig/nusmp-manacore-server.toml}, 3.51.0). Every value changes gameplay, so all are server settings (rule 9) and each is clamped again when read.
 * {@code enabled = false} restores the game as it was: no core drops, no passive or active works (the items stay in inventories).
 */
public final class ManaCoreConfig {
    private ManaCoreConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, EP_GATE, HIT_PLAYERS;
    private static final ModConfigSpec.DoubleValue DROP_SCALE, COST_SCALE, COOLDOWN_SCALE;
    private static final ModConfigSpec.IntValue TARGET_CAP, DROP_GAP_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("manacore");
        ENABLED = b.comment("Rank Mana Cores (E to SS). false = no core drops and no core power works.").define("enabled", true);
        EP_GATE = b.comment("A core only works for a wielder with enough Existence Points for its rank (E 0, D 300, C 1000, B 5000, A 20000, AA 50000, S 100000, SS 500000). false = any rank works for anyone.").define("requireEp", true);
        HIT_PLAYERS = b.comment("Core actives may hit other players (also needs the server's PvP on, and never a teammate).").define("hitPlayers", true);
        DROP_SCALE = b.comment("Multiplier on every core drop chance. 0 = no core ever drops.").defineInRange("dropChanceScale", 1.0, 0.0, 5.0);
        DROP_GAP_MINUTES = b.comment("Minutes a player waits between two core drops.").defineInRange("dropGapMinutes", 20, 1, 1440);
        COST_SCALE = b.comment("Multiplier on the magicule cost of every active.").defineInRange("costScale", 1.0, 0.1, 4.0);
        COOLDOWN_SCALE = b.comment("Multiplier on every active cooldown (kept between half and double).").defineInRange("cooldownScale", 1.0, 0.5, 2.0);
        TARGET_CAP = b.comment("The most creatures any one active may affect, whatever the table says.").defineInRange("maxTargetsCap", 24, 1, 32);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { return ENABLED.get(); }
    public static boolean epGate() { return EP_GATE.get(); }
    public static boolean hitPlayers() { return HIT_PLAYERS.get(); }
    public static double dropScale() { return Math.max(0.0, Math.min(5.0, DROP_SCALE.get())); }
    public static long dropGapTicks() { return Math.max(1, Math.min(1440, DROP_GAP_MINUTES.get())) * 1200L; }
    public static double costScale() { return Math.max(0.1, Math.min(4.0, COST_SCALE.get())); }
    public static double cooldownScale() { return Math.max(0.5, Math.min(2.0, COOLDOWN_SCALE.get())); }
    public static int targetCap() { return Math.max(1, Math.min(32, TARGET_CAP.get())); }
}
