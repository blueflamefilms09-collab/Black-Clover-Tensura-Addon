package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.layer.FlameMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.MagicCircleLayer;
import com.newuniverse.nusmp.vfx.client.layer.WindMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.WaterMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.RuneCircleLayer;
import com.newuniverse.nusmp.vfx.client.layer.AntiMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.ElementShapesLayer;
import com.newuniverse.nusmp.vfx.client.layer.AuraLayer;
import com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer2;
import com.newuniverse.nusmp.vfx.client.layer.DreamPaintLayer;
import com.newuniverse.nusmp.vfx.client.layer.TimeMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.FireSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.WaterSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.WindSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.EarthSpellLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Client-side owner of all running effects. Layers are registered here. */
public final class VfxManager {
    private static final VfxManager INSTANCE = new VfxManager();
    private static final int MAX_ACTIVE = 64;

    private final Map<VfxShape, AbstractVfxLayer> layers = new EnumMap<>(VfxShape.class);
    private final List<VfxInstance> active = new ArrayList<>();
    private final VfxVertexBuffer buffer = new VfxVertexBuffer();

    public static VfxManager get() { return INSTANCE; }

    private VfxManager() {
        // ---- register layers here (one line per new magic type) ----
        register(new MagicCircleLayer());
        register(new FlameMagicLayer());
        register(new WindMagicLayer());
        register(new WaterMagicLayer());
        register(new RuneCircleLayer());
        register(new AntiMagicLayer());
        register(new ElementShapesLayer());
        register(new AuraLayer());
        register(new TimeMagicLayer());
        register(new FireSpellLayer());
        register(new WaterSpellLayer());
        register(new WindSpellLayer());
        register(new EarthSpellLayer());
        register(new ArcaneSpellLayer());     // 0.34
        register(new ArcaneSpellLayer2());
        register(new DreamPaintLayer());      // 0.41
    }

    public void register(AbstractVfxLayer layer) { for (VfxShape s : layer.shapes()) layers.put(s, layer); }

    /** Called from the network handler on the client thread. */
    public void spawn(VfxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        VfxShape shape = p.shapeType();
        if (FxRenderer.handles(shape)) {
            if (mc.options.particles().get() == ParticleStatus.MINIMAL) dustFallback(p, 0xFFFFFFFF);
            else FxRenderer.spawn(p);
            return;
        }
        VfxDefinitions.Def def = VfxDefinitions.get(shape);
        AbstractVfxLayer layer = layers.get(shape);
        int color = p.color() != 0 ? p.color() : def != null && def.color() != 0 ? def.color()
                : layer != null ? layer.defaultColor(shape) : 0xFFFFFFFF;

        // Minimal particles (or a shape with no layer yet): cheap vanilla dust only.
        if (mc.options.particles().get() == ParticleStatus.MINIMAL || layer == null) {
            dustFallback(p, color);
            return;
        }
        int duration = p.duration() > 0 ? p.duration() : def != null && def.duration() > 0 ? def.duration() : layer.defaultDuration(shape);
        if (active.size() >= MAX_ACTIVE) active.remove(0);
        VfxInstance inst = new VfxInstance(p, layer, color, duration, mc.level);
        active.add(inst);
        layer.onSpawn(inst);
        if (def != null && def.shake() > 0 && inst.power >= 1.5f && shape != VfxShape.MAGIC_CIRCLE_EXPLOSION && shape != VfxShape.FLAME_EXPLOSION) {
            VfxShake.add(p.to(), def.shake() * inst.power, 8);
        }
    }

    public void tick() {
        VfxShake.tick();
        Iterator<VfxInstance> it = active.iterator();
        while (it.hasNext()) {
            VfxInstance inst = it.next();
            inst.layer.onTick(inst);
            inst.tick();
            if (inst.done()) it.remove();
        }
    }

    public void clear() { active.clear(); }

    /** Draw everything (render thread). */
    public void render(net.minecraft.client.Camera camera, Matrix4f modelView, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (active.isEmpty() || mc.level == null) return;
        float detail = mc.options.particles().get() == ParticleStatus.DECREASED ? 0.5f : 1f;
        VfxRenderContext ctx = new VfxRenderContext(camera, modelView, partialTick, mc.level, detail);
        for (VfxInstance inst : active) {
            buffer.beginActivation(inst.layer.vertexBudget(inst.shape));
            inst.layer.render(inst, ctx, buffer);
        }
        // Vertices are pre-transformed into view space, so the model-view uniform must be identity.
        var mvStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.identity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        buffer.flush(modelView);
        mvStack.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    private void dustFallback(VfxPayload p, int color) {
        var level = Minecraft.getInstance().level;
        Vector3f rgb = new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
        DustParticleOptions dust = new DustParticleOptions(rgb, 1.2f);
        Vec3 a = p.from(), b = p.to();
        for (int i = 0; i <= 6; i++) {
            Vec3 q = a.lerp(b, i / 6.0);
            level.addParticle(dust, q.x, q.y + 0.5, q.z, 0, 0, 0);
        }
    }
}
