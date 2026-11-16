package com.newuniverse.nusmp.dragonballs;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.saiyan.SaiyanManager;
import com.newuniverse.nusmp.saiyan.SaiyanStore;

/**
 * The ONLY file of the Dragon Balls that names the Saiyan classes: it fills {@link DbHooks#makeSaiyan} with the existing public switch ({@code SaiyanManager.setSaiyan}, the same call the Saiyan command
 * uses). If the Saiyan code changes, this is the one place to touch. Any failure is logged and answered as "not possible", never thrown into the wish.
 */
final class DbSaiyanBridge {
    private DbSaiyanBridge() {}

    static void install() {
        DbHooks.makeSaiyan = p -> {
            try {
                if (SaiyanStore.isSaiyan(p)) return false;
                SaiyanManager.setSaiyan(p, true);
                return SaiyanStore.isSaiyan(p);
            } catch (Throwable t) {
                LogUtils.getLogger().warn("[anime_multiverse] dragon balls: the Saiyan wish could not be applied", t);
                return false;
            }
        };
    }
}
