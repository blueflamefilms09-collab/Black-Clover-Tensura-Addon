package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.List;
import java.util.Optional;

/**
 * Why: the chest side of the standard gear is mostly WIRING (a codec registered on the mod bus, a JSON type, a condition, an entry in the global list of loot
 * modifiers, a loot table that carries its own id), and wiring is exactly what a unit test cannot see and what silently does nothing when one link is missing:
 * the chests would simply never hold a piece. So {@code gear.chest} (run by {@code /multiverse mega selftest}) drives the real thing. It builds a MADE-UP chest
 * table (a bread, one roll, a loot-table id of this test's own: it can never be a relic table and no other loot modifier of the mod has a reason to act on it)
 * and rolls it through the game's whole loot pipeline, so the modifier is found by its JSON, its {@code location_check} dimension condition is evaluated, and the
 * stack is added beside the table's own bread: in the Mega-Dungeon the number of pieces, and of each grade, must match the chest chances of the roller within six
 * standard deviations, never more than one per chest, the bread never replaced and never a relic; a treasure table of the same kind must match the treasure
 * chances. In the Overworld and, where it exists, the Sanctuary Hub the same rolls must yield NOTHING. Then the modifier is called directly with NO JSON
 * conditions, to prove the code's own gate: nothing in the Overworld, nothing for the two relic tables, a mob's table, a block's table or an unknown table in
 * the Mega-Dungeon, and with Luck the (higher) luck chances. Last, a chest opened by a fake player must not fail (the quest hook ignores fake players). A real
 * vault table is deliberately never rolled through the whole pipeline: other loot modifiers act on it and may touch the Order's ledger, and a self-test must not
 * burn relics. Statistics use one shared random source and six standard deviations, so the test is deterministic enough never to flake.
 */
public final class ChestSelfTests {
    private ChestSelfTests() {}

    private static final String PLAYER = "gear_chest_tester";
    private static final ResourceLocation STANDARD_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/gear_selftest");
    private static final ResourceLocation TREASURE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/gear_selftest_treasure");

    /** Registers the test; called from {@link GearSelfTests#register()}. */
    public static void register() {
        SelfTests.register("gear.chest", ChestSelfTests::chest);
    }

    /** What a batch of rolls found. */
    private static final class Tally {
        int rolls, pieces, maxPerRoll, breads, relics;
        final int[] byGrade = new int[GearGrade.values().length];

        void add(List<ItemStack> loot) {
            rolls++;
            int here = 0;
            for (ItemStack st : loot) {
                if (st.is(Items.BREAD)) breads++;
                GearInfo info = st.get(GearComponents.GEAR.get());
                if (info == null) continue;
                here++;
                byGrade[info.grade().ordinal()]++;
                if (GearSelfTests.carriesRelicMark(st)) relics++;
            }
            pieces += here;
            maxPerRoll = Math.max(maxPerRoll, here);
        }
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static ResourceLocation rl(String id) { return ResourceLocation.parse(id); }

    // ------------------------------------------------------------------------------------------------ the test
    private static SelfTests.Result chest(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        if (!GearConfig.get(GearConfig.ENABLED)) return ok("skipped: gear.enabled is false in the config");
        GearTables t = GearTablesReload.tables();
        if (!t.isUsable()) return fail("the gear tables are empty or unusable");
        Sector s = MegaData.get(server).sector(0);
        BlockPos at = s != null && !s.rooms.isEmpty() ? s.rooms.get(0).center() : BlockPos.ZERO.above(100);
        int floor = MegaDungeon.floorAt(mega, at);
        GearRates rates = GearConfig.chestRates();
        double factor = GearConfig.get(GearConfig.CHEST_TREASURE_FACTOR);

        String problem = classification(t.chests());
        if (problem != null) return fail(problem);

        RandomSource random = RandomSource.create(20_55_0L);
        LootTable standard = madeUp(STANDARD_ID), treasure = madeUp(TREASURE_ID);
        double[] pStd = ChestGear.chances(ChestRules.Kind.STANDARD, rates, factor, t, floor, 0);
        double[] pTrs = ChestGear.chances(ChestRules.Kind.TREASURE, rates, factor, t, floor, 0);
        boolean wired = GearRoller.total(pStd) > 0.0;

        // 1. the whole pipeline in the Mega-Dungeon: found by its JSON, condition passed, one stack added beside the table's own
        Tally a = viaTable(mega, at, standard, 5000, 0f, random, null);
        problem = verdict("standard chests", a, pStd, wired);
        if (problem != null) return fail(problem);
        Tally b = viaTable(mega, at, treasure, 5000, 0f, random, null);
        problem = verdict("treasure chests", b, pTrs, wired);
        if (problem != null) return fail(problem);

        // 2. the same rolls elsewhere give nothing: the JSON's dimension condition and the code's own gate
        ServerLevel overworld = server.overworld();
        BlockPos high = BlockPos.ZERO.above(70);
        Tally o = viaTable(overworld, high, standard, 1500, 0f, random, null);
        if (o.pieces != 0) return fail("a standard chest rolled in the Overworld gave " + o.pieces + " pieces of standard gear: the modifier acts outside the Mega-Dungeon");
        ServerLevel sanctuary = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
        if (sanctuary != null) {
            Tally h = viaTable(sanctuary, high, standard, 1500, 0f, random, null);
            if (h.pieces != 0) return fail("a standard chest rolled in the Sanctuary Hub gave " + h.pieces + " pieces: the Hub is not part of the Mega-Dungeon");
        }

        // 3. the modifier alone, with NO conditions in the JSON: the code's gate
        LootItemCondition[] none = new LootItemCondition[0];
        Tally d = direct(overworld, high, STANDARD_ID, 1500, 0f, random, none);
        if (d.pieces != 0) return fail("the modifier without JSON conditions added gear in the Overworld (" + d.pieces + "): MegaDungeon.in is not checked");
        for (ResourceLocation id : List.of(rl("nusmp:chests/dungeon_relic_vault"), rl("nusmp:chests/labyrinth_pedestal"), rl("minecraft:entities/zombie"),
                rl("minecraft:blocks/stone"), rl("nusmp:library"), LootTableIdCondition.UNKNOWN_LOOT_TABLE)) {
            Tally x = direct(mega, at, id, 800, 3f, random, none);
            if (x.pieces != 0) return fail("the table " + id + " got " + x.pieces + " pieces of gear in the Mega-Dungeon: it is a relic table or no chest table and must never be touched");
            if (x.breads != x.rolls) return fail("the table " + id + " lost the loot it came with");
        }

        // 4. Luck counts like Looting does for a kill
        double[] pLuck = ChestGear.chances(ChestRules.Kind.STANDARD, rates, factor, t, floor, 5);
        Tally l = direct(mega, at, STANDARD_ID, 4000, 5f, random, none);
        problem = verdict("standard chests with Luck 5", l, pLuck, wired);
        if (problem != null) return fail(problem);

        // 5. a chest opened by a player: the quest hook must cope (a fake player is ignored by the quest code)
        ServerPlayer opener = SelfTests.player(mega, PLAYER, at.getX(), at.getY(), at.getZ());
        Tally q = viaTable(mega, at, standard, 300, 0f, random, opener);
        if (q.maxPerRoll > 1) return fail("a chest opened by a player gave more than one piece");

        return ok("made-up chest tables rolled through the whole loot pipeline at floor " + FloorMap.label(floor) + (floor == 0 ? " (no sector here, counted as floor 1)" : "")
                + ": standard " + a.pieces + " pieces in " + a.rolls + " (table " + String.format(java.util.Locale.ROOT, "%.1f", a.rolls * GearRoller.total(pStd)) + "), grades " + a.byGrade[0] + "/" + a.byGrade[1] + "/" + a.byGrade[2]
                + "; treasure " + b.pieces + " in " + b.rolls + " (table " + String.format(java.util.Locale.ROOT, "%.1f", b.rolls * GearRoller.total(pTrs)) + "), grades " + b.byGrade[0] + "/" + b.byGrade[1] + "/" + b.byGrade[2]
                + "; never more than one, the table's own bread kept, never a relic; nothing in the Overworld" + (sanctuary != null ? " or the Hub" : "")
                + "; nothing for the vault tables, a mob's table, a block's table or an unknown id; Luck 5 gave " + l.pieces + " in " + l.rolls);
    }

    // ------------------------------------------------------------------------------------------------ pieces
    /** The classes of real and made-up ids; null when the rules answer as the design says. */
    private static String classification(ChestRules rules) {
        Object[][] want = {
                {"nusmp", "chests/labyrinth_trial", ChestRules.Kind.STANDARD}, {"nusmp", "chests/library_ruins", ChestRules.Kind.STANDARD},
                {"nusmp", "chests/grimoire_tower", ChestRules.Kind.STANDARD}, {"minecraft", "chests/simple_dungeon", ChestRules.Kind.STANDARD},
                {"nusmp", "chests/gear_selftest", ChestRules.Kind.STANDARD}, {"nusmp", "chests/gear_selftest_treasure", ChestRules.Kind.TREASURE},
                {"nusmp", "chests/dungeon_relic_vault", ChestRules.Kind.NONE}, {"nusmp", "chests/labyrinth_pedestal", ChestRules.Kind.NONE},
                {"minecraft", "entities/zombie", ChestRules.Kind.NONE}, {"minecraft", "blocks/stone", ChestRules.Kind.NONE},
                {LootTableIdCondition.UNKNOWN_LOOT_TABLE.getNamespace(), LootTableIdCondition.UNKNOWN_LOOT_TABLE.getPath(), ChestRules.Kind.NONE}};
        for (Object[] w : want) {
            ChestRules.Kind got = rules.classify((String) w[0], (String) w[1]);
            if (got != w[2]) return "the table " + w[0] + ":" + w[1] + " is classified " + got + ", the design says " + w[2];
        }
        return null;
    }

    /** Null when the tally matches the chances (and the structural rules hold), otherwise what is wrong. */
    private static String verdict(String what, Tally tl, double[] p, boolean expectPieces) {
        if (tl.maxPerRoll > 1) return what + ": one chest gave " + tl.maxPerRoll + " pieces, at most one is allowed";
        if (tl.relics > 0) return what + ": " + tl.relics + " of the added stacks are relics: standard gear must never be a relic";
        if (tl.breads != tl.rolls) return what + ": the table's own bread was found " + tl.breads + " times in " + tl.rolls + " rolls: the modifier must add beside the loot and never replace it";
        double total = GearRoller.total(p);
        double sigma = Math.sqrt(tl.rolls * total * (1 - total));
        if (Math.abs(tl.pieces - tl.rolls * total) > 6 * sigma + 5) {
            return what + ": " + tl.pieces + " pieces in " + tl.rolls + " chests, the chances say " + String.format(java.util.Locale.ROOT, "%.1f", tl.rolls * total) + " +- " + String.format(java.util.Locale.ROOT, "%.1f", 6 * sigma)
                    + (expectPieces && tl.pieces == 0 ? " (nothing at all: is nusmp:gear_chests registered, named in data/neoforge/loot_modifiers/global_loot_modifiers.json, and its conditions met?)" : "");
        }
        for (GearGrade g : GearGrade.values()) {
            double pg = p[g.ordinal()], sg = Math.sqrt(tl.rolls * pg * (1 - pg));
            if (Math.abs(tl.byGrade[g.ordinal()] - tl.rolls * pg) > 6 * sg + 5) return what + ": " + tl.byGrade[g.ordinal()] + " " + g.id() + " pieces in " + tl.rolls + " chests, the chances say " + String.format(java.util.Locale.ROOT, "%.1f", tl.rolls * pg);
        }
        return null;
    }

    /** A chest table that is not in any registry: one bread per roll, the chest parameter set, and a loot-table id of its own (NeoForge hands the id to the modifiers). */
    private static LootTable madeUp(ResourceLocation id) {
        LootTable table = LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).add(LootItem.lootTableItem(Items.BREAD))).build();
        table.setLootTableId(id);
        return table;
    }

    private static LootParams params(ServerLevel level, BlockPos at, float luck, ServerPlayer opener) {
        LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).withLuck(luck);
        if (opener != null) b.withParameter(LootContextParams.THIS_ENTITY, opener);
        return b.create(LootContextParamSets.CHEST);
    }

    /** Rolls the table through the real pipeline (every global loot modifier of the game acts on it). */
    private static Tally viaTable(ServerLevel level, BlockPos at, LootTable table, int rolls, float luck, RandomSource random, ServerPlayer opener) {
        Tally tl = new Tally();
        for (int i = 0; i < rolls; i++) tl.add(table.getRandomItems(params(level, at, luck, opener), random));
        return tl;
    }

    /** Calls only our modifier, with the given JSON conditions, on a list that already holds one bread, as the pipeline would after the table has rolled. */
    private static Tally direct(ServerLevel level, BlockPos at, ResourceLocation id, int rolls, float luck, RandomSource random, LootItemCondition[] conditions) {
        ChestLootModifier modifier = new ChestLootModifier(conditions);
        Tally tl = new Tally();
        for (int i = 0; i < rolls; i++) {
            LootContext context = new LootContext.Builder(params(level, at, luck, null)).withOptionalRandomSource(random).withQueriedLootTableId(id).create(Optional.empty());
            ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
            loot.add(new ItemStack(Items.BREAD));
            tl.add(modifier.apply(loot, context));
        }
        return tl;
    }
}
