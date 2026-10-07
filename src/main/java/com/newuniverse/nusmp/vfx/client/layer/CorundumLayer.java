package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 0.53 Corundum Magic: STUB (replaced by the attribute's own implementation). CORUNDUM_FX1, CORUNDUM_FX2 and CORUNDUM_FX3 are three effects of this
 * magic; document each one here (what it looks like, what from / to / power mean).
 */
public class CorundumLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CORUNDUM_FX1, VfxShape.CORUNDUM_FX2, VfxShape.CORUNDUM_FX3); }
    @Override public int defaultDuration(VfxShape s) { return 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFF6A7A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        Vector3f c = ctx.rel(inst.from(ctx));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 1.2f * Math.max(0.5f, inst.power), 0f, VfxVertexBuffer.withAlpha(inst.color, fade));
    }
}
