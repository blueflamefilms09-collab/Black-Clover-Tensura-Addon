package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.elite.world.WorldConfig;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import com.newuniverse.nusmp.elite.world.rules.MoveScheduler.Move;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the second calamity boss, an ancient lich that wakes on a new-moon night in a place thick with magicule. The design is the mod's own (the original is not available). It FIGHTS WITH
 * SPELLS: the void bolt ({@link VoidBoltProjectile}), a fan of them, a cursed ring on the ground, a blink away from anyone who gets close, and the dead called from the grave. Its soul is not in
 * its body: in phase 2 (66%) it raises PHYLACTERIES ({@link PhylacteryEntity}) and is shielded until every one is broken (the shield shatters, the Lich staggers and takes extra damage), and in
 * phase 3 (33%) it falls into a frenzy: a rain of void bolts from the sky and the Death Nova, a huge wave that only cover stops (a block between you and the Lich), and a second, smaller set
 * of phylacteries. It never heals. Every effect is Dark Rukh. It is a {@link CalamityBoss}: phases from the data file, the mod's damage budget and hit cap, a Calamity Core when it falls.
 */
public class LichBoss extends CalamityBoss {
    public static final String ID = "lich_calamity";

    private static final List<Move> MOVES = List.of(
            new Move("bolt", 0, 18, 10, 36, 3.0, 30.0, 6),
            new Move("volley", 0, 26, 14, 130, 4.0, 30.0, 4),
            new Move("curse_ring", 0, 36, 14, 180, 0.0, 24.0, 3),
            new Move("blink", 0, 12, 6, 170, 0.0, 5.5, 6),
            new Move("raise_skeletons", 0, 46, 18, 520, 0.0, 45.0, 2),
            new Move("void_rain", 1, 44, 18, 260, 0.0, 36.0, 4),
            new Move("death_nova", 2, 58, 26, 440, 0.0, 30.0, 4));

    private final List<UUID> phylacteries = new ArrayList<>();
    private final List<Vec3> rainSpots = new ArrayList<>();
    private int noVesselTicks;

    public LichBoss(EntityType<? extends LichBoss> type, Level level) { super(type, level); }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes() { return bossAttributes(1800.0, 10.0, 0.26, 4.0); }

    @Override public String defaultCalamityId() { return ID; }

    @Override protected List<Move> moves() { return MOVES; }

    @Override public RukhFx.Alignment rukh() { return RukhFx.Alignment.DARK; }

    @Override protected double meleeReach() { return 2.2; }

    private float atk() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }

    private Vec3 hand() { return position().add(0, getBbHeight() * 0.8, 0); }

    // ================================================================ telegraphs

    @Override
    protected void telegraph(Move m, LivingEntity target, Vec3 aim, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 here = position();
        if (left % 3 != 0 && left != total) return;
        Vec3 dir = BossFx.dirTo(here, aim);
        switch (m.id()) {
            case "bolt", "volley" -> {
                BossFx.line(sl, rukh(), here, dir.x, dir.z, 12.0, 6);
                RukhFx.gather(sl, this, rukh(), 4);
            }
            case "curse_ring" -> BossFx.ring(sl, rukh(), aim, 3.4, 12);
            case "blink" -> RukhFx.gather(sl, this, rukh(), 6);
            case "raise_skeletons" -> BossFx.ring(sl, rukh(), here, 5.0, 8);
            case "void_rain" -> {
                if (rainSpots.isEmpty() || left == total) pickRain(aim);
                for (Vec3 s : rainSpots) BossFx.ring(sl, rukh(), s, 1.4, 5);
            }
            case "death_nova" -> {
                BossFx.ring(sl, rukh(), here, 12.0, 12);
                BossFx.ring(sl, rukh(), here, 12.0 * (1.0 - left / (double) total), 12);
                if (left == total) {
                    for (LivingEntity e : foesAround(here, 24.0)) if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("nusmp.elite.lich.nova"), true);
                }
            }
            default -> { }
        }
    }

    private void pickRain(Vec3 aim) {
        rainSpots.clear();
        for (int i = 0; i < 7; i++) {
            double a = rng().nextDouble() * Math.PI * 2, r = i == 0 ? 0.0 : 2.0 + rng().nextDouble() * 5.0;
            Vec3 g = ground(aim.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (g != null) rainSpots.add(g);
        }
    }

    // ================================================================ the spells

    @Override
    protected int execute(Move m, LivingEntity target, Vec3 aim) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        Vec3 from = hand();
        switch (m.id()) {
            case "bolt" -> {
                Vec3 to = (target != null ? target.position().add(0, target.getBbHeight() * 0.6, 0) : aim.add(0, 1, 0));
                VoidBoltProjectile.launch(sl, this, from, to.subtract(from), 1.1f, atk() * 0.9f, 0);
                return 0;
            }
            case "volley" -> {
                int n = 3 + 2 * Math.min(2, phase());
                Vec3 base = aim.add(0, 1.0, 0).subtract(from);
                double yaw = Math.atan2(base.z, base.x);
                for (int i = 0; i < n; i++) {
                    double a = yaw + Math.toRadians((i - (n - 1) / 2.0) * 12.0);
                    VoidBoltProjectile.launch(sl, this, from, new Vec3(Math.cos(a) * Math.hypot(base.x, base.z), base.y, Math.sin(a) * Math.hypot(base.x, base.z)), 1.0f, atk() * 0.8f, 0);
                }
                return 0;
            }
            case "curse_ring" -> {
                for (LivingEntity e : foesAround(aim, 3.4)) {
                    if (strike(e, atk() * 1.2f, 0.4) > 0f) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                }
                RukhFx.burst(sl, aim.add(0, 0.5, 0), rukh(), 12, 2.0);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(aim), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0f, 0.6f);
                return 0;
            }
            case "blink" -> {
                Vec3 from0 = position();
                Vec3 to = target == null ? from0 : groundNear(target.position(), 13.0);
                for (int i = 0; i < 6 && target != null && to.distanceToSqr(target.position()) < 81.0; i++) to = groundNear(target.position(), 14.0);
                RukhFx.burst(sl, from0.add(0, 1.2, 0), rukh(), 12, 0.8);
                teleportTo(to.x, to.y, to.z);
                RukhFx.burst(sl, to.add(0, 1.2, 0), rukh(), 12, 0.8);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(to), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.5f);
                return 0;
            }
            case "raise_skeletons" -> {
                int room = Math.max(0, tuning().addCap() - helperCount());
                int n = Math.min(room, 2 + phase());
                for (int i = 0; i < n; i++) summonHelper(rng().nextInt(4) == 0 ? EntityType.STRAY : EntityType.SKELETON, groundNear(position(), 6.0));
                sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8f, 1.6f);
                return 0;
            }
            case "void_rain" -> {
                if (rainSpots.isEmpty()) pickRain(aim);
                return 3 * Math.max(1, rainSpots.size());
            }
            case "death_nova" -> {
                Vec3 here = position();
                for (LivingEntity e : foesAround(here, 12.0)) if (BossFx.clearLine(sl, this, e)) strike(e, atk() * 2.6f, 1.5);
                for (int r = 2; r <= 12; r += 2) BossFx.ring(sl, rukh(), here, r, 12);
                RukhFx.burst(sl, here.add(0, 1.0, 0), rukh(), 12, 4.0);
                sl.playSound(null, blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0f, 0.5f);
                return 0;
            }
            default -> { return 0; }
        }
    }

    @Override
    protected void tickActive(Move m, LivingEntity target, Vec3 aim, int elapsed, int total) {
        if (!(level() instanceof ServerLevel sl) || !m.id().equals("void_rain") || elapsed % 3 != 0) return;
        int i = elapsed / 3;
        if (i >= rainSpots.size()) return;
        Vec3 spot = rainSpots.get(i);
        VoidBoltProjectile.launch(sl, this, spot.add(0, 11.0, 0), new Vec3(0, -1, 0), 1.2f, atk() * 0.9f, 0);
        if (i == rainSpots.size() - 1) rainSpots.clear();
    }

    // ================================================================ the phylacteries

    @Override
    protected void onPhaseStart(int phase) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = phase == 1 ? 3 : 2;
        double hp = (phase == 1 ? 100.0 : 70.0) * (def() == null ? 1.0 : def().healthMult()) * WorldConfig.calamityHealth();
        phylacteries.clear();
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * (i + rng().nextDouble() * 0.4) / n;
            Vec3 g = ground(position().add(Math.cos(a) * 9.0, 0, Math.sin(a) * 9.0));
            Vec3 spot = g == null ? position().add(Math.cos(a) * 7.0, 0, Math.sin(a) * 7.0) : g;
            phylacteries.add(PhylacteryEntity.spawn(sl, spot, this, hp).getUUID());
        }
        setShield(true);
        noVesselTicks = 0;
    }

    public int phylacteriesLeft() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int n = 0;
        for (UUID id : phylacteries) {
            Entity e = sl.getEntity(id);
            if (e != null && e.isAlive()) n++;
        }
        return n;
    }

    /** A phylactery was broken. When it was the last one the shield shatters. */
    public void phylacteryDown(PhylacteryEntity p) {
        if (!(level() instanceof ServerLevel sl) || !shielded()) return;
        phylacteries.remove(p.getUUID());
        if (phylacteriesLeft() > 0) {
            CalamityManager.say(sl, this, Component.translatable("nusmp.elite.lich.vessel", phylacteriesLeft()));
            return;
        }
        breakShield(sl);
    }

    private void breakShield(ServerLevel sl) {
        setShield(false);
        stagger(80);
        RukhFx.burst(sl, this, rukh(), 12);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
        CalamityManager.say(sl, this, Component.translatable("nusmp.elite.lich.shield_broken"));
    }

    @Override protected float damageTakenFactor(DamageSource source) { return shielded() ? 0.03f : 1f; }

    @Override
    protected void bossTick(ServerLevel level, LivingEntity target) {
        if (shielded() && level.getGameTime() % 20 == 0) {
            if (phylacteriesLeft() == 0) {
                if (++noVesselTicks >= 3) breakShield(level);                                // no vessel left (lost or unloaded): the shield cannot hold on nothing
            } else {
                noVesselTicks = 0;
            }
            RukhFx.aura(level, this, rukh());
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            for (UUID id : phylacteries) {
                Entity e = sl.getEntity(id);
                if (e != null) e.discard();
            }
            phylacteries.clear();
        }
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }

    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag l = new ListTag();
        for (UUID id : phylacteries) l.add(NbtUtils.createUUID(id));
        tag.put("Vessels", l);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phylacteries.clear();
        for (net.minecraft.nbt.Tag t : tag.getList("Vessels", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) phylacteries.add(NbtUtils.loadUUID(t));
    }
}
