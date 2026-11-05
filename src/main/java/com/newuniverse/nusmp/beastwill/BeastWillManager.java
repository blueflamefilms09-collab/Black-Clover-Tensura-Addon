package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.shadow.ShadowMana;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the one place where a Beast Will LIVES (3.45.0): it turns a player's asks (form, ability, swap, yes, no) into the changes of {@link BeastWillState}, runs the once-a-second upkeep of the few players that
 * have a will, and keeps the client in step. The rules themselves are elsewhere (numbers and bounds in {@link BeastWillRules}, the data in {@link BeastWillDef}, what a kind of ability does in
 * {@link BeastWillAbilities}), so this class is only the order in which they are applied.
 *
 * <p>The cost for everybody else is one {@code hasData} lookup per player per tick; a player with a will gets one small upkeep a second and at most four sync packets a second (only when something
 * changed). Server thread only; the per-player runtime state ({@link Run}) is in memory, keyed by UUID, and dropped at logout.
 *
 * <p>The rules a player can feel: the form needs the unlock, energy for one second of drain, no lockout and no Integrate-blocking transformation; holding it drains energy and adds strain (more with a poor
 * fit and before you are used to it); strain makes the will weaker (30 and 60), at 60 adds weakness, at 85 ends the form and hurts a little (never below 4 health, never a death), and only a deliberate
 * second press of an ability at the brink can reject the will (lockout, a tenth of the assimilation lost; nothing else is ever taken). Resting sheds strain, meditating sheds it three times as fast and is
 * how the first assimilation is made.
 */
public final class BeastWillManager {
    private BeastWillManager() {}

    /** Per-player runtime state (not saved). */
    static final class Run {
        boolean dirty = true;
        long lastSent = -100;
        int lastHash = Integer.MIN_VALUE;
        boolean meditating;
        long lastActive, lastDamaged = -1000, lastAction = -100, lastCombatGain = -1000, pushWarnUntil, auraUntil;
        double lx = Double.NaN, lz = Double.NaN;
        String applied = "";
        /** The will whose {@link BeastWillNet.Info} the client has (empty = none yet, so the next send includes it). */
        String infoFor = "";
        /** The highest phase already announced as unlocked, per will (filled silently the first time). */
        final Map<String, Integer> announced = new HashMap<>();
    }

    private static final Map<UUID, Run> RUN = new ConcurrentHashMap<>();

    /** Players with a runtime record (the self-test and the stopping hook read it). */
    static int runs() { return RUN.size(); }

    /** The runtime record of the player, created if missing (the self-test reads it). */
    static Run runOf(ServerPlayer p) { return RUN.computeIfAbsent(p.getUUID(), u -> new Run()); }

    public static void markDirty(ServerPlayer p) {
        Run r = RUN.computeIfAbsent(p.getUUID(), u -> new Run());
        r.dirty = true;
    }

    // ================================================================== the tick
    /** Called for every server player every tick; costs one lookup for a player with no will. */
    public static void tick(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null) return;
        Run r = RUN.get(p.getUUID());
        if (r == null) {
            if (s.wills.isEmpty()) return;
            r = new Run();
            RUN.put(p.getUUID(), r);
        }
        long now = p.level().getGameTime();
        if (BeastWillAbilities.hasPending(p.getUUID())) BeastWillAbilities.tickPending(p);
        BeastWillStealth.tick(p);
        if ((p.tickCount % 20) == 0) second(p, s, r, now);
        if (r.dirty && now - r.lastSent >= 5) send(p, s, r, now);
    }

    // ================================================================== the second
    private static void second(ServerPlayer p, BeastWillState s, Run r, long now) {
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (def == null || prog == null || !BeastWillConfig.enabled()) {
            if (s.phase != BeastWillRules.OFF) endForm(p, s, r, "gone");
            else if (!r.applied.isEmpty()) {
                BeastWillAttributes.clear(p);
                r.applied = "";
            }
            updateHash(p, s, r, now);
            return;
        }
        double compat = compatOf(p, def);
        boolean moved = Double.isNaN(r.lx) || Math.abs(p.getX() - r.lx) + Math.abs(p.getZ() - r.lz) > 0.3;
        r.lx = p.getX();
        r.lz = p.getZ();
        if (moved) r.lastActive = now;
        boolean active = now - r.lastActive < 200;
        r.meditating = BeastWillConfig.meditation() && s.phase == BeastWillRules.OFF && p.isShiftKeyDown() && !moved && p.onGround() && !p.isUsingItem() && now - r.lastDamaged > 100;

        BeastWillRules.Tier tier = BeastWillConfig.penalties() ? BeastWillRules.tier(s.strain) : BeastWillRules.Tier.CALM;
        double rate = 1.0 / Math.max(1.0, def.assimilationSeconds());

        if (s.phase != BeastWillRules.OFF) {
            BeastWillDef.Phase ph = def.phase(s.phase);
            String stop = null;
            if (ph == null || !BeastWillRules.phaseUnlocked(s.phase, prog.assimilation, prog.mastery, ph.unlockAssimilation(), ph.unlockMastery())) stop = "gone";
            else if (s.locked(now)) stop = "locked_out";
            else if (conflict(p, s.phase)) stop = "conflict";
            if (stop != null) {
                endForm(p, s, r, stop);
            } else {
                double cost = ShadowMana.cost(p, ph.drainPct() * BeastWillConfig.drainScale());
                if (!ShadowMana.canPay(p, cost)) {
                    endForm(p, s, r, "out_of_energy");
                } else {
                    ShadowMana.pay(p, cost);
                    s.addStrain(BeastWillRules.secondStrain(ph.strainPerSecond(), compat, BeastWillRules.prematureMult(s.phase, prog.assimilation, prog.mastery), BeastWillConfig.strainRate()));
                    double idle = active ? 1.0 : 0.25;
                    s.gainAssimilation(prog, rate * BeastWillConfig.assimilationRate() * compat * idle, def.assimilationDayCap(), now);
                    s.gainMastery(prog, 0.3 / Math.max(1.0, def.masterySeconds()) * BeastWillConfig.masteryRate() * idle, def.masteryDayCap(), now);
                    tier = BeastWillConfig.penalties() ? BeastWillRules.tier(s.strain) : BeastWillRules.Tier.CALM;
                    if (tier == BeastWillRules.Tier.OVERLOAD) {
                        overload(p, s, r, now);
                    } else {
                        double scale = BeastWillRules.statScale(prog.assimilation, prog.mastery, tier, BeastWillConfig.statScale());
                        if (ph.regen() > 0 && p.getHealth() < p.getMaxHealth()) p.heal((float) (ph.regen() * scale));
                        if (tier == BeastWillRules.Tier.EXHAUSTED) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
                        if (tier != BeastWillRules.Tier.CALM) BeastWillFeedback.say(p, tier == BeastWillRules.Tier.EXHAUSTED ? "exhausted" : "strained", false);
                        refreshAura(p, def, s.phase, r, now);
                    }
                }
            }
        } else {
            s.addStrain(-BeastWillRules.restDecay(BeastWillConfig.restPerSecond(), r.meditating));
            if (r.meditating) {
                s.gainAssimilation(prog, rate * BeastWillConfig.assimilationRate() * compat, def.assimilationDayCap(), now);
                s.gainMastery(prog, 0.05 / Math.max(1.0, def.masterySeconds()) * BeastWillConfig.masteryRate(), def.masteryDayCap(), now);
            }
        }
        announceUnlocks(p, s, r);
        reapply(p, s, def, prog, r);
        updateHash(p, s, r, now);
    }

    // ================================================================== the form
    private static boolean conflict(ServerPlayer p, int phase) {
        return phase == BeastWillRules.INTEGRATE && BeastWillConfig.exclusiveIntegrate() && ModeArmor.active(p) != ModeArmor.Mode.NONE;
    }

    static double compatOf(ServerPlayer p, BeastWillDef def) {
        String race = MultiverseProfile.race(p).name();
        boolean listed = def.hostRace(race) || BeastWillConfig.extraHost(race);
        return BeastWillRules.compat(listed, def.otherHostCompat() * BeastWillConfig.otherRaceScale());
    }

    private static void startForm(ServerPlayer p, BeastWillState s, Run r, int phase, long now) {
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (def == null || prog == null || !BeastWillConfig.enabled()) return;
        BeastWillDef.Phase ph = def.phase(phase);
        if (ph == null) return;
        if (s.locked(now)) {
            BeastWillFeedback.say(p, "locked_out", false, (int) Math.ceil((s.lockUntil - now) / 20.0));
            return;
        }
        if (s.strain >= BeastWillRules.OVERLOAD_AT) {
            BeastWillFeedback.say(p, "too_strained", false);
            return;
        }
        if (!BeastWillRules.phaseUnlocked(phase, prog.assimilation, prog.mastery, ph.unlockAssimilation(), ph.unlockMastery())) {
            BeastWillFeedback.say(p, "not_unlocked", false, (int) Math.ceil(ph.unlockAssimilation() * 100), (int) Math.ceil(ph.unlockMastery() * 100));
            return;
        }
        if (conflict(p, phase)) {
            BeastWillFeedback.say(p, "conflict", false);
            return;
        }
        if (!ShadowMana.canPay(p, ShadowMana.cost(p, ph.drainPct() * BeastWillConfig.drainScale()))) {
            BeastWillFeedback.say(p, "no_energy", false);
            return;
        }
        s.phase = phase;
        r.lastActive = now;
        r.applied = "";
        BeastWillFeedback.say(p, phase == BeastWillRules.INTEGRATE ? "form_integrate" : "form_acquire", false, BeastWillText.name(def));
        reapply(p, s, def, prog, r);
        refreshAura(p, def, phase, r, now);
        r.dirty = true;
    }

    /** Ends the form safely: modifiers, aura, veil and any unfinished exclusion go; the strain stays (the body keeps it). */
    static void endForm(ServerPlayer p, BeastWillState s, Run r, String reason) {
        boolean was = s.phase != BeastWillRules.OFF;
        s.phase = BeastWillRules.OFF;
        BeastWillAbilities.cancel(p.getUUID());
        BeastWillStealth.forget(p);
        auraOff(p);
        r.auraUntil = 0;
        r.applied = "";
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (def != null && prog != null) reapply(p, s, def, prog, r);
        else BeastWillAttributes.clear(p);
        if (was && reason != null && !reason.isEmpty()) BeastWillFeedback.say(p, "end." + reason, false);
        r.dirty = true;
    }

    /** Strain reached the overload mark while the form was held: the form ends, a little health goes (never below 4, never a death) and the will goes quiet for 30 seconds. */
    private static void overload(ServerPlayer p, BeastWillState s, Run r, long now) {
        endForm(p, s, r, "overload");
        s.lockUntil = Math.max(s.lockUntil, now + 600);
        double dmg = Math.min(2.0 * BeastWillConfig.overloadDamage(), Math.max(0.0, p.getHealth() - 4.0));
        if (dmg > 0 && BeastWillConfig.penalties() && !p.isCreative()) p.hurt(p.damageSources().magic(), (float) dmg);
        BeastWillFeedback.say(p, "overload", true);
    }

    /** Rejection: the form ends, the will goes quiet, a tenth of the assimilation is lost; nothing else is taken. */
    private static void reject(ServerPlayer p, BeastWillState s, Run r, long now) {
        BeastWillAbilities.cancel(p.getUUID());
        BeastWillStealth.forget(p);
        auraOff(p);
        s.reject(now, BeastWillConfig.rejectLockTicks(), BeastWillConfig.rejectAssimLoss(), BeastWillRules.EXHAUSTED_AT);
        r.applied = "";
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (def != null && prog != null) reapply(p, s, def, prog, r);
        else BeastWillAttributes.clear(p);
        BeastWillFeedback.say(p, "rejected", true);
        r.dirty = true;
    }

    // ================================================================== modifiers and visuals
    /** The strength share the active will gives right now (phase 0 = only the passive, grown with the first assimilation). */
    static double scaleNow(BeastWillState s, BeastWillDef def, BeastWillState.Progress prog) {
        BeastWillRules.Tier tier = BeastWillConfig.penalties() ? BeastWillRules.tier(s.strain) : BeastWillRules.Tier.CALM;
        double scale = BeastWillRules.statScale(prog.assimilation, prog.mastery, tier, BeastWillConfig.statScale());
        if (s.phase == BeastWillRules.OFF) scale *= BeastWillRules.clamp(prog.assimilation / Math.max(0.01, def.acquire().unlockAssimilation()), 0, 1);
        return scale;
    }

    private static void reapply(ServerPlayer p, BeastWillState s, BeastWillDef def, BeastWillState.Progress prog, Run r) {
        double scale = scaleNow(s, def, prog);
        String key = def.id() + "|" + s.phase + "|" + Math.round(scale * 50);
        if (key.equals(r.applied)) return;
        BeastWillAttributes.apply(p, def, s.phase, scale);
        r.applied = key;
    }

    /** The aura style: the phase in bits 24..25, the look kind of the will in bits 26..29 ({@link BeastWillLook}, so the client can draw that beast's spirit), the tint below (the client has no copy of the data files). */
    public static int style(BeastWillDef def, int phase) { return BeastWillLook.style(phase, BeastWillLook.kindOf(def.id()), def.tint()); }

    /** Takes the aura off the player; a stand-in with no connection (the self-test) has nothing to tell. */
    private static void auraOff(ServerPlayer p) {
        if (p.connection != null) PlayerAuras.clear(p, Aura.BEAST_WILL);
    }

    private static void refreshAura(ServerPlayer p, BeastWillDef def, int phase, Run r, long now) {
        if (now + 40 > r.auraUntil) {
            if (p.connection != null) PlayerAuras.set(p, Aura.BEAST_WILL, 100, style(def, phase));
            r.auraUntil = now + 100;
        }
    }

    private static void announceUnlocks(ServerPlayer p, BeastWillState s, Run r) {
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (def == null || prog == null) return;
        int now = 0;
        if (BeastWillRules.phaseUnlocked(BeastWillRules.ACQUIRE, prog.assimilation, prog.mastery, def.acquire().unlockAssimilation(), def.acquire().unlockMastery())) now = BeastWillRules.ACQUIRE;
        if (BeastWillRules.phaseUnlocked(BeastWillRules.INTEGRATE, prog.assimilation, prog.mastery, def.integrate().unlockAssimilation(), def.integrate().unlockMastery())) now = BeastWillRules.INTEGRATE;
        Integer before = r.announced.put(def.id(), now);
        if (before != null && now > before) BeastWillFeedback.say(p, now == BeastWillRules.INTEGRATE ? "unlocked_integrate" : "unlocked_acquire", true, BeastWillText.name(def));
        else if (before != null && now < before) r.announced.put(def.id(), now);
    }

    // ================================================================== the asks
    public static void onAction(ServerPlayer p, int op, int arg) {
        BeastWillState s = BeastWillStore.peek(p);
        Run r = RUN.computeIfAbsent(p.getUUID(), u -> new Run());
        long now = p.level().getGameTime();
        if (now - r.lastAction < BeastWillNet.MIN_GAP_TICKS && now >= r.lastAction) return;                      // rate limit
        r.lastAction = now;
        if (op == BeastWillNet.OP_DECLINE) {
            BeastWillAcquire.decline(p);
            return;
        }
        if (op == BeastWillNet.OP_CONFIRM) {
            BeastWillAcquire.confirm(p);
            return;
        }
        if (s == null || s.active.isEmpty() || !BeastWillConfig.enabled() || !p.isAlive() || p.isSpectator()) return;
        switch (op) {
            case BeastWillNet.OP_FORM -> {
                if (s.phase == arg) endForm(p, s, r, "cancel");
                else startForm(p, s, r, arg, now);
            }
            case BeastWillNet.OP_ABILITY -> useAbility(p, s, r, arg, now);
            case BeastWillNet.OP_SWAP -> swap(p, s, r, arg, now);
            default -> { }
        }
    }

    private static void useAbility(ServerPlayer p, BeastWillState s, Run r, int slot, long now) {
        if (s.phase == BeastWillRules.OFF) {
            BeastWillFeedback.say(p, "need_form", false);
            return;
        }
        BeastWillDef def = BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        BeastWillDef.Ability a = def == null ? null : def.ability(s.phase, slot);
        if (a == null || prog == null) return;
        if (s.locked(now)) {
            BeastWillFeedback.say(p, "locked_out", false, (int) Math.ceil((s.lockUntil - now) / 20.0));
            return;
        }
        if (!s.ready(prog, a.kind(), now)) {
            BeastWillFeedback.say(p, "cooldown", false, (int) Math.ceil(s.left(prog, a.kind(), now) / 20.0));
            return;
        }
        double cost = ShadowMana.cost(p, a.costPct() * BeastWillConfig.drainScale());
        if (!ShadowMana.canPay(p, cost)) {
            BeastWillFeedback.say(p, "no_energy", false);
            return;
        }
        double compat = compatOf(p, def);
        double add = BeastWillRules.abilityStrain(a.strain(), compat, BeastWillConfig.strainRate());
        boolean brink = BeastWillConfig.rejection() && s.strain + add >= BeastWillRules.STRAIN_MAX;
        if (brink && now > r.pushWarnUntil) {                                              // the warning press: nothing happens, the HUD and the line say why
            r.pushWarnUntil = now + 60;
            BeastWillFeedback.say(p, "push_warning", true);
            r.dirty = true;
            return;
        }
        double scale = scaleNow(s, def, prog);
        if (!BeastWillAbilities.run(p, def, a, scale, prog.mastery)) return;                 // it did not take effect: nothing is charged
        ShadowMana.pay(p, cost);
        s.startCooldown(prog, a.kind(), now, BeastWillRules.cooldown(a.cooldownTicks(), BeastWillConfig.cooldownScale()));
        s.addStrain(add);
        if (!"shadow_veil".equals(a.kind())) BeastWillStealth.reveal(p);
        r.lastActive = now;
        s.gainMastery(prog, 20.0 / Math.max(1.0, def.masterySeconds()) * BeastWillConfig.masteryRate(), def.masteryDayCap(), now);
        if (brink && s.strain >= BeastWillRules.STRAIN_MAX) reject(p, s, r, now);
        r.dirty = true;
    }

    private static void swap(ServerPlayer p, BeastWillState s, Run r, int dir, long now) {
        if (s.wills.size() < 2) {
            BeastWillFeedback.say(p, "swap_none", false);
            return;
        }
        if (s.phase != BeastWillRules.OFF) {
            BeastWillFeedback.say(p, "swap_form", false);
            return;
        }
        if (s.swapLocked(now)) {
            BeastWillFeedback.say(p, "swap_locked", false, (int) Math.ceil((s.swapLockUntil - now) / 20.0));
            return;
        }
        List<String> ids = new ArrayList<>(s.wills.keySet());
        int i = Math.max(0, ids.indexOf(s.active));
        String next = ids.get(Math.floorMod(i + (dir < 0 ? -1 : 1), ids.size()));
        if (next.equals(s.active)) return;
        s.activate(next, now + BeastWillConfig.swapLockTicks());
        r.applied = "";
        BeastWillDef def = BeastWillCatalog.get(next);
        BeastWillState.Progress prog = s.activeProgress();
        if (def != null && prog != null) reapply(p, s, def, prog, r);
        else BeastWillAttributes.clear(p);
        BeastWillFeedback.say(p, "swapped", false, BeastWillText.name(next));
        r.dirty = true;
    }

    // ================================================================== game events (called by BeastWillEvents)
    /** The player was hurt (any source): the veil is lost, and a recent hit stops meditation. */
    static void onHurt(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null) return;
        Run r = RUN.get(p.getUUID());
        if (r != null) r.lastDamaged = p.level().getGameTime();
        BeastWillStealth.reveal(p);
    }

    /** The player hit a creature: the veil is lost, and a foe hit in form gives a little mastery (at most one every two seconds, inside the day's cap). */
    static void onHit(ServerPlayer p, LivingEntity target) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null || s.phase == BeastWillRules.OFF) return;
        Run r = RUN.get(p.getUUID());
        long now = p.level().getGameTime();
        if (r != null) r.lastActive = now;
        BeastWillStealth.reveal(p);
        BeastWillDef def = BeastWillCatalog.get(s.active);
        BeastWillState.Progress prog = s.activeProgress();
        if (r == null || def == null || prog == null || now - r.lastCombatGain < 40 || target == p || !SongTargets.isFoe(p, target)) return;
        r.lastCombatGain = now;
        s.gainMastery(prog, 4.0 / Math.max(1.0, def.masterySeconds()) * BeastWillConfig.masteryRate(), def.masteryDayCap(), now);
    }

    /** Death: the form is gone with the body; the strain is cut to 59 so a respawned player is not stuck in the red. */
    static void onDeath(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s != null) {
            s.phase = BeastWillRules.OFF;
            s.strain = Math.min(s.strain, 59f);
        }
        cleanRuntime(p);
    }

    /** Login, respawn and change of dimension: the form is cut off, the modifiers are rebuilt from the saved progress, the client gets the state again. */
    static void onEnter(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        cleanRuntime(p);
        if (s == null) return;
        s.phase = BeastWillRules.OFF;
        s.clampClock(p.level().getGameTime(), BeastWillRules.MAX_COOLDOWN * 2);
        if (!s.wills.isEmpty()) {
            Run r = new Run();
            r.lastActive = p.level().getGameTime();
            RUN.put(p.getUUID(), r);
            BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
            BeastWillState.Progress prog = s.activeProgress();
            if (def != null && prog != null) {
                announceUnlocks(p, s, r);                                                        // silent the first time
                reapply(p, s, def, prog, r);
            }
        }
    }

    /** Logout and the places above: everything in memory about the player is dropped, the modifiers and aura are removed. */
    static void cleanRuntime(ServerPlayer p) {
        RUN.remove(p.getUUID());
        BeastWillAbilities.cancel(p.getUUID());
        BeastWillStealth.forget(p);
        BeastWillAttributes.clear(p);
        auraOff(p);
    }

    static void onLogout(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s != null) s.phase = BeastWillRules.OFF;
        cleanRuntime(p);
        UUID id = p.getUUID();
        BeastWillFeedback.forget(id);
        BeastWillAcquire.forget(id);
        BeastWillStealth.forget(id);
    }

    static void clearAll() {
        RUN.clear();
    }

    // ================================================================== the client
    private static void updateHash(ServerPlayer p, BeastWillState s, Run r, long now) {
        int h = hash(p, s, r, now);
        if (h != r.lastHash) r.dirty = true;
    }

    private static int hash(ServerPlayer p, BeastWillState s, Run r, long now) {
        BeastWillState.Progress a = s.activeProgress();
        List<Object> parts = new ArrayList<>();
        for (var e : s.wills.entrySet()) {
            parts.add(e.getKey());
            parts.add(pct(e.getValue().assimilation));
            parts.add(pct(e.getValue().mastery));
        }
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        return Objects.hash(s.active, s.phase, Math.round(s.strain), a == null ? -1 : pct(a.assimilation), a == null ? -1 : pct(a.mastery), flags(p, s, r), s.locked(now), s.swapLocked(now), def == null ? 0 : Math.round(compatOf(p, def) * 100), parts);
    }

    private static int pct(float v) { return Math.max(0, Math.min(100, Math.round(v * 100f))); }

    private static int flags(ServerPlayer p, BeastWillState s, Run r) {
        int f = 0;
        if (r.meditating) f |= BeastWillNet.Sync.F_MEDITATING;
        if (BeastWillStealth.veiled(p)) f |= BeastWillNet.Sync.F_VEILED;
        if (BeastWillConfig.penalties() && BeastWillRules.tier(s.strain) != BeastWillRules.Tier.CALM) f |= BeastWillNet.Sync.F_REDUCED;
        if (conflict(p, BeastWillRules.INTEGRATE)) f |= BeastWillNet.Sync.F_CONFLICT;
        return f;
    }

    private static int sec(long ticks) { return (int) Math.min(36_000, (Math.max(0, ticks) + 19) / 20); }

    static BeastWillNet.Sync build(ServerPlayer p, BeastWillState s, Run r, long now) {
        BeastWillDef def = s.active.isEmpty() ? null : BeastWillCatalog.get(s.active);
        BeastWillState.Progress a = s.activeProgress();
        int cd1 = 0, cd2 = 0;
        if (def != null && a != null) {
            int shown = s.phase == BeastWillRules.OFF ? BeastWillRules.ACQUIRE : s.phase;
            BeastWillDef.Ability a1 = def.ability(shown, 1), a2 = def.ability(shown, 2);
            if (a1 != null) cd1 = sec(s.left(a, a1.kind(), now));
            if (a2 != null) cd2 = sec(s.left(a, a2.kind(), now));
        }
        List<BeastWillNet.Entry> list = new ArrayList<>();
        for (var e : s.wills.entrySet()) {
            if (list.size() >= BeastWillState.MAX_WILLS) break;
            list.add(new BeastWillNet.Entry(e.getKey(), pct(e.getValue().assimilation), pct(e.getValue().mastery)));
        }
        return new BeastWillNet.Sync(s.active, s.phase, Math.round(s.strain), a == null ? 0 : pct(a.assimilation), a == null ? 0 : pct(a.mastery),
                sec(s.lockUntil - now), sec(s.swapLockUntil - now), cd1, cd2, flags(p, s, r), def == null ? 100 : (int) Math.round(compatOf(p, def) * 100), list);
    }

    /** The facts the client can not read (the data files are on the server): beast, affinity, rank, tint, unlock shares, drains and abilities of one will. Display only. */
    static BeastWillNet.Info infoOf(BeastWillDef d) {
        List<BeastWillNet.AbilityInfo> abilities = new ArrayList<>();
        for (int phase = BeastWillRules.ACQUIRE; phase <= BeastWillRules.INTEGRATE; phase++) {
            BeastWillDef.Phase ph = d.phase(phase);
            if (ph == null) continue;
            for (BeastWillDef.Ability a : ph.abilities()) {
                if (abilities.size() >= 8) break;
                abilities.add(new BeastWillNet.AbilityInfo(phase, a.slot(), a.id(), a.cooldownTicks(), (int) Math.round(a.costPct()), (int) Math.round(a.strain())));
            }
        }
        return new BeastWillNet.Info(d.id(), d.beast(), d.affinity(), d.minRank().ordinal(), d.tint(), d.legendary(),
                pctOf(d.acquire().unlockAssimilation()), pctOf(d.acquire().unlockMastery()), pctOf(d.integrate().unlockAssimilation()), pctOf(d.integrate().unlockMastery()),
                (int) Math.round(d.acquire().drainPct() * 10), (int) Math.round(d.integrate().drainPct() * 10), abilities);
    }

    private static int pctOf(double v) { return (int) Math.max(0, Math.min(100, Math.round(v * 100))); }

    /** The data files were reloaded: every client gets the active will's facts again with the next sync. */
    static void onReload() {
        for (Run r : RUN.values()) {
            r.infoFor = "";
            r.dirty = true;
        }
    }

    private static void send(ServerPlayer p, BeastWillState s, Run r, long now) {
        if (!s.active.isEmpty() && !s.active.equals(r.infoFor)) {
            BeastWillDef d = BeastWillCatalog.get(s.active);
            if (d != null) BeastWillNet.send(p, infoOf(d));
            r.infoFor = s.active;
        }
        BeastWillNet.send(p, build(p, s, r, now));
        r.lastSent = now;
        r.lastHash = hash(p, s, r, now);
        r.dirty = false;
        if (s.wills.isEmpty()) RUN.remove(p.getUUID());
    }

    /** Sends the state now (a command or a login; still limited to one packet every five ticks by the tick). */
    public static void sync(ServerPlayer p) { markDirty(p); }
}
