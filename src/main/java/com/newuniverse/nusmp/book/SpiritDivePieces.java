package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 1.99.0 Great Spirits: which pieces the Spirit Dive draws for an element at a stage. Pure (no game classes, unit tested by SpiritDivePiecesTest); the server picks the stage
 * ({@link SpiritDiveForm}), the style number of the {@code Aura.SPIRIT_DIVE} lease carries element and stage to the clients ({@link #style}), and the painter
 * ({@code client.aura.SpiritDiveAura}) draws exactly the pieces listed here, each forged in and shattered out by {@code EquipAnimation}.
 *
 * <p>Canon (the owner's text). Spirit Dive (Spirit Assimilation): the mage merges the spirit into the body; the body is altered and cloaked in dense elemental mana.
 * <ul>
 *   <li><b>Wind</b> (Yuno, Sylph). The FIRST dive transforms only the LEFT half of the body: one wind wing, half a crown, a halo-like ring round the wrist (and a half mantle, sleeve and greave of
 *       wind mana). The FULL dive: two wings, the full crown, symmetrical, a ring round each wrist. {@link Stage#UPGRADE} is the moment a half dive becomes full: the left half stays on and the
 *       missing right half forges in.</li>
 *   <li><b>Water</b> (Noelle, Undine). Her usual water armour, the Valkyrie armour, at the half stage; at the full stage the SAINT VALKYRIE DRESS: the same water armour with feathered wing-like
 *       parts (wings, forearm feathers), a halo above the head and a gold sash, white - gold - blue.</li>
 *   <li><b>Fire</b> (Fuegoleon, Salamander). Arms cloaked in intense fire mana with draconic scales, claws and fiery wings: small wings at the half stage, the full pair, draconic greaves and chest
 *       at the full stage.</li>
 *   <li><b>Earth</b> (the earth spirit, never named or shown: generic). Mossy stone plates; the full stage adds greaves, a brow crown of crystals, a back slab and floating rocks.</li>
 * </ul>
 * The half / full stage is the one {@link ArmourRules#diveScale} already decides (the first {@link ArmourRules#HALF_DIVES} dives of an unmastered book are half dives).</p>
 *
 * <p>A piece id is {@code "<geo key>:<name>"}: the model is {@code assets/nusmp/geo/entity/<key>_arm_<name>.geo.json} (the same convention as client.aura.WaterAuraRig; the key
 * {@code water} is the old water armour, {@code spiritdive} the new pieces of tools/gen_spirit_dive_geo.py).</p>
 */
public final class SpiritDivePieces {
    private SpiritDivePieces() {}

    /**
     * True: the Spirit Dive is drawn by {@code Aura.SPIRIT_DIVE} ({@code client.aura.SpiritDiveAura}, driven by {@link SpiritDiveForm}) and {@code GrimoireBook.diveArmor()} starts no
     * {@code blackclover.ModeArmor} overlay for it. False brings back the 1.98 overlays (Salamander, Sylph, Valkyrie) and the Gnome Plate of Earth.
     */
    public static final boolean LOOK = true;

    /** The four spirit magics that dive. The ordinal is part of the style number: append only. */
    public enum Element { WIND, WATER, FIRE, EARTH }

    /** HALF: the first dives. FULL: later dives. UPGRADE: FULL reached during a half dive (the half's pieces are already on). The ordinal is part of the style number: append only. */
    public enum Stage { HALF, FULL, UPGRADE }

    /** The body parts a piece may sit on. */
    public enum Anchor { HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L }

    /**
     * One piece of the dive.
     *
     * @param id     "key:name", see the class text
     * @param delay  stagger in ticks, 0 to 12 (the rule of EquipAnimation: the last piece must be on before the shatter window opens)
     * @param dx dy dz the direction in model pixels the piece flies in from (EquipAnimation.begin)
     * @param glass  always translucent (water, wind mana, flame sheets)
     * @param flap   its clip runs faster while the wearer is in the air (wings)
     * @param clip   the animation clip played (looped), "" = none
     * @param detail skipped for players farther than 24 blocks
     * @param head   sits on the face: skipped for the local player in first person
     * @param glow   scales the glow of this piece
     */
    public record Def(String id, Anchor anchor, float delay, float dx, float dy, float dz, boolean glass, boolean flap, String clip, boolean detail, boolean head, float glow) {
        public String key() { return SpiritDivePieces.key(id); }
        public String name() { return SpiritDivePieces.name(id); }
    }

    // ------------------------------------------------------------------ the style number of the aura lease (a byte)
    /** element in bits 0-1, stage in bits 2-3 (0..11); since the Spirit Dive remake the TIER (grade: {@link SpiritDiveTier.Grade}) in bits 4-5 (0 = the standard look: the old numbers are unchanged). */
    public static int style(Element e, Stage s) { return e.ordinal() | (s.ordinal() << 2); }

    /** The style number with the grade of the look (the character tier of Yuno, Fuegoleon or Noelle). */
    public static int style(Element e, Stage s, SpiritDiveTier.Grade g) { return style(e, s) | (g.ordinal() << 4); }

    public static boolean valid(int style) {
        return style >= 0 && style < 64 && (style & 3) < Element.values().length && ((style >> 2) & 3) < Stage.values().length && (style >> 4) < SpiritDiveTier.Grade.values().length;
    }

    /** The element of a style number (WIND for a number that makes no sense: the painter never throws on a bad byte). */
    public static Element element(int style) { return valid(style) ? Element.values()[style & 3] : Element.WIND; }

    public static Stage stage(int style) { return valid(style) ? Stage.values()[(style >> 2) & 3] : Stage.HALF; }

    /** The grade (tier) of a style number: STANDARD for a bad byte and for a grade that does not belong to the element. */
    public static SpiritDiveTier.Grade grade(int style) {
        return valid(style) ? SpiritDiveTier.effective(element(style), SpiritDiveTier.Grade.values()[style >> 4]) : SpiritDiveTier.Grade.STANDARD;
    }

    /** The stage a dive starts at, from the scale {@link ArmourRules#diveScale} gave it: below 1 is a half dive. */
    public static Stage stageFor(float diveScale) { return diveScale < 1f ? Stage.HALF : Stage.FULL; }

    /** FULL and UPGRADE are both the full dive. */
    public static boolean full(Stage s) { return s != Stage.HALF; }

    /** The element of a magic's name ({@code MagicType.name()}): FIRE magic is FLAME, WIND, WATER, EARTH; null for every other magic. */
    public static Element elementOf(String magic) {
        if (magic == null) return null;
        return switch (magic.toUpperCase(Locale.ROOT)) {
            case "WIND" -> Element.WIND;
            case "WATER" -> Element.WATER;
            case "FLAME", "FIRE" -> Element.FIRE;
            case "EARTH" -> Element.EARTH;
            default -> null;
        };
    }

    /** The Saint Valkyrie Dress: the full stage of the water dive. */
    public static boolean saint(Element e, Stage s) { return e == Element.WATER && full(s); }

    // ------------------------------------------------------------------ the pieces
    private static Def d(String id, Anchor a, float delay, float dx, float dy, float dz, boolean glass, boolean flap, String clip, boolean detail, boolean head, float glow) {
        return new Def(id, a, delay, dx, dy, dz, glass, flap, clip, detail, head, glow);
    }

    private static final String S = "spiritdive:", W = "water:";

    /** Every piece the dive can draw, by id (the painter looks its definition up here). */
    private static final List<Def> ALL = new ArrayList<>();

    static {
        // ---- wind: every piece is wind mana (translucent). Left first, then right; the right half only at the full stage.
        for (int side = 0; side < 2; side++) {
            String t = side == 0 ? "l" : "r";
            float sg = side == 0 ? 1f : -1f;
            Anchor arm = side == 0 ? Anchor.ARM_L : Anchor.ARM_R, leg = side == 0 ? Anchor.LEG_L : Anchor.LEG_R;
            float lag = side == 0 ? 0f : 1f;                                                      // the right half follows the left by a tick or so in an upgrade
            ALL.add(d(S + "wind_mantle_" + t, Anchor.BODY, 1 + lag, sg * 6f, 0f, -14f, true, false, "", false, false, 1f));
            ALL.add(d(S + "wind_sleeve_" + t, arm, 3 + lag, sg * 12f, 2f, -6f, true, false, "idle", true, false, 0.8f));
            ALL.add(d(S + "wind_greave_" + t, leg, 5 + lag, sg * 3f, 14f, -6f, true, false, "idle", true, false, 0.8f));
            ALL.add(d(S + "wind_ring_" + t, arm, 4 + lag, sg * 10f, 0f, 0f, true, false, "idle", false, false, 1.2f));
            ALL.add(d(S + "wind_crown_" + t, Anchor.HEAD, 0 + lag, 0f, -16f, 0f, true, false, "idle", false, true, 1.2f));
            ALL.add(d(S + "wind_wing_" + t, Anchor.BODY, 7 + lag, sg * 10f, -4f, 12f, true, true, "idle", false, false, 1f));
        }
        ALL.add(d(S + "wind_crown_c", Anchor.HEAD, 2f, 0f, -16f, 0f, true, false, "idle", false, true, 1.2f));
        // ---- water: the old Valkyrie set (key water) with the delays of client.aura.WaterAura, and the new Saint pieces
        ALL.add(d(W + "chest", Anchor.BODY, 1, 0, 0, -14, true, false, "", false, false, 1f));
        ALL.add(d(W + "skirt", Anchor.BODY, 5, 0, 10, -6, true, false, "idle", true, false, 1f));
        ALL.add(d(W + "pauldron_r", Anchor.ARM_R, 2, -14, -4, 0, true, false, "", false, false, 1f));
        ALL.add(d(W + "pauldron_l", Anchor.ARM_L, 2, 14, -4, 0, true, false, "", false, false, 1f));
        ALL.add(d(W + "vambrace_r", Anchor.ARM_R, 4, -12, 2, -6, true, false, "", false, false, 0.5f));
        ALL.add(d(W + "vambrace_l", Anchor.ARM_L, 4, 12, 2, -6, true, false, "", false, false, 0.5f));
        ALL.add(d(W + "gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6, true, false, "", false, false, 1f));
        ALL.add(d(W + "gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6, true, false, "", false, false, 1f));
        ALL.add(d(W + "greave_r", Anchor.LEG_R, 6, -3, 14, -6, true, false, "", false, false, 0.5f));
        ALL.add(d(W + "greave_l", Anchor.LEG_L, 7, 3, 14, -6, true, false, "", false, false, 0.5f));
        ALL.add(d(W + "helm", Anchor.HEAD, 0, 0, -16, 0, true, false, "", false, true, 1f));
        ALL.add(d(W + "wing_r", Anchor.BODY, 8, -10, -4, 12, true, true, "idle", false, false, 1f));
        ALL.add(d(W + "wing_l", Anchor.BODY, 8, 10, -4, 12, true, true, "idle", false, false, 1f));
        ALL.add(d(W + "tail", Anchor.BODY, 9, 0, 6, 14, true, false, "idle", true, false, 1f));
        ALL.add(d(W + "halo", Anchor.HEAD, 10, 0, -16, 0, true, false, "idle", true, true, 1f));
        ALL.add(d(W + "backdrop", Anchor.BODY, 12, 0, 0, 12, true, false, "idle", true, false, 1f));
        ALL.add(d(S + "saint_sash", Anchor.BODY, 6, 0, 8, -8, true, false, "idle", true, false, 1f));
        ALL.add(d(S + "saint_sleeve_r", Anchor.ARM_R, 6, -12, 2, 6, true, false, "idle", true, false, 1f));
        ALL.add(d(S + "saint_sleeve_l", Anchor.ARM_L, 6, 12, 2, 6, true, false, "idle", true, false, 1f));
        ALL.add(d(S + "saint_wing_r", Anchor.BODY, 9, -10, -4, 12, true, true, "idle", false, false, 1f));
        ALL.add(d(S + "saint_wing_l", Anchor.BODY, 9, 10, -4, 12, true, true, "idle", false, false, 1f));
        ALL.add(d(S + "saint_halo", Anchor.HEAD, 11, 0, -16, 0, true, false, "idle", false, true, 1.3f));
        // ---- fire: draconic scales (solid), flame sheets (glass)
        ALL.add(d(S + "fire_chest", Anchor.BODY, 1, 0, 0, -14, false, false, "", false, false, 1f));
        ALL.add(d(S + "fire_pauldron_r", Anchor.ARM_R, 1, -14, -4, 0, false, false, "", false, false, 1f));
        ALL.add(d(S + "fire_pauldron_l", Anchor.ARM_L, 1, 14, -4, 0, false, false, "", false, false, 1f));
        ALL.add(d(S + "fire_arm_r", Anchor.ARM_R, 2, -12, 2, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "fire_arm_l", Anchor.ARM_L, 2, 12, 2, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "fire_claws_r", Anchor.ARM_R, 4, -10, 8, -6, false, false, "", false, false, 1f));
        ALL.add(d(S + "fire_claws_l", Anchor.ARM_L, 4, 10, 8, -6, false, false, "", false, false, 1f));
        ALL.add(d(S + "fire_greave_r", Anchor.LEG_R, 5, -3, 14, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "fire_greave_l", Anchor.LEG_L, 6, 3, 14, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "fire_wingbud_r", Anchor.BODY, 6, -8, -4, 10, true, true, "idle", false, false, 1f));
        ALL.add(d(S + "fire_wingbud_l", Anchor.BODY, 6, 8, -4, 10, true, true, "idle", false, false, 1f));
        ALL.add(d(S + "fire_wing_r", Anchor.BODY, 8, -10, -4, 12, true, true, "idle", false, false, 1f));
        ALL.add(d(S + "fire_wing_l", Anchor.BODY, 8, 10, -4, 12, true, true, "idle", false, false, 1f));
        // ---- earth: stone (solid)
        ALL.add(d(S + "earth_chest", Anchor.BODY, 1, 0, 0, -14, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_pauldron_r", Anchor.ARM_R, 2, -14, -4, 0, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_pauldron_l", Anchor.ARM_L, 2, 14, -4, 0, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_vambrace_r", Anchor.ARM_R, 3, -12, 2, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_vambrace_l", Anchor.ARM_L, 3, 12, 2, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_gauntlet_r", Anchor.ARM_R, 4, -10, 8, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_gauntlet_l", Anchor.ARM_L, 4, 10, 8, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_greave_r", Anchor.LEG_R, 5, -3, 14, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_greave_l", Anchor.LEG_L, 6, 3, 14, -6, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_helm", Anchor.HEAD, 0, 0, -16, 0, false, false, "", false, true, 1f));
        ALL.add(d(S + "earth_back", Anchor.BODY, 7, 0, 0, 12, false, false, "", false, false, 0.9f));
        ALL.add(d(S + "earth_orbit", Anchor.BODY, 9, 0, 0, 0, false, false, "idle", true, false, 0.9f));
        // ---- the Spirit Dive remake (appended): the signature pieces of the character tier and the rock crown of the earth dive
        ALL.add(d(S + "wind_halo", Anchor.HEAD, 3, 0, -16, 0, true, false, "idle", false, true, 1.5f));                // Yuno: the crown halo
        ALL.add(d(S + "wind_blades_l", Anchor.BODY, 8, 10, -4, 12, true, true, "idle", false, false, 1.3f));            // Yuno: the wing-like wind blades
        ALL.add(d(S + "wind_blades_r", Anchor.BODY, 9, -10, -4, 12, true, true, "idle", false, false, 1.3f));
        ALL.add(d(S + "wind_hair", Anchor.HEAD, 4, 0, -10, 6, true, false, "idle", true, true, 1.4f));                  // Yuno: the hair streaming in the wind
        ALL.add(d(S + "fire_mane", Anchor.HEAD, 2, 0, -8, 8, true, false, "idle", false, true, 1.4f));                  // Fuegoleon: the flame mane
        ALL.add(d(S + "fire_cape", Anchor.BODY, 6, 0, 0, 12, true, false, "idle", false, false, 1.2f));                 // Fuegoleon: the flame cape
        ALL.add(d(S + "flame_arm_r", Anchor.ARM_R, 1, -12, 2, -6, false, false, "idle", false, false, 1.5f));            // Fuegoleon: the molten right arm (also the persistent arm of the Salamander contract)
        ALL.add(d(S + "water_ribbon_l", Anchor.BODY, 7, 10, -2, 10, true, false, "idle", false, false, 1.1f));          // Noelle: water ribbons
        ALL.add(d(S + "water_ribbon_r", Anchor.BODY, 8, -10, -2, 10, true, false, "idle", false, false, 1.1f));
        ALL.add(d(S + "water_rings", Anchor.HEAD, 10, 0, -16, 0, true, false, "idle", true, true, 1.3f));              // Noelle: the halo of water rings
        ALL.add(d(S + "water_train", Anchor.BODY, 5, 0, 8, 10, true, false, "idle", true, false, 1.1f));                // Noelle: the flowing train of the dress
        ALL.add(d(S + "earth_crown", Anchor.HEAD, 3, 0, -16, 0, false, false, "idle", false, true, 1.1f));              // the rock crown with floating crystals
    }

    /** The definition of a piece id (null for an unknown id). */
    public static Def def(String id) {
        for (Def x : ALL) if (x.id().equals(id)) return x;
        return null;
    }

    /** Every piece that exists, in a fixed order (assets are checked against it). */
    public static List<Def> all() { return Collections.unmodifiableList(ALL); }

    public static String key(String id) { int i = id.indexOf(':'); return i < 0 ? "" : id.substring(0, i); }

    public static String name(String id) { int i = id.indexOf(':'); return i < 0 ? id : id.substring(i + 1); }

    private static List<String> list(String... ids) { return List.of(ids); }

    private static List<String> join(List<String> a, List<String> b) {
        List<String> l = new ArrayList<>(a);
        l.addAll(b);
        return Collections.unmodifiableList(l);
    }

    private static List<String> except(List<String> a, String... gone) {
        List<String> l = new ArrayList<>(a);
        for (String g : gone) l.remove(g);
        return Collections.unmodifiableList(l);
    }

    private static final List<String> WIND_LEFT = list(S + "wind_mantle_l", S + "wind_sleeve_l", S + "wind_greave_l", S + "wind_ring_l", S + "wind_crown_l", S + "wind_wing_l");
    private static final List<String> WIND_RIGHT = list(S + "wind_mantle_r", S + "wind_sleeve_r", S + "wind_greave_r", S + "wind_ring_r", S + "wind_crown_r", S + "wind_wing_r");
    private static final List<String> WIND_FULL = join(join(WIND_LEFT, WIND_RIGHT), list(S + "wind_crown_c"));

    private static final List<String> WATER_VALKYRIE = list(W + "chest", W + "skirt", W + "pauldron_r", W + "pauldron_l", W + "vambrace_r", W + "vambrace_l", W + "gauntlet_r", W + "gauntlet_l",
            W + "greave_r", W + "greave_l", W + "helm", W + "wing_r", W + "wing_l", W + "tail", W + "halo", W + "backdrop");
    /** The Saint Valkyrie Dress: the same water armour, its water wings, droplet halo and backdrop given up for feathered wings, a true halo, feathered forearms and a gold sash. */
    private static final List<String> WATER_SAINT = join(except(WATER_VALKYRIE, W + "wing_r", W + "wing_l", W + "halo", W + "backdrop"),
            list(S + "saint_sash", S + "saint_sleeve_r", S + "saint_sleeve_l", S + "saint_wing_r", S + "saint_wing_l", S + "saint_halo"));

    private static final List<String> FIRE_HALF = list(S + "fire_pauldron_r", S + "fire_pauldron_l", S + "fire_arm_r", S + "fire_arm_l", S + "fire_claws_r", S + "fire_claws_l",
            S + "fire_wingbud_r", S + "fire_wingbud_l");
    private static final List<String> FIRE_FULL = join(except(FIRE_HALF, S + "fire_wingbud_r", S + "fire_wingbud_l"),
            list(S + "fire_chest", S + "fire_greave_r", S + "fire_greave_l", S + "fire_wing_r", S + "fire_wing_l"));

    private static final List<String> EARTH_HALF = list(S + "earth_chest", S + "earth_pauldron_r", S + "earth_pauldron_l", S + "earth_vambrace_r", S + "earth_vambrace_l",
            S + "earth_gauntlet_r", S + "earth_gauntlet_l");
    private static final List<String> EARTH_FULL = join(EARTH_HALF, list(S + "earth_greave_r", S + "earth_greave_l", S + "earth_helm", S + "earth_back", S + "earth_orbit"));

    /** The pieces drawn for an element at a stage, in draw order. UPGRADE draws the same pieces as FULL (the half's are already on, see {@link #alreadyOn}). */
    public static List<String> pieces(Element e, Stage s) {
        boolean full = full(s);
        return switch (e) {
            case WIND -> full ? WIND_FULL : WIND_LEFT;
            case WATER -> full ? WATER_SAINT : WATER_VALKYRIE;
            case FIRE -> full ? FIRE_FULL : FIRE_HALF;
            case EARTH -> full ? EARTH_FULL : EARTH_HALF;
        };
    }

    /** The pieces of the half dive: what is already on when a half dive upgrades. */
    public static List<String> halfPieces(Element e) { return pieces(e, Stage.HALF); }

    /**
     * True when the piece is already fully on at this stage: in an UPGRADE the pieces of the half dive that the full dive keeps stay on (no new forge-in), only the missing ones assemble.
     * At HALF and FULL every piece forges in from the start.
     */
    public static boolean alreadyOn(Element e, Stage s, String id) { return s == Stage.UPGRADE && halfPieces(e).contains(id) && pieces(e, s).contains(id); }

    /** The ids of the pieces that forge in during the upgrade (the missing half). */
    public static List<String> forging(Element e, Stage s) {
        List<String> out = new ArrayList<>();
        for (String id : pieces(e, s)) if (!alreadyOn(e, s, id)) out.add(id);
        return out;
    }

    // ------------------------------------------------------------------ the remade dive (Spirit Dive remake): the pieces per element, stage and tier
    private static final List<String> V2_WIND_HALF = list(S + "wind_mantle_l", S + "wind_ring_l", S + "wind_crown_l", S + "wind_wing_l");
    private static final List<String> V2_WIND_FULL = list(S + "wind_mantle_l", S + "wind_mantle_r", S + "wind_ring_l", S + "wind_ring_r", S + "wind_crown_l", S + "wind_crown_r", S + "wind_crown_c",
            S + "wind_wing_l", S + "wind_wing_r");
    private static final List<String> V2_YUNO_HALF = join(V2_WIND_HALF, list(S + "wind_blades_l"));
    private static final List<String> V2_YUNO_FULL = join(V2_WIND_FULL, list(S + "wind_blades_l", S + "wind_blades_r", S + "wind_halo", S + "wind_hair"));

    private static final List<String> V2_FIRE_HALF = list(S + "fire_pauldron_r", S + "fire_pauldron_l", S + "fire_arm_r", S + "fire_arm_l", S + "fire_claws_r", S + "fire_claws_l", S + "fire_wingbud_r",
            S + "fire_wingbud_l");
    private static final List<String> V2_FIRE_FULL = list(S + "fire_pauldron_r", S + "fire_pauldron_l", S + "fire_arm_r", S + "fire_arm_l", S + "fire_claws_r", S + "fire_claws_l", S + "fire_chest",
            S + "fire_wing_r", S + "fire_wing_l");
    private static final List<String> V2_FUEGO_HALF = list(S + "flame_arm_r", S + "fire_pauldron_l", S + "fire_arm_l", S + "fire_claws_l", S + "fire_wingbud_r", S + "fire_wingbud_l", S + "fire_mane");
    private static final List<String> V2_FUEGO_FULL = list(S + "flame_arm_r", S + "fire_pauldron_l", S + "fire_arm_l", S + "fire_claws_l", S + "fire_chest", S + "fire_wing_r", S + "fire_wing_l",
            S + "fire_mane", S + "fire_cape");

    private static final List<String> V2_WATER_HALF = list(W + "chest", W + "skirt", W + "pauldron_r", W + "pauldron_l", W + "gauntlet_r", W + "gauntlet_l", W + "helm", W + "wing_r", W + "wing_l", W + "halo");
    private static final List<String> V2_WATER_FULL = list(W + "chest", W + "skirt", W + "pauldron_r", W + "pauldron_l", W + "gauntlet_r", W + "gauntlet_l", W + "helm", S + "saint_sash", S + "saint_wing_r",
            S + "saint_wing_l", S + "saint_halo");
    private static final List<String> V2_NOELLE_HALF = join(V2_WATER_HALF, list(S + "water_ribbon_l", S + "water_ribbon_r", S + "water_train"));
    /** Noelle's Saint dress: the gold halo gives way to the halo of water rings (one halo, not two). */
    private static final List<String> V2_NOELLE_FULL = join(except(V2_WATER_FULL, S + "saint_halo"), list(S + "saint_sleeve_r", S + "saint_sleeve_l", S + "water_ribbon_l", S + "water_ribbon_r", S + "water_train",
            S + "water_rings"));

    private static final List<String> V2_EARTH_HALF = list(S + "earth_chest", S + "earth_pauldron_r", S + "earth_pauldron_l", S + "earth_gauntlet_r", S + "earth_gauntlet_l");
    private static final List<String> V2_EARTH_FULL = join(V2_EARTH_HALF, list(S + "earth_crown", S + "earth_orbit"));

    /**
     * The pieces of the REMADE dive for an element, stage and tier, in draw order (the look every client draws unless it chose the classic look: {@link #pieces(Element, Stage)} is that old look).
     * The standard tier is the light version (the signature pieces only); the character tier (Yuno on Wind, Fuegoleon on Fire, Noelle on Water) adds the character's own pieces. UPGRADE is the
     * pieces of FULL, as before. A grade that does not belong to the element is the standard tier.
     */
    public static List<String> pieces(Element e, Stage s, SpiritDiveTier.Grade g) {
        boolean full = full(s);
        SpiritDiveTier.Grade eg = SpiritDiveTier.effective(e, g);
        return switch (e) {
            case WIND -> eg == SpiritDiveTier.Grade.YUNO ? (full ? V2_YUNO_FULL : V2_YUNO_HALF) : (full ? V2_WIND_FULL : V2_WIND_HALF);
            case FIRE -> eg == SpiritDiveTier.Grade.FUEGOLEON ? (full ? V2_FUEGO_FULL : V2_FUEGO_HALF) : (full ? V2_FIRE_FULL : V2_FIRE_HALF);
            case WATER -> eg == SpiritDiveTier.Grade.NOELLE ? (full ? V2_NOELLE_FULL : V2_NOELLE_HALF) : (full ? V2_WATER_FULL : V2_WATER_HALF);
            case EARTH -> full ? V2_EARTH_FULL : V2_EARTH_HALF;
        };
    }

    /** The pieces of the half dive of the remade look: what is already on when a half dive upgrades. */
    public static List<String> halfPieces(Element e, SpiritDiveTier.Grade g) { return pieces(e, Stage.HALF, g); }

    /** {@link #alreadyOn(Element, Stage, String)} for the remade look. */
    public static boolean alreadyOn(Element e, Stage s, SpiritDiveTier.Grade g, String id) { return s == Stage.UPGRADE && halfPieces(e, g).contains(id) && pieces(e, s, g).contains(id); }

    /** True when the dive draws the Fuegoleon grade: the right arm is the molten flame arm instead of the draconic one. */
    public static boolean flameArm(Element e, SpiritDiveTier.Grade g) { return e == Element.FIRE && SpiritDiveTier.effective(e, g) == SpiritDiveTier.Grade.FUEGOLEON; }

    // ------------------------------------------------------------------ the body overlay (the translucent skin drawn under the pieces)
    public static final int SHELL_BODY = 1, SHELL_ARM_R = 2, SHELL_ARM_L = 4, SHELL_LEG_R = 8, SHELL_LEG_L = 16, SHELL_ALL = 31;

    /**
     * Which parts of the player's body wear the translucent mana skin: the half wind dive only the LEFT arm and leg (the left half transforms), the half fire dive the arms ("the arms cloaked"),
     * the half earth dive the body and arms; every full dive the whole body (never the head: the face stays the player's own).
     */
    public static int shellParts(Element e, Stage s) {
        if (full(s)) return SHELL_ALL;
        return switch (e) {
            case WIND -> SHELL_ARM_L | SHELL_LEG_L;
            case FIRE -> SHELL_ARM_R | SHELL_ARM_L;
            case EARTH -> SHELL_BODY | SHELL_ARM_R | SHELL_ARM_L;
            case WATER -> SHELL_ALL;
        };
    }

    /** True for the one dive that transforms only half of the body: the first wind dive (Yuno), the LEFT half. */
    public static boolean halfBody(Element e, Stage s) { return e == Element.WIND && s == Stage.HALF; }

    /** Visits every piece id of every element and stage once (the asset test and the self-test use it). */
    public static void forEachId(Consumer<String> sink) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (Element e : Element.values()) for (Stage s : Stage.values()) seen.addAll(pieces(e, s));
        for (Element e : Element.values()) for (Stage s : Stage.values()) for (SpiritDiveTier.Grade g : SpiritDiveTier.Grade.values()) seen.addAll(pieces(e, s, g));          // the remade look
        seen.forEach(sink);
    }
}
