package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.nexus.vault.GateIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What the Ki-Resonance Lantern looks at (docs/living_dungeon.md section 3), once every few ticks while it burns in the off-hand: a bounded
 * fan of rays inside a cone of {@link FlameMode#CONE_DEG} degrees round the look direction, stepped out to {@link FlameMode#STONE_RANGE}
 * blocks, tells whether an illusion stone wall is ahead (a real wall stops a ray, an illusion does not); and the nearest descent gate
 * ({@link GateIndex#nearest}, within {@link FlameMode#GATE_RANGE} blocks) is ahead if it lies inside the same cone. Also draws the flame
 * beside the off-hand.
 */
final class LanternSense {
    private LanternSense() {}

    /** The rocks that look like walls and are not (the block tag nusmp:illusion_stones). */
    static final TagKey<Block> ILLUSION_STONES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "illusion_stones"));

    private static final double STEP = 0.6;
    private static final DustParticleOptions RED_FLAME = new DustParticleOptions(new Vector3f(0.95f, 0.14f, 0.10f), 1.0f);

    /** Is a block of an illusion rock inside the cone, within range and in sight (no solid block in the way)? */
    static boolean illusionAhead(ServerLevel level, Player p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (double[] d : Cone.fan(look.x, look.y, look.z, FlameMode.CONE_DEG)) {
            for (double s = 0.5; s <= FlameMode.STONE_RANGE; s += STEP) {
                BlockPos pos = BlockPos.containing(eye.x + d[0] * s, eye.y + d[1] * s, eye.z + d[2] * s);
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) break;
                BlockState state = level.getBlockState(pos);
                if (state.is(ILLUSION_STONES)) return true;
                if (!state.getCollisionShape(level, pos).isEmpty()) break;                  // a real wall: nothing behind it is in sight
            }
        }
        return false;
    }

    /** Does the nearest descent gate (to the level below) lie inside the cone of the look direction? */
    static boolean gateAhead(ServerLevel level, Player p) {
        BlockPos gate = GateIndex.nearest(level, p.blockPosition(), FlameMode.GATE_RANGE);
        if (gate == null) return false;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        return Cone.within(look.x, look.y, look.z, gate.getX() + 0.5 - eye.x, gate.getY() + 0.5 - eye.y, gate.getZ() + 0.5 - eye.z, FlameMode.CONE_DEG);
    }

    /** A small flame at the off-hand side of the player, amber, blue (soul fire) or red. */
    static void flame(ServerLevel level, ServerPlayer p, FlameMode mode) {
        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                                      // forward on the ground
        double lx = Math.cos(yaw), lz = Math.sin(yaw);                                       // to the player's left
        double side = p.getMainArm() == HumanoidArm.RIGHT ? 0.38 : -0.38;                     // the off-hand is on the other side from the main arm
        double x = p.getX() + fx * 0.42 + lx * side, y = p.getY() + (p.isCrouching() ? 0.95 : 1.12), z = p.getZ() + fz * 0.42 + lz * side;
        ParticleOptions particle = switch (mode) {
            case BLUE -> ParticleTypes.SOUL_FIRE_FLAME;
            case RED -> RED_FLAME;
            default -> ParticleTypes.FLAME;
        };
        level.sendParticles(particle, x, y, z, 1, 0.02, 0.03, 0.02, 0.004);
    }
}
