package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.grimoire.BookLook;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The grimoire's baked model. The instance the model manager holds is a shell: when an item is about to be drawn,
 * {@link Overrides#resolve} reads the stack's look ({@link GrimoireItem#look}) and returns the {@link View} for its geometry key
 * (emblem, motif, wear, held, open). The handful of keys is built once and kept; colours come from the item colour handler (tintindex).
 * The open V (open 1..3) is only ever asked for by the summoned-book renderer, through a client-side copy of the stack.
 * In a hand (first / third person) the "held" view is used: its pages and emblem glow.
 */
public final class GrimoireBakedModel implements BakedModel {
    private final Map<String, TextureAtlasSprite> sprites;
    private final ItemTransforms transforms;
    private final TextureAtlasSprite particle;
    private final Overrides overrides = new Overrides();
    private final Map<BookLook.Key, View> cache = new HashMap<>();

    GrimoireBakedModel(Map<String, TextureAtlasSprite> sprites, ItemTransforms transforms) {
        this.sprites = sprites;
        this.transforms = transforms;
        this.particle = sprites.get("cover_leather");
    }

    private View viewFor(BookLook.Key key) {
        synchronized (cache) {
            return cache.computeIfAbsent(key, k -> new View(k, GrimoireQuadBaker.bake(k, sprites)));
        }
    }

    // ---- the shell itself (what a plain getQuads() caller sees): the default look
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return side == null ? viewFor(BookLook.DEFAULT.key(false)).quads : List.of();
    }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public ItemTransforms getTransforms() { return transforms; }
    @Override public ItemOverrides getOverrides() { return overrides; }

    /** One resolved book. */
    private final class View implements BakedModel {
        final BookLook.Key key;
        final List<BakedQuad> quads;
        private View heldView;
        View(BookLook.Key key, List<BakedQuad> quads) { this.key = key; this.quads = quads; }

        View held() {
            if (key.held()) return this;
            View v = heldView;
            if (v == null) heldView = v = viewFor(new BookLook.Key(key.emblem(), key.motif(), key.tattered(), true, key.open(), key.magic()));
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
            return viewFor(GrimoireItem.look(stack).key(false, GrimoireItem.openView(stack)));
        }
    }
}
