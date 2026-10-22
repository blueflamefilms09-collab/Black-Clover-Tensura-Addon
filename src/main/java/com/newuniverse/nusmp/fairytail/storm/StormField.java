package com.newuniverse.nusmp.fairytail.storm;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.perf.PerfSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the storm system of the Stormcaller Staff. A storm is an area effect with NO entity: a {@link StormCell} record that lives here, is ticked once per server tick, runs the
 * weather parts of its spell ({@link StormComp}) on the server (rain, hail, bolts, gust rings, a funnel, the calm eye, a gust front, a surge) and tells nearby clients to draw it
 * through a few effect packets ({@link StormFx}). The server decides everything: where it is, how long it lives, who is hurt and how much (through the balance law, with per-creature
 * caps), how many storms a player and the server may have at once ({@link StormCaps}); the client only draws. Per-player state is keyed by UUID; a storm ends when its owner leaves,
 * dies or the server stops; a fault in a storm ends that storm and is logged once, never the server tick. Nothing here changes a block, spawns an entity or touches the food bar.
 */
public final class StormField {
    private StormField() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The live storms, oldest first. */
    private static final List<StormCell> LIVE = new ArrayList<>();
    private static final StormCooldowns COOLDOWNS = new StormCooldowns();
    private static int nextId = 1;
    /** 1.99.0 performance probe (off by default: one flag read): the whole storm tick. */
    private static final PerfProbe.Section PROBE_S_STORM_TICK = PerfProbe.section("s.storm.tick");
    private static boolean registered;

    /** What a cast did. */
    public record Cast(boolean done, String message) {
        static Cast ok() { return new Cast(true, null); }

        static Cast no(String why) { return new Cast(false, why); }
    }

    /** Starts the server-side listeners (idempotent). */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(StormField::onServerTick);
        NeoForge.EVENT_BUS.addListener(StormField::onServerStopping);
        NeoForge.EVENT_BUS.addListener(StormField::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(StormField::onDeath);
    }

    // ------------------------------------------------------------------------------------------------------------------ queries (the self-test and the tooltip use them)
    public static int liveCount() { return LIVE.size(); }

    public static int liveOf(UUID owner) {
        int n = 0;
        for (StormCell c : LIVE) if (c.owner.equals(owner)) n++;
        return n;
    }

    public static int cooldownLeft(UUID player, StormSpell s, long now) { return COOLDOWNS.left(player.toString(), s.ordinal(), now); }

    /** The newest live storm, or null (the self-test reads it). */
    static StormCell newest() { return LIVE.isEmpty() ? null : LIVE.get(LIVE.size() - 1); }

    /** Runs one tick of a storm at an explicit age and time, the same code the server tick runs (the self-test advances a storm with it, a stand-in player is not in the player list). */
    static void tickAt(ServerLevel level, ServerPlayer owner, StormCell c, int age, long now) {
        if (c.spell.isLine()) tickLine(level, owner, c, age, now);
        else tickCell(level, owner, c, age, now);
    }

    /** Ends every storm (the self-test and the server stop use it). */
    static void clearAll(boolean cancel, MinecraftServer server) {
        StormBudget.reset();
        for (StormCell c : new ArrayList<>(LIVE)) end(c, cancel, server);
        LIVE.clear();
        COOLDOWNS.clear();
    }

    // ------------------------------------------------------------------------------------------------------------------ cast
    /**
     * Casts a spell of the staff for a player: checks the cooldown and the caps, pays, places the storm where the caster looks and starts it. Server side only; the one place a storm
     * is made. Nothing is spent when the answer is "no".
     */
    public static Cast cast(ServerPlayer p, StormSpell spell) {
        if (p == null || spell == null || !p.isAlive() || p.isSpectator()) return Cast.no("nusmp.storm.cannot");
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        String key = p.getUUID().toString();
        int left = COOLDOWNS.left(key, spell.ordinal(), now);
        if (left > 0) return Cast.no("cooldown:" + (left + 19) / 20);
        List<StormCaps.Live> live = new ArrayList<>();
        for (StormCell c : LIVE) live.add(new StormCaps.Live(c.ownerKey(), c.start));
        StormCaps.Verdict verdict = StormCaps.decide(live, key, StormConfig.perPlayer(), StormConfig.global());
        if (!verdict.admit()) return Cast.no("nusmp.storm.sky_full");
        if (!StormCost.pay(p, spell)) return Cast.no(null);
        if (verdict.evicts()) end(LIVE.get(verdict.evictIndex()), true, level.getServer());

        int duration = StormRules.effectiveDuration(spell, StormConfig.maxDuration());
        double radius = StormRules.effectiveRadius(spell, StormConfig.maxRadius());
        Vec3 look = p.getViewVector(1.0F);
        double dx = look.x, dz = look.z, hl = Math.sqrt(dx * dx + dz * dz);
        if (hl < 0.05) {                                          // looking straight up or down: the way the body faces
            double yaw = Math.toRadians(p.getYRot());
            dx = -Math.sin(yaw);
            dz = Math.cos(yaw);
        } else {
            dx /= hl;
            dz /= hl;
        }
        long seed = StormMath.packSeed(level.random.nextLong(), spell.mask, spell.ordinal());
        double[] from, to;
        double length = 0;
        if (spell.isLine()) {
            length = Math.min(spell.aimRange, StormMath.MAX_AIM);
            from = new double[]{p.getX() + dx * 1.2, p.getY(), p.getZ() + dz * 1.2};
            to = new double[]{from[0] + dx * length, from[1], from[2] + dz * length};
        } else {
            Vec3 aim = spell.aimRange <= 0 ? p.position() : StormWorld.aimPoint(p, spell.aimRange);
            double gy = StormWorld.groundY(level, aim.x, aim.y, aim.z);
            from = new double[]{aim.x, gy, aim.z};
            to = StormMath.driftEnd(from, dx, dz, spell.driftPerSecond, duration);
        }
        StormCell cell = new StormCell(nextId++, p.getUUID(), level.dimension(), spell, now, duration, seed, from, to, radius, dx, dz, length);
        LIVE.add(cell);
        int cd = Math.max(20, (int) Math.round(spell.cooldownTicks * NUGameRules.spellCooldown(level) * ManaLaw.cooldownMult(p)));
        COOLDOWNS.start(key, spell.ordinal(), now, cd);
        if (spell.isLine()) StormFx.front(level, cell); else StormFx.cell(level, cell);
        StormFx.sound(level, new Vec3(from[0], from[1] + StormMath.CLOUD_HEIGHT, from[2]), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.7f);
        return Cast.ok();
    }

    // ------------------------------------------------------------------------------------------------------------------ ending
    private static void end(StormCell c, boolean cancel, MinecraftServer server) {
        LIVE.remove(c);
        if (!cancel || server == null) return;
        ServerLevel level = server.getLevel(c.dim);
        if (level != null) StormFx.cancel(level, c);
    }

    private static void onServerStopping(ServerStoppingEvent e) { clearAll(false, null); }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        COOLDOWNS.forget(p.getUUID().toString());
        for (StormCell c : new ArrayList<>(LIVE)) if (c.owner.equals(p.getUUID())) end(c, true, p.getServer());
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (StormCell c : new ArrayList<>(LIVE)) if (c.owner.equals(p.getUUID())) end(c, true, p.getServer());
    }

    // ------------------------------------------------------------------------------------------------------------------ the tick
    private static void onServerTick(ServerTickEvent.Post event) {
        if (LIVE.isEmpty()) return;                                          // no storm: one list-size read per tick
        long t0 = PerfProbe.begin();
        try {
            MinecraftServer server = event.getServer();
            for (int i = LIVE.size() - 1; i >= 0; i--) {                      // newest first, no copy of the list: a storm only ever removes itself
                if (i >= LIVE.size()) continue;
                StormCell c = LIVE.get(i);
                try {
                    if (!tick(server, c)) end(c, false, server);
                } catch (Throwable ex) {
                    SafeCast.fault("storm.tick", ex);
                    LOG.warn("[nusmp] a storm failed and was ended: {}", ex.toString());
                    end(c, true, server);
                }
            }
        } finally {
            PerfProbe.end(PROBE_S_STORM_TICK, t0);
        }
    }

    /** One tick of one storm; false when it is over. */
    private static boolean tick(MinecraftServer server, StormCell c) {
        ServerLevel level = server.getLevel(c.dim);
        ServerPlayer owner = server.getPlayerList().getPlayer(c.owner);
        if (level == null || owner == null || !owner.isAlive()) {
            end(c, true, server);
            return true;                                                   // already removed above
        }
        long now = level.getGameTime();
        int age = (int) (now - c.start);
        if (age < 0 || age >= c.duration) return false;
        if (c.spell.isLine()) tickLine(level, owner, c, age, now);
        else tickCell(level, owner, c, age, now);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------------ a standing cell
    private static void tickCell(ServerLevel level, ServerPlayer owner, StormCell c, int age, long now) {
        StormSpell s = c.spell;
        double[] ctr = StormMath.center(c.from, c.to, age, c.duration);
        double r = StormMath.radiusAt(c.radius, age, c.duration);
        // an entity scan only every few ticks (the performance pass's scan interval, never under 4), staggered by the storm's number, and only while the server-wide scan budget of the tick lasts
        boolean scan = (age + c.id) % Math.max(4, PerfSettings.entityScanInterval()) == 0 && StormBudget.scan(now);
        double cx = ctr[0], cy = ctr[1], cz = ctr[2];

        // rain and the funnel and the gust rings all work on the creatures under the cloud: one scan serves them
        List<LivingEntity> foes = List.of();
        boolean needFoes = scan && (s.has(StormComp.RAIN) || s.has(StormComp.FUNNEL) || s.has(StormComp.GUST));
        if (needFoes) foes = StormWorld.in(owner, level, cx, cy, cz, s.has(StormComp.FUNNEL) ? Math.max(r, c.radius * 2.2) : r, StormMath.CLOUD_HEIGHT + 2, true);

        if (s.has(StormComp.RAIN) && scan) rain(level, owner, c, foes, now, cx, cy, cz, r, age);
        if (s.has(StormComp.HAIL)) hail(level, owner, c, age, now, ctr);
        if (s.has(StormComp.BOLTS)) bolts(level, owner, c, age, now, ctr, r);
        if (s.has(StormComp.GUST) && scan) gust(c, foes, age, ctr);
        if (s.has(StormComp.FUNNEL) && scan) funnel(level, owner, c, foes, age, now, ctr);
        if (s.has(StormComp.EYE) && scan && age % 20 < Math.max(4, PerfSettings.entityScanInterval())) eye(level, owner, ctr);
        if (s.has(StormComp.RAIN) && age % 40 == 5) StormFx.sound(level, new Vec3(cx, cy + 2, cz), SoundEvents.WEATHER_RAIN, 0.7f, 1.0f);
    }

    private static void rain(ServerLevel level, ServerPlayer owner, StormCell c, List<LivingEntity> foes, long now, double cx, double cy, double cz, double r, int age) {
        StormSpell s = c.spell;
        boolean heavy = s == StormSpell.DOWNPOUR;
        for (LivingEntity f : foes) {
            f.clearFire();
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, heavy ? 1 : 0, true, true));
            if (heavy) f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, true));
            if (s.tickRaw > 0 && c.hits.tryHit(f.getId(), StormHits.Kind.PULSE, now)) StormWorld.hit(owner, f, s.tickRaw);
        }
        if (age % 20 < Math.max(4, PerfSettings.entityScanInterval())) for (LivingEntity friend : StormWorld.in(owner, level, cx, cy, cz, r, StormMath.CLOUD_HEIGHT + 2, false)) friend.clearFire();   // the rain puts out a friend's fire too
    }

    private static void hail(ServerLevel level, ServerPlayer owner, StormCell c, int age, long now, double[] ctr) {
        int v = StormMath.hailVolleyLanding(age);
        if (v < 0 || !StormBudget.hail(now)) return;                          // a volley the budget refuses is not queued: the next one lands on its own beat
        StormSpell s = c.spell;
        double r = StormMath.radiusAt(c.radius, v * StormMath.HAIL_PERIOD, c.duration);
        int n = StormMath.hailPerVolley(c.radius);
        // ONE scan serves the whole volley: the creatures under the cell, then each stone is tested against that short list (no query and no block search per stone)
        List<LivingEntity> foes = StormWorld.in(owner, level, ctr[0], ctr[1], ctr[2], r + StormMath.HAIL_RADIUS, 8.0, true);
        for (int i = 0; i < n; i++) {
            double x = ctr[0] + StormMath.hailStoneX(c.seed, v, i, r), z = ctr[2] + StormMath.hailStoneZ(c.seed, v, i, r);
            for (LivingEntity f : foes) {
                double dx = f.getX() - x, dz = f.getZ() - z, reach = StormMath.HAIL_RADIUS + f.getBbWidth() * 0.5;
                if (dx * dx + dz * dz > reach * reach || Math.abs(f.getY() - ctr[1]) > 7.0) continue;
                if (!c.hits.tryHit(f.getId(), StormHits.Kind.HAIL, now)) continue;
                StormWorld.hit(owner, f, s.hailRaw);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, true, true));
            }
        }
        if (v % 2 == 0) StormFx.sound(level, new Vec3(ctr[0], ctr[1] + 1.0, ctr[2]), SoundEvents.GLASS_HIT, 0.5f, 1.7f);
    }

    private static void bolts(ServerLevel level, ServerPlayer owner, StormCell c, int age, long now, double[] ctr, double r) {
        StormSpell s = c.spell;
        if (!StormMath.boltDue(age, c.duration, s.boltEvery, c.bolts, s.maxBolts) || !StormBudget.bolt(now)) return;      // one bolt a tick and a few a second over the whole server
        List<LivingEntity> foes = StormWorld.in(owner, level, ctr[0], ctr[1], ctr[2], r, StormMath.CLOUD_HEIGHT + 2, true);
        List<LivingEntity> fit = new ArrayList<>();
        for (LivingEntity f : foes) if (c.hits.count(f.getId(), StormHits.Kind.BOLT) < StormMath.MAX_BOLTS_PER_TARGET) fit.add(f);
        Vec3 mark;
        if (!fit.isEmpty()) {
            LivingEntity pick = fit.get(Math.min(fit.size() - 1, (int) (StormMath.hash(c.seed, c.bolts, 41) * fit.size())));
            mark = new Vec3(pick.getX(), pick.getY(), pick.getZ());
        } else {
            if (StormMath.hash(c.seed, c.bolts, 42) < 0.5f) { c.bolts++; return; }          // nothing to strike: the cloud mutters instead of throwing a bolt every beat
            double[] off = StormMath.strayPoint(c.seed, c.bolts, r);
            double x = ctr[0] + off[0], z = ctr[2] + off[1];
            mark = new Vec3(x, StormWorld.groundY(level, x, ctr[1] + 1.5, z), z);
        }
        int n = c.bolts++;
        Vec3 origin = new Vec3(mark.x + (StormMath.hash(c.seed, n, 43) - 0.5) * 3.0, Math.max(ctr[1] + StormMath.CLOUD_HEIGHT, mark.y + 7.0), mark.z + (StormMath.hash(c.seed, n, 44) - 0.5) * 3.0);
        boolean first = true;
        for (LivingEntity f : StormWorld.near(owner, level, mark, StormMath.BOLT_SPLASH)) {
            if (!c.hits.tryHit(f.getId(), StormHits.Kind.BOLT, now)) continue;
            StormWorld.hit(owner, f, first ? s.boltRaw : s.boltRaw * StormMath.BOLT_SECONDARY);
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, true, true));
            first = false;
        }
        StormFx.bolt(level, origin, mark, StormMath.packSeed(level.random.nextLong(), 0, s.ordinal()));
    }

    private static void gust(StormCell c, List<LivingEntity> foes, int age, double[] ctr) {
        int n = age / StormMath.GUST_PERIOD;
        for (int k = Math.max(0, n - 1); k <= n; k++) {
            double ring = StormMath.gustRingRadius(age, k, c.radius);
            if (ring < 0) continue;
            for (LivingEntity f : foes) {
                double ddx = f.getX() - ctr[0], ddz = f.getZ() - ctr[2], d = Math.sqrt(ddx * ddx + ddz * ddz);
                if (d < 0.05 || Math.abs(d - ring) > StormMath.GUST_WIDTH) continue;
                double k2 = c.spell.push * 0.4;
                StormWorld.push(f, ddx / d * k2, 0.12, ddz / d * k2);
            }
        }
    }

    private static void funnel(ServerLevel level, ServerPlayer owner, StormCell c, List<LivingEntity> foes, int age, long now, double[] ctr) {
        boolean flingNow = age >= c.duration - 6;
        double pull = c.spell.push;
        for (LivingEntity f : foes) {
            double ddx = ctr[0] - f.getX(), ddz = ctr[2] - f.getZ(), d = Math.max(0.3, Math.sqrt(ddx * ddx + ddz * ddz));
            double ux = ddx / d, uz = ddz / d;
            if (flingNow) {
                StormWorld.push(f, -ux * 0.9, 0.45, -uz * 0.9);          // thrown out as the funnel lifts
                continue;
            }
            // drawn in along the axis, swept round it, and lifted a little, the more so the nearer the axis
            double near = Math.max(0.0, 1.0 - d / (c.radius * 2.2));
            StormWorld.push(f, ux * pull * (0.5 + near) - uz * pull * 0.6, 0.05 + 0.1 * near, uz * pull * (0.5 + near) + ux * pull * 0.6);
            if (d < c.radius && c.hits.tryHit(f.getId(), StormHits.Kind.PULSE, now)) StormWorld.hit(owner, f, c.spell.tickRaw);
        }
    }

    private static void eye(ServerLevel level, ServerPlayer owner, double[] ctr) {
        for (LivingEntity friend : StormWorld.in(owner, level, ctr[0], ctr[1], ctr[2], StormMath.EYE_RADIUS, StormMath.CLOUD_HEIGHT, false)) {
            friend.clearFire();
            friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ a line: gale front, storm surge
    private static void tickLine(ServerLevel level, ServerPlayer owner, StormCell c, int age, long now) {
        if (age % 2 != 0) return;
        StormSpell s = c.spell;
        double head = StormMath.frontPos(age, c.duration, c.length);
        double mx = c.from[0] + c.dx * c.length * 0.5, mz = c.from[2] + c.dz * c.length * 0.5;
        double perpX = -c.dz, perpZ = c.dx;
        boolean surge = s.has(StormComp.SURGE);
        for (LivingEntity f : StormWorld.in(owner, level, mx, c.from[1], mz, c.length * 0.5 + c.radius, 5.0, true)) {
            double rx = f.getX() - c.from[0], rz = f.getZ() - c.from[2];
            double along = rx * c.dx + rz * c.dz, across = rx * perpX + rz * perpZ;
            if (!StormMath.inFront(along, across, head, 3.0, c.radius)) continue;
            if (!c.struck.add(f.getId())) continue;
            StormWorld.hit(owner, f, s.tickRaw);
            StormWorld.push(f, c.dx * s.push, surge ? 0.22 : 0.3, c.dz * s.push);
            if (surge) {
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, true));
                f.clearFire();
            }
        }
        if (age == 0) StormFx.sound(level, new Vec3(c.from[0], c.from[1] + 1, c.from[2]), SoundEvents.WIND_CHARGE_BURST.value(), 1.2f, surge ? 0.6f : 0.9f);
    }

    // ------------------------------------------------------------------------------------------------------------------ feedback
    /** Tells a player why a cast did not happen. */
    public static void tell(ServerPlayer p, Cast result) {
        if (result == null || result.done() || result.message() == null) return;
        String m = result.message();
        Component text;
        if (m.startsWith("cooldown:")) text = Component.translatableWithFallback("nusmp.storm.cooldown", "The sky is not ready: %s s.", m.substring(9));
        else if ("nusmp.storm.sky_full".equals(m)) text = Component.translatableWithFallback(m, "The sky is full of storms; wait for one to pass.");
        else text = Component.translatableWithFallback(m, "The storm will not answer.");
        p.displayClientMessage(text.copy().withStyle(ChatFormatting.GRAY), true);
    }
}
