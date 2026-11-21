package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.DbChat;
import com.newuniverse.nusmp.dragonballs.DbConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: this is the single door that spoken words come through, whatever heard them: the built-in recogniser, the admin relay command {@code /multiverse dragonballs say <player> <words>} (for any other
 * voice-to-text mod, plugin or command block), or a test. It re-checks on the server that the player is online and alive, optionally pulls the words onto the ritual vocabulary ({@link SpeechMatch}), and
 * hands the text to the unchanged chat handling ({@link DbChat#acceptLine}), so a ritual phrase or a wish said aloud behaves exactly like the same text typed. The text is NOT shown in chat; the speaker only
 * gets an action-bar line "Heard: ..." (no new packet type). Server thread; a call from another thread hops over to it.
 */
public final class SpeechBridge {
    private SpeechBridge() {}

    /** Longest text taken, like a chat line. */
    public static final int MAX_CHARS = 256;

    /**
     * Returns true when the words were passed on (or, from another thread, queued to be); false when voice input is off, the player is not there / not alive, or there was nothing to hear.
     * Never throws for an unknown player.
     */
    public static boolean heard(UUID player, String transcript) {
        MinecraftServer server = DbVoice.server();
        if (server == null || player == null || transcript == null || !DbVoiceConfig.enabled() || !DbConfig.enabled()) return false;
        if (!server.isSameThread()) {
            server.execute(() -> heard(player, transcript));
            return true;
        }
        String text = transcript.replace("[unk]", " ").strip();
        if (text.isEmpty() || text.length() > MAX_CHARS) return false;
        ServerPlayer p = server.getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.isSpectator()) return false;
        String line = DbVoiceConfig.matchTolerance() ? SpeechMatch.canonicalize(text, DbVoice.vocabulary()) : text;
        if (line.isEmpty()) return false;
        DbChat.acceptLine(p, line);
        p.displayClientMessage(Component.translatable("anime_multiverse.dbvoice.heard", text.length() > 80 ? text.substring(0, 80) : text), true);
        return true;
    }
}
