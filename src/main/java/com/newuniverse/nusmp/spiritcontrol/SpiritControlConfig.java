package com.newuniverse.nusmp.spiritcontrol;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the spirit control plane applies ({@code magic.spiritControl}, default true: the grimoire's spirit pages and the {@code /multiverse spirit} command; false hides the pages,
 * refuses the commands and stops combat mode; the spirits themselves are untouched, but note that their old keys are gone for good) and every number of combat mode. These change gameplay, so they are SERVER
 * settings. File: nusmp-spirit-control-server.toml (registered by {@link SpiritControlBoot}). Every number has hard bounds so a config cannot make combat mode unbounded.
 */
public final class SpiritControlConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CONTROL, COMBAT, SALAMANDER, SYLPH, UNDINE, GNOME, LIGHT, DARK, SPACE, LORD;
    public static final ModConfigSpec.DoubleValue USER_SHARE, DAMAGE_SCALE, MAX_DAMAGE, UNIT_PERCENT, USER_FLOOR, RANGE_SCALE, RESERVE, MANA_BASE, MANA_PER_TRUST, REGEN_BASE, REGEN_PER_TRUST;
    public static final ModConfigSpec.IntValue DECISION_TICKS, CAST_GAP, SERVER_CASTS_PER_SECOND, DECISIONS_PER_TICK, IDLE_SCAN_TICKS, TARGET_CAP, ASK_GAP, FIGHT_WINDOW;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        CONTROL = b.comment("The spirit control plane: spirit pages at the end of the grimoire (Summon or Release, Combat Mode, the spirit's own abilities) and the /multiverse spirit commands.",
                "false = the pages are hidden, the commands refuse and combat mode stops; the spirits themselves keep working. (The old spirit keybindings were removed in 3.0.0 and do not come back with this switch.)")
                .define("spiritControl", true);
        b.pop();
        b.push("spirits");
        SALAMANDER = b.comment("The Salamander (Fuegoleon's Fire grimoire) answers the control plane.").define("salamander", true);
        SYLPH = b.comment("The Sylph (Yuno's Wind grimoire) answers the control plane.").define("sylph", true);
        UNDINE = b.comment("Undine (Noelle's Water grimoire) answers the control plane.").define("undine", true);
        GNOME = b.comment("The Gnome (Earth Magic) answers the control plane.").define("gnome", true);
        LIGHT = b.comment("The Spirit of Light (Light Magic) answers the control plane.").define("spiritLight", true);
        DARK = b.comment("The Spirit of Darkness (Dark Magic) answers the control plane.").define("spiritDark", true);
        SPACE = b.comment("The Spirit of Space (Spatial Magic) answers the control plane.").define("spiritSpace", true);
        LORD = b.comment("The Spirit Lord (the orb of the Spirit Lord skill and the true bodies of the Spirit Charm) answers the control plane.").define("lord", true);
        b.pop();
        b.push("control");
        ASK_GAP = b.comment("Ticks a player must wait between two summon, release, combat or ability asks (from a page or the command).").defineInRange("askGapTicks", 10, 1, 200);
        b.pop();
        b.push("combat");
        COMBAT = b.comment("Combat mode: a spirit that is out casts spells at the owner's hostile target, paid by its own mana AND the owner's magicules. false = the toggle refuses.").define("enabled", true);
        USER_SHARE = b.comment("The share of every cast's price that the OWNER pays (0.5 = half and half with the spirit's own mana).").defineInRange("userManaShare", 0.5, 0.0, 1.0);
        DECISION_TICKS = b.comment("Ticks between two decisions of one player (a decision runs at most one target search and casts at most one spell).").defineInRange("decisionTicks", 10, 5, 100);
        CAST_GAP = b.comment("Fewest ticks between two casts of one player's spirits (the per second ceiling per spirit).").defineInRange("minCastGapTicks", 30, 10, 400);
        SERVER_CASTS_PER_SECOND = b.comment("Most spirit casts on the whole server in any second.").defineInRange("maxCastsPerSecondServer", 24, 1, 200);
        DECISIONS_PER_TICK = b.comment("Most combat decisions the whole server makes in one tick.").defineInRange("maxDecisionsPerTick", 8, 1, 64);
        IDLE_SCAN_TICKS = b.comment("When the owner is not in a fight, the search for a hostile target runs at most this often.").defineInRange("idleScanTicks", 40, 10, 400);
        FIGHT_WINDOW = b.comment("Ticks after the owner hit or was hit by a mob during which the spirit searches every decision.").defineInRange("fightWindowTicks", 200, 20, 1200);
        TARGET_CAP = b.comment("Most candidates looked at in one target search.").defineInRange("targetCap", 12, 1, 32);
        RANGE_SCALE = b.comment("Multiplies the range of every spell (the search radius follows).").defineInRange("rangeScale", 1.0, 0.25, 2.0);
        DAMAGE_SCALE = b.comment("Multiplies the damage of every combat spell.").defineInRange("damageScale", 1.0, 0.0, 5.0);
        MAX_DAMAGE = b.comment("The most damage one cast may deal, whatever the scaling.").defineInRange("maxDamagePerCast", 60.0, 1.0, 1000.0);
        RESERVE = b.comment("Percent of the owner's maximum magicule the spirit never spends: below this the spirit waits (it never empties the owner).").defineInRange("userReservePercent", 10.0, 0.0, 50.0);
        UNIT_PERCENT = b.comment("Percent of the owner's maximum magicule one mana unit of the owner's share is worth.").defineInRange("unitUserPercent", 0.25, 0.0, 5.0);
        USER_FLOOR = b.comment("The least magicules the owner's share of one cast costs.").defineInRange("userFloor", 15.0, 0.0, 10000.0);
        MANA_BASE = b.comment("A spirit's own mana pool at no bond.").defineInRange("spiritManaBase", 60.0, 1.0, 10000.0);
        MANA_PER_TRUST = b.comment("Pool added per point of trust (0 to 100).").defineInRange("spiritManaPerTrust", 0.6, 0.0, 100.0);
        REGEN_BASE = b.comment("A spirit's own mana regeneration per second at no bond.").defineInRange("spiritRegenBase", 2.0, 0.0, 1000.0);
        REGEN_PER_TRUST = b.comment("Regeneration per second added per point of trust.").defineInRange("spiritRegenPerTrust", 0.05, 0.0, 100.0);
        b.pop();
        SPEC = b.build();
    }

    private SpiritControlConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(CONTROL); }
    public static boolean combat() { return get(CONTROL) && get(COMBAT); }
    public static double userShare() { return ManaSplit.clampShare(get(USER_SHARE)); }
    public static int decisionTicks() { return get(DECISION_TICKS); }
    public static int castGap() { return get(CAST_GAP); }
    public static int serverCastsPerSecond() { return get(SERVER_CASTS_PER_SECOND); }
    public static int decisionsPerTick() { return get(DECISIONS_PER_TICK); }
    public static int idleScan() { return get(IDLE_SCAN_TICKS); }
    public static int fightWindow() { return get(FIGHT_WINDOW); }
    public static int targetCap() { return get(TARGET_CAP); }
    public static int askGap() { return get(ASK_GAP); }
    public static double rangeScale() { return get(RANGE_SCALE); }
    public static double damageScale() { return get(DAMAGE_SCALE); }
    public static float maxDamage() { return get(MAX_DAMAGE).floatValue(); }
    public static double unitPercent() { return get(UNIT_PERCENT); }
    public static double reservePercent() { return get(RESERVE); }
    public static double userFloor() { return get(USER_FLOOR); }
    public static double manaMax(int trust) { return SpiritMana.maxFor(get(MANA_BASE), get(MANA_PER_TRUST), trust); }
    public static double manaRegen(int trust) { return SpiritMana.regenFor(get(REGEN_BASE), get(REGEN_PER_TRUST), trust); }

    /** Is this spirit switched on? An unknown id reads as on. */
    public static boolean spiritOn(String id) {
        if (id == null) return true;
        return switch (id) {
            case "salamander" -> get(SALAMANDER);
            case "sylph" -> get(SYLPH);
            case "undine" -> get(UNDINE);
            case "gnome" -> get(GNOME);
            case "spirit_light" -> get(LIGHT);
            case "spirit_dark" -> get(DARK);
            case "spirit_space" -> get(SPACE);
            case "lord" -> get(LORD);
            default -> true;
        };
    }
}
