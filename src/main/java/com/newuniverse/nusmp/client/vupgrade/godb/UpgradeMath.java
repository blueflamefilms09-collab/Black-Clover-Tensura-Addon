package com.newuniverse.nusmp.client.vupgrade.godb;

/**
 * The pure maths of the upgraded weapons (release 1.102.0, slice god_melee_b): which mesh and which passes at which distance, the travelling vein wave, the breathing halo, where
 * an orbiting piece is, how a swing smears, how many ambient particles a layer may spawn. No Minecraft classes and no allocation (callers pass scratch arrays), so it is unit tested
 * and cheap enough for the render thread. tools/visual_upgrade/god_melee_b/preview.py mirrors {@link #orbit}.
 */
public final class UpgradeMath {
    private UpgradeMath() {}

    /** Level of detail by distance of the weapon from the camera (blocks): FULL = the full mesh and every pass; LOD = the level-of-detail mesh, the passes kept; FAR = the level-of-detail mesh, no additive pass. */
    public static final int TIER_FULL = 0, TIER_LOD = 1, TIER_FAR = 2;

    public static int tier(double distanceSq, float nearBlocks, float additiveBlocks) {
        if (distanceSq < (double) nearBlocks * nearBlocks) return TIER_FULL;
        return distanceSq < (double) additiveBlocks * additiveBlocks ? TIER_LOD : TIER_FAR;
    }

    /** The mesh id to draw: the upgraded one when the switch is on, the table exists and that mesh loads; otherwise the old one (the fallback of {@code visual.upgrade=false}). */
    public static String meshFor(boolean switchOn, boolean hasSpec, boolean upgradedMeshLoads, String id, String upgradedId) {
        return switchOn && hasSpec && upgradedMeshLoads ? upgradedId : id;
    }

    /** The brightness (base..peak) of the energy-vein pass at model height y: a wave that runs up the weapon, {@code speed} radians per tick, one crest every {@code wavelength} units. */
    public static float wave(float y, float time, float speed, float wavelength, float base, float peak) {
        float phase = (float) (2.0 * Math.PI) * (y / wavelength) - time * speed;
        return base + (peak - base) * (0.5f + 0.5f * (float) Math.sin(phase));
    }

    /** The slow multiplier on the core glow (1 +- amp), two incommensurate frequencies so it never repeats visibly. */
    public static float flicker(float time, float amp, float speed) {
        if (amp <= 0f) return 1f;
        return 1f + amp * (0.6f * (float) Math.sin(time * speed) + 0.4f * (float) Math.sin(time * speed * 2.71f + 1.3f));
    }

    /** The alpha (0..255) of the halo shell: its base alpha breathing by +-{@code breath} of itself. */
    public static int haloAlpha(int baseAlpha, float time, float breath, float speed) {
        float k = 1f + breath * 8f * (float) Math.sin(time * speed);
        return Math.max(0, Math.min(255, Math.round(baseAlpha * k)));
    }

    /** The halo shell's scale: its configured scale breathing a little. */
    public static float haloScale(float scale, float time, float breath, float speed) {
        return scale + breath * (float) Math.sin(time * speed + 1.1f);
    }

    /**
     * Where piece {@code i} of an orbiter is at {@code time} (ticks): out[0..2] = x, y, z in model units (about the weapon axis x = z = 0), out[3] = its angle round the axis (radians),
     * out[4] = its own spin (radians). {@code y0}..{@code y1} is the axis range of the mesh.
     */
    public static void orbit(UpgradeSpec.Orbiter o, int i, float time, float y0, float y1, float[] out) {
        float ang = o.phase() + o.speed() * time + (float) (2.0 * Math.PI) * i / Math.max(1, o.count());
        float y = y0 + (y1 - y0) * o.yFrac() + o.bob() * (float) Math.sin(o.bobSpeed() * time + i * 1.3f);
        out[0] = o.radius() * (float) Math.cos(ang);
        out[1] = y;
        out[2] = o.radius() * (float) Math.sin(ang);
        out[3] = ang;
        out[4] = o.spin() * time + i;
    }

    /** The alpha of ghost {@code g} (1-based) of {@code ghosts} while a swing is at {@code progress} (0..1): strongest mid-swing, fading with the lag. */
    public static int swingAlpha(float progress, int alpha, int g, int ghosts) {
        if (progress <= 0f || progress >= 1f || ghosts <= 0) return 0;
        float env = (float) Math.sin(Math.PI * progress);
        float fade = 1f - (g - 1f) / (ghosts + 0.5f);
        return Math.max(0, Math.min(255, Math.round(alpha * env * fade)));
    }

    /** The share of the full ambient particle rate a quality gets ("low", "medium", "high"; anything else = high). */
    public static float qualityFactor(String quality) {
        if (quality == null) return 1f;
        return switch (quality.toLowerCase(java.util.Locale.ROOT)) {
            case "low" -> 0.35f;
            case "medium" -> 0.7f;
            default -> 1f;
        };
    }

    /** The share of the particle rate at a distance from the camera: all of it near, half of it up to the particle distance, none beyond. */
    public static float distanceFactor(double distance, float nearBlocks, float particleBlocks) {
        if (distance <= nearBlocks) return 1f;
        return distance <= particleBlocks ? 0.5f : 0f;
    }

    /** How many particles to spawn this tick for an expected (fractional) count: the whole part, plus one with the fractional part as the chance ({@code rnd} in 0..1). */
    public static int stochasticCount(float expected, float rnd) {
        if (expected <= 0f) return 0;
        int whole = (int) expected;
        return whole + (rnd < expected - whole ? 1 : 0);
    }

    /** Vertex colour of a pass: alpha 0..255 from a 0..1 brightness, the colour white (the texture carries the hue). */
    public static int argb(float brightness) {
        int a = Math.max(0, Math.min(255, Math.round(brightness * 255f)));
        return a << 24 | 0xFFFFFF;
    }

    /** The wave of the vein pass, quantised into 64 steps so the render thread can look up instead of calling sin per vertex: the table of one frame. */
    public static void waveTable(float time, float speed, float wavelength, float base, float peak, float y0, float y1, float[] table) {
        int n = table.length;
        for (int k = 0; k < n; k++) table[k] = wave(y0 + (y1 - y0) * k / (n - 1), time, speed, wavelength, base, peak);
    }

    /** Looks a height up in a table built by {@link #waveTable} (clamped, linear). */
    public static float lookup(float[] table, float y, float y0, float y1) {
        float f = (y - y0) / (y1 - y0) * (table.length - 1);
        if (f <= 0f) return table[0];
        if (f >= table.length - 1) return table[table.length - 1];
        int i = (int) f;
        float t = f - i;
        return table[i] + (table[i + 1] - table[i]) * t;
    }
}
