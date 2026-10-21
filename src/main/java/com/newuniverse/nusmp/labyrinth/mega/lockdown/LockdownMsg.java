package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: every line the lockdown and the ambush director say is a TRANSLATABLE component with its English fallback ({@link LockdownText}), so each player reads it in
 * their own language and it still reads properly before the lang fragment is merged. Lines go to real players only: a fake player of a self-test has no connection and
 * sending to it would throw (the escape rules' helper does the same).
 */
final class LockdownMsg {
    private LockdownMsg() {}

    static MutableComponent text(String key, Object... args) {
        return Component.translatableWithFallback(key, LockdownText.english(key), args);
    }

    static void tell(ServerPlayer p, Component c) {
        if (p != null && SystemSync.hasConnection(p)) p.sendSystemMessage(c);
    }

    static void tellAll(List<ServerPlayer> ps, Component c) {
        for (ServerPlayer p : ps) tell(p, c);
    }

    /** A System pop-up (a lang key under {@code message.nusmp.mega.system.lockdown}) for real players. */
    static void toast(List<ServerPlayer> ps, ToastKind kind, String key, String... args) {
        for (ServerPlayer p : ps) if (SystemSync.hasConnection(p)) SystemSync.toast(p, kind, key, args);
    }
}
