package com.newuniverse.nusmp.boss;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Mod Boss Global Buff Engine: permanent Tensura barriers and thematic skills for THIS mod's registry-listed bosses, and nobody else.
 * <ul>
 *   <li><b>Who</b>: an entity joins a server level, {@link BossEntities#bossId} names it a boss (namespace {@code nusmp} and a row in
 *       {@link BossRegistry}), and only then is it tracked. Vanilla, base-Tensura and the God of Order never are.</li>
 *   <li><b>What</b>: the rows of {@link BossBuffTable}: infinite, ambient, particle-less Tensura effects (magic barrier, physical
 *       barrier, haki coat, instant regeneration), thematic Tensura skills lent to the boss itself, and a small anti-magic aura.</li>
 *   <li><b>When</b>: the first tick after joining, then a staggered refresh every {@value #REFRESH} ticks per boss ((game time + entity id)
 *       modulo 100). Nothing scans the world: the work is a loop over the handful of live bosses. The one per-tick read is the boss's
 *       exposure flag, so the barriers can be lifted the moment an ultimate leaves it exposed (and put back when it recovers).</li>
 *   <li><b>Fairness</b>: the barriers and the regeneration are suspended while the boss is exposed, transforming or (Lucius) under
 *       Julius's will; the config {@code bosses.globalBuffs} turns the engine off and {@code bosses.buffMultiplier} scales the levels.
 *       Idempotent: it reads what is on the boss and only changes the difference ({@link BossBuffTable#missing}).</li>
 *   <li><b>Aura</b>: the equivalent of Tensura's Anti-Magic Area for a mob: every refresh, while the fight is on, players within the
 *       aura's radius may have an Ultimate skill pushed onto a 2 to 3 second cooldown (Nullification.interfere): a cooldown, never a
 *       removal. Players with Anti-Magic are exempt.</li>
 * </ul>
 */
public final class BossBuffEngine {
    private BossBuffEngine() {}

    public static final int REFRESH = 100;

    private static final class Tracked {
        final LivingEntity entity;
        final String id;
        boolean applied, suspended, low, enabled = true;
        Tracked(LivingEntity entity, String id) { this.entity = entity; this.id = id; }
    }

    /** The live bosses (server main thread only; cleared when the server stops). */
    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();
    private static boolean warned;

    // ================================================================ events
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        String id = BossEntities.bossId(ent);
        if (id == null || !(ent instanceof LivingEntity le)) return;
        TRACKED.put(ent.getUUID(), new Tracked(le, id));
    }

    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        TRACKED.remove(e.getEntity().getUUID());
    }

    public static void onServerStopping(ServerStoppingEvent e) { TRACKED.clear(); }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (TRACKED.isEmpty()) return;
        for (Tracked t : new ArrayList<>(TRACKED.values())) {                           // a copy: nothing the refresh does can break the walk
            LivingEntity b = t.entity;
            if (b.isRemoved() || !b.isAlive()) { TRACKED.remove(b.getUUID(), t); continue; }
            if (!(b.level() instanceof ServerLevel sl)) continue;
            boolean on = BossBuffConfig.globalBuffs(), susp = suspended(b);
            boolean low = BossBuffTable.lowHealth(b.getHealth() / Math.max(1f, b.getMaxHealth()), t.low);
            long now = sl.getGameTime();
            boolean scheduled = (now + b.getId()) % REFRESH == 0;
            if (t.applied && !scheduled && susp == t.suspended && on == t.enabled && low == t.low) continue;
            if (scheduled && sl.getEntity(b.getUUID()) != b) { TRACKED.remove(b.getUUID(), t); continue; }      // it never made it into the level (a join another handler vetoed)
            t.applied = true;
            t.suspended = susp;
            t.low = low;
            t.enabled = on;
            try {
                refresh(sl, t, on, susp, low, now, scheduled);
            } catch (Throwable x) {
                if (!warned) {
                    warned = true;
                    LogUtils.getLogger().warn("[nusmp] boss buff engine failed for {}: {}", t.id, x.toString());
                }
            }
        }
    }

    // ================================================================ the work
    /** The boss cannot be hurt in full only if it is exposed, transforming, or (Lucius) Julius's will is up; then the barriers come off. */
    static boolean suspended(LivingEntity b) {
        if (b instanceof LuciusBoss l && l.julius()) return true;
        return b instanceof ZogratisBoss z && (z.exposed() || z.transforming());
    }

    static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    private static boolean engineOwned(MobEffectInstance i) { return i.isInfiniteDuration() && i.isAmbient(); }

    /** What of the engine's effects and skills is on the boss now. */
    static BossBuffTable.Applied read(LivingEntity b, BossBuffTable.Profile p) {
        Map<String, Integer> amps = new LinkedHashMap<>();
        for (String id : BossBuffTable.EFFECT_IDS) {
            Optional<Holder<MobEffect>> h = effect(id);
            MobEffectInstance i = h.isPresent() ? b.getEffect(h.get()) : null;
            if (i != null && engineOwned(i)) amps.put(id, i.getAmplifier());
        }
        Set<String> skills = new LinkedHashSet<>();
        for (String s : p.skills()) if (BossSkills.has(b, s)) skills.add(s);
        return new BossBuffTable.Applied(amps, skills);
    }

    private static void refresh(ServerLevel sl, Tracked t, boolean on, boolean susp, boolean low, long now, boolean scheduled) {
        BossBuffTable.Profile prof = BossBuffTable.profile(t.id);
        if (prof == null) return;                                               // not a row of the table: nothing is ever applied
        LivingEntity b = t.entity;
        BossBuffTable.Plan plan = on ? BossBuffTable.plan(t.id, BossBuffConfig.multiplier(), susp, low) : new BossBuffTable.Plan(Map.of(), Set.of());
        BossBuffTable.Applied now0 = read(b, prof);
        for (String id : BossBuffTable.surplus(plan, now0)) effect(id).ifPresent(h -> {
            MobEffectInstance i = b.getEffect(h);
            if (i != null && engineOwned(i)) b.removeEffect(h);
        });
        BossBuffTable.Plan todo = BossBuffTable.missing(plan, now0);
        for (Map.Entry<String, Integer> e : todo.amplifiers().entrySet()) effect(e.getKey()).ifPresent(h -> {
            if (b.hasEffect(h)) b.removeEffect(h);                              // a different level: replace it
            b.addEffect(new MobEffectInstance(h, MobEffectInstance.INFINITE_DURATION, e.getValue(), true, false, false));
        });
        for (String s : todo.skills()) BossSkills.grant(b, s);
        if (!on) for (String s : prof.skills()) if (BossSkills.has(b, s)) BossSkills.forget(b, s);     // the switch is off: the lent theme skills go too (a boss's own list only)
        if (b instanceof LuciusBoss l) LuciusProfile.refresh(l, sl, now);
        if (on && scheduled && !susp && prof.auraRadius() > 0) aura(sl, b, prof);
    }

    /** The anti-magic aura: during a fight, players near the boss may have an Ultimate skill jammed for 2 to 3 seconds (a cooldown, never a removal). */
    private static void aura(ServerLevel sl, LivingEntity b, BossBuffTable.Profile p) {
        if (!(b instanceof ZogratisBoss z) || !z.fightActive()) return;
        double r = p.auraRadius();
        AABB box = b.getBoundingBox().inflate(r);
        for (ServerPlayer pl : sl.getEntitiesOfClass(ServerPlayer.class, box, x -> x.isAlive() && !x.isCreative() && !x.isSpectator() && x.distanceToSqr(b) <= r * r)) {
            try {
                if (AntiMagic.isUser(pl)) continue;
                Nullification.interfere(pl, p.auraJamSeconds(), (float) p.auraChance(), 0.15f);
            } catch (Throwable noSkills) { /* nothing to jam */ }
        }
    }

    /** For tests and the status command: how many bosses the engine is tracking. */
    public static int tracked() { return TRACKED.size(); }
}
