package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.grimoire.GrimoireAppearance;
import com.newuniverse.nusmp.grimoire.GrimoireRenderKey;
import com.newuniverse.nusmp.grimoire.GrimoireSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The grimoire's baked model. The instance the model manager holds is a shell: when an item is about to be drawn,
 * {@link Overrides#resolve} reads the stack's {@link GrimoireAppearance}, maps it to a {@link GrimoireRenderKey} and returns the
 * {@link View} for that key - quads assembled once ({@link GrimoireQuadBaker}) and kept in a small LRU, so nothing is built
 * or allocated per frame for a book that has been drawn before. Colours are NOT part of the key; they come from the item colour
 * handler via tintindex.
 */
public final class GrimoireBakedModel implements BakedModel {
    /** Distinct looks kept hot. A hotbar + inventory + a few dropped books is far below this. */
    static final int CACHE_SIZE = 512;

    private final EnumMap<GrimoireSprite, TextureAtlasSprite> sprites;
    private final ItemTransforms transforms;
    private final TextureAtlasSprite particle;
    private final Overrides overrides = new Overrides();
    /** Access-ordered LRU: key -> quads (wrapped in the model view that serves them). Only the render thread touches it. */
    private final Map<GrimoireRenderKey, View> cache = new LinkedHashMap<>(CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<GrimoireRenderKey, View> eldest) { return size() > CACHE_SIZE; }
    };

    // one-entry identity memo: the same stack drawn every frame never even builds a key
    private GrimoireAppearance lastAppearance;
    private View lastView;

    GrimoireBakedModel(EnumMap<GrimoireSprite, TextureAtlasSprite> sprites, ItemTransforms transforms) {
        this.sprites = sprites;
        this.transforms = transforms;
        this.particle = sprites.get(GrimoireSprite.COVER_PLAIN);
    }

    /** Quads for a key, building and caching them on first use. */
    List<BakedQuad> quadsFor(GrimoireRenderKey key) { return viewFor(key).quads; }

    int cachedLooks() { synchronized (cache) { return cache.size(); } }

    private View viewFor(GrimoireRenderKey key) {
        synchronized (cache) {
            View v = cache.get(key);
            if (v == null) {
                v = new View(key, GrimoireQuadBaker.bake(key, sprites));
                cache.put(key, v);
            }
            return v;
        }
    }

    // ---- the shell itself (what a plain getQuads() caller sees): the default look
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return side == null ? viewFor(GrimoireAppearance.DEFAULT.renderKey()).quads : List.of();
    }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public ItemTransforms getTransforms() { return transforms; }
    @Override public ItemOverrides getOverrides() { return overrides; }

    /** The resolved per-look model: same flags and transforms, quads fixed. */
    private final class View implements BakedModel {
        final GrimoireRenderKey key;
        final List<BakedQuad> quads;
        private View heldView;
        View(GrimoireRenderKey key, List<BakedQuad> quads) { this.key = key; this.quads = quads; }

        /** The same book with its glow (canon: a grimoire glows slightly while it is out and in use). Looked up once, then remembered. */
        View held() {
            if (key.held()) return this;
            View v = heldView;
            if (v == null) heldView = v = viewFor(key.withHeld(true));
            return v;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext type, PoseStack poseStack, boolean leftHand) {
            transforms.getTransform(type).apply(leftHand, poseStack);
            boolean inHand = type.firstPerson() || type == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || type == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            return inHand ? held() : this;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? quads : List.of();
        }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return particle; }
        @Override public ItemTransforms getTransforms() { return transforms; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }

    private final class Overrides extends ItemOverrides {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            GrimoireAppearance a = GrimoireAppearance.of(stack);
            View v = lastView;
            if (a == lastAppearance && v != null) return v;
            v = viewFor(a.renderKey());
            lastAppearance = a;
            lastView = v;
            return v;
        }
    }
}
