package com.newuniverse.nusmp.client.relics.worn;

/**
 * Why: a worn armour relic (head, chest, feet) must NEVER be invisible. {@link ArmorSlotPlan} decides before the frame what the vanilla armour layer and the model library should draw, but
 * a plan is a promise, not a fact: the model library can come back with nothing drawn, an own renderer can fail, the vanilla armour layer can be skipped by another mod, a painted texture
 * can be missing after a reload. This class is the decision made AFTER the frame, from facts that were recorded while drawing ({@link ArmorTrace}): given what the plan said and what was
 * really drawn, is the relic visible, hidden on purpose, or does the last-resort failsafe (the plain painted boxes of {@code RelicWornLayer}) have to draw it? Pure: no Minecraft types.
 */
public final class ArmorVisible {
    private ArmorVisible() {}

    /** What ended up on the body for one armour relic in one frame. */
    public enum Outcome {
        /** A model of the library was drawn (at least one piece). */
        MODEL,
        /** An own renderer ran without error. */
        CUSTOM,
        /** The vanilla armour layer drew the painted layer and its texture exists and is not empty. */
        PAINTED,
        /** The player chose it: relicfx.hideArmourRelics, or the camera sits in the wearer's head. */
        OFF_ON_PURPOSE,
        /** Beyond the draw range of the worn layers (a few pixels on screen): nothing is forced. */
        OUT_OF_RANGE,
        /** Nothing of the relic was drawn by any path: the plain painted boxes are drawn now. */
        FAILSAFE
    }

    /**
     * The facts of one frame.
     *
     * @param modelPieces   how many pieces of the relic's model the library really drew (0: none)
     * @param customRan     the relic's own renderer ran without an error
     * @param paintedLayer  the vanilla armour layer was asked for this relic and answered "draw the painted layer" (not the empty model)
     * @param textureOk     the painted layer's texture is registered and not empty
     */
    public record Facts(int modelPieces, boolean customRan, boolean paintedLayer, boolean textureOk, boolean bodyNeedsPaint) {
        public boolean painted() { return paintedLayer && textureOk; }
    }

    /**
     * What to do for a relic, from the plan and the facts of the frame. A chest piece is a painted body with 3D ornaments over it, so ornaments alone do not count for it
     * ({@code bodyNeedsPaint}): without the painted layer (or an own renderer) the failsafe draws the body.
     */
    public static Outcome resolve(ArmorSlotPlan.Verdict plan, Facts f) {
        return resolve(plan, f.modelPieces(), f.customRan(), f.paintedLayer(), f.textureOk(), f.bodyNeedsPaint());
    }

    /**
     * The same decision from the plain facts. 1.112.0 (lag audit): the worn layer asks this once per armour relic per wearer per frame, and {@link Facts} was a record made for every ask;
     * this form allocates nothing ({@link #resolve(ArmorSlotPlan.Verdict, Facts)} is this, called with the record's fields).
     */
    public static Outcome resolve(ArmorSlotPlan.Verdict plan, int modelPieces, boolean customRan, boolean paintedLayer, boolean textureOk, boolean bodyNeedsPaint) {
        if (plan.why() == WornPlan.Why.ARMOUR_OPTION || plan.why() == WornPlan.Why.FIRST_PERSON_HEAD) return Outcome.OFF_ON_PURPOSE;
        if (plan.why() == WornPlan.Why.TOO_FAR) return Outcome.OUT_OF_RANGE;
        if (modelPieces > 0 && !bodyNeedsPaint) return Outcome.MODEL;
        if (customRan) return Outcome.CUSTOM;
        if (paintedLayer && textureOk) return modelPieces > 0 ? Outcome.MODEL : Outcome.PAINTED;
        return Outcome.FAILSAFE;
    }

    /** Does the failsafe have to draw the plain painted boxes for this relic? */
    public static boolean needsFailsafe(ArmorSlotPlan.Verdict plan, Facts f) { return resolve(plan, f) == Outcome.FAILSAFE; }

    /**
     * The decision table. Part 1, the plan alone: for every slot, circumstance and option the verdict leaves something of the relic on the body, unless the player switched armour visuals off or
     * the camera is in the head. Part 2, plan plus facts: whatever was or was not drawn, the answer is a drawn thing, a choice of the player, the distance, or the failsafe, never nothing.
     * Used by the tests and by the self-test {@code relic.armor_visible}. Returns null when every combination holds, else a description of the first one that fails.
     */
    public static String checkTable() {
        String[] slots = {"HEAD", "CHEST", "FEET"};
        boolean[] both = {true, false};
        for (String slot : slots) for (boolean hasModel : both) for (boolean custom : both) for (boolean models : both) for (boolean hide : both) for (boolean far : both)
            for (WornPlan.View view : WornPlan.View.values()) {
                ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(slot, hasModel, custom, models, hide, view, far);
                boolean onPurpose = hide || (view == WornPlan.View.SELF_FIRST && slot.equals("HEAD") && !custom);
                String at = slot + " hasModel=" + hasModel + " custom=" + custom + " models=" + models + " hide=" + hide + " far=" + far + " view=" + view + " -> " + v;
                if (!v.visible() && !onPurpose) return "invisible: " + at;
            }
        for (String slot : slots) for (WornPlan.View view : WornPlan.View.values()) for (boolean hasModel : both) for (boolean far : both) for (boolean hide : both) for (boolean custom : both) {
            ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(slot, hasModel, custom, true, hide, view, far);
            for (int pieces = 0; pieces <= 1; pieces++) for (boolean ran : both) for (boolean layer : both) for (boolean tex : both) {
                boolean chest = slot.equals("CHEST");
                Outcome o = resolve(v, new Facts(pieces, ran, layer, tex, chest));
                boolean drawn = (pieces > 0 && !chest) || ran || (layer && tex);
                String at = slot + " " + v + " pieces=" + pieces + " ran=" + ran + " layer=" + layer + " tex=" + tex + " -> " + o;
                boolean chosen = o == Outcome.OFF_ON_PURPOSE || o == Outcome.OUT_OF_RANGE;
                if (o == Outcome.FAILSAFE && drawn) return "failsafe although something was drawn: " + at;
                if (o != Outcome.FAILSAFE && !drawn && !chosen) return "nothing drawn and no failsafe: " + at;
                if (hide && o != Outcome.OFF_ON_PURPOSE) return "the player's switch is not obeyed: " + at;
            }
        }
        return null;
    }

    /** True when an ARGB image (as painted for a worn armour layer) has at least one visible pixel. */
    public static boolean nonEmpty(int[] argb) {
        if (argb == null) return false;
        for (int p : argb) if (p >>> 24 != 0) return true;
        return false;
    }

    /**
     * The layer used when a relic's painters fail or paint nothing: a plain 64x32 image in the relic's main colour covering the head, torso, arm and leg regions of a vanilla armour
     * texture (so any slot shows something). Pure; the layout is the vanilla one.
     */
    public static int[] solidLayer(int main, int trim) {
        int w = 64, h = 32;
        int[] px = new int[w * h];
        int body = 0xFF000000 | (main & 0xFFFFFF), edge = 0xFF000000 | (trim & 0xFFFFFF);
        fill(px, w, 0, 0, 32, 16, body);            // head and hat
        fill(px, w, 16, 16, 24, 16, body);          // torso
        fill(px, w, 40, 16, 16, 16, body);          // right arm
        fill(px, w, 0, 16, 16, 16, body);           // right leg / boots
        for (int x = 0; x < 32; x++) px[8 * w + x] = edge;            // a trim line across the head band
        for (int x = 16; x < 40; x++) px[(h - 1) * w + x] = edge;     // and the torso hem
        return px;
    }

    private static void fill(int[] px, int w, int x, int y, int cw, int ch, int argb) {
        for (int j = 0; j < ch; j++) for (int i = 0; i < cw; i++) px[(y + j) * w + x + i] = argb;
    }
}
