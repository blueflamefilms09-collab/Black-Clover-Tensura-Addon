package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.ritual.RitualClock;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

/**
 * Why: every number of the Soul Forge (which creation screen opens, whether the Genesis Core still works on its own, the ritual's length, the weight of the
 * taxes, the odds of the awakening) is a balance or taste decision the owner wants to change without a rebuild, so they live in their own server file
 * {@code nusmp-avatar-soulforge-server.toml}, section {@code [avatar]}. The lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, AvatarSoulForgeConfig.SPEC, "nusmp-avatar-soulforge-server.toml")}.
 * Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading,
 * so the two text switches are checked by hand in their readers and fall back to their defaults on a typo), and every reader goes through {@link #get}, which
 * answers the default while the spec is not loaded yet (tests, early ticks). {@link #rules()} hands the pure code its snapshot.
 */
public final class AvatarSoulForgeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> CREATION_UI, CORE_USE;
    public static final ModConfigSpec.BooleanValue CHOSEN_SKILLS_SECURE, EFFECTS;
    public static final ModConfigSpec.IntValue MAX_APTITUDES, RITUAL_SECONDS, MAX_OPEN_MINUTES, BIRTH_TICKS, AWAKENING_EXTRA_CORES, APTITUDE_ROLLS,
            START_GAP_SECONDS, ROLL_GAP_MS, ACTION_GAP_MS, FINALIZE_GAP_MS;
    public static final ModConfigSpec.DoubleValue REACH, APTITUDE_TAX_SCALE, SKILL_TAX_SCALE, MAX_TOTAL_TAX_PERCENT, UPFRONT_FACTOR, AWAKENING_BASE,
            AWAKENING_MAX_CHANCE, AWAKENING_TAX_PERCENT, LUCK_STRENGTH, SIGIL_RADIUS, SHAKE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("avatar");
        CREATION_UI = b.comment("Which creation screen opens: \"soulforge\" (the full-screen Soul Forge ritual, default) or \"classic\" (the four-page 1.51.0 screen, kept as it was).")
                .define("creationUi", "soulforge");
        CORE_USE = b.comment("What the Genesis Core does when used on its own: \"forge\" (default: it only works at a Soul Forge; elsewhere it shows a hint) or \"item\" (the old direct use).")
                .define("coreUse", "forge");
        MAX_APTITUDES = b.comment("The most Magic Aptitudes one Avatar can carry (hand picked plus the free roll).").defineInRange("maxAptitudes", 2, 0, 8);
        RITUAL_SECONDS = b.comment("Seconds the player stands locked while the circle draws, before the Soul Forge screen opens.").defineInRange("ritualSeconds", 4, 1, 30);
        MAX_OPEN_MINUTES = b.comment("The most minutes one ritual may keep the screen open (and the player locked) before it is cancelled.").defineInRange("maxOpenMinutes", 10, 1, 60);
        REACH = b.comment("How far (blocks) from the Soul Forge the creator may stray before the ritual is cancelled.").defineInRange("reach", 6.0, 2.0, 16.0);
        START_GAP_SECONDS = b.comment("Least seconds between two ritual starts of one player.").defineInRange("startGapSeconds", 3, 0, 120);
        BIRTH_TICKS = b.comment("Length in ticks of the birth spectacle (beam, Avatar steps out, first ability).").defineInRange("birthTicks", 50, 24, 240);
        APTITUDE_TAX_SCALE = b.comment("Multiplier on the permanent magicule tax of a hand-picked aptitude (1.0 = the numbers in data/nusmp/avatar_aptitudes).")
                .defineInRange("aptitudeTaxScale", 1.0, 0.0, 10.0);
        SKILL_TAX_SCALE = b.comment("Multiplier on the EP tax of a chosen Unique / Ultimate skill (the numbers in data/nusmp/avatar_skills).").defineInRange("skillTaxScale", 1.0, 0.0, 10.0);
        MAX_TOTAL_TAX_PERCENT = b.comment("The most permanent tax (percent of maximum magicule, all of a creator's Avatars together) a creation may leave the creator with.")
                .defineInRange("maxTotalTaxPercent", 60.0, 0.0, 90.0);
        UPFRONT_FACTOR = b.comment("Magicule paid at once = the permanent tax fraction x this x the creator's maximum magicule (0 = nothing up front).").defineInRange("upfrontFactor", 1.0, 0.0, 10.0);
        APTITUDE_ROLLS = b.comment("How many times one ritual may use the free Randomize Aptitude button.").defineInRange("aptitudeRolls", 3, 0, 20);
        LUCK_STRENGTH = b.comment("How strongly the creator's luck shifts the free aptitude roll towards the rarer classes (0 = luck does nothing).").defineInRange("aptitudeLuckStrength", 0.06, 0.0, 1.0);
        AWAKENING_BASE = b.comment("Instant Skill Awakening: the 5 of \"a d100 below 5 + total luck\".").defineInRange("awakeningBase", 5.0, 0.0, 100.0);
        AWAKENING_MAX_CHANCE = b.comment("The most chance (percent) the free awakening roll can have however lucky the creator is.").defineInRange("awakeningMaxChance", 75.0, 0.0, 100.0);
        AWAKENING_TAX_PERCENT = b.comment("Permanent tax (percent of maximum magicule) of paying for a guaranteed awakening.").defineInRange("awakeningTaxPercent", 5.0, 0.0, 90.0);
        AWAKENING_EXTRA_CORES = b.comment("Genesis Cores a guaranteed awakening costs on top of the one the Avatar needs.").defineInRange("awakeningExtraCores", 1, 0, 8);
        CHOSEN_SKILLS_SECURE = b.comment("true: an Avatar given a Unique or Ultimate skill in the forge counts as holding one (the three-day betrayal roll is skipped).").define("chosenSkillsSecure", true);
        EFFECTS = b.comment("Circle, beam, camera shake and sounds of the ritual. false keeps the mechanics and drops the spectacle.").define("effects", true);
        SIGIL_RADIUS = b.comment("Radius in blocks of the magic circle drawn on the ground.").defineInRange("sigilRadius", 4.5, 1.5, 12.0);
        SHAKE = b.comment("Strength of the camera shake at the moment of birth (0 = none).").defineInRange("birthShake", 1.0, 0.0, 4.0);
        ROLL_GAP_MS = b.comment("Least milliseconds between two free aptitude rolls of one player.").defineInRange("rollGapMillis", 1200, 0, 20000);
        ACTION_GAP_MS = b.comment("Least milliseconds between two small requests (a sample line, a refresh) of one player.").defineInRange("actionGapMillis", 250, 0, 10000);
        FINALIZE_GAP_MS = b.comment("Least milliseconds between two Finalize requests of one player.").defineInRange("finalizeGapMillis", 1500, 0, 20000);
        b.pop();
        SPEC = b.build();
    }

    private AvatarSoulForgeConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** True when the full-screen Soul Forge is the creation screen (anything but "classic"). */
    public static boolean soulForgeUi() { return !"classic".equals(get(CREATION_UI).strip().toLowerCase(Locale.ROOT)); }

    /** True when the Genesis Core opens the creation on its own (the old way); the default is that it needs a Soul Forge. */
    public static boolean coreWorksAlone() { return "item".equals(get(CORE_USE).strip().toLowerCase(Locale.ROOT)); }

    public static int maxAptitudes() { return get(MAX_APTITUDES); }
    public static int chargeTicks() { return get(RITUAL_SECONDS) * 20; }
    public static int maxOpenTicks() { return get(MAX_OPEN_MINUTES) * 1200; }
    public static double reach() { return get(REACH); }
    public static long startGapMillis() { return get(START_GAP_SECONDS) * 1000L; }
    public static RitualClock.Birth birth() { return RitualClock.Birth.of(get(BIRTH_TICKS)); }
    public static boolean effects() { return get(EFFECTS); }
    public static float sigilRadius() { return get(SIGIL_RADIUS).floatValue(); }
    public static float birthShake() { return get(SHAKE).floatValue(); }
    public static int rollGapMillis() { return get(ROLL_GAP_MS); }
    public static int actionGapMillis() { return get(ACTION_GAP_MS); }
    public static int finalizeGapMillis() { return get(FINALIZE_GAP_MS); }

    /** The snapshot the pure cost and validation code reads. */
    public static ForgeRules rules() {
        return new ForgeRules(get(MAX_APTITUDES), get(APTITUDE_TAX_SCALE), get(SKILL_TAX_SCALE), get(MAX_TOTAL_TAX_PERCENT) / 100.0, get(UPFRONT_FACTOR),
                get(AWAKENING_BASE), get(AWAKENING_MAX_CHANCE), get(AWAKENING_TAX_PERCENT) / 100.0, get(AWAKENING_EXTRA_CORES), get(APTITUDE_ROLLS),
                get(LUCK_STRENGTH), get(CHOSEN_SKILLS_SECURE));
    }
}
