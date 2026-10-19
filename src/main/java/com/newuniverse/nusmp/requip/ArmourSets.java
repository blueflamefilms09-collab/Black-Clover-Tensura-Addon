package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the pieces of each armour (which geo model, which model part it is anchored on, when it forges in and from which side it flies in) are data the client painter, the documentation and the
 * tests share, and the numbers must obey the Djinn Full Equip's rules: a stagger of 0 to 12 ticks, a fly-in direction of about 2 to 14 model pixels. The painter turns these rows into
 * {@code WaterAuraRig.Piece}s, which forge in and shatter out through {@code EquipAnimation}. No Minecraft classes here, so the table is unit-tested.
 * <p>
 * Models: {@code assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json} (+ animation, texture, {@code _glow} map), written by {@code tools/gen_erza_requip_geo.py}, which must define exactly the
 * pieces listed here.
 */
public final class ArmourSets {
    private ArmourSets() {}

    public enum Anchor { HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L }

    /**
     * One piece of an armour. {@code glass}: always translucent; {@code detail}: skipped beyond 24 blocks; {@code head}: skipped for the local player in first person; {@code clip}: the model's looped
     * animation clip (or null); {@code flap}: the clip runs faster in the air; {@code glow}: glow multiplier.
     */
    public record Piece(String name, Anchor anchor, float delay, float dx, float dy, float dz, boolean glass, boolean detail, boolean head, String clip, boolean flap, float glow) {
        public Piece withGlass() { return new Piece(name, anchor, delay, dx, dy, dz, true, detail, head, clip, flap, glow); }
        public Piece withDetail() { return new Piece(name, anchor, delay, dx, dy, dz, glass, true, head, clip, flap, glow); }
        public Piece withHead() { return new Piece(name, anchor, delay, dx, dy, dz, glass, detail, true, clip, flap, glow); }
        public Piece withClip(String c) { return new Piece(name, anchor, delay, dx, dy, dz, glass, detail, head, c, flap, glow); }
        public Piece withFlap() { return new Piece(name, anchor, delay, dx, dy, dz, glass, detail, head, clip, true, glow); }
        public Piece withGlow(float g) { return new Piece(name, anchor, delay, dx, dy, dz, glass, detail, head, clip, flap, g); }
    }

    private static Piece p(String name, Anchor a, float delay, float dx, float dy, float dz) { return new Piece(name, a, delay, dx, dy, dz, false, false, false, null, false, 1f); }

    // ------------------------------------------------------------ the standard pieces: the Djinn's delays and fly-in sides (head from above, arms from their side, greaves from below, the back from behind)
    static Piece helm() { return p("helm", Anchor.HEAD, 0, 0, -16, 0).withHead(); }
    static Piece chest() { return p("chest", Anchor.BODY, 1, 0, 0, -14); }
    static Piece pauldronR() { return p("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0); }
    static Piece pauldronL() { return p("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0); }
    static Piece vambraceR() { return p("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).withGlow(0f); }
    static Piece vambraceL() { return p("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).withGlow(0f); }
    static Piece gauntletR() { return p("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6); }
    static Piece gauntletL() { return p("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6); }
    static Piece skirt() { return p("skirt", Anchor.BODY, 5, 0, 10, -6).withDetail().withGlow(0f); }
    static Piece greaveR() { return p("greave_r", Anchor.LEG_R, 6, -3, 14, -6).withGlow(0f); }
    static Piece greaveL() { return p("greave_l", Anchor.LEG_L, 7, 3, 14, -6).withGlow(0f); }
    static Piece wingR() { return p("wing_r", Anchor.BODY, 8, -10, -4, 12).withClip("idle").withFlap().withGlass(); }
    static Piece wingL() { return p("wing_l", Anchor.BODY, 8, 10, -4, 12).withClip("idle").withFlap().withGlass(); }
    static Piece halo() { return p("halo", Anchor.HEAD, 10, 0, -16, 0).withClip("idle").withGlass().withDetail().withHead(); }
    static Piece backdrop() { return p("backdrop", Anchor.BODY, 12, 0, 0, 12).withClip("idle").withGlass().withDetail(); }
    static Piece cape() { return p("cape", Anchor.BODY, 9, 0, -4, 12).withClip("idle").withDetail().withGlow(0f); }
    static Piece tail() { return p("tail", Anchor.BODY, 9, 0, 6, 14).withClip("idle").withDetail(); }

    private static final Map<Armour, List<Piece>> SETS = new EnumMap<>(Armour.class);

    private static List<Piece> full(Piece... extras) {
        List<Piece> l = new ArrayList<>(List.of(chest(), pauldronR(), pauldronL(), vambraceR(), vambraceL(), greaveR(), greaveL(), helm()));
        l.addAll(List.of(extras));
        return List.copyOf(l);
    }

    static {
        SETS.put(Armour.HEART_KREUZ, full(skirt(), gauntletR(), gauntletL()));
        SETS.put(Armour.HEAVENS_WHEEL, full(skirt(), wingR(), wingL(), halo(), backdrop()));        // backdrop = the wheel of swords turning behind the back
        SETS.put(Armour.FLAME_EMPRESS, full(skirt(), gauntletR(), gauntletL(), halo(), tail()));
        SETS.put(Armour.LIGHTNING_EMPRESS, full(skirt(), gauntletR(), gauntletL(), halo(), wingR(), wingL()));
        SETS.put(Armour.SEA_EMPRESS, full(skirt(), gauntletR(), gauntletL(), halo(), wingR().withGlass(), wingL().withGlass()));   // the fins
        SETS.put(Armour.BLACK_WING, full(skirt(), gauntletR(), gauntletL(), wingR(), wingL()));
        SETS.put(Armour.CLEAR_HEART, List.of(chest(), vambraceR(), vambraceL(), skirt(), greaveR(), greaveL(), helm()));     // cloth: no plate on the shoulders, a ribbon for the helm
        SETS.put(Armour.NAKAGAMI, full(skirt(), gauntletR(), gauntletL(), halo(), cape()));
        SETS.put(Armour.GRAND_CHARIOT, full(skirt(), wingR(), wingL(), halo(), backdrop()));
        SETS.put(Armour.ADAMANTINE, full(skirt(), gauntletR(), gauntletL()));
        SETS.put(Armour.PURGATORY, full(skirt(), gauntletR(), gauntletL(), backdrop()));                                     // backdrop = the spikes of the back
        SETS.put(Armour.FLIGHT_ARMOR, full(skirt(), gauntletR(), gauntletL(), wingR(), wingL()));
        SETS.put(Armour.WIND_GOD, full(skirt(), gauntletR(), gauntletL(), wingR(), wingL(), halo(), tail()));
        SETS.put(Armour.FAIRY_ARMOR, full(skirt(), gauntletR(), gauntletL(), wingR(), wingL(), halo()));
    }

    /** The pieces of an armour, in draw order. */
    public static List<Piece> of(Armour a) { return SETS.get(a); }

    /** The geo model id of a piece: {@code erza_<armour>_arm_<piece>}. */
    public static String modelId(Armour a, Piece p) { return a.geoKey() + "_arm_" + p.name(); }

    /** Every model id the table needs (for the asset tests). */
    public static List<String> allModelIds() {
        List<String> ids = new ArrayList<>();
        for (Armour a : Armour.values()) for (Piece p : of(a)) ids.add(modelId(a, p));
        return ids;
    }

    /** The largest stagger of any piece of the armour (ticks): the time from the first piece to the last one starting to forge. */
    public static float maxDelay(Armour a) {
        float m = 0;
        for (Piece p : of(a)) m = Math.max(m, p.delay());
        return m;
    }
}
