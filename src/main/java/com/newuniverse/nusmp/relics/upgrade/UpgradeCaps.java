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

    /**
     * The caps of one upgraded relic: one model file, everything it wears at full detail, and the LOD share. The two slices keep their own tables because their relics differ (the Charms /
     * Genesis table is the file assets/nusmp/visual_upgrade/caps_charms_genesis_worn.txt, the God worn table is the GOD row above and tools/visual_upgrade/god_worn/caps.py); the lookup
     * below is the one place that answers for a relic, and a test pins both tables to their files.
     */
    public record Caps(int modelVertices, int ensembleVertices, double lodFraction) {}

    /** Slice names of {@link #lookup}. */
    public static final String SLICE_GOD_WORN = "god_worn", SLICE_CHARMS_GENESIS = "charms_genesis_worn";

    /** The caps of a relic of {@code slice} and {@code grade} ("GOD" or "GENESIS"); the legacy 2600 / 2600 caps for an unknown pair (nothing is allowed more than before by mistake). */
    public static Caps lookup(String slice, String grade) {
        if (SLICE_GOD_WORN.equals(slice) && "GOD".equals(grade)) return new Caps(GOD_MODEL_VERTICES, GOD_ENSEMBLE_VERTICES, GOD_LOD_FRACTION);
        if (SLICE_CHARMS_GENESIS.equals(slice)) {
            if ("GOD".equals(grade)) return new Caps(2500, 2500, 0.15);
            if ("GENESIS".equals(grade)) return new Caps(6000, 6000, 0.15);
        }
        return new Caps(LEGACY_MODEL_VERTICES, LEGACY_ENSEMBLE_VERTICES, GOD_LOD_FRACTION);
    }

    // ---- the one draw rule of every upgraded worn model (the God worn slice and the Charms / Genesis slice, merged in 1.106.0). Pure, so a test pins it.

    /** LOD model beyond {@link #LOD_DISTANCE} blocks, and at the low quality. */
    public static boolean useLod(double distSq, boolean lowQuality) { return lowQuality || distSq > LOD_DISTANCE * LOD_DISTANCE; }

    /** Additive passes (glow map, lit glow, shell, motes) up to {@link #ADDITIVE_CUTOFF} blocks. */
    public static boolean additive(double distSq) { return distSq <= ADDITIVE_CUTOFF * ADDITIVE_CUTOFF; }

    /** The rich additive passes (nebula, halos): in range of the additive passes and not at the low quality. */
    public static boolean rich(double distSq, boolean lowQuality) { return additive(distSq) && !lowQuality; }
}
