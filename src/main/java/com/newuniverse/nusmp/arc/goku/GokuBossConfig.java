package com.newuniverse.nusmp.arc.goku;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the Goku boss, {@code nusmp-goku-boss-server.toml}, section {@code [goku_boss]}. All of them change gameplay, so they are SERVER settings (rule 9). The strength
 * of the fight comes from the difficulty preset and from the Tensura EP of the players near him ({@link GokuBossMath}); every number is documented in docs/goku_boss.md. Every value is a
 * plain {@code define} / {@code defineInRange} (never a list-validated one: it throws while the server is still loading) and every reader goes through {@link #get}, which answers the
 * default while the spec is not loaded yet.
 */
public final class GokuBossConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue PUNCH_GAP;
    public static final ModConfigSpec.BooleanValue ENABLED, BLOCK_DAMAGE, EP_SCALING, FAREWELL, FORM_BARS, PUNCHES;
    /** Goku Omega (docs/goku_omega.md), section {@code [goku_boss.omega]}. */
    public static final ModConfigSpec.BooleanValue OMEGA_ENABLED, OMEGA_EP_SKILLS, OMEGA_HACKS, OMEGA_SECOND_WIND, OMEGA_TRAINING;
    public static final ModConfigSpec.DoubleValue OMEGA_HEALTH, OMEGA_DAMAGE, OMEGA_MAX_HIT, OMEGA_DODGE_CAP;
    public static final ModConfigSpec.IntValue OMEGA_LEVEL_CAP, OMEGA_XP_PER_HOUR, OMEGA_TRAINING_DAILY, OMEGA_FIGHT_DAILY;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY;
    public static final ModConfigSpec.DoubleValue PUNCH_MUL, PUNCH_SHARE, FORM_BAR_SHARE, HEALTH_MULTIPLIER, DAMAGE_MULTIPLIER, EP_WEIGHT, EP_CAP, PER_PLAYER_BONUS, MAX_HIT_PERCENT, UI_DODGE_CAP;
    public static final ModConfigSpec.IntValue PLAYER_CAP, GLOBAL_CAP, ENRAGE_MINUTES;

    /** The defaults, also read by the tests. */
    public static final double DEFAULT_EP_WEIGHT = 0.6, DEFAULT_EP_CAP = 8.0, DEFAULT_PER_PLAYER_BONUS = 0.35, DEFAULT_MAX_HIT = 0.30, DEFAULT_DODGE_CAP = 0.35;
    /** 4.102.0: his OWN Tensura Existence Points climb from {@code epStart} in Base to {@code epEnd} in his last form (docs/boss_vfx_goku_kakashi_sinbad.md). */
    public static final ModConfigSpec.BooleanValue EP_LADDER, EP_TAME;
    public static final ModConfigSpec.DoubleValue EP_START, EP_END, EP_CEILING, EP_MAX_CLIMB, EP_PLAYER_SHARE, FORM_FLOOR, TRUE_SHARE, SPIRIT_SHARE;
    public static final ModConfigSpec.BooleanValue SPLIT_DAMAGE, COUNTER_EFFECTS, COUNTER_FLARE;
    /** 4.107.0: stronger Goku, guaranteed true damage and the new VFX. */
    public static final ModConfigSpec.BooleanValue POWER_UP, TRUE_GUARANTEE, TRUE_FEEDBACK, FX_RICH, KI_VFX;
    public static final ModConfigSpec.DoubleValue POWER_HEALTH, POWER_DAMAGE, TRUE_FLOOR;
    public static final ModConfigSpec.ConfigValue<String> COUNTER_MODS;
    public static final ModConfigSpec.DoubleValue COUNTER_DAMAGE;
    public static final ModConfigSpec.BooleanValue SMART_FORMS;
    public static final ModConfigSpec.IntValue MAX_FORM_SKIP;
    public static final double DEFAULT_EP_START = 1.0e9, DEFAULT_EP_END = 1.0e10, DEFAULT_EP_CEILING = 1.0e11, DEFAULT_EP_CLIMB = 10.0, DEFAULT_EP_PLAYER_SHARE = 0.01;
    public static final int DEFAULT_GLOBAL_CAP = 2, DEFAULT_ENRAGE_MINUTES = 12;
    public static final double DEFAULT_OMEGA_HEALTH = 2.5, DEFAULT_OMEGA_DAMAGE = 1.5, DEFAULT_OMEGA_MAX_HIT = 0.5, DEFAULT_OMEGA_DODGE = 0.85;
    public static final int DEFAULT_XP_PER_HOUR = 60, DEFAULT_TRAINING_DAILY = 1500;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("goku_boss");
        ENABLED = b.comment("The Goku boss may be summoned (command, spawn egg, arc summon). False: he does not spawn and everything else stays as it was.")
                .define("enabled", true);
        EP_LADDER = b.comment("Goku's own Tensura Existence Points follow his forms: epStart in Base, rising by a fixed factor per form to epEnd in his last form. False: his EP is left as the game gives it (the behaviour before 4.102.0).")
                .define("epLadder", true);
        EP_START = b.comment("His Existence Points in Base (1 billion; 1 trillion before 4.104.0).").defineInRange("epStart", DEFAULT_EP_START, 1.0, 1.0e18);
        EP_END = b.comment("His Existence Points in his last form (10 billion; 500 trillion before 4.104.0).").defineInRange("epEnd", DEFAULT_EP_END, 1.0, 1.0e18);
        EP_TAME = b.comment("4.104.0: keeps the ladder gentle: the climb from Base to his last form is held to epMaxClimb and his last form never goes above epCeiling, even if epStart/epEnd (or an old config) say more, or the players are stronger. False = the 4.102.0 ladder exactly as the numbers above say (1 trillion to 500 trillion by default there).")
                .define("epTame", true);
        EP_CEILING = b.comment("4.104.0: the most Existence Points he can ever have (100 billion). Counts only with epTame.").defineInRange("epCeiling", DEFAULT_EP_CEILING, 1.0, 1.0e18);
        EP_MAX_CLIMB = b.comment("4.104.0: the most his EP can multiply from Base to his last form (10x). Counts only with epTame.").defineInRange("epMaxClimb", DEFAULT_EP_CLIMB, 1.0, 1000.0);
        EP_PLAYER_SHARE = b.comment("4.102.0: his EP never falls below this share of the strongest player's EP near him (0.01 = one hundredth). The ladder above is the floor and this only raises it; 0 = off.")
                .defineInRange("epPlayerShare", DEFAULT_EP_PLAYER_SHARE, 0.0, 10.0);
        FORM_FLOOR = b.comment("4.102.0: with formBars, the share of his health he is held at when a form's bar runs out: he cannot be hurt below it, powers up into the next form and comes back with a full bar (0.01 = 1%; 0 = the old 1 health).")
                .defineInRange("formFloor", GokuLadder.DEFAULT_FLOOR, 0.0, 0.5);
        SMART_FORMS = b.comment("4.102.0: he reads the strongest player near him; against a player far above his own EP he may skip forms when he changes instead of going in order. False = always the next form.")
                .define("smartForms", true);
        MAX_FORM_SKIP = b.comment("4.102.0: the most forms he skips in one change (0 = never; one is skipped per doubling of the player's EP over his).")
                .defineInRange("maxFormSkip", 3, 0, 9);
        SPLIT_DAMAGE = b.comment("4.103.0: part of every blow he lands on a creature is TRUE damage (ignores armour, enchantments and resistance) and part is SPIRITUAL damage (Tensura's spiritual type: shakes the soul, passes barriers and dodges); the rest is the normal blow. The hit cap still limits the whole blow. False = every blow is the plain blow as before.")
                .define("splitDamage", true);
        TRUE_SHARE = b.comment("4.103.0: the share of a blow that is true damage in his top form (about 40% of it in Base, rising evenly). 0..0.8.")
                .defineInRange("trueShare", 0.30, 0.0, 0.8);
        SPIRIT_SHARE = b.comment("4.103.0: the share of a blow that is spiritual damage in his top form (about 40% of it in Base, rising evenly). 0..0.8; the two shares together are held to 0.9.")
                .defineInRange("spiritShare", 0.20, 0.0, 0.8);
        TRUE_GUARANTEE = b.comment("4.107.0: the true part of a blow ALWAYS lands: it is delivered even when the normal part is blocked or dodged, it ignores hit-immunity and shields, and if another mod's rules ate part of it the missing health is taken directly. False = the 4.103.0 delivery (the true part goes with a refused blow).")
                .define("trueGuarantee", true);
        TRUE_FLOOR = b.comment("4.107.0: the true part of a blow is at least this share of the victim's MAX health (scaled by his form like trueShare), so a victim with a huge EP still feels it. 0.03 = 3% in his top form, 1.2% in Base. 0 = only trueShare of the blow (the old way). Held to 0.10.")
                .defineInRange("trueFloor", 0.03, 0.0, GokuBossMath.MAX_TRUE_FLOOR);
        TRUE_FEEDBACK = b.comment("4.107.0: a white-blue flash on the victim and a short action-bar line when true damage lands (at most one line per second per victim).")
                .define("trueFeedback", true);
        POWER_UP = b.comment("4.107.0: Goku is stronger: his health and damage get the two multipliers below, on top of the preset, EP scaling and your own multipliers, and his free punches come 20% faster. False = exactly the Goku of 4.106.")
                .define("powerUp", true);
        POWER_HEALTH = b.comment("4.107.0: the power-up's health multiplier (1.5 = half as much again). Counts only with powerUp.")
                .defineInRange("powerUpHealth", 1.5, 1.0, 10.0);
        POWER_DAMAGE = b.comment("4.107.0: the power-up's damage multiplier (1.35). One hit on a player still ends at the hit caps. Counts only with powerUp.")
                .defineInRange("powerUpDamage", 1.35, 1.0, 10.0);
        FX_RICH = b.comment("4.107.0: the richer ki visuals (layered aura, flames, form-change shockwave, beam with a core, true-hit flash). Every particle still goes through the shared particle budget. False = the plain 4.106 visuals.")
                .define("richFx", true);
        KI_VFX = b.comment("5.1.0: Goku's Kamehameha, Spirit Bomb charge and drop, power-up shock, fast blows and dodge are drawn with the Dragon Ball Ki layer (the same effects the Saiyan race uses), on top of the particles. One small effect packet per technique, to players within 96 blocks, through the effect budget. False = particles only (5.0).")
                .define("kiVfx", true);
        COUNTER_MODS = b.comment("4.103.0: namespaces of mods whose skills attack a fighter's strength (comma separated). The default is the Devil x Slime / High School DxD mod: Divine Dividing, Albion's poison, Compression Divider... Add any other mod's namespace.")
                .define("counterMods", GokuCounters.DEFAULT_MODS);
        COUNTER_EFFECTS = b.comment("4.103.0: harmful effects of the counterMods do not take hold on him, and attribute changes those mods put on him are purged every half second. False = he can be Divided like anyone.")
                .define("counterEffects", true);
        COUNTER_DAMAGE = b.comment("4.103.0: the damage of the counterMods' damage types is multiplied by this against him (0.5 = half; 1 = off).")
                .defineInRange("counterDamage", 0.5, 0.05, 1.0);
        COUNTER_FLARE = b.comment("4.103.0: each time he shrugs off one of their debuffs he flares his ki (a ring and a shove of the players near him, no damage), at most every 5 seconds.")
                .define("counterFlare", true);
        DIFFICULTY = b.comment("The preset: normal (half health and damage, 60% of the form bonuses), overpowered (the default) or anime (double health, 1.6x damage, 130% of the form bonuses). Unknown words mean overpowered.")
                .define("difficulty", "overpowered");
        HEALTH_MULTIPLIER = b.comment("Multiplies his health on top of the preset and the EP scaling.")
                .defineInRange("healthMultiplier", 1.0, 0.1, 50.0);
        DAMAGE_MULTIPLIER = b.comment("Multiplies his damage on top of the preset and the EP scaling (one hit on a player still ends at maxHitPercent of their health).")
                .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
        EP_SCALING = b.comment("His health, damage and form pace follow the Tensura Existence Points of the strongest players near him (read at spawn and then every 100 ticks). False: Battle Power, then the preset's fixed value.")
                .define("epScaling", true);
        FAREWELL = b.comment("4.90.0: when Goku is beaten he is not killed: he is knocked down, says goodbye (\"I'll see you next time, when I'm stronger!\"), flashes away and drops his loot. The fight counts as won. False = he dies as before.")
                .define("farewell", true);
        FORM_BARS = b.comment("4.96.0: every form has its own health bar, like Lucifero's phases: the bar runs down to 0, he cannot be hurt while he powers up, and the next, stronger form comes back with a FULL bar. He only dies (bows out) at the end of his last form. False = the old single bar where the form follows the health.")
                .define("formBars", true);
        PUNCHES = b.comment("4.98.0: Goku fights with his fists too: between his moves he runs at the target and punches (a blow every punchGapTicks), and every blow of his hand hits punchMultiplier times harder than before, up to punchHitShare of the victim's health. False = the old, weaker plain blows.")
                .define("punches", true);
        PUNCH_MUL = b.comment("4.98.0: how much harder every physical blow of Goku (his plain hit, the Meteor Combination, the new punches) hits. 1 = as before.")
                .defineInRange("punchMultiplier", 4.0, 1.0, 50.0);
        PUNCH_SHARE = b.comment("4.98.0: one punch of his takes at most this share of a player's maximum health (replaces maxHitPercent for his fists when higher; magic and beams keep their own cap).")
                .defineInRange("punchHitShare", 0.75, 0.05, 0.95);
        PUNCH_GAP = b.comment("4.98.0: ticks between two of his free punches (20 = one per second).")
                .defineInRange("punchGapTicks", 14, 6, 80);
        FORM_BAR_SHARE = b.comment("4.96.0: with formBars, the size of ONE form bar as a share of the old single bar (the fight has one bar per form, so a small share keeps it from taking ten times as long). 1.0 = every bar as big as the old whole bar.")
                .defineInRange("formBarShare", 0.4, 0.05, 1.0);
        EP_WEIGHT = b.comment("How strongly EP raises him: health x (1 + epWeight x (EP curve - 1)); damage takes half of it. 0 = EP changes nothing.")
                .defineInRange("epWeight", DEFAULT_EP_WEIGHT, 0.0, 5.0);
        EP_CAP = b.comment("The highest value of the EP curve that counts (the curve is 1 at 1,000 EP and +2.5 per decade of EP: 5 is reached at about 40,000 EP and 8 at about 630,000 EP). Lower = he stops growing earlier.")
                .defineInRange("epCap", DEFAULT_EP_CAP, 1.0, 12.0);
        PER_PLAYER_BONUS = b.comment("Extra health per player beyond the first near him (damage gets 40% of it).")
                .defineInRange("perPlayerBonus", DEFAULT_PER_PLAYER_BONUS, 0.0, 2.0);
        PLAYER_CAP = b.comment("How many players count toward that bonus (at most 10).")
                .defineInRange("playerCap", GokuBossMath.MAX_PLAYERS, 1, GokuBossMath.MAX_PLAYERS);
        MAX_HIT_PERCENT = b.comment("One hit of his takes at most this share of a player's maximum health (0.30 = 30%): he cannot one-shot through the mod's own balance law.")
                .defineInRange("maxHitPercent", DEFAULT_MAX_HIT, 0.05, 0.9);
        UI_DODGE_CAP = b.comment("The most chance his automatic Ultra Instinct dodge may ever have (it also has a cooldown and a limit per window).")
                .defineInRange("uiDodgeCap", DEFAULT_DODGE_CAP, 0.0, 0.6);
        ENRAGE_MINUTES = b.comment("The soft enrage timer: after this many minutes (shorter with high EP) he is in True Ultra Instinct whatever his health is.")
                .defineInRange("enrageMinutes", DEFAULT_ENRAGE_MINUTES, 3, 60);
        BLOCK_DAMAGE = b.comment("The Kamehameha and the Spirit Bomb may break blocks (a few, through the boss framework's block ledger, and only when the griefBlocks rule is on too). Off by default.")
                .define("blockDamage", false);
        GLOBAL_CAP = b.comment("How many Goku bosses may stand in the whole server at once (a hard cap so it cannot lag).")
                .defineInRange("globalCap", DEFAULT_GLOBAL_CAP, 1, 4);
        b.push("omega");
        OMEGA_ENABLED = b.comment("Goku Omega: the strongest boss of the mod. He is guaranteed above every other boss, levels up (fights and autonomous training), has Energy Drain, Skill Seal, adaptation and a list of hacks, and gains Mastered Ultra Instinct. False = exactly the Goku of before (everything below is ignored).")
                .define("enabled", true);
        OMEGA_EP_SKILLS = b.comment("Omega: Goku uses Tensura-style abilities on players (Energy Drain of magicules and aura, Skill Seal of one skill). Never hunger. Each has a cooldown, a cap per fight and a cap per player.")
                .define("epSkills", true);
        OMEGA_HACKS = b.comment("Omega: the other hacks (Ki Sense, Counter, Mirror, adaptation, Time Skip, Ki Barrier). Each unlocks at a level and has a cooldown and a cap per fight.")
                .define("hacks", true);
        OMEGA_SECOND_WIND = b.comment("Omega: a small capped heal at every form change (3% of his health, at most 15% per fight). It is a deliberate exception to the boss rule 'no heal in combat' (like the Gates refill); false = he never heals in a fight.")
                .define("secondWind", true);
        OMEGA_HEALTH = b.comment("Omega: his health factor is multiplied by this (on top of the level), then lifted to the strongest-boss floor.")
                .defineInRange("healthBonus", DEFAULT_OMEGA_HEALTH, 1.0, 10.0);
        OMEGA_DAMAGE = b.comment("Omega: the same for his damage.")
                .defineInRange("damageBonus", DEFAULT_OMEGA_DAMAGE, 1.0, 10.0);
        OMEGA_MAX_HIT = b.comment("Omega: one hit of his takes at most this share of a player's maximum health (0.5 = half; replaces maxHitPercent while Omega is on, never lower than it; at most 0.95, so no hit takes a whole bar).")
                .defineInRange("maxHitPercent", DEFAULT_OMEGA_MAX_HIT, 0.05, 0.95);
        OMEGA_DODGE_CAP = b.comment("Omega: the most chance his Ultra Instinct dodge may have (replaces uiDodgeCap while Omega is on; Mastered Ultra Instinct is near-perfect at 0.85+). It still has a cooldown and a limit per window.")
                .defineInRange("uiDodgeCap", DEFAULT_OMEGA_DODGE, 0.0, 0.95);
        OMEGA_LEVEL_CAP = b.comment("Omega: the highest level he can reach (hard maximum 200).")
                .defineInRange("levelCap", GokuOmegaMath.DEFAULT_LEVEL_CAP, 1, GokuOmegaMath.HARD_MAX_LEVEL);
        OMEGA_TRAINING = b.comment("Omega: he trains by himself while nobody fights him (pure arithmetic once an in-game hour and at the start of a fight; no entity, no chunk).")
                .define("training", true);
        OMEGA_XP_PER_HOUR = b.comment("Omega: experience he trains per in-game hour (1,000 ticks).")
                .defineInRange("trainingXpPerHour", DEFAULT_XP_PER_HOUR, 0, 1_000_000);
        OMEGA_TRAINING_DAILY = b.comment("Omega: the most experience training gives per in-game day (24,000 ticks).")
                .defineInRange("trainingDailyCap", DEFAULT_TRAINING_DAILY, 0, 10_000_000);
        OMEGA_FIGHT_DAILY = b.comment("Omega: the most experience fights give per in-game day (so summoning him again and again does not farm levels).")
                .defineInRange("fightDailyCap", (int) GokuOmegaMath.DEFAULT_FIGHT_DAILY_CAP, 0, 10_000_000);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private GokuBossConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean blockDamage() { return get(BLOCK_DAMAGE); }

    public static GokuBossMath.Preset preset() { return GokuBossMath.Preset.parse(get(DIFFICULTY)); }

    public static double maxHitPercent() { return get(MAX_HIT_PERCENT); }

    public static double uiDodgeCap() { return get(UI_DODGE_CAP); }

    public static int globalCap() { return get(GLOBAL_CAP); }

    public static int enrageMinutes() { return get(ENRAGE_MINUTES); }

    /** All the scaling inputs at once. */
    public static GokuBossMath.Params params() {
        return new GokuBossMath.Params(preset(), get(HEALTH_MULTIPLIER), get(DAMAGE_MULTIPLIER), get(EP_SCALING), get(EP_WEIGHT), get(EP_CAP), get(PER_PLAYER_BONUS), get(PLAYER_CAP));
    }

    public static boolean epScaling() { return get(EP_SCALING); }

    public static boolean farewell() { return get(FAREWELL); }

    public static boolean formBars() { return get(FORM_BARS); }

    public static boolean punches() { return get(PUNCHES); }

    public static double punchMul() { return get(PUNCH_MUL); }

    public static double punchShare() { return get(PUNCH_SHARE); }

    public static int punchGap() { return GokuBossMath.quickGap(get(PUNCH_GAP), powerUp()); }

    public static double formBarShare() { return get(FORM_BAR_SHARE); }

    // ------------------------------------------------------------------ Goku Omega
    public static boolean omega() { return get(OMEGA_ENABLED); }

    public static boolean omegaEpSkills() { return omega() && get(OMEGA_EP_SKILLS); }

    public static boolean omegaHacks() { return omega() && get(OMEGA_HACKS); }

    public static boolean omegaSecondWind() { return omega() && get(OMEGA_SECOND_WIND); }

    public static boolean omegaTraining() { return omega() && get(OMEGA_TRAINING); }

    public static int omegaLevelCap() { return get(OMEGA_LEVEL_CAP); }

    /** The share of a player's health one hit may take: the classic cap, or the Omega one while Omega is on. */
    public static double hitShare() { return GokuOmegaMath.hitShare(omega(), maxHitPercent(), get(OMEGA_MAX_HIT)); }

    /** The dodge chance cap: the classic one, or the Omega one (never lower than the classic) while Omega is on. */
    public static double dodgeCap() { return omega() ? Math.max(uiDodgeCap(), get(OMEGA_DODGE_CAP)) : uiDodgeCap(); }

    public static boolean epLadder() { return get(EP_LADDER); }

    public static double epStart() { return get(EP_START); }

    public static double epEnd() { return get(EP_END); }

    public static boolean epTame() { return get(EP_TAME); }

    public static double epCeiling() { return get(EP_CEILING); }

    public static double epMaxClimb() { return get(EP_MAX_CLIMB); }

    public static double epPlayerShare() { return get(EP_PLAYER_SHARE); }

    public static double formFloor() { return get(FORM_FLOOR); }

    public static boolean smartForms() { return get(SMART_FORMS); }

    public static int maxFormSkip() { return smartForms() ? get(MAX_FORM_SKIP) : 0; }

    public static boolean splitDamage() { return get(SPLIT_DAMAGE); }

    public static boolean trueGuarantee() { return get(TRUE_GUARANTEE); }

    public static double trueFloor() { return get(TRUE_FLOOR); }

    public static boolean trueFeedback() { return get(TRUE_FEEDBACK); }

    public static boolean powerUp() { return get(POWER_UP); }

    public static double powerHealth() { return get(POWER_HEALTH); }

    public static double powerDamage() { return get(POWER_DAMAGE); }

    public static boolean richFx() { return get(FX_RICH); }

    public static boolean kiVfx() { return get(KI_VFX); }

    public static double trueShare() { return get(TRUE_SHARE); }

    public static double spiritShare() { return get(SPIRIT_SHARE); }

    public static String counterMods() { return get(COUNTER_MODS); }

    public static boolean counterEffects() { return get(COUNTER_EFFECTS); }

    public static double counterDamage() { return get(COUNTER_DAMAGE); }

    public static boolean counterFlare() { return get(COUNTER_FLARE); }
}
