package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive Wind: the REEL look with the Wind of 3.3 (the option spiritDive.windLook = REEL), for the before / after sheets. */
public final class SdwReelWind {
    private SdwReelWind() {}

    public static void register() {
        SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.REEL);
    }
}
