package com.newuniverse.nusmp.vfx.client.portal;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Why: the shared effect buffer is how every other effect reaches the screen and how the headless preview sees quads, so the portal
 * can be drawn through it too. It is the path used when the portal shader is not available (a driver that cannot compile it, the
 * preview) and it makes the "fall back to plain translucent layers" requirement real: same shapes, plain textures, no refraction.
 * It is limited to the buffer's budget, so the caller picks a tier that fits.
 */
public final class PortalBufferSink implements PortalSink {
    private static final ResourceLocation[] TEX = new ResourceLocation[PortalPass.values().length];
    static {
        for (PortalPass pass : PortalPass.values()) TEX[pass.ordinal()] = VfxTextures.byName(pass.texture);
    }

    private final VfxVertexBuffer buf;
    private final Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f(), d = new Vector3f();

    public PortalBufferSink(VfxVertexBuffer buf) { this.buf = buf; }

    @Override
    public void quad(PortalPass pass, float[] p, float u0, float u1, float vInner, float vOuter, int cInner, int cOuter) {
        a.set(p[0], p[1], p[2]); b.set(p[3], p[4], p[5]); c.set(p[6], p[7], p[8]); d.set(p[9], p[10], p[11]);
        // VfxVertexBuffer.quad maps p0 -> (u0, v1) ... p2 -> (u1, v0): the inner edge takes its second v
        buf.quad(TEX[pass.ordinal()], pass.additive ? VfxBlend.ADD : VfxBlend.ALPHA, a, b, c, d, u0, vOuter, u1, vInner, cInner, cOuter);
    }
}
