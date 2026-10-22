import com.newuniverse.nusmp.compass3d.CompassMesh;

import java.util.Locale;

/**
 * Dumps the compass mesh (the real in-game geometry, compass3d/CompassMesh) for tools/compass/preview_compass3d.py.
 * One line per quad: part, layer, 4 x (x y z u v), normal (nx ny nz).
 */
public final class MeshDump {
    public static void main(String[] args) {
        String[] parts = {"rim", "face", "star", "needle", "glass", "arrow", "bar", "chevron"};
        int[] layers = {CompassMesh.RIM, CompassMesh.FACE, CompassMesh.STAR, CompassMesh.NEEDLE, CompassMesh.GLASS, CompassMesh.NEEDLE, CompassMesh.NEEDLE, CompassMesh.NEEDLE};
        for (int p = 0; p < parts.length; p++) {
            float[] q = CompassMesh.raw(parts[p]);
            for (int o = 0; o + CompassMesh.STRIDE <= q.length; o += CompassMesh.STRIDE) {
                StringBuilder sb = new StringBuilder(parts[p]).append(' ').append(layers[p]);
                for (int k = 0; k < CompassMesh.STRIDE; k++) sb.append(String.format(Locale.ROOT, " %.5f", q[o + k]));
                System.out.println(sb);
            }
        }
    }
}
