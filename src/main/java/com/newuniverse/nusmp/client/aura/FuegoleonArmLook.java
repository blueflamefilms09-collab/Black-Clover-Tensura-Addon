package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Look;

/**
 * Fuegoleon's flame arm (3.1): which LOOK of the arm this client draws ({@code fuegoleonArm.look} of the CLIENT config {@code nusmp-spiritdive-client.toml}, mirrored here by
 * {@link DiveClientConfig#apply} so the painters read a field, not a config, every frame). DJINN (default) is the Djinn-Equip flame limb ({@link FuegoleonArmDjinn}); V3 is the 3.0.0 arm
 * (molten model, flame sheets, mane strands); CLASSIC is the plain molten model of the 3.0.0 arm without sheets or mane. Only looks: the server's decision (who has the arm) is unchanged.
 */
public final class FuegoleonArmLook {
    private FuegoleonArmLook() {}

    private static volatile Look look = FuegoleonArmDjinnRules.DEFAULT_LOOK;

    public static Look get() { return look; }

    public static void set(Look l) { look = FuegoleonArmDjinnRules.pick(l); }
}
