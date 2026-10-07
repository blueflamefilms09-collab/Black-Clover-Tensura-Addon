package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

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

    /** nusmp's crimson void, or vanilla's end portal if the shader did not load. Vertex format POSITION either way. */
    public static RenderType demonVoid() { return NUShaders.demonVoid() != null ? DEMON_VOID : RenderType.endPortal(); }
}
