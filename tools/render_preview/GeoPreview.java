import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.AuraPainter;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropPainter;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The geo.json half of the preview: a geo scene is a prop scene (space PROP) or an aura scene (space PLAYER) whose painter is the one line
 * the game's code would write, {@code GeoDraw.paint(pose, buffers, spec, anim, seconds, space, layer, light, argb, glowArgb)}, so the REAL
 * GeoModelData / GeoAnim / GeoDraw / GeoSpec / GeoModels run, through MagicPropRenderer or PlayerAuraLayer (the real glue) and the preview's
 * vanilla layer-space pose. GeoModels reads the files from disk (GeoModels.setSource) instead of Minecraft's resource manager.
 * Compiled only when a scene of kind "geo" is in the run (it needs Gson).
 */
final class GeoPreview {
    private GeoPreview() {}

    /** assets/nusmp/&lt;path&gt; of the mod, then this tool's assets / examples folders (the test models live in examples/). */
    static Reader source(ResourceLocation id) {
        List<File> dirs = new ArrayList<>();
        String ns = id.getNamespace();
        if (ns.equals("nusmp")) {
            dirs.add(new File(Preview.modAssets, "nusmp"));
            dirs.add(new File(Preview.previewDir, "assets/nusmp"));
            dirs.add(new File(Preview.previewDir, "assets"));
            dirs.add(new File(Preview.previewDir, "examples"));
        } else {
            dirs.add(new File(Preview.modAssets, ns));
            dirs.add(new File(Preview.previewDir, "assets/" + ns));
        }
        for (File d : dirs) {
            File f = new File(d, id.getPath());
            if (f.isFile()) {
                try { return new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8); } catch (IOException e) { return null; }
            }
        }
        return null;
    }

    static GeoSpec specOf(Map<String, Object> sc) {
        String key = Preview.str(sc, "modelKey", null);
        if (key != null) {
            GeoSpec spec = GeoSpec.of(key, Preview.str(sc, "modelName", ""));
            String tex = Preview.str(sc, "texture", null);
            if (tex != null) spec = tex.contains(":") || tex.contains("/") ? new GeoSpec(spec.model(), spec.animations(), ResourceLocation.parse(tex), spec.glow()) : spec.skin(tex);
            return spec;
        }
        ResourceLocation model = ResourceLocation.parse(Preview.str(sc, "modelRl", ""));
        String anim = Preview.str(sc, "animationsRl", null), tex = Preview.str(sc, "textureRl", null), glow = Preview.str(sc, "glowRl", null);
        return new GeoSpec(model, anim == null ? null : ResourceLocation.parse(anim), tex == null ? ResourceLocation.fromNamespaceAndPath("nusmp", "textures/missing.png") : ResourceLocation.parse(tex),
                glow == null ? null : ResourceLocation.parse(glow));
    }

    static void run(Map<String, Object> sc, Preview.Out out, String binDir) throws Exception {
        GeoModels.setSource(GeoPreview::source);
        final GeoSpec spec = specOf(sc);
        final GeoDraw.Space space = GeoDraw.Space.valueOf(Preview.str(sc, "space", "PROP").toUpperCase(Locale.ROOT));
        final GeoDraw.Layer layer = GeoDraw.Layer.valueOf(Preview.str(sc, "layer", "CUTOUT").toUpperCase(Locale.ROOT));
        final String anim = Preview.str(sc, "anim", null);
        final int argb = (int) (long) Preview.num(sc, "argb", 4294967295.0), glow = (int) (long) Preview.num(sc, "glow", 0.0);
        Map<String, Object> sc2 = new LinkedHashMap<>(sc);
        if (space == GeoDraw.Space.PROP) {
            sc2.put("propKind", PropKind.values()[0].name());
            PropPainter painter = (e, partial, age, pose, buffers, light) -> GeoDraw.paint(pose, buffers, spec, anim, age / 20f, space, layer, light, argb, glow);
            Preview.runProp(sc2, out, binDir, painter);
        } else {
            sc2.put("aura", Aura.values()[0].name());
            AuraPainter painter = c -> GeoDraw.paint(c.pose(), c.buffers(), spec, anim, c.t() / 20f, space, layer, c.light(), argb, glow);
            Preview.runAura(sc2, out, binDir, painter);
        }
    }
}
