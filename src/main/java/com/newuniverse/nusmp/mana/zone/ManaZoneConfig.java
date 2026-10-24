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
    // the Mana Zone upgrade (docs/magic/mana_zone_upgrade.md): every switch off = the 1.99.0 zone
    public static final ModConfigSpec.BooleanValue UP_EVASION, UP_BUFFS, UP_CAST, UP_RELOCATE;
    public static final ModConfigSpec.DoubleValue UP_EVASION_CHANCE, UP_EVASION_COST, UP_ATTACK, UP_KNOCKBACK;
    public static final ModConfigSpec.IntValue UP_EVASION_COOLDOWN, UP_CAST_GAP;
    public static final ModConfigSpec.EnumValue<ZoneUpRules.OriginMode> UP_ORIGIN;

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
        b.push("upgrade");
        UP_EVASION = b.comment("Evasion: a zone owner sidesteps a projectile that is about to hit and the hit is dodged (server decides; chance, cooldown and a little mana). false = the 1.99.0 zone.")
                .define("evasion", true);
        UP_EVASION_CHANCE = b.comment("Base chance (0..0.95) that a threatening projectile is dodged; up to +25% with Reinforcement mastery, halved against a boss's projectile.")
                .defineInRange("evasionChance", ZoneUpRules.DEFAULT.evasionChance(), 0.0, 0.95);
        UP_EVASION_COOLDOWN = b.comment("Ticks between two dodges of one owner.").defineInRange("evasionCooldownTicks", ZoneUpRules.DEFAULT.evasionCooldown(), 10, 1200);
        UP_EVASION_COST = b.comment("Percent of the owner's maximum mana one dodge costs (a free zone and creative pay nothing).")
                .defineInRange("evasionCostPercent", ZoneUpRules.DEFAULT.evasionCostPct(), 0.0, 5.0);
        UP_BUFFS = b.comment("The zone also gives an attack-damage bonus and knockback resistance (the speed bonus is always on). false = the 1.99.0 zone.").define("buffs", true);
        UP_ATTACK = b.comment("Attack damage bonus as a fraction of the base (0.15 = +15%).").defineInRange("attackBonus", ZoneUpRules.DEFAULT.attackBonus(), 0.0, 0.5);
        UP_KNOCKBACK = b.comment("Knockback resistance added while the zone is open (1 = immune).").defineInRange("knockbackResistance", ZoneUpRules.DEFAULT.knockbackResist(), 0.0, 1.0);
        UP_CAST = b.comment("Omnidirectional casting: while the zone is open, a grimoire page is cast AT the point (or foe) under the caster's crosshair inside the zone, born behind it or at the edge, "
                + "not at the hand. The client sends the aim, the server verifies it against its own zone. false = pages are cast from the hand as before.").define("zoneCast", true);
        UP_ORIGIN = b.comment("Where a zone-cast spell is born: BEHIND (past the target, as seen from the caster), EDGE (on the zone's boundary, flying inward) or ABOVE.")
                .defineEnum("zoneCastOrigin", ZoneUpRules.OriginMode.BEHIND);
        UP_RELOCATE = b.comment("While the page runs, the caster is posed at the spell's origin looking at the target (position, look and velocity restored right after), so every existing "
                + "page becomes omnidirectional. false = only the pages' aim / target / beam / effect helpers are redirected.").define("zoneCastRelocate", true);
        UP_CAST_GAP = b.comment("Ticks between two zone-casts of one caster.").defineInRange("zoneCastGapTicks", ZoneUpRules.DEFAULT.castGap(), 2, 200);
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

    /** The upgrade's numbers in force (the shipped defaults until the config is loaded). */
    public static ZoneUpRules.Rules upgrade() {
        ZoneUpRules.Rules d = ZoneUpRules.DEFAULT;
        try {
            return ZoneUpRules.sanitize(new ZoneUpRules.Rules(UP_EVASION.get(), UP_EVASION_CHANCE.get(), UP_EVASION_COOLDOWN.get(), UP_EVASION_COST.get(), d.evasionHorizon(), d.evasionLooks(),
                    d.evasionHitRadius(), d.evasionImpulse(), UP_BUFFS.get(), UP_ATTACK.get(), UP_KNOCKBACK.get(), UP_CAST.get(), UP_ORIGIN.get(), UP_RELOCATE.get(), UP_CAST_GAP.get(), d.aimGap(),
                    d.aimTtl(), d.castReach(), d.castMinRun(), d.tolerance()));
        } catch (RuntimeException notLoaded) {
            return d;
        }
    }
}
