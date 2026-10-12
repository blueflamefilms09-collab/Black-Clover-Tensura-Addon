package com.newuniverse.nusmp.dungeon;

import java.util.List;

/**
 * The mood of each tier of the Living Dungeon (docs/living_dungeon.md section 3): the colour and thickness of its fog, the particles that drift
 * in its air and the sounds that come out of its dark. Only data and arithmetic, no Minecraft types, so the tables can be checked without the
 * game; the client (DungeonSky) and the server (DungeonAmbience) read it and turn the ids into particles and sound events.
 * Moods are listed in the order of {@link DungeonTier}; a tier added later falls back to the first mood until it gets its own.
 */
public final class DungeonAtmosphere {
    private DungeonAtmosphere() {}

    // ---------------------------------------------------------------- the kinds of ambient particle (DungeonSky draws each of them)
    public static final String DUST = "dust", DRIP = "drip", BUBBLE = "bubble", SMOKE = "smoke", EMBER = "ember", GOLD = "gold", GLINT = "glint";

    /** The most particles of one kind a client tick may add, whatever the rate says. */
    public static final int MAX_PARTICLES_PER_KIND = 3;
    /** Ambient sounds come from this far away (blocks) in a random direction; growls come from the creature itself, no nearer than the least. */
    public static final double SOUND_MIN_DISTANCE = 12, SOUND_MAX_DISTANCE = 28;
    /** A volume above 1 widens the range a sound carries (16 blocks per unit); this is the most we ever ask for. */
    public static final float MAX_VOLUME = 4f;
    /** An awake apex predator (the Dungeon's Direwolf and Basilisk, or a top-tier Tensura mob) this close (blocks) can be heard growling in the dark, a rarer sound than the tier's own. */
    public static final int GROWL_RANGE = 40;
    public static final int GROWL_MIN_DISTANCE = 12, GROWL_MIN_TICKS = 700, GROWL_MAX_TICKS = 1500, GROWL_RECHECK_MIN_TICKS = 200, GROWL_RECHECK_MAX_TICKS = 400;
    /** Ticks before the first ambient sound after a player arrives: some quiet to start with, then the Dungeon speaks. */
    public static final int FIRST_SOUND_MIN_TICKS = 60, FIRST_SOUND_MAX_TICKS = 160;

    /**
     * One ambient sound: a vanilla sound event id, how likely it is among its tier's sounds, and its volume and pitch ranges. Volume here is how far
     * and how loud it carries: sources are 12 to 28 blocks away, so the game only lets them reach the player with a volume well above 1.
     */
    public record Sound(String id, int weight, float volumeMin, float volumeMax, float pitchMin, float pitchMax) {}

    /** One kind of ambient particle and how many are expected per client tick (0.35 = about one every three ticks). */
    public record Drift(String kind, float rate) {}

    /** What a tier looks and sounds like: fog colour (0..1) and how much of the usual fog distances are kept, particles, sounds and the gap between sounds. */
    public record Mood(String tier, float red, float green, float blue, float fogNear, float fogFar, List<Drift> particles, List<Sound> sounds, int soundMin, int soundMax) {
        public Mood {
            particles = List.copyOf(particles);
            sounds = List.copyOf(sounds);
        }

        public float[] fog() { return new float[]{red, green, blue}; }
    }

    // ---------------------------------------------------------------- the four moods, in the order of DungeonTier
    /** Cold grey-blue dust and drips, far stone settling, now and then a growl a long way off. */
    public static final Mood UPPER_CATACOMBS = new Mood("UPPER_CATACOMBS", 0.10f, 0.12f, 0.16f, 1.0f, 1.0f,
            List.of(new Drift(DUST, 0.5f), new Drift(DRIP, 0.06f)),
            List.of(new Sound("minecraft:block.pointed_dripstone.drip_water", 5, 1.4f, 1.8f, 0.8f, 1.1f),
                    new Sound("minecraft:block.stone.break", 3, 1.6f, 2.0f, 0.5f, 0.7f),
                    new Sound("minecraft:block.gravel.break", 2, 1.5f, 1.9f, 0.5f, 0.8f),
                    new Sound("minecraft:ambient.cave", 2, 1.8f, 2.2f, 0.8f, 1.0f),
                    new Sound("minecraft:entity.wolf.growl", 1, 2.2f, 2.6f, 0.5f, 0.65f)),
            160, 360);

    /** Deep teal water: bubbles, echoing drips, a muffled rumble from far below. */
    public static final Mood DEEP_WATERWAYS = new Mood("DEEP_WATERWAYS", 0.03f, 0.14f, 0.17f, 0.8f, 0.75f,
            List.of(new Drift(BUBBLE, 0.45f), new Drift(DRIP, 0.08f)),
            List.of(new Sound("minecraft:block.water.ambient", 4, 1.5f, 2.0f, 0.8f, 1.0f),
                    new Sound("minecraft:block.bubble_column.bubble_pop", 3, 1.4f, 1.8f, 0.6f, 0.9f),
                    new Sound("minecraft:block.bubble_column.upwards_ambient", 2, 1.5f, 1.9f, 0.7f, 1.0f),
                    new Sound("minecraft:block.pointed_dripstone.drip_water", 4, 1.8f, 2.3f, 0.5f, 0.75f),
                    new Sound("minecraft:entity.generic.explode", 1, 1.8f, 2.2f, 0.5f, 0.6f)),
            120, 300);

    /** Red-black smoke and rising embers, lava popping, steam hissing. */
    public static final Mood VOLCANIC_VENTS = new Mood("VOLCANIC_VENTS", 0.20f, 0.05f, 0.03f, 0.5f, 0.55f,
            List.of(new Drift(SMOKE, 0.3f), new Drift(EMBER, 0.4f)),
            List.of(new Sound("minecraft:block.lava.pop", 5, 1.5f, 2.0f, 0.8f, 1.2f),
                    new Sound("minecraft:block.lava.ambient", 3, 1.5f, 2.0f, 0.8f, 1.0f),
                    new Sound("minecraft:block.fire.extinguish", 3, 1.4f, 1.8f, 0.6f, 0.9f),
                    new Sound("minecraft:block.lava.extinguish", 2, 1.5f, 1.9f, 0.6f, 0.8f),
                    new Sound("minecraft:block.fire.ambient", 2, 1.4f, 1.8f, 0.7f, 1.0f)),
            100, 260);

    /** A warm gold haze, soft amethyst chimes over a low sacred hum, motes of gold dust. */
    public static final Mood GOLDEN_CITY = new Mood("GOLDEN_CITY", 0.34f, 0.25f, 0.08f, 0.6f, 0.9f,
            List.of(new Drift(GOLD, 0.55f), new Drift(GLINT, 0.12f)),
            List.of(new Sound("minecraft:block.amethyst_block.chime", 5, 1.6f, 2.1f, 0.9f, 1.5f),
                    new Sound("minecraft:block.amethyst_block.resonate", 2, 1.7f, 2.2f, 0.7f, 1.0f),
                    new Sound("minecraft:block.beacon.ambient", 3, 2.0f, 2.5f, 0.5f, 0.7f),
                    new Sound("minecraft:block.conduit.ambient", 2, 1.8f, 2.3f, 0.6f, 0.8f)),
            140, 320);

    private static final Mood[] MOODS = {UPPER_CATACOMBS, DEEP_WATERWAYS, VOLCANIC_VENTS, GOLDEN_CITY};

    /** What a wolf-like or big-cat apex growls, and what a Basilisk sounds like, from far off in the dark. */
    public static final List<Sound> DIREWOLF_GROWLS = List.of(
            new Sound("minecraft:entity.wolf.growl", 4, 2.4f, 2.9f, 0.5f, 0.65f),
            new Sound("minecraft:entity.ravager.ambient", 1, 2.2f, 2.6f, 0.6f, 0.8f));
    public static final List<Sound> BASILISK_GROWLS = List.of(
            new Sound("minecraft:entity.ender_dragon.growl", 3, 2.4f, 2.9f, 0.5f, 0.65f),
            new Sound("minecraft:entity.ravager.roar", 2, 2.4f, 2.9f, 0.55f, 0.75f));

    /** The mood of the tier with this ordinal ({@code DungeonTier#ordinal()}); a tier without a mood of its own gets the Upper Catacombs'. */
    public static Mood mood(int tierOrdinal) { return tierOrdinal >= 0 && tierOrdinal < MOODS.length ? MOODS[tierOrdinal] : MOODS[0]; }

    /** The mood of the tier with this name ({@code DungeonTier#name()}), or the Upper Catacombs' if none matches. */
    public static Mood mood(String tierName) {
        for (Mood m : MOODS) if (m.tier().equals(tierName)) return m;
        return MOODS[0];
    }

    /** How many moods are written (one per tier of {@link DungeonTier} at the time of writing). */
    public static int moodCount() { return MOODS.length; }

    // ---------------------------------------------------------------- smoothing
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** A colour a fraction t of the way from a to b, channel by channel. */
    public static float[] lerpColor(float[] a, float[] b, float t) {
        int n = Math.min(a.length, b.length);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = lerp(a[i], b[i], t);
        return out;
    }

    /** The share of the remaining distance a value that approaches its target exponentially covers in dt seconds (tau seconds to cover 63%, about 3 tau for 95%). */
    public static float smoothing(float dtSeconds, float tauSeconds) {
        if (tauSeconds <= 0f) return 1f;
        if (dtSeconds <= 0f) return 0f;
        return 1f - (float) Math.exp(-dtSeconds / tauSeconds);
    }

    /** One step of an exponential approach of current toward target. */
    public static float approach(float current, float target, float dtSeconds, float tauSeconds) { return lerp(current, target, smoothing(dtSeconds, tauSeconds)); }

    // ---------------------------------------------------------------- deterministic picks from a seed
    /** A well mixed 64-bit value from any seed (splitmix64's finaliser). */
    public static long mix(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A float in [0, 1) from a seed. */
    public static float unit(long seed) { return (mix(seed) >>> 40) / 16777216f; }

    /** A whole number in [min, max] from a seed (min if the range is empty). */
    public static int between(int min, int max, long seed) {
        if (max <= min) return min;
        return min + (int) Math.floorMod(mix(seed), (long) (max - min + 1));
    }

    /** A float in [min, max] from a seed. */
    public static float between(float min, float max, long seed) { return lerp(min, max, unit(seed)); }

    /** A sound picked by weight, the same one for the same seed; null if the table has nothing to pick. */
    public static Sound pick(List<Sound> sounds, long seed) {
        long total = 0;
        for (Sound s : sounds) total += Math.max(0, s.weight());
        if (total <= 0) return null;
        long roll = Math.floorMod(mix(seed), total);
        for (Sound s : sounds) {
            roll -= Math.max(0, s.weight());
            if (roll < 0) return s;
        }
        return sounds.get(sounds.size() - 1);
    }

    /** Ticks until a tier's next ambient sound. */
    public static int soundInterval(Mood mood, long seed) { return between(mood.soundMin(), mood.soundMax(), seed); }

    /** A spot on a ring round the listener: {dx, dz, distance}, the direction random and the distance between min and max. */
    public static double[] ring(long seed, double minDistance, double maxDistance) {
        double angle = unit(seed) * Math.PI * 2.0;
        double distance = minDistance + unit(seed ^ 0x5DEECE66DL) * (maxDistance - minDistance);
        return new double[]{Math.cos(angle) * distance, Math.sin(angle) * distance, distance};
    }

    /** The volume to play a sound at so that it still reaches a listener this far away: never less than asked, and the range (16 per unit) always past the distance. */
    public static float audibleVolume(float volume, double distance) {
        return Math.min(MAX_VOLUME, Math.max(volume, (float) (distance / 16.0) + 0.35f));
    }

    /** How many particles of a kind to add this tick: the whole part of the rate, plus one if the roll (0..1) falls under its fraction; never more than the cap. */
    public static int particleCount(float rate, float roll) {
        if (rate <= 0f) return 0;
        int whole = (int) Math.floor(rate);
        return Math.min(MAX_PARTICLES_PER_KIND, whole + (roll < rate - whole ? 1 : 0));
    }
}
