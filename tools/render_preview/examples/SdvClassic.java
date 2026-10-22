package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive remake: draws the Spirit Dive as in 1.99 (the client option spiritDive.classicLook) for the scenes that preview the classic look. The flag is static: every scene of the dive names SdvClassic or SdvRemade. */
public final class SdvClassic {
    private SdvClassic() {}

    public static void register() { SpiritDiveLook.set(true, true, true, FuegoleonArmRules.View.ALWAYS); }
}
