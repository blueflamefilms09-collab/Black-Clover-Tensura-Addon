package com.newuniverse.nusmp.requip;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the pieces of each armour (which geo model, which model part it is anchored on, when it forges in and from which side it flies in) are data the client painter, the documentation and the
 * tests share, and the numbers must obey the Djinn Full Equip's rules: a stagger of 0 to 12 ticks, a fly-in direction of about 2 to 16 model pixels. The painter turns these rows into
 * {@code WaterAuraRig.Piece}s, which forge in and shatter out through {@code EquipAnimation}. No Minecraft classes here, so the table is unit-tested.
 * <p>
 * Models: {@code assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json} (+ animation, texture, {@code _glow} map), written by {@code tools/gen_erza_armour.py} (the 1.99.0 remake at the Djinn's
 * quality; the 1.96.0 generator was {@code tools/gen_erza_requip_geo.py}), which must define exactly the pieces listed here.
 * <p>
 * 1.99.0: every armour is a full set (helm, gorget, chest, backplate, belt, skirt, two pauldrons, two vambraces, two gauntlets, two cuisses, two greaves, two sabatons) plus its own signature pieces
 * (wings, halo, tail, cape, backdrop, ribbon). The forging order follows the weight of a piece ({@link #weightOf}): the light underlayers first, the plates, then the heavy cuirass and back, then the
 * wings and the halo, the heaviest backdrop last.
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
        public Piece withDelay(float d) { return new Piece(name, anchor, d, dx, dy, dz, glass, detail, head, clip, flap, glow); }
    }

    private static Piece p(String name, Anchor a, float delay, float dx, float dy, float dz) { return new Piece(name, a, delay, dx, dy, dz, false, false, false, null, false, 1f); }

    /**
     * How heavy a piece is, 1 (a strap, a collar) to 7 (the backdrop behind the back): a piece never forges before a lighter one. The delay of every piece below is chosen so that this holds
     * (the unit test {@code ErzaArmourPiecesTest} pins it).
     */
    public static int weightOf(String piece) {
        return switch (piece) {
            case "belt", "gorget", "ribbon" -> 1;
            case "gauntlet_r", "gauntlet_l", "vambrace_r", "vambrace_l", "sabaton_r", "sabaton_l" -> 2;
            case "greave_r", "greave_l", "cuisse_r", "cuisse_l", "skirt", "helm" -> 3;
            case "pauldron_r", "pauldron_l" -> 4;
            case "chest", "backplate", "cape", "tail" -> 5;
            case "wing_r", "wing_l", "halo" -> 6;
            case "backdrop" -> 7;
            default -> 3;
        };
    }

    // ------------------------------------------------------------ the standard pieces: the Djinn's fly-in sides (head from above, arms from their side, legs from below, the back from behind)
    static Piece belt() { return p("belt", Anchor.BODY, 0, 0, 8, -8).withDetail(); }
    static Piece gorget() { return p("gorget", Anchor.BODY, 1, 0, -10, -4).withDetail(); }
    static Piece ribbon() { return p("ribbon", Anchor.HEAD, 1, 0, -14, 4).withClip("idle").withDetail().withHead(); }
    static Piece gauntletR() { return p("gauntlet_r", Anchor.ARM_R, 2, -10, 8, -6).withDetail(); }
    static Piece gauntletL() { return p("gauntlet_l", Anchor.ARM_L, 2, 10, 8, -6).withDetail(); }
    static Piece vambraceR() { return p("vambrace_r", Anchor.ARM_R, 3, -12, 2, -6); }
    static Piece vambraceL() { return p("vambrace_l", Anchor.ARM_L, 3, 12, 2, -6); }
    static Piece sabatonR() { return p("sabaton_r", Anchor.LEG_R, 3, -3, 14, -8); }
    static Piece sabatonL() { return p("sabaton_l", Anchor.LEG_L, 4, 3, 14, -8); }
    static Piece greaveR() { return p("greave_r", Anchor.LEG_R, 5, -3, 14, -6); }
    static Piece greaveL() { return p("greave_l", Anchor.LEG_L, 6, 3, 14, -6); }
    static Piece cuisseR() { return p("cuisse_r", Anchor.LEG_R, 5, -4, 12, -6); }
    static Piece cuisseL() { return p("cuisse_l", Anchor.LEG_L, 6, 4, 12, -6); }
    static Piece skirt() { return p("skirt", Anchor.BODY, 6, 0, 10, -6).withClip("idle").withDetail(); }
    static Piece helm() { return p("helm", Anchor.HEAD, 6, 0, -16, 0).withHead(); }
    static Piece pauldronR() { return p("pauldron_r", Anchor.ARM_R, 7, -14, -4, 0); }
    static Piece pauldronL() { return p("pauldron_l", Anchor.ARM_L, 7, 14, -4, 0); }
    static Piece chest() { return p("chest", Anchor.BODY, 8, 0, 0, -14); }
    static Piece backplate() { return p("backplate", Anchor.BODY, 9, 0, 0, 12); }
    static Piece cape() { return p("cape", Anchor.BODY, 9, 0, -4, 12).withClip("idle").withDetail(); }
    static Piece tail() { return p("tail", Anchor.BODY, 9, 0, 6, 14).withClip("idle").withDetail(); }
    static Piece wingR() { return p("wing_r", Anchor.BODY, 10, -10, -4, 12).withClip("idle").withFlap(); }
    static Piece wingL() { return p("wing_l", Anchor.BODY, 10, 10, -4, 12).withClip("idle").withFlap(); }
    static Piece halo() { return p("halo", Anchor.HEAD, 10, 0, -16, 0).withClip("idle").withGlass().withDetail().withHead(); }
    static Piece backdrop() { return p("backdrop", Anchor.BODY, 12, 0, 0, 12).withClip("idle").withGlass().withDetail(); }

    private static final Map<Armour, List<Piece>> SETS = new EnumMap<>(Armour.class);

    /** The eighteen pieces every armour has, then its own. */
    private static List<Piece> full(Piece... extras) {
        List<Piece> l = new ArrayList<>(List.of(belt(), gorget(), gauntletR(), gauntletL(), vambraceR(), vambraceL(), sabatonR(), sabatonL(), greaveR(), greaveL(), cuisseR(), cuisseL(),
                skirt(), helm(), pauldronR(), pauldronL(), chest(), backplate()));
        l.addAll(List.of(extras));
        return List.copyOf(l);
    }

    static {
        SETS.put(Armour.HEART_KREUZ, full(ribbon()));
        SETS.put(Armour.HEAVENS_WHEEL, full(wingR(), wingL(), halo(), backdrop()));                      // backdrop = the wheel of swords turning behind the back
        SETS.put(Armour.FLAME_EMPRESS, full(tail(), halo()));
        SETS.put(Armour.LIGHTNING_EMPRESS, full(wingR(), wingL(), halo()));
        SETS.put(Armour.SEA_EMPRESS, full(wingR().withGlass(), wingL().withGlass(), halo()));            // the fins
        SETS.put(Armour.BLACK_WING, full(wingR(), wingL()));
        SETS.put(Armour.CLEAR_HEART, full(ribbon(), tail()));                                            // cloth: ribbons and a trailing scarf
        SETS.put(Armour.NAKAGAMI, full(cape(), halo()));
        SETS.put(Armour.GRAND_CHARIOT, full(wingR(), wingL(), halo(), backdrop()));
        SETS.put(Armour.ADAMANTINE, full(cape()));                                                       // the mantle
        SETS.put(Armour.PURGATORY, full(cape(), backdrop()));                                            // backdrop = the spikes of the back
        SETS.put(Armour.FLIGHT_ARMOR, full(wingR(), wingL(), ribbon()));
        SETS.put(Armour.WIND_GOD, full(wingR(), wingL(), halo(), tail()));
        SETS.put(Armour.FAIRY_ARMOR, full(wingR().withGlass(), wingL().withGlass(), halo()));
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
