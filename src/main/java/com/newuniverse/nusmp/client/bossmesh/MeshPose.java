package com.newuniverse.nusmp.client.bossmesh;

/**
 * Boss rework wave 2: the procedural animation of a Blender-made boss body (pure, unit tested, no Minecraft class). Instead of imported skeletal clips, each part is turned
 * about its pivot by a few curves of time: breathing at rest, a walk swing paced by the ground covered, and one curve per kind of blow, played over the progress of the
 * clip the boss's renderer already chooses (the wind-up ends at 0.8, the blow lands there, the rest is the settling, as {@code ZogratisPose.CAST_BIG_RELEASE}).
 *
 * <p>Parts are found by role name ({@link #ROLE_NAMES}); a model without a role simply does not move that part. Angles are radians about the part's own pivot, X then Y then Z,
 * in model space (y up, face +z, its left +x): a negative x turn swings a hanging arm or leg forward. Every curve starts and ends at rest, so a change of action never jumps.
 * {@link #solve} allocates nothing.
 */
public final class MeshPose {
    public static final int TORSO = 0, HEAD = 1, ARM_L = 2, ARM_R = 3, FOREARM_L = 4, FOREARM_R = 5, LEG_L = 6, LEG_R = 7, ROLES = 8;
    public static final String[] ROLE_NAMES = {"torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r"};
    /**
     * Boss rework wave 3: three more roles a body may have (appended; ROLES and ROLE_NAMES stay the eight every body has): the wings of Lucifero, the tail of the Charybdis Spawn.
     * A mesh without them simply does not move them.
     */
    public static final int WING_L = 8, WING_R = 9, TAIL = 10, ALL_ROLES = 11;
    public static final String[] ALL_ROLE_NAMES = {"torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r", "wing_l", "wing_r", "tail"};

    /** What the body is doing: picked from the renderer's clip by {@link #action}. */
    public static final int NONE = 0, SWING = 1, SLAM = 2, STOMP = 3, ROAR = 4, PUNCH = 5, HURL = 6, RISE = 7, DEATH = 8, FLINCH = 9,
            /** Wave 3 (appended): the staff levelled at a target (the God of Order's beam), a bow. */
            BEAM = 10, BOW = 11,
            /** Kakashi boss (docs/bosses/kakashi.md, appended): a low lunge with the right hand crackling, the hand seals, a throw, the eye hand, the arms spread over the Susanoo. */
            RAIKIRI = 12, SEALS = 13, THROW = 14, KAMUI_EYE = 15, SUSANOO = 16;

    /** rx, ry, rz per role. */
    public final float[] rot = new float[ALL_ROLES * 3];
    /** The whole body: lift (model units) and a forward tip (radians, about the feet). */
    public float rootY, rootPitch;

    /** The action of a clip name (the Zogratis and Goliath clip names); NONE for idle, walk, run and anything unknown. */
    public static int action(String clip) {
        if (clip == null) return NONE;
        return switch (clip) {
            case "attack" -> SWING;
            case "slam", "cast_big", "cast" -> SLAM;
            case "stomp" -> STOMP;
            case "roar", "transform", "read" -> ROAR;
            case "beam" -> BEAM;
            case "bow" -> BOW;
            case "fist", "line" -> PUNCH;
            case "rubble" -> HURL;
            case "rise", "spawn" -> RISE;
            case "death" -> DEATH;
            case "hurt" -> FLINCH;
            default -> NONE;
        };
    }

    /**
     * Works out the pose of one frame. {@code age} in ticks (breathing), {@code walkPhase} 0..1 of the stride, {@code walk} 0..1 how much it walks, {@code action} and
     * {@code t} 0..1 its progress (the blow lands at 0.8).
     */
    public void solve(float age, float walkPhase, float walk, int action, float t) {
        java.util.Arrays.fill(rot, 0f);
        rootY = 0f;
        rootPitch = 0f;
        // ---- breathing: the chest rises, the arms drift out a little, the head sways
        float br = (float) Math.sin(age * 0.07f);
        add(TORSO, 0, -0.025f * br);
        add(HEAD, 0, 0.03f * br);
        add(HEAD, 1, 0.06f * (float) Math.sin(age * 0.021f));
        add(ARM_L, 2, -0.035f - 0.025f * br);
        add(ARM_R, 2, 0.035f + 0.025f * br);
        add(FOREARM_L, 0, -0.12f);
        add(FOREARM_R, 0, -0.12f);
        rootY += 0.35f * br;
        add(WING_L, 2, 0.10f + 0.07f * br);                                // wave 3: wings beat slowly, the tail sways (a body without them ignores these)
        add(WING_R, 2, -0.10f - 0.07f * br);
        add(TAIL, 1, 0.10f * (float) Math.sin(age * 0.05f));
        // ---- the walk: legs and arms swing against each other, the torso rolls and bobs
        if (walk > 0.001f) {
            float a = (float) (walkPhase * Math.PI * 2), s = (float) Math.sin(a), c = (float) Math.cos(a);
            add(LEG_L, 0, walk * 0.45f * s);
            add(LEG_R, 0, -walk * 0.45f * s);
            add(ARM_L, 0, -walk * 0.32f * s);
            add(ARM_R, 0, walk * 0.32f * s);
            add(FOREARM_L, 0, -walk * (0.18f + 0.12f * Math.max(0f, -s)));
            add(FOREARM_R, 0, -walk * (0.18f + 0.12f * Math.max(0f, s)));
            add(TORSO, 0, walk * 0.06f);
            add(TORSO, 1, walk * 0.07f * s);
            add(TORSO, 2, walk * 0.045f * s);
            add(HEAD, 1, -walk * 0.05f * s);
            rootY -= walk * 1.4f * Math.abs(c);
            add(WING_L, 2, walk * 0.18f * s);
            add(WING_R, 2, -walk * 0.18f * s);
            add(TAIL, 1, walk * 0.40f * s);
        }
        if (action == NONE) return;
        t = clamp01(t);
        switch (action) {
            case SWING -> {                                                   // a backhand of the right fist: raised, then swept down and across
                float up = smooth(0f, 0.3f, t) * (1f - smooth(0.32f, 0.5f, t)), hit = smooth(0.32f, 0.5f, t) * (1f - smooth(0.6f, 1f, t));
                add(ARM_R, 0, -2.0f * up - 0.9f * hit);
                add(ARM_R, 2, 0.3f * up - 0.5f * hit);
                add(FOREARM_R, 0, -0.6f * up);
                add(TORSO, 1, -0.25f * up + 0.35f * hit);
            }
            case SLAM -> {                                                    // both fists high, then down on the floor at 0.8
                float up = smooth(0f, 0.72f, t) * (1f - smooth(0.76f, 0.82f, t)), down = smooth(0.76f, 0.82f, t) * (1f - smooth(0.86f, 1f, t));
                add(ARM_L, 0, -2.6f * up - 0.9f * down);
                add(ARM_R, 0, -2.6f * up - 0.9f * down);
                add(ARM_L, 2, -0.2f * up);
                add(ARM_R, 2, 0.2f * up);
                add(FOREARM_L, 0, -0.5f * up);
                add(FOREARM_R, 0, -0.5f * up);
                add(TORSO, 0, -0.12f * up + 0.42f * down);
                add(HEAD, 0, -0.3f * up + 0.1f * down);
                rootY += 1.5f * up - 2.0f * down;
                add(WING_L, 2, 0.6f * up);
                add(WING_R, 2, -0.6f * up);
            }
            case STOMP -> {                                                   // the right leg high, stamped down at 0.8
                float up = smooth(0f, 0.72f, t) * (1f - smooth(0.76f, 0.81f, t)), down = smooth(0.76f, 0.81f, t) * (1f - smooth(0.85f, 1f, t));
                add(LEG_R, 0, -0.95f * up);
                add(LEG_R, 2, 0.15f * up);
                add(TORSO, 0, -0.12f * up + 0.12f * down);
                add(TORSO, 2, 0.08f * up);
                add(ARM_L, 2, -0.35f * up);
                add(ARM_R, 2, 0.35f * up);
                rootY -= 2.2f * down;
            }
            case ROAR -> {                                                    // chest out, arms wide, head thrown back
                float k = smooth(0f, 0.45f, t) * (1f - smooth(0.85f, 1f, t)), shake = 0.04f * k * (float) Math.sin(age * 2.1f);
                add(ARM_L, 2, -1.05f * k);
                add(ARM_R, 2, 1.05f * k);
                add(ARM_L, 0, -0.35f * k);
                add(ARM_R, 0, -0.35f * k);
                add(FOREARM_L, 0, -0.55f * k);
                add(FOREARM_R, 0, -0.55f * k);
                add(TORSO, 0, -0.16f * k + shake);
                add(HEAD, 0, -0.5f * k + shake);
                add(WING_L, 2, 0.7f * k);
                add(WING_R, 2, -0.7f * k);
            }
            case PUNCH -> {                                                   // the right fist drawn back, then driven down and forward into the floor
                float back = smooth(0f, 0.7f, t) * (1f - smooth(0.74f, 0.8f, t)), hit = smooth(0.74f, 0.8f, t) * (1f - smooth(0.86f, 1f, t));
                add(ARM_R, 0, 0.65f * back - 1.35f * hit);
                add(FOREARM_R, 0, -0.9f * back - 0.1f * hit);
                add(TORSO, 1, 0.38f * back - 0.3f * hit);
                add(TORSO, 0, 0.36f * hit);
                add(ARM_L, 0, -0.3f * back);
                rootY -= 1.6f * hit;
            }
            case HURL -> {                                                    // a boulder raised overhead in the right hand, thrown at 0.8
                float up = smooth(0f, 0.7f, t) * (1f - smooth(0.74f, 0.82f, t)), thr = smooth(0.74f, 0.82f, t) * (1f - smooth(0.86f, 1f, t));
                add(ARM_R, 0, -2.9f * up - 0.8f * thr);
                add(FOREARM_R, 0, -0.9f * up);
                add(TORSO, 0, -0.18f * up + 0.3f * thr);
                add(TORSO, 1, 0.25f * up - 0.2f * thr);
                add(ARM_L, 0, -0.5f * up);
            }
            case RISE -> {                                                    // fists first through the floor, the body after
                float k = 1f - smooth(0.45f, 1f, t);
                add(ARM_L, 0, -2.8f * k);
                add(ARM_R, 0, -2.8f * k);
                add(ARM_L, 2, -0.15f * k);
                add(ARM_R, 2, 0.15f * k);
                add(HEAD, 0, -0.35f * k);
            }
            case DEATH -> {                                                   // sags to the knees and tips forward
                float k = smooth(0f, 1f, t);
                add(TORSO, 0, 0.5f * k);
                add(HEAD, 0, 0.4f * k);
                add(ARM_L, 0, -0.5f * k);
                add(ARM_R, 0, -0.5f * k);
                add(LEG_L, 0, -0.5f * k);
                add(LEG_R, 0, -0.5f * k);
                add(WING_L, 2, -0.7f * k);
                add(WING_R, 2, 0.7f * k);
                rootPitch = 0.75f * k * k;
                rootY -= 6f * k;
            }
            case BEAM -> {                                                    // the right arm levelled at the target and held, the head up
                float k = smooth(0f, 0.25f, t) * (1f - smooth(0.92f, 1f, t));
                add(ARM_R, 0, -1.55f * k);
                add(FOREARM_R, 0, -0.1f * k);
                add(TORSO, 1, 0.12f * k);
                add(HEAD, 0, -0.12f * k);
            }
            case BOW -> {                                                     // a bow from the waist and back up
                float k = (float) Math.sin(Math.PI * t);
                add(TORSO, 0, 0.55f * k);
                add(HEAD, 0, 0.25f * k);
                add(ARM_L, 0, -0.3f * k);
                add(ARM_R, 0, -0.3f * k);
            }
            case RAIKIRI -> {                                                 // crouch, the right hand drawn back at the hip, then driven forward at 0.8
                float back = smooth(0f, 0.7f, t) * (1f - smooth(0.74f, 0.8f, t)), hit = smooth(0.74f, 0.8f, t) * (1f - smooth(0.86f, 1f, t));
                add(ARM_R, 0, 0.5f * back - 1.5f * hit);
                add(FOREARM_R, 0, -0.5f * back);
                add(ARM_L, 0, -0.9f * back);
                add(TORSO, 0, 0.3f * back + 0.25f * hit);
                add(TORSO, 1, 0.3f * back - 0.4f * hit);
                add(LEG_L, 0, -0.45f * back);
                add(LEG_R, 0, 0.35f * back);
                rootY -= 1.8f * back;
            }
            case SEALS -> {                                                   // both hands together in front of the chest, a small shake
                float k = smooth(0f, 0.35f, t) * (1f - smooth(0.9f, 1f, t)), tremble = 0.04f * k * (float) Math.sin(age * 1.7f);
                add(ARM_L, 0, -1.2f * k + tremble);
                add(ARM_R, 0, -1.2f * k - tremble);
                add(ARM_L, 2, 0.5f * k);
                add(ARM_R, 2, -0.5f * k);
                add(FOREARM_L, 0, -1.2f * k);
                add(FOREARM_R, 0, -1.2f * k);
                add(HEAD, 0, 0.12f * k);
            }
            case THROW -> {                                                   // the right arm back, then snapped forward at 0.8
                float back = smooth(0f, 0.7f, t) * (1f - smooth(0.74f, 0.8f, t)), snap = smooth(0.74f, 0.8f, t) * (1f - smooth(0.86f, 1f, t));
                add(ARM_R, 0, 0.9f * back - 1.7f * snap);
                add(FOREARM_R, 0, -0.4f * back);
                add(TORSO, 1, 0.35f * back - 0.45f * snap);
                add(ARM_L, 0, -0.4f * back);
            }
            case KAMUI_EYE -> {                                               // a hand lifted to the brow, the head tilted
                float k = smooth(0f, 0.3f, t) * (1f - smooth(0.9f, 1f, t));
                add(ARM_L, 0, -1.9f * k);
                add(FOREARM_L, 0, -1.1f * k);
                add(HEAD, 2, 0.12f * k);
                add(HEAD, 0, -0.1f * k);
            }
            case SUSANOO -> {                                                 // arms spread wide and held, chest out
                float k = smooth(0f, 0.4f, t) * (1f - smooth(0.9f, 1f, t));
                add(ARM_L, 2, -1.2f * k);
                add(ARM_R, 2, 1.2f * k);
                add(TORSO, 0, -0.14f * k);
                add(HEAD, 0, -0.2f * k);
            }
            case FLINCH -> {
                float k = 1f - t;
                add(TORSO, 0, -0.12f * k);
                add(HEAD, 0, -0.25f * k);
                add(ARM_L, 2, -0.15f * k);
                add(ARM_R, 2, 0.15f * k);
            }
            default -> { }
        }
    }

    public float get(int role, int axis) { return rot[role * 3 + axis]; }

    private void add(int role, int axis, float v) { rot[role * 3 + axis] += v; }

    static float clamp01(float t) { return t < 0f ? 0f : t > 1f ? 1f : t; }

    /** Smoothstep from a to b. */
    static float smooth(float a, float b, float t) {
        float x = clamp01((t - a) / (b - a));
        return x * x * (3f - 2f * x);
    }
}
