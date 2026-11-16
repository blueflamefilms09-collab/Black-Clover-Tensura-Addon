package com.newuniverse.nusmp.vfx.client.layer;

/**
 * Why: the pure half of the wave 4 skill look ({@link SkillAnime2Layer}, docs/vfx_remake/skill_anime_wave4.md): the texture list the generator
 * ({@code tools/gen_skill_anime2_vfx_textures.py}) makes and the asset test pins, how a packet's power picks a style / family / element, and the client switch
 * {@code skills.classicVfx2} ({@code nusmp-skill-anime-vfx-client.toml}, mirrored here by {@code SkillAnimeVfxConfig}). No game type, so the asset test and the headless preview
 * read it without Minecraft. The server side ({@code skill.SkillAnimeFx2}) writes the same numbers; SkillAnime2VfxAssetTest pins the two against each other.
 */
public final class SkillAnime2Vfx {
    private SkillAnime2Vfx() {}

    public static final String PREFIX = "skillanime2_";
    /** Every texture, in the generator's order (the generator lists the same names with their sizes). */
    public static final String[] TEXTURE_NAMES = {"leaf", "droplet", "rock", "bubble", "feather", "rune", "claw", "card", "eye", "crescent", "hex", "stitch", "note", "puff", "facet"};

    /** ANIME2_SKILL's styles: the power is the index + 1 (a payload power of 0 reads as the first). Append only. */
    public static final String[] SKILL_STYLES = {"LIARIS", "HATI", "HAWK", "SPIRIT_CARD", "SUMMON", "SHISHOU", "GATE_ARMOR", "GATE_VIMANA", "GATE_KING", "GATE_ELIXIR", "DEVIL_KICK",
            "ALMIGHTY", "VISION_STAR", "VISION_VOID", "VISION_SELF"};
    /** ANIME2_BEAST: the element of the beast (the rest of the power, floor) and the shape of the move (the mode of the power). Append only. */
    public static final String[] ELEMENTS = {"FIRE", "WATER", "ICE", "LIGHTNING", "EARTH", "WIND", "DARK", "LIGHT", "POISON", "NATURE", "SPIRIT", "VOID"};
    public static final String[] BEAST_FAMILIES = {"LINE", "RUSH", "BURST", "VOLLEY", "SEAL", "AURA"};
    /** ANIME2_HERO: the motif of the weapon (the mode of the power); the variant and the ultimate flag ride in the rest (variant * 2 + ultimate). */
    public static final String[] HERO_MOTIFS = {"SLASH", "THRUST", "SHOCK", "WARD", "CAST", "THREAD", "SHARD"};
    /** ANIME2_GRIMOIRE: the attribute (the mode of the power); the rest is phase * 8 + scale. */
    public static final String[] GRIMOIRE_STYLES = {"COTTON", "MIRROR", "PLANT", "SEALING", "THREAD"};

    /** The power carries a mode in steps of this much (the same step as the wave 3 shapes). */
    public static final float STEP = 20f;

    /** true: the ANIME2_* shapes draw nothing (only the old effects are seen). A client setting, looks only. */
    private static volatile boolean classic = false;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }

    /** The mode a power stands for: 0 .. modes-1 (anything out of range reads as mode 0). */
    public static int mode(float power, int modes) {
        int m = (int) Math.floor(Math.max(0f, power) / STEP);
        return m < modes ? m : 0;
    }

    /** The mode's own number: the power less its mode's step. */
    public static float rest(float power, int modes) { return Math.max(0f, power) - mode(power, modes) * STEP; }

    /** The style index of an ANIME2_SKILL power; anything out of range reads as the first. */
    public static int skillStyle(float power) {
        int s = Math.round(power) - 1;
        return s >= 0 && s < SKILL_STYLES.length ? s : 0;
    }

    /** A style may carry one extra number (a radius up to 49) as hundredths on top of style + 1 (the style rounds it away): power = style + 1 + extra / 100. */
    public static float skillExtra(float power) { return Math.max(0f, Math.min(49f, (power - Math.round(power)) * 100f)); }

    public static int beastFamily(float power) { return mode(power, BEAST_FAMILIES.length); }

    public static int beastElement(float power) {
        int e = (int) Math.floor(rest(power, BEAST_FAMILIES.length) + 1e-3f);
        return e >= 0 && e < ELEMENTS.length ? e : 0;
    }

    /** A finisher (the element number carries a half). */
    public static boolean beastBig(float power) { return rest(power, BEAST_FAMILIES.length) - Math.floor(rest(power, BEAST_FAMILIES.length) + 1e-3f) >= 0.25f; }

    public static int heroMotif(float power) { return mode(power, HERO_MOTIFS.length); }

    /** 0..8: the weapon's sub-form of its motif (a claw instead of a blade, notes instead of sparks, ...). */
    public static int heroVariant(float power) { return Math.min(8, (int) (rest(power, HERO_MOTIFS.length) / 2f)); }

    public static boolean heroUltimate(float power) { return ((int) rest(power, HERO_MOTIFS.length) & 1) == 1; }

    public static int grimoireStyle(float power) { return mode(power, GRIMOIRE_STYLES.length); }

    /** 0 = the cast (the circle under the caster), 1 = the impact. */
    public static int grimoirePhase(float power) { return rest(power, GRIMOIRE_STYLES.length) >= 8f ? 1 : 0; }

    /** The spell's own scale 0..3 (the power argument of castCircle / impact). */
    public static float grimoireScale(float power) { return Math.max(0f, Math.min(3f, rest(power, GRIMOIRE_STYLES.length) - 8f * grimoirePhase(power))); }

    public static String fileName(String name) { return PREFIX + name; }

    /** "textures/particle/skillanime2_leaf.png", ... : the files the folder must hold. */
    public static String[] allTextures() {
        String[] out = new String[TEXTURE_NAMES.length];
        for (int i = 0; i < out.length; i++) out[i] = "textures/particle/" + fileName(TEXTURE_NAMES[i]) + ".png";
        return out;
    }
}
