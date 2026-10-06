package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.grimoire.GrimoireModelPlan;
import com.newuniverse.nusmp.grimoire.GrimoireModelPlan.PlannedQuad;
import com.newuniverse.nusmp.grimoire.GrimoireRenderKey;
import com.newuniverse.nusmp.grimoire.GrimoireSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/** Turns a {@link GrimoireModelPlan} into vanilla {@link BakedQuad}s (BLOCK vertex format, 8 ints per vertex). */
final class GrimoireQuadBaker {
    /** Packed lightmap for fullbright: block 15 in the low short, sky 15 in the high short. NeoForge takes max(world light, this). */
    private static final int FULLBRIGHT = (15 << 4) | (15 << 20);

    private GrimoireQuadBaker() {}

    static List<BakedQuad> bake(GrimoireRenderKey key, EnumMap<GrimoireSprite, TextureAtlasSprite> sprites) {
        List<PlannedQuad> plan = GrimoireModelPlan.build(key);
        List<BakedQuad> out = new ArrayList<>(plan.size());
        for (PlannedQuad q : plan) out.add(bake(q, sprites.get(q.sprite())));
        return Collections.unmodifiableList(out);
    }

    static BakedQuad bake(PlannedQuad q, TextureAtlasSprite sprite) {
        int[] v = new int[32];
        int normal = packNormal(q.direction());
        int light = q.emissive() ? FULLBRIGHT : 0;
        float[] pos = q.pos(), uv = q.uv();
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            v[o] = Float.floatToRawIntBits(pos[i * 3]);
            v[o + 1] = Float.floatToRawIntBits(pos[i * 3 + 1]);
            v[o + 2] = Float.floatToRawIntBits(pos[i * 3 + 2]);
            v[o + 3] = -1;                                                              // white: the item colour handler tints per quad
            v[o + 4] = Float.floatToRawIntBits(sprite.getU(uv[i * 2] / 16f));
            v[o + 5] = Float.floatToRawIntBits(sprite.getV(uv[i * 2 + 1] / 16f));
            v[o + 6] = light;
            v[o + 7] = normal;
        }
        return new BakedQuad(v, q.tint(), q.direction(), sprite, q.shade(), false);
    }

    private static int packNormal(Direction d) {
        return ((d.getStepX() * 127) & 0xFF) | (((d.getStepY() * 127) & 0xFF) << 8) | (((d.getStepZ() * 127) & 0xFF) << 16);
    }
}
