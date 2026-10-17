package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;

import java.util.List;

/** Why: harness shim of the classic screen's data holder (same fields). */
public final class AvatarClientData {
    private AvatarClientData() {}

    public static List<RaceEntry> races = List.of();
    public static int racesVersion;
    public static S2C_RollPayload roll;
    public static int rollVersion;
    public static S2C_CreationResultPayload result;
    public static int resultVersion;
}
