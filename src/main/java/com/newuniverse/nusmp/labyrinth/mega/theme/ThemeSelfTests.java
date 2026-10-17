package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.gen.BossFloors;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.NextTheme;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Why: the Region Theme processor is the one piece of the theme builder that sits in the middle of the sector generator, and everything that can go
 * wrong with it is an integration fault no unit test sees: the processor type not registered (the pool JSON then fails to load), the processor list
 * {@code nusmp:mega_interior} not naming it, a theme file that did not load (a sector would silently keep its deepslate), a swap that drops a stair's
 * facing, or a relic count outside its theme's range. The CI server smoke test runs these two self-tests in the real game ({@link SelfTests}); both are
 * pure reads and finish in milliseconds. {@code theme.processor} asserts the registration, the processor list, that every theme of RegionTheme is loaded, that
 * every theme's table maps only to blocks that exist, and then pushes a sample of palette blocks (stairs turned east and upside down, a log lying on its
 * side, a slab on top, a trapdoor ...) and some blocks that must never change through the real processor, inside a made-up sector of every theme, and
 * compares what comes out with the pure table and with the properties that went in (it walks every theme of {@code RegionTheme}, so a theme added later is covered). {@code theme.relic_count} asserts the relic count per region against
 * its theme's range and for determinism, and that the vault of the real sector 0 has as many pedestals as the sector's count.
 */
public final class ThemeSelfTests {
    private ThemeSelfTests() {}

    /** Registers the two tests; called once from {@code ThemeRegistry.init}. */
    public static void register() {
        SelfTests.register("theme.processor", ThemeSelfTests::processor);
        SelfTests.register("theme.relic_count", ThemeSelfTests::relicCount);
        SelfTests.register("theme.relic_themes", ThemeSelfTests::relicThemes);
    }

    // ---------------------------------------------------------------- theme.processor
    private static SelfTests.Result processor(MinecraftServer server) {
        ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath("nusmp", "region_theme");
        if (!BuiltInRegistries.STRUCTURE_PROCESSOR.containsKey(typeId)) return SelfTests.Result.fail("nusmp:region_theme is not in the structure processor registry");
        if (BuiltInRegistries.STRUCTURE_PROCESSOR.get(typeId) != ThemeRegistry.REGION_THEME.get()) return SelfTests.Result.fail("nusmp:region_theme is registered as a different type than ThemeRegistry.REGION_THEME");

        Registry<StructureProcessorList> lists = server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        StructureProcessorList interior = lists.get(ResourceLocation.fromNamespaceAndPath("nusmp", "mega_interior"));
        if (interior == null) return SelfTests.Result.fail("the processor list nusmp:mega_interior does not exist (every Mega pool names it)");
        boolean named = false;
        for (StructureProcessor p : interior.list()) if (p instanceof RegionThemeProcessor) named = true;
        if (!named) return SelfTests.Result.fail("the processor list nusmp:mega_interior does not contain nusmp:region_theme");

        for (RegionTheme t : RegionTheme.values()) {
            if (ThemeDefs.byId(t.id()) == null) return SelfTests.Result.fail("no theme file was loaded for '" + t.id() + "' (data/nusmp/mega/themes/" + t.id() + ".json): the reload listener is not registered or the file is missing");
        }

        LevelReader reader = server.overworld();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        BlockState[] samples = samples();

        // outside the generator (no context) every block is left exactly as it is
        for (BlockState st : samples) {
            StructureTemplate.StructureBlockInfo in = new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, st, null);
            if (RegionThemeProcessor.INSTANCE.processBlock(reader, BlockPos.ZERO, BlockPos.ZERO, in, in, settings) != in)
                return SelfTests.Result.fail("the processor changed " + id(st.getBlock()) + " outside the sector generator (no RegionContext)");
        }

        Predicate<String> exists = ThemeSelfTests::registered;
        StringBuilder detail = new StringBuilder();
        for (RegionTheme t : RegionTheme.values()) {
            ThemeDef def = ThemeDefs.get(t);
            Map<String, ThemeSwap.Resolved> table = ThemeSwap.table(def.families(), def.explicit(), exists);
            for (Map.Entry<String, ThemeSwap.Resolved> x : table.entrySet()) {
                String target = x.getValue().target();
                if (target == null) return SelfTests.Result.fail(t.id() + ": " + x.getKey() + " has no target");
                if (!registered(target) || "minecraft:air".equals(target)) return SelfTests.Result.fail(t.id() + ": " + x.getKey() + " maps to " + target + ", which is not a solid registered block");
            }
            List<String> problems = ThemeSwap.problems(def.families(), def.explicit(), exists);
            if (!problems.isEmpty()) return SelfTests.Result.fail(t.id() + ": " + problems.get(0));

            Sector sector = new Sector(-1);
            sector.theme = t;
            int changed = 0;
            for (BlockState st : samples) {
                StructureTemplate.StructureBlockInfo in = new StructureTemplate.StructureBlockInfo(new BlockPos(1, 2, 3), st, null);
                StructureTemplate.StructureBlockInfo out = RegionContext.call(sector, Map.of(), () -> RegionThemeProcessor.INSTANCE.processBlock(reader, BlockPos.ZERO, BlockPos.ZERO, in, in, settings));
                if (out == null) return SelfTests.Result.fail(t.id() + ": the processor returned null for " + id(st.getBlock()));
                if (!out.pos().equals(in.pos()) || out.nbt() != in.nbt()) return SelfTests.Result.fail(t.id() + ": the processor moved or re-tagged " + id(st.getBlock()));
                if (st.is(Blocks.LIGHT)) {                                          // the theme's light: the invisible light block is moved by the theme's shift (never under 6)
                    int wantLevel = ThemeSwap.shiftLight(st.getValue(LightBlock.LEVEL), def.lightShift());
                    if (out.state().getBlock() != Blocks.LIGHT || out.state().getValue(LightBlock.LEVEL) != wantLevel)
                        return SelfTests.Result.fail(t.id() + ": a light block of level " + st.getValue(LightBlock.LEVEL) + " came out as " + out.state() + ", level " + wantLevel + " was expected (shift " + def.lightShift() + ")");
                    continue;
                }
                ThemeSwap.Resolved r = table.get(id(st.getBlock()));
                boolean expectSwap = t != RegionTheme.GRAVITY && r != null && r.swaps();
                if (!expectSwap) {
                    if (out != in) return SelfTests.Result.fail(t.id() + ": " + id(st.getBlock()) + " should stay but became " + id(out.state().getBlock()));
                    continue;
                }
                if (!id(out.state().getBlock()).equals(r.target())) return SelfTests.Result.fail(t.id() + ": " + id(st.getBlock()) + " became " + id(out.state().getBlock()) + ", the table says " + r.target());
                changed++;
                for (Property<?> p : st.getProperties()) {
                    if (!out.state().hasProperty(p)) continue;                      // the new block has no such property (a stone with no axis)
                    if (!Objects.equals(st.getValues().get(p), out.state().getValues().get(p)))
                        return SelfTests.Result.fail(t.id() + ": " + id(st.getBlock()) + " lost its " + p.getName() + " (" + st.getValues().get(p) + ") going to " + id(out.state().getBlock()));
                }
            }
            if (t == RegionTheme.GRAVITY && changed != 0) return SelfTests.Result.fail("gravity must swap nothing, but it changed " + changed + " samples");
            if (t != RegionTheme.GRAVITY && changed < 8) return SelfTests.Result.fail(t.id() + " changed only " + changed + " of the sample blocks");
            if (t != RegionTheme.GRAVITY) detail.append(detail.length() == 0 ? "" : ", ").append(t.id()).append('=').append(changed);
        }
        return SelfTests.Result.ok("processor registered and in nusmp:mega_interior, " + RegionTheme.values().length + " themes loaded, samples rewritten per theme (" + detail + "), properties kept, nothing changed outside the generator");
    }

    /** Palette blocks with non-default properties (so a lost property shows), and blocks no theme may touch. */
    private static BlockState[] samples() {
        return new BlockState[] {
                Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                Blocks.CHISELED_DEEPSLATE.defaultBlockState(),
                Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
                Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
                Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState(),
                Blocks.DEEPSLATE.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).setValue(BlockStateProperties.HALF, Half.TOP),
                Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP),
                Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
                Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState().setValue(BlockStateProperties.UP, false),
                Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z),
                Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).setValue(BlockStateProperties.HALF, Half.TOP).setValue(BlockStateProperties.OPEN, true),
                Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).setValue(BlockStateProperties.OPEN, true),
                // blocks that are not palette blocks: no theme turns these into anything (a lantern only where the theme's explicit map says so)
                Blocks.IRON_BARS.defaultBlockState(),
                Blocks.GLASS.defaultBlockState(),
                Blocks.CHAIN.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                Blocks.RED_CARPET.defaultBlockState(),
                Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true),
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12),
                Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
        };
    }

    private static String id(Block b) { return BuiltInRegistries.BLOCK.getKey(b).toString(); }

    private static boolean registered(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
    }

    // ---------------------------------------------------------------- theme.relic_count
    private static SelfTests.Result relicCount(MinecraftServer server) {
        int draws = 0;
        for (RegionTheme t : RegionTheme.values()) {
            if (t.minRelics() < 1 || t.maxRelics() > 3 || t.minRelics() > t.maxRelics()) return SelfTests.Result.fail(t.id() + " has a relic range of " + t.minRelics() + ".." + t.maxRelics() + ", outside 1..3");
            Set<Integer> seen = new TreeSet<>();
            for (long seed : new long[] {0L, 1L, 42L, -7L, 0x9E3779B97F4A7C15L}) {
                for (int sector = 0; sector < 120; sector++) {
                    int c = t.relicCount(seed, sector);
                    draws++;
                    if (c < t.minRelics() || c > t.maxRelics()) return SelfTests.Result.fail(t.id() + " seed " + seed + " sector " + sector + " gave " + c + ", outside " + t.minRelics() + ".." + t.maxRelics());
                    if (c != t.relicCount(seed, sector)) return SelfTests.Result.fail(t.id() + " seed " + seed + " sector " + sector + " is not deterministic");
                    seen.add(c);
                }
            }
            if (t.maxRelics() > t.minRelics() && seen.size() < 2) return SelfTests.Result.fail(t.id() + " has a range of " + t.minRelics() + ".." + t.maxRelics() + " but always gives " + seen);
            if (t.maxRelics() == t.minRelics() && seen.size() != 1) return SelfTests.Result.fail(t.id() + " has a fixed count but gave " + seen);
        }
        MegaData data = MegaData.get(server);
        Sector s = BossFloors.first(data);
        if (s == null) return SelfTests.Result.fail("no boss floor exists (the smoke test grows the Holy road to floor +10 with /multiverse mega expand first)");
        RegionTheme t = s.theme;
        if (s.relicCount < t.minRelics() || s.relicCount > t.maxRelics())
            return SelfTests.Result.fail("sector 0 (" + t.id() + ") has relic count " + s.relicCount + ", outside its theme's " + t.minRelics() + ".." + t.maxRelics());
        int want = t.relicCount(data.seed, s.id);
        if (s.relicCount != want) return SelfTests.Result.fail("sector 0 has relic count " + s.relicCount + " but RegionTheme.relicCount(dungeon seed, 0) says " + want);
        if (s.pedestals.size() != s.relicCount)
            return SelfTests.Result.fail("sector 0 has relic count " + s.relicCount + " but its vault has " + s.pedestals.size() + " pedestal marker(s): the vault pool or a vault template is wrong");
        return SelfTests.Result.ok(draws + " draws in range and deterministic for " + RegionTheme.values().length + " themes; sector 0 (" + t.id() + ") has " + s.relicCount + " relic(s) and " + s.pedestals.size() + " pedestal(s)");
    }

    // ---------------------------------------------------------------- theme.relic_themes
    private static SelfTests.Result relicThemes(MinecraftServer server) {
        List<String> warnings = RelicThemeMap.warnings();
        if (!warnings.isEmpty()) return SelfTests.Result.fail("relic_themes.json loaded with " + warnings.size() + " warning(s), the first: " + warnings.get(0));
        Map<String, String> explicit = RelicThemeMap.explicit();
        if (explicit.size() < 100) return SelfTests.Result.fail("relic_themes.json placed only " + explicit.size() + " relics by hand: the reload listener is not registered or the file is missing");
        List<String> unknown = new ArrayList<>();
        for (String id : explicit.keySet()) if (Relics.byId(id) == null) unknown.add(id);
        if (!unknown.isEmpty()) return SelfTests.Result.fail("relic_themes.json names " + unknown.size() + " relic(s) that do not exist, e.g. " + unknown.subList(0, Math.min(5, unknown.size())));

        Map<RegionTheme, List<String>> keywords = new EnumMap<>(RegionTheme.class);
        for (RegionTheme t : RegionTheme.values()) keywords.put(t, ThemeDefs.get(t).keywords());
        Map<RegionTheme, Integer> perTheme = new EnumMap<>(RegionTheme.class);
        int total = 0, byTable = 0, byWords = 0, byHash = 0;
        for (RelicDef d : Relics.all()) {
            if (!d.isUnique() || !d.grade().atLeast(ThemeConfig.minGrade())) continue;
            total++;
            RegionTheme t = RelicThemes.themeOf(d.id(), d.cardName(), explicit, keywords);
            if (t == null) return SelfTests.Result.fail(d.id() + " leads to no theme");
            if (t != RelicThemes.themeOf(d.id(), d.cardName(), explicit, keywords)) return SelfTests.Result.fail(d.id() + " does not always lead to the same theme");
            boolean listed = explicit.containsKey(RelicThemes.key(d.id()));
            if (!listed && t == RegionTheme.GRAVITY) return SelfTests.Result.fail(d.id() + " reached the baseline theme without an entry in relic_themes.json");
            perTheme.merge(t, 1, Integer::sum);
            if (listed) byTable++;
            else if (RelicThemes.byKeywords(d.id(), d.cardName(), keywords) != null) byWords++;
            else byHash++;
        }
        if (total < 3) return SelfTests.Result.fail("only " + total + " relics of " + ThemeConfig.minGrade() + " or above exist: a vault could never offer three");
        for (RegionTheme t : RegionTheme.values()) if (!perTheme.containsKey(t)) return SelfTests.Result.fail("no relic of " + ThemeConfig.minGrade() + " or above leads to the " + t.id() + " theme");

        // the planner's question, asked of made-up sectors: nothing until the Crucible is decided, then a theme that never changes; asked through the
        // Mega-Dungeon's own hook (MegaDungeon.themeOverride), which is the door the generator uses, so a registration that was forgotten shows here
        Sector probe = new Sector(-7);
        probe.seed = 0x5DEECE66DL;
        probe.crucible.phase = Sector.Crucible.Phase.UNOPENED;
        if (NextTheme.nextThemeFor(probe) != null) return SelfTests.Result.fail("an UNOPENED Crucible already names the next theme");
        if (MegaDungeon.themeOverride(probe) != null) return SelfTests.Result.fail("MegaDungeon.themeOverride names a theme for an UNOPENED Crucible (or is registered wrongly)");
        probe.crucible.phase = Sector.Crucible.Phase.ROLLED;
        if (NextTheme.nextThemeFor(probe) != null) return SelfTests.Result.fail("a ROLLED Crucible already names the next theme");
        if (MegaDungeon.themeOverride(probe) != null) return SelfTests.Result.fail("MegaDungeon.themeOverride names a theme for a ROLLED Crucible");
        String sample = explicit.keySet().iterator().next();
        probe.crucible.phase = Sector.Crucible.Phase.CHOSEN;
        probe.crucible.chosen = sample;
        RelicDef def = Relics.byId(sample);
        RelicThemes.Road wantRoad = RelicThemes.roadOf(sample, def == null ? "" : def.cardName(), explicit, RelicThemeMap.rulesets(), keywords);
        RegionTheme want = wantRoad.theme();
        if (NextTheme.nextThemeFor(probe) != want) return SelfTests.Result.fail("nextThemeFor gives " + NextTheme.nextThemeFor(probe) + " for " + sample + ", RelicThemes gives " + want);
        if (MegaDungeon.themeOverride(probe) != want) return SelfTests.Result.fail("MegaDungeon.themeOverride gives " + MegaDungeon.themeOverride(probe) + " for " + sample + ", RelicThemes gives " + want
                + ": ThemeRegistry.init did not register NextTheme.HOOK");
        if (!wantRoad.ruleset().equals(NextTheme.nextRulesetFor(probe))) return SelfTests.Result.fail("nextRulesetFor gives " + NextTheme.nextRulesetFor(probe) + " for " + sample + ", RelicThemes gives " + wantRoad.ruleset());
        probe.crucible.phase = Sector.Crucible.Phase.RESOLVED;
        if (NextTheme.nextThemeFor(probe) != want) return SelfTests.Result.fail("a RESOLVED Crucible names another theme than when it was CHOSEN");
        probe.crucible.chosen = "";
        RegionTheme empty = NextTheme.nextThemeFor(probe);
        if (empty == null || empty == RegionTheme.GRAVITY) return SelfTests.Result.fail("a Crucible resolved empty names " + empty + " instead of a theme other than the baseline");
        if (empty != NextTheme.nextThemeFor(probe)) return SelfTests.Result.fail("a Crucible resolved empty does not always name the same theme");
        for (RegionTheme t : RegionTheme.values()) {
            String name = RelicThemeMap.rulesetName(t.id());
            if (name == null || name.isEmpty()) return SelfTests.Result.fail("the ruleset '" + t.id() + "' (the default of the " + t.id() + " theme) has no name in relic_themes.json");
        }

        // a region's ruleset is read back from the saved Crucible of the region before it, and only when the region really wears the theme that relic chose
        MegaData data = MegaData.get(server);
        Sector tail = data.chainTail(com.newuniverse.nusmp.labyrinth.mega.Polarity.HOLY);
        if (tail != null && tail.chainIndex > 0) {
            Sector before = data.chainSector(tail.polarity, tail.chainIndex - 1);
            String got = NextTheme.rulesetOf(data, tail);
            RelicThemes.Road road = NextTheme.roadAfter(before);
            String expect = road != null && road.theme() == tail.theme ? road.ruleset() : "";
            if (!got.equals(expect)) return SelfTests.Result.fail("rulesetOf(sector " + tail.id + ") gives '" + got + "', the saved Crucible of sector " + (before == null ? "?" : before.id) + " says '" + expect + "'");
        }
        return SelfTests.Result.ok(total + " relics of " + ThemeConfig.minGrade() + " or above lead to " + perTheme.size() + " themes " + perTheme + " (" + byTable + " by the table, " + byWords + " by keywords, " + byHash
                + " by hash); every table entry is a real relic; the next theme (and its ruleset) is unknown until the Crucible is decided, and MegaDungeon.themeOverride answers like NextTheme");
    }
}
