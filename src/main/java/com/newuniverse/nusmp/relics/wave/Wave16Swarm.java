package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 16: the creatures the charms keep. Snuggles (the Doll's cat), the Jar's bats and the three Weaverlings. Each owner has at most one of each
 * kind; every creature carries the core's owner and end-of-life keys (so it is nobody's foe and never turns on its owner), is gone when its owner
 * dies, leaves, changes world or stops wearing the charm, and the tables are emptied when the server stops. The sweeps run every ten or twenty ticks
 * and look at a handful of entities.
 */
final class Wave16Swarm {
    private Wave16Swarm() {}

    private static final Map<UUID, UUID> SNUGGLES = new HashMap<>();
    private static final Map<UUID, List<UUID>> BATS = new HashMap<>();
    private static final Map<UUID, Long> BAT_END = new HashMap<>();
    private static final Map<UUID, List<UUID>> WEAVERS = new HashMap<>();
    private static final Map<UUID, Long> WEAVER_NEXT = new HashMap<>();

    // ---------------------------------------------------------------- finding creatures
    private static Entity find(MinecraftServer server, UUID id) {
        if (server == null || id == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    private static void drop(MinecraftServer server, UUID id) {
        Entity e = find(server, id);
        if (e != null) e.discard();
    }

    // ================================================================ Snuggles
    /** Calls Snuggles, a tame cat made hardier (the Doll adds its bonus to hit points, armour and bite); the old one, if any, goes. */
    static LivingEntity snuggles(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        UUID old = SNUGGLES.remove(p.getUUID());
        if (old != null) drop(server, old);
        LivingEntity cat = Fx.summon(p, "minecraft:cat", Wave16Tune.SNUGGLES_TICKS);
        if (cat == null) return null;
        Wave16Kit.boost(cat, Attributes.MAX_HEALTH, "snuggles_hp", Wave16Tune.SNUGGLES_HEALTH);
        Wave16Kit.boost(cat, Attributes.ATTACK_DAMAGE, "snuggles_bite", Wave16Tune.SNUGGLES_ATTACK);
        Wave16Kit.boost(cat, Attributes.ARMOR, "snuggles_fur", Wave16Tune.SNUGGLES_ARMOR);
        cat.setHealth(cat.getMaxHealth());
        cat.setCustomName(net.minecraft.network.chat.Component.literal("Snuggles"));
        cat.setCustomNameVisible(true);
        cat.getPersistentData().putBoolean(Wave16Tune.ALLY, true);
        SNUGGLES.put(p.getUUID(), cat.getUUID());
        return cat;
    }

    /** Heals Snuggles fully; false if it is not here. */
    static boolean cuddle(ServerPlayer p) {
        Entity e = find(p.getServer(), SNUGGLES.get(p.getUUID()));
        if (!(e instanceof LivingEntity cat) || !cat.isAlive()) return false;
        cat.setHealth(cat.getMaxHealth());
        Fx.addEffect(cat, "minecraft:regeneration", 100, 1);
        Wave16Kit.puff((ServerLevel) cat.level(), cat.position().add(0, 0.6, 0), 0xB0C4D8, 16, 0.3);
        return true;
    }

    // ================================================================ Jar of Bats
    /** Opens the jar: six bats answer the holder for a minute. */
    static int release(ServerPlayer p) {
        recall(p);
        ServerLevel lv = p.serverLevel();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < Wave16Tune.BATS; i++) {
            Entity made = EntityType.BAT.create(lv);
            if (!(made instanceof Bat bat)) continue;
            double a = i * (Math.PI * 2 / Wave16Tune.BATS);
            bat.moveTo(p.getX() + Math.cos(a) * 1.5, p.getY() + 1.6, p.getZ() + Math.sin(a) * 1.5, p.getYRot(), 0f);
            bat.getPersistentData().putUUID(Wave16Tune.OWNER, p.getUUID());
            bat.getPersistentData().putLong(Wave16Tune.UNTIL, lv.getGameTime() + Wave16Tune.BATS_TICKS);
            if (lv.addFreshEntity(bat)) ids.add(bat.getUUID());
        }
        if (ids.isEmpty()) return 0;
        BATS.put(p.getUUID(), ids);
        BAT_END.put(p.getUUID(), lv.getGameTime() + Wave16Tune.BATS_TICKS);
        Wave16Kit.puff(lv, p.position().add(0, 1.6, 0), 0x7CFF4A, 30, 1.0);
        return ids.size();
    }

    /** The bats fly back into the jar. */
    static void recall(ServerPlayer p) {
        List<UUID> ids = BATS.remove(p.getUUID());
        BAT_END.remove(p.getUUID());
        if (ids != null) for (UUID id : ids) drop(p.getServer(), id);
    }

    private static void recallOffline(MinecraftServer server, UUID owner) {
        List<UUID> ids = BATS.remove(owner);
        BAT_END.remove(owner);
        if (ids != null) for (UUID id : ids) drop(server, id);
    }

    /** Every ten ticks: each swarm is kept near its owner, and once a second it falls on the nearest foe. */
    static void tickBats(MinecraftServer server) {
        if (BATS.isEmpty() || server.getTickCount() % 10 != 4) return;
        boolean bite = server.getTickCount() % 20 == 4;
        Iterator<Map.Entry<UUID, List<UUID>>> it = BATS.entrySet().iterator();
        List<UUID> done = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, List<UUID>> entry = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            long end = BAT_END.getOrDefault(entry.getKey(), 0L);
            if (owner == null || !owner.isAlive() || owner.level().getGameTime() >= end) { done.add(entry.getKey()); continue; }
            List<Bat> alive = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                Entity e = find(server, id);
                if (e instanceof Bat b && b.isAlive() && b.level() == owner.level()) alive.add(b);
            }
            if (alive.isEmpty()) { done.add(entry.getKey()); continue; }
            LivingEntity target = null;
            if (bite) {
                List<LivingEntity> foes = Fx.foesAround(owner, Wave16Tune.BATS_RANGE);
                if (!foes.isEmpty()) target = foes.get(0);
            }
            ServerLevel lv = owner.serverLevel();
            if (target != null) {
                int n = 0;
                for (Bat b : alive) {
                    double a = n++ * (Math.PI * 2 / Math.max(1, alive.size()));
                    b.teleportTo(target.getX() + Math.cos(a) * 0.9, target.getEyeY() + 0.2, target.getZ() + Math.sin(a) * 0.9);
                }
                Fx.damage(owner, target, Wave16Tune.BAT_BITE * alive.size(), "minecraft:mob_attack");
                Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
                com.newuniverse.nusmp.particle.fx.Fx.spray(lv, FxThemes.RELIC_WAVE_16, FxTone.DARK, at.x, at.y, at.z, 10, 0.4, 0.3, 0.4, 0.02);
            } else {
                for (Bat b : alive) if (b.distanceToSqr(owner) > 144) b.teleportTo(owner.getX(), owner.getY() + 1.6, owner.getZ());
            }
        }
        for (UUID owner : done) recallOffline(server, owner);
    }

    // ================================================================ Weaversong
    /** Once a second per player: the wearer of Weaversong keeps three Weaverlings at their side; anyone else keeps none. */
    static void tickWeavers(MinecraftServer server) {
        if (server.getTickCount() % 20 != 9) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean wears = p.isAlive() && !p.isSpectator() && Relics.worn(p, Wave16Tune.ID_WEAVER);
            if (wears) keepWeavers(p, false);
            else if (WEAVERS.containsKey(p.getUUID())) dismissWeavers(server, p.getUUID());
        }
        if (!WEAVERS.isEmpty()) {
            List<UUID> gone = new ArrayList<>();
            for (UUID owner : WEAVERS.keySet()) if (server.getPlayerList().getPlayer(owner) == null) gone.add(owner);
            for (UUID owner : gone) dismissWeavers(server, owner);
        }
    }

    /** Tops the Weaverlings up to three (one every two seconds, or all at once when called) and pulls strays back to the wearer's feet. */
    static int keepWeavers(ServerPlayer p, boolean all) {
        List<UUID> ids = WEAVERS.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        MinecraftServer server = p.getServer();
        List<LivingEntity> alive = new ArrayList<>();
        Iterator<UUID> it = ids.iterator();
        while (it.hasNext()) {
            Entity e = find(server, it.next());
            if (e instanceof LivingEntity l && l.isAlive() && l.level() == p.level()) alive.add(l);
            else { if (e != null) e.discard(); it.remove(); }
        }
        double leash = Wave16Tune.WEAVER_LEASH * Wave16Tune.WEAVER_LEASH;
        for (LivingEntity w : alive) if (all || w.distanceToSqr(p) > leash) w.teleportTo(p.getX(), p.getY(), p.getZ());
        long now = p.level().getGameTime();
        while (ids.size() < Wave16Tune.WEAVERLINGS && (all || now >= WEAVER_NEXT.getOrDefault(p.getUUID(), 0L))) {
            LivingEntity w = Fx.summon(p, "minecraft:cave_spider", Wave16Tune.WEAVER_LIFE);
            WEAVER_NEXT.put(p.getUUID(), now + Wave16Tune.WEAVER_RESPAWN);
            if (w == null) break;
            w.getPersistentData().putBoolean(Wave16Tune.ALLY, true);
            ids.add(w.getUUID());
            alive.add(w);
            if (!all) break;
        }
        if (all) for (LivingEntity w : alive) {
            Fx.addEffect(w, "minecraft:speed", 200, 1);
            Fx.addEffect(w, "minecraft:strength", 200, 0);
        }
        return ids.size();
    }

    private static void dismissWeavers(MinecraftServer server, UUID owner) {
        List<UUID> ids = WEAVERS.remove(owner);
        WEAVER_NEXT.remove(owner);
        if (ids != null) for (UUID id : ids) drop(server, id);
    }

    // ---------------------------------------------------------------- act bodies
    static void releaseAct(ActCtx c) {
        ServerPlayer p = c.player();
        int n = release(p);
        Fx.say(p, n == 0 ? "The jar opens, but no bat will come." : "The jar opens: " + n + " bats swarm out, and answer only to you.");
    }

    static void recallAct(ActCtx c) {
        ServerPlayer p = c.player();
        boolean had = BATS.containsKey(p.getUUID());
        recall(p);
        Fx.say(p, had ? "The bats fly back into the jar." : "No bats are out.");
    }

    static void weaverCall(ActCtx c) {
        ServerPlayer p = c.player();
        int n = keepWeavers(p, true);
        Fx.say(p, n + " Weaverlings gather at your feet, quick and strong for ten seconds.");
    }

    // ================================================================ lifetime
    /** The wearer left, died or changed world: their creatures go. */
    static void ownerGone(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        recallOffline(server, p.getUUID());
        dismissWeavers(server, p.getUUID());
        UUID cat = SNUGGLES.remove(p.getUUID());
        if (cat != null) drop(server, cat);
    }

    /** The server is stopping: every creature of the charms is sent away first (the tables are emptied right after, and the logouts that follow would find nothing to remove). */
    static void clear(MinecraftServer server) {
        if (server != null) {
            for (UUID id : SNUGGLES.values()) drop(server, id);
            for (List<UUID> ids : BATS.values()) for (UUID id : ids) drop(server, id);
            for (List<UUID> ids : WEAVERS.values()) for (UUID id : ids) drop(server, id);
        }
        clear();
    }

    static void clear() {
        SNUGGLES.clear();
        BATS.clear();
        BAT_END.clear();
        WEAVERS.clear();
        WEAVER_NEXT.clear();
    }
}
