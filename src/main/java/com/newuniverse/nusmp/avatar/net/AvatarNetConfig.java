package com.newuniverse.nusmp.avatar.net;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the one number the networking owns (how fast a client may press the dice button) is a balance and anti-spam decision the owner may want to
 * change without a rebuild. Own small spec; the lead registers it ({@code modContainer.registerConfig(ModConfig.Type.SERVER, AvatarNetConfig.SPEC,
 * "nusmp-avatar-net-server.toml")}). Plain define only; the accessor falls back to the default while the config is not loaded (tests, early use).
 */
public final class AvatarNetConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue ROLL_GAP_MS, NAME_GAP_MS, NAME_REACH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("avatar_net");
        ROLL_GAP_MS = b.comment("The least milliseconds between two stat rolls of one player (extra presses are ignored).").defineInRange("rollGapMillis", 500, 0, 10000);
        NAME_GAP_MS = b.comment("The least milliseconds between two naming requests of one player.").defineInRange("nameGapMillis", 1000, 0, 60000);
        NAME_REACH = b.comment("How close (blocks) the creator must be to the Avatar to name it.").defineInRange("nameReach", 12, 2, 64);
        b.pop();
        SPEC = b.build();
    }

    private AvatarNetConfig() {}

    public static int rollGapMillis() { return SPEC.isLoaded() ? ROLL_GAP_MS.get() : 500; }
    public static int nameGapMillis() { return SPEC.isLoaded() ? NAME_GAP_MS.get() : 1000; }
    public static int nameReach() { return SPEC.isLoaded() ? NAME_REACH.get() : 12; }
}
