package com.newuniverse.nusmp.relics.djinn;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The skeletal dragon's grip (owner notes: in the Full Djinn Equip Belial's massive skeletal dragon aura can physically restrain opponents). A passive of the canon Belial's Full Equip:
 * when the bearer lands a melee blow on a foe and the grip is ready, the dragon seizes that foe (the nearest one, the one in reach) and holds it where it stands for at most
 * {@link BelialGripRules#HOLD_TICKS} (2 s). The target stays hittable (it is restrained, not banished: unlike {@link BelialExile} it takes and deals blows as a player; a mob has no AI
 * while held), one victim per caster, a cooldown of 12 s per caster ({@link BelialGripRules#COOLDOWN_TICKS}), {@link BelialGripRules#MAX_GLOBAL} holds on the server. Never a boss, never a
 * creative or spectator player, never an ally, never what {@link BelialExile#vetoed} refuses (the guards of the exile: a boss fight near a player, a registered safe area), never a
 * target that is exiled.
 *
 * <p><b>Bounds.</b> Work per tick: nothing while no one is held; otherwise one entity lookup and a few field writes per hold (at most 16), a position hold every 4 ticks, one puff every 10
 * ticks. Start: one registry-free check and, for a player victim, one boss entity query (the exile's own guard). Packets: the vanilla ones of the calls (a hold teleport of a held player at
 * most every 4 ticks and only if it was pushed more than a block: at most 5 a second, for 2 s, once per 12 s per caster).
 * <p><b>Cleanup.</b> The hold ends at once when the victim or the caster dies, logs out or changes dimension, the caster's Full Equip ends, the setting is switched off, or the server stops;
 * the flags go back. A mob saved mid-hold carries {@code nusmp_belial_grip} and gets its mind back when it next loads. Server thread only; keyed by UUID.
 */
public final class BelialGrip {
    private BelialGrip() {}

    static final String MARK = "nusmp_belial_grip";

    private static final class Entry {
        final UUID victim, caster;
        final ResourceKey<Level> dim;
        final double x, y, z;
        final float yRot, xRot;
        final long until;
        final boolean wasNoAi, wasNoGravity;

        Entry(UUID victim, UUID caster, ResourceKey<Level> dim, Vec3 at, float yRot, float xRot, long until, boolean wasNoAi, boolean wasNoGravity) {
            this.victim = victim;
            this.caster = caster;
            this.dim = dim;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.until = until;
            this.wasNoAi = wasNoAi;
            this.wasNoGravity = wasNoGravity;
        }
    }

    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();
    /** Caster -> the game time the grip is ready again; victim -> the game time it can be gripped again. Both bounded and swept. */
    private static final Map<UUID, Long> READY = new HashMap<>();
    private static final Map<UUID, Long> IMMUNE = new HashMap<>();

    // ================================================================ queries
    public static boolean isGripped(UUID id) { return id != null && !ACTIVE.isEmpty() && ACTIVE.containsKey(id); }

    public static int activeCount() { return ACTIVE.size(); }

    private static boolean busy(UUID caster) {
        for (Entry e : ACTIVE.values()) if (e.caster.equals(caster)) return true;
        return false;
    }

    /** Is the caster's grip on cooldown at {@code now}? */
    public static boolean onCooldown(UUID caster, long now) {
        Long r = READY.get(caster);
        return r != null && BelialGripRules.running(r, now, BelialGripRules.COOLDOWN_TICKS);
    }

    // ================================================================ start
    /** Grips {@code t} for the caster. {@link BelialGripRules.Refusal#OK} or why not. The caller checked the caster's Full Equip. */
    public static BelialGripRules.Refusal start(ServerPlayer caster, LivingEntity t) {
        if (caster == null || t == null || !(t.level() instanceof ServerLevel level) || !t.isAlive() || t == caster || t.isAlliedTo(caster)) return BelialGripRules.Refusal.VETO;
        long now = level.getGameTime();
        UUID id = t.getUUID();
        if (onCooldown(caster.getUUID(), now)) return BelialGripRules.Refusal.NOT_READY;                    // the cheap answers first: no boss scan for a blow that cannot grip anyway
        if (busy(caster.getUUID())) return BelialGripRules.Refusal.CASTER_BUSY;
        BelialSeverRules.Kind kind = BelialSever.kindOf(t);
        boolean creative = t instanceof Player o && (o.isCreative() || o.isSpectator());
        Long imm = IMMUNE.get(id);
        BelialGripRules.Refusal r = BelialGripRules.canGrip(BelialConfig.grip(), kind, creative, kind != BelialSeverRules.Kind.BOSS && !creative && BelialExile.vetoed(t, caster.getUUID()),
                BelialExile.isExiled(id), imm != null && BelialGripRules.running(imm, now, BelialGripRules.IMMUNE_TICKS), ACTIVE.containsKey(id), onCooldown(caster.getUUID(), now), busy(caster.getUUID()), ACTIVE.size());
        if (r != BelialGripRules.Refusal.OK) return r;
        boolean mob = t instanceof Mob;
        boolean noAi = mob && ((Mob) t).isNoAi(), noGrav = t.isNoGravity();
        int len = BelialGripRules.holdTicks(BelialGripRules.HOLD_TICKS);
        ACTIVE.put(id, new Entry(id, caster.getUUID(), level.dimension(), t.position(), t.getYRot(), t.getXRot(), now + len, noAi, noGrav));
        READY.put(caster.getUUID(), now + BelialGripRules.COOLDOWN_TICKS);
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
        } else if (t instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, len + 2, 9, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, len + 2, 128, false, false));
        }
        t.setDeltaMovement(Vec3.ZERO);
        Vec3 mid = t.position().add(0, t.getBbHeight() * 0.5, 0);
        DjinnFx.ring(level, t.position(), 1.3, BelialCanon.COLOR, 16, 0.2);
        DjinnFx.tinted(level, BelialCanon.COLOR, mid.x, mid.y, mid.z, 14, 0.35, 0.6, 0.35, 0.03);
        level.playSound(null, BlockPos.containing(mid), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.5f);
        return BelialGripRules.Refusal.OK;
    }

    // ================================================================ end
    private static Entity find(MinecraftServer server, Entry e) {
        ServerLevel l = server.getLevel(e.dim);
        Entity ent = l == null ? null : l.getEntity(e.victim);
        if (ent != null) return ent;
        for (ServerLevel other : server.getAllLevels()) {                                                  // it changed dimension while held: it still carries the flags
            if (other == l) continue;
            ent = other.getEntity(e.victim);
            if (ent != null) return ent;
        }
        return null;
    }

    /** Ends one hold: the flags go back, the victim is immune for a while. */
    private static void end(MinecraftServer server, Entry e) {
        ACTIVE.remove(e.victim);
        Entity ent = find(server, e);
        if (ent instanceof LivingEntity t) {
            if (t instanceof Mob m) {
                m.setNoAi(e.wasNoAi);
                m.setNoGravity(e.wasNoGravity);
                m.getPersistentData().remove(MARK);
            }
            if (t.level() instanceof ServerLevel l) {
                if (IMMUNE.size() < BelialGripRules.MAX_TABLE) IMMUNE.put(e.victim, l.getGameTime() + BelialGripRules.IMMUNE_TICKS);
                DjinnFx.tinted(l, BelialCanon.GLOW, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    static void endAll(MinecraftServer server) {
        for (Entry e : new ArrayList<>(ACTIVE.values())) end(server, e);
    }

    private static void endWhere(MinecraftServer server, Predicate<Entry> which) {
        if (ACTIVE.isEmpty()) return;
        for (Entry e : new ArrayList<>(ACTIVE.values())) if (which.test(e)) end(server, e);
    }

    /** A mob loaded with the marker of a hold that is no longer running (a crash or an unload mid-hold) gets its flags back. */
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
        bus.addListener(BelialGrip::onServerTick);
        bus.addListener(BelialGrip::onBlow);
        bus.addListener(BelialGrip::onJoin);
        bus.addListener(BelialGrip::onDeath);
        bus.addListener(BelialGrip::onLogout);
        bus.addListener(BelialGrip::onDimension);
        bus.addListener(BelialGrip::onServerStopping);
    }

    /** The bearer's melee blow: the dragon seizes the foe it hit (Full Equip of the canon Belial only). */
    static void onBlow(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        LivingEntity t = e.getEntity();
        if (t == p || t.level().isClientSide() || !t.isAlive()) return;
        if (t instanceof Player o && !p.canHarmPlayer(o)) return;
        if (!BelialConfig.grip() || !BelialCanon.on()) return;
        DjinnState s = DjinnState.of(p);
        if (s.djinn() != Djinn.BELIAL || s.phase() < DjinnPhaseRules.FULL) return;
        start(p, t);
    }

    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 40 == 0 && (!READY.isEmpty() || !IMMUNE.isEmpty())) {
            long now = server.overworld().getGameTime();                                                   // every level shares the overworld's game clock
            BelialGripRules.sweep(READY, now);
            BelialGripRules.sweep(IMMUNE, now);
        }
        if (ACTIVE.isEmpty()) return;
        for (Entry en : new ArrayList<>(ACTIVE.values())) {
            ServerLevel l = server.getLevel(en.dim);
            Entity ent = l == null ? null : l.getEntity(en.victim);
            ServerPlayer caster = server.getPlayerList().getPlayer(en.caster);
            if (!(ent instanceof LivingEntity t) || !t.isAlive() || caster == null || !caster.isAlive() || caster.level() != l || !BelialConfig.grip()
                    || DjinnState.of(caster).phase() < DjinnPhaseRules.FULL) {
                end(server, en);                                                                           // gone, dead, in another world, or the Equip is over: flags back
                continue;
            }
            long now = l.getGameTime();
            if (now >= en.until) {
                end(server, en);
                continue;
            }
            if (now % BelialGripRules.HOLD_EVERY == 0) {
                t.setDeltaMovement(Vec3.ZERO);
                t.fallDistance = 0f;
                if (t.position().distanceToSqr(en.x, en.y, en.z) > BelialGripRules.HOLD_SLACK_SQ) {
                    if (t instanceof ServerPlayer sp && sp.connection != null) sp.connection.teleport(en.x, en.y, en.z, en.yRot, en.xRot);
                    else t.setPos(en.x, en.y, en.z);
                }
            }
            if (now % 10 == 0) DjinnFx.tinted(l, BelialCanon.COLOR, en.x, en.y + t.getBbHeight() * 0.5, en.z, 3, 0.3, 0.5, 0.3, 0.01);
        }
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide()) restoreMarked(e.getEntity());
    }

    static void onDeath(LivingDeathEvent e) {
        if (ACTIVE.isEmpty() || e.getEntity().level().isClientSide() || e.getEntity().getServer() == null) return;
        UUID id = e.getEntity().getUUID();
        endWhere(e.getEntity().getServer(), en -> en.victim.equals(id) || en.caster.equals(id));
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        UUID id = p.getUUID();
        READY.remove(id);
        endWhere(p.getServer(), en -> en.victim.equals(id) || en.caster.equals(id));
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (ACTIVE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        UUID id = p.getUUID();
        endWhere(p.getServer(), en -> en.victim.equals(id) || en.caster.equals(id));
    }

    static void onServerStopping(ServerStoppingEvent e) {
        endAll(e.getServer());
        ACTIVE.clear();
        READY.clear();
        IMMUNE.clear();
    }

    /** For the self-test: forgets the cooldown of a caster. */
    static void resetCooldown(UUID caster) { READY.remove(caster); }
}
