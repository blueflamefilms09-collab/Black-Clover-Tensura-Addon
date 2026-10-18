package com.newuniverse.nusmp.skilltree.ft.fire;

import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget.Kind;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTables;

/**
 * Why: the Fire Dragon Slayer route keeps its tuning in {@code data/nusmp/skill_trees/fire_dragon_slayer.json} so that balance is data, and this class is the safety net and the proof
 * under that data. It is the same numbers written once in code, in a PURE class (no Minecraft or NeoForge type): the skills fall back to it when the loaded route or one of
 * its keys is missing, so a cast never reads a hole; the unit test proves it equals the shipped JSON and obeys the budget of {@code DragonBudget}; and the node ids are
 * named once here so a skill, a passive and a test cannot disagree on a spelling. Kept as a separate class from the skills for exactly that reason: it can be tested, and
 * the tests run on the numbers the owner will actually fight with.
 */
public final class FireDragonNumbers {
    private FireDragonNumbers() {}

    public static final String ROUTE = "nusmp:fire_dragon_slayer";

    // the node ids of the route file
    public static final String IRON_FIST = "iron_fist";
    public static final String ROAR = "roar";
    public static final String CLAW = "claw";
    public static final String FLAME_EATER = "flame_eater";
    public static final String BRILLIANT_FLAME = "brilliant_flame";
    public static final String WING_ATTACK = "wing_attack";
    public static final String SWORD_HORN = "sword_horn";
    public static final String BLAZING_SCALES = "blazing_scales";
    public static final String CRIMSON_LOTUS = "crimson_lotus";

    /** The shipped numbers: one table per node (key to the number at each level, level 1 first). Order and values are those of the JSON file. */
    public static final DragonTables TABLES = DragonTables.of(ROUTE)
            .node(IRON_FIST, Kind.ACTIVE, 5)
                .v("damage", 6, 7, 8, 9, 11)
                .v("burn_seconds", 3, 3, 4, 4, 5)
                .v("range", 3.5, 3.5, 4.0, 4.0, 4.5)
                .v("half_angle", 35, 35, 40, 40, 45)
                .v("targets", 1, 1, 2, 2, 3)
                .v("knockback", 0.3, 0.3, 0.4, 0.5, 0.6)
                .v("cost_pct", 1, 1, 1, 1.5, 1.5)
                .v("cooldown", 100, 90, 80, 70, 60)
            .node(ROAR, Kind.ACTIVE, 3)
                .v("damage", 9, 11, 13)
                .v("range", 9, 11, 13)
                .v("width", 1.4, 1.8, 2.2)
                .v("burn_seconds", 4, 5, 6)
                .v("targets", 4, 6, 8)
                .v("windup", 8, 7, 6)
                .v("cost_pct", 3, 3, 3.5)
                .v("cooldown", 160, 140, 120)
            .node(CLAW, Kind.ACTIVE, 3)
                .v("damage", 7, 8, 10)
                .v("range", 3.2, 3.6, 4.0)
                .v("half_angle", 55, 60, 65)
                .v("targets", 3, 4, 5)
                .v("burn_seconds", 3, 4, 5)
                .v("launch", 0.5, 0.6, 0.7)
                .v("hop", 0.35, 0.4, 0.45)
                .v("cost_pct", 1.5, 1.5, 2)
                .v("cooldown", 120, 100, 80)
            .node(FLAME_EATER, Kind.PASSIVE, 3)
                .v("heal_pct", 2.0, 3.0, 4.0)
                .v("recharge_pct", 0.8, 1.4, 2.0)
                .v("radius", 1.5, 2.0, 2.5)
                .v("eat_heal_ratio", 0.25, 0.4, 0.6)
                .v("eat_recharge_pct", 2, 3, 4)
                .v("eats_lava", 0, 0, 1)
            .node(BRILLIANT_FLAME, Kind.ACTIVE, 3)
                .v("damage", 8, 10, 12)
                .v("radius", 3.0, 3.5, 4.0)
                .v("range", 20, 24, 28)
                .v("burn_seconds", 4, 5, 6)
                .v("targets", 6, 8, 10)
                .v("knockback", 0.6, 0.8, 1.0)
                .v("cost_pct", 3, 3.5, 4)
                .v("cooldown", 200, 180, 160)
            .node(WING_ATTACK, Kind.ACTIVE, 4)
                .v("damage", 8, 9, 10, 12)
                .v("range", 5.0, 5.5, 6.0, 7.0)
                .v("half_angle", 50, 55, 60, 65)
                .v("targets", 4, 6, 8, 10)
                .v("burn_seconds", 3, 4, 4, 5)
                .v("knockback", 0.9, 1.1, 1.3, 1.5)
                .v("cost_pct", 2, 2.5, 3, 3.5)
                .v("cooldown", 140, 125, 110, 100)
            .node(SWORD_HORN, Kind.ACTIVE, 3)
                .v("damage", 9, 11, 13)
                .v("dash", 5, 6, 7)
                .v("width", 1.1, 1.3, 1.5)
                .v("targets", 3, 4, 5)
                .v("burn_seconds", 4, 5, 6)
                .v("knockback", 0.8, 1.0, 1.2)
                .v("windup", 6, 5, 5)
                .v("cost_pct", 2.5, 3, 3.5)
                .v("cooldown", 150, 130, 110)
            .node(BLAZING_SCALES, Kind.PASSIVE, 3)
                .v("burn_seconds", 2, 3, 4)
                .v("recoil", 2, 3, 4)
                .v("icd", 40, 30, 20)
                .v("aura_radius", 1.5, 2.0, 2.5)
                .v("aura_burn_seconds", 2, 2, 3)
            .node(CRIMSON_LOTUS, Kind.SECRET_ART, 3)
                .v("hit_damage", 9, 11, 13)
                .v("hits", 3, 4, 5)
                .v("final_damage", 18, 24, 30)
                .v("range", 8, 9, 10)
                .v("width", 2.2, 2.6, 3.0)
                .v("final_radius", 4.5, 5.0, 5.5)
                .v("knockback", 1.0, 1.3, 1.6)
                .v("gap", 11, 11, 11)
                .v("burn_seconds", 6, 7, 8)
                .v("targets", 6, 9, 12)
                .v("cost_pct", 6, 7, 8)
                .v("cooldown", 1200, 1080, 900)
            .build();
}
