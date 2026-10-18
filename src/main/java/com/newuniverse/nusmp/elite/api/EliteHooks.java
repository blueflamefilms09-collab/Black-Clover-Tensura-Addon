package com.newuniverse.nusmp.elite.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Why: the 1.83.0 subsystems (forge, records and titles, skills, coins, nations, ...) read each other's state (a skill evolves when N items were forged, a title needs a record, a shop charges coins),
 * but they are built by separate builders in parallel. So every cross-system read goes through one of the late-bound hooks below: the OWNER of a number sets the hook once at init, every READER
 * calls the static method, which answers a safe default (0, false, empty) until the owner is built or when its config is off. No subsystem imports another subsystem's classes, so any can be removed
 * or left out without breaking the others. All methods are server-side and cheap (they may be called every tick). Players are addressed by UUID so offline data works too.
 */
public final class EliteHooks {
    private EliteHooks() {}

    // ---- owned by elite/forge
    public static volatile ToIntFunction<UUID> forgeCraftedCount = id -> 0;
    public static volatile ToIntFunction<UUID> forgeBestQuality = id -> 0;          // 0 none, 1 common ... 6 mythical

    // ---- owned by elite/records
    public static volatile BiPredicate<UUID, String> recordUnlocked = (id, record) -> false;
    public static volatile BiPredicate<UUID, String> hasTitle = (id, title) -> false;
    public static volatile Function<UUID, String> activeTitle = id -> "";
    public static volatile ToIntFunction<UUID> heldTitleCount = id -> 0;

    // ---- owned by elite/skills (skill evolution)
    public static volatile ToIntFunction<UUID> resetCount = id -> 0;

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

    // ---- owned by elite/nations
    public static volatile Function<UUID, String> nationOf = id -> "";               // nation id, "" for none
    public static volatile BiPredicate<String, String> atWar = (a, b) -> false;

    // ---- owned by elite/world: magicule density of the player's chunk, 0..1
    public static volatile java.util.function.ToDoubleFunction<ServerPlayer> chunkMagicule = p -> 0.0;
}
