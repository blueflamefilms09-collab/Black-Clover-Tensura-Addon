package com.newuniverse.nusmp.client.relics.fx;

import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.upgrade.UpgradeFxData;
import com.newuniverse.nusmp.relics.upgrade.UpgradeFxMath;
import com.newuniverse.nusmp.relics.upgrade.UpgradeParticleNames;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ambient effect layers of the upgraded God relics (1.102.0): drifting motes, rotating halo rings, energy lines along an edge, orbiting sparks, ground glow, stride trails. The layers of
 * each relic are data ({@code assets/nusmp/visual_upgrade/<id>.json}, {@link UpgradeFxData}); this class only turns them into client particles, inside the performance guards of the mod:
 * <ul>
 *   <li>off with {@code visual.upgrade} and with {@code relicfx.particles}; nothing for a spectator, an invisible wearer, a hidden Curios slot or hidden armour relics;</li>
 *   <li>only wearers within {@link UpgradeFxMath#FAR} blocks of the camera, at most eight per tick, and beyond 4 blocks only those in front of the camera;</li>
 *   <li>the rate of every layer falls with the distance ({@link UpgradeFxMath#distanceFactor}) and with the effect quality ({@code PerfSettings.vfxQuality()}: 30%, 60%, 100%);</li>
 *   <li>every particle is taken from the client particle budget of the tick ({@code PerfBudgets.clientParticles}), so a crowd of wearers can never flood the client;</li>
 *   <li>the options of a particle are built once per layer; the tick allocates no objects of its own.</li>
 * </ul>
 */
public final class UpgradeFxClient {
    private UpgradeFxClient() {}

    private static boolean inited;
    private static final double[] LOCAL = new double[3], WORLD = new double[3];
    private static final Map<String, Compiled> COMPILED = new HashMap<>();

    /** A layer with its particle options resolved once. */
    private record CLayer(UpgradeFxData.Layer layer, ParticleOptions options) {}

    private record Compiled(List<CLayer> layers) {}

    /** Game bus: registers the client tick. Safe to call twice. */
    public static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(UpgradeFxClient::onClientTick);
    }

    /**
     * The particle a data entry names, always one of the mod's own custom particles (1.108.0, {@code particle.fx}): "dust" is a custom mote dyed with the layer's colour
     * ({@link FxRelic#dust}); a vanilla name ({@code minecraft:end_rod}, {@code flame}, ...) is the particle that replaces it in the theme of the layer's colour ({@link Fx#options(com.newuniverse.nusmp.particle.fx.ThemeKey, FxOld)},
     * the name-to-constant decision is {@link UpgradeParticleNames}); an id of another namespace is the mod's own particle type, looked up in the registry (null when it is not a plain particle).
     */
    public static ParticleOptions options(String particle, int color, float size) {
        if (UpgradeParticleNames.isDust(particle)) return FxRelic.dust(color, Math.max(0.3f, size));
        if (UpgradeParticleNames.isVanilla(particle)) {
            FxOld old;
            try {
                old = FxOld.valueOf(UpgradeParticleNames.oldName(particle));
            } catch (IllegalArgumentException unknown) {
                old = FxOld.valueOf(UpgradeParticleNames.DEFAULT);
            }
            return Fx.options(FxRelic.theme(color), old);
        }
        ResourceLocation id = ResourceLocation.tryParse(particle);
        if (id == null) return null;
        ParticleType<?> t = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        return t instanceof ParticleOptions po ? po : null;
    }

    private static Compiled compile(String relicId) {
        Compiled c = COMPILED.get(relicId);
        if (c == null) {
            UpgradeFxData d = UpgradeFxData.forId(relicId);
            java.util.ArrayList<CLayer> out = new java.util.ArrayList<>();
            if (d != null) {
                for (UpgradeFxData.Layer l : d.layers()) {
                    ParticleOptions o = options(l.particle(), l.color(), l.size());
                    if (o != null) out.add(new CLayer(l, o));
                }
            }
            c = new Compiled(List.copyOf(out));
            COMPILED.put(relicId, c);
        }
        return c;
    }

    static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !RelicFxOptions.particles() || !RelicFxOptions.upgrade()) return;
        float qf = UpgradeFxMath.qualityFactor(PerfSettings.vfxQuality().ordinal());
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.getPosition();
        Vector3f look = cam.getLookVector();
        long tick = level.getGameTime();
        RandomSource rnd = level.random;
        int looked = 0;
        for (AbstractClientPlayer p : level.players()) {
            if (looked >= 8) break;
            if (p.isSpectator() || p.isInvisible()) continue;
            double dx = p.getX() - cp.x, dy = p.getEyeY() - cp.y, dz = p.getZ() - cp.z, d2 = dx * dx + dy * dy + dz * dz;
            float df = UpgradeFxMath.distanceFactor(d2);
            if (df <= 0f) continue;
            if (d2 > 16.0 && dx * look.x + dy * look.y + dz * look.z < 0.15 * Math.sqrt(d2)) continue;          // behind the camera or far off to the side
            List<Relics.Worn> worn = Relics.wornList(p);
            if (worn.isEmpty()) continue;
            looked++;
            List<CuriosLink.RenderFlag> flags = null;
            boolean hideArmour = RelicFxOptions.hideArmourRelics();
            double speed = Math.hypot(p.getX() - p.xo, p.getZ() - p.zo);
            double yaw = p.yBodyRot;
            for (int i = 0; i < worn.size(); i++) {
                RelicDef d = worn.get(i).def();
                if (d.slot().isHand() || d.slot() == RelicSlot.CONSUMABLE) continue;
                if (d.slot().isArmor() && hideArmour) continue;
                Compiled c = compile(d.id());
                if (c.layers().isEmpty()) continue;
                if (d.slot().isCurio()) {                                                          // the eye icon of the Curios screen: off means no effects either
                    if (flags == null) flags = CuriosLink.flags(p);
                    boolean on = true;
                    for (var f : flags) if (f.stack() == worn.get(i).stack()) { on = f.render(); break; }
                    if (!on) continue;
                }
                for (int li = 0; li < c.layers().size(); li++) {
                    CLayer cl = c.layers().get(li);
                    UpgradeFxData.Layer l = cl.layer();
                    int n = UpgradeFxMath.emitCount(l.perSecond(), df, qf, rnd.nextDouble(), l.maxPerTick());
                    if (n <= 0) continue;
                    n = PerfBudgets.clientParticles(tick, n);
                    for (int k = 0; k < n; k++) {
                        if (!UpgradeFxMath.point(l, tick, k, n, rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), speed, LOCAL)) break;
                        UpgradeFxMath.toWorld(yaw, LOCAL[0], LOCAL[1], LOCAL[2], WORLD);
                        double vy = l.kind().equals("motes") || l.kind().equals("line") ? l.speed() : 0.0;
                        ParticleOptions custom = cl.options();                                       // the mod's own particle of the layer (options() above)
                        level.addParticle(custom, p.getX() + WORLD[0], p.getY() + WORLD[1], p.getZ() + WORLD[2], (rnd.nextDouble() - 0.5) * 0.004, vy, (rnd.nextDouble() - 0.5) * 0.004);
                    }
                }
            }
        }
    }
}
