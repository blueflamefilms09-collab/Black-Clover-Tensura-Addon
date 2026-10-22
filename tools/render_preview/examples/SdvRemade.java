package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive remake: draws the remade Spirit Dive (the default look) and resets the classic option a scene before it may have set. */
public final class SdvRemade {
    private SdvRemade() {}

    public static void register() { SpiritDiveLook.set(false, true, true, FuegoleonArmRules.View.ALWAYS); }
}
