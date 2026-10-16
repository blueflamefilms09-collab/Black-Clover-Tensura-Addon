package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_OpenActionMenuPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RacesPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import net.minecraft.client.Minecraft;

/**
 * Why: the client ends of the creation packets, kept in a client-only class so a dedicated server never loads screen code. Each one only stores the
 * data (or opens the action screen); the creation screen reads it by polling {@link AvatarClientData}.
 */
public final class AvatarClientNet {
    private AvatarClientNet() {}

    public static void onRaces(S2C_RacesPayload p) {
        AvatarClientData.races = p.races();
        AvatarClientData.roll = null;                // a new session: an old roll is not valid any more
        AvatarClientData.result = null;
        AvatarClientData.racesVersion++; AvatarClientData.rollVersion++; AvatarClientData.resultVersion++;
    }

    public static void onRoll(S2C_RollPayload p) {
        AvatarClientData.roll = p;
        AvatarClientData.rollVersion++;
    }

    public static void onResult(S2C_CreationResultPayload p) {
        AvatarClientData.result = p;
        AvatarClientData.resultVersion++;
    }

    public static void onOpenActions(S2C_OpenActionMenuPayload p) {
        Minecraft.getInstance().setScreen(new AvatarActionScreen(p));
    }
}
