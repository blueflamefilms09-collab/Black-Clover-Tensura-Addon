package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Dungeon Master, {@code nusmp-labyrinth-mega-enc-server.toml}, section {@code [enc]}. Its own file (the
 * {@code DndConfig} pattern) so the encounter director can be retuned or switched off without touching the generator's switches, and so a
 * server that loves the mazes but hates surprises sets one key. Every number the design calls "a guess" is here: how often the dice are rolled,
 * how long a party is left alone afterwards, how big the fights are, how fast noise fades, how well a boss sees and hears. Every value is a
 * plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and
 * every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class EncConfig {
    public static final ModConfigSpec SPEC;
    // the Dungeon Master (random encounters)
    public static final ModConfigSpec.BooleanValue ENABLED, CUES, FLOOR_THREAT;
    public static final ModConfigSpec.IntValue ROLL_EVERY, BASE_COOLDOWN, MIN_COOLDOWN, MAX_LIVE, MAX_PER_ENCOUNTER, DC, MAX_AGE, SPAWN_ATTEMPTS;
    // 1.94.0, the fixes of "encounters do not trigger / spawn badly"
    public static final ModConfigSpec.BooleanValue ROLL_FOR_CREATIVE, SPAWN_WIDEN, HUNT_BY_ROOMS;
    public static final ModConfigSpec.IntValue TENSION_EVERY, TENSION_CAP, RELAX_SIGHT_TIER, IDLE_LOG_SECONDS, STUCK_TICKS, RETRY_PASSES;
    public static final ModConfigSpec.DoubleValue PARTY_RADIUS, PARTY_EXTRA, BASE_BUDGET, NOISE_DECAY, NOISE_SCALE, NOISY_AT, HEALTH_PER_LEVEL, DAMAGE_PER_LEVEL,
            DESPAWN_DISTANCE, SPAWN_MIN, SPAWN_MAX;
    public static final ModConfigSpec.ConfigValue<String> PARTY_FACTORS;
    // the wandering bosses
    public static final ModConfigSpec.BooleanValue WANDERERS, ANNOUNCE;
    public static final ModConfigSpec.IntValue WANDERER_MIN_GAP, WANDERER_CHECK, LOSE_TICKS;
    public static final ModConfigSpec.DoubleValue WANDERER_CHANCE, SIGHT_RANGE, HEAR_RANGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("enc");
        ENABLED = b.comment("The Dungeon Master: encounters are rolled for parties in the Mega-Dungeon from the sector's threat, the party size, the noise and the light. false = nothing is spawned by it (wandering bosses have their own switch).")
                .define("enabled", true);
        ROLL_EVERY = b.comment("Ticks between two passes of the Dungeon Master. Each pass rolls the dice once for every party, one party per tick.")
                .defineInRange("rollEveryTicks", 100, 20, 1200);
        DC = b.comment("The difficulty class of the d20 check (d20 + threat + noise + light + party must reach it). Higher = rarer encounters.")
                .defineInRange("encounterDc", 17, 5, 30);
        BASE_COOLDOWN = b.comment("Ticks a party is left alone after an encounter was spawned for it (shorter when the party is loud: base / (1 + noise / 5)).")
                .defineInRange("baseCooldownTicks", 1800, 100, 72000);
        MIN_COOLDOWN = b.comment("The cooldown never gets shorter than this, however loud the party is.")
                .defineInRange("minCooldownTicks", 400, 20, 72000);
        MAX_LIVE = b.comment("No new encounter while this many of the Dungeon Master's monsters live within 64 blocks of the party.")
                .defineInRange("maxLiveMobsPerParty", 8, 1, 64);
        MAX_PER_ENCOUNTER = b.comment("The most monsters one encounter may bring, whatever the budget says.")
                .defineInRange("maxMobsPerEncounter", 12, 1, 64);
        PARTY_RADIUS = b.comment("Players within this many blocks of each other (a chain of such players counts) are one party.")
                .defineInRange("partyRadius", 48.0, 8.0, 128.0);
        PARTY_FACTORS = b.comment("The encounter budget factor for a party of one, two, three ... players, comma separated. A bad list falls back to the default.")
                .define("partyFactors", PartyMath.DEFAULT_FACTORS);
        PARTY_EXTRA = b.comment("What every player beyond the list of partyFactors adds to the factor.")
                .defineInRange("partyFactorExtra", PartyMath.DEFAULT_EXTRA, 0.0, 5.0);
        BASE_BUDGET = b.comment("The base encounter budget in weak-monster equivalents (a 20-health monster is one point, a 100-health brute five), times the band, the threat and the party factor.")
                .defineInRange("baseBudget", 8.0, 1.0, 100.0);
        NOISE_DECAY = b.comment("The fraction of the noise level lost every second (0.2 = a half-life of about three seconds).")
                .defineInRange("noiseDecay", 0.2, 0.01, 1.0);
        NOISE_SCALE = b.comment("Multiplies every noise (footsteps, blocks, fights, blasts). 0 = the dungeon is deaf to the party's noise.")
                .defineInRange("noiseScale", 1.0, 0.0, 10.0);
        NOISY_AT = b.comment("A noise reading at or above this counts as 'noisy' for the encounter tables' needs.")
                .defineInRange("noisyAt", 3.0, 0.5, 40.0);
        HEALTH_PER_LEVEL = b.comment("Extra health of the Dungeon Master's monsters per threat level above one (0.06 = +6 percent a level).")
                .defineInRange("threatHealthPerLevel", 0.06, 0.0, 1.0);
        DAMAGE_PER_LEVEL = b.comment("Extra damage of the Dungeon Master's monsters per threat level above one (0.04 = +4 percent a level).")
                .defineInRange("threatDamagePerLevel", 0.04, 0.0, 1.0);
        SPAWN_MIN = b.comment("The nearest, in blocks, to any member of the party that an encounter may appear (a table entry can narrow it).")
                .defineInRange("spawnMinDistance", 24.0, 8.0, 64.0);
        SPAWN_MAX = b.comment("The farthest, in blocks, from the nearest member of the party that an encounter may appear.")
                .defineInRange("spawnMaxDistance", 48.0, 16.0, 96.0);
        SPAWN_ATTEMPTS = b.comment("At most four times this many positions are tried for EACH monster of an encounter before the Dungeon Master gives up on it (a bound on the work of one roll; before 1.94.0 it was twelve positions shared by the whole encounter, so one bad cell spent them all).")
                .defineInRange("spawnAttempts", 12, 2, 64);
        ROLL_FOR_CREATIVE = b.comment("false = players in Creative or Spectator mode are never rolled for (the monsters would not hunt them anyway). true = Creative players count as a party too, which is what a tester wants.")
                .define("rollForCreative", false);
        TENSION_EVERY = b.comment("The pity timer: every this many quiet passes (a roll that brought nothing) adds one point to the d20 of that party, up to tensionCap; an encounter that spawns spends them all.")
                .defineInRange("tensionEvery", Tension.DEFAULT_EVERY, 1, 50);
        TENSION_CAP = b.comment("The most points the pity timer adds. 0 = off (the dice alone decide: a party in the dark on the first floor then meets something only on a natural 20).")
                .defineInRange("tensionCap", Tension.DEFAULT_CAP, 0, 10);
        SPAWN_WIDEN = b.comment("When no place is found in the ring (spawnMinDistance to spawnMaxDistance, out of sight), try a wider ring, then every room, then a far place in view, instead of giving up.")
                .define("spawnWiden", true);
        RELAX_SIGHT_TIER = b.comment("From which rung of the search (1 ring, 2 wide ring, 3 every room, 4 far and in view) a place the party can see is accepted if it is far enough away (three quarters of the ring's far edge). 5 = never.")
                .defineInRange("spawnRelaxSightTier", 4, 1, 5);
        HUNT_BY_ROOMS = b.comment("The hunting monsters follow the doorways of the floor room by room instead of walking straight at the party (a straight walk through a maze finds no path). false = the old straight walk.")
                .define("huntByRooms", true);
        RETRY_PASSES = b.comment("When the dice say trouble but no place can be found, the next this many passes try again without rolling the dice. 0 = the roll is simply lost.")
                .defineInRange("retryPasses", 3, 0, 10);
        STUCK_TICKS = b.comment("A monster of the Dungeon Master that has not moved for this many ticks, has no target and is out of sight of every player is removed (it then no longer counts against maxLiveMobsPerParty).")
                .defineInRange("stuckTicks", 900, 200, 12000);
        IDLE_LOG_SECONDS = b.comment("Once this many seconds, while players are in the Mega-Dungeon and the Dungeon Master has spawned nothing, one line of the log says why. 0 = never.")
                .defineInRange("idleLogSeconds", 60, 0, 3600);
        DESPAWN_DISTANCE = b.comment("The Dungeon Master's monsters with no player within this many blocks are removed (a sweep every five seconds).")
                .defineInRange("despawnDistance", 96.0, 24.0, 256.0);
        MAX_AGE = b.comment("A monster of the Dungeon Master older than this many ticks, with no target and no player within 24 blocks, is removed.")
                .defineInRange("maxAgeTicks", 12000, 1200, 240000);
        CUES = b.comment("The Dungeon Master tells the party that something is coming with a distant sound and a little dust (an entry can have its own cue). false = no warning at all.")
                .define("cues", true);
        FLOOR_THREAT = b.comment("A sector is ten floors. true = the dice, the budget, the toughness window and the monsters' health follow the FLOOR the party stands on (its distance from the Hub, 1 to 10 in the first sector of a road; the deepest member of the party decides). false = the older rule: the threat of the sector, the same in every room of it.")
                .define("floorThreat", true);

        WANDERERS = b.comment("Wandering bosses: a big monster that walks the rooms of a sector and hunts by sight and sound. false = none appear.")
                .define("wanderers", true);
        WANDERER_CHANCE = b.comment("The chance, at each check of a sector with players, that a wandering boss appears (after the minimum gap; it grows with the time and the threat).")
                .defineInRange("wandererChance", 0.10, 0.0, 1.0);
        WANDERER_MIN_GAP = b.comment("Ticks after one wandering boss (appearing, dying or leaving) before the next may appear in the same sector.")
                .defineInRange("wandererMinGapTicks", 6000, 200, 720000);
        WANDERER_CHECK = b.comment("Ticks between two checks for a new wandering boss.")
                .defineInRange("wandererCheckTicks", 200, 40, 6000);
        SIGHT_RANGE = b.comment("How far a wandering boss sees a player who is standing in the light, moving, with a clear line of sight (half that crouching, 0.6 in the dark, 0.8 standing still).")
                .defineInRange("sightRange", 24.0, 4.0, 96.0);
        HEAR_RANGE = b.comment("How far a wandering boss hears a very loud sound (a blast); a quiet one carries a quarter as far.")
                .defineInRange("hearRange", 40.0, 8.0, 128.0);
        LOSE_TICKS = b.comment("Ticks without perceiving its target after which a wandering boss gives up the chase and searches.")
                .defineInRange("loseTicks", 160, 20, 2400);
        ANNOUNCE = b.comment("A low rumble and the line 'something heavy walks the halls' when a wandering boss appears. false = it arrives unannounced.")
                .define("announce", true);
        b.pop();
        SPEC = b.build();
    }

    private EncConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
