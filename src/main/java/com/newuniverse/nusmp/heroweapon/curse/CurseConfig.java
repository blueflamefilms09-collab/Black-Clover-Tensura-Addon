package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Curse Series of the hero weapons, {@code nusmp-heroweapon-curse-server.toml}, in its own file (the pattern of {@code DndConfig}) so the
 * owner of a server can make the curse gentler or harsher without touching the other hero systems: the despair each terrible thing adds and the threshold at
 * which the weapon whispers, the price of an Embrace (hearts, and the floor of maximum health that is never crossed), how fast corruption rises and falls, how long
 * the Overrun lasts and how hard it hits, what cleanses, and one strength multiplier per sin (0 switches that sin's downside off). Every value is a plain
 * {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes
 * through {@link #get}, which answers the default while the spec is not loaded yet. The defaults are the {@code DEFAULT_} constants of {@link CurseTuning}, the
 * same numbers the unit tests and the documentation use; {@link #tuning()} turns the current values into the plain object the pure machine takes.
 */
public final class CurseConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    // despair and the offer
    private static final ModConfigSpec.IntValue OFFER_THRESHOLD, OFFER_SECONDS, REOFFER_SECONDS, DECLINE_DESPAIR, EMBRACE_DESPAIR, DESPAIR_DECAY, CALM_SECONDS;
    private static final ModConfigSpec.IntValue AMT_PET, AMT_AVATAR, AMT_PARTY, AMT_NPC, AMT_BETRAYAL, AMT_NEAR_DEATH, AMT_DEFEAT, AMT_BOSS_LOSS, AMT_OVERRUN_KILL;
    private static final ModConfigSpec.IntValue PAUSE_ALLY, PAUSE_BETRAYAL, PAUSE_NEAR_DEATH;
    private static final ModConfigSpec.DoubleValue ALLY_RADIUS, NEAR_DEATH_SHARE;
    // corruption and the Overrun
    private static final ModConfigSpec.IntValue PER_TECHNIQUE, ULTIMATE_FACTOR, MIN_PER_SECOND, CORRUPTION_DECAY;
    private static final ModConfigSpec.DoubleValue CORRUPTION_SCALE;
    private static final ModConfigSpec.IntValue OVERRUN_MIN, OVERRUN_MAX, OVERRUN_END, WEAK_SECONDS;
    private static final ModConfigSpec.DoubleValue OVERRUN_REACH, OVERRUN_RAW, OVERRUN_PLAYER_SHARE;
    private static final ModConfigSpec.BooleanValue OVERRUN_HITS_PLAYERS, OVERRUN_BLOCKS_TECHNIQUES;
    // tax and cleansing
    private static final ModConfigSpec.IntValue TAX_FIRST, TAX_NEXT, MIN_HEARTS, CHARM_HEARTS, CHARM_USES;
    private static final ModConfigSpec.DoubleValue REST_SHARE;
    // the seven sins
    private static final ModConfigSpec.DoubleValue INT_WRATH, INT_PRIDE, INT_LUST, INT_ENVY, INT_GREED, INT_SLOTH, INT_GLUTTONY;
    private static final ModConfigSpec.DoubleValue WRATH_DRAIN_SHARE, WRATH_DRAIN_MIN, WRATH_FLOOR_SHARE, WRATH_HEAL_CUT;
    private static final ModConfigSpec.DoubleValue PRIDE_HELP_CUT, PRIDE_SELF_HEAL;
    private static final ModConfigSpec.DoubleValue LUST_RANGE, LUST_MIN_DISTANCE, LUST_PULL;
    private static final ModConfigSpec.IntValue ENVY_INTERVAL, ENVY_LOCK;
    private static final ModConfigSpec.IntValue GREED_FULL;
    private static final ModConfigSpec.DoubleValue GREED_MAX_SLOW, GREED_SURCHARGE;
    private static final ModConfigSpec.DoubleValue GLUTTONY_EXHAUST, GLUTTONY_KILL_HEAL;
    private static final ModConfigSpec.IntValue GLUTTONY_FLOOR, GLUTTONY_KILL_FOOD;
    private static final ModConfigSpec.BooleanValue GLUTTONY_XP;
    private static final ModConfigSpec.DoubleValue SLOTH_SLOW, SLOTH_ATTACK_SLOW;
    private static final ModConfigSpec.IntValue SLOTH_LOCK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("curse");
        ENABLED = b.comment("Master switch of the Curse Series. false = no despair, no offers, no corruption, no Overrun; curse forms already learned simply stay forms.")
                .define("enabled", true);

        b.push("despair");
        OFFER_THRESHOLD = i(b, "Despair (0 to 1000) at which the weapon whispers its offer of a Curse Series.", "offerThreshold", CurseTuning.DEFAULT_OFFER_THRESHOLD, 50, 1000);
        OFFER_SECONDS = i(b, "Seconds an offer stays open. Nothing ever unlocks by itself: an offer that is not accepted only runs out.", "offerSeconds", CurseTuning.DEFAULT_OFFER_SECONDS, 10, 600);
        REOFFER_SECONDS = i(b, "Seconds the weapon stays silent after an offer lapsed, was declined or was accepted.", "reofferSeconds", CurseTuning.DEFAULT_REOFFER_SECONDS, 10, 7200);
        DECLINE_DESPAIR = i(b, "Despair is lowered to at most this when an offer lapses or is declined.", "declineDespair", CurseTuning.DEFAULT_DECLINE_DESPAIR, 0, 999);
        EMBRACE_DESPAIR = i(b, "Despair is lowered to at most this when an offer is accepted.", "embraceDespair", CurseTuning.DEFAULT_EMBRACE_DESPAIR, 0, 999);
        DESPAIR_DECAY = i(b, "Despair that fades each second while the wielder is calm (not hurt, no new despair).", "decayPerSecond", CurseTuning.DEFAULT_DESPAIR_DECAY, 0, 100);
        CALM_SECONDS = i(b, "Seconds without new despair before it starts to fade.", "calmSeconds", CurseTuning.DEFAULT_CALM_SECONDS, 0, 600);
        AMT_PET = i(b, "Despair when a tamed animal or other creature of the wielder dies within reach.", "petDeath", Cause.PET_DEATH.defaultAmount(), 0, 1000);
        AMT_AVATAR = i(b, "Despair when the wielder's Avatar dies within reach.", "avatarDeath", Cause.AVATAR_DEATH.defaultAmount(), 0, 1000);
        AMT_PARTY = i(b, "Despair when a party or FTB team member dies within reach.", "partyDeath", Cause.PARTY_DEATH.defaultAmount(), 0, 1000);
        AMT_NPC = i(b, "Despair when a friendly arc character dies within reach.", "npcDeath", Cause.NPC_DEATH.defaultAmount(), 0, 1000);
        AMT_BETRAYAL = i(b, "Despair when an ally, a party member or the wielder's own summon hurts the wielder.", "betrayal", Cause.BETRAYAL.defaultAmount(), 0, 1000);
        AMT_NEAR_DEATH = i(b, "Despair when the wielder's health falls under nearDeathShare (at most once per pauseNearDeathTicks).", "nearDeath", Cause.NEAR_DEATH.defaultAmount(), 0, 1000);
        AMT_DEFEAT = i(b, "Despair when the wielder dies while bonded.", "defeat", Cause.DEFEAT.defaultAmount(), 0, 1000);
        AMT_BOSS_LOSS = i(b, "Extra despair when a boss kills the wielder.", "bossLoss", Cause.BOSS_LOSS.defaultAmount(), 0, 1000);
        AMT_OVERRUN_KILL = i(b, "Despair of the other wielders near a wielder in an Overrun who kills someone close to them.", "overrunKill", Cause.OVERRUN_KILL.defaultAmount(), 0, 1000);
        PAUSE_ALLY = i(b, "Ticks between two counted deaths of the same kind of ally (twenty pets in one blast count once).", "pauseAllyTicks", CurseTuning.DEFAULT_PAUSE_ALLY, 0, 72000);
        PAUSE_BETRAYAL = i(b, "Ticks between two counted betrayals.", "pauseBetrayalTicks", CurseTuning.DEFAULT_PAUSE_BETRAYAL, 0, 72000);
        PAUSE_NEAR_DEATH = i(b, "Ticks between two counted near-deaths (1200 = once a minute).", "pauseNearDeathTicks", CurseTuning.DEFAULT_PAUSE_NEAR_DEATH, 0, 72000);
        ALLY_RADIUS = d(b, "Blocks within which the death of an ally is felt.", "allyRadius", CurseTuning.DEFAULT_ALLY_RADIUS, 4.0, 128.0);
        NEAR_DEATH_SHARE = d(b, "Share of maximum health under which the wielder counts as near death.", "nearDeathShare", CurseTuning.DEFAULT_NEAR_DEATH_SHARE, 0.02, 0.5);
        b.pop();

        b.push("corruption");
        PER_TECHNIQUE = i(b, "Corruption added by each technique cast with a curse form worn (needs the dispatcher to call HeroCurse.onTechnique).", "perTechnique", CurseTuning.DEFAULT_PER_TECHNIQUE, 0, 500);
        ULTIMATE_FACTOR = i(b, "An ultimate technique adds this many times perTechnique.", "ultimateFactor", CurseTuning.DEFAULT_ULTIMATE_FACTOR, 1, 20);
        MIN_PER_SECOND = i(b, "A curse form never raises corruption slower than this per second, whatever its own rate says.", "minPerSecond", CurseTuning.DEFAULT_MIN_PER_SECOND, 0, 100);
        CORRUPTION_DECAY = i(b, "Corruption that fades each second while no curse form is worn.", "decayPerSecond", CurseTuning.DEFAULT_CORRUPTION_DECAY, 0, 100);
        CORRUPTION_SCALE = d(b, "Multiplier of every corruption gain (forms' rates and techniques). 0 = curse forms cost no corruption.", "scale", CurseTuning.DEFAULT_CORRUPTION_SCALE, 0.0, 10.0);
        b.pop();

        b.push("overrun");
        OVERRUN_MIN = i(b, "Shortest Overrun, in seconds.", "minSeconds", CurseTuning.DEFAULT_OVERRUN_MIN_SECONDS, 1, 300);
        OVERRUN_MAX = i(b, "Longest Overrun, in seconds.", "maxSeconds", CurseTuning.DEFAULT_OVERRUN_MAX_SECONDS, 1, 300);
        OVERRUN_END = i(b, "Corruption the wielder is left with when an Overrun ends.", "endCorruption", CurseTuning.DEFAULT_OVERRUN_END_CORRUPTION, 0, 999);
        WEAK_SECONDS = i(b, "Seconds of Weakness II and Slowness I after an Overrun.", "weakSeconds", CurseTuning.DEFAULT_WEAK_SECONDS, 0, 3600);
        OVERRUN_REACH = d(b, "Blocks within which the weapon swings at the nearest living thing (allies included).", "reach", CurseTuning.DEFAULT_OVERRUN_REACH, 1.5, 8.0);
        OVERRUN_RAW = d(b, "Raw damage of one forced swing, before the balance law (two swings a second).", "rawDamage", CurseTuning.DEFAULT_OVERRUN_RAW, 0.0, 30.0);
        OVERRUN_PLAYER_SHARE = d(b, "A forced swing takes at most this share of another player's maximum health, so a friend who steps away survives.", "playerShare", CurseTuning.DEFAULT_OVERRUN_PLAYER_SHARE, 0.0, 1.0);
        OVERRUN_HITS_PLAYERS = b.comment("Whether the Overrun's swings may hit other players (their pets and mobs are always in reach).").define("hitsPlayers", CurseTuning.DEFAULT_OVERRUN_HITS_PLAYERS);
        OVERRUN_BLOCKS_TECHNIQUES = b.comment("Whether the wielder cannot use techniques during an Overrun (needs the dispatcher to ask HeroCurse.refuses).").define("blocksTechniques", CurseTuning.DEFAULT_OVERRUN_BLOCKS_TECHNIQUES);
        b.pop();

        b.push("tax");
        TAX_FIRST = i(b, "Hearts of maximum health the first Curse Series takes for good.", "firstHearts", CurseTuning.DEFAULT_TAX_FIRST, 0, 9);
        TAX_NEXT = i(b, "Hearts every further Curse Series takes.", "nextHearts", CurseTuning.DEFAULT_TAX_NEXT, 0, 9);
        MIN_HEARTS = i(b, "The tax never leaves the wielder with less than this many hearts of maximum health.", "minHearts", CurseTuning.DEFAULT_MIN_HEARTS, 1, 10);
        b.pop();

        b.push("purify");
        CHARM_HEARTS = i(b, "Hearts of tax a Purification Charm gives back (at most what is owed).", "charmRefundHearts", CurseTuning.DEFAULT_CHARM_REFUND_HEARTS, 0, 9);
        CHARM_USES = i(b, "How many times in all a wielder's charms give hearts back (the first use by default; the corruption is always cleared).", "charmRefundUses", CurseTuning.DEFAULT_CHARM_REFUND_USES, 0, 100);
        REST_SHARE = d(b, "Share of the corruption a finished rest at a Safe Zone of the Mega-Dungeon clears.", "restClearShare", CurseTuning.DEFAULT_REST_CLEAR_SHARE, 0.0, 1.0);
        b.pop();

        b.push("sins");
        INT_WRATH = d(b, "Strength of Wrath's downside (the burning blood and the weaker healing). 0 = off, 3 = triple.", "intensityWrath", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_PRIDE = d(b, "Strength of Pride's downside (help does not heal). 0 = off.", "intensityPride", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_LUST = d(b, "Strength of Lust's downside (the pull toward the nearest foe). 0 = off.", "intensityLust", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_ENVY = d(b, "Strength of Envy's downside (it takes a technique and a boon). 0 = off.", "intensityEnvy", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_GREED = d(b, "Strength of Greed's downside (treasure weighs you down, techniques cost more). 0 = off.", "intensityGreed", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_SLOTH = d(b, "Strength of Sloth's downside (slower body, slower techniques). 0 = off.", "intensitySloth", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        INT_GLUTTONY = d(b, "Strength of Gluttony's downside (it devours magicules; the food bar is never touched). 0 = off.", "intensityGluttony", CurseTuning.DEFAULT_INTENSITY, 0.0, 3.0);
        WRATH_DRAIN_SHARE = d(b, "Wrath burns this share of maximum health each second (or wrathDrainMin, whichever is more).", "wrathDrainShare", CurseTuning.DEFAULT_WRATH_DRAIN_SHARE, 0.0, 0.2);
        WRATH_DRAIN_MIN = d(b, "Health points Wrath burns each second at least.", "wrathDrainMin", CurseTuning.DEFAULT_WRATH_DRAIN_MIN, 0.0, 10.0);
        WRATH_FLOOR_SHARE = d(b, "Wrath never burns the wielder under this share of maximum health (and never under one heart).", "wrathFloorShare", CurseTuning.DEFAULT_WRATH_FLOOR_SHARE, 0.0, 1.0);
        WRATH_HEAL_CUT = d(b, "Share of every heal that Wrath's curse flames take away.", "wrathHealCut", CurseTuning.DEFAULT_WRATH_HEAL_CUT, 0.0, 1.0);
        PRIDE_HELP_CUT = d(b, "Share of a heal that Pride refuses when it is help (more than prideSelfHealMax at once).", "prideHelpCut", CurseTuning.DEFAULT_PRIDE_HELP_CUT, 0.0, 1.0);
        PRIDE_SELF_HEAL = d(b, "A heal up to this many health points counts as the wielder's own recovery and is not refused.", "prideSelfHealMax", CurseTuning.DEFAULT_PRIDE_SELF_HEAL_MAX, 0.0, 20.0);
        LUST_RANGE = d(b, "Blocks within which Lust drags the wielder toward the nearest foe.", "lustRange", CurseTuning.DEFAULT_LUST_RANGE, 4.0, 32.0);
        LUST_MIN_DISTANCE = d(b, "Lust stops pulling once the foe is this close.", "lustMinDistance", CurseTuning.DEFAULT_LUST_MIN_DISTANCE, 1.0, 8.0);
        LUST_PULL = d(b, "Speed added toward the foe every half second (sneaking resists it).", "lustPull", CurseTuning.DEFAULT_LUST_PULL, 0.0, 0.5);
        ENVY_INTERVAL = i(b, "Seconds between two things Envy takes.", "envyIntervalSeconds", CurseTuning.DEFAULT_ENVY_INTERVAL_SECONDS, 3, 300);
        ENVY_LOCK = i(b, "Seconds a technique stays lost to Envy.", "envyLockSeconds", CurseTuning.DEFAULT_ENVY_LOCK_SECONDS, 1, 60);
        GREED_FULL = i(b, "Treasure score (an ingot or gem is 1, a block 9) at which Greed's burden is complete.", "greedBurdenFull", CurseTuning.DEFAULT_GREED_BURDEN_FULL, 1, 4096);
        GREED_MAX_SLOW = d(b, "Share of speed a complete burden of treasure takes.", "greedMaxSlow", CurseTuning.DEFAULT_GREED_MAX_SLOW, 0.0, 0.6);
        GREED_SURCHARGE = d(b, "Extra share of a technique's cost Greed charges after the cast.", "greedSurcharge", CurseTuning.DEFAULT_GREED_SURCHARGE, 0.0, 3.0);
        GLUTTONY_EXHAUST = d(b, "Gluttony devours this many half-percents of the maximum magicule pool each second (0.4 = 0.2% a second at normal strength; the hunger ban of 1.99.0 moved it off the food bar). The key keeps its old name so old configs load.", "gluttonyExhaust", CurseTuning.DEFAULT_GLUTTONY_EXHAUST, 0.0, 5.0);
        GLUTTONY_FLOOR = i(b, "No longer used (the hunger ban of 1.99.0): Gluttony does not touch the stomach. Kept so old configs load.", "gluttonyFoodFloor", CurseTuning.DEFAULT_GLUTTONY_FOOD_FLOOR, 0, 20);
        GLUTTONY_KILL_FOOD = i(b, "Food points a kill gives back while Gluttony is worn.", "gluttonyKillFood", CurseTuning.DEFAULT_GLUTTONY_KILL_FOOD, 0, 20);
        GLUTTONY_KILL_HEAL = d(b, "Share of maximum health a kill heals while Gluttony is worn (through the healing law).", "gluttonyKillHeal", CurseTuning.DEFAULT_GLUTTONY_KILL_HEAL, 0.0, 0.25);
        GLUTTONY_XP = b.comment("Gluttony turns experience away: vanilla experience orbs are not picked up while it is worn (they stay on the ground).").define("gluttonyBlocksXp", CurseTuning.DEFAULT_GLUTTONY_BLOCKS_XP);
        SLOTH_SLOW = d(b, "Share of movement speed Sloth takes.", "slothSlow", CurseTuning.DEFAULT_SLOTH_SLOW, 0.0, 0.6);
        SLOTH_ATTACK_SLOW = d(b, "Share of attack speed Sloth takes.", "slothAttackSlow", CurseTuning.DEFAULT_SLOTH_ATTACK_SLOW, 0.0, 0.6);
        SLOTH_LOCK = i(b, "Ticks after each technique during which Sloth holds the next one back (needs the dispatcher to ask HeroCurse.refuses).", "slothLockTicks", CurseTuning.DEFAULT_SLOTH_LOCK_TICKS, 0, 400);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private CurseConfig() {}

    private static ModConfigSpec.IntValue i(ModConfigSpec.Builder b, String comment, String name, int def, int min, int max) { return b.comment(comment).defineInRange(name, def, min, max); }

    private static ModConfigSpec.DoubleValue d(ModConfigSpec.Builder b, String comment, String name, double def, double min, double max) { return b.comment(comment).defineInRange(name, def, min, max); }

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Master switch. */
    public static boolean enabled() { return get(ENABLED); }

    /** The current values as the plain object the pure machine takes. */
    public static CurseTuning tuning() {
        CurseTuning t = CurseTuning.defaults();
        t.offerThreshold = get(OFFER_THRESHOLD);
        t.offerSeconds = get(OFFER_SECONDS);
        t.reofferSeconds = get(REOFFER_SECONDS);
        t.declineDespair = get(DECLINE_DESPAIR);
        t.embraceDespair = get(EMBRACE_DESPAIR);
        t.despairDecayPerSecond = get(DESPAIR_DECAY);
        t.calmSeconds = get(CALM_SECONDS);
        t.setAmount(Cause.PET_DEATH, get(AMT_PET));
        t.setAmount(Cause.AVATAR_DEATH, get(AMT_AVATAR));
        t.setAmount(Cause.PARTY_DEATH, get(AMT_PARTY));
        t.setAmount(Cause.NPC_DEATH, get(AMT_NPC));
        t.setAmount(Cause.BETRAYAL, get(AMT_BETRAYAL));
        t.setAmount(Cause.NEAR_DEATH, get(AMT_NEAR_DEATH));
        t.setAmount(Cause.DEFEAT, get(AMT_DEFEAT));
        t.setAmount(Cause.BOSS_LOSS, get(AMT_BOSS_LOSS));
        t.setAmount(Cause.OVERRUN_KILL, get(AMT_OVERRUN_KILL));
        t.pauseAlly = get(PAUSE_ALLY);
        t.pauseBetrayal = get(PAUSE_BETRAYAL);
        t.pauseNearDeath = get(PAUSE_NEAR_DEATH);
        t.allyRadius = get(ALLY_RADIUS);
        t.nearDeathShare = get(NEAR_DEATH_SHARE);
        t.corruptionPerTechnique = get(PER_TECHNIQUE);
        t.ultimateFactor = get(ULTIMATE_FACTOR);
        t.minCorruptionPerSecond = get(MIN_PER_SECOND);
        t.corruptionDecayPerSecond = get(CORRUPTION_DECAY);
        t.corruptionScale = get(CORRUPTION_SCALE);
        t.overrunMinSeconds = get(OVERRUN_MIN);
        t.overrunMaxSeconds = get(OVERRUN_MAX);
        t.overrunEndCorruption = get(OVERRUN_END);
        t.weakSeconds = get(WEAK_SECONDS);
        t.overrunReach = get(OVERRUN_REACH);
        t.overrunRaw = get(OVERRUN_RAW);
        t.overrunPlayerShare = get(OVERRUN_PLAYER_SHARE);
        t.overrunHitsPlayers = get(OVERRUN_HITS_PLAYERS);
        t.overrunBlocksTechniques = get(OVERRUN_BLOCKS_TECHNIQUES);
        t.taxFirstHearts = get(TAX_FIRST);
        t.taxNextHearts = get(TAX_NEXT);
        t.minHearts = get(MIN_HEARTS);
        t.charmRefundHearts = get(CHARM_HEARTS);
        t.charmRefundUses = get(CHARM_USES);
        t.restClearShare = get(REST_SHARE);
        t.setIntensity(Sin.WRATH, get(INT_WRATH));
        t.setIntensity(Sin.PRIDE, get(INT_PRIDE));
        t.setIntensity(Sin.LUST, get(INT_LUST));
        t.setIntensity(Sin.ENVY, get(INT_ENVY));
        t.setIntensity(Sin.GREED, get(INT_GREED));
        t.setIntensity(Sin.SLOTH, get(INT_SLOTH));
        t.setIntensity(Sin.GLUTTONY, get(INT_GLUTTONY));
        t.wrathDrainShare = get(WRATH_DRAIN_SHARE);
        t.wrathDrainMin = get(WRATH_DRAIN_MIN);
        t.wrathFloorShare = get(WRATH_FLOOR_SHARE);
        t.wrathHealCut = get(WRATH_HEAL_CUT);
        t.prideHelpCut = get(PRIDE_HELP_CUT);
        t.prideSelfHealMax = get(PRIDE_SELF_HEAL);
        t.lustRange = get(LUST_RANGE);
        t.lustMinDistance = get(LUST_MIN_DISTANCE);
        t.lustPull = get(LUST_PULL);
        t.envyIntervalSeconds = get(ENVY_INTERVAL);
        t.envyLockSeconds = get(ENVY_LOCK);
        t.greedBurdenFull = get(GREED_FULL);
        t.greedMaxSlow = get(GREED_MAX_SLOW);
        t.greedSurcharge = get(GREED_SURCHARGE);
        t.gluttonyExhaust = get(GLUTTONY_EXHAUST);
        t.gluttonyFoodFloor = get(GLUTTONY_FLOOR);
        t.gluttonyKillFood = get(GLUTTONY_KILL_FOOD);
        t.gluttonyKillHeal = get(GLUTTONY_KILL_HEAL);
        t.gluttonyBlocksXp = get(GLUTTONY_XP);
        t.slothSlow = get(SLOTH_SLOW);
        t.slothAttackSlow = get(SLOTH_ATTACK_SLOW);
        t.slothLockTicks = get(SLOTH_LOCK);
        return t;
    }
}
