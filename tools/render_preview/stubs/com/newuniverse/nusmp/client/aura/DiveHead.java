package com.newuniverse.nusmp.client.aura;

/** Preview stub of DiveHead: the preview has no equipment; set the environment variable SDA_HELMET=1 to see a wearer with a helmet (no eyes, no hair cap). */
public final class DiveHead {
    private DiveHead() {}

    private static final boolean HELMET = "1".equals(System.getenv("SDA_HELMET"));

    public static boolean helmet(net.minecraft.client.player.AbstractClientPlayer p) { return HELMET; }
}
