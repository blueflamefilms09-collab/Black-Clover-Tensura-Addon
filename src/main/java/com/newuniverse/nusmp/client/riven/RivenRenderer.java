package com.newuniverse.nusmp.client.riven;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.NUShaders;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireCarry;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.riven.RivenBossEntity;
import com.newuniverse.nusmp.entity.riven.StoryConstructEntity;
import com.newuniverse.nusmp.grimoire.CanonBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Riven Remake: the geo model (player proportions: messy hair, black high-collar coat with the Black Bull skull, tails, a violet glow)
 * played with the clips of assets/nusmp/animations/entity/riven_remake.animation.json (idle, walk, talk, casts, combos, hit, stagger,
 * phase2, final_form, death). Phase III swaps to the final-form model of the same entity (page-wings and the bull-skull crown), so the
 * boss bar never changes entity. His five-leaf-styled grimoire floats on his left side, bobbing and shaking when he casts from it.
 */
public final class RivenRenderer extends EntityRenderer<RivenBossEntity> {
    private static final GeoSpec BASE = GeoSpec.of("riven", "remake"), FINAL = GeoSpec.of("riven", "remake_final");
    private static ItemStack book = ItemStack.EMPTY;

    public RivenRenderer(EntityRendererProvider.Context ctx) { super(ctx); this.shadowRadius = 0.5f; }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.RIVEN.get(), RivenRenderer::new);
        e.registerEntityRenderer(NUEntities.STORY_CONSTRUCT.get(), ConstructRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(RivenBossEntity e) { return BASE.texture(); }

    @Override
    public void render(RivenBossEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
        String clip;
        float seconds;
        if (e.isDeadOrDying()) { clip = "death"; seconds = (e.deathTime + partial) / 20f; }
        else if (e.clip() != 0) { clip = RivenBossEntity.CLIPS[e.clip()]; seconds = Math.max(0f, age - e.clipStartTick()) / 20f; }
        else {
            double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
            clip = dx * dx + dz * dz > 0.0004 ? "walk" : "idle";
            seconds = age / 20f;
        }
        float glowPulse = 0.7f + 0.3f * Mth.sin(age * (e.phase() >= 3 ? 0.35f : 0.12f));
        int g = (int) (255 * glowPulse);
        GeoDraw.paint(pose, buffers, e.phase() >= 3 ? FINAL : BASE, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                0xFF000000 | (g << 16) | (g << 8) | g);
        if (e.signatureCasting() && NUShaders.zagredAura() != null) {
            GeoSpec aura = e.phase() >= 3 ? FINAL : BASE;
            GeoModelData model = GeoModels.model(aura.model());
            GeoAnim animations = GeoModels.animations(aura.animations());
            GeoAnim.Clip auraClip = animations == null ? null : animations.clip(clip);
            pose.pushPose();
            pose.scale(1.035f, 1.025f, 1.035f);
            GeoDraw.draw(pose, buffers.getBuffer(NURenderTypes.zagredAura(aura.texture())), model, auraClip, seconds,
                    GeoDraw.Space.PROP, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xAA756EFF);
            pose.popPose();
        }
        if (!e.isDeadOrDying() || e.deathTime < 30) renderBook(e, age, light, pose, buffers, clip);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private void renderBook(RivenBossEntity e, float age, int light, PoseStack pose, MultiBufferSource buffers, String clip) {
        if (book.isEmpty()) book = GrimoireItem.withOpenView(GrimoireItem.createCanon(CanonBook.RIVEN), GrimoireCarry.OPEN_STEPS);
        boolean casting = clip.startsWith("cast") || clip.equals("manifest_weapon") || clip.equals("soul_bond");
        pose.pushPose();
        pose.translate(0.62, 1.15 + Mth.sin(age * 0.08f) * 0.05, 0.1);                       // his left, hip-high, floating
        pose.mulPose(Axis.YP.rotationDegrees(-8f + Mth.sin(age * 0.05f) * 4f));
        pose.mulPose(Axis.ZP.rotationDegrees(casting ? Mth.sin(age * 0.6f) * 8f : Mth.sin(age * 0.04f) * 2f));
        pose.scale(0.85f, 0.85f, 0.85f);
        Minecraft.getInstance().getItemRenderer().renderStatic(book, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
        pose.popPose();
    }

    /** Story constructs: a floating sword, the bull-crest shield, or a pale clone of him. */
    public static final class ConstructRenderer extends EntityRenderer<StoryConstructEntity> {
        private static final ItemStack SWORD = new ItemStack(Items.IRON_SWORD), SHIELD = new ItemStack(Items.SHIELD);

        public ConstructRenderer(EntityRendererProvider.Context ctx) { super(ctx); this.shadowRadius = 0.3f; }

        @Override
        public ResourceLocation getTextureLocation(StoryConstructEntity e) { return BASE.texture(); }

        @Override
        public void render(StoryConstructEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            float age = e.tickCount + partial;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            switch (e.kind()) {
                case StoryConstructEntity.CLONE -> GeoDraw.paint(pose, buffers, BASE, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT,
                        LightTexture.FULL_BRIGHT, 0x99B8A8FF, 0);
                default -> {
                    boolean shield = e.kind() == StoryConstructEntity.SHIELD;
                    pose.translate(0, 1.0 + Mth.sin(age * 0.1f) * 0.12, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(shield ? 180f : age * 12f));
                    pose.scale(shield ? 1.8f : 1.6f, shield ? 1.8f : 1.6f, shield ? 1.8f : 1.6f);
                    Minecraft.getInstance().getItemRenderer().renderStatic(shield ? SHIELD : SWORD, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
                }
            }
            pose.popPose();
            super.render(e, yaw, partial, pose, buffers, light);
        }
    }
}
