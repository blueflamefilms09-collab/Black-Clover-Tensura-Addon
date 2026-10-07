package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 0.53 Crystal Magic: STUB (replaced by the attribute's own implementation). CRYSTAL_FX1, CRYSTAL_FX2 and CRYSTAL_FX3 are three effects of this
 * magic; document each one here (what it looks like, what from / to / power mean).
 */
public class CrystalLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CRYSTAL_FX1, VfxShape.CRYSTAL_FX2, VfxShape.CRYSTAL_FX3); }
    @Override public int defaultDuration(VfxShape s) { return 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFFA0E0FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        Vector3f c = ctx.rel(inst.from(ctx));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 1.2f * Math.max(0.5f, inst.power), 0f, VfxVertexBuffer.withAlpha(inst.color, fade));
    }
}
