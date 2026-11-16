package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.book.NoelleFormRules;

/**
 * Why: the pure rules of the devil's own form (docs/devil_forms.md), shared by the tests and the client painter ({@code client.devil.DevilFormLayer}). When a player's contract is with a devil of another world
 * the player's body wears that devil's mesh PARTS (entity_meshes/devil_&lt;id&gt;.txt, eleven roles) anchored on the body parts, forged in stage by stage like the Djinn Full Equip: the head piece from 20 %, the arms,
 * wings and tail from 50 %, the whole form (torso and legs) and the glow at 100 % or in the union. No Minecraft type here.
 *
 * <p>Cost: {@link #wanted} gives nothing beyond {@link #FAR_BLOCKS} (48), the full figure up to {@link #FULL_BLOCKS} (24) and the low-detail copy without glow between; the per-frame head count is
 * {@link NoelleFormRules#budgeted} (four full, six far; everybody else nothing). The mesh itself is capped at {@link #MAX_QUADS} quads (the boss mesh cap).
 */
public final class DevilFormRules {
    private DevilFormRules() {}

    /** The eleven role names, in the order of {@code MeshPose.ALL_ROLE_NAMES} (a test pins that). */
    public static final String[] ROLES = {"torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r", "wing_l", "wing_r", "tail"};
    public static final int TORSO = 0, HEAD = 1, ARM_L = 2, ARM_R = 3, FOREARM_L = 4, FOREARM_R = 5, LEG_L = 6, LEG_R = 7, WING_L = 8, WING_R = 9, TAIL = 10, COUNT = 11;

    /** The body parts a role is anchored on (the player model's parts). */
    public static final int A_HEAD = 0, A_BODY = 1, A_ARM_R = 2, A_ARM_L = 3, A_LEG_R = 4, A_LEG_L = 5;

    /** The assimilation levels the stages begin at (the head piece, the arms / wings / tail, the whole form). */
    public static final float HEAD_AT = DevilRules.HORNS_AT, LIMBS_AT = DevilRules.WINGS_AT, FULL_AT = DevilRules.UNION_AT;

    /** The most quads one devil mesh may hold (body and glow together): the boss-mesh cap, kept here so the pure tests need not load the client class. */
    public static final int MAX_QUADS = 3000;

    public static final double FULL_BLOCKS = 24.0, FAR_BLOCKS = 48.0;

    /** The name of the mesh files and skins of a devil: {@code devil_<id>} (entity_meshes/&lt;it&gt;.txt, &lt;it&gt;_lod.txt, textures/entity/boss_mesh/&lt;it&gt;.png and &lt;it&gt;_glow.png). */
    public static String meshId(String devilId) { return "devil_" + devilId; }

    public static String meshPath(String devilId) { return "assets/anime_multiverse/entity_meshes/" + meshId(devilId) + ".txt"; }

    public static String lodPath(String devilId) { return "assets/anime_multiverse/entity_meshes/" + meshId(devilId) + "_lod.txt"; }

    public static String skinPath(String devilId) { return "assets/anime_multiverse/textures/entity/boss_mesh/" + meshId(devilId) + ".png"; }

    public static String glowPath(String devilId) { return "assets/anime_multiverse/textures/entity/boss_mesh/" + meshId(devilId) + "_glow.png"; }

    /** The level a role first shows at: the head 20, the arms, wings and tail 50, the torso and legs 100. */
    public static float threshold(int role) {
        return switch (role) {
            case HEAD -> HEAD_AT;
            case TORSO, LEG_L, LEG_R -> FULL_AT;
            default -> LIMBS_AT;
        };
    }

    /** Whether the role is worn at this level (the union wears every role, and so does a level of 100). */
    public static boolean wants(int role, float level, boolean union) { return union || level >= threshold(role); }

    /** The stage of a level: 0 nothing but the markings, 1 the head, 2 arms, wings and tail, 3 the whole form. */
    public static int stage(float level, boolean union) { return union || level >= FULL_AT ? 3 : level >= LIMBS_AT ? 2 : level >= HEAD_AT ? 1 : 0; }

    /** The body part a role is anchored on: the arms and forearms on their arm, the legs on theirs, the wings and the tail on the back (the body). */
    public static int anchorOf(int role) {
        return switch (role) {
            case HEAD -> A_HEAD;
            case ARM_L, FOREARM_L -> A_ARM_L;
            case ARM_R, FOREARM_R -> A_ARM_R;
            case LEG_L -> A_LEG_L;
            case LEG_R -> A_LEG_R;
            default -> A_BODY;
        };
    }

    /** The role whose pivot the body part's turn is about: the forearm turns about its arm's pivot (the shoulder), the wings and the tail about the torso's, everything else about its own. */
    public static int leaderOf(int role) {
        return switch (role) {
            case FOREARM_L -> ARM_L;
            case FOREARM_R -> ARM_R;
            case WING_L, WING_R, TAIL -> TORSO;
            default -> role;
        };
    }

    /** The stagger of a role's forging, in ticks (0 to 12, as the Djinn equip). */
    public static float delay(int role) {
        return switch (role) {
            case HEAD -> 0f;
            case TORSO -> 1f;
            case ARM_L, FOREARM_L -> 2f;
            case ARM_R, FOREARM_R -> 3f;
            case LEG_L -> 6f;
            case LEG_R -> 7f;
            case WING_L -> 8f;
            case WING_R -> 9f;
            default -> 10f;
        };
    }

    /** Where a role flies in from, in model pixels in the mesh's frame (y up, front +z): {@code dir(role)[0..2]}. */
    private static final float[][] FLY = {
            {0f, 0f, 14f}, {0f, 16f, 0f}, {14f, 4f, 0f}, {-14f, 4f, 0f}, {12f, -2f, 6f}, {-12f, -2f, 6f}, {3f, -14f, 6f}, {-3f, -14f, 6f}, {8f, 4f, -14f}, {-8f, 4f, -14f}, {0f, -8f, -14f}};

    public static float fly(int role, int axis) { return FLY[role][axis]; }

    /** The detail a distance asks for, before the crowd budget: nothing beyond 48 blocks, the far copy between 24 and 48, the full figure inside 24. */
    public static NoelleFormRules.Lod wanted(double dist2) {
        if (dist2 > FAR_BLOCKS * FAR_BLOCKS) return NoelleFormRules.Lod.NONE;
        return dist2 > FULL_BLOCKS * FULL_BLOCKS ? NoelleFormRules.Lod.FAR : NoelleFormRules.Lod.STANDARD;
    }

    /** The glow of the form: faint while it is partly on, full and pulsing once the whole form is (level 100 or the union). */
    public static float glow(float level, boolean union, float age) {
        if (union || level >= FULL_AT) return 0.85f + 0.15f * (float) Math.sin(age * 0.2f);
        return level >= LIMBS_AT ? 0.35f : level >= HEAD_AT ? 0.2f : 0f;
    }

    // ---------------------------------------------------------------- the attribute's own effect (at 100 % and in the union)
    /** The ground / aura ring: 0 plain glow, 1 air, 2 water, 3 flame, 4 earth (the particle textures the Djinn dives use). */
    public static int ringOf(DevilAttribute a) {
        return switch (a) {
            case SPATIAL, ANTI_MAGIC, FORESIGHT, ETHER -> 1;
            case WATER, GHOST -> 2;
            case FLAME -> 3;
            case GRAVITY, EARTH, BEAST, ROT -> 4;
            default -> 0;
        };
    }

    /** How the motes move: 0 rise, 1 fall, 2 orbit the body, 3 drift. */
    public static int moteMode(DevilAttribute a) {
        return switch (a) {
            case FLAME, SOUL, LIGHT, WATER -> 0;
            case GRAVITY, CURSE, ROT, EARTH -> 1;
            case SPATIAL, ANTI_MAGIC, WORD, TIME, FORESIGHT, ETHER -> 2;
            default -> 3;
        };
    }

    /** Motes of the attribute's effect: six at the full level of detail inside the union or at 100 %, none otherwise. */
    public static int motes(NoelleFormRules.Lod lod, boolean whole) { return whole && lod == NoelleFormRules.Lod.STANDARD ? 6 : 0; }
}
