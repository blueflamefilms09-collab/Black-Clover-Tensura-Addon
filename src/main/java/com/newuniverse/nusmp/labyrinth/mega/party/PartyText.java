package com.newuniverse.nusmp.labyrinth.mega.party;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Why: every sentence the party layer says to a player comes from here, as a translation key with an English fallback ({@code Component.translatableWithFallback}), so a
 * language file can change the wording without touching code and the sentence still reads fine before the language fragment is merged (the keys live under
 * {@code message.nusmp.mega.party.*}). Nothing here decides anything; it builds components and says whether a player is a person who can be spoken to (a self-test's
 * stand-in has nobody to hear a message).
 */
final class PartyText {
    private PartyText() {}

    static final String PREFIX = "message.nusmp.mega.party.";

    static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback(PREFIX + key, english, args);
    }

    /** Is this a person with a connection (not a test's stand-in)? */
    static boolean real(ServerPlayer p) { return p != null && !(p instanceof FakePlayer) && p.connection != null; }
}
