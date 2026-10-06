package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Turns a {@link GrimoireBookPlan} into vanilla {@link BakedQuad}s (BLOCK vertex format, 8 ints per vertex). */
final class GrimoireQuadBaker {
    /** Packed lightmap for fullbright: block 15 in the low short, sky 15 in the high short. NeoForge takes max(world light, this). */
    private static final int FULLBRIGHT = (15 << 4) | (15 << 20);

    private GrimoireQuadBaker() {}

    static List<BakedQuad> bake(BookLook.Key key, Map<String, TextureAtlasSprite> sprites) {
        List<GrimoireBookPlan.Quad> plan = GrimoireBookPlan.build(key);
        List<BakedQuad> out = new ArrayList<>(plan.size());
        for (GrimoireBookPlan.Quad q : plan) out.add(bake(q, sprites.get(q.texture())));
        return Collections.unmodifiableList(out);
    }

    static BakedQuad bake(GrimoireBookPlan.Quad q, TextureAtlasSprite sprite) {
        int[] v = new int[32];
        Direction dir = direction(q.face());
        int normal = ((dir.getStepX() * 127) & 0xFF) | (((dir.getStepY() * 127) & 0xFF) << 8) | (((dir.getStepZ() * 127) & 0xFF) << 16);
        int light = q.emissive() ? FULLBRIGHT : 0;
        float[] pos = q.pos(), uv = q.uv();
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            v[o] = Float.floatToRawIntBits(pos[i * 3] / 16f);
            v[o + 1] = Float.floatToRawIntBits(pos[i * 3 + 1] / 16f);
            v[o + 2] = Float.floatToRawIntBits(pos[i * 3 + 2] / 16f);
            v[o + 3] = -1;                                                              // white: the item colour handler tints per quad
            v[o + 4] = Float.floatToRawIntBits(sprite.getU(uv[i * 2] / 16f));
            v[o + 5] = Float.floatToRawIntBits(sprite.getV(uv[i * 2 + 1] / 16f));
            v[o + 6] = light;
            v[o + 7] = normal;
        }
        return new BakedQuad(v, q.tint(), dir, sprite, !q.emissive(), false);
    }

    private static Direction direction(GrimoireBookPlan.Face f) {
        return switch (f) {
            case DOWN -> Direction.DOWN; case UP -> Direction.UP; case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH; case WEST -> Direction.WEST; case EAST -> Direction.EAST;
        };
    }
}
