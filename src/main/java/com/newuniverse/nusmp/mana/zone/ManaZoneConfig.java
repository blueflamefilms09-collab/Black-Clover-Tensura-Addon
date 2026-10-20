package com.newuniverse.nusmp.mana.zone;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the Mana Zone and the Grand Magic Zones: {@code serverconfig/nusmp-mana-zone-server.toml}. The numbers of the technique itself live
 * in {@code data/nusmp/mana_zone.json} (datapack-overridable); this file holds the few switches an owner may want to turn without a datapack. The gate
 * strictness can only make the gate stricter than the shipped numbers or a little easier (never under the floors of {@link ManaZoneGate}).
 */
public final class ManaZoneConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue GATE_STRICTNESS, MAX_RADIUS, HARM_SCALE;
    public static final ModConfigSpec.BooleanValue GRAND_ZONES_ENABLED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("manaZone");
        GATE_STRICTNESS = b.comment("Multiplier on the EP, boss, depth and attunement numbers of the Mana Zone gate (data/nusmp/mana_zone.json). 1 = as shipped; "
                + "0.5 is the easiest it can get (the floors of 1,000,000 EP, Senior 5th Class and 5 boss kills still hold); 4 makes it four times harder.")
                .defineInRange("gateStrictness", 1.0, ManaZoneGate.MIN_STRICTNESS, ManaZoneGate.MAX_STRICTNESS);
        MAX_RADIUS = b.comment("The largest radius (blocks) any Mana Zone may reach, whatever the caster's EP.").defineInRange("maxRadius", 40.0, 8.0, 64.0);
        b.pop();
        b.push("grandMagicZones");
        GRAND_ZONES_ENABLED = b.comment("Grand Magic Zone regions (data/<namespace>/grand_magic_zones/*.json) act on players and mobs inside them.").define("enabled", true);
        HARM_SCALE = b.comment("Scale of what a zone does to a player who is too weak for it (slowness, pressure, heat, bolts). 0 keeps only the visuals and the boons; 1 = as designed.")
                .defineInRange("harmScale", 1.0, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private ManaZoneConfig() {}

    public static double strictness() {
        try { return ManaZoneGate.clampStrictness(GATE_STRICTNESS.get()); } catch (RuntimeException notLoaded) { return 1.0; }
    }

    public static double maxRadius() {
        try { return MAX_RADIUS.get(); } catch (RuntimeException notLoaded) { return 40.0; }
    }

    public static boolean grandZones() {
        try { return GRAND_ZONES_ENABLED.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double harmScale() {
        try { return HARM_SCALE.get(); } catch (RuntimeException notLoaded) { return 1.0; }
    }
}
