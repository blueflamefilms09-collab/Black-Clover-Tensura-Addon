package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

/**
 * Why: nothing in this package can be played by its author, so each rule of the Hub economy has a SELF-TEST that the CI server smoke test runs in the real game ({@code /multiverse mega selftest},
 * keys {@code elite.hub.*}), with a fake player standing in for a person, and each fails loudly with the reason. {@code wallet}: the coin wallet counts a mixed purse, charges a price taking the
 * small coins first and keeping the big one, hands change back, refuses a price it cannot pay without touching the purse, and pays an amount in the best mix. {@code stones}: the old-economy wallet
 * charges whole Magic Stones. {@code loot}: the coin stacks add up to the amount, a chest roll loses its Magic Stones, the currency seam suppresses the stone drops only in the Mega-Dungeon
 * and only in coin mode, and the Hub (floor 0) pays nothing. {@code shop}: the shipped shop loaded, a purchase moves the right coins and the right goods, a locked ware and a sold-out ware are
 * refused, a ware the player cannot afford is refused. {@code quests}: the shipped quests loaded, a quest runs accept, floor event, ready, hand-in and pays its coins exactly once. {@code npcs}: both
 * NPC types create, are invulnerable, never despawn and have their attributes. {@code map}: the first generated sector flattens to a map with rooms, doors that join them and a visited flag.
 * The tests use the same public entry points the event handlers use and leave the stand-in player as they found it.
 */
public final class HubSelfTests {
    private HubSelfTests() {}

    private static final String PLAYER = "elite_hub_tester";

    public static void register() {
        SelfTests.register("elite.hub.wallet", HubSelfTests::wallet);
        SelfTests.register("elite.hub.stones", HubSelfTests::stones);
        SelfTests.register("elite.hub.loot", HubSelfTests::loot);
        SelfTests.register("elite.hub.shop", HubSelfTests::shop);
        SelfTests.register("elite.hub.quests", HubSelfTests::quests);
        SelfTests.register("elite.hub.npcs", HubSelfTests::npcs);
        SelfTests.register("elite.hub.map", HubSelfTests::map);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static FakePlayer tester(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.standIn(level, PLAYER, 0.5, 80.0, 0.5);
        p.getInventory().clearContent();
        p.setData(HubAttachments.QUESTS, new PlayerQuestData());
        p.setData(HubAttachments.MAP, new MapMemory());
        return p;
    }

    private static void give(ServerPlayer p, CoinKind kind, int n) {
        for (ItemStack s : CoinWallet.stacks(kind, n)) p.getInventory().add(s);
    }

    private static long count(ServerPlayer p, Item item) {
        long n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(item)) n += s.getCount();
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------------ the wallet
    private static SelfTests.Result wallet(MinecraftServer server) {
        if (!HubConfig.enabled()) return ok("skipped: hub.enabled is false");
        if (!Currency.coins()) return ok("skipped: economy.currency is not tensura_coins");
        if (!CoinWallet.available()) return fail("a Tensura coin item is missing (" + CoinKind.GOLD.itemId() + " ...): is Tensura Reincarnated loaded?");
        FakePlayer p = tester(server);
        try {
            give(p, CoinKind.GOLD, 1);
            give(p, CoinKind.SILVER, 3);
            give(p, CoinKind.BRONZE, 7);
            if (Currency.balance(p) != 10_307L) return fail("balance of 1 gold + 3 silver + 7 bronze should be 10307, is " + Currency.balance(p));
            if (!Currency.charge(p, 250)) return fail("could not charge 250 from 10307");
            if (Currency.balance(p) != 10_057L) return fail("after a 250 charge the purse should hold 10057, holds " + Currency.balance(p));
            if (count(p, CoinWallet.item(CoinKind.GOLD)) != 1) return fail("the gold coin was spent although the small coins covered the price");
            if (Currency.charge(p, 20_000L)) return fail("a charge of 20000 from 10057 was accepted");
            if (Currency.balance(p) != 10_057L) return fail("a refused charge changed the purse");
            if (Currency.charge(p, -1)) return fail("a negative charge was accepted");
            Currency.give(p, 1_234_567L);
            if (Currency.balance(p) != 10_057L + 1_234_567L) return fail("give(1234567) left the purse at " + Currency.balance(p));
            if (count(p, CoinWallet.item(CoinKind.STELLAR_GOLD)) != 1) return fail("1234567 should pay one stellar gold coin");
            if (!Currency.charge(p, Currency.balance(p)) || Currency.balance(p) != 0) return fail("could not spend the whole purse");
            if (EliteHooks.wallet != Currency.HOOK) return fail("EliteHooks.wallet is not the Hub's wallet");
            return ok("balance, charge with change (small coins first), refusal, best-mix payment and the hook");
        } finally {
            p.getInventory().clearContent();
        }
    }

    private static SelfTests.Result stones(MinecraftServer server) {
        if (!HubConfig.enabled()) return ok("skipped: hub.enabled is false");
        Item stone = StoneWallet.item();
        if (stone == null) return fail(StoneWallet.STONE_ID + " does not exist: is Tensura Reincarnated loaded?");
        FakePlayer p = tester(server);
        try {
            StoneWallet w = new StoneWallet();
            long unit = StoneWallet.unit();
            p.getInventory().add(new ItemStack(stone, 5));
            if (w.balance(p) != 5 * unit) return fail("5 stones should be worth " + 5 * unit + ", are " + w.balance(p));
            if (!w.charge(p, 2 * unit + 1)) return fail("could not charge 2 stones and a bit");
            if (count(p, stone) != 2) return fail("a charge of 2 stones + 1 should take 3 whole stones, left " + count(p, stone));
            if (w.charge(p, 3 * unit)) return fail("a charge above the purse was accepted");
            w.give(p, unit + 1);
            if (count(p, stone) != 3) return fail("give(unit + 1) should add one whole stone, now " + count(p, stone));
            return ok("stones are counted, rounded up on a charge and paid whole");
        } finally {
            p.getInventory().clearContent();
        }
    }

    // ------------------------------------------------------------------------------------------------ the loot
    private static SelfTests.Result loot(MinecraftServer server) {
        if (!CoinWallet.available()) return fail("a Tensura coin item is missing");
        for (long amount : new long[] {1, 99, 100, 12_345, 1_010_203L}) {
            long sum = 0;
            for (ItemStack s : CoinWallet.stacksFor(amount)) sum += CoinKind.ofItemId(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).value() * s.getCount();
            if (sum != amount) return fail("the coin stacks for " + amount + " add up to " + sum);
        }
        ObjectArrayList<ItemStack> chest = new ObjectArrayList<>();
        Item stone = StoneWallet.item();
        if (stone != null) chest.add(new ItemStack(stone, 4));
        chest.add(new ItemStack(net.minecraft.world.item.Items.BREAD, 2));
        int removed = CoinLootModifier.stripStones(chest);
        if (stone != null && (removed != 1 || chest.size() != 1)) return fail("a chest roll should lose its Magic Stones and keep the bread; removed " + removed + ", left " + chest.size());
        ServerLevel mega = MegaDungeon.level(server);
        ServerLevel overworld = server.overworld();
        if (Currency.stonesSuppressed(overworld)) return fail("the stone drops are suppressed outside the Mega-Dungeon");
        if (mega != null && Currency.stonesSuppressed(mega) != Currency.stonesSuppressed()) return fail("the currency seam disagrees with itself in the Mega-Dungeon");
        if (CoinLoot.roll(CoinLoot.Source.BOSS, 0, 0, HubConfig.lootParams(), () -> 0.5) != 0) return fail("the Hub (floor 0) paid coins");
        long boss = CoinLoot.roll(CoinLoot.Source.BOSS, 10, 0, HubConfig.lootParams(), () -> 0.5);
        if (Currency.coins() && HubConfig.get(HubConfig.LOOT_SCALE) > 0.0 && HubConfig.get(HubConfig.LOOT_BOSS_BASE) > 0 && boss <= 0) return fail("a boss on floor 10 paid nothing");
        return ok("stacks add up, chest stones stripped, seam scoped to the Mega-Dungeon, Hub pays 0, boss on floor 10 pays " + CoinMath.describe(boss));
    }

    // ------------------------------------------------------------------------------------------------ the shop
    private static SelfTests.Result shop(MinecraftServer server) {
        if (!HubConfig.enabled() || !ShopService.on()) return ok("skipped: the shop is switched off");
        if (!Currency.coins()) return ok("skipped: economy.currency is not tensura_coins");
        if (!CoinWallet.available()) return fail("a Tensura coin item is missing");
        ShopTable shop = HubCatalogs.shop();
        if (shop.isEmpty()) return fail("the shop is empty: data/nusmp/elite/hub/shop.json did not load");
        if (shop.get("dungeon_map") == null || shop.get("bread") == null) return fail("the shipped shop lacks the Dungeon Map or the bread");
        FakePlayer p = tester(server);
        try {
            give(p, CoinKind.SILVER, 10);
            ShopTable.Offer bread = shop.get("bread");
            long price = ShopRules.price(bread, HubConfig.get(HubConfig.PRICE_SCALE));
            if (ShopService.purchase(p, "bread") != ShopRules.Verdict.OK) return fail("buying bread with 10 silver was refused");
            if (Currency.balance(p) != 1000 - price) return fail("bread should cost " + price + ", the purse went from 1000 to " + Currency.balance(p));
            if (count(p, net.minecraft.world.item.Items.BREAD) != bread.count()) return fail("the goods did not arrive: " + count(p, net.minecraft.world.item.Items.BREAD));
            if (ShopService.purchase(p, "nope") != ShopRules.Verdict.UNKNOWN_OFFER) return fail("an unknown offer was not refused");
            ShopTable.Offer locked = shop.offers().stream().filter(o -> o.minFloor() > 0).findFirst().orElse(null);
            if (locked != null && ShopService.purchase(p, locked.id()) != ShopRules.Verdict.LOCKED) return fail("a ware locked behind floor " + locked.minFloor() + " was sold to a newcomer");
            ShopTable.Offer limited = shop.offers().stream().filter(o -> o.dailyLimit() > 0 && o.minFloor() == 0).findFirst().orElse(null);
            if (limited != null) {
                p.getInventory().clearContent();
                give(p, CoinKind.GOLD, 50);
                for (int i = 0; i < limited.dailyLimit(); i++) {
                    if (ShopService.purchase(p, limited.id()) != ShopRules.Verdict.OK) return fail("stock " + (i + 1) + " of " + limited.dailyLimit() + " of " + limited.id() + " was refused");
                }
                if (ShopService.purchase(p, limited.id()) != ShopRules.Verdict.SOLD_OUT) return fail(limited.id() + " was sold past its daily limit of " + limited.dailyLimit());
            }
            p.getInventory().clearContent();
            if (ShopService.purchase(p, "dungeon_map") != ShopRules.Verdict.CANNOT_AFFORD) return fail("the Dungeon Map was sold to a player with no coins");
            return ok(shop.size() + " offers; bought bread for " + price + " bronze; unknown, locked, sold-out and unaffordable refused");
        } finally {
            p.getInventory().clearContent();
            p.setData(HubAttachments.QUESTS, new PlayerQuestData());
        }
    }

    // ------------------------------------------------------------------------------------------------ the quests
    private static SelfTests.Result quests(MinecraftServer server) {
        if (!QuestService.on()) return ok("skipped: the quests are switched off");
        QuestCatalog catalog = HubCatalogs.quests();
        if (catalog.size() < 30) return fail("only " + catalog.size() + " quests loaded (data/nusmp/elite/hub/quests/*.json)");
        FakePlayer p = tester(server);
        try {
            QuestDef reach = catalog.get("road_holy_reach_5");
            if (reach == null) return fail("the shipped quest road_holy_reach_5 is missing");
            if (QuestService.accept(p, reach) != PlayerQuestData.AcceptResult.OK) return fail("a newcomer could not accept road_holy_reach_5");
            if (QuestService.data(p).find(reach.id()) == null) return fail("the accepted quest is not in the player's data");
            if (QuestService.turnIn(p, reach) != 0) return fail("a quest was handed in before it was finished");
            QuestService.onFloorEntered(p, 3);
            if (QuestService.data(p).find(reach.id()).ready()) return fail("floor 3 finished a 'reach floor 5' quest");
            QuestService.onFloorEntered(p, 5);
            if (!QuestService.data(p).find(reach.id()).ready()) return fail("reaching floor 5 did not finish the quest");
            long before = Currency.balance(p);
            long paid = QuestService.turnIn(p, reach);
            long want = QuestRules.reward(reach, HubConfig.get(HubConfig.REWARD_SCALE));
            if (paid != want) return fail("the reward was " + paid + ", expected " + want);
            if (Currency.coins() && CoinWallet.available() && Currency.balance(p) - before != want) return fail("the purse rose by " + (Currency.balance(p) - before) + ", expected " + want);
            if (QuestService.turnIn(p, reach) != 0) return fail("the quest paid twice");
            if (QuestService.accept(p, reach) != PlayerQuestData.AcceptResult.ALREADY_DONE) return fail("a done progression quest could be accepted again");
            QuestDef next = catalog.get("road_holy_reach_10");
            if (next != null && QuestService.accept(p, next) != PlayerQuestData.AcceptResult.LOCKED && next.minFloor() > QuestService.data(p).maxFloor()) return fail("the next quest should be locked by its floor");
            List<QuestDef> board = QuestService.board(QuestService.data(p), QuestService.today(server));
            if (HubConfig.get(HubConfig.DAILY_COUNT) > 0 && board.isEmpty()) return fail("today's daily board is empty");
            if (QuestService.hud(p).size() != QuestService.data(p).active().size()) return fail("the overlay does not list the accepted quests");
            return ok(catalog.size() + " quests; a reach-floor quest accepted, finished, handed in once for " + want + " bronze; board of " + board.size());
        } finally {
            p.getInventory().clearContent();
            p.setData(HubAttachments.QUESTS, new PlayerQuestData());
        }
    }

    // ------------------------------------------------------------------------------------------------ the NPCs
    private static SelfTests.Result npcs(MinecraftServer server) {
        ServerLevel level = server.overworld();
        QuestGiverNPC q = HubNpcs.QUEST_GIVER.get().create(level);
        MerchantNPC m = HubNpcs.MERCHANT.get().create(level);
        try {
            if (q == null || m == null) return fail("an NPC entity type could not create its entity");
            for (HubNpcBase npc : List.of(q, m)) {
                if (npc.getAttribute(Attributes.MAX_HEALTH) == null) return fail(npc.getType().getDescriptionId() + " has no attributes");
                if (npc.removeWhenFarAway(1.0e6)) return fail(npc.getType().getDescriptionId() + " may despawn");
                if (!npc.isInvulnerableTo(level.damageSources().generic())) return fail(npc.getType().getDescriptionId() + " can be hurt");
                if (npc.isInvulnerableTo(level.damageSources().genericKill()) ) return fail(npc.getType().getDescriptionId() + " survives /kill and the void");
                if (!npc.isPersistenceRequired()) return fail(npc.getType().getDescriptionId() + " is not persistent");
                if (npc.isPushable()) return fail(npc.getType().getDescriptionId() + " can be pushed off its post");
            }
            DamageSource creeper = level.damageSources().explosion(null, null);
            if (!q.isInvulnerableTo(creeper)) return fail("an explosion can hurt the Quest Giver");
            return ok("both NPC types create, never despawn, are invulnerable except to /kill and the void, and stay put");
        } finally {
            if (q != null) q.discard();
            if (m != null) m.discard();
        }
    }

    // ------------------------------------------------------------------------------------------------ the map
    private static SelfTests.Result map(MinecraftServer server) {
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return fail("no mega dimension");
        Sector sector = null;
        for (Sector s : MegaData.get(server).sectors()) {
            if (s.state != Sector.State.FAILED && !s.rooms.isEmpty()) {
                sector = s;
                break;
            }
        }
        if (sector == null) return fail("no generated sector to map (the smoke script creates sector 0 first)");
        MapMemory memory = new MapMemory();
        int floor = sector.firstFloor();
        MapView v0 = HubMap.view(sector, floor, memory, 400);
        if (v0.rooms().isEmpty()) return fail("floor " + floor + " of sector " + sector.id + " mapped to no rooms (" + sector.rooms.size() + " rooms in the sector)");
        if (v0.visitedCount() != 0) return fail("a fresh memory marks rooms as visited");
        memory.visit(sector.id, v0.rooms().get(0).id());
        MapView v1 = HubMap.view(sector, floor, memory, 400);
        if (v1.visitedCount() != 1) return fail("a visited room is not marked");
        for (int[] e : v1.edges()) {
            boolean a = false, b = false;
            for (MapView.Room r : v1.rooms()) {
                a |= r.id() == e[0];
                b |= r.id() == e[1];
            }
            if (!a || !b) return fail("a door joins " + e[0] + " and " + e[1] + " but one of them is not on the map");
        }
        if (v1.edges().isEmpty() && v1.rooms().size() > 1) return fail("a map of " + v1.rooms().size() + " rooms has no doors");
        MapView capped = HubMap.view(sector, floor, memory, 3);
        if (capped.rooms().size() > 3) return fail("the room cap was ignored");
        return ok("sector " + sector.id + " floor " + floor + ": " + v1.rooms().size() + " rooms, " + v1.edges().size() + " doors, fog of war and cap hold");
    }
}
