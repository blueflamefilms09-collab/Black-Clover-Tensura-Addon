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

import java.util.HashMap;
import java.util.Map;

/**
 * Riven Remake: the geo model (player proportions: messy hair, black high-collar coat with the Black Bull skull, tails, a violet glow)
 * played with the clips of assets/nusmp/animations/entity/riven_remake.animation.json (idle, walk, talk, casts, combos, hit, stagger,
 * phase2, final_form, death). Phase III swaps to the final-form model of the same entity (page-wings and the bull-skull crown), so the
 * boss bar never changes entity. His five-leaf-styled grimoire floats on his left side, bobbing and shaking when he casts from it.
 */
public final class RivenRenderer extends EntityRenderer<RivenBossEntity> {
    private static final GeoSpec BASE = GeoSpec.of("riven", "remake"), FINAL = GeoSpec.of("riven", "remake_final");
    private static final GeoSpec SKIN_MODEL = GeoSpec.of("marquis", "skin");
    private static final GeoSpec COAT_MODEL = GeoSpec.of("marquis", "coat");
    private static final GeoSpec FINAL_MODEL = GeoSpec.of("marquis", "final");
    private static final GeoSpec SKIN = new GeoSpec(SKIN_MODEL.model(), BASE.animations(), SKIN_MODEL.texture(), null);
    private static final GeoSpec COAT = new GeoSpec(COAT_MODEL.model(), BASE.animations(), COAT_MODEL.texture(), null);
    private static final GeoSpec FINAL_OVERLAY = new GeoSpec(FINAL_MODEL.model(), FINAL.animations(), FINAL.texture(), FINAL.glow());
    private static final Map<String, ItemStack> BOOKS = new HashMap<>();

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
        if (e.phase() == 1) {
            GeoDraw.paint(pose, buffers, SKIN, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0);
        } else if (e.phase() >= 2 && e.phase() < 4) {
            GeoDraw.paint(pose, buffers, SKIN, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0);
            float coatAlpha = e.phase() == 2 && clip.equals("phase2") ? Mth.clamp(seconds / 1.4f, 0f, 1f) : 1f;
            int alpha = (int) (255 * coatAlpha);
            GeoDraw.paint(pose, buffers, COAT, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light,
                    (alpha << 24) | 0x00FFFFFF, 0);
        } else {
            GeoDraw.paint(pose, buffers, SKIN, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0);
            GeoDraw.paint(pose, buffers, COAT, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0);
            GeoDraw.paint(pose, buffers, FINAL_OVERLAY, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                    0xFF000000 | (g << 16) | (g << 8) | g);
        }
        if (e.phase() >= 3 && e.phase() < 4 && e.tickCount % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = age * 0.08 + i * Math.PI * 2 / 3;
                e.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        e.getX() + Math.cos(angle) * 0.9, e.getY() + 0.7 + (i % 2) * 0.55, e.getZ() + Math.sin(angle) * 0.9,
                        0, 0.03, 0);
                e.level().addParticle(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                        e.getX() + Math.cos(angle) * 1.6, e.getY() + 0.05, e.getZ() + Math.sin(angle) * 1.6, 0, 0.02, 0);
            }
        }
        if (e.signatureCasting() && NUShaders.zagredAura() != null) {
            GeoSpec aura = e.phase() >= 4 ? FINAL_OVERLAY : e.phase() == 1 ? SKIN : COAT;
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
        CanonBook selection = CanonBook.byId(e.drawnGrimoire());
        if (selection == null) {
            selection = CanonBook.RIVEN;
            for (CanonBook candidate : CanonBook.values()) {
                if (candidate.owner.equals(e.drawnGrimoire())) {
                    selection = candidate;
                    break;
                }
            }
        }
        boolean open = e.phase() >= 2;
        CanonBook bookType = selection;
        String cacheKey = bookType.name() + (open ? "_open" : "_closed");
        ItemStack book = BOOKS.computeIfAbsent(cacheKey, key -> {
            ItemStack stack = GrimoireItem.createCanon(bookType);
            return open ? GrimoireItem.withOpenView(stack, GrimoireCarry.OPEN_STEPS) : stack;
        });
        boolean casting = clip.startsWith("cast") || clip.equals("manifest_weapon") || clip.equals("soul_bond");
        pose.pushPose();
        float orbit = e.phase() >= 2 ? 0.22f : 0f;
        pose.translate(0.28 + Mth.sin(age * 0.04f) * orbit, e.phase() >= 2 ? 2.05 + Mth.sin(age * 0.08f) * 0.08 : 1.05,
                e.phase() >= 2 ? -0.42 + Mth.cos(age * 0.04f) * orbit : 0.1);
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
                case StoryConstructEntity.CLONE, StoryConstructEntity.AVATAR -> GeoDraw.paint(pose, buffers, BASE, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT,
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
