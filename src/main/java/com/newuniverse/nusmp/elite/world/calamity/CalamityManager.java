package com.newuniverse.nusmp.elite.world.calamity;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.api.CalamityEvents;
import com.newuniverse.nusmp.elite.api.CalamityKillEvent;
import com.newuniverse.nusmp.elite.world.MagiculeApi;
import com.newuniverse.nusmp.elite.world.WorldConfig;
import com.newuniverse.nusmp.elite.world.WorldData;
import com.newuniverse.nusmp.elite.world.WorldGuard;
import com.newuniverse.nusmp.elite.world.WorldItems;
import com.newuniverse.nusmp.elite.world.block.MotherRockBlock;
import com.newuniverse.nusmp.elite.world.entity.CalamityBoss;
import com.newuniverse.nusmp.elite.world.item.CalamityCoreItem;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.CalamityDirector;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the world-event director of the calamity system, the Minecraft half of the pure {@link CalamityDirector}. It gathers what the director needs (the density where each player stands, the
 * Mother Rocks around, night and moon), applies its decision as an OMEN (a countdown that is announced, so nobody is surprised) and then the RISE of the boss at a place found near the player; it
 * also starts a calamity by ritual (a Calamity Core on a charged Mother Rock) and by command, keeps the list of living calamities and the cooldowns in a saved data file, announces every moment
 * through the public events of {@code elite/api} ({@link CalamityEvents}), reports damage and the kill to the records (those events are fired by the boss and here), and drops the Calamity Core
 * when one falls. It does nothing when the world engine or the calamity system is switched off, nothing in a protected dimension, and by default at most one calamity lives at a time.
 */
public final class CalamityManager {
    private CalamityManager() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final CalamityDirector DIRECTOR = new CalamityDirector();
    private static final Random RNG = new Random();
    private static final int[] COUNTDOWN = {10, 5, 3, 2, 1};

    public static void register(IEventBus gameBus) {
        gameBus.addListener(CalamityManager::onServerTick);
        gameBus.addListener((ServerStoppedEvent e) -> DIRECTOR.clear());
    }

    /** The director, for the self-test. */
    public static CalamityDirector director() { return DIRECTOR; }

    /** Fires an event on the game bus; a failing listener never reaches the caller. */
    public static void fire(Event e) {
        try {
            NeoForge.EVENT_BUS.post(e);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] elite world: a listener of {} failed: {}", e.getClass().getSimpleName(), ex.toString());
        }
    }

    // ================================================================ the tick

    private static void onServerTick(ServerTickEvent.Post e) {
        if (!WorldConfig.calamities()) return;
        MinecraftServer s = e.getServer();
        int t = s.getTickCount();
        CalamityData d = CalamityData.get(s);
        long now = s.overworld().getGameTime();
        if (!d.omens.isEmpty() && t % 5 == 0) tickOmens(s, d, now);
        if (!d.active.isEmpty() && t % 100 == 7) verifyActive(s, d);
        if (WorldConfig.calamityAuto() && t % WorldConfig.calamityCheckTicks() == 0) directorPass(s, d, now);
    }

    /** Runs the omen countdown now (the self-test; the server tick does the same every 5 ticks). */
    public static void tickNow(MinecraftServer s) { tickOmens(s, CalamityData.get(s), s.overworld().getGameTime()); }

    private static boolean busy(CalamityData d) { return d.active.size() + d.omens.size() >= WorldConfig.calamityMaxActive(); }

    /** One pass of the director over all players. Returns the id of the calamity it announced, or null. Public for the self-test. */
    public static String directorPass(MinecraftServer s, CalamityData d, long now) {
        List<CalamityDef> defs = WorldData.calamities();
        if (defs.isEmpty()) return null;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            ServerLevel level = p.serverLevel();
            if (p.isSpectator() || !WorldGuard.allowed(level)) continue;
            long day = level.getDayTime() % 24000L;
            int rocks = 0;
            ChunkPos cp = p.chunkPosition();
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) rocks += MagiculeApi.mothersAt(level, new BlockPos((cp.x + dx) * 16 + 8, level.getSeaLevel(), (cp.z + dz) * 16 + 8));
            CalamityDirector.Observation o = new CalamityDirector.Observation(p.getUUID(), level.dimension().location().toString(), MagiculeApi.effectiveAt(p), rocks,
                    day >= 13000L && day <= 23000L, level.getMoonPhase(), now);
            Optional<CalamityDirector.Decision> dec = DIRECTOR.check(defs, o, WorldConfig.calamityCheckTicks(), busy(d), d::woke, RNG);
            if (dec.isPresent()) {
                Component err = start(level, p, dec.get().calamityId(), dec.get().reason().name().toLowerCase(java.util.Locale.ROOT), -1);
                if (err == null) {
                    DIRECTOR.reset(p.getUUID());
                    return dec.get().calamityId();
                }
            }
        }
        return null;
    }

    // ================================================================ starting a calamity

    /**
     * Announces a calamity near a player: the OMEN. The boss rises {@code warnSeconds} later (the data file's number when {@code warnSeconds < 0}). Returns null on success, or the reason as a
     * message. Used by the director, the ritual and the command.
     */
    public static Component start(ServerLevel level, ServerPlayer target, String id, String reason, int warnSeconds) {
        MinecraftServer s = level.getServer();
        CalamityDef def = WorldData.calamity(id);
        if (def == null) return Component.translatable("nusmp.elite.calamity.unknown", id);
        if (!def.enabled()) return Component.translatable("nusmp.elite.calamity.disabled", def.name());
        if (!WorldConfig.calamities() || !WorldGuard.allowed(level)) return Component.translatable("nusmp.elite.calamity.here_no");
        CalamityData d = CalamityData.get(s);
        if (busy(d)) return Component.translatable("nusmp.elite.calamity.busy");
        Vec3 at = choosePos(level, target);
        if (at == null) return Component.translatable("nusmp.elite.calamity.no_spot");
        startAt(level, at, def, reason, target == null ? null : target.getUUID(), warnSeconds);
        return null;
    }

    /** The unchecked core of {@link #start}: an omen at a given place. */
    public static void startAt(ServerLevel level, Vec3 at, CalamityDef def, String reason, UUID target, int warnSeconds) {
        CalamityData d = CalamityData.get(level.getServer());
        int warn = warnSeconds >= 0 ? warnSeconds : def.warnSeconds();
        long now = level.getServer().overworld().getGameTime();
        CalamityData.Omen o = new CalamityData.Omen(def.id(), level.dimension().location().toString(), reason, at.x, at.y, at.z, now + warn * 20L, target);
        d.omens.add(o);
        d.setDirty();
        announce(level, def, "omen", at);
        fire(new CalamityEvents.Omen(def.id(), def.name(), level, at, def.announce().omen(), warn, reason));
    }

    /** A place 28 to 44 blocks (config) from the player, on the surface of a loaded chunk, dry. Null when none was found. */
    public static Vec3 choosePos(ServerLevel level, LivingEntity near) {
        if (near == null) return null;
        int min = WorldConfig.calamityMinDistance(), max = WorldConfig.calamityMaxDistance();
        for (int i = 0; i < 16; i++) {
            double a = RNG.nextDouble() * Math.PI * 2, dist = min + RNG.nextDouble() * (max - min);
            int x = Mth.floor(near.getX() + Math.cos(a) * dist), z = Mth.floor(near.getZ() + Math.sin(a) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinBuildHeight() + 1) continue;
            if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) continue;
            return new Vec3(x + 0.5, y, z + 0.5);
        }
        return null;
    }

    private static void tickOmens(MinecraftServer s, CalamityData d, long now) {
        for (CalamityData.Omen o : new ArrayList<>(d.omens)) {
            ResourceLocation rl = ResourceLocation.tryParse(o.dimension);
            ServerLevel level = rl == null ? null : s.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
            CalamityDef def = WorldData.calamity(o.id);
            if (level == null || def == null) {
                d.omens.remove(o);
                d.setDirty();
                continue;
            }
            int left = (int) Math.max(0, (o.riseAt - now + 19) / 20);
            for (int c : COUNTDOWN) {
                if (left == c && o.lastCount > c) {
                    o.lastCount = c;
                    for (ServerPlayer p : level.players()) if (p.distanceToSqr(o.x, p.getY(), o.z) < 192.0 * 192.0) p.displayClientMessage(Component.translatable("nusmp.elite.calamity.countdown", left), true);
                    if (c == 10 || c == 5) fire(new CalamityEvents.Omen(def.id(), def.name(), level, new Vec3(o.x, o.y, o.z), def.announce().omen(), c, o.reason));
                }
            }
            if (now < o.riseAt) continue;
            if (!level.hasChunk(Mth.floor(o.x) >> 4, Mth.floor(o.z) >> 4)) {
                o.waited += 5;
                if (o.waited > 1200) cancelOmen(level, d, o, def, "chunk_unloaded");
                continue;
            }
            rise(level, d, o, def, now);
        }
    }

    private static void cancelOmen(ServerLevel level, CalamityData d, CalamityData.Omen o, CalamityDef def, String why) {
        d.omens.remove(o);
        d.setDirty();
        fire(new CalamityEvents.Vanish(def.id(), def.name(), level, new Vec3(o.x, o.y, o.z), "", why));
    }

    private static void rise(ServerLevel level, CalamityData d, CalamityData.Omen o, CalamityDef def, long now) {
        d.omens.remove(o);
        d.setDirty();
        ResourceLocation rl = ResourceLocation.tryParse(def.entity());
        EntityType<?> type = rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        Entity e = type == null ? null : type.create(level);
        if (!(e instanceof CalamityBoss boss)) {
            LOG.warn("[nusmp] elite world: calamity {} names the entity {}, which is not a calamity boss", def.id(), def.entity());
            if (e != null) e.discard();
            fire(new CalamityEvents.Vanish(def.id(), def.name(), level, new Vec3(o.x, o.y, o.z), "", "bad_entity"));
            return;
        }
        int x = Mth.floor(o.x), z = Mth.floor(o.z);
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        boss.moveTo(o.x, y, o.z, RNG.nextFloat() * 360f, 0f);
        boss.setup(def.id());
        Player target = o.target == null ? null : level.getPlayerByUUID(o.target);
        if (target == null) target = level.getNearestPlayer(o.x, y, o.z, 96.0, false);
        if (target != null) boss.setTarget(target);
        level.addFreshEntity(boss);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(o.x, y, o.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        d.lastWoke.put(def.id(), now);
        d.setDirty();
    }

    /** A boss that was not seen for this long while its chunk is not loaded is dropped from the list of living calamities (it rejoins it when it loads back). */
    private static final long STALE_TICKS = 72000L;

    private static void verifyActive(MinecraftServer s, CalamityData d) {
        long now = s.overworld().getGameTime();
        for (CalamityData.Active a : new ArrayList<>(d.active)) {
            ResourceLocation rl = ResourceLocation.tryParse(a.dimension);
            ServerLevel level = rl == null ? null : s.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
            if (level == null) {
                d.active.remove(a);
                d.setDirty();
                continue;
            }
            Entity e = level.getEntity(a.boss);
            if (e != null && e.isAlive()) {
                a.x = e.getX();
                a.y = e.getY();
                a.z = e.getZ();
                a.misses = 0;
                a.seen = now;
                d.setDirty();
            } else if (a.seen == 0L) {
                a.seen = now;
            } else if (!level.hasChunk(Mth.floor(a.x) >> 4, Mth.floor(a.z) >> 4) && now - a.seen > STALE_TICKS) {
                d.active.remove(a);
                d.setDirty();
                CalamityDef def = WorldData.calamity(a.id);
                if (def != null) fire(new CalamityEvents.Vanish(def.id(), def.name(), level, new Vec3(a.x, a.y, a.z), "", "stale"));
            } else if (level.hasChunk(Mth.floor(a.x) >> 4, Mth.floor(a.z) >> 4) && ++a.misses >= 2) {
                d.active.remove(a);                                  // gone without telling us (an operator's /kill of a chunk, a crash)
                d.setDirty();
                CalamityDef def = WorldData.calamity(a.id);
                if (def != null) fire(new CalamityEvents.Vanish(def.id(), def.name(), level, new Vec3(a.x, a.y, a.z), "", "lost"));
            }
        }
    }

    // ================================================================ the ritual

    /** A Calamity Core used on a Mother Rock. Returns true when it was spent. */
    public static boolean ritual(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState rock, ItemStack core) {
        if (!WorldConfig.calamityRitual() || !WorldGuard.allowed(level)) {
            p.displayClientMessage(Component.translatable("nusmp.elite.calamity.here_no"), true);
            return false;
        }
        int charge = rock.getValue(MotherRockBlock.CHARGE);
        if (charge < 2) {
            p.displayClientMessage(Component.translatable("nusmp.elite.calamity.ritual_weak", charge), true);
            return false;
        }
        String id = CalamityCoreItem.calamityOf(core);
        CalamityDef def = id.isEmpty() ? firstRitual() : WorldData.calamity(id);
        if (def == null || !def.hasTrigger(CalamityDef.Type.RITUAL)) {
            p.displayClientMessage(Component.translatable("nusmp.elite.calamity.unknown", id), true);
            return false;
        }
        Component err = start(level, p, def.id(), "ritual", -1);
        if (err != null) {
            p.displayClientMessage(err, true);
            return false;
        }
        level.setBlock(pos, rock.setValue(MotherRockBlock.CHARGE, 0), 3);
        if (!p.getAbilities().instabuild) core.shrink(1);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.5f);
        return true;
    }

    private static CalamityDef firstRitual() {
        for (CalamityDef d : WorldData.calamities()) if (d.enabled() && d.hasTrigger(CalamityDef.Type.RITUAL)) return d;
        return null;
    }

    // ================================================================ what the bosses report

    /** The boss exists and has taken its first tick. */
    public static void bossRose(CalamityBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        CalamityData d = CalamityData.get(sl.getServer());
        CalamityDef def = WorldData.calamity(boss.calamityId());
        register(d, boss, sl);
        d.lastWoke.put(boss.calamityId(), sl.getServer().overworld().getGameTime());
        d.setDirty();
        if (def == null) return;
        announce(sl, def, "spawn", boss.position());
        fire(new CalamityEvents.Rise(def.id(), def.name(), sl, boss.position(), def.announce().spawn(), boss.getUUID()));
        MagiculeApi.bump(sl, boss.blockPosition(), 0.15, 2);
    }

    /** A boss loaded back from disk: it joins the list of living calamities again, nothing is announced. */
    public static void bossResumed(CalamityBoss boss) {
        if (boss.level() instanceof ServerLevel sl) register(CalamityData.get(sl.getServer()), boss, sl);
    }

    private static void register(CalamityData d, CalamityBoss boss, ServerLevel sl) {
        CalamityData.Active a = d.activeOf(boss.getUUID());
        if (a == null) {
            CalamityData.Active made = new CalamityData.Active(boss.getUUID(), boss.calamityId(), sl.dimension().location().toString(), boss.getX(), boss.getY(), boss.getZ());
            made.seen = sl.getServer().overworld().getGameTime();
            d.active.add(made);
            d.setDirty();
        }
    }

    public static void bossPhase(CalamityBoss boss, int phase) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        CalamityDef def = WorldData.calamity(boss.calamityId());
        if (def == null) return;
        announce(sl, def, "phase", boss.position());
        fire(new CalamityEvents.PhaseChange(def.id(), def.name(), sl, boss.position(), def.announce().phase(), boss.getUUID(), phase, boss.phaseCount()));
    }

    /** The boss died: the kill report for the records, the announcement, the magicule discharge. */
    public static void bossDefeated(CalamityBoss boss, DamageSource source) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        MinecraftServer s = sl.getServer();
        CalamityData d = CalamityData.get(s);
        CalamityDef def = WorldData.calamity(boss.calamityId());
        CalamityData.Active a = d.activeOf(boss.getUUID());
        if (a != null) {
            d.active.remove(a);
            d.setDirty();
        }
        Map<UUID, Float> who = new HashMap<>(boss.fighters());
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID id : who.keySet()) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) online.add(p);
        }
        ServerPlayer killer = null;
        Entity src = source == null ? null : source.getEntity();
        if (src instanceof ServerPlayer p) killer = p;
        else if (src instanceof OwnableEntity o && o.getOwner() instanceof ServerPlayer p) killer = p;
        else if (src instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() instanceof ServerPlayer p) killer = p;
        fire(new CalamityKillEvent(boss.calamityId(), boss, killer, who, online, boss.fightTicks(), boss.phaseCount(), !boss.tookDamageFromBoss()));
        if (def != null) {
            announce(sl, def, "defeat", boss.position());
            fire(new CalamityEvents.Defeat(def.id(), def.name(), sl, boss.position(), def.announce().defeat(), boss.getUUID(), who.keySet(), boss.fightTicks()));
        }
        MagiculeApi.bump(sl, boss.blockPosition(), -0.35, 3);                       // the calamity spent the magicule it fed on
        d.lastWoke.put(boss.calamityId(), s.overworld().getGameTime());
        d.setDirty();
    }

    /** The boss left the world. {@code defeated} is true when it died (already reported). */
    public static void bossGone(CalamityBoss boss, Entity.RemovalReason reason, boolean defeated) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        CalamityData d = CalamityData.get(sl.getServer());
        CalamityData.Active a = d.activeOf(boss.getUUID());
        if (a != null) {
            d.active.remove(a);
            d.setDirty();
        }
        boss.killHelpers();
        if (!defeated) {
            CalamityDef def = WorldData.calamity(boss.calamityId());
            if (def != null) fire(new CalamityEvents.Vanish(def.id(), def.name(), sl, boss.position(), "", reason.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    /** The Calamity Core and the rest of the loot of a fallen calamity, dropped where it fell. */
    public static void dropLoot(CalamityBoss boss, ServerLevel level) {
        boss.spawnAtLocation(CalamityCoreItem.stackFor(boss.calamityId()));
        boss.spawnAtLocation(new ItemStack(WorldItems.MAGICULE_SHARD.get(), 10 + RNG.nextInt(7)));
        Item crystal = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("tensura:high_quality_magic_crystal")).orElse(null);
        if (crystal != null && crystal != net.minecraft.world.item.Items.AIR) boss.spawnAtLocation(new ItemStack(crystal, 1 + RNG.nextInt(2)));
    }

    // ================================================================ words

    private static MutableComponent line(CalamityDef def, String kind) {
        CalamityDef.Announce a = def.announce();
        String english = switch (kind) {
            case "omen" -> a.omen();
            case "spawn" -> a.spawn();
            case "phase" -> a.phase();
            default -> a.defeat();
        };
        return Component.translatableWithFallback("nusmp.elite.calamity." + def.id() + "." + kind, english.isEmpty() ? def.name() : english);
    }

    /** Sends an announcement of a calamity (kind: omen, spawn, phase, defeat) to everyone (config) or to the players of the level within 160 blocks. */
    public static void announce(ServerLevel level, CalamityDef def, String kind, Vec3 pos) {
        Component msg = line(def, kind).withStyle(kind.equals("defeat") ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        if (WorldConfig.calamityBroadcast()) {
            level.getServer().getPlayerList().broadcastSystemMessage(msg, false);
            return;
        }
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(pos.x, p.getY(), pos.z) < 160.0 * 160.0) p.sendSystemMessage(msg);
    }

    /** A line from the boss to the players near it (64 blocks). */
    public static void say(ServerLevel level, CalamityBoss boss, Component text) {
        Component msg = Component.empty().append(boss.getDisplayName().copy().withStyle(ChatFormatting.DARK_PURPLE)).append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(text);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(boss) < 64.0 * 64.0) p.sendSystemMessage(msg);
    }

    // ================================================================ commands and tests

    /** One line per living calamity and per omen. */
    public static List<Component> status(MinecraftServer s) {
        CalamityData d = CalamityData.get(s);
        long now = s.overworld().getGameTime();
        List<Component> out = new ArrayList<>();
        for (CalamityData.Active a : d.active) out.add(Component.translatable("nusmp.elite.calamity.status.active", a.id, a.dimension, (int) a.x, (int) a.y, (int) a.z));
        for (CalamityData.Omen o : d.omens) out.add(Component.translatable("nusmp.elite.calamity.status.omen", o.id, Math.max(0, (o.riseAt - now) / 20), (int) o.x, (int) o.z));
        for (CalamityDef def : WorldData.calamities()) {
            long since = now - d.woke(def.id());
            long left = Math.max(0, def.cooldownTicks() - since) / 20 / 60;
            out.add(Component.translatable("nusmp.elite.calamity.status.def", def.id(), def.enabled() ? "on" : "off", left));
        }
        return out;
    }

    /** Removes every living calamity (the bosses are discarded, helpers die) and every omen. Returns how many things were cleared. */
    public static int clearAll(MinecraftServer s) {
        CalamityData d = CalamityData.get(s);
        int n = d.omens.size();
        d.omens.clear();
        Set<UUID> ids = new HashSet<>();
        for (CalamityData.Active a : d.active) ids.add(a.boss);
        for (ServerLevel level : s.getAllLevels()) {
            for (UUID id : ids) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                    n++;
                }
            }
        }
        d.active.clear();
        d.setDirty();
        return n;
    }

    /** Test hook: forget a player's timers. */
    public static void forget(UUID player) { DIRECTOR.reset(player); }
}
