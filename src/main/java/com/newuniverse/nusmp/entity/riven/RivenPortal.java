package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Riven's boredom reaction. {@link RivenBossEntity} measures how little the fight is threatening him (almost no damage taken for a long
 * while); once that boredom is high and the dice roll, he tears open a gate, steps in, and comes out behind his target with two story
 * constructs "invited along". Entry: {@link #open}; exit a second later: {@link #emerge}.
 */
final class RivenPortal {
    static final int OPEN_TICKS = 34, LOCK_TICKS = 52, GATE_LIFE = 80;
    private static final String[] OPEN_LINES = {
            "This is getting boring. Let's raise the stakes.",
            "You're not even trying. Fine, I'll write a better ending.",
            "A story with no tension? Unacceptable. Plot twist!"};
    private static final String[] EXIT_LINES = {
            "Surprise! Behind you. Meet my friends.",
            "Chapter two: the cavalry arrives.",
            "Now it's a fight."};

    private RivenPortal() {}

    /** Entry gate in front of Riven, swirling shut around him. */
    static void open(RivenBossEntity b, ServerLevel sl, LivingEntity target) {
        Vec3 look = flat(target.position().subtract(b.position()));
        Vec3 gate = b.position().add(look.scale(2.2));
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, gate, b.position(), 0xFFB088FF, GATE_LIFE, 2.2f);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, b.position(), b.position().add(0, 1, 0), RivenAttacks.VIOLET, 30, 2.6f);
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, b, b.position().add(0, 0.05, 0), RivenAttacks.VIOLET, OPEN_TICKS + 6, 1.8f);
        sl.playSound(null, b.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 2.0f, 1.3f);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, gate.x, gate.y + 1.4, gate.z, 120, 0.7, 1.2, 0.7, 0.2);
        b.playClip("cast_grimoire");
        b.say(sl, OPEN_LINES[b.getRandom().nextInt(OPEN_LINES.length)]);
    }

    /** Exit gate behind the target; Riven steps out, and the reinforcements come through with him. */
    static void emerge(RivenBossEntity b, ServerLevel sl, LivingEntity target) {
        Vec3 back = flat(target.getLookAngle()).scale(-1);
        Vec3 out = target.position().add(back.scale(4.0));
        Vec3 from = b.position();
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, out, target.position(), 0xFFB088FF, GATE_LIFE, 2.2f);
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, out, out.add(0, 1.4, 0), 0xFF82DFFF, 40, 1.4f);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, from, from.add(0, 1, 0), RivenAttacks.VIOLET, 24, 1.8f);
        b.teleportTo(out.x, out.y, out.z);
        b.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        b.getNavigation().stop();
        Vec3 side = new Vec3(-back.z, 0, back.x).scale(1.8);
        StoryConstructEntity.spawn(sl, b, StoryConstructEntity.WEAPON, out.add(side).add(back.scale(-1)));
        StoryConstructEntity.spawn(sl, b, StoryConstructEntity.CLONE, out.subtract(side).add(back.scale(-1)));
        sl.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0f, 0.6f);
        sl.sendParticles(ParticleTypes.PORTAL, out.x, out.y + 1.2, out.z, 160, 0.8, 1.2, 0.8, 0.5);
        b.say(sl, EXIT_LINES[b.getRandom().nextInt(EXIT_LINES.length)]);
        b.addStory(15f);
        b.brain().invalidate();
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }
}
