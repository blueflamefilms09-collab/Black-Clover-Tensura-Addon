package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.BookUse;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.item.SkillItems;
import com.newuniverse.nusmp.skilltree.rarity.BlockedFranchises;
import com.newuniverse.nusmp.skilltree.rarity.BookFacts;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.rarity.BossThemes;
import com.newuniverse.nusmp.skilltree.rarity.RarityConfig;
import com.newuniverse.nusmp.skilltree.shop.ShopGoods;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Why: the rarity tiers and the franchise blacklist (3.26.0) are WIRING on top of pure rules (a component on the stack, a table per rarity cached by the registry version and the setting, three loot doors,
 * the shop's shelf, the swap of an old book) and wiring is what a unit test cannot see. Three self-tests, run by the CI server smoke test with a stand-in player. {@code skillbook.rarity.drops}: the
 * per-rarity weight tables partition the weight table and hold only lines of their rarity; a monster's book is never mythic and never of a blocked franchise; the Guardian's book is the same book twice for
 * the same site, never mythic, never blocked; the rift modifier hands out a mythic book (an epic one when none is left) with the tiers on and the old weighted book with them off; the old rule is
 * reproduced by {@code LootRules.plan} with the tiers off. {@code skillbook.rarity.read}: a book wears the rarity of its line (a component on the stack wins), epic and mythic books glint, a plain one
 * does not, reading teaches and uses one book up, a second copy is refused and kept, and the swap of an old book never hands out a mythic or a blocked book. {@code skillbook.blocked}: with the three
 * franchises of the default blacklist set, no pick path (the weight table, the per-rarity tables, the Guardian, the rift, the shop's stock) ever names one of their lines, a held book of one still reads,
 * the table is rebuilt once when the setting changes and not per draw, and putting the setting back restores everything. Where the world lacks something (no routes, no mega dimension, a stand-in with
 * no skill list) the answer says so and checks what it can.
 */
public final class RaritySelfTests {
    private RaritySelfTests() {}

    private static final ResourceLocation RELIC_VAULT = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/dungeon_relic_vault");

    public static void register() {
        SelfTests.register("skillbook.rarity.drops", RaritySelfTests::drops);
        SelfTests.register("skillbook.rarity.read", RaritySelfTests::read);
        SelfTests.register("skillbook.blocked", RaritySelfTests::blocked);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static BookRarity rarityOfBook(ItemStack book) { return SkillItems.rarityOf(book); }

    private static String bareOfBook(ItemStack book) {
        SkillRoute r = RouteRegistry.byBook(SkillItems.bookIdOf(book));
        return r == null ? null : LineIndex.bare(r.id());
    }

    // ================================================================ skillbook.rarity.drops
    private static SelfTests.Result drops(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        StringBuilder said = new StringBuilder();
        if (RouteRegistry.all().isEmpty()) return SelfTests.Result.ok("no route data loaded (drops not driven)");

        // 1. the per-rarity tables are a partition of the weight table
        RouteTable.Snap snap = RouteTable.current();
        long sum = 0;
        for (BookRarity r : BookRarity.values()) {
            WeightedTable t = snap.rarityTable(r);
            sum += t.total();
            for (int i = 0; i < snap.routes().length; i++) {
                int w = t.weightOf(i);
                if (w > 0 && BookFacts.rarityOf(snap.routes()[i].id()) != r) bad.add("the " + r.id() + " table holds " + snap.routes()[i].id() + " of rarity " + BookFacts.rarityOf(snap.routes()[i].id()).id());
                if (w > 0 && w != snap.table().weightOf(i)) bad.add("the " + r.id() + " table changes the weight of " + snap.routes()[i].id());
            }
        }
        if (sum != snap.table().total()) bad.add("the rarity tables weigh " + sum + " together, the weight table " + snap.table().total());
        said.append("rarity tables partition ").append(sum).append(" weight; ");

        // 2. a monster's book: never mythic, never blocked, of the drawn rarity or the next one down
        Random random = new Random(26);
        Dice dice = random::nextDouble;
        int builds = RouteTable.builds();
        int mythic = 0, blockedHit = 0, above = 0, empty = 0;
        for (int i = 0; i < 1500; i++) {
            BookRarity want = BookRarity.fromRank(i % 4);                      // common .. epic, a monster never asks for mythic
            ItemStack b = LootDrops.tieredBook(want, dice, null);
            if (b.isEmpty()) {
                empty++;
                continue;
            }
            BookRarity got = rarityOfBook(b);
            if (got == BookRarity.MYTHIC) mythic++;
            if (got.rank() > want.rank()) above++;
            String bare = bareOfBook(b);
            if (bare != null && BookFacts.isBlocked(bare)) blockedHit++;
        }
        if (mythic != 0) bad.add(mythic + " monster books were mythic");
        if (above != 0) bad.add(above + " monster books were rarer than the rarity drawn");
        if (blockedHit != 0) bad.add(blockedHit + " monster books were of a blocked franchise");
        if (RouteTable.builds() != builds) bad.add("1500 monster picks rebuilt the weight table");
        said.append("1500 monster picks (").append(empty).append(" empty); ");

        // 3. the Guardian: same site, same book; never mythic, never blocked
        int guardians = 0;
        for (String theme : BossThemes.themeIds()) {
            for (int floor : new int[] {10, 20, 30, 40, 50, 90}) {
                for (BookRarity want : new BookRarity[] {BookRarity.RARE, BookRarity.EPIC}) {
                    LootVictims.Site site = new LootVictims.Site(null, floor, theme);
                    ItemStack a = LootDrops.guardianBook(want, site, null), b = LootDrops.guardianBook(want, site, null);
                    if (a.isEmpty() != b.isEmpty() || (!a.isEmpty() && !SkillItems.bookIdOf(a).equals(SkillItems.bookIdOf(b)))) bad.add("the Guardian book of floor " + floor + " / " + theme + " is not deterministic");
                    if (a.isEmpty()) continue;
                    guardians++;
                    if (rarityOfBook(a) == BookRarity.MYTHIC) bad.add("the Guardian of floor " + floor + " / " + theme + " dropped a mythic book");
                    String bare = bareOfBook(a);
                    if (bare != null && BookFacts.isBlocked(bare)) bad.add("the Guardian of floor " + floor + " / " + theme + " dropped a blocked book");
                }
            }
        }
        said.append(guardians).append(" Guardian picks deterministic; ");

        // 4. the old rule, reproduced with the tiers off
        LootRules.Victim monster = new LootRules.Victim(true, true, LootRules.Kind.MONSTER, com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL, 3);
        int differ = 0;
        for (int i = 0; i < 2000; i++) {
            long seed = 1000 + i;
            Random a = new Random(seed), b = new Random(seed);
            LootRules.Chances loud = new LootRules.Chances(0.5, 0.5, 0.5, 1, 3, false);
            LootRules.Plan old = LootRules.decide(monster, loud, a::nextDouble);
            LootRules.Decision now = LootRules.plan(monster, loud, RarityConfig.DEFAULT.withTiers(false), 9.0, 5, b::nextDouble);
            if (now.tiered() != null || !now.old().equals(old)) differ++;
        }
        if (differ != 0) bad.add("with the tiers off " + differ + " of 2000 decisions differ from the 3.25 rule");
        said.append("tiers off = old rule; ");
        if (!bad.isEmpty()) return fail(bad);

        // 5. the rift modifier (needs the mega dimension)
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) {
            said.append("rift not driven (no mega dimension)");
            return SelfTests.Result.ok(said.toString());
        }
        boolean routes = LootConfig.get(LootConfig.ENABLED) && LootConfig.get(LootConfig.RIFT_BOOKS);
        if (!routes) {
            said.append("rift not driven (loot or rift books switched off)");
            return SelfTests.Result.ok(said.toString());
        }
        ServerPlayer opener = SelfTests.player(mega, "rarity_opener", 4_000_000, 96, 4_000_000);
        Vec3 inPocket = new Vec3(RiftRules.pocketX(0) + 18, RiftRules.POCKET_Y, RiftRules.POCKET_Z);
        Map<String, Double> sure = Map.of(RELIC_VAULT.toString(), 1.0);
        RandomSource rs = RandomSource.create(7L);
        int mythicBooks = 0, other = 0, none = 0;
        boolean anyMythicLine = !snap.rarityTable(BookRarity.MYTHIC).isEmpty();
        for (int i = 0; i < 60; i++) {
            ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
            int added = RiftBookLootModifier.add(loot, context(mega, inPocket, opener, rs), sure, 1.0, RarityConfig.DEFAULT.withTiers(true), dice);
            if (added != 1 || loot.isEmpty()) {
                none++;
                continue;
            }
            BookRarity r = rarityOfBook(loot.get(loot.size() - 1));
            if (r == BookRarity.MYTHIC) mythicBooks++;
            else if (r != BookRarity.EPIC) other++;
        }
        if (none != 0) bad.add(none + " of 60 sure rift chests added no book with the tiers on");
        if (other != 0) bad.add(other + " rift books were neither mythic nor the epic fallback");
        if (anyMythicLine && mythicBooks == 0) bad.add("no rift book was mythic in 60 rolls although mythic lines can drop");
        int oldBooks = 0;
        for (int i = 0; i < 20; i++) {
            ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
            oldBooks += RiftBookLootModifier.add(loot, context(mega, inPocket, opener, rs), sure, 1.0, RarityConfig.DEFAULT.withTiers(false), dice);
        }
        if (oldBooks != 20) bad.add("with the tiers off only " + oldBooks + " of 20 sure rift chests paid a book");
        said.append("rift: ").append(mythicBooks).append(" mythic of 60 with the tiers on, 20 of 20 with them off");
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    // ================================================================ skillbook.rarity.read
    private static SelfTests.Result read(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<LineIndex.Line> lines = UniqueSkills.lines();
        if (lines.isEmpty()) return fail("the shipped table of books is empty");
        StringBuilder said = new StringBuilder();

        // 1. a stack wears the rarity of its line; a component on the stack wins; epic and mythic glint
        int[] seen = new int[BookRarity.count()];
        for (LineIndex.Line l : lines) {
            ItemStack s = SkillBookItem.stackFor(l);
            if (s.isEmpty()) continue;
            if (rarityOfBook(s) != l.rarity()) bad.add(l.bare() + ": the stack reads " + rarityOfBook(s).id() + ", the table " + l.rarity().id());
            seen[l.rarity().rank()]++;
            if (!com.newuniverse.nusmp.skilltree.item.LegacyLook.enabled() && s.getItem().isFoil(s) != l.rarity().glint()) bad.add(l.bare() + ": glint " + s.getItem().isFoil(s) + " for " + l.rarity().id());
        }
        ItemStack probe = SkillBookItem.stackFor(lines.get(0));
        probe.set(SkillItems.BOOK_RARITY.get(), "mythic");
        if (rarityOfBook(probe) != BookRarity.MYTHIC) bad.add("a stack with the component mythic does not read mythic");
        probe.set(SkillItems.BOOK_RARITY.get(), "no_such_rarity");
        if (rarityOfBook(probe) != BookRarity.COMMON) bad.add("an unknown rarity on a stack does not read common");
        said.append("stacks wear their line's rarity (").append(seen[0]).append('/').append(seen[1]).append('/').append(seen[2]).append('/').append(seen[3]).append('/').append(seen[4]).append("); ");
        if (!bad.isEmpty()) return fail(bad);

        // 2. the book on a stand-in
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.standIn(level, "rarity_reader", 0.5, 100, 0.5);
        if (io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(fp) == null || RouteRegistry.all().isEmpty()) {
            said.append("the reading was not driven (the stand-in has no skill list, or no data is loaded)");
            return SelfTests.Result.ok(said.toString());
        }
        LineIndex.Line first = null;
        for (LineIndex.Line l : lines) {
            if (RouteRegistry.byId(l.routeId()) != null && l.rarity().rank() >= 1) {
                first = l;
                break;
            }
        }
        if (first == null) first = lines.get(0);
        boolean creative = fp.getAbilities().instabuild;
        boolean tiers = LootConfig.get(LootConfig.RARITY_TIERS);
        try {
            fp.getAbilities().instabuild = false;
            UniqueBooks.forget(fp, first.bare());
            ItemStack book = SkillBookItem.stackFor(first);
            book.setCount(2);
            if (BookUse.readSkillBook(fp, book) != BookUse.Result.LEARNED) bad.add("reading " + first.bare() + " (" + first.rarity().id() + ") did not teach it");
            if (book.getCount() != 1) bad.add("reading used up " + (2 - book.getCount()) + " books, not one");
            fp.getCooldowns().removeCooldown(book.getItem());
            if (BookUse.readSkillBook(fp, book) != BookUse.Result.ALREADY_OWNED) bad.add("a second copy of an owned skill was not refused");
            if (book.getCount() != 1) bad.add("a refused book was used up");
            said.append("read ").append(first.bare()).append(" (").append(first.rarity().id()).append("), refused the copy and kept it; ");

            // 3. the swap of an old book never hands out a mythic (tiers on) or a blocked book
            int swaps = 0, mythic = 0, blockedGiven = 0;
            for (int i = 0; i < 24; i++) {
                fp.getInventory().clearContent();
                ItemStack old = SkillBookItem.stack(LegacyIds.bookId(LegacyIds.REMOVED_LINES.get(0)));
                fp.getCooldowns().removeCooldown(old.getItem());
                if (BookUse.readSkillBook(fp, old) != BookUse.Result.REPLACED) continue;
                swaps++;
                for (ItemStack s : fp.getInventory().items) {
                    if (s.isEmpty() || !(s.getItem() instanceof SkillBookItem)) continue;
                    String bare = bareOfBook(s);
                    if (tiers && rarityOfBook(s) == BookRarity.MYTHIC) mythic++;
                    if (bare != null && BookFacts.isBlocked(bare)) blockedGiven++;
                }
            }
            if (mythic != 0) bad.add(mythic + " swapped books were mythic");
            if (blockedGiven != 0) bad.add(blockedGiven + " swapped books were of a blocked franchise");
            said.append(swaps).append(" swaps clean");
        } finally {
            fp.getAbilities().instabuild = creative;
            UniqueBooks.forget(fp, first.bare());
            fp.getInventory().clearContent();
        }
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    // ================================================================ skillbook.blocked
    private static SelfTests.Result blocked(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (RouteRegistry.all().isEmpty()) return SelfTests.Result.ok("no route data loaded (the blacklist was not driven)");
        Supplier<String> before = BlockedFranchises.source();
        StringBuilder said = new StringBuilder();
        FakePlayer fp = null;
        LineIndex.Line held = null;
        boolean creative = false;
        try {
            BlockedFranchises.setSource(() -> BlockedFranchises.DEFAULT);
            int blockedLines = 0;
            for (LineIndex.Line l : UniqueSkills.lines()) if (BookFacts.isBlocked(l.bare(), BlockedFranchises.DEFAULT)) blockedLines++;
            if (blockedLines == 0) {
                said.append("the shipped table has no line of the default blacklist (nothing to hide)");
                return SelfTests.Result.ok(said.toString());
            }

            // 1. the tables: zero weight for a blocked line, rebuilt once for the new setting, not per draw
            RouteTable.Snap snap = RouteTable.current();
            int builds = RouteTable.builds();
            for (int i = 0; i < snap.routes().length; i++) {
                if (BookFacts.isBlocked(snap.routes()[i].id()) && snap.table().weightOf(i) != 0) bad.add(snap.routes()[i].id() + " is blocked but still weighs " + snap.table().weightOf(i));
            }
            Random random = new Random(3);
            Dice dice = random::nextDouble;
            int hit = 0;
            for (int i = 0; i < 3000; i++) {
                SkillRoute r = snap.at(snap.table().pick(dice));
                if (r != null && BookFacts.isBlocked(r.id())) hit++;
                ItemStack b = LootDrops.tieredBook(BookRarity.fromRank(i % 4), dice, null);
                String bare = b.isEmpty() ? null : bareOfBook(b);
                if (bare != null && BookFacts.isBlocked(bare)) hit++;
                ItemStack rift = LootDrops.riftBook(dice, null);
                bare = rift.isEmpty() ? null : bareOfBook(rift);
                if (bare != null && BookFacts.isBlocked(bare)) hit++;
            }
            if (hit != 0) bad.add(hit + " picks of the weight tables, the monster tier and the rift named a blocked line");
            if (RouteTable.builds() != builds || RouteTable.current() != snap) bad.add("the table was rebuilt during the draws");
            said.append(blockedLines).append(" lines blocked, 9000 picks clean; ");

            // 2. the Guardian and the shop
            int guard = 0;
            for (String theme : BossThemes.themeIds()) {
                for (int floor : new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90}) {
                    for (BookRarity want : new BookRarity[] {BookRarity.RARE, BookRarity.EPIC}) {
                        ItemStack g = LootDrops.guardianBook(want, new LootVictims.Site(null, floor, theme), null);
                        String bare = g.isEmpty() ? null : bareOfBook(g);
                        if (bare != null && BookFacts.isBlocked(bare)) guard++;
                    }
                }
            }
            if (guard != 0) bad.add(guard + " Guardian picks named a blocked line");
            for (String id : ShopGoods.routeIds()) if (BookFacts.isBlocked(id)) bad.add("the shop stocks the blocked line " + id);
            said.append("Guardian and shop clean; ");

            // 3. a held book of a blocked line still reads
            for (LineIndex.Line l : UniqueSkills.lines()) {
                if (BookFacts.isBlocked(l.bare(), BlockedFranchises.DEFAULT) && RouteRegistry.byId(l.routeId()) != null) {
                    held = l;
                    break;
                }
            }
            ServerLevel level = server.overworld();
            fp = SelfTests.standIn(level, "blocked_reader", 0.5, 100, 0.5);
            if (held != null && io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(fp) != null) {
                creative = fp.getAbilities().instabuild;
                fp.getAbilities().instabuild = false;
                UniqueBooks.forget(fp, held.bare());
                ItemStack book = SkillBookItem.stackFor(held);
                if (BookUse.readSkillBook(fp, book) != BookUse.Result.LEARNED) bad.add("a held book of the blocked line " + held.bare() + " would not read");
                said.append("held ").append(held.bare()).append(" reads; ");
            } else {
                said.append("held-book read not driven; ");
            }
        } finally {
            if (fp != null && held != null) {
                fp.getAbilities().instabuild = creative;
                UniqueBooks.forget(fp, held.bare());
                fp.getInventory().clearContent();
            }
            BlockedFranchises.setSource(before);
        }

        // 4. putting the setting back restores the old table
        RouteTable.Snap back = RouteTable.current();
        String now = BlockedFranchises.setting();
        for (int i = 0; i < back.routes().length; i++) {
            SkillRoute r = back.routes()[i];
            boolean off = BookFacts.isBlocked(r.id(), now);
            if (!off && r.lootWeight() > 0 && back.table().weightOf(i) != r.lootWeight()) bad.add("after the setting was put back " + r.id() + " weighs " + back.table().weightOf(i) + ", not " + r.lootWeight());
        }
        said.append("setting restored");
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    private static LootParams chestParams(ServerLevel level, Vec3 origin, ServerPlayer opener) {
        LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).withOptionalParameter(LootContextParams.THIS_ENTITY, opener);
        return b.create(LootContextParamSets.CHEST);
    }

    private static LootContext context(ServerLevel level, Vec3 origin, ServerPlayer opener, RandomSource rs) {
        LootContext c = new LootContext.Builder(chestParams(level, origin, opener)).withOptionalRandomSource(rs).create(Optional.empty());
        c.setQueriedLootTableId(RELIC_VAULT);
        return c;
    }
}
