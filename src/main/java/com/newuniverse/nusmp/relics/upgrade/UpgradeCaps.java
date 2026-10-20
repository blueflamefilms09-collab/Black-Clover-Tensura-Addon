package com.newuniverse.nusmp.relics.upgrade;

/**
 * The per-grade cap table of the worn relic models as the game and its tests know it (1.102.0). Until then every worn model had one cap, 2600 vertices (tools/relics/gen_worn_all.py
 * MODEL_CAP and ENSEMBLE_CAP); those numbers are unchanged for every relic that is not upgraded. The God grade upgrade raises the allowance for its own relics only. The Python table
 * (tools/visual_upgrade/god_worn/caps.py, GRADE_CAPS) is the source; {@code GodWornUpgradeTest} checks that the two agree. Append-only: a later grade adds a row, nothing here is edited.
 * Pure constants.
 */
public final class UpgradeCaps {
    private UpgradeCaps() {}

    /** The cap every worn model had before the upgrade and still has when it is not upgraded. */
    public static final int LEGACY_MODEL_VERTICES = 2600, LEGACY_ENSEMBLE_VERTICES = 2600;

    /** GOD: one model file, everything one relic wears (a mirrored pair counts twice), and the held form of one relic. */
    public static final int GOD_MODEL_VERTICES = 4800, GOD_ENSEMBLE_VERTICES = 16000, GOD_HELD_VERTICES = 2500;

    /** GOD: the LOD form is at most this share of the full vertices; it is drawn beyond {@link #LOD_DISTANCE} blocks and at the low quality; the additive passes stop beyond {@link #ADDITIVE_CUTOFF}. */
    public static final double GOD_LOD_FRACTION = 0.15, LOD_DISTANCE = 12.0, ADDITIVE_CUTOFF = 24.0;
}
