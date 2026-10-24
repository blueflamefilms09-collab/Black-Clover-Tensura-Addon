package com.newuniverse.nusmp.client.mode;

import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Preview stub of {@code ModeArmorValkyrieWorld} (the real one asks the level for the wearers, the player's hands and the swing): no other wearers (the layer then admits the one player on the spot under the same
 * pool), the full pool, no lance item in hand, and a settable "busy" flag (a scene's example sets it to show the lance spinning up).
 */
final class ModeArmorValkyrieWorld {
    private ModeArmorValkyrieWorld() {}

    /** Preview helper (not in the game): true while the wearer is charging or attacking. */
    public static boolean BUSY;
    /** Preview helper: true when the wearer holds the lance item. */
    public static boolean HOLDS;

    static int scan(long gameTime, int[] ids, double[] distSq, boolean[] holds) { return 0; }

    static int pool() { return com.newuniverse.nusmp.book.ModeArmorValkyrieBudget.POOL_QUADS; }

    static boolean holdsLance(AbstractClientPlayer p) { return HOLDS; }

    static boolean busy(AbstractClientPlayer p) { return BUSY; }
}
