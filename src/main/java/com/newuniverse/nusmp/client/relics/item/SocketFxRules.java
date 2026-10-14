package com.newuniverse.nusmp.client.relics.item;

import java.util.Locale;

/**
 * The pure half of {@link SocketFx}: the particle budget and the quality level. No Minecraft types, so every row is tested. The ambient particles of a
 * bound socket are a handful per second per holder, only for holders near the local camera, never more than {@link #MAX_PARTICLES_PER_TICK} in a
 * tick for the whole screen, and off at quality 0. The quality is the shared relic effects option (relicfx.quality, created by the worn-relic layer's
 * options); {@link #parseQuality} reads whatever shape it has and falls back to {@link #DEFAULT_QUALITY}.
 */
public final class SocketFxRules {
    private SocketFxRules() {}

    public static final int OFF = 0, LOW = 1, MEDIUM = 2, HIGH = 3, DEFAULT_QUALITY = MEDIUM;
    /** Most holders given particles in one tick, and most particles of all of them together. */
    public static final int MAX_HOLDERS = 8, MAX_PARTICLES_PER_TICK = 10;
    /** No holder farther than this many blocks from the local player gets particles, whatever the view distance. */
    public static final double MAX_DISTANCE = 24.0;

    /** Ticks between two bursts of one holder at a quality (0 = never). */
    public static int interval(int quality) {
        return switch (clamp(quality)) {
            case LOW -> 8;
            case MEDIUM -> 4;
            case HIGH -> 2;
            default -> 0;
        };
    }

    /** Base particles per burst (a coloured dust mote comes on top). */
    public static int perBurst(int quality) { return clamp(quality) >= HIGH ? 2 : 1; }

    /** Is this holder's burst due on this tick? Staggered by the entity id so a crowd never bursts together. */
    public static boolean due(long tick, int entityId, int quality) {
        int iv = interval(quality);
        return iv > 0 && Math.floorMod(tick + entityId, (long) iv) == 0L;
    }

    /** The squared distance a holder may be at: the view distance in blocks, capped at {@link #MAX_DISTANCE}. */
    public static double maxDistanceSq(int viewDistanceChunks) {
        double d = Math.min(MAX_DISTANCE, Math.max(1, viewDistanceChunks) * 16.0);
        return d * d;
    }

    public static int clamp(int q) { return Math.max(OFF, Math.min(HIGH, q)); }

    /**
     * Reads a quality from an option of any shape: an int (0 to 3), a boolean (false = off, true = default), or a name/enum text (off, low, medium/normal/
     * default, high/ultra/max/fancy/fabulous); a null or unknown value gives {@link #DEFAULT_QUALITY}.
     */
    public static int parseQuality(Object o) {
        if (o == null) return DEFAULT_QUALITY;
        if (o instanceof Integer i) return clamp(i);
        if (o instanceof Number n) return clamp(n.intValue());
        if (o instanceof Boolean b) return b ? DEFAULT_QUALITY : OFF;
        String s = o.toString().trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "0", "off", "none", "false", "disabled" -> OFF;
            case "1", "low", "minimal", "fast" -> LOW;
            case "2", "medium", "normal", "default", "mid" -> MEDIUM;
            case "3", "high", "ultra", "max", "fancy", "fabulous", "true" -> HIGH;
            default -> DEFAULT_QUALITY;
        };
    }
}
