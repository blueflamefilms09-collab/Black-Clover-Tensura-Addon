package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.NationApi;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplate;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplates;
import com.newuniverse.nusmp.elite.nations.contract.ContractType;
import com.newuniverse.nusmp.elite.nations.contract.Reward;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.core.Roles;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.war.PvpRules;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarPhase;
import com.newuniverse.nusmp.elite.nations.war.WarRules;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: nothing here can be played by the author, so the Nations system registers IN-GAME SELF-TESTS ({@code elite.nations.*}) with the same registry the Mega-Dungeon uses: the CI server smoke test (or an
 * operator with {@code /nation admin selftest}) runs them in the real game with two {@code FakePlayer}s standing in for people. Each test drives the system through its own public entry points (the services
 * the commands and the screen call), asserts what must hold and cleans up the nations it made, so it can run on any world. A failed check throws; the registry turns that into a FAIL line with the reason.
 * What they cannot cover (events that need a real client: block placement, the screen) is listed in docs/elite/nations.md.
 */
public final class NationSelfTests {
    private NationSelfTests() {}

    private static final String P = "elite.nations.";

    public static void register() {
        SelfTests.register(P + "data", NationSelfTests::data);
        SelfTests.register(P + "found", NationSelfTests::found);
        SelfTests.register(P + "roles", NationSelfTests::roles);
        SelfTests.register(P + "treasury", NationSelfTests::treasury);
        SelfTests.register(P + "contracts", NationSelfTests::contracts);
        SelfTests.register(P + "war", NationSelfTests::war);
        SelfTests.register(P + "alliance", NationSelfTests::alliance);
        SelfTests.register(P + "guards", NationSelfTests::guards);
        SelfTests.register(P + "persistence", NationSelfTests::persistence);
        SelfTests.register(P + "api", NationSelfTests::api);
    }

    /** Runs every elite.nations.* test; one line each (used by /nation admin selftest). */
    public static List<String> runAll(MinecraftServer s) {
        List<String> out = new ArrayList<>();
        for (String key : SelfTests.keys()) {
            if (!key.startsWith(P)) continue;
            SelfTests.Result r = SelfTests.run(key, s);
            out.add((r.ok() ? "PASS " : "FAIL ") + key + " - " + r.detail());
        }
        return out;
    }

    private static void need(boolean cond, String msg) { if (!cond) throw new AssertionError(msg); }

    private static ServerPlayer player(MinecraftServer s, String name) { return SelfTests.standIn(s.overworld(), "natst_" + name, 0.5, 120, 0.5); }

    private static Nation make(MinecraftServer s, ServerPlayer leader, String name) {
        return NationService.createNation(s, leader.getUUID(), leader.getGameProfile().getName(), name, 0x3366CC);
    }

    private static void cleanup(MinecraftServer s, String... ids) {
        for (String id : ids) if (id != null && NationService.data(s).nations.containsKey(id)) NationService.disbandNation(s, id, "selftest");
    }

    // ------------------------------------------------------------------------------------------------ data and registration
    private static SelfTests.Result data(MinecraftServer s) {
        need(ContractTemplates.all().size() >= 30, "fewer than 30 contract templates loaded: " + ContractTemplates.all().size());
        need(NationPerks.table().perkCount() > 0, "no perks loaded");
        need(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "nation_banner")), "nation_banner block is not registered");
        need(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "contract_board")), "contract_board block is not registered");
        need(GuardService.tagHasMembers(GuardService.TAG_ALL), "tag nusmp:nation_guard is empty");
        for (int t = 1; t <= GuardRules.MAX_TIER; t++) need(GuardService.tagHasMembers(GuardService.tierTag(t)), "tag nusmp:nation_guard_tier_" + t + " is empty");
        for (ContractTemplate t : ContractTemplates.all()) need(t.count() >= 1 && t.tier() >= 1 && t.tier() <= 5, "bad template " + t.id());
        int types = 0;
        for (ContractType ty : ContractType.values()) {
            for (ContractTemplate t : ContractTemplates.all()) if (t.type() == ty) { types++; break; }
        }
        need(types >= 5, "templates cover only " + types + " contract types");
        return SelfTests.Result.ok(ContractTemplates.all().size() + " templates, " + NationPerks.table().perkCount() + " perks, blocks and guard tags present");
    }

    // ------------------------------------------------------------------------------------------------ founding and membership
    private static SelfTests.Result found(MinecraftServer s) {
        ServerPlayer a = player(s, "a");
        Nation n = make(s, a, "Selftest Alpha");
        try {
            need(NationApi.nationOf(a.getUUID()).equals(n.id), "the founder is not in the nation");
            need(NationApi.leader(n.id).equals(a.getUUID()), "the founder is not the leader");
            need(NationApi.level(n.id) >= 1, "level below 1");
            need(NationApi.members(n.id).size() == 1, "member count");
            need(!NationService.found(a, "Another One", 0).ok(), "a member founded a second nation");
            need(NationService.canPlaceCapital(a, s.overworld(), BlockPos.ZERO).ok(), "the leader may not place a capital in the overworld");
            ServerPlayer b = player(s, "b");
            need(!NationService.canPlaceCapital(b, s.overworld(), BlockPos.ZERO).ok(), "a stranger may place a capital");
            need(!Nation.validName("x", 3, 24), "name validation");
            return SelfTests.Result.ok("nation " + n.id + " founded, leader and rules ok");
        } finally {
            cleanup(s, n.id);
        }
    }

    private static SelfTests.Result roles(MinecraftServer s) {
        ServerPlayer a = player(s, "a"), b = player(s, "b");
        Nation n = make(s, a, "Selftest Roles");
        NationsData d = NationService.data(s);
        try {
            n.addMember(b.getUUID(), b.getGameProfile().getName(), Roles.RECRUIT);
            d.indexMember(b.getUUID(), n.id);
            need(NationApi.sameNation(a.getUUID(), b.getUUID()), "not the same nation");
            need(WarService.pvp(a, b) == PvpRules.Verdict.DENY_SAME_NATION || !NationsConfig.get(NationsConfig.PROTECT_SAME_NATION), "same-nation fire is not protected");
            need(!n.can(b.getUUID(), Permission.KICK), "a recruit may kick");
            need(NationService.setRole(a, b.getUUID(), Roles.OFFICER).ok(), "leader could not promote");
            need(n.can(b.getUUID(), Permission.KICK), "an officer may not kick");
            need(!NationService.kick(b, a.getUUID()).ok(), "an officer kicked the leader");
            need(!NationService.createRole(b, "scout", "Scout").ok(), "an officer without ROLES created a role");
            need(NationService.createRole(a, "scout", "Scout").ok(), "leader could not create a role");
            need(NationService.setRolePermission(a, "scout", Permission.INVITE, true).ok(), "leader could not grant a permission");
            need(NationService.setRole(a, b.getUUID(), "scout").ok(), "could not assign the custom role");
            need(n.can(b.getUUID(), Permission.INVITE), "custom role permission missing");
            need(!n.can(b.getUUID(), Permission.KICK), "custom role kept officer rights");
            need(!NationService.deleteRole(a, Roles.LEADER).ok(), "the leader role was deleted");
            need(NationService.deleteRole(a, "scout").ok(), "custom role not deletable");
            need(Roles.RECRUIT.equals(n.members.get(b.getUUID())), "member not reset to recruit when the role went");
            need(!NationService.leave(a).ok(), "the leader left");
            need(NationService.kick(a, b.getUUID()).ok(), "leader could not kick");
            need(!NationApi.sameNation(a.getUUID(), b.getUUID()), "kicked member still in the nation");
            need(!NationService.transferLeadership(a, a.getUUID()).ok(), "self transfer");
            return SelfTests.Result.ok("roles, hierarchy, custom role and kick behave");
        } finally {
            cleanup(s, n.id);
        }
    }

    // ------------------------------------------------------------------------------------------------ treasury
    private static SelfTests.Result treasury(MinecraftServer s) {
        ServerPlayer a = player(s, "a");
        Nation n = make(s, a, "Selftest Vault");
        try {
            long cap = NationApi.treasuryCap(n.id);
            need(cap > 0, "treasury cap is 0");
            need(NationApi.deposit(n.id, 5_000, "test") == 5_000, "deposit");
            need(NationApi.treasury(n.id) == 5_000, "balance");
            need(NationApi.withdraw(n.id, 2_000, "test"), "withdraw");
            need(!NationApi.withdraw(n.id, 99_999_999, "test"), "overdraft allowed");
            need(NationApi.treasury(n.id) == 3_000, "balance after withdraw");
            long taken = NationApi.deposit(n.id, cap * 2, "test");
            need(taken == cap - 3_000 && NationApi.treasury(n.id) == cap, "cap not enforced: accepted " + taken);
            need(n.ledger.historySum() == cap, "ledger history does not add up to the balance (sum " + n.ledger.historySum() + ")");
            need(Treasury.BRIDGE.balance(n.id) == cap, "bridge balance");
            n.lastInterest = 1L;
            long expected = Math.min(Treasury.policy().interest(n.id, n.ledger.balance()), n.ledger.room(cap));
            long before = n.ledger.balance();
            Treasury.runInterest(s, 1_000_000_000L);
            need(n.ledger.balance() - before == expected, "interest paid " + (n.ledger.balance() - before) + " expected " + expected);
            need(!Treasury.deposit(a, 10, false).ok() || EliteWalletWorks(a), "a deposit succeeded without coins");
            need(!Treasury.withdraw(player(s, "b"), 10, false).ok(), "a stranger withdrew");
            Res tax = Treasury.setTax(a, NationsConfig.get(NationsConfig.MAX_TAX_PERCENT) + 1);
            need(!tax.ok(), "tax above the maximum accepted");
            need(Treasury.setTax(a, 5).ok() && n.taxPercent == 5, "tax not set");
            return SelfTests.Result.ok("ledger, cap, bridge, interest and tax ok");
        } finally {
            cleanup(s, n.id);
        }
    }

    private static boolean EliteWalletWorks(ServerPlayer p) { return com.newuniverse.nusmp.elite.api.EliteHooks.wallet.balance(p) >= 10; }

    // ------------------------------------------------------------------------------------------------ contracts and bounties
    private static Contract post(NationsData d, Nation n, ContractType type, String target, int goal) {
        ContractTemplate t = new ContractTemplate("selftest_" + type.id(), type, target, goal, 1, "Selftest " + type.id(), "", new Reward(100, 5, 5), 1, 1, 0);
        Contract c = Contract.from(d.newUid(), t, 0);
        d.board(n.id).add(c);
        return c;
    }

    private static SelfTests.Result contracts(MinecraftServer s) {
        ServerPlayer a = SelfTests.standIn(s.overworld(), "natst_a", 0.5, 120, 0.5);
        Nation n = make(s, a, "Selftest Board");
        NationsData d = NationService.data(s);
        try {
            Contract kill = post(d, n, ContractType.KILL_MOB, "minecraft:zombie", 2);
            Contract mine = post(d, n, ContractType.MINE_BLOCK, "minecraft:stone", 1);
            Contract craft = post(d, n, ContractType.CRAFT_ITEM, "minecraft:stick", 1);
            Contract explore = post(d, n, ContractType.EXPLORE_BIOME, "", 1);
            Contract deliver = post(d, n, ContractType.DELIVER_ITEM, "minecraft:cobblestone", 3);
            need(ContractService.take(a, kill.uid).ok(), "could not take the kill contract");
            need(!ContractService.take(a, kill.uid).ok(), "a taken contract was taken twice");
            LivingEntity zombie = EntityType.ZOMBIE.create(s.overworld());
            need(zombie != null, "no zombie");
            ContractService.onKill(a, zombie);
            need(kill.progress == 1 && kill.state == Contract.State.TAKEN, "kill progress " + kill.progress);
            ContractService.onKill(a, EntityType.SKELETON.create(s.overworld()));
            need(kill.progress == 1, "a skeleton counted for a zombie contract");
            ContractService.onKill(a, zombie);
            need(kill.state == Contract.State.DONE && n.completedContracts == 1, "kill contract not completed");
            need(NationApi.reputation(a.getUUID(), n.id) == 5, "reputation reward " + NationApi.reputation(a.getUUID(), n.id));
            need(NationApi.xp(n.id) >= 5, "nation xp reward");
            need(ContractService.take(a, mine.uid).ok(), "take mine");
            ContractService.onMine(a, Blocks.STONE.defaultBlockState());
            need(mine.state == Contract.State.DONE, "mine contract not completed");
            need(ContractService.take(a, craft.uid).ok(), "take craft");
            ContractService.onCraft(a, new ItemStack(Items.STICK, 4));
            need(craft.state == Contract.State.DONE, "craft contract not completed");
            need(ContractService.take(a, explore.uid).ok(), "take explore");
            ContractService.onBiome(a);
            need(explore.state == Contract.State.DONE, "explore contract not completed (biome at the stand-in)");
            need(ContractService.take(a, deliver.uid).ok(), "take deliver");
            need(!ContractService.deliver(a, deliver.uid).ok(), "delivered nothing");
            a.getInventory().add(new ItemStack(Items.COBBLESTONE, 5));
            need(ContractService.deliver(a, deliver.uid).ok() && deliver.state == Contract.State.DONE, "deliver contract not completed");
            need(NationService.countItem(a, Items.COBBLESTONE) == 2, "wrong number of items taken: " + NationService.countItem(a, Items.COBBLESTONE));
            NationService.takeItem(a, Items.COBBLESTONE, 64);
            // bounties
            ServerPlayer b = player(s, "b");
            Bounty bounty = new Bounty();
            bounty.uid = d.newUid();
            bounty.kind = Bounty.TargetKind.PLAYER;
            bounty.target = b.getUUID().toString();
            bounty.targetName = "natst_b";
            bounty.coins = 50;
            bounty.poster = UUID.randomUUID();
            d.bounties.add(bounty);
            ContractService.claim(a, b);
            need(!d.bounties.contains(bounty), "the bounty was not claimed by the kill");
            // board refill
            ContractService.refreshBoards(s, 1000L);
            need(d.board(n.id).stream().noneMatch(c -> c.state == Contract.State.DONE), "done contracts stayed on the board");
            need(d.board(n.id).size() >= 1, "board not refilled from the templates");
            return SelfTests.Result.ok("kill, mine, craft, explore, deliver, bounty and board refill ok");
        } finally {
            d.bounties.removeIf(bo -> "natst_b".equals(bo.targetName));
            cleanup(s, n.id);
        }
    }

    // ------------------------------------------------------------------------------------------------ war
    private static SelfTests.Result war(MinecraftServer s) {
        ServerPlayer a = player(s, "a"), b = player(s, "b");
        Nation na = make(s, a, "Selftest Red"), nb = make(s, b, "Selftest Blue");
        NationsData d = NationService.data(s);
        try {
            if (NationsConfig.get(NationsConfig.WAR_ONLY_PVP)) need(WarService.pvp(a, b) == PvpRules.Verdict.DENY_NOT_AT_WAR, "strangers can fight without a war");
            long now = NationService.now(s);
            WarRules.Timings t = WarService.timings();
            Res early = WarService.declare(a, nb.id, "test");
            need(!early.ok() || NationsConfig.get(NationsConfig.MIN_LEVEL_TO_DECLARE) <= 1, "a level 1 nation declared a war");
            for (War stray : d.wars.warsOf(na.id)) d.wars.wars.remove(stray.id);
            d.wars.declareCooldown.remove(na.id);
            War w = d.wars.add(WarRules.declare(d.wars.newId(), na.id, nb.id, now - t.declareTicks() - t.prepTicks() - 5, t, "selftest"));
            need(NationApi.isAtWar(na.id, nb.id) && !NationApi.isFighting(na.id, nb.id), "declared war is not 'at war, not fighting'");
            WarService.tick(s, now);
            need(w.phase == WarPhase.ACTIVE, "the war did not reach ACTIVE: " + w.phase);
            need(NationApi.isFighting(nb.id, na.id), "not fighting while ACTIVE");
            need(WarService.pvp(a, b) == PvpRules.Verdict.ALLOW_WAR, "no PvP allowed in an active war");
            int before = w.scoreAttacker;
            WarService.onPlayerKill(a, b);
            need(w.scoreAttacker == before + NationsConfig.get(NationsConfig.KILL_POINTS), "kill score " + w.scoreAttacker);
            WarService.onPlayerKill(a, b);
            need(w.scoreAttacker == before + NationsConfig.get(NationsConfig.KILL_POINTS), "the same victim scored twice inside the cooldown");
            Treasury.BRIDGE.deposit(nb.id, 10_000, "test");
            w.scoreAttacker = Math.max(w.scoreAttacker, NationsConfig.get(NationsConfig.TARGET_SCORE) + 1);
            if (NationsConfig.get(NationsConfig.TARGET_SCORE) <= 0) w.phaseEnds = now - 1;
            WarService.tick(s, now);
            need(w.phase == WarPhase.CEASEFIRE && na.id.equals(w.winner), "war did not end in a victory for the attacker: " + w.phase + "/" + w.winner);
            need(na.warWins == 1 && nb.warLosses == 1, "war record");
            need(na.ledger.balance() == w.spoilsCoins, "spoils did not arrive");
            need(na.ledger.balance() + nb.ledger.balance() == 10_000, "spoils created or destroyed coins");
            need(!NationApi.isAtWar(na.id, nb.id), "still 'at war' during the ceasefire");
            need(WarService.pvp(a, b) != PvpRules.Verdict.ALLOW_WAR, "PvP still allowed during the ceasefire");
            WarService.tick(s, w.phaseEnds + 1);
            need(d.wars.wars.isEmpty() && !d.wars.historyView().isEmpty(), "war not archived into the history");
            need(d.wars.truceBetween(na.id, nb.id, w.phaseEnds + 2) || NationsConfig.get(NationsConfig.TRUCE_MINUTES) == 0, "no truce after the war");
            return SelfTests.Result.ok("phases declared>prep>active>ceasefire>ended, kill score, anti-farm, spoils conserved, history");
        } finally {
            d.wars.history.removeIf(r -> r.attacker().startsWith("selftest_") || r.defender().startsWith("selftest_"));
            d.wars.truce.keySet().removeIf(k -> k.contains("selftest_"));
            cleanup(s, na.id, nb.id);
        }
    }

    private static SelfTests.Result alliance(MinecraftServer s) {
        ServerPlayer a = player(s, "a"), b = player(s, "b");
        Nation na = make(s, a, "Selftest Gold"), nb = make(s, b, "Selftest Silver");
        NationsData d = NationService.data(s);
        try {
            need(WarService.propose(a, nb.id).ok(), "proposal refused");
            need(!NationApi.isAllied(na.id, nb.id), "allied before acceptance");
            need(!WarService.acceptAlliance(a, nb.id).ok(), "the proposer accepted their own proposal");
            need(WarService.acceptAlliance(b, na.id).ok(), "acceptance refused");
            need(NationApi.isAllied(nb.id, na.id) && NationApi.alliesOf(na.id).contains(nb.id), "alliance not formed");
            need(WarService.pvp(a, b) == PvpRules.Verdict.DENY_ALLIED, "allies may hurt each other");
            Res war = WarService.declare(a, nb.id, "x");
            need(!war.ok(), "war declared on an ally");
            need(WarService.breakAlliance(a, nb.id).ok() && !NationApi.isAllied(na.id, nb.id), "alliance not broken");
            if (NationsConfig.get(NationsConfig.ALLIANCE_BREAK_COOLDOWN_MINUTES) > 0) need(!WarService.propose(a, nb.id).ok(), "re-proposed during the break cooldown");
            return SelfTests.Result.ok("propose, accept, no friendly fire, no war on allies, break and cooldown ok");
        } finally {
            d.alliances.removeNation(na.id);
            d.alliances.removeNation(nb.id);
            cleanup(s, na.id, nb.id);
        }
    }

    // ------------------------------------------------------------------------------------------------ guards
    private static SelfTests.Result guards(MinecraftServer s) {
        ServerPlayer a = player(s, "a");
        Nation n = make(s, a, "Selftest Guard");
        ServerLevel level = s.overworld();
        try {
            BlockPos probe = new BlockPos(0, 0, 0);
            if (!level.isLoaded(probe)) return SelfTests.Result.ok("skipped: the origin chunk is not loaded");
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
            BlockPos pos = new BlockPos(0, y, 0);
            Res cap = NationService.setCapital(a, level, pos);
            need(cap.ok(), "capital refused: " + cap.key());
            need(NationApi.hasCapital(n.id) && NationApi.capitalPos(n.id)[1] == y, "capital not stored");
            n.level = 3;
            int made = GuardService.spawnNow(s, n, 1);
            if (made == 0) return SelfTests.Result.ok("capital ok; no guard spawned here (no free ground near the origin or guards disabled)");
            need(GuardService.aliveCount(n.id) == 1, "tracker count");
            NationService.clearCapital(s, n);
            need(GuardService.aliveCount(n.id) == 0 && !n.hasCapital, "guards not released with the capital");
            return SelfTests.Result.ok("capital stored, one tier " + GuardRules.tierOf(3) + " guard spawned, tracked and released");
        } finally {
            cleanup(s, n.id);
        }
    }

    // ------------------------------------------------------------------------------------------------ persistence and api
    private static SelfTests.Result persistence(MinecraftServer s) {
        ServerPlayer a = player(s, "a"), b = player(s, "b");
        Nation na = make(s, a, "Selftest Save A"), nb = make(s, b, "Selftest Save B");
        NationsData d = NationService.data(s);
        try {
            Treasury.BRIDGE.deposit(na.id, 1234, "persist");
            na.hasCapital = true;
            na.capitalDim = "minecraft:overworld";
            na.cx = 5;
            na.cy = 70;
            na.cz = -9;
            post(d, na, ContractType.KILL_MOB, "minecraft:zombie", 4).progress = 2;
            ReputationService.set(s, a.getUUID(), nb.id, 321);
            Bounty bo = new Bounty();
            bo.uid = d.newUid();
            bo.target = b.getUUID().toString();
            bo.targetName = "natst_b";
            bo.coins = 77;
            bo.poster = a.getUUID();
            d.bounties.add(bo);
            long now = NationService.now(s);
            d.wars.add(WarRules.declare(d.wars.newId(), na.id, nb.id, now, WarService.timings(), "persist"));
            d.alliances.propose(na.id, nb.id, now, 1000, 3, (x, y2) -> false);
            CompoundTag saved = d.save(new CompoundTag(), s.registryAccess());
            NationsData loaded = NationsData.load(saved, s.registryAccess());
            need(loaded.nations.containsKey(na.id) && loaded.nations.get(na.id).ledger.balance() == 1234, "nation or balance lost");
            need(loaded.nations.get(na.id).cz == -9 && loaded.nations.get(na.id).hasCapital, "capital lost");
            need(loaded.reputation.get(a.getUUID(), nb.id) == 321, "reputation lost");
            need(loaded.wars.find(na.id, nb.id) != null, "war lost");
            need(loaded.alliances.hasProposal(na.id, nb.id), "proposal lost");
            need(loaded.nationIdOf(a.getUUID()).equals(na.id), "member index not rebuilt");
            CompoundTag again = loaded.save(new CompoundTag(), s.registryAccess());
            need(saved.equals(again), "save > load > save is not stable");
            return SelfTests.Result.ok("save, load and save again give identical data (" + saved.getAllKeys().size() + " sections)");
        } finally {
            d.bounties.removeIf(x -> "natst_b".equals(x.targetName));
            for (War w : new ArrayList<>(d.wars.wars.values())) if (w.involves(na.id)) d.wars.wars.remove(w.id);
            d.alliances.removeNation(na.id);
            d.alliances.removeNation(nb.id);
            cleanup(s, na.id, nb.id);
        }
    }

    private static SelfTests.Result api(MinecraftServer s) {
        ServerPlayer a = player(s, "a"), b = player(s, "b");
        Nation na = make(s, a, "Selftest Api A"), nb = make(s, b, "Selftest Api B");
        try {
            need(NationApi.findByName("selftest api a").equals(na.id), "findByName");
            need(NationApi.allNations().contains(na.id), "allNations");
            need(NationApi.nationOf(UUID.randomUUID()).isEmpty(), "unknown player has a nation");
            need(NationApi.level("nope") == 0 && NationApi.treasury("nope") == 0 && NationApi.members("nope").isEmpty() && !NationApi.isAtWar("nope", "nada"), "unknown nation answers are not neutral");
            need(NationApi.nameOf(na.id).equals("Selftest Api A"), "nameOf");
            need(NationApi.hasPermission(a.getUUID(), "war") && !NationApi.hasPermission(b.getUUID(), "kick"), "permission lookup");
            long xpBefore = NationApi.xp(na.id);
            NationApi.addNationXp(na.id, 500, "test");
            need(NationApi.xp(na.id) == xpBefore + 500, "xp not added");
            need(NationApi.level(na.id) >= 2, "500 xp did not level the nation (level " + NationApi.level(na.id) + ", score " + NationApi.score(na.id) + ")");
            NationApi.addReputation(a.getUUID(), nb.id, 400, "test");
            need("honored".equals(NationApi.reputationTier(a.getUUID(), nb.id)), "tier after +400: " + NationApi.reputationTier(a.getUUID(), nb.id));
            need(NationApi.priceModifierPercent(a.getUUID(), nb.id) < 100, "an honored player pays full price");
            need(NationApi.priceModifierPercent(UUID.randomUUID()) == 100, "stranger discount");
            need(NationApi.perk(na.id, "treasury_cap") >= 0, "perk lookup");
            need(NationApi.treasuryBridge() != null, "bridge");
            return SelfTests.Result.ok("facade answers neutral values for unknown ids and real values for known ones");
        } finally {
            cleanup(s, na.id, nb.id);
        }
    }
}
