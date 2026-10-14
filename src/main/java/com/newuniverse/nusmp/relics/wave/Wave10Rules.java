package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Relics of Many Worlds, wave 10 (ten weapons: Krakeblade, Unicorn Dagger, Rune Splitter, Junglethorn Whip, Needler, Sidras' swearing sword,
 * Stegosaurus Greatsword, Jade Xiphos, Returning Hand Axe, Dead Blade): the pure rules, with no Minecraft types, so they are unit-tested. The
 * cards are tabletop; this is where their rolls, saves and areas become Minecraft numbers.
 */
public final class Wave10Rules {
    private Wave10Rules() {}

    // ---------------------------------------------------------------- Unicorn Dagger: the d8 buff
    /** The eight buffs of the d8 (index 0 = a roll of 1): mob effect id, amplifier, duration in ticks. */
    public record Buff(String effect, int amp, int ticks) {}

    private static final Buff[] D8 = {
            new Buff("minecraft:strength", 0, 1200),
            new Buff("minecraft:speed", 0, 1800),
            new Buff("minecraft:regeneration", 0, 600),
            new Buff("minecraft:resistance", 0, 1200),
            new Buff("minecraft:jump_boost", 1, 1800),
            new Buff("minecraft:haste", 1, 1800),
            new Buff("minecraft:absorption", 1, 1800),
            new Buff("tensura:magicule_regeneration", 0, 1800),
    };

    /** The buff of a d8 roll, 1 to 8 (any other number is wrapped into range). */
    public static Buff buff(int d8) { return D8[Math.floorMod(d8 - 1, D8.length)]; }

    public static int buffCount() { return D8.length; }

    /** A poke is 1 damage that can never kill: it is skipped for a target on 2 health or less. */
    public static boolean pokeHurts(float health) { return health > 2.0f; }

    // ---------------------------------------------------------------- Wall of Water (Krakeblade)
    public static final double WALL_HALF_WIDTH = 4.5, WALL_THICK = 1.4, WALL_HEIGHT = 4.0, WALL_DISTANCE = 4.0;
    public static final int WALL_TICKS = 160;

    /**
     * Is the point inside the wall? The wall stands on (cx, cy, cz), faces along the unit vector (dx, dz) (the way the caster looked) and is
     * WALL_HALF_WIDTH to each side, WALL_THICK deep and WALL_HEIGHT tall.
     */
    public static boolean inWall(double px, double py, double pz, double cx, double cy, double cz, double dx, double dz) {
        if (py < cy - 0.5 || py > cy + WALL_HEIGHT) return false;
        double rx = px - cx, rz = pz - cz;
        double along = rx * dx + rz * dz, across = rx * -dz + rz * dx;
        return Math.abs(along) <= WALL_THICK * 0.5 && Math.abs(across) <= WALL_HALF_WIDTH;
    }

    /** Which side of the wall a point is on: +1 on the side the caster faces, -1 behind it, 0 on the plane. */
    public static int wallSide(double px, double pz, double cx, double cz, double dx, double dz) {
        double along = (px - cx) * dx + (pz - cz) * dz;
        return along > 0.05 ? 1 : (along < -0.05 ? -1 : 0);
    }

    // ---------------------------------------------------------------- Returning Hand Axe: the way back
    /** Distance from a point to the segment a-b. */
    public static double distToSegment(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double vx = bx - ax, vy = by - ay, vz = bz - az;
        double len2 = vx * vx + vy * vy + vz * vz;
        double t = len2 < 1.0e-9 ? 0.0 : ((px - ax) * vx + (py - ay) * vy + (pz - az) * vz) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        double qx = ax + vx * t - px, qy = ay + vy * t - py, qz = az + vz * t - pz;
        return Math.sqrt(qx * qx + qy * qy + qz * qz);
    }

    /** The axe is out for this long after a throw: the return trip must be called inside the time. */
    public static final int AXE_AWAY_TICKS = 160;

    // ---------------------------------------------------------------- Rune Splitter: Dispel Magic
    /**
     * Does a magical effect of this amplifier end? Level III or lower (amplifier 0 and 1) always do; above that it takes a roll (0 to 1), and an
     * infernal target (a devil, a demon, a daemon) is harder to strip. {@code roll} is a uniform number from 0 to 1.
     */
    public static boolean dispelled(int amplifier, boolean infernal, double roll) {
        if (amplifier <= 1) return true;
        double chance = infernal ? 0.30 : 0.60;
        return roll < chance;
    }

    /** A creature of the abyss: its registry path names a devil, demon or daemon. */
    public static boolean infernalPath(String entityPath) {
        if (entityPath == null) return false;
        String s = entityPath.toLowerCase(Locale.ROOT);
        return s.contains("devil") || s.contains("demon") || s.contains("daemon") || s.contains("imp") || s.contains("hell") || s.contains("fiend");
    }

    /** The six colours of the axe's light, cycled each time it is lit. */
    public static final int[] LIGHT_COLOURS = {0xFFE27A, 0xFF8A3A, 0xFF5A7A, 0x8A7CFF, 0x5AD0FF, 0x7CFFB0};

    public static int lightColour(int index) { return LIGHT_COLOURS[Math.floorMod(index, LIGHT_COLOURS.length)]; }

    public static final double LIGHT_RADIUS = 10.0;

    // ---------------------------------------------------------------- Sidras' swearing sword
    public static final int WIS_DC = 13;

    private static final String[] INSULTS = {
            "You rotting... ehrm... %s!",
            "You... you big... smelly %s!",
            "Your mother was a... a... well, a %s!",
            "Ha! What a pathetic... err... %s!",
            "You, sir, are a... a... %s-faced... thing!",
            "I have seen better... ehh... %ss in a swamp!",
            "Come on, %s, is that all you've got? Yes, probably.",
            "Look at you, you stupid... %s... thing!",
    };

    /** The sword's insult for a creature of this name (the pick is any non-negative number). */
    public static String insult(String targetName, int pick) {
        String n = targetName == null || targetName.isBlank() ? "thing" : targetName;
        return String.format(INSULTS[Math.floorMod(pick, INSULTS.length)], n);
    }

    /** 1d4 of psychic damage, from a roll of 0 to 3. */
    public static float psychic(int roll0to3) { return 1 + Math.floorMod(roll0to3, 4); }

    // ---------------------------------------------------------------- Stegosaurus Greatsword
    public static final double FLAIL_REACH_SWORD = 3.2, FLAIL_REACH_WHIP = 5.0;
    public static final int FLAIL_GAP = 30;
    public static final float FRONT_SCALE = 0.4f;

    /** The creature is in the rear arc of the wielder: the dot of the way they face and the way to it is under -0.3 (about 107 degrees round the back). */
    public static boolean inRear(double faceX, double faceZ, double toX, double toZ) {
        double fl = Math.hypot(faceX, faceZ), tl = Math.hypot(toX, toZ);
        if (fl < 1.0e-6 || tl < 1.0e-6) return false;
        return (faceX * toX + faceZ * toZ) / (fl * tl) < -0.3;
    }

    /** The creature is in front of the wielder (more than 90 degrees from the back arc). */
    public static boolean inFront(double faceX, double faceZ, double toX, double toZ) {
        double fl = Math.hypot(faceX, faceZ), tl = Math.hypot(toX, toZ);
        if (fl < 1.0e-6 || tl < 1.0e-6) return true;
        return (faceX * toX + faceZ * toZ) / (fl * tl) > 0.2;
    }

    /** How many creatures one lash can reach: the greatsword one, the whip two. */
    public static int flailTargets(boolean whip) { return whip ? 2 : 1; }

    public static float flailDamage(boolean whip) { return whip ? 5.0f : 7.0f; }

    // ---------------------------------------------------------------- Jade Xiphos
    /** One blow of force, thunder or sound this strong (health points) shatters the blade (the card's 30 points, scaled to 20 health). */
    public static final float SHATTER_DAMAGE = 15.0f;

    public static boolean shatters(float damage, boolean forceOrThunder) { return forceOrThunder && damage >= SHATTER_DAMAGE; }

    /** The shards: 6d10 piercing on the card, here 6 to 20 damage by a roll of 6 dice (each 0 to 9) and the target's size. */
    public static float shardDamage(int sumOfSixD10, boolean stuck) { return Math.min(20f, Math.max(6f, sumOfSixD10 * 0.6f)) * (stuck ? 1.0f : 0.6f); }

    // ---------------------------------------------------------------- Dead Blade: Finger of Death
    /** 7d8 + 30 necrotic on the card; here 14 plus 12% of the target's maximum health, at most 60 (half when it saves). */
    public static float fingerDamage(double maxHealth, boolean saved) {
        float d = (float) Math.min(60.0, 14.0 + Math.max(0.0, maxHealth) * 0.12);
        return saved ? d * 0.5f : d;
    }

    public static final int FINGER_DC = 16;

    /** A creature the Finger of Death can raise: its registry path names a person (villager, illager, piglin, zombie, ...) or a Tensura humanoid. */
    public static boolean humanoidPath(String path) {
        if (path == null) return false;
        String s = path.toLowerCase(Locale.ROOT);
        for (String w : new String[]{"villager", "pillager", "vindicator", "evoker", "illusioner", "witch", "piglin", "zombie", "goblin", "orc", "lizardman", "gnome", "skeleton", "player"}) {
            if (s.contains(w)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Needler
    public static final int NEEDLES = 4;
    public static final float NEEDLE_DAMAGE = 2.2f;
    public static final double NEEDLE_RANGE = 40.0, NEEDLE_SPREAD_DEG = 3.2;

    /** The aim offset (yaw, pitch degrees) of needle i of n: a flat fan, the middle needle dead on when n is odd. */
    public static double[] needleOffset(int i, int n) {
        double mid = (n - 1) / 2.0;
        return new double[]{(i - mid) * NEEDLE_SPREAD_DEG, ((i % 2 == 0) ? 0.5 : -0.5) * NEEDLE_SPREAD_DEG * 0.35};
    }

    // ---------------------------------------------------------------- Junglethorn Whip
    public static final double SWING_RANGE = 24.0, THORN_RANGE = 10.0, DISARM_RANGE = 7.0;

    /** The pull of a swing towards an anchor this far away: a speed in blocks per tick, never more than 1.5. */
    public static double swingSpeed(double dist) { return Math.max(0.5, Math.min(1.5, dist * 0.09 + 0.4)); }
}
