package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.RecordsNet.Prog;
import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.Describe;
import com.newuniverse.nusmp.elite.records.rule.Engine;
import com.newuniverse.nusmp.elite.records.rule.Eval;
import com.newuniverse.nusmp.elite.records.rule.Facts;
import com.newuniverse.nusmp.elite.records.rule.FactsBuilder;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.Requirement;
import com.newuniverse.nusmp.elite.records.rule.SynergyMatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Why: builds the packets of {@link RecordsNet} from the catalog and one player's data, and sends them. Definitions are sent in chunks of at most 40 records / 30 titles (the first chunk replaces,
 * so a data reload or a hidden record being revealed simply re-sends the set); a HIDDEN record the player has not unlocked is left out entirely, so a modified client cannot read what it is. The
 * state carries the progress of every visible locked record and of every requirement of every title not yet held, computed from the same {@link Facts} the unlock loop uses, so the bars on the
 * screen can never disagree with what the server will accept. Server thread only.
 */
final class RecordsSync {
    private RecordsSync() {}

    private static final int RECORD_CHUNK = 40, TITLE_CHUNK = 30;

    static void resyncAll(MinecraftServer server) {
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            sendDefs(p);
            sendState(p, null);
        }
    }

    /** False for a stand-in player (a self-test's fake player has no connection): nothing can be sent to it. */
    static boolean wired(ServerPlayer p) { return p != null && p.connection != null; }

    static void sendDefs(ServerPlayer p) {
        if (!wired(p)) return;
        Catalog cat = RecordsCatalog.get();
        RecordData d = RecordsAttachments.of(p);
        List<RecordsNet.RecordInfo> recs = new ArrayList<>();
        for (RecordDef r : cat.records()) {
            if (r.hidden() && !d.isUnlocked(r.id())) continue;
            recs.add(new RecordsNet.RecordInfo(r.id(), r.name(), r.description(), r.category().ordinal(), r.rarity().ordinal(), r.hidden()));
        }
        List<RecordsNet.TitleInfo> tits = new ArrayList<>();
        for (TitleDef t : cat.titles()) {
            List<String> reqs = new ArrayList<>();
            for (Requirement q : t.requirements()) reqs.add(Describe.requirement(q, cat));
            tits.add(new RecordsNet.TitleInfo(t.id(), t.name(), t.description(), t.rarity().ordinal(), new ArrayList<>(t.tags()), reqs, Describe.bonusLines(t.bonuses())));
        }
        List<RecordsNet.SynergyInfo> syns = new ArrayList<>();
        for (SynergyDef s : cat.synergies()) {
            List<String> names = new ArrayList<>();
            for (String id : s.requires()) {
                TitleDef t = cat.title(id);
                names.add(t == null ? id : t.name());
            }
            syns.add(new RecordsNet.SynergyInfo(s.id(), s.name(), s.description(), names, s.tag(), s.tagCount(), Describe.bonusLines(s.bonuses())));
        }
        boolean first = true;
        int ri = 0, ti = 0;
        do {
            List<RecordsNet.RecordInfo> rc = recs.subList(ri, Math.min(recs.size(), ri + RECORD_CHUNK));
            List<RecordsNet.TitleInfo> tc = tits.subList(ti, Math.min(tits.size(), ti + TITLE_CHUNK));
            ri += rc.size();
            ti += tc.size();
            boolean last = ri >= recs.size() && ti >= tits.size();
            PacketDistributor.sendToPlayer(p, new RecordsNet.Defs(first, new ArrayList<>(rc), new ArrayList<>(tc), last ? syns : List.of()));
            first = false;
            if (last) break;
        } while (true);
    }

    /** Sends the state; {@code facts} may be null (then they are built from the saved data). */
    static void sendState(ServerPlayer p, Facts facts) {
        if (!wired(p)) return;
        Catalog cat = RecordsCatalog.get();
        RecordData d = RecordsAttachments.of(p);
        Facts f = facts != null ? facts : FactsBuilder.build(d);
        List<String> rids = new ArrayList<>();
        List<Prog> rprog = new ArrayList<>();
        for (RecordDef r : cat.records()) {
            if (d.isUnlocked(r.id()) || r.hidden()) continue;
            rids.add(r.id());
            rprog.add(prog(Engine.recordEval(r, f)));
        }
        List<String> tids = new ArrayList<>();
        List<List<Prog>> tprog = new ArrayList<>();
        for (TitleDef t : cat.titles()) {
            if (d.holds(t.id())) continue;
            tids.add(t.id());
            List<Prog> l = new ArrayList<>();
            for (Eval e : Engine.titleEvals(cat, t, f, d)) l.add(prog(e));
            tprog.add(l);
        }
        List<String> sids = new ArrayList<>();
        List<int[]> sprog = new ArrayList<>();
        for (SynergyDef s : cat.synergies()) {
            sids.add(s.id());
            sprog.add(SynergyMatcher.progress(s, d.heldTitles(), cat::tagsOf));
        }
        int points = 0;
        for (String id : d.unlockedIds()) points += cat.pointsOf(id);
        Bonuses now = TitleApplier.desired(cat, d);
        PacketDistributor.sendToPlayer(p, new RecordsNet.State(new LinkedHashMap<>(timesOf(d, true)), new LinkedHashMap<>(timesOf(d, false)), d.active(), points, cat.totalPoints(),
                rids, rprog, tids, tprog, sids, sprog, Describe.bonusLines(now)));
    }

    private static java.util.Map<String, Long> timesOf(RecordData d, boolean records) {
        java.util.Map<String, Long> m = new LinkedHashMap<>();
        for (String id : records ? d.unlockedIds() : d.heldTitles()) m.put(id, records ? d.unlockedAt(id) : d.heldAt(id));
        return m;
    }

    private static Prog prog(Eval e) {
        int state = switch (e.state()) {
            case MET -> 0;
            case NOT_MET -> 1;
            case UNAVAILABLE -> 2;
        };
        return new Prog(state, e.current(), e.target());
    }

    static void toast(ServerPlayer p, int kind, String id, String name, int rarity) {
        if (!wired(p)) return;
        PacketDistributor.sendToPlayer(p, new RecordsNet.Toast(kind, id, name, rarity));
    }
}
