package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.boss.rework.BossRework;
import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): the four moves appended to the Charybdis Spawn's table ({@link CharybdisTable#OLD_MOVES} onward), from Tensura's
 * Charybdis, the storm leviathan born of Veldora's magicules: the Scale-Shard Storm (shards rain on circles marked under the foes), the Tempest Current (the tide drags the
 * party toward it, by velocity only and never faster than walking), the Whirlpool Maw (a whirlpool round its body whose heart snaps shut) and Magic Interference (its scales'
 * anti-magic: timed blessings torn away in a ring). Every one is telegraphed for the whole wind-up (teal, red for the last third), lands where the wind-up drew it, goes
 * through the boss's damage law and changes no block. Worst case per cast: 24 foes looked at per step, one zone of at most 60 ticks stepping every 2 ticks, 4 boss effects.
 */
final class CharybdisRework {
    private CharybdisRework() {}

    private static final int MAX_VICTIMS = 24;

    private static CharybdisSpawnBoss of(ZogratisBoss b) { return (CharybdisSpawnBoss) b; }

    private static boolean first(int left, int total) { return left >= total - 3; }

    private static int colour(int left, int total) { return left * 3 < total ? CharybdisMoves.WARN : CharybdisMoves.TEAL; }

    private static void warn(CharybdisSpawnBoss boss, String id) {
        String w = CharybdisTable.warning(id);
        if (w.isEmpty()) return;
        Component c = Component.literal(w).withStyle(ChatFormatting.AQUA);
        for (LivingEntity e : boss.foes(48)) if (e instanceof ServerPlayer p) p.displayClientMessage(c, true);
    }

    static Move.MoveTell tellOf(String id) {
        return switch (id) {
            case CharybdisTable.SCALE_SHARD_STORM -> (b, t, left, total) -> tellStorm(of(b), left, total);
            case CharybdisTable.TEMPEST_CURRENT -> (b, t, left, total) -> tellRing(of(b), id, CharybdisTable.CURRENT_RADIUS, CharybdisTable.CURRENT_CORE, left, total);
            case CharybdisTable.WHIRLPOOL_MAW -> (b, t, left, total) -> tellRing(of(b), id, CharybdisTable.MAW_RADIUS, CharybdisTable.MAW_CORE, left, total);
            case CharybdisTable.MAGIC_INTERFERENCE -> (b, t, left, total) -> tellRing(of(b), id, CharybdisTable.INTERFERENCE_RADIUS, 0, left, total);
            default -> null;
        };
    }

    static Move.MoveRun runOf(String id) {
        return switch (id) {
            case CharybdisTable.SCALE_SHARD_STORM -> (b, t) -> storm(of(b));
            case CharybdisTable.TEMPEST_CURRENT -> (b, t) -> current(of(b));
            case CharybdisTable.WHIRLPOOL_MAW -> (b, t) -> maw(of(b));
            case CharybdisTable.MAGIC_INTERFERENCE -> (b, t) -> interference(of(b));
            default -> (b, t) -> { };
        };
    }

    // ================================================================ telegraphs
    /** A ring round the boss at the move's reach (and its core, when it has one), a closing ring, and one tide ring effect for the whole wind-up. */
    private static void tellRing(CharybdisSpawnBoss boss, String id, double radius, double core, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = CharybdisMoves.floorUnder(sl, boss.position());
        int rgb = colour(left, total);
        if (first(left, total)) {
            warn(boss, id);
            BossMoveVfx.telegraph(sl, CharybdisTable.TEMPEST_CURRENT.equals(id) ? VfxShape.BOSS_TIDE_RING : VfxShape.BOSS_TIDE_VORTEX, c, c.add(0, 1, 0), CharybdisVfx.tint(boss), total, radius);
            sl.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 1.8f, 0.6f);
        }
        Tells.ring(sl, c, radius, rgb, 1.3f, (int) Math.min(72, 16 + radius * 3));
        if (core > 0) Tells.ring(sl, c, core, CharybdisMoves.WARN, 1.2f, 20);
        Tells.ring(sl, c, Math.max(0.8, radius * left / Math.max(1, total)), CharybdisMoves.GLOW, 1.0f, 28);
        Tells.gather(sl, boss.position().add(0, boss.getBbHeight() / 2, 0), 1.4, CharybdisMoves.GLOW, 5);
    }

    /** The shard circles: one under each foe (up to four), locked when the wind-up begins. */
    private static void tellStorm(CharybdisSpawnBoss boss, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total) || boss.reworkSpots.isEmpty()) {
            warn(boss, CharybdisTable.SCALE_SHARD_STORM);
            List<Vec3> spots = new ArrayList<>(CharybdisTable.SHARD_SPOTS);
            for (LivingEntity e : boss.foes(40)) {
                spots.add(CharybdisMoves.floorUnder(sl, e.position()));
                if (spots.size() >= CharybdisTable.SHARD_SPOTS) break;
            }
            boss.reworkSpots = List.copyOf(spots);
            for (Vec3 s : spots) BossMoveVfx.telegraph(sl, VfxShape.BOSS_TIDE_RING, s, s.add(0, 1, 0), CharybdisVfx.tint(boss), total, CharybdisTable.SHARD_RADIUS);
            sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.8f, 1.2f);
        }
        int rgb = colour(left, total);
        for (Vec3 s : boss.reworkSpots) {
            Tells.ring(sl, s, CharybdisTable.SHARD_RADIUS, rgb, 1.3f, 24);
            Tells.column(sl, s, 4, CharybdisMoves.GLOW);
        }
    }

    // ================================================================ the moves
    /** The Scale-Shard Storm: shards fall for {@link CharybdisTable#SHARD_LAND} ticks, then each circle strikes whoever stands in it (once, however many circles). */
    static void storm(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        List<Vec3> spots = boss.reworkSpots;
        boss.reworkSpots = List.of();
        if (spots.isEmpty()) return;
        for (Vec3 s : spots) BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_SPOUT, s, s.add(0, 1, 0), CharybdisVfx.tint(boss), CharybdisTable.SHARD_LAND + 8, CharybdisTable.SHARD_RADIUS * 1.4);
        sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0f, 0.7f);
        boss.later(CharybdisTable.SHARD_LAND, CharybdisMoves.safe("scale shards", () -> {
            if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive()) return;
            Set<Integer> hit = new HashSet<>();
            double r = CharybdisTable.SHARD_RADIUS;
            for (Vec3 s : spots) {
                level.playSound(null, BlockPos.containing(s), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4f, 0.6f);
                int seen = 0;
                for (LivingEntity e : boss.foesAround(s.add(0, 3, 0), r + 6)) {
                    if (++seen > MAX_VICTIMS || hit.contains(e.getId())) continue;
                    double dx = e.getX() - s.x, dz = e.getZ() - s.z;
                    if (dx * dx + dz * dz > (r + e.getBbWidth() / 2) * (r + e.getBbWidth() / 2) || e.getY() < s.y - 1.5 || e.getY() > s.y + 10) continue;
                    hit.add(e.getId());
                    if (boss.strike(e, CharybdisTable.SHARD, true)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        }));
    }

    /** The Tempest Current: for {@link CharybdisTable#CURRENT_TICKS} ticks the tide drags every foe within reach toward the boss (slower than walking, nothing in the core). */
    static void current(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = boss.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_TIDE_VORTEX, c, c.add(0, 1, 0), CharybdisVfx.tint(boss), CharybdisTable.CURRENT_TICKS, CharybdisTable.CURRENT_RADIUS);
        sl.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6f, 0.6f);
        SpellRuntime.zone(sl, CharybdisTable.CURRENT_TICKS, 2, CharybdisMoves.safe("tempest current", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            Vec3 at = boss.position();
            int seen = 0;
            for (LivingEntity f : boss.foesAround(at, CharybdisTable.CURRENT_RADIUS)) {
                if (++seen > MAX_VICTIMS) break;
                Vec3 to = at.subtract(f.position()).multiply(1, 0, 1);
                double d = to.length();
                double speed = BossRework.pull(d, CharybdisTable.CURRENT_RADIUS, CharybdisTable.CURRENT_CORE, CharybdisTable.CURRENT_SPEED);
                if (speed <= 0 || Math.abs(f.getY() - at.y) > CharybdisMath.WHIRL_HEIGHT) continue;
                Vec3 v = f.getDeltaMovement(), push = to.scale(speed / d);
                f.setDeltaMovement(v.x + push.x, v.y, v.z + push.z);
                f.hurtMarked = true;
            }
            if (age % 10 == 0) Tells.ring(sl, at, CharybdisTable.CURRENT_RADIUS, CharybdisMoves.TEAL, 1.0f, 60);
        }));
    }

    /** The Whirlpool Maw: a whirlpool round the body (the old one's pull), and when it ends its heart snaps shut on whoever is still in the core. */
    static void maw(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel)) return;
        Vec3 c = boss.position().add(0, 0.5, 0);
        CharybdisMoves.whirl(boss, c, CharybdisTable.MAW_RADIUS, CharybdisTable.MAW_TICKS);
        boss.later(CharybdisTable.MAW_TICKS, CharybdisMoves.safe("whirlpool maw", () -> {
            if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive()) return;
            BossMoveVfx.strike(level, VfxShape.BOSS_TIDE_SPOUT, c, c.add(0, 1, 0), CharybdisVfx.tint(boss), CharybdisTable.MAW_CORE * 1.6);
            level.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);
            boss.swing(InteractionHand.MAIN_HAND);
            int seen = 0;
            for (LivingEntity e : boss.foesAround(c, CharybdisTable.MAW_CORE + 3)) {
                if (++seen > MAX_VICTIMS) break;
                double dx = e.getX() - c.x, dz = e.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > CharybdisTable.MAW_CORE + e.getBbWidth() / 2 || Math.abs(e.getY() - c.y) > CharybdisMath.WHIRL_HEIGHT) continue;
                if (boss.strike(e, CharybdisTable.MAW, false)) {
                    Vec3 v = e.getDeltaMovement();
                    double k = 0.9 / Math.max(1e-3, d);
                    e.setDeltaMovement(v.x + dx * k, Math.max(v.y, 0.35), v.z + dz * k);
                    e.hurtMarked = true;
                }
            }
        }));
    }

    /** Magic Interference: everyone in the ring loses up to two timed blessings, is struck lightly and weakened for three seconds. */
    static void interference(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = boss.position();
        BossMoveVfx.strike(sl, VfxShape.BOSS_TIDE_RING, c, c.add(0, 1, 0), CharybdisVfx.tint(boss), CharybdisTable.INTERFERENCE_RADIUS);
        ReworkFx.plain(sl, VfxShape.ANTI_MAGIC_SLASH, c.add(0, boss.getBbHeight() / 2, 0), c.add(boss.getViewVector(1f).scale(6)).add(0, boss.getBbHeight() / 2, 0), CharybdisVfx.tint(boss), 16, 3.0);
        sl.playSound(null, boss.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0f, 0.5f);
        int seen = 0;
        for (LivingEntity e : boss.foes(CharybdisTable.INTERFERENCE_RADIUS)) {
            if (++seen > MAX_VICTIMS) break;
            if (Math.abs(e.getY() - c.y) > CharybdisMath.WHIRL_HEIGHT) continue;
            ReworkFx.cancelMagic(e, CharybdisTable.INTERFERENCE_STRIPS);
            if (boss.strike(e, CharybdisTable.INTERFERENCE, true)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
    }
}
