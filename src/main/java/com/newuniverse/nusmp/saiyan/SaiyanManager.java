package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the one place where a Saiyan LIVES (3.63.0, Stage 1): it turns the player's asks (a Ki Blast, a Rapid Ki Barrage) into the changes of {@link SaiyanState}, runs the once-a-second upkeep (stamina,
 * the extra Ki, the racial modifiers, the Ki Awakening), pays out Battle Power for a kill under the anti-farm rules, scales melee damage by the strength bonus and incoming damage by the durability bonus,
 * and keeps the client in step. The rules themselves are elsewhere ({@link SaiyanRules} for the numbers, {@link SaiyanConfig} for the settings, {@link SaiyanAbilities} for the attacks).
 *
 * <p>Cost: a player who is not a Saiyan costs one {@code hasData} lookup per tick; a Saiyan gets one small upkeep a second and at most four sync packets a second, only when something changed. Server
 * thread only; the per-player runtime state ({@link Run}) is in memory, keyed by UUID, and dropped at logout, death and when the race is taken away.
 */
public final class SaiyanManager {
    private SaiyanManager() {}

    /** Per-player runtime state (not saved). */
    static final class Run {
        boolean dirty = true;
        long lastSent = -100, lastAsk = -100, lastDamaged = -1000;
        int lastHash = Integer.MIN_VALUE;
        /** Seconds the player has sat still (sneaking, on the ground) toward the Ki Awakening. */
        int still;
        double lx = Double.NaN, ly = Double.NaN, lz = Double.NaN;
        boolean applied;
        // ---- 3.65.0: Ki Flight
        boolean flying;
        int groundSecs, hoverSecs, flightTier = -1;
        long burstUntil = -1, safeFallUntil = -1;
        // ---- 3.65.0: senses and suppression
        int radarLeft;
        boolean suppressed;
        long lastPing = -1000;
        // ---- 3.65.0: combat passives
        long lastCombat = -1000, reflexReadyAt, dodgeWinStart = -1000, counterUntil = -1, nearDeathAt = -1, flowAt = -1000, adaptLast = -100_000;
        int dodgeUsed, flow;
        double counterBonus;
        final int[] adapt = new int[SaiyanCombatRules.Kind.values().length];
        // ---- 3.65.0: Goku
        boolean charging;
        long kameStart, comboAt = -1000;
        int comboStage;
        // ---- 3.65.0: forms
        String form = "";
        int kaioStage = 1;
        long formReadyAt;
        // ---- 3.66.0: Ki Barrier, Instant Transmission, Spirit Bomb, Meteor Combination
        final SaiyanTechniques.State tech = new SaiyanTechniques.State();
    }

    private static final Map<UUID, Run> RUN = new ConcurrentHashMap<>();

    /** Players with a runtime record (the self-test and the stopping hook read it). */
    static int runs() { return RUN.size(); }

    static Run runOf(ServerPlayer p) { return RUN.computeIfAbsent(p.getUUID(), u -> new Run()); }

    /** The runtime record if the player has one, else null (never creates one: the fall event asks for every player). */
    static Run peekRun(ServerPlayer p) { return RUN.get(p.getUUID()); }

    public static void markDirty(ServerPlayer p) { runOf(p).dirty = true; }

    /** The stamina this Saiyan can hold: the base plus the racial capacity bonus (which grows with Battle Power). */
    public static double staminaMax(SaiyanState s) { return SaiyanConfig.staminaBase() * (1.0 + SaiyanConfig.bonuses(s.battlePower).stamina()) * (1.0 - SaiyanForms.strainPenalty(s.strain)); }

    // ================================================================== the tick
    /** Called for every server player every tick; costs one lookup for a player who is not a Saiyan. */
    public static void tick(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return;
        Run r = runOf(p);
        long now = p.level().getGameTime();
        if ((p.tickCount % 20) == 0) second(p, s, r, now);
        if (r.charging) GokuAbilities.chargeTick(p, s, r, now);
        if (r.tech.gathering) SaiyanTechniques.gatherTick(p, s, r, now);
        if (r.tech.bombFlying) SaiyanTechniques.flightTick(p, s, r, now);
        if (r.tech.meteorLeft > 0) SaiyanTechniques.meteorTick(p, s, r, now);
        if (r.radarLeft > 0) SaiyanSenses.radarTick(p, s, r, now);
        if (!r.form.isEmpty() && (p.tickCount % 10) == 0) SaiyanFormEngine.aura(p, r);
        if (r.dirty && now - r.lastSent >= 5) send(p, s, r, now);
    }

    private static void second(ServerPlayer p, SaiyanState s, Run r, long now) {
        if (!SaiyanConfig.enabled()) {
            if (r.applied) {
                SaiyanAttributes.clear(p);
                stopActive(p, r, now);
                r.applied = false;
                r.dirty = true;
            }
            return;
        }
        SaiyanAttributes.apply(p, SaiyanConfig.bonuses(s.battlePower), SaiyanConfig.kiBase());
        r.applied = true;
        double max = staminaMax(s);
        if (s.stamina < 0.0) s.stamina = max;
        s.stamina = SaiyanRules.stamina(s.stamina, SaiyanConfig.staminaRegen() * (p.isSprinting() ? 1.0 / 3.0 : 1.0), max);
        if (s.awakened) SaiyanKi.regen(p, SaiyanConfig.kiRegenPercent());
        else meditate(p, s, r, now);
        double moved = Double.isNaN(r.lx) ? 0.0 : Math.sqrt(sq(p.getX() - r.lx) + sq(p.getY() - r.ly) + sq(p.getZ() - r.lz));
        SaiyanFlight.second(p, s, r, now, moved);
        SaiyanSenses.second(p, s, r);
        SaiyanFormEngine.second(p, s, r, now);
        SaiyanCombat.second(p, s, r, now);
        GokuAbilities.second(r, now);
        SaiyanTechniques.second(p, r, now);
        if ((p.tickCount % 100) == 0) GokuAbilities.awakeningCheck(p, s, r);
        r.lx = p.getX();
        r.ly = p.getY();
        r.lz = p.getZ();
        int h = hash(p, s, r, now);
        if (h != r.lastHash) r.dirty = true;
    }

    private static double sq(double v) { return v * v; }

    /** Everything that is switched on and needs the system: the flight, the forms, the charge, the radar and the suppression stop (the system was disabled, or the race was taken away). */
    private static void stopActive(ServerPlayer p, Run r, long now) {
        if (r.flying) SaiyanFlight.stop(p, r, now);
        SaiyanFormEngine.reset(r);
        GokuAbilities.cancel(r);
        SaiyanTechniques.cancel(r);
        r.radarLeft = 0;
        r.suppressed = false;
        r.comboStage = 0;
    }

    /** Ki Awakening: sneak and keep still on the ground, unhurt for five seconds; the seconds add up, any break starts over. */
    private static void meditate(ServerPlayer p, SaiyanState s, Run r, long now) {
        boolean moved = Double.isNaN(r.lx) || Math.abs(p.getX() - r.lx) + Math.abs(p.getY() - r.ly) + Math.abs(p.getZ() - r.lz) > 0.3;
        boolean still = p.isShiftKeyDown() && !moved && p.onGround() && !p.isUsingItem() && now - r.lastDamaged > 100;
        r.still = still ? r.still + 1 : 0;
        if (SaiyanRules.awakened(r.still, SaiyanConfig.awakenSeconds())) awaken(p, s, r);
    }

    static void awaken(ServerPlayer p, SaiyanState s, Run r) {
        if (s.awakened) return;
        s.awakened = true;
        r.still = 0;
        r.dirty = true;
        p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.awakened"), false);
    }

    // ================================================================== the asks
    /** The player pressed a Ki key (already range-checked by the net class): rate limited, and only a real, enabled Saiyan is answered. */
    static void onAction(ServerPlayer p, int op) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return;
        long now = p.level().getGameTime();
        Run r = runOf(p);
        if (op == SaiyanNet.OP_KAME_RELEASE) {                          // a release is never rate limited or dropped, so a charge can not be stuck
            if (SaiyanConfig.enabled()) {
                if (GokuAbilities.kameRelease(p, s, r, now)) r.dirty = true;
            } else {
                GokuAbilities.cancel(r);
            }
            return;
        }
        if (op == SaiyanNet.OP_SPIRIT_RELEASE) {                        // the same for the Spirit Bomb
            if (SaiyanConfig.enabled()) {
                if (SaiyanTechniques.bombRelease(p, s, r, now)) r.dirty = true;
            } else {
                SaiyanTechniques.cancel(r);
            }
            return;
        }
        if (!SaiyanConfig.enabled()) return;
        if (now >= r.lastAsk && now - r.lastAsk < SaiyanNet.MIN_GAP_TICKS) return;
        r.lastAsk = now;
        s.clampClock(now, 24000);
        boolean did = switch (op) {
            case SaiyanNet.OP_BLAST -> SaiyanAbilities.blast(p, s, now);
            case SaiyanNet.OP_BARRAGE -> SaiyanAbilities.barrage(p, s, now);
            case SaiyanNet.OP_FLIGHT -> SaiyanFlight.toggle(p, s, r, now);
            case SaiyanNet.OP_BURST -> SaiyanFlight.burst(p, s, r, now);
            case SaiyanNet.OP_SENSE -> SaiyanSenses.sense(p, s, r, now);
            case SaiyanNet.OP_SUPPRESS -> SaiyanSenses.suppress(p, s, r, now);
            case SaiyanNet.OP_COMBO -> GokuAbilities.combo(p, s, r, now);
            case SaiyanNet.OP_KAME_START -> GokuAbilities.kameStart(p, s, r, now);
            case SaiyanNet.OP_FORM_TOGGLE -> SaiyanFormEngine.toggle(p, s, r, now);
            case SaiyanNet.OP_FORM_NEXT -> SaiyanFormEngine.cycle(p, s, r, now, 1);
            case SaiyanNet.OP_FORM_PREV -> SaiyanFormEngine.cycle(p, s, r, now, -1);
            case SaiyanNet.OP_TRANSMIT -> SaiyanTechniques.transmit(p, s, r, now);
            case SaiyanNet.OP_BARRIER -> SaiyanTechniques.barrier(p, s, r, now);
            case SaiyanNet.OP_SPIRIT_START -> SaiyanTechniques.bombStart(p, s, r, now);
            case SaiyanNet.OP_METEOR -> SaiyanTechniques.meteor(p, s, r, now);
            default -> false;
        };
        if (did) r.dirty = true;
    }

    // ================================================================== Battle Power
    /** A creature died by this Saiyan's hand: the numbers are read from it and handed to {@link #credit}. Never gives anything for an ally, a pet, a player (unless allowed) or a young creature. */
    public static void creditKill(ServerPlayer killer, LivingEntity victim) { creditKill(killer, victim, null); }

    /**
     * The same with the blow that killed it (3.65.0): a kill with the Saiyan's own hands (or a martial technique) of a challenging foe counts toward the Goku awakening and its trial, and a boss kill is
     * a point of divine training. Nothing is counted for an ally, a pet, a player (unless allowed) or a young creature.
     */
    public static void creditKill(ServerPlayer killer, LivingEntity victim, DamageSource blow) {
        SaiyanState s = SaiyanStore.peek(killer);
        if (s == null || !s.saiyan || !SaiyanConfig.enabled() || victim == killer || victim instanceof ArmorStand) return;
        if (SongTargets.isAlly(killer, victim)) return;
        if (victim instanceof Player && !SaiyanConfig.playersGiveBp()) return;
        if (victim.tickCount < SaiyanConfig.minVictimAge()) return;
        double power = powerOf(victim);
        long now = killer.level().getGameTime();
        double ratio = s.battlePower > 0.0 ? power / s.battlePower : 0.0;
        if (blow != null && blow.is(DamageTypes.PLAYER_ATTACK)) {
            if (ratio >= SaiyanConfig.challengingRatio()) s.hardMartialKills = Math.min(1_000_000, s.hardMartialKills + 1);
            if (ratio >= 0.75) s.trialKills = Math.min(1_000_000, s.trialKills + 1);
        }
        if (BalanceLaw.isBoss(victim)) s.train(SaiyanTraining.Cat.DIVINE, 1.0, now, SaiyanConfig.trainingScale());
        credit(killer, s, power, BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), now);
        runOf(killer).dirty = true;
    }

    /** How hard a creature is, in Battle Power units (health, attack, armour and a share of its Existence Points). */
    static double powerOf(LivingEntity victim) {
        double attack = victim.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? victim.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
        double ep = SaiyanConfig.useEp() ? TensuraBridge.maxEp(victim) : 0.0;
        return SaiyanRules.victimPower(victim.getMaxHealth(), attack, victim.getArmorValue(), ep, SaiyanConfig.victimScale(), SaiyanConfig.epWeight());
    }

    /** The power of any creature as the senses and the training read it: a Saiyan player counts as their Battle Power. */
    static double powerOfAny(LivingEntity e) {
        if (e instanceof ServerPlayer sp) {
            SaiyanState s = SaiyanStore.peek(sp);
            if (s != null && s.saiyan) return s.battlePower;
        }
        return powerOf(e);
    }

    /** Gives the Battle Power of one kill of a creature of this power and type; returns what was really added (the self-test calls this with numbers). */
    static double credit(ServerPlayer p, SaiyanState s, double victimPower, String type, long now) {
        SaiyanRules.Rank before = s.rank();
        double weight = SaiyanRules.difficultyWeight(victimPower, s.battlePower, SaiyanConfig.minRatio());
        double repeat = SaiyanRules.repeatFactor(s.recentKills(type, now, SaiyanConfig.repeatWindow()), SaiyanConfig.repeatLimit());
        double xp = SaiyanConfig.bonuses(s.battlePower).combatXp();
        double room = s.roomToday(now, SaiyanConfig.dailyFraction(), SaiyanConfig.dailyFloor());
        double gain = SaiyanRules.killGain(s.battlePower, SaiyanConfig.gainPerKill(), weight, repeat, xp, room);
        gain = Math.min(gain, Math.max(0.0, SaiyanConfig.maxBp() - s.battlePower));
        s.noteKill(type, now, SaiyanConfig.repeatWindow());
        double given = s.addBattlePower(gain, now, SaiyanConfig.dailyFraction(), SaiyanConfig.dailyFloor());
        if (given > 0.0) {
            runOf(p).dirty = true;
            SaiyanRules.Rank after = s.rank();
            if (after.ordinal() > before.ordinal()) p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.rank_up", Component.translatable(after.key())), false);
        }
        return given;
    }

    // ================================================================== damage
    /**
     * Before the damage is applied: a blow from a Saiyan's hands is raised by the strength bonus, a blow that lands on a Saiyan is cut by the durability bonus. Both are calculations, not attributes (so
     * they can not stack with another mod's attribute and never show up as a modifier). Ki attacks are magic damage, not a blow, so the strength bonus does not touch them. Void and kill damage are left alone.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!SaiyanConfig.enabled()) return;
        DamageSource src = e.getSource();
        if (e.getEntity() instanceof ServerPlayer victim && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            SaiyanState s = SaiyanStore.peek(victim);
            if (s != null && s.saiyan) {
                SaiyanCombat.defend(e, victim, s, runOf(victim), victim.level().getGameTime());
                if (e.isCanceled()) return;                              // dodged
            }
        }
        if (src.is(DamageTypes.PLAYER_ATTACK) && src.getEntity() instanceof ServerPlayer hitter && src.getDirectEntity() == hitter) {
            SaiyanState s = SaiyanStore.peek(hitter);
            if (s != null && s.saiyan) SaiyanCombat.attack(e, hitter, s, runOf(hitter), hitter.level().getGameTime());
        }
    }

    // ================================================================== life cycle
    /** Login, respawn and change of dimension: the modifiers are rebuilt from the saved progress and the client gets the state again. */
    static void onEnter(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        RUN.remove(p.getUUID());
        if (s == null || !s.saiyan) {
            if (s != null) {
                SaiyanAttributes.clear(p);
                SaiyanFlight.release(p);
            }
            return;
        }
        long now = p.level().getGameTime();
        s.clampClock(now, 24000);
        SaiyanAttributes.clearForm(p);          // the run was just dropped, so no form is active: a change of dimension must not leave its speed behind
        Run r = runOf(p);
        if (SaiyanFlight.release(p)) r.safeFallUntil = now + 100;          // a change of dimension ends the flight: the fall is safe
        if (SaiyanConfig.enabled()) {
            SaiyanAttributes.apply(p, SaiyanConfig.bonuses(s.battlePower), SaiyanConfig.kiBase());
            r.applied = true;
        } else {
            SaiyanAttributes.clear(p);
        }
        r.dirty = true;
    }

    /** Death: the body is gone, the stamina starts full again, the cooldowns are over, the modifiers are removed (the respawn rebuilds them). Battle Power and the awakening are kept. */
    static void onDeath(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s != null && s.saiyan) {
            s.stamina = -1.0;
            s.readyAt.clear();
        }
        cleanRuntime(p);
    }

    static void onLogout(ServerPlayer p) { cleanRuntime(p); }

    static void cleanRuntime(ServerPlayer p) {
        RUN.remove(p.getUUID());
        SaiyanAttributes.clear(p);
        SaiyanFlight.release(p);
    }

    static void clearAll() { RUN.clear(); }

    /** Makes the player a Saiyan or takes the race away (a command). Taking it away removes every modifier and tells the client to show Tensura's own bar again; Battle Power is kept for a later return. */
    public static void setSaiyan(ServerPlayer p, boolean on) {
        SaiyanState s = SaiyanStore.of(p);
        s.saiyan = on;
        if (on) {
            onEnter(p);
            return;
        }
        s.awakened = false;
        s.stamina = -1.0;
        s.readyAt.clear();
        cleanRuntime(p);
        SaiyanNet.send(p, off());
    }

    // ================================================================== the client
    private static SaiyanNet.Sync off() {
        return new SaiyanNet.Sync(false, 0f, 0f, 0f, 0f, SaiyanRules.START_BP, 0, false, 0, 0, 0, 0, 0, 0, false, 0, false, false, 0, 0, 0, 0, 0, 0);
    }

    private static int hash(ServerPlayer p, SaiyanState s, Run r, long now) {
        double kiMax = SaiyanKi.max(p);
        int ki = kiMax <= 0.0 ? 0 : (int) Math.round(SaiyanKi.current(p) / kiMax * 100.0);
        double sMax = staminaMax(s);
        int st = sMax <= 0.0 ? 0 : (int) Math.round(Math.max(0.0, s.stamina) / sMax * 100.0);
        SaiyanForms.Form f = SaiyanFormEngine.active(r);
        return Objects.hash(ki, st, Math.round(kiMax), Math.round(s.battlePower), s.awakened, meditatePct(r), SaiyanConfig.enabled(), r.form, s.selectedForm, SaiyanFlight.tier(s), r.flying,
                SaiyanSenses.tier(s), r.suppressed, r.radarLeft > 0, GokuAbilities.isGoku(s), Math.max(GokuAbilities.chargePercent(s, r, now), SaiyanTechniques.chargePercent(r, now)) / 10, r.comboStage, f == null ? 0 : (int) (s.mastery(f.id()) * 20.0),
                (int) (s.strain / 5.0));
    }

    private static int meditatePct(Run r) { return Math.max(0, Math.min(99, r.still * 100 / SaiyanConfig.awakenSeconds())); }

    private static int cd(SaiyanState s, String kind, long now) { return (int) Math.min(36_000, s.left(kind, now)); }

    static SaiyanNet.Sync build(ServerPlayer p, SaiyanState s, Run r, long now) {
        double sMax = staminaMax(s);
        SaiyanForms.Form f = SaiyanFormEngine.active(r);
        return new SaiyanNet.Sync(true, (float) SaiyanKi.current(p), (float) SaiyanKi.max(p), (float) (s.stamina < 0.0 ? sMax : s.stamina), (float) sMax, s.battlePower, s.rank().ordinal(),
                s.awakened, cd(s, "blast", now), cd(s, "barrage", now), s.awakened ? 100 : meditatePct(r),
                SaiyanForms.code(r.form), SaiyanForms.code(s.selectedForm), SaiyanFlight.tier(s).ordinal(), r.flying, SaiyanSenses.tier(s).ordinal(), r.suppressed, GokuAbilities.isGoku(s),
                Math.max(GokuAbilities.chargePercent(s, r, now), SaiyanTechniques.chargePercent(r, now)), r.comboStage, f == null ? 0 : (int) Math.round(s.mastery(f.id()) * 100.0), (int) Math.round(s.strain), cd(s, "kame", now), cd(s, "sense", now));
    }

    private static void send(ServerPlayer p, SaiyanState s, Run r, long now) {
        SaiyanNet.send(p, build(p, s, r, now));
        r.lastSent = now;
        r.lastHash = hash(p, s, r, now);
        r.dirty = false;
    }
}
