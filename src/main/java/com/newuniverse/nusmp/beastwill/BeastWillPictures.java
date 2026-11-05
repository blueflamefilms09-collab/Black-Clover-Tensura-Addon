package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.vfx.VfxShape;

/**
 * Why: the big picture an ability leaves for each of the 18 later wills (docs/beastwill_new.md), as one table instead of two more switches in {@link BeastWillAbilities}. A picture is an EXISTING
 * {@link VfxShape} (never a Time shape, nothing new was drawn): {@code travel} runs along a line (a breath, a rush, a bite, a streak; null = none) and {@code burst} marks a point (a field, a pool, a
 * vortex). {@code lift} raises the burst point, and the burst's power is {@code radius * scale} cut to {@code max}, so the largest picture stays small whatever the file says. The abilities send at most
 * the same two packets as before (see BeastWillLookTest): this table only chooses WHICH shape. Pure data (an enum switch), no allocation per call, no client class.
 */
public final class BeastWillPictures {
    private BeastWillPictures() {}

    public record Pic(VfxShape travel, VfxShape burst, float lift, float scale, float max) {}

    private static Pic p(VfxShape travel, VfxShape burst, float lift, float scale, float max) { return new Pic(travel, burst, lift, scale, max); }

    private static final Pic FENRIR = p(VfxShape.MIST_MAGIC, VfxShape.SOUL_VORTEX, 0f, 0.35f, 3f);               // pale soul-mist rush; soul wisps spiral in
    private static final Pic JORMUNGANDR = p(VfxShape.WATER_DRAGON, VfxShape.WATER_NEST, 0f, 0.5f, 4f);          // a sea serpent of water; a dome of whirling water
    private static final Pic CERBERUS = p(VfxShape.DARK_SLASH_AVIDYA, VfxShape.DARK_SLASH_WILD, 0f, 0.25f, 1.5f);  // a crescent of dark; the three heads' crossed cuts
    private static final Pic MINOTAUR = p(VfxShape.EARTH_RAMPAGE, VfxShape.HERCULES_SHOCKWAVE, 0f, 0.4f, 3f);     // the charge; the shock where it lands
    private static final Pic HYDRA = p(VfxShape.POISON_BREATH, VfxShape.WATER_BURST, 0f, 0.35f, 2f);              // a cone of toxic mist; a swamp splash
    private static final Pic CU_SITH = p(VfxShape.DARK_IAI, VfxShape.CORPSE_RAISE, 0f, 0.35f, 1.5f);              // one bright cut with a black afterimage; a soul torn up
    private static final Pic NIDHOGGR = p(VfxShape.DARK_THRUST, VfxShape.NIHILITY_ZONE, 0f, 0.5f, 4f);            // a spear of darkness; a dead dome with cracks
    private static final Pic HAFGUFA = p(null, VfxShape.GRAVITY_WELL, 0f, 0.5f, 4f);                              // no travelling shape; a well that draws everything in
    private static final Pic KRAKEN = p(VfxShape.WATER_JAVELIN, VfxShape.WATER_CRADLE, 0f, 0.4f, 3f);             // a lance of water; a whirling sphere
    private static final Pic TYPHON = p(VfxShape.FIRE_SPEAR, VfxShape.LIGHTNING_GOD, 0f, 0.5f, 4f);               // a spiralling flame spear; a lightning pillar
    private static final Pic SCYLLA = p(VfxShape.EARTH_SPIKES, VfxShape.WIND_TORNADO, 0f, 0.4f, 3f);              // rock teeth; the whirlpool as a funnel
    private static final Pic SPHINX = p(VfxShape.DIVINE_RAY, VfxShape.DIVINE_PILLAR, 0f, 0.4f, 3f);               // a beam of judgement; a pillar of light with halo rings
    private static final Pic PEGASUS = p(VfxShape.LIGHT_BLADE, VfxShape.LIGHT_FLARE, 1.4f, 0.33f, 1.5f);          // a streak of light; a flare of light
    private static final Pic OROCHI = p(VfxShape.BLOOD_ARCS, VfxShape.BLOOD_SIGIL, 0f, 0.5f, 4f);                 // crimson arcs leaping between heads; a thorned sigil
    private static final Pic KITSUNE = p(VfxShape.FLAME_TRAIL, VfxShape.DREAM_DOME, 0f, 0.4f, 3f);                // foxfire; a dome of dream
    private static final Pic NUE = p(VfxShape.SPACE_TEAR, VfxShape.SPACE_CRACK, 0f, 0.3f, 1.5f);                  // a slit of dread; a cracked pane
    private static final Pic AMMIT = p(null, VfxShape.FOOD_MAW, 0f, 0.35f, 2f);                                   // a maw that devours
    private static final Pic APOPHIS = p(VfxShape.DARK_SLASH_DIMENSION, VfxShape.DARK_BLACK_HOLE, 0f, 0.4f, 3f);  // a fracture cut through space; a black hole

    /** The picture of this look kind, or null when the kind has none (the nine first wills have their own switches in BeastWillAbilities). */
    public static Pic of(int kind) {
        return switch (kind) {
            case BeastWillLook.FENRIR -> FENRIR;
            case BeastWillLook.JORMUNGANDR -> JORMUNGANDR;
            case BeastWillLook.CERBERUS -> CERBERUS;
            case BeastWillLook.MINOTAUR -> MINOTAUR;
            case BeastWillLook.HYDRA -> HYDRA;
            case BeastWillLook.CU_SITH -> CU_SITH;
            case BeastWillLook.NIDHOGGR -> NIDHOGGR;
            case BeastWillLook.HAFGUFA -> HAFGUFA;
            case BeastWillLook.KRAKEN -> KRAKEN;
            case BeastWillLook.TYPHON -> TYPHON;
            case BeastWillLook.SCYLLA -> SCYLLA;
            case BeastWillLook.SPHINX -> SPHINX;
            case BeastWillLook.PEGASUS -> PEGASUS;
            case BeastWillLook.OROCHI -> OROCHI;
            case BeastWillLook.KITSUNE -> KITSUNE;
            case BeastWillLook.NUE -> NUE;
            case BeastWillLook.AMMIT -> AMMIT;
            case BeastWillLook.APOPHIS -> APOPHIS;
            default -> null;
        };
    }
}
