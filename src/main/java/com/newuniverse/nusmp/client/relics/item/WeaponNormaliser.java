package com.newuniverse.nusmp.client.relics.item;

import java.util.List;
import java.util.Set;

/**
 * The per-game safety net for weapon meshes (docs/weapon_scale_orientation.md): a pure normaliser WeaponRenderer runs once on every freshly loaded mesh.
 * It never touches a mesh marked {@code orient manual} (that is the caller's check) and it only acts on clear faults:
 * <ul>
 *   <li><b>tip down</b>: the place line points the weapon down the sprite, or a head-heavy class has its weight at the butt: turned over;</li>
 *   <li><b>head toward the player</b>: a class whose edge or head is on one side (axe, crescent, hammer ...) has its mass on +x: mirrored across
 *       the haft axis, so the edge faces forward (see {@link WeaponOrientation});</li>
 *   <li><b>grip at the far end</b> (1.34.0): a sword-like mesh whose hilt is at the high-y end (the Windcutter of the owner's screenshot 68: the hand held the
 *       blade), decided by the mesh's own {@code grip <fraction>} directive when it has one, else by the geometry ({@link WeaponOrientation#gripAtFarEnd}):
 *       turned over along its length (a half turn about the x axis, so the edge keeps its side);</li>
 *   <li><b>too small</b>: the apparent length (mesh length times the third-person display scale) is below {@link WeaponClasses#RUNTIME_SMALL} of the
 *       class minimum (a held item: its largest dimension below that share of {@link WeaponClasses#ITEM_MIN_EXTENT}): scaled up to the minimum
 *       about the point the hand holds.</li>
 * </ul>
 * The offline fixes (tools/relics/weapon_fixes.json) bring the shipped weapons right; this only catches a weapon that arrives wrong later. Running
 * it on a corrected mesh changes nothing.
 */
public final class WeaponNormaliser {
    private WeaponNormaliser() {}

    /** Largest scale-up the normaliser applies (a mesh more than four times too small is a broken file, not a size problem). */
    public static final float MAX_SCALE_UP = 4f;

    /** What happened: {@code log} is empty when nothing did. {@code gripFlipped} (1.34.0, appended): the mesh was turned over along its length. */
    public record Result(boolean changed, boolean rotated, boolean flipped, float scaleFactor, String log, boolean gripFlipped) {
        public Result(boolean changed, boolean rotated, boolean flipped, float scaleFactor, String log) {
            this(changed, rotated, flipped, scaleFactor, log, false);
        }
    }

    private static final Result NOTHING = new Result(false, false, false, 1f, "");

    /**
     * Normalises a mesh in place.
     *
     * @param layers  the vertex lists per layer (0 body, 1 void, 2 glow), eight floats per vertex (x y z u v nx ny nz), four vertices per quad
     * @param place   {ox, oy, angle, scale, ymin} of the mesh's place line; updated when the weapon is scaled or turned
     * @param clsName the class name of the weapon (see {@link WeaponClasses})
     * @param cls     its class row
     * @param thirdPersonScale the model json's thirdperson_righthand scale (0.85 when there is none)
     */
    public static Result normalise(List<float[]>[] layers, float[] place, String clsName, WeaponClasses.Cls cls, float thirdPersonScale) {
        return normalise(layers, place, clsName, cls, thirdPersonScale, Float.NaN);
    }

    /** As above for a mesh with a {@code grip <fraction>} directive ({@code gripDirective}, NaN when it has none). */
    public static Result normalise(List<float[]>[] layers, float[] place, String clsName, WeaponClasses.Cls cls, float thirdPersonScale, float gripDirective) {
        List<float[]> body = layers[0].isEmpty() ? layers[2] : layers[0];
        if (body.isEmpty() || cls == null || !(place[3] > 0f) || !(thirdPersonScale > 0f)) return NOTHING;
        WeaponOrientation.Measures m = WeaponOrientation.measure(body, place[3]);
        if (m.empty) return NOTHING;
        boolean item = WeaponClasses.isItemClass(clsName);
        boolean rotated = false, flipped = false, gripFlipped = false;
        StringBuilder log = new StringBuilder();
        Set<WeaponOrientation.Flag> flags = WeaponOrientation.detect(m, clsName, place[2], gripDirective);
        if (flags.contains(WeaponOrientation.Flag.TIP_DOWN)) {
            if (Math.sin(Math.toRadians(place[2])) < -0.1) {
                place[2] = wrap(place[2] + 180f);
                log.append("tip pointed down the sprite: turned over");
            } else {
                double pivot = (m.y0 + m.y1) / 2.0;
                for (List<float[]> layer : layers) WeaponOrientation.rotateZ(layer, 180.0, pivot);
                log.append("head at the butt: turned over");
            }
            rotated = true;
            m = WeaponOrientation.measure(body, place[3]);
        } else if (flags.contains(WeaponOrientation.Flag.GRIP_FAR_END)) {
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;                       // the pivot over every layer, as the offline tool does
            for (List<float[]> layer : layers) for (float[] v : layer) { lo = Math.min(lo, v[1]); hi = Math.max(hi, v[1]); }
            for (List<float[]> layer : layers) WeaponOrientation.flipY(layer, (lo + hi) / 2.0);
            gripFlipped = true;
            log.append("grip at the far end: turned over along its length");
            m = WeaponOrientation.measure(body, place[3]);
        }
        if (WeaponOrientation.hasTrait(clsName, "handed") && m.imbalance > WeaponOrientation.IMBALANCE_MIN) {
            for (List<float[]> layer : layers) WeaponOrientation.mirrorX(layer, m.axisX);
            flipped = true;
            if (log.length() > 0) log.append("; ");
            log.append("head toward the player: mirrored");
        }
        float ymax = -Float.MAX_VALUE, xmin = Float.MAX_VALUE, xmax = -Float.MAX_VALUE, zmin = Float.MAX_VALUE, zmax = -Float.MAX_VALUE;
        for (List<float[]> layer : layers) {
            for (float[] v : layer) ymax = Math.max(ymax, v[1]);
        }
        for (float[] v : body) {
            xmin = Math.min(xmin, v[0]);
            xmax = Math.max(xmax, v[0]);
            zmin = Math.min(zmin, v[2]);
            zmax = Math.max(zmax, v[2]);
        }
        float length = Math.max(0f, (ymax - place[4]) * place[3]);                       // blocks, as WeaponRenderer computes it
        float apparent = length * thirdPersonScale;
        float k = 1f;
        if (item) {
            float extent = Math.max(apparent, Math.max((xmax - xmin) * place[3], (zmax - zmin) * place[3]) * thirdPersonScale);
            if (extent > 0f && extent < WeaponClasses.RUNTIME_SMALL * WeaponClasses.ITEM_MIN_EXTENT) k = WeaponClasses.ITEM_MIN_EXTENT / extent;
        } else if (apparent > 0f && apparent < WeaponClasses.RUNTIME_SMALL * cls.min()) {
            k = cls.min() / apparent;
        }
        k = Math.min(k, MAX_SCALE_UP);
        if (k > 1.0001f) {
            double a = Math.toRadians(place[2]);
            place[0] += (float) (Math.cos(a) * cls.grip() * length * (1f - k));              // grow about the grip, not the butt
            place[1] += (float) (Math.sin(a) * cls.grip() * length * (1f - k));
            place[3] *= k;
            if (log.length() > 0) log.append("; ");
            log.append(String.format(java.util.Locale.ROOT, "too small (%.2f blocks, class %s needs %.2f): scaled x%.2f", apparent, clsName, cls.min(), k));
        } else {
            k = 1f;
        }
        if (!rotated && !flipped && !gripFlipped && k == 1f) return NOTHING;
        return new Result(true, rotated, flipped, k, log.toString(), gripFlipped);
    }

    private static float wrap(float deg) {
        float d = deg % 360f;
        if (d > 180f) d -= 360f;
        if (d <= -180f) d += 360f;
        return d;
    }
}
