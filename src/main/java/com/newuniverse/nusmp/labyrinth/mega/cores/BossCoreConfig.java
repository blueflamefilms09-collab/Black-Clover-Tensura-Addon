package com.newuniverse.nusmp.labyrinth.mega.cores;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the Boss Cores system is the server owner's to change, and all of them change gameplay, so they are SERVER settings: {@code nusmp-boss-cores-server.toml} (registered by
 * {@link BossCoreBoot}); each is clamped by the spec. A server config can be read before it is loaded (the creative menu and the item tooltips ask for attribute modifiers on the client main menu), so
 * every read goes through {@link #num}/{@link #flag}, which answer the default instead of throwing.
 */
public final class BossCoreConfig {
    private BossCoreConfig() {}

    public static final ModConfigSpec SPEC;
    private static final Map<Object, Object> DEFAULTS = new IdentityHashMap<>();

    private static final ModConfigSpec.BooleanValue ENABLED, AEGIS_OTHER_MODS, NIHILITY_CLEANSE, EP_FROM_PLAYERS;
    private static final ModConfigSpec.IntValue MIN_FLOOR, BAND2, BAND3, BAND4, COUNT1, COUNT2, COUNT3, COUNT4, MAX_TIERS, EXTRA_HITS;
    private static final ModConfigSpec.DoubleValue AEGIS_HEALTH, AEGIS_PER, AEGIS_SET2, AEGIS_SET4, AEGIS_CAP, STRIKER_ATTACK, STRIKER_SET2, STRIKER_SHARE,
            REGEN_PER, REGEN_CAP, TIER2, TIER4, KI_SAIYAN, NIHILITY_COOLDOWN, EP_SHARE, EP_CAP;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_TYPES;

    private static ModConfigSpec.BooleanValue bool(ModConfigSpec.Builder b, String name, boolean def, String comment) {
        ModConfigSpec.BooleanValue v = b.comment(comment).define(name, def);
        DEFAULTS.put(v, def);
        return v;
    }

    private static ModConfigSpec.IntValue integer(ModConfigSpec.Builder b, String name, int def, int min, int max, String comment) {
        ModConfigSpec.IntValue v = b.comment(comment).defineInRange(name, def, min, max);
        DEFAULTS.put(v, def);
        return v;
    }

    private static ModConfigSpec.DoubleValue real(ModConfigSpec.Builder b, String name, double def, double min, double max, String comment) {
        ModConfigSpec.DoubleValue v = b.comment(comment).defineInRange(name, def, min, max);
        DEFAULTS.put(v, def);
        return v;
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossCores");
        ENABLED = bool(b, "enabled", true, "The Boss Cores system (docs/boss_cores.md). false = Decennial bosses pay no core, the armour keeps its plain armour points and every set effect is off.");
        b.push("pay");
        MIN_FLOOR = integer(b, "minFloor", 250, 10, 1000, "Only a Decennial boss standing on this floor or deeper (either side, |floor|) pays cores.");
        BAND2 = integer(b, "band2Floor", 500, 10, 5000, "From this floor the second count applies.");
        BAND3 = integer(b, "band3Floor", 750, 10, 5000, "From this floor the third count applies.");
        BAND4 = integer(b, "band4Floor", 1000, 10, 5000, "From this floor the fourth count applies.");
        COUNT1 = integer(b, "count1", 2, 0, 64, "Cores a boss pays from minFloor up to band2Floor.");
        COUNT2 = integer(b, "count2", 3, 0, 64, "Cores a boss pays from band2Floor up to band3Floor.");
        COUNT3 = integer(b, "count3", 4, 0, 64, "Cores a boss pays from band3Floor up to band4Floor.");
        COUNT4 = integer(b, "count4", 5, 0, 64, "Cores a boss pays from band4Floor.");
        b.pop();
        b.push("aegis");
        AEGIS_HEALTH = real(b, "maxHealthPerPiece", 4.0, 0.0, 40.0, "Maximum health (half hearts) each Aegis piece adds.");
        AEGIS_PER = real(b, "resistPercentPerPiece", 5.0, 0.0, 25.0, "Percent less damage from damage types of other mods, per Aegis piece.");
        AEGIS_SET2 = real(b, "setBonus2Percent", 3.0, 0.0, 50.0, "Extra percent of that reduction with two pieces of one Aegis set.");
        AEGIS_SET4 = real(b, "setBonus4Percent", 7.0, 0.0, 50.0, "Extra percent of that reduction with all four pieces (replaces the two piece bonus).");
        AEGIS_CAP = real(b, "resistCapPercent", 60.0, 0.0, 90.0, "The most the Aegis ever takes off one hit.");
        AEGIS_OTHER_MODS = bool(b, "resistOtherModTypes", true, "Every damage type whose registry namespace is not minecraft counts (Tensura, Tensura No Index with its Coronzon type, this mod, ...).");
        EXTRA_TYPES = b.comment("Damage type ids that also count, e.g. [\"tensuranoindex:coronzon\"] (write the exact id; a type that does not exist is simply never met). Types outside minecraft already count by default.")
                .defineListAllowEmpty("extraResistedTypes", List.of(), () -> "", o -> o instanceof String);
        b.pop();
        b.push("striker");
        STRIKER_ATTACK = real(b, "attackDamagePerPiece", 1.5, 0.0, 20.0, "Attack damage each Striker piece adds (any held weapon).");
        STRIKER_SET2 = real(b, "setBonus2Percent", 8.0, 0.0, 100.0, "Percent more damage on the wearer's hits with two pieces.");
        STRIKER_SHARE = real(b, "secondTypePercent", 10.0, 0.0, 100.0, "With all four pieces: this share of the hit is dealt again as magic damage that bypasses armour.");
        EXTRA_HITS = integer(b, "maxSecondTypeHitsPerTick", 3, 1, 10, "The most second-type hits one player can cause in one tick (area weapons).");
        b.pop();
        b.push("wellspring");
        REGEN_PER = real(b, "regenPercentPerPiecePerSecond", 0.5, 0.0, 10.0, "Percent of the maximum of the pool (magicule, aura, Ki) each piece gives back every second.");
        REGEN_CAP = real(b, "regenCapPercentPerSecond", 6.0, 0.0, 50.0, "The most one set gives back in one second.");
        TIER2 = real(b, "setFactor2", 1.5, 1.0, 5.0, "Factor on the regeneration (and the Ascendant EP share) with two pieces.");
        TIER4 = real(b, "setFactor4", 2.0, 1.0, 10.0, "Factor with all four pieces.");
        KI_SAIYAN = real(b, "kiSaiyanFactor", 1.5, 1.0, 5.0, "Ki is the magicule in this mod; a Saiyan gets this factor more from the Ki Wellspring.");
        NIHILITY_COOLDOWN = real(b, "nihilityCooldownPercentPerPiece", 6.0, 0.0, 50.0, "Nihility Wellspring: percent each active skill's cooldown falls extra every second, per piece.");
        NIHILITY_CLEANSE = bool(b, "nihilityCleanseAtFour", true, "Nihility Wellspring with all four pieces: one harmful effect is wiped every second.");
        b.pop();
        b.push("ascendant");
        EP_SHARE = real(b, "epSharePercentPerPiece", 0.5, 0.0, 10.0, "Percent of a slain creature's EP added to the wearer's base EP, per piece.");
        EP_CAP = real(b, "epCapPercentOfBase", 0.5, 0.0, 5.0, "One kill never adds more than this percent of the wearer's own base EP.");
        EP_FROM_PLAYERS = bool(b, "epFromPlayers", false, "true = killing a player also pays the EP share (off: it would be a farm).");
        b.pop();
        b.push("mixing");
        MAX_TIERS = integer(b, "maxSetBonusTiers", 2, 1, 4, "Set bonuses of different sets worn together share this budget (a two piece bonus costs 1, a four piece bonus 2), so mixing never beats a full set.");
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    // ---------------------------------------------------------------- safe reads
    private static double num(ModConfigSpec.DoubleValue v) {
        try { return v.get(); } catch (RuntimeException notLoaded) { return (Double) DEFAULTS.get(v); }
    }

    private static int num(ModConfigSpec.IntValue v) {
        try { return v.get(); } catch (RuntimeException notLoaded) { return (Integer) DEFAULTS.get(v); }
    }

    private static boolean flag(ModConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (RuntimeException notLoaded) { return (Boolean) DEFAULTS.get(v); }
    }

    public static boolean enabled() { return flag(ENABLED); }

    public static int minFloor() { return num(MIN_FLOOR); }

    /** The cores a boss on this floor pays (the pure rule over the configured bands). */
    public static int coresFor(int floor) { return BossCoreRules.count(floor, num(MIN_FLOOR), num(BAND2), num(BAND3), num(BAND4), num(COUNT1), num(COUNT2), num(COUNT3), num(COUNT4)); }

    public static double aegisHealth() { return num(AEGIS_HEALTH); }

    public static double aegisPer() { return num(AEGIS_PER); }

    public static double aegisSet2() { return num(AEGIS_SET2); }

    public static double aegisSet4() { return num(AEGIS_SET4); }

    public static double aegisCap() { return num(AEGIS_CAP); }

    public static boolean aegisOtherMods() { return flag(AEGIS_OTHER_MODS); }

    /** Is this damage type id on the owner's list? */
    public static boolean extraResisted(String id) {
        try { return EXTRA_TYPES.get().contains(id); } catch (RuntimeException notLoaded) { return false; }
    }

    public static double strikerAttack() { return num(STRIKER_ATTACK); }

    public static double strikerSet2() { return num(STRIKER_SET2); }

    public static double strikerShare() { return num(STRIKER_SHARE); }

    public static int extraHitsPerTick() { return num(EXTRA_HITS); }

    public static double regenPer() { return num(REGEN_PER); }

    public static double regenCap() { return num(REGEN_CAP); }

    public static double tier2() { return num(TIER2); }

    public static double tier4() { return num(TIER4); }

    public static double kiSaiyan() { return num(KI_SAIYAN); }

    public static double nihilityCooldown() { return num(NIHILITY_COOLDOWN); }

    public static boolean nihilityCleanse() { return flag(NIHILITY_CLEANSE); }

    public static double epShare() { return num(EP_SHARE); }

    public static double epCap() { return num(EP_CAP); }

    public static boolean epFromPlayers() { return flag(EP_FROM_PLAYERS); }

    public static int maxTiers() { return num(MAX_TIERS); }
}
