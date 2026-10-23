package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive Wind remake: the REEL look with the Wind design B (B_CLIP). */
public final class SdwV3B {
    private SdwV3B() {}

    public static void register() {
        SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.B_CLIP);
    }
}
