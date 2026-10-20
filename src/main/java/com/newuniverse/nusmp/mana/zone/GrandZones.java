package com.newuniverse.nusmp.mana.zone;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.mana.zone.GrandZoneMath.Exposure;
import com.newuniverse.nusmp.mana.zone.GrandZoneRegion.Effect;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Grand Magic Zones (Strong Magic Regions) as PLACES: the registry of the regions read from {@code data/<namespace>/grand_magic_zones/*.json}, the dungeon's
 * hook ({@link #bind}), and the effects they have on the players inside them. See docs/mana/mana_zone_redesign.md for the canon and the numbers.
 *
 * <p>Efficiency: ONE small tracked entry per player that is inside a region (nothing at all for a player outside every region); the region list is an
 * immutable array compared by squared distance or box; each player is looked at once every {@link GrandZoneMath#SCAN_INTERVAL} ticks, staggered by entity id;
 * the entities round a player are queried only for the effects that need them (gravity, currents, a haven's calm) and only near that player; the world is
 * never edited (the lightning is visual only); the network carries a state only when the player enters, leaves, or the harm / boon moves a twentieth,
 * plus a refresh every {@link GrandZoneMath#SYNC_REFRESH} ticks.
 */
public final class GrandZones {
    private GrandZones() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIRECTORY = "grand_magic_zones";

    private record Entry(GrandZoneRegion region, ResourceLocation dim, long hash) {}

    private static volatile Entry[] entries = new Entry[0];
    private static volatile List<GrandZoneRegion> fromData = List.of();
    private static final Map<String, GrandZoneRegion> DYNAMIC = new LinkedHashMap<>();       // server thread

    /** The one tracked entry of a player inside a region. */
    private static final class Track {
        String id = "";
        long hash;
        int n;                       // counts the passes (every SCAN_INTERVAL ticks)
        long nextGravity;
        int gravityCount;
        int harmBucket = -1, boonBucket = -1;
        long nextSync;
        double harm, boon, fade, density;
        boolean inRegion;
    }

    private static final Map<UUID, Track> TRACK = new HashMap<>();

    // ---------------------------------------------------------------- the registry
    private static void rebuild() {
        List<Entry> out = new ArrayList<>();
        for (GrandZoneRegion r : fromData) out.add(compile(r));
        for (GrandZoneRegion r : DYNAMIC.values()) out.add(compile(r));
        entries = out.toArray(new Entry[0]);
    }

    private static Entry compile(GrandZoneRegion r) {
        ResourceLocation dim = ResourceLocation.tryParse(r.dimension());
        return new Entry(r, dim == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "overworld") : dim, GrandZoneMath.mix(r.id().hashCode(), 7));
    }

    public static List<GrandZoneRegion> all() {
        List<GrandZoneRegion> l = new ArrayList<>(fromData);
        l.addAll(DYNAMIC.values());
        return l;
    }

    public static GrandZoneRegion byId(String id) {
        if (id == null) return null;
        for (Entry e : entries) if (e.region.id().equalsIgnoreCase(id)) return e.region;
        return null;
    }

    /** The densest region that holds the point, or null. No allocation. */
    public static GrandZoneRegion regionAt(ResourceLocation dimension, double x, double y, double z) {
        GrandZoneRegion best = null;
        for (Entry e : entries) {
            if (!e.dim.equals(dimension) || !e.region.contains(x, y, z)) continue;
            if (best == null || e.region.density() > best.density()) best = e.region;
        }
        return best;
    }

    public static GrandZoneRegion regionAt(Level level, BlockPos pos) { return regionAt(level.dimension().location(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5); }

    /** The density a region gives a place (0 when no region holds it): ManaLaw.density takes the larger of this and the biome's. Server side only. */
    public static double densityAt(Level level, BlockPos pos) {
        if (level.isClientSide || entries.length == 0) return 0;
        GrandZoneRegion r = regionAt(level, pos);
        return r == null ? 0 : r.density();
    }

    // ---------------------------------------------------------------- the dungeon's hook
    /**
     * Gives a box of a dimension the environment of a Grand Magic Zone: the floor or room generator calls this with the id of a zone template (for example
     * {@code gravito_rock}, {@code seabed_temple}) and the box of the floor; every player inside it then feels that zone. {@code key} names the binding (call again with
     * the same key to move it; {@link #unbind} removes it). Bindings are not saved: call it again when the floor loads. Returns false for an unknown template.
     */
    public static boolean bind(String key, String zoneId, String dimension, double x0, double y0, double z0, double x1, double y1, double z1) {
        GrandZoneRegion t = byId(zoneId);
        if (t == null || key == null || key.isBlank()) return false;
        DYNAMIC.put(key, t.withBox(key, dimension, x0, y0, z0, x1, y1, z1));
        rebuild();
        return true;
    }

    public static boolean unbind(String key) {
        boolean had = DYNAMIC.remove(key) != null;
        if (had) rebuild();
        return had;
    }

    public static void clearDynamic() {
        if (DYNAMIC.isEmpty()) return;
        DYNAMIC.clear();
        rebuild();
    }

    // ---------------------------------------------------------------- loading
    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() { super(new Gson(), DIRECTORY); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            List<GrandZoneRegion> out = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                String id = e.getKey().getNamespace().equals("nusmp") ? e.getKey().getPath() : e.getKey().toString();
                try {
                    if (!e.getValue().isJsonObject()) throw new IllegalArgumentException(id + ": not an object");
                    GrandZoneRegion r = GrandZoneRegion.parse(id, e.getValue().getAsJsonObject());
                    for (String p : r.problems()) LOG.warn("[nusmp] grand magic zone: {}", p);
                    out.add(r);
                } catch (RuntimeException ex) {
                    LOG.warn("[nusmp] grand magic zone {} skipped: {}", id, ex.getMessage());
                }
            }
            out.sort((a, b) -> a.id().compareTo(b.id()));
            fromData = List.copyOf(out);
            rebuild();
            LOG.info("[nusmp] {} Grand Magic Zone region(s) loaded", out.size());
        }
    }

    // ---------------------------------------------------------------- exposure
    /** The player's harm and boon in a region (before the edge fade). */
    public static Exposure exposure(ServerPlayer p, GrandZoneRegion r) {
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(p); } catch (Throwable ignored) { /* no Tensura data */ }
        return GrandZoneMath.exposure(ep, r.recommendedEp(), r.density(), r.has(Effect.CALM), ManaZoneManager.active(p));
    }

    /** The boon of the zone a player stands in, 0 elsewhere: spell power and regeneration read it. */
    public static double boonOf(Player p) {
        if (p.level().isClientSide) return 0;
        Track t = TRACK.get(p.getUUID());
        return t == null || !t.inRegion ? 0 : t.boon * t.fade;
    }

    /** Is the player inside a region now, and how dense is it, how hard is it on them (for the attunement and the status line). */
    public static boolean inRegion(Player p) {
        Track t = TRACK.get(p.getUUID());
        return t != null && t.inRegion;
    }

    public static double harmOf(Player p) {
        Track t = TRACK.get(p.getUUID());
        return t == null || !t.inRegion ? 0 : t.harm;
    }

    public static String regionIdOf(Player p) {
        Track t = TRACK.get(p.getUUID());
        return t == null || !t.inRegion ? "" : t.id;
    }

    // ---------------------------------------------------------------- the per-player pass
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Entry[] es = entries;
        if (es.length == 0 && TRACK.isEmpty()) return;                                   // nothing loaded, nobody tracked: the fast exit
        if ((p.tickCount + p.getId()) % GrandZoneMath.SCAN_INTERVAL != 0) return;
        pass(p, es);
    }

    private static void pass(ServerPlayer p, Entry[] es) {
        ServerLevel sl = p.serverLevel();
        Track t = TRACK.get(p.getUUID());
        Entry hit = null;
        if (ManaZoneConfig.grandZones() && !p.isSpectator()) {
            ResourceLocation dim = sl.dimension().location();
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (Entry e : es) {
                if (!e.dim.equals(dim) || !e.region.contains(x, y, z)) continue;
                if (hit == null || e.region.density() > hit.region.density()) hit = e;
            }
        }
        if (hit == null) {
            if (t != null) {
                TRACK.remove(p.getUUID());
                if (t.inRegion) toPlayer(p, ManaZoneNet.GrandZonePayload.none());
            }
            return;
        }
        GrandZoneRegion r = hit.region;
        long now = sl.getGameTime();
        boolean entered = t == null || !t.inRegion || !t.id.equals(r.id());
        if (t == null) { t = new Track(); TRACK.put(p.getUUID(), t); }
        if (entered) { t.id = r.id(); t.hash = hit.hash; t.n = 0; t.nextGravity = now + 40; t.harmBucket = -1; t.boonBucket = -1; t.gravityCount = 0; }
        t.inRegion = true;
        Exposure ex = exposure(p, r);
        t.harm = ex.harm() * ManaZoneConfig.harmScale();
        t.boon = ex.boon();
        t.fade = GrandZoneMath.edgeFade(r.depth(p.getX(), p.getY(), p.getZ()));
        t.density = r.density();
        t.n++;

        // the sync: on entering, when the harm or the boon moves a twentieth, and a cheap refresh
        int hb = GrandZoneMath.bucket(t.harm), bb = GrandZoneMath.bucket(t.boon);
        if (entered || hb != t.harmBucket || bb != t.boonBucket || now >= t.nextSync) {
            t.harmBucket = hb;
            t.boonBucket = bb;
            t.nextSync = now + GrandZoneMath.SYNC_REFRESH;
            toPlayer(p, payload(r, t));
        }
        if (entered && now - ManaZonePlayer.data(p).getLong("ZoneNoticeAt") > 600) {                // the border does not nag: one notice a half minute at most
            ManaZonePlayer.data(p).putLong("ZoneNoticeAt", now);
            announce(p, r, t);
        }
        if (p.isCreative()) return;                                                       // creative players feel the weather, not the pressure

        double harm = t.harm * t.fade, boon = t.boon * t.fade;
        boolean user = ManaZoneManager.active(p);
        int n = t.n;

        if (r.has(Effect.PRESSURE) && n % 4 == 0 && harm >= 0.3) {
            int amp = GrandZoneMath.slownessAmplifier(harm);
            if (amp >= 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amp, false, true));
            if (harm >= 0.5) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, true));
            hurtNonLethal(p, GrandZoneMath.pressureDamage(harm, p.getMaxHealth()));
        }
        if (r.has(Effect.MAGMA_HEAT) && n % 4 == 1) heat(p, harm);
        if (r.has(Effect.WITCH_GLOW) && n % 6 == 2 && harm >= 0.3) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            if (GrandZoneMath.mix(t.hash, now / 20) % 3 == 0) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        }
        if (r.has(Effect.MANA_STORM) && now % 20 < GrandZoneMath.SCAN_INTERVAL) storm(sl, p, t, now, harm);
        if (r.has(Effect.CHAOTIC_GRAVITY) && now >= t.nextGravity) gravity(sl, p, t, now, harm, boon, user);
        if (r.has(Effect.VIOLENT_CURRENTS)) currents(sl, p, now, r, harm, boon, user);
        if (r.has(Effect.CALM) && n % 4 == 0) haven(sl, p);

        // the boon: the dense mana feeds a player strong enough for it
        if (boon > 0.05 && n % 2 == 0) {
            try {
                double max = EnergyHelper.getMaxMagicule(p);
                EnergyHelper.gainMagicule(p, max * GrandZoneMath.manaRegenPercent(boon, r.density()) / 100.0 * (GrandZoneMath.SCAN_INTERVAL * 2 / 20.0), EnergyHelper.GainType.NORMAL);
            } catch (Throwable ignored) { /* no Tensura data: nothing to feed */ }
        }
    }

    private static void toPlayer(ServerPlayer p, ManaZoneNet.GrandZonePayload payload) {
        try { PacketDistributor.sendToPlayer(p, payload); } catch (RuntimeException noConnection) { /* a stand-in player (CI self-test) has no connection */ }
    }

    private static ManaZoneNet.GrandZonePayload payload(GrandZoneRegion r, Track t) {
        float minX = (float) (r.box() ? r.minX() : r.cx() - r.radius()), minY = (float) (r.box() ? r.minY() : r.cy() - r.radius()), minZ = (float) (r.box() ? r.minZ() : r.cz() - r.radius());
        float maxX = (float) (r.box() ? r.maxX() : r.cx() + r.radius()), maxY = (float) (r.box() ? r.maxY() : r.cy() + r.radius()), maxZ = (float) (r.box() ? r.maxZ() : r.cz() + r.radius());
        return new ManaZoneNet.GrandZonePayload(true, r.id(), r.name(), r.box(), (float) r.cx(), (float) r.cy(), (float) r.cz(), (float) r.radius(), minX, minY, minZ, maxX, maxY, maxZ,
                r.theme().ordinal(), r.effectMask(), (int) Math.round(r.density() * 10), t.harmBucket * 5, t.boonBucket * 5, r.tint());
    }

    private static void announce(ServerPlayer p, GrandZoneRegion r, Track t) {
        String key = r.has(Effect.CALM) ? "haven" : t.harm >= 0.6 ? "crushing" : t.harm >= 0.3 ? "heavy" : t.harm > 0 ? "pressing" : t.boon >= 0.3 ? "fed" : "balanced";
        String fallback = switch (key) {
            case "haven" -> "A haven: the mana here is calm and gives to everyone.";
            case "crushing" -> "The mana here is far beyond you. Turn back.";
            case "heavy" -> "The mana here is heavy and unstable. You are not ready for it.";
            case "pressing" -> "The mana presses on you; stay on guard.";
            case "fed" -> "The dense mana feeds you.";
            default -> "The mana here is thick and wild, and it answers your strength.";
        };
        p.displayClientMessage(Component.translatableWithFallback("nusmp.zone.enter", "You enter %s, a Grand Magic Zone. %s", r.name(),
                Component.translatableWithFallback("nusmp.zone.feel." + key, fallback)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------------------------------------------------------- the effects (each capped, none lethal on its own, none edits the world)
    /** Magic damage that never kills: it leaves one point of health. */
    private static void hurtNonLethal(ServerPlayer p, double amount) {
        float dmg = (float) Math.min(amount, p.getHealth() - 1f);
        if (dmg > 0f) p.hurt(p.serverLevel().damageSources().magic(), dmg);
    }

    private static void heat(ServerPlayer p, double harm) {
        int secs = GrandZoneMath.heatFireSeconds(harm);
        if (secs <= 0 || p.fireImmune() || p.hasEffect(MobEffects.FIRE_RESISTANCE)) return;
        p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), secs * 20));
        float dmg = (float) GrandZoneMath.heatDamage(harm, p.getMaxHealth());
        if (dmg > 0f) p.hurt(p.serverLevel().damageSources().onFire(), dmg);
    }

    private static void storm(ServerLevel sl, ServerPlayer p, Track t, long now, double harm) {
        long seconds = now / 20;
        if (!GrandZoneMath.stormBolt(seconds, t.hash ^ p.getId())) return;
        double a = (GrandZoneMath.mix(t.hash, seconds * 3 + 1) & 0xFFFF) / 65536.0 * Math.PI * 2, d = 10 + (GrandZoneMath.mix(t.hash, seconds * 3 + 2) & 0xFF) / 255.0 * 18;
        int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!sl.canSeeSky(pos.above()) || !sl.hasChunkAt(pos)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(pos));
        bolt.setVisualOnly(true);                                                         // light and thunder only: no fire, no damage, nothing persists
        sl.addFreshEntity(bolt);
        if (GrandZoneMath.boltHitChance(harm) > 0 && ((GrandZoneMath.mix(t.hash, seconds) >>> 8) & 1023) / 1023.0 < GrandZoneMath.boltHitChance(harm))
            hurtNonLethal(p, GrandZoneMath.boltDamage(p.getMaxHealth()));                // a bolt that finds a weak player: a share of their health, never lethal
    }

    private static void gravity(ServerLevel sl, ServerPlayer p, Track t, long now, double harm, double boon, boolean user) {
        t.gravityCount++;
        t.nextGravity = now + GrandZoneMath.pulseGap(t.hash ^ p.getId(), t.gravityCount);
        double[] v = new double[3];
        List<LivingEntity> near = sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12.0), e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e != p);
        int cap = 10;
        pulse(p, p, t, harm, boon, user, v);
        for (LivingEntity e : near) {
            if (cap-- <= 0) break;
            double eh = e instanceof Player ? harm : 0.6;                                 // mobs are always "weak" against the zone: thrown about
            long seed = t.hash ^ (e.getId() * 0x9E3779B97F4A7C15L);
            GrandZoneMath.PulseKind k = GrandZoneMath.gravityPulse(seed, t.gravityCount, eh, 0, false, v);
            kick(e, v, k);
        }
    }

    private static void pulse(ServerPlayer p, LivingEntity e, Track t, double harm, double boon, boolean user, double[] v) {
        GrandZoneMath.PulseKind k = GrandZoneMath.gravityPulse(t.hash ^ (e.getId() * 0x9E3779B97F4A7C15L), t.gravityCount, harm, boon, user, v);
        kick(e, v, k);
    }

    private static void kick(LivingEntity e, double[] v, GrandZoneMath.PulseKind kind) {
        e.setDeltaMovement(e.getDeltaMovement().add(v[0], v[1], v[2]));
        e.hurtMarked = true;
        if (kind == GrandZoneMath.PulseKind.LIFT) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, GrandZoneMath.liftSlowFallTicks(v[1] / GrandZoneMath.MAX_GRAVITY_SPEED), 0, false, false));   // low gravity for a moment
            e.fallDistance = 0f;
        } else if (kind == GrandZoneMath.PulseKind.SLAM) {
            e.fallDistance = Math.min(e.fallDistance, 2f);                               // the slam shoves, it does not kill by falling
        }
    }

    private static void currents(ServerLevel sl, ServerPlayer p, long now, GrandZoneRegion r, double harm, double boon, boolean user) {
        double[] v = new double[3];
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(14.0), e -> e.isAlive() && e.isInWater() && !e.isSpectator() && !(e instanceof ArmorStand))) {
            boolean player = e instanceof Player;
            GrandZoneMath.currentPush(now, e.getX(), e.getZ(), r.cx(), r.cz(), player ? harm : 0.6, player ? boon : 0, player && user && e == p, v);
            e.setDeltaMovement(e.getDeltaMovement().add(v[0], v[1], v[2]));
            e.hurtMarked = true;
        }
    }

    private static void haven(ServerLevel sl, ServerPlayer p) {
        if (p.getHealth() < p.getMaxHealth()) p.heal((float) GrandZoneMath.havenHeal());
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16.0), e -> e instanceof Enemy && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false));
        }
    }

    // ---------------------------------------------------------------- lifecycle
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { TRACK.remove(e.getEntity().getUUID()); }

    /** Server stopping: drop every runtime binding and every tracked entry. */
    public static void clearRuntime() {
        TRACK.clear();
        clearDynamic();
    }
}
