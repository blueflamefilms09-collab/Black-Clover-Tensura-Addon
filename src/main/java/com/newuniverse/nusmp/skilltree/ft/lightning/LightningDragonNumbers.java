package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget.Kind;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTables;

/**
 * Why: the Lightning Dragon Slayer route keeps its tuning in {@code data/nusmp/skill_trees/lightning_dragon_slayer.json} so that balance is data, and this class is the safety net and the proof
 * under that data. It is the same numbers written once in code, in a PURE class (no Minecraft or NeoForge type): the skills fall back to it when the loaded route or one of
 * its keys is missing, so a cast never reads a hole; the unit test proves it equals the shipped JSON and obeys the budget of {@code DragonBudget}; and the node ids are
 * named once here so a skill, a passive and a test cannot disagree on a spelling. Kept as a separate class from the skills for exactly that reason: it can be tested, and
 * the tests run on the numbers the owner will actually fight with.
 */
public final class LightningDragonNumbers {
    private LightningDragonNumbers() {}

    public static final String ROUTE = "nusmp:lightning_dragon_slayer";

    // the node ids of the route file
    public static final String IRON_FIST = "iron_fist";
    public static final String ROAR = "roar";
    public static final String BREAKDOWN_FIST = "breakdown_fist";
    public static final String LIGHTNING_BODY = "lightning_body";
    public static final String HALBERD = "heavenward_halberd";
    public static final String THUNDER_PALACE = "thunder_palace";
    public static final String JAW = "jaw";
    public static final String LIGHTNING_FLASH = "lightning_flash";
    public static final String THUNDER_EATER = "thunder_eater";
    public static final String ROARING_THUNDER = "roaring_thunder";

    /** The shipped numbers: one table per node (key to the number at each level, level 1 first). Order and values are those of the JSON file. */
    public static final DragonTables TABLES = DragonTables.of(ROUTE)
            .node(IRON_FIST, Kind.ACTIVE, 5)
                .v("damage", 6, 7, 8, 9, 11)
                .v("numb_seconds", 1.0, 1.5, 1.5, 2.0, 2.5)
                .v("range", 3.5, 3.5, 4.0, 4.0, 4.5)
                .v("half_angle", 35, 35, 40, 40, 45)
                .v("targets", 1, 1, 1, 2, 2)
                .v("chain_targets", 0, 1, 1, 2, 2)
                .v("chain_range", 4.0, 4.0, 4.5, 4.5, 5.0)
                .v("chain_damage", 4, 4, 5, 5, 6)
                .v("knockback", 0.2, 0.2, 0.3, 0.3, 0.4)
                .v("cost_pct", 1, 1, 1, 1.5, 1.5)
                .v("cooldown", 100, 90, 80, 70, 60)
            .node(ROAR, Kind.ACTIVE, 3)
                .v("damage", 9, 11, 13)
                .v("range", 10, 12, 14)
                .v("half_angle", 12, 15, 18)
                .v("targets", 4, 6, 8)
                .v("numb_seconds", 1.5, 2.0, 2.5)
                .v("windup", 6, 5, 4)
                .v("cost_pct", 3, 3, 3.5)
                .v("cooldown", 160, 140, 120)
            .node(BREAKDOWN_FIST, Kind.ACTIVE, 3)
                .v("damage", 9, 11, 13)
                .v("range", 10, 12, 14)
                .v("width", 0.9, 1.0, 1.1)
                .v("stun_ticks", 20, 30, 40)
                .v("numb_seconds", 2.0, 2.5, 3.0)
                .v("windup", 6, 5, 4)
                .v("cost_pct", 2.5, 3, 3.5)
                .v("cooldown", 140, 120, 100)
            .node(LIGHTNING_BODY, Kind.PASSIVE, 3)
                .v("speed_amp", 0, 0, 1)
                .v("evade_pct", 8, 12, 16)
                .v("evade_icd", 60, 50, 40)
            .node(HALBERD, Kind.ACTIVE, 3)
                .v("damage", 10, 12, 14)
                .v("range", 18, 22, 26)
                .v("width", 0.8, 0.9, 1.0)
                .v("targets", 3, 4, 5)
                .v("numb_seconds", 1.0, 1.5, 2.0)
                .v("windup", 8, 7, 6)
                .v("cost_pct", 3, 3.5, 4)
                .v("cooldown", 180, 160, 140)
            .node(THUNDER_PALACE, Kind.ACTIVE, 3)
                .v("damage", 8, 9, 10)
                .v("range", 10, 12, 14)
                .v("radius", 4.0, 5.0, 6.0)
                .v("strikes", 4, 5, 6)
                .v("strike_radius", 1.8, 2.0, 2.2)
                .v("delay", 20, 18, 16)
                .v("gap", 13, 12, 11)
                .v("numb_seconds", 1.0, 1.5, 2.0)
                .v("targets", 6, 8, 10)
                .v("cost_pct", 3.5, 4, 4)
                .v("cooldown", 240, 210, 180)
            .node(JAW, Kind.ACTIVE, 3)
                .v("damage", 8, 10, 12)
                .v("radius", 3.0, 3.5, 4.0)
                .v("reach", 2.0, 2.5, 3.0)
                .v("targets", 5, 7, 9)
                .v("knockback", 0.5, 0.7, 0.9)
                .v("numb_seconds", 1.0, 1.5, 2.0)
                .v("cost_pct", 2, 2.5, 3)
                .v("cooldown", 150, 130, 110)
            .node(LIGHTNING_FLASH, Kind.ACTIVE, 3)
                .v("damage", 4, 5, 6)
                .v("dash", 6, 8, 10)
                .v("width", 1.0, 1.1, 1.2)
                .v("targets", 2, 3, 4)
                .v("numb_seconds", 1.0, 1.0, 1.5)
                .v("cost_pct", 2, 2, 2.5)
                .v("cooldown", 100, 90, 80)
            .node(THUNDER_EATER, Kind.PASSIVE, 3)
                .v("heal_pct", 2.0, 3.0, 4.0)
                .v("recharge_pct", 1.0, 1.5, 2.0)
                .v("eat_heal_ratio", 0.25, 0.4, 0.6)
                .v("eat_recharge_pct", 2, 3, 4)
            .node(ROARING_THUNDER, Kind.SECRET_ART, 3)
                .v("damage", 18, 24, 30)
                .v("splash_damage", 10, 12, 14)
                .v("rush_range", 10, 12, 14)
                .v("burst_radius", 3.5, 4.0, 4.5)
                .v("knockback", 1.4, 1.8, 2.2)
                .v("stun_ticks", 20, 30, 40)
                .v("windup", 8, 7, 6)
                .v("targets", 6, 9, 12)
                .v("cost_pct", 6, 7, 8)
                .v("cooldown", 1200, 1080, 900)
            .build();
}
