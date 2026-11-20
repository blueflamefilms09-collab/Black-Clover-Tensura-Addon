package com.newuniverse.nusmp.boss.rules;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner's boss rules (no regeneration, no Unique/Ultimate skills, Lucius's combo AI, the one refill at the Gates) must each be
 * a switch, because the brief overrides an older design and the old code paths are kept reachable behind them. Own spec file
 * ({@code nusmp-boss-rules-server.toml}, SERVER) so the rules can be tuned without touching the other boss configs; the lead registers
 * it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, BossRulesConfig.SPEC, "nusmp-boss-rules-server.toml")}. Plain
 * {@code define}/{@code defineInRange} only (the forbidden words are one comma separated string, parsed by the pure policy). Reads fall
 * back to the defaults before the config is loaded, so the rules hold from the very first tick.
 */
public final class BossRulesConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STRIP_REGENERATION, BAN_UNIQUE_ULTIMATE, LUCIUS_COMBO_AI, REFILL_AT_GATES,
            LUCIUS_ANIME_ARC, LUCIUS_TIME_REVERSAL, ENVIRONMENT_IMMUNE, EFFECT_IMMUNE;
    public static final ModConfigSpec.ConfigValue<String> FORBIDDEN_WORDS;
    public static final ModConfigSpec.DoubleValue BLACK_CLOVER_STATS;
    /** Boss rework wave 1 (docs/bosses/rework_wave1.md): the appended moves, and the per-boss shaped effects. */
    public static final ModConfigSpec.BooleanValue REWORK_MOVES, REWORK_VFX, REWORK3_MOVES, REWORK3_DECENNIAL;
    /** Leon study fixes 5 and 9 (docs/boss_leon_scaling.md, docs/boss_data_files.md). */
    public static final ModConfigSpec.BooleanValue PARTY_SCALING, BOSS_DATA;
    /** Dungeon-boss rework (docs/dungeon_bosses_rework.md): the master switch and the per-boss switches of the Floor Warden and the three creatures. */
    public static final ModConfigSpec.BooleanValue DUNGEON_REWORK, DUNGEON_WARDEN, DUNGEON_CREATURES;
    /** Dungeon-boss rework 2 (docs/dungeon_bosses_rework2.md): the Labyrinth Guardians' telegraphed move sets and colourways, and the new boss sound set. */
    public static final ModConfigSpec.BooleanValue DUNGEON_GUARDIANS, DUNGEON_SOUNDS, DUNGEON_THEMES;
    /** Leon study fixes #4 and #2 (docs/boss_leon_combat.md): the reactive guard of the opted-in sword bosses and the per-attack hit timeline. */
    public static final ModConfigSpec.BooleanValue REACTIVE_GUARD, ATTACK_TIMELINE;
    public static final ModConfigSpec.DoubleValue GUARD_BLOCK_CHANCE, GUARD_DODGE_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossRules");
        STRIP_REGENERATION = b.comment("Bosses get no regeneration: no Regeneration / Instant Regeneration / Self Regeneration skills or effects, no heal while in combat.",
                        "The scripted refill at the Gates (refillAtGates) is the only restoration left.")
                .define("stripRegeneration", true);
        BAN_UNIQUE_ULTIMATE = b.comment("Bosses never own or use Unique or Ultimate skills (Tensura's skill types). Standard skills, innate traits and spellcasting stay.")
                .define("banUniqueUltimate", true);
        FORBIDDEN_WORDS = b.comment("Extra words that mark a skill or effect id as part of the regeneration family, comma separated (the built-in words always apply).")
                .define("forbiddenWords", BossSkillPolicy.DEFAULT_WORDS);
        ENVIRONMENT_IMMUNE = b.comment("1.68.0: a boss of the mod takes none of the world's own damage (suffocation inside a wall, cramming, falling, drowning, cactus, berry bush, stalagmite, magma, powder snow, storm lightning, an undead boss's daylight burn) when nothing attacked it. Fixes bosses 'taking random damage' when spawned outside the dungeons. false = the old behaviour.")
                .define("environmentImmune", true);
        EFFECT_IMMUNE = b.comment("4.62.2: a boss of the mod takes none of the damage a harmful effect on it deals (poison, wither, damage-over-time effects of other mods) when nothing attacked: that damage read as the boss losing health 'randomly'. A blow with an attacker is untouched. false = the old behaviour.")
                .define("effectImmune", true);
        LUCIUS_COMBO_AI = b.comment("Lucius chains distinct spells (spatial, offensive, defensive or control, then one heavy magic cast) under cooldowns. false = his old move chooser.")
                .define("luciusComboAi", true);
        REFILL_AT_GATES = b.comment("The Gates (100% mode) refill a Zogratis boss's health bar once. The one deliberate exception to 'no second wind'; false removes it.")
                .define("refillAtGates", true);
        LUCIUS_ANIME_ARC = b.comment("Lucius fights as in the final arc: three acts (levitating Messiah, grounded Soul Corruptor, the clones and the chest gem), spell chains,",
                        "Soul Touch, a precognition that only simultaneous or very fast strikes, anti-magic or synchronized relics beat. In this mode the Gates do not refill his health bar.",
                        "false = the old 1.20 to 1.37 Lucius (Julius windows, charges of precognition, the Gates refill) untouched.")
                .define("luciusAnimeArc", true);
        LUCIUS_TIME_REVERSAL = b.comment("The owner's second exception to 'no second wind' (like refillAtGates): in act 2, after a burst of damage Lucius channels Time Reversal for 3 seconds",
                        "and returns to the health he had when the burst began, never above it. Anti-magic, a heavy hit or a lock interrupts it. false = he never rewinds wounds.",
                        "Needs luciusAnimeArc. BossHealGuard stays the whitelist: the restore runs through BossHealGuard.allow and the budget bypass.")
                .define("luciusTimeReversal", true);
        BLACK_CLOVER_STATS = b.comment("3.64.0: the Black Clover bosses (Dark Triad, Lucius, Devil Avatar, Zagred, the Midnight Sun elves, the early-arc and Spade Kingdom bosses) have this many times their maximum health",
                        "and deal this many times the damage (a boosted hit never takes more than half of a victim's maximum health). 3 = three times as strong, 1 = off (the old numbers).")
                .defineInRange("blackCloverStatMultiplier", 3.0, 1.0, 10.0);
        PARTY_SCALING = b.comment("Leon study fix 5: every boss of the Zogratis framework (not Lucius, who has his own arena scaling, and not the Tier List rows) scales by the fighters present when the fight starts:",
                        "health x0.70 / 1.00 / 1.15 / 1.30 and damage x0.80 / 1.00 / 1.06 / 1.12 for 1 / 2 / 3 / 4+ players, read once at fight start and kept under a combined cap with the Black Clover x3 and the floor scale",
                        "(health at most x8, damage at most x5 in total). false = no party scaling (the old numbers).")
                .define("partyScaling", true);
        BOSS_DATA = b.comment("Leon study fix 9: data packs may override a boss's health, attack, speed, armor and move cooldown scale in data/anime_multiverse/arc_bosses/<boss id>.json (read when the boss joins the world).",
                        "false = the files are ignored and the Java numbers stand. No file ships with the mod.")
                .define("bossData", true);
        REWORK_MOVES = b.comment("Boss rework wave 1: the new moves appended to the Goliath, the God of Order, the Charybdis Spawn and seven Tier List bosses. false = their old move lists (read when a boss is built).")
                .define("bossRework1Moves", true);
        REWORK_VFX = b.comment("Boss rework wave 1: Lucifero's shaped gravity effects and the signature effects of the Black Goliath, the Minotaur, the Amphisbaena and the new moves. false = the old dust bursts.",
                        "A server setting because the server decides which effect it sends; it changes looks only.")
                .define("bossRework1Vfx", true);
        REWORK3_MOVES = b.comment("Boss rework wave 3: the one new signature move appended to 16 early, Midnight Sun and Seven Deadly Sins bosses (read when a boss is built). false = their old move lists.")
                .define("bossRework3Moves", true);
        REWORK3_DECENNIAL = b.comment("Boss rework wave 3: the extra move of each decennial boss family (read when a boss is built). false = their old moves. The visual signatures are client looks.")
                .define("bossRework3DecennialMoves", true);
        b.pop();
        b.push("dungeonBosses");
        DUNGEON_REWORK = b.comment("Dungeon-boss rework: the master switch of the new Floor Warden move set (Hammer Slam, Chain Lash, Shadow Step, Seal Pulse, the Key Toll, three phases, an enrage timer) and the creatures' signature moves.",
                        "false = every dungeon boss and creature fights as before the rework (read every tick; the new look is a resource-pack matter and stays).")
                .define("rework", true);
        DUNGEON_WARDEN = b.comment("The Floor Warden's new move set. Needs dungeonBosses.rework. false = the old Ground Slam and Shadow Step only. Labyrinth Guardians are never affected.")
                .define("warden", true);
        DUNGEON_CREATURES = b.comment("The Direwolf's pounce and howl, the Basilisk's gaze, spit and tail sweep, the Living Armor's shield bash and rattle. Needs dungeonBosses.rework. false = their old bite-only fight.")
                .define("creatures", true);
        DUNGEON_GUARDIANS = b.comment("Dungeon-boss rework 2: the Labyrinth Guardians (sector bosses and the 200 Decennial bosses) fight with a themed, telegraphed move set (a slam, a lance, a rain of marks, a wave, an ultimate at a quarter of its health) and wear their sector's colourway.",
                        "Needs dungeonBosses.rework. false = the Guardians keep the old Ground Slam and Shadow Step and the plain Warden look (their traits, D&D layer and Decennial layer are never affected).")
                .define("guardians", true);
        DUNGEON_SOUNDS = b.comment("Dungeon-boss rework 2: the bosses' and creatures' own sound set (dboss.* cues: telegraph whoosh, slam, chain rattle, roar, howl, hiss, gong, bell toll ...). Needs dungeonBosses.rework.",
                        "false = the moves play the vanilla sounds they used before. Read at every cue.")
                .define("sounds", true);
        DUNGEON_THEMES = b.comment("Dungeon-boss rework 2: each boss and creature has its own particle theme (colours and particles) instead of the shared seal theme. Needs dungeonBosses.rework. false = the old seal theme. A looks-only setting the server decides because it sends the effects.")
                .define("themes", true);
        b.pop();
        b.push("bossCombat");
        REACTIVE_GUARD = b.comment("Leon study fix #4: Meliodas, Ban, Heath Grice, Vetto, Mars and Udaeus guard when a foe is within 5 blocks (blocks let a quarter of the hit through, dodges take none and last 8 ticks, a block is answered with a counter swing).",
                        "Cooldown limited (block 40 ticks, dodge 60, at most one dodge a second), never while the boss is exposed, winding up a move or transforming, and it runs before the power-step clamp and the damage budget, which stay the arbiters.",
                        "false = these bosses take every hit as before.")
                .define("reactiveGuard", true);
        GUARD_BLOCK_CHANCE = b.comment("Chance that a hit on a guarding boss starts a block (when its block cooldown is over). Leon's 0.70. Clamped to 0.9 at most.")
                .defineInRange("guardBlockChance", 0.70, 0.0, 0.9);
        GUARD_DODGE_CHANCE = b.comment("Chance that a hit on a guarding boss starts a dodge (8 invulnerable ticks, when its dodge cooldown is over). Leon's 0.30. Clamped to 0.9 at most.")
                .defineInRange("guardDodgeChance", 0.30, 0.0, 0.9);
        ATTACK_TIMELINE = b.comment("Leon study fix #2: the melee of Meliodas, Ban, Escanor, Heath Grice, Vetto, Mars, Udaeus, the Minotaur and the Black Goliath lands on the hit tick of a per-attack clip (the swing is drawn to that tick) instead of on a fixed cooldown.",
                        "Damage per second is unchanged (each move's multiplier is its clip length over 20 ticks). false = the old fixed cooldown and the old client lead.")
                .define("attackTimeline", true);
        b.pop();
        SPEC = b.build();
    }

    private BossRulesConfig() {}

    public static boolean stripRegeneration() { return get(STRIP_REGENERATION); }

    public static boolean banUniqueUltimate() { return get(BAN_UNIQUE_ULTIMATE); }

    public static String forbiddenWords() { return get(FORBIDDEN_WORDS); }

    /** 3.64.0: the factor on the health and damage of the Black Clover bosses (1 = off). */
    public static double blackCloverStatMultiplier() { return get(BLACK_CLOVER_STATS); }

    /** Leon study fix 5: party-size scaling of every framework boss (false = the old numbers). */
    public static boolean partyScaling() { return get(PARTY_SCALING); }

    /** Leon study fix 9: the arc_bosses data files are read (false = ignored). */
    public static boolean bossData() { return get(BOSS_DATA); }

    public static boolean luciusComboAi() { return get(LUCIUS_COMBO_AI); }

    public static boolean refillAtGates() { return get(REFILL_AT_GATES); }

    /** The final-arc Lucius (1.42.0): three acts, chains, Soul Touch, the new precognition, clones and the chest gem. */
    public static boolean luciusAnimeArc() { return get(LUCIUS_ANIME_ARC); }

    /** The owner's exception: Lucius's channelled Time Reversal (act 2) may restore the health a burst took. */
    public static boolean luciusTimeReversal() { return get(LUCIUS_TIME_REVERSAL); }

    /** The switches and words as the pure policy wants them. */
    public static BossSkillPolicy.Settings settings() {
        return new BossSkillPolicy.Settings(stripRegeneration(), banUniqueUltimate(), forbiddenWords());
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Boss rework wave 1: are the appended moves in the move lists? */
    public static boolean reworkMoves() { return get(REWORK_MOVES); }

    /** Boss rework wave 1: the shaped effects (false = the old dust bursts). */
    public static boolean reworkVfx() { return get(REWORK_VFX); }

    /** Boss rework wave 3: the appended signature moves of the early, midnight and Sins bosses. */
    public static boolean rework3Moves() { return get(REWORK3_MOVES); }

    /** Boss rework wave 3: the extra move of each decennial boss family. */
    public static boolean rework3Decennial() { return get(REWORK3_DECENNIAL); }

    /** Dungeon-boss rework: the master switch. */
    public static boolean dungeonRework() { return get(DUNGEON_REWORK); }

    /** The Floor Warden's new move set (master switch and its own). */
    public static boolean dungeonWarden() { return get(DUNGEON_REWORK) && get(DUNGEON_WARDEN); }

    /** The creatures' signature moves (master switch and their own). */
    public static boolean dungeonCreatures() { return get(DUNGEON_REWORK) && get(DUNGEON_CREATURES); }

    /** Dungeon-boss rework 2: the Labyrinth Guardians' move sets and colourways (master switch and their own). */
    public static boolean dungeonGuardians() { return get(DUNGEON_REWORK) && get(DUNGEON_GUARDIANS); }

    /** Dungeon-boss rework 2: the boss sound set (master switch and its own). */
    public static boolean dungeonSounds() { return get(DUNGEON_REWORK) && get(DUNGEON_SOUNDS); }

    /** Dungeon-boss rework 2: the per-boss particle themes (master switch and their own). */
    public static boolean dungeonThemes() { return get(DUNGEON_REWORK) && get(DUNGEON_THEMES); }

    /** Leon study fix #4: the opted-in sword bosses block and dodge (false = every hit lands as before). */
    public static boolean reactiveGuard() { return get(REACTIVE_GUARD); }

    public static double guardBlockChance() { return get(GUARD_BLOCK_CHANCE); }

    public static double guardDodgeChance() { return get(GUARD_DODGE_CHANCE); }

    /** Leon study fix #2: melee damage on the hit tick of a per-attack timeline (false = the old fixed cooldown). */
    public static boolean attackTimeline() { return get(ATTACK_TIMELINE); }
}
