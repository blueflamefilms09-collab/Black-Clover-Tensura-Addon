package com.newuniverse.nusmp.client.song;

/**
 * Why: the server is the only side that hears the voice, so the HUD meter shows what it last reported. This holds that report in
 * plain fields (written on the client thread by the packet handler, read by the HUD layer) and fades the meter out when reports stop.
 */
public final class SongHudState {
    private static int level, mode, charge = 255;
    private static long receivedAt;

    private SongHudState() {}

    public static void receive(int lv, int md, int ch) {
        level = lv; mode = md; charge = ch;
        receivedAt = System.currentTimeMillis();
    }

    public static int level() { return level; }
    public static int mode() { return mode; }
    /** Crescendo charge 0..100, or a negative number when Crescendo is not armed. */
    public static int charge() { return charge > 100 ? -1 : charge; }

    /** The meter is shown while a mode is set and the server has reported in the last few seconds. */
    public static boolean visible() { return mode != 0 && System.currentTimeMillis() - receivedAt < 5000; }

    public static void clear() { level = 0; mode = 0; charge = 255; }
}
