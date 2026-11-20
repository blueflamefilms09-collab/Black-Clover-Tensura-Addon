package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Belial's mind magic on the server: Belior Zakera (the scythe's third act: memory and sense illusion) and the five senses Belior Zauto devours. The numbers are
 * {@link BelialMindRules}'; the damage of a strain goes through {@link BelialSever} (the balance law's caps and the boss caps).
 *
 * <p><b>Zakera.</b> A mob forgets its target and, for a few ticks, cannot choose a new one except the foe it was turned on; a player gets Nausea and Darkness (vanilla effects, through the balance
 * law's control time); a boss only a short Darkness and a small strain; a target that blocks with a shield or carries a barrier effect is not affected but takes the strain. The mod has no Borg
 * model; a shield held up and a barrier effect are what this reads (Tensura's barrier skills are not read: see docs/belial_rework.md).
 * <p><b>Bounds.</b> At most {@link BelialMindRules#MAX_TARGETS} targets per cast, {@link BelialMindRules#MAX_MINDS} minds on the server and {@link BelialMindRules#MAX_PER_CASTER} per caster. Work per
 * tick: nothing while no mind is held; a sweep of at most 48 entries once a second. The redirect picks from the foes of the same cone (no extra world scan). No packet of its own: the vanilla
 * ones of an effect, and particles for at most six targets.
 * <p><b>Cleanup.</b> The mob or the caster dies, the caster logs out or changes dimension, the server stops: the entries end at once (a mob has no flag to give back, only its target is
 * forgotten). Server thread only; keyed by UUID.
 */
public final class BelialMind {
    private BelialMind() {}

    /** One forgotten mind: who, by whom, until when it cannot choose a target, until when it is tracked, and the foe it was turned on (or null). */
    private static final class Entry {
        final UUID mob, caster;
        final long blockUntil, until;
        final UUID redirect;

        Entry(UUID mob, UUID caster, long blockUntil, long until, UUID redirect) {
            this.mob = mob;
            this.caster = caster;
            this.blockUntil = blockUntil;
            this.until = until;
            this.redirect = redirect;
        }
    }

    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();

    public static int activeCount() { return ACTIVE.size(); }

    public static boolean forgets(UUID mob) { return mob != null && ACTIVE.containsKey(mob); }

    private static int countOf(UUID caster) {
        int n = 0;
        for (Entry e : ACTIVE.values()) if (e.caster.equals(caster)) n++;
        return n;
    }

    /** What stands in for the magician's Borg: a shield held up, or a barrier effect (an effect whose id has "barrier" in it, the way the mod's own Nullification reads it). */
    static boolean defended(LivingEntity t) {
        if (t.isBlocking()) return true;
        for (MobEffectInstance m : t.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(m.getEffect().value());
            if (id != null && id.getPath().contains("barrier")) return true;
        }
        return false;
    }

    /** The result of a cast, for the self-test and the action bar. */
    public record Result(int minds, int senses, int blocked, int bosses) {
        public int total() { return minds + senses + blocked + bosses; }
    }

    // ================================================================ Belior Zakera
    /** Casts Belior Zakera on {@code foes} (the cone the act found; at most {@link BelialMindRules#MAX_TARGETS} are touched). */
    public static Result zakera(ServerPlayer p, List<LivingEntity> foes, double power, boolean full, boolean dark) {
        int minds = 0, senses = 0, blocked = 0, bosses = 0;
        if (p == null || foes == null || foes.isEmpty() || !BelialConfig.zakera() || !(p.level() instanceof ServerLevel level)) return new Result(0, 0, 0, 0);
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity t : foes) {
            if (targets.size() >= BelialMindRules.MAX_TARGETS) break;
            if (t == null || t == p || !t.isAlive() || t.isAlliedTo(p) || BelialExile.isExiled(t.getUUID())) continue;
            if (t instanceof Player o && (o.isCreative() || o.isSpectator())) continue;
            targets.add(t);
        }
        for (LivingEntity t : targets) {
            BelialMindRules.Fate fate = BelialMindRules.fate(BelialSever.kindOf(t), defended(t));
            switch (fate) {
                case BLOCKED -> {
                    strain(p, t, power, fate);
                    int trauma = BalanceLaw.controlTicks(t, BelialMindRules.BLOCKED_NAUSEA);                          // the Borg held, the head still rings
                    DjinnFx.effect(t, "minecraft:nausea", trauma, 0);
                    DjinnFx.effect(t, "minecraft:darkness", Math.min(trauma, BelialMindRules.BLOCKED_DARKNESS), 0);
                    blocked++;
                }
                case BOSS -> {
                    DjinnFx.effect(t, "minecraft:darkness", BalanceLaw.controlTicks(t, BelialMindRules.DARKNESS_BOSS), 0);
                    strain(p, t, power, fate);
                    bosses++;
                }
                case SENSES -> {
                    int ticks = BalanceLaw.controlTicks(t, BelialMindRules.nauseaTicks(full, dark));                   // one control roll, so the diminishing returns count the cast once
                    DjinnFx.effect(t, "minecraft:nausea", ticks, 0);
                    DjinnFx.effect(t, "minecraft:darkness", Math.min(ticks, BelialMindRules.DARKNESS_PLAYER), 0);
                    senses++;
                }
                case MIND -> {
                    if (t instanceof Mob m && forget(p, m, targets, level, full, dark)) minds++;
                }
            }
            DjinnFx.tinted(level, BelialCanon.GLOW, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), fate == BelialMindRules.Fate.BLOCKED ? 4 : 6, 0.3, 0.4, 0.3, 0.02);
        }
        return new Result(minds, senses, blocked, bosses);
    }

    /** The mental strain: a small sever (a defended target or a boss takes it; the caps of {@link BelialSever} and the balance law hold). */
    private static void strain(ServerPlayer p, LivingEntity t, double power, BelialMindRules.Fate fate) {
        double s = BelialMindRules.strain(fate, power);
        if (s > 0.0) BelialSever.sever(p, t, s, 0.0);
    }

    /** A mob forgets its target; half the time it is turned on the nearest other mob of the cone. False if the tables are full (it then only forgets once). */
    private static boolean forget(ServerPlayer p, Mob m, List<LivingEntity> cone, ServerLevel level, boolean full, boolean dark) {
        UUID id = m.getUUID();
        boolean already = ACTIVE.containsKey(id);
        if (!BelialMindRules.accept(already, countOf(p.getUUID()), ACTIVE.size())) {
            m.setTarget(null);
            return false;
        }
        long now = level.getGameTime();
        int len = BelialMindRules.forgetTicks(full, dark);
        Mob other = null;
        if (BelialMindRules.shouldRedirect(level.random.nextDouble())) {
            double best = Double.MAX_VALUE;
            for (LivingEntity o : cone) {
                if (o == m || !(o instanceof Mob om) || !om.isAlive() || BelialSever.kindOf(om) != BelialSeverRules.Kind.MOB) continue;
                double d = om.distanceToSqr(m);
                if (d < best) {
                    best = d;
                    other = om;
                }
            }
        }
        ACTIVE.put(id, new Entry(id, p.getUUID(), now + Math.min(len, BelialMindRules.RETARGET_BLOCK), now + len, other == null ? null : other.getUUID()));   // before the target changes: the listener reads it
        m.setTarget(null);
        if (other != null) m.setTarget(other);
        return true;
    }

    // ================================================================ Belior Zauto: the five senses
    /** Devours the five senses of a victim of Belior Zauto for a few seconds (a boss: none). Called once per victim and cast. */
    static void devourSenses(ServerPlayer p, LivingEntity t, boolean dark) {
        if (!BelialConfig.senses() || p == null || t == null || t == p || !t.isAlive() || BelialExile.isExiled(t.getUUID())) return;
        if (t instanceof Player o && (o.isCreative() || o.isSpectator())) return;
        int base = BelialMindRules.senseTicks(BelialSever.kindOf(t), dark);
        if (base <= 0) return;
        int control = BalanceLaw.controlTicks(t, base);                                                          // one control roll for the three that can hold a fighter
        for (BelialMindRules.Sense s : BelialMindRules.SENSES) DjinnFx.effect(t, s.effectId(), s.control() ? control : base, s.amp());
    }

    // ================================================================ Belior Goldoreza: dead weight
    /** Victim -> the game time its healing is blocked until (bounded, swept every second, cleared on death, logout and stop). */
    private static final Map<UUID, Long> DEAD = new java.util.HashMap<>();

    /** The struck part of {@code t} goes numb: Slowness I and Weakness I, and normal healing is blocked, for {@link BelialMindRules#deadWeightTicks}. Never a boss. */
    public static boolean deadWeight(ServerPlayer p, LivingEntity t, boolean full) {
        if (!BelialConfig.deadWeight() || p == null || t == null || t == p || !t.isAlive() || BelialExile.isExiled(t.getUUID())) return false;
        if (t instanceof Player o && (o.isCreative() || o.isSpectator())) return false;
        int ticks = BelialMindRules.deadWeightTicks(BelialSever.kindOf(t), full);
        if (ticks <= 0 || (!DEAD.containsKey(t.getUUID()) && DEAD.size() >= BelialMindRules.DEAD_MAX)) return false;
        DEAD.put(t.getUUID(), t.level().getGameTime() + ticks);
        DjinnFx.effect(t, "minecraft:slowness", ticks, 0);
        DjinnFx.effect(t, "minecraft:weakness", ticks, 0);
        return true;
    }

    public static boolean deadWeighted(UUID id) { return id != null && !DEAD.isEmpty() && DEAD.containsKey(id); }

    /** Normal healing of a numb victim is refused (the event fires for regeneration, potions and food; the sever and heals that bypass the event are not covered). */
    static void onHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent e) {
        if (DEAD.isEmpty()) return;
        Long until = DEAD.get(e.getEntity().getUUID());
        if (until != null && e.getEntity().level().getGameTime() < until) e.setCanceled(true);
    }

    // ================================================================ events
    public static void register(IEventBus bus) {
        bus.addListener(BelialMind::onServerTick);
        bus.addListener(BelialMind::onChangeTarget);
        bus.addListener(BelialMind::onHeal);
        bus.addListener(BelialMind::onDeath);
        bus.addListener(BelialMind::onLogout);
        bus.addListener(BelialMind::onDimension);
        bus.addListener(BelialMind::onServerStopping);
    }

    /** Once a second: entries whose time is over are dropped (at most 48 of them). */
    static void onServerTick(ServerTickEvent.Post e) {
        if ((ACTIVE.isEmpty() && DEAD.isEmpty()) || e.getServer().getTickCount() % 20 != 0) return;
        long now = e.getServer().overworld().getGameTime();
        ACTIVE.values().removeIf(en -> en.until <= now);
        DEAD.values().removeIf(until -> until <= now);
    }

    /** A mob that forgot cannot choose a target for a moment, except the foe it was turned on. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (ACTIVE.isEmpty()) return;
        Entry en = ACTIVE.get(e.getEntity().getUUID());
        if (en == null) return;
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next == null || e.getEntity().level().getGameTime() >= en.blockUntil) return;
        if (en.redirect == null || !en.redirect.equals(next.getUUID())) e.setNewAboutToBeSetTarget(null);
    }

    private static void endWhere(java.util.function.Predicate<Entry> which) {
        if (!ACTIVE.isEmpty()) ACTIVE.values().removeIf(which);
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        UUID id = e.getEntity().getUUID();
        DEAD.remove(id);
        endWhere(en -> en.mob.equals(id) || en.caster.equals(id));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        DEAD.remove(id);
        endWhere(en -> en.caster.equals(id));
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        UUID id = e.getEntity().getUUID();
        endWhere(en -> en.caster.equals(id));
    }

    static void onServerStopping(ServerStoppingEvent e) {
        ACTIVE.clear();
        DEAD.clear();
    }

    /** The sound of a cast: a low chime that turns (kept here so the act has one line). */
    public static void cue(ServerLevel l, BlockPos at) {
        l.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1f, 0.7f);
        l.playSound(null, at, SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    /** For the self-test: forgets every entry (the test cleans up after itself). */
    static void clearAll() {
        ACTIVE.clear();
        DEAD.clear();
    }
}
