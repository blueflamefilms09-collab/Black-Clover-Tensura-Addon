package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Why: every line the curse says goes through one small helper so that it is a translatable component with an English fallback (the text shows even before a
 * language file carrying the key is merged), so the sin's colour is applied the same way everywhere, and so that a message to a stand-in player of a self-test, or a
 * player whose connection is gone, is dropped quietly instead of throwing out of an event handler. The English texts are {@link CurseText}'s.
 */
final class CurseMessages {
    private CurseMessages() {}

    /** A translatable component with the English text of {@link CurseText} as its fallback. */
    static MutableComponent tr(String key, Object... args) { return Component.translatableWithFallback(key, CurseText.english(key), args); }

    /** The component in the colour of a sin. */
    static MutableComponent colored(Sin sin, MutableComponent c) { return c.withStyle(style -> style.withColor(sin.rgb())); }

    private static boolean reachable(ServerPlayer p) { return p != null && !(p instanceof FakePlayer) && p.connection != null; }

    /** A line on the action bar. */
    static void bar(ServerPlayer p, Component c) {
        if (reachable(p)) p.displayClientMessage(c, true);
    }

    /** A line in the chat. */
    static void chat(ServerPlayer p, Component c) {
        if (reachable(p)) p.sendSystemMessage(c);
    }

    /** A title and a subtitle with the given fade-in, stay and fade-out in ticks. */
    static void title(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (!reachable(p)) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
