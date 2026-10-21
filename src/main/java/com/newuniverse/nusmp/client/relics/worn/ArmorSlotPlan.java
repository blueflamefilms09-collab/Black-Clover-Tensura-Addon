package com.newuniverse.nusmp.client.relics.worn;

import java.util.List;

/**
 * Why: a head or boots relic whose 3D model replaces its painted armour layer used to be made invisible at TEXTURE time (the layer was registered blank when the library had a model),
 * while whether the model was really drawn is decided on every frame by {@link WornPlan}. When the two disagreed (the layer not reached, the plan skipping the relic, an entity without
 * the worn layer, a wearer out of range) the relic was gone from the body. This class is the one frame-time answer to "what does the vanilla armour layer draw for this relic": it asks
 * {@link WornPlan} itself, so the painted layer is hidden exactly when the plan draws a model (or a renderer of its own, or the player switched armour visuals off) and is shown in every
 * other case. The painted layer always exists. Pure: no Minecraft types.
 */
public final class ArmorSlotPlan {
    private ArmorSlotPlan() {}

    /** What the vanilla armour layer draws for the piece: its painted layer, or nothing (an empty humanoid model). */
    public enum Layer { PAINTED, EMPTY }

    /**
     * @param kind  what {@link WornPlan} decided for the relic
     * @param why   why
     * @param layer what the vanilla armour layer draws
     * @param glow  whether the emissive pass of the painted layer is drawn (only over a painted layer, and not while the player has armour visuals switched off)
     */
    public record Verdict(WornPlan.Kind kind, WornPlan.Why why, Layer layer, boolean glow) {
        public boolean modelDrawn() { return kind == WornPlan.Kind.LIBRARY_MODEL || kind == WornPlan.Kind.CUSTOM; }

        /** True when something of the relic is on the body: a model, or the painted layer. */
        public boolean visible() { return modelDrawn() || layer == Layer.PAINTED; }
    }

    /** The answer for an item of a plan (the layer of the player calls this for the glow of every armour relic). */
    public static Verdict of(WornPlan.Item it) {
        WornPlan.Entry e = it.entry();
        WornPlan.Kind k = it.kind();
        WornPlan.Why y = it.why();
        Layer layer;
        if ("CHEST".equals(e.slot())) layer = Layer.PAINTED;                         // a chest piece is a painted body with 3D ornaments over it: the painted layer is always its base
        else if (k == WornPlan.Kind.LIBRARY_MODEL) layer = Layer.EMPTY;
        else if (k == WornPlan.Kind.CUSTOM) layer = e.hasModel() ? Layer.EMPTY : Layer.PAINTED;
        else if (y == WornPlan.Why.ARMOUR_OPTION || y == WornPlan.Why.FIRST_PERSON_HEAD) layer = Layer.EMPTY;
        else layer = Layer.PAINTED;                                                  // no model, too far, nothing known: the painted armour, never nothing
        return new Verdict(k, y, layer, layer == Layer.PAINTED && y != WornPlan.Why.ARMOUR_OPTION);
    }

    /** The answer for one armour relic worn in {@code slot} ("HEAD", "CHEST", "FEET") in the given circumstances. */
    public static Verdict decide(String slot, boolean hasModel, boolean custom, boolean modelsOption, boolean hideArmourOption, WornPlan.View view, boolean tooFar) {
        WornPlan.Entry e = new WornPlan.Entry("armour_relic", slot, "", WornPlan.Source.ARMOUR, null, custom, hasModel, 3);
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, view, false, false, tooFar, modelsOption, hideArmourOption, WornPlan.Fallback.BOXES), List.of(e));
        return of(plan.all().get(0));
    }
}
