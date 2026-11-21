package com.newuniverse.nusmp.grimoire;

/**
 * The numbers of the revamped grimoire's effects and of its draw cost, as pure maths (no game classes, unit-tested): which detail tier an owner at a distance gets, how the
 * rune ring and the glow halo fade in and flare on a cast, and how many particles a summon, a cast and the idle page flutter may spawn. {@code client.grimoire.GrimoireRevampFx} only
 * applies them. Worst case of the whole layer, per floating book in the FULL tier: 2 translucent ring quads and 1 halo quad (each two-sided = 6 quads per frame), and at most
 * {@link #SUMMON_BURST} particles once on arrival, {@link #maxCast()} per cast and one flutter mote per {@link #FLUTTER_TICKS} ticks. Owners further than {@link #LOD_DIST} blocks get none
 * of it and the cheap classic book model; further than {@link #CULL_DIST} blocks the book is not drawn at all.
 */
public final class GrimoireRevampMath {
    private GrimoireRevampMath() {}

    /** Beyond this distance (blocks) the owner's book is the classic model and has no effects. */
    public static final double LOD_DIST = 24.0;
    /** Beyond this distance (blocks) the owner's book is not drawn. */
    public static final double CULL_DIST = 64.0;

    public enum Tier { FULL, LOD, CULL }

    /** The detail tier from a SQUARED distance in blocks. */
    public static Tier tier(double distSq) {
        return distSq > CULL_DIST * CULL_DIST ? Tier.CULL : distSq > LOD_DIST * LOD_DIST ? Tier.LOD : Tier.FULL;
    }

    /** Particles: once when the book finishes opening, the flare of a chant / a release / a signature, and the idle flutter. */
    public static final int SUMMON_BURST = 14, CHANT_BURST = 4, RELEASE_BURST = 8, SIGNATURE_BURST = 12, FLUTTER_TICKS = 8;
    /** A new flare is ignored for this many ticks after the last one (a held cast key re-sends its clip). */
    public static final float FLARE_COOLDOWN = 6f;
    /** Ticks a flare lasts. */
    public static final float FLARE_LIFE = 16f;

    public static int maxCast() { return Math.max(CHANT_BURST, Math.max(RELEASE_BURST, SIGNATURE_BURST)); }

    /** 0 = no flare for this clip name, else the strength 0..1 of the flare a body animation clip starts (a cast chant, a release, a signature). */
    public static float flareStrength(String clip) {
        if (clip == null || clip.isEmpty()) return 0f;
        if (clip.equals("cast_signature")) return 1f;
        if (clip.startsWith("cast_release") || clip.equals("cast_weapon_sword") || clip.equals("cast_weapon_heavy") || clip.equals("cast_weapon_katana")
                || clip.equals("cast_weapon_dance")) return 0.65f;
        if (clip.equals("cast_chant") || clip.equals("cast_mana_zone")) return 0.35f;
        return 0f;
    }

    /** Particles for a flare of this strength (4 chant .. 12 signature). */
    public static int burstFor(float strength) {
        return strength >= 0.9f ? SIGNATURE_BURST : strength >= 0.5f ? RELEASE_BURST : strength > 0f ? CHANT_BURST : 0;
    }

    /** The flare envelope 0..1 {@code age} ticks after it started: a fast rise (2 ticks) and an ease-out over {@link #FLARE_LIFE}. */
    public static float flare(float age) {
        if (age < 0f || age >= FLARE_LIFE) return 0f;
        if (age < 2f) return age / 2f;
        float t = (age - 2f) / (FLARE_LIFE - 2f);
        return (1f - t) * (1f - t);
    }

    /** Opacity of the rune ring: fades in over the opening steps, a little brighter on a flare. */
    public static float ringAlpha(float open, float flareNow, float strength) {
        float base = 0.38f * clamp01(open);
        return clamp01(base + 0.55f * flareNow * strength);
    }

    /** Opacity of the glow halo behind the pages. */
    public static float haloAlpha(float open, float flareNow, float strength, float time) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.12f);
        return clamp01(clamp01(open) * (0.16f + 0.05f * pulse) + 0.35f * flareNow * strength);
    }

    /** Ring radius (model units): breathes a little and swells on a flare. */
    public static float ringRadius(float flareNow, float strength, float time) {
        return 6.6f + 0.25f * (float) Math.sin(time * 0.07f) + 2.2f * flareNow * strength;
    }

    /** Ring spin (degrees): slow, faster on a flare. */
    public static float ringSpin(float time, float flareNow) {
        return time * 0.9f + 40f * flareNow;
    }

    /**
     * Which of the theme's four particle slots (FxSlot ordinal: 0 MOTE, 1 ACCENT, 2 SPARK, 3 AMBIENT) a magic's page motes use: the per-magic accent. Anti-magic and Kotodama
     * crackle (spark), flame embers spark, water and earth are soft and heavy (ambient), wind is the second voice (accent), everything else the signature mote.
     */
    public static int accentSlot(String soulType, boolean antiOrCorrupt) {
        if (antiOrCorrupt) return 2;
        if (soulType == null) return 0;
        return switch (soulType) {
            case "FLAME" -> 2;
            case "WATER", "EARTH" -> 3;
            case "WIND" -> 1;
            default -> 0;
        };
    }

    /** True when an idle flutter mote may spawn: every {@link #FLUTTER_TICKS} ticks, and only once the book is fully open. */
    public static boolean flutterDue(float now, float lastFlutter, int step) {
        return step >= GrimoireBookPlan.OPEN_STEPS && now - lastFlutter >= FLUTTER_TICKS;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
