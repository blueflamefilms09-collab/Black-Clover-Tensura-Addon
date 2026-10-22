package examples;

import com.newuniverse.nusmp.client.aura.FuegoleonArmAura;

/** Preview helper of the Spirit Dive remake: registers the painter of Fuegoleon's flaming arm (Aura.FUEGOLEON_ARM) next to the painter of the scene's attribute (use "alsoAuras": ["FUEGOLEON_ARM:2"]). */
public final class SdvFuegoleonArm {
    private SdvFuegoleonArm() {}

    public static void register() { FuegoleonArmAura.register(); }
}
