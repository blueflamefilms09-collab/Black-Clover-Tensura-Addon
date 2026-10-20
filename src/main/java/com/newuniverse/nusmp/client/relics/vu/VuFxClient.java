package com.newuniverse.nusmp.client.relics.vu;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.fx.FxQuality;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the ambient effect layers of an upgraded relic that are particles (idle motes, sparks, embers, arcs, drips, wisps) are spawned here, from the data of
 * assets/nusmp/visual_upgrade/fx/&lt;relic id&gt;.json ({@link VuFx}), while the relic is held (the main hand, the off hand) or worn (Curios or armour slot) by a player near the camera.
 * Bounded like the other relic particles: only players within 24 blocks (at most six, those in front of the camera first past 4 blocks), the layer's own rate thinned by the quality
 * options ({@code relicfx.quality}, the performance pass's vfx quality) and at most {@link FxQuality#particlesPerTick} particles in one client tick over all wearers (and under the
 * performance pass's client allowance). Nothing runs when {@code visual.upgrade} or {@code relicfx.particles} is off. It also keeps the draw (equip) animation clock of the local
 * player's main hand ({@link #equipProgress}). Client thread only.
 */
public final class VuFxClient {
    private VuFxClient() {}

    private static boolean inited;
    private static int budget;
    private static final Map<String, Optional<VuFx>> FX = new HashMap<>();
    private static final Map<VuFx.Layer, ParticleOptions> OPTS = new IdentityHashMap<>();
    private static String heldId;
    private static long heldSince;
    /** Ticks the draw animation of a newly equipped upgraded relic takes. */
    public static final float EQUIP_TICKS = 12f;

    public static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(VuFxClient::onClientTick);
    }

    /** Forgets the parsed fx files (a resource reload). */
    public static void clear() {
        FX.clear();
        OPTS.clear();
    }

    /** The fx data of a relic, or null. */
    public static VuFx fxOf(String id) {
        Optional<VuFx> f = FX.get(id);
        if (f == null) {
            VuFx parsed = null;
            try {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("nusmp", "visual_upgrade/fx/" + id + ".json"));
                if (res.isPresent()) {
                    try (BufferedReader in = res.get().openAsReader()) {
                        parsed = VuFx.parse(String.join("\n", in.lines().toList()));
                    }
                }
            } catch (Exception ex) {
                LogUtils.getLogger().error("[nusmp] visual_upgrade/fx/{}.json could not be read", id, ex);
            }
            f = Optional.ofNullable(parsed);
            FX.put(id, f);
        }
        return f.orElse(null);
    }

    /** 0 just after the main-hand relic was taken up .. 1 when its draw animation is over (1 when no upgraded relic is in the hand): an ease-out with a little overshoot. */
    public static float equipProgress(float partial) {
        if (heldId == null) return 1f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1f;
        float t = (mc.level.getGameTime() - heldSince + partial) / EQUIP_TICKS;
        if (t >= 1f) return 1f;
        if (t <= 0f) return 0f;
        float u = t - 1f;
        return 1f + 2.2f * u * u * u + 1.2f * u * u;                                // easeOutBack
    }

    static void onClientTick(ClientTickEvent.Post e) {
        budget = 0;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        String main = idOf(mc.player.getMainHandItem());
        if (main != null && fxOf(main) == null) main = null;
        if (!java.util.Objects.equals(main, heldId)) {
            heldId = main;
            heldSince = level.getGameTime();
        }
        if (!VuModels.enabled() || !RelicFxOptions.particles()) return;
        FxQuality q = RelicFxOptions.quality();
        VfxQuality vq = PerfSettings.vfxQuality();
        int cap = q.particlesPerTick;
        int clientCap = PerfSettings.clientParticlesPerTick();
        if (clientCap > 0) cap = Math.min(cap, clientCap);
        if (vq == VfxQuality.LOW) cap = Math.max(2, cap / 2);
        float rate = q.particleRate * (vq == VfxQuality.LOW ? 0.5f : 1f);
        long tick = level.getGameTime();
        var cam = mc.gameRenderer.getMainCamera();
        var cp = cam.getPosition();
        var look = cam.getLookVector();
        int looked = 0;
        for (AbstractClientPlayer p : level.players()) {
            if (budget >= cap || looked >= 6) break;
            if (p.isSpectator() || p.isInvisible()) continue;
            double dx = p.getX() - cp.x, dy = p.getEyeY() - cp.y, dz = p.getZ() - cp.z, d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > 24.0 * 24.0) continue;
            if (d2 > 16.0 && dx * look.x + dy * look.y + dz * look.z < 0.15 * Math.sqrt(d2)) continue;
            looked++;
            handLayers(level, p, p.getMainHandItem(), true, tick, rate, cap);
            handLayers(level, p, p.getOffhandItem(), false, tick, rate, cap);
            List<Relics.Worn> worn = Relics.wornList(p);
            for (int i = 0; i < worn.size() && budget < cap; i++) {
                RelicDef d = worn.get(i).def();
                VuFx fx = fxOf(d.id());
                if (fx == null) continue;
                for (VuFx.Layer l : fx.layers) if (l.isParticle() && l.in("worn")) spawn(level, p, d.slot(), l, tick + i * 5L, rate, cap, false, true);
            }
        }
    }

    private static String idOf(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        RelicDef d = Relics.defOf(s);
        return d == null ? null : d.id();
    }

    private static void handLayers(ClientLevel level, AbstractClientPlayer p, ItemStack stack, boolean main, long tick, float rate, int cap) {
        String id = idOf(stack);
        if (id == null) return;
        VuFx fx = fxOf(id);
        if (fx == null) return;
        for (VuFx.Layer l : fx.layers) if (l.isParticle() && l.in("hand")) spawn(level, p, RelicSlot.MAIN_HAND, l, tick, rate, cap, main, false);
    }

    private static ParticleOptions options(VuFx.Layer l) {
        ParticleOptions o = OPTS.get(l);
        if (o == null) {
            if (l.particle.equals("dust")) {
                int c = l.color < 0 ? 0xFFFFFF : l.color;
                o = new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 0.9f);
            } else {
                var type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.withDefaultNamespace(l.particle));
                o = type instanceof ParticleOptions po ? po : net.minecraft.core.particles.ParticleTypes.END_ROD;
            }
            OPTS.put(l, o);
        }
        return o;
    }

    /** One layer for one holder: every {@code layer.every / rate} ticks (staggered by the holder's id), {@code count} particles round the place the relic is at. */
    private static void spawn(ClientLevel level, AbstractClientPlayer p, RelicSlot slot, VuFx.Layer l, long tick, float rate, int cap, boolean mainHand, boolean worn) {
        int every = Math.max(1, Math.round(l.every / Math.max(0.25f, rate)));
        if ((tick + p.getId() * 7L) % every != 0) return;
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                              // forward
        double rx = -fz, rz = fx;                                                     // to the wearer's left? (sign fixed per slot below)
        double y0, f, side;
        switch (slot) {
            case HEAD -> { y0 = p.getBbHeight() + 0.1; f = 0; side = 0; }
            case BACK -> { y0 = 1.1; f = -0.32; side = 0; }
            case HANDS -> { y0 = 0.85; f = 0.1; side = 0.42; }
            case NECK, CHEST -> { y0 = 1.25; f = 0.2; side = 0; }
            case MAIN_HAND -> { y0 = 0.95; f = 0.5; side = mainHand ? -0.3 : 0.3; }
            default -> { y0 = 0.9; f = 0; side = 0.38; }                              // a charm hovers at the hip
        }
        RandomSource r = level.random;
        ParticleOptions opts = options(l);
        for (int k = 0; k < l.count && budget < cap; k++) {
            double a = r.nextDouble() * Math.PI * 2, rad = l.radius * (0.4 + 0.6 * r.nextDouble());
            double x = p.getX() + fx * f + rx * side + Math.cos(a) * rad;
            double z = p.getZ() + fz * f + rz * side + Math.sin(a) * rad;
            double y = p.getY() + y0 + l.dy + (r.nextDouble() - 0.3) * rad;
            double vy = l.rise, vx = Math.cos(a + 1.57) * l.speed, vz = Math.sin(a + 1.57) * l.speed;
            if (l.type.equals("arcs") || l.type.equals("sparks")) {                   // short bright jumps
                vx += (r.nextDouble() - 0.5) * 0.06;
                vz += (r.nextDouble() - 0.5) * 0.06;
            } else if (l.type.equals("drips")) {
                vy = -Math.abs(l.rise);
            }
            level.addParticle(opts, x, y, z, vx, vy, vz);
            budget++;
        }
    }
}
