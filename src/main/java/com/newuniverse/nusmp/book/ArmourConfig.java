package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;

/** 1.99.0: the three strengths of the armour policy ({@code config/nusmp-common.toml}, section armour), read safely: before the config is loaded every answer is 1. */
public final class ArmourConfig {
    private ArmourConfig() {}

    public static double upkeepScale() { return read(0); }

    public static double bonusScale() { return read(1); }

    public static double diveAbsorbScale() { return read(2); }

    private static double read(int which) {
        try {
            return switch (which) {
                case 0 -> NUConfig.ARMOUR_UPKEEP_SCALE.get();
                case 1 -> NUConfig.ARMOUR_BONUS_SCALE.get();
                default -> NUConfig.DIVE_ABSORB_SCALE.get();
            };
        } catch (RuntimeException | LinkageError e) {
            return 1.0;
        }
    }
}
