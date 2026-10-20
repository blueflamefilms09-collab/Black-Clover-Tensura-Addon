package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: an extras file is ONE geo model whose root bones are named for the model part they are drawn with ({@code x_head}, {@code x_body}, {@code x_arm_r} ...), and drawing it means drawing each
 * root under that part's pose. {@link GeoModels} parses the file once; this keeps, per file, the views of it by anchor ({@link GeoModelData#view}: the same bones, only the roots of one
 * anchor), the clips it has and its parsed animation, so a frame does no parsing and no allocation. Everything is dropped when the models are reloaded ({@link GeoModels} replaces the parsed
 * model: the cached views are recognised as stale by comparing the model instance).
 */
public final class ExtraModels {
    private ExtraModels() {}

    /** The ready-to-draw form of one extras file. */
    public static final class Ready {
        public final ResourceLocation rl;
        public final GeoModelData model;
        public final Map<PlayerLook.Anchor, GeoModelData> views = new EnumMap<>(PlayerLook.Anchor.class);
        public final GeoAnim anim;
        public final Set<String> clips = new HashSet<>();

        Ready(ResourceLocation rl, GeoModelData model, GeoAnim anim) {
            this.rl = rl;
            this.model = model;
            this.anim = anim;
            for (PlayerLook.Anchor a : PlayerLook.Anchor.values()) {
                String root = "x_" + a.key;
                GeoModelData v = model.view(b -> b.name.equals(root));
                if (!v.roots.isEmpty()) views.put(a, v);
            }
            if (anim != null) clips.addAll(anim.names());
        }

        public boolean empty() { return views.isEmpty(); }
    }

    private static final Map<String, Ready> CACHE = new HashMap<>();

    public static ResourceLocation modelRl(String model) { return PlayerAssets.rl("geo/entity/" + model + ".geo.json"); }

    public static ResourceLocation animRl(String model) { return PlayerAssets.rl("animations/entity/" + model + ".animation.json"); }

    /**
     * The ready form of a model name ({@code pmx_asta}); an empty one when the file is missing or malformed (the geo loader logs it once). A frame with a known model does one map get and one
     * identity check against the loader's parsed model (no resource location is built, nothing is allocated); a resource reload makes the loader parse a new instance, which is how a stale entry is found.
     */
    public static synchronized Ready of(String model) {
        Ready r = CACHE.get(model);
        if (r != null && GeoModels.model(r.rl) == r.model) return r;
        ResourceLocation rl = modelRl(model);
        r = new Ready(rl, GeoModels.model(rl), GeoModels.animations(animRl(model)));
        CACHE.put(model, r);
        return r;
    }

    public static synchronized void clear() { CACHE.clear(); }
}
