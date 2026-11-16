package com.newuniverse.nusmp.dragonballs;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Why: the 'turn me into a Saiyan' wish must not depend on how the Saiyan race is built (another worker owns it). The wish only calls this one hook: it returns true when the player became a Saiyan and
 * false when that was not possible (already one, race missing, switch off). {@link DbSaiyanBridge} fills it at start-up; a test or a build without the Saiyan code can leave the default, which refuses.
 */
public final class DbHooks {
    private DbHooks() {}

    public static volatile Predicate<ServerPlayer> makeSaiyan = p -> false;
}
