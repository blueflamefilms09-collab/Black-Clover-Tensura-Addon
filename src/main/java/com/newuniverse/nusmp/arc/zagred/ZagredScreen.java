package com.newuniverse.nusmp.arc.zagred;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: "a spoken word appears on screen" is the heart of the fight's design (the owner's reference: the party must READ what the devil says and answer it),
 * so the way it appears is one small piece of code with one look: the Word in big letters in its own colour, the one-line counter under it, shown to
 * everyone near the fight for as long as the wind-up lasts. It is plain title packets (vanilla, server to client): no client code, no new network message,
 * no resource, and every line has a language key with the English text as its fallback, so a missing language entry still reads right. A personal
 * message (a verdict, a hint to the one who strikes a closed heart) goes to one player only, on the action bar.
 */
final class ZagredScreen {
    private ZagredScreen() {}

    /** Players this close to the boss see his words. */
    static final double RANGE = 56;

    /** A line with a language key (the fallback is the English text), in a colour, bold or not. */
    static MutableComponent text(String key, String fallback, int rgb, boolean bold) {
        return Component.translatableWithFallback(key, fallback).withStyle(Style.EMPTY.withColor(rgb).withBold(bold));
    }

    /** The Word on every screen near {@code at}: its name in big letters, the way to answer it underneath. {@code windUpTicks} is how long it stays up. */
    static void word(ServerLevel sl, Vec3 at, ZagredWord w, int windUpTicks) {
        title(sl, at, text(w.titleKey(), w.title(), w.rgb(), true), text(w.hintKey(), w.hint(), 0xE8E0F8, false), 2, Math.max(16, Math.min(70, windUpTicks + 8)), 8);
    }

    /** A big line and a small one on every screen near {@code at}. */
    static void title(ServerLevel sl, Vec3 at, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        double r2 = RANGE * RANGE;
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.distanceToSqr(at) > r2) continue;
            send(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /** A big line and a small one on one screen. The subtitle goes first and the title last, because the title is what starts the display. */
    static void send(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (p.connection == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** A verdict for one player: the word over their head, and what THEY must do about it. */
    static void personal(ServerPlayer p, ZagredWord w, ZagredAnswers.Answer answer, int windowTicks) {
        MutableComponent how = text(answer.hintKey(), answer.hint(), 0xFFB0B8, true);
        MutableComponent sub = Component.translatableWithFallback("message.nusmp.zagred.verdict.answer", "Your answer: %s", how).withStyle(Style.EMPTY.withColor(0xFFD8DC));
        send(p, text(w.titleKey(), w.title(), w.rgb(), true), sub, 2, windowTicks, 8);
    }

    /** One line on the action bar of everyone near {@code at}. */
    static void bar(ServerLevel sl, Vec3 at, Component line) {
        double r2 = RANGE * RANGE;
        for (ServerPlayer p : sl.players()) if (!p.isSpectator() && p.distanceToSqr(at) <= r2) p.displayClientMessage(line, true);
    }

    /** A short announcement under the usual title time (the act changes, the ward is rewritten, the heart opens). */
    static void notice(ServerLevel sl, Vec3 at, String key, String fallback, int rgb, String subKey, String subFallback) {
        title(sl, at, text(key, fallback, rgb, true), text(subKey, subFallback, 0xE8E0F8, false), 4, 30, 10);
    }
}
