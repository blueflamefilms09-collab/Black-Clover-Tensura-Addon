import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.PrintWriter;
import java.util.Random;

/**
 * Headless harness for the VFX vertex buffer (docs/vfx_square_bug.md). Runs one scenario against whichever VfxVertexBuffer is on the class path (the one of 1.93 or the current one) with fake
 * RenderSystem / BufferBuilder classes that only record, and writes everything a draw would have seen: the texture, the blend, and every vertex bit for bit with its RGBA bytes.
 *
 * Frame 0, 1, 3: random effects (all shapes, six textures, every blend, texture objects that are equal but not identical). Frame 2: the state is dirtied first the way foreign code can leave it
 * (colour modulator, blend off). Frame 4: a colour sweep through quad() that checks that no alpha or colour byte is lost on the way (all 256 alphas, every red that makes the packed float a
 * signalling NaN).
 */
public final class Harness {
    private static final String[] NAMES = {"glow", "flamefx_ring", "air_ring", "magic_circle", "spark", "time_sparkle"};

    private static ResourceLocation tex(int i) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/" + NAMES[i] + ".png"); }   // a new object every call, equal by value

    private static int randomColour(Random r) { return (r.nextInt(256) << 24) | r.nextInt(0x1000000); }

    private static Vector3f v(Random r, float s) { return new Vector3f((r.nextFloat() - 0.5f) * s, (r.nextFloat() - 0.5f) * s, (r.nextFloat() - 0.5f) * s - 3f); }

    public static void main(String[] args) throws Exception {
        {
            VfxVertexBuffer buf = new VfxVertexBuffer();
            Camera cam = new Camera();
            Matrix4f mv = new Matrix4f().rotateX(0.3f).rotateY(0.7f).translate(0.1f, -0.2f, 0.3f);
            VfxRenderContext ctx = new VfxRenderContext(cam, mv, 0.5f, new ClientLevel(), 1f);
            VfxBlend[] blends = VfxBlend.values();
            for (int frame = 0; frame < 5; frame++) {
                Random r = new Random(1000 + frame);
                if (frame == 2) {                                                         // stale state from some other renderer
                    RenderSystem.setShaderColor(0.25f, 0.25f, 0.25f, 1f);
                    RenderSystem.disableBlend();
                    RenderSystem.disableCull();
                    RenderSystem.depthMask(true);
                    RenderSystem.LOG.add("--- dirtied state");
                }
                RenderSystem.LOG.add("=== frame " + frame);
                if (frame == 4) {
                    for (int a = 0; a < 256; a++) {
                        buf.beginActivation(400);
                        int c0 = (a << 24) | (0x80 + (a & 0x3F)) << 16 | (a * 7 & 255) << 8 | (a * 13 & 255), c1 = (a << 24) | (a * 5 & 255) << 16 | 0x00FF00 | (a & 255);
                        Vector3f p = new Vector3f(0, 0, -2);
                        buf.quad(tex(0), VfxBlend.ALPHA, p, new Vector3f(1, 0, -2), new Vector3f(1, 1, -2), new Vector3f(0, 1, -2), 0, 0, 1, 1, c0, c1);
                        buf.quad(tex(1), VfxBlend.ADD, p, new Vector3f(1, 0, -2), new Vector3f(1, 1, -2), new Vector3f(0, 1, -2), 0, 0, 1, 1, c0, c1);
                    }
                    buf.flush(mv);
                    continue;
                }
                for (int act = 0; act < 40; act++) {
                    buf.beginActivation(40 + r.nextInt(361));
                    int shapes = 1 + r.nextInt(4);
                    for (int s = 0; s < shapes; s++) {
                        ResourceLocation t = tex(r.nextInt(NAMES.length));
                        VfxBlend b = blends[r.nextInt(blends.length)];
                        int col = randomColour(r), col2 = randomColour(r);
                        VfxPose pose = VfxPose.ground(v(r, 4f)).spin(r.nextFloat() * 6f);
                        switch (r.nextInt(6)) {
                            case 0 -> buf.billboard(ctx, t, b, v(r, 6f), 0.2f + r.nextFloat() * 2f, r.nextFloat() * 6f, col);
                            case 1 -> buf.plane(t, b, pose, 0.5f + r.nextFloat() * 2f, col);
                            case 2 -> buf.ring(t, b, pose, 0.4f, 1.1f + r.nextFloat(), 8 + r.nextInt(40), 2f + r.nextInt(5), r.nextFloat(), col);
                            case 3 -> buf.beam(ctx, t, b, v(r, 5f), v(r, 5f), 0.3f, 0.1f + r.nextFloat(), 3 + r.nextInt(30), r.nextFloat(), col, col2);
                            case 4 -> buf.arc(t, b, pose, 1f + r.nextFloat(), 0.4f + r.nextFloat(), r.nextFloat() * 6f, 1f + r.nextFloat() * 4f, 6 + r.nextInt(30), col);
                            default -> buf.quad(t, b, v(r, 3f), v(r, 3f), v(r, 3f), v(r, 3f), 0, 0, 1, 1, col, col2);
                        }
                    }
                }
                buf.flush(mv);
            }
            try (PrintWriter out = new PrintWriter(args[0], "UTF-8")) {
                for (String line : RenderSystem.LOG) out.println(line);
            }
        }
    }
}
