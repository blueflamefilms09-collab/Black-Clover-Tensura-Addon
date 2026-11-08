package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.avatar.TensuraBridge;
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
    }

    private static final Map<UUID, Run> RUN = new ConcurrentHashMap<>();

    /** Players with a runtime record (the self-test and the stopping hook read it). */
    static int runs() { return RUN.size(); }

    static Run runOf(ServerPlayer p) { return RUN.computeIfAbsent(p.getUUID(), u -> new Run()); }

    public static void markDirty(ServerPlayer p) { runOf(p).dirty = true; }

    /** The stamina this Saiyan can hold: the base plus the racial capacity bonus (which grows with Battle Power). */
    public static double staminaMax(SaiyanState s) { return SaiyanConfig.staminaBase() * (1.0 + SaiyanConfig.bonuses(s.battlePower).stamina()); }

    // ================================================================== the tick
    /** Called for every server player every tick; costs one lookup for a player who is not a Saiyan. */
    public static void tick(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return;
        Run r = runOf(p);
        long now = p.level().getGameTime();
        if ((p.tickCount % 20) == 0) second(p, s, r, now);
        if (r.dirty && now - r.lastSent >= 5) send(p, s, r, now);
    }

    private static void second(ServerPlayer p, SaiyanState s, Run r, long now) {
        if (!SaiyanConfig.enabled()) {
            if (r.applied) {
                SaiyanAttributes.clear(p);
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
        r.lx = p.getX();
        r.ly = p.getY();
        r.lz = p.getZ();
        int h = hash(p, s, r);
        if (h != r.lastHash) r.dirty = true;
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
        if (s == null || !s.saiyan || !SaiyanConfig.enabled()) return;
        long now = p.level().getGameTime();
        Run r = runOf(p);
        if (now >= r.lastAsk && now - r.lastAsk < SaiyanNet.MIN_GAP_TICKS) return;
        r.lastAsk = now;
        s.clampClock(now, 24000);
        boolean did = op == SaiyanNet.OP_BLAST ? SaiyanAbilities.blast(p, s, now) : op == SaiyanNet.OP_BARRAGE && SaiyanAbilities.barrage(p, s, now);
        if (did) r.dirty = true;
    }

    // ================================================================== Battle Power
    /** A creature died by this Saiyan's hand: the numbers are read from it and handed to {@link #credit}. Never gives anything for an ally, a pet, a player (unless allowed) or a young creature. */
    public static void creditKill(ServerPlayer killer, LivingEntity victim) {
        SaiyanState s = SaiyanStore.peek(killer);
        if (s == null || !s.saiyan || !SaiyanConfig.enabled() || victim == killer || victim instanceof ArmorStand) return;
        if (SongTargets.isAlly(killer, victim)) return;
        if (victim instanceof Player && !SaiyanConfig.playersGiveBp()) return;
        if (victim.tickCount < SaiyanConfig.minVictimAge()) return;
        double attack = victim.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? victim.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
        double ep = SaiyanConfig.useEp() ? TensuraBridge.maxEp(victim) : 0.0;
        double power = SaiyanRules.victimPower(victim.getMaxHealth(), attack, victim.getArmorValue(), ep, SaiyanConfig.victimScale(), SaiyanConfig.epWeight());
        credit(killer, s, power, BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), killer.level().getGameTime());
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
                runOf(victim).lastDamaged = victim.level().getGameTime();
                e.setAmount((float) (e.getAmount() * SaiyanRules.takenFactor(SaiyanConfig.bonuses(s.battlePower).durability())));
            }
        }
        if (src.is(DamageTypes.PLAYER_ATTACK) && src.getEntity() instanceof ServerPlayer hitter && src.getDirectEntity() == hitter) {
            SaiyanState s = SaiyanStore.peek(hitter);
            if (s != null && s.saiyan) e.setAmount((float) (e.getAmount() * SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength())));
        }
    }

    // ================================================================== life cycle
    /** Login, respawn and change of dimension: the modifiers are rebuilt from the saved progress and the client gets the state again. */
    static void onEnter(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        RUN.remove(p.getUUID());
        if (s == null || !s.saiyan) {
            if (s != null) SaiyanAttributes.clear(p);
            return;
        }
        s.clampClock(p.level().getGameTime(), 24000);
        Run r = runOf(p);
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
    private static SaiyanNet.Sync off() { return new SaiyanNet.Sync(false, 0f, 0f, 0f, 0f, SaiyanRules.START_BP, 0, false, 0, 0, 0); }

    private static int hash(ServerPlayer p, SaiyanState s, Run r) {
        double kiMax = SaiyanKi.max(p);
        int ki = kiMax <= 0.0 ? 0 : (int) Math.round(SaiyanKi.current(p) / kiMax * 100.0);
        double sMax = staminaMax(s);
        int st = sMax <= 0.0 ? 0 : (int) Math.round(Math.max(0.0, s.stamina) / sMax * 100.0);
        return Objects.hash(ki, st, Math.round(kiMax), Math.round(s.battlePower), s.awakened, meditatePct(r), SaiyanConfig.enabled());
    }

    private static int meditatePct(Run r) { return Math.max(0, Math.min(99, r.still * 100 / SaiyanConfig.awakenSeconds())); }

    private static int cd(SaiyanState s, String kind, long now) { return (int) Math.min(36_000, s.left(kind, now)); }

    static SaiyanNet.Sync build(ServerPlayer p, SaiyanState s, Run r, long now) {
        double sMax = staminaMax(s);
        return new SaiyanNet.Sync(true, (float) SaiyanKi.current(p), (float) SaiyanKi.max(p), (float) (s.stamina < 0.0 ? sMax : s.stamina), (float) sMax, s.battlePower, s.rank().ordinal(),
                s.awakened, cd(s, "blast", now), cd(s, "barrage", now), s.awakened ? 100 : meditatePct(r));
    }

    private static void send(ServerPlayer p, SaiyanState s, Run r, long now) {
        SaiyanNet.send(p, build(p, s, r, now));
        r.lastSent = now;
        r.lastHash = hash(p, s, r);
        r.dirty = false;
    }
}
