package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.contract.ContractType;
import com.newuniverse.nusmp.elite.nations.contract.Reward;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Role;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.war.AllianceBook;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarBook;
import com.newuniverse.nusmp.elite.nations.war.WarPhase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the on-disk format of the Nations data, kept apart from the pure model classes so those stay free of Minecraft types. Everything is written with plain keys and read leniently (a missing key is a
 * default, an unknown enum ordinal is skipped) so an older or hand-edited save never stops the server from loading. Enum values are saved by NAME, never by ordinal, so reordering is safe.
 */
public final class NationNbt {
    private NationNbt() {}

    private static <E extends Enum<E>> E enumOf(Class<E> cls, String name, E fallback) {
        if (name == null) return fallback;
        try { return Enum.valueOf(cls, name); } catch (IllegalArgumentException e) { return fallback; }
    }

    private static UUID uuid(CompoundTag t, String key) { return t.hasUUID(key) ? t.getUUID(key) : null; }

    // ---------------------------------------------------------------- nation
    public static CompoundTag write(Nation n) {
        CompoundTag t = new CompoundTag();
        t.putString("Id", n.id);
        t.putString("Name", n.name);
        t.putInt("Color", n.color);
        t.putUUID("Leader", n.leader);
        t.putLong("Created", n.createdAt);
        t.putString("CapDim", n.capitalDim);
        t.putInt("Cx", n.cx);
        t.putInt("Cy", n.cy);
        t.putInt("Cz", n.cz);
        t.putBoolean("HasCap", n.hasCapital);
        t.putLong("Xp", n.xp);
        t.putInt("Level", n.level);
        t.putInt("Peak", n.peakLevel);
        t.putInt("Done", n.completedContracts);
        t.putInt("Wins", n.warWins);
        t.putInt("Losses", n.warLosses);
        t.putInt("Area", n.areaScore);
        t.putInt("Tax", n.taxPercent);
        t.putLong("LastInterest", n.lastInterest);
        ListTag members = new ListTag();
        for (Map.Entry<UUID, String> e : n.members.entrySet()) {
            CompoundTag m = new CompoundTag();
            m.putUUID("U", e.getKey());
            m.putString("R", e.getValue());
            m.putString("N", n.names.getOrDefault(e.getKey(), ""));
            members.add(m);
        }
        t.put("Members", members);
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
        t.putLong("Balance", n.ledger.balance());
        ListTag led = new ListTag();
        for (Ledger.Entry e : n.ledger.all()) {
            CompoundTag c = new CompoundTag();
            c.putLong("T", e.time());
            c.putString("K", e.kind().name());
            if (e.actor() != null) c.putUUID("A", e.actor());
            c.putString("Note", e.note());
            c.putLong("V", e.amount());
            c.putLong("B", e.balanceAfter());
            led.add(c);
        }
        t.put("Ledger", led);
        return t;
    }

    public static Nation readNation(CompoundTag t, int ledgerCap) {
        if (!t.hasUUID("Leader") || t.getString("Id").isEmpty()) return null;
        Nation n = new Nation(t.getString("Id"), t.getString("Name"), t.getInt("Color"), t.getUUID("Leader"), null, t.getLong("Created"), ledgerCap);
        n.members.clear();
        n.names.clear();
        n.roles.clear();
        n.capitalDim = t.getString("CapDim");
        n.cx = t.getInt("Cx");
        n.cy = t.getInt("Cy");
        n.cz = t.getInt("Cz");
        n.hasCapital = t.getBoolean("HasCap");
        n.xp = t.getLong("Xp");
        n.level = Math.max(1, t.getInt("Level"));
        n.peakLevel = Math.max(n.level, t.getInt("Peak"));
        n.completedContracts = t.getInt("Done");
        n.warWins = t.getInt("Wins");
        n.warLosses = t.getInt("Losses");
        n.areaScore = t.getInt("Area");
        n.taxPercent = t.getInt("Tax");
        n.lastInterest = t.getLong("LastInterest");
        for (Tag e : t.getList("Roles", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            if (!c.getString("Id").isEmpty()) n.roles.put(c.getString("Id"), new Role(c.getString("Id"), c.getString("Name"), c.getInt("Rank"), c.getInt("Mask")));
        }
        if (n.roles.isEmpty()) for (Role r : com.newuniverse.nusmp.elite.nations.core.Roles.defaults()) n.roles.put(r.id(), r);
        for (Tag e : t.getList("Members", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) e;
            if (!m.hasUUID("U")) continue;
            UUID id = m.getUUID("U");
            n.members.put(id, m.getString("R"));
            if (!m.getString("N").isEmpty()) n.names.put(id, m.getString("N"));
        }
        if (!n.members.containsKey(n.leader)) n.members.put(n.leader, com.newuniverse.nusmp.elite.nations.core.Roles.LEADER);
        List<Ledger.Entry> hist = new ArrayList<>();
        for (Tag e : t.getList("Ledger", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            hist.add(new Ledger.Entry(c.getLong("T"), enumOf(Ledger.Kind.class, c.getString("K"), Ledger.Kind.ADMIN), uuid(c, "A"), c.getString("Note"), c.getLong("V"), c.getLong("B")));
        }
        n.ledger.restore(t.getLong("Balance"), hist);
        return n;
    }

    // ---------------------------------------------------------------- contracts
    public static CompoundTag write(Contract c) {
        CompoundTag t = new CompoundTag();
        t.putLong("Uid", c.uid);
        t.putString("Tpl", c.templateId);
        t.putString("Type", c.type.name());
        t.putString("Target", c.target);
        t.putInt("Goal", c.goal);
        t.putInt("Prog", c.progress);
        t.putInt("Tier", c.tier);
        t.putLong("Coins", c.reward.coins());
        t.putLong("Xp", c.reward.nationXp());
        t.putInt("Rep", c.reward.reputation());
        t.putString("Title", c.title);
        t.putString("State", c.state.name());
        if (c.taker != null) t.putUUID("Taker", c.taker);
        t.putLong("Created", c.createdAt);
        t.putLong("Deadline", c.deadline);
        ListTag seen = new ListTag();
        for (String s : c.seen) seen.add(net.minecraft.nbt.StringTag.valueOf(s));
        t.put("Seen", seen);
        return t;
    }

    public static Contract readContract(CompoundTag t) {
        Contract c = new Contract();
        c.uid = t.getLong("Uid");
        c.templateId = t.getString("Tpl");
        c.type = enumOf(ContractType.class, t.getString("Type"), ContractType.KILL_MOB);
        c.target = t.getString("Target");
        c.goal = Math.max(1, t.getInt("Goal"));
        c.progress = Math.max(0, t.getInt("Prog"));
        c.tier = Math.max(1, t.getInt("Tier"));
        c.reward = new Reward(t.getLong("Coins"), t.getLong("Xp"), t.getInt("Rep"));
        c.title = t.getString("Title");
        c.state = enumOf(Contract.State.class, t.getString("State"), Contract.State.OPEN);
        c.taker = uuid(t, "Taker");
        c.createdAt = t.getLong("Created");
        c.deadline = t.getLong("Deadline");
        for (Tag s : t.getList("Seen", Tag.TAG_STRING)) c.seen.add(s.getAsString());
        if (c.state == Contract.State.TAKEN && c.taker == null) c.state = Contract.State.OPEN;
        return c;
    }

    public static CompoundTag write(Bounty b) {
        CompoundTag t = new CompoundTag();
        t.putLong("Uid", b.uid);
        t.putString("Kind", b.kind.name());
        t.putString("Target", b.target);
        t.putString("TName", b.targetName);
        t.putLong("Coins", b.coins);
        t.putString("Source", b.source.name());
        if (b.poster != null) t.putUUID("Poster", b.poster);
        t.putString("PNation", b.posterNation);
        t.putLong("Created", b.createdAt);
        t.putLong("Expires", b.expires);
        return t;
    }

    public static Bounty readBounty(CompoundTag t) {
        Bounty b = new Bounty();
        b.uid = t.getLong("Uid");
        b.kind = enumOf(Bounty.TargetKind.class, t.getString("Kind"), Bounty.TargetKind.PLAYER);
        b.target = t.getString("Target");
        b.targetName = t.getString("TName");
        b.coins = t.getLong("Coins");
        b.source = enumOf(Bounty.Source.class, t.getString("Source"), Bounty.Source.PLAYER);
        b.poster = uuid(t, "Poster");
        b.posterNation = t.getString("PNation");
        b.createdAt = t.getLong("Created");
        b.expires = t.getLong("Expires");
        return b;
    }

    // ---------------------------------------------------------------- wars
    public static CompoundTag write(War w) {
        CompoundTag t = new CompoundTag();
        t.putLong("Id", w.id);
        t.putString("A", w.attacker);
        t.putString("D", w.defender);
        t.putString("Phase", w.phase.name());
        t.putLong("Declared", w.declaredAt);
        t.putLong("Ends", w.phaseEnds);
        t.putLong("Since", w.activeSince);
        t.putInt("SA", w.scoreAttacker);
        t.putInt("SD", w.scoreDefender);
        t.putInt("KA", w.killsAttacker);
        t.putInt("KD", w.killsDefender);
        t.putInt("CA", w.capturesAttacker);
        t.putInt("CD", w.capturesDefender);
        t.putInt("GA", w.siegeAttacker);
        t.putInt("GD", w.siegeDefender);
        t.putString("Outcome", w.outcome.name());
        t.putString("Winner", w.winner);
        t.putString("CeaseFrom", w.ceasefireFrom);
        t.putString("Reason", w.reason);
        t.putLong("Spoils", w.spoilsCoins);
        CompoundTag lk = new CompoundTag();
        w.lastKill.forEach(lk::putLong);
        t.put("LastKill", lk);
        return t;
    }

    public static War readWar(CompoundTag t) {
        if (t.getString("A").isEmpty() || t.getString("D").isEmpty()) return null;
        War w = new War(t.getLong("Id"), t.getString("A"), t.getString("D"));
        w.phase = enumOf(WarPhase.class, t.getString("Phase"), WarPhase.ENDED);
        w.declaredAt = t.getLong("Declared");
        w.phaseEnds = t.getLong("Ends");
        w.activeSince = t.getLong("Since");
        w.scoreAttacker = t.getInt("SA");
        w.scoreDefender = t.getInt("SD");
        w.killsAttacker = t.getInt("KA");
        w.killsDefender = t.getInt("KD");
        w.capturesAttacker = t.getInt("CA");
        w.capturesDefender = t.getInt("CD");
        w.siegeAttacker = t.getInt("GA");
        w.siegeDefender = t.getInt("GD");
        w.outcome = enumOf(War.Outcome.class, t.getString("Outcome"), War.Outcome.NONE);
        w.winner = t.getString("Winner");
        w.ceasefireFrom = t.getString("CeaseFrom");
        w.reason = t.getString("Reason");
        w.spoilsCoins = t.getLong("Spoils");
        CompoundTag lk = t.getCompound("LastKill");
        for (String k : lk.getAllKeys()) w.lastKill.put(k, lk.getLong(k));
        return w;
    }

    public static CompoundTag write(WarBook.Record r) {
        CompoundTag t = new CompoundTag();
        t.putLong("Id", r.id());
        t.putString("A", r.attacker());
        t.putString("D", r.defender());
        t.putLong("Declared", r.declaredAt());
        t.putLong("Ended", r.endedAt());
        t.putString("Outcome", r.outcome().name());
        t.putString("Winner", r.winner());
        t.putInt("SA", r.scoreAttacker());
        t.putInt("SD", r.scoreDefender());
        t.putLong("Spoils", r.spoils());
        t.putString("Reason", r.reason());
        return t;
    }

    public static WarBook.Record readRecord(CompoundTag t) {
        return new WarBook.Record(t.getLong("Id"), t.getString("A"), t.getString("D"), t.getLong("Declared"), t.getLong("Ended"),
                enumOf(War.Outcome.class, t.getString("Outcome"), War.Outcome.NONE), t.getString("Winner"), t.getInt("SA"), t.getInt("SD"), t.getLong("Spoils"), t.getString("Reason"));
    }

    public static CompoundTag writeLongMap(Map<String, Long> m) {
        CompoundTag t = new CompoundTag();
        m.forEach(t::putLong);
        return t;
    }

    public static void readLongMap(CompoundTag t, Map<String, Long> into) {
        for (String k : t.getAllKeys()) into.put(k, t.getLong(k));
    }

    public static CompoundTag write(AllianceBook a) {
        CompoundTag t = new CompoundTag();
        t.put("Allied", writeLongMap(a.allied));
        t.put("Proposals", writeLongMap(a.proposals));
        t.put("Cooldown", writeLongMap(a.cooldown));
        return t;
    }

    public static void read(CompoundTag t, AllianceBook a) {
        readLongMap(t.getCompound("Allied"), a.allied);
        readLongMap(t.getCompound("Proposals"), a.proposals);
        readLongMap(t.getCompound("Cooldown"), a.cooldown);
    }
}
