package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;

/**
 * Why: "my relics do not show on the player" had no definition of when a worn relic MAY be missing. This class is that definition, in one place, with no Minecraft type in it.
 * <p>
 * <b>The contract.</b> A worn relic, Curios or armour, is drawn on every player: the local player in third person, the other players, the inventory preview entity and the paper doll of the
 * Curios screen (all of them are a living player drawn by a player renderer, so one rule serves them all), by default. It is NOT drawn only for one of these reasons, and for no other:
 * <ol>
 *   <li>the wearer is a spectator or invisible (nothing worn is drawn on a body that is not there);</li>
 *   <li>the player switched ALL worn relics off ({@code relicfx.showWorn}, {@code /multiverse relic show off}, the key);</li>
 *   <li>a Curios relic: the eye icon of its slot in the Curios screen is off (the per-slot switch of Curios);</li>
 *   <li>an armour relic: {@code relicfx.hideArmourRelics} is on (a head or boots relic is hidden whole; a chest relic loses its 3D ornaments and its glow but keeps the painted chest
 *       piece, the armour itself, as it always did: {@code ArmorSlotPlanTest.theArmourSwitchHidesTheHelmetAndBootsButNotTheChestBody});</li>
 *   <li>a Curios relic: the wearer is farther than {@link #CULL_RANGE} blocks from the camera (distance culling; an armour relic keeps its painted armour layer at any distance, only its
 *       3D ornaments are culled);</li>
 *   <li>a HEAD armour relic seen from inside the wearer's own head (first person): head pieces would block the camera. Everything else of the body is not affected, and a relic with a
 *       renderer of its own handles the camera itself.</li>
 * </ol>
 * {@code relicfx.models} off is NOT a reason: the relic falls back to plain boxes (Curios) or to its painted armour (armour), never to nothing. Whether a model exists, whether the Curios
 * read of the client worked, which renderer drew the player, and how fresh a cache is, are NOT reasons either: those paths have fallbacks ({@link WornPlan}, the failsafe of
 * {@code RelicWornLayer}, the server's list of {@code WornSync}) and each says its cause once ({@code WornOnce}).
 * <p>
 * {@link #decide} is the contract as a function; {@link #checkTable()} walks every combination of source, slot, eye, options, view, distance and wearer state and compares it with
 * {@link WornPlan} and {@link ArmorSlotPlan}, which are what the layers really use, so the layers cannot drift from the contract without the test failing.
 */
public final class WornVisibility {
    private WornVisibility() {}

    /** The distance (blocks, from the camera) past which the worn layers draw nothing for a Curios relic. */
    public static final double CULL_RANGE = WornFxPlan.CULL_RANGE;

    /** Where the relic is worn. */
    public enum Source { CURIOS, ARMOUR }

    /** Why a worn relic is not drawn; {@link #NONE} means it is. */
    public enum Reason {
        NONE("drawn"),
        SPECTATOR("the wearer is a spectator"),
        INVISIBLE("the wearer is invisible"),
        SHOW_OFF("the player switched all worn relics off (relicfx.showWorn)"),
        CURIOS_EYE_OFF("the Curios eye icon of its slot is off"),
        ARMOUR_OPTION("relicfx.hideArmourRelics is on"),
        TOO_FAR("a Curios relic farther than " + (int) WornFxPlan.CULL_RANGE + " blocks from the camera"),
        FIRST_PERSON_HEAD("a head piece seen from inside the wearer's head");

        public final String text;

        Reason(String text) { this.text = text; }
    }

    /** The answer: drawn or not, and the one reason when not. */
    public record Result(boolean visible, Reason reason) {}

    /** One answer per reason, built once: {@link #decide} runs for every worn relic of every player in every frame and allocates nothing. */
    private static final Result[] BY_REASON = new Result[Reason.values().length];

    static {
        for (Reason r : Reason.values()) BY_REASON[r.ordinal()] = new Result(r == Reason.NONE, r);
    }

    private static Result of(Reason r) { return BY_REASON[r.ordinal()]; }

    private static final Result SEEN = of(Reason.NONE);

    /**
     * Is this worn relic drawn?
     *
     * @param source        Curios or armour
     * @param slot          the relic slot name (HEAD, CHEST, FEET, NECK, BACK, BELT, HANDS, CHARM)
     * @param eye           the Curios eye icon of the slot: true on, false off, null unknown (an unknown eye counts as on)
     * @param custom        the relic has a renderer of its own
     * @param hideArmour    {@code relicfx.hideArmourRelics}
     * @param showWorn      {@code relicfx.showWorn}
     * @param view          whose eyes (the first-person case is the only one that hides anything of the body)
     * @param tooFar        the wearer is beyond {@link #CULL_RANGE}
     */
    public static Result decide(Source source, String slot, Boolean eye, boolean custom, boolean hideArmour, boolean showWorn, WornPlan.View view, boolean tooFar, boolean spectator, boolean invisible) {
        if (spectator) return of(Reason.SPECTATOR);
        if (invisible) return of(Reason.INVISIBLE);
        if (!showWorn) return of(Reason.SHOW_OFF);
        if (source == Source.CURIOS) {
            if (Boolean.FALSE.equals(eye)) return of(Reason.CURIOS_EYE_OFF);
            if (tooFar) return of(Reason.TOO_FAR);
            return SEEN;
        }
        if (hideArmour && !"CHEST".equals(slot)) return of(Reason.ARMOUR_OPTION);
        if (view == WornPlan.View.SELF_FIRST && "HEAD".equals(slot) && !custom) return of(Reason.FIRST_PERSON_HEAD);
        return SEEN;                                                                 // too far: the painted armour layer stays, only the 3D ornaments are culled
    }

    /** Does at least one of the contract's hide conditions hold? Written as a plain disjunction, independent of {@link #decide}, so the table can compare the two. */
    static boolean anyHideCondition(Source source, String slot, Boolean eye, boolean custom, boolean hideArmour, boolean showWorn, WornPlan.View view, boolean tooFar, boolean spectator, boolean invisible) {
        boolean curios = source == Source.CURIOS;
        return spectator || invisible || !showWorn
                || (curios && (Boolean.FALSE.equals(eye) || tooFar))
                || (!curios && ((hideArmour && !"CHEST".equals(slot)) || (view == WornPlan.View.SELF_FIRST && "HEAD".equals(slot) && !custom)));
    }

    private static final String[] CURIOS_SLOTS = {"NECK", "BACK", "BELT", "HANDS", "CHARM"}, ARMOUR_SLOTS = {"HEAD", "CHEST", "FEET"};

    /**
     * Walks every combination and returns null when all hold, else a description of the first that fails. Part 1: {@link #decide} against the plain list of hide conditions (visible unless
     * one holds, never hidden without a reason). Part 2: the same circumstances through {@link WornPlan} (and {@link ArmorSlotPlan} for the vanilla armour layer): what the layers draw
     * is what the contract says, for every combination of "has a model" and "relicfx.models".
     */
    public static String checkTable() {
        Boolean[] eyes = {null, Boolean.TRUE, Boolean.FALSE};
        boolean[] both = {true, false};
        for (Source src : Source.values()) {
            String[] slots = src == Source.CURIOS ? CURIOS_SLOTS : ARMOUR_SLOTS;
            for (String slot : slots) for (Boolean eye : eyes) for (boolean custom : both) for (boolean hide : both) for (boolean show : both)
                for (WornPlan.View view : WornPlan.View.values()) for (boolean far : both) for (boolean spec : both) for (boolean inv : both) {
                    String at = src + " " + slot + " eye=" + eye + " custom=" + custom + " hideArmour=" + hide + " show=" + show + " view=" + view + " far=" + far + " spectator=" + spec + " invisible=" + inv;
                    Result r = decide(src, slot, eye, custom, hide, show, view, far, spec, inv);
                    boolean hidden = anyHideCondition(src, slot, eye, custom, hide, show, view, far, spec, inv);
                    if (r.visible() == hidden) return "contract: " + (hidden ? "drawn although a hide condition holds" : "hidden without a reason (" + r.reason() + ")") + ": " + at;
                    if (r.visible() != (r.reason() == Reason.NONE)) return "contract: visible and reason disagree: " + at;
                    if (far && view != WornPlan.View.OTHER) continue;                        // the local player is never farther than the draw range from its own camera: not a real combination
                    for (boolean hasModel : both) for (boolean models : both) {
                        String planAt = at + " hasModel=" + hasModel + " models=" + models;
                        boolean planVisible = planVisible(src, slot, eye, custom, hide, show, view, far, spec, inv, hasModel, models);
                        if (planVisible != r.visible()) return "plan disagrees with the contract (contract " + r + ", plan " + (planVisible ? "draws" : "hides") + "): " + planAt;
                    }
                }
        }
        return null;
    }

    /** What the real layers do for one relic in these circumstances: the plan's item is drawn, or (armour) the vanilla armour layer draws the painted piece. */
    static boolean planVisible(Source src, String slot, Boolean eye, boolean custom, boolean hide, boolean show, WornPlan.View view, boolean far, boolean spec, boolean inv, boolean hasModel, boolean models) {
        WornPlan.Source ws = src == Source.CURIOS ? WornPlan.Source.CURIOS : WornPlan.Source.ARMOUR;
        WornPlan.Entry e = new WornPlan.Entry("table_relic", slot, "", ws, src == Source.CURIOS ? eye : null, custom, hasModel, 3);
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, view, spec, inv, far, models, hide, WornPlan.Fallback.BOXES, show), java.util.List.of(e));
        WornPlan.Item it = plan.all().get(0);
        if (src == Source.CURIOS) return it.drawn();
        if (spec || inv) return it.drawn();                                          // the vanilla layer is not run for such a body either
        // the vanilla armour layer is decided by ArmorSlotPlan from the same plan; ArmorSlotPlan.decide does the far/hide mixing the layers do
        return ArmorSlotPlan.decide(slot, hasModel, custom, models, hide, view, far, show).visible();
    }
}
