package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner asked for the anti-dead-end generator behind a switch, {@code generation.cyclic}, ON for new sectors and with the old behaviour still there when
 * it is off. This is that switch and the knobs of the cyclic pass, in their own server config ({@code world/serverconfig/nusmp-labyrinth-mega-gen-server.toml},
 * section {@code [generation]}), following the pattern of the other Mega config classes: plain {@code define} / {@code defineInRange} only, every reader goes through
 * {@link #get} (which answers the default while the spec is not loaded yet), and the pure {@link LoopConfig} is built from the values so nothing below this class
 * knows about the config system. The switch is read ONCE per sector, when it is created ({@code Sector.cyclic}); the numbers are read each time a cyclic sector is
 * planned (a regeneration after the numbers changed plans with the new numbers, exactly as {@code mega.floorRooms} does).
 */
public final class CyclicSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CYCLIC, PRUNE;
    public static final ModConfigSpec.DoubleValue DEAD_END_TARGET, CYCLE_TARGET, HALLWAY_SHARE, BALANCE_WEIGHT, FILLER_SHARE;
    public static final ModConfigSpec.IntValue MAX_HALLWAY, MAX_LINKS, LOOKAHEAD;

    static {
        LoopConfig d = LoopConfig.defaults();
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("generation");
        CYCLIC = b.comment("Anti-dead-end cyclic generation (docs/labyrinth/cyclic_wfc.md). true = NEW sectors and floors are generated with loops: the placement step prefers rooms whose doorways line up with open doorways, loose doorways are joined by direct doorways or short transitional hallways, and dangling rooms that no loop can cure are trimmed, so at most deadEndTarget of the rooms are dead ends. false = the old generator (a tree that grows until the budget is spent). Read once when a sector is created and saved with it: sectors that already exist are never changed, and a regeneration keeps the generator the sector was created with.")
                .define("cyclic", true);
        DEAD_END_TARGET = b.comment("The most dead ends, as a share of all rooms, the cyclic generator tolerates (0.08 = 8 %). Entrance, stairs, boss chain, Safe Zones and the rooms the recipe requires are destinations, not dead ends.")
                .defineInRange("deadEndTarget", d.deadEndTarget(), 0.0, 0.5);
        CYCLE_TARGET = b.comment("After the dead ends are under the target, extra loops are added until this share of the rooms lies on a cycle (0 = only loops that cure a dead end).")
                .defineInRange("cycleTarget", d.cycleTarget(), 0.0, 1.0);
        MAX_HALLWAY = b.comment("Longest transitional hallway, in 9-block modules, that may join two loose doorways (0 = no hallways, only doorways that already line up).")
                .defineInRange("maxHallway", d.maxHallway(), 0, 6);
        HALLWAY_SHARE = b.comment("Hallway rooms may be at most this share of all rooms, so the Transition share of the dungeon is not swamped by them.")
                .defineInRange("hallwayShare", d.hallwayShare(), 0.0, 0.5);
        MAX_LINKS = b.comment("The most doorways / hallways the loop pass adds to one floor.")
                .defineInRange("maxLinks", d.maxLinks(), 0, 64);
        PRUNE = b.comment("May the generator remove a dangling room (a leaf that is not a destination and not required by the recipe) when no loop can cure it and the dead-end ratio is still over the target?")
                .define("prune", d.prune());
        FILLER_SHARE = b.comment("The ordinary filler places this share of a floor's room budget (mega.floorRooms); the hallways of the loop pass make up the rest, so the floor ends near the budget. 0.9 = 90 %.")
                .defineInRange("fillerShare", d.overgrow(), 0.5, 2.0);
        LOOKAHEAD = b.comment("How many different fitting templates the placement step compares for each doorway (1 = take the first that fits, the old behaviour). Higher finds more loops and keeps the room categories balanced, and costs a little generation time.")
                .defineInRange("lookahead", d.lookahead(), 1, 32);
        BALANCE_WEIGHT = b.comment("How strongly placement and trimming are nudged towards the target mix of the five room categories (Combat, Puzzles and Hazards, Lore, Transitions, Safe Rooms). 0 = not at all.")
                .defineInRange("balanceWeight", d.balanceWeight(), 0.0, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private CyclicSettings() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Should a sector created now be generated cyclic? */
    public static boolean enabled() { return get(CYCLIC); }

    /** The pure configuration of the pass, from the current values. */
    public static LoopConfig loopConfig() {
        return LoopConfig.defaults().withDeadEndTarget(get(DEAD_END_TARGET)).withCycleTarget(get(CYCLE_TARGET)).withMaxHallway(get(MAX_HALLWAY)).withHallwayShare(get(HALLWAY_SHARE))
                .withMaxLinks(get(MAX_LINKS)).withPrune(get(PRUNE)).withOvergrow(get(FILLER_SHARE)).withLookahead(get(LOOKAHEAD)).withBalanceWeight(get(BALANCE_WEIGHT));
    }

    /** Registers the spec as {@code nusmp-labyrinth-mega-gen-server.toml}; called once from {@code MegaGenRegistry.init} while the mod is being constructed. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-gen-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The cyclic generation config could not be registered ({}); the defaults apply (generation.cyclic = true)", e.toString());
        }
    }
}
