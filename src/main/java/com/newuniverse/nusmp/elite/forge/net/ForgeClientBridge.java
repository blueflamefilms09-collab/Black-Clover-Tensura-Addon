package com.newuniverse.nusmp.elite.forge.net;

import java.util.function.Consumer;

/**
 * Why: the packet handlers are registered in common code (the server needs the same payload types), but what a client DOES with an arriving packet is opening a
 * screen, which must never load on a dedicated server. So the handlers call these late-bound consumers, which do nothing until the client's
 * {@code ForgeClient.init} replaces them (the same pattern as {@code EliteHooks}); no client class is referenced from here.
 */
public final class ForgeClientBridge {
    private ForgeClientBridge() {}

    public static volatile Consumer<ForgeNet.OpenStation> open = p -> { };
    public static volatile Consumer<ForgeNet.Start> start = p -> { };
    public static volatile Consumer<ForgeNet.Done> done = p -> { };
    public static volatile Consumer<ForgeNet.Sync> sync = p -> { };
}
