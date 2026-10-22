package com.newuniverse.nusmp.book;

import java.util.List;

/**
 * Why: the owner asked for Light Magic and Bone Magic to be remade after the Black Clover wiki (English first, the Japanese wiki as the fallback). This pure class is the one table of that
 * remake: every spell of the two books with its page id, its English name, the Japanese name (kanji and romaji) shown in the page tooltip, the source it came from, and whether it is NEW,
 * a REPLACEMENT of an older page of the same spell (the old page id is kept, only what it casts changes) or KEPT as it was. It also holds the caps and the arithmetic of the casts
 * (how many swords, arrows and fangs, where an arrow lands, how a whip curls) so that the numbers are unit tested and the server code only calls them. No game classes: JUnit runs it as it is.
 * <p>
 * Page order (the mode numbers follow the order; saves keep their unlocked bits): the ten Light pages are the five old ones first (judgment, divine_punishment, healing_ray, lamp_of_avior,
 * light_speed) and the five new ones appended; the seven Bone pages are the five old ones first and two appended. With {@code magic.lightBoneJa} off the books show the old pages only.
 */
public final class LightBoneJa {
    private LightBoneJa() {}

    public enum Status { NEW, REPLACED, KEPT }

    /** One spell row: {@code verified} is false where the Japanese name could not be read from a fetched wiki page (see docs/magic/light_bone_ja.md). */
    public record Spell(String magic, String id, String english, String kanji, String romaji, String source, String user, Status status, boolean verified) {
        /** The tooltip line that names the spell in both languages. */
        public String title() { return english + " (" + kanji + ", " + romaji + ")"; }
    }

    // ================================================================ the page tables
    /** The five Light pages before the remake, in their old order (the new set keeps these ids at these places). */
    public static final List<String> LIGHT_OLD = List.of("judgment", "divine_punishment", "healing_ray", "lamp_of_avior", "light_speed");
    /** The Light pages of the remake: the old five, then the new ones. */
    public static final List<String> LIGHT_JA = List.of("judgment", "divine_punishment", "healing_ray", "lamp_of_avior", "light_speed",
            "bright_judgment_whip", "heaven_splitting_flash", "arrows_of_judgment", "avior_lamp", "absolute_monarch");
    public static final List<String> BONE_OLD = List.of("bone_spear", "bone_armor", "ossuary", "rib_cage", "bone_rain");
    public static final List<String> BONE_JA = List.of("bone_spear", "bone_armor", "ossuary", "rib_cage", "bone_rain", "demon_sword_dainsleif", "spatial_rupture");

    private static final String EN_LIGHT = "Black Clover Wiki (English), Light Magic page and its spell pages, read through a search extract (the wiki host itself could not be fetched here)";
    private static final String EN_BONE = "Black Clover Wiki (English), Bone Magic page and its spell pages, read through a search extract (the wiki host itself could not be fetched here)";
    private static final String OWNER = "the spell list the owner pasted from the Light Magic wiki page";
    private static final String JA_BONE = "Japanese sources found by search (ja.wikipedia, the 必殺技辞典 spell dictionary); the ja fandom page itself could not be fetched";

    public static final List<Spell> LIGHT = List.of(
            new Spell("LIGHT", "judgment", "Light Sword of Judgment", "審判の光剣", "Shinpan no Kōken", EN_LIGHT + "; " + OWNER, "Patolli (Licht), Rhya (copy)", Status.REPLACED, false),
            new Spell("LIGHT", "divine_punishment", "Light Shaft of Divine Punishment", "神罰の光芒", "Shinbatsu no Kōbō", EN_LIGHT + "; Japanese name from ja.wikipedia (search extract); " + OWNER, "Patolli (Licht)", Status.REPLACED, true),
            new Spell("LIGHT", "healing_ray", "Healing Ray of Light", "癒やしの光粒", "Iyashi no Kōryū", EN_LIGHT + "; Japanese name from ja.wikipedia (search extract); " + OWNER, "Lumiere, Rhya (copy)", Status.REPLACED, true),
            new Spell("LIGHT", "lamp_of_avior", "Lamp of Avior Gloria", "アヴィオールの灯・栄光", "Aviōru no Tomoshibi: Gloria", EN_LIGHT + "; Japanese name from pixiv encyclopedia (search extract); " + OWNER, "Lumiere Silvamillion Clover", Status.REPLACED, true),
            new Spell("LIGHT", "light_speed", "Light Speed", "光速移動", "Kōsoku Idō", "the Light Magic page text (speed is the attribute's defining trait); no spell page of that name", "Lumiere, Patolli", Status.KEPT, false),
            new Spell("LIGHT", "bright_judgment_whip", "Bright Judgment Whip", "裁きの光鞭", "Sabaki no Kōben", EN_LIGHT + "; Japanese name from pixiv encyclopedia (search extract); " + OWNER, "Patolli (Licht)", Status.NEW, true),
            new Spell("LIGHT", "heaven_splitting_flash", "Heaven-Splitting Flash", "裂天の閃光", "Retten no Senkō", EN_LIGHT + "; Japanese name from the namu.wiki spell list (search extract); " + OWNER, "Rhya (copy), Patolli (Licht)", Status.NEW, true),
            new Spell("LIGHT", "arrows_of_judgment", "Arrows of Judgment", "審判の矢", "Shinpan no Ya", EN_LIGHT + "; Japanese name from ja.wikipedia (search extract); " + OWNER, "Patolli (Licht)", Status.NEW, true),
            new Spell("LIGHT", "avior_lamp", "Lamp of Avior", "アヴィオールの灯", "Aviōru no Tomoshibi", EN_LIGHT + "; Japanese name from pixiv encyclopedia (search extract); " + OWNER, "Lumiere Silvamillion Clover", Status.NEW, true),
            new Spell("LIGHT", "absolute_monarch", "Absolute Monarch of Divine Brilliance", "神聖なる光輝の絶対君主", "Shinsei naru Kōki no Zettai Kunshu", OWNER + "; the Japanese name is a back-translation, the namu.wiki list calls the spell 神聖な光の絶対王", "Augustus Kira Clover (as listed)", Status.NEW, false));

    public static final List<Spell> BONE = List.of(
            new Spell("BONE", "bone_spear", "Bone Bullets", "骨弾", "Kotsudan", EN_BONE + " (Bone Bullets page: fragments from a hole in the palm, spinning); the Japanese name is a back-translation, no Japanese page was found", "Lucius Zogratis", Status.REPLACED, false),
            new Spell("BONE", "bone_armor", "Bone Armor", "骨の鎧", "Hone no Yoroi", "ja.wikipedia (Zenon's bones block nearly every physical blow) and the English wiki's Zenon page (bone armour at full devil power); not a wiki spell page of its own", "Zenon Zogratis", Status.KEPT, false),
            new Spell("BONE", "ossuary", "Eternal Fangs", "無間骨牙", "Mugen Kotsuga", EN_BONE + "; " + JA_BONE + " (first appears in chapter 235)", "Zenon Zogratis", Status.REPLACED, true),
            new Spell("BONE", "rib_cage", "Rib Cage", "肋骨の檻", "Rokkotsu no Ori", "kept from the old book: not a wiki spell, an original page that suits Bone Magic (bones that grow and branch)", "Bone Magic users", Status.KEPT, false),
            new Spell("BONE", "bone_rain", "Bone Rain", "骨雨", "Kotsuu", "kept from the old book: not a wiki spell, an original page that suits Bone Magic", "Bone Magic users", Status.KEPT, false),
            new Spell("BONE", "demon_sword_dainsleif", "Demon Sword Dainsleif", "魔剣ダーインスレイヴ", "Maken Dāinsureivu", EN_BONE + " (Demon Sword Dainsleif page: a bone sword coated in offensive Spatial Magic); " + JA_BONE + " (chapter 310)", "Zenon Zogratis", Status.NEW, true),
            new Spell("BONE", "spatial_rupture", "Spatial Rupture", "空間断裂", "Kūkan Danretsu", EN_BONE + " (Spatial Rupture page: flying slashes of Spatial Magic from the Dainsleif); the Japanese name is a back-translation of 空間を断裂する in the 必殺技辞典 text", "Zenon Zogratis", Status.NEW, false));

    public static List<Spell> spells(String magic) { return "LIGHT".equals(magic) ? LIGHT : BONE; }

    /** The row of a page id in one book, or null. */
    public static Spell byId(String magic, String id) {
        for (Spell s : spells(magic)) if (s.id().equals(id)) return s;
        return null;
    }

    /** The page ids of a book in the order it shows them. */
    public static List<String> ids(String magic, boolean ja) {
        return "LIGHT".equals(magic) ? (ja ? LIGHT_JA : LIGHT_OLD) : (ja ? BONE_JA : BONE_OLD);
    }

    // ================================================================ caps (a server must never be flooded by one cast)
    public static final int MAX_SWORDS = 8, MAX_ARROWS = 20, MAX_FANGS = 24, MAX_BULLETS = 5, MAX_WHIPS = 3, MAX_SLASHES = 3;
    /** Foes one pulse of a spell may touch. */
    public static final int MAX_HITS = 12;
    /** Props of one caster alive at once (both magics together): a new cast past the cap ends that caster's oldest prop first. */
    public static final int MAX_PROPS_PER_CASTER = 40;
    /** Props of both magics alive on the whole server: past it a new prop is not made (the spell still works, only its model is missing). */
    public static final int MAX_PROPS_GLOBAL = 160;
    public static final int REFUSE = 0, ADMIT = 1, ADMIT_AFTER_ENDING_OLDEST = 2;

    /** What to do with a new prop, from how many the caster and the whole server already have. Pure. */
    public static int admit(int casterLive, int globalLive) {
        if (globalLive >= MAX_PROPS_GLOBAL && casterLive <= 0) return REFUSE;
        if (casterLive >= MAX_PROPS_PER_CASTER) return ADMIT_AFTER_ENDING_OLDEST;
        if (globalLive >= MAX_PROPS_GLOBAL) return ADMIT_AFTER_ENDING_OLDEST;      // the server is full: this caster gives up its oldest instead of adding one
        return ADMIT;
    }

    /** Flying props are not moved by the server (that would cost a packet per tick each): the painter moves them from the age. The speed of a flying prop in blocks per tick is kept in 1/8 blocks in bits 8-15 of the param. */
    public static int speedBits(double blocksPerTick) { return clamp((int) Math.round(blocksPerTick * 8.0), 1, 255) << 8; }
    public static double speedOf(int param) { return ((param >> 8) & 0xFF) / 8.0; }
    /** How far a flying prop has travelled after {@code age} ticks, never past the end of its life. */
    public static double travelled(double speed, double age, double maxLife) { return speed * Math.max(0.0, Math.min(age, maxLife)); }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    // ================================================================ counts by the caster's EP modifier (0..4)
    public static int swordCount(int modifier) { return clamp(4 + modifier, 4, MAX_SWORDS); }
    public static int arrowCount(int modifier) { return clamp(12 + 2 * modifier, 12, MAX_ARROWS); }
    public static int fangCount(int modifier) { return clamp(16 + 2 * modifier, 16, MAX_FANGS); }
    public static int bulletCount(int modifier) { return clamp(3 + modifier / 2, 3, MAX_BULLETS); }
    public static int slashCount(int modifier) { return clamp(1 + modifier / 2, 1, MAX_SLASHES); }

    // ================================================================ Light numbers
    /** Light Sword of Judgment: blocks per tick of a sword (the fastest things in the mod) and raw damage of one. */
    public static double swordSpeed(float power) { return 6.0 * (0.85 + 0.25 * power); }
    public static final float SWORD_RAW = 5f;
    /** Bright Judgment Whip: three lashes, reach, width, raw damage per lash. */
    public static final int WHIP_LASHES = 3;
    public static final double WHIP_REACH = 13.0, WHIP_WIDTH = 1.1;
    public static final float WHIP_RAW = 6.5f;
    public static final double[] WHIP_FAN_DEG = {-24.0, 0.0, 24.0};
    /** Heaven-Splitting Flash: charge ticks, beam length by scale (blocks), beam width, raw damage, ticks the beam lingers. */
    public static final int FLASH_CHARGE = 8, FLASH_TICKS = 10;
    public static double flashLength(float scale) { return clamp((float) (30.0 * scale), 28f, 46f); }
    public static final double FLASH_WIDTH = 0.9;
    public static final float FLASH_RAW = 15f;
    /** Light Shaft of Divine Punishment: telegraph ticks, column radius by scale, the pulses and their spacing, raw damage per pulse. */
    public static final int SHAFT_TELEGRAPH = 14, SHAFT_PULSES = 3, SHAFT_GAP = 6, SHAFT_LIFE = 14 + 3 * 6 + 8;
    public static double shaftRadius(float scale) { return clamp((float) (2.6 * scale), 2.6f, 4.2f); }
    public static final float SHAFT_RAW = 8.5f;
    /** Arrows of Judgment: area radius, drop height, fall speed, waves, raw damage of one landing and its blast radius. */
    public static final double ARROW_RADIUS = 8.0, ARROW_HEIGHT = 22.0, ARROW_SPEED = 2.4, ARROW_BLAST = 1.7;
    public static final int ARROW_WAVES = 3, ARROW_WAVE_GAP = 6, ARROW_RANGE = 36;
    public static final float ARROW_RAW = 4.2f;
    /** How many ticks an arrow falls. */
    public static int fallTicks(double height, double speed) { return Math.max(1, (int) Math.ceil(height / Math.max(0.1, speed))); }
    /** The landing offset (x, z) of arrow i of n inside a disc of the given radius: a golden-angle spiral, so any count covers the disc evenly and no random is needed. */
    public static double[] arrowOffset(int i, int n, double radius) {
        double r = radius * Math.sqrt((i + 0.5) / Math.max(1, n)), a = i * 2.399963229728653;
        return new double[]{Math.cos(a) * r, Math.sin(a) * r};
    }
    /** Lamp of Avior: orb flight speed, blast radius, raw damage, blind ticks. */
    public static final double LAMP_SPEED = 2.6, LAMP_RADIUS = 6.0;
    public static final float LAMP_RAW = 11f;
    public static final int LAMP_BLIND = 50, LAMP_CHARGE = 8, LAMP_RANGE = 40;
    /** Lamp of Avior Gloria: charge ticks, blast radius, raw damage. */
    public static final int GLORIA_CHARGE = 26;
    public static double gloriaRadius(float size) { return 14.0 * size; }
    public static final float GLORIA_RAW = 13f;
    /** Healing Ray of Light: pulses, spacing, share of max health per pulse, radius that shares the light. */
    public static final int HEAL_PULSES = 3, HEAL_GAP = 12;
    public static final float HEAL_SHARE = 0.05f;
    public static final double HEAL_RADIUS = 3.5;
    public static final int HEAL_MAX_ALLIES = 5;
    /** Absolute Monarch of Divine Brilliance: ticks it stands, ticks between its blows, reach, raw damage of a blow, the radius it shelters allies in. */
    public static final int MONARCH_TICKS = 240, MONARCH_BLOW_GAP = 30;
    public static final double MONARCH_REACH = 14.0, MONARCH_SHELTER = 9.0;
    public static final float MONARCH_RAW = 8f;

    // ================================================================ Bone numbers
    /** Bone Bullets: speed, raw damage of one bullet, spread in degrees, the share of armour the spin ignores. */
    public static final double BULLET_SPEED = 2.6;
    public static final float BULLET_RAW = 4.5f, BULLET_BYPASS = 0.5f;
    public static final double BULLET_SPREAD_DEG = 5.0;
    /** Eternal Fangs: reach in blocks, waves, ticks between waves, ticks a fang takes to come out, raw damage of one wave, armour share the fangs ignore. */
    public static final double FANG_REACH = 9.0;
    public static final int FANG_WAVES = 3, FANG_WAVE_GAP = 6, FANG_OUT = 5, FANG_LIFE = 24;
    public static final float FANG_RAW = 5.5f, FANG_BYPASS = 0.4f;
    /** Demon Sword Dainsleif: swings, ticks between swings, reach of a swing, arc in degrees, raw damage of a swing, the sword's ticks, armour share a cut ignores. */
    public static final int DAINSLEIF_SWINGS = 3, DAINSLEIF_GAP = 6, DAINSLEIF_TICKS = 44;
    public static final double DAINSLEIF_REACH = 4.6, DAINSLEIF_ARC_DEG = 110.0;
    public static final float DAINSLEIF_RAW = 7.5f, SPACE_BYPASS = 0.9f;
    /** Spatial Rupture: slash speed, how far it flies, the width it cuts, raw damage. */
    public static final double SLASH_SPEED = 1.9, SLASH_WIDTH = 1.2;
    public static final int SLASH_LIFE = 12;
    public static final float SLASH_RAW = 8.5f;

    /**
     * The direction of fang i of n: a golden-angle spiral over the part of the sphere above the feet (y from 0.05 to 0.9), so the fangs leave the body in every direction and never into
     * the ground. A unit vector {x, y, z}.
     */
    public static double[] fangDirection(int i, int n) {
        double t = (i + 0.5) / Math.max(1, n), y = 0.05 + 0.85 * t, r = Math.sqrt(Math.max(0, 1 - y * y)), a = i * 2.399963229728653;
        return new double[]{Math.cos(a) * r, y, Math.sin(a) * r};
    }

    /** Ticks after the cast at which fang i of n comes out: the first third at once, the rest spread over two more waves. */
    public static int fangDelay(int i, int n) { return (i * FANG_WAVES / Math.max(1, n)) * FANG_WAVE_GAP; }

    // ================================================================ whip geometry (shared by the painter and its test)
    /**
     * A point of a curling whip: t from 0 (the hand) to 1 (the tip), the whole whip of the given length, age in ticks. The whip leaves the hand along +z, snakes with a growing amplitude
     * and curls its tip back over itself ("long curling lines"). Writes {x, y, z} in the whip's own space into {@code out} (no allocation: the painter calls it every frame).
     */
    public static void whipPoint(double t, double length, double age, double[] out) {
        double z = t * length * (1.0 - 0.18 * t * t), amp = 0.22 * length * t * t * (1.0 - 0.35 * t);
        double ph = age * 0.55 - t * 7.0;
        double curl = t <= 0.7 ? 0.0 : Math.min(1.0, (t - 0.7) / 0.1), loop = (t - 0.7) / 0.3 * Math.PI * 1.7 + age * 0.25, rad = 0.06 * length * curl;
        out[0] = Math.sin(ph) * amp + Math.sin(loop) * rad;                                  // the tip curls over itself in a loop
        out[1] = Math.cos(ph * 0.8 + 1.3) * amp * 0.55 + 0.09 * t * length * Math.sin(Math.PI * t) + (1.0 - Math.cos(loop)) * rad;
        out[2] = z;
    }

    // ================================================================ client draw cost (the painters read these; pure so that a test pins them)
    /** Level of detail of a prop by its distance from the camera: full up to 24 blocks, 0.7 up to 48, 0.45 up to 96, 0.25 up to 128, nothing beyond. */
    public static float lod(double distance) {
        if (distance < 24) return 1f;
        if (distance < 48) return 0.7f;
        if (distance < 96) return 0.45f;
        return distance < 128 ? 0.25f : 0f;
    }

    /** A segment or piece count scaled by the level of detail and the video quality, never below {@code min}. */
    public static int seg(int base, float lod, float quality, int min) { return Math.max(min, Math.round(base * Math.max(0f, Math.min(1f, lod)) * Math.max(0.25f, Math.min(1f, quality)))); }

    /** The most quads one prop of each magic may draw in one frame at full detail (the preview report must stay under it; the painters scale their counts down with {@link #lod}). */
    public static final int MAX_QUADS_PER_PROP = 1500;

    /** Whether the casting numbers are inside their caps: used by the unit test and the self test. */
    public static boolean capsHold() {
        for (int m = 0; m <= 4; m++)
            if (swordCount(m) > MAX_SWORDS || arrowCount(m) > MAX_ARROWS || fangCount(m) > MAX_FANGS || bulletCount(m) > MAX_BULLETS || slashCount(m) > MAX_SLASHES) return false;
        return true;
    }
}
