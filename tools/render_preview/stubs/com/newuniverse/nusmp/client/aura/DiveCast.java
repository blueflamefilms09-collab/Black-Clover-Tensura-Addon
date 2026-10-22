package com.newuniverse.nusmp.client.aura;

/** Preview stub of DiveCast: the preview has no cast animation system; set the environment variable SDV_CAST=1 to see the flare of a cast. */
public final class DiveCast {
    private DiveCast() {}

    private static final boolean CAST = "1".equals(System.getenv("SDV_CAST"));

    public static boolean casting(int entityId) { return CAST; }
}
