package com.newuniverse.nusmp.elite.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the one late-bound hook that is left, the Tensura-coin economy. The OWNER of the number (elite/hub) sets it once at init and every reader calls the static field, which answers a safe default
 * (0, false) until the owner is built or when the economy is off. The other hooks of the 1.83.0 plan (forge, records, nations, magicule, ...) went with their systems in 1.88.0.
 */
public final class EliteHooks {
    private EliteHooks() {}

    // ---- owned by elite/hub: the Tensura-coin economy (value in copper coins; 0 / false when the economy is off)
    public interface Wallet {
        long balance(ServerPlayer player);
        boolean charge(ServerPlayer player, long coins);
        void give(ServerPlayer player, long coins);
    }

    public static volatile Wallet wallet = new Wallet() {
        @Override public long balance(ServerPlayer player) { return 0L; }
        @Override public boolean charge(ServerPlayer player, long coins) { return false; }
        @Override public void give(ServerPlayer player, long coins) { }
    };
}
