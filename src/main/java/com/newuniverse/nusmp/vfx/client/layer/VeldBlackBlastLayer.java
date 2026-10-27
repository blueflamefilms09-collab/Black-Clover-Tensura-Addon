package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;

import java.util.EnumSet;
import java.util.Set;

/** 3.30.0: Veldanava's effect VELD_BLACK_BLAST (placeholder until its builder fills it in). */
public class VeldBlackBlastLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_BLACK_BLAST); }
    @Override public int defaultDuration(VfxShape s) { return 26; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFFFFF; }
    @Override public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {}
}
