package com.newuniverse.nusmp.visual.genesis;

/**
 * The shapes of the signature bursts of the acts (pure maths, no Minecraft classes): the i-th of n particles of a burst of `radius` blocks, as an offset in the frame of
 * the caster: x to the right, y up, z forward. The velocity gives the direction the particle should drift (so an inward burst really pulls in).
 */
public final class VuBurst {
    private VuBurst() {}

    public static final String[] PATTERNS = {"ring", "line", "column", "spiral", "inward", "nova", "cone"};

    public static boolean known(String p) {
        for (String s : PATTERNS) if (s.equals(p)) return true;
        return false;
    }

    /** out = {x, y, z, vx, vy, vz}. {@code t} is 0..1 through the burst's life (a line or a spiral grows with it). */
    public static void point(String pattern, int i, int n, float radius, float t, float[] out) {
        float f = n <= 1 ? 0f : (float) i / (n - 1);
        double a = 2.0 * Math.PI * i / Math.max(1, n);
        float cx = (float) Math.cos(a), sx = (float) Math.sin(a);
        switch (pattern) {
            case "line" -> {
                out[0] = 0f; out[1] = 0.1f * sx; out[2] = radius * f * (0.4f + 0.6f * t);
                out[3] = 0f; out[4] = 0f; out[5] = 0.18f;
            }
            case "column" -> {
                out[0] = cx * radius * 0.35f; out[1] = radius * f * 0.9f; out[2] = sx * radius * 0.35f + radius * 0.4f;
                out[3] = 0f; out[4] = 0.12f; out[5] = 0f;
            }
            case "spiral" -> {
                double s = a * 2.5 + t * 6.0;
                float rr = radius * (0.25f + 0.75f * f);
                out[0] = (float) Math.cos(s) * rr; out[1] = radius * f * 0.6f; out[2] = (float) Math.sin(s) * rr + radius * 0.3f;
                out[3] = 0f; out[4] = 0.08f; out[5] = 0f;
            }
            case "inward" -> {
                out[0] = cx * radius; out[1] = 0.2f + sx * 0.4f; out[2] = sx * radius + radius * 0.2f;
                out[3] = -cx * 0.22f; out[4] = 0f; out[5] = -sx * 0.22f;
            }
            case "nova" -> {
                float el = (float) Math.sin(a * 3.0) * 0.6f;
                out[0] = cx * radius * 0.2f; out[1] = el * 0.5f; out[2] = sx * radius * 0.2f + radius * 0.3f;
                out[3] = cx * 0.35f; out[4] = el * 0.12f; out[5] = sx * 0.35f;
            }
            case "cone" -> {
                float ang = (f - 0.5f) * 1.6f;
                out[0] = (float) Math.sin(ang) * radius * (0.5f + 0.5f * t); out[1] = 0.1f; out[2] = (float) Math.cos(ang) * radius * (0.5f + 0.5f * t);
                out[3] = (float) Math.sin(ang) * 0.2f; out[4] = 0f; out[5] = (float) Math.cos(ang) * 0.2f;
            }
            default -> {                                                               // ring
                out[0] = cx * radius; out[1] = 0.15f; out[2] = sx * radius + radius * 0.3f;
                out[3] = cx * 0.12f; out[4] = 0.02f; out[5] = sx * 0.12f;
            }
        }
    }
}
