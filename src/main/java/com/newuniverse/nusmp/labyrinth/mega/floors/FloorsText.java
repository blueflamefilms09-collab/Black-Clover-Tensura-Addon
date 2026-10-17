package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Why: every sentence this module says to a player or an operator comes from here, in one place, as a translation key with an English fallback
 * ({@code Component.translatableWithFallback}): a language file can change the wording without touching code, and the sentence still reads fine before the
 * lead has merged the language fragment (the keys live under {@code message.nusmp.mega.mfl.*}). Road names are their own keys so a sentence can name "your
 * road" in the player's language. Nothing here decides anything; it only builds components and says whether a player is a person who can be spoken to.
 */
final class FloorsText {
    private FloorsText() {}

    static final String PREFIX = "message.nusmp.mega.mfl.";

    static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback(PREFIX + key, english, args);
    }

    /** "Holy road (Ascension)" or "Abyssal road (Descent)"; "no road" for null. */
    static MutableComponent road(Polarity p) {
        if (p == null) return text("road.none", "no road");
        return p == Polarity.HOLY ? text("road.holy", "Holy road (Ascension)") : text("road.abyssal", "Abyssal road (Descent)");
    }

    /** "+3", "-12", "0": the floor as it is written on screen. */
    static String floor(int floor) { return FloorMap.label(floor); }

    /** Is this a person with a connection (not a test's stand-in)? Messages and sounds are only sent to such a player: a stand-in has nobody to hear them. */
    static boolean real(ServerPlayer p) { return p != null && !(p instanceof FakePlayer) && p.connection != null; }
}
