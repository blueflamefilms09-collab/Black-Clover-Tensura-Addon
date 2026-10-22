package com.newuniverse.nusmp.charset;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Why: the character spell sets (docs/magic/character_spells.md) are one self-contained feature, so they have one entry point: the single line the mod constructor calls. It registers the server
 * config {@code nusmp-character-spells-server.toml} ({@code magic.characterSpellSets}) and the CI self-test {@code magic.character_spells}. Safe to call twice.
 */
public final class CharacterSetBoot {
    private CharacterSetBoot() {}

    private static boolean done;

    public static synchronized void init(ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, CharacterSetConfig.SPEC, "nusmp-character-spells-server.toml");
        CharacterSetSelfTests.register();
        CharacterRestSelfTests.register();      // the squad captains / Spade / Heart / Diamond / Seabed sets (magic.character_spells_rest)
    }
}
