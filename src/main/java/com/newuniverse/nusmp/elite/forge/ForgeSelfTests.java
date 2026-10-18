package com.newuniverse.nusmp.elite.forge;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.craft.EngravingHandler;
import com.newuniverse.nusmp.elite.forge.craft.Inventories;
import com.newuniverse.nusmp.elite.forge.craft.QualityApplier;
import com.newuniverse.nusmp.elite.forge.craft.SkillProbe;
import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.net.ForgeNet;
import com.newuniverse.nusmp.elite.forge.net.ForgeServer;
import com.newuniverse.nusmp.elite.forge.rules.Category;
import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import com.newuniverse.nusmp.elite.forge.rules.Minigame;
import com.newuniverse.nusmp.elite.forge.rules.Minigames;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.session.ForgeFinisher;
import com.newuniverse.nusmp.elite.forge.session.ForgeSessions;
import com.newuniverse.nusmp.elite.forge.station.StationUpgrade;
import com.newuniverse.nusmp.elite.forge.store.ForgeLedger;
import com.newuniverse.nusmp.elite.forge.store.ForgeStore;
import com.newuniverse.nusmp.elite.forge.store.ProgressTag;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the forge cannot be played by its author, so each part has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest},
 * keys {@code elite.forge.*}). They drive the system through its own entry points with a fake player standing at a station placed high in the overworld: the registry and
 * the recipe data (every item of every recipe exists), the quality applier (the sword really gets a bigger attack damage and durability, once), the station upgrade chain
 * (a wrong kit is refused, the right one changes the block and keeps its numbers), a WHOLE forging session from the first packet to the item in the inventory (with the
 * clock replaced so it runs in no time, then the same result sent too early and refused without costing anything), the engraving handler against the installed Tensura,
 * the NBT storage and its tolerance, the hooks of {@code EliteHooks}, the skill probe, and the packet codecs. A test cleans up what it placed.
 */
public final class ForgeSelfTests {
    private ForgeSelfTests() {}

    public static void register() {
        SelfTests.register("elite.forge.registry", ForgeSelfTests::registry);
        SelfTests.register("elite.forge.recipes", ForgeSelfTests::recipes);
        SelfTests.register("elite.forge.quality", ForgeSelfTests::quality);
        SelfTests.register("elite.forge.upgrade", ForgeSelfTests::upgrade);
        SelfTests.register("elite.forge.flow", ForgeSelfTests::flow);
        SelfTests.register("elite.forge.engrave", ForgeSelfTests::engrave);
        SelfTests.register("elite.forge.storage", ForgeSelfTests::storage);
        SelfTests.register("elite.forge.skills", ForgeSelfTests::skills);
        SelfTests.register("elite.forge.net", ForgeSelfTests::net);
    }

    private static final int Y = 200;

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static ServerPlayer smith(ServerLevel level, BlockPos station) {
        ServerPlayer p = SelfTests.player(level, "elite_forge_smith", station.getX() + 0.5, station.getY() + 1, station.getZ() + 2.5);
        p.setData(ForgeStore.PROGRESS.get(), new ForgeProgress());
        p.getInventory().clearContent();
        p.getAbilities().instabuild = false;
        return p;
    }

    private static BlockPos place(ServerLevel level, StationTier t, int dx) {
        BlockPos pos = new BlockPos(8 + dx, Y, 8);
        level.getChunk(pos);
        level.setBlock(pos, ForgeRegistry.station(t).get().defaultBlockState(), 3);
        return pos;
    }

    private static void clear(ServerLevel level, BlockPos pos) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }

    // ------------------------------------------------------------------ registry
    private static SelfTests.Result registry(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (StationTier t : StationTier.values()) {
            if (!BuiltInRegistries.BLOCK.containsKey(rl("forge_station_" + t.id()))) bad.add("no block for " + t.id());
            if (!BuiltInRegistries.ITEM.containsKey(rl("forge_station_" + t.id()))) bad.add("no item for " + t.id());
            if (t.prev() != null && !BuiltInRegistries.ITEM.containsKey(rl("forge_upgrade_kit_" + t.id()))) bad.add("no kit for " + t.id());
            if (!(ForgeRegistry.station(t).get() instanceof ForgeStationBlock b) || b.tier() != t) bad.add("block tier mismatch " + t.id());
            if (!ForgeRegistry.STATION_BE.get().isValid(ForgeRegistry.station(t).get().defaultBlockState())) bad.add("block entity type does not accept " + t.id());
        }
        for (String c : new String[]{"forge_quality", "forge_smith", "forge_engraved", "forge_kind"}) {
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(rl(c))) bad.add("component " + c + " missing");
        }
        if (ForgeRecipes.all().size() < 40) bad.add("only " + ForgeRecipes.all().size() + " recipes loaded");
        if (!bad.isEmpty()) return SelfTests.Result.fail(String.join("; ", bad));
        return SelfTests.Result.ok("4 stations, 3 kits, 4 components, " + ForgeRecipes.all().size() + " recipes");
    }

    // ------------------------------------------------------------------ recipes
    private static SelfTests.Result recipes(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int[] perTier = new int[4];
        for (ForgeRecipes.Loaded l : ForgeRecipes.all().values()) {
            if (!l.available()) bad.add(l.def().id() + " needs " + l.missing());
            perTier[l.def().tier()]++;
        }
        for (int t = 0; t < 4; t++) if (perTier[t] == 0) bad.add("no recipe of tier " + t);
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems: " + String.join("; ", bad.subList(0, Math.min(4, bad.size()))));
        return SelfTests.Result.ok(ForgeRecipes.all().size() + " recipes, every item exists; per tier " + perTier[0] + "/" + perTier[1] + "/" + perTier[2] + "/" + perTier[3]);
    }

    // ------------------------------------------------------------------ quality applier
    private static double attackOf(ItemStack s) {
        double sum = 0;
        for (ItemAttributeModifiers.Entry e : s.getAttributeModifiers().modifiers()) {
            if (e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.unwrapKey().get())
                    && e.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) sum += e.modifier().amount();
        }
        return sum;
    }

    private static SelfTests.Result quality(MinecraftServer server) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        int before = sword.getMaxDamage();
        if (!QualityApplier.apply(sword, Quality.MYTHICAL, "Tester", 1.0)) return SelfTests.Result.fail("apply returned false on a plain sword");
        if (ForgeComponents.qualityOf(sword) != Quality.MYTHICAL) return SelfTests.Result.fail("the quality component was not written");
        if (sword.getMaxDamage() <= before) return SelfTests.Result.fail("max durability did not grow: " + before + " -> " + sword.getMaxDamage());
        double extra = attackOf(sword);
        if (Math.abs(extra - Quality.MYTHICAL.statBonus()) > 1e-6) return SelfTests.Result.fail("attack bonus " + extra + " instead of " + Quality.MYTHICAL.statBonus());
        if (QualityApplier.apply(sword, Quality.COMMON, "Tester", 1.0)) return SelfTests.Result.fail("a forged item was forged again");
        ItemStack helm = new ItemStack(Items.IRON_HELMET);
        QualityApplier.apply(helm, Quality.EPIC, "Tester", 1.0);
        boolean armour = false;
        for (ItemAttributeModifiers.Entry e : helm.getAttributeModifiers().modifiers()) if (e.modifier().id().equals(rl("forge_quality"))) armour = true;
        if (!armour) return SelfTests.Result.fail("the helmet got no armour bonus");
        ItemStack bow = new ItemStack(Items.BOW);
        QualityApplier.apply(bow, Quality.RARE, "Tester", 1.0);
        if (ForgeComponents.qualityOf(bow) != Quality.RARE || bow.get(DataComponents.MAX_DAMAGE) == null) return SelfTests.Result.fail("the bow got no label or durability");
        ItemStack scaled = new ItemStack(Items.IRON_SWORD);
        QualityApplier.apply(scaled, Quality.MYTHICAL, "Tester", 0.0);
        if (scaled.getMaxDamage() != before) return SelfTests.Result.fail("a stat scale of 0 still changed durability");
        return SelfTests.Result.ok("durability " + before + " -> " + sword.getMaxDamage() + ", sword +" + Math.round(extra * 100) + "% damage, helmet and bow labelled");
    }

    // ------------------------------------------------------------------ upgrade
    private static SelfTests.Result upgrade(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = place(level, StationTier.BASIC, 0);
        try {
            if (level.getBlockEntity(pos) instanceof ForgeStationBlockEntity be) be.restore(7, 3, 0);
            else return SelfTests.Result.fail("the station has no block entity");
            ItemStack wrong = new ItemStack(ForgeRegistry.kit(StationTier.MYTHRIL).get());
            if (StationUpgrade.refusal(level, pos, wrong) == null) return SelfTests.Result.fail("a Mythril kit was accepted on a Basic station");
            ItemStack right = new ItemStack(ForgeRegistry.kit(StationTier.MAGISTEEL).get());
            if (StationUpgrade.refusal(level, pos, right) != null) return SelfTests.Result.fail("the right kit was refused: " + StationUpgrade.refusal(level, pos, right));
            ItemStack notKit = new ItemStack(Items.STICK);
            if (StationUpgrade.refusal(level, pos, notKit) == null) return SelfTests.Result.fail("a stick was accepted as a kit");
            ServerPlayer p = smith(level, pos);
            ItemStack kit = new ItemStack(ForgeRegistry.kit(StationTier.MAGISTEEL).get(), 2);
            if (!StationUpgrade.tryUpgrade(p, level, pos, kit)) return SelfTests.Result.fail("the upgrade did not happen");
            if (kit.getCount() != 1) return SelfTests.Result.fail("the kit was not used up");
            if (ForgeStationBlock.tierAt(level, pos) != StationTier.MAGISTEEL) return SelfTests.Result.fail("the block is not a Magisteel station");
            if (!(level.getBlockEntity(pos) instanceof ForgeStationBlockEntity after) || after.forged() != 7 || after.best() != 3 || after.upgrades() != 1) {
                return SelfTests.Result.fail("the station's numbers were not carried over");
            }
            for (StationTier t : new StationTier[]{StationTier.MYTHRIL, StationTier.DRAGONFORGE}) {
                if (!StationUpgrade.tryUpgrade(p, level, pos, new ItemStack(ForgeRegistry.kit(t).get()))) return SelfTests.Result.fail("the chain broke at " + t.id());
            }
            if (ForgeStationBlock.tierAt(level, pos) != StationTier.DRAGONFORGE) return SelfTests.Result.fail("the chain did not end at Dragonforge");
            if (StationUpgrade.refusal(level, pos, new ItemStack(ForgeRegistry.kit(StationTier.DRAGONFORGE).get())) == null) return SelfTests.Result.fail("a Dragonforge station accepted a kit");
            return SelfTests.Result.ok("Basic -> Magisteel -> Mythril -> Dragonforge, wrong kits refused, numbers carried (7 forged, best 3)");
        } finally {
            clear(level, pos);
        }
    }

    // ------------------------------------------------------------------ the whole session
    private static void give(ServerPlayer p, Item item, int n) { p.getInventory().add(new ItemStack(item, n)); }

    private static List<int[]> perfectPlay(ForgeSessions.Session s) {
        List<int[]> in = new ArrayList<>();
        for (int i = 0; i < s.steps().size(); i++) in.add(Minigames.of(s.steps().get(i).type()).perfectInput(s.seeds()[i], s.steps().get(i).difficulty()));
        return in;
    }

    private static long claimedBy(ForgeSessions.Session s, List<int[]> in) {
        long c = 0;
        for (int i = 0; i < s.steps().size(); i++) c += Minigames.of(s.steps().get(i).type()).claimedMillis(s.seeds()[i], s.steps().get(i).difficulty(), in.get(i));
        return c;
    }

    private static SelfTests.Result flow(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = place(level, StationTier.BASIC, 3);
        BlockPos top = place(level, StationTier.DRAGONFORGE, 6);
        long[] now = {1_000_000L};
        java.util.function.LongSupplier real = ForgeSessions.clock;
        ForgeSessions.clock = () -> now[0];
        ServerPlayer p = null;
        try {
            p = smith(level, pos);
            ForgeSessions.clearAll();
            // 1. missing materials: refused, nothing opened
            ForgeServer.Begun none = ForgeServer.begin(p, pos, "nusmp:iron_sword", false);
            if (none.session() != null || !"nusmp.forge.msg.missing".equals(none.error())) return SelfTests.Result.fail("begin without materials: " + none);
            // 2. a recipe of a higher tier is refused at a Basic station
            give(p, Items.IRON_INGOT, 2);
            give(p, Items.STICK, 1);
            give(p, Items.NETHERITE_INGOT, 1);
            ForgeServer.Begun high = ForgeServer.begin(p, pos, "nusmp:netherite_sword", false);
            if (high.session() != null || !"nusmp.forge.msg.tier".equals(high.error())) return SelfTests.Result.fail("a tier-2 recipe at a Basic station: " + high);
            // 3. a long sequence reported before it could have been played is refused and costs nothing
            ServerPlayer rich = SelfTests.player(level, "elite_forge_rich", top.getX() + 0.5, top.getY() + 1, top.getZ() + 2.5);
            rich.setData(ForgeStore.PROGRESS.get(), new ForgeProgress());
            rich.getInventory().clearContent();
            ForgeRecipes.Loaded cleave = ForgeRecipes.get("nusmp:thundercleave");
            if (cleave == null) return SelfTests.Result.fail("nusmp:thundercleave is not loaded");
            for (var st : cleave.def().inputs()) give(rich, Inventories.item(st.item()), st.count());
            ForgeServer.Begun long1 = ForgeServer.begin(rich, top, "nusmp:thundercleave", false);
            if (long1.session() == null) return SelfTests.Result.fail("a Dragonforge begin failed: " + long1.error());
            ForgeSessions.Session ls = ForgeSessions.take(rich.getUUID(), long1.session().id());
            List<int[]> lp = perfectPlay(ls);
            if (claimedBy(ls, lp) < 20_000L) return SelfTests.Result.fail("the four-step sequence claims only " + claimedBy(ls, lp) + " ms");
            now[0] += 500;
            ForgeFinisher.Outcome early = ForgeFinisher.finish(rich, ls, lp);
            if (early.ok()) return SelfTests.Result.fail("a result that came before it could be played was accepted");
            for (var st : cleave.def().inputs()) if (Inventories.count(rich, Inventories.item(st.item())) != st.count()) return SelfTests.Result.fail("the early result cost materials");
            ForgeSessions.Session again = ForgeServer.begin(rich, top, "nusmp:thundercleave", false).session();
            ForgeSessions.Session again2 = ForgeSessions.take(rich.getUUID(), again.id());
            now[0] += claimedBy(again2, perfectPlay(again2)) + 400;
            ForgeFinisher.Outcome cleaved = ForgeFinisher.finish(rich, again2, perfectPlay(again2));
            if (!cleaved.ok()) return SelfTests.Result.fail("the long honest result was refused: " + cleaved.message() + " " + cleaved.reason());
            Quality top1 = ForgeComponents.qualityOf(cleaved.stack());
            if (top1 == null || top1.level() < Quality.EPIC.level()) return SelfTests.Result.fail("a perfect Dragonforge play gave " + top1);
            // 4. the honest short path at the Basic station
            ForgeServer.Begun b = ForgeServer.begin(p, pos, "nusmp:iron_sword", false);
            if (b.session() == null) return SelfTests.Result.fail("begin failed: " + b.error());
            if (!ForgeSessions.activeAt(level, pos)) return SelfTests.Result.fail("the session is not on the book");
            if (!level.getBlockState(pos).getValue(ForgeStationBlock.LIT)) return SelfTests.Result.fail("the station is not lit while forging");
            ForgeSessions.Session s = ForgeSessions.take(p.getUUID(), b.session().id());
            if (ForgeSessions.take(p.getUUID(), b.session().id()) != null) return SelfTests.Result.fail("a session could be taken twice");
            List<int[]> perfect = perfectPlay(s);
            now[0] += claimedBy(s, perfect) + 300;
            ForgeFinisher.Outcome ok = ForgeFinisher.finish(p, s, perfect);
            if (!ok.ok()) return SelfTests.Result.fail("the honest result was refused: " + ok.message() + " " + ok.reason());
            Quality q = ForgeComponents.qualityOf(ok.stack());
            if (q == null) return SelfTests.Result.fail("the item carries no quality");
            if (q.level() < Quality.UNCOMMON.level()) return SelfTests.Result.fail("a perfect play gave only " + q);
            if (q.level() > StationTier.BASIC.ceiling().level()) return SelfTests.Result.fail("a Basic station made a " + q);
            if (Inventories.count(p, Items.IRON_INGOT) != 0 || Inventories.count(p, Items.STICK) != 0) return SelfTests.Result.fail("the materials were not taken");
            if (Inventories.count(p, Items.IRON_SWORD) != 1) return SelfTests.Result.fail("the sword is not in the inventory");
            ForgeProgress pr = ForgeStore.of(p);
            if (pr.crafted() != 1 || pr.best() != q.level() || !pr.knows("nusmp:iron_sword")) return SelfTests.Result.fail("progress not recorded: " + pr.toMap());
            if (EliteHooks.forgeCraftedCount.applyAsInt(p.getUUID()) != 1 || EliteHooks.forgeBestQuality.applyAsInt(p.getUUID()) != q.level()) return SelfTests.Result.fail("the hooks do not answer the progress");
            if (!(level.getBlockEntity(pos) instanceof ForgeStationBlockEntity be) || be.forged() != 1) return SelfTests.Result.fail("the station did not count the item");
            ForgeSessions.clearAll();
            ForgeStationBlock.setLit(level, pos, false);
            ForgeServer.Begun empty = ForgeServer.begin(p, pos, "nusmp:iron_sword", false);
            if (empty.session() != null) return SelfTests.Result.fail("a session opened with no materials left");
            return SelfTests.Result.ok("iron sword at Basic: " + q.id() + " (score " + ok.score() + ", " + Math.round(ok.roll().points()) + " points); Dragonforge thundercleave: " + top1.id()
                    + "; the early report was refused at no cost");
        } finally {
            ForgeSessions.clock = real;
            ForgeSessions.clearAll();
            clear(level, pos);
            clear(level, top);
            if (p != null) p.getInventory().clearContent();
        }
    }

    // ------------------------------------------------------------------ engraving
    private static SelfTests.Result engrave(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        QualityApplier.apply(sword, Quality.LEGENDARY, "Tester", 1.0);
        int slots = EngravingHandler.freeSlots(sword, Quality.LEGENDARY, Specialization.WEAPONSMITH, Category.WEAPON);
        if (slots != 5) return SelfTests.Result.fail("a Legendary weapon of a Weaponsmith should have 5 slots, has " + slots);
        List<EngravingHandler.Applied> done = EngravingHandler.engrave(level.registryAccess(), sword, Quality.LEGENDARY, Specialization.WEAPONSMITH, Category.WEAPON, 99, 42L);
        if (done.size() > slots) return SelfTests.Result.fail("more engravings than slots: " + done.size());
        if (ForgeComponents.engravedCount(sword) != done.size()) return SelfTests.Result.fail("the engraved counter says " + ForgeComponents.engravedCount(sword) + " for " + done.size());
        int left = EngravingHandler.freeSlots(sword, Quality.LEGENDARY, Specialization.WEAPONSMITH, Category.WEAPON);
        if (left != slots - done.size()) return SelfTests.Result.fail("free slots did not shrink");
        for (EngravingHandler.Applied a : done) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", a.id()));
            var h = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key);
            if (h.isEmpty() || !h.get().is(EngravingHandler.ENGRAVING_TAG)) return SelfTests.Result.fail(a.id() + " was applied but is not in #tensura:engraving");
            if (sword.getEnchantments().getLevel(h.get()) != a.level()) return SelfTests.Result.fail(a.id() + " is not on the sword at level " + a.level());
        }
        ItemStack common = new ItemStack(Items.IRON_SWORD);
        QualityApplier.apply(common, Quality.COMMON, "Tester", 1.0);
        if (!EngravingHandler.engrave(level.registryAccess(), common, Quality.COMMON, null, Category.WEAPON, 3, 1L).isEmpty()) return SelfTests.Result.fail("a Common item was engraved");
        String note = done.isEmpty() ? "no Tensura engraving found on this install (all skipped, no error)" : done.size() + " engravings: " + done;
        return SelfTests.Result.ok(note);
    }

    // ------------------------------------------------------------------ storage
    private static SelfTests.Result storage(MinecraftServer server) {
        ForgeProgress p = new ForgeProgress();
        p.recordForge(StationTier.MYTHRIL, "nusmp:a", Quality.EPIC);
        p.recordForge(StationTier.BASIC, "nusmp:b", Quality.COMMON);
        p.setSpecialization(Specialization.RUNESMITH);
        CompoundTag tag = ProgressTag.write(p);
        ForgeProgress back = ProgressTag.read(tag);
        if (!back.toMap().equals(p.toMap())) return SelfTests.Result.fail("NBT round trip changed the progress: " + back.toMap() + " vs " + p.toMap());
        if (!ProgressTag.read(new CompoundTag()).isEmpty()) return SelfTests.Result.fail("an empty tag is not an empty progress");
        CompoundTag junk = new CompoundTag();
        junk.putString("crafted", "many");
        junk.putString("by_tier", "x");
        junk.putInt("spec", 5);
        junk.putInt("known", 3);
        junk.putIntArray("by_quality", new int[]{1, 2});
        ForgeProgress tolerant = ProgressTag.read(junk);
        if (tolerant.crafted() != 0 || tolerant.craftedOf(Quality.UNCOMMON) != 2) return SelfTests.Result.fail("the tolerant read gave " + tolerant.toMap());
        if (!ProgressTag.write(new ForgeProgress()).isEmpty()) return SelfTests.Result.fail("an empty progress writes keys");
        // the ledger and the hooks answer for an offline UUID
        UUID ghost = UUID.nameUUIDFromBytes("nusmp-selftest:elite-forge-ghost".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ForgeLedger.get(server).put(ghost, 9, 4);
        if (EliteHooks.forgeCraftedCount.applyAsInt(ghost) != 9 || EliteHooks.forgeBestQuality.applyAsInt(ghost) != 4) return SelfTests.Result.fail("the hooks do not read the ledger of an offline player");
        ForgeLedger.get(server).put(ghost, 0, 0);
        return SelfTests.Result.ok("round trip, tolerant read, empty writes nothing, offline hooks read the ledger");
    }

    // ------------------------------------------------------------------ skills
    private static SelfTests.Result skills(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = smith(level, new BlockPos(8, Y, 8));
        if (SkillProbe.level(p, "nusmp:no_such_skill_at_all") != 0 || SkillProbe.level(p, "NOT A VALID ID") != 0 || SkillProbe.level(p, "") != 0) return SelfTests.Result.fail("an unknown skill id did not answer 0");
        int bonus = SkillProbe.bonus(p);
        if (bonus != 0) return SelfTests.Result.fail("a fresh fake player has a skill bonus of " + bonus);
        int cap = ForgeRecipes.skillTable().cap();
        if (cap <= 0 || cap > 16 || ForgeRecipes.skillTable().entries().isEmpty()) return SelfTests.Result.fail("the skill table is empty or has a bad cap");
        return SelfTests.Result.ok(ForgeRecipes.skillTable().entries().size() + " table rows, cap " + cap + ", unknown ids answer 0");
    }

    // ------------------------------------------------------------------ packets
    private static SelfTests.Result net(MinecraftServer server) {
        ForgeNet.Progress pr = new ForgeNet.Progress(12, 4, 2, "runesmith", true, 20, 5, 9, List.of(3, 4, 5, 0));
        ForgeNet.RecipeView r = new ForgeNet.RecipeView("nusmp:iron_sword", 0, "weapon", "minecraft:iron_sword", 1,
                List.of(new ForgeNet.StackView("minecraft:iron_ingot", 2, 1)), List.of(new ForgeNet.StackView("minecraft:gold_ingot", 2, 0)), List.of(2),
                List.of(new ForgeNet.StepView("hammer_timing", 1)), 16, true, true, true, false);
        ForgeNet.OpenStation open = new ForgeNet.OpenStation(new BlockPos(1, 2, 3), 1, 7, true, pr, List.of(r));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ForgeNet.OpenStation.STREAM_CODEC.encode(buf, open);
        if (!ForgeNet.OpenStation.STREAM_CODEC.decode(buf).equals(open)) return SelfTests.Result.fail("OpenStation did not survive the codec");
        ForgeNet.Start start = new ForgeNet.Start(5, new BlockPos(1, 2, 3), "nusmp:iron_sword", "minecraft:iron_sword", 0, List.of(new ForgeNet.StepView("rune_alignment", 3)), new long[]{1L, -2L});
        buf = new FriendlyByteBuf(Unpooled.buffer());
        ForgeNet.Start.STREAM_CODEC.encode(buf, start);
        ForgeNet.Start s2 = ForgeNet.Start.STREAM_CODEC.decode(buf);
        if (s2.session() != 5 || !s2.outItem().equals("minecraft:iron_sword") || !java.util.Arrays.equals(s2.seeds(), start.seeds()) || !s2.steps().equals(start.steps())) return SelfTests.Result.fail("Start did not survive the codec");
        ForgeNet.Result res = new ForgeNet.Result(5, List.of(new int[]{250, 900}, new int[0], new int[]{-4, 1000000}));
        buf = new FriendlyByteBuf(Unpooled.buffer());
        ForgeNet.Result.STREAM_CODEC.encode(buf, res);
        ForgeNet.Result r2 = ForgeNet.Result.STREAM_CODEC.decode(buf);
        if (r2.session() != 5 || r2.inputs().size() != 3 || !java.util.Arrays.equals(r2.inputs().get(2), res.inputs().get(2))) return SelfTests.Result.fail("Result did not survive the codec");
        ForgeNet.Done done = new ForgeNet.Done(true, "nusmp.forge.msg.done", 6, 975, "minecraft:iron_sword", 1, 1013, false, List.of("severance 2"));
        buf = new FriendlyByteBuf(Unpooled.buffer());
        ForgeNet.Done.STREAM_CODEC.encode(buf, done);
        if (!ForgeNet.Done.STREAM_CODEC.decode(buf).equals(done)) return SelfTests.Result.fail("Done did not survive the codec");
        // a hostile oversized list is refused by the codec
        FriendlyByteBuf evil = new FriendlyByteBuf(Unpooled.buffer());
        evil.writeVarInt(5);
        evil.writeVarInt(1000);
        boolean refused = false;
        try {
            ForgeNet.Result.STREAM_CODEC.decode(evil);
        } catch (RuntimeException e) {
            refused = true;
        }
        if (!refused) return SelfTests.Result.fail("an oversized step list was not refused");
        return SelfTests.Result.ok("OpenStation, Start, Result and Done round-trip; an oversized list is refused");
    }
}
