package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Belial's EXILE: the pale scythe cuts a target's spirit loose and it is "sent to another dimension". Played as a SHORT, SAFE state in place (never a teleport between real
 * dimensions): for 3 to 6 seconds ({@link BelialSeverRules#exileTicks}) the target has no AI (a mob), is invisible, held where it stood, cannot be hit, cannot hit, and cannot be
 * targeted; then it is back where it was. Bosses, creative and spectator players, a player while a boss is near (a boss fight owns its own rules), and anything a registered
 * guard vetoes ({@link #addVeto}) are never exiled: they take the sever hit instead (the act's job). Server thread only; everything is keyed by UUID.
 *
 * <p><b>Bounds.</b> At most {@link BelialSeverRules#MAX_PER_CASTER} victims per caster and {@link BelialSeverRules#MAX_GLOBAL} on the server, three per cast, a returning victim is
 * immune to a new exile for {@link BelialSeverRules#IMMUNE_TICKS}. Work per tick: nothing while nobody is exiled; otherwise one entity lookup and a few field writes per victim
 * (at most 24), a position hold every 4 ticks, one tinted puff every 10 ticks. Start: one entity query (players only, for the boss check) and one for mobs that hunt the victim
 * (at most 32 are touched). Packets: the vanilla ones of those calls (a hold teleport per exiled player every 4 ticks, at most 6 a second each).
 *
 * <p><b>Cleanup.</b> The victim logs out or dies, changes dimension, the caster dies or logs out or changes dimension, the server stops: the state ends at once and the flags are
 * given back. A mob's saved flags (NoAI, NoGravity) carry a marker in its persistent data, so a mob saved mid-exile (a crash, an unload) is given its mind back when it next loads.
 */
public final class BelialExile {
    private BelialExile() {}

    static final String MARK = "nusmp_belial_exile";

    /** One exile: who, by whom, where it stood, until when, and what the entity was before. */
    private static final class Entry {
        final UUID victim, caster;
        final ResourceKey<Level> dim;
        final double x, y, z;
        final float yRot, xRot;
        final long until;
        final boolean player, wasNoAi, wasNoGravity, ownInvisibility;

        Entry(UUID victim, UUID caster, ResourceKey<Level> dim, Vec3 at, float yRot, float xRot, long until, boolean player, boolean wasNoAi, boolean wasNoGravity, boolean ownInvisibility) {
            this.victim = victim;
            this.caster = caster;
            this.dim = dim;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.until = until;
            this.player = player;
            this.wasNoAi = wasNoAi;
            this.wasNoGravity = wasNoGravity;
            this.ownInvisibility = ownInvisibility;
        }
    }

    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, Long> IMMUNE = new HashMap<>();
    private static final List<Predicate<LivingEntity>> VETOES = new ArrayList<>();

    // ================================================================ queries
    public static boolean isExiled(UUID id) { return id != null && !ACTIVE.isEmpty() && ACTIVE.containsKey(id); }

    public static int activeCount() { return ACTIVE.size(); }

    /** How many victims this caster has out now. */
    public static int countOf(UUID caster) {
        int n = 0;
        for (Entry e : ACTIVE.values()) if (e.caster.equals(caster)) n++;
        return n;
    }

    /** A system that owns a no-exile rule (a safe area, a boss arena, a rest point) registers a guard: true = this entity may not be exiled. Registered once, at startup. */
    public static void addVeto(Predicate<LivingEntity> guard) { if (guard != null && !VETOES.contains(guard)) VETOES.add(guard); }

    private static boolean bossLike(LivingEntity e) { return BalanceLaw.isBoss(e) || BossIdentity.isBoss(e); }

    static boolean vetoed(LivingEntity t, UUID caster) {                                           // package-private: the bone dragon's grip (BelialGrip) asks the same guards
        if (t.getUUID().equals(caster)) return true;
        for (Predicate<LivingEntity> g : VETOES) if (g.test(t)) return true;
        if (t instanceof Player && t.level() instanceof ServerLevel l)                               // a player in a boss fight (a boss within 40 blocks) keeps the fight's own rules
            return !l.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(40.0), e -> e != t && e.isAlive() && bossLike(e)).isEmpty();
        return false;
    }

    // ================================================================ start
    /** Banishes {@code t} for {@code ticks} (the caller took them from {@link BelialSeverRules#exileTicks}). Returns {@link BelialSeverRules.Refusal#OK} or why not. */
    public static BelialSeverRules.Refusal start(ServerPlayer caster, LivingEntity t, int ticks) {
        if (caster == null || t == null || !(t.level() instanceof ServerLevel level) || !t.isAlive()) return BelialSeverRules.Refusal.VETO;
        long now = level.getGameTime();
        UUID id = t.getUUID();
        if (BelialGrip.isGripped(id)) return BelialSeverRules.Refusal.ALREADY;                           // a held mob already carries the flags: never two states on one mob
        BelialSeverRules.Kind kind = BelialSever.kindOf(t);
        boolean creative = t instanceof Player o && (o.isCreative() || o.isSpectator());
        Long imm = IMMUNE.get(id);
        BelialSeverRules.Refusal r = BelialSeverRules.canExile(BelialConfig.exile(), kind, creative, kind != BelialSeverRules.Kind.BOSS && !creative && vetoed(t, caster.getUUID()),
                imm != null && imm > now, ACTIVE.containsKey(id), countOf(caster.getUUID()), ACTIVE.size());
        if (r != BelialSeverRules.Refusal.OK) return r;
        int len = Math.max(BelialSeverRules.EXILE_MIN, Math.min(BelialSeverRules.EXILE_MAX, ticks));
        boolean mob = t instanceof Mob;
        boolean noAi = mob && ((Mob) t).isNoAi(), noGrav = t.isNoGravity(), invis = t.hasEffect(MobEffects.INVISIBILITY);
        ACTIVE.put(id, new Entry(id, caster.getUUID(), level.dimension(), t.position(), t.getYRot(), t.getXRot(), now + len, t instanceof Player, noAi, noGrav, !invis));
        if (mob) {
            Mob m = (Mob) t;
            CompoundTag d = m.getPersistentData();
            if (!d.contains(MARK)) {                                                                       // the flags to give back, saved with the mob
                CompoundTag c = new CompoundTag();
                c.putBoolean("noai", noAi);
                c.putBoolean("nograv", noGrav);
                d.put(MARK, c);
            }
            m.setNoAi(true);
            m.setNoGravity(true);
            m.setTarget(null);
            for (Mob hunter : level.getEntitiesOfClass(Mob.class, t.getBoundingBox().inflate(24.0), h -> h != t && h.getTarget() == t)) hunter.setTarget(null);   // nothing keeps hunting a ghost
        } else if (t instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, len + 2, 9, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, len + 2, 128, false, false));
        }
        if (!invis) t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, len + 2, 0, false, false, false));
        t.setDeltaMovement(Vec3.ZERO);
        Vec3 mid = t.position().add(0, t.getBbHeight() * 0.5, 0);
        DjinnFx.ring(level, t.position(), 1.4, BelialCanon.COLOR, 18, 0.2);
        DjinnFx.tinted(level, BelialCanon.GLOW, mid.x, mid.y, mid.z, 18, 0.3, 0.5, 0.3, 0.04);
        level.playSound(null, BlockPos.containing(mid), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.5f);
        return BelialSeverRules.Refusal.OK;
    }

    // ================================================================ end
    private static Entity find(MinecraftServer server, Entry e) {
        ServerLevel l = server.getLevel(e.dim);
        Entity ent = l == null ? null : l.getEntity(e.victim);
        if (ent != null) return ent;
        for (ServerLevel other : server.getAllLevels()) {                                                  // it changed dimension while out: it still carries the flags
            if (other == l) continue;
            ent = other.getEntity(e.victim);
            if (ent != null) return ent;
        }
        return null;
    }

    /** Ends one exile: the flags go back, the victim returns to where it stood (if it is still in that world and the place is loaded), it is immune for a while. */
    private static void end(MinecraftServer server, Entry e, boolean home) {
        ACTIVE.remove(e.victim);
        Entity ent = find(server, e);
        if (ent instanceof LivingEntity t) {
            if (t instanceof Mob m) {
                m.setNoAi(e.wasNoAi);
                m.setNoGravity(e.wasNoGravity);
                m.getPersistentData().remove(MARK);
            }
            if (e.ownInvisibility) t.removeEffect(MobEffects.INVISIBILITY);
            if (t instanceof ServerPlayer sp) {
                sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                sp.removeEffect(MobEffects.JUMP);
            }
            if (home && t.isAlive() && t.level() instanceof ServerLevel l && l.dimension().equals(e.dim) && l.hasChunkAt(BlockPos.containing(e.x, e.y, e.z))) {
                if (t.position().distanceToSqr(e.x, e.y, e.z) > BelialSeverRules.RETURN_SLACK * BelialSeverRules.RETURN_SLACK) {
                    if (t instanceof ServerPlayer sp && sp.connection != null) sp.connection.teleport(e.x, e.y, e.z, e.yRot, e.xRot);
                    else t.setPos(e.x, e.y, e.z);
                }
                t.setDeltaMovement(Vec3.ZERO);
                Vec3 mid = t.position().add(0, t.getBbHeight() * 0.5, 0);
                DjinnFx.ring(l, t.position(), 1.2, BelialCanon.GLOW, 14, 0.2);
                DjinnFx.tinted(l, BelialCanon.COLOR, mid.x, mid.y, mid.z, 12, 0.3, 0.5, 0.3, 0.03);
                l.playSound(null, BlockPos.containing(mid), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            if (t.level() instanceof ServerLevel l && IMMUNE.size() < BelialSeverRules.MAX_IMMUNE) IMMUNE.put(e.victim, l.getGameTime() + BelialSeverRules.IMMUNE_TICKS);
        }
    }

    static void endAll(MinecraftServer server, boolean home) {
        for (Entry e : new ArrayList<>(ACTIVE.values())) end(server, e, home);
    }

    private static void endWhere(MinecraftServer server, Predicate<Entry> which, boolean home) {
        if (ACTIVE.isEmpty()) return;
        for (Entry e : new ArrayList<>(ACTIVE.values())) if (which.test(e)) end(server, e, home);
    }

    /** A mob loaded with the marker of an exile that is no longer running (a crash or an unload mid-exile) gets its flags back. */
    static void restoreMarked(Entity ent) {
        if (!(ent instanceof Mob m)) return;
        CompoundTag d = m.getPersistentData();
        if (!d.contains(MARK) || ACTIVE.containsKey(m.getUUID())) return;
        CompoundTag c = d.getCompound(MARK);
        m.setNoAi(c.getBoolean("noai"));
        m.setNoGravity(c.getBoolean("nograv"));
        d.remove(MARK);
    }

    // ================================================================ events
    public static void register(IEventBus bus) {
        bus.addListener(BelialExile::onServerTick);
        bus.addListener(EventPriority.HIGHEST, BelialExile::onIncoming);
        bus.addListener(EventPriority.HIGHEST, BelialExile::onAttack);
        bus.addListener(BelialExile::onChangeTarget);
        bus.addListener(BelialExile::onJoin);
        bus.addListener(BelialExile::onDeath);
        bus.addListener(BelialExile::onLogout);
        bus.addListener(BelialExile::onDimension);
        bus.addListener(BelialExile::onServerStopping);
    }

    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 40 == 0) {
            long now = server.overworld().getGameTime();                                                   // every level shares the overworld's game clock
            BelialSever.sweep(now);
            BelialSeverRules.sweepImmune(IMMUNE, now);
        }
        if (ACTIVE.isEmpty()) return;
        for (Entry en : new ArrayList<>(ACTIVE.values())) {
            ServerLevel l = server.getLevel(en.dim);
            Entity ent = l == null ? null : l.getEntity(en.victim);
            if (!(ent instanceof LivingEntity t) || !t.isAlive()) {
                end(server, en, false);                                                                    // gone, dead or in another world: flags back, no teleport
                continue;
            }
            long now = l.getGameTime();
            if (now >= en.until) {
                end(server, en, true);
                continue;
            }
            if (now % BelialSeverRules.HOLD_EVERY == 0) {
                t.setDeltaMovement(Vec3.ZERO);
                t.fallDistance = 0f;
                if (t.position().distanceToSqr(en.x, en.y, en.z) > 0.25) {
                    if (t instanceof ServerPlayer sp && sp.connection != null) sp.connection.teleport(en.x, en.y, en.z, en.yRot, en.xRot);
                    else t.setPos(en.x, en.y, en.z);
                }
            }
            if (now % 10 == 0) DjinnFx.tinted(l, BelialCanon.GLOW, en.x, en.y + t.getBbHeight() * 0.5, en.z, 2, 0.25, 0.4, 0.25, 0.01);
        }
    }

    /** An exiled target takes nothing (only /kill and the void reach it) and deals nothing. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (ACTIVE.isEmpty()) return;
        LivingEntity t = e.getEntity();
        if (ACTIVE.containsKey(t.getUUID())) {
            DamageSource s = e.getSource();
            if (!s.is(DamageTypes.GENERIC_KILL) && !s.is(DamageTypes.FELL_OUT_OF_WORLD)) e.setCanceled(true);
            return;
        }
        Entity by = e.getSource().getEntity();
        if (by != null && ACTIVE.containsKey(by.getUUID())) e.setCanceled(true);
    }

    static void onAttack(AttackEntityEvent e) {
        if (!ACTIVE.isEmpty() && ACTIVE.containsKey(e.getEntity().getUUID())) e.setCanceled(true);
    }

    /** Nothing picks an exiled target. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (ACTIVE.isEmpty()) return;
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next != null && ACTIVE.containsKey(next.getUUID())) e.setNewAboutToBeSetTarget(null);
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide()) restoreMarked(e.getEntity());
    }

    /** A victim dies, or a caster dies: the state ends where it stands. */
    static void onDeath(LivingDeathEvent e) {
        if (ACTIVE.isEmpty() || e.getEntity().level().isClientSide() || e.getEntity().getServer() == null) return;
        UUID id = e.getEntity().getUUID();
        endWhere(e.getEntity().getServer(), en -> en.victim.equals(id) || en.caster.equals(id), false);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (ACTIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        UUID id = p.getUUID();
        endWhere(p.getServer(), en -> en.victim.equals(id) || en.caster.equals(id), false);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (ACTIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        UUID id = p.getUUID();
        endWhere(p.getServer(), en -> en.victim.equals(id) || en.caster.equals(id), false);
    }

    static void onServerStopping(ServerStoppingEvent e) {
        endAll(e.getServer(), false);
        ACTIVE.clear();
        IMMUNE.clear();
        BelialSever.clearAll();
    }
}
