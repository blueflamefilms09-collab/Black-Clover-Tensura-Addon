package com.newuniverse.nusmp.book;

import java.util.Locale;

/**
 * Mode Armour, VALKYRIE DRESS v2: the pure rules of the player-fitted armour layer {@code client/mode/ModeArmorValkyrieV2} (docs/vfx_remake/valkyrie_ingame.md). No game class: the meshes, the budgets, the
 * self-test, the unit tests and the headless preview read the very same tables the painter draws from.
 *
 * <p>What is here: the three client options and their parsing ({@link Look} {@code modeArmor.valkyrieLook}, {@link Form} {@code modeArmor.valkyrieForm}, {@link LanceMode} {@code modeArmor.valkyrieLance}),
 * the three levels of detail by distance ({@link Level}), the piece table of the preview ({@link Piece}: bone, copies, which look wears it, texture of each mesh layer, quads per level) and the quad counts
 * a wearer costs ({@link #armourQuads}, {@link #lanceQuads}), which the frame budget ({@link ModeArmorValkyrieBudget}) is built on.</p>
 *
 * <p>Layers of a mesh file ({@code q <layer> 4 x (x y z u v) nx ny nz r g b}): 0 plate (flat toon), 1 emissive flat (additive), 3 card (textured, alpha tested), 4 veil (textured, translucent),
 * 5 glow (textured, additive), 6 frost, 7 deep, 8 scale (flat plates of the lance) and 9 glass (flat translucent).</p>
 */
public final class ModeArmorValkyrieRules {
    private ModeArmorValkyrieRules() {}

    // ------------------------------------------------------------------ options
    /** {@code modeArmor.valkyrieLook}: V2 draws the new armour, CLASSIC the old overlay of {@code ModeArmorLayer} exactly. */
    public enum Look { V2, CLASSIC }

    public static final Look DEFAULT_LOOK = Look.V2;

    /** {@code modeArmor.valkyrieForm}: which look V2 draws. */
    public enum Form { VALKYRIE, SAINT, MERMAID }

    public static final Form DEFAULT_FORM = Form.VALKYRIE;

    /** {@code modeArmor.valkyrieLance}: no lance, attached at the hips (the tail's lance for the Mermaid look), or floating beside the wearer. */
    public enum LanceMode { OFF, ATTACHED, FLOATING }

    public static final LanceMode DEFAULT_LANCE = LanceMode.ATTACHED;

    public static Look parseLook(String s) { return parse(Look.values(), s, DEFAULT_LOOK); }

    public static Form parseForm(String s) { return parse(Form.values(), s, DEFAULT_FORM); }

    public static LanceMode parseLance(String s) { return parse(LanceMode.values(), s, DEFAULT_LANCE); }

    private static <E extends Enum<E>> E parse(E[] all, String s, E fallback) {
        if (s == null) return fallback;
        String t = s.trim().toUpperCase(Locale.ROOT);
        for (E e : all) if (e.name().equals(t)) return e;
        return fallback;
    }

    // ------------------------------------------------------------------ level of detail
    /** Level of detail of the meshes: inside 16 blocks the full one, to 32 the mid one, to 64 the far one, nothing beyond. */
    public enum Level {
        NEAR(""), MID("_mid"), FAR("_lo");

        public final String suffix;

        Level(String suffix) { this.suffix = suffix; }
    }

    public static final double NEAR_RANGE = 16.0, MID_RANGE = 32.0, FAR_RANGE = 64.0;
    public static final double NEAR_RANGE_SQ = NEAR_RANGE * NEAR_RANGE, MID_RANGE_SQ = MID_RANGE * MID_RANGE, FAR_RANGE_SQ = FAR_RANGE * FAR_RANGE;

    /** The best level distance allows (0 NEAR, 1 MID, 2 FAR), or -1 beyond 64 blocks (nothing is drawn). {@code distSq} is the squared distance to the camera. */
    public static int levelOf(double distSq) {
        if (distSq <= NEAR_RANGE_SQ) return 0;
        if (distSq <= MID_RANGE_SQ) return 1;
        return distSq <= FAR_RANGE_SQ ? 2 : -1;
    }

    // ------------------------------------------------------------------ textures
    /** The cells of the 256 x 256 atlas {@code textures/entity/mode/valk2_atlas.png} (cell {@code i} at column i % 4, row i / 4; tools/gen_valkyrie_ingame_assets.py), and the separate scale sheet (it repeats). */
    public enum Tex {
        BOOK, FEATHER, FIN, FLAME, FOAM, GLOW, HALO, PETAL, VEIL, WATER, WHITE, SCALES;

        public static final int ATLAS_SIZE = 256, CELL = 64, COLUMNS = 4;

        /** True for a cell of the atlas (every sheet but the repeating scales). */
        public boolean inAtlas() { return this != SCALES; }

        public int column() { return ordinal() % COLUMNS; }

        public int row() { return ordinal() / COLUMNS; }
    }

    /** The mesh layers. */
    public static final int L_PLATE = 0, L_EMISSIVE = 1, L_CARD = 3, L_VEIL = 4, L_GLOW = 5, L_FROST = 6, L_DEEP = 7, L_SCALE = 8, L_GLASS = 9, LAYERS = 10;

    // ------------------------------------------------------------------ bones
    /**
     * What a piece is fitted to. The preview space is the vanilla player at rest (px, y up, feet at y = 0, front +z); a mesh is written for the left copy (+x) and the layer draws the mirror.
     * The wing bones are body-fixed hinges that flutter per vertex ({@link ModeArmorValkyrieMesh.Wings}).
     */
    public enum Bone {
        BODY(0f, 24f, false), HEAD(0f, 24f, false), ARM(5f, 22f, false), LEG(2f, 12f, false), WING(0f, 24f, true), WING_LOW(0f, 24f, true), SAINT_WING(0f, 24f, true);

        /** The part's pivot in preview space (x of the left copy, height above the feet). */
        public final float pivotX, pivotY;
        public final boolean wing;

        Bone(float pivotX, float pivotY, boolean wing) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.wing = wing;
        }

        /** True when the piece is mirrored to the other side by a copy that sits on the mirror part (arm, leg) or across the body's middle (copies = 2 on a body piece). */
        public boolean paired() { return this == ARM || this == LEG || wing; }
    }

    /** Wing hinges in preview space: {x, y, z, wing length} of the left copy (tools/gen_valkyrie_armor_v2.py BONES). */
    public static float[] hinge(Bone b) {
        return switch (b) {
            case WING -> new float[]{4.6f, 21.5f, -2.2f, 32f};
            case WING_LOW -> new float[]{2.4f, 15.5f, -2.6f, 15f};
            case SAINT_WING -> new float[]{4.6f, 21.5f, -2.4f, 34f};
            default -> throw new IllegalArgumentException(b + " is not a wing bone");
        };
    }

    // ------------------------------------------------------------------ pieces
    private static final int V = 1, S = 2, M = 4;

    /**
     * The pieces of the armour (tools/gen_valkyrie_armor_v2.py PIECES, the table in {@code assets/nusmp/valkyrie_meshes/pieces.json}): bone, copies (2 = a mirror copy), the looks that wear it, the sheet of
     * the mesh layers 3, 4 and 5 and the quads of one copy at NEAR, MID and FAR. {@code ModeArmorValkyrieAssetTest} pins this table to the json and to the real mesh files.
     */
    public enum Piece {
        WING_MAIN("wing_main", Bone.WING, 2, V, null, Tex.VEIL, Tex.GLOW, 90, 30, 10),
        WING_LOW("wing_low", Bone.WING_LOW, 2, V, null, Tex.VEIL, Tex.GLOW, 24, 10, 6),
        ARM_VEIL("arm_veil", Bone.ARM, 2, V, null, Tex.VEIL, null, 15, 6, 2),
        SLEEVE("sleeve", Bone.ARM, 2, V | S, null, Tex.VEIL, null, 12, 5, 4),
        CROWN("crown", Bone.HEAD, 1, V | M, Tex.PETAL, null, null, 33, 27, 15),
        BODICE("bodice", Bone.BODY, 1, V | S, Tex.PETAL, null, null, 98, 62, 30),
        SKIRT("skirt", Bone.BODY, 1, V | S, Tex.PETAL, null, null, 60, 34, 14),
        SHOULDER("shoulder", Bone.ARM, 2, V | S | M, Tex.PETAL, null, null, 30, 12, 10),
        CUFF("cuff", Bone.ARM, 2, V | S | M, Tex.PETAL, null, null, 12, 10, 8),
        LEG("leg", Bone.LEG, 2, V | S, Tex.FLAME, Tex.WATER, null, 39, 19, 10),
        GRIMOIRE("grimoire", Bone.BODY, 1, V | S | M, null, Tex.BOOK, Tex.GLOW, 9, 3, 3),
        SCARF("scarf", Bone.BODY, 1, V, null, Tex.VEIL, null, 18, 12, 4),
        FIN_GREAVE("fin_greave", Bone.LEG, 2, V, Tex.FIN, null, null, 15, 11, 4),
        HORN("horn", Bone.HEAD, 1, M, null, null, null, 30, 15, 8),
        SHELL_TOP("shell_top", Bone.BODY, 2, M, Tex.PETAL, null, null, 24, 10, 8),
        MERMAID_TAIL("mermaid_tail", Bone.BODY, 1, M, Tex.SCALES, null, null, 101, 52, 27),
        MERMAID_FRILL("mermaid_frill", Bone.BODY, 1, M, Tex.PETAL, null, null, 38, 16, 12),
        SAINT_WING("saint_wing", Bone.SAINT_WING, 2, S, Tex.FEATHER, null, null, 50, 30, 16),
        SAINT_HALO("saint_halo", Bone.HEAD, 1, S, null, null, Tex.HALO, 16, 10, 6),
        SAINT_CROWN("saint_crown", Bone.HEAD, 1, S, Tex.FEATHER, null, null, 29, 23, 15),
        SAINT_SASH("saint_sash", Bone.BODY, 1, S, null, null, null, 14, 12, 6);

        /** File name stem ({@code valk2_<name>[_mid|_lo].txt}). */
        public final String name;
        public final Bone bone;
        public final int copies;
        private final int looks;
        /** The sheet of the mesh layers 3, 4 and 5 (null = the piece has none of that layer). */
        public final Tex card, veil, glow;
        /** Quads of ONE copy at NEAR, MID, FAR. */
        private final int[] quads;

        Piece(String name, Bone bone, int copies, int looks, Tex card, Tex veil, Tex glow, int near, int mid, int far) {
            this.name = name;
            this.bone = bone;
            this.copies = copies;
            this.looks = looks;
            this.card = card;
            this.veil = veil;
            this.glow = glow;
            this.quads = new int[]{near, mid, far};
        }

        public boolean wornIn(Form f) { return (looks & (1 << f.ordinal())) != 0; }

        public int quads(int level) { return quads[level]; }

        public String file(int level) { return "valk2_" + name + Level.values()[level].suffix; }

        /** The sheet a mesh layer of this piece samples: the flat layers use the white cell. */
        public Tex texOf(int layer) {
            return switch (layer) {
                case L_CARD -> card == null ? Tex.WHITE : card;
                case L_VEIL -> veil == null ? Tex.WHITE : veil;
                case L_GLOW -> glow == null ? Tex.WHITE : glow;
                default -> Tex.WHITE;
            };
        }
    }

    private static final Piece[][] WORN = new Piece[Form.values().length][];

    static {
        for (Form f : Form.values()) {
            int n = 0;
            for (Piece p : Piece.values()) if (p.wornIn(f)) n++;
            Piece[] a = new Piece[n];
            n = 0;
            for (Piece p : Piece.values()) if (p.wornIn(f)) a[n++] = p;
            WORN[f.ordinal()] = a;
        }
    }

    /** The pieces a form wears, in draw order (a shared array: do not modify). */
    public static Piece[] worn(Form f) { return WORN[f.ordinal()]; }

    // ------------------------------------------------------------------ the lance
    /** The merged drill lance AB (tools/gen_valkyrie_lance.py): quads at NEAR, MID, FAR; it is drawn at NEAR and MID only (a far wearer goes without). */
    public static final int[] LANCE_QUADS = {312, 179, 76};
    public static final String LANCE_FILE = "lance_AB";
    /** Length of the lance in px (3.4 blocks) and where the hand holds it. */
    public static final float LANCE_LENGTH = 54f, LANCE_GRIP = 13.5f;

    /** True when the lance may be drawn at that level (NEAR and MID). */
    public static boolean lanceAt(int level) { return level == 0 || level == 1; }

    public static String lanceFile(int level) { return LANCE_FILE + Level.values()[level].suffix; }

    /** Mesh quads of a wearer's armour (both copies of the paired pieces) at a level. */
    public static int armourQuads(Form f, int level) {
        int n = 0;
        for (Piece p : WORN[f.ordinal()]) n += p.quads(level) * p.copies;
        return n;
    }

    /** Mesh quads of the lance at a level (0 where it is not drawn). */
    public static int lanceQuads(int level) { return lanceAt(level) ? LANCE_QUADS[level] : 0; }

    /** Vertices of a wearer (4 per quad) with or without the lance. */
    public static int vertices(Form f, int level, boolean lance) { return 4 * (armourQuads(f, level) + (lance ? lanceQuads(level) : 0)); }

    // ------------------------------------------------------------------ wing flutter (the idle proposal of docs/vfx_remake/valkyrie_wings.md section 5)
    /** The flutter of the wings at game time {@code t} (ticks): flap about the hinge, extra bend towards the tip, sweep about x, the wave amplitude and the wave phase. {@code swing} = limb swing amount (a run beats harder). */
    public static void flutter(float t, float swing, float[] out) {
        out[0] = 5f * ModeArmorValkyrieMesh.sin(0.12f * t) + 4f * swing;
        out[1] = 6f * ModeArmorValkyrieMesh.sin(0.12f * t + 0.6f) + 3f * swing;
        out[2] = 2f * ModeArmorValkyrieMesh.sin(0.09f * t);
        out[3] = 0.55f + 0.25f * swing;
        out[4] = 0.2f * t;
    }

    // ------------------------------------------------------------------ the water dive pieces the V2 armour replaces
    /** Spirit Dive piece ids ({@code SpiritDivePieces}) whose job the V2 armour does itself (wings, helm, halo): the aura skips them while the V2 layer is on. */
    public static boolean replacesAuraPiece(String id) {
        return switch (id) {
            case "water:wing_r", "water:wing_l", "water:helm", "spiritdive:saint_wing_r", "spiritdive:saint_wing_l", "spiritdive:saint_halo" -> true;
            default -> false;
        };
    }
}
