package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's Shadow Magic brief wants a real Shadow Travel: you can only slip into a shadow and out of another one, so the spell checks
 * the light of the block cells instead of just moving you where you look (Dark Garden Invitation, which stays as it is). This is the cast of the
 * {@code shadow_travel} page: it checks that the caster is in shadow, picks the aimed shadow or else the nearest one in front within a bounded
 * search, charges magicules by distance, teleports the caster and up to two sneaking allies, and plays the shadow pool effect at both ends.
 *
 * <p>Rules: the caster stands in shadow (light level at most {@code shadow.maxLight} at feet and head, see {@link ShadowLight}), unless a Unite mode
 * is running or the config switch is off. The destination is in the same dimension, within {@code shadow.travelRange}, a standing spot with two free
 * cells, solid ground, no fluid, lava, fire or void, in shadow. Not usable while sealed, rooted or riding.
 */
public final class ShadowTravel {
    private ShadowTravel() {}

    /** The page cast. */
    public static boolean cast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (p.getPersistentData().getLong(ManaLaw.SEALED) > now) { GrimoireBook.fail(p, "Your grimoire is sealed; the shadows will not open."); return false; }
        MobEffectInstance slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 6) { GrimoireBook.fail(p, "You are held fast; you can not slip into a shadow."); return false; }
        if (p.isPassenger()) { GrimoireBook.fail(p, "You can not slip into a shadow while riding."); return false; }
        int maxLight = ShadowConfig.maxLight();
        BlockPos here = p.blockPosition();
        boolean ownShade = UniteState.active(p) != null;
        if (ShadowConfig.requireShadow() && !ownShade && !ShadowWorld.inShadow(level, here, maxLight)) {
            GrimoireBook.fail(p, "There is no shadow here to slip into. Stand where it is dark (light level " + maxLight + " or less).");
            return false;
        }
        int range = ShadowConfig.travelRange();
        ShadowWorld.Budget budget = new ShadowWorld.Budget(ShadowConfig.searchCap());
        BlockPos dest = aimed(level, p, range, maxLight, budget);
        if (dest == null) dest = nearest(level, p, range, maxLight, budget);
        if (dest == null) { GrimoireBook.fail(p, "No shadow within reach to rise from."); return false; }

        Vec3 from = p.position();
        Vec3 to = new Vec3(dest.getX() + 0.5, ShadowWorld.standY(level, dest), dest.getZ() + 0.5);
        if (!level.noCollision(p, p.getBoundingBox().move(to.subtract(from)))) { GrimoireBook.fail(p, "The shadow there is too cramped."); return false; }
        double dist = from.distanceTo(to);
        double cost = ShadowMana.cost(p, ShadowSearch.extraManaPercent(dist, ShadowConfig.manaPerBlock()));
        if (!ShadowMana.pay(p, cost)) { GrimoireBook.fail(p, "Not enough magicules to carry you " + (int) dist + " blocks."); return false; }

        List<Player> followers = followers(p, ShadowConfig.followers());
        b.vfx(p, VfxShape.SHADOW_POOL, from, from.add(0, 1, 0), 30, 1.4f);
        b.vfx(p, VfxShape.SHADOW_POOL, to, to.add(0, 1, 0), 30, 1.4f);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, 0.6f);
        p.teleportTo(to.x, to.y, to.z);
        p.resetFallDistance();
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, 0.8f);
        int n = 0;
        for (Player f : followers) {
            Vec3 at = followerSpot(level, dest, n++);
            if (at == null) continue;
            f.teleportTo(at.x, at.y, at.z);
            f.resetFallDistance();
        }
        return true;
    }

    /** The shadow spot under the crosshair: the block face the caster looks at, or null if it is sky or not a shadow. */
    private static BlockPos aimed(ServerLevel level, ServerPlayer p, int range, int maxLight, ShadowWorld.Budget budget) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getViewVector(1f).scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos spot = ShadowWorld.findShadowSpot(level, hit.getBlockPos().relative(hit.getDirection()), maxLight, budget);
        return spot != null && far(p, spot, range) ? spot : null;
    }

    /** The nearest shadow spot in the cone in front of the caster, looking at no more than the budget allows. */
    private static BlockPos nearest(ServerLevel level, ServerPlayer p, int range, int maxLight, ShadowWorld.Budget budget) {
        Vec3 look = p.getViewVector(1f);
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        for (int[] o : ShadowSearch.candidates(look.x, look.y, look.z, range, ShadowConfig.searchCap())) {
            if (budget.left() <= 0) return null;
            BlockPos spot = ShadowWorld.findShadowSpot(level, eye.offset(o[0], o[1], o[2]), maxLight, budget);
            if (spot != null && far(p, spot, range)) return spot;
        }
        return null;
    }

    /** Far enough to be worth it (2 blocks) and not beyond the range. */
    private static boolean far(ServerPlayer p, BlockPos spot, int range) {
        double d = p.position().distanceTo(Vec3.atBottomCenterOf(spot));
        return d >= 2.0 && d <= range + 1.5;
    }

    /** Sneaking allies within 2.5 blocks, up to {@code max}, nearest first. */
    private static List<Player> followers(ServerPlayer p, int max) {
        List<Player> all = new ArrayList<>(p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(2.5),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.isShiftKeyDown() && x.isAlliedTo(p)));
        all.sort((a, c) -> Double.compare(a.distanceToSqr(p), c.distanceToSqr(p)));
        return all.size() > max ? new ArrayList<>(all.subList(0, Math.max(0, max))) : all;
    }

    /** A free standing spot next to the arrival cell for the n-th follower (it need not be in shadow). */
    private static Vec3 followerSpot(ServerLevel level, BlockPos dest, int n) {
        int[][] ring = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int k = 0; k < ring.length; k++) {
            int[] r = ring[(n + k) % ring.length];
            BlockPos feet = dest.offset(r[0], 0, r[1]);
            for (int dy : new int[]{0, -1, 1}) {
                BlockPos f = feet.above(dy);
                if (ShadowWorld.standable(level, f)) return new Vec3(f.getX() + 0.5, ShadowWorld.standY(level, f), f.getZ() + 0.5);
            }
        }
        return null;
    }
}
