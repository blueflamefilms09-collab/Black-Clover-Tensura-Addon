package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The two heavy weapons of wave 12.
 * <ul>
 *   <li>Giant Jawblade: right-click charges it up to three times (each charge slows you for three seconds, as a heavy swing should); the next blow
 *       is multiplied by (3 + charges) / 3, shoves the foe back and, at three charges, staggers it. Charges fade after ten seconds.</li>
 *   <li>The Lumberjack: right-click a natural tree to fell it. The logs are broken through the normal block-break rules (so claims and protection
 *       mods are respected), the tree falls the way you face and hurts the foes along its line.</li>
 * </ul>
 */
final class Wave12Heavy {
    private Wave12Heavy() {}

    static final String JAW = "giant_jawblade", AXE = "the_lumberjack";
    static final String JAW_FLAG = "w12_giant_jawblade", AXE_FLAG = "w12_the_lumberjack";

    // ================================================================ Giant Jawblade
    /** Current valid charges of the wielder (0 when they have faded). */
    static int jawCharges(ServerPlayer p) {
        if (!Wave12Data.hasPlayerData(p)) return 0;
        CompoundTag d = Wave12Data.player(p);
        return d.getLong("jaw_until") >= p.level().getGameTime() ? Math.max(0, Math.min(Wave12Rules.JAW_MAX_CHARGES, d.getInt("jaw"))) : 0;
    }

    static void charge(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave12Data.player(p);
        long now = p.level().getGameTime();
        int n = jawCharges(p);
        if (n >= Wave12Rules.JAW_MAX_CHARGES) {
            Wave5Aim.msg(p, "The Jawblade is fully charged: swing it.", ChatFormatting.GOLD);
            return;
        }
        n++;
        d.putInt("jaw", n);
        d.putLong("jaw_until", now + Wave12Rules.JAW_WINDOW);
        Fx.addEffect(p, "minecraft:slowness", 60, 2);                    // "only half your movement while you charge it"
        ServerLevel l = p.serverLevel();
        Wave5Aim.sound(l, p.position(), SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 0.6f + 0.15f * n);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 6 * n, 0.5);
        Wave5Aim.msg(p, "Jawblade charge " + n + "/" + Wave12Rules.JAW_MAX_CHARGES + ": the next blow hits " + Math.round(Wave12Rules.jawMultiplier(n) * 100) + "% hard.", ChatFormatting.GOLD);
    }

    /** Called from the damage hook: the blow of a charged Jawblade. Returns the new amount; the charges are spent. */
    static float jawBlow(ServerPlayer atk, LivingEntity victim, float amount) {
        int n = jawCharges(atk);
        if (n <= 0) return amount;
        CompoundTag d = Wave12Data.player(atk);
        d.putInt("jaw", 0);
        d.putInt("jaw_hit", n);
        float boosted = (float) (amount * Wave12Rules.jawMultiplier(n));
        return Wave12Rules.boundedBonus(amount, boosted, victim.getMaxHealth(), victim instanceof net.minecraft.world.entity.player.Player, Wave12Rules.PLAYER_BONUS_SHARE);
    }

    /** After the blow landed: the shove, and at three charges the stagger. */
    static void jawAfter(ServerPlayer atk, LivingEntity victim) {
        if (!Wave12Data.hasPlayerData(atk)) return;
        CompoundTag d = Wave12Data.player(atk);
        int n = d.getInt("jaw_hit");
        if (n <= 0) return;
        d.putInt("jaw_hit", 0);
        victim.knockback(0.35 + 0.35 * n, atk.getX() - victim.getX(), atk.getZ() - victim.getZ());
        if (n >= Wave12Rules.JAW_MAX_CHARGES) Fx.addEffect(victim, "minecraft:slowness", 40, 1);
        ServerLevel l = atk.serverLevel();
        Wave5Aim.puff(l, victim.position().add(0, victim.getBbHeight() * 0.5, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 8 * n, 0.4);
        Wave5Aim.sound(l, victim.position(), SoundEvents.ANVIL_LAND, 0.4f + 0.2f * n, 0.7f);
    }

    // ================================================================ The Lumberjack
    static void fell(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        if (LivingDungeon.in(l)) {
            Wave5Aim.msg(p, "The Dungeon's walls and roots cannot be cut.", ChatFormatting.GRAY);
            return;
        }
        HitResult hr = p.pick(5.0, 1.0f, false);
        if (hr.getType() != HitResult.Type.BLOCK || !(hr instanceof BlockHitResult b) || !l.getBlockState(b.getBlockPos()).is(BlockTags.LOGS)) {
            Wave5Aim.msg(p, "Face the trunk of a tree.", ChatFormatting.GRAY);
            return;
        }
        List<BlockPos> logs = new ArrayList<>();
        int leaves = gather(l, b.getBlockPos(), logs);
        if (!Wave12Rules.looksLikeTree(logs.size(), leaves)) {
            Wave5Aim.msg(p, "That is not a living tree (no natural leaves on it).", ChatFormatting.GRAY);
            return;
        }
        BlockPos foot = logs.get(0);
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos q : logs) {
            minY = Math.min(minY, q.getY());
            maxY = Math.max(maxY, q.getY());
            if (q.getY() < foot.getY()) foot = q;
        }
        List<BlockPos> order = new ArrayList<>(logs);
        order.sort((u, w) -> Integer.compare(w.getY(), u.getY()));
        int broken = 0;
        for (BlockPos q : order) {
            BlockState st = l.getBlockState(q);
            if (!st.is(BlockTags.LOGS)) continue;
            if (p.gameMode.destroyBlock(q)) broken++;
        }
        if (broken == 0) {
            Wave5Aim.msg(p, "You may not cut that tree here.", ChatFormatting.GRAY);
            return;
        }
        int height = Math.max(3, maxY - minY + 1);
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : dir.normalize();
        Vec3 base = Vec3.atBottomCenterOf(foot);
        Wave5Aim.sound(l, base, SoundEvents.WOOD_BREAK, 1.0f, 0.6f);
        int hit = 0;
        for (LivingEntity t : Fx.foesAround(p, height + 3.0)) {
            Vec3 to = t.position().subtract(base);
            double along = to.dot(dir);
            if (along < 0.5 || along > height + 1.0) continue;
            double perp = Math.sqrt(Math.max(0.0, to.x * to.x + to.z * to.z - along * along));
            if (perp > 1.8 || Math.abs(to.y) > 5.0) continue;
            Wave0Util.extra(p, t, Wave12Rules.treeDamage(along), "minecraft:player_attack");
            Fx.addEffect(t, "minecraft:slowness", 40, 1);
            hit++;
        }
        for (int i = 1; i <= Math.min(height, 24); i++) {                 // the trunk coming down, a line of splinters
            Vec3 at = base.add(dir.scale(i)).add(0, 0.4, 0);
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 3, 0.4, 0.3, 0.4, 0.05);
        }
        Wave5Aim.msg(p, "Timber! " + broken + " logs felled" + (hit > 0 ? ", " + hit + " caught under it." : "."), ChatFormatting.GOLD);
    }

    /** The logs of the tree the start block belongs to (bounded: 96 logs, 8 blocks sideways, 40 up); returns how many natural leaves touch them (counted to 3). */
    private static int gather(ServerLevel l, BlockPos start, List<BlockPos> out) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(start);
        seen.add(start.asLong());
        int leaves = 0;
        while (!queue.isEmpty() && out.size() < Wave12Rules.TREE_MAX_LOGS) {
            BlockPos q = queue.poll();
            out.add(q);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                if (leaves >= 3) break;
                BlockState n = l.getBlockState(q.relative(dir));
                if (n.getBlock() instanceof LeavesBlock && !n.getValue(LeavesBlock.PERSISTENT)) leaves++;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos r = q.offset(dx, dy, dz);
                        if (Math.abs(r.getX() - start.getX()) > Wave12Rules.TREE_RADIUS || Math.abs(r.getZ() - start.getZ()) > Wave12Rules.TREE_RADIUS) continue;
                        if (r.getY() < start.getY() - Wave12Rules.TREE_DOWN || r.getY() > start.getY() + Wave12Rules.TREE_UP) continue;
                        if (!seen.add(r.asLong())) continue;
                        if (l.getBlockState(r).is(BlockTags.LOGS)) queue.add(r);
                    }
                }
            }
        }
        return leaves;
    }
}
