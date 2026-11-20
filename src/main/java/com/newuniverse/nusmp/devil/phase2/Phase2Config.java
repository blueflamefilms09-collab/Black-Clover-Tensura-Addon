package com.newuniverse.nusmp.devil.phase2;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the contract rules of phase 2 ({@code serverconfig/nusmp-devil-phase2-server.toml}, docs/devil_contract_phase2.md). Every one changes gameplay, so it is a server setting (rule 9),
 * every value is clamped again when read, and EVERY rule has its own switch that gives the game as it was before phase 2: {@code enabled = false} turns all of it off; {@code summon.enabled}, {@code ep.enabled},
 * {@code trial.enabled}, {@code kinds.enabled}, {@code stack.enabled}, {@code shared.enabled}, {@code sever.enabled} and {@code sabotage.enabled} turn one rule off each. The numbers that belong to a rank (the
 * EP table, the chances, which devil fights) are DATA ({@link Phase2Table}), not settings. The game rule {@code devilSeverOnKill} (default true) is checked next to {@code sever.enabled}.
 */
public final class Phase2Config {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, SUMMON, BETRAY, EP, TRIAL, KINDS, CONFIRM, STACK, SHARED, SEVER, SABOTAGE, CURSE, PACT_ONE, HOOK;
    private static final ModConfigSpec.DoubleValue EP_SCALE, IMPRESS, HEALTH_SCALE, DOMINANCE, FIRST, MAX, DEBUFF_POWER;
    private static final ModConfigSpec.IntValue TRIAL_SECONDS, TRIAL_MAX, WEAK_MAX, STRONG_MAX, CONTRACTS_MAX, DEBUFF_SECONDS, FF_HITS, FF_WINDOW;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("devilPhase2");
        ENABLED = b.comment("Phase 2 of the devil contracts as a whole. false = the game as it was before it: a ritual always brings the devil, no EP is asked, no trial, one contract, one holder per devil.")
                .define("enabled", true);
        b.push("summon");
        SUMMON = b.comment("A finished ritual does not always bring the devil: the chance is the rank's (data) plus a bonus when you carry an item of that devil (tag anime_multiverse:devil_relics/<devil>). false = it always comes.")
                .define("enabled", true);
        BETRAY = b.comment("A devil that is flagged in the data (devil_phase2.json, betray) may turn on everyone in the circle except the caller when it appears. false = it never does.").define("betrayal", true);
        b.pop();
        b.push("ep");
        EP = b.comment("The ego rule: a contract asks the player's Tensura existence points (EP), by rank (data: Supreme about 1 billion, a tenth less for each rank down). false = no requirement.").define("enabled", true);
        EP_SCALE = b.comment("Multiplier on the whole EP table (0.5 = half of every requirement).").defineInRange("scale", 1.0, 0.0, 1000.0);
        b.pop();
        b.push("trial");
        TRIAL = b.comment("A devil flagged in the data fights back before it agrees (the trial). false = no devil fights: no trial, and no dominance contract.").define("enabled", true);
        TRIAL_SECONDS = b.comment("The longest a trial may last, in seconds, whatever else happens.").defineInRange("seconds", 150, 30, 600);
        TRIAL_MAX = b.comment("The most trial fights on the whole server at once (one per player at most). Over the cap a ritual waits: the devil does not take shape.").defineInRange("max", 4, 0, 16);
        IMPRESS = b.comment("The devil is impressed when its health falls to this share of its maximum (or under): it offers an even or a submission contract. Beating it unlocks dominance.").defineInRange("impressShare", 0.5, 0.05, 0.95);
        HEALTH_SCALE = b.comment("Multiplier on the health of the trial devil (the numbers per rank are in the rules).").defineInRange("healthScale", 1.0, 0.1, 20.0);
        b.pop();
        b.push("kinds");
        KINDS = b.comment("Three kinds of contract: submission (a quarter of the devil's power, safer: the creeping corruption grows half as fast), even (half) and dominance (all of it). false = one kind, full power, as before.")
                .define("enabled", true);
        DOMINANCE = b.comment("A dominance contract asks this many times the single EP requirement, and a won trial.").defineInRange("dominanceEpFactor", 3.0, 1.0, 100.0);
        CONFIRM = b.comment("The confirmation panel before signing (how many contracts the devil already has, your share, the kinds you can take). false = /devil sign signs at once.").define("confirm", true);
        b.pop();
        b.push("stack");
        STACK = b.comment("A player may hold several devils. false = one contract per player, as before.").define("enabled", true);
        WEAK_MAX = b.comment("Weaker devils (every rank under Supreme) stack freely, up to this many at once (a bound, so the cost stays bounded).").defineInRange("weakMax", 8, 1, Phase2Rules.MAX_WEAK_CAP);
        STRONG_MAX = b.comment("The most Supreme devils one player may hold.").defineInRange("supremeMax", 2, 1, Phase2Rules.MAX_STRONG_CAP);
        FIRST = b.comment("The second strong devil asks this many times the single EP requirement (the owner's 5 to 10 times).").defineInRange("strongFirstFactor", 5.0, 1.0, 100.0);
        MAX = b.comment("The factor rises with each further strong devil, up to this (the owner's 10 times).").defineInRange("strongMaxFactor", 10.0, 1.0, 100.0);
        b.pop();
        b.push("shared");
        SHARED = b.comment("Devils under Supreme are SHARED: many players may hold one, and its power splits evenly between them. false = every devil has one holder (phase 1 One of One for all).").define("enabled", true);
        CONTRACTS_MAX = b.comment("The most contracts a shared devil may have; 4 = a quarter of its power each.").defineInRange("maxContracts", 4, 1, Phase2Rules.MAX_CONTRACTS_CAP);
        PACT_ONE = b.comment("The four grimoire pact devils (Ignivar, Verbael, Gravemaw, Kairovore) stay open to everybody. true = each has one holder in the world (enforced when a pact is bound).").define("pactDevilsOneOfOne", false);
        b.pop();
        b.push("sever");
        SEVER = b.comment("A holder who kills another holder of the SAME shared devil severs that contract for good (a ban list per player and devil). Also the game rule devilSeverOnKill. false = killing severs nothing.")
                .define("enabled", true);
        b.pop();
        b.push("sabotage");
        SABOTAGE = b.comment("Sabotage costs: the killer who severs a contract, and a holder who keeps hitting fellow holders, take a debuff (reduced devil power). false = no debuff.").define("enabled", true);
        DEBUFF_SECONDS = b.comment("How long the debuff lasts, in seconds (default 3 minutes).").defineInRange("seconds", 180, 10, 3600);
        DEBUFF_POWER = b.comment("The share of the devil's power left while the debuff lasts.").defineInRange("power", 0.5, 0.0, 1.0);
        CURSE = b.comment("While the debuff lasts the saboteur also carries the curse mark: Weakness.").define("curseMark", true);
        FF_HITS = b.comment("This many blows on fellow holders of one shared devil inside the window trip the same debuff. 0 = friendly fire is not punished.").defineInRange("friendlyFireHits", 5, 0, 100);
        FF_WINDOW = b.comment("The window of the friendly-fire count, in seconds.").defineInRange("friendlyFireWindowSeconds", 30, 1, 600);
        HOOK = b.comment("RESERVED, default off. The owner's third sabotage trigger (three contracts with one devil inside one Minecraft day) was not finished, so nothing acts on it. true = every new contract also posts the event DevilPactJoinedEvent with the day number, for a later rule.")
                .define("sameDayTripleHook", false);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private Phase2Config() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) { try { return Math.max(lo, Math.min(hi, v.get())); } catch (RuntimeException notLoaded) { return def; } }

    private static double real(ModConfigSpec.DoubleValue v, double def, double lo, double hi) { try { return Phase2Rules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; } }

    public static boolean enabled() { return on(ENABLED, true); }
    public static boolean summon() { return enabled() && on(SUMMON, true); }
    public static boolean betrayal() { return summon() && on(BETRAY, true); }
    public static boolean ep() { return enabled() && on(EP, true); }
    public static double epScale() { return real(EP_SCALE, 1.0, 0.0, 1000.0); }
    public static boolean trial() { return enabled() && on(TRIAL, true); }
    public static int trialTicks() { return whole(TRIAL_SECONDS, 150, 30, 600) * 20; }
    public static int trialMax() { return whole(TRIAL_MAX, 4, 0, 16); }
    public static double impressShare() { return real(IMPRESS, 0.5, 0.05, 0.95); }
    public static double trialHealthScale() { return real(HEALTH_SCALE, 1.0, 0.1, 20.0); }
    public static boolean kinds() { return enabled() && on(KINDS, true); }
    public static double dominanceFactor() { return real(DOMINANCE, 3.0, 1.0, 100.0); }
    public static boolean confirm() { return enabled() && on(CONFIRM, true); }
    public static boolean stack() { return enabled() && on(STACK, true); }
    public static int weakMax() { return whole(WEAK_MAX, 8, 1, Phase2Rules.MAX_WEAK_CAP); }
    public static int strongMax() { return whole(STRONG_MAX, 2, 1, Phase2Rules.MAX_STRONG_CAP); }
    public static double strongFirst() { return real(FIRST, 5.0, 1.0, 100.0); }
    public static double strongMaxFactor() { return real(MAX, 10.0, 1.0, 100.0); }
    public static boolean shared() { return enabled() && on(SHARED, true); }
    public static int maxContracts() { return Phase2Rules.contractCap(whole(CONTRACTS_MAX, 4, 1, Phase2Rules.MAX_CONTRACTS_CAP)); }
    public static boolean pactOneOfOne() { return enabled() && on(PACT_ONE, false); }
    public static boolean sever() { return enabled() && on(SEVER, true); }
    public static boolean sabotage() { return enabled() && on(SABOTAGE, true); }
    public static int debuffSeconds() { return whole(DEBUFF_SECONDS, 180, 10, 3600); }
    public static double debuffPower() { return real(DEBUFF_POWER, 0.5, 0.0, 1.0); }
    public static boolean curseMark() { return on(CURSE, true); }
    public static int friendlyFireHits() { return whole(FF_HITS, 5, 0, 100); }
    public static int friendlyFireWindow() { return whole(FF_WINDOW, 30, 1, 600); }
    public static boolean sameDayHook() { return enabled() && on(HOOK, false); }
}
