package com.newuniverse.nusmp.manacore;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.song.SongTargets;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: everything the rank Mana Cores do between presses (3.51.0): the once-a-second upkeep of the passive of the core a player holds or wears, the damage hooks of the passives, and all per-player runtime state.
 * Cost and bounds: a player with no core and no state costs one map read per tick and one hand check per second (staggered by entity id); one player with a core costs a few attribute writes, at most one
 * entity query (radius 12, capped at 6 hits) and at most 16 item pulls per second; nothing is spawned. State is keyed by player UUID, lives in {@link #RUN}, and is dropped on logout, death, dimension change,
 * server stop, and when the core leaves the hands. Server only: no client class is named here.
 */
public final class ManaCoreRuntime {
    private ManaCoreRuntime() {}

    /** One player's runtime state. */
    static final class Run {
        final Map<String, Long> readyAt = new HashMap<>();
        final List<Vec3> history = new ArrayList<>();                // one position per second, newest last
        final List<String> mods = new ArrayList<>();                 // attribute ids that carry one of our modifiers (to remove them again)
        final List<Rune> runes = new ArrayList<>();
        final List<Vec3> trail = new ArrayList<>();
        final List<Delayed> delayed = new ArrayList<>();
        String applied = "";                                         // id of the core whose passive is on
        String histDim = "";
        long shieldAt, phaseAt, projAt, lethalAt, lastCast, lastHurt, infuseUntil, barrierUntil, thornsUntil, warnAt, summon;
        int casts;
        LastSecond last;

        boolean busy() { return last != null || !delayed.isEmpty() || !runes.isEmpty(); }

        boolean idle(long now) {
            if (!applied.isEmpty() || busy() || !trail.isEmpty()) return false;
            readyAt.values().removeIf(t -> t <= now);
            return readyAt.isEmpty();
        }
    }

    record Rune(String dim, Vec3 at, long expire) {}
    record Delayed(long at, Vec3 center, String coreId, int n, String dim) {}
    record LastSecond(String dim, Vec3 center, long end) {}

    static final Map<UUID, Run> RUN = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IN_THORNS = ThreadLocal.withInitial(() -> false);
    static final long STAMP_GAP = 20;

    static Run run(ServerPlayer p) { return RUN.computeIfAbsent(p.getUUID(), u -> new Run()); }

    public static int runs() { return RUN.size(); }

    // ================================================================== tick
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        Run r = RUN.get(p.getUUID());
        if (r != null && r.busy()) ManaCoreActives.tickBusy(p, r);
        if ((p.tickCount + (p.getId() & 15)) % 20 != 0) return;
        second(p, r);
    }

    /** The carried core: main hand, off hand, then Curios. Null for none. */
    static ManaCoreRules.Core carried(ServerPlayer p) {
        ManaCoreRules.Core c = ManaCoreItems.coreOf(p.getMainHandItem());
        if (c == null) c = ManaCoreItems.coreOf(p.getOffhandItem());
        if (c == null && CuriosLink.available()) for (ItemStack s : CuriosLink.equippedCached(p, 20)) if ((c = ManaCoreItems.coreOf(s)) != null) break;
        return c;
    }

    /** May this player use the core's power (the feature is on and the rank's Existence Points are there)? */
    static boolean allowed(ServerPlayer p, ManaCoreRules.Core c) {
        return ManaCoreConfig.enabled() && ManaCoreRules.mayUse(c.rank(), TensuraBridge.ep(p), ManaCoreConfig.epGate());
    }

    static void second(ServerPlayer p, Run r) {
        long now = p.level().getGameTime();
        ManaCoreRules.Core c = ManaCoreConfig.enabled() && p.isAlive() && !p.isSpectator() ? carried(p) : null;
        if (c != null && !allowed(p, c)) {
            if (r == null) r = run(p);
            if (now - r.warnAt > 600) {
                r.warnAt = now;
                p.displayClientMessage(Component.translatable("manacore.msg.too_weak", ManaCoreRules.RANK_LABEL[c.rank()], (long) ManaCoreRules.EP_GATE[c.rank()]), true);
            }
            c = null;
        }
        if (c == null) {
            if (r != null) {
                if (!r.applied.isEmpty()) unequip(p, r);
                if (r.idle(now)) RUN.remove(p.getUUID());
            }
            return;
        }
        if (r == null) r = run(p);
        if (!c.id().equals(r.applied)) {
            clearMods(p, r);
            r.applied = c.id();
            applyMods(p, r, c);
        }
        passiveSecond(p, r, c, now);
    }

    static void unequip(ServerPlayer p, Run r) {
        clearMods(p, r);
        r.applied = "";
        r.trail.clear();
    }

    // ================================================================== modifiers
    private static ResourceLocation modId(String coreId, int i) { return ResourceLocation.fromNamespaceAndPath("nusmp", "manacore/" + coreId + "/" + i); }

    private static Holder<Attribute> attr(String id) { return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).orElse(null); }

    private static void applyMods(ServerPlayer p, Run r, ManaCoreRules.Core c) {
        int i = 0;
        for (ManaCoreRules.Mod m : c.passive().mods()) {
            Holder<Attribute> a = attr(m.attribute());
            AttributeInstance inst = a == null ? null : p.getAttribute(a);
            if (inst != null) {
                ResourceLocation id = modId(c.id(), i);
                inst.removeModifier(id);
                inst.addTransientModifier(new AttributeModifier(id, m.amount(), m.op().equals("add") ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                r.mods.add(m.attribute() + "|" + id);
            }
            i++;
        }
    }

    static void clearMods(ServerPlayer p, Run r) {
        for (String s : r.mods) {
            int bar = s.indexOf('|');
            Holder<Attribute> a = attr(s.substring(0, bar));
            AttributeInstance inst = a == null ? null : p.getAttribute(a);
            if (inst != null) inst.removeModifier(ResourceLocation.parse(s.substring(bar + 1)));
        }
        r.mods.clear();
    }

    /** True when any core modifier is on this player (for tests). */
    public static boolean anyMods(ServerPlayer p) {
        Run r = RUN.get(p.getUUID());
        return r != null && !r.mods.isEmpty();
    }

    // ================================================================== the passive, once a second
    private static void effect(ServerPlayer p, String id, int amp, int ticks) {
        Holder<net.minecraft.world.effect.MobEffect> h = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null);
        if (h != null) p.addEffect(new MobEffectInstance(h, ticks, amp, true, false));
    }

    private static void passiveSecond(ServerPlayer p, Run r, ManaCoreRules.Core c, long now) {
        ManaCoreRules.Passive ps = c.passive();
        for (ManaCoreRules.Fx f : ps.effects()) effect(p, f.effect(), f.amp(), 260);
        // magicules per second as a share of the maximum (a leak is negative, and never goes under zero)
        double pct = ps.mpPct();
        if (ps.flag("combat_mp") > 0 && now - r.lastHurt < 200) pct += ps.flag("combat_mp");
        if (pct != 0) addMagicule(p, pct);
        if (ps.flag("poison_immune") > 0) p.removeEffect(MobEffects.POISON);
        if (ps.flag("fire_immune") > 0) {
            p.clearFire();
            effect(p, "minecraft:fire_resistance", 0, 60);
        }
        if (ps.flag("time_immune") > 0) TimeStop.immune(p, 60);
        // one position a second, for Temporal Echo and Temporal Safeguard (cleared by a dimension change)
        String dim = p.level().dimension().location().toString();
        if (!dim.equals(r.histDim)) {
            r.history.clear();
            r.histDim = dim;
        }
        r.history.add(p.position());
        while (r.history.size() > ManaCoreRules.POSITION_KEEP) r.history.remove(0);
        if (ps.flag("item_pull") > 0) pullItems(p, ps.flag("item_pull"));
        if (ps.flag("void_trail") > 0) voidTrail(p, r, c, (int) ps.flag("void_trail"));
    }

    static void addMagicule(ServerPlayer p, double pct) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = TensuraBridge.maxMagicule(p);
            if (ex == null || max <= 0) return;
            double now = ex.getMagicule();
            double next = Math.max(0, Math.min(max, now + max * pct));
            if (next != now) {
                ex.setMagicule(next);
                ex.markDirty();
            }
        } catch (Throwable t) { /* Tensura storage unavailable: no regeneration, nothing else to do */ }
    }

    private static void pullItems(ServerPlayer p, double radius) {
        int n = 0;
        for (ItemEntity it : p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(radius), i -> i.isAlive() && i.distanceToSqr(p) > 1.5)) {
            Vec3 d = p.position().add(0, 0.5, 0).subtract(it.position());
            it.setDeltaMovement(it.getDeltaMovement().scale(0.4).add(d.normalize().scale(0.35)));
            it.hurtMarked = true;
            if (++n >= 16) break;
        }
    }

    private static void voidTrail(ServerPlayer p, Run r, ManaCoreRules.Core c, int maxHits) {
        r.trail.add(p.position());
        while (r.trail.size() > 6) r.trail.remove(0);
        if (r.trail.size() < 2) return;
        int hits = 0;
        double dmg = ManaCoreRules.POWER[c.rank()] * 0.3;
        for (LivingEntity t : SongTargets.foes(p, p.position(), 12)) {
            if (hits >= maxHits) break;
            if (!ManaCoreActives.allowed(p, t)) continue;
            for (int i = 0; i < r.trail.size() - 1; i++) {
                if (t.position().distanceToSqr(r.trail.get(i)) <= 2.56) {
                    ManaCoreActives.hurt(p, t, dmg);
                    hits++;
                    break;
                }
            }
        }
    }

    // ================================================================== damage hooks
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (RUN.isEmpty()) return;
        DamageSource src = e.getSource();
        if (src.getEntity() instanceof ServerPlayer a && a != e.getEntity()) attackerSide(e, a);
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        Run r = RUN.get(p.getUUID());
        if (r == null) return;
        long now = p.level().getGameTime();
        r.lastHurt = now;
        ManaCoreRules.Core c = r.applied.isEmpty() ? null : ManaCoreRules.byId(r.applied);
        float amount = e.getAmount();
        boolean projectile = src.getDirectEntity() instanceof Projectile;
        boolean melee = src.getEntity() instanceof LivingEntity && !projectile && !src.is(DamageTypeTags.IS_EXPLOSION);
        if (now < r.barrierUntil && (projectile || src.is(DamageTypeTags.IS_PROJECTILE))) {
            e.setCanceled(true);
            return;
        }
        if (c == null) return;
        ManaCoreRules.Passive ps = c.passive();
        if (ps.flag("lethal_rewind") > 0 && now >= r.lethalAt && amount >= p.getHealth() + p.getAbsorptionAmount() && !src.is(DamageTypes.GENERIC_KILL) && !src.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            r.lethalAt = now + ManaCoreRules.LETHAL_REWIND_COOLDOWN;
            e.setCanceled(true);
            p.setHealth(Math.max(1f, p.getMaxHealth() * 0.5f));
            ManaCoreActives.rewindTo(p, r, ManaCoreRules.historyIndex(r.history.size(), 3));
            p.displayClientMessage(Component.translatable("manacore.msg.safeguard"), true);
            return;
        }
        if (ps.flag("fire_immune") > 0 && (src.is(DamageTypeTags.IS_FIRE))) {
            e.setCanceled(true);
            return;
        }
        if (ps.flag("lightning_resist") > 0 && src.is(DamageTypeTags.IS_LIGHTNING)) amount *= (float) (1.0 - ps.flag("lightning_resist"));
        if (ps.flag("magic_resist") > 0 && (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC))) amount *= (float) (1.0 - ps.flag("magic_resist"));
        if (ps.flag("proj_absorb") > 0 && projectile && amount <= ps.flag("proj_absorb") && now >= r.projAt) {
            r.projAt = now + 20;
            addMagicule(p, ps.flag("proj_mp"));
            e.setCanceled(true);
            return;
        }
        if (ps.flag("proj_shield") > 0 && projectile && now >= r.projAt) {
            r.projAt = now + (long) ps.flag("proj_shield");
            e.setCanceled(true);
            return;
        }
        if (melee && ps.flag("dodge") > 0 && p.getRandom().nextDouble() < ps.flag("dodge")) {
            e.setCanceled(true);
            return;
        }
        if (src.getEntity() != null && ps.flag("phase_cd") > 0 && now >= r.phaseAt) {
            r.phaseAt = now + (long) ps.flag("phase_cd");
            e.setCanceled(true);
            return;
        }
        if (ps.flag("aura_shield") > 0 && now >= r.shieldAt) {
            r.shieldAt = now + 200;
            amount = Math.max(0f, amount - (float) ps.flag("aura_shield"));
        }
        e.setAmount(amount);
        if (amount <= 0f) {
            e.setCanceled(true);
            return;
        }
        if (melee && src.getEntity() instanceof LivingEntity foe && foe != p && !IN_THORNS.get()) {
            double th = ps.flag("thorns");
            if (now < r.thornsUntil) th = Math.max(th, ManaCoreRules.POWER[c.rank()] * 0.5);
            if (th > 0) {
                IN_THORNS.set(true);
                try { foe.hurt(p.damageSources().indirectMagic(p, p), (float) th); } finally { IN_THORNS.set(false); }
            }
        }
    }

    /** The attacker's own core: the elemental infusion ignites what it hits. */
    private static void attackerSide(LivingIncomingDamageEvent e, ServerPlayer a) {
        Run r = RUN.get(a.getUUID());
        if (r != null && a.level().getGameTime() < r.infuseUntil && e.getSource().getDirectEntity() == a) e.getEntity().igniteForSeconds(3);
    }

    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v instanceof ServerPlayer p) {
            cleanup(p);
            return;
        }
        if (RUN.isEmpty() || !(e.getSource().getEntity() instanceof ServerPlayer k) || k.level().isClientSide) return;
        Run r = RUN.get(k.getUUID());
        ManaCoreRules.Core c = r == null || r.applied.isEmpty() ? null : ManaCoreRules.byId(r.applied);
        if (c == null) return;
        double heal = c.passive().flag("kill_heal");
        if (heal > 0) k.heal((float) (k.getMaxHealth() * heal));
    }

    // ================================================================== cleanup
    /** Drops everything of this player: modifiers, runes, a countdown, delayed bursts (death, logout, dimension change, server stop). */
    public static void cleanup(ServerPlayer p) {
        Run r = RUN.remove(p.getUUID());
        if (r != null) clearMods(p, r);
    }

    public static void clearAll() { RUN.clear(); }

    /** For tests: the player's readiness of one active (game tick it is ready at), or 0. */
    public static long readyAt(ServerPlayer p, String key) {
        Run r = RUN.get(p.getUUID());
        return r == null ? 0 : r.readyAt.getOrDefault(key, 0L);
    }

    static ServerLevel level(ServerPlayer p) { return p.serverLevel(); }

    static AABB box(Vec3 c, double r) { return new AABB(c, c).inflate(r); }
}
