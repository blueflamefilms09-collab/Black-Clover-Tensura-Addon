package examples;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.client.aura.FuegoleonArmAura;
import com.newuniverse.nusmp.client.aura.FuegoleonArmLook;

/** Preview helper of the 3.0.0 look of Fuegoleon's arm kept as a client option (fuegoleonArm.look = V3): same painter, the older look. */
public final class FadArmV3 {
    private FadArmV3() {}

    public static void register() {
        FuegoleonArmLook.set(FuegoleonArmDjinnRules.Look.V3);
        FuegoleonArmAura.register();
    }
}
