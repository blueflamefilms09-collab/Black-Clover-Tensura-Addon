package com.newuniverse.nusmp.elite.races.client;

import com.newuniverse.nusmp.elite.races.net.RaceClientState;
import net.minecraft.client.Minecraft;

/** Why: the payload handler (a common class) reaches the client only through this one entry point, by name inside a lambda, so a dedicated server never loads it. It stamps the server's word with the client's own clock for the HUD countdown. */
public final class RaceClientHooks {
    private RaceClientHooks() {}

    public static void onState(String state, int remainingTicks, int argb) {
        Minecraft mc = Minecraft.getInstance();
        RaceClientState.set(state, remainingTicks, argb, mc.level == null ? 0L : mc.level.getGameTime());
    }
}
