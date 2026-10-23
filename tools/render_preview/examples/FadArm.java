package examples;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.client.aura.FuegoleonArmAura;
import com.newuniverse.nusmp.client.aura.FuegoleonArmLook;

/** Preview helper of Fuegoleon's Djinn-style flame arm (3.1): registers the painter of Aura.FUEGOLEON_ARM; the look is the default (DJINN). Use "attr": "FuegoleonArm". */
public final class FadArm {
    private FadArm() {}

    public static void register() {
        FuegoleonArmLook.set(FuegoleonArmDjinnRules.Look.DJINN);          // the look is a static: a scene that ran before in the same process may have set another
        FuegoleonArmAura.register();
    }
}
