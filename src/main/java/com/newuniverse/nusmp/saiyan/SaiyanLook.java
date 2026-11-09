package com.newuniverse.nusmp.saiyan;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the LOOK of every Saiyan form (3.69.0, Stage 8) as pure data and pure math, so the client painter ({@code client.aura.SaiyanAura}), the preview, the tests and the budget all read one table.
 * A form is drawn as: glowing eyes, an aura of flame tongues and lightning bolts in the form's colour, the Saiyan battle armour (forged on while the form lasts), for the Super Saiyan 4 forms red fur
 * on the body and a tail, and the CHARGE-UP when the form starts: rocks lift off the ground, streaks of energy are pulled into the body, cracks spread over the ground, a flash, a column of light,
 * and shockwave rings. Hair is NOT drawn (the owner put the hair on hold; the hair maths is kept in {@code tools/saiyan_look_parked/}). Nothing here touches the game: the server only tells nearby
 * players which form code a Saiyan has ({@code PlayerAuras.set(p, Aura.SAIYAN, ticks, SaiyanForms.code(id))}); each client draws the look itself.
 * <p>Colours come from the owner's chart "All Saiyan Forms of Goku" and the series' well-known looks (Super Saiyan: gold aura, teal eyes; Super Saiyan 3: lightning; Super Saiyan 4: red fur, gold
 * eyes, tail; God: red; Blue: blue; Ultra Instinct: silver). The wiki pages could not be fetched from the build session, so every number is tuned by eye and is a table cell to change.
 * <p>Cost (worst case, one Super Saiyan 4 Limit Breaker in full detail, in the middle of its charge-up): 12 flames and 1 body haze, 8 bolts, 4 eye quads, 2 steady ring quads, about 72 fur quads (the model drawn once
 * more), 12 tail quads, plus the charge-up: {@link #MAX_CHIPS} rock chips, {@link #MAX_STREAKS} streaks, 2 crack quads, 2 pillar quads, 4 ring quads, 2 flash quads = under {@link #MAX_QUADS}; a player
 * more than {@link #NEAR} blocks away draws about a fifth of that (no fur, no tail, no bolts, no rock chips, half the flames and streaks). Hard cap {@link #MAX_QUADS}.
 */
public final class SaiyanLook {
    private SaiyanLook() {}

    /** Up to this distance (blocks) a look is drawn in full detail; further, in the light version. */
    public static final double NEAR = 32.0;
    /** Beyond this distance nothing is drawn at all. */
    public static final double FAR = 64.0;
    /** The most quads one look may draw in full detail, charge-up included (a test pins every row under it). */
    public static final int MAX_QUADS = 190;
    public static final int MAX_FLAMES = 12, MAX_BOLTS = 8, MAX_CHIPS = 10, MAX_STREAKS = 12;
    /** Ticks of the gathering part of the charge-up (rocks lift, streaks flow in, cracks spread). */
    public static final int GATHER_TICKS = 22;
    /** Ticks the whole charge-up lasts (the burst, the column of light and the rings end here). */
    public static final int BURST_TICKS = 46;
    /** Ticks until the ground cracks have faded. */
    public static final int CRACK_TICKS = 78;

    /**
     * One form's look. eyes/glow/fur are RGB (no alpha). flames/bolts are the counts, auraH the height of the aura in blocks. {@code burst} scales the charge-up (1 = Super Saiyan; the lightning forms and
     * the gods are bigger, Kaio-ken smaller). {@code armour}: the form wears the battle armour.
     */
    public record Look(String id, int eyes, boolean eyeGlow, int glow, int fur, boolean tail, int flames, int bolts, float auraH, float burst, boolean armour) {
        public boolean hasFur() { return fur != 0; }
    }

    private static Look l(String id, int eyes, boolean eyeGlow, int glow, int fur, boolean tail, int flames, int bolts, float auraH, float burst, boolean armour) {
        return new Look(id, eyes, eyeGlow, glow, fur, tail, flames, bolts, auraH, burst, armour);
    }

    private static final Map<String, Look> TABLE = new LinkedHashMap<>();

    private static void add(Look k) { TABLE.put(k.id(), k); }

    static {
        //   id                                  eyes    glow?  aura      fur       tail    fl bolt  H     burst armour
        add(l("kaioken",                         0xFF4030, true,  0xE03020, 0,        false,  8, 0, 2.1f, 0.70f, true));
        add(l("false_super_saiyan",              0x80D0C0, true,  0xE0B040, 0,        false,  6, 0, 2.0f, 0.75f, true));
        add(l("super_saiyan",                    0x30E8C8, true,  0xFFD020, 0,        false,  9, 0, 2.3f, 1.00f, true));
        add(l("super_saiyan_full_power",         0x40F0D0, true,  0xFFE060, 0,        false,  5, 0, 2.0f, 0.90f, true));
        add(l("super_saiyan_2",                  0x40F0D8, true,  0xFFE840, 0,        false, 10, 6, 2.5f, 1.15f, true));
        add(l("super_saiyan_3",                  0x40F0D8, true,  0xFFC820, 0,        false, 12, 8, 2.9f, 1.40f, true));
        add(l("super_saiyan_4",                  0xFFC020, true,  0xC02828, 0xC02020, true,   9, 0, 2.4f, 1.20f, false));
        add(l("super_full_power_super_saiyan_4", 0xFFD040, true,  0xE03838, 0xD83030, true,  10, 0, 2.6f, 1.30f, false));
        add(l("super_saiyan_4_limit_breaker",    0xFFE060, true,  0xFF4080, 0xFF4468, true,  12, 6, 3.0f, 1.50f, false));
        add(l("super_saiyan_god",                0xFF3030, true,  0xFF3030, 0,        false,  7, 0, 2.2f, 1.25f, true));
        add(l("super_saiyan_blue",               0x50C8FF, true,  0x40B0FF, 0,        false, 10, 0, 2.4f, 1.30f, true));
        add(l("perfected_super_saiyan_blue",     0xB0ECFF, true,  0x80D8FF, 0,        false, 12, 4, 2.7f, 1.40f, true));
        add(l("super_saiyan_blue_kaioken",       0xFF5068, true,  0xFF4060, 0,        false, 12, 5, 2.7f, 1.45f, true));
        add(l("ultra_instinct_sign",             0xC8D4F0, true,  0xB0C8FF, 0,        false,  8, 0, 2.4f, 1.20f, true));
        add(l("perfected_ultra_instinct",        0xF0F6FF, true,  0xE8F0FF, 0,        false,  8, 0, 2.7f, 1.45f, true));
        add(l("true_ultra_instinct",             0xFFFFFF, true,  0xFFFFFF, 0,        false, 10, 5, 3.0f, 1.60f, true));
    }

    /** The look of a form id, or null (Base has none). */
    public static Look of(String formId) { return formId == null ? null : TABLE.get(formId); }

    /** The look for the code carried by the aura packet ({@link SaiyanForms#code}); null for 0 / unknown. */
    public static Look ofCode(int code) {
        SaiyanForms.Form f = SaiyanForms.ofCode(code);
        return f == null ? null : TABLE.get(f.id());
    }

    public static java.util.Collection<Look> all() { return TABLE.values(); }

    // ------------------------------------------------------------------ the charge-up (t = ticks since the form began)
    /** 0..1: how far the gathering has come (0 at the start, 1 at {@link #GATHER_TICKS}). */
    public static float gather(float t) { return clamp01(t / GATHER_TICKS); }

    /** 0..1: the white flash at the heart of the power-up; builds slowly through the gathering, peaks at the burst, dies in about 10 ticks. */
    public static float flash(float t) {
        if (t < 0f || t > GATHER_TICKS + 10f) return 0f;
        if (t <= GATHER_TICKS) { float u = t / GATHER_TICKS; return 0.25f * u * u; }
        float u = (t - GATHER_TICKS) / 10f;
        return 1f - u * u;
    }

    /** 0..1: the height of the column of light as a share of its full height; it shoots up at the burst, holds a moment, then thins and fades out by {@link #BURST_TICKS}. */
    public static float pillar(float t) {
        float start = GATHER_TICKS - 4f;
        if (t < start || t >= BURST_TICKS) return 0f;
        return t < GATHER_TICKS + 2f ? (t - start) / 6f : 1f;
    }

    /** 0..1: the opacity of the column of light (full at the burst, thinning to nothing). */
    public static float pillarAlpha(float t) {
        if (t < GATHER_TICKS - 4f || t >= BURST_TICKS) return 0f;
        if (t < GATHER_TICKS + 2f) return (t - (GATHER_TICKS - 4f)) / 6f;
        float u = (t - (GATHER_TICKS + 2f)) / (BURST_TICKS - GATHER_TICKS - 2f);
        return 1f - u * u;
    }

    /** The width (half, blocks) of the column of light: narrow while it climbs, wide at the burst, closing. */
    public static float pillarWidth(float t, float burst) {
        float p = pillarAlpha(t);
        return (0.35f + 0.55f * p) * (0.7f + 0.3f * burst);
    }

    /** The radius (blocks) of shockwave ring k (0 or 1) t ticks after a form starts, or 0 when it is not out (ring 1 follows ring 0 by 6 ticks). */
    public static float ringRadius(float t, int k, float burst) {
        float s = GATHER_TICKS + 6f * k, d = BURST_TICKS - s;
        float u = (t - s) / d;
        if (u <= 0f || u >= 1f) return 0f;
        return 0.6f + (4.2f + 2.6f * burst) * (float) Math.pow(u, 0.55);
    }

    /** 0..1: the opacity of ring k (bright at the start, gone at the end). */
    public static float ringAlpha(float t, int k) {
        float s = GATHER_TICKS + 6f * k, d = BURST_TICKS - s;
        float u = (t - s) / d;
        return u <= 0f || u >= 1f ? 0f : 1f - u;
    }

    /** The radius (blocks) of the ground cracks: spread over the gathering, then stay and fade by {@link #CRACK_TICKS}. */
    public static float crackRadius(float t, float burst) {
        if (t < 0f || t >= CRACK_TICKS) return 0f;
        return (0.4f + 2.6f * burst) * smooth(clamp01(t / (GATHER_TICKS + 4f)));
    }

    /** 0..1: the opacity of the cracks (they glow white-hot at the burst and cool off). */
    public static float crackAlpha(float t) {
        if (t < 0f || t >= CRACK_TICKS) return 0f;
        float rise = clamp01(t / 8f);
        float cool = t < GATHER_TICKS ? 1f : 1f - clamp01((t - GATHER_TICKS) / (CRACK_TICKS - GATHER_TICKS));
        return rise * cool;
    }

    /**
     * Rock chip i: out[0] angle (radians), out[1] radius from the body (blocks), out[2] height above the ground (blocks), out[3] size (half, blocks), out[4] spin (radians).
     * They sit on a ring round the player, lift while the energy gathers (the higher the later), hang, and are thrown outwards and down by the burst.
     */
    public static void chip(int i, float t, float burst, float[] out) {
        float a = i * 2.39996f + hash(i) * 0.6f;
        float r0 = 0.9f + 1.5f * hash(i + 31);
        float top = (0.5f + 1.4f * hash(i + 77)) * (0.6f + 0.4f * burst);
        float lift = smooth(clamp01((t - 2f - hash(i + 5) * 6f) / (GATHER_TICKS - 4f)));
        float h = top * lift, r = r0;
        if (t > GATHER_TICKS) {
            float u = clamp01((t - GATHER_TICKS) / (BURST_TICKS - GATHER_TICKS + 8f));
            r = r0 + (0.8f + 1.2f * hash(i + 9)) * burst * u;
            h = Math.max(0f, top * (1f - u * u) + 0.6f * u * (1f - u));
        }
        out[0] = a + t * 0.02f * (hash(i + 3) - 0.5f);
        out[1] = r;
        out[2] = h;
        out[3] = 0.07f + 0.06f * hash(i + 13);
        out[4] = t * 0.13f * (hash(i + 21) - 0.5f) * 4f;
    }

    /** 0..1: the opacity of the rock chips (they pop in with the lift and are gone when the burst has thrown them away). */
    public static float chipAlpha(float t) {
        if (t < 1f) return 0f;
        float in = clamp01(t / 4f);
        float out = t < BURST_TICKS ? 1f : 1f - clamp01((t - BURST_TICKS) / 8f);
        return in * out;
    }

    /**
     * Energy streak i: the head's position (x, y, z in blocks, relative to the feet, centred on the body) in out[0..2], and out[3] its 0..1 opacity. A streak starts out wide at the edge of the
     * gathering and is pulled into the chest in 14 ticks, over and over while the energy gathers (several streaks are always in flight); after the burst they stop.
     */
    public static void streak(int i, float t, float[] out) {
        int n = MAX_STREAKS;
        float a = i * 2.39996f + 0.7f * hash(i + 2);
        float cycle = 14f;
        float ph = (t / cycle + i / (float) n) % 1f;
        float u = ph * ph;                                    // slow at the start, accelerating into the body
        float r = 3.4f * (1f - u) + 0.15f;
        float h = 0.1f + (0.2f + 1.2f * hash(i + 17)) * (1f - u) + 0.9f * u;
        out[0] = (float) Math.cos(a) * r;
        out[1] = h;
        out[2] = (float) Math.sin(a) * r;
        float live = t < GATHER_TICKS + 4f ? 1f : 1f - clamp01((t - GATHER_TICKS - 4f) / 6f);
        out[3] = live * clamp01(ph / 0.15f) * clamp01((1f - ph) / 0.1f);
    }

    // ------------------------------------------------------------------ the steady aura
    /** The height (blocks) of flame number i of n at time age (ticks): licks up and down. */
    public static float flameHeight(Look k, int i, float age) {
        float w = 0.5f + 0.5f * (float) Math.sin(age * 0.32f + i * 2.1f);
        return k.auraH() * (0.7f + 0.5f * w) * (0.8f + 0.2f * hash(i + 41));
    }

    /** A cheap deterministic 0..1 number from an index (no Random, so the preview and the game agree). */
    public static float hash(int i) {
        int h = i * 0x9E3779B1 ^ 0x85EBCA6B;
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 12;
        return (h & 0xFFFF) / 65536f;
    }

    /** The number of quads a look draws at its busiest (t = the middle of the charge-up); far = the light version (beyond {@link #NEAR} blocks). */
    public static int quads(Look k, boolean far) {
        int flames = Math.min(k.flames(), MAX_FLAMES), bolts = Math.min(k.bolts(), MAX_BOLTS);
        if (far) return flames / 2 + 1 + (k.eyeGlow() ? 2 : 0) + 2 + MAX_STREAKS / 2 + 2 + 2 + 2;
        return flames + 1 + bolts + (k.eyeGlow() ? 4 : 0) + 2 + (k.hasFur() ? 72 : 0) + (k.tail() ? 12 : 0) + MAX_CHIPS + MAX_STREAKS + 2 + 2 + 4 + 2;
    }

    private static float smooth(float x) { x = clamp01(x); return x * x * (3f - 2f * x); }

    private static float clamp01(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }
}
