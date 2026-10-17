package net.neoforged.neoforge.network;

import java.util.ArrayList;
import java.util.List;

/** Why: harness shim: the packets the screen would send are collected for the scenario to look at. */
public final class PacketDistributor {
    private PacketDistributor() {}
    public static final List<Object> SENT = new ArrayList<>();
    public static void sendToServer(Object payload) { SENT.add(payload); }
}
