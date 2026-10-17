package com.newuniverse.nusmp.arc.dialogue;

import com.newuniverse.nusmp.arc.ArcCharacter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a spoken line is a chat message in the character's colour with its name in bold, the same look as the mod's boss lines, so a conversation reads
 * the same whoever speaks. A line with a language key is sent as a translatable component whose fallback is the file's English (the client translates it
 * with its own language file); a line without a key is sent as the literal text, so a datapack's words are heard exactly as written. The placeholders
 * {@code {player}}, {@code {npc}} and {@code {saga}} are filled here (as positional arguments for a keyed line).
 */
public final class DialogueSender {
    private DialogueSender() {}

    /** The chat component of one line, spoken by {@code c} to {@code p}. {@code voice} is the book's colour or -1 for the character's own. */
    public static Component compose(ServerPlayer p, ArcCharacter c, int voice, DialogueLine line, String sagaTitle) {
        String player = p.getGameProfile().getName();
        MutableComponent words = line.key().isEmpty()
                ? Component.literal(line.plain(player, c.name(), sagaTitle))
                : Component.translatableWithFallback(line.key(), line.template(), player, c.name(), sagaTitle);
        int rgb = (voice >= 0 ? voice : c.color()) & 0xFFFFFF;
        MutableComponent name = Component.literal(c.name()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true));
        return name.append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(words.withStyle(ChatFormatting.WHITE));
    }

    public static void send(ServerPlayer p, ArcCharacter c, int voice, DialogueLine line, String sagaTitle) {
        p.sendSystemMessage(compose(p, c, voice, line, sagaTitle));
    }
}
