package com.newuniverse.nusmp.skilltree.ft.kit;

import com.newuniverse.nusmp.labyrinth.mega.system.SystemParty;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: every Fairy Tail technique asks the same few questions about the world (where does the player's gaze end, where is the ground under that point, who among the creatures in
 * a shape is really a target, which are the nearest few), and the shared {@code AbilityKit} deliberately stays small (shapes and the balance law). Answering them here once keeps the
 * fifteen techniques of the Sky Dragon Slayer and Ice-Make routes a few lines each and keeps the one decision that matters for fairness in one place: {@link #hostile} takes a shape's
 * creatures and removes the caster's party (the same read-only {@link SystemParty} seam the dungeon quests use, so the tree and the quests agree on who plays together), the pets of
 * those players and villagers, so a gale or a hammer never hurts the people it is meant to protect, and other players are only targets where the server allows PvP, so a soft effect such as a chill
 * can never grief a stranger on a peaceful server (the damage itself would be refused by the game, but a slowness would not). It reads blocks only to LOOK (a clip along a ray) and never writes one, as the
 * owner's rule demands. It touches no Black Clover grimoire class. Stateless; server thread only.
 */
public final class FtWorld {
    private FtWorld() {}

    /** Where the player's gaze ends within {@code range}: the first solid block it meets, or the point at the full range. */
    public static Vec3 rayEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        return clip(p, eye, eye.add(p.getViewVector(1f).scale(range)));
    }

    /** The end of the segment {@code from} to {@code to} as far as no solid block is in the way: the point where a block stops it, or {@code to}. */
    public static Vec3 clip(ServerPlayer p, Vec3 from, Vec3 to) {
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** True when no solid block stands between the two points. */
    public static boolean clear(ServerPlayer p, Vec3 from, Vec3 to) {
        return p.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    /** The first solid surface under {@code at} (looking up to 8 blocks down, starting 1 block above), or {@code at} itself when there is none. */
    public static Vec3 groundAt(ServerPlayer p, Vec3 at) {
        BlockHitResult hit = p.serverLevel().clip(new ClipContext(at.add(0, 1, 0), at.add(0, -8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }

    /** The middle of a creature's body (where an effect should land). */
    public static Vec3 center(Entity e) { return e.getBoundingBox().getCenter(); }

    /** The unit vector of the player's gaze flattened onto the ground (never zero: looking straight up or down falls back to the body's facing). */
    public static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getViewVector(1f);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) {
            float yaw = (float) Math.toRadians(p.getYRot());
            return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        return flat.normalize();
    }

    /** Plays a sound where an effect lands (not where the caster stands), for everyone near it. */
    public static void soundAt(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Shoves a creature away from the caster by the game's own knockback rule (so knockback resistance is honoured) and tells the clients. Soft control: it needs no hard-control gate. */
    public static void shove(LivingEntity target, ServerPlayer from, double strength) {
        target.knockback(strength, from.getX() - target.getX(), from.getZ() - target.getZ());
        target.hurtMarked = true;
    }

    /** Throws a creature up by {@code up} blocks per tick of speed, reduced by its knockback resistance, and tells the clients. This is hard control: call it only after {@code AbilityKit.control}. */
    public static void launch(LivingEntity target, double up) {
        double resist = Math.max(0.0, Math.min(1.0, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
        target.setDeltaMovement(target.getDeltaMovement().add(0, up * (1.0 - resist), 0));
        target.hurtMarked = true;
    }

    /** Is {@code target} inside the cone of half-angle {@code halfAngleDeg} that opens from {@code eye} along {@code look}? The same test {@code AbilityKit.cone} uses, for points that are not creatures. */
    public static boolean inCone(Vec3 eye, Vec3 look, Vec3 target, double halfAngleDeg) {
        Vec3 to = target.subtract(eye);
        return to.lengthSqr() < 1e-6 || to.normalize().dot(look) >= Math.cos(Math.toRadians(halfAngleDeg));
    }

    /**
     * The creatures of a shape that are fair targets: not the caster's party, not their pets, not villagers, and other players only when the server allows PvP (and never a creative player).
     * The list is a new one; the input is not changed.
     */
    public static List<LivingEntity> hostile(ServerPlayer caster, List<LivingEntity> in) {
        Set<UUID> friends = friendIds(caster);
        boolean pvp = caster.getServer() != null && caster.getServer().isPvpAllowed();
        List<LivingEntity> out = new ArrayList<>(in.size());
        for (LivingEntity e : in) {
            if (friends.contains(e.getUUID()) || e instanceof AbstractVillager) continue;
            if (e instanceof Player other && (!pvp || other.isCreative())) continue;
            if (e instanceof OwnableEntity pet && pet.getOwnerUUID() != null && friends.contains(pet.getOwnerUUID())) continue;
            out.add(e);
        }
        return out;
    }

    /** The ids of the caster and everyone who plays with them (read-only party seam). */
    public static Set<UUID> friendIds(ServerPlayer caster) {
        Set<UUID> ids = new HashSet<>();
        ids.add(caster.getUUID());
        for (ServerPlayer m : SystemParty.members(caster)) ids.add(m.getUUID());
        return ids;
    }

    /** The {@code cap} creatures nearest to {@code from}, nearest first (a new list). */
    public static List<LivingEntity> nearest(List<LivingEntity> in, Vec3 from, int cap) {
        List<LivingEntity> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return sorted.size() <= cap ? sorted : new ArrayList<>(sorted.subList(0, Math.max(0, cap)));
    }

    /** How practised a skill is, 0 to 1, the way every addon technique feeds the balance law (mastery over the skill's maximum). */
    public static double masteryFrac(ManasSkillInstance instance) {
        return Math.max(0.0, Math.min(1.0, instance.getMastery() / (double) Math.max(1, instance.getMaxMastery())));
    }
}
