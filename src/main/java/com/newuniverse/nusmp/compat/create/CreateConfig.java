package com.newuniverse.nusmp.compat.create;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: server config of the Create compatibility, {@code world/serverconfig/nusmp-labyrinth-create-compat-server.toml}, section {@code [create]}. Its own file (the pattern of
 * {@code SafeConfig} and {@code MapsConfig}) so a server owner who finds the machinery too thick, too hard or simply unwanted changes one file. Only plain {@code define} /
 * {@code defineInRange}; every reader goes through {@link #get}, which answers the default while the spec is not loaded yet. Nothing here has an effect unless the Create mod
 * is installed.
 */
public final class CreateConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, REFUSE_CONTRAPTIONS, SAW_RUN, PRESS, DRILL_WALL, TREADMILL, POUR, SEQUENCE;
    public static final ModConfigSpec.DoubleValue DENSITY, DAMAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("create");
        ENABLED = b.comment("Master switch. When Create is installed, the Mega-Dungeon arms Create traps (saws, presses, drills, belts, spouts, nixie sequences) in corridors. false = the dungeon is as it was without Create. Traps already built stay in the walls but stop (no hurt, no hints).")
                .define("enabled", true);
        DENSITY = b.comment("Multiplier of how many traps a sector gets. 1 = a few per sector (at most 7% of its rooms, only in corridors and a few other room kinds, never next to a Safe Zone), 0 = none are armed from now on, 4 = four times as many (a cap of 24 per sector still holds). Applies to sectors generated or planned from now on.")
                .defineInRange("densityMultiplier", 1.0, 0.0, 4.0);
        DAMAGE = b.comment("Multiplier of the damage of a hit. A hit never takes more than 35% of a player's health, a run of one trap never more than 70%, and a trap never hurts a player who is below 20% (it only throws them): a trap does not kill.")
                .defineInRange("damageMultiplier", 1.0, 0.0, 3.0);
        SAW_RUN = b.comment("Family: mechanical saws in the corridor walls (from floor 1).").define("sawRun", true);
        PRESS = b.comment("Family: mechanical pistons and the rams that close on the lane (from floor 4).").define("press", true);
        DRILL_WALL = b.comment("Family: mechanical drills fed by gearboxes (from floor 7).").define("drillWall", true);
        TREADMILL = b.comment("Family: a conveyor belt that carries you towards a press (from floor 9).").define("treadmill", true);
        POUR = b.comment("Family: spouts under a fluid pipe pouring molten metal (from floor 3).").define("pour", true);
        SEQUENCE = b.comment("Family: nixie tubes that light in order, and the discharge that follows (from floor 12).").define("sequence", true);
        REFUSE_CONTRAPTIONS = b.comment("Create contraptions (pistons, bearings, carriages, gantries) may not assemble from, join or carry blocks of the Dungeon: they cannot move or break its protected stone. Leave on; turning it off lets a contraption duplicate dungeon blocks.")
                .define("refuseContraptions", true);
        b.pop();
        SPEC = b.build();
    }

    private CreateConfig() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The families the server has switched on. */
    public static Set<TrapFamily> families() {
        Set<TrapFamily> out = EnumSet.noneOf(TrapFamily.class);
        if (get(SAW_RUN)) out.add(TrapFamily.SAW_RUN);
        if (get(PRESS)) out.add(TrapFamily.PRESS);
        if (get(DRILL_WALL)) out.add(TrapFamily.DRILL_WALL);
        if (get(TREADMILL)) out.add(TrapFamily.TREADMILL);
        if (get(POUR)) out.add(TrapFamily.POUR);
        if (get(SEQUENCE)) out.add(TrapFamily.SEQUENCE);
        return out;
    }
}
