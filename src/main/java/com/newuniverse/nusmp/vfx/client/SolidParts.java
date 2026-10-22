package com.newuniverse.nusmp.vfx.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Why (docs/visual/flat_props.md): the spells that used to float a flat white cut-out of a sword, an axe, a shield or a person now draw a small SOLID model, built from a few shaded parts (boxes,
 * pyramids, bevelled plates), through the same VFX vertex buffer. A model is a handful of faces of the material sheet {@code textures/flatprops/solid.png} (steel, gold, bone, glass, oak, ...),
 * each lit by a fixed sun direction so it reads as a body with a lit side and a dark side, never as a flat shape.
 *
 * <p>Built for the buffer's limits: only the faces that look at the camera are kept (a back face is dropped when the part is added, so a box costs three quads, not six), the kept faces are drawn far to
 * near through ONE texture and one blend, so they overlap correctly without a depth buffer of their own, and a model that does not fit the instance's remaining vertex budget is skipped whole (never
 * half a sword), and one further than {@link #MAX_DISTANCE} blocks is not built. One shared scratch model is reused every frame: nothing is allocated per face or per draw. Client render thread only; no Minecraft client class is named, so the headless preview can run it.
 *
 * <p>Use: {@code begin(centre, yaw, tilt, scale, alpha)}, then {@code box / pyramid / plate}, then {@code draw(buf)}. Coordinates of the parts are local (x right, y up along the object, z towards
 * the viewer when yaw = 0), in blocks before the scale; the camera is at the origin of the world space the layers work in.
 */
public final class SolidParts {
    private SolidParts() {}

    public static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/flatprops/solid.png");
    /** Material cells of the sheet (4 x 2). */
    public static final int STEEL = 0, GOLD = 1, BONE = 2, GLASS = 3, OAK = 4, DARK = 5, LEATHER = 6, SILVER = 7;

    private static final float INSET = 1.5f / 128f;                      // keep a texel and a half off the cell edge: no bleeding from the next material
    private static final Vector3f SUN = new Vector3f(0.35f, 0.85f, 0.40f).normalize();
    private static final int MAX_FACES = 100;                           // the buffer holds 100 quads per effect at most (400 vertices): a model can never be bigger
    /** Beyond this distance (blocks) a model is not built at all (distance culling: far casters cost nothing). */
    public static final float MAX_DISTANCE = 40f;

    private static final Model MODEL = new Model();

    /** The shared scratch model (render thread only). */
    public static Model begin(Vector3f centre, float yaw, float tilt, float scale, float alpha) {
        return MODEL.reset(centre, yaw, tilt, scale, alpha);
    }

    public static final class Model {
        private final Vector3f o = new Vector3f();
        private final Matrix3f basis = new Matrix3f();
        private float scale = 1f, alpha = 1f;
        private int count;
        private final float[] pts = new float[MAX_FACES * 12];           // four vertices (x, y, z) per kept face, camera-relative
        private final float[] key = new float[MAX_FACES];                 // squared distance of the face centre
        private final int[] colour = new int[MAX_FACES];
        private final int[] material = new int[MAX_FACES];
        private final float[] tmp = new float[12];
        private final float[] lc = new float[3];
        private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();     // scratch for draw: nothing is allocated per frame
        private boolean culled;

        /** True when the model is too far away (or fully faded) to be worth building: callers may skip their own work too. */
        public boolean culled() { return culled; }

        Model reset(Vector3f centre, float yaw, float tilt, float scale, float alpha) {
            culled = alpha <= 0.02f || centre.lengthSquared() > MAX_DISTANCE * MAX_DISTANCE;
            o.set(centre);
            basis.identity().rotateY(yaw).rotateZ(tilt);
            this.scale = scale;
            this.alpha = alpha;
            count = 0;
            return this;
        }

        /** How many faces the model keeps so far (the vertices it will draw are four times this). */
        public int faces() { return count; }

        /** Local point -> camera-relative world point, written into out[k..k+2]. */
        private void world(float x, float y, float z, float[] out, int k) {
            float wx = basis.m00 * x + basis.m10 * y + basis.m20 * z;
            float wy = basis.m01 * x + basis.m11 * y + basis.m21 * z;
            float wz = basis.m02 * x + basis.m12 * y + basis.m22 * z;
            out[k] = o.x + wx * scale;
            out[k + 1] = o.y + wy * scale;
            out[k + 2] = o.z + wz * scale;
        }

        /**
         * One face from four local points; {@code cx, cy, cz} is the local centre of the part it belongs to (to point the normal outward). Kept only when it looks at the camera.
         */
        private void face(float ax, float ay, float az, float bx, float by, float bz, float cx2, float cy2, float cz2, float dx, float dy, float dz,
                          float pcx, float pcy, float pcz, int mat, int tint) {
            if (culled || count >= MAX_FACES) return;
            world(ax, ay, az, tmp, 0);
            world(bx, by, bz, tmp, 3);
            world(cx2, cy2, cz2, tmp, 6);
            world(dx, dy, dz, tmp, 9);
            float ux = tmp[3] - tmp[0], uy = tmp[4] - tmp[1], uz = tmp[5] - tmp[2];
            float vx = tmp[9] - tmp[0], vy = tmp[10] - tmp[1], vz = tmp[11] - tmp[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-9f) {                                           // a triangle written as (a, b, c, c): use the other pair
                vx = tmp[6] - tmp[0]; vy = tmp[7] - tmp[1]; vz = tmp[8] - tmp[2];
                nx = uy * vz - uz * vy; ny = uz * vx - ux * vz; nz = ux * vy - uy * vx;
                len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len < 1e-9f) return;
            }
            nx /= len; ny /= len; nz /= len;
            float mx = (tmp[0] + tmp[3] + tmp[6] + tmp[9]) * 0.25f, my = (tmp[1] + tmp[4] + tmp[7] + tmp[10]) * 0.25f, mz = (tmp[2] + tmp[5] + tmp[8] + tmp[11]) * 0.25f;
            world(pcx, pcy, pcz, lc, 0);
            if (nx * (mx - lc[0]) + ny * (my - lc[1]) + nz * (mz - lc[2]) < 0f) { nx = -nx; ny = -ny; nz = -nz; }     // outward
            if (nx * mx + ny * my + nz * mz >= 0f) return;                                                              // turned away from the camera at the origin
            float lit = Math.max(0f, nx * SUN.x + ny * SUN.y + nz * SUN.z);
            float shade = 0.46f + 0.54f * lit;
            int a = Mth.clamp((int) (alpha * (tint >>> 24) ), 0, 255);
            int r = Mth.clamp((int) (shade * ((tint >> 16) & 255)), 0, 255), g = Mth.clamp((int) (shade * ((tint >> 8) & 255)), 0, 255), b = Mth.clamp((int) (shade * (tint & 255)), 0, 255);
            int i = count++;
            System.arraycopy(tmp, 0, pts, i * 12, 12);
            key[i] = mx * mx + my * my + mz * mz;
            colour[i] = (a << 24) | (r << 16) | (g << 8) | b;
            material[i] = mat;
        }

        /** An axis-aligned box of the local frame. */
        public Model box(float x0, float y0, float z0, float x1, float y1, float z1, int mat, int tint) {
            float cx = (x0 + x1) * 0.5f, cy = (y0 + y1) * 0.5f, cz = (z0 + z1) * 0.5f;
            face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, cx, cy, cz, mat, tint);      // up
            face(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, cx, cy, cz, mat, tint);      // down
            face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, cx, cy, cz, mat, tint);      // -z
            face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, cx, cy, cz, mat, tint);      // +z
            face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, cx, cy, cz, mat, tint);      // -x
            face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, cx, cy, cz, mat, tint);      // +x
            return this;
        }

        /** A four-sided pyramid: a rectangular base of half-sizes (hx, hz) at height y0 and its apex at (ax, y1, az). */
        public Model pyramid(float cx, float y0, float cz, float hx, float hz, float ax, float y1, float az, int mat, int tint) {
            float pcx = (cx + ax) * 0.5f, pcy = (y0 + y1) * 0.5f, pcz = (cz + az) * 0.5f;
            float x0 = cx - hx, x1 = cx + hx, z0 = cz - hz, z1 = cz + hz;
            face(x0, y0, z1, x1, y0, z1, ax, y1, az, ax, y1, az, pcx, pcy, pcz, mat, tint);     // +z
            face(x1, y0, z0, x0, y0, z0, ax, y1, az, ax, y1, az, pcx, pcy, pcz, mat, tint);     // -z
            face(x0, y0, z0, x0, y0, z1, ax, y1, az, ax, y1, az, pcx, pcy, pcz, mat, tint);     // -x
            face(x1, y0, z1, x1, y0, z0, ax, y1, az, ax, y1, az, pcx, pcy, pcz, mat, tint);     // +x
            return this;
        }

        /**
         * A wedge (an axe head): the spine at local x = {@code sx}, y from sy0 to sy1 and {@code shz} half thick in z, narrowing to a sharp edge line at x = {@code ex} from ey0 to ey1. The edge may be
         * longer than the spine (a flared bit) and lie on either side of the spine.
         */
        public Model wedge(float sx, float sy0, float sy1, float shz, float ex, float ey0, float ey1, int mat, int tint) {
            float pcx = (sx + ex) * 0.5f, pcy = (sy0 + sy1 + ey0 + ey1) * 0.25f;
            face(sx, sy0, shz, ex, ey0, 0f, ex, ey1, 0f, sx, sy1, shz, pcx, pcy, 0f, mat, tint);              // the front slope
            face(sx, sy0, -shz, sx, sy1, -shz, ex, ey1, 0f, ex, ey0, 0f, pcx, pcy, 0f, mat, tint);            // the back slope
            face(sx, sy1, shz, ex, ey1, 0f, sx, sy1, -shz, sx, sy1, -shz, pcx, pcy, 0f, mat, tint);           // the top
            face(sx, sy0, -shz, ex, ey0, 0f, sx, sy0, shz, sx, sy0, shz, pcx, pcy, 0f, mat, tint);            // the bottom
            face(sx, sy0, shz, sx, sy0, -shz, sx, sy1, -shz, sx, sy1, shz, pcx, pcy, 0f, mat, tint);          // the spine
            return this;
        }

        /**
         * A bevelled plate: a convex outline (x, y pairs, counter-clockwise) extruded {@code t} thick along local z (centred). The outline rim is chamfered towards a smaller outline
         * (scaled by {@code 1 - bevel} about the outline's centre) on both faces, so the plate catches light on its edges like a forged blade or a shield rim.
         */
        public Model plate(float[] outline, float z, float t, float bevel, int mat, int tint) {
            int n = outline.length / 2;
            float mx = 0f, my = 0f;
            for (int i = 0; i < n; i++) { mx += outline[2 * i]; my += outline[2 * i + 1]; }
            mx /= n; my /= n;
            float inner = 1f - bevel, h = t * 0.5f, edge = h * 0.35f;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ox0 = outline[2 * i], oy0 = outline[2 * i + 1], ox1 = outline[2 * j], oy1 = outline[2 * j + 1];
                float ix0 = mx + (ox0 - mx) * inner, iy0 = my + (oy0 - my) * inner, ix1 = mx + (ox1 - mx) * inner, iy1 = my + (oy1 - my) * inner;
                for (int s = -1; s <= 1; s += 2) {
                    float zf = z + s * h, ze = z + s * edge;
                    face(mx, my, zf, ix0, iy0, zf, ix1, iy1, zf, ix1, iy1, zf, mx, my, z, mat, tint);          // the flat middle (a triangle of the fan)
                    face(ix0, iy0, zf, ox0, oy0, ze, ox1, oy1, ze, ix1, iy1, zf, mx, my, z, mat, tint);        // the chamfer
                }
                face(ox0, oy0, z - edge, ox1, oy1, z - edge, ox1, oy1, z + edge, ox0, oy0, z + edge, mx, my, z, mat, tint);   // the edge band
            }
            return this;
        }

        /** Draws the kept faces far to near, all through the material sheet; nothing at all when the instance's budget cannot hold them. */
        public boolean draw(VfxVertexBuffer buf) {
            if (count == 0) return true;
            if (!buf.hasBudget(count * 4)) return false;
            for (int i = 1; i < count; i++) {                           // insertion sort on the squared distance, far first (few faces)
                int j = i;
                while (j > 0 && key[j - 1] < key[j]) { swap(j - 1, j); j--; }
            }
            for (int i = 0; i < count; i++) {
                int k = i * 12;
                p0.set(pts[k], pts[k + 1], pts[k + 2]);
                p1.set(pts[k + 3], pts[k + 4], pts[k + 5]);
                p2.set(pts[k + 6], pts[k + 7], pts[k + 8]);
                p3.set(pts[k + 9], pts[k + 10], pts[k + 11]);
                int cx = material[i] & 3, cy = (material[i] >> 2) & 1;
                float u0 = cx * 0.25f + INSET, u1 = (cx + 1) * 0.25f - INSET, v0 = cy * 0.5f + INSET * 2f, v1 = (cy + 1) * 0.5f - INSET * 2f;
                buf.quad(SHEET, VfxBlend.ALPHA, p0, p1, p2, p3, u0, v0, u1, v1, colour[i], colour[i]);
            }
            return true;
        }

        private void swap(int a, int b) {
            for (int k = 0; k < 12; k++) { float t = pts[a * 12 + k]; pts[a * 12 + k] = pts[b * 12 + k]; pts[b * 12 + k] = t; }
            float tk = key[a]; key[a] = key[b]; key[b] = tk;
            int tc = colour[a]; colour[a] = colour[b]; colour[b] = tc;
            int tm = material[a]; material[a] = material[b]; material[b] = tm;
        }
    }
}
