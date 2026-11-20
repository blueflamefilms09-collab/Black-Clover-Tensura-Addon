package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilEntities;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.devil.phase2.Phase2Rules.Kind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the contract rules of phase 2 can not be seen in a game by their author, so the CI server smoke test boots the mod and runs these (prefix {@code devil.phase2}, docs/devil_contract_phase2.md). Each one is
 * an area of the rules, run against the real classes of the running server:
 * <ul>
 * <li>{@code rules}: the three contract kinds, the share formula, the caps and the settings' bounds.</li>
 * <li>{@code ep}: every devil of the roster has an EP requirement, higher ranks ask more, the server gate refuses a body with no existence points and lets one through when the rule is off.</li>
 * <li>{@code summon}: the chance per rank, the relic bonus, the roll, and the relic tags of the devils that have a relic (a real item in a real inventory raises the chance).</li>
 * <li>{@code stack}: weak devils stack, a Supreme devil needs five to ten times the requirement, two Supreme at most, the gate says banned and full.</li>
 * <li>{@code share}: a lesser devil is shared (one-of-one stays for Supreme), the ledger counts holders without scanning, the cap of contracts, the even split of power.</li>
 * <li>{@code sever}: the kill rule, the ban list, the debuff and the friendly-fire counter, the game rule.</li>
 * <li>{@code trial}: the trial devil is a real entity, its numbers grow with the rank, a stranger can not hurt it, the server cap is read.</li>
 * <li>{@code save}: the ledger survives its save, a damaged file can not grow it past its bounds.</li>
 * </ul>
 * Nothing here sends a chat line or a packet to the stand-in (it has no connection), and every row written to the world's ledger is taken out again.
 */
public final class Phase2SelfTests {
    private Phase2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("devil.phase2.rules", Phase2SelfTests::rules);
        SelfTests.register("devil.phase2.ep", Phase2SelfTests::ep);
        SelfTests.register("devil.phase2.summon", Phase2SelfTests::summon);
        SelfTests.register("devil.phase2.stack", Phase2SelfTests::stack);
        SelfTests.register("devil.phase2.share", Phase2SelfTests::share);
        SelfTests.register("devil.phase2.sever", Phase2SelfTests::sever);
        SelfTests.register("devil.phase2.trial", Phase2SelfTests::trial);
        SelfTests.register("devil.phase2.save", Phase2SelfTests::save);
    }

    private static WorldDevil first(DevilTier tier) {
        for (int i = 0; i < WorldDevil.count(); i++) if (WorldDevil.at(i).tier() == tier) return WorldDevil.at(i);
        return null;
    }

    private static List<WorldDevil> weak(int n) {
        List<WorldDevil> out = new ArrayList<>();
        for (int i = 0; i < WorldDevil.count() && out.size() < n; i++) if (!Phase2Rules.strong(WorldDevil.at(i).tier())) out.add(WorldDevil.at(i));
        return out;
    }

    private static UUID id(String s) { return UUID.nameUUIDFromBytes(("devil-phase2-" + s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    // ---------------------------------------------------------------- rules
    private static Result rules(MinecraftServer server) {
        if (!(Kind.SUBMISSION.share < Kind.EVEN.share && Kind.EVEN.share < Kind.DOMINANCE.share && Kind.DOMINANCE.share == 1.0)) return Result.fail("the kinds do not grow from submission to dominance");
        if (Kind.byName("submit") != Kind.SUBMISSION || Kind.byName("EVEN") != Kind.EVEN || Kind.byName("dominate") != Kind.DOMINANCE || Kind.byName("nonsense") != null || Kind.byName(null) != null) return Result.fail("the kind names moved");
        if (Math.abs(Phase2Rules.power(Kind.EVEN, 2, 1.0) - 0.25) > 1e-9 || Math.abs(Phase2Rules.power(Kind.DOMINANCE, 4, 0.5) - 0.125) > 1e-9 || Phase2Rules.power(Kind.DOMINANCE, 1, 1.0) != 1.0) return Result.fail("the share formula moved");
        if (Phase2Rules.contractCap(0) != 1 || Phase2Rules.contractCap(99) != Phase2Rules.MAX_CONTRACTS_CAP || !Phase2Rules.canJoin(3, 4) || Phase2Rules.canJoin(4, 4)) return Result.fail("the cap of contracts moved");
        int n = Phase2Config.maxContracts(), w = Phase2Config.weakMax(), s = Phase2Config.strongMax(), t = Phase2Config.trialMax(), ff = Phase2Config.friendlyFireWindow();
        if (n < 1 || n > Phase2Rules.MAX_CONTRACTS_CAP || w < 1 || w > Phase2Rules.MAX_WEAK_CAP || s < 1 || s > Phase2Rules.MAX_STRONG_CAP || t < 0 || t > 16 || ff < 1) return Result.fail("a setting is out of its bounds");
        double dp = Phase2Config.debuffPower();
        if (dp < 0 || dp > 1 || Phase2Config.debuffSeconds() < 10 || Phase2Config.dominanceFactor() < 1) return Result.fail("a sabotage or dominance setting is out of its bounds");
        return Result.ok("devil phase2 rules: kinds " + Kind.SUBMISSION.share + "/" + Kind.EVEN.share + "/" + Kind.DOMINANCE.share + ", split, caps (contracts " + n + ", weak " + w + ", Supreme " + s + "), settings in bounds; enabled=" + Phase2Config.enabled());
    }

    // ---------------------------------------------------------------- the EP table
    private static Result ep(MinecraftServer server) {
        Phase2Table t = Phase2Table.get();
        double low = t.tierEp(DevilTier.LOW), mid = t.tierEp(DevilTier.MID), high = t.tierEp(DevilTier.HIGH), top = t.tierEp(DevilTier.SUPREME);
        if (!(low > 0 && low < mid && mid < high && high < top)) return Result.fail("the EP of the ranks does not grow with the rank: " + low + ", " + mid + ", " + high + ", " + top);
        if (top < 5.0E8 || top > 5.0E9) return Result.fail("Supreme does not ask about a billion EP: " + top);
        for (int i = 0; i < WorldDevil.count(); i++) {
            WorldDevil d = WorldDevil.at(i);
            double need = t.requiredEp(d.id(), d.tier(), 1.0);
            if (need <= 0 || need > Phase2Rules.MAX_EP) return Result.fail(d.id() + " has no EP requirement in bounds: " + need);
            if (need < t.tierEp(DevilTier.LOW) / 10) return Result.fail(d.id() + " asks less than a tenth of the lowest rank");
        }
        if (Phase2Rules.epOk(0.0, 1.0E6) || !Phase2Rules.epOk(1.0E6, 1.0E6) || Phase2Rules.epOk(Double.NaN, 1.0) || !Phase2Rules.epOk(0.0, 0.0)) return Result.fail("the EP comparison moved");
        WorldDevil lo = first(DevilTier.LOW), up = first(DevilTier.SUPREME);
        if (lo == null || up == null) return Result.fail("the roster has no Low or no Supreme devil");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_phase2_ep", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        if (!Phase2.on()) return Result.ok("devil phase2 ep: table checked; the live gate was skipped (phase 2 is off by the server config)");
        double need = Phase2Config.ep() ? Phase2.need(up) : 0.0;
        if (Phase2Config.ep() && need <= Phase2.need(lo)) return Result.fail("the server asks no more for Supreme than for Low");
        Phase2.Gate g = Phase2.gate(p, null, up);
        if (Phase2Config.ep() && !p.isCreative() && Phase2.ep(p) < need && (g.ok() || !"ep".equals(g.why()))) return Result.fail("the gate let in a body with " + Phase2.ep(p) + " EP for a devil that asks " + need + " (" + g.why() + ")");
        if (!Phase2Config.ep() && !g.ok()) return Result.fail("the EP rule is off but the gate refused: " + g.why());
        return Result.ok("devil phase2 ep: Low " + low + ", Mid " + mid + ", High " + high + ", Supreme " + top + " EP; every devil of the roster asks a bounded amount; the gate gave '" + (g.ok() ? "ok" : g.why()) + "' to a stand-in with " + Phase2.ep(p) + " EP");
    }

    // ---------------------------------------------------------------- the summon chance and the relic
    private static Result summon(MinecraftServer server) {
        Phase2Table t = Phase2Table.get();
        double prev = 2.0;
        for (DevilTier tier : new DevilTier[] {DevilTier.LOW, DevilTier.MID, DevilTier.HIGH, DevilTier.SUPREME}) {
            double c = t.summonChance(tier, false), r = t.summonChance(tier, true);
            if (c <= 0 || c > 1 || r < c || r > 1) return Result.fail(tier + " has a chance outside (0, 1] or a relic that lowers it: " + c + " / " + r);
            if (c >= prev) return Result.fail("a higher rank is not harder to call: " + tier);
            prev = c;
        }
        if (t.summonChance(DevilTier.SUPREME, false) >= 1.0) return Result.fail("a Supreme devil answers every call");
        if (t.summonChance(DevilTier.SUPREME, true) <= t.summonChance(DevilTier.SUPREME, false)) return Result.fail("a relic does not raise the chance");
        if (Phase2Rules.roll(0.0, 0.0) || !Phase2Rules.roll(1.0, 0.999) || !Phase2Rules.roll(0.5, 0.49) || Phase2Rules.roll(0.5, 0.5)) return Result.fail("the roll moved");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_phase2_summon", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        WorldDevil liebe = WorldDevil.byId("liebe");
        if (liebe == null) return Result.fail("the roster has no Liebe");
        ResourceLocation core = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_core_liebe");
        Item item = BuiltInRegistries.ITEM.get(core);
        if (item == Items.AIR) return Result.fail("the item devil_core_liebe is not registered, so its relic tag has nothing to hold");
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) p.getInventory().setItem(i, ItemStack.EMPTY);
        try {
            if (Phase2.hasRelic(p, liebe)) return Result.fail("an empty inventory counts as carrying a relic");
            p.getInventory().setItem(0, new ItemStack(item));
            if (!Phase2.hasRelic(p, liebe)) return Result.fail("the core of Liebe in the inventory is not a relic of Liebe (tag anime_multiverse:devil_relics/liebe)");
            WorldDevil other = WorldDevil.byId("lucifero");
            if (other != null && Phase2.hasRelic(p, other)) return Result.fail("the core of Liebe counts as a relic of Lucifero");
        } finally {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        return Result.ok("devil phase2 summon: chance falls with the rank (Supreme " + t.summonChance(DevilTier.SUPREME, false) + "), a relic raises it (" + t.summonChance(DevilTier.SUPREME, true) + "), the roll is exact, the core of Liebe is a relic of Liebe only");
    }

    // ---------------------------------------------------------------- stacking
    private static Result stack(MinecraftServer server) {
        double base = 1.0E9;
        Phase2Rules.Stack a = Phase2Rules.stack(true, false, 0, 0, true, base, base, 8, 2, 5.0, 10.0);
        if (!a.ok() || a.needEp() != base) return Result.fail("the first Supreme devil asks more than its requirement");
        Phase2Rules.Stack b = Phase2Rules.stack(true, true, 0, 1, true, base, 4.9 * base, 8, 2, 5.0, 10.0);
        if (b.ok() || !"ep_stack".equals(b.why()) || b.needEp() != 5.0 * base) return Result.fail("the second Supreme devil does not ask five times the requirement: " + b);
        if (!Phase2Rules.stack(true, true, 0, 1, true, base, 5.0 * base, 8, 2, 5.0, 10.0).ok()) return Result.fail("five times the requirement is not enough for the second Supreme devil");
        if (Phase2Rules.stack(true, true, 0, 2, true, base, 1.0E15, 8, 2, 5.0, 10.0).ok()) return Result.fail("a third Supreme devil was allowed with the cap at two");
        if (Math.abs(Phase2Rules.strongFactor(2, 5.0, 10.0) - 7.5) > 1e-9 || Phase2Rules.strongFactor(3, 5.0, 10.0) != 10.0 || Phase2Rules.strongFactor(9, 5.0, 10.0) != 10.0) return Result.fail("the factor does not rise from 5 to 10");
        if (!Phase2Rules.stack(true, true, 3, 0, false, 1.0E6, 1.0E6, 8, 2, 5.0, 10.0).ok() || Phase2Rules.stack(true, true, 8, 0, false, 1.0E6, 1.0E15, 8, 2, 5.0, 10.0).ok()) return Result.fail("weak devils do not stack up to their bound");
        if (Phase2Rules.stack(false, true, 1, 0, false, 1.0E6, 1.0E15, 8, 2, 5.0, 10.0).ok()) return Result.fail("with stacking off a holder took another devil");
        WorldDevil lo = weak(1).isEmpty() ? null : weak(1).get(0);
        if (lo == null) return Result.fail("the roster has no lesser devil");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_phase2_stack", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        if (!Phase2.on()) return Result.ok("devil phase2 stack: rules checked; the live gate was skipped (phase 2 is off)");
        DevilLedger l = DevilLedger.get(server);
        UUID me = p.getUUID();
        List<UUID> crowd = new ArrayList<>();
        try {
            l.ban(me, lo.id());
            Phase2.Gate banned = Phase2.gate(p, null, lo);
            if (banned.ok() || !"banned".equals(banned.why())) return Result.fail("the gate did not say banned: " + banned);
            l.unban(me, lo.id());
            if (Phase2.gate(p, null, lo).why().equals("banned")) return Result.fail("lifting a ban left the ban");
            if (!Phase2.oneOfOne(lo.id()) && Phase2Config.shared()) {
                for (int i = 0; i < Phase2Config.maxContracts(); i++) {
                    UUID o = id("crowd" + i);
                    crowd.add(o);
                    l.join(o, lo.id(), Kind.EVEN, 1L);
                }
                Phase2.Gate full = Phase2.gate(p, null, lo);
                if (full.ok() || !"full".equals(full.why())) return Result.fail("a devil with all its contracts did not say full: " + full);
            }
        } finally {
            l.unban(me, lo.id());
            for (UUID o : crowd) l.leave(o, lo.id());
        }
        for (UUID o : crowd) if (l.pact(o, lo.id()) != null) return Result.fail("the ledger kept a row of the test");
        return Result.ok("devil phase2 stack: first Supreme x1, second x5, rising to x10, two at most, weak devils up to " + Phase2Config.weakMax() + "; the gate says banned and full");
    }

    // ---------------------------------------------------------------- sharing
    private static Result share(MinecraftServer server) {
        WorldDevil lo = weak(1).isEmpty() ? null : weak(1).get(0), up = first(DevilTier.SUPREME);
        if (lo == null || up == null) return Result.fail("the roster has no lesser or no Supreme devil");
        if (!Phase2.oneOfOne(up.id())) return Result.fail("a Supreme devil is not one-of-one");
        if (Phase2Config.shared() && Phase2.on() && Phase2.oneOfOne(lo.id())) return Result.fail("a lesser devil is one-of-one with sharing on");
        if (!Phase2Rules.oneOfOne(false, lo.tier()) || Phase2Rules.oneOfOne(true, lo.tier())) return Result.fail("the one-of-one rule moved");
        DevilLedger l = new DevilLedger();
        int cap = Phase2Rules.contractCap(4);
        List<UUID> in = new ArrayList<>();
        for (int i = 0; i < cap; i++) {
            if (!Phase2Rules.canJoin(l.count(lo.id()), cap)) return Result.fail("the devil was full at " + l.count(lo.id()));
            UUID o = id("share" + i);
            in.add(o);
            if (!l.join(o, lo.id(), Kind.DOMINANCE, 5L)) return Result.fail("a join was refused");
        }
        if (l.count(lo.id()) != cap || Phase2Rules.canJoin(l.count(lo.id()), cap)) return Result.fail("the cap of contracts did not stop the fifth");
        if (l.join(in.get(0), lo.id(), Kind.EVEN, 6L)) return Result.fail("a player joined the same devil twice");
        if (Math.abs(Phase2Rules.power(Kind.DOMINANCE, l.count(lo.id()), 1.0) - 1.0 / cap) > 1e-9) return Result.fail("the power is not split evenly");
        if (l.holders(lo.id()).size() != cap) return Result.fail("the list of holders is wrong");
        l.leave(in.get(0), lo.id());
        if (l.count(lo.id()) != cap - 1 || l.pact(in.get(0), lo.id()) != null) return Result.fail("a leave did not take the row");
        if (Phase2Rules.power(Kind.DOMINANCE, l.count(lo.id()), 1.0) <= 1.0 / cap) return Result.fail("the share of the others did not grow");
        for (UUID o : in) l.leave(o, lo.id());
        if (l.count(lo.id()) != 0 || l.playerRows() != 0) return Result.fail("the ledger kept rows after every holder left");
        UUID many = id("many");
        List<WorldDevil> some = weak(DevilLedger.MAX_PACTS + 4);
        int joined = 0;
        for (WorldDevil d : some) if (l.join(many, d.id(), Kind.EVEN, 1L)) joined++;
        if (joined > DevilLedger.MAX_PACTS) return Result.fail("a player holds more than " + DevilLedger.MAX_PACTS + " contracts");
        return Result.ok("devil phase2 share: Supreme is one-of-one, a lesser devil is shared (cap " + cap + "), the power splits 1/" + cap + ", a leave grows the others' share, no double join, a player holds at most " + DevilLedger.MAX_PACTS);
    }

    // ---------------------------------------------------------------- sever, sabotage
    private static Result sever(MinecraftServer server) {
        WorldDevil lo = weak(1).isEmpty() ? null : weak(1).get(0), up = first(DevilTier.SUPREME);
        if (lo == null || up == null) return Result.fail("the roster has no lesser or no Supreme devil");
        List<String> killer = List.of(lo.id(), up.id()), victim = List.of(lo.id(), up.id(), "other");
        List<String> cut = Phase2Rules.severed(true, false, killer, victim, d -> !Phase2Rules.strong(WorldDevil.byId(d) == null ? null : WorldDevil.byId(d).tier()));
        if (!cut.equals(List.of(lo.id()))) return Result.fail("the kill cut " + cut + " instead of the one shared lesser devil");
        if (!Phase2Rules.severed(false, false, killer, victim, d -> true).isEmpty() || !Phase2Rules.severed(true, true, killer, victim, d -> true).isEmpty()) return Result.fail("the sever rule acts when it is off or on the same man");
        if (NUGameRules.DEVIL_SEVER_ON_KILL == null) return Result.fail("the game rule devilSeverOnKill is not registered");
        boolean rule = NUGameRules.devilSeverOnKill(server.overworld());
        long now = 1000L;
        long until = Phase2Rules.debuffUntil(now, 180);
        if (until != now + 3600L || !Phase2Rules.debuffActive(until, now + 3599L) || Phase2Rules.debuffActive(until, now + 3600L)) return Result.fail("the debuff clock moved");
        if (Phase2Rules.sabotageFactor(true, 0.5) != 0.5 || Phase2Rules.sabotageFactor(false, 0.5) != 1.0) return Result.fail("the debuff factor moved");
        Phase2Rules.Fire f = new Phase2Rules.Fire(0, 0, false);
        boolean tripped = false;
        for (int i = 0; i < 5 && !tripped; i++) {
            f = Phase2Rules.friendlyFire(f.hits(), f.start(), 100L + i, 30, 5);
            tripped = f.trip();
        }
        if (!tripped) return Result.fail("five blows in a few ticks did not trip the friendly-fire rule");
        Phase2Rules.Fire slow = new Phase2Rules.Fire(0, 0, false);
        for (int i = 0; i < 12; i++) {
            slow = Phase2Rules.friendlyFire(slow.hits(), slow.start(), 100L + i * 800L, 30, 5);
            if (slow.trip()) return Result.fail("blows spread over minutes tripped the friendly-fire rule");
        }
        if (!Phase2.on()) return Result.ok("devil phase2 sever: rules checked; the live part was skipped (phase 2 is off)");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "devil_phase2_sever", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        DevilContract c = DevilStore.of(p);
        DevilLedger l = DevilLedger.get(server);
        UUID me = p.getUUID();
        DevilTier oldTier = c.tier;
        String oldDevil = c.devil;
        try {
            c.tier = lo.tier();
            c.devil = lo.id();
            l.join(me, lo.id(), Kind.EVEN, 1L);
            double plain = Phase2.power(p, c);
            double expect = Phase2Config.kinds() ? 0.5 / (Phase2.oneOfOne(lo.id()) ? 1 : Math.max(1, l.count(lo.id()))) : 1.0 / (Phase2.oneOfOne(lo.id()) ? 1 : Math.max(1, l.count(lo.id())));
            if (Math.abs(plain - expect) > 1e-9) return Result.fail("the power of an even contract is " + plain + ", expected " + expect);
            if (Phase2Config.sabotage()) {
                l.setDebuff(me, level.getServer().overworld().getGameTime() + 1200L);
                double cut2 = Phase2.power(p, c);
                if (Math.abs(cut2 - plain * Phase2Config.debuffPower()) > 1e-9) return Result.fail("the sabotage debuff did not cut the power: " + cut2 + " from " + plain);
                l.setDebuff(me, 0L);
                if (Math.abs(Phase2.power(p, c) - plain) > 1e-9) return Result.fail("the debuff did not end");
            }
            l.ban(me, lo.id());
            if (!l.banned(me, lo.id()) || !l.bans(me).contains(lo.id())) return Result.fail("a ban was not kept");
            l.unban(me, lo.id());
            if (l.banned(me, lo.id())) return Result.fail("an operator could not lift a ban");
        } finally {
            l.leave(me, lo.id());
            l.unban(me, lo.id());
            l.setDebuff(me, 0L);
            c.tier = oldTier;
            c.devil = oldDevil;
        }
        return Result.ok("devil phase2 sever: only the shared lesser devil is cut, the ban list and the debuff work, the debuff clock and the friendly-fire counter are exact; gamerule devilSeverOnKill=" + rule);
    }

    // ---------------------------------------------------------------- the trial
    private static Result trial(MinecraftServer server) {
        double[] low = DevilTrialEntity.stats(DevilTier.LOW, 1.0), mid = DevilTrialEntity.stats(DevilTier.MID, 1.0), high = DevilTrialEntity.stats(DevilTier.HIGH, 1.0), top = DevilTrialEntity.stats(DevilTier.SUPREME, 1.0);
        if (!(low[0] < mid[0] && mid[0] < high[0] && high[0] < top[0] && low[1] < top[1] && low[2] < top[2])) return Result.fail("the trial devil does not grow with the rank");
        if (DevilTrialEntity.stats(DevilTier.LOW, 1000.0)[0] > low[0] * 20.0 + 1e-6 || DevilTrialEntity.stats(DevilTier.LOW, 0.0)[0] < low[0] * 0.1 - 1e-6) return Result.fail("the health scale is not bounded");
        if (Phase2Rules.trialResult(0, 0.4, false, 0.5) != Phase2Rules.TRIAL_IMPRESSED || Phase2Rules.trialResult(0, 0.9, false, 0.5) != Phase2Rules.TRIAL_NONE || Phase2Rules.trialResult(0, 0.9, true, 0.5) != Phase2Rules.TRIAL_WON || Phase2Rules.trialResult(2, 0.9, false, 0.5) != Phase2Rules.TRIAL_WON)
            return Result.fail("the trial result rule moved");
        if (!Phase2Rules.kindAllowed(Kind.EVEN, true, true, true, true, Phase2Rules.TRIAL_IMPRESSED) || Phase2Rules.kindAllowed(Kind.EVEN, true, true, true, true, Phase2Rules.TRIAL_NONE)
                || Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, true, true, Phase2Rules.TRIAL_IMPRESSED) || !Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, true, true, Phase2Rules.TRIAL_WON)
                || Phase2Rules.kindAllowed(Kind.DOMINANCE, true, false, true, true, Phase2Rules.TRIAL_WON) || Phase2Rules.kindAllowed(Kind.DOMINANCE, true, true, false, true, Phase2Rules.TRIAL_WON)) return Result.fail("the kinds a trial result unlocks moved");
        ServerLevel level = server.overworld();
        DevilTrialEntity e = DevilEntities.TRIAL.get().create(level);
        if (e == null) return Result.fail("the trial entity could not be made");
        double y = level.getMaxBuildHeight() - 60;
        e.setPos(0.5, y, 0.5);
        try {
            if (e.isInvulnerable() || e.fireImmune() == false) return Result.fail("the trial devil is invulnerable or burns");
            float before = e.getHealth();
            if (e.hurt(level.damageSources().generic(), 3f) || e.getHealth() != before) return Result.fail("a stranger could hurt the trial devil");
            if (e.shouldBeSaved() || e.removeWhenFarAway(1.0E6)) return Result.fail("the trial devil is saved or despawns by itself");
            if (Trials.live(server) < 0 || Trials.live(server) > Phase2Config.trialMax() + 64) return Result.fail("the count of running trials is not read");
            if (!e.devilId().isEmpty() || e.getAnimatableInstanceCache() == null) return Result.fail("the trial devil's synced devil id or its GeckoLib cache is wrong (docs/devil_trial_model.md)");
            if (Phase2Config.betrayalLeaves() < 0 || Phase2Config.betrayalLeaves() > 20) return Result.fail("the betrayal's floor is not bounded");
        } finally {
            e.discard();
        }
        return Result.ok("devil phase2 trial: health " + low[0] + "/" + mid[0] + "/" + high[0] + "/" + top[0] + " by rank, health scale bounded, a stranger can not hurt it, it never saves or despawns, results unlock the kinds exactly; max " + Phase2Config.trialMax() + " at once");
    }

    // ---------------------------------------------------------------- the save
    private static Result save(MinecraftServer server) {
        List<WorldDevil> ds = weak(3);
        if (ds.size() < 3) return Result.fail("the roster has fewer than three lesser devils");
        DevilLedger l = new DevilLedger();
        UUID a = id("save-a"), b = id("save-b");
        l.join(a, ds.get(0).id(), Kind.SUBMISSION, 11L);
        l.join(a, ds.get(1).id(), Kind.DOMINANCE, 12L);
        l.join(b, ds.get(0).id(), Kind.EVEN, 13L);
        l.ban(b, ds.get(2).id());
        l.setDebuff(b, 5000L);
        DevilLedger back = DevilLedger.load(l.save(new CompoundTag(), server.registryAccess()), server.registryAccess());
        if (back.count(ds.get(0).id()) != 2 || back.count(ds.get(1).id()) != 1 || back.playerRows() != l.playerRows()) return Result.fail("the ledger lost rows in its save");
        if (back.pact(a, ds.get(0).id()) == null || back.pact(a, ds.get(0).id()).kind() != Kind.SUBMISSION || back.pact(a, ds.get(1).id()).since() != 12L) return Result.fail("a pact lost its kind or its day");
        if (!back.banned(b, ds.get(2).id()) || back.debuffUntil(b) != 5000L) return Result.fail("the ban list or the debuff did not survive");
        CompoundTag damaged = l.save(new CompoundTag(), server.registryAccess());
        net.minecraft.nbt.ListTag players = damaged.getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag row = players.getCompound(i);
            net.minecraft.nbt.ListTag pacts = row.getList("pacts", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int k = 0; k < DevilLedger.MAX_PACTS * 3; k++) {
                CompoundTag extra = new CompoundTag();
                extra.putString("devil", "extra_" + k);
                extra.putString("kind", k % 2 == 0 ? "EVEN" : "junk");
                pacts.add(extra);
            }
            row.put("pacts", pacts);
        }
        DevilLedger grown = DevilLedger.load(damaged, server.registryAccess());
        if (grown.pacts(a).size() > DevilLedger.MAX_PACTS || grown.pacts(b).size() > DevilLedger.MAX_PACTS) return Result.fail("a damaged file grew a player past " + DevilLedger.MAX_PACTS + " pacts");
        for (DevilLedger.Pact pa : grown.pacts(a)) if (pa.kind() == null) return Result.fail("a damaged kind was loaded");
        return Result.ok("devil phase2 save: pacts, kinds, days, bans and the debuff survive; a damaged file can not grow past " + DevilLedger.MAX_PACTS + " pacts and bad kinds are dropped");
    }
}
