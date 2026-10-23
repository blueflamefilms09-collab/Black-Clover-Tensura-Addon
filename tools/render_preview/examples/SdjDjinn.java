package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Djinn-style Spirit Dive: draws the DJINN look (the default) and resets the option a scene before it may have set. */
public final class SdjDjinn {
    private SdjDjinn() {}

    public static void register() { SpiritDiveLook.set(Look.DJINN, true, true, FuegoleonArmRules.View.ALWAYS); }
}
