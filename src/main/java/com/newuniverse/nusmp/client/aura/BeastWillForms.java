package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.beastwill.BeastWillFormTable;
import com.newuniverse.nusmp.beastwill.BeastWillLook;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.aura.NoelleFormRig.Piece;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import net.minecraft.util.Mth;

/**
 * Why (3.61.0): the 3D bodies of the Beast Will forms. The flat painted sprites of 3.60.0 read as pasted pictures; each will now has a real model built from many small, angled, layered cubes
 * (tools/beastwill/remake/forms_gen.py), anchored on the player's own body parts like the Noelle forms: head, body, both arms, both legs, and where the will has them a tail and wings (every piece
 * with moving parts plays its own idle clip: breathing chest, swaying tail, flapping wings, twitching ears, swaying manes, necks and tentacles). In Acquire only ONE piece is worn, the forearm bracer with its claws ({@code acq}). Every piece of a will reads one shared sheet ({@code bwf_<stem>.png}). This class only
 * names the pieces and decides how much to draw; {@link NoelleFormRig#draw} does the drawing, fly-in and shatter-out, the same engine as the other armours.
 *
 * <p>Cost, per player per frame: nothing beyond 48 blocks (the caller's cull) or for an invisible player. Within 24 blocks and under the per-frame budget ({@link NoelleFormRules#MAX_FULL_PER_FRAME}
 * players) the whole model plus its glow pass; the next {@link NoelleFormRules#MAX_FAR_PER_FRAME} players draw the model without the glow pass; every player over those is not drawn at all (the eyes
 * and motes stay). Biggest will (Typhon): 384 cubes = 1976 quads, so the worst frame (every one of the 10 admitted players wearing Typhon) is 4 * 2 * 1976 (colour + glow pass) + 6 * 1976 = about 27.7k quads; with a typical will (150 cubes, 900 quads) about 13k. Acquire
 * is the bracer alone: at most 14 cubes. The sets are built once per kind; nothing is allocated per frame.
 */
public final class BeastWillForms {
    private BeastWillForms() {}

    private static final Forms[] SETS = new Forms[BeastWillLook.COUNT];

    private static final class Forms { NoelleFormRig.Set full, acq; }

    /** Is there a 3D form for this kind? (the table lists it) */
    public static boolean has(int kind) { return kind > 0 && kind < BeastWillLook.COUNT && !BeastWillFormTable.parts(BeastWillLook.STEM[kind]).isEmpty(); }

    private static Forms of(int kind) {
        Forms s = SETS[kind];
        if (s != null) return s;
        s = new Forms();
        String stem = BeastWillLook.STEM[kind], key = "bwf_" + stem;
        java.util.List<Piece> list = new java.util.ArrayList<>();
        add(list, stem, "body", Anchor.BODY, 0f, 0f, 0f, 5f, false, "idle");
        add(list, stem, "armL", Anchor.ARM_L, 2f, 9f, 0f, 0f, false, "idle");
        add(list, stem, "armR", Anchor.ARM_R, 2f, -9f, 0f, 0f, false, "idle");
        add(list, stem, "legL", Anchor.LEG_L, 3f, 0f, 9f, 0f, false, "idle");
        add(list, stem, "legR", Anchor.LEG_R, 3f, 0f, 9f, 0f, false, "idle");
        add(list, stem, "head", Anchor.HEAD, 4f, 0f, -9f, 0f, true, "idle");
        add(list, stem, "tail", Anchor.BODY, 6f, 0f, 2f, 10f, false, "idle");
        add(list, stem, "wings", Anchor.BODY, 8f, 0f, -2f, 8f, false, "idle");
        s.full = new NoelleFormRig.Set(key, key, list.toArray(new Piece[0]));
        java.util.List<Piece> a = new java.util.ArrayList<>();
        add(a, stem, "acq", Anchor.ARM_R, 0f, -9f, 0f, 0f, false, null);
        s.acq = new NoelleFormRig.Set(key, key, a.toArray(new Piece[0]));
        SETS[kind] = s;
        return s;
    }

    private static void add(java.util.List<Piece> out, String stem, String name, Anchor anchor, float delay, float dx, float dy, float dz, boolean head, String clip) {
        if (!BeastWillFormTable.has(stem, name)) return;
        Piece p = Piece.of(name, anchor, delay, dx, dy, dz).silhouette();
        if (head) p = p.head();
        p = p.clip(clip, name.equals("wings") ? 1.4f : 1f);        // a piece without an idle file simply stays still (a missing animation is silently null)
        if (name.equals("wings")) p = p.unfold().flap();
        out.add(p);
    }

    // ---------------------------------------------------------------------------------------------------------------- the per-frame budget of a crowd
    private static final int[] SEEN = new int[16];
    private static int seenN, fullDrawn, farDrawn;

    /** Same crowd rule as the Noelle forms ({@link NoelleFormRules#budgeted}): the first players of a frame get the full model, the next ones the model without glow, the rest nothing. Render thread only. */
    private static NoelleFormRules.Lod frameBudget(int playerId, NoelleFormRules.Lod wanted) {
        for (int i = 0; i < seenN; i++) {
            if (SEEN[i] == playerId) { seenN = 0; fullDrawn = 0; farDrawn = 0; break; }
        }
        if (seenN >= SEEN.length) { seenN = 0; fullDrawn = 0; farDrawn = 0; }
        SEEN[seenN++] = playerId;
        NoelleFormRules.Lod l = NoelleFormRules.budgeted(wanted, 0, fullDrawn, farDrawn);
        if (l == NoelleFormRules.Lod.STANDARD) fullDrawn++; else if (l == NoelleFormRules.Lod.FAR) farDrawn++;
        return l;
    }

    /**
     * Draws the form (phase 2) or its single piece (phase 1) of this kind on the player. Returns true when something was drawn, so the caller can leave out the flat sprites and the glints that
     * the model's own head already carries.
     */
    public static boolean paint(AuraContext c, int phase, int kind, int rgb, float pulse) {
        if (!has(kind)) return false;
        NoelleFormRules.Lod lod = frameBudget(c.player().getId(), NoelleFormRules.lod(NoelleFormRig.dist2(c), false));
        if (lod == NoelleFormRules.Lod.NONE) return false;
        Forms s = of(kind);
        float glow = (phase == 2 ? 0.85f : 0.6f) * Mth.clamp(pulse, 0.5f, 1.2f);
        NoelleFormRig.draw(c, phase == 2 ? s.full : s.acq, lod, rgb, glow, 0xFFFFFF);
        return true;
    }
}
