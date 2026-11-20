package com.newuniverse.nusmp.lords;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: every drawback of a Mortal Sin weapon must make sense and be explained to the player (owner request). One place builds the words, from the lang file ({@code tools/lang/lords.json}): the tooltip of every form
 * of every weapon (what it takes from you, how much, how it recovers, how to avoid it, and what the mastery does about it) and the equip warning the wielder reads when the weapon binds. Keys:
 * {@code lords.drawback.<sin>.1..n} (line 1 what it takes, 2 how much, 3 how it recovers, 4 how to avoid it; Greed has two more: Half-Starvation and its mastery) and the names. Pure text, no rules.
 */
public final class LordText {
    private LordText() {}

    /** How many explanation lines a sin has: the four weapons four, Greed six (Half-Starvation and its mastery). */
    public static int drawbackLines(LordRules.Sin s) { return s == LordRules.Sin.GREED ? 6 : 4; }

    /** The lang keys a sin must have in the lang file (a test reads them). */
    public static List<String> requiredKeys(LordRules.Sin s) {
        java.util.ArrayList<String> k = new java.util.ArrayList<>();
        for (int i = 1; i <= drawbackLines(s); i++) k.add(s.drawbackKey(i));
        k.add(s.drawbackNameKey());
        k.add(s.masteryKey());
        k.add(s.ultimateKey());
        k.add("lords.name." + s.id);
        for (int f = 0; f < s.forms(); f++) {
            k.add(s.formKey(f));
            k.add(s.artKey(f));
        }
        return k;
    }

    /** The tooltip of one form of a Lord weapon. */
    public static void tooltip(List<Component> lines, LordRules.Sin sin, int form) {
        lines.add(Component.translatable("lords.tooltip.title", Component.translatable("lords.name." + sin.id)).withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("lords.tooltip.form", form, Component.translatable(sin.formKey(form))).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("lords.tooltip.art", Component.translatable(sin.artKey(form))).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("lords.tooltip.mastery", Component.translatable(sin.masteryKey()), LordRules.FORM1_LEVEL, LordRules.FORM2_LEVEL, LordRules.ULTIMATE_LEVEL, Component.translatable(sin.ultimateKey())).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("lords.tooltip.drawback", Component.translatable(sin.drawbackNameKey())).withStyle(ChatFormatting.RED));
        for (int i = 1; i <= drawbackLines(sin); i++) lines.add(Component.translatable(sin.drawbackKey(i)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("lords.tooltip.keys").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The warning the wielder reads when a weapon binds to them: what it is, what it takes, how they get out of it. Sent once per bind, to the one player. */
    public static void warn(ServerPlayer p, LordRules.Sin sin) {
        p.sendSystemMessage(Component.translatable("lords.warning.title", Component.translatable(sin.formKey(0))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.translatable("lords.warning.intro", Component.translatable(sin.drawbackNameKey())).withStyle(ChatFormatting.RED));
        for (int i = 1; i <= drawbackLines(sin); i++) p.sendSystemMessage(Component.translatable(sin.drawbackKey(i)).withStyle(ChatFormatting.YELLOW));
        p.sendSystemMessage(Component.translatable("lords.warning.release").withStyle(ChatFormatting.GRAY));
    }
}
