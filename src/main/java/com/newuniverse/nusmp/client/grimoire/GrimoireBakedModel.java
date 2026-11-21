package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.GrimoireRevampConfig;
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
    /** The two looks of a book: the revamped model and the classic one (visual.classicGrimoires, distant owners). */
    private record Id(BookLook.Key key, boolean classic) {}
    private final Map<Id, View> cache = new HashMap<>();

    GrimoireBakedModel(Map<String, TextureAtlasSprite> sprites, ItemTransforms transforms) {
        this.sprites = sprites;
        this.transforms = transforms;
        this.particle = sprites.get("cover_leather");
    }

    private View viewFor(BookLook.Key key, boolean classic) {
        synchronized (cache) {
            return cache.computeIfAbsent(new Id(key, classic), id -> new View(id.key(), id.classic(), GrimoireQuadBaker.bake(id.key(), sprites, id.classic())));
        }
    }

    /** 4.89.2: the book is solid. The default item render type is translucent with back-face culling, which made the big faces (pages, boards) vanish or show the world through them; this one is opaque (cut-out) and draws both sides. */
    private static final java.util.List<net.minecraft.client.renderer.RenderType> SOLID = java.util.List.of(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));

    // ---- the shell itself (what a plain getQuads() caller sees): the default look
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return side == null ? viewFor(BookLook.DEFAULT.key(false), GrimoireRevampConfig.classic()).quads : List.of();
    }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public ItemTransforms getTransforms() { return transforms; }
    @Override public ItemOverrides getOverrides() { return overrides; }
    @Override public java.util.List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return SOLID; }

    /** One resolved book. */
    private final class View implements BakedModel {
        final BookLook.Key key;
        final boolean classic;
        final List<BakedQuad> quads;
        private View heldView;
        View(BookLook.Key key, boolean classic, List<BakedQuad> quads) { this.key = key; this.classic = classic; this.quads = quads; }

        View held() {
            if (key.held()) return this;
            View v = heldView;
            if (v == null) heldView = v = viewFor(new BookLook.Key(key.emblem(), key.motif(), key.tattered(), true, key.open(), key.magic(), key.fusion()), classic);
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
        @Override public java.util.List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return SOLID; }
    }

    private final class Overrides extends ItemOverrides {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            boolean classic = GrimoireRevampConfig.classic() || GrimoireItem.classicView(stack);      // the setting, or the cheap copy the floating renderer makes for distant owners
            if (stack.getItem() instanceof com.newuniverse.nusmp.compat.CompatCodexItem)                       // 1.34.0: the Compatibility Codex has a fixed look (compat/CompatCodexLook)
                return viewFor(com.newuniverse.nusmp.compat.CompatCodexLook.LOOK.key(false), classic);
            if (stack.getItem() instanceof com.newuniverse.nusmp.engraving.codex.EngravingCodexItem)           // 1.99.0: the Engravement Codex has a fixed look (engraving/codex/EngravingCodexLook)
                return viewFor(com.newuniverse.nusmp.engraving.codex.EngravingCodexLook.LOOK.key(false), classic);
            return viewFor(GrimoireItem.look(stack).key(false, GrimoireItem.openView(stack)), classic);
        }
    }
}
