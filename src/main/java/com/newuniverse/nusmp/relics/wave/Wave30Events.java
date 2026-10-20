package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: all of these listeners react to the same events; one class registers them once and keeps the per-world tables together so they can be cleared
 * in one place.
 * <p>
 * Wave 30: the game-bus handlers of the special wave. One damage listener (Vecna's cold hands, the Paradox's extra, the surprise guard of the pair and
 * of Foresight, the fairness cap on the Deck's and Hunger's summons, the last blow recorded for the Gauntlet's reroll), one death listener (the Soul
 * Stone's stored souls and its resurrection, the Comet, the end of a graft), a player tick (once a second, staggered: X-ray, the pair's gifts, wards,
 * the Gauntlet's gifts, the stars, grafting, the Paradox's path), a server tick (the timed tables: mad, dominated, shrunk and confused mobs, hostile
 * summons, door portals) and the arcane locks. Every table is bounded and cleared when the server stops or the player leaves.
 */
final class Wave30Events {
    private Wave30Events() {}

    // ================================================================ the tables (server memory only; every timer is in game time)
    private record Thrall(UUID owner, long until) {}

    private record Fog(UUID owner, long until) {}

    private record Portal(UUID owner, String dim, BlockPos a, BlockPos b, long until) {}

    private record Spot(double x, double y, double z, String dim) {}

    private static final Map<UUID, Long> MAD = new HashMap<>(), SHRUNK = new HashMap<>();
    private static final Map<UUID, Thrall> THRALLS = new HashMap<>();
    private static final Map<UUID, Fog> FOG = new HashMap<>();
    private static final Set<UUID> FOES = new HashSet<>();
    private static final List<Portal> PORTALS = new ArrayList<>();
    private static final Map<UUID, Long> PORTAL_CD = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Spot>> PATHS = new HashMap<>();
    private static final int TABLE_CAP = 256;

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave30Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onDrops);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onExperience);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onJoin);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave30Events::onStopping);
    }

    // ================================================================ entering the tables (called by the acts)
    static void trackFoe(Mob m) {
        if (FOES.size() >= TABLE_CAP) FOES.clear();
        FOES.add(m.getUUID());
    }

    static void madden(Mob m, long until) {
        if (MAD.size() >= TABLE_CAP) MAD.clear();
        MAD.put(m.getUUID(), until);
    }

    /** Dominate: the mob is the owner's ally until the time is up (it is marked as a relic ally, so it never targets the owner and the owner's area attacks skip it). */
    static void dominate(Mob m, ServerPlayer owner, long until) {
        if (THRALLS.size() >= TABLE_CAP) THRALLS.clear();
        m.getPersistentData().putUUID(Wave30Kit.ALLY_OWNER, owner.getUUID());
        m.setTarget(null);
        THRALLS.put(m.getUUID(), new Thrall(owner.getUUID(), until));
    }

    static void shrink(Mob m, long until) {
        if (SHRUNK.size() >= TABLE_CAP) SHRUNK.clear();
        double d = Wave30Tune.POLY_SCALE - 1.0;
        Wave30Kit.setMod(m, Wave30Tune.AT_HEIGHT, "poly_h", d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        Wave30Kit.setMod(m, Wave30Tune.AT_WIDTH, "poly_w", d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        SHRUNK.put(m.getUUID(), until);
    }

    static void confuse(Mob m, ServerPlayer owner, long until) {
        if (FOG.size() >= TABLE_CAP) FOG.clear();
        m.setTarget(null);
        FOG.put(m.getUUID(), new Fog(owner.getUUID(), until));
    }

    static void openPortal(ServerPlayer owner, String dim, BlockPos a, BlockPos b, long until) {
        PORTALS.removeIf(q -> q.dim().equals(dim) && (q.a().equals(a) || q.b().equals(a) || q.a().equals(b) || q.b().equals(b)));
        int mine = 0;
        for (Portal q : PORTALS) if (q.owner().equals(owner.getUUID())) mine++;
        for (Iterator<Portal> it = PORTALS.iterator(); mine >= Wave30Tune.KEY_PORTAL_MAX && it.hasNext(); ) {
            if (it.next().owner().equals(owner.getUUID())) {
                it.remove();
                mine--;
            }
        }
        PORTALS.add(new Portal(owner.getUUID(), dim, a, b, until));
    }

    /** Where the wielder stood about ten seconds ago in this dimension, or null. */
    static double[] pathBack(ServerPlayer p) {
        ArrayDeque<Spot> q = PATHS.get(p.getUUID());
        if (q == null || q.isEmpty()) return null;
        String dim = p.serverLevel().dimension().location().toString();
        Spot s = q.peekFirst();
        return s != null && s.dim().equals(dim) ? new double[]{s.x(), s.y(), s.z()} : null;
    }

    // ================================================================ damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        long now = victim.level().getGameTime();

        // ---- the attacker's side
        if (src.getEntity() instanceof ServerPlayer a && a != victim && src.getDirectEntity() == a && !src.is(DamageTypeTags.IS_PROJECTILE)) {
            if (RelicFlags.has(a, Wave30Tune.F_HAND)) {
                e.setAmount(e.getAmount() + (victim instanceof Player ? Wave30Tune.HAND_COLD_DAMAGE * 0.5f : Wave30Tune.HAND_COLD_DAMAGE));
                Fx.addEffect(victim, Wave30Tune.EF_FROST, Wave30Tune.HAND_FROST_TICKS, 0);
                Fx.addEffect(victim, "minecraft:slowness", 60, 0);
                victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 40, victim.getTicksFrozen() + 60));
            }
            if (RelicFlags.has(a, Wave30Tune.F_PARADOX)) {
                float extra = Wave30Rules.paradoxExtra(a.getRandom().nextInt(4) + 1, a.getRandom().nextInt(4) + 1) + (float) Wave30Tune.PARADOX_BONUS_FLAT;
                float amount = e.getAmount() + (victim instanceof Player ? extra * 0.5f : extra);
                if (Wave30Rules.paradoxCrit(a.getRandom().nextInt(20) + 1)) amount *= 1.5f;
                e.setAmount(amount);
            }
        }

        // ---- the victim's side
        if (!(victim instanceof ServerPlayer v)) return;
        boolean attacked = src.getEntity() != null && src.getEntity() != v;
        if (attacked && (Wave30Data.getLong(v, Wave30Data.FORESIGHT_UNTIL) > now || (RelicFlags.has(v, Wave30Tune.F_EYE) && Wave30Vecna.set(v)))) {
            if (Wave30Rules.surpriseReady(now, Wave30Data.getLong(v, Wave30Data.SURPRISE_READY))) {
                Wave30Data.putLong(v, Wave30Data.SURPRISE_READY, now + Wave30Tune.SURPRISE_COOLDOWN);
                e.setCanceled(true);
                Wave30Kit.hint(v, "You felt it coming: the first blow is turned aside.");
                com.newuniverse.nusmp.particle.fx.Fx.burst(v.serverLevel(), FxThemes.RELIC_WAVE_30, v, FxTone.LIGHT, 6);
                return;
            }
        }
        if (src.getEntity() instanceof LivingEntity att && att.getPersistentData().hasUUID(Wave30Kit.FOE_OWNER)) {
            float capped = Wave30Rules.summonHit(e.getAmount(), v.getMaxHealth(), v.getHealth() + v.getAbsorptionAmount());
            if (capped <= 0f) {
                e.setCanceled(true);
                return;
            }
            e.setAmount(capped);
        }
        if (RelicFlags.has(v, Wave30Tune.F_GAUNTLET)) {
            Wave30Data.putInt(v, Wave30Data.LAST_HIT, Float.floatToIntBits(e.getAmount()));
            Wave30Data.putLong(v, Wave30Data.LAST_HIT_AT, now);
        }
    }

    // ================================================================ death
    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide() || !(dead.level() instanceof ServerLevel level)) return;
        DamageSource src = e.getSource();
        long now = level.getGameTime();

        // the Soul Stone: True Resurrection, once a day, from a stored soul
        if (dead instanceof ServerPlayer v && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && Wave30Stones.soulBearer(v)) {
            long today = UseRules.day(now);
            int souls = Wave30Data.getInt(v, Wave30Data.SOULS);
            if (Wave30Rules.soulReviveOk(souls, Wave30Data.getLong(v, Wave30Data.SOUL_DAY), today + 1)) {
                e.setCanceled(true);
                v.setHealth(Wave30Rules.revivedHealth(v.getMaxHealth()));
                Wave30Kit.cleanse(v);
                Wave30Data.putInt(v, Wave30Data.SOULS, souls - 1);
                Wave30Data.putLong(v, Wave30Data.SOUL_DAY, today + 1);
                Fx.addEffect(v, "minecraft:regeneration", Wave30Tune.SOUL_BLESS_TICKS, 2);
                Fx.addEffect(v, "minecraft:resistance", Wave30Tune.SOUL_BLESS_TICKS, 1);
                Fx.addEffect(v, "minecraft:absorption", Wave30Tune.SOUL_BLESS_TICKS, 1);
                v.invulnerableTime = 40;
                com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_30, v, FxTone.LIGHT, 12);
                Wave30Kit.shout(v, "TRUE RESURRECTION: a soul in the Stone is spent, and you stand again.", net.minecraft.ChatFormatting.GOLD);
                return;
            }
        }

        // the dead's soul goes to every Soul Stone bearer within sixty-four blocks
        double r = Wave30Tune.SOUL_RADIUS;
        for (ServerPlayer p : level.players()) {
            if (p == dead || p.isSpectator() || p.distanceToSqr(dead) > r * r || !Wave30Stones.soulBearer(p)) continue;
            Wave30Data.putInt(p, Wave30Data.SOULS, Math.min(Wave30Tune.SOUL_MAX, Wave30Data.getInt(p, Wave30Data.SOULS) + 1));
            if (dead instanceof Player) {
                String names = Wave30Data.getString(p, Wave30Data.SOUL_NAMES);
                String add = dead.getName().getString();
                names = names.isEmpty() ? add : names + ", " + add;
                if (names.length() > 120) names = names.substring(names.length() - 120);
                Wave30Data.putString(p, Wave30Data.SOUL_NAMES, names);
            }
            Wave30Kit.hint(p, "The Soul Stone drinks a soul.");
        }

        // the Comet: the next hostile monster slain alone in the day
        if (src.getEntity() instanceof ServerPlayer k && dead instanceof Enemy && Wave30Data.getLong(k, Wave30Data.COMET_UNTIL) > now) {
            boolean alone = true;
            for (ServerPlayer o : level.players()) if (o != k && !o.isSpectator() && o.distanceToSqr(k) < 24 * 24) alone = false;
            if (alone) {
                Wave30Data.remove(k, Wave30Data.COMET_UNTIL);
                k.giveExperienceLevels(Wave30Tune.COMET_LEVELS);
                Wave30Kit.say(k, "The comet's mark burns away: " + Wave30Tune.COMET_LEVELS + " levels.");
            }
        }

        if (dead instanceof ServerPlayer d) Wave30Bond.onDeath(d);
        if (dead instanceof Mob m) {
            UUID id = m.getUUID();
            FOES.remove(id);
            MAD.remove(id);
            THRALLS.remove(id);
            SHRUNK.remove(id);
            FOG.remove(id);
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        if (!e.getEntity().level().isClientSide() && e.getEntity().getPersistentData().hasUUID(Wave30Kit.FOE_OWNER)) e.setCanceled(true);
    }

    private static void onExperience(LivingExperienceDropEvent e) {
        if (!e.getEntity().level().isClientSide() && e.getEntity().getPersistentData().hasUUID(Wave30Kit.FOE_OWNER)) e.setDroppedExperience(0);
    }

    /** A hostile summon that comes back from disk is not tracked any more: it goes, so none stays for ever. */
    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.loadedFromDisk()) return;
        if (e.getEntity().getPersistentData().hasUUID(Wave30Kit.FOE_OWNER)) e.setCanceled(true);
    }

    // ================================================================ targets
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity target = e.getNewAboutToBeSetTarget();
        if (target == null || e.getEntity().level().isClientSide()) return;
        Entity self = e.getEntity();
        UUID id = self.getUUID();
        if (self.getPersistentData().hasUUID(Wave30Kit.FOE_OWNER)) {                                  // a hostile summon of the deck or of the Hunger hunts its owner alone
            if (!target.getUUID().equals(self.getPersistentData().getUUID(Wave30Kit.FOE_OWNER))) e.setCanceled(true);
            return;
        }
        Fog fog = FOG.get(id);
        if (fog != null && target instanceof Player) e.setCanceled(true);                              // a mirage: the player is no longer there
        Thrall th = THRALLS.get(id);
        if (th != null && (target.getUUID().equals(th.owner()) || target.getPersistentData().hasUUID(Wave30Kit.ALLY_OWNER) && target.getPersistentData().getUUID(Wave30Kit.ALLY_OWNER).equals(th.owner()))) e.setCanceled(true);
    }

    // ================================================================ the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase != 0) return;
        long now = p.level().getGameTime();
        boolean eye = RelicFlags.has(p, Wave30Tune.F_EYE), hand = RelicFlags.has(p, Wave30Tune.F_HAND);
        if (eye || hand) Wave30Bond.tick(p);
        boolean pair = Wave30Rules.vecnaSet(eye, hand);
        if (eye) Wave30Vecna.xray(p, pair);
        if (pair) Wave30Vecna.pairTick(p);
        if (Wave30Data.getLong(p, Wave30Data.WARD_UNTIL) > now) Wave30Kit.cleanse(p);                  // the Greater Wish's immunity
        int sec = p.tickCount / 20;
        if (sec % 2 == 0) {
            if (RelicFlags.has(p, Wave30Tune.F_GAUNTLET)) Wave30Stones.gauntletTick(p);
            else Wave30Stones.setPower(p, false);
        }
        if (sec % 5 == 0 && Wave30Data.getInt(p, Wave30Data.STARS) > 0) Wave30Deck.applyStars(p);
        if (sec % 2 == 0 && RelicFlags.has(p, Wave30Tune.F_PARADOX)) {                                  // the path the Paradox throws a failed wielder back along
            ArrayDeque<Spot> q = PATHS.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            q.addLast(new Spot(p.getX(), p.getY(), p.getZ(), p.serverLevel().dimension().location().toString()));
            while (q.size() > Wave30Tune.PATH_SAMPLES) q.pollFirst();
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        PATHS.remove(id);
        PORTAL_CD.remove(id);
    }

    // ================================================================ the server tick
    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 10 == 3) tens(server);
        if (t % 20 == 7 && !FOES.isEmpty()) foes(server);
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /** Every half second: the mad, the dominated, the shrunk, the confused, the portals. */
    private static void tens(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (!MAD.isEmpty()) {
            for (Iterator<Map.Entry<UUID, Long>> it = MAD.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Long> en = it.next();
                Entity ent = find(server, en.getKey());
                if (!(ent instanceof Mob m) || !m.isAlive() || now >= en.getValue()) {
                    if (ent instanceof Mob m2) m2.setTarget(null);
                    it.remove();
                    continue;
                }
                LivingEntity near = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity o : m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(Wave30Tune.CROWN_ALLY_RADIUS),
                        x -> x != m && x.isAlive() && !(x instanceof Player) && !(x instanceof net.minecraft.world.entity.decoration.ArmorStand))) {
                    double d = o.distanceToSqr(m);
                    if (d < best) {
                        best = d;
                        near = o;
                    }
                }
                m.setTarget(near);
            }
        }
        if (!THRALLS.isEmpty()) {
            for (Iterator<Map.Entry<UUID, Thrall>> it = THRALLS.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Thrall> en = it.next();
                Entity ent = find(server, en.getKey());
                ServerPlayer owner = server.getPlayerList().getPlayer(en.getValue().owner());
                if (!(ent instanceof Mob m) || !m.isAlive()) {
                    it.remove();
                    continue;
                }
                if (owner == null || !owner.isAlive() || now >= en.getValue().until()) {
                    m.getPersistentData().remove(Wave30Kit.ALLY_OWNER);
                    m.setTarget(null);
                    com.newuniverse.nusmp.particle.fx.Fx.burst((ServerLevel) m.level(), FxThemes.RELIC_WAVE_30, m, FxTone.DARK, 6);
                    it.remove();
                    continue;
                }
                LivingEntity cur = m.getTarget();
                if (cur == null || !cur.isAlive() || cur == owner) {
                    List<LivingEntity> foes = Fx.foesAround(owner, 16);
                    m.setTarget(foes.isEmpty() ? null : foes.get(0));
                }
            }
        }
        if (!SHRUNK.isEmpty()) {
            for (Iterator<Map.Entry<UUID, Long>> it = SHRUNK.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Long> en = it.next();
                Entity ent = find(server, en.getKey());
                if (!(ent instanceof Mob m) || !m.isAlive() || now >= en.getValue()) {
                    if (ent instanceof Mob m2) {
                        Wave30Kit.setMod(m2, Wave30Tune.AT_HEIGHT, "poly_h", 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
                        Wave30Kit.setMod(m2, Wave30Tune.AT_WIDTH, "poly_w", 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
                    }
                    it.remove();
                }
            }
        }
        if (!FOG.isEmpty()) {
            for (Iterator<Map.Entry<UUID, Fog>> it = FOG.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Fog> en = it.next();
                Entity ent = find(server, en.getKey());
                if (!(ent instanceof Mob m) || !m.isAlive() || now >= en.getValue().until()) {
                    it.remove();
                    continue;
                }
                if (m.getTarget() instanceof Player) m.setTarget(null);
            }
        }
        if (!PORTALS.isEmpty()) portals(server, now);
        if (!PORTAL_CD.isEmpty()) PORTAL_CD.values().removeIf(v -> v < now);
    }

    /** The door portals: dropped when either door is shut or gone or the hour is up; players who step into a doorway come out of the other. */
    private static void portals(MinecraftServer server, long now) {
        for (Iterator<Portal> it = PORTALS.iterator(); it.hasNext(); ) {
            Portal q = it.next();
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(q.dim())));
            if (level == null || now >= q.until() || !level.isLoaded(q.a()) || !level.isLoaded(q.b())) {
                if (level == null || now >= q.until()) it.remove();
                continue;
            }
            BlockState sa = level.getBlockState(q.a()), sb = level.getBlockState(q.b());
            if (!Wave30Key.isDoorLike(sa) || !Wave30Key.isDoorLike(sb) || !Wave30Key.isOpen(sa) || !Wave30Key.isOpen(sb)) {
                it.remove();
                continue;
            }
            Vec3 ca = Vec3.atBottomCenterOf(q.a()), cb = Vec3.atBottomCenterOf(q.b());
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_30, FxTone.LIGHT, ca.x, ca.y + 1.0, ca.z, 2, 0.3, 0.5, 0.3, 0.02);
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_30, FxTone.LIGHT, cb.x, cb.y + 1.0, cb.z, 2, 0.3, 0.5, 0.3, 0.02);
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || PORTAL_CD.containsKey(p.getUUID())) continue;
                Vec3 to = null;
                if (near(p, ca)) to = Wave30Key.arrival(q.b());
                else if (near(p, cb)) to = Wave30Key.arrival(q.a());
                if (to == null) continue;
                p.teleportTo(to.x, to.y, to.z);
                p.fallDistance = 0;
                PORTAL_CD.put(p.getUUID(), now + 60);
                Wave30Kit.hint(p, "You step through the door.");
            }
        }
    }

    private static boolean near(Player p, Vec3 doorBase) {
        double dx = p.getX() - doorBase.x, dz = p.getZ() - doorBase.z, dy = p.getY() - doorBase.y;
        return dx * dx + dz * dz < Wave30Tune.KEY_PORTAL_STEP * Wave30Tune.KEY_PORTAL_STEP * 0.5 && dy > -1.0 && dy < 2.0;
    }

    /** Every second: hostile summons that are over their time, or whose owner is gone, leave. */
    private static void foes(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Iterator<UUID> it = FOES.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity ent = find(server, id);
            if (!(ent instanceof Mob m) || !m.isAlive()) {
                it.remove();
                continue;
            }
            UUID owner = m.getPersistentData().hasUUID(Wave30Kit.FOE_OWNER) ? m.getPersistentData().getUUID(Wave30Kit.FOE_OWNER) : null;
            ServerPlayer op = owner == null ? null : server.getPlayerList().getPlayer(owner);
            if (op == null || !op.isAlive() || now >= m.getPersistentData().getLong(Wave30Kit.FOE_UNTIL)) {
                com.newuniverse.nusmp.particle.fx.Fx.burst((ServerLevel) m.level(), FxThemes.RELIC_WAVE_30, m, FxTone.DARK, 8);
                m.discard();
                it.remove();
            } else if (m.getTarget() != op) {
                m.setTarget(op);
            }
        }
    }

    // ================================================================ arcane locks
    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getLevel() instanceof ServerLevel level) || e.getEntity().isCreative()) return;
        BlockPos pos = Wave30Key.normalize(level, e.getPos());
        if (!Wave30Key.lockable(level, pos)) return;
        Wave30Locks locks = Wave30Locks.get(level.getServer());
        if (locks.size() == 0) return;
        UUID owner = locks.ownerAt(level.dimension().location().toString(), pos.asLong());
        if (owner == null || owner.equals(e.getEntity().getUUID())) return;
        e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer sp) Wave30Kit.hint(sp, "An arcane lock holds it shut.");
    }

    // ================================================================ end of the world
    private static void onStopping(ServerStoppingEvent e) {
        MinecraftServer server = e.getServer();
        for (Map.Entry<UUID, Thrall> en : THRALLS.entrySet()) {
            Entity ent = find(server, en.getKey());
            if (ent != null) ent.getPersistentData().remove(Wave30Kit.ALLY_OWNER);
        }
        for (UUID id : FOES) {
            Entity ent = find(server, id);
            if (ent != null) ent.discard();
        }
        for (Map.Entry<UUID, Long> en : SHRUNK.entrySet()) {
            Entity ent = find(server, en.getKey());
            if (ent instanceof Mob m) {
                Wave30Kit.setMod(m, Wave30Tune.AT_HEIGHT, "poly_h", 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
                Wave30Kit.setMod(m, Wave30Tune.AT_WIDTH, "poly_w", 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
            }
        }
        MAD.clear();
        SHRUNK.clear();
        THRALLS.clear();
        FOG.clear();
        FOES.clear();
        PORTALS.clear();
        PORTAL_CD.clear();
        PATHS.clear();
    }
}
