package com.newuniverse.nusmp.entity.riven;

/** The animation clips of assets/nusmp/animations/entity/riven_remake.animation.json, by synced id, and how many ticks each plays. Pure data. */
public final class RivenClips {
    private RivenClips() {}

    public static final String[] NAMES = {"idle", "walk", "talk", "cast_grimoire", "cast_song", "eldritch_blast", "shadow_step", "manifest_weapon",
            "manifest_shield", "soul_bond", "sword_combo_1", "sword_combo_2", "sword_combo_3", "hit", "stagger", "phase2", "final_form", "death"};
    public static final int[] TICKS = {0, 0, 24, 16, 32, 9, 7, 12, 10, 14, 8, 8, 8, 6, 16, 28, 44, 40};

    public static int idOf(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        return -1;
    }
}
