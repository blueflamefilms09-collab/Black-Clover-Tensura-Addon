package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the Spirit Dive Wind: the REEL look with the PACK Wind (the default). A scene before it may have chosen another look. */
public final class SdwPack {
    private SdwPack() {}

    public static void register() {
        SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.PACK);
    }
}
