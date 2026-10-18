package com.newuniverse.nusmp.client.remodel;

import java.util.Map;

/**
 * Why: the numbers of every remodelled model, copied from tools/arc/manifest.json ("arc_look") by tools/remodel/gen_table.py so the renderers need no file to read: the height the model was
 * built to, the blocks of ground one cycle of its walk and its run covers (the feet must not slide), the shadow, and the box that keeps a wing or a horn from popping at the screen edge.
 * GENERATED: run python3 -B tools/remodel/gen_table.py after rebuilding a character, never edit by hand (RemodelLooksTest fails when it drifts from the manifest).
 */
final class RemodelLooks {
    private RemodelLooks() {}

    /** The facts of one model (blocks). */
    record Look(float height, float walkCycle, float runCycle, float shadow, float growXz, float growUp) {}

    /** Used when a model is not listed (never for a remodelled one: the test checks them all). */
    static final Look FALLBACK = new Look(2.0f, 1.4f, 2.4f, 0.45f, 1.2f, 1.0f);

    static final Map<String, Look> BY_MODEL = Map.ofEntries(
            Map.entry("basilisk", new Look(1.56f, 0.94f, 1.64f, 0.5f, 1.74f, 0.86f)),
            Map.entry("blood_beast", new Look(1.5f, 0.88f, 1.54f, 0.45f, 1.26f, 0.74f)),
            Map.entry("charybdis_spawn", new Look(3.19f, 1.63f, 2.97f, 1.3f, 2.77f, 1.67f)),
            Map.entry("charybdis_spawn_form2", new Look(3.19f, 1.63f, 2.97f, 1.3f, 2.77f, 1.67f)),
            Map.entry("cotton_cloud", new Look(1.41f, 0.75f, 1.29f, 0.95f, 1.86f, 0.4f)),
            Map.entry("cotton_sheep", new Look(1.91f, 1.11f, 1.97f, 0.47f, 1.17f, 0.64f)),
            Map.entry("cotton_sheep_cook", new Look(1.91f, 1.11f, 1.97f, 0.48f, 1.19f, 0.64f)),
            Map.entry("dante_zogratis", new Look(2.0f, 1.16f, 2.06f, 0.42f, 1.09f, 0.69f)),
            Map.entry("dante_zogratis_form2", new Look(2.0f, 1.16f, 2.06f, 0.42f, 1.09f, 1.35f)),
            Map.entry("dark_disciple", new Look(1.91f, 0.81f, 1.5f, 0.52f, 1.25f, 0.45f)),
            Map.entry("devil_avatar", new Look(2.84f, 1.37f, 2.53f, 0.4f, 2.79f, 1.38f)),
            Map.entry("devil_avatar_form2", new Look(2.84f, 1.37f, 2.53f, 0.76f, 2.79f, 1.68f)),
            Map.entry("dungeon_direwolf", new Look(1.91f, 1.06f, 1.88f, 0.46f, 1.58f, 0.83f)),
            Map.entry("dungeon_slime", new Look(1.41f, 0.75f, 1.29f, 0.35f, 1.07f, 0.72f)),
            Map.entry("floor_warden", new Look(3.19f, 1.63f, 2.97f, 0.65f, 1.43f, 0.8f)),
            Map.entry("god_of_order", new Look(3.19f, 1.58f, 2.92f, 0.44f, 1.12f, 3.09f)),
            Map.entry("god_of_order_form2", new Look(3.19f, 1.58f, 2.92f, 0.83f, 1.86f, 3.09f)),
            Map.entry("goliath", new Look(6.0f, 3.3f, 6.01f, 1.23f, 2.26f, 0.4f)),
            Map.entry("goliath_form2", new Look(6.0f, 3.3f, 6.01f, 1.23f, 2.26f, 0.4f)),
            Map.entry("grimoire_daemon", new Look(1.5f, 0.59f, 1.09f, 0.44f, 1.12f, 0.46f)),
            Map.entry("grimoire_daemon_arch", new Look(3.19f, 1.62f, 2.99f, 1.34f, 2.42f, 1.66f)),
            Map.entry("grimoire_daemon_greater", new Look(2.31f, 1.07f, 1.97f, 0.47f, 1.18f, 0.97f)),
            Map.entry("grimoire_daemon_stone", new Look(1.69f, 0.7f, 1.29f, 0.35f, 0.99f, 0.45f)),
            Map.entry("living_armor", new Look(1.94f, 1.11f, 1.97f, 0.48f, 1.18f, 0.83f)),
            Map.entry("lucius_zogratis", new Look(2.09f, 0.92f, 1.7f, 0.63f, 1.41f, 0.71f)),
            Map.entry("lucius_zogratis_form2", new Look(2.09f, 0.92f, 1.7f, 1.27f, 2.31f, 1.51f)),
            Map.entry("painted_beast", new Look(1.5f, 0.81f, 1.43f, 0.35f, 1.26f, 0.74f)),
            Map.entry("painted_giant", new Look(3.91f, 2.05f, 3.73f, 0.75f, 1.57f, 0.4f)),
            Map.entry("painted_knight", new Look(1.94f, 1.16f, 2.06f, 0.4f, 1.08f, 0.86f)),
            Map.entry("rouge", new Look(1.41f, 0.75f, 1.29f, 0.35f, 1.72f, 0.68f)),
            Map.entry("spirit_black_devil", new Look(1.41f, 0.77f, 1.35f, 0.62f, 1.61f, 0.96f)),
            Map.entry("spirit_gnome", new Look(3.0f, 1.5f, 2.74f, 0.67f, 1.46f, 0.4f)),
            Map.entry("spirit_salamander", new Look(1.91f, 1.06f, 1.88f, 0.9f, 1.88f, 1.03f)),
            Map.entry("spirit_sylph", new Look(1.41f, 0.77f, 1.35f, 0.67f, 1.45f, 0.82f)),
            Map.entry("spirit_undine", new Look(2.0f, 0.88f, 1.63f, 0.35f, 0.97f, 0.49f)),
            Map.entry("tower_wizard", new Look(1.91f, 0.81f, 1.5f, 0.4f, 1.07f, 1.47f)),
            Map.entry("vanica_zogratis", new Look(1.91f, 1.16f, 2.06f, 0.35f, 0.91f, 0.68f)),
            Map.entry("vanica_zogratis_form2", new Look(1.91f, 1.16f, 2.06f, 0.5f, 1.22f, 1.37f)),
            Map.entry("zagred_legacy", new Look(2.19f, 1.35f, 2.41f, 1.16f, 2.49f, 1.34f)),
            Map.entry("zagred_legacy_form2", new Look(2.19f, 1.35f, 2.41f, 1.16f, 2.49f, 2.05f)),
            Map.entry("zenon_zogratis", new Look(2.0f, 1.21f, 2.15f, 0.35f, 0.97f, 0.6f)),
            Map.entry("zenon_zogratis_form2", new Look(2.0f, 1.21f, 2.15f, 0.46f, 1.15f, 0.86f))
    );

    /** The facts of a model name such as {@code dante_zogratis} or {@code dante_zogratis_form2}. */
    static Look of(String model) {
        Look l = BY_MODEL.get(model);
        return l == null ? FALLBACK : l;
    }
}
