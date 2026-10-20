package com.newuniverse.nusmp.relics;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 1.104.0: the two switches of the attune lock ({@link RelicPower}) in one plain ModConfigSpec, the SERVER file {@code nusmp-relicpower-server.toml}:
 * {@code relics.attuneLock} (default true) and {@code relics.unattunedPower} (default 0.20, 0.05 to 1.0). Reads fall back to the defaults while the
 * config is not loaded (a client before it joined, a unit test).
 */
public final class RelicPowerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ATTUNE_LOCK;
    public static final ModConfigSpec.DoubleValue UNATTUNED_POWER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("relics");
        ATTUNE_LOCK = b.comment("A relic that needs attunement deals only a fraction of its damage to anyone it is not attuned to (melee, arrows, bolts, thrown weapons). false: full power for everyone, as before 1.104.0.")
                .define("attuneLock", true);
        UNATTUNED_POWER = b.comment("The fraction of its damage an unattuned relic deals (0.20 = 20%).")
                .defineInRange("unattunedPower", RelicPower.DEFAULT_UNATTUNED, RelicPower.MIN_UNATTUNED, RelicPower.MAX_UNATTUNED);
        b.pop();
        SPEC = b.build();
    }

    private RelicPowerConfig() {}

    public static boolean lockOn() { try { return ATTUNE_LOCK.get(); } catch (IllegalStateException | NullPointerException e) { return true; } }

    public static double unattunedPower() { try { return UNATTUNED_POWER.get(); } catch (IllegalStateException | NullPointerException e) { return RelicPower.DEFAULT_UNATTUNED; } }

    /** The settings now. */
    public static RelicPower.Config current() { return new RelicPower.Config(lockOn(), unattunedPower()); }
}
