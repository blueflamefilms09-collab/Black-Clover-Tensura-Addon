package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;

import java.util.EnumSet;
import java.util.Set;

/** 3.29.0: the unique Ultimate effect ULT_FINAL_DOSE (placeholder until its builder fills it in). */
public class UltFinalDoseLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_FINAL_DOSE); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFFFFF; }
    @Override public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {}
}
