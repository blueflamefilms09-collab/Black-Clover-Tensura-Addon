import com.newuniverse.nusmp.client.DemonSlayerMesh;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Dumps DemonSlayerMesh's quads (the real in-game mesh) in item space for tools/item_preview/preview_demon_slayer.py.
 * Args: resonance fracture time. One line per quad: layer, 4 x (x y z u v), normal (nx ny nz), argb.
 */
public final class MeshDump {
    public static void main(String[] args) {
        int res = Integer.parseInt(args[0]);
        float fracture = Float.parseFloat(args[1]), time = Float.parseFloat(args[2]);
        DemonSlayerMesh.draw(new Sink(), res, fracture, time);
    }

    /** A row-major 4x4 matrix stack. */
    static final class Sink implements DemonSlayerMesh.Sink {
        final Deque<double[]> stack = new ArrayDeque<>();
        double[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

        static double[] mul(double[] a, double[] b) {
            double[] r = new double[16];
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
                double s = 0;
                for (int k = 0; k < 4; k++) s += a[i * 4 + k] * b[k * 4 + j];
                r[i * 4 + j] = s;
            }
            return r;
        }

        public void push() { stack.push(m.clone()); }
        public void pop() { m = stack.pop(); }
        public void translate(float x, float y, float z) { m = mul(m, new double[]{1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1}); }
        public void rotateZ(float deg) { double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a); m = mul(m, new double[]{c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}); }
        public void rotateX(float deg) { double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a); m = mul(m, new double[]{1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1}); }
        public void scale(float s) { m = mul(m, new double[]{s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1}); }

        public void quad(int layer, float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz, int argb) {
            StringBuilder sb = new StringBuilder().append(layer);
            for (float[] k : new float[][]{a, b, c, d}) {
                double x = m[0] * k[0] + m[1] * k[1] + m[2] * k[2] + m[3], y = m[4] * k[0] + m[5] * k[1] + m[6] * k[2] + m[7], z = m[8] * k[0] + m[9] * k[1] + m[10] * k[2] + m[11];
                sb.append(String.format(Locale.ROOT, " %.5f %.5f %.5f %.4f %.4f", x, y, z, k[3], k[4]));
            }
            double tx = m[0] * nx + m[1] * ny + m[2] * nz, ty = m[4] * nx + m[5] * ny + m[6] * nz, tz = m[8] * nx + m[9] * ny + m[10] * nz;
            double l = Math.sqrt(tx * tx + ty * ty + tz * tz);
            sb.append(String.format(Locale.ROOT, " %.4f %.4f %.4f %d", tx / l, ty / l, tz / l, argb & 0xFFFFFFFFL));
            System.out.println(sb);
        }
    }
}
