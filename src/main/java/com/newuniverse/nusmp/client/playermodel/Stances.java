package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: a goblin does not stand like a paladin and a ghoul does not walk like a priest, and on the player model every person has the same spine and the same arms. A look names a STANCE
 * ({@code "stance"} in the table), a posture laid over the vanilla animation as a {@link PoseState} before the entity's own body adds its poses: {@code hunch} (the back bends and the head
 * juts forward, the arms hang a little in front: goblins, kobolds, fish-folk), {@code zombie} (the arms reach forward, high while the creature hunts, low and swaying while it shambles: the
 * dead). The weaker pull of a stance gives way to any pull of the body itself (a cast, a talk), and the arms are left alone during a swing so the blow keeps its vanilla animation. Pure, tested.
 */
public final class Stances {
    private Stances() {}

    public static final String NONE = "", HUNCH = "hunch", ZOMBIE = "zombie";

    public static boolean known(String s) { return s == null || s.isEmpty() || s.equals(HUNCH) || s.equals(ZOMBIE); }

    /**
     * @param aggressive the creature is hunting something (the mob's aggressive flag)
     * @param swinging   it is in the middle of a swing of the main arm
     * @param moving     it is walking or running
     * @param age        {@code tickCount + partial tick}
     */
    public static void apply(String stance, PoseState p, boolean aggressive, boolean swinging, boolean moving, float age) {
        if (stance == null || stance.isEmpty()) return;
        switch (stance) {
            case ZOMBIE -> {
                float sway = (float) Math.sin(age * 0.09f) * 0.045f;
                float k = aggressive ? 0.85f : 0.4f;
                float up = aggressive ? -1.45f : -0.5f;
                if (!swinging) {
                    p.pull(PoseState.ARM_R, k, up + sway, 0.07f, 0.05f);
                    p.pull(PoseState.ARM_L, k, up - sway, -0.07f, -0.05f);
                }
                p.add(PoseState.HEAD, 0.1f, 0f, 0f);
                p.add(PoseState.BODY, 0.1f, 0f, 0f);
            }
            case HUNCH -> {
                float lean = 0.26f + (moving ? 0.08f : 0f);
                p.add(PoseState.BODY, lean, 0f, 0f);
                p.add(PoseState.HEAD, -lean * 0.85f, 0f, 0f);
                if (!swinging) {
                    p.add(PoseState.ARM_R, 0.3f, 0f, 0f);
                    p.add(PoseState.ARM_L, 0.3f, 0f, 0f);
                }
            }
            default -> { }
        }
    }
}
