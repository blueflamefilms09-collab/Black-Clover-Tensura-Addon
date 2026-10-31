package com.newuniverse.nusmp.beastwill;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Why: the SERVER settings of the Beast Will system ({@code serverconfig/nusmp-beastwill-server.toml}, 3.45.0). Every value changes gameplay, so every value is a server setting (rule 9); each one is
 * clamped again when it is read, so a hand-edited file cannot go past a bound. {@code enabled = false} restores the game as it was before the feature: no core drops, no core works, a form in progress ends.
 * The numbers of the four wills themselves (costs, strain, cooldowns, stat bonuses) live in {@code data/nusmp/beast_will/*.json}; the scales here multiply them all at once.
 */
public final class BeastWillConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, EXCLUSIVE_INTEGRATE, PENALTIES, REJECTION, MEDITATION, STEALTH, HIT_PLAYERS, OTHER_RACES;
    private static final ModConfigSpec.DoubleValue DROP_SCALE, STRAIN_RATE, REST_PER_SECOND, STAT_SCALE, DRAIN_SCALE, COOLDOWN_SCALE, ASSIM_RATE, MASTERY_RATE, REJECT_ASSIM_LOSS, OTHER_RACE_SCALE,
            OVERLOAD_DAMAGE, RANK_C, RANK_B, RANK_A, RANK_S, RANK_SS;
    private static final ModConfigSpec.IntValue REJECT_LOCK_MINUTES, SWAP_LOCK_MINUTES, MIN_VICTIM_AGE, MAX_TARGETS_CAP;
    private static final ModConfigSpec.ConfigValue<String> MIN_DROP_RANK;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_HOST_RACES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("beastwill");
        ENABLED = b.comment("Beast Wills: the essence of a powerful mana beast. false = the feature is off: no core drops, no core can be absorbed, an active will ends.").define("enabled", true);
        b.push("acquisition");
        DROP_SCALE = b.comment("Multiplier on the drop chance of a Beast Will Core (each will has its own base chance in its data file). 0 = no core ever drops.").defineInRange("dropChanceScale", 1.0, 0.0, 5.0);
        MIN_DROP_RANK = b.comment("The lowest beast rank that can drop a core at all: C, B, A, S or SS. A will's own file can ask for more, never less than A.").define("minDropRank", "A");
        MIN_VICTIM_AGE = b.comment("A beast must have lived at least this many ticks to drop a core (stops spawn-and-kill farms). 1200 = one minute.").defineInRange("minVictimAgeTicks", 1200, 0, 72000);
        RANK_C = b.comment("The Existence Points at which a mana beast counts as rank C.").defineInRange("rankEpC", BeastWillRules.DEFAULT_RANK_EP[0], 0.0, 1e12);
        RANK_B = b.comment("... rank B.").defineInRange("rankEpB", BeastWillRules.DEFAULT_RANK_EP[1], 0.0, 1e12);
        RANK_A = b.comment("... rank A.").defineInRange("rankEpA", BeastWillRules.DEFAULT_RANK_EP[2], 0.0, 1e12);
        RANK_S = b.comment("... rank S.").defineInRange("rankEpS", BeastWillRules.DEFAULT_RANK_EP[3], 0.0, 1e12);
        RANK_SS = b.comment("... rank SS.").defineInRange("rankEpSS", BeastWillRules.DEFAULT_RANK_EP[4], 0.0, 1e12);
        b.pop();
        b.push("hosts");
        OTHER_RACES = b.comment("Races that a will does not list (anything but human, elf and dwarf by default) may still host it, slower and with more strain. false = only the listed races can.").define("otherRacesMayHost", true);
        OTHER_RACE_SCALE = b.comment("Multiplier on how well another race fits (the will's own otherHostCompat), kept between 0.2 and 1.").defineInRange("otherRaceCompatScale", 1.0, 0.0, 2.0);
        EXTRA_HOST_RACES = b.comment("Race names (HUMAN, ELF, DWARF, BEASTFOLK, OGRE, GOBLIN, GIANT, ORC, DRAGONEWT, MERFOLK, SLIME, VAMPIRE, UNDEAD, DAEMON, DEVIL) that count as full hosts for every will.")
                .defineListAllowEmpty("extraHostRaces", List.of(), () -> "BEASTFOLK", o -> o instanceof String);
        b.pop();
        b.push("progress");
        ASSIM_RATE = b.comment("Multiplier on how fast assimilation grows (the daily cap of each will still applies).").defineInRange("assimilationRate", 1.0, 0.0, 4.0);
        MASTERY_RATE = b.comment("Multiplier on how fast mastery grows (the daily cap still applies).").defineInRange("masteryRate", 1.0, 0.0, 4.0);
        MEDITATION = b.comment("Sneaking still for a few seconds with the will off meditates: strain falls three times as fast and a little assimilation is gained (inside the daily cap).").define("meditation", true);
        SWAP_LOCK_MINUTES = b.comment("Minutes before the will can be swapped for another one after a swap (strain and cooldowns carry over either way).").defineInRange("swapLockMinutes", 10, 0, 120);
        b.pop();
        b.push("strain");
        STRAIN_RATE = b.comment("Multiplier on every strain gain. 0 = no strain at all.").defineInRange("strainRate", 1.0, 0.0, 4.0);
        REST_PER_SECOND = b.comment("Strain shed per second while the will is off.").defineInRange("restPerSecond", 0.5, 0.0, 5.0);
        PENALTIES = b.comment("Strain has effects: weaker bonuses and abilities, weakness when exhausted, a little damage (never lethal) in overload. false = the strain bar is only shown.").define("penalties", true);
        OVERLOAD_DAMAGE = b.comment("Health lost once when overload ends the form (times two; scaled by this). It never takes the last hearts: the damage stops below 4 health.").defineInRange("overloadDamage", 1.0, 0.0, 4.0);
        REJECTION = b.comment("At full strain the will rejects its host: the form ends and the will goes quiet for a while, and a little assimilation is lost. Never death, never a lost skill or level. false = strain stops at overload.").define("rejection", true);
        REJECT_LOCK_MINUTES = b.comment("Minutes the will stays quiet after a rejection.").defineInRange("rejectLockMinutes", 10, 1, 120);
        REJECT_ASSIM_LOSS = b.comment("Share of the whole assimilation lost on a rejection (at most half; it can be won back).").defineInRange("rejectAssimilationLoss", 0.10, 0.0, 0.5);
        b.pop();
        b.push("balance");
        STAT_SCALE = b.comment("Multiplier on every attribute bonus of every will (each attribute also has a hard cap in the rules).").defineInRange("statScale", 1.0, 0.0, 2.0);
        DRAIN_SCALE = b.comment("Multiplier on the energy every form and ability drains.").defineInRange("drainScale", 1.0, 0.0, 4.0);
        COOLDOWN_SCALE = b.comment("Multiplier on every ability cooldown (kept between half and double of what the file says).").defineInRange("cooldownScale", 1.0, 0.5, 2.0);
        MAX_TARGETS_CAP = b.comment("The most creatures any one ability may affect, whatever the will's file says.").defineInRange("maxTargetsCap", 10, 1, 10);
        b.pop();
        b.push("rules");
        EXCLUSIVE_INTEGRATE = b.comment("The second phase (Integrate) cannot be held together with another transformation of this mod (a spirit dive, a mode armour, a devil form). The first phase stacks, its bonuses capped.").define("exclusiveIntegrate", true);
        HIT_PLAYERS = b.comment("Abilities may hit other players (they must still be foes: the server's PvP setting, teams and allies are respected). false = creatures only.").define("abilitiesHitPlayers", true);
        STEALTH = b.comment("The Shadow Panther's stealth lowers how far creatures notice the player. false = no stealth at all.").define("stealth", true);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private BeastWillConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return BeastWillRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return (int) BeastWillRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean enabled() { return on(ENABLED, true); }
    public static double dropScale() { return num(DROP_SCALE, 1.0, 0, 5); }
    public static int minVictimAge() { return whole(MIN_VICTIM_AGE, 1200, 0, 72000); }
    public static BeastRank minDropRank() {
        try {
            BeastRank r = BeastRank.byName(MIN_DROP_RANK.get());
            return r != null && r.atLeast(BeastRank.A) ? r : BeastRank.A;
        } catch (RuntimeException notLoaded) { return BeastRank.A; }
    }
    public static double[] rankEp() {
        try { return new double[] {RANK_C.get(), RANK_B.get(), RANK_A.get(), RANK_S.get(), RANK_SS.get()}; } catch (RuntimeException notLoaded) { return BeastWillRules.DEFAULT_RANK_EP; }
    }
    public static boolean otherRacesMayHost() { return on(OTHER_RACES, true); }
    public static double otherRaceScale() { return num(OTHER_RACE_SCALE, 1.0, 0, 2); }
    public static boolean extraHost(String race) {
        try { for (Object o : EXTRA_HOST_RACES.get()) if (String.valueOf(o).equalsIgnoreCase(race)) return true; } catch (RuntimeException notLoaded) { /* default: none */ }
        return false;
    }
    public static double assimilationRate() { return num(ASSIM_RATE, 1.0, 0, 4); }
    public static double masteryRate() { return num(MASTERY_RATE, 1.0, 0, 4); }
    public static boolean meditation() { return on(MEDITATION, true); }
    public static int swapLockTicks() { return whole(SWAP_LOCK_MINUTES, 10, 0, 120) * 1200; }
    public static double strainRate() { return num(STRAIN_RATE, 1.0, 0, 4); }
    public static double restPerSecond() { return num(REST_PER_SECOND, 0.5, 0, 5); }
    public static boolean penalties() { return on(PENALTIES, true); }
    public static double overloadDamage() { return num(OVERLOAD_DAMAGE, 1.0, 0, 4); }
    public static boolean rejection() { return on(REJECTION, true); }
    public static int rejectLockTicks() { return whole(REJECT_LOCK_MINUTES, 10, 1, 120) * 1200; }
    public static double rejectAssimLoss() { return num(REJECT_ASSIM_LOSS, 0.10, 0, 0.5); }
    public static double statScale() { return num(STAT_SCALE, 1.0, 0, 2); }
    public static double drainScale() { return num(DRAIN_SCALE, 1.0, 0, 4); }
    public static double cooldownScale() { return num(COOLDOWN_SCALE, 1.0, 0.5, 2); }
    public static int maxTargetsCap() { return whole(MAX_TARGETS_CAP, 10, 1, 10); }
    public static boolean exclusiveIntegrate() { return on(EXCLUSIVE_INTEGRATE, true); }
    public static boolean hitPlayers() { return on(HIT_PLAYERS, true); }
    public static boolean stealth() { return on(STEALTH, true); }
}
