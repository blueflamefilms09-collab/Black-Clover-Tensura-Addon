package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.elite.world.rules.MoveScheduler.Move;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the first calamity boss, a heavy undead golem of grave soil and bone that climbs out of the ground when a region has been dense with magicule for too long. The design is the mod's own
 * (the original is not available): a slow, enormous bruiser whose danger is the GROUND. Phase 1 "Stirring": it swings, slams (a ring wave with spikes after it) and raises a line of Gaia spikes
 * toward you. Phase 2 "Grave Rising" (66%): it calls the dead to fight beside it and charges in a straight line. Phase 3 "Collapse" (33%): wide rending blows, and the Collapse, three waves
 * that break the whole floor of the arena, which only those who are off the ground (jumping, flying) escape. Everything is telegraphed with Light Rukh on the ground for the whole wind-up.
 * It is a {@link CalamityBoss}: phases from the data file, the mod's damage budget and hit cap, no regeneration, and a Calamity Core when it falls.
 */
public class GraveboundColossus extends CalamityBoss {
    public static final String ID = "gravebound_colossus";

    private static final List<Move> MOVES = List.of(
            new Move("swing", 0, 22, 14, 40, 0.0, 7.5, 6),
            new Move("slam", 0, 34, 16, 140, 0.0, 10.0, 5),
            new Move("quake_line", 0, 30, 14, 170, 4.0, 26.0, 4),
            new Move("raise_dead", 1, 50, 20, 560, 0.0, 45.0, 3),
            new Move("charge", 1, 28, 16, 230, 8.0, 30.0, 3),
            new Move("grave_rend", 2, 42, 22, 210, 0.0, 13.0, 4),
            new Move("collapse", 2, 74, 30, 640, 0.0, 34.0, 3));

    private final Set<Integer> chargeHit = new HashSet<>();
    private Vec3 chargeDir = Vec3.ZERO;

    public GraveboundColossus(EntityType<? extends GraveboundColossus> type, Level level) { super(type, level); }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes() { return bossAttributes(2400.0, 14.0, 0.24, 10.0); }

    @Override public String defaultCalamityId() { return ID; }

    @Override protected List<Move> moves() { return MOVES; }

    @Override public RukhFx.Alignment rukh() { return RukhFx.Alignment.LIGHT; }

    @Override protected double meleeReach() { return 3.6; }

    private float atk() { return (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE); }

    // ================================================================ telegraphs

    @Override
    protected void telegraph(Move m, LivingEntity target, Vec3 aim, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 here = position();
        Vec3 dir = BossFx.dirTo(here, aim);
        if (left % 3 != 0 && left != total) return;
        switch (m.id()) {
            case "swing" -> BossFx.cone(sl, rukh(), here, dir.x, dir.z, 7.5, 55.0);
            case "slam" -> BossFx.ring(sl, rukh(), here, 7.0 * (1.0 - 0.6 * left / (double) total), 12);
            case "quake_line" -> BossFx.line(sl, rukh(), here, dir.x, dir.z, 24.0, 12);
            case "raise_dead" -> BossFx.ring(sl, RukhFx.Alignment.DARK, here, 6.0, 8);
            case "charge" -> BossFx.line(sl, rukh(), here, dir.x, dir.z, 16.0, 12);
            case "grave_rend" -> BossFx.cone(sl, rukh(), here, dir.x, dir.z, 12.0, 55.0);
            case "collapse" -> {
                BossFx.ring(sl, rukh(), here, 14.0, 12);
                BossFx.ring(sl, rukh(), here, 14.0 * (1.0 - left / (double) total), 12);
                if (left == total) {
                    for (LivingEntity e : foesAround(here, 20.0)) if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("nusmp.elite.colossus.collapse"), true);
                }
            }
            default -> { }
        }
    }

    // ================================================================ the blows

    @Override
    protected int execute(Move m, LivingEntity target, Vec3 aim) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        Vec3 here = position();
        Vec3 dir = BossFx.dirTo(here, aim);
        switch (m.id()) {
            case "swing" -> {
                for (LivingEntity e : foesAround(here, 8.5)) if (BossFx.inCone(here, dir.x, dir.z, e.position(), 7.5, 55.0)) strike(e, atk() * 1.0f, 1.4);
                sound(SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.6f);
                RukhFx.burst(sl, here.add(dir.scale(3.0)).add(0, 1.2, 0), rukh(), 8);
                return 0;
            }
            case "slam" -> {
                for (LivingEntity e : foesAround(here, 7.0)) strike(e, atk() * 1.3f, 1.6);
                ring(sl, here, 4.0, 6, 8, 0.7f);
                ring(sl, here, 8.0, 10, 16, 0.7f);
                sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.5f);
                RukhFx.burst(sl, here.add(0, 0.3, 0), rukh(), 12, 2.5);
                return 0;
            }
            case "quake_line" -> {
                int n = 12;
                for (int i = 1; i <= n; i++) {
                    Vec3 p = ground(here.add(dir.scale(2.0 * i)));
                    if (p != null) GaiaSpikeEntity.raise(sl, p, this, 6 + i * 2, atk() * 0.9f);
                }
                sound(SoundEvents.POINTED_DRIPSTONE_LAND, 1.5f, 0.5f);
                return 0;
            }
            case "raise_dead" -> {
                summonWave(Math.max(2, tuning().addCap() - helperCount()));
                return 0;
            }
            case "charge" -> {
                chargeDir = dir;
                chargeHit.clear();
                sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.6f);
                return 14;
            }
            case "grave_rend" -> {
                for (LivingEntity e : foesAround(here, 13.0)) if (BossFx.inCone(here, dir.x, dir.z, e.position(), 12.0, 55.0)) strike(e, atk() * 2.2f, 2.0);
                for (int i = 1; i <= 5; i++) {
                    Vec3 p = ground(here.add(dir.scale(2.4 * i)));
                    if (p != null) GaiaSpikeEntity.raise(sl, p, this, 4 + i, atk() * 0.8f);
                }
                sound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.4f);
                return 0;
            }
            case "collapse" -> {
                return 40;                                               // three waves, at 0, 14 and 28 ticks of the active part
            }
            default -> { return 0; }
        }
    }

    @Override
    protected void tickActive(Move m, LivingEntity target, Vec3 aim, int elapsed, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (m.id().equals("charge")) {
            setDeltaMovement(chargeDir.x * 1.0, getDeltaMovement().y, chargeDir.z * 1.0);
            hurtMarked = true;
            for (LivingEntity e : foesAround(position(), 3.2)) if (chargeHit.add(e.getId())) strike(e, atk() * 1.6f, 1.8);
            if (elapsed % 2 == 0) RukhFx.burst(sl, position().add(0, 0.4, 0), rukh(), 4, 1.2);
        } else if (m.id().equals("collapse") && elapsed % 14 == 0) {
            Vec3 here = position();
            for (LivingEntity e : foesAround(here, 14.0)) if (e.onGround()) strike(e, atk() * 2.4f, 1.0);       // only those on the ground are caught
            for (int i = 0; i < 12; i++) {
                Vec3 p = groundNear(here, 13.0);
                GaiaSpikeEntity.raise(sl, p, this, 3, atk() * 0.6f);
            }
            RukhFx.burst(sl, here.add(0, 0.3, 0), rukh(), 12, 5.0);
            sound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.35f + 0.15f * (elapsed / 14));
        }
    }

    private void ring(ServerLevel sl, Vec3 centre, double radius, int count, int delay, float dmg) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3 p = ground(centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
            if (p != null) GaiaSpikeEntity.raise(sl, p, this, delay, atk() * dmg);
        }
    }

    private void sound(SoundEvent s, float volume, float pitch) {
        if (level() instanceof ServerLevel sl) sl.playSound(null, blockPosition(), s, SoundSource.HOSTILE, volume, pitch);
    }

    // ================================================================ phases and helpers

    @Override
    protected void onPhaseStart(int phase) {
        summonWave(phase == 1 ? 3 : 5);
    }

    /** Calls up to {@code want} of the dead (within the phase's cap). */
    private void summonWave(int want) {
        int room = Math.max(0, tuning().addCap() - helperCount());
        int n = Math.min(want, room);
        for (int i = 0; i < n; i++) {
            boolean skeleton = rng().nextBoolean();
            summonHelper(skeleton ? EntityType.SKELETON : EntityType.ZOMBIE, groundNear(position(), 7.0));
        }
        if (n > 0) sound(SoundEvents.WITHER_SPAWN, 0.8f, 1.4f);
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f); }
}
