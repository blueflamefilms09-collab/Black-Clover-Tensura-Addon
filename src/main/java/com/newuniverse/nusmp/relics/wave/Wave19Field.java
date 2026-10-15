package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Wave 19, what stays in the world after a scroll is read: the creatures charmed by the Scroll of Charm Parson and the Mug of The Boss (they leave
 * their charmer alone until the time is up or the charmer strikes them), and the Greater Crab of the fourth Bargain Bin Scroll (a telegraphed
 * spawn, a hostile-to-everyone crab, gone after twenty seconds). Everything is bounded; all of it is cleared when the server stops.
 */
final class Wave19Field {
    private Wave19Field() {}

    static final String CRAB_MARK = "nusmp_w19_crab";
    private static final int MAX_CHARMS = 96, MAX_PENDING = 8;

    // ================================================================ charms
    private record Charm(UUID owner, long until, boolean calm) {}

    private static final Map<UUID, Charm> CHARMS = new HashMap<>();

    /** The creature stops hunting this player (and, when {@code calm}, everyone) until the time is up or the player strikes it. */
    static void charm(Mob m, ServerPlayer owner, int ticks, boolean calm) {
        if (CHARMS.size() >= MAX_CHARMS && !CHARMS.containsKey(m.getUUID())) return;
        CHARMS.put(m.getUUID(), new Charm(owner.getUUID(), m.level().getGameTime() + ticks, calm));
        LivingEntity t = m.getTarget();
        if (t != null && (calm || t == owner)) m.setTarget(null);
        try {                                                                           // a vanilla brain has no ATTACK_TARGET memory registered: getMemory throws there
            m.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(x -> {
                if (calm || x == owner) m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            });
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }

    /** Should this creature leave this target alone right now? */
    static boolean spares(LivingEntity mob, LivingEntity target) {
        if (CHARMS.isEmpty()) return false;
        Charm c = CHARMS.get(mob.getUUID());
        if (c == null || mob.level().getGameTime() >= c.until()) return false;
        return c.calm() || target.getUUID().equals(c.owner());
    }

    /** A blow from this attacker frees a creature it had charmed. */
    static void struck(LivingEntity victim, Entity attacker) {
        if (CHARMS.isEmpty() || attacker == null) return;
        Charm c = CHARMS.get(victim.getUUID());
        if (c != null && c.owner().equals(attacker.getUUID())) CHARMS.remove(victim.getUUID());
    }

    // ================================================================ the Greater Crab
    private record Pending(ResourceKey<Level> dim, Vec3 at, long due, UUID owner) {}
    private record Alive(UUID owner, long until, ResourceKey<Level> dim) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, Alive> CRABS = new HashMap<>();

    /** How many crabs this caster has now (waiting to appear or alive). */
    static int crabsOf(ServerPlayer p) {
        int n = 0;
        for (Pending q : PENDING) if (q.owner().equals(p.getUUID())) n++;
        for (Alive a : CRABS.values()) if (a.owner().equals(p.getUUID())) n++;
        return n;
    }

    /** The scroll is read: a warning ring on the ground now, the crab a couple of seconds later. False if the caster has the most they may, or the list is full. */
    static boolean summonCrab(ServerPlayer p, Vec3 at) {
        if (PENDING.size() >= MAX_PENDING || crabsOf(p) >= Wave19Rules.CRAB_MAX) return false;
        PENDING.add(new Pending(p.level().dimension(), at, p.level().getGameTime() + Wave19Rules.CRAB_WARN, p.getUUID()));
        return true;
    }

    private static Mob spawnCrab(ServerLevel level, Vec3 at, Alive alive) {
        try {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("tensura", "armorsaurus"));
            if (type.isEmpty()) type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.withDefaultNamespace("ravager"));   // the nearest heavy bruiser in plain Minecraft
            if (type.isEmpty()) return null;
            Entity made = type.get().create(level);
            if (!(made instanceof Mob m)) return null;
            m.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            AttributeInstance scale = m.getAttribute(Attributes.SCALE);
            if (scale != null) scale.setBaseValue(Wave19Rules.CRAB_SCALE);
            AttributeInstance dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(Wave19Rules.CRAB_DAMAGE);
            m.setCustomName(Component.literal("Greater Crab"));
            m.setPersistenceRequired();
            m.getPersistentData().putBoolean(CRAB_MARK, true);
            CRABS.put(m.getUUID(), alive);                                              // tracked BEFORE it joins, or the join check below would throw it out as an orphan
            if (!level.addFreshEntity(m)) {
                CRABS.remove(m.getUUID());
                return null;
            }
            return m;
        } catch (RuntimeException | LinkageError failed) {
            return null;
        }
    }

    /** A crab that is not one this session is tracking (saved by a crash or a stop) goes the moment it joins the world. */
    static boolean orphan(Entity e) {
        return e.getPersistentData().getBoolean(CRAB_MARK) && !CRABS.containsKey(e.getUUID());
    }

    // ================================================================ the tick
    /** Server tick, every five ticks: crabs appear, are driven, and go; spent charms are dropped. */
    static void tick(MinecraftServer server) {
        if (CHARMS.isEmpty() && PENDING.isEmpty() && CRABS.isEmpty()) return;
        int tc = server.getTickCount();
        if (tc % 5 != 2) return;
        long now = server.overworld().getGameTime();
        if (tc % 100 == 2 && !CHARMS.isEmpty()) CHARMS.values().removeIf(c -> now >= c.until());
        stepPending(server, now);
        if (tc % 10 == 2) stepCrabs(server, now);
    }

    private static ServerLevel level(MinecraftServer server, ResourceKey<Level> dim) { return server.getLevel(dim); }

    private static void stepPending(MinecraftServer server, long now) {
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending q = it.next();
            ServerLevel sl = level(server, q.dim());
            if (sl == null) { it.remove(); continue; }
            if (now < q.due()) {                                                        // the telegraph: a red ring that tightens
                double left = (q.due() - now) / (double) Wave19Rules.CRAB_WARN;
                double r = 1.0 + 3.0 * left;
                for (int i = 0; i < 20; i++) {
                    double a = i * (Math.PI * 2 / 20);
                    RukhFx.spray(sl, Wave19Util.align(0xE04020), q.at().x + Math.cos(a) * r, q.at().y + 0.1, q.at().z + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
                }
                RukhFx.spray(sl, RukhFx.Alignment.LIGHT, q.at().x, q.at().y + 0.4, q.at().z, 6, 0.6, 0.3, 0.6, 0.02);
                continue;
            }
            it.remove();
            Mob crab = spawnCrab(sl, q.at(), new Alive(q.owner(), now + Wave19Rules.CRAB_TICKS, q.dim()));
            if (crab != null) {
                sl.playSound(null, crab.blockPosition(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.6f, 0.5f);
                RukhFx.spray(sl, RukhFx.Alignment.LIGHT, q.at().x, q.at().y + 0.5, q.at().z, 40, 1.2, 0.5, 1.2, 0.1);
            }
        }
    }

    private static void stepCrabs(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, Alive>> it = CRABS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Alive> en = it.next();
            ServerLevel sl = level(server, en.getValue().dim());
            Entity e = sl == null ? null : sl.getEntity(en.getKey());
            if (!(e instanceof Mob m) || !m.isAlive()) { it.remove(); continue; }
            if (now >= en.getValue().until()) {
                RukhFx.spray(sl, RukhFx.Alignment.LIGHT, m.getX(), m.getY() + 0.8, m.getZ(), 30, 0.8, 0.6, 0.8, 0.05);
                m.discard();
                it.remove();
                continue;
            }
            drive(m, now);
        }
    }

    /** The crab is hostile to everyone: the nearest living thing within sixteen blocks that is not creative, a spectator, a stand or another crab. */
    private static void drive(Mob m, long now) {
        try {
            LivingEntity target = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity l : m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(16.0), x ->
                    x != m && x.isAlive() && !x.isSpectator() && !(x instanceof ArmorStand) && !x.getPersistentData().getBoolean(CRAB_MARK)
                            && !(x instanceof Player pl && (pl.isCreative() || pl.isSpectator() || pl.getHealth() < pl.getMaxHealth() * 0.35f)))) {   // it never goes for a player already hurt badly
                double d = l.distanceToSqr(m);
                if (d < best) { best = d; target = l; }
            }
            m.setTarget(target);
            if (target == null) return;
            try {
                m.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            } catch (RuntimeException | LinkageError otherBrain) {
                // a vanilla brain: the target slot is enough
            }
            m.getNavigation().moveTo(target, 1.1);
            if (now - m.getPersistentData().getLong("nusmp_w19_strike") >= 20 && m.getBoundingBox().inflate(1.4, 0.8, 1.4).intersects(target.getBoundingBox())) {
                m.getPersistentData().putLong("nusmp_w19_strike", now);
                m.swing(InteractionHand.MAIN_HAND);
                m.doHurtTarget(target);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    // ================================================================ cleanup
    static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        PENDING.removeIf(q -> q.owner().equals(id));
        CHARMS.values().removeIf(c -> c.owner().equals(id));
    }

    /** The server is stopping: every crab goes (none may be saved), the lists are emptied. */
    static void clear(MinecraftServer server) {
        if (server != null) {
            for (Map.Entry<UUID, Alive> en : CRABS.entrySet()) {
                ServerLevel sl = server.getLevel(en.getValue().dim());
                Entity e = sl == null ? null : sl.getEntity(en.getKey());
                if (e != null) e.discard();
            }
        }
        CRABS.clear();
        PENDING.clear();
        CHARMS.clear();
    }
}
