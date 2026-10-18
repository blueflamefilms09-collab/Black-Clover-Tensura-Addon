package com.newuniverse.nusmp.elite.nations.world;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

/**
 * Why: the server config of the Nations & War system, {@code world/serverconfig/nusmp-elite-nations-server.toml}. Every number of the system is here (owner's rule: "every number configurable"), and
 * {@code general.enabled} is the master switch: off, the system registers its blocks (they must exist) but answers nothing, spawns nothing, ticks nothing and protects nothing. Plain
 * {@code define}/{@code defineInRange} values only (a list-validated value throws while the server is still loading), and every reader goes through {@link #get}, which answers the default while the spec is
 * not loaded yet (a self-test or a chunk event can ask before the server config is read).
 */
public final class NationsConfig {
    public static final ModConfigSpec SPEC;

    // general
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue TICK_INTERVAL, SYNC_MIN_INTERVAL, MAX_NATIONS;
    // founding / members
    public static final ModConfigSpec.ConfigValue<String> FOUND_COST_MODE, FOUND_COST_ITEM, ENTRY_ROLE;
    public static final ModConfigSpec.LongValue FOUND_COST_COINS;
    public static final ModConfigSpec.IntValue FOUND_COST_ITEM_COUNT, NAME_MIN, NAME_MAX, CAPITAL_RADIUS, BASE_MEMBER_CAP, MAX_MEMBER_CAP, INVITE_TTL_SECONDS, MAX_CUSTOM_ROLES;
    // levels
    public static final ModConfigSpec.IntValue MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue THRESHOLD_BASE, THRESHOLD_GROWTH, W_MEMBER, W_TREASURY_CAP, W_CONTRACT, W_AREA, W_WARWIN;
    public static final ModConfigSpec.LongValue COINS_PER_TREASURY_POINT;
    public static final ModConfigSpec.IntValue AREA_SCORE_CAP, AREA_INTERVAL_SECONDS;
    // economy
    public static final ModConfigSpec.LongValue BASE_TREASURY_CAP, INTEREST_MIN_BALANCE, INTEREST_MAX_PER_PERIOD;
    public static final ModConfigSpec.IntValue LEDGER_MAX_ENTRIES, INTEREST_PERIOD_MINUTES, INTEREST_PER_MILLE, MAX_TAX_PERCENT;
    public static final ModConfigSpec.ConfigValue<String> INTEREST_MODE, UBS_API_CLASS, UBS_BALANCE_METHOD, UBS_DEPOSIT_METHOD, UBS_WITHDRAW_METHOD, UBS_INTEREST_METHOD;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> UBS_MOD_IDS;
    // contracts
    public static final ModConfigSpec.IntValue BOARD_BASE_SLOTS, MAX_TAKEN_PER_PLAYER, BOARD_REFRESH_MINUTES, BOUNTY_FEE_PERCENT, BOUNTY_MAX_PER_PLAYER, BOUNTY_TTL_HOURS;
    public static final ModConfigSpec.LongValue BOUNTY_MIN;
    // reputation
    public static final ModConfigSpec.IntValue REP_DECAY_AMOUNT, REP_DECAY_INTERVAL_MINUTES, REP_KILL_MEMBER_PENALTY, REP_GUARD_ATTACK_PENALTY, REP_BOUNTY_KILL;
    // guards
    public static final ModConfigSpec.BooleanValue GUARDS_ENABLED, GUARD_SCALE_STATS;
    public static final ModConfigSpec.IntValue GUARD_BASE, GUARD_HARD_CAP, GUARD_GLOBAL_CAP, GUARD_INTERVAL_SECONDS, GUARD_PER_ROUND, GUARD_ACTIVE_RANGE;
    public static final ModConfigSpec.DoubleValue GUARD_PER_LEVEL;
    // war
    public static final ModConfigSpec.BooleanValue WAR_ENABLED, WAR_ONLY_PVP, PROTECT_SAME_NATION, PROTECT_CAPITAL, PROTECT_EXPLOSIONS, WAR_SCORE_IN_DUNGEON;
    public static final ModConfigSpec.IntValue DECLARE_SECONDS, PREP_MINUTES, ACTIVE_MINUTES, CEASEFIRE_MINUTES, TARGET_SCORE, KILL_POINTS, KILL_COOLDOWN_MINUTES, HOLD_POINTS, CAPTURE_POINTS,
            CAPTURE_INTERVALS, SIEGE_INTERVAL_SECONDS, MAX_WARS, MIN_LEVEL_TO_DECLARE, DECLARE_COOLDOWN_MINUTES, TRUCE_MINUTES, SPOILS_PERCENT, SPOILS_REP, WINNER_BUFF_MINUTES,
            ALLIANCE_MAX, PROPOSAL_TTL_MINUTES, ALLIANCE_BREAK_COOLDOWN_MINUTES, HISTORY_MAX, WAR_BOUNTY_COINS, WAR_WIN_XP;
    public static final ModConfigSpec.LongValue SPOILS_CAP;
    // perks
    public static final ModConfigSpec.BooleanValue PERK_ATTRIBUTES;
    public static final ModConfigSpec.IntValue PERK_REFRESH_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Master switch of the whole Nations & War system. Off: nothing is simulated, protected or spawned (the blocks still exist so a world keeps its placed banners).").define("enabled", true);
        TICK_INTERVAL = b.comment("Game ticks between two runs of the nation loop (war phases, expiry, interest, perks). 20 = once a second.").defineInRange("tickInterval", 20, 5, 1200);
        SYNC_MIN_INTERVAL = b.comment("Minimum game ticks between two screen updates sent to one player.").defineInRange("syncMinInterval", 10, 2, 200);
        MAX_NATIONS = b.comment("The most nations the server holds.").defineInRange("maxNations", 64, 1, 4096);
        b.pop();

        b.push("founding");
        FOUND_COST_MODE = b.comment("What founding costs: \"coins\" (the wallet of the Tensura-coin economy), \"item\", \"both\" or \"free\". Until the coin economy is installed the wallet refuses everything, so a server without it should use \"item\" or \"free\".")
                .define("costMode", "coins");
        FOUND_COST_COINS = b.comment("Founding price in copper coins (mode coins/both).").defineInRange("costCoins", 5000L, 0L, 1_000_000_000L);
        FOUND_COST_ITEM = b.comment("Item id taken when founding (mode item/both), for example minecraft:gold_block. Empty = none.").define("costItem", "minecraft:gold_ingot");
        FOUND_COST_ITEM_COUNT = b.comment("How many of that item.").defineInRange("costItemCount", 16, 0, 2304);
        NAME_MIN = b.defineInRange("nameMinLength", 3, 1, 16);
        NAME_MAX = b.defineInRange("nameMaxLength", 24, 4, 32);
        CAPITAL_RADIUS = b.comment("Radius in blocks of the capital area: where guards patrol, members get the level perks, a siege is counted and (protectCapital) strangers cannot build.").defineInRange("capitalRadius", 32, 8, 128);
        BASE_MEMBER_CAP = b.defineInRange("baseMemberCap", 10, 1, 1000);
        MAX_MEMBER_CAP = b.comment("Hard ceiling for the member cap, perks included.").defineInRange("maxMemberCap", 100, 1, 1000);
        INVITE_TTL_SECONDS = b.defineInRange("inviteTtlSeconds", 120, 10, 86400);
        MAX_CUSTOM_ROLES = b.comment("Custom roles a nation may add on top of the four default ones.").defineInRange("maxCustomRoles", 6, 0, 32);
        ENTRY_ROLE = b.comment("Role id a new member starts with (leader, officer, member, recruit or a custom role id; an unknown id falls back to recruit).").define("entryRole", "member");
        b.pop();

        b.push("levels");
        MAX_LEVEL = b.defineInRange("maxLevel", 10, 2, 50);
        THRESHOLD_BASE = b.comment("Score for level 2. Level n needs base * (n-1)^growth.").defineInRange("thresholdBase", 150.0, 1.0, 1.0e9);
        THRESHOLD_GROWTH = b.defineInRange("thresholdGrowth", 1.6, 0.1, 5.0);
        W_MEMBER = b.comment("Score points per member.").defineInRange("weightMember", 20.0, 0.0, 1.0e6);
        COINS_PER_TREASURY_POINT = b.comment("Copper coins in the treasury that make one score point. 0 = the treasury gives no score.").defineInRange("coinsPerTreasuryPoint", 1000L, 0L, 1_000_000_000L);
        W_TREASURY_CAP = b.comment("The most score points the treasury can give.").defineInRange("treasuryPointCap", 300.0, 0.0, 1.0e6);
        W_CONTRACT = b.comment("Score points per completed contract.").defineInRange("weightContract", 10.0, 0.0, 1.0e6);
        W_AREA = b.comment("Score points per point of held area score.").defineInRange("weightArea", 1.0, 0.0, 1.0e6);
        W_WARWIN = b.comment("Score points per war won.").defineInRange("weightWarWin", 50.0, 0.0, 1.0e6);
        AREA_SCORE_CAP = b.comment("Most held-area score a nation can have (it grows while members stand in the capital area).").defineInRange("areaScoreCap", 200, 0, 1_000_000);
        AREA_INTERVAL_SECONDS = b.comment("Seconds between two held-area checks (one point per check with a member inside).").defineInRange("areaIntervalSeconds", 60, 5, 3600);
        b.pop();

        b.push("economy");
        BASE_TREASURY_CAP = b.comment("Treasury cap in copper coins at level 1 (perks add to it).").defineInRange("baseTreasuryCap", 100_000L, 0L, 1_000_000_000_000L);
        LEDGER_MAX_ENTRIES = b.comment("History lines kept per nation (oldest dropped).").defineInRange("ledgerMaxEntries", 200, 10, 5000);
        MAX_TAX_PERCENT = b.comment("Highest tax a nation may set on the coin rewards its members earn from contracts.").defineInRange("maxTaxPercent", 30, 0, 100);
        INTEREST_MODE = b.comment("\"builtin\" (the simple interest below), \"ubs\" (ask the UBS banking mod through the bridge; falls back to builtin when UBS is absent) or \"off\".").define("interestMode", "builtin");
        INTEREST_PERIOD_MINUTES = b.defineInRange("interestPeriodMinutes", 60, 1, 10080);
        INTEREST_PER_MILLE = b.comment("Built-in interest per period, in thousandths of the balance (5 = 0.5 percent).").defineInRange("interestPerMille", 5, 0, 1000);
        INTEREST_MIN_BALANCE = b.defineInRange("interestMinBalance", 1000L, 0L, 1_000_000_000_000L);
        INTEREST_MAX_PER_PERIOD = b.comment("Most coins one period of built-in interest pays. 0 = no limit.").defineInRange("interestMaxPerPeriod", 5000L, 0L, 1_000_000_000_000L);
        UBS_MOD_IDS = b.comment("Mod ids under which the UBS (Ultimate Banking System) mod may be installed. The bridge does nothing if none is loaded.").defineList("ubsModIds", List.of("ubs", "ultimatebankingsystem", "ultimate_banking_system"), o -> o instanceof String);
        UBS_API_CLASS = b.comment("Fully qualified class of the UBS API (static methods taking a player UUID). Empty = the bridge stays inert even when UBS is loaded. The UBS API could not be inspected when this was written, so these names are yours to fill in.")
                .define("ubsApiClass", "");
        UBS_BALANCE_METHOD = b.comment("Static method (UUID) -> long returning a player's UBS cash.").define("ubsBalanceMethod", "getBalance");
        UBS_DEPOSIT_METHOD = b.comment("Static method (UUID, long) giving UBS cash.").define("ubsDepositMethod", "addBalance");
        UBS_WITHDRAW_METHOD = b.comment("Static method (UUID, long) -> boolean taking UBS cash.").define("ubsWithdrawMethod", "removeBalance");
        UBS_INTEREST_METHOD = b.comment("Optional static method (long balance) -> long: the interest UBS would pay on a balance. Empty = use the built-in rate.").define("ubsInterestMethod", "");
        b.pop();

        b.push("contracts");
        BOARD_BASE_SLOTS = b.comment("Open contracts on a nation's board at level 1.").defineInRange("boardBaseSlots", 4, 1, 50);
        MAX_TAKEN_PER_PLAYER = b.defineInRange("maxTakenPerPlayer", 3, 1, 20);
        BOARD_REFRESH_MINUTES = b.comment("Minutes between two refills of the boards.").defineInRange("boardRefreshMinutes", 30, 1, 1440);
        BOUNTY_FEE_PERCENT = b.comment("Percent on top of a bounty that the poster pays and that is burned.").defineInRange("bountyFeePercent", 10, 0, 100);
        BOUNTY_MIN = b.defineInRange("bountyMin", 100L, 1L, 1_000_000_000L);
        BOUNTY_MAX_PER_PLAYER = b.defineInRange("bountyMaxPerPlayer", 5, 1, 100);
        BOUNTY_TTL_HOURS = b.comment("Hours a bounty stays up; the escrow is refunded when it lapses. 0 = never.").defineInRange("bountyTtlHours", 72, 0, 8760);
        b.pop();

        b.push("reputation");
        REP_DECAY_AMOUNT = b.comment("Points every reputation moves toward zero per decay step. 0 = reputation never decays.").defineInRange("decayAmount", 1, 0, 100);
        REP_DECAY_INTERVAL_MINUTES = b.defineInRange("decayIntervalMinutes", 60, 1, 10080);
        REP_KILL_MEMBER_PENALTY = b.comment("Reputation lost with a nation when you kill one of its members outside a war.").defineInRange("killMemberPenalty", 60, 0, 1000);
        REP_GUARD_ATTACK_PENALTY = b.comment("Reputation lost with a nation when you kill one of its guards.").defineInRange("killGuardPenalty", 40, 0, 1000);
        REP_BOUNTY_KILL = b.comment("Reputation gained with the posting nation when you claim one of its bounties.").defineInRange("bountyClaimGain", 15, 0, 1000);
        b.pop();

        b.push("guards");
        GUARDS_ENABLED = b.comment("Nation guards patrol the capital (needs a placed capital banner).").define("enabled", true);
        GUARD_BASE = b.comment("Guards at level 1.").defineInRange("base", 2, 0, 50);
        GUARD_PER_LEVEL = b.comment("Extra guards per level above 1.").defineInRange("perLevel", 0.75, 0.0, 10.0);
        GUARD_HARD_CAP = b.comment("Most guards one nation keeps.").defineInRange("hardCapPerNation", 12, 0, 100);
        GUARD_GLOBAL_CAP = b.comment("Most guards alive on the whole server.").defineInRange("globalCap", 80, 0, 2000);
        GUARD_INTERVAL_SECONDS = b.comment("Seconds between two spawn rounds of one nation.").defineInRange("spawnIntervalSeconds", 15, 1, 3600);
        GUARD_PER_ROUND = b.defineInRange("perRound", 2, 1, 20);
        GUARD_ACTIVE_RANGE = b.comment("Guards only spawn while a player is this close to the capital (no chunk loading).").defineInRange("activeRange", 96, 16, 512);
        GUARD_SCALE_STATS = b.comment("Higher nation tiers give guards more health, damage and armour.").define("scaleStats", true);
        b.pop();

        b.push("war");
        WAR_ENABLED = b.comment("Wars and alliances. Off: nations still work but nobody can declare war.").define("enabled", true);
        DECLARE_SECONDS = b.comment("Seconds of notice between a declaration and the preparation phase.").defineInRange("declareSeconds", 60, 0, 86400);
        PREP_MINUTES = b.defineInRange("prepMinutes", 10, 0, 10080);
        ACTIVE_MINUTES = b.defineInRange("activeMinutes", 60, 1, 10080);
        CEASEFIRE_MINUTES = b.comment("Minutes of truce window after a fight (no fighting, no new war between the pair).").defineInRange("ceasefireMinutes", 30, 0, 10080);
        TARGET_SCORE = b.comment("First side to this score wins at once. 0 = play out the clock.").defineInRange("targetScore", 200, 0, 1_000_000);
        KILL_POINTS = b.defineInRange("killPoints", 10, 0, 10000);
        KILL_COOLDOWN_MINUTES = b.comment("The same victim scores for the other side only once per this many minutes (anti-farming).").defineInRange("killCooldownMinutes", 10, 0, 1440);
        HOLD_POINTS = b.comment("Points per siege interval in which a side outnumbers the other at the enemy capital.").defineInRange("holdPoints", 1, 0, 10000);
        CAPTURE_POINTS = b.defineInRange("capturePoints", 25, 0, 10000);
        CAPTURE_INTERVALS = b.comment("Consecutive held siege intervals that capture a capital.").defineInRange("captureIntervals", 6, 1, 1000);
        SIEGE_INTERVAL_SECONDS = b.defineInRange("siegeIntervalSeconds", 10, 1, 600);
        MAX_WARS = b.comment("Open wars one nation may have at once. 0 = unlimited.").defineInRange("maxWarsPerNation", 3, 0, 100);
        MIN_LEVEL_TO_DECLARE = b.defineInRange("minLevelToDeclare", 2, 1, 50);
        DECLARE_COOLDOWN_MINUTES = b.comment("Minutes a nation must wait after declaring before it declares again.").defineInRange("declareCooldownMinutes", 30, 0, 10080);
        TRUCE_MINUTES = b.comment("Minutes after a war ended during which the same pair cannot go to war again.").defineInRange("truceMinutes", 60, 0, 100000);
        SPOILS_PERCENT = b.comment("Percent of the loser's treasury the winner takes.").defineInRange("spoilsPercent", 10, 0, 100);
        SPOILS_CAP = b.defineInRange("spoilsCap", 50_000L, 0L, 1_000_000_000_000L);
        SPOILS_REP = b.comment("Reputation the winner's members earn with their own nation.").defineInRange("spoilsReputation", 25, 0, 1000);
        WINNER_BUFF_MINUTES = b.comment("Minutes of the temporary victory buff (Strength + Resistance) for the winner's online members. 0 = none.").defineInRange("winnerBuffMinutes", 20, 0, 1440);
        WAR_WIN_XP = b.comment("Nation XP the winner gets.").defineInRange("winXp", 100, 0, 1_000_000);
        WAR_BOUNTY_COINS = b.comment("Coins a nation's treasury puts on an enemy member who killed 3 of its members in one war (0 = no automatic war bounties).").defineInRange("warBountyCoins", 0, 0, 1_000_000);
        ALLIANCE_MAX = b.defineInRange("allianceMax", 3, 0, 50);
        PROPOSAL_TTL_MINUTES = b.defineInRange("proposalTtlMinutes", 10, 1, 10080);
        ALLIANCE_BREAK_COOLDOWN_MINUTES = b.defineInRange("allianceBreakCooldownMinutes", 60, 0, 100000);
        HISTORY_MAX = b.defineInRange("historyMax", 100, 1, 10000);
        WAR_ONLY_PVP = b.comment("Members of two different nations may only hurt each other while a war between them is ACTIVE. Players without a nation are not affected.").define("warOnlyPvp", true);
        PROTECT_SAME_NATION = b.comment("No friendly fire inside a nation (allied nations are always protected).").define("protectSameNation", true);
        PROTECT_CAPITAL = b.comment("Strangers (and members without the build permission) cannot break or place blocks inside a capital area. War never allows griefing.").define("protectCapital", true);
        PROTECT_EXPLOSIONS = b.comment("Explosions do not damage blocks inside a capital area.").define("protectExplosions", true);
        WAR_SCORE_IN_DUNGEON = b.comment("Kills inside the dungeon dimensions count for war score and PvP rules (default off: the dungeon is outside the nations' politics).").define("scoreInDungeon", false);
        b.pop();

        b.push("perks");
        PERK_ATTRIBUTES = b.comment("Members near their capital get the attribute perks of their nation's level.").define("applyAttributes", true);
        PERK_REFRESH_SECONDS = b.defineInRange("refreshSeconds", 5, 1, 120);
        b.pop();
        SPEC = b.build();
    }

    private NationsConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean warEnabled() { return get(ENABLED) && get(WAR_ENABLED); }

    public static long minutes(ModConfigSpec.IntValue v) { return get(v) * 60L * 20L; }

    public static long seconds(ModConfigSpec.IntValue v) { return get(v) * 20L; }

    public static String lower(ModConfigSpec.ConfigValue<String> v) { return get(v).toLowerCase(Locale.ROOT).trim(); }
}
