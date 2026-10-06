package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.blackclover.ModeArmor;

/** Preview stub: the mode under preview, fully grown. */
public final class ModeArmorClient {
    public static ModeArmor.Mode MODE = ModeArmor.Mode.NONE;
    static ModeArmor.Mode mode(int id, long now) { return MODE; }
    static float age(int id, float now) { return 100f; }
    static float left(int id, float now) { return 100f; }
}
