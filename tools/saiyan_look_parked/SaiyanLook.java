package com.newuniverse.nusmp.saiyan;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the LOOK of every Saiyan form (3.66.0, Stage 8) as pure data and pure math, so the client painter ({@code client.aura.SaiyanAura}), the preview, the tests and the budget all read one table.
 * A form is drawn as: hair strands on the head (colour, count, length, how they sweep, how much they droop), glowing eyes, an aura of flame tongues and lightning bolts in the form's colour, a
 * shockwave ring when the form starts, and, for the Super Saiyan 4 forms, red fur on the body and a tail. Nothing here touches the game: the server only tells nearby players which form code a Saiyan
 * has ({@code PlayerAuras.set(p, Aura.SAIYAN, ticks, SaiyanForms.code(id))}); each client draws the look itself.
 * <p>Colours come from the owner's chart "All Saiyan Forms of Goku" and the series' well-known looks (Super Saiyan: gold hair, teal eyes; Super Saiyan 2: spikier, with sparks; Super Saiyan 3: hair to the
 * knees; Super Saiyan 4: black hair, red fur, gold eyes, tail; God: red; Blue: blue; Ultra Instinct: silver). The wiki pages could not be fetched from this session (see docs/saiyan.md), so the numbers are
 * tuned by eye and every one is a table cell to change.
 * <p>Space of the hair maths: the head's own space after the model's head transform, in blocks, y pointing DOWN (the head cube spans x -0.25..0.25, y -0.5..0, z -0.25..0.25, the face looks to -z).
 * <p>Cost (worst case, one Super Saiyan 4 Limit Breaker in full detail): 14 strands x 3 segments x 2 crossed quads = 84, 12 flames, 6 bolts, 4 eye quads, 2 ring quads, about 72 fur quads
 * (the model drawn once more), 12 tail quads = 192 quads; a player more than {@link #NEAR} blocks away draws about a third of that (no fur, half the flames, no bolts). Hard cap {@link #MAX_QUADS}.
 */
public final class SaiyanLook {
    private SaiyanLook() {}

    /** Up to this distance (blocks) a look is drawn in full detail; further, in the light version. */
    public static final double NEAR = 32.0;
    /** The most quads one look may draw in full detail (a test pins every row under it). */
    public static final int MAX_QUADS = 220;
    public static final int MAX_STRANDS = 16, MAX_SEGS = 4, MAX_FLAMES = 12, MAX_BOLTS = 8;
    /** Ticks the burst of a new form lasts (the ring and the flash). */
    public static final int BURST_TICKS = 26;

    /**
     * One form's look. hair/eyes/glow/fur are RGB (no alpha). spikes counts all strands, bangs of them fall over the forehead. segs is how many joints a strand has, length its whole length in
     * blocks, lift how upright it grows, sweep how far it is combed backwards, droop how fast gravity bends it. flames/bolts are the counts, auraH the height of the aura in blocks.
     */
    public record Look(String id, int hair, int eyes, boolean eyeGlow, int glow, int fur, boolean tail,
                       int spikes, int bangs, int segs, float length, float lift, float sweep, float droop, float width,
                       int flames, int bolts, float auraH) {
        public boolean hasFur() { return fur != 0; }
    }

    private static Look l(String id, int hair, int eyes, boolean eyeGlow, int glow, int fur, boolean tail, int spikes, int bangs, int segs,
                          float length, float lift, float sweep, float droop, float width, int flames, int bolts, float auraH) {
        return new Look(id, hair, eyes, eyeGlow, glow, fur, tail, spikes, bangs, segs, length, lift, sweep, droop, width, flames, bolts, auraH);
    }

    private static final Map<String, Look> TABLE = new LinkedHashMap<>();

    private static void add(Look k) { TABLE.put(k.id(), k); }

    static {
        //   id                                  hair      eyes    glow?  aura      fur       tail   n  bng seg  len   lift  swp   drp   wid   fl bolt  H
        add(l("kaioken",                         0x1A1A24, 0x202020, false, 0xE03020, 0,        false,  8, 2, 1, 0.34f, 0.90f, 0.25f, 0.00f, 0.11f,  8, 0, 2.1f));
        add(l("false_super_saiyan",              0xD0B040, 0x80D0C0, true,  0xE0B040, 0,        false,  9, 3, 1, 0.36f, 0.90f, 0.20f, 0.00f, 0.12f,  6, 0, 2.0f));
        add(l("super_saiyan",                    0xFFD93A, 0x30E8C8, true,  0xFFD020, 0,        false, 10, 3, 2, 0.42f, 0.95f, 0.15f, 0.10f, 0.12f,  9, 0, 2.3f));
        add(l("super_saiyan_full_power",         0xFFE266, 0x40F0D0, true,  0xFFE060, 0,        false, 10, 3, 2, 0.42f, 0.95f, 0.15f, 0.10f, 0.12f,  5, 0, 2.0f));
        add(l("super_saiyan_2",                  0xFFE850, 0x40F0D8, true,  0xFFE840, 0,        false, 12, 3, 2, 0.50f, 1.00f, 0.10f, 0.05f, 0.13f, 10, 6, 2.5f));
        add(l("super_saiyan_3",                  0xFFC82A, 0x40F0D8, true,  0xFFC820, 0,        false, 15, 0, 4, 1.45f, 0.60f, 0.55f, 1.00f, 0.16f, 12, 8, 2.9f));
        add(l("super_saiyan_4",                  0x14101A, 0xFFC020, true,  0xC02828, 0xC02020, true,  12, 2, 3, 0.75f, 0.70f, 0.60f, 0.45f, 0.14f,  9, 0, 2.4f));
        add(l("super_full_power_super_saiyan_4", 0x1C1420, 0xFFD040, true,  0xE03838, 0xD83030, true,  13, 2, 3, 0.85f, 0.70f, 0.60f, 0.45f, 0.14f, 10, 0, 2.6f));
        add(l("super_saiyan_4_limit_breaker",    0x241018, 0xFFE060, true,  0xFF4080, 0xFF4468, true,  14, 2, 3, 1.00f, 0.70f, 0.65f, 0.50f, 0.15f, 12, 6, 3.0f));
        add(l("super_saiyan_god",                0xE02424, 0xFF3030, true,  0xFF3030, 0,        false,  9, 3, 2, 0.40f, 0.90f, 0.20f, 0.05f, 0.12f,  7, 0, 2.2f));
        add(l("super_saiyan_blue",               0x38B4FF, 0x50C8FF, true,  0x40B0FF, 0,        false,  9, 3, 2, 0.40f, 0.90f, 0.20f, 0.05f, 0.12f, 10, 0, 2.4f));
        add(l("perfected_super_saiyan_blue",     0x7CD6FF, 0xB0ECFF, true,  0x80D8FF, 0,        false,  9, 3, 2, 0.40f, 0.90f, 0.20f, 0.05f, 0.12f, 12, 4, 2.7f));
        add(l("super_saiyan_blue_kaioken",       0x38B4FF, 0xFF5068, true,  0xFF4060, 0,        false,  9, 3, 2, 0.40f, 0.90f, 0.20f, 0.05f, 0.12f, 12, 5, 2.7f));
        add(l("ultra_instinct_sign",             0x9AA4C0, 0xC8D4F0, true,  0xB0C8FF, 0,        false,  9, 3, 2, 0.40f, 0.90f, 0.20f, 0.05f, 0.12f,  8, 0, 2.4f));
        add(l("perfected_ultra_instinct",        0xDCE6FF, 0xF0F6FF, true,  0xE8F0FF, 0,        false, 11, 3, 3, 0.55f, 0.85f, 0.30f, 0.30f, 0.13f,  8, 0, 2.7f));
        add(l("true_ultra_instinct",             0xFFFFFF, 0xFFFFFF, true,  0xFFFFFF, 0,        false, 11, 3, 3, 0.60f, 0.85f, 0.30f, 0.30f, 0.13f, 10, 5, 3.0f));
    }

    /** The look of a form id, or null (Base has none). */
    public static Look of(String formId) { return formId == null ? null : TABLE.get(formId); }

    /** The look for the code carried by the aura packet ({@link SaiyanForms#code}); null for 0 / unknown. */
    public static Look ofCode(int code) {
        SaiyanForms.Form f = SaiyanForms.ofCode(code);
        return f == null ? null : TABLE.get(f.id());
    }

    public static java.util.Collection<Look> all() { return TABLE.values(); }

    // ------------------------------------------------------------------ hair maths (head space, blocks, y down)
    /**
     * Strand k of the hair: out[0..2] its base on the head, out[3..5] its first direction (unit length), out[6] the length of one segment. k must be below {@code look.spikes()}.
     * The first {@code bangs} strands fall forwards over the forehead; the others stand on the crown on a golden-angle spiral, leaning outwards, up and (by {@code sweep}) backwards.
     */
    public static void strand(Look k, int i, float[] out) {
        int n = Math.max(1, Math.min(k.spikes(), MAX_STRANDS));
        int bangs = Math.min(k.bangs(), n);
        float dx, dy, dz, bx, by, bz, len = k.length() / Math.max(1, k.segs());
        if (i < bangs) {
            float x = (i - (bangs - 1) / 2f) * 0.13f;
            bx = x; by = -0.47f; bz = -0.24f;
            dx = x * 0.5f; dy = 0.45f; dz = -0.75f;
            len *= 0.7f;
        } else {
            int j = i - bangs, m = Math.max(1, n - bangs);
            float a = j * 2.39996f + 0.5f, rr = 0.20f * (float) Math.sqrt((j + 0.5f) / m);
            float hx = (float) Math.cos(a), hz = (float) Math.sin(a);
            bx = hx * rr; by = -0.5f; bz = hz * rr;
            dx = hx * 0.55f; dy = -k.lift(); dz = hz * 0.55f + k.sweep();
            len *= 0.85f + 0.3f * hash(j);
        }
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        out[0] = bx; out[1] = by; out[2] = bz;
        out[3] = dx / l; out[4] = dy / l; out[5] = dz / l;
        out[6] = len;
    }

    /** Bends a unit direction by gravity (droop > 0 pulls down, +y) and renormalises it, in place: d[0..2]. */
    public static void bend(float[] d, float droop) {
        d[1] += droop * 0.55f;
        float l = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        if (l < 1e-6f) { d[0] = 0f; d[1] = 1f; d[2] = 0f; return; }
        d[0] /= l; d[1] /= l; d[2] /= l;
    }

    /** The width of a strand (half width, blocks) at joint s of segs (0 = the base): tapers to 12% at the tip. */
    public static float halfWidth(Look k, int s) {
        float t = s / (float) Math.max(1, k.segs());
        return k.width() * (1f - 0.88f * t) * 0.5f;
    }

    /** A cheap deterministic 0..1 number from an index (no Random, so the preview and the game agree). */
    public static float hash(int i) {
        int h = i * 0x9E3779B1 ^ 0x85EBCA6B;
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 12;
        return (h & 0xFFFF) / 65536f;
    }

    // ------------------------------------------------------------------ the burst, the budget, the level of detail
    /** 0..1: how far the form's hair has grown (springs up over the first 12 ticks of the form). */
    public static float grow(float t) { return clamp01(t / 12f); }

    /** The radius (blocks) of the power-up ring t ticks after a form starts, or 0 once the burst is over. */
    public static float ringRadius(float t) {
        if (t < 0f || t >= BURST_TICKS) return 0f;
        float u = t / BURST_TICKS;
        return 0.5f + 3.5f * (float) Math.pow(u, 0.6);
    }

    /** 0..1: the strength of the burst (1 at the start, 0 at the end). */
    public static float burst(float t) { return t < 0f || t >= BURST_TICKS ? 0f : 1f - t / BURST_TICKS; }

    /** The number of quads a look draws; far = the light version (beyond {@link #NEAR} blocks). */
    public static int quads(Look k, boolean far) {
        int strands = Math.min(k.spikes(), MAX_STRANDS), segs = Math.min(k.segs(), MAX_SEGS);
        int flames = Math.min(k.flames(), MAX_FLAMES), bolts = Math.min(k.bolts(), MAX_BOLTS);
        if (far) return Math.min(strands, 8) * segs + flames / 2 + (k.eyeGlow() ? 2 : 0) + 2;
        return strands * segs * 2 + flames + bolts + (k.eyeGlow() ? 4 : 0) + 2 + (k.hasFur() ? 72 : 0) + (k.tail() ? 12 : 0);
    }

    private static float clamp01(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }
}
