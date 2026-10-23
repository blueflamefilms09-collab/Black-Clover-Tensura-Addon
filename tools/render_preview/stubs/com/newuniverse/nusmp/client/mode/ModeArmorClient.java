package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.blackclover.ModeArmor;

/** Preview stub: the mode every player wears (a scene's example sets it), started at game time 0 and never ending (the dive's own clock comes from the game time of the frame). */
public final class ModeArmorClient {
    public static ModeArmor.Mode MODE = ModeArmor.Mode.NONE;
    static ModeArmor.Mode mode(int id, long now) { return MODE; }
    static float age(int id, float now) { return now; }
    static float left(int id, float now) { return 1000f; }
}
