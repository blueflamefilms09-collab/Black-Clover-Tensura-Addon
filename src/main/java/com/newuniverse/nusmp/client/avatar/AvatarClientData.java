package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;

import java.util.List;

/**
 * Why: the server's answers (races, the roll, a refusal) arrive as packets, possibly a moment before or after the screen exists; this holds the latest
 * of each with a version number the screen polls, so it never misses one and never has to be told. Cleared on logout and whenever a new creation
 * session opens.
 */
public final class AvatarClientData {
    private AvatarClientData() {}

    public static List<RaceEntry> races = List.of();
    public static int racesVersion;
    public static S2C_RollPayload roll;
    public static int rollVersion;
    public static S2C_CreationResultPayload result;
    public static int resultVersion;

    public static void clear() {
        races = List.of(); roll = null; result = null;
        racesVersion++; rollVersion++; resultVersion++;
    }
}
