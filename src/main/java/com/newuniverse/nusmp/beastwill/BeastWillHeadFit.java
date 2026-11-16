package com.newuniverse.nusmp.beastwill;

import java.util.Set;

/**
 * Why (docs/body_parts_pass.md): the body-parts pass shrank the heads of the Beast Will forms (Fenrir's wolf head was "way too big" next to the player's 8 px head; the great manes, ears and horns of
 * a dozen wills made their heads 2 to 2.5 times the player's) and fanned out the necks of the many-headed wills (hydra, typhon, yamata no orochi). The wills listed in {@link #REWORKED} keep their
 * earlier models as {@code bwf_<stem>_v2...} (made by tools/beastwill/remake/snapshot_heads_v2.py before the generator changed); the client setting {@code beastwill.classicHeads} draws those again.
 * Pure data and pure rules (no client class), so the asset test and the painter read one table.
 */
public final class BeastWillHeadFit {
    private BeastWillHeadFit() {}

    /** The stems ({@link BeastWillLook#STEM}) whose head, neck or mane changed in the body-parts pass, and so have a {@code _v2} copy of the model before it. */
    public static final Set<String> REWORKED = Set.of("hydra", "typhon", "yamata_no_orochi", "fenrir", "wolf", "lion", "chimera", "cu_sith", "cerberus", "minotaur", "pegasus", "sphinx", "ammit", "elder",
            "scylla_charybdis");

    /** The player's head in model pixels: the yardstick of every ratio. */
    public static final float PLAYER_HEAD = 8f;
    /** The most the width or height of a reworked will's head piece (mane, ears and horns included) may be, as a multiple of the player's head; Fenrir has its own, stricter, limit. */
    public static final float SILHOUETTE_MAX = 2.05f, FENRIR_MAX = 1.75f;
    /** The smallest angle (degrees) between any two necks of a many-headed will. */
    public static final float FAN_MIN_DEGREES = 35f;

    public static boolean reworked(String stem) { return REWORKED.contains(stem); }

    /** The file suffix of the model set to draw: {@code _v1} (classic forms, 3.61.0), {@code _v2} (classic heads, before the body-parts pass, only for the reworked stems) or none. */
    public static String suffix(boolean classicForms, boolean classicHeads, String stem) {
        if (classicForms) return "_v1";
        return classicHeads && reworked(stem) ? "_v2" : "";
    }
}
