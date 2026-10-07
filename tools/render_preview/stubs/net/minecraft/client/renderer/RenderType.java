package net.minecraft.client.renderer;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Preview stub of Minecraft 1.21.1's RenderType: only the static factories of the ENTITY-format render types, each one carrying what the
 * preview rasteriser needs to draw like the game does (copied from the 1.21.1 render type definitions and their core shaders):
 * <pre>
 *   name                         blend        cull  depth write  lightmap+shading  sorted far->near  discard (alpha &lt; 0.1)
 *   entity_solid                 none         yes   yes          yes               no                never (opaque)
 *   entity_cutout                none         yes   yes          yes               no                texture alpha
 *   entity_cutout_no_cull        none         no    yes          yes               no                texture alpha
 *   entity_cutout_no_cull_z_offset none       no    yes          yes               no                texture alpha, pulled 0.02% toward the eye
 *   entity_smooth_cutout         none         no    yes          yes               no                texture alpha
 *   entity_translucent           alpha        no    yes          yes               yes               texture * vertex alpha
 *   entity_translucent_cull      alpha        yes   yes          yes               yes               texture * vertex alpha
 *   entity_translucent_emissive  alpha        no    NO           no (full bright)  yes               texture * vertex alpha
 *   entity_no_outline            alpha        no    NO           yes               yes               texture * vertex alpha
 *   entity_decal                 none         no    yes (EQUAL depth test)  yes    no                texture alpha
 *   armor_cutout_no_cull         none         no    yes          yes               no                texture alpha, pulled toward the eye
 *   eyes                         additive     yes   NO           no (full bright)  yes               texture * vertex alpha
 *   energy_swirl                 additive     no    yes          yes               yes               texture * vertex alpha, uv scrolled
 *   lightning                    additive     yes   yes          no, no texture    yes               never   (format: position + colour only)
 * </pre>
 * "lightmap+shading" = the entity vertex shader's two-light directional shading and the packed light's lightmap colour.
 * The fields are the preview's own descriptor (the game's RenderType has different members).
 */
public class RenderType {
    public enum Blend { NONE, TRANSLUCENT, ADDITIVE }

    public enum Format { ENTITY, POSITION_COLOR }

    public final String name;
    public final ResourceLocation texture;
    public final Blend blend;
    public final boolean cull;
    public final boolean depthWrite;
    public final boolean lit;
    public final boolean sort;
    public final boolean depthEqual;
    /** "none", "tex" (texture alpha &lt; 0.1) or "final" (texture * vertex alpha &lt; 0.1). */
    public final String discard;
    public final boolean viewOffset;
    public final float uOff, vOff;
    public final Format format;
    private final boolean flag;

    private RenderType(String name, ResourceLocation texture, Blend blend, boolean cull, boolean depthWrite, boolean lit, boolean sort, boolean depthEqual,
                       String discard, boolean viewOffset, float uOff, float vOff, Format format, boolean flag) {
        this.name = name;
        this.texture = texture;
        this.blend = blend;
        this.cull = cull;
        this.depthWrite = depthWrite;
        this.lit = lit;
        this.sort = sort;
        this.depthEqual = depthEqual;
        this.discard = discard;
        this.viewOffset = viewOffset;
        this.uOff = uOff;
        this.vOff = vOff;
        this.format = format;
        this.flag = flag;
    }

    private static RenderType entity(String name, ResourceLocation t, Blend blend, boolean cull, boolean depthWrite, boolean lit, boolean sort, String discard, boolean flag) {
        return new RenderType(name, Objects.requireNonNull(t, "texture"), blend, cull, depthWrite, lit, sort, false, discard, false, 0f, 0f, Format.ENTITY, flag);
    }

    // ---------------------------------------------------------------- opaque and cutout
    public static RenderType entitySolid(ResourceLocation t) { return entity("entity_solid", t, Blend.NONE, true, true, true, false, "none", false); }

    public static RenderType entityCutout(ResourceLocation t) { return entity("entity_cutout", t, Blend.NONE, true, true, true, false, "tex", false); }

    public static RenderType entityCutoutNoCull(ResourceLocation t, boolean affectsOutline) { return entity("entity_cutout_no_cull", t, Blend.NONE, false, true, true, false, "tex", affectsOutline); }

    public static RenderType entityCutoutNoCull(ResourceLocation t) { return entityCutoutNoCull(t, true); }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation t, boolean affectsOutline) {
        return new RenderType("entity_cutout_no_cull_z_offset", Objects.requireNonNull(t, "texture"), Blend.NONE, false, true, true, false, false, "tex", true, 0f, 0f, Format.ENTITY, affectsOutline);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation t) { return entityCutoutNoCullZOffset(t, true); }

    public static RenderType entitySmoothCutout(ResourceLocation t) { return entity("entity_smooth_cutout", t, Blend.NONE, false, true, true, false, "tex", false); }

    public static RenderType armorCutoutNoCull(ResourceLocation t) {
        return new RenderType("armor_cutout_no_cull", Objects.requireNonNull(t, "texture"), Blend.NONE, false, true, true, false, false, "tex", true, 0f, 0f, Format.ENTITY, false);
    }

    public static RenderType entityDecal(ResourceLocation t) {
        return new RenderType("entity_decal", Objects.requireNonNull(t, "texture"), Blend.NONE, false, true, true, false, true, "tex", false, 0f, 0f, Format.ENTITY, false);
    }

    // ---------------------------------------------------------------- alpha blended
    public static RenderType entityTranslucent(ResourceLocation t, boolean affectsOutline) { return entity("entity_translucent", t, Blend.TRANSLUCENT, false, true, true, true, "final", affectsOutline); }

    public static RenderType entityTranslucent(ResourceLocation t) { return entityTranslucent(t, true); }

    public static RenderType entityTranslucentCull(ResourceLocation t) { return entity("entity_translucent_cull", t, Blend.TRANSLUCENT, true, true, true, true, "final", false); }

    public static RenderType itemEntityTranslucentCull(ResourceLocation t) { return entity("item_entity_translucent_cull", t, Blend.TRANSLUCENT, true, true, true, true, "final", false); }

    public static RenderType entityTranslucentEmissive(ResourceLocation t, boolean affectsOutline) { return entity("entity_translucent_emissive", t, Blend.TRANSLUCENT, false, false, false, true, "final", affectsOutline); }

    public static RenderType entityTranslucentEmissive(ResourceLocation t) { return entityTranslucentEmissive(t, true); }

    public static RenderType entityNoOutline(ResourceLocation t) { return entity("entity_no_outline", t, Blend.TRANSLUCENT, false, false, true, true, "final", false); }

    // ---------------------------------------------------------------- additive
    public static RenderType eyes(ResourceLocation t) { return entity("eyes", t, Blend.ADDITIVE, true, false, false, true, "final", false); }

    public static RenderType energySwirl(ResourceLocation t, float u, float v) {
        return new RenderType("energy_swirl", Objects.requireNonNull(t, "texture"), Blend.ADDITIVE, false, true, true, true, false, "final", false, u, v, Format.ENTITY, false);
    }

    /** Untextured additive strips (vertex colour only; the vertex format has no uv, overlay, light or normal). */
    public static RenderType lightning() {
        return new RenderType("lightning", null, Blend.ADDITIVE, true, true, false, true, false, "none", false, 0f, 0f, Format.POSITION_COLOR, false);
    }

    // ---------------------------------------------------------------- identity
    @Override public boolean equals(Object o) {
        return this == o || (o instanceof RenderType r && name.equals(r.name) && Objects.equals(texture, r.texture) && flag == r.flag && uOff == r.uOff && vOff == r.vOff);
    }

    @Override public int hashCode() { return Objects.hash(name, texture, flag, uOff, vOff); }

    @Override public String toString() { return "RenderType[" + name + (texture == null ? "" : ":" + texture) + "]"; }
}
