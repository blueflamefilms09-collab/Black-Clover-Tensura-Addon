package com.newuniverse.nusmp.client.relics.item.vu;

import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.visual.genesis.VuBurst;
import com.newuniverse.nusmp.visual.genesis.VuRules;
import com.newuniverse.nusmp.visual.genesis.VuSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The particle half of the upgraded Genesis weapons, a client tick (never a draw call): the ambient layers of the effect data that are particles (motes off the blade, a ring of
 * motes on the ground, a glow at the shoulder), the swing trail while a holder swings, and the signature burst of an act when its cooldown starts. Everything is bounded: at
 * most {@link #MAX_HOLDERS} holders within 32 blocks per tick, {@link VuRules#particleBudget} particles per holder (none at low effect quality), and every particle goes
 * through the shared client budget ({@code PerfBudgets.clientParticles}). The tip of a swing is an approximation from the swing progress (an arc across the front of the
 * holder), not the pose Better Combat animates.
 */
public final class VuFx {
    private VuFx() {}

    private static final int MAX_HOLDERS = 8;
    private static final double MAX_DIST_SQ = 32.0 * 32.0;
    private static final IdentityHashMap<Item, VuSpec> SPECS = new IdentityHashMap<>();
    private static final IdentityHashMap<VuSpec, Emitter[]> EMITTERS = new IdentityHashMap<>();
    private static final Map<Integer, float[]> TIPS = new HashMap<>();                 // entity id -> {x, y, z, tick} of the last trail point
    private static final float[] OUT = new float[6];
    private static final Vector3f SCRATCH = new Vector3f();
    private static long lastActTick = -100;
    private static boolean inited;

    private record Emitter(VuSpec.Layer layer, ParticleOptions options, int period) {}

    public static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(VuFx::onClientTick);
    }

    public static void clear() {
        SPECS.clear();
        EMITTERS.clear();
        TIPS.clear();
    }

    // ================================================================ lookup
    private static VuSpec specFor(Item item) {
        if (SPECS.containsKey(item)) return SPECS.get(item);
        VuSpec s = VuRenderer.specOf(BuiltInRegistries.ITEM.getKey(item).getPath());
        SPECS.put(item, s);
        return s;
    }

    private static Emitter[] emitters(VuSpec spec) {
        Emitter[] e = EMITTERS.get(spec);
        if (e != null) return e;
        java.util.List<VuSpec.Layer> ls = spec.particleLayers();
        e = new Emitter[ls.size()];
        for (int i = 0; i < e.length; i++) {
            VuSpec.Layer l = ls.get(i);
            int period = Math.max(1, Math.round(1f / Math.max(0.05f, l.rate() <= 0f ? 0.5f : l.rate())));
            e[i] = new Emitter(l, options(l.particle(), l.rgb()), period);
        }
        EMITTERS.put(spec, e);
        return e;
    }

    private static ParticleOptions options(String name, int rgb) {
        if (name == null || name.isEmpty() || name.equals("dust")) return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 0.9f);
        ParticleType<?> t = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(name.contains(":") ? name : "minecraft:" + name));
        return t instanceof SimpleParticleType s ? s : ParticleTypes.END_ROD;
    }

    // ================================================================ the tick
    static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !VuOptions.upgrade()) return;
        VfxQuality q = PerfSettings.vfxQuality();
        int per = VuRules.particleBudget(q);
        if (per <= 0) return;
        long now = level.getGameTime();
        boolean particles = VuOptions.particles(), trails = VuOptions.trails();
        if (!particles && !trails) return;
        int holders = 0;
        RandomSource rng = level.random;
        for (AbstractClientPlayer p : level.players()) {
            if (holders >= MAX_HOLDERS) break;
            if (p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > MAX_DIST_SQ) continue;
            ItemStack st = p.getMainHandItem();
            if (st.isEmpty()) continue;
            VuSpec spec = specFor(st.getItem());
            if (spec == null) continue;
            holders++;
            int left = per;
            if (particles) {
                Emitter[] em = emitters(spec);
                for (int i = 0; i < em.length && left > 0; i++) {
                    if ((now + p.getId() * 3L + i) % em[i].period() != 0) continue;
                    int want = Math.min(left, 1 + (q == VfxQuality.HIGH ? 1 : 0));
                    int n = PerfBudgets.clientParticles(now, want);
                    for (int k = 0; k < n; k++) ambient(level, p, em[i].layer(), em[i].options(), rng, now);
                    left -= n;
                }
            }
            if (trails && spec.swing != null) swingTrail(level, p, spec, rng, now, left);
        }
        if (!TIPS.isEmpty() && now % 40 == 0) TIPS.entrySet().removeIf(en -> now - (long) en.getValue()[3] > 20);
    }

    /** One ambient particle of a layer: where it appears depends on `where` (blade, ground, shoulder). */
    private static void ambient(ClientLevel level, Player p, VuSpec.Layer l, ParticleOptions opt, RandomSource rng, long now) {
        double yaw = Math.toRadians(p.yBodyRot), sin = Math.sin(yaw), cos = Math.cos(yaw);
        double crouch = p.isCrouching() ? 0.85 : 1.0;
        String where = l.kind().equals("ground_glow") ? "ground" : l.kind().equals("shoulder_glow") ? "shoulder" : l.where().isEmpty() ? "blade" : l.where();
        double x, y, z, vx = 0.0, vy = 0.012, vz = 0.0;
        switch (where) {
            case "ground" -> {
                double a = now * 0.21 + p.getId() + rng.nextDouble() * 0.6, r = l.radius() > 0f ? l.radius() : 0.9;
                x = p.getX() + Math.cos(a) * r;
                z = p.getZ() + Math.sin(a) * r;
                y = p.getY() + 0.05;
                vx = -Math.sin(a) * 0.012;
                vz = Math.cos(a) * 0.012;
            }
            case "shoulder" -> {
                x = p.getX() - cos * 0.42 - sin * 0.05 + (rng.nextDouble() - 0.5) * 0.2;
                z = p.getZ() - sin * 0.42 + cos * 0.05 + (rng.nextDouble() - 0.5) * 0.2;
                y = p.getY() + 1.45 * crouch + (rng.nextDouble() - 0.5) * 0.2;
            }
            default -> {                                                                // along the weapon, from the hand outward along the look direction
                Vec3 look = p.getLookAngle();
                double len = 0.2 + rng.nextDouble() * (l.radius() > 0f ? l.radius() : 1.4);
                double hx = p.getX() - cos * 0.45 - sin * 0.35, hz = p.getZ() - sin * 0.45 + cos * 0.35, hy = p.getY() + 0.95 * crouch;
                x = hx + look.x * len + (rng.nextDouble() - 0.5) * 0.12;
                y = hy + look.y * len * 0.5 + 0.3 + rng.nextDouble() * 0.5;
                z = hz + look.z * len + (rng.nextDouble() - 0.5) * 0.12;
                vx = (rng.nextDouble() - 0.5) * 0.01;
                vz = (rng.nextDouble() - 0.5) * 0.01;
            }
        }
        level.addParticle(opt, x, y, z, vx, vy, vz);
    }

    // ================================================================ swing trail
    private static void swingTrail(ClientLevel level, AbstractClientPlayer p, VuSpec spec, RandomSource rng, long now, int budget) {
        float progress = p.getAttackAnim(0f);
        if (!p.swinging || progress <= 0.02f || budget <= 0) {
            TIPS.remove(p.getId());
            return;
        }
        float reach = Math.max(0.8f, spec.swing.length() * 1.4f);
        VuRules.swingTip(progress, reach, OUT);
        Vec3 eye = p.getEyePosition();
        double yaw = Math.toRadians(p.getYRot()), fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -fz, rz = fx;
        double tx = eye.x + rx * OUT[0] + fx * OUT[2], ty = eye.y + OUT[1] - 0.2, tz = eye.z + rz * OUT[0] + fz * OUT[2];
        float[] last = TIPS.get(p.getId());
        if (last == null) {
            TIPS.put(p.getId(), new float[]{(float) tx, (float) ty, (float) tz, now});
            return;
        }
        int steps = Math.min(budget, 4);
        int n = PerfBudgets.clientParticles(now, steps);
        ParticleOptions a = dust(spec.swing.rgb()), b = dust(spec.swing.rgb2());
        for (int i = 0; i < n; i++) {
            double f = (i + 1.0) / n;
            double px = last[0] + (tx - last[0]) * f, py = last[1] + (ty - last[1]) * f, pz = last[2] + (tz - last[2]) * f;
            level.addParticle(i % 2 == 0 ? a : b, px, py, pz, 0.0, 0.0, 0.0);
            if (i == n - 1 && rng.nextInt(3) == 0) level.addParticle(ParticleTypes.END_ROD, px, py, pz, (rng.nextDouble() - 0.5) * 0.02, 0.01, (rng.nextDouble() - 0.5) * 0.02);
        }
        last[0] = (float) tx;
        last[1] = (float) ty;
        last[2] = (float) tz;
        last[3] = now;
    }

    private static final Map<Integer, ParticleOptions> DUSTS = new HashMap<>();

    private static ParticleOptions dust(int rgb) {
        ParticleOptions d = DUSTS.get(rgb);
        if (d == null) {
            if (DUSTS.size() > 64) DUSTS.clear();
            d = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.0f);
            DUSTS.put(rgb, d);
        }
        return d;
    }

    // ================================================================ the act burst
    /** From the cooldown payload of the server (RelicPayloads): an act of the holder's weapon was just used; plays its signature burst once. */
    public static void onAct(String relicId, String actKey, long readyAt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !VuOptions.upgrade() || !VuOptions.particles()) return;
        long now = mc.level.getGameTime();
        if (readyAt <= now || now - lastActTick < 4 || mc.player.tickCount < 100) return;                             // a cleared cooldown, or the same act told twice
        VuSpec spec = VuRenderer.specOf(relicId);
        if (spec == null) return;
        VuSpec.Act act = spec.act(actKey);
        if (act == null) return;
        lastActTick = now;
        Player p = mc.player;
        double yaw = Math.toRadians(p.getYRot()), fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -fz, rz = fx;
        ParticleOptions main = dust(act.rgb());
        int n = Math.min(60, act.particles());
        int allowed = PerfBudgets.clientParticles(now, n);
        for (int i = 0; i < allowed; i++) {
            VuBurst.point(act.pattern(), i, allowed, act.ring(), (float) i / Math.max(1, allowed), OUT);
            double x = p.getX() + rx * OUT[0] + fx * OUT[2], y = p.getY() + 1.0 + OUT[1], z = p.getZ() + rz * OUT[0] + fz * OUT[2];
            double vx = rx * OUT[3] + fx * OUT[5], vz = rz * OUT[3] + fz * OUT[5];
            mc.level.addParticle(i % 5 == 0 ? ParticleTypes.END_ROD : main, x, y, z, vx, OUT[4], vz);
        }
        if (!act.sound().isEmpty()) {
            ResourceLocation id = ResourceLocation.parse(act.sound());
            BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(s -> mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 0.8f, 1.0f, false));
        }
    }
}
