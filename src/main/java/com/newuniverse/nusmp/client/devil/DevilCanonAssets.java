package com.newuniverse.nusmp.client.devil;

import com.newuniverse.nusmp.client.playermodel.PlayerModelOptions;
import com.newuniverse.nusmp.devil.DevilCanonRules;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: where the canon model of each devil lives, for both devil renderers (the trial fight's {@link DevilTrialGeoRenderer} and the summoning figure's {@link DevilNpcGeoRenderer}); docs/devil_trial_model.md,
 * "Canon looks". One table, made once when the class loads (26 devils x 3 locations), read with a map lookup: no allocation per frame. Client only (it reads the client option).
 *
 * <p>A devil is drawn with its canon model when it has one ({@link DevilCanonRules#has}) and the client option {@code bossLooks.devilCanonLooks} is on (default); otherwise the renderers draw the generic horned
 * brute they always had (every unknown or pack devil does).
 */
final class DevilCanonAssets {
    private DevilCanonAssets() {}

    private record Files(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {}

    private static final Map<String, Files> BY_ID = new HashMap<>();

    static {
        for (String id : DevilCanonRules.IDS)
            BY_ID.put(id, new Files(loc(DevilCanonRules.geoPath(id)), loc(DevilCanonRules.texturePath(id)), loc(DevilCanonRules.animationPath(id))));
    }

    private static ResourceLocation loc(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", path); }

    /** The canon assets of this devil, or null when it has none or the option is off (then the generic brute is drawn). */
    private static Files of(String id) { return id == null || !PlayerModelOptions.devilCanonLooks() ? null : BY_ID.get(id); }

    static boolean canon(String id) { return of(id) != null; }

    /** The canon geometry, or {@code fallback} (the brute's). */
    static ResourceLocation model(String id, ResourceLocation fallback) { Files s = of(id); return s == null ? fallback : s.model; }

    static ResourceLocation texture(String id, ResourceLocation fallback) { Files s = of(id); return s == null ? fallback : s.texture; }

    static ResourceLocation animation(String id, ResourceLocation fallback) { Files s = of(id); return s == null ? fallback : s.animation; }
}
