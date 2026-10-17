package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.net.S2C_AptitudeRollPayload;
import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_OpenActionMenuPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RacesPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import com.newuniverse.nusmp.avatar.net.S2C_SoulForgeBirthPayload;
import com.newuniverse.nusmp.avatar.net.S2C_SoulForgeStatePayload;
import com.newuniverse.nusmp.vfx.client.VfxShake;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the client ends of the creation packets, kept in a client-only class so a dedicated server never loads screen code. Each one only stores the
 * data (or opens the action screen); the creation screen reads it by polling {@link AvatarClientData}, and the Soul Forge screen reads its own
 * packets (rules and reserves, the free aptitude roll, a sample line) from {@link SoulForgeClientData}. The birth packet shakes the camera.
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
        if ("bark".equals(p.kind())) { SoulForgeClientData.onBark(p.message()); return; }          // a sample line is not a verdict
        AvatarClientData.result = p;
        AvatarClientData.resultVersion++;
    }

    public static void onForgeState(S2C_SoulForgeStatePayload p) { SoulForgeClientData.onState(p); }

    public static void onAptitudeRoll(S2C_AptitudeRollPayload p) { SoulForgeClientData.onRoll(p); }

    /** The strike of the birth spectacle: the camera shakes, harder the closer it is to the circle (VfxShake does the falloff). */
    public static void onForgeBirth(S2C_SoulForgeBirthPayload p) {
        if (p.power() > 0f) VfxShake.add(new Vec3(p.x(), p.y(), p.z()), p.power(), Math.max(1, Math.min(60, p.ticks())));
    }

    public static void onOpenActions(S2C_OpenActionMenuPayload p) {
        Minecraft.getInstance().setScreen(new AvatarActionScreen(p));
    }
}
