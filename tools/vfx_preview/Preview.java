import com.newuniverse.nusmp.vfx.FlatProps;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxShake;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.world.phys.Vec3;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Headless VFX preview: runs a real layer's render() against the preview stubs and writes the quads it emits (camera-relative,
 * with texture, blend and vertex colours) as JSON for preview.py to rasterise.
 *
 * args: --layer CLASS --shape NAME --from x,y,z --to x,y,z --power P --duration D --color AARRGGBB|0 --seed N
 *       [--flat 1 draws the old flat cut-outs, visual.flatProps on]  --cam x,y,z --target x,y,z --ages a,b,c --detail 1 [--budget N] --out FILE
 */
public final class Preview {
    public static void main(String[] a) throws Exception {
        Map<String, String> o = new HashMap<>();
        for (int i = 0; i + 1 < a.length; i += 2) o.put(a[i], a[i + 1]);
        FlatProps.setDrawFlat("1".equals(o.get("--flat")));                   // a scene may ask for the old flat cut-outs (the before picture)
        com.newuniverse.nusmp.vfx.WindMc.setStyle(com.newuniverse.nusmp.vfx.WindMc.parse(o.get("--style")));      // the wind pixel look (default MC_C) or V1, the old wind layers
        com.newuniverse.nusmp.vfx.WaterBc.setStyle(com.newuniverse.nusmp.vfx.WaterBc.parse(o.get("--water")));    // the water hybrid (default BC) or V1, the old water layers
        com.newuniverse.nusmp.vfx.WaterBc.forceTier(Integer.parseInt(o.getOrDefault("--tier", "-1")));            // a water tier forced for the picture (0 full mesh, 1 light mesh, 2 flat sprites)
        com.newuniverse.nusmp.vfx.FireV2.setStyle(com.newuniverse.nusmp.vfx.FireV2.parse(o.get("--fire-style")));   // the fire V2 look (default) or V1, the old fire layers
        com.newuniverse.nusmp.vfx.FireV2.setReducedFlashes("1".equals(o.get("--reduced")));                          // the reduced-flashes option of the fire look
        com.newuniverse.nusmp.vfx.StarV2.setStyle(com.newuniverse.nusmp.vfx.StarV2.parse(o.get("--star-style")));    // the star V2 look (default) or V1, the old starlight beam
        com.newuniverse.nusmp.vfx.StarV2.setReducedFlashes("1".equals(o.get("--reduced")));                          // the reduced-flashes option of the star look
        AbstractVfxLayer layer = (AbstractVfxLayer) Class.forName(o.get("--layer")).getDeclaredConstructor().newInstance();
        VfxShape shape = VfxShape.valueOf(o.get("--shape"));
        Vec3 from = vec(o.getOrDefault("--from", "0,0,0")), to = vec(o.getOrDefault("--to", "0,1,0"));
        float power = Float.parseFloat(o.getOrDefault("--power", "1"));
        int color = (int) Long.parseLong(o.getOrDefault("--color", "0"), 16);
        if (color == 0) color = layer.defaultColor(shape);
        int duration = Integer.parseInt(o.getOrDefault("--duration", "0"));
        if (duration <= 0) duration = layer.defaultDuration(shape);
        long seed = Long.parseLong(o.getOrDefault("--seed", "12345"));
        Vec3 cam = vec(o.get("--cam")), target = vec(o.get("--target"));
        float detail = Float.parseFloat(o.getOrDefault("--detail", "1"));

        VfxPayload payload = new VfxPayload(shape.ordinal(), from, to, color, duration, power, "1".equals(o.get("--follow")) ? 1 : -1, seed);        // --follow 1: an effect that follows its caster (the water Cradle)
        VfxInstance inst = new VfxInstance(payload, layer, color, duration);
        layer.onSpawn(inst);
        VfxVertexBuffer buf = new VfxVertexBuffer();
        StringBuilder sb = new StringBuilder("{\"duration\":" + duration + ",\"frames\":[");
        boolean firstFrame = true;
        for (String ageStr : o.get("--ages").split(",")) {
            float age = Float.parseFloat(ageStr);
            inst.setAge((int) Math.floor(age));
            float partial = age - (float) Math.floor(age);
            VfxRenderContext ctx = new VfxRenderContext(cam, target, partial, detail);
            buf.beginActivation(o.containsKey("--budget") ? Integer.parseInt(o.get("--budget")) : layer.vertexBudget(shape));
            com.newuniverse.nusmp.vfx.client.WaterBcFrame.plan(new ArrayList<>(), new double[0], new float[0], new boolean[0], 0, 40000);      // empties the water mesh buffer and the sprite allowance of the last frame
            layer.render(inst, ctx, buf);
            List<Object[]> quads = buf.drain();
            if (!firstFrame) sb.append(',');
            firstFrame = false;
            sb.append("{\"age\":").append(age).append(",\"vertices\":").append(quads.size() * 4).append(",\"mesh\":").append(meshJson()).append(",\"quads\":[");
            for (int q = 0; q < quads.size(); q++) {
                Object[] e = quads.get(q);
                float[] d = (float[]) e[3];
                if (q > 0) sb.append(',');
                sb.append("{\"t\":\"").append(e[0]).append("\",\"b\":\"").append(e[1]).append("\",\"o\":").append(e[2]).append(",\"v\":[");
                for (int k = 0; k < 4; k++) {
                    if (k > 0) sb.append(',');
                    int c = Float.floatToRawIntBits(d[k * 6 + 5]);
                    sb.append(String.format(Locale.ROOT, "[%.4f,%.4f,%.4f,%.4f,%.4f,%d]", d[k * 6], d[k * 6 + 1], d[k * 6 + 2], d[k * 6 + 3], d[k * 6 + 4], c & 0xFFFFFFFFL));
                }
                sb.append("]}");
            }
            sb.append("]}");
        }
        sb.append("],\"shake\":").append(VfxShake.last).append("}");
        try (FileWriter w = new FileWriter(o.get("--out"))) { w.write(sb.toString()); }
    }

    /** The 3D cores the water hybrid put in its mesh buffer this frame (toon shaded, camera relative): [{"k": pass 0 solid / 1 translucent / 2 glow, "c": argb, "p": 12 floats}] */
    private static String meshJson() {
        com.newuniverse.nusmp.vfx.WaterBcMeshBuffer mb = com.newuniverse.nusmp.vfx.WaterBcMeshBuffer.FRAME;
        StringBuilder m = new StringBuilder("[");
        boolean first = true;
        for (int pass = 0; pass < com.newuniverse.nusmp.vfx.WaterBcMeshBuffer.PASSES; pass++) {
            float[] d = mb.data(pass);
            int[] c = mb.colours(pass);
            for (int q = 0; q < mb.count(pass); q++) {
                if (!first) m.append(',');
                first = false;
                m.append("{\"k\":").append(pass).append(",\"c\":").append(c[q] & 0xFFFFFFFFL).append(",\"p\":[");
                for (int i = 0; i < 12; i++) m.append(i > 0 ? "," : "").append(String.format(Locale.ROOT, "%.4f", d[q * 12 + i]));
                m.append("]}");
            }
        }
        mb.reset();
        return m.append("]").toString();
    }

    private static Vec3 vec(String s) {
        String[] p = s.split(",");
        return new Vec3(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
    }
}
