package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Check;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Verdict;
import com.newuniverse.nusmp.multiverse.WizardKing;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The Mana Zone (docs/mana/mana_zone_redesign.md): the late-game technique of commanding the natural mana of the surroundings instead of only your own
 * reserve. Canon: (1) offence from any point of the zone, bigger and more numerous spells, and a spell born inside an enemy attack to destroy it from within;
 * (2) sharper senses and reactions (with Reinforcement / Mana Skin the user foresees blows), and pressurised protective spaces for the user and allies;
 * (3) standing and moving in mid-air on the ambient mana as invisible footholds.
 *
 * <p>Efficiency (the owner's explicit ask): ONE tracked entry ({@link Zone}) per zone, nothing per entity; the radius is {@link ManaZoneMath#radiusAt} of the
 * ticks since the start tick (no per-tick state change); the server tick returns at once when no zone exists, otherwise looks at each zone and runs its jobs
 * only when due (the entity query every {@code scan_interval} ticks, an AABB first and a squared distance second; the upkeep once a second; the client refresh
 * every {@code refresh_interval} ticks); the network carries the state at start, at end and as that refresh; the perception and all drawing are the
 * client's, from the synced state. No allocation per tick on the idle path.
 */
public final class ManaZoneManager {
    private ManaZoneManager() {}

    public enum End { CANCEL, COLLAPSE, EXPIRED, DEATH, DIMENSION, MOUNT, LOGOUT, ADMIN }

    /** The one tracked entry of a zone. Server thread only. */
    static final class Zone {
        final UUID owner;
        final int ownerId;
        final long serial, startTick, endTick;
        final ResourceKey<Level> dim;
        final double cap;
        final int style, tint;
        final boolean free, riding;
        final double reinforcement;
        double cx, cy, cz;
        long nextScan, nextUpkeep, nextRefresh;
        final double[] bx = new double[4], by = new double[4], bz = new double[4];
        final long[] bUntil = new long[4];

        Zone(UUID owner, int ownerId, long serial, long startTick, long endTick, ResourceKey<Level> dim, double cap, int style, int tint, boolean free, boolean riding, double reinforcement) {
            this.owner = owner;
            this.ownerId = ownerId;
            this.serial = serial;
            this.startTick = startTick;
            this.endTick = endTick;
            this.dim = dim;
            this.cap = cap;
            this.style = style;
            this.tint = tint;
            this.free = free;
            this.riding = riding;
            this.reinforcement = reinforcement;
        }
    }

    private static final Map<UUID, Zone> ZONES = new HashMap<>();
    private static final List<Zone> SCRATCH = new ArrayList<>();
    private static long serialCounter = (System.nanoTime() & 0xFFFFFFL) << 16 | 1L;
    private static final Predicate<Projectile> LIVE_PROJECTILE = pr -> pr.isAlive() && !pr.isRemoved();

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_speed"), FLY_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_foothold");
    private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_attack"), KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_knockback");
    private static final String SLOWED = "nusmp_mz_slow";

    // ---------------------------------------------------------------- queries
    /** True while this player's Mana Zone is open (server side; false on a client). */
    public static boolean active(Player p) { return !p.level().isClientSide && ZONES.containsKey(p.getUUID()); }

    static Zone zoneOf(Player p) { return ZONES.get(p.getUUID()); }

    public static int activeCount() { return ZONES.size(); }

    /** The zone's radius now: a function of the ticks since the start tick. */
    static double radius(Zone z, long now) { return ManaZoneMath.radiusAt(now - z.startTick, ManaZoneData.zone().growTicks(), z.cap); }

    /** The radius a player's zone has (or would have) at game time {@code now}, 0 when none. */
    public static double radiusAt(Player p, long now) {
        Zone z = zoneOf(p);
        return z == null ? 0 : radius(z, now);
    }

    /** The start tick of a player's zone, -1 when none. */
    public static long startTickOf(Player p) {
        Zone z = zoneOf(p);
        return z == null ? -1 : z.startTick;
    }

    /** The radius cap of a player's zone, 0 when none. */
    public static double capOf(Player p) {
        Zone z = zoneOf(p);
        return z == null ? 0 : z.cap;
    }

    /** The radius of a player's open zone, 0 when none. */
    public static double radiusOf(Player p) {
        Zone z = zoneOf(p);
        return z == null ? 0 : radius(z, p.level().getGameTime());
    }

    // ---------------------------------------------------------------- the gate
    private record Cached(long at, Verdict verdict) {}

    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, long[]> SENT = new ConcurrentHashMap<>();       // last gate payload: [open, at]

    /** A fresh evaluation of the gate for a player. */
    public static Verdict verdict(ServerPlayer p) { return ManaZoneGate.evaluate(ManaZoneData.gate(), ManaZoneConfig.strictness(), ManaZonePlayer.progress(p)); }

    /** The same, at most once every two seconds (the grimoire asks it on every key press and every held tick). */
    public static Verdict verdictCached(ServerPlayer p) {
        long now = p.level().getGameTime();
        Cached c = CACHE.get(p.getUUID());
        if (c == null || now - c.at() >= 40 || now < c.at()) {
            c = new Cached(now, verdict(p));
            CACHE.put(p.getUUID(), c);
        }
        return c.verdict();
    }

    /**
     * The late-game gate as the grimoire's pages ask it: the server's verdict for a player, and on the client (or with no entity: a menu) what the server last
     * told this client. The Mana Zone, Omni Volley and Pressure Space pages are not castable, listed or selectable until it is open.
     */
    public static boolean gateOpenFor(LivingEntity e) {
        if (e instanceof ServerPlayer p) return verdictCached(p).open();
        return ManaZoneClientGate.open();
    }

    private static void refuse(ServerPlayer p, Verdict v) {
        p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_zone.refused", "The mana of the land is beyond you. The Mana Zone still needs: %s.", v.summary()).withStyle(ChatFormatting.RED), true);
        long now = p.level().getGameTime();
        var d = ManaZonePlayer.data(p);
        if (now - d.getLong("RefusedAt") < 200 && now >= d.getLong("RefusedAt")) return;       // the full list goes to the chat at most every ten seconds
        d.putLong("RefusedAt", now);
        p.sendSystemMessage(Component.translatableWithFallback("nusmp.mana_zone.refused_head", "Mana Zone: a technique for the end of the road, after mana skin and reinforcement. You are missing:").withStyle(ChatFormatting.GRAY));
        for (Check c : v.unmet())
            p.sendSystemMessage(Component.literal(" - " + ManaZoneGate.label(c.req()) + ": " + c.have() + " of " + c.need()).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------------------------------------------------------------- opening and closing
    /** The Mana Zone page: toggles. Returns true when something happened (opened or closed), false when it was refused. */
    public static boolean toggle(ServerPlayer p) {
        if (ZONES.containsKey(p.getUUID())) {
            end(p, End.CANCEL);
            return true;
        }
        Verdict v = verdict(p);
        CACHE.put(p.getUUID(), new Cached(p.level().getGameTime(), v));
        if (!v.open()) { refuse(p, v); return false; }
        long now = p.level().getGameTime();
        if (now < ManaZonePlayer.cooldownUntil(p)) {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_zone.cooldown", "The ambient mana has not settled yet (%s s).", (ManaZonePlayer.cooldownUntil(p) - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null && !p.isCreative() && ex.getMagicule() < EnergyHelper.getMaxMagicule(p) * zr.startMinPercent() / 100.0) {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_zone.low_mana", "You need at least %s%% of your mana to open a Mana Zone.", (int) zr.startMinPercent()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!p.isCreative() && ex != null) EnergyHelper.isOutOfMagiculeConsuming(p, EnergyHelper.getMaxMagicule(p) * zr.openCostPercent() / 100.0);
        start(p, false, 0);
        return true;
    }

    /** The Wizard King's Crown of Mana: a full Mana Zone with no upkeep for {@code ticks}, whatever the gate. */
    public static void startForced(ServerPlayer p, int ticks) {
        if (ZONES.containsKey(p.getUUID())) end(p, End.ADMIN);
        start(p, true, ticks);
    }

    /** Opens a zone for a CI self-test: no gate, no cost, no cooldown. */
    public static void startForTest(ServerPlayer p, int ticks) { startForced(p, ticks); }

    private static void start(ServerPlayer p, boolean free, int forcedTicks) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        ManaZoneRules rules = ManaZoneData.rules();
        ManaZoneRules.Zone zr = rules.zone();
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(p); } catch (Throwable ignored) { /* no Tensura data */ }
        double cap = Math.min(ManaZoneConfig.maxRadius(), ManaZoneMath.capRadius(ep, ManaZoneGate.needEp(rules.gate(), ManaZoneConfig.strictness()), zr));
        if (free) cap = Math.max(cap, Math.min(ManaZoneConfig.maxRadius(), 24.0));
        int tint = 0xFF8A7CFF, style = ManaZoneStyle.ARCANE;
        double reinforcement = 0;
        try {
            var g = GrimoirePages.grimoireOf(p);
            if (g.isPresent() && g.get().getSkill() instanceof GrimoireBook b) {
                tint = b.color | 0xFF000000;
                style = ManaZoneStyle.of(GrimoirePages.magicOf(g.get()).soulType());
            }
            reinforcement = ManaZonePlayer.progress(p).reinforcementMastery();
        } catch (Throwable ignored) { /* a stand-in player has no grimoire: the arcane look */ }
        boolean king = false;
        try { king = WizardKing.isKing(p); } catch (Throwable ignored) { /* no king data */ }
        long serial = ++serialCounter;
        long end = free ? now + Math.max(20, forcedTicks) : now + zr.maxDurationTicks();
        Zone z = new Zone(p.getUUID(), p.getId(), serial, now, end, sl.dimension(), cap, style, tint, free || king, p.isPassenger(), reinforcement);
        z.cx = p.getX();
        z.cy = p.getY() + 0.9;
        z.cz = p.getZ();
        z.nextScan = ManaZoneMath.nextDue(now, zr.scanInterval(), p.getId());
        z.nextUpkeep = now + 20;
        z.nextRefresh = now + zr.refreshInterval();
        ZONES.put(p.getUUID(), z);

        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
            speed.addTransientModifier(new AttributeModifier(SPEED_ID, zr.speedBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        buffsOn(p);
        flightOn(p);
        ManaZonePlayer.countCast(p);
        sync(p, payload(z, ManaZoneNet.START, now));
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.3f, 1.6f);
        if (!free) com.newuniverse.nusmp.anim.CastAnim.play(p, com.newuniverse.nusmp.anim.CastAnim.MANA_ZONE);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_zone.open", "Mana Zone: the mana around you answers (radius up to %s).", (int) cap).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** Sends to everyone who tracks the caster and to the caster; a stand-in player (CI self-test) has no connection and is skipped. */
    private static void sync(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        try { PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, payload); } catch (RuntimeException noConnection) { /* nobody to tell */ }
    }

    private static void toPlayer(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        try { PacketDistributor.sendToPlayer(p, payload); } catch (RuntimeException noConnection) { /* nobody to tell */ }
    }

    private static ManaZoneNet.ZonePayload payload(Zone z, int op, long now) {
        return new ManaZoneNet.ZonePayload(op, z.ownerId, z.serial, (float) z.cx, (float) z.cy, (float) z.cz, (float) z.cap, ManaZoneData.zone().growTicks(), 0, z.style, z.tint, z.startTick, (int) Math.max(0, now - z.startTick));
    }

    /** Closes the zone of a player (does nothing when none is open). */
    public static void end(ServerPlayer p, End why) {
        Zone z = ZONES.remove(p.getUUID());
        if (z == null) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        double r = radius(z, now);
        long elapsed = now - z.startTick;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        buffsOff(p);
        ZoneCast.clear(p.getUUID());                                                      // the upgrade's per-owner state goes with the zone (death, logout, dimension, expiry, collapse)
        ZoneEvasion.clear(p.getUUID());
        flightOff(p);
        boolean leaving = why == End.DEATH || why == End.LOGOUT;
        if (!leaving && !p.onGround() && !p.isInWater()) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false));          // whoever stood on the footholds comes down gently
            p.fallDistance = 0f;
        }
        if (!leaving && !z.free) {
            int cd = ManaZoneMath.cooldownTicks(r, zr);
            if (why == End.CANCEL) cd = elapsed < 100 ? cd / 4 : cd / 2;
            ManaZonePlayer.setCooldownUntil(p, now + cd);
            if (elapsed >= 100 && why != End.ADMIN) {                                                    // the exhaustion: slow and weak, never hungry
                int ex = ManaZoneMath.exhaustionTicks(elapsed, why == End.COLLAPSE, zr);
                if (why == End.CANCEL) ex /= 2;
                if (ex > 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ex, 0, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ex, 0, false, true));
                    ManaZonePlayer.setExhaustUntil(p, now + ex);
                }
            }
        }
        boolean finisher = why == End.COLLAPSE || why == End.EXPIRED;
        sync(p, payload(z, finisher ? ManaZoneNet.COLLAPSE : ManaZoneNet.END, now));
        if (!leaving) {
            sl.playSound(null, p.blockPosition(), finisher ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.1f, finisher ? 0.8f : 1.4f);
            String fb = switch (why) {
                case COLLAPSE -> "The Mana Zone collapses: you have no mana left to hold it.";
                case EXPIRED -> "Your Mana Zone fades: the mana is too wild to hold any longer.";
                case MOUNT -> "Your Mana Zone breaks as you mount or dismount.";
                case DIMENSION -> "Your Mana Zone does not follow you across the dimensions.";
                default -> "Your Mana Zone fades.";
            };
            p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_zone.end." + why.name().toLowerCase(java.util.Locale.ROOT), fb).withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---------------------------------------------------------------- the upgrade's attribute buffs (docs/magic/mana_zone_upgrade.md; the speed bonus above is the 1.99.0 one)
    private static void buffsOn(ServerPlayer p) {
        ZoneUpRules.Rules up = ManaZoneConfig.upgrade();
        if (!up.buffs()) return;
        AttributeInstance atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.removeModifier(ATTACK_ID);
            if (up.attackBonus() > 0) atk.addTransientModifier(new AttributeModifier(ATTACK_ID, up.attackBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(KNOCKBACK_ID);
            if (up.knockbackResist() > 0) kb.addTransientModifier(new AttributeModifier(KNOCKBACK_ID, up.knockbackResist(), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void buffsOff(ServerPlayer p) {
        AttributeInstance atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.removeModifier(ATTACK_ID);
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.removeModifier(KNOCKBACK_ID);
    }

    // ---------------------------------------------------------------- mid-air footholds (NeoForge's creative-flight attribute, as the other flight powers of the mod)
    private static void flightOn(ServerPlayer p) {
        AttributeInstance fly = p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (fly != null) {
            fly.removeModifier(FLY_ID);
            fly.addTransientModifier(new AttributeModifier(FLY_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
        Abilities ab = p.getAbilities();
        if (!ab.mayfly) {
            ab.mayfly = true;
            ManaZonePlayer.setFlightFlag(p, true);
            p.onUpdateAbilities();
        }
    }

    private static void flightOff(ServerPlayer p) {
        AttributeInstance fly = p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (fly != null) fly.removeModifier(FLY_ID);
        if (!ManaZonePlayer.flightFlag(p)) return;
        ManaZonePlayer.setFlightFlag(p, false);
        if (p.isCreative() || p.isSpectator() || (fly != null && p.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0)) return;
        Abilities ab = p.getAbilities();
        ab.mayfly = false;
        ab.flying = false;
        p.onUpdateAbilities();
    }

    // ---------------------------------------------------------------- the server tick
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MANA_ZONE = com.newuniverse.nusmp.perf.PerfProbe.section("s.mana.zone");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onServerTick(ServerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onServerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MANA_ZONE, t0);
        }
    }

    private static void onServerTick0(ServerTickEvent.Post event) {
        if (ZONES.isEmpty()) return;                                                      // the idle path: nothing at all
        tickAll(event.getServer(), event.getServer().overworld().getGameTime());
    }

    /** One pass over the open zones at game time {@code now}. */
    public static void tickAll(MinecraftServer server, long now) {
        SCRATCH.clear();
        SCRATCH.addAll(ZONES.values());
        for (int i = 0, n = SCRATCH.size(); i < n; i++) {
            Zone z = SCRATCH.get(i);
            ServerPlayer p = server.getPlayerList().getPlayer(z.owner);
            if (p == null || p.hasDisconnected()) { ZONES.remove(z.owner); ZoneCast.clear(z.owner); ZoneEvasion.clear(z.owner); continue; }
            tickOne(p, z, now);
        }
        SCRATCH.clear();
    }

    /** The same for one player's zone (the self-test drives a stand-in player that is not in the player list, at later times, to run a zone's life without waiting). */
    public static void tickPlayer(ServerPlayer p, long now) {
        Zone z = ZONES.get(p.getUUID());
        if (z != null) tickOne(p, z, now);
    }

    private static void tickOne(ServerPlayer p, Zone z, long now) {
        if (!p.isAlive()) { end(p, End.DEATH); return; }
        if (p.level().dimension() != z.dim) { end(p, End.DIMENSION); return; }
        if (p.isPassenger() != z.riding) { end(p, End.MOUNT); return; }
        if (now >= z.endTick) { end(p, End.EXPIRED); return; }
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        if (now >= z.nextScan) {
            z.nextScan = now + zr.scanInterval();
            scan(p, z, now, zr);
        }
        if (now >= z.nextUpkeep) {
            z.nextUpkeep = now + 20;
            if (!upkeep(p, z, now, zr)) return;
        }
        if (now >= z.nextRefresh) {
            z.nextRefresh = now + zr.refreshInterval();
            sync(p, payload(z, ManaZoneNet.REFRESH, now));
        }
    }

    /** Once a second: the upkeep. Returns false when the zone collapsed. */
    private static boolean upkeep(ServerPlayer p, Zone z, long now, ManaZoneRules.Zone zr) {
        if (!z.free && !p.isCreative()) {
            double max = EnergyHelper.getMaxMagicule(p);
            double unpaid = EnergyHelper.isOutOfMagiculeConsuming(p, max * ManaZoneMath.upkeepPercent(radius(z, now), zr) / 100.0, max * 0.05);
            if (unpaid > 0) { end(p, End.COLLAPSE); return false; }
        }
        if (!p.getAbilities().mayfly) flightOn(p);                                       // something reset the abilities: stand again
        return true;
    }

    /** Every few ticks: the zone's centre, its bubbles, and the attacks that enter it. An AABB query for projectiles only, then a squared distance. */
    private static void scan(ServerPlayer p, Zone z, long now, ManaZoneRules.Zone zr) {
        z.cx = p.getX();
        z.cy = p.getY() + 0.9;
        z.cz = p.getZ();
        double r = radius(z, now);
        if (r < 2.0) return;
        ServerLevel sl = p.serverLevel();
        List<Projectile> list = sl.getEntitiesOfClass(Projectile.class, new AABB(z.cx - r, z.cy - r, z.cz - r, z.cx + r, z.cy + r, z.cz + r), LIVE_PROJECTILE);
        if (list.isEmpty()) return;
        int budget = zr.maxProjectilesPerScan(), done = 0, looks = 0, lookCap = ManaZoneConfig.upgrade().evasionLooks();
        for (int i = 0, n = list.size(); i < n; i++) {
            Projectile pr = list.get(i);
            if (!ManaZoneMath.inside(pr.getX() - z.cx, pr.getY() - z.cy, pr.getZ() - z.cz, r)) continue;
            if (pr instanceof ThrownEnderpearl || pr instanceof FishingHook || pr instanceof ThrownExperienceBottle) continue;
            Entity owner = pr.getOwner();
            if (owner == p || (owner instanceof LivingEntity lo && Foes.isAlly(p, lo))) continue;
            boolean boss = owner instanceof LivingEntity lb && BalanceLaw.isBoss(lb);
            if (boss || done >= budget) {                                                 // a boss's attack is slowed, never undone; so is whatever exceeds this scan's budget
                slow(pr, zr);
                if (looks < lookCap) { looks++; ZoneEvasion.look(p, z, pr, now); }    // the upgrade: the owner may still step out of the way of what was only slowed
                continue;
            }
            Vec3 at = pr.position();
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), z.tint, 10, 0.45f);        // the spell born inside the attack, and the attack is gone
            pr.discard();
            done++;
        }
        if (done > 0) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.5f);
            if (!z.free && !p.isCreative()) EnergyHelper.isOutOfMagiculeConsuming(p, EnergyHelper.getMaxMagicule(p) * 0.001 * done, EnergyHelper.getMaxMagicule(p) * 0.05);
        }
    }

    private static void slow(Projectile pr, ManaZoneRules.Zone zr) {
        var tag = pr.getPersistentData();
        int n = tag.getInt(SLOWED);
        if (n >= 3) return;                                                               // at most three slowings: it still arrives, late
        tag.putInt(SLOWED, n + 1);
        pr.setDeltaMovement(pr.getDeltaMovement().scale(ManaZoneMath.bossProjectileFactor(zr)));
        pr.hurtMarked = true;
    }

    // ---------------------------------------------------------------- damage: the foresight, the allies and the pressurised spaces
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ZONES.isEmpty()) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;      // the void and /kill are not blows anyone foresees or a space holds off
        long now = victim.level().getGameTime();
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        Entity attacker = event.getSource().getEntity();
        boolean boss = attacker instanceof LivingEntity la && BalanceLaw.isBoss(la);
        double f = 1.0;
        Zone own = victim instanceof ServerPlayer sp ? ZONES.get(sp.getUUID()) : null;
        if (own != null && attacker != null) f *= 1.0 - ManaZoneMath.predictReduction(zr, own.reinforcement, boss);      // only a blow struck by someone can be foreseen: not a fall, not the cold
        double bubble = 1.0, ally = 1.0;
        for (Zone z : ZONES.values()) {
            if (z.dim != victim.level().dimension()) continue;
            boolean theirs = z == own;
            ServerPlayer owner = theirs ? (ServerPlayer) victim : victim.getServer().getPlayerList().getPlayer(z.owner);
            if (owner == null || (!theirs && !Foes.isAlly(owner, victim))) continue;
            double dx = victim.getX() - z.cx, dy = victim.getY() - z.cy, dz = victim.getZ() - z.cz;
            if (!theirs && ManaZoneMath.inside(dx, dy, dz, radius(z, now))) ally = Math.min(ally, ManaZoneMath.allyFactor(zr));
            for (int b = 0; b < 4; b++) {
                if (z.bUntil[b] <= now) continue;
                if (ManaZoneMath.inside(victim.getX() - z.bx[b], victim.getY() - z.by[b], victim.getZ() - z.bz[b], zr.bubbleRadius()))
                    bubble = Math.min(bubble, ManaZoneMath.bubbleFactor(zr, boss));
            }
        }
        f *= ally * bubble;
        if (f >= 0.9999) return;
        f = Math.max(0.30, f);                                                            // never more than 70% off in total
        event.setAmount((float) (event.getAmount() * f));
    }

    // ---------------------------------------------------------------- the pages' numbers (the grimoire and ManaLaw ask these)
    /** Multiplier on a spell's mana cost for this caster: cheaper inside a zone, dearer while exhausted. */
    public static double costMult(ServerPlayer p) {
        Zone z = ZONES.get(p.getUUID());
        boolean exhausted = ManaZonePlayer.exhausted(p);
        if (z != null) return ManaZoneMath.costMult(ManaZoneData.zone(), false);
        return exhausted ? ManaZoneMath.costMult(ManaZoneData.zone(), true) : 1.0;
    }

    /** Multiplier on a spell's power for this caster: the zone's bonus ramps in with its growth and grows with the local mana density. */
    public static double powerMult(ServerPlayer p, double density) {
        Zone z = ZONES.get(p.getUUID());
        if (z == null) return 1.0;
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        return ManaZoneMath.powerMult(zr, ManaZoneMath.grown(p.level().getGameTime() - z.startTick, zr.growTicks()), density);
    }

    /** Mana Strike's reach for this caster (24 normally). */
    public static double strikeRange(ServerPlayer p, double base) {
        Zone z = ZONES.get(p.getUUID());
        return z == null ? base : ManaZoneMath.strikeRange(base, radius(z, p.level().getGameTime()), ManaZoneData.zone());
    }

    /** How many foes Mana Strike may pierce for this caster (3 normally). */
    public static int strikeTargets(ServerPlayer p, int base) { return ZONES.containsKey(p.getUUID()) ? ManaZoneMath.strikeTargets(base, ManaZoneData.zone()) : base; }

    /** Adds a pressurised space (the Pressure Space page): returns false when the caster has no zone. The oldest of four is replaced. */
    static boolean addBubble(ServerPlayer p, double x, double y, double z0) {
        Zone z = ZONES.get(p.getUUID());
        if (z == null) return false;
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        long now = p.level().getGameTime();
        int free = -1, soonest = 0, live = 0;
        long earliest = Long.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            if (z.bUntil[i] > now) {
                live++;
                if (z.bUntil[i] < earliest) { earliest = z.bUntil[i]; soonest = i; }
            } else if (free < 0) free = i;
        }
        int slot = free < 0 || live >= zr.maxBubbles() ? soonest : free;            // the oldest space is let go when the limit is reached
        z.bx[slot] = x;
        z.by[slot] = y;
        z.bz[slot] = z0;
        z.bUntil[slot] = now + zr.bubbleTicks();
        return true;
    }

    static int bubbleCount(Player p) {
        Zone z = ZONES.get(p.getUUID());
        if (z == null) return 0;
        long now = p.level().getGameTime();
        int n = 0;
        for (int i = 0; i < 4; i++) if (z.bUntil[i] > now) n++;
        return n;
    }

    static int tintOf(Player p) {
        Zone z = ZONES.get(p.getUUID());
        return z == null ? 0xFF8A7CFF : z.tint;
    }

    // ---------------------------------------------------------------- once a second per player (ManaLaw.onPlayerTick calls it)
    public static void onSecond(ServerPlayer p) {
        long now = p.level().getGameTime();
        Verdict v = verdictCached(p);
        // the attunement: time spent in dense mana with the mana skin held
        if (!v.open() || ManaZonePlayer.attuneSeconds(p) < ManaZoneGate.needAttune(ManaZoneData.gate(), ManaZoneConfig.strictness())) {
            double density = ManaLaw.density(p.level(), p.blockPosition());
            boolean inRegion = GrandZones.inRegion(p);
            boolean skin = false;
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) skin = ex.getMagicule() >= EnergyHelper.getMaxMagicule(p) * 0.5;
            double gain = GrandZones.inHaven(p) ? 0.0 : ManaZoneGate.attuneGain(inRegion, density, skin, GrandZones.harmOf(p));        // a haven teaches nothing: the pressure is the teacher
            if (gain > 0) {
                int need = ManaZoneGate.needAttune(ManaZoneData.gate(), ManaZoneConfig.strictness());
                double before = ManaZonePlayer.attuneSeconds(p);
                ManaZonePlayer.addAttune(p, gain);
                if (before < need && ManaZonePlayer.attuneSeconds(p) >= need) {
                    p.sendSystemMessage(Component.translatableWithFallback("nusmp.mana_zone.attuned", "The mana of this place has taught you all it can: your attunement is complete.").withStyle(ChatFormatting.LIGHT_PURPLE));
                    NusmpEvents.fire(p, "mana_zone.attuned");
                    CACHE.remove(p.getUUID());
                }
            }
        }
        // tell the client whether the gate is open (its menus), when that changes and once a minute
        long[] last = SENT.get(p.getUUID());
        boolean open = v.open();
        if (last == null || (last[0] == 1) != open || now - last[1] >= 1200 || now < last[1]) {
            SENT.put(p.getUUID(), new long[]{open ? 1 : 0, now});
            toPlayer(p, new ManaZoneNet.GatePayload(open, (int) ManaZonePlayer.attuneSeconds(p), ManaZoneGate.needAttune(ManaZoneData.gate(), ManaZoneConfig.strictness())));
        }
    }

    // ---------------------------------------------------------------- lifecycle
    public static void onDeath(LivingDeathEvent e) {
        if (ZONES.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        end(p, End.DEATH);
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) end(p, End.DIMENSION);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        end(p, End.LOGOUT);
        CACHE.remove(p.getUUID());
        SENT.remove(p.getUUID());
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SENT.remove(p.getUUID());
        if (!ZONES.containsKey(p.getUUID()) && ManaZonePlayer.flightFlag(p)) flightOff(p);   // a zone that was open when the server stopped left mayfly behind: take it back
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!ZONES.containsKey(p.getUUID()) && ManaZonePlayer.flightFlag(p)) flightOff(p);
    }

    public static void onServerStopping() {
        ZONES.clear();
        ZoneCast.clearAll();
        ZoneEvasion.clearAll();
        CACHE.clear();
        SENT.clear();
        GrandZones.clearRuntime();
    }
}
