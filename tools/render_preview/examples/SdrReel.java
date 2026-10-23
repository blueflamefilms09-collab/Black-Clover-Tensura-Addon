package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive REEL look: makes sure the REEL look is the one drawn (a scene before it may have chosen another look). */
public final class SdrReel {
    private SdrReel() {}

    public static void register() { SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS); }
}
