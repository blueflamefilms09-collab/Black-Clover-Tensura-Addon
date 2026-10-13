package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusCells;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the Monarch of the Monarch's Vault from outside (docs/living_dungeon.md section 5, zone 1). Its body is a real mob of Tensura, driven
 * by a brain of its own, so this does what dungeon/wild/WildDriver does: one handler on the server tick, with nothing but the public API of a
 * mob (a target, the navigation, a strike). The Monarch sleeps standing on its dais (no brain, no target, not to be hurt) until a player comes
 * within eighteen blocks, hits it, or a relic is taken; then it roars and the fight is a play of area attacks ({@link MonarchAttacks}) chosen
 * by {@link MonarchScheduler} in three phases (at 66% and 33% of its health), with shadow soldiers to manage. Every attack is telegraphed and
 * can be dodged or answered. If everybody leaves or falls it goes back to sleep, healed, its soldiers gone. When it dies it posts the Nexus's
 * {@link DungeonCollapseEvent} and drops nothing but experience. All state is kept while its chunk is loaded and nothing but the marks on the
 * entity is saved, so a restart or a relog clears every temporary effect.
 */
public final class MonarchDriver {
    private MonarchDriver() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The fights of the Monarchs that are loaded, by their entity's UUID; the soldiers seen joining, by UUID, with the tick they were first seen. */
    private static final Map<UUID, MonarchFight> FIGHTS = new LinkedHashMap<>();
    private static final Map<UUID, Long> SOLDIERS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    /** The core cells (NexusCells keys) whose hall has been told to collapse in this session: the Monarch's fall adds no second collapse to one that is under way. */
    private static final Set<Long> COLLAPSING = new HashSet<>();
    /** Ticks a soldier may stand without a Monarch to serve before it is let go. */
    private static final int ORPHAN_AFTER = 200;
    /** How far from the middle of a hall a block still belongs to it (its shell and the edge the Nexus counts as inside). */
    private static final double HALL = MonarchVaultPlan.OUTER + 2;

    // ---------------------------------------------------------------- events
    /** A Monarch (or a soldier) joins the Dungeon's level, new or loaded from disk: it is taken into the driver. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel l) || !LivingDungeon.in(l)) return;
        Entity en = e.getEntity();
        if (MonarchBoss.is(en)) {
            Mob m = (Mob) en;
            MonarchFight f = fightOf(l, m);
            if (MonarchBoss.asleep(m)) {
                m.setNoAi(true);
            } else {                                                          // it was fighting when the world was saved: the fight starts again where its health is
                m.setNoAi(false);
                f.awake = true;
                f.phase = MonarchRules.phaseOf(m.getHealth(), m.getMaxHealth());
                f.scheduler.reset(l.getGameTime(), 80);
                f.lastFoe = l.getGameTime();
            }
        } else if (MonarchBoss.isSoldier(en)) {
            SOLDIERS.putIfAbsent(en.getUUID(), l.getGameTime());
        }
    }

    /** The fight of a Monarch: the one the driver already has, else a new one made now, so a Monarch the driver was not told of is never deaf to a blow or a step. */
    private static MonarchFight fightOf(ServerLevel l, Mob m) {
        MonarchFight f = FIGHTS.get(m.getUUID());
        if (f == null) {
            f = new MonarchFight(m.getUUID(), MonarchBoss.centreOf(m));
            FIGHTS.put(m.getUUID(), f);
            if (!MonarchBoss.asleep(m)) {
                f.awake = true;
                f.phase = MonarchRules.phaseOf(m.getHealth(), m.getMaxHealth());
                f.scheduler.reset(l.getGameTime(), 80);
                f.lastFoe = l.getGameTime();
            }
        }
        return f;
    }

    public static void onServerStopping(ServerStoppingEvent e) { clear(); }

    private static void clear() {
        for (MonarchFight f : FIGHTS.values()) f.bar.removeAllPlayers();
        FIGHTS.clear();
        SOLDIERS.clear();
        WARNED.clear();
        COLLAPSING.clear();
    }

    /** A hall is made (again): whatever was said of an earlier collapse of it no longer holds. */
    static void forgetCollapse(BlockPos centre) { COLLAPSING.remove(cellKey(centre)); }

    private static long cellKey(BlockPos at) { return NexusCells.key(Math.floorDiv(at.getX(), NexusZone.CELL), Math.floorDiv(at.getZ(), NexusZone.CELL)); }

    /** Has the Nexus of this hall already collapsed (its record says so)? */
    private static boolean collapsed(ServerLevel l, BlockPos centre) {
        return NexusZones.stateOf(l.getServer(), Math.floorDiv(centre.getX(), NexusZone.CELL), Math.floorDiv(centre.getZ(), NexusZone.CELL)) == NexusState.COLLAPSED;
    }

    /** While it sleeps nothing hurts it, but a blow wakes it. While it fights each blow is soaked and capped, and it cannot be taken through a phase at once. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer victim) {
                capMelee(e, victim);
                return;
            }
            if (!(e.getEntity() instanceof Mob m) || !MonarchBoss.is(m) || !(m.level() instanceof ServerLevel l)) return;
            guard(e, m, l);
        } catch (RuntimeException | LinkageError ex) {
            warn("guarding the Monarch", ex);                                    // a blow must never bring the server down
        }
    }

    private static void guard(LivingIncomingDamageEvent e, Mob m, ServerLevel l) {
        Entity source = e.getSource().getEntity();
        ServerPlayer by = source instanceof ServerPlayer sp ? sp : null;
        if (e.getSource().is(DamageTypes.GENERIC_KILL)) return;                  // /kill still kills it
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) {                         // the Dungeon is switched off: nothing is left asleep, and no phase floor can hold it
            if (MonarchBoss.asleep(m)) MonarchBoss.wake(m);
            return;
        }
        MonarchFight f = fightOf(l, m);
        if (MonarchBoss.asleep(m)) {
            e.setCanceled(true);
            if (source != null && source != m) rouse(l, m, f, by);
            return;
        }
        if (by != null) f.lastAttacker = by.getUUID();
        if (f.transitioning(l.getGameTime())) {
            e.setCanceled(true);
            return;
        }
        float real = MonarchRules.gate(m.getHealth(), m.getMaxHealth(), MonarchRules.realDamage(e.getAmount(), m.getMaxHealth()), f.phase);
        if (real <= 0f) {
            e.setCanceled(true);
            return;
        }
        e.setAmount(real);
    }

    /** A blow struck by the Monarch's own hand or a soldier's takes at most a share of the player's health (its area attacks have their own, smaller, caps). */
    private static void capMelee(LivingIncomingDamageEvent e, ServerPlayer victim) {
        Entity source = e.getSource().getEntity();
        if (source == null || e.getSource().is(DamageTypes.INDIRECT_MAGIC)) return;
        boolean boss = MonarchBoss.is(source);
        if (!boss && !MonarchBoss.isSoldier(source)) return;
        e.setAmount(MonarchRules.capMelee(e.getAmount(), victim.getMaxHealth(), boss));
    }

    /** The Monarch falls: the Nexus collapses (the event is the same one the central relic posts), the server is told, there is experience and nothing else. */
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !(m.level() instanceof ServerLevel l)) return;
        if (MonarchBoss.is(m)) {
            fell(l, m, e);
        } else if (MonarchBoss.isSoldier(m)) {
            SOLDIERS.remove(m.getUUID());
            l.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 0.6, m.getZ(), 16, 0.4, 0.6, 0.4, 0.07);
        }
    }

    /** Loot-free: neither the Monarch nor its soldiers leave anything behind (nor a carcass: the Dungeon's own drop handler comes after this one). */
    public static void onDrops(LivingDropsEvent e) {
        Entity v = e.getEntity();
        if (MonarchBoss.is(v) || MonarchBoss.isSoldier(v)) e.setCanceled(true);
    }

    /**
     * The Nexus is collapsing (a relic was taken, or somebody's Wild Magic backfired): the hall that holds the place of the event is marked, and
     * its Monarch wakes if it was asleep. The event says where the trouble is (the pedestal that was robbed, a player), not the middle of the hall,
     * and its zone is the cell's hashed one, which a forced zone does not share, so the hall is found by position alone.
     */
    public static void onCollapse(DungeonCollapseEvent e) {
        try {
            MonarchFight f = hallAt(e.centre());
            if (f == null) return;
            COLLAPSING.add(cellKey(f.centre));
            if (e.bossDefeated()) return;                                        // the Monarch's own fall: nothing to wake
            if (e.level().getEntity(f.boss) instanceof Mob m && m.isAlive() && MonarchBoss.asleep(m)) rouse(e.level(), m, f, e.cause());
        } catch (RuntimeException | LinkageError ex) {
            warn("answering a collapse in the Monarch's Vault", ex);
        }
    }

    /** The fight of the loaded Monarch whose hall (shell, dome and a little below the floor) holds this block, or null. */
    private static MonarchFight hallAt(BlockPos at) {
        for (MonarchFight f : FIGHTS.values()) {
            double dx = at.getX() - f.centre.getX(), dz = at.getZ() - f.centre.getZ();
            if (dx * dx + dz * dz > HALL * HALL) continue;
            if (at.getY() >= f.centre.getY() - ZonePlan.BELOW && at.getY() <= f.centre.getY() + MonarchVaultPlan.HEIGHT + MonarchVaultPlan.ROOF + ZonePlan.ABOVE) return f;
        }
        return null;
    }

    // ---------------------------------------------------------------- waking, from outside
    /** Wakes the Monarch whose Nexus has this centre (give or take a block or two), if it is loaded and asleep. Returns true if one woke. */
    public static boolean rouseAt(ServerLevel level, BlockPos centre, ServerPlayer cause) {
        for (MonarchFight f : new ArrayList<>(FIGHTS.values())) {
            if (Math.abs(f.centre.getX() - centre.getX()) > 3 || Math.abs(f.centre.getZ() - centre.getZ()) > 3 || Math.abs(f.centre.getY() - centre.getY()) > 6) continue;
            if (level.getEntity(f.boss) instanceof Mob m && m.isAlive() && MonarchBoss.asleep(m)) {
                rouse(level, m, f, cause);
                return true;
            }
        }
        return false;
    }

    /** Is the Monarch of this Nexus loaded and awake? */
    public static boolean fighting(BlockPos centre) {
        for (MonarchFight f : FIGHTS.values()) if (f.awake && Math.abs(f.centre.getX() - centre.getX()) <= 3 && Math.abs(f.centre.getZ() - centre.getZ()) <= 3) return true;
        return false;
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (FIGHTS.isEmpty() && SOLDIERS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel l = server.getLevel(LivingDungeon.DIMENSION);
        if (l == null) {
            clear();
            return;
        }
        long now = l.getGameTime();
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) {                          // switched off: nothing is left asleep or held still
            if (now % 100 == 7) releaseAll(l);
            return;
        }
        for (MonarchFight f : new ArrayList<>(FIGHTS.values())) {                 // a copy: a blow that kills the Monarch (thorns) removes its fight from the map
            if (FIGHTS.get(f.boss) != f) continue;
            Entity en = l.getEntity(f.boss);
            if (!(en instanceof Mob m) || !m.isAlive() || m.isRemoved()) {      // dead, or its chunk is gone: nothing of the fight is kept
                f.hazards.clear();
                f.bar.removeAllPlayers();
                FIGHTS.remove(f.boss);
                continue;
            }
            try {
                tick(l, m, f, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("running the Monarch", ex);
            }
        }
        if (now % 20 == 3) {
            try {
                soldiers(l, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("looking after the soldiers", ex);
            }
        }
    }

    private static void tick(ServerLevel l, Mob m, MonarchFight f, long now) {
        if (f.frozenUntil != 0 && now >= f.frozenUntil) {                         // its stillness is over: the brain comes back, whatever fails later in this tick
            f.frozenUntil = 0;
            if (!MonarchBoss.asleep(m)) m.setNoAi(false);
        }
        if (now % 20 == 11 && collapsed(l, f.centre) && MonarchAttacks.hallFoes(l, f).isEmpty()) {          // the hall is gone and nobody is left in it: so is its master
            vanish(l, m, f);
            return;
        }
        if (MonarchBoss.asleep(m)) {
            if (now % 10 == 0) sleeping(l, m, f, now);
            return;
        }
        if (m.isNoAi() && !f.frozen(now)) m.setNoAi(false);                       // awake and not held still for a move: its brain is on, whatever left it off
        Vec3 centre = f.centreVec();
        List<ServerPlayer> foes = MonarchAttacks.hallFoes(l, f);
        if (!foes.isEmpty()) f.lastFoe = now;
        if (now % 10 == 0) bar(l, m, f);
        int ph = MonarchRules.phaseOf(m.getHealth(), m.getMaxHealth());
        if (ph > f.phase && !f.transitioning(now)) transition(l, m, f, ph, now);

        for (Iterator<MonarchFight.Hazard> it = f.hazards.iterator(); it.hasNext(); ) {
            MonarchFight.Hazard h = it.next();
            boolean over;
            try {
                over = h.tick(l, m, f, now);
            } catch (RuntimeException | LinkageError ex) {
                warn("running an attack of the Monarch", ex);
                over = true;
            }
            if (over) it.remove();
        }
        if (now % 60 == 0) VfxSpawn.sendFollowing(l, VfxShape.KOTO_AURA, m, m.position(), 0xFFB026FF, 70, 2.2f);
        if (f.transitioning(now) || f.frozen(now)) return;

        ServerPlayer target = MonarchAttacks.nearest(foes, m);
        if (target != null && now % 5 == 0) pursue(l, m, f, target, now);
        if (target != null && now >= f.scheduler.nextAt()) {
            MonarchScheduler.Situation s = new MonarchScheduler.Situation(f.phase, MonarchAttacks.alive(l, f), foes.size(), Math.sqrt(m.distanceToSqr(target)));
            MonarchMove move = f.scheduler.pick(now, s, l.random.nextDouble());
            if (move != null) start(l, m, f, move, target, foes, now);
        }
        if (now % 20 == 0) {
            if (m.position().distanceToSqr(centre) > 52.0 * 52.0) {              // it does not leave its hall
                Vec3 a = MonarchBoss.anchor(f.centre);
                m.moveTo(a.x, a.y, a.z, m.getYRot(), 0f);
                m.getNavigation().stop();
            }
            if (foes.isEmpty() && now - f.lastFoe > MonarchRules.RESET_AFTER) restSoldiersAndSleep(l, m, f);
        }
    }

    /** The Dungeon is switched off: every Monarch that is loaded wakes and stands free, its bar and its soldiers go. */
    private static void releaseAll(ServerLevel l) {
        for (MonarchFight f : new ArrayList<>(FIGHTS.values())) {
            f.hazards.clear();
            f.bar.removeAllPlayers();
            f.frozenUntil = 0;
            f.transitionUntil = 0;
            if (l.getEntity(f.boss) instanceof Mob m && m.isAlive()) {
                MonarchBoss.wake(m);
                m.setNoAi(false);
            }
            dismissSoldiers(l, f);
            f.awake = false;
        }
    }

    /** The hall has collapsed: the Monarch and every soldier go with it, in a puff of shadow. */
    private static void vanish(ServerLevel l, Mob m, MonarchFight f) {
        f.hazards.clear();
        clearEffects(l, f);
        dismissSoldiers(l, f);
        f.bar.removeAllPlayers();
        FIGHTS.remove(f.boss);
        l.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 1.0, m.getZ(), 40, 0.8, 1.2, 0.8, 0.08);
        m.discard();
    }

    // ---------------------------------------------------------------- sleeping, waking, going back to sleep
    private static void sleeping(ServerLevel l, Mob m, MonarchFight f, long now) {
        Vec3 a = MonarchBoss.anchor(f.centre);
        if (m.position().distanceToSqr(a) > 0.5) m.moveTo(a.x, a.y, a.z, m.getYRot(), 0f);          // somebody pushed it
        if (m.getHealth() < m.getMaxHealth()) m.setHealth(m.getMaxHealth());
        if (m.getTarget() != null) m.setTarget(null);
        if (!m.isNoAi()) m.setNoAi(true);
        for (ServerPlayer p : MonarchAttacks.hallFoes(l, f)) {                  // only someone in the hall wakes it (not a cave under the floor), and never a creative one
            if (MonarchRules.wakes(p.getX() - a.x, p.getZ() - a.z, p.getY() - a.y)) {
                rouse(l, m, f, p);
                return;
            }
        }
    }

    /** The roar and the title; the server is told, once for this Monarch. */
    static void rouse(ServerLevel l, Mob m, MonarchFight f, ServerPlayer cause) {
        if (!MonarchBoss.wake(m)) return;
        long now = l.getGameTime();
        f.awake = true;
        f.phase = MonarchRules.phaseOf(m.getHealth(), m.getMaxHealth());
        f.scheduler.reset(now, 80);
        f.lastFoe = now;
        f.lastHit.clear();
        stand(m, f, now + 30);
        MonarchAttacks.sound(l, m.getX(), m.getY(), m.getZ(), SoundEvents.WARDEN_ROAR, 6.0f, 0.6f);
        MonarchAttacks.sound(l, m.getX(), m.getY(), m.getZ(), SoundEvents.ENDER_DRAGON_GROWL, 5.0f, 0.5f);
        VfxSpawn.sendFollowing(l, VfxShape.KOTO_AURA, m, m.position(), 0xFFB026FF, 90, 3.0f);
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.getX(), m.getY() + 1.0, m.getZ(), 1, 0, 0, 0, 0);
        Vec3 c = f.centreVec();
        for (ServerPlayer p : l.players()) {
            if (p.isSpectator() || !MonarchShapes.inside(p.getX(), p.getZ(), c.x, c.z, MonarchRules.BAR_RANGE)) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(MonarchText.title()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(MonarchText.subtitle()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC)));
        }
        if (!m.getPersistentData().getBoolean(MonarchBoss.WOKEN)) {
            m.getPersistentData().putBoolean(MonarchBoss.WOKEN, true);
            DungeonMaw.broadcast(l.getServer(), Component.literal(MonarchText.woken(cause == null ? null : cause.getName().getString())).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        }
    }

    /** Nobody is left to fight: the soldiers go, the Monarch heals and goes back to its throne and its sleep. */
    private static void restSoldiersAndSleep(ServerLevel l, Mob m, MonarchFight f) {
        f.hazards.clear();
        clearEffects(l, f);
        dismissSoldiers(l, f);
        f.bar.removeAllPlayers();
        f.awake = false;
        f.phase = 1;
        f.frozenUntil = 0;
        f.transitionUntil = 0;
        f.lastHit.clear();
        m.removeAllEffects();
        MonarchBoss.sleep(m, f.centre);
    }

    private static void dismissSoldiers(ServerLevel l, MonarchFight f) {
        for (UUID id : new ArrayList<>(f.soldiers)) {
            Entity s = l.getEntity(id);
            if (s != null) poof(l, s);
        }
        f.soldiers.clear();
    }

    /** Takes the slowness of the Authority off the players it was laid on (what is left of it, a second or two). */
    private static void clearEffects(ServerLevel l, MonarchFight f) {
        for (UUID id : f.slowed) {
            ServerPlayer p = l.getServer().getPlayerList().getPlayer(id);
            if (p == null) continue;
            MobEffectInstance e = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (e != null && e.getDuration() <= MonarchRules.AUTHORITY_REFRESH + 30) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        f.slowed.clear();
    }

    private static void poof(ServerLevel l, Entity e) {
        l.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 0.6, e.getZ(), 18, 0.4, 0.6, 0.4, 0.07);
        SOLDIERS.remove(e.getUUID());
        e.discard();
    }

    // ---------------------------------------------------------------- the phases
    /** The roar: the Monarch stands still and cannot be hurt for a moment, then pushes everyone back and brings more soldiers. */
    private static void transition(ServerLevel l, Mob m, MonarchFight f, int toPhase, long now) {
        f.phase = toPhase;
        f.hazards.clear();
        clearEffects(l, f);
        f.transitionUntil = now + MonarchRules.TRANSITION;
        stand(m, f, f.transitionUntil);
        f.scheduler.holdUntil(f.transitionUntil + MonarchRules.gap(toPhase));
        f.hazards.add(MonarchAttacks.roar(l, m, f, toPhase, now));
    }

    // ---------------------------------------------------------------- a move
    private static void start(ServerLevel l, Mob m, MonarchFight f, MonarchMove move, ServerPlayer target, List<ServerPlayer> foes, long now) {
        f.scheduler.started(move, now, f.phase);
        face(m, target);
        stand(m, f, now + MonarchRules.freeze(move, f.phase));
        MonarchAttacks.warn(l, m, MonarchText.warning(move));
        f.hazards.add(switch (move) {
            case SLAM -> MonarchAttacks.slam(l, m, f, now);
            case RAIN -> MonarchAttacks.rain(l, m, f, foes, now);
            case SUMMON -> MonarchAttacks.summon(l, m, f, now);
            case BEAM -> MonarchAttacks.beam(l, m, f, target, now);
            case AUTHORITY -> MonarchAttacks.authority(l, m, f, now);
        });
    }

    /** Stands it still (brain off) until the given tick. */
    private static void stand(Mob m, MonarchFight f, long until) {
        f.frozenUntil = Math.max(f.frozenUntil, until);
        m.setNoAi(true);
        m.getNavigation().stop();
        m.setDeltaMovement(m.getDeltaMovement().multiply(0, 1, 0));
    }

    private static void face(Mob m, LivingEntity to) {
        float yaw = (float) (Mth.atan2(to.getZ() - m.getZ(), to.getX() - m.getX()) * 57.29577951308232) - 90.0f;
        m.setYRot(yaw);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
    }

    // ---------------------------------------------------------------- between moves
    /** Tells its brain who it is after, walks to them, and strikes if its own brain has not (it has a reach and a cooldown like any mob). */
    private static void pursue(ServerLevel l, Mob m, MonarchFight f, ServerPlayer target, long now) {
        aim(m, target);
        m.getLookControl().setLookAt(target, 40f, 40f);
        double d = m.distanceTo(target);
        if (d > 3.2 && now % 10 == 0) m.getNavigation().moveTo(target, 1.2);
        if (f.cannotStrike || now < f.nextMelee || !m.getBoundingBox().inflate(1.1, 0.7, 1.1).intersects(target.getBoundingBox())) return;
        f.nextMelee = now + 30;
        if (target.getLastHurtByMob() == m && target.tickCount - target.getLastHurtByMobTimestamp() < 30) return;       // its own brain just did
        m.swing(InteractionHand.MAIN_HAND);
        try {
            m.doHurtTarget(target);
        } catch (RuntimeException | LinkageError ex) {
            f.cannotStrike = true;
            warn("striking with the Monarch", ex);
        }
    }

    /** Tells a mob's own brain (when it has the memory) and the plain target slot who it is after; a brain of another shape ignores it. */
    static void aim(Mob m, LivingEntity prey) {
        try {
            m.setTarget(prey);
            m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, prey);
        } catch (RuntimeException | LinkageError ignored) {
            // the walk and the strike do not depend on it
        }
    }

    private static void bar(ServerLevel l, Mob m, MonarchFight f) {
        f.bar.setProgress(Mth.clamp(m.getHealth() / m.getMaxHealth(), 0f, 1f));
        f.bar.setName(Component.literal(MonarchText.bar(f.phase, MonarchAttacks.alive(l, f))));
        for (ServerPlayer p : new ArrayList<>(f.bar.getPlayers())) if (p.isRemoved() || p.serverLevel() != l) f.bar.removePlayer(p);          // logged out, or gone to another dimension
        Vec3 c = f.centreVec();
        for (ServerPlayer p : l.players()) {
            boolean near = !p.isSpectator() && MonarchShapes.inside(p.getX(), p.getZ(), c.x, c.z, MonarchRules.BAR_RANGE);
            boolean shown = f.bar.getPlayers().contains(p);
            if (near && !shown) f.bar.addPlayer(p);
            else if (!near && shown) f.bar.removePlayer(p);
        }
    }

    // ---------------------------------------------------------------- the soldiers
    /** Looks after every shadow soldier: it serves a Monarch that is awake, lives a while, aims at the nearest foe, and is kept out of the food web. */
    private static void soldiers(ServerLevel l, long now) {
        List<ServerPlayer> foes = MonarchAttacks.foes(l);
        Iterator<Map.Entry<UUID, Long>> it = SOLDIERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Entity en = l.getEntity(entry.getKey());
            if (!(en instanceof Mob s) || !s.isAlive() || s.isRemoved()) {
                it.remove();
                continue;
            }
            UUID owner = MonarchBoss.ownerOf(s);
            MonarchFight f = owner == null ? null : FIGHTS.get(owner);
            boolean tooOld = now - s.getPersistentData().getLong(MonarchBoss.BORN) > MonarchRules.SOLDIER_LIFE;
            boolean orphan = f == null ? now - entry.getValue() > ORPHAN_AFTER : !f.awake;
            if (tooOld || orphan) {
                it.remove();
                l.sendParticles(ParticleTypes.SQUID_INK, s.getX(), s.getY() + 0.6, s.getZ(), 18, 0.4, 0.6, 0.4, 0.07);
                s.discard();
                continue;
            }
            if (f != null && !f.soldiers.contains(s.getUUID())) f.soldiers.add(s.getUUID());
            MonarchBoss.pinHunger(s);
            LivingEntity t = s.getTarget();
            if (t == null || !t.isAlive()) {
                ServerPlayer p = MonarchAttacks.nearest(foes, s);
                if (p != null) aim(s, p);
            }
            l.sendParticles(ParticleTypes.SOUL, s.getX(), s.getY() + 1.0, s.getZ(), 1, 0.3, 0.5, 0.3, 0.01);
        }
    }

    // ---------------------------------------------------------------- the end
    /**
     * The Monarch falls: once (a mark on the entity), its bar and soldiers go, there is experience and a word to the server, and the Nexus collapses
     * unless it already has (a relic was taken first, or the cell's record says it fell): the event is posted exactly once per hall. Each part is
     * guarded, so a failure in one cannot cost the others or bring the server down from the middle of a death.
     */
    private static void fell(ServerLevel l, Mob m, LivingDeathEvent e) {
        if (m.getPersistentData().getBoolean(MonarchBoss.FELL)) return;
        m.getPersistentData().putBoolean(MonarchBoss.FELL, true);
        MonarchFight f = FIGHTS.remove(m.getUUID());
        BlockPos centre = MonarchBoss.centreOf(m);
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        if (killer == null && f != null && f.lastAttacker != null) killer = l.getServer().getPlayerList().getPlayer(f.lastAttacker);
        boolean already = COLLAPSING.contains(cellKey(centre)) || collapsed(l, centre);
        try {
            if (f != null) {
                f.hazards.clear();
                clearEffects(l, f);
                dismissSoldiers(l, f);
                f.bar.removeAllPlayers();
            }
            MonarchAttacks.sound(l, m.getX(), m.getY(), m.getZ(), SoundEvents.WARDEN_ROAR, 6.0f, 0.4f);
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.getX(), m.getY() + 1.0, m.getZ(), 2, 1.0, 1.0, 1.0, 0.0);
            ExperienceOrb.award(l, m.position(), MonarchRules.XP);
            String who = killer == null ? null : killer.getName().getString();
            DungeonMaw.broadcast(l.getServer(), Component.literal(already ? MonarchText.fallenDuringCollapse(who) : MonarchText.fallen(who)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        } catch (RuntimeException | LinkageError ex) {
            warn("seeing the Monarch fall", ex);
        }
        if (already) return;
        COLLAPSING.add(cellKey(centre));
        try {
            NeoForge.EVENT_BUS.post(new DungeonCollapseEvent(l, centre, NexusZone.MONARCH_VAULT, killer, true));
        } catch (RuntimeException | LinkageError ex) {
            warn("announcing the collapse of the Monarch's Vault", ex);
        }
    }

    private static void warn(String what, Throwable ex) {
        if (WARNED.add(what + ex.getClass().getName())) LOG.warn("[nusmp] Trouble {} (this is logged once)", what, ex);
    }
}
