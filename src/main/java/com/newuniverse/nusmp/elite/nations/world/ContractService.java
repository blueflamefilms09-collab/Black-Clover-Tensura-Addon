package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.contract.ContractRules;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplate;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplates;
import com.newuniverse.nusmp.elite.nations.contract.ContractType;
import com.newuniverse.nusmp.elite.nations.contract.Reward;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.event.BountyClaimedEvent;
import com.newuniverse.nusmp.elite.nations.event.ContractCompletedEvent;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Why: contracts and bounties ("Contracts & Missions: contract types, bounty posting (Contract Bounties), mission tiers, reward structures"). The boards of the nations are refilled from the templates on a timer;
 * a member with the CONTRACTS permission takes one (tier-gated by level and by their reputation), game events advance it ({@link #onKill}, {@link #onMine}, {@link #onCraft}, {@link #onBiome}) or the player
 * hands items in ({@link #deliver}), and completion pays coins (through the wallet hook, minus the nation's tax which goes to the treasury), nation XP and reputation, and fires ContractCompletedEvent.
 * Bounties are escrowed coins on a player or a mob type that the killer collects. The pure rules are {@link ContractRules}. Server thread only.
 */
public final class ContractService {
    private ContractService() {}

    private static final Random RND = new Random();

    // ---------------------------------------------------------------- the board
    /** Expires old contracts and refills every nation's board up to its size. */
    public static void refreshBoards(MinecraftServer s, long now) {
        NationsData d = NationService.data(s);
        List<ContractTemplate> all = ContractTemplates.all();
        for (Nation n : d.nations.values()) {
            List<Contract> board = d.board(n.id);
            boolean changed = false;
            for (Iterator<Contract> it = board.iterator(); it.hasNext(); ) {
                Contract c = it.next();
                if (c.state == Contract.State.DONE || c.state == Contract.State.EXPIRED) { it.remove(); changed = true; }
                else if (c.expired(now)) {
                    ServerPlayer taker = c.taker == null ? null : s.getPlayerList().getPlayer(c.taker);
                    if (taker != null) Msg.tell(taker, "nusmp.nation.m.contract_expired", c.title);
                    it.remove();
                    changed = true;
                }
            }
            if (board.removeIf(c -> c.state == Contract.State.TAKEN && !n.isMember(c.taker))) changed = true;   // the taker left the nation
            int size = ContractRules.boardSize(NationsConfig.get(NationsConfig.BOARD_BASE_SLOTS), n.level, NationPerks.boardSlots(n));
            if (board.size() < size && !all.isEmpty()) {
                Set<String> on = new HashSet<>();
                for (Contract c : board) on.add(c.templateId);
                for (ContractTemplate t : ContractRules.pick(all, on, n.level, size - board.size(), RND)) {
                    Contract c = Contract.from(d.newUid(), t, now);
                    c.reward = ContractRules.scaleForLevel(t.reward(), n.level, t.minNationLevel());
                    if (c.title.isEmpty()) c.title = ContractRules.autoTitle(c.type, c.target, c.goal);
                    board.add(c);
                    changed = true;
                }
            }
            if (changed) d.touch(n.id);
        }
    }

    public static Contract find(NationsData d, Nation n, long uid) {
        for (Contract c : d.board(n.id)) if (c.uid == uid) return c;
        return null;
    }

    public static Res take(ServerPlayer p, long uid) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!n.can(p.getUUID(), Permission.CONTRACTS)) return Res.fail("nusmp.nation.m.no_permission", Permission.CONTRACTS.id());
        Contract c = find(d, n, uid);
        if (c == null || c.state != Contract.State.OPEN) return Res.fail("nusmp.nation.m.contract_gone");
        ReputationTier tier = ReputationService.tier(s, p.getUUID(), n.id);
        if (!tier.mayUseBoard) return Res.fail("nusmp.nation.m.rep_too_low", Msg.of("nusmp.nation.tier." + tier.id()));
        if (!ContractRules.mayTake(c.tier, Math.max(tier.maxContractTier, tier.mayUseBoard ? 1 : 0))) return Res.fail("nusmp.nation.m.tier_locked", c.tier, tier.maxContractTier);
        if (!ContractRules.underLimit(d.board(n.id), p.getUUID(), NationsConfig.get(NationsConfig.MAX_TAKEN_PER_PLAYER))) return Res.fail("nusmp.nation.m.too_many_contracts", NationsConfig.get(NationsConfig.MAX_TAKEN_PER_PLAYER));
        c.state = Contract.State.TAKEN;
        c.taker = p.getUUID();
        c.progress = 0;
        c.seen.clear();
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.contract_taken", c.title);
    }

    public static Res abandon(ServerPlayer p, long uid) {
        NationsData d = NationService.data(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        Contract c = find(d, n, uid);
        if (c == null || !c.takenBy(p.getUUID())) return Res.fail("nusmp.nation.m.contract_gone");
        c.state = Contract.State.OPEN;
        c.taker = null;
        c.progress = 0;
        c.seen.clear();
        d.touch(n.id);
        return Res.ok("nusmp.nation.m.contract_abandoned", c.title);
    }

    /** Hands items in for a deliver contract. */
    public static Res deliver(ServerPlayer p, long uid) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
        Contract c = find(d, n, uid);
        if (c == null || !c.takenBy(p.getUUID())) return Res.fail("nusmp.nation.m.contract_gone");
        if (c.type != ContractType.DELIVER_ITEM) return Res.fail("nusmp.nation.m.not_deliver");
        Predicate<ItemStack> match = itemMatcher(c.target);
        int carried = 0;
        for (ItemStack st : p.getInventory().items) if (!st.isEmpty() && match.test(st)) carried += st.getCount();
        int take = ContractRules.deliverable(c, carried);
        if (take <= 0) return Res.fail("nusmp.nation.m.nothing_to_deliver");
        int left = take;
        for (ItemStack st : p.getInventory().items) {
            if (left <= 0) break;
            if (!st.isEmpty() && match.test(st)) { int t = Math.min(left, st.getCount()); st.shrink(t); left -= t; }
        }
        ContractRules.applyDelivery(c, take);
        d.touch(n.id);
        if (c.complete()) complete(s, n, p, c);
        return Res.ok("nusmp.nation.m.delivered", take, c.title);
    }

    static Predicate<ItemStack> itemMatcher(String target) {
        if (target == null || target.isEmpty()) return st -> true;
        if (target.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(target.substring(1));
            if (rl == null) return st -> false;
            TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, rl);
            return st -> st.is(tag);
        }
        return st -> BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(target);
    }

    // ---------------------------------------------------------------- completion
    static void complete(MinecraftServer s, Nation n, ServerPlayer p, Contract c) {
        NationsData d = NationService.data(s);
        c.state = Contract.State.DONE;
        d.board(n.id).remove(c);
        d.lastBoardRefresh = 0L;   // refill the slot at the next loop run
        Reward r = c.reward.withBonusPercent(NationPerks.contractBonusPercent(n));
        long[] split = ContractRules.splitTax(r.coins(), n.taxPercent);
        if (split[0] > 0) EliteHooks.wallet.give(p, split[0]);
        if (split[1] > 0) Treasury.credit(s, n, Ledger.Kind.TAX, p.getUUID(), split[1], "contract tax: " + c.title);
        n.completedContracts++;
        NationService.addXp(s, n, r.nationXp(), "contract");
        ReputationService.add(s, p.getUUID(), n.id, r.reputation(), "contract");
        d.touch(n.id);
        Msg.tell(p, "nusmp.nation.m.contract_done", c.title, split[0], r.nationXp(), r.reputation());
        Treasury.post(new ContractCompletedEvent(p.getUUID(), n.id, c.uid, c.templateId, c.tier, split[0], r.nationXp(), r.reputation()));
    }

    // ---------------------------------------------------------------- progress from game events
    private static void progress(ServerPlayer p, ContractType type, String key, int amount, Predicate<String> tagTest) {
        if (!NationsConfig.enabled() || p == null) return;
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation n = d.nationOf(p.getUUID());
        if (n == null) return;
        List<Contract> board = d.boards.get(n.id);
        if (board == null || board.isEmpty()) return;
        for (Contract c : new ArrayList<>(board)) {
            if (!c.takenBy(p.getUUID())) continue;
            int gained = ContractRules.advance(c, type, key, amount, tagTest);
            if (gained <= 0) continue;
            d.touch(n.id);
            if (c.complete()) complete(s, n, p, c);
            else if (c.progress % Math.max(1, c.goal / 4) == 0) p.displayClientMessage(Msg.of("nusmp.nation.m.contract_progress", c.title, c.progress, c.goal), true);
        }
    }

    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof ServerPlayer) return;
        EntityType<?> type = victim.getType();
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        progress(killer, ContractType.KILL_MOB, key, 1, tag -> {
            ResourceLocation rl = ResourceLocation.tryParse(tag);
            return rl != null && type.is(TagKey.create(Registries.ENTITY_TYPE, rl));
        });
    }

    public static void onMine(ServerPlayer p, BlockState state) {
        String key = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        progress(p, ContractType.MINE_BLOCK, key, 1, tag -> {
            ResourceLocation rl = ResourceLocation.tryParse(tag);
            return rl != null && state.is(TagKey.create(Registries.BLOCK, rl));
        });
    }

    public static void onCraft(ServerPlayer p, ItemStack crafted) {
        if (crafted.isEmpty()) return;
        String key = BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
        progress(p, ContractType.CRAFT_ITEM, key, crafted.getCount(), tag -> {
            ResourceLocation rl = ResourceLocation.tryParse(tag);
            return rl != null && crafted.is(TagKey.create(Registries.ITEM, rl));
        });
    }

    public static void onBiome(ServerPlayer p) {
        var holder = p.level().getBiome(p.blockPosition());
        String key = holder.unwrapKey().map(k -> k.location().toString()).orElse(null);
        if (key == null) return;
        progress(p, ContractType.EXPLORE_BIOME, key, 1, tag -> {
            ResourceLocation rl = ResourceLocation.tryParse(tag);
            return rl != null && holder.is(TagKey.create(Registries.BIOME, rl));
        });
    }

    // ---------------------------------------------------------------- bounties
    public static int bountiesOf(NationsData d, UUID poster) {
        int n = 0;
        for (Bounty b : d.bounties) if (poster.equals(b.poster)) n++;
        return n;
    }

    /** A player posts a bounty on a player or a mob type, paying coins + fee from the wallet (or, as the nation, from the treasury). */
    public static Res post(ServerPlayer poster, Bounty.TargetKind kind, String target, String targetName, long coins, boolean asNation) {
        MinecraftServer s = poster.getServer();
        if (!NationsConfig.enabled()) return Res.fail("nusmp.nation.m.disabled");
        NationsData d = NationService.data(s);
        long min = NationsConfig.get(NationsConfig.BOUNTY_MIN);
        if (coins < min) return Res.fail("nusmp.nation.m.bounty_min", min);
        if (bountiesOf(d, poster.getUUID()) >= NationsConfig.get(NationsConfig.BOUNTY_MAX_PER_PLAYER)) return Res.fail("nusmp.nation.m.too_many_bounties", NationsConfig.get(NationsConfig.BOUNTY_MAX_PER_PLAYER));
        if (kind == Bounty.TargetKind.PLAYER && target.equals(poster.getUUID().toString())) return Res.fail("nusmp.nation.m.cannot_target_self");
        if (kind == Bounty.TargetKind.MOB) {
            ResourceLocation rl = ResourceLocation.tryParse(target);
            if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl) || rl.equals(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER))) return Res.fail("nusmp.nation.m.bad_target");
        }
        long total = Bounty.totalWithFee(coins, NationsConfig.get(NationsConfig.BOUNTY_FEE_PERCENT));
        Nation n = d.nationOf(poster.getUUID());
        Bounty b = new Bounty();
        b.kind = kind;
        b.target = target;
        b.targetName = targetName;
        b.coins = coins;
        b.poster = poster.getUUID();
        b.createdAt = NationService.now(s);
        long ttl = NationsConfig.get(NationsConfig.BOUNTY_TTL_HOURS) * 60L * 60L * 20L;
        b.expires = ttl > 0 ? b.createdAt + ttl : 0L;
        if (asNation) {
            if (n == null) return Res.fail("nusmp.nation.m.not_in_nation");
            if (!n.can(poster.getUUID(), Permission.TREASURY)) return Res.fail("nusmp.nation.m.no_permission", Permission.TREASURY.id());
            if (Treasury.debit(s, n, Ledger.Kind.FEE, poster.getUUID(), total, "bounty on " + targetName) <= 0) return Res.fail("nusmp.nation.m.treasury_short", n.ledger.balance());
            b.source = Bounty.Source.NATION;
            b.posterNation = n.id;
        } else {
            if (!EliteHooks.wallet.charge(poster, total)) return Res.fail("nusmp.nation.m.no_coins", total);
            b.source = Bounty.Source.PLAYER;
            b.posterNation = n == null ? "" : n.id;
        }
        b.uid = d.newUid();
        d.bounties.add(b);
        d.setDirty();
        Msg.everyone(s, "nusmp.nation.m.bounty_posted", targetName, coins);
        return Res.ok("nusmp.nation.m.bounty_ok", targetName, coins);
    }

    /** The war system's own bounty: paid from a nation's treasury, no fee. */
    public static boolean postWarBounty(MinecraftServer s, Nation payer, UUID target, String targetName, long coins) {
        if (coins <= 0 || Treasury.debit(s, payer, Ledger.Kind.FEE, null, coins, "war bounty on " + targetName) <= 0) return false;
        NationsData d = NationService.data(s);
        Bounty b = new Bounty();
        b.uid = d.newUid();
        b.kind = Bounty.TargetKind.PLAYER;
        b.target = target.toString();
        b.targetName = targetName;
        b.coins = coins;
        b.source = Bounty.Source.WAR;
        b.posterNation = payer.id;
        b.createdAt = NationService.now(s);
        long ttl = NationsConfig.get(NationsConfig.BOUNTY_TTL_HOURS) * 60L * 60L * 20L;
        b.expires = ttl > 0 ? b.createdAt + ttl : 0L;
        d.bounties.add(b);
        d.setDirty();
        return true;
    }

    /** A kill: pay every bounty the victim carried. */
    public static void claim(ServerPlayer killer, LivingEntity victim) {
        if (!NationsConfig.enabled()) return;
        MinecraftServer s = killer.getServer();
        NationsData d = NationService.data(s);
        if (d.bounties.isEmpty()) return;
        boolean isPlayer = victim instanceof ServerPlayer;
        String key = isPlayer ? victim.getUUID().toString() : BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        String killerNation = d.nationIdOf(killer.getUUID());
        long total = 0;
        for (Iterator<Bounty> it = d.bounties.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            if (!b.claimableBy(killer.getUUID(), killerNation, isPlayer, key)) continue;
            it.remove();
            total += b.coins;
            if (!b.posterNation.isEmpty() && d.nations.containsKey(b.posterNation)) ReputationService.add(s, killer.getUUID(), b.posterNation, NationsConfig.get(NationsConfig.REP_BOUNTY_KILL), "bounty");
            Treasury.post(new BountyClaimedEvent(killer.getUUID(), key, b.coins, b.posterNation));
        }
        if (total > 0) {
            EliteHooks.wallet.give(killer, total);
            d.setDirty();
            Msg.tell(killer, "nusmp.nation.m.bounty_claimed", total);
        }
    }

    /** Lapsed bounties return their escrow to the poster (or the nation). One whose poster is offline waits for them. */
    public static void expireBounties(MinecraftServer s, long now) {
        NationsData d = NationService.data(s);
        for (Iterator<Bounty> it = d.bounties.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            if (!b.expired(now)) continue;
            if (b.source != Bounty.Source.PLAYER) {
                Nation n = d.nations.get(b.posterNation);
                if (n != null) Treasury.credit(s, n, Ledger.Kind.REWARD, null, b.coins, "bounty lapsed");
                it.remove();
                d.setDirty();
                continue;
            }
            ServerPlayer p = b.poster == null ? null : s.getPlayerList().getPlayer(b.poster);
            if (p == null) continue;
            EliteHooks.wallet.give(p, b.coins);
            Msg.tell(p, "nusmp.nation.m.bounty_lapsed", b.targetName, b.coins);
            it.remove();
            d.setDirty();
        }
    }

    /** Cancels one of your own bounties: the escrow comes back (the fee does not). */
    public static Res cancel(ServerPlayer p, long uid) {
        NationsData d = NationService.data(p.getServer());
        for (Iterator<Bounty> it = d.bounties.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            if (b.uid != uid) continue;
            if (!p.getUUID().equals(b.poster)) return Res.fail("nusmp.nation.m.not_your_bounty");
            if (b.source != Bounty.Source.PLAYER) {
                Nation n = d.nations.get(b.posterNation);
                if (n != null) Treasury.credit(p.getServer(), n, Ledger.Kind.REWARD, p.getUUID(), b.coins, "bounty cancelled");
            } else {
                EliteHooks.wallet.give(p, b.coins);
            }
            it.remove();
            d.setDirty();
            return Res.ok("nusmp.nation.m.bounty_cancelled", b.targetName);
        }
        return Res.fail("nusmp.nation.m.no_such_bounty");
    }
}
