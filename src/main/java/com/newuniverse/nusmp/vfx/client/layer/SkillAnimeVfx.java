package com.newuniverse.nusmp.vfx.client.layer;

/**
 * Why: the pure half of the anime-style Unique / Ultimate skill look ({@link SkillAnimeLayer}, VFX wave 3, docs/vfx_remake/skill_anime.md): the texture list the generator
 * ({@code tools/gen_skill_anime_vfx_textures.py}) makes and the asset test pins, how a packet's power picks a mode, the sigil styles, and the client switch {@code skills.classicVfx}
 * ({@code nusmp-skill-anime-vfx-client.toml}, mirrored here by {@code SkillAnimeVfxConfig}). No game type, so the asset test and the headless preview read it without Minecraft. The server side
 * ({@code skill.SkillAnimeFx}) writes the same numbers; SkillAnimeVfxAssetTest pins the two against each other.
 */
public final class SkillAnimeVfx {
    private SkillAnimeVfx() {}

    public static final String PREFIX = "skillanime_";
    /** Every texture, in the generator's order (the generator lists the same names with their sizes). */
    public static final String[] TEXTURE_NAMES = {"core", "glow", "flash", "streak", "bolt", "ring", "flame", "ripple", "chain", "bell", "snowflake", "shard", "mist", "petal", "halo",
            "rays", "sigil", "sparkle", "wing", "swirl"};

    /** SKILL_SIGIL's styles: the power is the index + 1 (a payload power of 0 reads as 1). Append only. */
    public static final String[] SIGIL_STYLES = {"FAITH", "GODDESS", "FORGE", "MALLET", "WILL", "HELL", "GRAVITY", "FIRE_FORCE"};

    /** SKILL_GATE, SKILL_ARGONAUT, SKILL_HOLY_FIRE: the power carries a mode in steps of this much (mode = power / STEP, the rest is the mode's own number). */
    public static final float STEP = 20f;
    public static final int GATE_BARRAGE = 0, GATE_RAIN = 1, GATE_CHAINS = 2, GATE_EA = 3;
    public static final int ARGO_CHIME = 0, ARGO_STRIKE = 1, ARGO_BEAM = 2;
    public static final int FIRE_HEARTH = 0, FIRE_FIELD = 1;

    /** true: the SKILL_* shapes draw nothing (only the old particles and shapes of 3.98.0 are seen). A client setting, looks only. */
    private static volatile boolean classic = false;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }

    /** The mode a power stands for: 0 .. modes-1 (anything out of range reads as mode 0). */
    public static int mode(float power, int modes) {
        int m = (int) Math.floor(Math.max(0f, power) / STEP);
        return m < modes ? m : 0;
    }

    /** The mode's own number (the count, the radius, the charge): the power less its mode's step. */
    public static float rest(float power, int modes) { return Math.max(0f, power) - mode(power, modes) * STEP; }

    /** The style index (0..7) a SKILL_SIGIL power stands for; anything out of range reads as FAITH. */
    public static int sigil(float power) {
        int s = Math.round(power) - 1;
        return s >= 0 && s < SIGIL_STYLES.length ? s : 0;
    }

    public static String fileName(String name) { return PREFIX + name; }

    /** "textures/particle/skillanime_core.png", ... : the files the folder must hold. */
    public static String[] allTextures() {
        String[] out = new String[TEXTURE_NAMES.length];
        for (int i = 0; i < out.length; i++) out[i] = "textures/particle/" + fileName(TEXTURE_NAMES[i]) + ".png";
        return out;
    }
}
