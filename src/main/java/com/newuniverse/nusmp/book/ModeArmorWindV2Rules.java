package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mode Armour, WIND SPIRIT DIVE v2 (Yuno's overlay, "10x better"): everything about the new overlay that can be decided without a game, pure and unit tested
 * ({@code ModeArmorWindV2Test}); the painter ({@code client.mode.ModeArmorWindV2}) reads these tables and the meshes of {@link ModeArmorWindV2Mesh}, so the worst case of one wearer is exactly what is
 * counted here.
 *
 * <p>What it is: the overlay of {@code ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO} (the wind spirit dive or Spirit of Zephyr cast by the mage whose bound grimoire is Yuno's): a layered 3D coat (tapered fur collar standing
 * up round the neck, shoulder mantles with fur trim, a button line with gold trim, a sash with a tassel, split coat tails that sway, fur cuffs, boots with fur tops), a small crown of arrow blades floating over the
 * head, two big smooth leaf wings with veins and a cross ornament at the tip (and a second smaller leaf), a glow pass over the coat seams, wind ribbons from the cuffs and the tails, a few drifting leaves,
 * glowing eyes and a hair tint. The old 73-quad overlay stays for every other Wind user and as {@link Look#CLASSIC}.</p>
 *
 * <p>Cost model: the meshes are built once at class load (static arrays). A wearer draws, per level of detail ({@link #lod}), the pieces of {@link #entries}; the vertices are counted from the real meshes,
 * {@link #vertices}. At most {@link #CHARACTER_PER_FRAME} wearers per frame draw the full level; the rest draw the mid level.</p>
 */
public final class ModeArmorWindV2Rules {
    private ModeArmorWindV2Rules() {}

    /** The client option {@code modeArmor.windLook}. Append only. */
    public enum Look { V2, CLASSIC }

    public static final Look DEFAULT_LOOK = Look.V2;

    public static Look parse(String s) {
        if (s == null) return DEFAULT_LOOK;
        for (Look l : Look.values()) if (l.name().equalsIgnoreCase(s.trim())) return l;
        return DEFAULT_LOOK;
    }

    /** How many wearers per frame draw the full level of detail (the rest the mid level), the same number as the Spirit Dive character tier. */
    public static final int CHARACTER_PER_FRAME = SpiritDiveTier.CHARACTER_PER_FRAME;

    // ------------------------------------------------------------------ the budgets
    /** Vertices one wearer may draw in a frame, per level of detail (FULL, MID, FAR); beyond 64 blocks nothing. */
    public static final int CAP_FULL = 3200, CAP_MID = 1200, CAP_FAR = 150;

    /** The most vertices the overlay and the Spirit Dive aura of a Yuno dive draw together in one frame at the full level of detail (calm steady state). */
    public static final int CAP_COMBINED_STEADY = 6500;

    public static int cap(Lod lod) {
        return switch (lod) { case FULL -> CAP_FULL; case MID -> CAP_MID; case FAR -> CAP_FAR; case CULLED -> 0; };
    }

    /** The level of detail from the squared distance to the camera: the same ranges as the Spirit Dive (24 / 48 / 64 blocks). */
    public static Lod lod(double distSq) { return SpiritDiveTier.lod(distSq); }

    /** The level a wearer really draws: FULL only when admitted by the per-frame budget, otherwise MID. */
    public static Lod effective(Lod lod, boolean admitted) { return lod == Lod.FULL && !admitted ? Lod.MID : lod; }

    // ------------------------------------------------------------------ the textures (tools/gen_mwv2_textures.py writes them; tools/mwv2_layout.json is the same layout)
    public static final String COAT = "textures/entity/mode/mwv2_coat.png", FUR = "textures/entity/mode/mwv2_fur.png", GLOW = "textures/entity/mode/mwv2_glow.png",
            WING = "textures/entity/mode/mwv2_wing.png", FX = "textures/entity/mode/mwv2_fx.png";
    public static List<String> allTextures() { return List.of(COAT, FUR, GLOW, WING, FX); }

    /** Texture sizes (px). The coat and the glow map share one layout. */
    public static final int COAT_W = 256, COAT_H = 256, FUR_W = 128, FUR_H = 128, WING_W = 128, WING_H = 256, FX_W = 128, FX_H = 128;

    /** A region of a texture in pixels: {x0, y0, x1, y1}. */
    public static final int[] R_TORSO = {0, 0, 160, 64}, R_TAIL_BACK = {160, 0, 192, 48}, R_TAIL_FRONT = {192, 0, 224, 40}, R_SLEEVE = {0, 64, 64, 96}, R_MANTLE = {64, 64, 144, 88},
            R_TROUSER = {0, 96, 64, 124}, R_BOOT = {64, 96, 128, 120}, R_SASH = {128, 96, 256, 112}, R_BUCKLE = {160, 64, 176, 72}, R_CORD = {176, 64, 192, 96}, R_TASSEL = {192, 64, 208, 80};
    public static final int[] R_FUR_OUTER = {0, 0, 128, 32}, R_FUR_LIP = {0, 32, 64, 48}, R_FUR_INNER = {64, 32, 128, 48};
    public static final int[] R_WING = {0, 0, 128, 256};
    public static final int[] R_BLADE = {0, 0, 16, 32}, R_BLADE_FLAT = {48, 0, 64, 32}, R_ARROW = {16, 0, 48, 16}, R_RING = {0, 32, 64, 40}, R_RING_TOP = {64, 32, 128, 40}, R_HALO = {0, 40, 40, 80},
            R_CROSS = {48, 40, 88, 80}, R_RIBBON = {96, 48, 112, 112}, R_LEAF = {0, 80, 16, 96}, R_EYE = {112, 40, 128, 48}, R_HAIR = {96, 0, 128, 32};

    // ------------------------------------------------------------------ the pieces and what a wearer draws
    /** The render passes in drawing order: the cloth (lit, alpha cut), the fur (lit, alpha cut), the glow over the coat (additive), the wings (translucent, full bright), the solid light pieces (crown, crosses: emissive), the additive effects (halo, ribbons, eyes, hair tint), the additive glow of the big wings (the same mesh and texture again, so the sheets shine instead of looking like grey glass). */
    public enum Pass { COAT, FUR, GLOW, WING, FX, ADD, WING_ADD }

    /** The body parts a piece is bound to (the painter captures one pose per part). */
    public enum Part { BODY, ARM_R, ARM_L, LEG_R, LEG_L, HEAD, CROWN, WING_R, WING_L }

    /**
     * The pieces. {@code part} is where the piece lives; mirrored pieces are built for the left side and drawn for both ({@code copies} = 2: the right copy mirrors x).
     */
    public enum Piece {
        TORSO, SASH, BUCKLE, CORDS, TASSEL, HEM_FUR, TAILS_BACK, TAILS_FRONT, COLLAR,
        SLEEVE, CUFF, MANTLE, MANTLE_TRIM,
        TROUSER, BOOT, BOOT_FUR,
        EYES, HAIR,
        CROWN, HALO,
        WING_BIG, WING_SMALL, WING_ORN,
        RIBBON_CUFF, RIBBON_TAIL
    }

    /** One thing a wearer draws: the piece at a detail index of its mesh, in a pass, once or mirrored for both sides. */
    public record Entry(Piece piece, int detail, Pass pass, int copies) {}

    /** The body part a piece is bound to (the left / right variants are chosen by the painter when {@link Entry#copies} is 2). */
    public static Part partOf(Piece p) {
        return switch (p) {
            case SLEEVE, CUFF, MANTLE, MANTLE_TRIM, RIBBON_CUFF -> Part.ARM_L;
            case TROUSER, BOOT, BOOT_FUR -> Part.LEG_L;
            case EYES, HAIR -> Part.HEAD;
            case CROWN, HALO -> Part.CROWN;
            case WING_BIG, WING_SMALL, WING_ORN -> Part.WING_L;
            default -> Part.BODY;
        };
    }

    private static Entry e(Piece p, int d, Pass pass, int copies) { return new Entry(p, d, pass, copies); }

    private static final Entry[] FULL = {
            e(Piece.TORSO, 0, Pass.COAT, 1), e(Piece.SASH, 0, Pass.COAT, 1), e(Piece.BUCKLE, 0, Pass.COAT, 1), e(Piece.CORDS, 0, Pass.COAT, 1), e(Piece.TASSEL, 0, Pass.COAT, 1),
            e(Piece.TAILS_BACK, 0, Pass.COAT, 1), e(Piece.TAILS_FRONT, 0, Pass.COAT, 1),
            e(Piece.SLEEVE, 0, Pass.COAT, 2), e(Piece.MANTLE, 0, Pass.COAT, 2),
            e(Piece.TROUSER, 0, Pass.COAT, 2), e(Piece.BOOT, 0, Pass.COAT, 2),
            e(Piece.COLLAR, 0, Pass.FUR, 1), e(Piece.HEM_FUR, 0, Pass.FUR, 1), e(Piece.CUFF, 0, Pass.FUR, 2), e(Piece.MANTLE_TRIM, 0, Pass.FUR, 2), e(Piece.BOOT_FUR, 0, Pass.FUR, 2),
            e(Piece.TORSO, 0, Pass.GLOW, 1), e(Piece.TAILS_BACK, 0, Pass.GLOW, 1), e(Piece.SLEEVE, 0, Pass.GLOW, 2),
            e(Piece.WING_BIG, 0, Pass.WING, 2), e(Piece.WING_SMALL, 0, Pass.WING, 2), e(Piece.WING_BIG, 0, Pass.WING_ADD, 2), e(Piece.WING_SMALL, 0, Pass.WING_ADD, 2),
            e(Piece.CROWN, 0, Pass.FX, 1), e(Piece.HALO, 0, Pass.ADD, 1), e(Piece.WING_ORN, 0, Pass.FX, 2), e(Piece.RIBBON_CUFF, 0, Pass.ADD, 2), e(Piece.RIBBON_TAIL, 0, Pass.ADD, 2),
            e(Piece.EYES, 0, Pass.ADD, 1), e(Piece.HAIR, 0, Pass.ADD, 1)
    };

    private static final Entry[] MID = {
            e(Piece.TORSO, 1, Pass.COAT, 1), e(Piece.SASH, 1, Pass.COAT, 1), e(Piece.TAILS_BACK, 1, Pass.COAT, 1),
            e(Piece.SLEEVE, 1, Pass.COAT, 2), e(Piece.TROUSER, 1, Pass.COAT, 2), e(Piece.BOOT, 1, Pass.COAT, 2),
            e(Piece.COLLAR, 1, Pass.FUR, 1), e(Piece.CUFF, 1, Pass.FUR, 2),
            e(Piece.TORSO, 1, Pass.GLOW, 1),
            e(Piece.WING_BIG, 1, Pass.WING, 2), e(Piece.WING_SMALL, 1, Pass.WING, 2),
            e(Piece.CROWN, 1, Pass.FX, 1), e(Piece.HALO, 1, Pass.ADD, 1)
    };

    private static final Entry[] FAR = {
            e(Piece.WING_BIG, 2, Pass.WING, 2),
            e(Piece.CROWN, 2, Pass.FX, 1), e(Piece.HALO, 2, Pass.ADD, 1)
    };

    private static final Entry[] NONE = {};

    /** The things a wearer draws at a level of detail, in pass order (the painter draws a pass at a time). */
    public static Entry[] entries(Lod lod) {
        return switch (lod) { case FULL -> FULL; case MID -> MID; case FAR -> FAR; case CULLED -> NONE; };
    }

    /** Drifting leaves (quads of 4 vertices) per level of detail. */
    public static int leaves(Lod lod) { return switch (lod) { case FULL -> 6; case MID -> 2; default -> 0; }; }

    /** Vertices of one wearer at a level of detail, counted from the real meshes: every entry's mesh times its copies, and the leaves. */
    public static int vertices(Lod lod) {
        int v = leaves(lod) * 4;
        for (Entry en : entries(lod)) v += ModeArmorWindV2Mesh.get(en.piece(), en.detail()).verts * en.copies();
        return v;
    }

    /** Vertices a wearer draws in a pass at a level of detail (for the doc's table). */
    public static int vertices(Lod lod, Pass pass) {
        int v = pass == Pass.FX ? leaves(lod) * 4 : 0;
        for (Entry en : entries(lod)) if (en.pass() == pass) v += ModeArmorWindV2Mesh.get(en.piece(), en.detail()).verts * en.copies();
        return v;
    }

    /** The pieces a level draws, by name (for the doc and the self-test). */
    public static List<String> pieceNames(Lod lod) {
        List<String> l = new ArrayList<>();
        for (Entry en : entries(lod)) l.add(en.piece().name().toLowerCase(Locale.ROOT) + "/" + en.pass().name().toLowerCase(Locale.ROOT));
        return l;
    }

    // ------------------------------------------------------------------ which mode draws it
    /** The wire ordinal the server sends for a wind dive overlay: Yuno's grimoire holder gets the Yuno variant (append only in {@code ModeArmor.Mode}). */
    public static boolean yunoVariant(String modeName) { return "WIND_SPIRIT_DIVE_YUNO".equals(modeName); }

    // ------------------------------------------------------------------ the motion constants (pure so the test can pin them)
    /** The crown: bob (px) and its speed, turn (degrees a tick), the height of the ring above the head top (px, head y is down so negative is up), the radius and the arrow heights (60 to 65 percent of the old crown). */
    public static final float CROWN_BOB = 0.45f, CROWN_BOB_SPEED = 0.1f, CROWN_SPIN_DEG = 1.0f, CROWN_Y = -10.2f;
    /** Wings: flap amplitude (degrees) and speed (rad a tick), the sway of the leaf (px) and its speed. */
    public static final float FLAP_DEG = 7f, FLAP_SPEED = 0.16f, WING_SWAY = 1.7f, WING_SWAY_SPEED = 0.11f;
    /** The tails flare as you run (px at full limb swing) and sway (px). */
    public static final float TAIL_FLARE = 2.6f, TAIL_SWAY = 0.9f, TAIL_SWAY_SPEED = 0.13f;

    /** The old crown for the size comparison: ring radius 4.6 px, blades 7 and 5 px. */
    public static final float OLD_CROWN_RADIUS = 4.6f, OLD_CROWN_TALL = 7f, OLD_CROWN_SHORT = 5f;
}
