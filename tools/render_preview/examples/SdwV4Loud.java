package examples;

import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;

/** Preview helper of the final Spirit Dive Wind A: the final Wind A with spiritDive.calm = false (the 3.5 density all the time). */
public final class SdwV4Loud {
    private SdwV4Loud() {}

    public static void register() {
        SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.A_ANIME);
        SpiritDiveLook.setCalm(false);
    }
}
