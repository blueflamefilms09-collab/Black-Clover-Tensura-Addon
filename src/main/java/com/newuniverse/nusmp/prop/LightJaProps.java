package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.LightBoneJa;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the server half of the props of the Light Magic remake (PropKind.LIGHT_JA). A prop is only the body of a spell (the hitboxes, damage and timing are in book.LightJaArts and run on the
 * server); the client draws it (client.prop.LightJaPropPainter). The variant is the low byte of the param, the rest of the param is data of the variant:
 * <ul>
 *   <li>0 SWORD and 5 ARROW: a flying body (speed in bits 8-15): the server never moves it, the painter moves it from its age along its direction (no packet per tick); the hitbox is the SpellRuntime bolt fired with it.</li>
 *   <li>1 CROSS: the two crossing blades where a sword ended. 2 WHIP (bits 8-9: the lash 0..2): one curling lash of light. 3 BEAM (bits 8-15: the length in half blocks): the beam of the Heaven-Splitting
 *       Flash. 4 COLUMN: the ring and then the column of the Light Shaft (follows its marked foe until the column falls). All still, short lived.</li>
 *   <li>6 ORB: the Lamp of Avior; flag 0x10000 FLIGHT = flying (the speed is in bits 8-15, see LightBoneJa.speedBits), flag 0x20000 BURST = the starburst where it ended, neither = the orb gathering in the raised hand (follows the caster).</li>
 *   <li>7 HEAL: the white sphere with the halo that follows the one it heals. 8 MONARCH: the giant of golden light standing behind its caster; flag 0x10000 STRIKE = it is striking.
 *       9 GLORIA: the black-and-white burst of the Lamp of Avior Gloria growing over its caster.</li>
 * </ul>
 * A following prop ends when its caster is gone or dead. Props are never saved; one that outlives its caster only lives its few ticks.
 */
public final class LightJaProps {
    private LightJaProps() {}

    public static final int SWORD = LightJaVariant.SWORD, CROSS = LightJaVariant.CROSS, WHIP = LightJaVariant.WHIP, BEAM = LightJaVariant.BEAM, COLUMN = LightJaVariant.COLUMN, ARROW = LightJaVariant.ARROW,
            ORB = LightJaVariant.ORB, HEAL = LightJaVariant.HEAL, MONARCH = LightJaVariant.MONARCH, GLORIA = LightJaVariant.GLORIA;
    public static final int FLIGHT = LightJaVariant.FLIGHT, BURST = LightJaVariant.BURST, STRIKE = LightJaVariant.STRIKE;

    public static int variant(int param) { return param & 0xFF; }

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.LightBoneJaBudget.wire();                  // 1.99.0: props end with their caster (death, logout, dimension)
        com.newuniverse.nusmp.book.LightBoneJaSelfTests.register();      // 1.99.0: the CI server smoke test runs the self-test magic.light_bone_ja
        MagicProps.register(PropKind.LIGHT_JA, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                switch (variant(e.param())) {
                    case SWORD, ARROW -> e.setSize(0.5f * e.scale(), 0.5f * e.scale());
                    case ORB -> e.setSize(0.8f * e.scale(), 0.8f * e.scale());
                    case MONARCH -> e.setSize(1.4f * e.scale(), 5.0f * e.scale());
                    default -> e.setSize(0.6f, 0.6f);
                }
            }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) { LightJaProps.tick(e); }
        });
    }

    private static void tick(MagicPropEntity e) {
        int v = variant(e.param());
        switch (v) {
            case ORB -> { if ((e.param() & (FLIGHT | BURST)) == 0) hand(e); }
            case HEAL -> {
                Entity t = e.target();
                if (!(t instanceof LivingEntity l) || !l.isAlive()) { e.expire(); return; }
                e.setPos(l.getX(), l.getY() + l.getBbHeight() * 0.55, l.getZ());
            }
            case MONARCH -> {
                if (!(e.owner() instanceof LivingEntity o)) { e.expire(); return; }
                Vec3 look = o.getViewVector(1f);
                Vec3 flat = new Vec3(look.x, 0, look.z);
                flat = flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
                e.setPos(o.getX() - flat.x * 3.4, o.getY(), o.getZ() - flat.z * 3.4);
                e.setYRot(o.getYRot());
            }
            case GLORIA -> {
                if (!(e.owner() instanceof LivingEntity o)) { e.expire(); return; }
                e.setPos(o.getX(), o.getY() + 5.0, o.getZ());
            }
            case COLUMN -> {
                Entity t = e.target();
                if (t != null && t.isAlive() && e.life() < LightBoneJa.SHAFT_TELEGRAPH) e.setPos(t.getX(), t.getY(), t.getZ());
            }
            default -> { }
        }
    }

    /** The gathering orb sits in front of the caster's raised hand. */
    private static void hand(MagicPropEntity e) {
        if (!(e.owner() instanceof LivingEntity o)) { e.expire(); return; }
        Vec3 look = o.getViewVector(1f);
        Vec3 at = o.getEyePosition().add(look.scale(1.4)).add(0, 0.1, 0);
        e.setPos(at.x, at.y, at.z);
    }
}
