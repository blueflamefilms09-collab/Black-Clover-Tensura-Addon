package com.newuniverse.nusmp.charset;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the character grimoires change what a player can cast, so the switches are SERVER settings (NeoForge syncs a server config to the clients, so the pages the client lists match the server).
 * File: nusmp-character-spells-server.toml (registered by {@link CharacterSetBoot}).
 */
public final class CharacterSetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SETS, BASIC_HIDES;
    public static final ModConfigSpec.IntValue SAINT_TRUST, SAINT_DIVES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        SETS = b.comment("Character spell sets: a grimoire bound to a character with a data file (Yuno, Noelle, Fuegoleon, Asta, Yami ...) shows only the spells that character used in the anime, and each opens by its own",
                "acquisition rule (Spirit of Zephyr only after the Spirit Dive). false = today's behaviour for everybody: every grimoire shows the whole shared element book and pages open by mastery order.")
                .define("characterSpellSets", true);
        BASIC_HIDES = b.comment("The basic grimoire (bound to nobody) hides the signature spells that a character set marks exclusive, so a character's own spells never appear in the basic book.",
                "false = the basic book lists them as before (they stay locked to their owner's grimoire). Only read when characterSpellSets is true.")
                .define("basicBookHidesSignatures", true);
        SAINT_TRUST = b.comment("Saint Stage (near 100% resonance with the bonded spirit): the spirit trust that counts as 'near full resonance'.").defineInRange("saintStageTrust", 95, 50, 100);
        SAINT_DIVES = b.comment("Saint Stage also needs this many Spirit Dives cast with the grimoire.").defineInRange("saintStageDives", 3, 1, 100);
        b.pop();
        SPEC = b.build();
    }

    private CharacterSetConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(SETS); }
    public static boolean basicHidesSignatures() { return get(BASIC_HIDES); }
    public static int saintTrust() { return get(SAINT_TRUST); }
    public static int saintDives() { return get(SAINT_DIVES); }
}
