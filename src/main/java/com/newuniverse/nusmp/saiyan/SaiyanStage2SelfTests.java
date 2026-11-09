package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: the CI server smoke test of the 3.65.0 Saiyan work (prefixes {@code saiyan.stage2} and {@code saiyan.goku} in SMOKE_EXPECT). Nothing here can be seen by its author in a game, so the real game checks it.
 * <ul>
 * <li>{@code saiyan.stage2.rules}: the numbers the doc promises for Ki Flight, the senses, the dodge and adaptation caps, Zenkai and the training caps, and the save format of every new field.</li>
 * <li>{@code saiyan.stage2.player}: on a stand-in player: Ki Flight gives and takes back exactly its own flight, a client that moves too fast loses it, the burst needs a Master, the radar and the
 * suppression switch on and off, a recovered near-death becomes ONE Zenkai (the cool-down holds), and none of it spawns an entity.</li>
 * <li>{@code saiyan.goku.rules}: the Ultimate registry is restricted to the Saiyan race, the awakening requirements, the Kamehameha and Rock Scissors Paper numbers, the form roster (sixteen forms, no ape,
 * the owner's chart order), the unlock rules and the Kaio-ken strain.</li>
 * <li>{@code saiyan.goku.player}: a Saiyan that meets the requirements awakens Son Goku and a non-Saiyan can not; forms unlock from training, enter and leave with their modifiers, drain Ki, are refused
 * when locked; a Kamehameha charges, fires, starts its cool-down and spawns nothing; the combo advances Rock, Scissors, Paper and then needs its cool-down.</li>
 * </ul>
 * Server safe: no client class is touched. The stand-in has no connection, so no packet is sent; the parts that need Tensura's magicule data say so in the result if the stand-in has none.
 */
public final class SaiyanStage2SelfTests {
    private SaiyanStage2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.stage2.rules", SaiyanStage2SelfTests::stage2Rules);
        SelfTests.register("saiyan.stage2.player", SaiyanStage2SelfTests::stage2Player);
        SelfTests.register("saiyan.goku.rules", SaiyanStage2SelfTests::gokuRules);
        SelfTests.register("saiyan.goku.player", SaiyanStage2SelfTests::gokuPlayer);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-6; }

    // ================================================================== pure checks of the game's own classes
    private static Result stage2Rules(MinecraftServer server) {
        if (SaiyanFlightRules.tierOf(0) != SaiyanFlightRules.Tier.NOVICE || SaiyanFlightRules.tierOf(120) != SaiyanFlightRules.Tier.APPRENTICE || SaiyanFlightRules.tierOf(5400) != SaiyanFlightRules.Tier.MASTER) return Result.fail("the flight tiers moved");
        for (int i = 1; i < SaiyanFlightRules.SPEED.length; i++) if (!(SaiyanFlightRules.SPEED[i] > SaiyanFlightRules.SPEED[i - 1]) || !(SaiyanFlightRules.KI_PER_SECOND[i] > SaiyanFlightRules.KI_PER_SECOND[i - 1])) return Result.fail("flight speed or cost does not rise with the tier");
        if (SaiyanFlightRules.speed(SaiyanFlightRules.Tier.MASTER, 100.0) > SaiyanFlightRules.MAX_SPEED + 1.0e-9) return Result.fail("the flight speed passed its hard cap");
        if (SaiyanFlightRules.canBurst(SaiyanFlightRules.Tier.EXPERT) || !SaiyanFlightRules.canBurst(SaiyanFlightRules.Tier.MASTER) || !SaiyanFlightRules.hoverLimited(SaiyanFlightRules.Tier.NOVICE)) return Result.fail("burst or hover rules moved");
        if (SaiyanFlightRules.speedOk(500.0, SaiyanFlightRules.SPEED[0], false) || !SaiyanFlightRules.speedOk(5.0, SaiyanFlightRules.SPEED[2], false) || !SaiyanFlightRules.speedOk(100.0, SaiyanFlightRules.SPEED[0], true)) return Result.fail("the server-side speed check moved");
        if (SaiyanSenseRules.Tier.BEGINNER.radius != 16.0 || SaiyanSenseRules.Tier.INTERMEDIATE.radius != 32.0 || SaiyanSenseRules.Tier.ADVANCED.radius != 64.0 || SaiyanSenseRules.Tier.MASTER.radius != 128.0) return Result.fail("the sense radii moved");
        if (SaiyanSenseRules.detectable(20.0, 64.0, true, false, false) || !SaiyanSenseRules.detectable(10.0, 64.0, true, false, false) || SaiyanSenseRules.detectable(20.0, 64.0, false, true, false)) return Result.fail("stealth does not shorten the senses");
        if (SaiyanSenseRules.MAX_ENTRIES > 32 || SaiyanSenseRules.signature(1.0, 1000.0) != 0 || SaiyanSenseRules.signature(1.0e6, 1000.0) != 4) return Result.fail("the signature classes or the ping size moved");
        if (SaiyanCombatRules.reflexChance(0.9, 0.9, 1.0e9) > SaiyanCombatRules.HARD_DODGE_CAP + 1.0e-9 || SaiyanCombatRules.capped(0.7, 0.5) > SaiyanCombatRules.HARD_DODGE_CAP + 1.0e-9) return Result.fail("a dodge chance passed the hard cap");
        if (SaiyanCombatRules.adaptation(1_000_000, 0.99, 10) > SaiyanCombatRules.HARD_ADAPT_CAP + 1.0e-9 || SaiyanCombatRules.adaptation(0, 0.15, 10) != 0.0) return Result.fail("adaptation passed its cap or starts above zero");
        if (SaiyanConfig.reflexMax() > 0.75 + 1.0e-9 || SaiyanConfig.adaptMax() > 0.5 + 1.0e-9) return Result.fail("a passive's config passed its hard cap");
        if (!(SaiyanCombatRules.zenkaiGain(1000.0, 0.05, 5, 0.25) < SaiyanCombatRules.zenkaiGain(1000.0, 0.05, 0, 0.25)) || SaiyanCombatRules.zenkaiAllowed(100, 0, 24000, 0, 1) || !SaiyanCombatRules.zenkaiAllowed(30000, 0, 24000, 0, 1)) return Result.fail("Zenkai does not diminish or its cool-down moved");
        if (SaiyanTraining.room(SaiyanTraining.Cat.DIVINE, 3.0, 1.0) != 0.0 || SaiyanTraining.UNIVERSAL.size() != 5) return Result.fail("training caps or the technique list moved");

        SaiyanState s = new SaiyanState();
        s.saiyan = true;
        s.awakened = true;
        s.use("flight", 700);
        s.use("sense", 40);
        s.zenkai = 3;
        s.zenkaiAt = 1234;
        s.ultimate = SaiyanUltimates.GOKU;
        s.hardMartialKills = 4;
        s.trialKills = 7;
        s.train(SaiyanTraining.Cat.KI, 5.0, 1000, 1.0);
        s.addMastery("super_saiyan", 0.4);
        s.unlock("super_saiyan");
        s.grantFlag("limit_breaker");
        s.selectedForm = "super_saiyan";
        s.strain = 42.0;
        SaiyanState back = SaiyanTag.fromTag(SaiyanTag.toTag(s));
        if (back.uses("flight") != 700 || back.uses("sense") != 40 || back.zenkai != 3 || !SaiyanUltimates.GOKU.equals(back.ultimate) || back.hardMartialKills != 4 || back.trialKills != 7 || !(back.trained(SaiyanTraining.Cat.KI) > 4.9)
                || !near(back.mastery("super_saiyan"), 0.4) || !back.unlocked.contains("super_saiyan") || !back.flags.contains("limit_breaker") || !"super_saiyan".equals(back.selectedForm) || !near(back.strain, 42.0)) return Result.fail("a new Stage 2 field did not round-trip in the save format");
        return Result.ok("flight tiers/caps/speed check, sense radii/stealth/ping size, dodge and adaptation hard caps, Zenkai cool-down and diminishing return, training caps, and the save format of every new field hold");
    }

    // ================================================================== a stand-in: flight, senses, combat passives
    private static Result stage2Player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan2", at.x, at.y, at.z);
        try {
            SaiyanManager.setSaiyan(p, true);
            SaiyanState s = SaiyanStore.of(p);
            s.awakened = true;
            s.battlePower = 5000.0;
            SaiyanManager.Run r = SaiyanManager.runOf(p);
            long now = level.getGameTime();
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = SaiyanKi.max(p);
            AABB box = new AABB(at, at).inflate(48);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            String part;
            if (ex != null && max > 1.0) {
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                s.uses.clear();
                // Ki Flight: gives exactly its own flight, takes it back, never touches a creative flight
                boolean own = !p.isCreative() && !p.isSpectator() && !p.getAbilities().mayfly;
                if (!SaiyanFlight.toggle(p, s, r, now) || !r.flying) return Result.fail("the flight key did not start the flight");
                if (own && (!p.getAbilities().mayfly || !p.getPersistentData().getBoolean(SaiyanFlight.MARK))) return Result.fail("Ki Flight did not give and mark its flight");
                if (SaiyanFlight.tier(s) != SaiyanFlightRules.Tier.NOVICE) return Result.fail("a new Saiyan is not a Novice flier");
                if (SaiyanAbilities.pay(p, s, 0.0, 0.0) && SaiyanFlight.burst(p, s, r, now)) return Result.fail("a Novice burst");
                if (own) {
                    SaiyanFlight.second(p, s, r, now, 5000.0);                         // a client that covered 5000 blocks in a second
                    if (r.flying || p.getAbilities().mayfly || p.getPersistentData().getBoolean(SaiyanFlight.MARK)) return Result.fail("a flier that moved too fast kept the flight");
                    if (r.safeFallUntil <= now) return Result.fail("ending the flight gave no landing grace");
                    SaiyanFlight.toggle(p, s, r, now);
                }
                s.uses.put("flight", 5400);
                if (SaiyanFlight.tier(s) != SaiyanFlightRules.Tier.MASTER) return Result.fail("5400 flight seconds are not a Master");
                if (!r.flying) SaiyanFlight.toggle(p, s, r, now);
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                if (own && (!SaiyanFlight.burst(p, s, r, now) || s.ready("burst", now))) return Result.fail("a Master's burst did not work and start its cool-down");
                SaiyanFlight.stop(p, r, now);
                if (r.flying || (own && (p.getAbilities().mayfly || p.getPersistentData().getBoolean(SaiyanFlight.MARK)))) return Result.fail("stopping the flight did not take it back");

                // senses and suppression
                ex.setMagicule(max);
                if (!SaiyanSenses.sense(p, s, r, now) || r.radarLeft <= 0 || s.ready("sense", now)) return Result.fail("the senses did not switch on and start their cool-down");
                SaiyanSenses.radarTick(p, s, r, now);
                if (r.lastPing != now) return Result.fail("the radar did not scan");
                SaiyanSenses.second(p, s, r);
                if (!SaiyanSenses.suppress(p, s, r, now) || !SaiyanSenses.suppressed(p)) return Result.fail("Ki Suppression did not switch on");
                SaiyanSenses.suppress(p, s, r, now);
                if (SaiyanSenses.suppressed(p)) return Result.fail("Ki Suppression did not switch off");
                part = "Ki Flight (own flight given and taken back, speed check, landing grace, Novice/Master burst), the radar and Ki Suppression";
            } else {
                part = "flight, senses and suppression NOT exercised (the stand-in has no Tensura magicule data)";
            }

            // a recovered near-death becomes one Zenkai, and the cool-down holds
            double bp = s.battlePower;
            s.zenkai = 0;
            s.zenkaiAt = -1;
            s.zenkaiDay = -1;
            s.zenkaiToday = 0;
            r.nearDeathAt = Math.max(0L, now - 1000);
            r.lastDamaged = now - 1000;
            p.setHealth(p.getMaxHealth());
            SaiyanCombat.second(p, s, r, now);
            if (s.zenkai != 1 || !(s.battlePower > bp)) return Result.fail("a recovered near-death gave no Zenkai");
            r.nearDeathAt = Math.max(0L, now - 1000);
            SaiyanCombat.second(p, s, r, now);
            if (s.zenkai != 1) return Result.fail("a second Zenkai came inside the cool-down");
            if (SaiyanCombat.kindOf(p.damageSources().magic()) != SaiyanCombatRules.Kind.MAGIC || SaiyanCombat.kindOf(p.damageSources().inFire()) != SaiyanCombatRules.Kind.FIRE) return Result.fail("the damage kinds are not told apart");

            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("Stage 2 spawned " + (after - before) + " entities");
            return Result.ok(part + "; Zenkai (one, cool-down holds), the damage kinds; no entity spawned");
        } finally {
            SaiyanFlight.release(p);
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.uses.clear();
                s.zenkai = 0;
                s.zenkaiAt = -1;
                s.readyAt.clear();
                s.stamina = -1.0;
            }
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }

    // ================================================================== pure checks: the Goku path and the forms
    private static Result gokuRules(MinecraftServer server) {
        SaiyanUltimates.Ultimate goku = SaiyanUltimates.byId(SaiyanUltimates.GOKU);
        if (goku == null || !SaiyanUltimates.openTo(goku, "saiyan") || SaiyanUltimates.openTo(goku, "human") || SaiyanUltimates.openTo(goku, "") || SaiyanUltimates.mayTake("goku") || !SaiyanUltimates.mayTake("")) return Result.fail("the Ultimate registry is not restricted to the Saiyan race");
        SaiyanUltimates.Need need = SaiyanUltimates.Need.GOKU_DEFAULT;
        if (SaiyanUltimates.ready(need, new SaiyanUltimates.Progress(0, 0, 0, 0, 0, 0, 0)) || !SaiyanUltimates.ready(need, new SaiyanUltimates.Progress(need.bp(), need.techniques(), need.flightTier(), need.senseTier(), need.hardMartialKills(), need.zenkai(), need.trialKills()))
                || SaiyanUltimates.missing(need, new SaiyanUltimates.Progress(0, 0, 0, 0, 0, 0, 0)).size() != 7) return Result.fail("the awakening requirements moved");
        SaiyanUltimates.Need set = SaiyanConfig.gokuNeed();
        if (set.bp() <= 0.0 || set.techniques() > 5 || set.flightTier() > 4 || set.senseTier() > 3) return Result.fail("a configured awakening requirement is out of bounds");

        double fast = GokuRules.maxChargeSeconds(6.0, 1.0), slow = GokuRules.maxChargeSeconds(6.0, 0.0);
        if (!(fast < slow) || fast < 0.5 || !near(GokuRules.charge(1_000_000, 6.0), 1.0) || GokuRules.canFire(3, 10) || !GokuRules.canFire(10, 10)) return Result.fail("the Kamehameha charge numbers moved");
        if (!(GokuRules.damage(40.0, 1.0) > GokuRules.damage(40.0, 0.0)) || !(GokuRules.range(56.0, 1.0) > GokuRules.range(56.0, 0.0)) || GokuRules.pierce(0.0) != 1 || GokuRules.pierce(1.0) != 8 || GokuRules.kiPercent(12.0, 1.0) != 12.0) return Result.fail("a Kamehameha number does not grow with the charge");
        if (GokuRules.nextStage(0, 100, 0, 6, 30) != GokuRules.ROCK || GokuRules.nextStage(1, 110, 100, 6, 30) != GokuRules.SCISSORS || GokuRules.nextStage(2, 120, 110, 6, 30) != GokuRules.PAPER
                || GokuRules.nextStage(1, 102, 100, 6, 30) != 0 || GokuRules.nextStage(1, 200, 100, 6, 30) != GokuRules.ROCK || GokuRules.nextStage(3, 140, 120, 6, 30) != GokuRules.ROCK) return Result.fail("the Rock Scissors Paper chain moved");

        if (SaiyanForms.ALL.size() != 16) return Result.fail("the form roster is not the owner's chart minus the apes (16 forms)");
        Set<String> ids = new HashSet<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            if (!ids.add(f.id()) || f.id().contains("ape") || f.id().contains("oozaru")) return Result.fail("a duplicate or ape form: " + f.id());
            if (f.drain() < 0.0 || f.damage() < 0.0 || f.defense() < 0.0 || f.cooldown() < 0 || f.masterySeconds() <= 0) return Result.fail("a form has a negative number: " + f.id());
            if (f.parent() != null && SaiyanForms.byId(f.parent()) == null || f.parent2() != null && SaiyanForms.byId(f.parent2()) == null) return Result.fail("a form names a parent that does not exist: " + f.id());
            if (SaiyanForms.ofCode(SaiyanForms.code(f.id())) != f) return Result.fail("the form code does not round-trip: " + f.id());
        }
        if (SaiyanForms.SUPER_SAIYAN_3.speed() >= 0.0 || SaiyanForms.SUPER_SAIYAN_3.attackSpeed() >= 0.0) return Result.fail("Super Saiyan 3 lost its speed penalty");
        if (!SaiyanForms.SUPER_SAIYAN_4.goku() || SaiyanForms.SUPER_SAIYAN.goku() || !SaiyanForms.TRUE_ULTRA_INSTINCT.goku()) return Result.fail("the Goku-only branches moved");
        if (SaiyanForms.dodge("perfected_ultra_instinct", 0.0) == null || SaiyanForms.dodge("super_saiyan", 0.5) != null || SaiyanForms.dodge("true_ultra_instinct", 1.0).chance() > SaiyanCombatRules.HARD_DODGE_CAP) return Result.fail("the Ultra Instinct dodges moved or pass the hard cap");
        SaiyanForms.Progress none = new SaiyanForms.Progress(1.0e12, 99, false, Set.of(), java.util.Map.of(), java.util.Map.of());
        if (SaiyanForms.meets(SaiyanForms.SUPER_SAIYAN_4, SaiyanConfig.formMinBp(SaiyanForms.SUPER_SAIYAN_4), none)) return Result.fail("a Saiyan without the Goku Ultimate could unlock a Goku form");
        if (SaiyanForms.meets(SaiyanForms.SUPER_SAIYAN, 1.0, new SaiyanForms.Progress(1.0e12, 99, false, Set.of(), java.util.Map.of(), java.util.Map.of()))) return Result.fail("a form unlocked without its training");
        if (SaiyanForms.kaioMaxStage(0.0) != 1 || SaiyanForms.kaioMaxStage(1.0) != 3 || !(SaiyanForms.strainAfter(0.0, true, 3, 10.0) > SaiyanForms.strainAfter(0.0, true, 1, 10.0)) || SaiyanForms.strainAfter(50.0, false, 1, 10.0) >= 50.0
                || SaiyanForms.strainPenalty(100.0) > 0.4 + 1.0e-9) return Result.fail("the Kaio-ken strain rules moved");
        if (!(SaiyanForms.effect(1.0) > SaiyanForms.effect(0.0)) || !(SaiyanForms.drainFactor(1.0) < SaiyanForms.drainFactor(0.0))) return Result.fail("mastery does not strengthen and cheapen a form");
        for (SaiyanForms.Form f : SaiyanForms.ALL) if (SaiyanConfig.formDamage(f) < 0.0 || SaiyanConfig.formDefense(f) < 0.0 || SaiyanConfig.formDrain(f) < 0.0 || SaiyanConfig.formMinBp(f) < SaiyanRules.START_BP) return Result.fail("the config of the form " + f.id() + " is out of bounds");
        return Result.ok("the Ultimate registry is Saiyan-only, the awakening requirements, the Kamehameha and Rock Scissors Paper numbers, the 16-form roster (no ape, parents exist, codes round-trip), the Goku gate, the Ultra Instinct dodges, Kaio-ken strain and mastery all hold");
    }

    // ================================================================== a stand-in: the awakening, the forms, the Kamehameha and the combo
    private static Result gokuPlayer(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan3", at.x, at.y, at.z);
        try {
            SaiyanManager.setSaiyan(p, true);
            SaiyanState s = SaiyanStore.of(p);
            SaiyanManager.Run r = SaiyanManager.runOf(p);
            long now = level.getGameTime();
            s.awakened = true;
            s.battlePower = 200_000.0;

            // the awakening: not before the requirements, and only for a Saiyan
            GokuAbilities.awakeningCheck(p, s, r);
            if (!s.ultimate.isEmpty()) return Result.fail("a Saiyan without the requirements awakened the Ultimate");
            for (String t : SaiyanTraining.UNIVERSAL) s.uses.put(t, SaiyanTraining.masteryNeed(t));
            s.zenkai = 5;
            s.hardMartialKills = 5;
            s.trialKills = 20;
            s.saiyan = false;
            GokuAbilities.awakeningCheck(p, s, r);
            s.saiyan = true;
            if (!s.ultimate.isEmpty()) return Result.fail("a player who is not a Saiyan awakened a Saiyan Ultimate");
            GokuAbilities.awakeningCheck(p, s, r);
            if (!GokuAbilities.isGoku(s)) return Result.fail("a Saiyan who met every requirement did not awaken Son Goku");

            // the forms: unlock from training, refuse when locked, enter and leave with their modifiers
            for (SaiyanTraining.Cat c : SaiyanTraining.Cat.values()) s.training.put(c.id(), 500.0);
            SaiyanFormEngine.unlockCheck(p, s, r);
            if (!s.unlocked.contains("super_saiyan") || !s.unlocked.contains("kaioken")) return Result.fail("the training and Battle Power did not unlock the first forms");
            if (s.unlocked.contains("super_saiyan_4_limit_breaker")) return Result.fail("the Limit Breaker unlocked without its operator flag");
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = SaiyanKi.max(p);
            AABB box = new AABB(at, at).inflate(48);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            String part;
            if (ex != null && max > 1.0) {
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                if (SaiyanFormEngine.activate(p, s, r, SaiyanForms.byId("super_saiyan_blue"), now, true)) return Result.fail("a locked form could be entered");
                SaiyanAttributes.clear(p);
                if (!SaiyanFormEngine.activate(p, s, r, SaiyanForms.SUPER_SAIYAN, now, true) || !"super_saiyan".equals(r.form) || !SaiyanAttributes.any(p)) return Result.fail("Super Saiyan did not start and put its modifiers on");
                if (!(SaiyanFormEngine.damageFactor(s, r) > 1.0) || !(SaiyanFormEngine.takenFactor(s, r) < 1.0)) return Result.fail("a form changed neither damage nor defence");
                double kiBefore = SaiyanKi.current(p);
                SaiyanFormEngine.second(p, s, r, now);
                if (!(SaiyanKi.current(p) < kiBefore)) return Result.fail("a form did not drain Ki");
                SaiyanFormEngine.deactivate(p, s, r, now, "form_off");
                SaiyanAttributes.clear(p);
                if (!r.form.isEmpty() || SaiyanAttributes.any(p) || now >= r.formReadyAt || !near(SaiyanFormEngine.damageFactor(s, r), 1.0)) return Result.fail("leaving a form left a modifier, a bonus or no cool-down");
                ex.setMagicule(max);
                r.formReadyAt = 0;
                if (!SaiyanFormEngine.activate(p, s, r, SaiyanForms.KAIOKEN, now, true)) return Result.fail("Kaio-ken did not start");
                for (int i = 0; i < 5; i++) SaiyanFormEngine.second(p, s, r, now);
                if (!(s.strain > 0.0)) return Result.fail("Kaio-ken built no strain");
                SaiyanFormEngine.deactivate(p, s, r, now, "form_off");
                SaiyanAttributes.clear(p);

                // Kamehameha: charge, release, cool-down, mastery, nothing spawned
                ex.setMagicule(max);
                s.readyAt.clear();
                if (!GokuAbilities.kameStart(p, s, r, now) || !r.charging) return Result.fail("the Kamehameha did not start charging");
                if (!GokuAbilities.kameRelease(p, s, r, now + 100) || r.charging || s.ready("kame", now) || s.uses("kame") != 1) return Result.fail("the Kamehameha did not fire and start its cool-down");
                if (GokuAbilities.kameStart(p, s, r, now)) return Result.fail("the Kamehameha ignored its cool-down");
                ex.setMagicule(max);
                s.readyAt.clear();
                GokuAbilities.kameStart(p, s, r, now);
                GokuAbilities.kameRelease(p, s, r, now + 1);
                if (s.uses("kame") != 1) return Result.fail("a Kamehameha released too soon fired");

                // Rock, Scissors 'N' Paper
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                s.readyAt.clear();
                r.comboStage = 0;
                if (!GokuAbilities.combo(p, s, r, now) || r.comboStage != GokuRules.ROCK) return Result.fail("the combo did not start with Rock");
                if (GokuAbilities.combo(p, s, r, now + 2)) return Result.fail("a combo part came before the timing gap");
                if (!GokuAbilities.combo(p, s, r, now + 12) || r.comboStage != GokuRules.SCISSORS) return Result.fail("the combo did not go on to Scissors");
                if (!GokuAbilities.combo(p, s, r, now + 24) || r.comboStage != 0 || s.ready("combo", now + 24) || s.uses("combo") != 1) return Result.fail("the combo did not finish with Paper and start its cool-down");
                part = "form unlock/enter/leave/drain/strain, the Kamehameha (charge, fizzle, cool-down) and Rock Scissors Paper (timing, order, cool-down)";
            } else {
                part = "forms, Kamehameha and the combo NOT exercised (the stand-in has no Tensura magicule data)";
            }
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("a Goku technique spawned " + (after - before) + " entities");
            return Result.ok("the awakening (gated by the requirements and the race), " + part + "; no entity spawned");
        } finally {
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.uses.clear();
                s.zenkai = 0;
                s.ultimate = "";
                s.hardMartialKills = 0;
                s.trialKills = 0;
                s.training.clear();
                s.unlocked.clear();
                s.formMastery.clear();
                s.flags.clear();
                s.selectedForm = "";
                s.strain = 0.0;
                s.readyAt.clear();
                s.stamina = -1.0;
            }
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
