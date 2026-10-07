package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.48: render types for this mod's core shaders ({@link NUShaders}). Extends RenderType only to reach its protected state
 * shards; it is never instantiated. Each getter falls back to a vanilla render type when its shader did not load.
 */
public final class NURenderTypes extends RenderType {
    private NURenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    static final ResourceLocation END_SKY = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    static final ResourceLocation END_PORTAL = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    /** The Demon-Slayer's void: opaque, both faces, the end sky and portal stars bound as Sampler0 / Sampler1. */
    private static final RenderType DEMON_VOID = create("nusmp_demon_void", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(NUShaders::demonVoid))
                    .setTextureState(MultiTextureStateShard.builder().add(END_SKY, false, false).add(END_PORTAL, false, false).build())
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** Zagred's aura: the model again, additive, both faces, the fresnel rim shader over its texture's silhouette. */
    private static final java.util.function.Function<ResourceLocation, RenderType> ZAGRED_AURA = net.minecraft.Util.memoize(tex ->
            create("nusmp_zagred_aura", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    CompositeState.builder()
                            .setShaderState(new ShaderStateShard(NUShaders::zagredAura))
                            .setTextureState(new TextureStateShard(tex, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    /** Zagred's fresnel aura, or vanilla's additive eyes pass if the shader did not load. */
    public static RenderType zagredAura(ResourceLocation tex) { return NUShaders.zagredAura() != null ? ZAGRED_AURA.apply(tex) : RenderType.eyes(tex); }

    /** nusmp's crimson void, or vanilla's end portal if the shader did not load. Vertex format POSITION either way. */
    public static RenderType demonVoid() { return NUShaders.demonVoid() != null ? DEMON_VOID : RenderType.endPortal(); }

    /**
     * 0.56 Yami's Dimension Slash: a translucent quad with the fracture shader (black core, jagged violet aura). Format POSITION_TEX_COLOR, so if the
     * shader did not load the vanilla position-tex-color shader draws the texture as it is (the baked tear still reads as a tear).
     */
    private static final java.util.function.Function<ResourceLocation, RenderType> DIMENSION_SLASH = net.minecraft.Util.memoize(tex ->
            create("nusmp_dimension_slash", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
                    CompositeState.builder()
                            .setShaderState(new ShaderStateShard(() -> NUShaders.dimensionSlash() != null ? NUShaders.dimensionSlash() : GameRenderer.getPositionTexColorShader()))
                            .setTextureState(new TextureStateShard(tex, true, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    /** The Dimension Slash render type for a tear texture (U across the tear, V along it). */
    public static RenderType dimensionSlash(ResourceLocation tex) { return DIMENSION_SLASH.apply(tex); }

    /**
     * Draws one tear quad through the fracture shader right now (the VFX buffer batches only know one shader). The corners are camera-relative world
     * positions (p0 bottom left, p1 bottom right, p2 top right, p3 top left), moved to view space here with {@code modelView}.
     * Returns false, drawing nothing, when the shader did not load; the caller then draws the same texture as a plain sprite.
     */
    public static boolean drawDimensionSlash(ResourceLocation tex, Matrix4f modelView, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int argb) {
        if (NUShaders.dimensionSlash() == null) return false;
        RenderType type = dimensionSlash(tex);
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();
        try {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vector3f v = new Vector3f();
            int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255, a = argb >>> 24;
            modelView.transformPosition(p0, v); bb.addVertex(v.x, v.y, v.z).setUv(0f, 1f).setColor(r, g, b, a);
            modelView.transformPosition(p1, v); bb.addVertex(v.x, v.y, v.z).setUv(1f, 1f).setColor(r, g, b, a);
            modelView.transformPosition(p2, v); bb.addVertex(v.x, v.y, v.z).setUv(1f, 0f).setColor(r, g, b, a);
            modelView.transformPosition(p3, v); bb.addVertex(v.x, v.y, v.z).setUv(0f, 0f).setColor(r, g, b, a);
            MeshData mesh = bb.build();
            if (mesh != null) type.draw(mesh);
        } finally {
            stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        return true;
    }
}
