package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the server half of the props of the Bone Magic remake (PropKind.BONE_JA); the casts, hitboxes and damage are in book.BoneJaArts, the models in client.prop.BoneJaPropPainter. The variant
 * is the low byte of the param, the rest is data of the variant:
 * <ul>
 *   <li>0 FANG (bits 8-15: its length in half blocks): one bone fang of the Eternal Fangs, still, aimed by the prop's yaw and pitch, coming out of the body and drawing back (the painter times it by its age).</li>
 *   <li>1 BULLET: a spinning bone fragment of the Bone Bullets; like every flying prop the server never moves it, the painter moves it from its age and the speed in bits 8-15 (the hitbox is the SpellRuntime bolt fired with it).</li>
 *   <li>2 DAINSLEIF: the bone sword coated in spatial magic, held in front of the caster (follows the caster, aimed where the caster looks).</li>
 *   <li>3 SLASH: a flying crescent of cut space (Spatial Rupture), moved by the painter from its age (speed in bits 8-15); flag 0x10000 SHORT = only the short crescent of a Dainsleif swing, no hitbox.</li>
 * </ul>
 * A following prop ends when its caster is gone. Props are never saved.
 */
public final class BoneJaProps {
    private BoneJaProps() {}

    public static final int FANG = BoneJaVariant.FANG, BULLET = BoneJaVariant.BULLET, DAINSLEIF = BoneJaVariant.DAINSLEIF, SLASH = BoneJaVariant.SLASH;
    public static final int SHORT = BoneJaVariant.SHORT;

    public static int variant(int param) { return param & 0xFF; }

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.LightBoneJaBudget.wire();
        MagicProps.register(PropKind.BONE_JA, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                switch (variant(e.param())) {
                    case BULLET -> e.setSize(0.35f * e.scale(), 0.35f * e.scale());
                    case SLASH -> e.setSize(0.8f * e.scale(), 0.8f * e.scale());
                    case DAINSLEIF -> e.setSize(0.8f * e.scale(), 0.8f * e.scale());
                    default -> e.setSize(0.5f, 0.5f);
                }
            }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) { BoneJaProps.tick(e); }
        });
    }

    private static void tick(MagicPropEntity e) {
        switch (variant(e.param())) {
            case DAINSLEIF -> {
                if (!(e.owner() instanceof LivingEntity o)) { e.expire(); return; }
                Vec3 look = o.getViewVector(1f);
                Vec3 at = o.getEyePosition().add(look.scale(1.1)).add(0, -0.55, 0);
                e.setPos(at.x, at.y, at.z);
                e.setYRot(o.getYRot());
                e.setXRot(o.getXRot());
            }
            default -> { }
        }
    }
}
