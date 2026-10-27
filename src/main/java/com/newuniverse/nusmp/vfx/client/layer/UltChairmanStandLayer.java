package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;

import java.util.EnumSet;
import java.util.Set;

/** 3.29.0: the unique Ultimate effect ULT_CHAIRMAN_STAND (placeholder until its builder fills it in). */
public class UltChairmanStandLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_CHAIRMAN_STAND); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFFFFF; }
    @Override public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {}
}
