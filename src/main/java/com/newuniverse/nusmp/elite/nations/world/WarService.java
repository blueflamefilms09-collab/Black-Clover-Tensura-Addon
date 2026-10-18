package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.core.Res;
import com.newuniverse.nusmp.elite.nations.econ.Ledger;
import com.newuniverse.nusmp.elite.nations.event.AllianceBrokenEvent;
import com.newuniverse.nusmp.elite.nations.event.AllianceFormedEvent;
import com.newuniverse.nusmp.elite.nations.event.WarDeclaredEvent;
import com.newuniverse.nusmp.elite.nations.event.WarEndedEvent;
import com.newuniverse.nusmp.elite.nations.event.WarPhaseChangedEvent;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import com.newuniverse.nusmp.elite.nations.war.AllianceBook;
import com.newuniverse.nusmp.elite.nations.war.PvpRules;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarPhase;
import com.newuniverse.nusmp.elite.nations.war.WarRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: wars and alliances ("Nation War & Alliances: war phases, alliance proposals, player bounties, war score handling, war spoils, war history tracking, tick handlers"). It wraps the pure state machine
 * {@link WarRules}, {@link WarRules.Timings} from the config, the book of wars and the alliance book with: permission checks, the clock, the messages and events of each phase, the war score handler
 * (kills of enemy members, siege holding and capture at the capitals), the spoils of a victory (treasury transfer, reputation, a temporary buff, nation XP), the history, and the PvP protection verdict.
 * Nothing here changes a block: war never allows griefing. Kills and rules do not apply inside the dungeon dimensions unless the config says so. Server thread only.
 */
public final class WarService {
    private WarService() {}

    /** Kills in a row per player in the current runtime (for the optional automatic war bounty); not saved. */
    private static final Map<UUID, Integer> STREAK = new HashMap<>();

    public static WarRules.Timings timings() {
        return new WarRules.Timings(NationsConfig.seconds(NationsConfig.DECLARE_SECONDS), NationsConfig.minutes(NationsConfig.PREP_MINUTES), NationsConfig.minutes(NationsConfig.ACTIVE_MINUTES),
                NationsConfig.minutes(NationsConfig.CEASEFIRE_MINUTES), NationsConfig.get(NationsConfig.TARGET_SCORE));
    }

    // ---------------------------------------------------------------- queries
    /** Hostile relations: a war between the two is declared, preparing or fighting. */
    public static boolean atWar(NationsData d, String a, String b) {
        return a != null && b != null && !a.isEmpty() && !b.isEmpty() && !a.equals(b) && d.wars.hostileBetween(a, b) != null;
    }

    /** Fighting is allowed: the war between the two is ACTIVE. */
    public static boolean fighting(NationsData d, String a, String b) {
        return a != null && b != null && !a.isEmpty() && !b.isEmpty() && !a.equals(b) && d.wars.fightingBetween(a, b) != null;
    }

    public static boolean allied(NationsData d, String a, String b) { return d.alliances.areAllied(a, b); }

    // ---------------------------------------------------------------- declaring and ending by agreement
    public static Res declare(ServerPlayer p, String targetId, String reason) {
        if (!NationsConfig.warEnabled()) return Res.fail("nusmp.nation.m.war_disabled");
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = d.nationOf(p.getUUID());
        if (a == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!a.can(p.getUUID(), Permission.WAR)) return Res.fail("nusmp.nation.m.no_permission", Permission.WAR.id());
        Nation b = d.nations.get(targetId);
        long now = NationService.now(s);
        WarRules.DeclareContext ctx = new WarRules.DeclareContext(b != null, b != null && d.alliances.areAllied(a.id, b.id), b != null && d.wars.find(a.id, b.id) != null,
                b != null && d.wars.truceBetween(a.id, b.id, now), d.wars.openWarCount(a.id), NationsConfig.get(NationsConfig.MAX_WARS), a.level, NationsConfig.get(NationsConfig.MIN_LEVEL_TO_DECLARE),
                d.wars.declarerCoolingDown(a.id, now));
        WarRules.Refusal ref = WarRules.canDeclare(a.id, targetId, ctx);
        if (ref != WarRules.Refusal.NONE) return Res.fail("nusmp.nation.m.war_refused." + ref.name().toLowerCase(java.util.Locale.ROOT), b == null ? targetId : b.name, NationsConfig.get(NationsConfig.MIN_LEVEL_TO_DECLARE));
        String why = reason == null ? "" : reason.trim();
        if (why.length() > 80) why = why.substring(0, 80);
        War w = d.wars.add(WarRules.declare(d.wars.newId(), a.id, b.id, now, timings(), why));
        d.wars.declareCooldown.put(a.id, now + NationsConfig.minutes(NationsConfig.DECLARE_COOLDOWN_MINUTES));
        d.touch(a.id);
        d.touch(b.id);
        Msg.everyone(s, "nusmp.nation.m.war_declared", a.name, b.name);
        Treasury.post(new WarDeclaredEvent(w.id, a.id, b.id, w.reason));
        Treasury.post(new WarPhaseChangedEvent(w.id, a.id, b.id, WarPhase.DECLARED.id()));
        return Res.ok("nusmp.nation.m.war_ok", b.name);
    }

    private static War warWith(NationsData d, Nation a, String otherId) { return a == null ? null : d.wars.find(a.id, otherId); }

    /** Proposes a ceasefire, or accepts the one the other side proposed. */
    public static Res ceasefire(ServerPlayer p, String otherId) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = d.nationOf(p.getUUID());
        if (a == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!a.can(p.getUUID(), Permission.WAR)) return Res.fail("nusmp.nation.m.no_permission", Permission.WAR.id());
        War w = warWith(d, a, otherId);
        if (w == null || !w.fighting()) return Res.fail("nusmp.nation.m.no_active_war");
        long now = NationService.now(s);
        Nation b = d.nations.get(otherId);
        if (!w.ceasefireFrom.isEmpty() && !w.ceasefireFrom.equals(a.id)) {
            if (!WarRules.acceptCeasefire(w, a.id, now, timings())) return Res.fail("nusmp.nation.m.no_active_war");
            fightEnded(s, d, w, now);
            return Res.ok("nusmp.nation.m.ceasefire_agreed");
        }
        if (!WarRules.proposeCeasefire(w, a.id)) return Res.fail("nusmp.nation.m.ceasefire_pending");
        d.setDirty();
        if (b != null) Msg.broadcast(s, b, "nusmp.nation.m.ceasefire_proposed", a.name);
        return Res.ok("nusmp.nation.m.ceasefire_sent", b == null ? otherId : b.name);
    }

    public static Res declineCeasefire(ServerPlayer p, String otherId) {
        NationsData d = NationService.data(p.getServer());
        Nation a = d.nationOf(p.getUUID());
        if (a == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!a.can(p.getUUID(), Permission.WAR)) return Res.fail("nusmp.nation.m.no_permission", Permission.WAR.id());
        War w = warWith(d, a, otherId);
        if (w == null || !WarRules.declineCeasefire(w, a.id)) return Res.fail("nusmp.nation.m.no_ceasefire");
        d.setDirty();
        return Res.ok("nusmp.nation.m.ceasefire_declined");
    }

    public static Res surrender(ServerPlayer p, String otherId) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = d.nationOf(p.getUUID());
        if (a == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!a.leader.equals(p.getUUID())) return Res.fail("nusmp.nation.m.leader_only");
        War w = warWith(d, a, otherId);
        long now = NationService.now(s);
        if (w == null || !WarRules.surrender(w, a.id, now, timings())) return Res.fail("nusmp.nation.m.no_active_war");
        fightEnded(s, d, w, now);
        return Res.ok("nusmp.nation.m.surrendered");
    }

    /** The attacker withdraws a war that has not started fighting. */
    public static Res cancel(ServerPlayer p, String otherId) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = d.nationOf(p.getUUID());
        if (a == null) return Res.fail("nusmp.nation.m.not_in_nation");
        if (!a.can(p.getUUID(), Permission.WAR)) return Res.fail("nusmp.nation.m.no_permission", Permission.WAR.id());
        War w = warWith(d, a, otherId);
        if (w == null || !WarRules.cancel(w, a.id)) return Res.fail("nusmp.nation.m.cannot_cancel");
        long now = NationService.now(s);
        archive(s, d, w, now);
        return Res.ok("nusmp.nation.m.war_cancelled");
    }

    public static Res adminEnd(MinecraftServer s, String a, String b) {
        NationsData d = NationService.data(s);
        War w = d.wars.find(a, b);
        if (w == null) return Res.fail("nusmp.nation.m.no_active_war");
        long now = NationService.now(s);
        if (w.phase == WarPhase.CEASEFIRE) { w.phase = WarPhase.ENDED; archive(s, d, w, now); return Res.ok("nusmp.nation.m.war_force_ended"); }
        boolean fighting = w.phase == WarPhase.ACTIVE;
        WarRules.force(w, now, timings());
        if (fighting) fightEnded(s, d, w, now);
        else { w.phase = WarPhase.ENDED; archive(s, d, w, now); }
        return Res.ok("nusmp.nation.m.war_force_ended");
    }

    // ---------------------------------------------------------------- the clock
    /** Advances every open war. Called from the throttled loop. */
    public static void tick(MinecraftServer s, long now) {
        NationsData d = NationService.data(s);
        if (d.wars.wars.isEmpty()) return;
        WarRules.Timings t = timings();
        for (War w : new ArrayList<>(d.wars.wars.values())) {
            List<WarRules.Transition> steps = WarRules.advance(w, now, t);
            if (steps.isEmpty()) continue;
            d.touch(w.attacker);
            d.touch(w.defender);
            for (WarRules.Transition tr : steps) {
                switch (tr) {
                    case TO_PREPARATION -> {
                        phaseNotice(s, d, w, "nusmp.nation.m.war_prep");
                        Treasury.post(new WarPhaseChangedEvent(w.id, w.attacker, w.defender, WarPhase.PREPARATION.id()));
                    }
                    case TO_ACTIVE -> {
                        phaseNotice(s, d, w, "nusmp.nation.m.war_active");
                        Treasury.post(new WarPhaseChangedEvent(w.id, w.attacker, w.defender, WarPhase.ACTIVE.id()));
                    }
                    case ACTIVE_ENDED -> fightEnded(s, d, w, now);
                    case TO_ENDED -> archive(s, d, w, now);
                }
            }
        }
    }

    private static void phaseNotice(MinecraftServer s, NationsData d, War w, String key) {
        Nation a = d.nations.get(w.attacker), b = d.nations.get(w.defender);
        if (a == null || b == null) return;
        Msg.broadcast(s, a, key, a.name, b.name);
        Msg.broadcast(s, b, key, a.name, b.name);
    }

    /** The fight is over (the war is now in the ceasefire phase): spoils, records, messages. */
    private static void fightEnded(MinecraftServer s, NationsData d, War w, long now) {
        Nation a = d.nations.get(w.attacker), b = d.nations.get(w.defender);
        if (a == null || b == null) { d.wars.wars.remove(w.id); d.setDirty(); return; }
        boolean decisive = !w.winner.isEmpty() && (w.outcome == War.Outcome.VICTORY || w.outcome == War.Outcome.SURRENDER);
        if (decisive) {
            Nation win = w.winner.equals(a.id) ? a : b, lose = win == a ? b : a;
            win.warWins++;
            lose.warLosses++;
            long wanted = WarRules.spoilsCoins(lose.ledger.balance(), NationsConfig.get(NationsConfig.SPOILS_PERCENT), NationsConfig.get(NationsConfig.SPOILS_CAP));
            if (wanted > 0) {
                long taken = Treasury.debit(s, lose, Ledger.Kind.SPOILS, null, wanted, "spoils lost to " + win.name);
                long got = Treasury.credit(s, win, Ledger.Kind.SPOILS, null, taken, "spoils of the war against " + lose.name);
                if (got < taken) Treasury.credit(s, lose, Ledger.Kind.SPOILS, null, taken - got, "spoils refunded (treasury full)");
                w.spoilsCoins = got;
            }
            NationService.addXp(s, win, NationsConfig.get(NationsConfig.WAR_WIN_XP), "war");
            NationService.refresh(s, lose);
            int rep = NationsConfig.get(NationsConfig.SPOILS_REP);
            for (UUID m : win.members.keySet()) ReputationService.add(s, m, win.id, rep, "war victory");
            giveBuff(s, win);
            Msg.everyone(s, "nusmp.nation.m.war_won", win.name, lose.name, w.spoilsCoins);
        } else {
            Msg.everyone(s, "nusmp.nation.m.war_ended_draw", a.name, b.name);
        }
        d.touch(a.id);
        d.touch(b.id);
        STREAK.keySet().removeIf(id -> a.isMember(id) || b.isMember(id));
        Treasury.post(new WarPhaseChangedEvent(w.id, w.attacker, w.defender, WarPhase.CEASEFIRE.id()));
        Treasury.post(new WarEndedEvent(w.id, w.attacker, w.defender, w.winner, w.outcome.name().toLowerCase(java.util.Locale.ROOT), w.spoilsCoins));
    }

    private static void giveBuff(MinecraftServer s, Nation winner) {
        int minutes = NationsConfig.get(NationsConfig.WINNER_BUFF_MINUTES);
        if (minutes <= 0) return;
        int ticks = minutes * 60 * 20;
        for (UUID id : winner.members.keySet()) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, true, true));
        }
    }

    /** Moves a finished war into the history. */
    private static void archive(MinecraftServer s, NationsData d, War w, long now) {
        w.phase = WarPhase.ENDED;
        d.wars.archive(w, now, NationsConfig.minutes(NationsConfig.TRUCE_MINUTES));
        d.touch(w.attacker);
        d.touch(w.defender);
        Treasury.post(new WarPhaseChangedEvent(w.id, w.attacker, w.defender, WarPhase.ENDED.id()));
    }

    // ---------------------------------------------------------------- score handler
    private static boolean scoringHere(ServerPlayer p) {
        return NationsConfig.get(NationsConfig.WAR_SCORE_IN_DUNGEON) || !com.newuniverse.nusmp.dungeon.DungeonGuard.on(p.level());
    }

    /** A player killed another player: the war score handler. */
    public static void onPlayerKill(ServerPlayer killer, ServerPlayer victim) {
        if (!NationsConfig.warEnabled() || killer == victim) return;
        MinecraftServer s = killer.getServer();
        NationsData d = NationService.data(s);
        String kn = d.nationIdOf(killer.getUUID()), vn = d.nationIdOf(victim.getUUID());
        STREAK.remove(victim.getUUID());
        if (kn.isEmpty() || vn.isEmpty()) return;
        if (kn.equals(vn)) return;
        if (!scoringHere(killer)) return;
        War w = d.wars.fightingBetween(kn, vn);
        if (w == null) {
            Nation victimNation = d.nations.get(vn);
            if (victimNation != null && !d.alliances.areAllied(kn, vn) && NationsConfig.get(NationsConfig.REP_KILL_MEMBER_PENALTY) > 0) {
                ReputationService.add(s, killer.getUUID(), vn, -NationsConfig.get(NationsConfig.REP_KILL_MEMBER_PENALTY), "killed a member");
            }
            return;
        }
        long now = NationService.now(s);
        int pts = WarRules.scoreKill(w, kn, victim.getUUID().toString(), now, NationsConfig.get(NationsConfig.KILL_POINTS), NationsConfig.minutes(NationsConfig.KILL_COOLDOWN_MINUTES));
        d.touch(kn);
        d.touch(vn);
        if (pts > 0) killer.displayClientMessage(Msg.of("nusmp.nation.m.war_point", pts, w.score(kn)), true);
        int streak = STREAK.merge(killer.getUUID(), 1, Integer::sum);
        int bounty = NationsConfig.get(NationsConfig.WAR_BOUNTY_COINS);
        Nation victimNation = d.nations.get(vn);
        if (bounty > 0 && streak == 3 && victimNation != null) {
            if (ContractService.postWarBounty(s, victimNation, killer.getUUID(), killer.getGameProfile().getName(), bounty)) {
                Msg.everyone(s, "nusmp.nation.m.war_bounty", killer.getGameProfile().getName(), bounty);
            }
        }
    }

    /** A guard of a nation was killed by a player: reputation penalty with that nation. */
    public static void onGuardKilled(ServerPlayer killer, String nationId) {
        int pen = NationsConfig.get(NationsConfig.REP_GUARD_ATTACK_PENALTY);
        if (pen > 0) ReputationService.add(killer.getServer(), killer.getUUID(), nationId, -pen, "killed a guard");
    }

    /** One siege interval: counts members standing in the enemy capital area and awards the points. */
    public static void siegeTick(MinecraftServer s) {
        NationsData d = NationService.data(s);
        int radius = NationsConfig.get(NationsConfig.CAPITAL_RADIUS);
        for (War w : new ArrayList<>(d.wars.wars.values())) {
            if (!w.fighting()) continue;
            Nation a = d.nations.get(w.attacker), b = d.nations.get(w.defender);
            if (a == null || b == null) continue;
            if (b.hasCapital) {
                int at = present(s, a, b, radius), df = present(s, b, b, radius);
                var r = WarRules.scoreSiege(w, true, at, df, NationsConfig.get(NationsConfig.HOLD_POINTS), NationsConfig.get(NationsConfig.CAPTURE_POINTS), NationsConfig.get(NationsConfig.CAPTURE_INTERVALS));
                if (r.captured()) captured(s, a, b);
                if (r.points() > 0) { d.touch(a.id); d.touch(b.id); }
            }
            if (a.hasCapital) {
                int df = present(s, b, a, radius), at = present(s, a, a, radius);
                var r = WarRules.scoreSiege(w, false, df, at, NationsConfig.get(NationsConfig.HOLD_POINTS), NationsConfig.get(NationsConfig.CAPTURE_POINTS), NationsConfig.get(NationsConfig.CAPTURE_INTERVALS));
                if (r.captured()) captured(s, b, a);
                if (r.points() > 0) { d.touch(a.id); d.touch(b.id); }
            }
            d.setDirty();
        }
    }

    private static void captured(MinecraftServer s, Nation by, Nation of) { Msg.everyone(s, "nusmp.nation.m.capital_captured", by.name, of.name); }

    /** How many online members of {@code who} stand in the capital area of {@code at}. */
    public static int present(MinecraftServer s, Nation who, Nation at, int radius) {
        int n = 0;
        for (UUID id : who.members.keySet()) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive() || p.isSpectator()) continue;
            if (!p.level().dimension().location().toString().equals(at.capitalDim)) continue;
            if (GuardRules.inZone(p.blockPosition().getX(), p.blockPosition().getY(), p.blockPosition().getZ(), at.cx, at.cy, at.cz, radius, 64)) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- PvP protection
    /** The verdict for one player hurting another. Players without a nation, and fights inside the dungeon dimensions, are not touched. */
    public static PvpRules.Verdict pvp(ServerPlayer attacker, ServerPlayer victim) {
        if (!NationsConfig.enabled() || attacker == victim) return PvpRules.Verdict.ALLOW_VANILLA;
        if (!scoringHere(attacker)) return PvpRules.Verdict.ALLOW_VANILLA;
        NationsData d = NationService.data(attacker.getServer());
        String an = d.nationIdOf(attacker.getUUID()), vn = d.nationIdOf(victim.getUUID());
        return PvpRules.check(an, vn, NationsConfig.get(NationsConfig.PROTECT_SAME_NATION), d.alliances.areAllied(an, vn), fighting(d, an, vn), NationsConfig.warEnabled() && NationsConfig.get(NationsConfig.WAR_ONLY_PVP));
    }

    // ---------------------------------------------------------------- alliances
    private static Res allianceResult(AllianceBook.Result r, String otherName) {
        return switch (r) {
            case OK -> Res.ok("nusmp.nation.m.ally_ok", otherName);
            default -> Res.fail("nusmp.nation.m.ally_refused." + r.name().toLowerCase(java.util.Locale.ROOT), otherName);
        };
    }

    private static Nation diplomat(NationsData d, ServerPlayer p) {
        Nation a = d.nationOf(p.getUUID());
        return a != null && a.can(p.getUUID(), Permission.DIPLOMACY) ? a : null;
    }

    public static Res propose(ServerPlayer p, String otherId) {
        if (!NationsConfig.warEnabled()) return Res.fail("nusmp.nation.m.war_disabled");
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = diplomat(d, p);
        if (a == null) return Res.fail("nusmp.nation.m.no_permission", Permission.DIPLOMACY.id());
        Nation b = d.nations.get(otherId);
        if (b == null) return Res.fail("nusmp.nation.m.no_such_nation", otherId);
        long now = NationService.now(s);
        AllianceBook.Result r = d.alliances.propose(a.id, b.id, now, NationsConfig.minutes(NationsConfig.PROPOSAL_TTL_MINUTES), NationsConfig.get(NationsConfig.ALLIANCE_MAX), (x, y) -> atWar(d, x, y));
        if (r == AllianceBook.Result.OK) {
            d.setDirty();
            if (d.alliances.areAllied(a.id, b.id)) formed(s, a, b);
            else Msg.broadcast(s, b, "nusmp.nation.m.ally_proposed", a.name, a.id);
        }
        Res res = allianceResult(r, b.name);
        return res.ok() && !d.alliances.areAllied(a.id, b.id) ? Res.ok("nusmp.nation.m.ally_sent", b.name) : res;
    }

    public static Res acceptAlliance(ServerPlayer p, String fromId) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation b = diplomat(d, p);
        if (b == null) return Res.fail("nusmp.nation.m.no_permission", Permission.DIPLOMACY.id());
        Nation a = d.nations.get(fromId);
        if (a == null) return Res.fail("nusmp.nation.m.no_such_nation", fromId);
        AllianceBook.Result r = d.alliances.accept(b.id, a.id, NationService.now(s), NationsConfig.get(NationsConfig.ALLIANCE_MAX), (x, y) -> atWar(d, x, y));
        if (r == AllianceBook.Result.OK) { d.setDirty(); formed(s, a, b); }
        return allianceResult(r, a.name);
    }

    public static Res declineAlliance(ServerPlayer p, String fromId) {
        NationsData d = NationService.data(p.getServer());
        Nation b = diplomat(d, p);
        if (b == null) return Res.fail("nusmp.nation.m.no_permission", Permission.DIPLOMACY.id());
        AllianceBook.Result r = d.alliances.decline(b.id, fromId);
        d.setDirty();
        return r == AllianceBook.Result.OK ? Res.ok("nusmp.nation.m.ally_declined") : Res.fail("nusmp.nation.m.ally_refused.no_proposal", fromId);
    }

    public static Res breakAlliance(ServerPlayer p, String otherId) {
        MinecraftServer s = p.getServer();
        NationsData d = NationService.data(s);
        Nation a = diplomat(d, p);
        if (a == null) return Res.fail("nusmp.nation.m.no_permission", Permission.DIPLOMACY.id());
        Nation b = d.nations.get(otherId);
        AllianceBook.Result r = d.alliances.breakAlliance(a.id, otherId, NationService.now(s), NationsConfig.minutes(NationsConfig.ALLIANCE_BREAK_COOLDOWN_MINUTES));
        if (r != AllianceBook.Result.OK) return Res.fail("nusmp.nation.m.ally_refused.not_allied", otherId);
        d.touch(a.id);
        d.touch(otherId);
        if (b != null) Msg.broadcast(s, b, "nusmp.nation.m.ally_broken", a.name);
        Treasury.post(new AllianceBrokenEvent(a.id, otherId, a.id));
        return Res.ok("nusmp.nation.m.ally_break_ok", b == null ? otherId : b.name);
    }

    private static void formed(MinecraftServer s, Nation a, Nation b) {
        NationService.data(s).touch(a.id);
        NationService.data(s).touch(b.id);
        Msg.everyone(s, "nusmp.nation.m.ally_formed", a.name, b.name);
        Treasury.post(new AllianceFormedEvent(a.id, b.id));
    }

    /** Drops expired proposals. */
    public static void expire(MinecraftServer s, long now) {
        NationsData d = NationService.data(s);
        if (d.alliances.expire(now) > 0) d.setDirty();
    }

    /** A nation's reputation tier toward a player, as seen by its guards. */
    public static boolean guardsHostileTo(MinecraftServer s, String nationId, ServerPlayer p) {
        NationsData d = NationService.data(s);
        String pn = d.nationIdOf(p.getUUID());
        if (pn.equals(nationId)) return false;
        if (!pn.isEmpty() && d.alliances.areAllied(pn, nationId)) return false;
        if (!pn.isEmpty() && fighting(d, pn, nationId)) return true;
        return ReputationTier.of(d.reputation.get(p.getUUID(), nationId)).guardsHostile;
    }
}
