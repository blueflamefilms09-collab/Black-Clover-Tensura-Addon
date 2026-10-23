package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive Wind remake: the REEL look with the Wind design C (C_SAINT). */
public final class SdwV3C {
    private SdwV3C() {}

    public static void register() {
        SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.C_SAINT);
    }
}
