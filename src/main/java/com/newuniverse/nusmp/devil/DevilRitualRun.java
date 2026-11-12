package com.newuniverse.nusmp.devil;

/**
 * Why: a ritual that is running (3.76.0) is a few numbers that live with the caller, in the live part of the contract attachment (never saved, never in a static map): which devil is being
 * called, when it began, where the circle is and which way the caller faced. Plain data, no Minecraft type; {@code DevilRituals} reads it every tick while it runs.
 */
public final class DevilRitualRun {
    public final String devil;
    public final long start;
    public final double x, y, z, faceX, faceZ;
    public final String dimension;

    public DevilRitualRun(String devil, long start, double x, double y, double z, double faceX, double faceZ, String dimension) {
        this.devil = devil;
        this.start = start;
        this.x = x;
        this.y = y;
        this.z = z;
        this.faceX = faceX;
        this.faceZ = faceZ;
        this.dimension = dimension;
    }
}
