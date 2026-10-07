package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.util.function.Consumer;

/**
 * 0.48: this mod's core shaders for items and entities (assets/nusmp/shaders/core). A shader that fails to compile on someone's
 * driver is logged and left null; {@link NURenderTypes} then falls back to a vanilla render type, so the game never breaks
 * over a shader.
 * <ul>
 *   <li>{@code rendertype_demon_void}: the Genesis Demon-Slayer's void (crimson end-portal parallax), format POSITION.</li>
 *   <li>{@code rendertype_zagred_aura}: Zagred's pulsing fresnel rim (purple to crimson), format NEW_ENTITY, additive.</li>
 * </ul>
 */
public final class NUShaders {
    private NUShaders() {}

    private static ShaderInstance demonVoid, zagredAura;

    public static ShaderInstance demonVoid() { return demonVoid; }
    public static ShaderInstance zagredAura() { return zagredAura; }

    public static void register(RegisterShadersEvent e) {
        load(e, "rendertype_demon_void", DefaultVertexFormat.POSITION, s -> demonVoid = s);
        load(e, "rendertype_zagred_aura", DefaultVertexFormat.NEW_ENTITY, s -> zagredAura = s);
    }

    /** Registers one shader; if it can't be built it is logged and set to null (the vanilla fallback is used). */
    private static void load(RegisterShadersEvent e, String name, VertexFormat format, Consumer<ShaderInstance> loaded) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("nusmp", name), format), loaded);
        } catch (Exception ex) {
            loaded.accept(null);
            LogUtils.getLogger().error("[nusmp] shader {} failed to load; a vanilla look is used instead", name, ex);
        }
    }
}
