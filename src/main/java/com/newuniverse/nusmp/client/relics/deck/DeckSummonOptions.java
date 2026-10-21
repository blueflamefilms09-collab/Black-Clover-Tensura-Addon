package com.newuniverse.nusmp.client.relics.deck;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client option of the Deck of Many Things card summon: config/nusmp-deck-summon-client.toml, key {@code deck.summonEffect} (default true). Off = no summon animation is drawn (the chat text
 * of the drawn card and the card's effects are unchanged, they are decided by the server). Registered once from the client section of the mod constructor.
 */
public final class DeckSummonOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SUMMON_EFFECT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("deck");
        SUMMON_EFFECT = b.comment("Play the short card summon (the card slides out of the duel disk, is slapped on a card zone, flips, a column of light rises, it dissolves into sparks) when a Deck of Many Things card is drawn near you.")
                .define("summonEffect", true);
        b.pop();
        SPEC = b.build();
    }

    private DeckSummonOptions() {}

    /** True when the summon is drawn ({@code deck.summonEffect}); true when the config is not loaded yet. */
    public static boolean enabled() {
        try {
            return SUMMON_EFFECT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
