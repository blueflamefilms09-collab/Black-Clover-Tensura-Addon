package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.WaterBc;
import com.newuniverse.nusmp.vfx.WaterBcMeshBuffer;
import com.newuniverse.nusmp.vfx.WaterBcScene;
import com.newuniverse.nusmp.vfx.WaterBcToon;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Water in the hybrid style "BC": 3D toon cores wrapped in volumetric glass (docs/vfx_remake/water_bc.md, water_ingame.md). This layer is registered AFTER {@link WaterFxLayer}, {@link WaterMagicLayer} and
 * {@link WaterSpellLayer}, so for the five shapes below it is the one the manager finds; it is the single hook of the change. With {@code vfx.element.water = V1} (the client config) every call is handed
 * straight to the old layer that owned the shape, so the old look is exactly the old code; the six old {@link WaterSpellLayer} shapes (WATER_DRAGON ... WATER_DRESS, the Valkyrie Dress among them) are not in
 * this layer's shapes at all and stay with their layer in every setting.
 *
 * <ul>
 *   <li>WATERFX_CAST: the Sea Dragon's Roar (a dragon head with an open jaw and a banded glass body) or, when it flies fast (Aqua Javelin, Tide Lance), a spear of water ({@link WaterBc#kindOf}).</li>
 *   <li>WATERFX_ZONE: the Cradle (a glass sphere with whirling bands, when it follows its caster) or the Nest (a dome with spiral foam bands).</li>
 *   <li>WATERFX_BURST: the impact, a crown of water tongues with a glass crown between them, ripples and flying drops.</li>
 *   <li>WATER_SPLASH and WATER_RING: a small crown and the ridged rings; both are reused by blood and paint pages with their own colour, so a colour that is not a water blue draws the grey twins of the
 *       textures and a palette turned to that colour.</li>
 * </ul>
 *
 * <p>Cost: the geometry is made by the pure {@link WaterBcScene} (the 3D cores go to {@link WaterBcMeshBuffer}, toon shaded on the CPU by {@link WaterBcToon}, drawn once a frame by {@link WaterBcDraw}; the
 * glass goes to the vertex buffer in a fixed order). Each effect gets a tier from {@link WaterBcFrame#tierOf}: the frame plan admits them nearest first under the ceilings of {@link WaterBc#FRAME_MESH_CEILING}
 * mesh quads and {@link WaterBc#FRAME_SPRITE_CEILING} sprite quads for all water effects together, and what does not fit falls back to the flat sprites or is not drawn. Nothing is allocated per frame (the
 * scene, the toon instance and every vector are fields or statics), nothing is sent or computed on a server.
 */
public class WaterBcLayer extends AbstractVfxLayer implements WaterBcScene.Out {
    /** [0 = native textures, 1 = grey twins where the sprite has one][sprite id] */
    private static final ResourceLocation[][] TEX = new ResourceLocation[2][WaterBc.SPRITES.length];

    static {
        for (int i = 0; i < WaterBc.SPRITES.length; i++) {
            TEX[0][i] = VfxTextures.byName(WaterBc.fileName(WaterBc.SPRITES[i], false));
            TEX[1][i] = WaterBc.hasTwin(i) ? VfxTextures.byName(WaterBc.fileName(WaterBc.SPRITES[i], true)) : TEX[0][i];
        }
    }

    private static final AbstractVfxLayer OLD_FX = new WaterFxLayer(), OLD_MAGIC = new WaterMagicLayer();

    // scratch: the render thread is the only user (one layer instance, drawn one effect at a time)
    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), P2 = new Vector3f(), P3 = new Vector3f();

    private final WaterBcScene scene = new WaterBcScene();
    private VfxVertexBuffer buf;
    private int set;
    private boolean tinted;
    private int tintRgb;

    @Override public Set<VfxShape> shapes() { return EnumSet.copyOf(WaterBc.SHAPES); }

    /** The layer that drew the shape before this change. */
    private static AbstractVfxLayer old(VfxShape s) { return s == VfxShape.WATER_SPLASH || s == VfxShape.WATER_RING ? OLD_MAGIC : OLD_FX; }

    @Override public int defaultDuration(VfxShape s) { return old(s).defaultDuration(s); }

    @Override public int defaultColor(VfxShape s) { return old(s).defaultColor(s); }

    @Override
    public int vertexBudget(VfxShape s) { return WaterBc.bcLook() ? WaterBc.vertexBudget(s) : old(s).vertexBudget(s); }

    @Override
    public void onSpawn(VfxInstance inst) { old(inst.shape).onSpawn(inst); }

    /** Which of the seven looks this effect gets (from its shape, the distance it covers, its duration and whether it follows its caster); -1 for a shape that is not the layer's. */
    public static int kindOf(VfxInstance inst) {
        Vec3 a = inst.payload.from(), b = inst.payload.to();
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return WaterBc.kindOf(inst.shape, Math.sqrt(dx * dx + dy * dy + dz * dz), inst.duration, inst.payload.followEntity() >= 0);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!WaterBc.bcLook()) {
            old(inst.shape).render(inst, ctx, buf);
            return;
        }
        int kind = kindOf(inst);
        if (kind < 0) return;
        int tier = WaterBcFrame.tierOf(inst);
        if (tier == WaterBc.LOD0 && ctx.detail < 0.6f) tier = WaterBc.LOD1;                  // particles on "Decreased" or the far level of detail: the light mesh
        if (tier == WaterBc.SKIP) return;
        this.buf = buf;
        this.tinted = WaterBc.tintable(kind) && !WaterBc.nativePalette(inst.color);          // blood and paint pages that reuse the splash and the ring keep their own colour
        this.tintRgb = inst.color & 0xFFFFFF;
        this.set = tinted ? 1 : 0;
        Vec3 from = inst.from(ctx), to = inst.to(ctx), cam = ctx.camPos;
        scene.view.set(ctx.camRight.x, ctx.camRight.y, ctx.camRight.z, ctx.camUp.x, ctx.camUp.y, ctx.camUp.z);
        scene.begin(this, (float) (from.x - cam.x), (float) (from.y - cam.y), (float) (from.z - cam.z), (float) (to.x - cam.x), (float) (to.y - cam.y), (float) (to.z - cam.z),
                inst.power, inst.ageTicks(ctx.partialTick), inst.duration, inst.seed, tinted, tintRgb).build(kind, tier);
    }

    // ------------------------------------------------------------------ the scene's output
    @Override
    public void sprite(int tex, boolean add, float u0, float u1, int argb, float[] q) {
        if (WaterBcFrame.spriteBudgetLeft() <= 0 || !buf.hasBudget(4)) return;               // the frame ceiling and the activation's own budget
        WaterBcFrame.spent(1);
        P0.set(q[9], q[10], q[11]);                                                          // bottom left
        P1.set(q[6], q[7], q[8]);                                                            // bottom right
        P2.set(q[3], q[4], q[5]);                                                            // top right
        P3.set(q[0], q[1], q[2]);                                                            // top left
        buf.quad(TEX[set][tex], add ? VfxBlend.ADD : VfxBlend.ALPHA, P0, P1, P2, P3, u0, 0f, u1, 1f, argb, argb);
    }

    @Override
    public void mesh(WaterBcToon.Inst m) { WaterBcToon.emit(m, scene.view, WaterBcMeshBuffer.FRAME, tinted, tintRgb); }
}
