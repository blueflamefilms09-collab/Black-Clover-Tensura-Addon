package com.newuniverse.nusmp.client.manamethod;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Geometry;
import com.newuniverse.nusmp.mana.method.RuneNet;
import com.newuniverse.nusmp.mana.method.RuneRules;
import com.newuniverse.nusmp.mana.method.RuneSpec;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-only holder of the Mana Method rune: it keeps the tiny state of the runes near the camera (at most {@link RuneGeneratorUtil#MAX_RUNES_DRAWN}) and turns each into
 * particles. Everything about the shape is {@link RuneGeneratorUtil}'s (pure, tested); this class only asks the camera and the budgets and calls {@code addParticle}.
 * <ul>
 *   <li>The rune draws itself over its charge: a growing prefix of the figure (rings from the outside in, then the symbols, then the star), each point spawned once; when the
 *       charge is over the outer ring flares once, a little wider.</li>
 *   <li>Cost: one tick handler that returns at once with no rune; per rune per tick the NEW points only (about 170 points over 18 to 30 ticks, so 6 to 10 per tick), thinned by the
 *       effect quality and the distance (a rune beyond {@value RuneGeneratorUtil#CULL_DISTANCE} blocks is not drawn), and every spawn first asks the client particle budget
 *       ({@code PerfBudgets.clientParticles}). The geometry is cached; the particle options are built once per rune; nothing is allocated per point.</li>
 *   <li>The custom rune particles of the catalog are used when {@code particles.custom} is on, plain dust of the same colour otherwise.</li>
 * </ul>
 */
public final class RuneClient {
    private RuneClient() {}

    private static final class CRune {
        Frame frame;
        Geometry geo;
        RuneGeneratorUtil.Params params;
        ParticleOptions ring, glyph;
        long born;
        int charge, drawn;
        boolean own, flared;
    }

    private static final List<CRune> RUNES = new ArrayList<>();
    private static final double[] OUT = new double[3];

    public static void init() {
        NeoForge.EVENT_BUS.addListener(RuneClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> RUNES.clear());
    }

    /** A rune packet arrived: validate it, build the frame once, and keep it. */
    public static void onRune(RuneNet.RunePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.valid(p)) return;
        RuneSpec spec = RuneSpec.unpack(p.spec());
        CRune r = new CRune();
        r.params = RuneGeneratorUtil.paramsFor(spec);
        r.geo = RuneGeneratorUtil.geometry(r.params);
        r.frame = RuneGeneratorUtil.frameFacing(p.x(), p.y(), p.z(), p.lx(), p.ly(), p.lz());
        r.born = mc.level.getGameTime();
        r.charge = p.charge();
        r.own = mc.player != null && mc.player.getId() == p.ownerId();
        r.ring = options(r.params.ringParticle(), r.params.tint(), 1.0f);
        r.glyph = options(r.params.glyphParticle(), r.params.tint(), 1.2f);
        if (RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN) RUNES.remove(0);       // the oldest goes first
        RUNES.add(r);
    }

    /** The particle of a catalog id in the rune's colour; plain dust of that colour when the custom particles are off or not bound yet. */
    private static ParticleOptions options(String id, int rgb, float scale) {
        if (FxConfig.clientCustom()) {
            TintedParticleType type = FxTypes.type(id);
            FxCatalog.Def d = FxCatalog.byId(id);
            if (type != null && d != null) return new TintedParticleOptions(type, d.tint() ? (rgb & 0xFFFFFF) : -1, scale, false, d.tint());
        }
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 0.8f * scale);
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        if (RUNES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) { RUNES.clear(); return; }
        SafeCast.client("mana-method-rune", () -> draw(mc, level));
    }

    private static void draw(Minecraft mc, Level level) {
        long now = level.getGameTime();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        VfxQuality q = PerfSettings.vfxQuality();
        for (int k = RUNES.size() - 1; k >= 0; k--) {
            CRune r = RUNES.get(k);
            long age = now - r.born;
            if (age < 0 || age > r.charge + RuneRules.FLARE_TICKS) { RUNES.remove(k); continue; }
            double dx = r.frame.cx() - cam.x, dy = r.frame.cy() - cam.y, dz = r.frame.cz() - cam.z;
            float detail = Math.min(q.detail, RuneGeneratorUtil.distanceDetailSq(dx * dx + dy * dy + dz * dz, r.own));
            int stride = RuneGeneratorUtil.strideFor(detail);
            if (stride == 0) { r.drawn = r.geo.count; continue; }                       // too far: skipped, nothing is spawned and nothing is owed
            double radius = r.params.radius();
            double spin = age * 0.06;
            if (age <= r.charge) {
                int now2 = RuneGeneratorUtil.visible(r.geo, age / (double) r.charge);
                int n = RuneGeneratorUtil.newPoints(r.drawn, now2, stride);
                n = PerfBudgets.clientParticles(now, n);
                for (int j = 0; j < n; j++) spawn(level, r, RuneGeneratorUtil.newPointIndex(r.drawn, stride, j), radius, spin);
                r.drawn = now2;
            } else if (!r.flared) {
                r.flared = true;                                                        // the release: the outer ring once more, wider and faster
                int count = RuneGeneratorUtil.flareCount(r.geo);
                int want = (count + stride - 1) / stride;
                int n = PerfBudgets.clientParticles(now, want);
                for (int j = 0; j < n; j++) spawn(level, r, j * stride, radius * 1.35, spin);
            }
        }
    }

    private static void spawn(Level level, CRune r, int index, double radius, double spin) {
        if (index < 0 || index >= r.geo.count) return;
        RuneGeneratorUtil.toWorld(r.geo, index, radius, r.frame, spin, OUT);
        ParticleOptions o = r.geo.kind[index] == RuneGeneratorUtil.KIND_GLYPH ? r.glyph : r.ring;
        level.addParticle(o, OUT[0], OUT[1], OUT[2], 0.0, 0.0, 0.0);
    }
}
