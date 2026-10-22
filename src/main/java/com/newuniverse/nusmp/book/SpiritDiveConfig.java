package com.newuniverse.nusmp.book;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Spirit Dive remake: the SERVER switches (they decide what the server tells the clients; file nusmp-spiritdive-server.toml, registered by {@link SpiritDiveBoot}).
 * <ul>
 *   <li>{@code spiritDive.characterTier}: the character tier of the dive (Yuno, Fuegoleon, Noelle). false = every dive is sent as the standard tier.</li>
 *   <li>{@code spiritDive.fuegoleonArm}: Fuegoleon's flaming right arm. false = the server never turns the arm on.</li>
 * </ul>
 * What a client DRAWS is its own choice (client.aura.SpiritDiveLook, nusmp-spiritdive-client.toml): the classic look, the arm view, the character effects.
 */
public final class SpiritDiveConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CHARACTER_TIER, FUEGOLEON_ARM;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spiritDive");
        CHARACTER_TIER = b.comment("The character tier of the Spirit Dive: the mage whose bound grimoire is Yuno's (Wind), Fuegoleon's (Fire) or Noelle's (Water) dives with that character's own look.",
                "false = every dive is the standard tier. Only changes what the clients are told to draw.").define("characterTier", true);
        FUEGOLEON_ARM = b.comment("Fuegoleon's flaming right arm while the Salamander is his (the contract, the spirit out, or the Fire dive).",
                "false = the arm is never switched on.").define("fuegoleonArm", true);
        b.pop();
        SPEC = b.build();
    }

    private SpiritDiveConfig() {}

    private static boolean get(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean characterTier() { return get(CHARACTER_TIER); }

    public static boolean fuegoleonArm() { return get(FUEGOLEON_ARM); }
}
