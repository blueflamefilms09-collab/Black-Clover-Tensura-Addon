package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.wave.Wave26Rules.Mark;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the three relics of wave 26 whose ability is a physical act rather than a mind spell (the Goggles of Dwarvenkin that show hidden
 * stonework, the Helmet of Headbutting that butts, the Lens of Straight Creepin' that shows who passed by) share the same plumbing: look at
 * something close by, mark it with Rukh, say what was found. Each act is bounded (a capped number of blocks, marks and targets) and none of them
 * edits a block, so they are safe in the Labyrinth.
 */
final class Wave26Acts {
    private Wave26Acts() {}

    private enum Find { ORE, TRAP, HIDDEN }

    private record Spot(BlockPos pos, Find kind, double dist) {}

    // ================================================================ Goggles of Dwarvenkin
    /** Stone Cunning: ores, hidden stonework (infested blocks, suspicious sand and gravel) and traps (plates, tripwire, TNT, dispensers, spawners, sculk) within fourteen blocks glow through the stone. */
    static void stoneSense(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        BlockPos o = p.blockPosition();
        int r = Wave26Rules.GOGGLES_RADIUS;
        List<Spot> spots = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        outer:
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                m.set(o.getX() + dx, 0, o.getZ() + dz);
                if (!sl.hasChunkAt(m)) continue;
                for (int dy = -r; dy <= r; dy++) {
                    m.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
                    if (m.getY() < sl.getMinBuildHeight() || m.getY() >= sl.getMaxBuildHeight()) continue;
                    BlockState st = sl.getBlockState(m);
                    if (st.isAir()) continue;
                    Find f = classify(st);
                    if (f == null) continue;
                    spots.add(new Spot(m.immutable(), f, Math.sqrt(m.distSqr(o))));
                    if (spots.size() >= 600) break outer;                                   // enough: the nearest of these are shown
                }
            }
        }
        spots.sort(Comparator.comparingDouble(Spot::dist));
        int ore = 0, trap = 0, hidden = 0;
        for (int i = 0; i < spots.size(); i++) {
            Spot s = spots.get(i);
            switch (s.kind) {
                case ORE -> ore++;
                case TRAP -> trap++;
                default -> hidden++;
            }
            if (i < Wave26Rules.GOGGLES_MARKS)
                com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_26, FxTone.of(s.kind == Find.TRAP), s.pos.getX() + 0.5, s.pos.getY() + 0.5, s.pos.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.02);
        }
        sl.playSound(null, o, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.6f);
        Wave26Util.tell(p, "Amber light along the seams: " + ore + " ore, " + hidden + " hidden stone, " + trap + " traps (nearest " + Math.min(spots.size(), Wave26Rules.GOGGLES_MARKS) + " marked).", ChatFormatting.GOLD);
    }

    private static Find classify(BlockState st) {
        if (st.getBlock() instanceof InfestedBlock || st.is(Blocks.SUSPICIOUS_SAND) || st.is(Blocks.SUSPICIOUS_GRAVEL)) return Find.HIDDEN;
        if (st.is(BlockTags.PRESSURE_PLATES) || st.is(Blocks.TRIPWIRE_HOOK) || st.is(Blocks.TNT) || st.is(Blocks.DISPENSER) || st.is(Blocks.SPAWNER)
                || st.is(Blocks.TRIAL_SPAWNER) || st.is(Blocks.SCULK_SHRIEKER) || st.is(Blocks.SCULK_SENSOR) || st.is(Blocks.CALIBRATED_SCULK_SENSOR)) return Find.TRAP;
        if (st.is(Tags.Blocks.ORES)) return Find.ORE;
        return null;
    }

    // ================================================================ Helmet of Headbutting
    /** Headbutt: a lunge head first at what you face (within 2.8 blocks): 1d8 bludgeoning, half again as much and a stun on a critical hit (a falling blow, or a natural 20). */
    static void headbutt(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave26Util.Aim a = Wave26Util.aim(p, Wave26Rules.HEADBUTT_REACH, e -> Wave26Util.target(p, e));
        if (a.hit() == null) {
            Wave26Util.tell(p, "You butt at thin air. Your head hurts.", ChatFormatting.GRAY);
            return;
        }
        LivingEntity t = a.hit();
        boolean falling = p.fallDistance > 0.0f && !p.onGround() && !p.isInWater() && !p.onClimbable();
        boolean crit = Wave26Rules.headbuttCrit(falling, Wave26Rules.die(Wave26Util.rng(p), 20));
        float dmg = Wave26Rules.headbuttDamage(Wave26Rules.die(Wave26Util.rng(p), 8)) * (crit ? 1.5f : 1.0f);
        Fx.damage(p, t, dmg, "minecraft:player_attack");
        Vec3 away = t.position().subtract(p.position());
        Vec3 n = away.lengthSqr() < 1.0e-6 ? p.getLookAngle() : new Vec3(away.x, 0, away.z).normalize();
        t.push(n.x * 0.7, 0.25, n.z * 0.7);
        t.hurtMarked = true;
        if (crit) {
            Wave26Util.give(t, Wave26Rules.STUN_TICKS, 0, "tensura:paralysis");
            Wave26Util.give(t, Wave26Rules.STUN_TICKS, 6, "minecraft:slowness");
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(sl, FxThemes.RELIC_WAVE_26, t, FxTone.LIGHT, crit ? 10 : 5);
        sl.playSound(null, t.blockPosition(), crit ? SoundEvents.ANVIL_LAND : SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, crit ? 0.6f : 1.0f, crit ? 1.4f : 0.8f);
        Wave26Util.tell(p, crit ? "CRITICAL HEADBUTT! " + t.getName().getString() + " is stunned." : "Headbutt!", ChatFormatting.YELLOW);
    }

    // ================================================================ Lens of Straight Creepin'
    /** See Tracks (once a day): the footprints and markings left near here in the last ten minutes (the lens remembers what moved past while you wore it) are drawn in the air, and every creature within 24 blocks glows for ten seconds. */
    static void seeTracks(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        List<Mark> tracks = Wave26Wear.ledger(p).recent(now, Wave26Rules.TRACK_AGE, p.getX(), p.getZ(), Wave26Rules.LENS_RADIUS, Wave26Rules.LENS_MARKS);
        for (Mark mk : tracks) {
            com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_26, FxTone.of(mk.kind() == 0), mk.x(), mk.y() + 0.15, mk.z(), 2, 0.1, 0.02, 0.1, 0.0);
        }
        int seen = 0;
        for (LivingEntity e : Wave26Util.near(sl, p.position(), 24.0, e -> e != p)) {
            Wave26Util.give(e, 10 * 20, 0, "minecraft:glowing");
            seen++;
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        Wave26Util.tell(p, tracks.isEmpty() ? "No tracks yet: the lens only remembers what passed while you wore it. " + seen + " creatures glow nearby."
                : tracks.size() + " tracks light up in the dust; " + seen + " creatures glow nearby.", ChatFormatting.RED);
    }
}
