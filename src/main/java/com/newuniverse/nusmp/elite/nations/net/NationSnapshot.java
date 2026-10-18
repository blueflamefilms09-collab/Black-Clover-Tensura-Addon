package com.newuniverse.nusmp.elite.nations.net;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.NationLevels;
import com.newuniverse.nusmp.elite.nations.core.Perk;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Role;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.econ.UbsBridge;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarBook;
import com.newuniverse.nusmp.elite.nations.world.NationPerks;
import com.newuniverse.nusmp.elite.nations.world.NationService;
import com.newuniverse.nusmp.elite.nations.world.NationsConfig;
import com.newuniverse.nusmp.elite.nations.world.NationsData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: builds the compact view of the nations that one player's screen shows (see {@link SnapshotPayload}). Everything is plain display values with short keys, and every list is capped, so the packet
 * stays small however large the server is. Only the player's own nation is detailed; for the others the client gets a one-line directory. The server decides what is visible: a non-member sees no treasury, no
 * members, no board. Server thread only.
 */
public final class NationSnapshot {
    private NationSnapshot() {}

    public static final int MAX_NATIONS_LISTED = 64, MAX_LEDGER = 30, MAX_BOUNTIES = 40, MAX_HISTORY = 10;

    public static CompoundTag build(MinecraftServer s, ServerPlayer p) {
        NationsData d = NationService.data(s);
        long now = NationService.now(s);
        CompoundTag root = new CompoundTag();
        root.putLong("Now", now);
        root.putBoolean("On", NationsConfig.enabled());
        root.putBoolean("War", NationsConfig.warEnabled());
        root.putLong("Wallet", EliteHooks.wallet.balance(p));
        root.putBoolean("Ubs", UbsBridge.available());
        Nation mine = d.nationOf(p.getUUID());
        root.putString("Mine", mine == null ? "" : mine.id);
        if (mine != null) root.put("N", own(s, d, mine, p, now));
        // directory
        ListTag all = new ListTag();
        int listed = 0;
        for (Nation n : d.nations.values()) {
            if (listed++ >= MAX_NATIONS_LISTED) break;
            CompoundTag c = new CompoundTag();
            c.putString("Id", n.id);
            c.putString("Name", n.name);
            c.putInt("Color", n.color);
            c.putInt("Lv", n.level);
            c.putInt("Mem", n.memberCount());
            if (mine != null && !mine.id.equals(n.id)) {
                c.putBoolean("Ally", d.alliances.areAllied(mine.id, n.id));
                War w = d.wars.find(mine.id, n.id);
                if (w != null) c.putString("Phase", w.phase.id());
                c.putBoolean("PropIn", d.alliances.hasProposal(n.id, mine.id));
                c.putBoolean("PropOut", d.alliances.hasProposal(mine.id, n.id));
            }
            Long exp = n.invites.get(p.getUUID());
            if (exp != null && now < exp) c.putBoolean("Invited", true);
            all.add(c);
        }
        root.put("All", all);
        // bounties
        ListTag bl = new ListTag();
        int bc = 0;
        for (Bounty b : d.bounties) {
            if (bc++ >= MAX_BOUNTIES) break;
            CompoundTag c = new CompoundTag();
            c.putLong("Uid", b.uid);
            c.putString("Kind", b.kind.name());
            c.putString("Name", b.targetName);
            c.putLong("Coins", b.coins);
            c.putString("Src", b.source.name());
            c.putBoolean("Mine", p.getUUID().equals(b.poster));
            bl.add(c);
        }
        root.put("Bounties", bl);
        // reputation
        ListTag rep = new ListTag();
        for (Map.Entry<String, Integer> e : d.reputation.of(p.getUUID()).entrySet()) {
            Nation n = d.nations.get(e.getKey());
            if (n == null) continue;
            CompoundTag c = new CompoundTag();
            c.putString("Id", n.id);
            c.putString("Name", n.name);
            c.putInt("V", e.getValue());
            c.putString("Tier", ReputationTier.of(e.getValue()).id());
            rep.add(c);
        }
        root.put("Rep", rep);
        return root;
    }

    private static CompoundTag own(MinecraftServer s, NationsData d, Nation n, ServerPlayer p, long now) {
        CompoundTag t = new CompoundTag();
        t.putString("Id", n.id);
        t.putString("Name", n.name);
        t.putInt("Color", n.color);
        t.putInt("Lv", n.level);
        t.putInt("Peak", n.peakLevel);
        t.putLong("Xp", n.xp);
        double score = NationService.score(n);
        long[] th = NationService.thresholds();
        t.putInt("Score", (int) Math.min(Integer.MAX_VALUE, Math.round(score)));
        t.putInt("Missing", (int) Math.min(Integer.MAX_VALUE, NationLevels.missingForNext(score, th)));
        t.putInt("Prog", (int) Math.round(NationLevels.progress(score, th) * 1000));
        t.putInt("MaxLv", th.length + 1);
        t.putString("Leader", n.nameOf(n.leader));
        t.putInt("Tax", n.taxPercent);
        t.putInt("MaxTax", NationsConfig.get(NationsConfig.MAX_TAX_PERCENT));
        t.putLong("Bal", n.ledger.balance());
        t.putLong("Cap", NationPerks.treasuryCap(n));
        t.putInt("MemCap", NationPerks.memberCap(n));
        t.putBoolean("HasCap", n.hasCapital);
        if (n.hasCapital) {
            t.putString("Dim", n.capitalDim);
            t.putInt("Cx", n.cx);
            t.putInt("Cy", n.cy);
            t.putInt("Cz", n.cz);
        }
        t.putInt("Done", n.completedContracts);
        t.putInt("Wins", n.warWins);
        t.putInt("Losses", n.warLosses);
        t.putInt("Area", n.areaScore);
        Role mr = n.roleOf(p.getUUID());
        t.putString("Role", mr == null ? "" : mr.id());
        int mask = 0;
        for (Permission perm : Permission.values()) if (n.can(p.getUUID(), perm)) mask |= perm.bit();
        t.putInt("Perms", mask);
        t.putBoolean("IsLeader", n.leader.equals(p.getUUID()));
        // members
        ListTag mem = new ListTag();
        for (Map.Entry<UUID, String> e : n.members.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("N", n.nameOf(e.getKey()));
            c.putString("R", e.getValue());
            c.putBoolean("On", s.getPlayerList().getPlayer(e.getKey()) != null);
            mem.add(c);
        }
        t.put("Members", mem);
        ListTag roles = new ListTag();
        for (Role r : n.roles.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", r.id());
            c.putString("Name", r.name());
            c.putInt("Rank", r.rank());
            c.putInt("Mask", r.permMask());
            roles.add(c);
        }
        t.put("Roles", roles);
        // ledger
        ListTag led = new ListTag();
        for (Ledger.Entry e : n.ledger.recent(MAX_LEDGER)) {
            CompoundTag c = new CompoundTag();
            c.putLong("T", e.time());
            c.putString("K", e.kind().name());
            c.putString("A", e.actor() == null ? "" : n.nameOf(e.actor()));
            c.putString("Note", e.note().length() > 60 ? e.note().substring(0, 60) : e.note());
            c.putLong("V", e.amount());
            c.putLong("B", e.balanceAfter());
            led.add(c);
        }
        t.put("Ledger", led);
        // perks: active, then what the next level adds
        ListTag perks = new ListTag();
        for (Perk perk : NationPerks.table().upTo(n.level)) perks.add(perk(perk, false));
        for (Perk perk : NationPerks.table().at(n.level + 1)) perks.add(perk(perk, true));
        t.put("Perks", perks);
        // board
        ListTag board = new ListTag();
        List<Contract> bd = d.boards.get(n.id);
        if (bd != null) {
            for (Contract c : bd) {
                CompoundTag e = new CompoundTag();
                e.putLong("Uid", c.uid);
                e.putString("Title", c.title);
                e.putString("Type", c.type.name());
                e.putString("Target", c.target);
                e.putInt("Goal", c.goal);
                e.putInt("Prog", c.progress);
                e.putInt("Tier", c.tier);
                e.putLong("Coins", c.reward.coins());
                e.putLong("Xp", c.reward.nationXp());
                e.putInt("Rep", c.reward.reputation());
                e.putString("State", c.state.name());
                e.putBoolean("Mine", c.takenBy(p.getUUID()));
                e.putString("Taker", c.taker == null ? "" : n.nameOf(c.taker));
                e.putLong("Left", c.deadline > 0 ? Math.max(0L, c.deadline - now) : -1L);
                board.add(e);
            }
        }
        t.put("Board", board);
        // wars
        ListTag wars = new ListTag();
        for (War w : d.wars.warsOf(n.id)) {
            Nation o = d.nations.get(w.other(n.id));
            CompoundTag c = new CompoundTag();
            c.putLong("Id", w.id);
            c.putString("Other", w.other(n.id));
            c.putString("OtherName", o == null ? w.other(n.id) : o.name);
            c.putString("Phase", w.phase.id());
            c.putBoolean("Attacker", w.attacker.equals(n.id));
            c.putLong("Ends", Math.max(0L, w.phaseEnds - now));
            c.putInt("Me", w.score(n.id));
            c.putInt("Them", w.score(w.other(n.id)));
            c.putString("CeaseFrom", w.ceasefireFrom);
            wars.add(c);
        }
        t.put("Wars", wars);
        ListTag hist = new ListTag();
        for (WarBook.Record r : d.wars.historyOf(n.id, MAX_HISTORY)) {
            CompoundTag c = new CompoundTag();
            Nation a = d.nations.get(r.attacker()), b = d.nations.get(r.defender());
            c.putString("A", a == null ? r.attacker() : a.name);
            c.putString("D", b == null ? r.defender() : b.name);
            c.putString("Outcome", r.outcome().name());
            c.putString("Winner", r.winner());
            c.putInt("SA", r.scoreAttacker());
            c.putInt("SD", r.scoreDefender());
            c.putLong("Spoils", r.spoils());
            hist.add(c);
        }
        t.put("History", hist);
        return t;
    }

    private static CompoundTag perk(Perk p, boolean next) {
        CompoundTag c = new CompoundTag();
        c.putString("Type", p.type());
        c.putString("Arg", p.arg());
        c.putDouble("V", p.value());
        c.putBoolean("Next", next);
        return c;
    }
}
